package util;

import phrases.Phrase;

import java.util.*;

/**
 * // todo: describe algorithm and usage
 * Created by gpfinley on 10/11/16.
 */
public class PhraseGraph {

    private final Map<String, Object> graph;

    public PhraseGraph() {
        graph = new HashMap<>();
    }

    public PhraseGraph(Iterable<Phrase> phrases) {
        graph = new HashMap<>();
        for (Phrase phrase : phrases) {
            addPhrase(phrase);
        }
    }

    public boolean addPhrase(Phrase phrase) {
        List<String> words = new ArrayList<>(phrase.getWords());
        Map<String, Object> addToThisMap = graph;
        while (true) {
            String firstWord = words.get(0);
            addToThisMap.putIfAbsent(firstWord, new HashMap<String, Map>());
            addToThisMap = (Map) addToThisMap.get(firstWord);
            words.remove(0);
            if (words.size() == 0) {
                if (addToThisMap.containsKey(null)) return false;
                addToThisMap.put(null, phrase);
                return true;
            }
        }
    }

    public boolean removePhrase(Phrase phrase) {
        Map<String, Object> whereInGraph = graph;
        try {
            for (String word : phrase.getWords()) {
                whereInGraph = (Map) whereInGraph.get(word);
            }
            whereInGraph.remove(null);
            return true;
        } catch (NullPointerException e) {
            return false;
        }
    }

    /**
     *
     * @param words a list of tokens
     * @param index the index to start looking in that list
     * @return the longest possible phrase, or null if none found from this index
     */
    public Phrase getLongestPhraseFrom(List<String> words, int index) {
        Phrase lastEligiblePhrase = null;
        Map<String, Object> lookup = graph;
        int i;
        for (i = index; i < words.size(); i++) {
            String thisWord = words.get(i);
            lastEligiblePhrase = (Phrase) lookup.getOrDefault(null, lastEligiblePhrase);
            if (lookup.containsKey(thisWord)) {
                lookup = (Map) lookup.get(words.get(i));
            } else {
                break;
            }
        }
        return lastEligiblePhrase;
    }

    // todo: is this as quick as just copying the above code for arrays?
    public Phrase getLongestPhraseFrom(String[] words, int index) {
        return getLongestPhraseFrom(Arrays.asList(words), index);
    }

    // todo: temp (test)
    public static void main(String[] args) {
        String text = "disambiguation in clinical texts is a problem handled well by fully supervised machine learning methods. Acquiring training data, however, is expensive and would be impractical for large numbers of abbreviations in specialized corpora. An alternative is a semi-supervised approach, in which training data are automatically generated by substituting long forms in natural text with their corresponding abbreviations. Most prior implementations of this method either focus on very few abbreviations or do not test on real-world data. We present a realistic use case by testing several semi-supervised ";
        String[] words = text.split("\\s+");
        PhraseGraph g = new PhraseGraph();
        g.addPhrase(new Phrase("well by"));
        g.addPhrase(new Phrase("training data"));
        g.addPhrase(new Phrase("large numbers"));
        g.addPhrase(new Phrase("large numbers of abbreviations"));
        g.addPhrase(new Phrase("numbers of abbreviations"));

        for (int i=0; i<words.length; i++) {
            System.out.println(g.getLongestPhraseFrom(Arrays.asList(words), i));
        }
        g.removePhrase(new Phrase("large numbers of abbreviations"));
        for (int i=0; i<words.length; i++) {
            System.out.println(g.getLongestPhraseFrom(Arrays.asList(words), i));
        }
    }

}
