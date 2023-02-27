/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining.analyse.tools;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.evolveum.midpoint.gui.api.component.mining.RoleMiningFilter;
import com.evolveum.midpoint.gui.api.component.mining.analyse.structure.RoleAnalyseStructure;
import com.evolveum.midpoint.gui.api.component.mining.structure.RoleMembersList;
import com.evolveum.midpoint.gui.api.component.mining.structure.RoleMiningStructureList;
import com.evolveum.midpoint.gui.api.component.mining.structure.UPStructure;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class RoleAnalyseHelper implements Serializable {

    public double actualSupportRInputPermission(List<AuthorizationType> authorizationTypes,
            List<RoleAnalyseStructure> roleAnalyseStructure, int userCount) {

        double value = ((double) assignUserRInputPermission(authorizationTypes, roleAnalyseStructure).size()) / userCount;

        return (Math.round(value * 100.0) / 100.0);
    }

    public List<UserType> assignUserRInputPermission(List<AuthorizationType> authorizationTypes,
            List<RoleAnalyseStructure> roleAnalyseStructure) {

        List<UserType> parentMemberList = new ArrayList<>();
        for (int i = 0; i < roleAnalyseStructure.size(); i++) {
            if (roleAnalyseStructure.get(i).getPermission().containsAll(authorizationTypes)
                    && !roleAnalyseStructure.get(i).getPermission().equals(authorizationTypes)) {
                List<PrismObject<UserType>> removableMembers = roleAnalyseStructure.get(i).getMembers();
                int bound = removableMembers.size();
                for (int r = 0; r < bound; r++) {
                    if (!parentMemberList.contains(removableMembers.get(r).asObjectable())) {
                        parentMemberList.add(removableMembers.get(r).asObjectable());
                    }
                }
            }
        }

        List<UserType> members = new ArrayList<>();
        for (int i = 0; i < roleAnalyseStructure.size(); i++) {
            if (roleAnalyseStructure.get(i).getPermission().equals(authorizationTypes)) {
                List<PrismObject<UserType>> tempMembers = roleAnalyseStructure.get(i).getMembers();
                tempMembers.stream().map(tempMember -> tempMember.asObjectable()).forEach(members::add);
                int bound = tempMembers.size();
                for (int m = 0; m < bound; m++) {
                    if (!parentMemberList.contains(tempMembers.get(m).asObjectable())) {
                        members.add(tempMembers.get(m).asObjectable());
                    }
                }
            }
        }

        return members;
    }

    public List<String> roleObjectIdRefType(AssignmentHolderType object) {
        return IntStream.range(0, object.getRoleMembershipRef().size())
                .filter(i -> object.getRoleMembershipRef().get(i).getType().getLocalPart()
                        .equals("RoleType")).mapToObj(i -> object.getRoleMembershipRef().get(i).getOid()).collect(Collectors.toList());
    }

    //TODO check if correct
    // if obtain parent role at this time we ignore user (We will decide later whether to count
    // these us, despite the fact that they also consist of a parent role)
    // we can use logicParentCheck()
    public List<PrismObject<UserType>> assignUserR(RoleType role, List<RoleAnalyseStructure> roleAnalyseStructures) {

        List<PrismObject<UserType>> result = new ArrayList<>();

        List<PrismObject<UserType>> members = roleAnalyseStructures.stream().filter(roleAnalyseStructure -> roleAnalyseStructure.getRoleType().equals(role))
                .findFirst().map(RoleAnalyseStructure::getMembers).orElse(new ArrayList<>());

        List<RoleType> listOfParents = getRoleParent(role, roleAnalyseStructures);

        for (PrismObject<UserType> member : members) {
            int counter = 0;
            AssignmentHolderType object = member.asObjectable();
            List<String> roleOidList = roleObjectIdRefType(object);

            for (RoleType listOfParent : listOfParents) {
                if (roleOidList.contains(listOfParent.getOid())) {
                    counter++;
                }
            }

            if (counter == 0) {
                result.add(member);
            }

        }

        return result;
    }

    //TODO
    public List<AuthorizationType> assignPermR(RoleType role, List<RoleAnalyseStructure> roleAnalyseStructures) {

        if (getRoleParent(role, roleAnalyseStructures).size() != 0) {
            return null;
        }
        return new RoleMiningFilter().getAuthorization(role);
    }

    //TODO
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

    public List<PrismObject<UserType>> authUserRInputPermission(List<AuthorizationType> rolePermission, List<UPStructure> upStructures) {

        if (rolePermission == null || upStructures == null) {
            return null;
        }

        List<PrismObject<UserType>> authUserList;

        authUserList = upStructures.stream().filter(upStructure -> upStructure.getAssignPermission()
                .containsAll(rolePermission)).map(UPStructure::getUserObject).collect(Collectors.toList());

        return authUserList;
    }

    //indicates the number of permissions of the selected role (?) \\TODO
    public List<AuthorizationType> authPermsR(RoleType role) {
        return new RoleMiningFilter().getAuthorization(role);
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
        double supportR = (double) authUserR / userCount;

        return (Math.round(supportR * 100.0) / 100.0);

    }

    public double supportRInputPermission(List<AuthorizationType> rolePermission, List<UPStructure> upStructures,
            int userCount) {

        if (rolePermission == null || upStructures == null || userCount == 0) {
            return 0;
        }

        List<PrismObject<UserType>> authUserList = authUserRInputPermission(rolePermission, upStructures);
        if (authUserList == null) {
            return 0;
        }

        int authUserR = authUserList.size();

        return (double) authUserR / userCount;

    }

    //indicates the number of permissions of the selected role (?) \\TODO
    public int degreeR(RoleType role) {
        return authPermsR(role).size();
    }

    public double confidenceR2R1(RoleType parent, RoleType child, List<UPStructure> upStructures,
            int userCount) {

        double confidenceR2R1 = supportR(parent, upStructures, userCount) / supportR(child, upStructures, userCount);
        return (Math.round(confidenceR2R1 * 100.0) / 100.0);
    }

    public double confidenceConnection(double supportParent, double supportChild) {

        double confidenceR2R1 = supportParent / supportChild;
        return (Math.round(confidenceR2R1 * 100.0) / 100.0);
    }

    //TODO
    //Among all users possessing the permissions assigned to role r, only a subset will likely be assigned to r.
    // Therefore, we define actual support, as actual_support(r) = |assign_users(r)|/|USERS|.
    public double actualSupportR(RoleType role, List<RoleAnalyseStructure> roleAnalyseStructures, int userCount) {

        double value = ((double) assignUserR(role, roleAnalyseStructures).size()) / userCount;

        return (Math.round(value * 100.0) / 100.0);
    }

    public boolean logicParentCheck(RoleType parent, RoleType child, List<UPStructure> upStructures) {

        List<PrismObject<UserType>> authUserParent = authUserR(parent, upStructures);
        List<PrismObject<UserType>> authUserChild = authUserR(child, upStructures);
        List<AuthorizationType> permParent = authPermsR(parent);
        List<AuthorizationType> permChild = authPermsR(child);

        /*RH ⊆ ROLES × ROLES is a partial order on ROLES called the inheritance relation
         if all permissions of r2 are also  permissions of r1, and all users of r1 are also users of r2.
        authorized_permissions(r2)⊆ authorized_permissions(r1).*/
        //r1 -> parent
        //r2 -> child
        //r1 ≽ r2 = auth_users(r1) ⊆ auth_users(r2) ∧ auth_perms(r1) ⊇ auth_perms(r2).
        return authUserChild.containsAll(authUserParent) && permParent.containsAll(permChild);
    }

    public List<RoleType> getRoleParent(RoleType role, List<RoleAnalyseStructure> roleAnalyseStructures) {

        List<RoleType> roleTypes = new ArrayList<>();
        //parent containsAll permission of child and can have some extra or additional permission.
        List<AuthorizationType> rolePermission = new RoleMiningFilter().getAuthorization(role);
        for (RoleAnalyseStructure roleAnalyseStructure : roleAnalyseStructures) {
            RoleType roleObject = roleAnalyseStructure.getRoleType();

            List<AuthorizationType> rolePermissionTemp = new RoleMiningFilter().getAuthorization(roleObject);

            if (rolePermissionTemp.containsAll(rolePermission)) {
                if (!role.getName().equals(roleObject.getName())) {
                    roleTypes.add(roleObject);
                }
            }

        }

        return roleTypes;
    }

    public List<RoleType> getRoleChild(RoleType role, List<RoleAnalyseStructure> roleAnalyseStructures) {

        List<RoleType> roleTypeList = new ArrayList<>();
        //parent containsAll permission of child and can have some extra or additional permission.
        List<AuthorizationType> rolePermission = new RoleMiningFilter().getAuthorization(role);
        for (RoleAnalyseStructure roleAnalyseStructure : roleAnalyseStructures) {
            RoleType roleObject = roleAnalyseStructure.getRoleType();
            List<AuthorizationType> rolePermissionTemp = new RoleMiningFilter().getAuthorization(roleObject);

            if (!rolePermissionTemp.containsAll(rolePermission)) {
                if (!role.getName().equals(roleObject.getName())) {
                    roleTypeList.add(roleObject);
                }
            }

        }

        return roleTypeList;
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
                jaccardResultRoles.add(new RoleMiningFilter().getRoleByOid(objectReferenceType.getOid(), pageBase));
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
