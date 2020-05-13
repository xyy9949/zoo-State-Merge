package edu.upenn.zktester.harness;

import edu.upenn.zktester.ensemble.ZKProperty;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SequentialConsistency implements ZKProperty {

    private final Logger LOG = LoggerFactory.getLogger(SequentialConsistency.class);

    private final List<String> keys;
    private final Set<Map<String, Integer>> possibleStates;

    /**
     * The property retrieves the data associated with keys from zookeeper nodes, and checks that all nodes
     * are in the same state, and that this state is one of the possible final states under sequential consistency.
     *
     * @param keys           List of keys
     * @param possibleStates Set of possible states (key->value maps)
     */
    public SequentialConsistency(final List<String> keys, final Set<Map<String, Integer>> possibleStates) {
        this.keys = keys;
        this.possibleStates = possibleStates;
    }

    @Override
    public boolean test(final List<ZooKeeper> clients, final List<Integer> clientForServer) {
        final List<Map<String, Integer>> states = retrieveStates(clients, clientForServer);
        final Map<String, Integer> first = states.get(0);

        // The first state is allowed
        return possibleStates.contains(first)

                // All other states are equal to the first one
                && states.subList(1, states.size()).stream().allMatch(first::equals);
    }

    private List<Map<String, Integer>> retrieveStates(final List<ZooKeeper> clients,
                                                      final List<Integer> clientForServer) {
        return IntStream.range(0, clientForServer.size()).mapToObj(
                serverId -> {
                    final int clientId = clientForServer.get(serverId);
                    final ZooKeeper zk = clients.get(clientId);
                    final Map<String, Integer> state = new HashMap<>();
                    keys.forEach(key -> {
                        try {
                            final byte[] rawValue = zk.getData(key, false, null);
                            final Integer value = Integer.valueOf(new String(rawValue));
                            LOG.info("Association @ {}: {} -> {}", serverId, key, value);
                            state.put(key, value);
                        } catch (KeeperException | InterruptedException e) {
                            LOG.error("Couldn't retrieve data from server {} (key = '{}')", serverId, key);
                        }
                    });
                    return state;
                }
        ).collect(Collectors.toList());
    }
}
