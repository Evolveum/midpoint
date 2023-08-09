/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.MiningOperationChunk;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.MiningRoleTypeChunk;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.MiningUserTypeChunk;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.*;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.*;

public class PrepareChunkStructure implements MiningStructure, Serializable {

    public MiningOperationChunk executeOperation(@NotNull RoleAnalysisClusterType cluster, boolean fullProcess,
            RoleAnalysisProcessModeType mode, PageBase pageBase, OperationResult result, String state) {
        if (fullProcess) {
            return resolveFullChunkStructures(cluster, mode, pageBase, result, state);
        } else {
            return resolvePartialChunkStructures(cluster, mode, pageBase, result, state);
        }
    }

    private MiningOperationChunk resolvePartialChunkStructures(@NotNull RoleAnalysisClusterType cluster,
            RoleAnalysisProcessModeType mode, PageBase pageBase, OperationResult result, String state) {
        if (mode.equals(RoleAnalysisProcessModeType.USER)) {
            return preparePartialUserBasedStructure(cluster, pageBase, result, state);

        } else if (mode.equals(RoleAnalysisProcessModeType.ROLE)) {
            return preparePartialRoleBasedStructure(cluster, pageBase, result, state);
        }

        return new MiningOperationChunk(new ArrayList<>(), new ArrayList<>());
    }

    private MiningOperationChunk resolveFullChunkStructures(@NotNull RoleAnalysisClusterType cluster,
            RoleAnalysisProcessModeType mode, PageBase pageBase, OperationResult result, String state) {

        if (mode.equals(RoleAnalysisProcessModeType.USER)) {
            return prepareUserBasedStructure(cluster, pageBase, result, state);

        } else if (mode.equals(RoleAnalysisProcessModeType.ROLE)) {
            return prepareRoleBasedStructure(cluster, pageBase, result, state);
        }
        return new MiningOperationChunk(new ArrayList<>(), new ArrayList<>());
    }

    @Override
    public MiningOperationChunk prepareRoleBasedStructure(@NotNull RoleAnalysisClusterType cluster, PageBase pageBase,
            OperationResult result, String state) {
        List<MiningUserTypeChunk> miningUserTypeChunks = new ArrayList<>();
        List<MiningRoleTypeChunk> miningRoleTypeChunks = new ArrayList<>();
        ListMultimap<List<String>, String> userChunk = ArrayListMultimap.create();
        ListMultimap<String, String> roleMap = ArrayListMultimap.create();
        List<ObjectReferenceType> members = cluster.getMember();

        Set<String> membersOidSet = new HashSet<>();

        int membersCount = members.size();
        for (int i = 0; i < members.size(); i++) {
            String membersOid = members.get(i).getOid();
            PrismObject<RoleType> role = getRoleTypeObject(pageBase, membersOid, result);
            if (role == null) {
                continue;
            }
            membersOidSet.add(membersOid);

            List<PrismObject<UserType>> userMembers = extractRoleMembers(result, pageBase, membersOid);
            List<String> users = extractOid(userMembers);
            Collections.sort(users);

            userChunk.putAll(users, Collections.singletonList(membersOid));
            for (String userOid : users) {
                roleMap.putAll(userOid, Collections.singletonList(membersOid));
            }
            state = i + "/" + membersCount + " (1/4 mapping elements)";
            System.out.println(state);
        }

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
            int rolesSize = roles.size();
            String chunkName = "Group (" + rolesSize + " Roles)";
            if (rolesSize == 1) {
                PrismObject<RoleType> role = getRoleTypeObject(pageBase, roles.get(0), result);
                chunkName = "NOT FOUND";
                if (role != null) {
                    chunkName = role.getName().toString();
                }
            }
            miningRoleTypeChunks.add(new MiningRoleTypeChunk(roles, key, chunkName, 0, Status.NEUTRAL));

            state = counter + "/" + userChunkSize + " (3/4 prepare elements)";
            System.out.println(state);
            counter++;
        }

        int memberCount = membersOidSet.size();
        int roleCount = 0;
        int roleChunkSize = roleChunk.size();
        for (List<String> key : roleChunk.keySet()) {
            List<String> users = roleChunk.get(key);
            key.retainAll(membersOidSet);
            int size = key.size();
            double frequency = Math.min(size / (double) memberCount, 1);
            int userSize = users.size();
            String chunkName = "Group (" + userSize + " Users)";
            if (userSize == 1) {
                PrismObject<UserType> user = getUserTypeObject(pageBase, users.get(0), result);
                chunkName = "NOT FOUND";
                if (user != null) {
                    chunkName = user.getName().toString();
                }
            }
            miningUserTypeChunks.add(new MiningUserTypeChunk(users, key, chunkName, frequency, Status.NEUTRAL));

            state = roleCount + "/" + roleChunkSize + " (3/4 prepare points)";
            System.out.println(state);
            roleCount++;
        }

        return new MiningOperationChunk(miningUserTypeChunks, miningRoleTypeChunks);
    }

    @Override
    public MiningOperationChunk prepareUserBasedStructure(@NotNull RoleAnalysisClusterType cluster, PageBase pageBase,
            OperationResult result, String state) {
        List<MiningUserTypeChunk> miningUserTypeChunks = new ArrayList<>();
        List<MiningRoleTypeChunk> miningRoleTypeChunks = new ArrayList<>();

        ListMultimap<List<String>, String> userChunk = ArrayListMultimap.create();
        ListMultimap<String, String> roleMap = ArrayListMultimap.create();

        List<ObjectReferenceType> members = cluster.getMember();
        Set<String> membersOidSet = new HashSet<>();
        int membersCount = members.size();

        for (int i = 0; i < membersCount; i++) {
            String membersOid = members.get(i).getOid();
            PrismObject<UserType> user = getUserTypeObject(pageBase, membersOid, result);
            if (user == null) {
                continue;
            }
            membersOidSet.add(membersOid);
            List<String> rolesOid = getRolesOidAssignment(user.asObjectable());
            Collections.sort(rolesOid);
            userChunk.putAll(rolesOid, Collections.singletonList(membersOid));
            for (String roleId : rolesOid) {
                roleMap.putAll(roleId, Collections.singletonList(membersOid));
            }

            state = i + "/" + membersCount + " (1/4 mapping elements)";
            System.out.println(state);
        }

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
            int usersSize = usersElements.size();
            String chunkName = "Group (" + usersSize + " Users)";
            if (usersSize == 1) {
                PrismObject<UserType> user = getUserTypeObject(pageBase, usersElements.get(0), result);
                chunkName = "NOT FOUND";
                if (user != null) {
                    chunkName = user.getName().toString();
                }
            }

            miningUserTypeChunks.add(new MiningUserTypeChunk(usersElements, key, chunkName, 0, Status.NEUTRAL));
            state = counter + "/" + userChunkSize + " (3/4 prepare elements)";
            System.out.println(state);
            counter++;
        }

        int memberCount = membersOidSet.size();

        int roleCount = 0;
        int roleChunkSize = roleChunk.size();
        for (List<String> key : roleChunk.keySet()) {
            List<String> roles = roleChunk.get(key);
            key.retainAll(membersOidSet);
            int size = key.size();
            double frequency = Math.min(size / (double) memberCount, 1);
            int rolesSize = roles.size();
            String chunkName = "Group (" + rolesSize + " Roles)";
            if (rolesSize == 1) {
                PrismObject<RoleType> role = getRoleTypeObject(pageBase, roles.get(0), result);
                chunkName = "NOT FOUND";
                if (role != null) {
                    chunkName = role.getName().toString();
                }
            }
            miningRoleTypeChunks.add(new MiningRoleTypeChunk(roles, key, chunkName, frequency, Status.NEUTRAL));
            state = roleCount + "/" + roleChunkSize + " (3/4 prepare points)";
            System.out.println(state);
            roleCount++;
        }

        return new MiningOperationChunk(miningUserTypeChunks, miningRoleTypeChunks);
    }

    @Override
    public MiningOperationChunk preparePartialRoleBasedStructure(@NotNull RoleAnalysisClusterType cluster,
            PageBase pageBase, OperationResult result, String state) {
        List<MiningUserTypeChunk> miningUserTypeChunks = new ArrayList<>();
        List<MiningRoleTypeChunk> miningRoleTypeChunks = new ArrayList<>();

        ListMultimap<List<String>, String> userChunk = ArrayListMultimap.create();
        ListMultimap<String, String> roleMap = ArrayListMultimap.create();
        Set<String> membersOidSet = new HashSet<>();

        List<ObjectReferenceType> members = cluster.getMember();
        for (ObjectReferenceType objectReferenceType : members) {
            String oid = objectReferenceType.getOid();
            PrismObject<RoleType> role = getRoleTypeObject(pageBase, oid, result);
            if (role == null) {
                continue;
            }
            membersOidSet.add(oid);

            List<PrismObject<UserType>> userMembers = extractRoleMembers(result, pageBase, oid);
            List<String> users = extractOid(userMembers);
            Collections.sort(users);

            userChunk.putAll(users, Collections.singletonList(oid));
            for (String roleId : users) {
                roleMap.putAll(roleId, Collections.singletonList(oid));
            }
        }

        int membersCount = membersOidSet.size();
        //user //role
        ListMultimap<List<String>, String> roleChunk = ArrayListMultimap.create();
        for (String key : roleMap.keySet()) {
            List<String> values = roleMap.get(key);
            roleChunk.put(values, key);
        }

        for (List<String> key : roleChunk.keySet()) {
            List<String> users = roleChunk.get(key);
            key.retainAll(membersOidSet);
            int size = key.size();
            double frequency = Math.min(size / (double) membersCount, 1);
            int userSize = users.size();
            String chunkName = "Group (" + userSize + " Users)";
            if (userSize == 1) {
                PrismObject<UserType> user = getUserTypeObject(pageBase, users.get(0), result);
                chunkName = "NOT FOUND";
                if (user != null) {
                    chunkName = user.getName().toString();
                }
            }
            miningUserTypeChunks.add(new MiningUserTypeChunk(users, key, chunkName, frequency, Status.NEUTRAL));
        }
        return new MiningOperationChunk(miningUserTypeChunks, miningRoleTypeChunks);
    }

    @Override
    public MiningOperationChunk preparePartialUserBasedStructure(@NotNull RoleAnalysisClusterType cluster,
            PageBase pageBase, OperationResult result, String state) {
        List<MiningUserTypeChunk> miningUserTypeChunks = new ArrayList<>();
        List<MiningRoleTypeChunk> miningRoleTypeChunks = new ArrayList<>();

        ListMultimap<List<String>, String> userChunk = ArrayListMultimap.create();
        ListMultimap<String, String> roleMap = ArrayListMultimap.create();
        Set<String> membersOidSet = new HashSet<>();

        List<ObjectReferenceType> members = cluster.getMember();
        for (ObjectReferenceType objectReferenceType : members) {
            String oid = objectReferenceType.getOid();
            PrismObject<UserType> user = getUserTypeObject(pageBase, oid, result);
            if (user == null) {
                continue;
            }
            membersOidSet.add(oid);
            List<String> rolesOid = getRolesOidAssignment(user.asObjectable());
            Collections.sort(rolesOid);
            userChunk.putAll(rolesOid, Collections.singletonList(oid));
            for (String roleId : rolesOid) {
                roleMap.putAll(roleId, Collections.singletonList(oid));
            }
        }

        int membersCount = membersOidSet.size();
        //user //role
        ListMultimap<List<String>, String> roleChunk = ArrayListMultimap.create();
        for (String key : roleMap.keySet()) {
            List<String> values = roleMap.get(key);
            roleChunk.put(values, key);
        }

        for (List<String> key : roleChunk.keySet()) {
            List<String> roles = roleChunk.get(key);
            key.retainAll(membersOidSet);
            int size = key.size();
            double frequency = Math.min(size / (double) membersCount, 1);
            int rolesSize = roles.size();
            String chunkName = "Group (" + rolesSize + " Roles)";
            if (rolesSize == 1) {
                PrismObject<RoleType> role = getRoleTypeObject(pageBase, roles.get(0), result);
                chunkName = "NOT FOUND";
                if (role != null) {
                    chunkName = role.getName().toString();
                }
            }
            miningRoleTypeChunks.add(new MiningRoleTypeChunk(roles, key, chunkName, frequency, Status.NEUTRAL));
        }
        return new MiningOperationChunk(miningUserTypeChunks, miningRoleTypeChunks);
    }

}
