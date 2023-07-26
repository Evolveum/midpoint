/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.*;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.RoleUtils.jacquardSimilarity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessMode;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.utils.JaccardSorter;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisCluster;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class MiningOperationChunk implements Serializable {
    List<MiningUserTypeChunk> miningUserTypeChunks = new ArrayList<>();
    List<MiningRoleTypeChunk> miningRoleTypeChunks = new ArrayList<>();
    String state = "START";
    OperationResult result = new OperationResult("PrepareData");

    public MiningOperationChunk() {
    }

    public MiningOperationChunk(@NotNull RoleAnalysisCluster clusterType,
            PageBase pageBase, RoleAnalysisProcessMode mode, OperationResult operationResult, boolean chunk, boolean full) {

        resetState();
        resetList();

        resolveAndExecute(clusterType, pageBase, mode, operationResult, chunk, full);
    }

    public void run(RoleAnalysisCluster clusterType,
            PageBase pageBase, RoleAnalysisProcessMode mode, OperationResult operationResult, boolean chunk, boolean full) {

        resetState();
        resetList();

        resolveAndExecute(clusterType, pageBase, mode, operationResult, chunk, full);
    }

    private void getMiningClusterStructureFull(@NotNull RoleAnalysisCluster cluster,
            PageBase pageBase, RoleAnalysisProcessMode mode, OperationResult operationResult) {

        ListMultimap<String, String> roleChunk = ArrayListMultimap.create();

        if (mode.equals(RoleAnalysisProcessMode.USER)) {
            List<String> members = cluster.getElements();
            int usersCount = members.size();
            for (int i = 0; i < members.size(); i++) {
                String membersOid = members.get(i);
                PrismObject<UserType> user = getUserTypeObject(pageBase, membersOid, operationResult);

                if (user == null) {
                    continue;
                }

                List<String> rolesOid = getRolesOid(user.asObjectable());
                int size = rolesOid.size();
                double frequency = Math.min(size / (double) usersCount, 1);
                String chunkName = "Unknown User";
                if (user.getName() != null) {
                    chunkName = user.getName().toString();
                }
                miningUserTypeChunks.add(new MiningUserTypeChunk(Collections.singletonList(membersOid), rolesOid, chunkName,
                        frequency, Status.NEUTRAL));

                for (String roleId : rolesOid) {
                    roleChunk.putAll(roleId, Collections.singletonList(user.getOid()));
                }

                state = i + "/" + usersCount + " ((1 & 2)/4 mapping elements)";
                System.out.println(state);
            }

            int roleChunkSize = roleChunk.size();
            int counter = 0;
            for (String key : roleChunk.keySet()) {
                List<String> strings = roleChunk.get(key);
                double frequency = Math.min(strings.size() / (double) usersCount, 1);

                PrismObject<RoleType> role = getRoleTypeObject(pageBase, key, operationResult);
                String chunkName = "Unknown Role";
                if (role != null) {
                    chunkName = role.getName().toString();
                }

                miningRoleTypeChunks.add(new MiningRoleTypeChunk(Collections.singletonList(key), strings, chunkName, frequency, Status.NEUTRAL));
                state = counter + "/" + roleChunkSize + " (3/4 mapping points)";
                System.out.println(state);
                counter++;

            }

        } else if (mode.equals(RoleAnalysisProcessMode.ROLE)) {
            List<String> rolesElements = cluster.getElements();

            ListMultimap<String, String> userChunk = ArrayListMultimap.create();

            int rolesCount = rolesElements.size();
            for (int i = 0; i < rolesElements.size(); i++) {
                String rolesOid = rolesElements.get(i);
                List<PrismObject<UserType>> userMembers = extractRoleMembers(result, pageBase, rolesOid);
                List<String> users = extractOid(userMembers);

                int size = users.size();
                double frequency = Math.min(size / (double) rolesCount, 1);

                PrismObject<RoleType> role = getRoleTypeObject(pageBase, rolesOid, operationResult);
                String chunkName = "Unknown Role";
                if (role != null) {
                    chunkName = role.getName().toString();
                }

                miningRoleTypeChunks.add(new MiningRoleTypeChunk(Collections.singletonList(rolesOid),
                        users, chunkName, frequency, Status.NEUTRAL));

                for (String user : users) {
                    userChunk.putAll(user, Collections.singletonList(rolesOid));
                }

                state = i + "/" + rolesCount + " ((1 & 2)/4 mapping elements)";
                System.out.println(state);
            }

            int counter = 0;
            int userChunkSize = userChunk.size();
            for (String key : userChunk.keySet()) {
                List<String> strings = userChunk.get(key);
                double frequency = Math.min(strings.size() / (double) rolesCount, 1);
                PrismObject<UserType> user = getUserTypeObject(pageBase, key, operationResult);
                String chunkName = "Unknown User";
                if (user != null) {
                    chunkName = user.getName().toString();
                }

                miningUserTypeChunks.add(new MiningUserTypeChunk(Collections.singletonList(key), strings, chunkName, frequency, Status.NEUTRAL));
                state = counter + "/" + userChunkSize + " (3/4 mapping points)";
                System.out.println(state);
                counter++;
            }
        }

    }

    private void getChunkedMiningClusterStructureFull(@NotNull RoleAnalysisCluster cluster,
            PageBase pageBase, RoleAnalysisProcessMode mode, OperationResult operationResult) {

        //this set of roles List<String> has users String...
        ListMultimap<List<String>, String> userChunk = ArrayListMultimap.create();
        ListMultimap<String, String> roleMap = ArrayListMultimap.create();
        if (mode.equals(RoleAnalysisProcessMode.USER)) {
            List<String> members = cluster.getElements();
            int membersCount = members.size();
            for (int i = 0; i < membersCount; i++) {
                String membersOid = members.get(i);
                PrismObject<UserType> user = getUserTypeObject(pageBase, membersOid, operationResult);
                if (user == null) {
                    continue;
                }
                List<String> rolesOid = getRolesOid(user.asObjectable());
                Collections.sort(rolesOid);
                userChunk.putAll(rolesOid, Collections.singletonList(membersOid));
                for (String roleId : rolesOid) {
                    roleMap.putAll(roleId, Collections.singletonList(membersOid));
                }

                state = i + "/" + membersCount + " (1/4 mapping elements)";
                System.out.println(state);
            }

            int usersCount = members.size();

            //user //role
            int countMap = 0;
            int roleMapSize = roleMap.size();
            ListMultimap<List<String>, String> roleChunk = ArrayListMultimap.create();
            for (String key : roleMap.keySet()) {
                List<String> values = roleMap.get(key);
                roleChunk.put(values, key);
                state = countMap + "/" + roleMapSize + " (2/4 mapping points)";
                System.out.println(state);
                countMap++;
            }

            int counter = 0;
            int userChunkSize = userChunk.size();
            for (List<String> key : userChunk.keySet()) {
                List<String> usersElements = userChunk.get(key);
                int size = key.size();
                double frequency = Math.min(size / (double) usersCount, 1);
                int usersSize = usersElements.size();
                String chunkName = "Group (" + usersSize + " Users)";
                if (usersSize == 1) {
                    PrismObject<UserType> user = getUserTypeObject(pageBase, usersElements.get(0), operationResult);
                    chunkName = "Unknown User";
                    if (user != null) {
                        chunkName = user.getName().toString();
                    }
                }

                miningUserTypeChunks.add(new MiningUserTypeChunk(usersElements, key, chunkName, frequency, Status.NEUTRAL));
                state = counter + "/" + userChunkSize + " (3/4 prepare elements)";
                System.out.println(state);
                counter++;
            }

            int roleCount = 0;
            int roleChunkSize = roleChunk.size();
            for (List<String> key : roleChunk.keySet()) {
                List<String> roles = roleChunk.get(key);
                int size = key.size();
                double frequency = Math.min(size / (double) usersCount, 1);
                int rolesSize = roles.size();
                String chunkName = "Group (" + rolesSize + " Roles)";
                if (rolesSize == 1) {
                    PrismObject<RoleType> role = getRoleTypeObject(pageBase, roles.get(0), operationResult);
                    chunkName = "Unknown Role";
                    if (role != null) {
                        chunkName = role.getName().toString();
                    }
                }
                miningRoleTypeChunks.add(new MiningRoleTypeChunk(roles, key, chunkName, frequency, Status.NEUTRAL));
                state = roleCount + "/" + roleChunkSize + " (3/4 prepare points)";
                System.out.println(state);
                roleCount++;
            }

        } else if (mode.equals(RoleAnalysisProcessMode.ROLE)) {
            List<String> rolesElements = cluster.getElements();

            int elementsCount = rolesElements.size();
            for (int i = 0; i < rolesElements.size(); i++) {
                String membersOid = rolesElements.get(i);

                List<PrismObject<UserType>> userMembers = extractRoleMembers(result, pageBase, membersOid);
                List<String> users = extractOid(userMembers);
                Collections.sort(users);

                userChunk.putAll(users, Collections.singletonList(membersOid));
                for (String roleId : users) {
                    roleMap.putAll(roleId, Collections.singletonList(membersOid));
                }
                state = i + "/" + elementsCount + " (1/4 mapping elements)";
                System.out.println(state);
            }

            int rolesCount = rolesElements.size();

            //user //role
            int countMap = 0;
            int roleMapSize = roleMap.size();
            ListMultimap<List<String>, String> roleChunk = ArrayListMultimap.create();
            for (String key : roleMap.keySet()) {
                List<String> values = roleMap.get(key);
                roleChunk.put(values, key);

                state = countMap + "/" + roleMapSize + " (2/4 mapping points)";
                System.out.println(state);
                countMap++;
            }

            int counter = 0;
            int userChunkSize = userChunk.size();
            for (List<String> key : userChunk.keySet()) {
                List<String> roles = userChunk.get(key);
                int size = key.size();
                double frequency = Math.min(size / (double) rolesCount, 1);
                int rolesSize = roles.size();
                String chunkName = "Group (" + rolesSize + " Roles)";
                if (rolesSize == 1) {
                    PrismObject<RoleType> role = getRoleTypeObject(pageBase, roles.get(0), operationResult);
                    chunkName = "Unknown Role";
                    if (role != null) {
                        chunkName = role.getName().toString();
                    }

                }
                miningRoleTypeChunks.add(new MiningRoleTypeChunk(roles, key, chunkName, frequency, Status.NEUTRAL));

                state = counter + "/" + userChunkSize + " (3/4 prepare elements)";
                System.out.println(state);
                counter++;
            }

            int roleCount = 0;
            int roleChunkSize = roleChunk.size();
            for (List<String> key : roleChunk.keySet()) {
                List<String> users = roleChunk.get(key);
                int size = key.size();
                double frequency = Math.min(size / (double) rolesCount, 1);
                int userSize = users.size();
                String chunkName = "Group (" + userSize + " Users)";
                if (userSize == 1) {
                    PrismObject<UserType> user = getUserTypeObject(pageBase, users.get(0), operationResult);
                    chunkName = "Unknown User";
                    if (user != null) {
                        chunkName = user.getName().toString();
                    }

                }
                miningUserTypeChunks.add(new MiningUserTypeChunk(users, key, chunkName, frequency, Status.NEUTRAL));

                state = roleCount + "/" + roleChunkSize + " (3/4 prepare points)";
                System.out.println(state);
                roleCount++;
            }
        }

    }

    private void getMiningClusterStructure(@NotNull RoleAnalysisCluster cluster,
            PageBase pageBase, RoleAnalysisProcessMode mode, OperationResult operationResult) {

        ListMultimap<String, String> roleChunk = ArrayListMultimap.create();

        if (mode.equals(RoleAnalysisProcessMode.USER)) {
            List<String> members = cluster.getElements();
            int usersCount = members.size();
            for (String membersOid : members) {
                PrismObject<UserType> user = getUserTypeObject(pageBase, membersOid, operationResult);

                if (user == null) {
                    continue;
                }

                List<String> rolesOid = getRolesOid(user.asObjectable());
                int size = rolesOid.size();
                double frequency = Math.min(size / (double) usersCount, 1);
                String chunkName = "Unknown User";
                if (user.getName() != null) {
                    chunkName = user.getName().toString();
                }
                miningUserTypeChunks.add(new MiningUserTypeChunk(Collections.singletonList(membersOid), rolesOid, chunkName,
                        frequency, Status.NEUTRAL));

                for (String roleId : rolesOid) {
                    roleChunk.putAll(roleId, Collections.singletonList(user.getOid()));
                }
            }

            for (String key : roleChunk.keySet()) {
                List<String> strings = roleChunk.get(key);
                double frequency = Math.min(strings.size() / (double) usersCount, 1);

                PrismObject<RoleType> role = getRoleTypeObject(pageBase, key, operationResult);
                String chunkName = "Unknown Role";
                if (role != null) {
                    chunkName = role.getName().toString();
                }

                miningRoleTypeChunks.add(new MiningRoleTypeChunk(Collections.singletonList(key), strings, chunkName, frequency, Status.NEUTRAL));
            }

        } else if (mode.equals(RoleAnalysisProcessMode.ROLE)) {
            List<String> rolesElements = cluster.getElements();

            ListMultimap<String, String> userChunk = ArrayListMultimap.create();

            int rolesCount = rolesElements.size();
            for (String rolesOid : rolesElements) {
                List<PrismObject<UserType>> userMembers = extractRoleMembers(result, pageBase, rolesOid);
                List<String> users = extractOid(userMembers);

                int size = users.size();
                double frequency = Math.min(size / (double) rolesCount, 1);

                PrismObject<RoleType> role = getRoleTypeObject(pageBase, rolesOid, operationResult);
                String chunkName = "Unknown Role";
                if (role != null) {
                    chunkName = role.getName().toString();
                }

                miningRoleTypeChunks.add(new MiningRoleTypeChunk(Collections.singletonList(rolesOid),
                        users, chunkName, frequency, Status.NEUTRAL));

                for (String user : users) {
                    userChunk.putAll(user, Collections.singletonList(rolesOid));
                }
            }

            for (String key : userChunk.keySet()) {
                List<String> strings = userChunk.get(key);
                double frequency = Math.min(strings.size() / (double) rolesCount, 1);
                PrismObject<UserType> user = getUserTypeObject(pageBase, key, operationResult);
                String chunkName = "Unknown User";
                if (user != null) {
                    chunkName = user.getName().toString();
                }

                miningUserTypeChunks.add(new MiningUserTypeChunk(Collections.singletonList(key), strings, chunkName, frequency, Status.NEUTRAL));
            }
        }

    }

    private void getChunkedMiningClusterStructure(@NotNull RoleAnalysisCluster cluster,
            PageBase pageBase, RoleAnalysisProcessMode mode, OperationResult operationResult) {

        //this set of roles List<String> has users String...
        ListMultimap<List<String>, String> userChunk = ArrayListMultimap.create();
        ListMultimap<String, String> roleMap = ArrayListMultimap.create();
        if (mode.equals(RoleAnalysisProcessMode.USER)) {
            List<String> members = cluster.getElements();
            for (String membersOid : members) {
                PrismObject<UserType> user = getUserTypeObject(pageBase, membersOid, operationResult);
                if (user == null) {
                    continue;
                }
                List<String> rolesOid = getRolesOid(user.asObjectable());
                Collections.sort(rolesOid);
                userChunk.putAll(rolesOid, Collections.singletonList(membersOid));
                for (String roleId : rolesOid) {
                    roleMap.putAll(roleId, Collections.singletonList(membersOid));
                }
            }

            int usersCount = members.size();

            //user //role
            ListMultimap<List<String>, String> roleChunk = ArrayListMultimap.create();
            for (String key : roleMap.keySet()) {
                List<String> values = roleMap.get(key);
                roleChunk.put(values, key);
            }

//            for (List<String> key : userChunk.keySet()) {
//                List<String> usersElements = userChunk.get(key);
//                int size = key.size();
//                double frequency = Math.min(size / (double) usersCount, 1);
//                int usersSize = usersElements.size();
//                String chunkName = "Group (" + usersSize + " Users)";
//                if (usersSize == 1) {
//                    PrismObject<UserType> user = getUserTypeObject(pageBase, usersElements.get(0), operationResult);
//                    chunkName = "Unknown User";
//                    if (user != null) {
//                        chunkName = user.getName().toString();
//                    }
//                }
//                miningUserTypeChunks.add(new MiningUserTypeChunk(usersElements, key, chunkName, frequency, Status.NEUTRAL));
//            }

            for (List<String> key : roleChunk.keySet()) {
                List<String> roles = roleChunk.get(key);
                int size = key.size();
                double frequency = Math.min(size / (double) usersCount, 1);
                int rolesSize = roles.size();
                String chunkName = "Group (" + rolesSize + " Roles)";
                if (rolesSize == 1) {
                    PrismObject<RoleType> role = getRoleTypeObject(pageBase, roles.get(0), operationResult);
                    chunkName = "Unknown Role";
                    if (role != null) {
                        chunkName = role.getName().toString();
                    }
                }
                miningRoleTypeChunks.add(new MiningRoleTypeChunk(roles, key, chunkName, frequency, Status.NEUTRAL));
            }

        } else if (mode.equals(RoleAnalysisProcessMode.ROLE)) {
            List<String> rolesElements = cluster.getElements();

            for (String membersOid : rolesElements) {

                List<PrismObject<UserType>> userMembers = extractRoleMembers(result, pageBase, membersOid);
                List<String> users = extractOid(userMembers);
                Collections.sort(users);

                userChunk.putAll(users, Collections.singletonList(membersOid));
                for (String roleId : users) {
                    roleMap.putAll(roleId, Collections.singletonList(membersOid));
                }
            }

            int rolesCount = rolesElements.size();

            //user //role
            ListMultimap<List<String>, String> roleChunk = ArrayListMultimap.create();
            for (String key : roleMap.keySet()) {
                List<String> values = roleMap.get(key);
                roleChunk.put(values, key);
            }

//            for (List<String> key : userChunk.keySet()) {
//                List<String> roles = userChunk.get(key);
//                int size = key.size();
//                double frequency = Math.min(size / (double) rolesCount, 1);
//                int rolesSize = roles.size();
//                String chunkName = "Group (" + rolesSize + " Roles)";
//                if (rolesSize == 1) {
//                    PrismObject<RoleType> role = getRoleTypeObject(pageBase, roles.get(0), operationResult);
//                    chunkName = "Unknown Role";
//                    if (role != null) {
//                        chunkName = role.getName().toString();
//                    }
//
//                }
//                miningRoleTypeChunks.add(new MiningRoleTypeChunk(roles, key, chunkName, frequency, Status.NEUTRAL));
//            }

            for (List<String> key : roleChunk.keySet()) {
                List<String> users = roleChunk.get(key);
                int size = key.size();
                double frequency = Math.min(size / (double) rolesCount, 1);
                int userSize = users.size();
                String chunkName = "Group (" + userSize + " Users)";
                if (userSize == 1) {
                    PrismObject<UserType> user = getUserTypeObject(pageBase, users.get(0), operationResult);
                    chunkName = "Unknown User";
                    if (user != null) {
                        chunkName = user.getName().toString();
                    }

                }
                miningUserTypeChunks.add(new MiningUserTypeChunk(users, key, chunkName, frequency, Status.NEUTRAL));
            }
        }

    }

    public List<MiningUserTypeChunk> getMiningUserTypeChunks(SORT sort) {
        resetState();

        if (sort.equals(SORT.JACCARD)) {
            return sortUserTypeChunks(miningUserTypeChunks);
        } else if (sort.equals(SORT.FREQUENCY)) {
            return JaccardSorter.sortByFrequencyUserType(miningUserTypeChunks);
        } else {
            return miningUserTypeChunks;
        }
    }

    public List<MiningRoleTypeChunk> getMiningRoleTypeChunks(SORT sort) {
        resetState();

        if (sort.equals(SORT.JACCARD)) {
            return sortRoleTypeChunks(miningRoleTypeChunks);
        } else if (sort.equals(SORT.FREQUENCY)) {
            return JaccardSorter.sortByFrequencyRoleType(miningRoleTypeChunks, state);
        } else {
            return miningRoleTypeChunks;
        }

    }

    private List<MiningUserTypeChunk> sortUserTypeChunks(List<MiningUserTypeChunk> dataPoints) {

        List<MiningUserTypeChunk> sorted = new ArrayList<>();
        List<MiningUserTypeChunk> remaining = new ArrayList<>(dataPoints);

        remaining.sort(Comparator.comparingInt(set -> -set.getRoles().size()));

        int counter = 0;
        int size = dataPoints.size();
        while (!remaining.isEmpty()) {
            MiningUserTypeChunk current = remaining.remove(0);
            double maxSimilarity = 0;
            int insertIndex = -1;

            if (sorted.size() < 2) {
                if (sorted.isEmpty()) {
                    sorted.add(current);
                } else {
                    sorted.add(0, current);
                }
            } else {
                for (int i = 1; i < sorted.size(); i++) {
                    MiningUserTypeChunk previous = sorted.get(i - 1);
                    MiningUserTypeChunk next = sorted.get(i);
                    List<String> currentRoles = current.getRoles();
                    double similarity = jacquardSimilarity(currentRoles,
                            previous.getRoles());
                    double nextSimilarity = jacquardSimilarity(currentRoles,
                            next.getRoles());

                    if (Math.max(similarity, nextSimilarity) > maxSimilarity
                            && Math.min(similarity, nextSimilarity) >= jacquardSimilarity(
                            previous.getRoles(), next.getRoles())) {
                        maxSimilarity = Math.max(similarity, nextSimilarity);
                        insertIndex = i;
                    }
                }

                if (insertIndex == -1) {
                    if (jacquardSimilarity(current.getRoles(),
                            sorted.get(0).getRoles())
                            > jacquardSimilarity(sorted.get(0).getRoles(),
                            sorted.get(1).getRoles())) {
                        sorted.add(0, current);
                    } else {
                        sorted.add(current);
                    }
                } else {
                    sorted.add(insertIndex, current);
                }
            }

            state = counter + "/" + size + " (4/4 sorting)";
            System.out.println(state);
            counter++;
        }
        return sorted;
    }

    private List<MiningRoleTypeChunk> sortRoleTypeChunks(List<MiningRoleTypeChunk> dataPoints) {

        List<MiningRoleTypeChunk> sorted = new ArrayList<>();
        List<MiningRoleTypeChunk> remaining = new ArrayList<>(dataPoints);

        remaining.sort(Comparator.comparingInt(set -> -set.getUsers().size()));

        int counter = 0;
        int size = dataPoints.size();
        while (!remaining.isEmpty()) {
            MiningRoleTypeChunk current = remaining.remove(0);
            double maxSimilarity = 0;
            int insertIndex = -1;

            if (sorted.size() < 2) {
                if (sorted.isEmpty()) {
                    sorted.add(current);
                } else {
                    sorted.add(0, current);
                }
            } else {
                for (int i = 1; i < sorted.size(); i++) {
                    MiningRoleTypeChunk previous = sorted.get(i - 1);
                    MiningRoleTypeChunk next = sorted.get(i);
                    double similarity = jacquardSimilarity(current.getUsers(),
                            previous.getUsers());
                    double nextSimilarity = jacquardSimilarity(current.getUsers(),
                            next.getUsers());

                    if (Math.max(similarity, nextSimilarity) > maxSimilarity
                            && Math.min(similarity, nextSimilarity) >= jacquardSimilarity(
                            previous.getUsers(), next.getUsers())) {
                        maxSimilarity = Math.max(similarity, nextSimilarity);
                        insertIndex = i;
                    }
                }

                if (insertIndex == -1) {
                    if (jacquardSimilarity(current.getUsers(),
                            sorted.get(0).getUsers())
                            > jacquardSimilarity(sorted.get(0).getUsers(),
                            sorted.get(1).getUsers())) {
                        sorted.add(0, current);
                    } else {
                        sorted.add(current);
                    }
                } else {
                    sorted.add(insertIndex, current);
                }
            }

            state = counter + "/" + size + " (4/4 sorting)";
            System.out.println(state);
            counter++;
        }

        return sorted;
    }

    private void resolveAndExecute(RoleAnalysisCluster clusterType, PageBase pageBase, RoleAnalysisProcessMode mode, OperationResult operationResult,
            boolean chunk, boolean full) {
        if (full) {
            if (chunk) {
                getChunkedMiningClusterStructureFull(clusterType, pageBase, mode, operationResult);
            } else {
                getMiningClusterStructureFull(clusterType, pageBase, mode, operationResult);
            }
        } else {
            if (chunk) {
                getChunkedMiningClusterStructure(clusterType, pageBase, mode, operationResult);
            } else {
                getMiningClusterStructure(clusterType, pageBase, mode, operationResult);
            }
        }
    }

    private void resetList() {
        miningUserTypeChunks = new ArrayList<>();
        miningRoleTypeChunks = new ArrayList<>();
    }

    private void resetState() {
        state = "START";
    }

    public String getState() {
        return state;
    }

}
