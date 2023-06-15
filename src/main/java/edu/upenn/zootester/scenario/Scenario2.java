package edu.upenn.zootester.scenario;

import edu.upenn.zootester.ensemble.ZKEnsemble;
import edu.upenn.zootester.ensemble.ZKProperty;
import edu.upenn.zootester.harness.SequentialConsistency;
import edu.upenn.zootester.util.Assert;
import edu.upenn.zootester.util.Config;
import edu.upenn.zootester.util.PropertyModUtil;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Scenario2 implements Scenario {

    private static final Logger LOG = LoggerFactory.getLogger(Scenario2.class);

    private static final int TOTAL_SERVERS = 3;

    private final ZKEnsemble zkEnsemble = new ZKEnsemble(TOTAL_SERVERS);
    private static final List<String> KEYS = List.of("/key0", "/key1");
    private static final ZKProperty CONSISTENT_VALUES = new SequentialConsistency(KEYS,
            Set.of(Map.of("/key0", 0, "/key1", 1001),
                    Map.of("/key0", 1000, "/key1", 1001)));

    @Override
    public void init(final Config config) throws IOException {
        zkEnsemble.init();
    }

    @Override
    public void execute() throws Exception {
        try {
            zkEnsemble.startEnsemble();
            final int srvC = zkEnsemble.getLeader();
            final int srvA = (srvC + 1) % TOTAL_SERVERS;
            final int srvB = (srvC + 2) % TOTAL_SERVERS;

            Assert.assertTrue("There should be a leader", srvC >= 0);

            // Create initial znodes
            PropertyModUtil.setNodeNum(3);
            PropertyModUtil.setRequestId(0);
            zkEnsemble.handleRequest(srvC, (zk, serverId) -> {
                zk.create(KEYS.get(0), "0".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                zk.create(KEYS.get(1), "1".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            });

            PropertyModUtil.setRequestId(1);
            zkEnsemble.handleRequest(srvB, (zk, serverId) -> {
                zk.setData(KEYS.get(0), "1000".getBytes(), -1, null, null);
                Thread.sleep(500);
//                System.gc();
            });

            zkEnsemble.stopAllServers();
            zkEnsemble.startAllServers();
            zkEnsemble.checkProperty(CONSISTENT_VALUES);

            PropertyModUtil.setRequestId(2);
            zkEnsemble.handleRequest(srvA, (zk, serverId) -> {
                zk.setData(KEYS.get(1), "1001".getBytes(), -1, null, null);
                Thread.sleep(500);
//                System.gc();
            });

            final boolean result = zkEnsemble.checkProperty(CONSISTENT_VALUES);
            Assert.assertTrue("All keys on all servers should have the same value", result);
        }
        catch (Exception e)
        {
            LOG.error(e.getMessage());
        }
        finally {
            Thread.sleep(5000);
            zkEnsemble.stopEnsemble();
            zkEnsemble.tearDown();
        }
    }
}
