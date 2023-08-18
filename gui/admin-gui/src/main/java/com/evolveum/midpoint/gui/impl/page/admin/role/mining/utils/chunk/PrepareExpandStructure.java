/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.chunk;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.*;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.chunk.CacheUtils.*;

import java.io.Serializable;
import java.util.*;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.MiningOperationChunk;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.MiningRoleTypeChunk;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.MiningUserTypeChunk;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class PrepareExpandStructure implements MiningStructure, Serializable {

    public MiningOperationChunk executeOperation(@NotNull RoleAnalysisClusterType cluster, boolean fullProcess,
            RoleAnalysisProcessModeType mode, PageBase pageBase, OperationResult result, String state) {
        if (fullProcess) {
            return resolveFullStructures(cluster, mode, pageBase, result, state);
        } else {
            return resolvePartialStructures(cluster, mode, pageBase, result, state);
        }
    }

    private MiningOperationChunk resolvePartialStructures(@NotNull RoleAnalysisClusterType cluster,
            RoleAnalysisProcessModeType mode, PageBase pageBase, OperationResult result, String state) {

        if (mode.equals(RoleAnalysisProcessModeType.USER)) {
            return preparePartialRoleBasedStructure(cluster, pageBase, result, state);

        } else if (mode.equals(RoleAnalysisProcessModeType.ROLE)) {
            return preparePartialUserBasedStructure(cluster, pageBase, result, state);
        }

        return new MiningOperationChunk(new ArrayList<>(), new ArrayList<>());
    }

    private MiningOperationChunk resolveFullStructures(@NotNull RoleAnalysisClusterType cluster,
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

        Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();
        Map<String, PrismObject<RoleType>> roleExistCache = new HashMap<>();

        List<MiningUserTypeChunk> miningUserTypeChunks = new ArrayList<>();
        List<MiningRoleTypeChunk> miningRoleTypeChunks = new ArrayList<>();

        List<ObjectReferenceType> rolesElements = cluster.getMember();
        ListMultimap<String, String> userChunk = ArrayListMultimap.create();
        Set<String> membersOidSet = new HashSet<>();

        int rolesCount = rolesElements.size();
        for (int i = 0; i < rolesElements.size(); i++) {
            String roleId = rolesElements.get(i).getOid();
            PrismObject<RoleType> role = cacheRole(pageBase, result, roleExistCache, roleId);
            if (role == null) {
                continue;
            }
            membersOidSet.add(roleId);
            //TODO add filter
            List<PrismObject<UserType>> userMembers = extractRoleMembers(null, result, pageBase, roleId);
            List<String> users = extractOid(userMembers);

            String chunkName = role.getName().toString();

            miningRoleTypeChunks.add(new MiningRoleTypeChunk(Collections.singletonList(roleId),
                    users, chunkName, 0, Status.NEUTRAL));

            for (String user : users) {
                userChunk.putAll(user, Collections.singletonList(roleId));
            }

            System.out.println(generateState(i, rolesCount,"((1 & 2)/4 mapping elements)"));
        }

        int memberCount = membersOidSet.size();

        int counter = 0;
        int userChunkSize = userChunk.size();
        for (String key : userChunk.keySet()) {
            List<String> roleIds = userChunk.get(key);
            roleIds.retainAll(membersOidSet);
            double frequency = Math.min(roleIds.size() / (double) memberCount, 1);
            PrismObject<UserType> user = cacheUser(pageBase, result, userExistCache, key);
            String chunkName = "NOT FOUND";
            if (user != null) {
                chunkName = user.getName().toString();
            }

            miningUserTypeChunks.add(new MiningUserTypeChunk(Collections.singletonList(key), roleIds, chunkName, frequency,
                    Status.NEUTRAL));

            System.out.println(generateState(counter, userChunkSize,"(3/4 mapping points)"));

            counter++;
        }

        return new MiningOperationChunk(miningUserTypeChunks, miningRoleTypeChunks);
    }

    @Override
    public MiningOperationChunk prepareUserBasedStructure(@NotNull RoleAnalysisClusterType cluster, PageBase pageBase,
            OperationResult result, String state) {

        Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();
        Map<String, PrismObject<RoleType>> roleExistCache = new HashMap<>();

        List<MiningUserTypeChunk> miningUserTypeChunks = new ArrayList<>();
        List<MiningRoleTypeChunk> miningRoleTypeChunks = new ArrayList<>();

        ListMultimap<String, String> roleChunk = ArrayListMultimap.create();
        Set<String> membersOidSet = new HashSet<>();

        List<ObjectReferenceType> members = cluster.getMember();
        int usersCount = members.size();
        for (int i = 0; i < usersCount; i++) {
            String userOid = members.get(i).getOid();
            PrismObject<UserType> user = cacheUser(pageBase, result, userExistCache, userOid);
            if (user == null) {continue;}

            String chunkName = "NOT FOUND";
            if (user.getName() != null) {
                chunkName = user.getName().toString();
            }
            membersOidSet.add(userOid);

            List<String> roleOids = getRolesOidAssignment(user.asObjectable());
            List<String> existingRolesAssignment = new ArrayList<>();
            for (String roleId : roleOids) {
                PrismObject<RoleType> role = cacheRole(pageBase, result, roleExistCache, roleId);
                if (role == null) {continue;}
                existingRolesAssignment.add(roleId);
                roleChunk.putAll(roleId, Collections.singletonList(user.getOid()));

            }

            miningUserTypeChunks.add(new MiningUserTypeChunk(Collections.singletonList(userOid), existingRolesAssignment, chunkName,
                    0, Status.NEUTRAL));

            System.out.println(generateState(i, usersCount,"((1 & 2)/4 mapping elements)"));

        }

        int memberCount = membersOidSet.size();

        int roleChunkSize = roleChunk.size();
        int counter = 0;
        for (String key : roleChunk.keySet()) {
            List<String> userOids = roleChunk.get(key);
            double frequency = Math.min(userOids.size() / (double) memberCount, 1);

            PrismObject<RoleType> role = cacheRole(pageBase, result, roleExistCache, key);
            String chunkName = "NOT FOUND";
            if (role != null) {
                chunkName = role.getName().toString();
            }

            miningRoleTypeChunks.add(new MiningRoleTypeChunk(Collections.singletonList(key), userOids, chunkName, frequency,
                    Status.NEUTRAL));

            System.out.println(generateState(counter, roleChunkSize,"(3/4 mapping points)"));

            counter++;
        }

        return new MiningOperationChunk(miningUserTypeChunks, miningRoleTypeChunks);
    }

    @Override
    public MiningOperationChunk preparePartialRoleBasedStructure(@NotNull RoleAnalysisClusterType cluster,
            PageBase pageBase, OperationResult result, String state) {

        Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();
        Map<String, PrismObject<RoleType>> roleExistCache = new HashMap<>();

        List<MiningUserTypeChunk> miningUserTypeChunks = new ArrayList<>();
        List<MiningRoleTypeChunk> miningRoleTypeChunks = new ArrayList<>();

        List<ObjectReferenceType> rolesElements = cluster.getMember();
        ListMultimap<List<String>, String> userChunk = ArrayListMultimap.create();
        ListMultimap<String, String> roleMap = ArrayListMultimap.create();
        Set<String> membersOidSet = new HashSet<>();

        for (ObjectReferenceType objectReferenceType : rolesElements) {
            String oid = objectReferenceType.getOid();
            PrismObject<RoleType> role = cacheRole(pageBase, result, roleExistCache, oid);
            if (role == null) {continue;}
            membersOidSet.add(oid);

            //TODO add filter
            List<PrismObject<UserType>> userMembers = extractRoleMembers(null, result, pageBase, oid);
            List<String> users = extractOid(userMembers);
            Collections.sort(users);

            userChunk.putAll(users, Collections.singletonList(oid));
            for (String roleId : users) {
                roleMap.putAll(roleId, Collections.singletonList(oid));
            }
        }

        //user //role
        ListMultimap<List<String>, String> roleChunk = ArrayListMultimap.create();
        for (String key : roleMap.keySet()) {
            List<String> values = roleMap.get(key);
            roleChunk.put(values, key);
        }

        int memberCount = membersOidSet.size();

        for (List<String> key : roleChunk.keySet()) {
            List<String> users = roleChunk.get(key);
            key.retainAll(membersOidSet);
            int size = key.size();
            double frequency = Math.min(size / (double) memberCount, 1);
            int userSize = users.size();
            String chunkName = "Group (" + userSize + " Users)";
            if (userSize == 1) {
                PrismObject<UserType> user = cacheUser(pageBase, result, userExistCache, users.get(0));
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
        Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();
        Map<String, PrismObject<RoleType>> roleExistCache = new HashMap<>();

        List<MiningUserTypeChunk> miningUserTypeChunks = new ArrayList<>();
        List<MiningRoleTypeChunk> miningRoleTypeChunks = new ArrayList<>();

        ListMultimap<List<String>, String> userChunk = ArrayListMultimap.create();
        ListMultimap<String, String> roleMap = ArrayListMultimap.create();
        Set<String> membersOidSet = new HashSet<>();

        List<ObjectReferenceType> members = cluster.getMember();
        for (ObjectReferenceType objectReferenceType : members) {
            String oid = objectReferenceType.getOid();
            PrismObject<UserType> user = cacheUser(pageBase, result, userExistCache, oid);
            if (user == null) {
                continue;
            }
            membersOidSet.add(oid);

            List<String> rolesOid = getRolesOidAssignment(user.asObjectable());
            List<String> existingRolesAssignment = new ArrayList<>();
            for (String roleId : rolesOid) {
                PrismObject<RoleType> role = cacheRole(pageBase, result, roleExistCache, roleId);
                if (role == null) {continue;}
                existingRolesAssignment.add(roleId);
                roleMap.putAll(roleId, Collections.singletonList(oid));
            }

            Collections.sort(existingRolesAssignment);
            userChunk.putAll(existingRolesAssignment, Collections.singletonList(oid));

        }

        //user //role
        ListMultimap<List<String>, String> roleChunk = ArrayListMultimap.create();
        for (String key : roleMap.keySet()) {
            List<String> values = roleMap.get(key);
            roleChunk.put(values, key);
        }

        int memberCount = membersOidSet.size();

        for (List<String> key : roleChunk.keySet()) {
            List<String> roles = roleChunk.get(key);
            key.retainAll(membersOidSet);
            int size = key.size();
            double frequency = Math.min(size / (double) memberCount, 1);
            int rolesSize = roles.size();
            String chunkName = "Group (" + rolesSize + " Roles)";
            if (rolesSize == 1) {
                PrismObject<RoleType> role = roleExistCache.get(roles.get(0));
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

