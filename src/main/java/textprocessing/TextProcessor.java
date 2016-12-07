package textprocessing;

/**
 * Created by gpfinley on 8/8/16.
 */
public interface TextProcessor {

    /**
     * Process a piece of text
     * @param orig
     * @return
     */
    String process(String orig);

    /**
     * Determine if the string contains a dummy token as used by this TextProcessor
     * Have this method always return false if the processor does not use dummy tokens
     * @param str
     * @return
     */
    boolean containsDummyToken(String str);

}
