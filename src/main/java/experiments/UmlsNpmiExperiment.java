package experiments;

import phrases.Phrase;
import run.CollocationsMaker;
import textprocessing.TextProcessor;
import util.PropertiesLoader;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

/**
 * Get stats on the normalized pointwise mutual information of real phrases in the UMLS
 * Save these stats to tab-separated text
 * Use this to guide threshold selection
 *
 * Created by gpfinley on 8/19/16.
 */
public class UmlsNpmiExperiment {

    private static Logger LOGGER = Logger.getLogger(UmlsNpmiExperiment.class.getName());

    private final CollocationsMaker cm;
    private final Set<Phrase> phrases;

    private Map<Phrase, Double> highestOrderNpmi;
    private Map<Phrase, Double> fakeBigramNpmi;

    private static double mean(double[] val) {
        double sum = 0;
        for (double v : val) sum += v;
        return sum / val.length;
    }

    private UmlsNpmiExperiment(Set<Phrase> phrases, CollocationsMaker cm) {
        this.phrases = phrases;
        this.cm = cm;
        highestOrderNpmi=new HashMap<>();
        fakeBigramNpmi=new HashMap<>();
        for(Phrase phrase : phrases) {
            if(phrase.size() > 1) {
                highestOrderNpmi.put(phrase, cm.highestOrderNpmi(phrase));
                if (phrase.size() == 3) {
                    fakeBigramNpmi.put(phrase, cm.fakeBigramNpmi(phrase));
                }
            }
        }
    }

    public static UmlsNpmiExperiment fromMrconso(File mrconso, TextProcessor textProcessor, CollocationsMaker cm) throws IOException {
        Set<Phrase> mrconsoPhrases = new HashSet<>();
        BufferedReader reader = new BufferedReader(new FileReader(mrconso));
        String line;
        while((line = reader.readLine()) != null) {
            String[] fields = line.split("\\|");
            if(fields[1].equals("ENG")) {
                String term = textProcessor.process(fields[14]);
                if(!textProcessor.containsDummyToken(term)) {
                    Phrase phrase = new Phrase(term);
                    if(phrase.size() <= 7) {
                        mrconsoPhrases.add(phrase);
                    }
                }
            }
        }
        return new UmlsNpmiExperiment(mrconsoPhrases, cm);
    }

    /**
     * Generate a control condition: phrases constructed from a bunch of random unigrams
     * @param unigrams a set of unigrams to use to build phrases
     * @param nRandom an array of the number of bigrams, trigrams, etc. to generate
     * @return
     */
    public static UmlsNpmiExperiment fromRandomUnigrams(Set<String> unigrams, int[] nRandom) {

        // todo:
        return null;
    }

    public void dump(OutputStream stream) throws IOException {
        LOGGER.info("Saving npmi values...");
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream));
        writer.write("term\torder\tscore");
        writer.write("\n");
        for(Map.Entry<Phrase, Double> e : highestOrderNpmi.entrySet()) {
            StringBuilder builder = new StringBuilder();
            builder.append(e.getKey().toString());
            builder.append("\t");
            builder.append(((Integer) e.getKey().size()).toString());
            builder.append("\t");
            builder.append(e.getValue().toString());
            builder.append("\n");
            writer.write(builder.toString());
        }
        for(Map.Entry<Phrase, Double> e : fakeBigramNpmi.entrySet()) {
            StringBuilder builder = new StringBuilder();
            builder.append(e.getKey().toString());
            builder.append("\t2.5\t");
            builder.append(e.getValue().toString());
            builder.append("\n");
            writer.write(builder.toString());
        }
        writer.flush();
        writer.close();
    }


    /**
     * todo: document
     * Two arguments:
     *      - folder with ngram files in it (see properties file for basenames)
     *      - output file
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
//        args = new String[]{"/Volumes/gregdata/metathesaurus/2015AA/META/MRCONSO.RRF",
//                            "npmi_stats.txt"};
        Path ngramsPath = Paths.get(args[0]);
        String[] ngramFilenames = PropertiesLoader.getNgramFilenames();
        for(int i=0; i<ngramFilenames.length; i++){
            ngramFilenames[i] = ngramsPath.resolve(ngramFilenames[i]).toString();
        }
        String outFile = args[1];
        CollocationsMaker col = new CollocationsMaker(ngramFilenames, PropertiesLoader.getMinCount(), PropertiesLoader.getNpmiThresholds(), PropertiesLoader.getTextProcessor());
        col.generateCollocations();
        UmlsNpmiExperiment exp = fromMrconso(PropertiesLoader.getMrconsoPath().toFile(), PropertiesLoader.getTextProcessor(), col);
        exp.dump(new FileOutputStream(outFile));
    }
}
