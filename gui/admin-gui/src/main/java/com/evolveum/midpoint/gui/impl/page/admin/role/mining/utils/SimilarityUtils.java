/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SimilarityUtils {


    public static double jacquardSimilarity(List<String> set1, List<String> set2) {
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        return (double) intersection.size() / union.size();
    }


    public static double jacquardSimilarity(Set<String> set1, Set<String> set2) {
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        return (double) intersection.size() / union.size();
    }

    public static double jacquardSimilarity(Set<String> set1, Set<String> set2, Set<String> intersection) {
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        return (double) intersection.size() / union.size();
    }

    public static Set<String> intersection(List<String> set1, List<String> set2) {

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        return intersection;
    }

    public static Set<String> intersection(Set<String> set1, Set<String> set2) {

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        return intersection;
    }

}
