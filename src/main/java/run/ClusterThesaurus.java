package run;

import io.Word2vecReader;
import phrases.Phrase;
import thesaurus.ClusteredThesaurus;
import thesaurus.Thesaurus;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Generate a clustered thesaurus from a lookup thesaurus
 *
 * Created by gpfinley on 12/7/16.
 */
public class ClusterThesaurus {

    public static void main(String[] args) throws IOException {
        String vocabPath = args[0];
        int preThesaurusTrim = Integer.parseInt(args[1]);
        String thesaurusPath = args[2];
        int postThesaurusTrim = Integer.parseInt(args[3]);
        String clustersOut = args[4];
        Map<Phrase, Integer> freqs = Word2vecReader.readVocabFile(vocabPath, preThesaurusTrim);
        Thesaurus thesaurus = Thesaurus.load(new FileInputStream(thesaurusPath));

        ClusteredThesaurus clust = new ClusteredThesaurus(freqs, thesaurus, postThesaurusTrim);
        clust.save(clustersOut);

        System.out.println(clust.getClusterSizes().toString());
    }

}
