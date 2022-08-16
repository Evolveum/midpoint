package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.query.fuzzy.LevenshteinComputer;
import com.evolveum.midpoint.prism.query.fuzzy.TriGramSimilarityComputer;

import java.util.List;

public class CorrelationAlgorithm {

    public double triGramSimilarity(String lObject, String rObject) {
        return TriGramSimilarityComputer.getSimilarity(lObject, rObject);
    }

    public List<String> triGramForm(String object) {
        return TriGramSimilarityComputer.generateTriGram(object);
    }

    public double levenshteinSimilarity(String lObject, String rObject) {
        int levenshteinDistance = levenshteinDistance(lObject, rObject);
        return LevenshteinComputer.computeLevenshteinSimilarity(lObject, rObject, levenshteinDistance);
    }

    public int levenshteinDistance(String lObject, String rObject) {
        return LevenshteinComputer.computeLevenshteinDistance(lObject, rObject);
    }



}
