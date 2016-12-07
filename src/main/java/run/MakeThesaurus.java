package run;

import io.Word2vecReader;
import semantics.Embeddings;
import thesaurus.Thesaurus;
import thesaurus.ThesaurusMaker;
import util.PropertiesLoader;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * todo: document
 *
 * Created by gpfinley on 12/7/16.
 */
public class MakeThesaurus {

    public static void main(String[] args) throws IOException {

        // todo: permit use of glove with proper flags
        String embeddingsFile = args[0];
        String saveTo = args[1];

        Embeddings emb;
        emb = Word2vecReader.readBinFile(embeddingsFile);

        ThesaurusMaker thesaurusMaker = new ThesaurusMaker.ThesaurusMakerBuilder(emb)
                .setSemanticThreshold(PropertiesLoader.getSemanticMinimum())
                .setOrthoThreshold(PropertiesLoader.getOrthoMaximum())
                .setAbbrThreshold(PropertiesLoader.getAbbrMaximum())
                .setOverallThreshold(PropertiesLoader.getOverallThreshold())
                .useTextProcessor(PropertiesLoader.getTextProcessor())
                .setCaseSensitive(PropertiesLoader.getEmbeddingsCaseSensitive())
                .setSemanticOrthoWeightRatio(PropertiesLoader.getSemanticOrthoWeightRatio())
                .setPermutationPenalty(PropertiesLoader.getPermutationPenalty())
                .build();

        Thesaurus thesaurus = thesaurusMaker.buildThesaurus();
        thesaurus.save(new FileOutputStream(saveTo));
    }
}
