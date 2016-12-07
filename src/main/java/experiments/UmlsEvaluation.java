package experiments;

import phrases.Phrase;
import textprocessing.TextProcessor;
import thesaurus.Thesaurus;
import util.PropertiesLoader;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Evaluate a thesaurus by a simple metric:
 *      - count true equivalencies in the UMLS as hits,
 *      - count equivalencies with other UMLS terms as misses,
 *      - and ignore equivalencies that are not in the UMLS (while also measuring precision).
 * More comprehensive evaluation will require more manual effort.
 *
 * Created by gpfinley on 8/1/16.
 */
// todo: update to work with new (aug 24) thesaurus style!!
    // todo: does this work or should it be deleted??
public class UmlsEvaluation {

    private static Logger LOGGER = Logger.getLogger(UmlsEvaluation.class.getName());

    // todo: convert these variables to local, or break everything up into methods
    private final Thesaurus thesaurus;
    private final Set<Phrase> usedPhrases;
    private final Map<Phrase, Set<String>> phrasesAndCuis;

//    private Set<Pair<Phrase>> sameCuiEntries;
//    private Set<Pair<Phrase>> differentCuiEntries;

    private List<Thesaurus.FullEntry> sameCuiEntries;
    private List<Thesaurus.FullEntry> differentCuiEntries;

    private int inUmls;
    private int notInUmls;

    public int nTruePairs() {
        return sameCuiEntries.size();
    }

    public int nFalsePairs() {
        return differentCuiEntries.size();
    }

    public int nPairsInUmls() {
        return inUmls;
    }

    public int nPairsNotInUmls() {
        return notInUmls;
    }

    public double getPrecision() {
        return ((double) inUmls) / (inUmls + notInUmls);
    }

    /**
     * Add a thesaurus to evaluate and get all phrases used in it
     * @param thesaurus
     */
    public UmlsEvaluation(Thesaurus thesaurus, File mrconso, TextProcessor textProcessor) throws IOException {
        this.thesaurus = thesaurus;
        phrasesAndCuis = new HashMap<>();
        usedPhrases = new HashSet<>();
//        for(Phrase headword : thesaurus) {
//            List<Thesaurus.FullEntry> entries = thesaurus.getEquivalents(headword);
//            for(Thesaurus.FullEntry entry : entries) {
//                usedPhrases.add(entry.p1);
//                usedPhrases.add(entry.p2);
//            }
//        }
        for(Thesaurus.FullEntry entry : thesaurus.getAllEntries()) {
            usedPhrases.add(entry.p1);
            usedPhrases.add(entry.p2);
        }

        BufferedReader mrconsoReader = new BufferedReader(new FileReader(mrconso));
        LOGGER.info("Loading terms from MRCONSO...");
        int ctr = 0;
        String line;
        while ((line = mrconsoReader.readLine()) != null) {
            ctr++;
            if (ctr % 1000000 == 0) {
                LOGGER.info("Parsed " + ctr + " lines");
            }
            String[] fields = line.split("\\|");
            if (!fields[1].equals("ENG")) continue;
            String cui = fields[0];
            String form = textProcessor.process(fields[14]);
            if (!thesaurus.isCaseSensitive()) form = form.toLowerCase();
            Phrase phrase = new Phrase(form);
            if(usedPhrases.contains(phrase)) {
                phrasesAndCuis.putIfAbsent(phrase, new HashSet<String>());
                phrasesAndCuis.get(phrase).add(cui);
            }
        }
        mrconsoReader.close();
        LOGGER.info("Loaded " + phrasesAndCuis.size() + " phrases from MRCONSO");

        sameCuiEntries = new ArrayList<>();
        differentCuiEntries = new ArrayList<>();
        Set<Phrase> phrasesInUmls = phrasesAndCuis.keySet();
        for(Thesaurus.FullEntry entry : thesaurus.getAllEntries()) {

            if (phrasesAndCuis.containsKey(entry.p1) && phrasesAndCuis.containsKey(entry.p2)) {
                inUmls++;
                Set<String> cuisTermTwo = phrasesAndCuis.get(entry.p2);
                // see if any of the cuis for the first term match any cuis for the second.
                lookForCuiMatch: {
                    for (String cui : phrasesAndCuis.get(entry.p1)) {
                        if (cuisTermTwo.contains(cui)) {
                            sameCuiEntries.add(entry);
                            break lookForCuiMatch;
                        }
                    }
                    differentCuiEntries.add(entry);
                }
            } else {
                notInUmls++;
            }
        }

//            Collection<Phrase> equivs = thesaurus.getEquivalents(term);
//            Pairwise<Phrase> pairsOfThisSet = new Pairwise<>(equivs, phrasesInUmls);
//
//            inUmls += sameCuiEntries.size();
//            // expected # of pairs minus those that were in UMLS
//            notInUmls += equivs.size() * (equivs.size() - 1) / 2 - pairsOfThisSet.size();
//            for(Pair<Phrase> pair : pairsOfThisSet) {
//                Set<String> cuisTermTwo = phrasesAndCuis.get(pair.two());
//                // see if any of the cuis for the first term match any cuis for the second.
//                lookForCuiMatch: {
//                    for (String cui : phrasesAndCuis.get(pair.one())) {
//                        if (cuisTermTwo.contains(cui)) {
//                            sameCuiEntries.add(pair);
//                            break lookForCuiMatch;
//                        }
//                    }
//                    differentCuiEntries.add(pair);
//                }
//            }
//        }

        LOGGER.info(inUmls + " total phrase pairings in thesaurus that had both terms in the UMLS; "
                + notInUmls + " did not (" + getPrecision()*100 + "%)");
        LOGGER.info(nTruePairs() + " had two terms with the same CUI; " + nFalsePairs() + " did not");
    }

    public void writePairs(OutputStream stream) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream));
        writer.write("phrase1\tphrase2\toverall\tsemantic\torthographic\tabbreviation\tcuis\n");
//        writer.write(": PAIRS WITH SAME CUI\t\n");
        for(Thesaurus.FullEntry entry : sameCuiEntries) {
            writer.write(entry.toTabbed());
            writer.write("\tsame\n");
        }
//        writer.write(": PAIRS WITH DIFFERENT CUIS\t\n");
        for(Thesaurus.FullEntry entry : differentCuiEntries) {
            writer.write(entry.toTabbed());
            writer.write("\tdiff\n");
        }
        writer.flush();
        writer.close();
    }

    /**
     * todo: doc
     * @param args thesaurus text dump and an output file for the evaluation tab-separated text
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        String thesaurusLocation = args[0];
        String pairsFile = args[1];

        UmlsEvaluation eval = new UmlsEvaluation(Thesaurus.load(new FileInputStream(thesaurusLocation)),
                PropertiesLoader.getMrconsoPath().toFile(),
                PropertiesLoader.getTextProcessor());

        eval.writePairs(new FileOutputStream(pairsFile));
    }

}
