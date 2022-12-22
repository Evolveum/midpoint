/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class RoleAnalyseHelper implements Serializable {

    //indicates the number of users who have all permissions corresponding to the selected role
    public List<PrismObject<UserType>> authUserR(RoleType role, List<UPStructure> upStructures) {

        List<AuthorizationType> rolePermission = new RoleMiningFilter().getAuthorization(role);

        if (rolePermission == null || upStructures == null) {
            return null;
        }

        List<PrismObject<UserType>> authUserList;

        authUserList = upStructures.stream().filter(upStructure -> upStructure.getAssignPermission()
                .containsAll(rolePermission)).map(UPStructure::getUserObject).collect(Collectors.toList());

        return authUserList;
    }

    //indicates the percentage of users who have all permissions corresponding to the selected role
    public double supportR(RoleType role, List<UPStructure> upStructures,
            int userCount) {

        List<AuthorizationType> rolePermission = new RoleMiningFilter().getAuthorization(role);
        if (rolePermission == null || upStructures == null || userCount == 0) {
            return 0;
        }

        List<PrismObject<UserType>> authUserList = authUserR(role, upStructures);
        if (authUserList == null) {
            return 0;
        }

        int authUserR = authUserList.size();
        double supportR =(double) authUserR / userCount;

        return (Math.round(supportR * 100.0) / 100.0);

    }

    //indicates the number of permissions of the selected role (?) \\TODO
    public List<AuthorizationType> authPermsR(RoleType role) {
        return new RoleMiningFilter().getAuthorization(role);
    }

    //indicates the number of permissions of the selected role (?) \\TODO
    public int degreeR(RoleType role) {
        return authPermsR(role).size();
    }

    public double confidenceR2R1(RoleType parent, RoleType child, List<UPStructure> upStructures,
            int userCount) {

        double confidenceR2R1 =  supportR(parent, upStructures, userCount) / supportR(child, upStructures, userCount);
        return (Math.round(confidenceR2R1 * 100.0) / 100.0);
    }


    public boolean logicParentCheck(RoleType parent, RoleType child, List<UPStructure> upStructures) {

        List<PrismObject<UserType>> authUserParent = authUserR(parent, upStructures);
        List<PrismObject<UserType>> authUserChild = authUserR(child, upStructures);
        List<AuthorizationType> permParent = authPermsR(parent);
        List<AuthorizationType> permChild = authPermsR(child);

        //r1 -> parent
        //r2 -> child
        //r1 ≽ r2 = auth_users(r1) ⊆ auth_users(r2) ∧ auth_perms(r1) ⊇ auth_perms(r2).
        return authUserChild.containsAll(authUserParent) && permParent.containsAll(permChild);
    }

    public List<PrismObject<RoleType>> jaccardGetRolesGroup(RoleMiningStructureList selectedData,
            List<PrismObject<UserType>> jaccardUsersAnalysed,
            PageBase pageBase) {

        List<PrismObject<RoleType>> jaccardResultRoles = new ArrayList<>();
        if (selectedData != null) {

            List<ObjectReferenceType> rolesForCompare = new RoleMiningFilter().getRoleObjectReferenceTypes(selectedData.getUserObject().asObjectable());
            for (PrismObject<UserType> userTypePrismObject : jaccardUsersAnalysed) {
                rolesForCompare = roleIntersected(rolesForCompare,
                        new RoleMiningFilter().getRoleObjectReferenceTypes(userTypePrismObject.asObjectable()));
            }

            for (ObjectReferenceType objectReferenceType : rolesForCompare) {
                try {
                    jaccardResultRoles.add(new RoleMiningFilter().getRoleByOid(objectReferenceType.getOid(), pageBase));
                } catch (CommonException e) {
                    e.printStackTrace();
                }
            }
        }
        return jaccardResultRoles;
    }

    public List<PrismObject<UserType>> jaccardGetUserGroup(RoleMiningStructureList selectedData,
            double jaccardThreshold,
            List<PrismObject<UserType>> users) {

        List<PrismObject<UserType>> jaccardUsersAnalysed = new ArrayList<>();

        if (selectedData != null) {

            for (int j = 0; j < selectedData.getObjectPartialResult().size(); j++) {
                if (selectedData.getObjectPartialResult().get(j) >= jaccardThreshold) {
                    jaccardUsersAnalysed.add(users.get(j));
                }
            }
        }
        return jaccardUsersAnalysed;
    }

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
