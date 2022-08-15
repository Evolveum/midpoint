package com.evolveum.midpoint.schema.util.correlation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TriGram {

    private static final int nGramValue = 3;

    public List<String> generateTriGram(String object) {
        String[] normalizedInput = normalization(object);
        List<String> triGrams = new ArrayList<>();

        for (String preparedString : normalizedInput) {
            for (int j = 0; j < preparedString.length() - nGramValue + 1; j++) {
                String triGramSubstring = preparedString.substring(j, j + nGramValue);

                if (!triGrams.contains(triGramSubstring)) {
                    triGrams.add(triGramSubstring);
                }

            }
        }
        return triGrams;
    }

    private String[] normalization(String object) {
        String removeNonAlpha = object.replaceAll("[^\\p{Alnum}]", " ");
        String normalizeWhiteSpaces = removeNonAlpha.replaceAll("\\s{2,}", " ").trim();
        String[] strArray = normalizeWhiteSpaces.split(" ");

        for (int i = 0; i < strArray.length; i++) {
            String normalizedString = "  " + strArray[i] + " ";
            strArray[i] = normalizedString.toLowerCase();
        }

        return strArray;
    }

    public double getSimilarity(String lObject, String rObject) {
        List<String> firstTriGrams = generateTriGram(lObject);
        List<String> secondTriGrams = generateTriGram(rObject);

        List<String> intersectionList = intersection(firstTriGrams, secondTriGrams);
        List<String> unionList = union(firstTriGrams, secondTriGrams);

        double intersectionListSize = intersectionList.size();
        double unionListSize = unionList.size();

        return (intersectionListSize / unionListSize);
    }

    private <T> List<T> union(List<T> list1, List<T> list2) {
        Set<T> set = new HashSet<>();
        set.addAll(list1);
        set.addAll(list2);
        return new ArrayList<>(set);
    }

    private <T> List<T> intersection(List<T> list1, List<T> list2) {
        List<T> list = new ArrayList<>();
        for (T t : list1) {
            if (list2.contains(t)) {
                list.add(t);
            }
        }
        return list;
    }

}
