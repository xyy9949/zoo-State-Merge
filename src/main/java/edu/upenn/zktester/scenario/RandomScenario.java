package edu.upenn.zktester.scenario;

import edu.upenn.zktester.ensemble.ZKEnsemble;
import edu.upenn.zktester.fault.FaultGenerator;
import edu.upenn.zktester.subset.MinimalQuorumGenerator;
import edu.upenn.zktester.subset.RandomSubsetGenerator;
import edu.upenn.zktester.util.Assert;
import edu.upenn.zktester.util.AssertionFailureError;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class RandomScenario implements Scenario {

    private static final Logger LOG = LoggerFactory.getLogger(RandomScenario.class);

    private static final int TOTAL_SERVERS = 3;
    private static final int QUORUM_SIZE = 2;
    private static final List<String> KEYS = List.of("/key0", "/key1");

    private final int numExecutions;
    private final int numPhases;
    private final Random random;
    private final MinimalQuorumGenerator quorumGenerator;
    private final RandomSubsetGenerator subsetGenerator;
    private final FaultGenerator faultGenerator;
    private final FaultGenerator requestGenerator;
    private final ZKEnsemble zkEnsemble;

    public RandomScenario(final int numExecutions, final int numPhases, final int faultBudget, final int requestBudget) {
        this.numExecutions = numExecutions;
        this.numPhases = numPhases;
        this.random = new Random();
        this.quorumGenerator = new MinimalQuorumGenerator(TOTAL_SERVERS, random);
        this.subsetGenerator = new RandomSubsetGenerator(random);
        this.faultGenerator = new FaultGenerator(numPhases, QUORUM_SIZE - 1, faultBudget, random);
        this.requestGenerator = new FaultGenerator(numPhases, 1, requestBudget, random);
        this.zkEnsemble = new ZKEnsemble(TOTAL_SERVERS);
    }

    @Override
    public void init() throws IOException {
        zkEnsemble.init();
    }

    @Override
    public void execute() {
        int failedAssertions = 0;
        int failedOtherwise = 0;

        for (int i = 1; i <= numExecutions; ++i) {
            final long seed = random.nextLong();
            random.setSeed(seed);
            LOG.info("Starting execution {}: seed = {}", i, seed);
            try {
                singleExecution();
            } catch (final Exception e) {
                LOG.error("Exception while executing scenario", e);
                ++failedOtherwise;
            } catch (final AssertionFailureError e) {
                LOG.error("Assertion failed", e);
                ++failedAssertions;
            }
            LOG.info("Finished execution {}: seed = {}", i, seed);
        }

        LOG.info("Finished executions:\n\tFailed assertions: {}\tFailed otherwise: {}\tTotal: {}",
                failedAssertions, failedOtherwise, numExecutions);
    }

    private void singleExecution() throws Exception {
        try (final AutoCloseable cleanUp = () -> {
            zkEnsemble.stopEnsemble();
            faultGenerator.reset();
            requestGenerator.reset();
        }) {
            zkEnsemble.startEnsemble();

            // We have an initial phase in which we create the znodes
            final int leader = zkEnsemble.getLeader();
            zkEnsemble.handleRequest(leader, zk -> {
                for (final var key : KEYS) {
                    zk.create(key, Integer.toString(leader).getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                    LOG.info("Initial association: {} -> {}", key, leader);
                }
            });
            zkEnsemble.stopAllServers();

//            final List<List<Integer>> toStart = List.of(List.of(0, 1), List.of(0, 2), List.of(1, 2));
//            final List<List<Integer>> toCrash = List.of(List.of(0), List.of(), List.of());
//            final List<Boolean> request = List.of(true, false, true);
//            final List<Integer> ids = List.of(1, 0, 2);
//            final List<String> keys = List.of("/key0", "", "/key1");

            for (int phase = 1; phase <= numPhases; ++phase) {
                final List<Integer> serversToStart = quorumGenerator.generate();
//                final List<Integer> serversToStart = toStart.get(phase - 1);
                zkEnsemble.startServers(serversToStart);

                final int faults = faultGenerator.generate();
                final List<Integer> serversToCrash = subsetGenerator.generate(serversToStart.size(), faults).stream()
                        .map(i -> serversToStart.get(i)).collect(Collectors.toList());
//                final List<Integer> serversToCrash = toCrash.get(phase - 1);
                zkEnsemble.crashServers(serversToCrash);

                // Randomly choose whether to make a client request
                if (requestGenerator.generate() == 1) {
//                if (request.get(phase - 1)) {
                    final List<Integer> serversStillRunning = serversToStart.stream()
                            .filter(i -> !serversToCrash.contains(i)).collect(Collectors.toList());
                    final int id = serversStillRunning.get(random.nextInt(serversStillRunning.size()));
//                    final int id = ids.get(phase - 1);
                    final String key = KEYS.get(random.nextInt(KEYS.size()));
//                    final String key = keys.get(phase - 1);
                    final int value = 100 * phase + id;
                    final byte[] rawValue = Integer.toString(value).getBytes();
                    LOG.info("Initiating request to {}: set {} -> {}", id, key, value);
                    zkEnsemble.handleRequest(id, zk -> {
                        zk.setData(key, rawValue, -1, null, null);
                        Thread.sleep(1000);
                        System.gc();
                    });
                }

                zkEnsemble.stopServers(serversToStart);
            }

            zkEnsemble.startAllServers();
            Assert.assertTrue("All keys on all servers should have the same value", checkProperty());
        }
    }

    private boolean checkProperty() throws KeeperException, InterruptedException {
        return zkEnsemble.checkProperty(zookeepers -> {
            boolean result = true;
            for (final var key : KEYS) {
                final ZooKeeper zk0 = zookeepers.get(0);
                final byte[] rawValue0 = zk0.getData(key, false, null);
                LOG.info("{}\n\tAssociation: {} -> {}", zk0.toString(), key, new String(rawValue0));

                final boolean valueOK = zookeepers.subList(1, zookeepers.size()).stream()
                        .allMatch(zk -> {
                            try {
                                final byte[] rawValue = zk.getData(key, false, null);
                                LOG.info("{}\n\tAssociation: {} -> {}", zk.toString(), key, new String(rawValue));
                                return Arrays.equals(rawValue0, rawValue);
                            } catch (final KeeperException | InterruptedException e) {
                                return false;
                            }
                        });
                result = result && valueOK;
            }
            return result;
        });
    }
}