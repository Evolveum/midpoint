/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining.analyse.tools.prune;

import static com.evolveum.midpoint.gui.api.component.mining.DataStorage.*;
import static com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions.LOGGER;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.github.openjson.JSONObject;

import com.evolveum.midpoint.gui.api.component.mining.RoleMiningFilter;
import com.evolveum.midpoint.gui.api.component.mining.analyse.structure.prune.CandidateRole;
import com.evolveum.midpoint.gui.api.component.mining.analyse.structure.prune.Connection;
import com.evolveum.midpoint.gui.api.component.mining.analyse.structure.prune.ResultData;
import com.evolveum.midpoint.gui.api.component.mining.analyse.structure.prune.UpType;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class PruneCandidateProcess implements PruneFunctionality {

    PageBase pageBase;

    double sigma;
    double tau;
    double omega;

    int usersCount = 0;
    int permissionCount = 0;
    int processed = 0;

    long timeMining = 0;

    List<HashMap<Integer, CandidateRole>> rolesDegrees = new ArrayList<>();
    HashMap<Integer, List<UpType>> candidateKeyUpStructureMap = new HashMap<>();
    HashMap<Integer, List<AuthorizationType>> candidateKeyAuthorizationsMap = new HashMap<>();

    public PruneCandidateProcess(PageBase pageBase) {
        this.pageBase = pageBase;
        if (getUserPermissionList().size() == 0) {
            importantFill();
        }
    }

    //O(k * n^2) ?
    public void process(double sigma, double tau, double omega) {
        LOGGER.info("START -> process()");
        candidateKeyUpStructureMap = new HashMap<>();
        rolesDegrees = new ArrayList<>();
        candidateKeyAuthorizationsMap = new HashMap<>();

        this.sigma = sigma;
        this.tau = tau;
        this.omega = omega;

        List<List<AuthorizationType>> permission = getPermissionImport();
        List<UpType> userPermissionList = getUserPermissionList();
        // -1 administrator
        usersCount = getUsers().size() - 1;
        permissionCount = permission.size();

        timeMining = 0;
        long startTime = System.nanoTime();
        LOGGER.info("PRUNE ALGORITHM START");
        miningAlgorithm(sigma, tau, omega, usersCount, permissionCount, permission, userPermissionList);

        long endTime = System.nanoTime();
        long seconds = TimeUnit.NANOSECONDS.toSeconds(endTime - startTime);
        timeMining = seconds;
        LOGGER.info("PRUNE ALGORITHM END -> TIME: " + seconds + "seconds.");

        if (rolesDegrees.size() > 0) {
            HashMap<Integer, CandidateRole> integerCandidateRole2HashMap = rolesDegrees.get(0);
            if (integerCandidateRole2HashMap != null && integerCandidateRole2HashMap.size() != 0) {
                for (Map.Entry<Integer, CandidateRole> entry : integerCandidateRole2HashMap.entrySet()) {
                    CandidateRole value = entry.getValue();
                    if (value.getActualSupport() == 0.000) {
                        value.setActive(false);
                    }
                }
            }
        }

        //TODO result
        generateUa();
        generateResult(getResultUaMap());

        LOGGER.info("END -> process()");
    }

    public void miningAlgorithm(double sigma, double tau, double omega, int usersCount, int permissionCount,
            List<List<AuthorizationType>> permission, List<UpType> userPermissionList) {
        //importantFill();

        if (permission == null || userPermissionList == null) {
            return;
        }

        rolesDegrees = new ArrayList<>();
        HashMap<Integer, CandidateRole> roleDegreePlus = new HashMap<>();

        int degree = 0;
        int key = 1;
        double minSupport = sigma * (degree + 1) + tau + omega;

        LOGGER.info("Processing degree: " + 1 + ".");
        for (int p = 0; p < permissionCount; p++) {
            List<AuthorizationType> childrenPermission = permission.get(p);
            List<UpType> upTypeCandidates = possibleAssign(userPermissionList, childrenPermission);
            double support = upTypeCandidates.size() / (double) usersCount;

            if (minSupport >= support) {
                continue;
            }

            roleDegreePlus.put(key, new CandidateRole(degree, key, new HashSet<>(), new HashMap<>(), support, support,
                    true, childrenPermission));
            candidateKeyUpStructureMap.put(key, upTypeCandidates);
            candidateKeyAuthorizationsMap.put(key, childrenPermission);
            key++;
        }

        if (roleDegreePlus.size() == 0) {
            LOGGER.info("END -> there is no suitable candidate role for the given inputs.");
            return;
        } else {
            rolesDegrees.add(roleDegreePlus);
            degree = degree + 1;
        }

        while (degree != permission.size()) {
            minSupport = sigma * (degree + 1) + tau + omega;
            LOGGER.info("Processing degree: " + (degree + 1) + ".");
            HashMap<Integer, CandidateRole> roleDegreeMinus = rolesDegrees.get(degree - 1);
            roleDegreePlus = new HashMap<>();

            List<CandidateRole> mappedValues = new ArrayList<>(roleDegreeMinus.values());

            int mappedValuesSize = mappedValues.size();
            for (int i = 0; i < mappedValuesSize; i++) {
                for (int j = i + 1; j < mappedValuesSize; j++) {
                    List<AuthorizationType> firstSet = mappedValues.get(i).getCandidatePermissions();
                    List<AuthorizationType> secondSet = mappedValues.get(j).getCandidatePermissions();

                    boolean canJoin = IntStream.range(0, firstSet.size() - 1)
                            .allMatch(k -> firstSet.get(k).equals(secondSet.get(k)));

                    if (canJoin) {
                        List<AuthorizationType> childrenPermission = new ArrayList<>(firstSet);
                        childrenPermission.add(secondSet.get(secondSet.size() - 1));
                        List<UpType> upTypeCandidates = possibleAssign(userPermissionList, childrenPermission);
                        double actualSupport = upTypeCandidates.size() / (double) usersCount;

                        if (minSupport >= actualSupport) {
                            continue;
                        }

                        HashMap<Integer, Connection> parentConnections = new HashMap<>();

                        for (CandidateRole parentCandidateRole : mappedValues) {
                            if (childrenPermission.containsAll(parentCandidateRole.getCandidatePermissions())) {

                                double confidence = new PruneTools().confidenceConnection(parentCandidateRole.getActualSupport(),
                                        actualSupport);

                                parentConnections.put(parentCandidateRole.getKey(), new Connection(parentCandidateRole.getKey(),
                                        confidence, parentCandidateRole.getDegree()));
                                parentCandidateRole.getChildrenKeys().add(key);

                            }
                        }

                        //while generating new candidate role is support == actual_support
                        CandidateRole candidateChildren = new CandidateRole(degree, key, new HashSet<>(),
                                parentConnections, actualSupport, actualSupport, true, childrenPermission);

                        roleDegreePlus.put(key, candidateChildren);
                        candidateKeyUpStructureMap.put(key, upTypeCandidates);
                        candidateKeyAuthorizationsMap.put(key, childrenPermission);

                        key++;
                    }
                }
            }

            if (roleDegreePlus.size() == 0) {
                LOGGER.info("End at degree: " + (degree + 1) + ".");
                setRolesDegrees(rolesDegrees);
                setCandidateKeyUpStructureMapUP(candidateKeyUpStructureMap);
                return;
            } else {
                //(degree + 1) -> degree -> (degree - 1)
                minSupport = sigma * (degree) + tau + omega;
                rolesDegrees.add(roleDegreePlus);

                //O(k * n)
                userPurification(usersCount,
                        roleDegreeMinus);

                if (degree >= 2) {

                    //O(n^2)
                    List<Integer> removableRoles = identifyRemovableRoles(minSupport,
                            roleDegreeMinus);
                    //O(n^2) // TODO
                    removeRoles(removableRoles,
                            rolesDegrees.get(degree), roleDegreeMinus);

                    //O(n * m)

                    updateAfterRemoveOperation(removableRoles,
                            roleDegreeMinus, rolesDegrees, usersCount);

                }
                degree = degree + 1;
            }

        }

        setRolesDegrees(rolesDegrees);
        setCandidateKeyUpStructureMapUP(candidateKeyUpStructureMap);

        LOGGER.info("End -> the algorithm has reached the last degree." + (degree + 1));
    }

    @Override
    public void userPurification(int usersCount, HashMap<Integer, CandidateRole> roleDegreeMinus) {
        for (Map.Entry<Integer, CandidateRole> connectionEntry : roleDegreeMinus.entrySet()) {
            CandidateRole candidateRole = connectionEntry.getValue();
            Integer parentKey = connectionEntry.getKey();

            Set<Integer> childrenKeys = candidateRole.getChildrenKeys();

            for (Integer key : childrenKeys) {
                List<UpType> parentUpType = candidateKeyUpStructureMap.get(parentKey);
                boolean removed = parentUpType.removeAll(candidateKeyUpStructureMap.get(key));
                if (removed) {
                    double actualSupport = parentUpType.size() / (double) usersCount;
                    candidateRole.setActualSupport(actualSupport);
                }

            }
        }
    }

    @Override
    public List<Integer> identifyRemovableRoles(double minSupport, HashMap<Integer, CandidateRole> ROLE_DEGREE_MINUS) {
        List<Integer> removableRoleKeys = new ArrayList<>();

        for (Map.Entry<Integer, CandidateRole> entry : ROLE_DEGREE_MINUS.entrySet()) {
            CandidateRole candidateRole = entry.getValue();

            //O(n)
            if (identifyRemovableConditions(candidateRole, minSupport)) {
                removableRoleKeys.add(candidateRole.getKey());
                candidateRole.setActive(false);
            }

        }

        return removableRoleKeys;
    }

    //O(n)
    public boolean identifyRemovableConditions(
            CandidateRole candidateRole, double minSupport) {
        double accSupport = candidateRole.getActualSupport();

        if (accSupport == 0) {
            return true;
        } else if (accSupport <= minSupport) {

            HashMap<Integer, Connection> parentKeys = candidateRole.getParentKeys();

            Set<AuthorizationType> parentsPermissions = new HashSet<>();
            for (Map.Entry<Integer, Connection> entryDegree : parentKeys.entrySet()) {
                Integer key = entryDegree.getKey();
                int degree = entryDegree.getValue().getDegree();
                parentsPermissions.addAll(rolesDegrees.get(degree).get(key).getCandidatePermissions());
            }

            List<AuthorizationType> checkIf = new ArrayList<>(parentsPermissions);

            return checkIf.containsAll(candidateRole.getCandidatePermissions());

        }
        return false;
    }

    @Override
    public void removeRoles(List<Integer> removableRoleKeys, HashMap<Integer,
            CandidateRole> ROLE_DEGREE_PLUS, HashMap<Integer, CandidateRole> ROLE_DEGREE_MINUS) {

        for (int i = 0; i < removableRoleKeys.size(); i++) {
            int removeRoleKey = removableRoleKeys.get(i);

            CandidateRole candidateForDelete = ROLE_DEGREE_MINUS.get(removeRoleKey);
            HashMap<Integer, Connection> candidateParentsKeys = candidateForDelete.getParentKeys();
            Set<Integer> candidateChildrenKeys = candidateForDelete.getChildrenKeys();

            switchConnection(candidateForDelete, candidateChildrenKeys, candidateParentsKeys,
                    removableRoleKeys, removeRoleKey, ROLE_DEGREE_PLUS);
        }
    }

    public void switchConnection(CandidateRole candidateForDelete, Set<Integer> candidateChildrenKeys,
            HashMap<Integer, Connection> parentCandidateList, List<Integer> removableRolesKeys, int removeRoleKey,
            HashMap<Integer, CandidateRole> ROLE_DEGREE_PLUS) {

        for (Integer keyChildren : candidateChildrenKeys) {
            CandidateRole childrenCandidate = ROLE_DEGREE_PLUS.get(keyChildren);

            for (Map.Entry<Integer, Connection> entryDegree : parentCandidateList.entrySet()) {
                Integer keyParent = entryDegree.getKey();
                Connection connection = entryDegree.getValue();
                int degreesParent = entryDegree.getValue().getDegree();
                CandidateRole parentCandidate = rolesDegrees.get(degreesParent).get(keyParent);
                boolean canAdd = canAdd(childrenCandidate, parentCandidate, removableRolesKeys);

                if (canAdd) {

                    Connection connection21 = childrenCandidate.getParentKeys().get(removeRoleKey);
                    Connection connection22 = candidateForDelete.getParentKeys().get(keyParent);
                    double newConfidence = connection21.getConfidence() * connection22.getConfidence();

                    childrenCandidate.getParentKeys().put(parentCandidate.getKey(), new Connection(keyParent,
                            newConfidence, connection.getDegree()));
                    parentCandidate.getChildrenKeys().add(childrenCandidate.getKey());
                    parentCandidate.getParentKeys().forEach((key, value) -> childrenCandidate.getParentKeys().remove(key));
                }

                parentCandidate.getChildrenKeys().remove(candidateForDelete.getKey());

            }

            childrenCandidate.getParentKeys().remove(candidateForDelete.getKey());

        }

    }

    public boolean canAdd(CandidateRole childrenCandidate, CandidateRole parentCandidate, List<Integer> removableRolesKeys) {
        List<AuthorizationType> parentCandidatePermissions = parentCandidate.getCandidatePermissions();
        HashMap<Integer, Connection> childrenCandidateParentKeys = childrenCandidate.getParentKeys();

        for (Map.Entry<Integer, Connection> entry : childrenCandidateParentKeys.entrySet()) {
            List<AuthorizationType> candidatePermissions = rolesDegrees.get(entry.getValue().getDegree())
                    .get(entry.getKey()).getCandidatePermissions();
            if (removableRolesKeys.contains(entry.getKey())) {
                continue;
            }
            if (candidatePermissions.containsAll(parentCandidatePermissions)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void updateAfterRemoveOperation(List<Integer> removableRoleKeys, HashMap<Integer, CandidateRole> ROLE_DEGREE_MINUS,
            List<HashMap<Integer, CandidateRole>> rolesDegrees, int usersCount) {
        for (int removeRoleKey : removableRoleKeys) {

            List<UpType> transferUsers = candidateKeyUpStructureMap.get(removeRoleKey);

            if (transferUsers == null) {
                continue;
            }

            CandidateRole candidateRole = ROLE_DEGREE_MINUS.get(removeRoleKey);

            HashMap<Integer, Connection> parentKeys = candidateRole.getParentKeys();

            for (Map.Entry<Integer, Connection> entryDegree : parentKeys.entrySet()) {
                Integer parentKey = entryDegree.getKey();
                int parentDegree = entryDegree.getValue().getDegree();

                List<UpType> upTypes = candidateKeyUpStructureMap.get(parentKey);

                if (upTypes == null) {
                    candidateKeyUpStructureMap.put(parentKey, transferUsers);
                } else {
                    upTypes.addAll(transferUsers);
                }

                double updatedActualSupport = candidateKeyUpStructureMap.get(parentKey).size() / (double) usersCount;
                rolesDegrees.get(parentDegree).get(parentKey).setActualSupport(updatedActualSupport);
            }

            candidateKeyUpStructureMap.remove(removeRoleKey);
            ROLE_DEGREE_MINUS.remove(removeRoleKey);
        }
    }

    public List<JSONObject> getJsonEdges() {

        if (rolesDegrees == null) {
            return null;
        }

        List<JSONObject> jsonObjectEdges = new ArrayList<>();
        for (int i = 1; i < rolesDegrees.size(); i++) {

            for (Map.Entry<Integer, CandidateRole> entry : rolesDegrees.get(i).entrySet()) {

                CandidateRole value = entry.getValue();

                HashMap<Integer, Connection> parentKeys = value.getParentKeys();

                for (Map.Entry<Integer, Connection> entryDegree : parentKeys.entrySet()) {
                    Integer parentKey = entryDegree.getKey();
                    Connection connection = entryDegree.getValue();

                    String coordinateY = String.valueOf(value.getKey());
                    String coordinateX = String.valueOf(parentKey);

                    double confidence = connection.getConfidence();
                    double roundConfidence = (Math.round(confidence * 100.0) / 100.0);

                    jsonObjectEdges.add(new JSONObject()
                            .put("from", coordinateX)
                            .put("to", coordinateY)
                            .put("label", String.valueOf(roundConfidence)));

                }

            }

        }

        return jsonObjectEdges;
    }

    public List<JSONObject> getJsonIds() {
        List<JSONObject> jsonObjectIds = new ArrayList<>();

        if (rolesDegrees == null) {
            return null;
        }
        for (int i = 0; i < rolesDegrees.size(); i++) {

            for (Map.Entry<Integer, CandidateRole> roleEntry : rolesDegrees.get(i).entrySet()) {
                CandidateRole candidateRole = roleEntry.getValue();
                boolean active = candidateRole.isActive();
                String id = String.valueOf(candidateRole.getKey());

                StringBuilder stringBuilder = new StringBuilder();

                stringBuilder.append("Key: ").append(candidateRole.getKey()).append(" \\n");
                stringBuilder.append("Role permission:").append(" \\n");
                for (int s = 0; s < candidateRole.getCandidatePermissions().size(); s++) {
                    stringBuilder.append(candidateRole.getCandidatePermissions().get(s).getName()).append(" \\n");
                }
                stringBuilder.append("Actual users:").append(" \\n");

                List<UpType> upTypeList = candidateKeyUpStructureMap.get(candidateRole.getKey());
                if (upTypeList != null) {
                    for (UpType upType : upTypeList) {
                        stringBuilder.append(upType.getUserObjectType().getName()).append(" \\n");
                    }
                }

                stringBuilder.append("---------").append(" \\n");

                stringBuilder.append("ac_supp(r): ").append((Math.round(candidateRole.getActualSupport() * 100.0) / 100.0))
                        .append(" \\n");

                stringBuilder.append("---------").append(" \\n");

                if (active) {
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

    //    TODO use this
    //    O(U*K + P + R)
    public void importantFill() {
        LOGGER.info("START -> importantFill() process.");
        long startTimeMain = System.nanoTime();

        generateIP();
        generateUP();

        long endTimeMain = System.nanoTime();
        long secondsMain = TimeUnit.NANOSECONDS.toSeconds(endTimeMain - startTimeMain);
        LOGGER.info("END -> importantFill() process. Total time: " + secondsMain + "seconds.");
    }

    public void generateIP() {
        int iterateRA = 0;

        List<AuthorizationType> permissions = new ArrayList<>();
        List<PrismObject<RoleType>> roleList = getRoles();

        for (PrismObject<RoleType> roleTypePrismObject : roleList) {
            if (!roleTypePrismObject.getName().toString().contains("R_")) {
                continue;
            }
            iterateRA++;
            List<AuthorizationType> roleAuthorizations = roleTypePrismObject.asObjectable().getAuthorization();

            roleAuthorizations.stream().filter(authorizationType -> !permissions
                            .contains(authorizationType))
                    .forEach(permissions::add);
        }

        List<List<AuthorizationType>> permission = new ArrayList<>();
        for (AuthorizationType authorizationType : permissions) {
            if (authorizationType.getName() != null && authorizationType.getName().contains("P_")) {
                permission.add(List.of(authorizationType));
            }
        }

        setPermissionImport(permission);
        setIterateRA(iterateRA);
    }

    public void generateUP() {
        int iterateUA = 0;
        int iteratePA = 0;

        List<PrismObject<UserType>> userList = getUsers();
        List<UpType> userPermissionList = new ArrayList<>();
        for (PrismObject<UserType> userTypePrismObject : userList) {
            UserType userObject = userTypePrismObject.asObjectable();

            if (userObject.getName().toString().equals("administrator")) {
                userPermissionList.add(new UpType(userObject, new ArrayList<>()));
                continue;
            }
            List<String> rolesIds = new RoleMiningFilter().roleObjectIdRefType(userObject);
            //O(K * N)
            List<AuthorizationType> userAuthorizations = new RoleMiningFilter().getUserAuthorizations(rolesIds, getPageBase());
            userPermissionList.add(new UpType(userObject, userAuthorizations));
            iteratePA = iteratePA + userAuthorizations.size();
            iterateUA = iterateUA + rolesIds.size();

        }

        setUserPermissionList(userPermissionList);

        setIteratePA(iteratePA);
        setIterateUA(iterateUA);
    }

    private PageBase getPageBase() {
        return pageBase;
    }

    public void generateUa() {
        LOGGER.info("START -> generateResultStructure()");
        HashMap<UserType, List<List<AuthorizationType>>> resultUaMap = new HashMap<>();
        for (Map.Entry<Integer, List<UpType>> entry : candidateKeyUpStructureMap.entrySet()) {
            Integer key = entry.getKey();
            List<UpType> candidateRole2UpType = entry.getValue();
            if (candidateRole2UpType.size() == 0) {
                continue;
            }

            List<AuthorizationType> authorizationTypes = candidateKeyAuthorizationsMap.get(key);
            if (authorizationTypes == null) {
                continue;
            }

            for (UpType upType : candidateRole2UpType) {
                UserType userObject = upType.getUserObjectType();

                List<List<AuthorizationType>> resultUa = resultUaMap.get(userObject);

                if (resultUa == null) {
                    resultUaMap.put(userObject, new ArrayList<>(List.of(authorizationTypes)));
                } else {
                    resultUa.add(authorizationTypes);
                }
            }
        }

        setResultUaMap(resultUaMap);
        LOGGER.info("END -> generateResultStructure()");
    }

    public List<PrismObject<RoleType>> getRoles() {
        List<PrismObject<RoleType>> roleList = getDataStorageRoles();
        if (roleList == null || roleList.size() == 0) {
            roleList = new RoleMiningFilter().filterRoles(getPageBase());
        }
        return roleList;
    }

    public List<PrismObject<UserType>> getUsers() {
        List<PrismObject<UserType>> userList = getDataStorageUsers();
        if (userList == null || userList.size() == 0) {
            userList = new RoleMiningFilter().filterUsers(getPageBase());
        }
        return userList;
    }

    public List<UpType> possibleAssign(List<UpType> userPermissionList, List<AuthorizationType> candidatePermission) {
        List<UpType> partOfUsers = new ArrayList<>();
        for (UpType upType : userPermissionList) {
            if (upType.getPermission().containsAll(candidatePermission)) {
                partOfUsers.add(upType);
            }
        }
        return partOfUsers;
    }

    public void generateResult(HashMap<UserType, List<List<AuthorizationType>>> RESULT_UA) {

        processed = 0;
        LOGGER.info("START -> generateUA()");
        List<UpType> userPermissionList = getUserPermissionList();
        if (userPermissionList == null) {
            return;
        }

        List<ResultData> resultDataList = new ArrayList<>();

        IntStream.range(0, userPermissionList.size()).forEach(i -> {
            UserType userObject = userPermissionList.get(i).getUserObjectType();
            List<List<AuthorizationType>> resultUa = RESULT_UA.get(userObject);
            if (resultUa != null) {
                List<RoleType> userRoles = new RoleMiningFilter().getUserRoles(userObject, getPageBase());
                List<AuthorizationType> permission = userPermissionList.get(i).getPermission();

                Set<AuthorizationType> set = resultUa.stream().flatMap(Collection::stream).collect(Collectors.toSet());
                boolean correlateStatus = set.containsAll(permission);

                Set<AuthorizationType> requiredPermissions = new HashSet<>(permission);
                List<Integer> indices = resultOp(resultUa, requiredPermissions);

                double reducedCount = resultUa.size() - (resultUa.size() - indices.size());

                double reduceValue = ((userRoles.size() - reducedCount) / (double) userRoles.size()) * 100;

                if (correlateStatus) {
                    processed++;
                }
                resultDataList.add(new ResultData(userObject, reduceValue, permission, userRoles,
                        resultUa, indices, correlateStatus));
            }
        });

        setResultData(resultDataList);
        LOGGER.info(("END -> generateUA()"));
    }

    public static List<Integer> resultOp(List<List<AuthorizationType>> lists, Set<AuthorizationType> requiredPermissions) {
        List<Integer> result = new ArrayList<>();

        Set<AuthorizationType> remaining = new HashSet<>(requiredPermissions);
        for (int i = 0; i < lists.size(); i++) {
            List<AuthorizationType> list = lists.get(i);
            Set<AuthorizationType> intersection = new HashSet<>(list);
            intersection.retainAll(remaining);
            if (!intersection.isEmpty()) {
                result.add(i);
                remaining.removeAll(intersection);
            }
            if (remaining.isEmpty()) {
                break;
            }
        }
        return result;
    }

    public double calculateModelBasicCost() {
        //-1 administrator
        double cost = getIterateUA() + ((sigma * (usersCount)) * getIteratePA()) + ((tau * (usersCount)) * getIterateRA());
        return (Math.round(cost * 100.0) / 100.0);
    }

    public double calculateNewModelCost() {
        int iterateUA = 0;
        int iterateRA = 0;
        int iteratePA = 0;

        for (HashMap<Integer, CandidateRole> roleDegree : rolesDegrees) {

            for (Map.Entry<Integer, CandidateRole> entry : roleDegree.entrySet()) {
                CandidateRole candidate = entry.getValue();
                if (candidate.isActive()) {
                    Integer key = entry.getKey();
                    iterateRA++;
                    iterateUA = iterateUA + candidateKeyUpStructureMap.get(key).size();
                    iteratePA = iteratePA + (candidate.getCandidatePermissions().size());
                }
            }

        }

        double cost = iterateUA + ((sigma * (usersCount)) * iteratePA) + ((tau * (usersCount)) * iterateRA);

        return (Math.round(cost * 100.0) / 100.0);
    }

    public double processedValue() {
        //administrator -1
        double value = (processed / (double) (usersCount)) * 100;
        return (Math.round(value * 100.0) / 100.0);
    }

    public long getTimeMining() {
        return timeMining;
    }

}
