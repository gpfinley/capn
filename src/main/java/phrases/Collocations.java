package phrases;

import util.ByValue;
import util.PhraseGraph;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Store collocations and confidence scores for them
 * Also includes methods for reading/writing a set of collocations and for applying them to a file
 * Created by gpfinley on 3/1/16.
 */
public class Collocations implements Iterable<Phrase> {

    private Map<Phrase, Double> collocationScores;

    // a directed graph made up of nested HashMaps
    // Stores all collocations in a format that is convenient when applying them to text
    private final PhraseGraph phraseGraph;

    /**
     * Set up the collocations to be kept in sorted order: first by size, then alphabetically
     */
    public Collocations() {
        this.collocationScores = new TreeMap<>(new Comparator<Phrase>() {
            @Override
            public int compare(Phrase o1, Phrase o2) {
                if(o1.size() == o2.size()) {
                    return o1.compareTo(o2);
                }
                return ((Integer) o1.size()).compareTo(o2.size());
            }
        });
        phraseGraph = new PhraseGraph();
    }

    public Collocations(Map<Phrase, Double> collocationScores) {
        this.collocationScores = collocationScores;
        phraseGraph = new PhraseGraph();
    }

    /**
     * Return the n most highly scored collocations in the model
     * @param n the number to return
     * @return an ArrayList of Phrase objects
     */
    public List<Phrase> highestNPhrases(int n) {
        if(n > collocationScores.size()) n = collocationScores.size();
        List<Phrase> phrases = new ArrayList<>(collocationScores.keySet());
        Collections.sort(phrases, new ByValue<>(collocationScores));
        Collections.reverse(phrases);
        return phrases.subList(0, n);
    }

    public void limitToHighestN(int n) {
        List<Phrase> keep = highestNPhrases(n);
        Map<Phrase, Double> collocationScores = new HashMap<>();
        for(Phrase phrase : keep) {
            collocationScores.put(phrase, this.collocationScores.get(phrase));
        }
        this.collocationScores = collocationScores;
    }

    /**
     * Return a set of phrases that are a given length
     * @param length the length in words of the desired phrases
     * @return a Set of Phrases
     */
    public Set<Phrase> getCollocationsOfLength(int length) {
        Set<Phrase> toReturn = new HashSet<>();
        for (Phrase p : collocationScores.keySet()) {
            if(p.size() == length) toReturn.add(p);
        }
        return toReturn;
    }

    /**
     * Calculate the precision of identified collocations
     * @param gold a set of Phrases that are considered true collocations
     * @param top the number of top collocated scores to consider (0 = all)
     */
    public double calculatePrecision(Set<Phrase> gold, int top) {
        if(top < 1) top = collocationScores.size();
        List<Phrase> hypothesis = highestNPhrases(top);
        double score = 0;
        for(Phrase phrase : hypothesis) {
            if(gold.contains(phrase)) score ++;
        }
        return score/hypothesis.size();
    }

    public double getScore(Phrase phrase) {
        Double score = collocationScores.get(phrase);
        return score == null ? 0 : score;
    }

    public Double put(Phrase phrase, double score) {
//        wordGraph = null;
        phraseGraph.addPhrase(phrase);
        return collocationScores.put(phrase, score);
    }

    public Double remove(Phrase phrase) {
//        wordGraph = null;
        return collocationScores.remove(phrase);
    }

    public boolean contains(Phrase phrase) {
        return collocationScores.containsKey(phrase);
    }

    public int size() {
        return collocationScores.size();
    }

    public Iterator<Phrase> iterator() {
        return collocationScores.keySet().iterator();
    }

    /**
     * Apply collocations to this bit of text
     * @param orig a String of text with words separated by whitespace
     * @return a String with collocations applied by replacing spaces with underscores.
     *          Will also collapse consecutive whitespace characters to a single space; not suitable for whole files.
     */
    public String apply(String orig, boolean keepCase) {
        StringBuilder builder = new StringBuilder();
        String[] lines = orig.split("\\n+");
        for(String line : lines) {
            String[] words = line.split("\\s+");
            for (int i = 0; i < words.length; i++) {
                if (words[i].length() == 0) continue;
                Phrase longest = phraseGraph.getLongestPhraseFrom(words, i);
                if (longest != null) {
                    int longestPhraseEnd = i + longest.size();
                    builder.append(words[i]);
                    for (int k = i + 1; k < longestPhraseEnd; k++) {
                        builder.append("_");
                        builder.append(words[k]);
                    }
                    i = longestPhraseEnd;
                } else {
                    builder.append(words[i]);
                }
                if (i < words.length) builder.append(" ");
            }
            builder.append("\n");
        }
        return keepCase ? builder.toString() : builder.toString().toLowerCase();
    }
    public String apply(String orig) {
        return apply(orig, true);
    }

    /**
     * Go through the file and write collocations of the maximum possible length
     * @param infile a raw input file of space-delimited tokens (and ideally documents on different lines)
     * @param outfile similar format, with underscores between words in a collocation
     * @param keepCase whether or not to keep case distinctions *after* writing collocations
     * @throws IOException
     */
    public void applyToCorpus(String infile, String outfile, boolean keepCase) throws IOException {
        System.out.println("Writing collocations for all phrases...");
        OutputStream outputStream = new FileOutputStream(outfile);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));

        /** Crashes on files with super long lines */
        InputStream inputStream = new FileInputStream(infile);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String nextLine;
        long charsWritten = 0;
        int nextReport = 10000000;
        while ((nextLine = reader.readLine()) != null) {
            String toWrite = apply(nextLine, keepCase);
            writer.write(toWrite + "\n");
            charsWritten += toWrite.length() + 1;
            if(charsWritten > nextReport) {
                System.out.println(charsWritten + " characters written");
                nextReport += 10000000;
            }
        }

        writer.flush();
        writer.close();
    }
    public void applyToCorpus(String infile, String outfile) throws IOException {
        applyToCorpus(infile, outfile, true);
    }

    /**
     * Walk a file tree and apply collocations to many files
     * @param fromdir directory containing files
     * @param todir directory to write new collocated files to (will preserve structure)
     * @throws IOException
     */
    public void walkAndApply(String fromdir, final String todir, final boolean keepCase) throws IOException {
        class BigramWriter extends SimpleFileVisitor<Path> {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attr) throws IOException {
                String fname = file.getFileName().toString();
                String outname = "bigrammed_" + fname;
                Path outPath = Paths.get(todir).resolve(outname);
                applyToCorpus(file.toString(), outPath.toString(), keepCase);
                return FileVisitResult.CONTINUE;
            }
        }
        Files.walkFileTree(Paths.get(fromdir), new BigramWriter());
    }
    public void walkAndApply(String fromdir, final String todir) throws IOException {
        walkAndApply(fromdir, todir, true);
    }

    @Override
    public String toString() {
        return collocationScores.toString();
    }

    /**
     * Write these collocations to a plaintext file
     * @param stream an output stream (probably FileOutputStream)
     * @throws IOException
     */
    public void savePlaintext(OutputStream stream) throws IOException {
        OutputStreamWriter writer = new OutputStreamWriter(stream);
        for(Map.Entry<Phrase, Double> e : collocationScores.entrySet()) {
            writer.write(e.getKey().toString());
            writer.write("\t");
            writer.write(e.getValue().toString());
            writer.write("\n");
        }
        writer.close();
    }

    /**
     * Read in a new Collocations object from a file
     * @param stream an input stream (probably FileInputStream)
     * @return Collocations loaded from this file
     * @throws IOException
     */
    public static Collocations loadPlaintext(InputStream stream) throws IOException {
        Collocations col = new Collocations();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line;
        while((line = reader.readLine()) != null) {
            String[] fields = line.split("\t");
            col.put(new Phrase(fields[0]), Double.parseDouble(fields[1]));
        }
        return col;
    }

}
