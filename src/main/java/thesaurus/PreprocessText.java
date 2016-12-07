package thesaurus;

import textprocessing.TextProcessor;
import util.PropertiesLoader;

import java.io.*;
import java.util.logging.Logger;

/**
 * Call from command line to preprocess a text block
 *
 * Created by gpfinley on 8/11/16.
 */
public class PreprocessText {

    private static Logger LOGGER = Logger.getLogger(PreprocessText.class.getName());

    public static void main(String[] args) throws IOException, ClassNotFoundException {

        TextProcessor processor = PropertiesLoader.getTextProcessor();
        String inFile = args[0];
        String outFile = args[1];

        LOGGER.info("Processing text in " + inFile + " using processor " + processor.getClass().getName());

        BufferedReader reader = new BufferedReader(new FileReader(inFile));
        BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));

        String line;
        int i = 0;
        while ((line = reader.readLine()) != null) {
            if (line.matches("\\s*")) continue;
            String outln = processor.process(line);
            writer.write(outln);
            writer.write("\n");
            i++;
            if (i % 1000 == 0) writer.flush();
            if (i % 100000 == 0) LOGGER.info("Processed " + i + " lines");
        }
        writer.close();
    }

}
