package textprocessing;

/**
 * Simple utility class if you want any class that uses a TextProcessor to do no processing
 * Created by gpfinley on 8/16/16.
 */
public class NullProcessor implements TextProcessor {
    public String process(String orig) {
        return orig;
    }
    public boolean containsDummyToken(String str) {
        return false;
    }
}
