package thesaurus;

import io.Word2vecReader;
import phrases.Phrase;
import util.StepwiseClustering;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * todo: document (how is this different from regular lookup thesaurus, etc.)
 *
 * Created by greg on 10/1/16.
 */
public class ClusteredThesaurus {

    private final static Logger LOGGER = Logger.getLogger(ClusteredThesaurus.class.getName());

    private Map<Phrase, Set<Phrase>> myClusterMap;
    private Map<Phrase, Phrase> lookupMap;
    private Set<Set<Phrase>> allClusters;

    private final StepwiseClustering<Phrase> stepwiseClustering;

    public ClusteredThesaurus(Map<Phrase, Integer> vocab, Thesaurus thesaurus, int k) {
        stepwiseClustering = new StepwiseClustering<>(vocab.keySet());

        LOGGER.info("Setting up clustered thesaurus with " + thesaurus.getAllEntries().size() + " pairs for " + thesaurus.numWords() + "entries");
        for (Thesaurus.FullEntry entry : thesaurus.getAllEntries()) {
            if (!entry.p1.equals(entry.p2) && vocab.containsKey(entry.p1) && vocab.containsKey(entry.p2)) {
                stepwiseClustering.addLink(entry.p1, entry.p2, entry.overallScore);
            }
        }
        
        stepwiseClustering.cluster(k);
        myClusterMap = stepwiseClustering.getMyClusterMap();
        lookupMap = new HashMap<>();
        allClusters = stepwiseClustering.getAllClusters();

        for (Set<Phrase> cluster : allClusters) {
            int maxFreq = 0;
            Phrase mostFrequent = cluster.iterator().next();
            for (Phrase phrase : cluster) {
                if (vocab.containsKey(phrase)) {
                    int freq = vocab.get(phrase);
                    if (freq > maxFreq) {
                        maxFreq = freq;
                        mostFrequent = phrase;
                    }
                } else {
                    System.out.println(phrase);
                }
            }
            for (Phrase phrase : cluster) {
                lookupMap.put(phrase, mostFrequent);
            }
        }
    }

    private ClusteredThesaurus() {
        stepwiseClustering = null;
        myClusterMap = new HashMap<>();
        lookupMap = new HashMap<>();
        allClusters = new HashSet<>();
    }

    public void save(String fileName) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
        for (Set<Phrase> cluster : allClusters) {
            // all Phrases in this cluster should give lookup to same term, so just use the first one
            Phrase headWord = lookup(cluster.iterator().next());
            System.out.println(headWord);
            System.out.println(cluster);
            writer.write(headWord.toString());
            for (Phrase phrase : cluster) {
                if (!phrase.equals(headWord)) {
                    writer.write("\t");
                    writer.write(phrase.toString());
                }
            }
            writer.write("\n");
        }
    }

    public static ClusteredThesaurus load(String fileName) throws IOException {
        ClusteredThesaurus thes = new ClusteredThesaurus();
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        String nextLine;
        while ((nextLine = reader.readLine()) != null) {
            String[] fields = nextLine.split("\\t");
            Phrase[] phrases = new Phrase[fields.length];
            Set<Phrase> thisCluster = new HashSet<>();
            for (int i=0; i<fields.length; i++) {
                phrases[i] = new Phrase(fields[i]);
                thisCluster.add(phrases[i]);
            }
            for (int i=1; i<phrases.length; i++) {
                thes.lookupMap.put(phrases[i], phrases[0]);
                thes.myClusterMap.put(phrases[i], thisCluster);
            }
            thes.myClusterMap.put(phrases[0], thisCluster);
            thes.allClusters.add(thisCluster);
        }
        return thes;
    }

    public Phrase lookup(Phrase from) {
        return lookupMap.getOrDefault(from, from);
    }

    public Set<Phrase> getCluster(Phrase phrase) {
        return myClusterMap.getOrDefault(phrase, Collections.singleton(phrase));
    }

    public List<Integer> getClusterSizes() {
//        final List<Integer> clustSizes = new ArrayList<>();
//        allClusters.forEach(c -> clustSizes.add(c.size()));
        List<Integer> clustSizes = allClusters.stream().map(Collection::size).collect(Collectors.toList());
        clustSizes.sort(null);
//        clustSizes.sort((x, y) -> y.compareTo(x));
        return clustSizes;
    }

}
