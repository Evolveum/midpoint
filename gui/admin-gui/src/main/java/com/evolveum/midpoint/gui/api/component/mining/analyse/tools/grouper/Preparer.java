/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining.analyse.tools.grouper;

import static com.evolveum.midpoint.gui.api.component.mining.analyse.tools.jaccard.JacquardSorter.jaccSortUn;
import static com.evolveum.midpoint.gui.api.component.mining.analyse.tools.grouper.Grouper.generateIntersectionGroups;
import static com.evolveum.midpoint.gui.api.component.mining.analyse.tools.grouper.Grouper.generateUniqueSetsGroup;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.component.mining.analyse.tools.utils.RoleUtils;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.mining.analyse.tools.jaccard.UniqueRoleSet;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class Preparer {

    public @NotNull
    static List<MiningSet> prepareMiningSet(List<PrismObject<UserType>> users, double threshold) {
        List<MiningSet> miningSets = new ArrayList<>();
        List<UniqueRoleSet> uniqueRoleSet = jaccSortUn(generateUniqueSetsGroup(users));
        int size = uniqueRoleSet.size();

        for (int i = 0; i < size; i++) {
            List<Integer> rolesByThreshold = new ArrayList<>();
            double ration = 0;
            for (int j = 0; j < size; j++) {
                if (j != i) {

                    double similarity = RoleUtils.jacquardSimilarity(uniqueRoleSet.get(i).getRoles(),
                            uniqueRoleSet.get(j).getRoles());
                    ration += similarity;

                    if (similarity >= threshold) {
                        rolesByThreshold.add(j);
                    }

                }
            }
            ration /= size;
            UniqueRoleSet roleSet = uniqueRoleSet.get(i);
            miningSets.add(new MiningSet(uniqueRoleSet.get(i).getoId(),
                    i, ration, roleSet.getRoles(), rolesByThreshold, roleSet.getUsers()));
        }
        return miningSets;
    }

    public @NotNull
    static List<MiningSet> prepareMiningSetIntersected(List<PrismObject<UserType>> users, int minIntersection, double threshold) {
        List<MiningSet> miningSets = new ArrayList<>();
        List<UniqueRoleSet> uniqueRoleSet = jaccSortUn(generateIntersectionGroups(users, minIntersection));
        int size = uniqueRoleSet.size();

        for (int i = 0; i < size; i++) {
            List<Integer> rolesByThreshold = new ArrayList<>();
            double ration = 0;
            for (int j = 0; j < size; j++) {
                if (j != i) {

                    double similarity = RoleUtils.jacquardSimilarity(uniqueRoleSet.get(i).getRoles(),
                            uniqueRoleSet.get(j).getRoles());
                    ration += similarity;

                    if (similarity >= threshold) {
                        rolesByThreshold.add(j);
                    }

                }
            }
            ration /= size;
            UniqueRoleSet roleSet = uniqueRoleSet.get(i);
            miningSets.add(new MiningSet(uniqueRoleSet.get(i).getoId(), i, ration, roleSet.getRoles(),
                    rolesByThreshold, roleSet.getUsers()));
        }
        return miningSets;
    }

}
