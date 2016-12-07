package run;

import phrases.Collocations;
import util.PropertiesLoader;

import java.io.*;
import java.util.logging.Logger;

/**
 * Load collocations and apply them to a file
 *
 * Created by gpfinley on 7/18/16.
 */
public class ApplyCollocations {

    private final static Logger LOGGER = Logger.getLogger(ApplyCollocations.class.getName());

    public static void main(String[] args) throws IOException {
        Collocations col = Collocations.loadPlaintext(new FileInputStream(args[0]));
        LOGGER.info("Applying collocations to file " + args[1]);
        col.applyToCorpus(args[1], args[2], PropertiesLoader.getEmbeddingsCaseSensitive());
        // todo: confirm that this and the new code in Collocations will actually properly collapse case when applying
    }
}
