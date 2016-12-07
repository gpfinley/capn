package run;

import thesaurus.Thesaurus;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Simple class for pruning down a thesaurus by overall score
 *
 * Created by gpfinley on 9/22/16.
 */
public class TrimThesaurus {
    public static void main(String[] args) throws IOException {
        String inPath = args[0];
        String outPath = args[1];
        double minScore = Double.parseDouble(args[2]);
        Thesaurus thesaurus = Thesaurus.load(new FileInputStream(inPath));
        thesaurus.removeEntriesBelow(minScore);
        thesaurus.save(new FileOutputStream(outPath));
    }
}
