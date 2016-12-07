package thesaurus;

import phrases.Phrase;

import java.util.*;

/**
 * Dynamic program for calculating orthographic distance between two words
 * Can be used for abbreviations, in which case it is asymmetric (call it with abbreviation first, then longform!)
 *
 * Created by gpfinley on 7/21/16.
 */
public class OrthographicDistance {

    // Penalties:
    // For matching a character between abbreviation and longform (probably a zero penalty)
    private final double match;
    // For character substitution
    private final double sub;
    // For matching after a breaking character (might have negative penalty)
    private final double wordInitialMatch;
    // For matching at the beginning of both strings (highly preferred; might have a negative penalty)
    private final double initialMatch;
    // For deleting a character from the first word (happens rarely for abbreviations)
    private final double delA;
    // For deleting a character from the second word (happens all the time for abbreviations)
    private final double delB;
    // For deleting a word-initial character from the second word (long form for abbreviation usage)
    private final double delBWordInitial;
    // For deleting a the string-initial character from the second word
    private final double delBInitial;

    private final boolean caseSensitive;

    private static final Set<Character> breakingChars;
    static {
        breakingChars = new HashSet<>();
        Collections.addAll(breakingChars, ' ', '-', '/', '_', '&');
    }

    /**
     * Builder allows setting of specific parameters.
     * Any unset parameters default to those required for modified (i.e., substitutions not possible) edit distance.
     */
    public static class OrthographicDistanceBuilder {
        private boolean caseSensitive = false;
        private double match = 0;
        private double wordInitialMatch = 0;
        private double initialMatch = 0;
        private double delA = 1;
        private double delB = 1;
        private double sub = 1;
        private Double delBWordInitial;
        private Double delBInitial;
        public OrthographicDistanceBuilder caseSensitive(boolean caseSensitive) {
            this.caseSensitive = caseSensitive;
            return this;
        }
        public OrthographicDistanceBuilder matchPenalty(double matchPenalty) {
            this.match = matchPenalty;
            return this;
        }
        public OrthographicDistanceBuilder subPenalty(double subPenalty) {
            this.sub = subPenalty;
            return this;
        }
        public OrthographicDistanceBuilder wordInitialMatchPenalty(double wordInitialMatchPenalty) {
            this.wordInitialMatch = wordInitialMatchPenalty;
            return this;
        }
        public OrthographicDistanceBuilder stringInitialMatchPenalty(double initialMatchPenalty) {
            this.initialMatch = initialMatchPenalty;
            return this;
        }
        public OrthographicDistanceBuilder deleteString1Penalty(double deleteString1Penalty) {
            this.delA = deleteString1Penalty;
            return this;
        }
        public OrthographicDistanceBuilder deleteString2Penalty(double deleteString2Penalty) {
            this.delB = deleteString2Penalty;
            return this;
        }
        public OrthographicDistanceBuilder deleteString2WordInitialPenalty(double deleteString2WordInitialPenalty) {
            this.delBWordInitial = deleteString2WordInitialPenalty;
            return this;
        }
        public OrthographicDistanceBuilder deleteString2InitialPenalty(double deleteString2InitialPenalty) {
            this.delBInitial = deleteString2InitialPenalty;
            return this;
        }
        public OrthographicDistance build() {
            if(delBWordInitial == null) {
                delBWordInitial = delB;
            }
            if(delBInitial == null) {
                delBInitial = delBWordInitial;
            }
            return new OrthographicDistance(this);
        }
    }

//    private OrthographicDistance(boolean caseSensitive, double match, double wordInitialMatch, double initialMatch, double delA, double delB, double delBWordInitial) {
    private OrthographicDistance(OrthographicDistanceBuilder builder) {
        caseSensitive = builder.caseSensitive;
        match = builder.match;
        sub = builder.sub;
        wordInitialMatch = builder.wordInitialMatch;
        initialMatch = builder.initialMatch;
        delA = builder.delA;
        delB = builder.delB;
        delBWordInitial = builder.delBWordInitial;
        delBInitial = builder.delBInitial;
    }

    // todo: should these be specified in the properties file as well? or is it good to not make them that directly editable?
    // todo: they do need to be modifiable in some way without going to the ThesaurusMaker code; pass them in to the builder (that way, advanced users can define their own object)
    public static OrthographicDistance forAbbreviations() {
        return new OrthographicDistanceBuilder()
                .caseSensitive(false)
                .matchPenalty(0)
                .subPenalty(4)                      // effectively rule this out (highest str 1 penalty + highest str 2 penalty)
                .wordInitialMatchPenalty(0)
                .stringInitialMatchPenalty(0)
                .deleteString1Penalty(2)            // big penalty here
                .deleteString2Penalty(0)
                .deleteString2InitialPenalty(2)
                .deleteString2WordInitialPenalty(1)
                .build();
    }

    public static OrthographicDistance forNonAbbreviations() {
        return new OrthographicDistanceBuilder()
                .caseSensitive(false)
                .matchPenalty(0)
                .subPenalty(1)
                .wordInitialMatchPenalty(0)
                .stringInitialMatchPenalty(0)
                .deleteString1Penalty(1)
                .deleteString2Penalty(1)
                .build();
    }

    /**
     * Return the shortest possible distance between two Phrases when permuting the shorter of them.
     * Does NOT test the distance between both unpermuted phrases--
     *      this is so that a penalty can be applied just to the permutations
     * @param phraseA one phrase
     * @param phraseB another
     * @return the shortest distance (as defined by this object) for a permutation of these words
     */
    // todo: should this be made public?? would it be useful outside of this class?
    private double shortestMustPermute(Phrase phraseA, Phrase phraseB) {
        double shortestDist = Double.MAX_VALUE;
        if(phraseA.size() < phraseB.size()) {
            for(Phrase newA : phraseA.permutations()) {
                if(newA.equals(phraseA)) continue;
                double dist = distance(newA, phraseB);
                if(dist < shortestDist) {
                    shortestDist = dist;
                }
            }
        } else {
            for(Phrase newB : phraseB.permutations()) {
                if(newB.equals(phraseB)) continue;
                double dist = distance(phraseA, newB);
                if(dist < shortestDist) {
                    shortestDist = dist;
                }
            }
        }
        return shortestDist;
    }

    /**
     * Returns the shortest distance possible when considering all permutations
     * Will not add the penalty to the distance score between the unpermuted phrases
     * @param phraseA one phrase
     * @param phraseB another
     * @param permutationPenalty a constant penalty to assess on any distance measure that involves permuting a phrase
     *                           If null, do not attempt permutations, and just return the distance
     * @return the lowest score possible when taking the penalty into account
     */
    public double shortestPermutedDistance(Phrase phraseA, Phrase phraseB, Double permutationPenalty) {
        if(permutationPenalty == null) {
            return distance(phraseA, phraseB);
        }
        return Math.min(shortestMustPermute(phraseA, phraseB) + permutationPenalty, distance(phraseA, phraseB));
    }

    public double distance(Phrase wordA, Phrase wordB) {
        return distance(wordA.toString(), wordB.toString());
    }

    /**
     * Calculate the edit distance of an optimal alignment
     * @param wordA the first word to align (the abbreviation, if applicable)
     * @param wordB the second word to align (the longform, if applicable)
     * @return the distance between them
     */
    public double distance(String wordA, String wordB) {

        if(!caseSensitive) {
            wordA = wordA.toLowerCase();
            wordB = wordB.toLowerCase();
        }

        int m = wordA.length();
        int n = wordB.length();
        double[][] matrix = new double[m+1][n+1];

        for(int i=0; i<=m; i++) {

            for(int j=0; j<=n; j++) {

                double min=Double.MAX_VALUE;
                if(i==0 && j==0)
                    min = 0;

                if(i>0 && j>0) {
                    if(wordA.charAt(i-1) == wordB.charAt(j-1)) {
                        double diagScore = matrix[i-1][j-1];
                        if(j==1 && i==1) {
                            diagScore += initialMatch;
                        }
                        else {
                            diagScore += match;
                        }
                        if (diagScore < min)
                            min = diagScore;
                    } else {
                        double diagScore = matrix[i-1][j-1] + sub;
                        if (diagScore < min)
                            min = diagScore;
                    }
                }
                if(i>0) {
                    double downScore = matrix[i-1][j] + delA;
                    if(downScore < min)
                        min = downScore;
                }
                if(j>0) {
                    double rightScore = matrix[i][j-1];
                    if(j<2) {
                        rightScore += delBInitial;
                    } else if(breakingChars.contains(wordB.charAt(j - 2)) && !breakingChars.contains(wordB.charAt(j-1)) ) {
                        rightScore += delBWordInitial;
                    } else {
                        rightScore += delB;
                    }
                    if (rightScore < min)
                        min = rightScore;
                }
                matrix[i][j] = min;
            }
        }
        return matrix[m][n];
    }

}
