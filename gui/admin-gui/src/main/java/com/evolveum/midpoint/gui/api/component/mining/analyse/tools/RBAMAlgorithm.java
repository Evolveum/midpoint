/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining.analyse.tools;

import com.evolveum.midpoint.gui.api.component.mining.analyse.structure.rbam.*;
import com.evolveum.midpoint.gui.api.component.mining.structure.UPStructure;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;

import com.github.openjson.JSONObject;


import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RBAMAlgorithm {

    List<RK> rolesItemSet;
    List<HK> hierarchyItemSet;

    List<UPStructure> upStructures;
    List<AuthorizationType> permissions;
    int userCounts;

    double sigmaWeight;
    double tauWeight;
    double omegaWeight;

    List<UA> uaList = new ArrayList<>();
    double updatedModelCost = 0;
    double basicModelCost = 0;

    public RBAMAlgorithm(List<UPStructure> upStructures, List<AuthorizationType> permissions,
            int userCounts, double sigmaWeight, double tauWeight, double omegaWeight) {
        this.upStructures = upStructures;
        this.permissions = permissions;
        this.userCounts = userCounts;
        this.sigmaWeight = sigmaWeight;
        this.tauWeight = tauWeight;
        this.omegaWeight = omegaWeight;
    }

    public double calculateModelBasic(List<UPStructure> upStructures, double sigmaWeight, double tauWeight) {
        //TODO just temporary
        int roleCount;
        int permissionCount = 0;
        int userCount = 0;

        List<String> roles = new ArrayList<>();
        for (int i = 0; i < upStructures.size(); i++) {
            UPStructure upStructure = upStructures.get(i);
            if (!upStructure.getUserObject().getName().toString().equals("administrator")) {
                for (int j = 0; j < upStructure.getAssignRolesObjectIds().size(); j++) {
                    if (!roles.contains(String.valueOf(upStructure.getAssignRolesObjectIds()))) {
                        roles.add(String.valueOf(upStructure.getAssignRolesObjectIds()));
                    }
                }
                permissionCount = permissionCount + upStructure.getAssignPermission().size();

                userCount = userCount + upStructure.getAssignRolesObjectIds().size();
            }
        }

        roleCount = roles.size();

        return userCount + ((sigmaWeight * userCounts) * permissionCount) + ((tauWeight * userCounts) * roleCount);
    }

    public double calculateModelCost(List<RK> RkItemSet, double sigmaWeight, double tauWeight) {
        int roleCount = 0;
        int permissionCount = 0;
        int userCount = 0;

        for (int i = 0; i < RkItemSet.size(); i++) {
            RK rk = RkItemSet.get(i);
            for (int j = 0; j < rk.getCandidatesGroup().size(); j++) {
                CandidateRole candidateRole = rk.getCandidatesGroup().get(j);

                if (candidateRole.isActive()) {
                    permissionCount = permissionCount + candidateRole.getRolePermission().size();
                    roleCount++;
                    userCount = userCount + candidateRole.getUsers().size();
                }
            }
        }

        return userCount + ((sigmaWeight * userCounts) * permissionCount) + ((tauWeight * userCounts) * roleCount);
    }

    public void preprocess() {
        List<List<AuthorizationType>> initListOfPerms = new ArrayList<>();

        IntStream.range(0, permissions.size()).forEach(i -> {
            List<AuthorizationType> temp = new ArrayList<>();
            temp.add(permissions.get(i));
            initListOfPerms.add(temp);
        });

        rolesItemSet = generateRK(initListOfPerms, upStructures, userCounts, sigmaWeight, tauWeight, omegaWeight);
        if (rolesItemSet == null) {
            return;
        }
        hierarchyItemSet = generateHK(rolesItemSet);
        List<UA> uaList = fillUA(rolesItemSet, upStructures);
        fillRKUsers(uaList);
        setUaList(pruneProcess(uaList));

        updatedModelCost = calculateModelCost(rolesItemSet, sigmaWeight, tauWeight);
        basicModelCost = calculateModelBasic(upStructures, sigmaWeight, tauWeight);
    }

    public List<UA> pruneProcess(List<UA> uaList) {

        int degree = rolesItemSet.size();

        for (int k = 0; k < degree - 1; k++) {
            userPurification(uaList, k);

            if (k > 0) {
                List<Integer> removableRoles = identifyRemovableRoles(k, uaList);
                removeRemovableRole(removableRoles, uaList, k);
            }
            fillRKUsers(uaList);
        }

        //TODO can be deleted. It's just for visualization effect
        if (rolesItemSet.get(0) != null) {
            int bound = rolesItemSet.get(0).getCandidatesGroup().size();
            for (int i = 0; i < bound; i++) {
                double accSupport = rolesItemSet.get(0).getCandidatesGroup().get(i).getActualSupport();
                if (accSupport == 0) {
                    rolesItemSet.get(0).getCandidatesGroup().get(i).setActive(false);
                }
            }
        }

        identifyCorrelateStatus(uaList);

        System.out.println("Procedure finish");

        return uaList;
    }

    public void identifyCorrelateStatus(List<UA> uaList) {
        for (int i = 0; i < uaList.size(); i++) {
            List<AuthorizationType> userBasicPermission = uaList.get(i).getUserUP().getAssignPermission();
            Set<Integer> keys = uaList.get(i).getCandidateRoleSet().keySet();
            List<AuthorizationType> union = new ArrayList<>();
            for (Integer key : keys) {
                List<AuthorizationType> authorizationTypes = uaList.get(i).getCandidateRoleSet().get(key);

                for (int j = 0; j < authorizationTypes.size(); j++) {
                    if (!union.contains(authorizationTypes.get(j))) {
                        union.add(authorizationTypes.get(j));
                    }
                }

            }
            boolean processCheck = true;
            if (union.containsAll(userBasicPermission)) {
                for (int j = 0; j < union.size(); j++) {
                    AuthorizationType unionPerm = union.get(j);

                    if (!userBasicPermission.contains(unionPerm)) {
                        processCheck = false;
                        break;
                    }
                }
            } else {
                processCheck = false;
            }

            uaList.get(i).setCorrelateStatus(processCheck);
        }
    }

    public void userPurification(List<UA> uaList, int kMinus) {
        int k = kMinus + 1;
        RK rkMinus = rolesItemSet.get(kMinus);
        HK hkK = hierarchyItemSet.get(k);

        for (int i = 0; i < rkMinus.getCandidatesGroup().size(); i++) {
            int preparingRoleKey = rkMinus.getCandidatesGroup().get(i).getKey();

            for (int j = 0; j < hkK.getConnections().size(); j++) {
                Connection hkKConnection = hkK.getConnections().get(j);
                int hParentHk = hkKConnection.gethParent().getKey();
                int hChildrenHk = hkKConnection.gethChildren().getKey();

                if (preparingRoleKey == hParentHk) {
                    purificationRemoveStep(uaList, preparingRoleKey, hChildrenHk);
                }
            }

            List<UA> assignUsers = getAssignUsers(uaList, preparingRoleKey);

            double actualSupport = assignUsers.size() / (double) userCounts;
            rolesItemSet.get(kMinus).getCandidatesGroup().get(i).setUsers(assignUsers);
            rolesItemSet.get(kMinus).getCandidatesGroup().get(i).setActualSupport(actualSupport);

        }

    }

    public void purificationRemoveStep(List<UA> uaList, int keyParent, int keyChildren) {
        uaList.stream().map(UA::getCandidateRoleSet)
                .filter(candidateRoleSet -> candidateRoleSet.get(keyParent) != null && candidateRoleSet.get(keyChildren) != null)
                .forEach(candidateRoleSet -> candidateRoleSet.remove(keyParent));
    }

    public List<Integer> identifyRemovableRoles(int k, List<UA> basicUA) {

        List<CandidateRole> rkMinus = rolesItemSet.get(k).getCandidatesGroup();
        HK hk = hierarchyItemSet.get(k);

        List<Integer> removableRolesKeys = new ArrayList<>();
        int bound = rkMinus.size();
        for (int i = 0; i < bound; i++) {
            CandidateRole candidateRole = rkMinus.get(i);
            double minSupport = sigmaWeight * (k - 1) + tauWeight + omegaWeight;
            double accSupport = candidateRole.getActualSupport();
            double support = candidateRole.getSupport();
            int key = candidateRole.getKey();

            if (accSupport == 0) {
                rolesItemSet.get(k).getCandidatesGroup().get(i).setActive(false);
                removableRolesKeys.add(key);
            } else if (accSupport <= minSupport) {

                if (markRemovableCondition(candidateRole.getKey(), hk, basicUA, support, userCounts)) {
                    rolesItemSet.get(k).getCandidatesGroup().get(i).setActive(false);
                    removableRolesKeys.add(key);
                }

            } else {
                rolesItemSet.get(k).getCandidatesGroup().get(i).setActive(true);
            }

        }
        return removableRolesKeys;
    }

    public boolean markRemovableCondition(int hChildCandidateKey, HK hkMinus, List<UA> basicUA, double support,
            int usersCount) {

        List<UA> assignSupportUsers = getAssignSupportUsers(basicUA, hChildCandidateKey);

        List<UA> union = new ArrayList<>();
        List<Connection> hkMinusConnections = hkMinus.getConnections();
        for (int i = 0; i < hkMinusConnections.size(); i++) {
            Connection hkMinusConnection = hkMinusConnections.get(i);
            int hMinusChildKey = hkMinusConnection.gethChildren().getKey();

            if (hChildCandidateKey == hMinusChildKey) {
                int hMinusParentKey = hkMinusConnection.gethParent().getKey();

                List<UA> tempAssignUsers = getAssignSupportUsers(basicUA, hMinusParentKey);

                for (int j = 0; j < tempAssignUsers.size(); j++) {
                    if (!union.contains(tempAssignUsers.get(j))) {
                        union.add(tempAssignUsers.get(j));
                    }
                }

            }
        }

        int iterator = (int) IntStream.range(0, assignSupportUsers.size()).filter(i -> union.contains(assignSupportUsers.get(i))).count();

        double tempV = support * usersCount;
        int comparator = (int) Math.round(tempV);

        return iterator == comparator;
    }

    public void removeRemovableRole(List<Integer> removableRolesKeys, List<UA> uaList, int k) {

        HK hk = hierarchyItemSet.get(k + 1);
        HK hkMinus = hierarchyItemSet.get(k);

        List<Connection> hcConnectionHK = hk.getConnections();
        List<Connection> hpConnectionHMinus = hkMinus.getConnections();

        List<Integer> removed = new ArrayList<>();
        List<Connection> connectionListK = new ArrayList<>(hierarchyItemSet.get(k + 1).getConnections());
        List<Connection> connectionListKMinus = new ArrayList<>(hierarchyItemSet.get(k).getConnections());

        for (int r = 0; r < removableRolesKeys.size(); r++) {

            int removableKey = removableRolesKeys.get(r);

            // need parent key; parent permission; connection
            List<Connection> hcConnectionsReplace = new ArrayList<>();
            for (int c = 0; c < hcConnectionHK.size(); c++) {
                Connection hcConnection = hcConnectionHK.get(c);
                int hcParentKey = hcConnection.gethParent().getKey();
                if (hcParentKey == removableKey) {
                    hcConnectionsReplace.add(hcConnection);
                }
            }

            for (int p = 0; p < hpConnectionHMinus.size(); p++) {
                Connection hpConnection = hpConnectionHMinus.get(p);
                int hpChildrenKey = hpConnection.gethChildren().getKey();
                if (hpChildrenKey == removableKey) {

                    List<CandidateRole> parentCandidateRoleList = new ArrayList<>();
                    for (int i = 0; i < hcConnectionsReplace.size(); i++) {
                        Connection hcConnection = hcConnectionsReplace.get(i);

                        List<AuthorizationType> hpParentPermission = hpConnection.gethParent().getRolePermission();
                        int hpParentKey = hpConnection.gethParent().getKey();
                        int hpChildKey = hpConnection.gethChildren().getKey();
                        boolean allowed = allowRemove(hk, removableRolesKeys, hpParentPermission,
                                hpParentKey, hpChildKey, hcConnection.gethChildren().getKey());

                        if (allowed) {

                            removed.add(removableKey);
                            CandidateRole hpParentRole = hpConnection.gethParent();
                            CandidateRole hcChildrenRole = hcConnection.gethChildren();
                            double confidence = hpConnection.gethConfidence() * hcConnection.gethConfidence();
                            connectionListK.add(new Connection(hpParentRole, hcChildrenRole, confidence));
                            parentCandidateRoleList.add(hpParentRole);
                        }
                    }

                    switchRemovedUsers(uaList, parentCandidateRoleList, removableKey);

                }
            }

            if (removed.contains(removableKey)) {
                for (int i = 0; i < uaList.size(); i++) {
                    UA ua = uaList.get(i);
                    ua.getCandidateRoleSet().remove(removableKey);
                }
            }

            List<Connection> resultConnectionK = IntStream.range(0, connectionListK.size())
                    .filter(i -> !removed.contains(connectionListK.get(i).gethParent().getKey()))
                    .mapToObj(connectionListK::get).collect(Collectors.toList());

            hierarchyItemSet.get(k + 1).setConnections(resultConnectionK);

            List<Connection> resultConnectionKMinus = IntStream.range(0, connectionListKMinus.size())
                    .filter(i -> !removed.contains(connectionListKMinus.get(i).gethChildren().getKey()))
                    .mapToObj(connectionListKMinus::get).collect(Collectors.toList());

            hierarchyItemSet.get(k).setConnections(resultConnectionKMinus);

            IntStream.range(0, rolesItemSet.get(k).getCandidatesGroup().size())
                    .filter(f -> removed.contains(rolesItemSet.get(k).getCandidatesGroup().get(f).getKey()))
                    .forEach(f -> rolesItemSet.get(k).getCandidatesGroup().get(f).setActive(false));

        }

    }

    public void switchRemovedUsers(List<UA> uaList, List<CandidateRole> assignToRole, int removableKey) {

        List<UA> assignUserForSwitch = getAssignUsers(uaList, removableKey);

        if (assignUserForSwitch.isEmpty() || assignToRole.isEmpty()) {
            return;
        }

        for (int i = 0; i < uaList.size(); i++) {
            UA ua = uaList.get(i);
            if (assignUserForSwitch.contains(ua)) {
                for (int j = 0; j < assignToRole.size(); j++) {
                    ua.getCandidateRoleSet().put(assignToRole.get(j).getKey(), assignToRole.get(j).getRolePermission());
                }
            }
        }

    }

    public boolean allowRemove(HK hk, List<Integer> removableRolesKeys, List<AuthorizationType> parentPermsHP,
            int hpParentKey, int hpChildrenKey, int hcChildKey) {

        int bound = hk.getConnections().size();
        for (int i = 0; i < bound; i++) {
            int hkParentKey = hk.getConnections().get(i).gethParent().getKey();
            int hkChildrenKey = hk.getConnections().get(i).gethChildren().getKey();

            if ((hkParentKey != hpParentKey) && (hkChildrenKey != hpChildrenKey)) {

                if (hkChildrenKey == hcChildKey) {
                    if (!removableRolesKeys.contains(hkParentKey)) {
                        List<AuthorizationType> parentPermsC = hk.getConnections().get(i).gethParent().getRolePermission();

                        if (parentPermsC.containsAll(parentPermsHP)) {
                            return false;
                        }
                    }
                }

            }

        }

        return true;
    }

    public List<UA> fillUA(List<RK> rkStructure, List<UPStructure> upStructures) {
        List<UA> uaList = new ArrayList<>();

        for (int u = 0; u < upStructures.size(); u++) {
            UPStructure upStructure = upStructures.get(u);
            List<AuthorizationType> userObjectPermission = upStructures.get(u).getAssignPermission();
            HashMap<Integer, List<AuthorizationType>> candidateRoleSet = new HashMap<>();
            for (int i = 0; i < rkStructure.size(); i++) {
                RK rk = rkStructure.get(i);
                for (int j = 0; j < rk.getCandidatesGroup().size(); j++) {
                    CandidateRole candidateRole = rk.getCandidatesGroup().get(j);
                    List<AuthorizationType> candidateRolePermission = candidateRole.getRolePermission();

                    if (userObjectPermission.containsAll(candidateRolePermission)) {
                        candidateRoleSet.put(candidateRole.getKey(), candidateRolePermission);
                    }
                }
            }
            uaList.add(new UA(upStructure, candidateRoleSet, new HashMap<>(candidateRoleSet), true));
        }
        return uaList;
    }

    public List<UA> getAssignUsers(List<UA> uaList, int roleKey) {

        List<UA> assignUsers = new ArrayList<>();
        for (int i = 0; i < uaList.size(); i++) {
            UA ua = uaList.get(i);
            HashMap<Integer, List<AuthorizationType>> uaRoleList = ua.getCandidateRoleSet();

            if (uaRoleList.get(roleKey) != null) {
                assignUsers.add(ua);
            }

        }
        return assignUsers;
    }

    public List<UA> getAssignSupportUsers(List<UA> uaList, int roleKey) {

        List<UA> assignUsers = new ArrayList<>();
        for (int i = 0; i < uaList.size(); i++) {
            UA ua = uaList.get(i);
            HashMap<Integer, List<AuthorizationType>> uaRoleList = ua.getSupportRole();

            if (uaRoleList.get(roleKey) != null) {
                assignUsers.add(ua);
            }

        }
        return assignUsers;
    }

    public void fillRKUsers(List<UA> uaList) {

        for (int i = 0; i < rolesItemSet.size(); i++) {
            List<CandidateRole> candidatesGroup = rolesItemSet.get(i).getCandidatesGroup();

            for (int j = 0; j < candidatesGroup.size(); j++) {
                CandidateRole candidateRole = candidatesGroup.get(j);
                List<UA> assignUsers = getAssignUsers(uaList, candidateRole.getKey());
                rolesItemSet.get(i).getCandidatesGroup().get(j).setUsers(assignUsers);
                double actualSupport = assignUsers.size() / (double) userCounts;
                rolesItemSet.get(i).getCandidatesGroup().get(j).setActualSupport(actualSupport);
            }
        }
    }

    public double confidenceConnection(double supportParent, double supportChild) {

        double confidenceR2R1 = supportParent / supportChild;
        return (Math.round(confidenceR2R1 * 100.0) / 100.0);
    }

    public List<UPStructure> findUser(List<UPStructure> userRolesAssignments, List<AuthorizationType> generatedList) {
        List<UPStructure> partOfUsers = new ArrayList<>();
        for (int i = 0; i < userRolesAssignments.size(); i++) {
            if (userRolesAssignments.get(i).getAssignPermission().containsAll(generatedList)) {
                partOfUsers.add(userRolesAssignments.get(i));
            }
        }
        return partOfUsers;
    }

    public List<RK> generateRK(List<List<AuthorizationType>> initListOfPerms, List<UPStructure> upStructures,
            int userCounts, double o, double t, double vC) {

        List<RK> rkList = new ArrayList<>();
        int n = initListOfPerms.size();
        int keyIterator = 0;
        boolean sufficientSupport;
        if (n < 2) {
            return null;
        }
        double minSupport;
        double support;
        double accSupport;

        List<CandidateRole> candidateRoleStructures;

        int degree = 1;

        List<AuthorizationType> candidate;

        candidateRoleStructures = new ArrayList<>();

        for (int i = 0; i < initListOfPerms.size(); i++) {
            keyIterator++;
            candidate = initListOfPerms.get(i);
            minSupport = o * degree + t + vC;
            List<UPStructure> users = findUser(upStructures, candidate);

            support = users.size() / (double) userCounts;
            accSupport = support;
            sufficientSupport = !(minSupport >= support);

            List<UA> ua = users.stream()
                    .map(user -> new UA(user, null, null, true))
                    .collect(Collectors.toList());

            if (sufficientSupport) {
                candidateRoleStructures.add(new CandidateRole(
                        candidate, support, accSupport, degree, keyIterator, true, true, ua, users));
            }
        }

        if (candidateRoleStructures.size() > 0) {
            rkList.add(new RK(candidateRoleStructures, degree));
            n--;
        } else {
            return null;
        }

        int iterator = 0;
        while (n != 0) {

            RK rkMinus = rkList.get(iterator);
            List<CandidateRole> candidateGroupsMinusK = rkMinus.getCandidatesGroup();
            iterator++;
            degree++;
            minSupport = o * (degree) + t + vC;
            candidateRoleStructures = new ArrayList<>();

            for (int i = 0; i < candidateGroupsMinusK.size(); i++) {
                for (int j = i + 1; j < candidateGroupsMinusK.size(); j++) {
                    List<AuthorizationType> set1 = candidateGroupsMinusK.get(i).getRolePermission();
                    List<AuthorizationType> set2 = candidateGroupsMinusK.get(j).getRolePermission();

                    boolean equal = true;
                    for (int k = 0; k < set1.size() - 1; k++) {
                        if (!set1.get(k).equals(set2.get(k))) {
                            equal = false;
                            break;
                        }
                    }

                    if (equal) {
                        keyIterator++;
                        candidate = new ArrayList<>(set1);
                        candidate.add(set2.get(set2.size() - 1));
                        List<UPStructure> users = findUser(upStructures, candidate);
                        support = users.size() / (double) userCounts;
                        accSupport = support;

                        List<UA> ua = users.stream()
                                .map(user -> new UA(user, null, null, true))
                                .collect(Collectors.toList());

                        sufficientSupport = !(minSupport >= support);
                        if (sufficientSupport) {
                            candidateRoleStructures.add(new CandidateRole(
                                    candidate, support, accSupport, degree, keyIterator, true, true, ua, users));
                        }
                    }
                }
            }

            if (candidateRoleStructures.size() > 0) {

                rkList.add(new RK(candidateRoleStructures, degree));
            } else {
                rkList.sort(Comparator.comparing(RK::getDegree));
                return rkList;
            }
            n--;
        }

        rkList.sort(Comparator.comparing(RK::getDegree));

        return rkList;
    }

    public List<HK> generateHK(List<RK> rkList) {

        if (rkList == null) {
            return null;
        }

        List<HK> hkList = new ArrayList<>();

        hkList.add(new HK(null, 1));

        int level = 2;
        for (int k = 0; k < rkList.size() - 1; k++) {

            List<Connection> connections = new ArrayList<>();

            RK rkStructureParent = rkList.get(k);
            RK rkStructureChildren = rkList.get(k + 1);

            for (int c = 0; c < rkStructureChildren.getCandidatesGroup().size(); c++) {
                CandidateRole cCandidateRole = rkStructureChildren.getCandidatesGroup().get(c);
                List<AuthorizationType> cPermission = cCandidateRole.getRolePermission();

                for (int p = 0; p < rkStructureParent.getCandidatesGroup().size(); p++) {
                    CandidateRole pCandidateRole = rkStructureParent.getCandidatesGroup().get(p);
                    List<AuthorizationType> pPermission = pCandidateRole.getRolePermission();

                    if (cPermission.containsAll(pPermission)) {
                        double supportP = pCandidateRole.getSupport();
                        double supportC = cCandidateRole.getSupport();
                        double confidence = confidenceConnection(supportC, supportP);
                        connections.add(new Connection(pCandidateRole, cCandidateRole, confidence));

                    }

                }

            }

            hkList.add(new HK(connections, level));
            level++;
        }

        return hkList;

    }

    public List<JSONObject> getJsonEdges() {

        if (hierarchyItemSet == null) {
            return null;
        }

        List<JSONObject> jsonObjectEdges = new ArrayList<>();
        for (int i = 1; i < hierarchyItemSet.size(); i++) {

            for (int j = 0; j < hierarchyItemSet.get(i).getConnections().size(); j++) {
                String coordinateY = String.valueOf(hierarchyItemSet.get(i).getConnections().get(j).gethChildren().getKey());
                String coordinateX = String.valueOf(hierarchyItemSet.get(i).getConnections().get(j).gethParent().getKey());

                double confidence = hierarchyItemSet.get(i).getConnections().get(j).gethConfidence();
                boolean active = hierarchyItemSet.get(i).getConnections().get(j).gethChildren().isActive();
                double roundConfidence = (Math.round(confidence * 100.0) / 100.0);
                if (active) {
                    jsonObjectEdges.add(new JSONObject()
                            .put("from", coordinateX)
                            .put("to", coordinateY)
                            .put("label", String.valueOf(roundConfidence)));

                } else {
                    jsonObjectEdges.add(new JSONObject()
                            .put("from", coordinateX)
                            .put("to", coordinateY)
                            .put("label", String.valueOf(roundConfidence))
                            .put("color", "#d41525"));
                }

            }
        }

        return jsonObjectEdges;
    }

    public List<JSONObject> getJsonIds() {
        List<JSONObject> jsonObjectIds = new ArrayList<>();

        if (rolesItemSet == null) {
            return null;
        }
        for (int i = 0; i < rolesItemSet.size(); i++) {

            for (int j = 0; j < rolesItemSet.get(i).getCandidatesGroup().size(); j++) {

                CandidateRole candidateRole = rolesItemSet.get(i).getCandidatesGroup().get(j);

                boolean active = candidateRole.isActive();

                String id = String.valueOf(candidateRole.getKey());

                StringBuilder stringBuilder = new StringBuilder();

                stringBuilder.append("Key: ").append(candidateRole.getKey()).append(" \\n");
                stringBuilder.append("Role permission:").append(" \\n");
                for (int s = 0; s < candidateRole.getRolePermission().size(); s++) {
                    stringBuilder.append(candidateRole.getRolePermission().get(s).getName()).append(" \\n");
                }
                stringBuilder.append("Actual users:").append(" \\n");

                if (candidateRole.getUsers() != null) {
                    for (int s = 0; s < candidateRole.getUsers().size(); s++) {
                        stringBuilder.append(candidateRole.getUsers().get(s).getUserUP().getUserObject().getName()).append(" \\n");
                    }
                }

                stringBuilder.append("Support users:").append(" \\n");

                for (int s = 0; s < candidateRole.getSupportUsers().size(); s++) {
                    stringBuilder.append(candidateRole.getSupportUsers().get(s).getUserObject().getName()).append(" \\n");
                }

                stringBuilder.append("---------").append(" \\n");

                stringBuilder.append("support(r): ").append((Math.round(candidateRole.getSupport() * 100.0) / 100.0)).append(" \\n");
                stringBuilder.append("ac_supp(r): ").append((Math.round(candidateRole.getActualSupport() * 100.0) / 100.0)).append(" \\n");

                stringBuilder.append("---------").append(" \\n");

                if (!candidateRole.isSufficientSupport()) {
                    jsonObjectIds.add(new JSONObject()
                            .put("id", id)
                            .put("label", stringBuilder)
                            .put("level", i)
                            .put("shape", "box")
                            .put("color", new JSONObject()
                                    .put("background", "#ffffff")
                                    .put("border", "#f2990a")));
                } else if (active) {
                    jsonObjectIds.add(new JSONObject()
                            .put("id", id)
                            .put("label", stringBuilder)
                            .put("level", i)
                            .put("shape", "box"));
                } else {
                    jsonObjectIds.add(new JSONObject()
                            .put("id", id)
                            .put("label", stringBuilder)
                            .put("level", i)
                            .put("shape", "box")
                            .put("color", new JSONObject()
                                    .put("background", "#ffffff")
                                    .put("border", "#d41525")));
                }

            }

        }
        return jsonObjectIds;
    }

    public List<RK> getRolesItemSet() {
        return rolesItemSet;
    }

    public void setRolesItemSet(List<RK> rolesItemSet) {
        this.rolesItemSet = rolesItemSet;
    }

    public List<UPStructure> getUpStructures() {
        return upStructures;
    }

    public void setUpStructures(List<UPStructure> upStructures) {
        this.upStructures = upStructures;
    }

    public List<UA> getUaList() {
        return uaList;
    }

    public void setUaList(List<UA> uaList) {
        this.uaList = uaList;
    }

    public double getUpdatedModelCost() {
        return updatedModelCost;
    }

    public void setUpdatedModelCost(double updatedModelCost) {
        this.updatedModelCost = updatedModelCost;
    }

    public double getBasicModelCost() {
        return basicModelCost;
    }

    public void setBasicModelCost(double basicModelCost) {
        this.basicModelCost = basicModelCost;
    }

}
