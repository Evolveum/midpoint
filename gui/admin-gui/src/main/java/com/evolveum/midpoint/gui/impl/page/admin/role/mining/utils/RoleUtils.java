/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

public class RoleUtils {

    public static List<String> getRolesId(AssignmentHolderType object) {
        return IntStream.range(0, object.getRoleMembershipRef().size())
                .filter(i -> object.getRoleMembershipRef().get(i).getType().getLocalPart()
                        .equals("RoleType")).mapToObj(i -> object.getRoleMembershipRef().get(i).getOid())
                .sorted(Comparator.naturalOrder()).collect(Collectors.toList());
    }

    public static double jacquardSimilarity(List<String> set1, List<String> set2) {
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

    public static int intersectionCount(List<String> set1, List<String> set2) {
        return intersection(set1, set2).size();
    }



    public static Set<String> intersection(Set<String> set1, Set<String> set2) {

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        return intersection;
    }

    public static int intersectionCount(Set<String> set1, Set<String> set2) {
        return intersection(set1, set2).size();
    }



}
