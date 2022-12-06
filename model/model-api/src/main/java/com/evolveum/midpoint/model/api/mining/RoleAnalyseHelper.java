/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.mining;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class RoleAnalyseHelper implements Serializable {

    public double jaccardIndex(List<String> membersRoleA, List<String> membersRoleB, int minRolesCount) {
        if (membersRoleA == null || membersRoleB == null) {
            return 0.0;
        }
        int totalSum = membersRoleA.size() + membersRoleB.size();
        if (totalSum == 0) {
            return 0.0;
        }
        int intersectionSum = 0;
        for (String role : membersRoleA) {
            if (membersRoleB.contains(role)) {
                intersectionSum++;
            }
        }

        if (intersectionSum < minRolesCount) {
            return 0.0;
        }

        return (double) (intersectionSum) / (totalSum - intersectionSum);
    }

    public List<ObjectReferenceType> roleIntersected(List<ObjectReferenceType> membersRoleA, List<ObjectReferenceType> membersRoleB) {
        List<ObjectReferenceType> intersectedRole = new ArrayList<>();
        for (ObjectReferenceType role : membersRoleA) {
            if (membersRoleB.contains(role)) {
                intersectedRole.add(role);
            }
        }
        return intersectedRole;
    }

    public double permissionSimilarity(List<AuthorizationType> permissionsA, List<AuthorizationType> permissionsB) {
        if (permissionsA == null || permissionsB == null) {
            return 0.0;
        }
        int totalSum = permissionsA.size() + permissionsB.size();
        if (totalSum == 0) {
            return 0.0;
        }
        int intersectionSum = 0;
        for (AuthorizationType permission : permissionsA) {
            if (permissionsB.contains(permission)) {
                intersectionSum++;
            }
        }
        return (double) (2 * intersectionSum) / totalSum;
    }

    public double membersSimilarity(List<PrismObject<UserType>> membersA, List<PrismObject<UserType>> membersB) {
        if (membersA == null || membersB == null) {
            return 0.0;
        }
        int totalSum = membersA.size() + membersB.size();
        if (totalSum == 0) {
            return 0.0;
        }
        int intersectionSum = 0;
        for (PrismObject<UserType> permission : membersA) {
            if (membersB.contains(permission)) {
                intersectionSum++;
            }
        }
        return (double) (2 * intersectionSum) / totalSum;
    }

    public double membersSimpleSimilarity(List<PrismObject<UserType>> membersA, List<PrismObject<UserType>> membersB) {
        if (membersA == null || membersB == null) {
            return 0.0;
        }
        int totalSum = membersA.size();
        if (totalSum == 0) {
            return 0.0;
        }
        int intersectionSum = 0;
        for (PrismObject<UserType> permission : membersA) {
            if (membersB.contains(permission)) {
                intersectionSum++;
            }
        }
        return (double) (intersectionSum) / totalSum;
    }

    public int rolesMembersIntersection(List<PrismObject<UserType>> membersA, List<PrismObject<UserType>> membersB) {
        if (membersA == null || membersB == null) {
            return 0;
        }
        int totalSum = membersA.size() + membersB.size();
        if (totalSum == 0) {
            return 0;
        }
        int intersectionSum = 0;
        for (PrismObject<UserType> permission : membersA) {
            if (membersB.contains(permission)) {
                intersectionSum++;
            }
        }
        return intersectionSum;
    }

    public int usersMembersIntersection(List<String> membersA, List<String> membersB) {
        if (membersA == null || membersB == null) {
            return 0;
        }
        int totalSum = membersA.size() + membersB.size();
        if (totalSum == 0) {
            return 0;
        }
        int intersectionSum = 0;
        for (String roleObjectId : membersA) {
            if (membersB.contains(roleObjectId)) {
                intersectionSum++;
            }
        }
        return intersectionSum;
    }

    public void rolesMembersIntersectionsMining(List<RoleMembersList> input, int minUser) {

        int inputSize = input.size();

        List<List<List<PrismObject<UserType>>>> combinationList = new ArrayList<>();
        for (int i = 0; i < inputSize; i++) {

            RoleMembersList inputElementRow = input.get(i);
            List<PrismObject<UserType>> membersElementRow = inputElementRow.getMembers();

            List<List<PrismObject<UserType>>> listsIntersection = new ArrayList<>();
            for (int j = 0; j < inputSize; j++) {
                if (i != j) {
                    RoleMembersList inputElementColumn = input.get(j);
                    List<PrismObject<UserType>> membersElementColumn = inputElementColumn.getMembers();

                    List<PrismObject<UserType>> intersection = membersElementRow.stream()
                            .filter(membersElementColumn::contains)
                            .distinct()
                            .collect(Collectors.toList());

                    if (intersection.size() >= minUser) {
                        listsIntersection.add(intersection);
                    }
                }

            }
            combinationList.add(listsIntersection);
        }
    }

}
