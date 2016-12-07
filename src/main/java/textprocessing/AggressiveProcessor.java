package textprocessing;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Class for performing basic text processing
 * Will split on any non-alphanumeric characters and remove any no-letter tokens
 * Makes all tokens lowercase
 *
 * Created by gpfinley on 8/8/16.
 */
public class AggressiveProcessor implements TextProcessor, Serializable {

    /**
     * Basic tuple class to make sure that patterns and their replacements stay together
     */
    private class PatternRepl {
        Pattern pattern;
        String repl;
        PatternRepl(Pattern pattern, String repl) {
            this.pattern = pattern;
            this.repl = repl;
        }
    }
    private final List<PatternRepl> patternsRepls;

    private final static Pattern symbolPattern = Pattern.compile("[\\W_]+");
    private final static String symbolRepl = " ";

    private final static Pattern noLetterWordPattern = Pattern.compile("(\\s)\\d+\\s");
    private final static String noLetterWordRepl = "$1";

    public AggressiveProcessor() {
        patternsRepls = new ArrayList<>();
        Collections.addAll(patternsRepls,
                new PatternRepl(symbolPattern, symbolRepl),
                new PatternRepl(noLetterWordPattern, noLetterWordRepl)
        );
    }

    @Override
    public String process(String orig) {
        for(PatternRepl pr : patternsRepls) {
            orig = pr.pattern.matcher(orig).replaceAll(pr.repl);
        }
        return orig.toLowerCase();
    }

    /**
     * This class uses no dummy tokens
     */
    @Override
    public boolean containsDummyToken(String str) {
        return false;
    }
}
