package util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Class that implements an efficient algorithm for stepwise clustering based on maximum similarity
 *
 * todo: document usage/api
 *
 * Created by gpfinley on 9/30/16.
 */
public class StepwiseClustering<T> {

    private final static Logger LOGGER = Logger.getLogger(StepwiseClustering.class.getName());

    private final List<T> items;
    private final int N;
    private final Map<T, Integer> indexOf;
    private final List<Link> links;
    private Map<Integer, Integer> clusterInCluster = new HashMap<>();
    private int[] partOf;
    private boolean[] hasBeenLinked;

    private Set<Set<T>> allClusters;
    private Map<T, Set<T>> myClusterMap;

    public StepwiseClustering(Iterable<T> initItems) {
        items = new ArrayList<>();
        indexOf = new HashMap<>();
        links = new ArrayList<>();
        for (T item : initItems) {
            indexOf.put(item, items.size());
            items.add(item);
        }
        N = items.size();
    }

    public void addLink(T from, T to, double score) {
        if (indexOf.containsKey(from) && indexOf.containsKey(to)) {
            links.add(new Link(indexOf.get(from), indexOf.get(to), score));
        } else {
            if (!indexOf.containsKey(from)) {
                LOGGER.warning("Clustering domain does not include " + from);
            }
            if (!indexOf.containsKey(to)) {
                LOGGER.warning("Clustering domain does not include " + to);
            }
        }
    }

    public void cluster(int k) {
        LOGGER.info("Clustering " + N + " nodes into " + k + " clusters...");
        links.sort((x, y) -> y.score.compareTo(x.score));
        int nClusters = N;
        int nNewClusters = 0;
        hasBeenLinked = new boolean[nClusters];
        partOf = new int[nClusters];
        clusterInCluster = new HashMap<>();

        for (Link link : links) {
            if (nClusters <= k) break;
            int a = link.one;
            int b = link.two;
            if (a == b) continue;
            if (!hasBeenLinked[a] && !hasBeenLinked[b]) {
                partOf[a] = nNewClusters;
                partOf[b] = nNewClusters;
                hasBeenLinked[a] = true;
                hasBeenLinked[b] = true;
                nClusters--;
                nNewClusters++;
            } else if (hasBeenLinked[a] && !hasBeenLinked[b]) {
                hasBeenLinked[b] = true;
                partOf[b] = trace(a);
                nClusters--;
            } else if (!hasBeenLinked[a] && hasBeenLinked[b]) {
                hasBeenLinked[a] = true;
                partOf[a] = partOf[b];
                nClusters--;
            } else { // if (hasBeenLinked[a] && hasBeenLinked[b]){
                int aCluster = trace(a);
                partOf[a] = aCluster;
                int bCluster = trace(b);
                partOf[b] = bCluster;
                if (aCluster != bCluster) {
                    clusterInCluster.put(aCluster, nNewClusters);
                    clusterInCluster.put(bCluster, nNewClusters);
                    nNewClusters++;
                    nClusters--;
                }
            }
        }
        if (nClusters > k) {
            LOGGER.warning("Exhausted all vertices and still have " + nClusters + " clusters");
        }

        LOGGER.info("Building map of nodes to their biggest cluster");
        final Map<T, Set<T>> itemToCluster = new HashMap<>();
        Map<Integer, Set<T>> intermediateMap = new HashMap<>();
        for (int i=0; i<N; i++) {
            if (hasBeenLinked[i]) {
                T item = items.get(i);
                int clusterId = trace(i);
                intermediateMap.putIfAbsent(clusterId, new HashSet<>());
                intermediateMap.get(clusterId).add(item);
            }
        }
        allClusters = new HashSet<>();
        for (Set<T> cluster : intermediateMap.values()) {
            allClusters.add(cluster);
            cluster.forEach(T -> itemToCluster.put(T, cluster));
        }
        for (int i=0; i<N; i++) {
            if (!hasBeenLinked[i]) {
                Set<T> singletonCluster = Collections.singleton(items.get(i));
                allClusters.add(singletonCluster);
                itemToCluster.put(items.get(i), singletonCluster);
            }
        }
        myClusterMap = itemToCluster;
    }

    public Set<T> getClusterOf(T item) {
        return myClusterMap.get(item);
    }

    public Map<T, Set<T>> getMyClusterMap() {
        return myClusterMap;
    }

    /**
     * Save all clusters to a text file, one cluster per line with custom separator.
     * Not useful unless T has a meaningful toString() method.
     * @param fileName
     * @throws IOException
     */
    public void saveClusters(String fileName, String itemSeparator) throws IOException {
        LOGGER.info("Writing clusters to file...");
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
        for (Set<T> set: allClusters) {
            StringBuilder builder = new StringBuilder();
            set.forEach(val -> {
                builder.append(itemSeparator);
                builder.append(val.toString());
            });
            builder.append("\n");
            writer.write(builder.substring(1));
        }
        writer.close();
    }

    public Set<Set<T>> getAllClusters() {
        return allClusters;
    }

    /**
     * Find the biggest supercluster that this element is a part of
     * @param index
     * @return
     */
    private int trace(int index) {
        int cluster = partOf[index];
        while (clusterInCluster.containsKey(cluster)) {
            cluster = clusterInCluster.get(cluster);
        }
        return cluster;
    }

    private static class Link {
        int one;
        int two;
        Double score;
        Link(int one, int two, Double score) {
            this.one = one;
            this.two = two;
            this.score = score;
        }
        @Override
        public String toString() {
            return one + " " + two + " " + score;
        }
        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Link)) return false;
            Link linkOther = (Link) other;
            return one == linkOther.one && two == linkOther.two;
        }
        @Override
        public int hashCode() {
            return (one + " " + two + " " + score).hashCode();
        }
    }

}
