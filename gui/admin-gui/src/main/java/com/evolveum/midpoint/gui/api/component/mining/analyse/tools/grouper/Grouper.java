/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining.analyse.tools.grouper;

import com.evolveum.midpoint.gui.api.component.mining.analyse.tools.jaccard.UniqueRoleSet;
import com.evolveum.midpoint.gui.api.component.mining.analyse.tools.utils.RoleUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.util.OidUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.evolveum.midpoint.gui.api.component.mining.analyse.tools.utils.RoleUtils.getRolesId;
import static com.evolveum.midpoint.gui.api.component.mining.analyse.tools.utils.RoleUtils.intersectionCount;

public class Grouper {
    private static List<PrismObject<UserType>> extractUsersWithMinimumIntersection(List<PrismObject<UserType>> users,
            int minIntersection) {
        List<PrismObject<UserType>> result = new ArrayList<>();

        for (int i = 0; i < users.size(); i++) {
            PrismObject<UserType> currentUser = users.get(i);
            int intersectionCount = 0;

            for (int j = 0; j < users.size(); j++) {
                if (i != j) {
                    PrismObject<UserType> otherUser = users.get(j);

                    List<String> currentUserValues = getRolesId(currentUser.asObjectable());
                    List<String> otherUserValues = getRolesId(otherUser.asObjectable());

                    int matchingValues = intersectionCount(currentUserValues, otherUserValues);

                    if (matchingValues >= minIntersection) {
                        intersectionCount++;
                    }
                }
            }

            if (intersectionCount >= minIntersection) {
                result.add(currentUser);
            }
        }

        return result;
    }

    public static List<UniqueRoleSet> generateIntersectionGroups(List<PrismObject<UserType>> users, int minIntersection) {

        List<UniqueRoleSet> usersGroupedByRoles = new ArrayList<>();

        List<PrismObject<UserType>> userTypeList;
        List<String> roleTypeList;
        for (int i = 1; i < users.size(); i++) {
            PrismObject<UserType> userA = users.get(i);
            roleTypeList = new ArrayList<>(getRolesId(userA.asObjectable()));
            userTypeList = new ArrayList<>();
            userTypeList.add(userA);

            for (PrismObject<UserType> userB : users) {
                int similarity = intersectionCount(getRolesId(userA.asObjectable()), getRolesId(userB.asObjectable()));
                if (similarity >= minIntersection) {
                    userTypeList.add(userB);
                }
            }

            List<PrismObject<UserType>> prismObjects = extractUsersWithMinimumIntersection(userTypeList, minIntersection);
            usersGroupedByRoles.add(new UniqueRoleSet(userA.getName().toString(), roleTypeList, prismObjects));
        }

        return usersGroupedByRoles;
    }

    public static List<UniqueRoleSet> generateUniqueSetsGroup(List<PrismObject<UserType>> users) {

        Map<String, List<PrismObject<UserType>>> roleUserMap = new TreeMap<>();
        for (PrismObject<UserType> user : users) {
            List<String> roles = new ArrayList<>(getRolesId(user.asObjectable()));
            if (roles.isEmpty()) {
                continue;
            }

            String roleKey = roles.toString();

            if (roleUserMap.containsKey(roleKey)) {
                roleUserMap.get(roleKey).add(user);
            } else {
                List<PrismObject<UserType>> userList = new ArrayList<>();
                userList.add(user);
                roleUserMap.put(roleKey, userList);
            }
        }

        List<UniqueRoleSet> usersGroupedByRoles = new ArrayList<>();

        for (List<PrismObject<UserType>> usersList : roleUserMap.values()) {
            usersGroupedByRoles.add(new UniqueRoleSet(OidUtil.generateOid(),
                    getRolesId(usersList.get(0).asObjectable()), usersList));
        }

        return usersGroupedByRoles;
    }


    public static List<UniqueRoleSet> getRoleGroupByJc(List<PrismObject<UserType>> users, double threshold) {

        List<UniqueRoleSet> usersGroupedByRoles = new ArrayList<>();

        List<PrismObject<UserType>> userTypeList;
        List<String> roleTypeList;
        for (int i = 1; i < users.size(); i++) {
            PrismObject<UserType> userA = users.get(i);
            roleTypeList = new ArrayList<>(getRolesId(userA.asObjectable()));
            userTypeList = new ArrayList<>();
            userTypeList.add(userA);

            for (int j = i + 1; j < users.size(); j++) {
                PrismObject<UserType> userB = users.get(j);
                double similarity = RoleUtils.jacquardSimilarity(
                        getRolesId(userA.asObjectable()), getRolesId(userB.asObjectable()));
                if (similarity >= threshold) {
                    userTypeList.add(userB);
                }
            }
            usersGroupedByRoles.add(new UniqueRoleSet(OidUtil.generateOid(), roleTypeList, userTypeList));

        }

        return usersGroupedByRoles;
    }

}
