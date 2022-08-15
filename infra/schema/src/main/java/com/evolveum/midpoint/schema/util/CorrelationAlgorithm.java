package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.schema.util.correlation.Levenshtein;
import com.evolveum.midpoint.schema.util.correlation.TriGram;

import java.util.List;

public class CorrelationAlgorithm {

    Levenshtein levenshtein;
    TriGram triGram;

    public CorrelationAlgorithm() {
        levenshtein = new Levenshtein();
        triGram = new TriGram();
    }

    public double triGramSimilarity(String lObject, String rObject) {
        return triGram.getSimilarity(lObject, rObject);
    }

    public List<String> triGramForm(String object) {
        return triGram.generateTriGram(object);
    }

    public double levenshteinSimilarity(String lObject, String rObject) {
        int levenshteinDistance = levenshteinDistance(lObject, rObject);
        return levenshtein.computeLevenshteinSimilarity(lObject, rObject, levenshteinDistance);
    }

    public int levenshteinDistance(String lObject, String rObject) {
        return levenshtein.computeLevenshteinDistance(lObject, rObject);
    }



}
