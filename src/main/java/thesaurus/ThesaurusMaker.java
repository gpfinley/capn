package thesaurus;

import io.Word2vecReader;
import phrases.Phrase;
import semantics.Embeddings;
import semantics.WordEmbedding;
import textprocessing.ModerateProcessor;
import textprocessing.TextProcessor;

import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * todo: document
 *
 * Created by gpfinley on 7/21/16.
 */
public class ThesaurusMaker {

    private final static Logger LOGGER = Logger.getLogger(Word2vecReader.class.getName());
    private final static double SEMANTIC_DEFAULT = .3;
    private final static double ORTHO_DEFAULT = .4;
    private final static double ABBR_DEFAULT = .5;
    private final static double WEIGHT_RATIO_DEFAULT = 1;

    private final Embeddings emb;
    private final List<Phrase> embPhrases;
    private final OrthographicDistance abbrDistance;
    private final OrthographicDistance orthoDistance;

    private final TextProcessor textProcessor;

    // todo: normalize thresholds by word length
    private final double semanticThreshold;
    private final double orthoMax;
    private final double abbrMax;
    private final double overallThreshold;
    private final boolean caseSensitive;
    private final Double permutationPenalty;
    private final double semanticOrthoRatio;

    public static class ThesaurusMakerBuilder {
        private final Embeddings emb;
        private Double semanticThreshold;
        private Double orthoThreshold;
        private Double abbrThreshold;
        private Double overallThreshold;
        private Double semanticOrthoWeightRatio;
        private boolean caseSensitive;
        private Double permutationPenalty;
        private TextProcessor textProcessor;
        public ThesaurusMakerBuilder(Embeddings emb) {
            LOGGER.info("Normalizing all embeddings");
            emb.normalizeAll();
            this.emb = emb;
        }
        public ThesaurusMaker build() {
            return new ThesaurusMaker(this);
        }
        public ThesaurusMakerBuilder setSemanticThreshold(double semanticThreshold) {
            this.semanticThreshold = semanticThreshold;
            return this;
        }
        public ThesaurusMakerBuilder setOrthoThreshold(double orthoThreshold) {
            this.orthoThreshold = orthoThreshold;
            return this;
        }
        public ThesaurusMakerBuilder setAbbrThreshold(double abbrThreshold) {
            this.abbrThreshold = abbrThreshold;
            return this;
        }
        public ThesaurusMakerBuilder setOverallThreshold(Double overallThreshold) {
            this.overallThreshold = overallThreshold;
            return this;
        }
        public ThesaurusMakerBuilder setCaseSensitive(boolean caseSensitive) {
            this.caseSensitive = caseSensitive;
            return this;
        }
        public ThesaurusMakerBuilder useTextProcessor(TextProcessor textProcessor) {
            this.textProcessor = textProcessor;
            return this;
        }
        public ThesaurusMakerBuilder setSemanticOrthoWeightRatio(Double semanticOrthoWeightRatio) {
            this.semanticOrthoWeightRatio = semanticOrthoWeightRatio;
            return this;
        }
        public ThesaurusMakerBuilder setPermutationPenalty(Double permutationPenalty) {
            this.permutationPenalty = permutationPenalty;
            return this;
        }
    }


    private ThesaurusMaker(ThesaurusMakerBuilder builder) {

        emb = builder.emb;
        embPhrases = emb.getLexicon();
        caseSensitive = builder.caseSensitive;
        permutationPenalty = builder.permutationPenalty;

        // values that should not be null
        if(builder.semanticThreshold == null) {
            LOGGER.warning("Semantic threshold not set; using default of " + SEMANTIC_DEFAULT);
            semanticThreshold = SEMANTIC_DEFAULT;
        } else {
            semanticThreshold = builder.semanticThreshold;
        }
        if(builder.orthoThreshold == null) {
            LOGGER.warning("Orthographic threshold not set; using default of " + ORTHO_DEFAULT);
            orthoMax = ORTHO_DEFAULT;
        } else {
            orthoMax = builder.orthoThreshold;
        }
        if(builder.abbrThreshold == null) {
            LOGGER.warning("Abbreviation orthographic threshold not set; using default of " + ABBR_DEFAULT);
            abbrMax = ABBR_DEFAULT;
        } else {
            abbrMax = builder.abbrThreshold;
        }
        if(builder.overallThreshold == null) {
            overallThreshold = -Double.MAX_VALUE;
        } else {
            overallThreshold = builder.overallThreshold;
        }
        if(builder.semanticOrthoWeightRatio == null) {
            LOGGER.warning("Semantic-to-orthographic weight ratio not set; using default of " + WEIGHT_RATIO_DEFAULT);
            semanticOrthoRatio = WEIGHT_RATIO_DEFAULT;
        } else {
            semanticOrthoRatio = builder.semanticOrthoWeightRatio;
        }
        if(builder.textProcessor == null) {
            LOGGER.warning("Text processor not set; using default of ModerateProcessor");
            textProcessor = new ModerateProcessor();
        } else {
            textProcessor = builder.textProcessor;
        }

        // Remove phrases that have no letters
        final Pattern hasLetter = Pattern.compile(".*[A-Za-z].*");
        Iterator<Phrase> phraseIter = emb.iterator();
        while(phraseIter.hasNext()) {
            Phrase phrase = phraseIter.next();
            Matcher matcher = hasLetter.matcher(phrase.toString());
            if(!matcher.find()) {
                phraseIter.remove();
            }
        }
        abbrDistance = OrthographicDistance.forAbbreviations();
        orthoDistance = OrthographicDistance.forNonAbbreviations();
    }

    public double getSemanticThreshold() {
        return semanticThreshold;
    }
    public double getOrthoMax() {
        return orthoMax;
    }
    public double getAbbrMax() {
        return abbrMax;
    }
    public Double getOverallThreshold() {
        return overallThreshold;
    }
    public double getSemanticOrthoRatio() {
        return semanticOrthoRatio;
    }

    public Thesaurus buildThesaurus() {
        Thesaurus thesaurus = new Thesaurus(caseSensitive, textProcessor);
        LOGGER.info("Finding matches for " + emb.size() + " headwords");
        int counter = 0;
        for(Phrase headword : emb.getLexicon()) {
            counter++;
            if(textProcessor.containsDummyToken(headword.toString())) continue;
            if (counter % 1000 == 0) {
                LOGGER.info(counter + " words processed");
            }
            Map<Phrase, Double> equivalents = new HashMap<>();
            List<Phrase> matches = getSemanticEquivalents(headword);
            if(matches.size() > 1) {
                long millis = System.currentTimeMillis();
                LOGGER.info(matches.size() + " semantic matches found for " + headword);
                for(Phrase candidatePhrase : matches) {
                    if (candidatePhrase.equals(headword)
                            || textProcessor.containsDummyToken(candidatePhrase.toString())
                            || candidatePhrase.size() == 0
                            ) continue;

                    // normalize orthoDist to longer phrase length
                    double orthoDist = orthoDistance.shortestPermutedDistance(headword, candidatePhrase, permutationPenalty);
                    double normalOrthoDist = orthoDist / Math.max(headword.length(), candidatePhrase.length());
                    // normalize abbreviation distance to *abbr length* (an asymmetric distance measure, unlike ortho)
                    double normalAbbrDist = Math.min(
                            abbrDistance.distance(headword, candidatePhrase) / headword.length(),
                            abbrDistance.distance(candidatePhrase, headword) / candidatePhrase.length());

                    if (normalOrthoDist <= orthoMax || normalAbbrDist <= abbrMax) {
                        // todo: could be sped up a little (save double[] from before rather than re-dot)
                        double semanticSim = emb.get(headword).dot(emb.get(candidatePhrase));
                        double thisScore = overallScore(semanticSim, normalOrthoDist, normalAbbrDist);
                        if (thisScore >= overallThreshold) {
                            thesaurus.addPairing(new Thesaurus.FullEntry(headword, candidatePhrase, thisScore, semanticSim, normalOrthoDist, normalAbbrDist));
                        }
                    }
                }
            }
        }

        return thesaurus;
    }

    /**
     * todo: document
     * @param phrase
     * @return
     */
    private List<Phrase> getSemanticEquivalents(Phrase phrase) {
        int n = emb.size();
        final WordEmbedding compVector = emb.get(phrase);
        double[] scores = emb.calculateScoresThreaded(compVector);
        List<Phrase> matches = new ArrayList<>();
        for(int i=0; i<n; i++) {
            if(scores[i] >= semanticThreshold) {
                matches.add(embPhrases.get(i));
            }
        }
        return matches;
    }


    /**
     * Generate an overall similarity score based on semantic and orthographic scores
     * @param semanticSim the semantic similarity
     * @param orthoDist the orthographic distance (not similarity)
     * @param abbrDist the abbreviation orthographic distance
     * @return
     */
    public double overallScore(double semanticSim, double orthoDist, double abbrDist) {
        return (semanticOrthoRatio * semanticSim + 1 - Math.min(orthoDist, abbrDist)) / (semanticOrthoRatio + 1);
    }

}
