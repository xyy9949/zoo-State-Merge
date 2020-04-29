package edu.upenn.zktester.harness;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

class HarnessIterator implements Iterator<Harness> {

    private final int numKeys;
    private final int numNodes;
    private final int numPhases;
    private List<Request> requests;
    private List<Phase> phases;
    private boolean hasNext = true;

    public HarnessIterator(final int numKeys, final int numNodes, final int numRequests, final int numPhases) {
        this.numKeys = numKeys;
        this.numNodes = numNodes;
        this.numPhases = numPhases;
        this.requests = initialRequests(numRequests);
        this.phases = initialPhases(requests, numPhases);
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    @Override
    public Harness next() {
        // We clone the list as we'll be changing our copy
        final Harness harness = new Harness(new ArrayList<>(phases), numKeys);

        // Find the first occurrence of RequestPhase followed by an EmptyPhase and swap them
        boolean found = false;
        for (int i = 0; i + 1 < phases.size() && !found; ++i) {
            final int fst = i;
            final int snd = i + 1;
            found = phases.get(fst).match(
                    ignoreEmptyPhase -> false,
                    fstReqPhase -> phases.get(snd).match(
                            sndEmptyPhase -> {
                                Collections.swap(phases, fst, snd);
                                return true;
                            },
                            ignoreReqPhase -> false
                    )
            );
        }

        if (found) {
            hasNext = true;
            return harness;
        }

        // Move to the next list of requests
        if (hasNext = nextRequests(requests, numNodes, numKeys)) {
            phases = initialPhases(requests, numPhases);
        }
        return harness;
    }

    private enum RequestType {
        CONDITIONAL, UNCONDITIONAL
    }

    private static class Request {
        public int node;
        public int key;
        public int readsFrom;
        public RequestType type;

        public Request(final RequestType type, final int node, final int key, final int readsFrom) {
            this.type = type;
            this.node = node;
            this.key = key;
            this.readsFrom = readsFrom;
        }
    }

    private static List<Phase> initialPhases(final List<Request> requests, final int numPhases) {
        final List<Phase> phases = new ArrayList<>();
        for (int i = 0; i < requests.size(); ++i) {
            final Request request = requests.get(i);
            final String key = Harness.keyMapper(request.key);
            final int valueToWrite = 100 * (i + 1) + request.node;
            final Phase phase;
            if (RequestType.UNCONDITIONAL.equals(request.type)) {
                phase = new UnconditionalWritePhase(request.node, key, valueToWrite);
            } else {
                final int valueToRead;
                if (request.readsFrom >= 0) {
                    valueToRead = phases.get(request.readsFrom).match(
                            ignoreEmptyPhase -> 0,
                            requestPhase -> requestPhase.getValueToWrite()
                    );
                } else {
                    valueToRead = 0;
                }
                phase = new ConditionalWritePhase(request.node, key, valueToRead, valueToWrite);
            }
            phases.add(phase);
        }
        // Note that nCopies doesn't actually duplicate the object, it just stores the same
        // object n times. But this is OK in our case.
        phases.addAll(Collections.nCopies(numPhases - requests.size(), new EmptyPhase()));
        return phases;
    }

    private static List<Request> initialRequests(final int numRequests) {
        final List<Request> requests = new ArrayList<>();
        for (int i = 0; i < numRequests; ++i) {
            requests.add(new Request(RequestType.UNCONDITIONAL, 0, 0, -1));
        }
        return requests;
    }

    private static void reset(final List<Request> requests, final int from) {
        for (int i = from; i < requests.size(); ++i) {
            final Request request = requests.get(i);
            request.type = RequestType.UNCONDITIONAL;
            request.node = 0;
            request.key = 0;
            request.readsFrom = -1;
        }
    }

    private static boolean nextRequests(final List<Request> requests, final int numNodes, final int numKeys) {
        int i = requests.size() - 1;
        for ( ; i >= 0; --i) {
            final Request request = requests.get(i);
            if (request.type.equals(RequestType.UNCONDITIONAL)) {
                request.type = RequestType.CONDITIONAL;
                // Reads from the initial value
                request.readsFrom = -1;
                break;
            } else {
                // Is there a next requests before this one that sets the same key?
                final int nextReadsFrom = nextReadsFrom(requests, request.readsFrom + 1, request.key);
                if (nextReadsFrom != i) {
                    request.readsFrom = nextReadsFrom;
                    break;
                }
            }
            if (request.key + 1  < numKeys) {
                ++request.key;
                request.type = RequestType.UNCONDITIONAL;
                break;
            }
            if (request.node + 1 < numNodes) {
                ++request.node;
                request.key = 0;
                request.type = RequestType.UNCONDITIONAL;
                break;
            }
        }
        if (i >= 0) {
            reset(requests, i + 1);
            return true;
        }
        return false;
    }

    private static int nextReadsFrom(final List<Request> requests, int from, final int key) {
        while (requests.get(from).key != key) {
            ++from;
        }
        return from;
    }
}
