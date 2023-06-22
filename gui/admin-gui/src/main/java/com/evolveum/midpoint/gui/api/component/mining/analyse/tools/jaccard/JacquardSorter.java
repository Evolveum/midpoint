/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining.analyse.tools.jaccard;

import static com.evolveum.midpoint.gui.api.component.mining.analyse.tools.grouper.Grouper.generateUniqueSetsGroup;
import static com.evolveum.midpoint.gui.api.component.mining.analyse.tools.grouper.Grouper.getRoleGroupByJc;

import java.util.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.RoleUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MiningType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class JacquardSorter {

    private static double jacquardSimilarity(@NotNull UserSet set1, UserSet set2) {
        Set<String> union = new HashSet<>(set1.roles);
        union.addAll(set2.roles);

        Set<String> intersection = new HashSet<>(set1.roles);
        intersection.retainAll(set2.roles);

        return (double) intersection.size() / union.size();
    }

    private static double jacquardSimilarityMs(@NotNull UniqueRoleSet set1, UniqueRoleSet set2) {
        Set<String> union = new HashSet<>(set1.roles);
        union.addAll(set2.roles);

        Set<String> intersection = new HashSet<>(set1.roles);
        intersection.retainAll(set2.roles);

        return (double) intersection.size() / union.size();
    }

    public static List<PrismObject<MiningType>> jaccSortMiningSet(List<PrismObject<MiningType>> miningSets) {

        List<PrismObject<MiningType>> sortedUserSets = new ArrayList<>();
        List<PrismObject<MiningType>> remainingUserSets = new ArrayList<>(miningSets);

        remainingUserSets.sort(Comparator.comparingInt(set -> -set.asObjectable().getRoles().size()));

        while (!remainingUserSets.isEmpty()) {
            PrismObject<MiningType> currentUserSet = remainingUserSets.remove(0);
            double maxSimilarity = 0;
            int insertIndex = -1;

            if (sortedUserSets.size() < 2) {
                if (sortedUserSets.isEmpty()) {
                    sortedUserSets.add(currentUserSet);
                } else {
                    sortedUserSets.add(0, currentUserSet);
                }
            } else {
                for (int i = 1; i < sortedUserSets.size(); i++) {
                    PrismObject<MiningType> prevUserSet = sortedUserSets.get(i - 1);
                    PrismObject<MiningType> nextUserSet = sortedUserSets.get(i);
                    double similarity = RoleUtils.jacquardSimilarity(currentUserSet.asObjectable().getRoles(),
                            prevUserSet.asObjectable().getRoles());
                    double nextSimilarity = RoleUtils.jacquardSimilarity(currentUserSet.asObjectable().getRoles(),
                            nextUserSet.asObjectable().getRoles());

                    if (Math.max(similarity, nextSimilarity) > maxSimilarity
                            && Math.min(similarity, nextSimilarity) >= RoleUtils.jacquardSimilarity(
                                    prevUserSet.asObjectable().getRoles(), nextUserSet.asObjectable().getRoles())) {
                        maxSimilarity = Math.max(similarity, nextSimilarity);
                        insertIndex = i;
                    }
                }

                if (insertIndex == -1) {
                    if (RoleUtils.jacquardSimilarity(currentUserSet.asObjectable().getRoles(),
                            sortedUserSets.get(0).asObjectable().getRoles())
                            > RoleUtils.jacquardSimilarity(sortedUserSets.get(0).asObjectable().getRoles(),
                            sortedUserSets.get(1).asObjectable().getRoles())) {
                        sortedUserSets.add(0, currentUserSet);
                    } else {
                        sortedUserSets.add(currentUserSet);
                    }
                } else {
                    sortedUserSets.add(insertIndex, currentUserSet);
                }
            }
        }

        return sortedUserSets;
    }

    public static List<UniqueRoleSet> jaccSortUn(List<UniqueRoleSet> miningSets) {

        List<UniqueRoleSet> sortedUserSets = new ArrayList<>();
        List<UniqueRoleSet> remainingUserSets = new ArrayList<>(miningSets);

        remainingUserSets.sort(Comparator.comparingInt(set -> -set.roles.size()));

        while (!remainingUserSets.isEmpty()) {
            UniqueRoleSet currentUserSet = remainingUserSets.remove(0);
            double maxSimilarity = 0;
            int insertIndex = -1;

            if (sortedUserSets.size() < 2) {
                if (sortedUserSets.isEmpty()) {
                    sortedUserSets.add(currentUserSet);
                } else {
                    sortedUserSets.add(0, currentUserSet);
                }
            } else {
                for (int i = 1; i < sortedUserSets.size(); i++) {
                    UniqueRoleSet prevUserSet = sortedUserSets.get(i - 1);
                    UniqueRoleSet nextUserSet = sortedUserSets.get(i);
                    double similarity = jacquardSimilarityMs(currentUserSet, prevUserSet);
                    double nextSimilarity = jacquardSimilarityMs(currentUserSet, nextUserSet);

                    if (Math.max(similarity, nextSimilarity) > maxSimilarity
                            && Math.min(similarity, nextSimilarity) >= jacquardSimilarityMs(prevUserSet, nextUserSet)) {
                        maxSimilarity = Math.max(similarity, nextSimilarity);
                        insertIndex = i;
                    }
                }

                if (insertIndex == -1) {
                    if (jacquardSimilarityMs(currentUserSet, sortedUserSets.get(0))
                            > jacquardSimilarityMs(sortedUserSets.get(0), sortedUserSets.get(1))) {
                        sortedUserSets.add(0, currentUserSet);
                    } else {
                        sortedUserSets.add(currentUserSet);
                    }
                } else {
                    sortedUserSets.add(insertIndex, currentUserSet);
                }
            }
        }

        return sortedUserSets;
    }

    public static @NotNull List<UserSet> sortUserSets(List<PrismObject<UserType>> userTypeList) {

        List<UserSet> userSets = prepareUserSet(userTypeList);

        List<UserSet> sortedUserSets = new ArrayList<>();
        List<UserSet> remainingUserSets = new ArrayList<>(userSets);

        remainingUserSets.sort(Comparator.comparingInt(set -> -set.roles.size()));

        while (!remainingUserSets.isEmpty()) {
            UserSet currentUserSet = remainingUserSets.remove(0);
            double maxSimilarity = 0;
            int insertIndex = -1;

            if (sortedUserSets.size() < 2) {
                if (sortedUserSets.isEmpty()) {
                    sortedUserSets.add(currentUserSet);
                } else {
                    sortedUserSets.add(0, currentUserSet);
                }
            } else {
                for (int i = 1; i < sortedUserSets.size(); i++) {
                    UserSet prevUserSet = sortedUserSets.get(i - 1);
                    UserSet nextUserSet = sortedUserSets.get(i);
                    double similarity = jacquardSimilarity(currentUserSet, prevUserSet);
                    double nextSimilarity = jacquardSimilarity(currentUserSet, nextUserSet);

                    if (Math.max(similarity, nextSimilarity) > maxSimilarity
                            && Math.min(similarity, nextSimilarity) >= jacquardSimilarity(prevUserSet, nextUserSet)) {
                        maxSimilarity = Math.max(similarity, nextSimilarity);
                        insertIndex = i;
                    }
                }

                if (insertIndex == -1) {
                    if (jacquardSimilarity(currentUserSet, sortedUserSets.get(0))
                            > jacquardSimilarity(sortedUserSets.get(0), sortedUserSets.get(1))) {
                        sortedUserSets.add(0, currentUserSet);
                    } else {
                        sortedUserSets.add(currentUserSet);
                    }
                } else {
                    sortedUserSets.add(insertIndex, currentUserSet);
                }
            }
        }

        return sortedUserSets;
    }

    public static @NotNull List<UserSet> sortUserSetsJc(List<PrismObject<UserType>> userTypeList, double jcThreshold) {

        List<UserSet> userSets = prepareUserSetJc(userTypeList, jcThreshold);

        List<UserSet> sortedUserSets = new ArrayList<>();
        List<UserSet> remainingUserSets = new ArrayList<>(userSets);

        remainingUserSets.sort(Comparator.comparingInt(set -> -set.roles.size()));

        while (!remainingUserSets.isEmpty()) {
            UserSet currentUserSet = remainingUserSets.remove(0);
            double maxSimilarity = 0;
            int insertIndex = -1;

            if (sortedUserSets.size() < 2) {
                if (sortedUserSets.isEmpty()) {
                    sortedUserSets.add(currentUserSet);
                } else {
                    sortedUserSets.add(0, currentUserSet);
                }
            } else {
                for (int i = 1; i < sortedUserSets.size(); i++) {
                    UserSet prevUserSet = sortedUserSets.get(i - 1);
                    UserSet nextUserSet = sortedUserSets.get(i);
                    double similarity = jacquardSimilarity(currentUserSet, prevUserSet);
                    double nextSimilarity = jacquardSimilarity(currentUserSet, nextUserSet);

                    if (Math.max(similarity, nextSimilarity) > maxSimilarity
                            && Math.min(similarity, nextSimilarity) >= jacquardSimilarity(prevUserSet, nextUserSet)) {
                        maxSimilarity = Math.max(similarity, nextSimilarity);
                        insertIndex = i;
                    }
                }

                if (insertIndex == -1) {
                    if (jacquardSimilarity(currentUserSet, sortedUserSets.get(0))
                            > jacquardSimilarity(sortedUserSets.get(0), sortedUserSets.get(1))) {
                        sortedUserSets.add(0, currentUserSet);
                    } else {
                        sortedUserSets.add(currentUserSet);
                    }
                } else {
                    sortedUserSets.add(insertIndex, currentUserSet);
                }
            }
        }

        return sortedUserSets;
    }

    public static @NotNull List<UserSet> prepareUserSet(List<PrismObject<UserType>> users) {

        List<UniqueRoleSet> itemSets = generateUniqueSetsGroup(users);
        List<UserSet> userSets = new ArrayList<>();

        for (UniqueRoleSet item : itemSets) {
            userSets.add(new UserSet(new HashSet<>(item.getUsers()), new HashSet<>(item.getRoles())));
        }
        return userSets;

    }

    public static @NotNull List<UserSet> prepareUserSetJc(List<PrismObject<UserType>> users, double jcThreshold) {

        List<UniqueRoleSet> itemSets = getRoleGroupByJc(users, jcThreshold);
        List<UserSet> userSets = new ArrayList<>();

        for (UniqueRoleSet item : itemSets) {
            userSets.add(new UserSet(new HashSet<>(item.getUsers()), new HashSet<>(item.getRoles())));
        }
        return userSets;

    }

}
