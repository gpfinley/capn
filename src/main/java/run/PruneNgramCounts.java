package run;

import textprocessing.TextProcessor;
import util.PropertiesLoader;

import java.io.*;
import java.util.logging.Logger;

/**
 * Trim down a SRILM-formatted count of n-grams to save on disk space and on future processing time
 * Will remove phrases below a certain frequency threshold as well as phrases containing dummy tokens
 * (This is not a strictly necessary step, as the collocator knows to ignore the pruned counts)
 * Created by gpfinley on 8/12/16.
 */
public class PruneNgramCounts {

    private static Logger LOGGER = Logger.getLogger(PruneNgramCounts.class.getName());

    public static void main(String[] args) throws IOException {
        TextProcessor tProc = PropertiesLoader.getTextProcessor();
        int minCount = PropertiesLoader.getMinCount();

        String inPath=null;
        String outPath=null;
        try {
            inPath = args[0];
            outPath = args[1];
        } catch (ArrayIndexOutOfBoundsException e) {
            LOGGER.severe("Need to supply two arguments: an input and output file");
            System.exit(1);
        }

        LOGGER.info("Pruning n-gram counts in file " + inPath + " with counts below " + minCount);

        BufferedReader reader = new BufferedReader(new FileReader(inPath));
        BufferedWriter writer = new BufferedWriter(new FileWriter(outPath));
        String line;
        while ((line = reader.readLine()) != null) {
            if(tProc.containsDummyToken(line)) continue;
            if(Integer.parseInt(line.split("\\t")[1]) < minCount) continue;
            writer.write(line);
            writer.write("\n");
        }
        writer.flush();
        writer.close();
    }
}
