package experiments;

import io.Word2vecReader;
import phrases.Phrase;
import semantics.Embeddings;
import textprocessing.TextProcessor;
import thesaurus.OrthographicDistance;
import util.Pair;
import util.Pairwise;
import util.PropertiesLoader;
import util.Threading;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Go through phrases in the UMLS and calculate similarities between them
 * Save the results to files that can be used for further analysis
 *
 * Created by gpfinley on 8/17/16.
 */
public class UmlsSimilarityExperiment {

    private static final Logger LOGGER = Logger.getLogger(UmlsSimilarityExperiment.class.getName());

    private static String language = "ENG";

    // column headers for saving/loading text files
    private final static String SEM = "semantic";
    private final static String ORTHO = "orthographic";
    private final static String ABBR = "abbreviation";

    private final List<Pair<Phrase>> pairs;

    // semantic similarities. origin pairs don't matter; we just need the distribution
    private List<Double> semanticSimilarities;
    // orthographic distances
    private List<Double> orthoDistances;
    // abbreviation distances
    private List<Double> abbrDistances;

    public static String getLanguage() {
        return language;
    }

    public static void setLanguage(String language) {
        UmlsSimilarityExperiment.language = language;
    }

    private UmlsSimilarityExperiment(Collection<Pair<Phrase>> pairs) {
        this.pairs = new ArrayList<>(pairs);
    }

    public static UmlsSimilarityExperiment luiExperiment(File mrconso,
                                                         File lrabr,
                                                         TextProcessor textProcessor,
                                                         int maxPhraseLength,
                                                         boolean caseSensitive) throws IOException {
        return uiExperiment(mrconso, lrabr, textProcessor, maxPhraseLength, caseSensitive, false);
    }

    private static UmlsSimilarityExperiment uiExperiment(File mrconso,
                                                         File lrabr,
                                                         TextProcessor textProcessor,
                                                         int maxPhraseLength,
                                                         boolean caseSensitive,
                                                         boolean cuiNotLui) throws IOException {
        Map<String, Set<Phrase>> luisAndForms = new HashMap<>();
        Set<String> abbreviations = new HashSet<>();
        String line;

        BufferedReader mrconsoReader = new BufferedReader(new FileReader(mrconso));

        if (lrabr != null) {
            BufferedReader lrabrReader = new BufferedReader(new FileReader(lrabr));
            LOGGER.info("Loading abbreviations from LRABR to exclude from consideration...");
            while ((line = lrabrReader.readLine()) != null) {
                String[] fields = line.split("\\|");
                String abbr = textProcessor.process(fields[1]).trim();
                if(!textProcessor.containsDummyToken(abbr) && abbr.length() > 0) {
                    abbreviations.add(abbr);
                }
            }
            lrabrReader.close();
        } else {
            LOGGER.info("Not excluding abbreviations");
        }

        LOGGER.info("Loading same-LUI terms from MRCONSO...");
        int ctr = 0;
        while ((line = mrconsoReader.readLine()) != null) {
            ctr++;
//            if(ctr > 100000) break;
            if (ctr % 1000000 == 0) {
                LOGGER.info("Parsed " + ctr + " lines");
            }
            String[] fields = line.split("\\|");
            if (!fields[1].equals(language)) continue;
            String lui = cuiNotLui ? fields[0] : fields[3];
            String form = textProcessor.process(fields[14]).trim();
            if(!textProcessor.containsDummyToken(form) && form.length() > 0) {
                if (lrabr == null || !abbreviations.contains(form)) {
                    luisAndForms.putIfAbsent(lui, new HashSet<>());
                    Phrase phrase = new Phrase(form);
                    if (phrase.size() <= maxPhraseLength) {
                        luisAndForms.get(lui).add(new Phrase(caseSensitive ? form : form.toLowerCase()));
                    }
                }
            }
        }
        mrconsoReader.close();

        int pairsToBuild = 0;
        for (Set<Phrase> set : luisAndForms.values()) {
            int n = set.size();
            pairsToBuild += n * (n - 1) / 2;
        }
        LOGGER.info("Building " + pairsToBuild + " pairs from " + luisAndForms.size() + " LUI sets...");
        Set<Pair<Phrase>> pairs = new HashSet<>(pairsToBuild);
        for (Set<Phrase> set : luisAndForms.values()) {
            for (Pair<Phrase> pair : new Pairwise<>(set, false)) {
                pairs.add(pair);
            }
        }
        LOGGER.info("Built " + pairs.size() + " total pairs");
        return new UmlsSimilarityExperiment(pairs);
    }

    /**
     * For generating an abbreviation experiment. Loads abbreviations and their possible long forms from LRABR
     * @param lrabr a File object pointing to a file formatted like SPECIALIST's LRABR
     * @param textProcessor a TextProcessor of the same Class used for other processing
     * @return a new experiment
     * @throws IOException
     */
    public static UmlsSimilarityExperiment abbrExperiment(File lrabr, TextProcessor textProcessor) throws IOException {
        BufferedReader lrabrReader = new BufferedReader(new FileReader(lrabr));
        LOGGER.info("Loading abbreviations from LRABR...");
        String line;
        List<Pair<Phrase>> pairs = new ArrayList<>();
        while ((line = lrabrReader.readLine()) != null) {
            String[] fields = line.split("\\|");
            String abbr = textProcessor.process(fields[1]).trim();
            String longform = textProcessor.process(fields[4]).trim();
            if(!textProcessor.containsDummyToken(abbr) && !textProcessor.containsDummyToken(longform) && abbr.length() > 0 && longform.length() > 0) {
                pairs.add(new Pair<>(new Phrase(abbr), new Phrase(longform)));
            }
        }
        lrabrReader.close();
        return new UmlsSimilarityExperiment(pairs);
    }

    public static UmlsSimilarityExperiment randomPairsExperiment(File mrconso, int nPairs, TextProcessor textProcessor, boolean caseSensitive) throws IOException {
        BufferedReader mrconsoReader = new BufferedReader(new FileReader(mrconso));
        LOGGER.info("Loading terms from MRCONSO...");
        String line;
        List<Phrase> phrases = new ArrayList<>();
        int ctr=0;
        while ((line = mrconsoReader.readLine()) != null) {
            ctr++;
            if (ctr % 1000000 == 0) {
                LOGGER.info("Parsed " + ctr + " lines");
            }
            String[] fields = line.split("\\|");
            if (!fields[1].equals(language)) continue;
            String form = textProcessor.process(fields[14]).trim();
            if(!textProcessor.containsDummyToken(form) && form.length() > 0) {
                Phrase phrase = new Phrase(caseSensitive ? form : form.toLowerCase());
                if (phrase.size() <= 8) {
                    phrases.add(phrase);
                }
            }
        }
        mrconsoReader.close();

        List<Pair<Phrase>> pairs = new ArrayList<>();
        int n = phrases.size()-1;
        Collections.shuffle(phrases);
        for(int i=0; i<nPairs; i++) {
            pairs.add(new Pair<>(phrases.get(i), phrases.get(n-i)));
        }
        return new UmlsSimilarityExperiment(pairs);
    }

    /**
     * For generic use
     * @param pairFirstElements a list of strings for the first elements of all pairs
     * @param pairSecondElements for the second elements of all pairs (should be same length)
     * @return a new experiment with pairs built
     */
    public static UmlsSimilarityExperiment fromStringLists(List<String> pairFirstElements, List<String> pairSecondElements) {
        List<Pair<Phrase>> pairs = new ArrayList<>();
        for(int i=0; i<pairFirstElements.size(); i++) {
            pairs.add(new Pair<>(new Phrase(pairFirstElements.get(i)), new Phrase(pairSecondElements.get(i))));
        }
        return new UmlsSimilarityExperiment(pairs);
    }

    /**
     * Typical way to calculate non-abbreviation orthographic distance.
     * Can also pass in a distance metric if using something other than the standard
     */
    public void calculateOrthographicDistances(Double permutationPenalty) {
        calculateOrthographicDistances(permutationPenalty, OrthographicDistance.forNonAbbreviations());
    }

    public void calculateOrthographicDistances(Double permutationPenalty, OrthographicDistance distanceMetric) {
        int n = pairs.size();
        double[] distances = new double[n];

        Threading.chunkAndThread(n, OrthoDistThread.class, distances, distanceMetric, pairs, false, permutationPenalty);

        orthoDistances = new ArrayList<>(n);
        for(int i=0; i<n; i++) {
            orthoDistances.add(distances[i]);
        }
    }

    /**
     * todo: doc
     */
    public static class OrthoDistThread extends Threading.IntRangeThread {
        private double[] distances;
        private OrthographicDistance orthoDist;
        private Double permutationPenalty;
        private List<Pair<Phrase>> pairs;
        private boolean forAbbreviations;
        @Override
        public void initializeParams(Object[] args) {
            distances = (double[]) args[0];
            orthoDist = (OrthographicDistance) args[1];
            pairs = (List<Pair<Phrase>>) args[2];
            forAbbreviations = (boolean) args[3];
            if(!forAbbreviations) {
                permutationPenalty = (Double) args[4];
            } else {
                permutationPenalty = null;
            }
        }
        @Override
        public void run() {
            for(int i=begin; i<end; i++) {
                Phrase p1 = pairs.get(i).one();
                Phrase p2 = pairs.get(i).two();
                double normalizedDist;
                if(forAbbreviations) {
                    double dist = orthoDist.distance(p1, p2);
                    normalizedDist = dist / p1.length();
                } else {
                    double dist = orthoDist.shortestPermutedDistance(p1, p2, permutationPenalty);
                    normalizedDist = dist / Math.max(p1.length(), p2.length());
                }
                distances[i] = normalizedDist;
                if(i % 1000 == 0) {
                    LOGGER.info(i + " orthographic distance calculated on this thread");
                }
            }
        }
    }

    /**
     * Typical way to calculate abbreviation orthographic distance.
     * Can also pass in a distance metric if using something other than the standard
     */
    public void calculateAbbrDistances() {
        calculateAbbrDistances(OrthographicDistance.forAbbreviations());
    }
    public void calculateAbbrDistances(OrthographicDistance distanceMetric) {
        int n = pairs.size();
        double[] distances = new double[n];

        Threading.chunkAndThread(n, OrthoDistThread.class, distances, distanceMetric, pairs, true);

        abbrDistances = new ArrayList<>(n);
        for(int i=0; i<n; i++) {
            abbrDistances.add(distances[i]);
        }
    }

    /**
     * Calculate semantic similarity for all pairs where possible
     * Append nulls to the list where the embeddings do not contain one or the other
     * @param emb word embeddings to use
     */
    public void calculateSemanticSimilarities(Embeddings emb) {
        semanticSimilarities = new ArrayList<>(pairs.size());
        emb.normalizeAll();
        int calculated = 0;
        for(Pair<Phrase> pair : pairs) {
            if(emb.contains(pair.one()) && emb.contains(pair.two())) {
                semanticSimilarities.add(emb.get(pair.one()).dot(emb.get(pair.two())));
                calculated++;
            } else {
                semanticSimilarities.add(null);
            }
        }
        LOGGER.info("Calculated " + calculated + " semantic similarity scores from " + pairs.size() + " pairs");
    }

    /**
     * Write out all pairs in this experiment and the scores associated with them
     * @param stream an output stream
     * @throws IOException
     */
    public void dump(OutputStream stream) throws IOException {
        LOGGER.info("Saving parameters...");
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream));
        writer.write("phrase1\tphrase2");
        if(semanticSimilarities != null) {
            writer.write("\t" + SEM);
        }
        if(orthoDistances != null) {
            writer.write("\t" + ORTHO);
        }
        if(abbrDistances != null) {
            writer.write("\t" + ABBR);
        }
        writer.write("\n");
        for(int i=0; i<pairs.size(); i++) {
            StringBuilder builder = new StringBuilder();
            builder.append(pairs.get(i).one().toString());
            builder.append("\t");
            builder.append(pairs.get(i).two().toString());
            if(semanticSimilarities != null) {
                builder.append("\t");
                builder.append(semanticSimilarities.get(i));
            }
            if(orthoDistances != null) {
                builder.append("\t");
                builder.append(orthoDistances.get(i));
            }
            if(abbrDistances != null) {
                builder.append("\t");
                builder.append(abbrDistances.get(i));
            }
            builder.append("\n");
            writer.write(builder.toString());
        }
        writer.flush();
        writer.close();
    }

    public static UmlsSimilarityExperiment load(InputStream stream) throws IOException {
        LOGGER.info("Loading saved experiment...");
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        List<Double> semanticSimilarities;
        List<Double> orthoDistances;
        List<Double> abbrDistances;
        List<Pair<Phrase>> pairs = new ArrayList<>();

        String line = reader.readLine();
        List<String> headers = Arrays.asList(line.split("\\t"));
        List<List<Double>> saveInto = new ArrayList<>();
        if(headers.contains(SEM)) {
            semanticSimilarities = new ArrayList<>();
            saveInto.add(semanticSimilarities);
        } else {
            semanticSimilarities = null;
        }
        if(headers.contains(ORTHO)) {
            orthoDistances = new ArrayList<>();
            saveInto.add(orthoDistances);
        } else {
            orthoDistances = null;
        }
        if(headers.contains(ABBR)) {
            abbrDistances = new ArrayList<>();
            saveInto.add(abbrDistances);
        } else {
            abbrDistances = null;
        }
        while((line = reader.readLine()) != null) {
            String[] fields = line.split("\\t");
            pairs.add(new Pair<>(new Phrase(fields[0]), new Phrase(fields[1])));
            for(int i=2; i<fields.length; i++) {
                saveInto.get(i-2).add(Double.parseDouble(fields[i]));
            }
        }
        UmlsSimilarityExperiment exp = new UmlsSimilarityExperiment(pairs);
        exp.semanticSimilarities = semanticSimilarities;
        exp.orthoDistances = orthoDistances;
        exp.abbrDistances = abbrDistances;
        return exp;
    }

    private static double mean(List<Double> nums) {
        double sum=0;
        int n = 0;
        for(Double num : nums) {
            if(num != null) {
                sum += num;
                n++;
            }
        }
        return sum/n;
    }

    /**
     * Simple analysis. More in-depth analyses can be performed on the tab-separated exported files
     */
    public void analyze() {
        if(semanticSimilarities != null) {
            System.out.println("mean semantic sim: " + mean(semanticSimilarities));
        }
        if(orthoDistances != null) {
            System.out.println("mean ortho dist: " + mean(orthoDistances));
        }
        if(abbrDistances != null) {
            System.out.println("mean abbr dist: " + mean(abbrDistances));
        }
    }

    public static class LuiExperiment {
        /**
         * Usage:
         * // todo: document
         *
         * @param args
         * @throws IOException
         */
        public static void main(String[] args) throws IOException {
            UmlsSimilarityExperiment exp;
            String saveTo;
            if (args[0].equals("load")) {
                String saved = args[1];
                exp = load(new FileInputStream(saved));
            } else {
                String embeddings = args[0];
                saveTo = args[1];
                Embeddings emb = Word2vecReader.readBinFile(embeddings);
                exp = luiExperiment(PropertiesLoader.getMrconsoPath().toFile(),
                        PropertiesLoader.getLrabrPath().toFile(),
                        PropertiesLoader.getTextProcessor(),
                        PropertiesLoader.getMaxPhraseLength(),
                        PropertiesLoader.getEmbeddingsCaseSensitive());
                exp.calculateSemanticSimilarities(emb);
                exp.calculateOrthographicDistances(PropertiesLoader.getPermutationPenalty());
                exp.dump(new FileOutputStream(saveTo));
            }
            exp.analyze();
        }
    }

    public static class AbbreviationExperiment {
        public static void main(String[] args) throws IOException {
            String saveTo = null;
            if(args.length > 0) {
                saveTo = args[0];
            }
            UmlsSimilarityExperiment exp = abbrExperiment(PropertiesLoader.getLrabrPath().toFile(), PropertiesLoader.getTextProcessor());
            // calculate orthographic distance with 'null' permutation penalty--i.e., don't consider permutations at all
            exp.calculateOrthographicDistances(null);
            exp.calculateAbbrDistances();
            if(saveTo != null) {
                exp.dump(new FileOutputStream(saveTo));
            }
            exp.analyze();
        }
    }

    public static class RandomPhrasesExperiment {
        public static void main(String[] args) throws IOException {
            String embeddings = args[0];
            int numPairs = Integer.parseInt(args[1]);
            String saveTo = null;
            if(args.length > 2) {
                saveTo = args[2];
            }
            Embeddings emb = Word2vecReader.readBinFile(embeddings);
            UmlsSimilarityExperiment exp = randomPairsExperiment(PropertiesLoader.getMrconsoPath().toFile(), numPairs, PropertiesLoader.getTextProcessor(), PropertiesLoader.getEmbeddingsCaseSensitive());
            exp.calculateSemanticSimilarities(emb);
            exp.calculateOrthographicDistances(PropertiesLoader.getPermutationPenalty());
            exp.calculateAbbrDistances();
            if(saveTo != null) {
                exp.dump(new FileOutputStream(saveTo));
            }
            exp.analyze();
        }
    }

}
