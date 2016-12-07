package run;

import phrases.Collocations;
import phrases.Phrase;
import textprocessing.TextProcessor;
import util.PropertiesLoader;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Create collocations through unsupervised means.
 * Needs text files of n-gram counts.
 *
 * Created by gpfinley on 7/18/16.
 */
public class CollocationsMaker {

    private final static Logger LOGGER = Logger.getLogger(CollocationsMaker.class.getName());

    private final static double BOOTSTRAP_CONFIDENCE = .1;
    private final static Pattern alphanumRegex = Pattern.compile(".*\\w+.*");

    private final double[] npmiThresholds;
    private final String[] ngramCountFilenames;
    private final int minCount;
    private final int maxOrder;
    private final TextProcessor textProcessor;

    // Will store counts up to but not including the highest order
    private List<Map<Phrase,Long>> nGramCounts;
    private Collocations collocations;

    // Total count of all unigrams
    private long nWords;

    public static void main(String[] args) throws IOException {
        int minCount = PropertiesLoader.getMinCount();
        int maxPhraseLength = PropertiesLoader.getMaxPhraseLength();
        String[] ngramCountFilenames = PropertiesLoader.getNgramFilenames();
        TextProcessor textProcessor = PropertiesLoader.getTextProcessor();
        double[] npmiThresholds = PropertiesLoader.getNpmiThresholds();

        if (args.length > 1) {
            Path startPath = Paths.get(args[1]);
            for (int i = 0; i < ngramCountFilenames.length; i++) {
                ngramCountFilenames[i] = startPath.resolve(ngramCountFilenames[i]).toString();
            }
        }
        CollocationsMaker collocationsMaker = new CollocationsMaker(ngramCountFilenames, minCount, npmiThresholds, textProcessor);
        collocationsMaker.generateCollocations();
        Collocations col = collocationsMaker.getCollocations();
        for(int b = ngramCountFilenames.length; b < maxPhraseLength; b++) {
            collocationsMaker.bootstrapCollocations(b);
        }
        LOGGER.info(col.size() + " total collocations found");
        col.savePlaintext(new FileOutputStream(args[0]));

        // print out all collocs plus their scores
//        for(Phrase phrase: col) {
//            System.out.println(col.getScore(phrase) + "\t\t" + phrase);
//        }

//        // go through MRCONSO and score real phrases to get some stats on where their NPMI falls
//        Set<Phrase> mrconsoPhrases = getMrconsoPhrases();
//        collocator.scorePhrases(mrconsoPhrases);

//        System.out.println(col);

//        collocator.bootstrapCollocations(4);
//        int[] sizes = new int[5];
//        for(Phrase p : col) {
//            if(p.size() == 2) sizes[0]++;
//            else if(p.size() == 3) sizes[1]++;
//            else if(p.size() == 4) {
//                System.out.println(p);
//                sizes[2]++;
//            }
//            else if(p.size() == 5) {
////                System.out.println(p);
//                sizes[3]++;
//            }
//            else if(p.size() == 6) {
//                System.out.println(p);
//                sizes[4]++;
//            }
//        }
//        for(int x : sizes) System.out.println(x);

    }


    /**
     *
     * @param ngramCountFilenames one filename for each order of n-gram (including unigrams)
     * @param minCount the minimum frequency of any gram for consideration
     * @param npmiThresholds the thresholds for chunking (should be n - 1 entries--bigrams thru n-grams)
     * @param textProcessor solely used to determine
     */
    public CollocationsMaker(String[] ngramCountFilenames, int minCount, double[] npmiThresholds, TextProcessor textProcessor) {
        this.ngramCountFilenames = ngramCountFilenames;
        this.maxOrder = ngramCountFilenames.length;
        this.npmiThresholds = npmiThresholds;
        this.minCount = minCount;
        this.textProcessor = textProcessor;
    }

    public void generateCollocations() throws IOException {

        collocations = new Collocations();

        nWords = 0;
        nGramCounts = new ArrayList<>();
        // More intuitive to start counting from one!
        nGramCounts.add(null);

        // Load n-gram counts for all orders except the maximum order (don't need to store these and they'll take lots of memory)
        // Will count total number of words from the unigrams table as it goes
        for (int order = 1; order <= maxOrder; order++) {
            LOGGER.info("Loading ngrams for order " + order);
            Map<Phrase, Long> theseGramCounts = new HashMap<>();
            if (order < maxOrder) {
                nGramCounts.add(theseGramCounts);
            }

            BufferedReader reader = new BufferedReader(new FileReader(ngramCountFilenames[order-1]));
            int linesRead = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                // Skip this gram if it contains a dummy token or if it has insufficient count
                if (textProcessor.containsDummyToken(line)) continue;
                String[] fields = line.split("\\t");
                long count = Long.parseLong(fields[1]);

                // Note that this may exclude some unusual-cased variants of things
                if (count < minCount) continue;

                // Get counts of this n-gram, which will be needed when determining NPMI of higher orders
                Phrase thisPhrase = new Phrase(fields[0]);
                if (order < maxOrder) {
                    // Need to add to the old count, if it's there, for the case-insensitive strategy
                    Long oldCount = theseGramCounts.get(thisPhrase);
                    if (oldCount == null) {
                        theseGramCounts.put(thisPhrase, count);
                    } else {
                        theseGramCounts.put(thisPhrase, count + oldCount);
                    }
                }

                // Either increment total word count (for unigrams) or look for high-NPMI grams (for bigrams+)
                if (order == 1) {
                    nWords += count;
                } else if (order == 2) {
                    double score = twoPhraseNpmi(thisPhrase.getOneWordPhrase(0), thisPhrase.getOneWordPhrase(1), count);
                    if (score > npmiThresholds[0]) {
                        collocations.put(thisPhrase, score);
                    }
                } else {
                    // If phrase is length 3 or longer, only put in a new phrase if its subphrase is a good one
                    //      and if the two together reach the NPMI threshold

                    Phrase leadingPhrase = thisPhrase.without(-1);
                    Phrase trailingPhrase = thisPhrase.without(0);
                    Phrase leadingWord = thisPhrase.getOneWordPhrase(0);
                    Phrase trailingWord = thisPhrase.getOneWordPhrase(-1);

                    double maxScore = 0;
                    // ONLY add a collocation if there was a collocation at a lower order
                    if (collocations.contains(leadingPhrase)) {
                        maxScore = twoPhraseNpmi(leadingPhrase, trailingWord, count);
                    }
                    if (collocations.contains(trailingPhrase)) {
                        double score = twoPhraseNpmi(leadingWord, trailingPhrase, count);
                        if (score > maxScore) {
                            maxScore = score;
                        }
                    }
                    if(maxScore >= npmiThresholds[order-2]) {
                        collocations.put(thisPhrase, maxScore);
                    } else
                    // Invoke this rule only if no phrase found using the additive/incremental method.
                    // Add a trigram as if it were a bigram (w1, w3).
                    // This should help match phrases with common words in second position
                    // Uses the bigram threshold; tests show this closer for these types of chunks than the trigram one
                    if(order == 3) {
                        double score = twoPhraseNpmi(leadingWord, trailingWord, count);
                        if (score > npmiThresholds[0]) {
                            collocations.put(thisPhrase, score);
                        }
                    }
                }
                // todo: better counting (this does not include below-threshold n-grams in the counts at all)
                linesRead++;
                if(linesRead % 1000000 == 0) {
                    LOGGER.info(linesRead + " " + order + "-grams processed");
                }
            }
        }

        // Filter out bad phrases
        Iterator<Phrase> collocIterator = collocations.iterator();
        while(collocIterator.hasNext()) {
            Phrase phrase = collocIterator.next();
            if(gramIsRemovable(phrase)) {
                collocIterator.remove();
            }
        }
    }

    /**
     * Get NPMI scores on a set of known phrases.
     * Need to do this after reading all the n-grams and building nGramCounts.
     * @param phrases
     */
    //todo: either remove this method or make it less redundant
    public void scorePhrases(Iterable<Phrase> phrases) {
        double[] sums = new double[4];
        int[] ns = new int[4];
        double sum = 0;
        int n = 0;
        for(Phrase phrase : phrases) {
            int order = phrase.size();
            if (order < 2) continue;
            if(order < nGramCounts.size() && nGramCounts.get(order).containsKey(phrase)) {
                n++;
                System.out.println(phrase);
                double a = twoPhraseNpmi(phrase.getOneWordPhrase(0), phrase.without(0), nGramCounts.get(order).get(phrase));
                double b = twoPhraseNpmi(phrase.getOneWordPhrase(-1), phrase.without(-1), nGramCounts.get(order).get(phrase));
                System.out.println(a + "\t" + b);
                sum += Math.max(a, b);
                ns[order-2]++;
                sums[order-2] += Math.max(a,b);
            }
        }
        System.out.println(n);
        System.out.println(sum/n);
        for(int i=0; i<sums.length; i++) {
            System.out.println(i+2 + "-grams:");
            System.out.println(ns[i]);
            System.out.println(sums[i]/ns[i]);
        }
    }

    /**
     * Return the maximum npmi of this phrase and either of its immediately lower-order phrases
     * Used by UmlsNpmiExperiment
     * @param phrase any phrase that we have n-gram counts for
     * @return the best npmi of an order-(n-1) phrase and a single word on either side
     */
    public double highestOrderNpmi(Phrase phrase) {
        int order = phrase.size();
        if(order < 2 || order >= nGramCounts.size()) {
            return 0;
        }
        Long count = nGramCounts.get(phrase.size()).get(phrase);
        if(count == null) return 0;
        Phrase leadingPhrase = phrase.without(-1);
        Phrase trailingPhrase = phrase.without(0);
        Phrase leadingWord = phrase.getOneWordPhrase(0);
        Phrase trailingWord = phrase.getOneWordPhrase(-1);
        if(order == 2) {
            return twoPhraseNpmi(leadingWord, trailingWord, count);
        }
        return Math.max(twoPhraseNpmi(leadingPhrase, trailingWord, count), twoPhraseNpmi(leadingWord, trailingPhrase, count));
    }

    /**
     * Returns the npmi of this n-gram (n probably equals 3) using the fake bigram style (ignore the middle word)
     * Used by UmlsNpmiExperiment
     * @param phrase
     * @return
     */
    public double fakeBigramNpmi(Phrase phrase) {
        Long count = nGramCounts.get(phrase.size()).get(phrase);
        if(count == null) return 0;
        Phrase leadingWord = phrase.getOneWordPhrase(0);
        Phrase trailingWord = phrase.getOneWordPhrase(-1);
        return twoPhraseNpmi(leadingWord, trailingWord, count);
    }

    /**
     * Generate more collocations using existing (hopefully high-order) collocations by seeing if any overlap
     * @param fromOrder use collocations at this order to get those at the next order
     *
     * Could be made faster? Sort phrases beforehand?
     */
    public void bootstrapCollocations(int fromOrder) {
        LOGGER.info("Generating collocations of length " + (fromOrder + 1) + " without n-gram counts of that order");
        Set<Phrase> colsThisLength = collocations.getCollocationsOfLength(fromOrder);
        int added = 0;
        for(Phrase phrase1 : colsThisLength) {
            List<String> p1Words = phrase1.getWords();
            for(Phrase phrase2 : colsThisLength) {
                List<String> p2Words = phrase2.getWords();
                if(p1Words.subList(1, fromOrder).equals(p2Words.subList(0, fromOrder-1))) {
                    List<String> addWords = new ArrayList<>(p1Words);
                    addWords.add(p2Words.get(fromOrder-1));
                    // todo: what to use for confidence value?
                    collocations.put(new Phrase(addWords), BOOTSTRAP_CONFIDENCE);
                    added++;
                }
                else if(p1Words.subList(0, fromOrder - 1).equals(p2Words.subList(1, fromOrder))) {
                    List<String> addWords = new ArrayList<>(p2Words);
                    addWords.add(p1Words.get(fromOrder-1));
                    collocations.put(new Phrase(addWords), BOOTSTRAP_CONFIDENCE);
                    added++;
                }
            }
        }
        LOGGER.info(added + " collocations added for this length");
    }

    /**
     * Get the NPMI of any two phrases. Can be ngrams of any order. Need to have built n-gram counts first.
     * @param p1 a first word or phrase
     * @param p2 a second word or phrase
     * @param count the number of occurrences of these phrases concatenated together
     * @return the NPMI of the super-phrase
     */
    private double twoPhraseNpmi(Phrase p1, Phrase p2, long count) {
        double nWords = (double) this.nWords;
        int l1 = p1.size();
        int l2 = p2.size();
        if(!nGramCounts.get(l1).containsKey(p1) || !nGramCounts.get(l2).containsKey(p2) ) return 0;
        double px = nGramCounts.get(l1).get(p1) / nWords;
        double py = nGramCounts.get(l2).get(p2) / nWords;
        double pxy = count / nWords;
        return npmi(px, py, pxy);
    }

    /**
     * Normalized pointwise mutual entropy
     * @param px probability of one event
     * @param py probability of one event
     * @param pxy joint probability
     * @return a double between 0 and 1
     */
    private static double npmi(double px, double py, double pxy) {
        return Math.log(pxy / (px*py)) / -Math.log(pxy);
        // alternative that requires more calls to Math.log but doesn't multiply small numbers together
//        return (Math.log(px) + Math.log(py)) / Math.log(pxy) - 1;
    }

    /**
     * Determine if this n-gram is ineligible for being a phrase
     * These criteria are only invoked after all intermediate steps
     * Applies conditions:
     *      - must have alphanumeric characters in both first and last word
     *      - todo: other conditions to add?
     * @param phrase
     * @return
     */
    private boolean gramIsRemovable(Phrase phrase) {
        if(!alphanumRegex.matcher(phrase.word(0)).find() || !alphanumRegex.matcher(phrase.word(-1)).find()) {
            return true;
        }
        return false;
    }

    public Collocations getCollocations() {
        return collocations;
        // todo: write a vocabulary file, consistent with collocation applications, that should make training word2vec faster
    }

}
