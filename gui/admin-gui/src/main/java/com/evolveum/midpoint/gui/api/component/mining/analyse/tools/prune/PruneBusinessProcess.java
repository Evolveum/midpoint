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
import com.evolveum.midpoint.gui.api.component.mining.analyse.structure.prune.*;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class PruneBusinessProcess implements PruneFunctionality {

    PageBase pageBase;

    double sigma;
    double tau;
    double omega;

    int usersCount = 0;
    int rolesCount = 0;
    int processed = 0;
    long timeMining = 0;

    List<HashMap<Integer, CandidateRole>> rolesDegrees = new ArrayList<>();
    HashMap<Integer, List<UrType>> candidateKeyUpStructureMap = new HashMap<>();
    HashMap<Integer, List<RoleType>> candidateKeyAuthorizationsMap = new HashMap<>();

    public PruneBusinessProcess(PageBase pageBase) {
        this.pageBase = pageBase;
        if (getUserRolesList().size() == 0) {
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

        List<List<RoleType>> roles = getRolesImport();
        List<UrType> userRolesMemberList = getUserRolesList();
        // -1 administrator
        usersCount = getUsers().size() - 1;
        rolesCount = roles.size();

        timeMining = 0;
        long startTime = System.nanoTime();
        LOGGER.info("PRUNE ALGORITHM START");
        miningAlgorithm(sigma, tau, omega, usersCount, rolesCount, roles, userRolesMemberList);

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
        generateResult(getResultUrMap());

        LOGGER.info("END -> process()");
    }

    public void miningAlgorithm(double sigma, double tau, double omega, int usersCount, int rolesCount,
            List<List<RoleType>> roles, List<UrType> userRolesList) {
        //importantFill();

        if (roles == null || userRolesList == null) {
            return;
        }

        rolesDegrees = new ArrayList<>();
        HashMap<Integer, CandidateRole> roleDegreePlus = new HashMap<>();

        int degree = 0;
        int key = 1;
        double minSupport = sigma * (degree + 1) + tau + omega;

        LOGGER.info("Processing degree: " + 1 + ".");
        for (int p = 0; p < rolesCount; p++) {
            List<RoleType> childrenRoles = roles.get(p);
            List<UrType> upCandidates = possibleAssign(userRolesList, childrenRoles);
            double support = upCandidates.size() / (double) usersCount;

            if (minSupport >= support) {
                continue;
            }

            roleDegreePlus.put(key, new CandidateRole(degree, key, childrenRoles, new HashSet<>(), new HashMap<>(), support,
                    support, true));
            candidateKeyUpStructureMap.put(key, upCandidates);
            candidateKeyAuthorizationsMap.put(key, childrenRoles);
            key++;
        }

        if (roleDegreePlus.size() == 0) {
            LOGGER.info("END -> there is no suitable candidate role for the given inputs.");
            return;
        } else {
            rolesDegrees.add(roleDegreePlus);
            degree = degree + 1;
        }

        while (degree != roles.size()) {
            minSupport = sigma * (degree + 1) + tau + omega;
            LOGGER.info("Processing degree: " + (degree + 1) + ".");
            HashMap<Integer, CandidateRole> roleDegreeMinus = rolesDegrees.get(degree - 1);
            roleDegreePlus = new HashMap<>();

            List<CandidateRole> mappedValues = new ArrayList<>(roleDegreeMinus.values());

            int mappedValuesSize = mappedValues.size();
            for (int i = 0; i < mappedValuesSize; i++) {
                for (int j = i + 1; j < mappedValuesSize; j++) {
                    List<RoleType> firstSet = mappedValues.get(i).getCandidateRoles();
                    List<RoleType> secondSet = mappedValues.get(j).getCandidateRoles();

                    boolean canJoin = IntStream.range(0, firstSet.size() - 1)
                            .allMatch(k -> firstSet.get(k).equals(secondSet.get(k)));

                    if (canJoin) {
                        List<RoleType> childrenRoles = new ArrayList<>(firstSet);
                        childrenRoles.add(secondSet.get(secondSet.size() - 1));
                        List<UrType> upCandidates = possibleAssign(userRolesList, childrenRoles);
                        double actualSupport = upCandidates.size() / (double) usersCount;

                        if (minSupport >= actualSupport) {
                            continue;
                        }

                        HashMap<Integer, Connection> parentConnections = new HashMap<>();

                        for (CandidateRole parentCandidateRole : mappedValues) {
                            if (childrenRoles.containsAll(parentCandidateRole.getCandidateRoles())) {

                                double confidence = new PruneTools().confidenceConnection(parentCandidateRole.getActualSupport(),
                                        actualSupport);

                                parentConnections.put(parentCandidateRole.getKey(), new Connection(parentCandidateRole.getKey(),
                                        confidence, parentCandidateRole.getDegree()));
                                parentCandidateRole.getChildrenKeys().add(key);

                            }
                        }

                        //while generating new candidate role is support == actual_support
                        CandidateRole candidateChildren = new CandidateRole(degree, key, childrenRoles, new HashSet<>(),
                                parentConnections, actualSupport, actualSupport, true);

                        roleDegreePlus.put(key, candidateChildren);
                        candidateKeyUpStructureMap.put(key, upCandidates);
                        candidateKeyAuthorizationsMap.put(key, childrenRoles);

                        key++;
                    }
                }
            }

            if (roleDegreePlus.size() == 0) {
                LOGGER.info("End at degree: " + (degree + 1) + ".");
                setRolesDegrees(rolesDegrees);
                setCandidateKeyUpStructureMapUR(candidateKeyUpStructureMap);
                return;
            } else {
                //(degree + 1) -> degree -> (degree - 1)
                minSupport = sigma * (degree) + tau + omega;
                rolesDegrees.add(roleDegreePlus);

                //prune steps
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
        setCandidateKeyUpStructureMapUR(candidateKeyUpStructureMap);

        LOGGER.info("End -> the algorithm has reached the last degree." + (degree + 1));
    }

    @Override
    public void userPurification(int usersCount, HashMap<Integer, CandidateRole> roleDegreeMinus) {
        for (Map.Entry<Integer, CandidateRole> connectionEntry : roleDegreeMinus.entrySet()) {
            CandidateRole candidateRole = connectionEntry.getValue();
            Integer parentKey = connectionEntry.getKey();

            Set<Integer> childrenKeys = candidateRole.getChildrenKeys();

            for (Integer key : childrenKeys) {
                List<UrType> parentUp = candidateKeyUpStructureMap.get(parentKey);
                boolean removed = parentUp.removeAll(candidateKeyUpStructureMap.get(key));
                if (removed) {
                    double actualSupport = parentUp.size() / (double) usersCount;
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

            Set<RoleType> parentsRoles = new HashSet<>();
            for (Map.Entry<Integer, Connection> entryDegree : parentKeys.entrySet()) {
                Integer key = entryDegree.getKey();
                int degree = entryDegree.getValue().getDegree();
                parentsRoles.addAll(rolesDegrees.get(degree).get(key).getCandidateRoles());
            }

            List<RoleType> checkIf = new ArrayList<>(parentsRoles);

            return checkIf.containsAll(candidateRole.getCandidateRoles());

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

            switchConnection(candidateForDelete, candidateChildrenKeys, candidateParentsKeys, removableRoleKeys,
                    removeRoleKey, ROLE_DEGREE_PLUS);
        }
    }

    public void switchConnection(CandidateRole candidateForDelete, Set<Integer> candidateChildrenKeys,
            HashMap<Integer, Connection> parentCandidateList, List<Integer> removableRolesKeys, int removeRoleKey, HashMap<Integer,
            CandidateRole> ROLE_DEGREE_PLUS) {

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

                    childrenCandidate.getParentKeys().put(parentCandidate.getKey(), new Connection(keyParent, newConfidence,
                            connection.getDegree()));
                    parentCandidate.getChildrenKeys().add(childrenCandidate.getKey());
                    parentCandidate.getParentKeys().forEach((key, value) -> childrenCandidate.getParentKeys().remove(key));
                }

                parentCandidate.getChildrenKeys().remove(candidateForDelete.getKey());

            }

            childrenCandidate.getParentKeys().remove(candidateForDelete.getKey());

        }

    }

    public boolean canAdd(CandidateRole childrenCandidate, CandidateRole parentCandidate, List<Integer> removableRolesKeys) {
        List<RoleType> parentCandidateRoles = parentCandidate.getCandidateRoles();
        HashMap<Integer, Connection> childrenCandidateParentKeys = childrenCandidate.getParentKeys();

        for (Map.Entry<Integer, Connection> entry : childrenCandidateParentKeys.entrySet()) {
            List<RoleType> candidateRoles = rolesDegrees.get(entry.getValue().getDegree()).get(entry.getKey()).getCandidateRoles();
            if (removableRolesKeys.contains(entry.getKey())) {
                continue;
            }
            if (candidateRoles.containsAll(parentCandidateRoles)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void updateAfterRemoveOperation(List<Integer> removableRoleKeys, HashMap<Integer, CandidateRole> ROLE_DEGREE_MINUS,
            List<HashMap<Integer, CandidateRole>> rolesDegrees, int usersCount) {
        for (int removeRoleKey : removableRoleKeys) {

            List<UrType> transferUsers = candidateKeyUpStructureMap.get(removeRoleKey);

            if (transferUsers == null) {
                continue;
            }

            CandidateRole candidateRole = ROLE_DEGREE_MINUS.get(removeRoleKey);

            HashMap<Integer, Connection> parentKeys = candidateRole.getParentKeys();

            for (Map.Entry<Integer, Connection> entryDegree : parentKeys.entrySet()) {
                Integer parentKey = entryDegree.getKey();
                int parentDegree = entryDegree.getValue().getDegree();

                List<UrType> ups = candidateKeyUpStructureMap.get(parentKey);

                if (ups == null) {
                    candidateKeyUpStructureMap.put(parentKey, transferUsers);
                } else {
                    ups.addAll(transferUsers);
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
                stringBuilder.append("Roles:").append(" \\n");
                for (int s = 0; s < candidateRole.getCandidateRoles().size(); s++) {
                    stringBuilder.append(candidateRole.getCandidateRoles().get(s).getName()).append(" \\n");
                }
                stringBuilder.append("Actual users:").append(" \\n");

                List<UrType> upList = candidateKeyUpStructureMap.get(candidateRole.getKey());
                if (upList != null) {
                    for (UrType urType : upList) {
                        stringBuilder.append(urType.getUserObjectType().getName()).append(" \\n");
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

    private PageBase getPageBase() {
        return pageBase;
    }

    public void generateUa() {
        LOGGER.info(("START -> generateResultStructure()"));
        HashMap<UserType, List<List<RoleType>>> resultUaMap = new HashMap<>();
        for (Map.Entry<Integer, List<UrType>> entry : candidateKeyUpStructureMap.entrySet()) {
            Integer key = entry.getKey();
            List<UrType> candidateRoleUrType = entry.getValue();
            if (candidateRoleUrType.size() == 0) {
                continue;
            }

            List<RoleType> roleObjectList = candidateKeyAuthorizationsMap.get(key);
            if (roleObjectList == null) {
                continue;
            }

            for (UrType urType : candidateRoleUrType) {
                UserType userObject = urType.getUserObjectType();

                List<List<RoleType>> resultUa = resultUaMap.get(userObject);

                if (resultUa == null) {
                    resultUaMap.put(userObject, new ArrayList<>(List.of(roleObjectList)));
                } else {
                    resultUa.add(roleObjectList);
                }
            }
        }

        setResultUrMap(resultUaMap);
        LOGGER.info(("END -> generateResultStructure()"));
    }

    public List<PrismObject<RoleType>> getRoles() {
        List<PrismObject<RoleType>> roleList = getDataStorageRoles();
        if (roleList == null || roleList.size() == 0) {
            roleList = new RoleMiningFilter().filterRoles(getPageBase());
        }
        return roleList;
    }

    //    TODO use this
//    O(U*K + P + R)
    public void importantFill() {
        LOGGER.info("START -> importantFill() process.");
        long startTimeMain = System.nanoTime();

        generateIR();
        generateUR();

        long endTimeMain = System.nanoTime();
        long secondsMain = TimeUnit.NANOSECONDS.toSeconds(endTimeMain - startTimeMain);
        LOGGER.info("END -> importantFill() process. Total time: " + secondsMain + "seconds.");
    }

    public void generateIR() {
        int iterateRA = 0;

        List<List<RoleType>> rolesImport = new ArrayList<>();
        List<PrismObject<RoleType>> roleList = getRoles();

        for (PrismObject<RoleType> roleTypePrismObject : roleList) {
            if (roleTypePrismObject.getName() == null || !roleTypePrismObject.getName().toString().contains("R_")) {
                continue;
            }
            iterateRA++;
            rolesImport.add(List.of(roleTypePrismObject.asObjectable()));
        }
        setRolesImport(rolesImport);
        setIterateRA(iterateRA);
    }

    public void generateUR() {
        int iterateUA = 0;
        int iteratePA = 0;

        List<PrismObject<UserType>> userList = getUsers();
        List<UrType> userRolesList = new ArrayList<>();
        for (PrismObject<UserType> userTypePrismObject : userList) {
            UserType userObject = userTypePrismObject.asObjectable();

            if (userObject.getName().toString().equals("administrator")) {
                userRolesList.add(new UrType(userObject, new ArrayList<>()));
                continue;
            }

            List<RoleType> userRoles = new RoleMiningFilter().getUserRoles(userObject, getPageBase());
            List<AuthorizationType> userAuthorizations = new RoleMiningFilter().getUserAuthorizationsR(userRoles, getPageBase());

            //O(K * N)
            userRolesList.add(new UrType(userObject, userRoles));
            iteratePA = iteratePA + userAuthorizations.size();
            iterateUA = iterateUA + userRoles.size();

        }

        setUserRolesList(userRolesList);

        setIteratePA(iteratePA);
        setIterateUA(iterateUA);
    }

    public List<UpType> generateUP() {
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
        }

        setUserPermissionList(userPermissionList);
        return userPermissionList;
    }

    public List<PrismObject<UserType>> getUsers() {
        List<PrismObject<UserType>> userList = getDataStorageUsers();
        if (userList == null || userList.size() == 0) {
            userList = new RoleMiningFilter().filterUsers(getPageBase());
        }
        return userList;
    }

    public List<UrType> possibleAssign(List<UrType> userRoleMembersList, List<RoleType> candidateRole) {
        return IntStream.range(0, userRoleMembersList.size())
                .filter(i -> userRoleMembersList.get(i).getRoleMembers()
                        .containsAll(candidateRole)).mapToObj(userRoleMembersList::get)
                .collect(Collectors.toList());
    }

    public void generateResult(HashMap<UserType, List<List<RoleType>>> RESULT_UA) {

        processed = 0;
        LOGGER.info(("START -> generateUA()"));
        List<UpType> userPermissionList = generateUP();
        if (userPermissionList == null) {
            return;
        }

        List<ResultData> resultDataList = new ArrayList<>();

        IntStream.range(0, userPermissionList.size()).forEach(i -> {
            UserType userObject = userPermissionList.get(i).getUserObjectType();
            List<List<RoleType>> resultUa = RESULT_UA.get(userObject);
            if (resultUa != null) {
                List<RoleType> userRoles = new RoleMiningFilter().getUserRoles(userObject, pageBase);
                List<AuthorizationType> permission = userPermissionList.get(i).getPermission();

                Set<AuthorizationType> set = new HashSet<>();
                for (List<RoleType> roleTypes : resultUa) {
                    for (RoleType roleType : roleTypes) {
                        set.addAll(roleType.getAuthorization());
                    }
                }
                boolean correlateStatus = set.containsAll(permission);

                Set<AuthorizationType> requiredPermissions = new HashSet<>(permission);
                List<Integer> indices = resultOp(resultUa, requiredPermissions);

                if (correlateStatus) {
                    processed++;
                }

                double reducedCount = resultUa.size() - (resultUa.size() - indices.size());

                double reduceValue = ((userRoles.size() - reducedCount) / (double) userRoles.size()) * 100;

                resultDataList.add(new ResultData(userObject, reduceValue, permission, userRoles,
                        resultUa, correlateStatus, indices));
            }
        });

        setResultData(resultDataList);
        LOGGER.info(("END -> generateUA()"));
    }

    public static List<Integer> resultOp(List<List<RoleType>> lists, Set<AuthorizationType> requiredPermissions) {
        List<Integer> result = new ArrayList<>();

        Set<AuthorizationType> remaining = new HashSet<>(requiredPermissions);
        for (int i = 0; i < lists.size(); i++) {
            Set<AuthorizationType> intersection = new HashSet<>();
            for (int j = 0; j < lists.get(i).size(); j++) {
                intersection.addAll(lists.get(i).get(j).getAuthorization());
            }
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
                    iteratePA = iteratePA + (candidate.getCandidateRoles().size());
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
