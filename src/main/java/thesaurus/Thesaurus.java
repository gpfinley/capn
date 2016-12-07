package thesaurus;

import phrases.Phrase;
import textprocessing.TextProcessor;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by gpfinley on 7/21/16.
 */
public class Thesaurus implements Iterable<Phrase> {

    private static Logger LOGGER = Logger.getLogger(Thesaurus.class.getName());

    // todo: implement spaceless lookup (will probably need a separate hashmap for it)

    // use when writing the file (makes it more human readable)
    private static final String IS_CASE_SENSITIVE_STRING = "case-sensitive thesaurus";
    private static final String NOT_CASE_SENSITIVE_STRING = "non-case-sensitive thesaurus";

    private final Map<Phrase, List<FullEntry>> thesaurus;

    private Set<FullEntry> allEntries;

    private final boolean caseSensitive;

    private final TextProcessor textProcessor;

    /**
     *
     * @param caseSensitive
     * @param textProcessor
     */
    public Thesaurus(boolean caseSensitive, TextProcessor textProcessor) {
        this.caseSensitive = caseSensitive;
        this.textProcessor = textProcessor;
        thesaurus = new LinkedHashMap<>();
    }

    /**
     * iterates over headwords
     * Will only return one entry per word pair, with arbitrary pair order
     * @return
     */
    @Override
    public Iterator<Phrase> iterator() {
        return thesaurus.keySet().iterator();
    }

    /**
     * Get the number of words and phrases in the thesaurus
     * @return size of thesaurus
     */
    public int numWords() {
        return thesaurus.size();
    }

    /**
     * Get all entries in this thesaurus
     * @return all entries in a Set of Thesaurus.FullEntry objects
     */
    public Set<FullEntry> getAllEntries() {
        if(allEntries == null) {
            allEntries = new HashSet<>();
            for(List<FullEntry> entries : thesaurus.values()) {
                allEntries.addAll(entries);
            }
        }
        return allEntries;
    }

    /**
     * @return true if this is a case-sensitive thesaurus
     */
    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    /**
     * Prune this thesaurus by removing entries with an overall score below a certain threshold
     * @param minOverallScore the minimum overall score
     */
    public void removeEntriesBelow(double minOverallScore) {
        LOGGER.info("Removing all entries below " + minOverallScore + " overall score");
        Iterator<Map.Entry<Phrase, List<FullEntry>>> iter = thesaurus.entrySet().iterator();
        while(iter.hasNext()) {
            Map.Entry<Phrase, List<FullEntry>> e = iter.next();
            Phrase phrase = e.getKey();
            List<FullEntry> entries = e.getValue();
            for (int i=0; i<entries.size(); i++) {
                if (entries.get(i).overallScore < minOverallScore) {
                    if (i == 0) {
                        iter.remove();
                    } else {
                        thesaurus.put(phrase, entries.subList(0, i));
                    }
                    break;
                }
            }
        }
    }

    /**
     * Generic function for adding a pairing to the thesaurus. Will not also add the reverse pairing!
     * @param e a Thesaurus.FullEntry object (contains forms and scores)
     */
    public void addPairing(FullEntry e) {
        if(!thesaurus.containsKey(e.p1)) {
            thesaurus.put(e.p1, new ArrayList<>());
        }
        thesaurus.get(e.p1).add(e);
        if(allEntries != null) {
            allEntries.add(e);
        }
    }

    /**
     * Basic function for querying an already-built thesaurus.
     * @param phrase a word or phrase query
     * @return a Collection (may be empty)
     */
    // todo: update
    public List<FullEntry> getEquivalents(Phrase phrase) {
        return getEquivalentsExact(textProcessor.process(phrase.toString()));
    }

    public List<FullEntry> getEquivalentsExact(String form) {
        if(!caseSensitive) {
            form = form.toLowerCase();
        }
        Phrase lookupPhrase = new Phrase(form);
        return thesaurus.containsKey(lookupPhrase) ? thesaurus.get(lookupPhrase) : new ArrayList<FullEntry>();
    }

    /**
     * Return the pairing of the two current phrases as a FullEntry (which will include scores).
     * @param p1 one phrase
     * @param p2 another phrase
     * @return The FullEntry from p1's entries matching p2, or null if no such entry exists
     */
    public FullEntry getEntry(Phrase p1, Phrase p2) {
        for (FullEntry e : getEquivalents(p1)) {
            if (e.p2.equals(p2)) return e;
        }
        return null;
    }

    /**
     * Check for a headword in the thesaurus
     * @param phrase the phrase to check
     * @return true if present, false otherwise
     */
    public boolean hasPhrase(Phrase phrase) {
        return thesaurus.containsKey(phrase);
    }

    /**
     * Save this thesaurus to a human-readable text file, which can be read back with the static method load()
     * @param output an OutputStream (probably FileOutputStream)
     * @throws IOException
     */
    public void save(OutputStream output) throws IOException {
        LOGGER.info("Saving thesaurus to output stream " + output.toString());
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output));

        // sort all entries by overallScore before saving
        for(List<FullEntry> list : thesaurus.values()) {
            list.sort(new Comparator<FullEntry>() {
                @Override
                public int compare(FullEntry o1, FullEntry o2) {
                    return ((Double)o2.overallScore).compareTo(o1.overallScore);
                }
            });
        }

        if(caseSensitive) writer.write(IS_CASE_SENSITIVE_STRING);
        else writer.write(NOT_CASE_SENSITIVE_STRING);
        writer.write("\n");
        writer.write(textProcessor.getClass().toString().substring(6));
        writer.write("\n");
        for(Map.Entry<Phrase, List<FullEntry>> e : thesaurus.entrySet()) {
            Phrase headPhrase = e.getKey();
            List<FullEntry> equivs = e.getValue();
            String writeln = headPhrase.toString();
            for(FullEntry equiv : equivs) {
                writeln += "\t";
                writeln += equiv.p2.toString();
                writeln += "\t" + equiv.overallScore + ";" + equiv.semanticSimilarity + ";" + equiv.orthoDistance + ";" + equiv.abbrDistance;
            }
            writer.write(writeln + "\n");
        }
        writer.flush();
        writer.close();
    }

    /**
     * Load a thesaurus from an input stream (probably a text file)
     * @param textSave an InputStream containing the output of this class's save() method
     * @return a new Thesaurus
     * @throws IOException
     */
    public static Thesaurus load(InputStream textSave) throws IOException {
        LOGGER.info("Loading thesaurus from input stream " + textSave.toString());
        boolean caseSensitive = false;
        TextProcessor textProcessor;
        BufferedReader reader = new BufferedReader(new InputStreamReader(textSave));
        if(reader.readLine().equals(IS_CASE_SENSITIVE_STRING)) {
            caseSensitive = true;
        }
        try {
            String className = reader.readLine();
            if (className.startsWith("class ")) className = className.substring(6);
            textProcessor = (TextProcessor) Class.forName(className).newInstance();
        } catch (Exception e) {
            LOGGER.severe("Couldn't load text processor class");
            throw new IOException();
        }
        Thesaurus thesaurus = new Thesaurus(caseSensitive, textProcessor);
        String line;
        while((line = reader.readLine()) != null) {
            String[] terms = line.split("\\t");
            if(terms.length < 2) continue;
            Phrase headword = new Phrase(terms[0]);
            for(int i=1; i<terms.length; i+=2) {
                Phrase phrase = new Phrase(terms[i]);
                String[] scores = terms[i+1].split(";");
                thesaurus.addPairing(new FullEntry(
                        headword,
                        phrase,
                        Double.parseDouble(scores[0]),
                        Double.parseDouble(scores[1]),
                        Double.parseDouble(scores[2]),
                        Double.parseDouble(scores[3]) ));
            }
        }
        return thesaurus;
    }

    public static class FullEntry {
        public Phrase p1;
        public Phrase p2;
        public double overallScore;
        public double semanticSimilarity;
        public double orthoDistance;
        public double abbrDistance;

        @Override
        public String toString() {
            return p1.toString() + ":" + p2.toString() + "\t" + overallScore + ";" + semanticSimilarity + ";" + orthoDistance + ";" + abbrDistance;
        }

        public String toTabbed() {
            StringBuilder builder = new StringBuilder();
            builder.append(p1);
            builder.append("\t");
            builder.append(p2);
            builder.append("\t");
            builder.append(overallScore);
            builder.append("\t");
            builder.append(semanticSimilarity);
            builder.append("\t");
            builder.append(orthoDistance);
            builder.append("\t");
            builder.append(abbrDistance);
            return builder.toString();
        }

        // equals and hashCode generated by intelliJ. My mods: allow p1/p2 to be interchangeable
        // todo: should these be considered equal even if the scores don't turn out to be the same?
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FullEntry fullEntry = (FullEntry) o;

            if (Double.compare(fullEntry.overallScore, overallScore) != 0) return false;
            if (Double.compare(fullEntry.semanticSimilarity, semanticSimilarity) != 0) return false;
            if (Double.compare(fullEntry.orthoDistance, orthoDistance) != 0) return false;
            if (Double.compare(fullEntry.abbrDistance, abbrDistance) != 0) return false;
            return samePhrases(fullEntry);
        }

        /**
         * Return true iff this entry and another have the same two phrases, in either order
         * @param other another FullEntry
         * @return
         */
        public boolean samePhrases(FullEntry other) {
            return (p1.equals(other.p1) && p2.equals(other.p2)) || (p1.equals(other.p2) && p2.equals(other.p1));
        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            result = p1.hashCode();
            result = 31 * result + 31 * p2.hashCode();
            temp = Double.doubleToLongBits(overallScore);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(semanticSimilarity);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(orthoDistance);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(abbrDistance);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            return result;
        }

        public FullEntry(Phrase p1, Phrase p2, double overallScore, double semanticSimilarity, double orthoDistance, double abbrDistance) {
            this.p1 = p1;
            this.p2 = p2;
            this.overallScore = overallScore;
            this.semanticSimilarity = semanticSimilarity;
            this.orthoDistance = orthoDistance;
            this.abbrDistance = abbrDistance;
        }
    }

}
