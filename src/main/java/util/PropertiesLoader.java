package util;

import textprocessing.ModerateProcessor;
import textprocessing.TextProcessor;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Loads a properties file and facilitates access to key properties across modules
 * No other Classes should need to load the properties directly
 * Generally speaking, properties are best loaded by only a main() function
 *
 * Created by gpfinley on 7/18/16.
 */
public class PropertiesLoader {

    private static Logger LOGGER = Logger.getLogger(PropertiesLoader.class.getName());

    private static Properties properties;
    static {
        String propertiesFileName = System.getProperties().getProperty("propertiesFile");
        if(propertiesFileName == null) propertiesFileName = "default.properties";
        try {
            properties = new Properties();
            properties.load(new FileInputStream(propertiesFileName));
        } catch (IOException e) {
            LOGGER.severe("Couldn't load properties file. Make sure it is in the current directory, or specify one with -DpropertiesFile=<path-to-properties>.");
            System.exit(1);
        }
    }

    /**
     * To get any generic property that doesn't have its own function
     * @param prop the String name of the property
     * @return its String value
     */
    public static String getProperty(String prop) {
        return properties.getProperty(prop);
    }

    public static double[] getNpmiThresholds() {
        String[] threshStrings = properties.getProperty("npmiThresholds").split(",");
        double[] threshes = new double[getMaxNgramOrder()];
        for(int i=0; i<threshes.length; i++) {
            if(i >= threshStrings.length) {
                threshes[i] = Double.parseDouble(threshStrings[threshStrings.length-1]);
            } else {
                threshes[i] = Double.parseDouble(threshStrings[i]);
            }
        }
        return threshes;
    }

    public static int getMinCount() {
        return Integer.parseInt(properties.getProperty("minCount"));
    }

    public static String[] getNgramFilenames() {
        int ngramOrder = Integer.parseInt(properties.getProperty("maxNgramOrder"));
        String[] filenames = properties.getProperty("ngramCountFilenames").split(",");
        return filenames.length <= ngramOrder ? filenames : Arrays.copyOfRange(filenames, 0, ngramOrder);
    }

    public static int getMaxPhraseLength() {
        return Integer.parseInt(properties.getProperty("maxPhraseLength"));
    }

    // private because this value isn't used outside this class; instead, use getNgramFilenames().length
    private static int getMaxNgramOrder() {
        return Integer.parseInt(properties.getProperty("maxNgramOrder"));
    }

    public static TextProcessor getTextProcessor() {
        try {
            String textProcessorClassName = properties.getProperty("textProcessor");
            return (TextProcessor) Class.forName(textProcessorClassName).newInstance();
        } catch(Exception e) {
            LOGGER.warning("Couldn't load TextProcessor specified in the properties file. Using ModerateProcessor.");
            return new ModerateProcessor();
        }
    }

    public static double getSemanticMinimum() {
        return Double.parseDouble(properties.getProperty("semanticMinimum"));
    }

    public static double getOrthoMaximum() {
        return Double.parseDouble(properties.getProperty("orthoMaximum"));
    }

    public static double getAbbrMaximum() {
        return Double.parseDouble(properties.getProperty("abbrMaximum"));
    }

    public static Double getSemanticOrthoWeightRatio() {
        try {
            return Double.parseDouble(properties.getProperty("semanticOrthoWeightRatio"));
        } catch(Exception e) {
            return null;
        }
    }

    public static Double getOverallThreshold() {
        try {
            return Double.parseDouble(properties.getProperty("overallThreshold"));
        } catch(Exception e) {
            return null;
        }
    }

    public static boolean getEmbeddingsCaseSensitive() {
        return Boolean.parseBoolean(properties.getProperty("embeddingsCaseSensitive"));
    }

    public static boolean getThesaurusKeepScore() {
        return Boolean.parseBoolean(properties.getProperty("thesaurusKeepScore"));
    }

    public static Double getPermutationPenalty() {
        try {
            String pp = properties.getProperty("permutationPenalty");
            if (pp.equals("null")) {
                return null;
            }
            return Double.parseDouble(pp);
        } catch(NullPointerException e) {
            return null;
        }
    }

    public static Path getMrconsoPath() {
        return Paths.get(properties.getProperty("metathesaurusHome")).resolve("MRCONSO.RRF");
    }

    public static Path getLrabrPath() {
        return Paths.get(properties.getProperty("lexiconHome")).resolve("LRABR");
    }

    public static String getLanguage() {
        String lang = properties.getProperty("language");
        return lang == null ? "ENG" : lang;
    }

}

