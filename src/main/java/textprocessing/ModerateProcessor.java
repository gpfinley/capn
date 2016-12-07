package textprocessing;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Class for performing basic text processing
 * // todo: use this (and its interface) for text processing, not the class in PropertiesLoader
 *
 * Created by gpfinley on 8/8/16.
 */
public class ModerateProcessor implements TextProcessor, Serializable {

    // This should be pretty distinctive without being too long
    private static final String dummyTokenPrefix = "zxzxpz";

    /**
     * It is the philosophy of this module that any word with a dummy token should disqualify its phrase.
     * Thus, all of the categories below are things that should never be part of a stored phrase,
     * and they all contain a dummy prefix that should be easily identified.
     */
    private static final String NEWLINE = spacePad(dummyTokenPrefix + "newln");
    // todo: decide if NUMERAL should participate in this philosophy (should n-grams with it be kept?)
    private static final String NUMERAL = spacePad(dummyTokenPrefix + "numeral");
    private static final String DATE = spacePad(dummyTokenPrefix + "date");
    private static final String PHONE = spacePad(dummyTokenPrefix + "phone");
    private static final String EMAIL = spacePad(dummyTokenPrefix + "email");
    private static final String TIME = spacePad(dummyTokenPrefix + "time");


    private static final Pattern emailPattern = Pattern.compile("[\\w.\\-]+@[\\w\\-]+\\.[\\w]{2,3}");
    private static final String emailRepl = EMAIL;

    // date matcher. Can get a little greedy
    private static final Pattern datePattern = Pattern.compile("[0-9]{1,4}/[0-9]{1,2}/[0-9]{1,4}");
    private static final String dateRepl = DATE;

    // match H:MM:SS, HH:MM:SS, HH:MM, H:MM
    private static final Pattern timePattern = Pattern.compile("\\d{1,2}:\\d{2}(:\\d{2})?");
    private static final String timeRepl = TIME;

       // phone number matcher (works for 10-digit numbers)
    private static final Pattern phonePattern = Pattern.compile("(\\d{3}-|\\(\\d{3}\\) ?)\\d{3}-\\d{4}");
    private static final String phoneRepl = PHONE;

    // replace numerals more than three characters long (can definitely get greedy)
    private static final Pattern numeralPattern = Pattern.compile("[\\d\\-.,~]{3,}(\\D|$)");
    private static final String numeralRepl = NUMERAL + "$1";

    // Remove most punctuation altogether
    private static final Pattern punctuationPattern = Pattern.compile("[()\\[\\]{}\".,;:?!]");
    private static final String punctuationRepl = "";

    // Insert space between -/&~ and a letter (i.e., don't break up dates or number ranges)
    private static final Pattern spaceInsertionPatternA = Pattern.compile("([a-zA-Z])([~\\-/&])");
    private static final Pattern spaceInsertionPatternB = Pattern.compile("([~\\-/&])([a-zA-Z])");
    private static final String spaceInsertionRepl = "$1 $2";

    // Replace lots of consecutive spaces with newline tokens (extra spaces can creep in from replacing dates and numerals)
    private static final Pattern newlinePattern = Pattern.compile("[ \\t]{4,}");
    private static final String newlineRepl = NEWLINE;
    private static final Pattern spaceCollapsePattern = Pattern.compile("[ \\t]+");
    private static final String spaceCollapseRepl = " ";

    // Collapse 2+ consecutive non-word characters (notes might have '~~~~~' or '*****' as headings)
    // (Don't use \W because I want to collapse underscores too)
    private static final Pattern symbolStringPattern = Pattern.compile("([^a-zA-Z0-9])\\1{2,}");
    private static final String symbolStringRepl = "$1$1";

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

    public ModerateProcessor() {
        patternsRepls = new ArrayList<>();
        Collections.addAll(patternsRepls,
                new PatternRepl(emailPattern, emailRepl),
                new PatternRepl(datePattern, dateRepl),
                new PatternRepl(timePattern, timeRepl),
                new PatternRepl(phonePattern, phoneRepl),
                new PatternRepl(numeralPattern, numeralRepl),
                new PatternRepl(punctuationPattern, punctuationRepl),
                new PatternRepl(spaceInsertionPatternA, spaceInsertionRepl),
                new PatternRepl(spaceInsertionPatternB, spaceInsertionRepl),
                new PatternRepl(newlinePattern, newlineRepl),
                new PatternRepl(spaceCollapsePattern, spaceCollapseRepl),
                new PatternRepl(symbolStringPattern, symbolStringRepl)
        );
    }

    @Override
    public String process(String orig) {
        for(PatternRepl pr : patternsRepls) {
            orig = pr.pattern.matcher(orig).replaceAll(pr.repl);
        }
        return orig;
    }

    /**
     * Determine if a dummy token, as used by this class, is part of this string.
     * All dummy tokens used by this class should have dummyTokenPrefix in them!
     * @param str
     * @return
     */
    @Override
    public boolean containsDummyToken(String str) {
        return str.contains(dummyTokenPrefix);
    }

    private static String spacePad(String str) {
        return " " + str + " ";
    }

    public static void main(String[] args) {
    }
}
