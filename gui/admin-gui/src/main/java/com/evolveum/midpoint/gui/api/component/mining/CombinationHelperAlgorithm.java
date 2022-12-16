/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining;

import java.util.*;

public class CombinationHelperAlgorithm {

    public List<List<String>> generateCombinations(List<String> elements, int minSize) {
        List<List<String>> combinationList = new ArrayList<>();
        for (long i = 1; i < Math.pow(2, elements.size()); i++) {
            List<String> list = new ArrayList<>();
            for (int j = 0; j < elements.size(); j++) {
                if ((i & (long) Math.pow(2, j)) > 0) {
                    list.add(elements.get(j));
                }
            }
            if (list.size() > minSize) {
                combinationList.add(list);
            }
        }
        return combinationList;
    }

    public List<List<String>> combinationsResult(List<List<String>> combination, List<List<String>> matrix) {
        ArrayList<Integer> counts = new ArrayList<>();
        List<CombinationHelperList> combinationHelperLists = new ArrayList<>();
        for (List<String> combinationForCheck : combination) {
            int counter = 0;
            for (List<String> matrixRowForCheck : matrix) {
                if (new HashSet<>(matrixRowForCheck).containsAll(combinationForCheck)) {
                    counter++;
                }
            }

            if(counter != 0) {
                counts.add(counter);
                combinationHelperLists.add(new CombinationHelperList(counter, combinationForCheck));
            }
        }

        if(counts.size() > 0) {
            Integer maxCount = Collections.max(counts);

            List<List<String>> result = new ArrayList<>();
            for (CombinationHelperList storageRow : combinationHelperLists) {
                if (storageRow.getCount() == maxCount) {
                    result.add(storageRow.getCombinations());
                }
            }
            return sort(result);
        }
        return null;
    }

    public int findDuplicates(List<String> combination, List<List<String>> matrix) {
        int counter = 0;
        for (List<String> strings : matrix) {
            if (new HashSet<>(strings).containsAll(combination)) {
                counter++;
            }
        }
        return counter;
    }

    public static <T> List<List<T>> sort(List<List<T>> list) {
        list.sort(Comparator.comparingInt(List::size));
        return list;
    }

}



