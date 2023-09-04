/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.chunk;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.extractOid;
import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.getRolesOidAssignment;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.RoleAnalysisObjectUtils.*;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.chunk.CacheUtils.*;

import java.io.Serializable;
import java.util.*;

import com.evolveum.midpoint.common.mining.objects.handler.RoleAnalysisProgressIncrement;

import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisOperationMode;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.task.api.Task;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class PrepareExpandStructure implements MiningStructure, Serializable {

    RoleAnalysisProgressIncrement handler = new RoleAnalysisProgressIncrement("Data Preparation", 3);

    public MiningOperationChunk executeOperation(@NotNull RoleAnalysisClusterType cluster, boolean fullProcess,
            RoleAnalysisProcessModeType mode, ModelService modelService, OperationResult result, Task task) {
        if (fullProcess) {
            return resolveFullStructures(cluster, mode, modelService, result, task);
        } else {
            return resolvePartialStructures(cluster, mode, modelService, result, task);
        }
    }

    private MiningOperationChunk resolvePartialStructures(@NotNull RoleAnalysisClusterType cluster,
            RoleAnalysisProcessModeType mode, ModelService modelService, OperationResult result, Task task) {

        if (mode.equals(RoleAnalysisProcessModeType.USER)) {
            return preparePartialRoleBasedStructure(cluster, modelService, result, handler, task);

        } else if (mode.equals(RoleAnalysisProcessModeType.ROLE)) {
            return preparePartialUserBasedStructure(cluster, modelService, result, handler, task);
        }

        return new MiningOperationChunk(new ArrayList<>(), new ArrayList<>());
    }

    private MiningOperationChunk resolveFullStructures(@NotNull RoleAnalysisClusterType cluster,
            RoleAnalysisProcessModeType mode, ModelService pageBase, OperationResult result, Task task) {

        if (mode.equals(RoleAnalysisProcessModeType.USER)) {
            return prepareUserBasedStructure(cluster, pageBase, result, handler, task);
        } else if (mode.equals(RoleAnalysisProcessModeType.ROLE)) {
            return prepareRoleBasedStructure(cluster, pageBase, result, handler, task);
        }

        return new MiningOperationChunk(new ArrayList<>(), new ArrayList<>());
    }

    @Override
    public MiningOperationChunk prepareRoleBasedStructure(@NotNull RoleAnalysisClusterType cluster, ModelService modelService,
            OperationResult result, RoleAnalysisProgressIncrement handler, Task task) {

        Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();
        Map<String, PrismObject<RoleType>> roleExistCache = new HashMap<>();

        List<MiningUserTypeChunk> miningUserTypeChunks = new ArrayList<>();
        List<MiningRoleTypeChunk> miningRoleTypeChunks = new ArrayList<>();

        List<ObjectReferenceType> rolesElements = cluster.getMember();
        ListMultimap<String, String> userChunk = ArrayListMultimap.create();
        Set<String> membersOidSet = new HashSet<>();

        int rolesCount = rolesElements.size();
        handler.setActive(true);
        handler.enterNewStep("Prepare Role Structure");
        handler.setOperationCountToProcess(rolesCount);
        for (ObjectReferenceType rolesElement : rolesElements) {
            handler.iterateActualStatus();

            String roleId = rolesElement.getOid();
            PrismObject<RoleType> role = cacheRole(modelService, result, roleExistCache, roleId, task);
            if (role == null) {
                continue;
            }
            membersOidSet.add(roleId);
            //TODO add filter
            List<PrismObject<UserType>> userMembers = extractRoleMembers(null, result, modelService, roleId, task);
            List<String> users = extractOid(userMembers);

            String chunkName = role.getName().toString();

            miningRoleTypeChunks.add(new MiningRoleTypeChunk(Collections.singletonList(roleId),
                    users, chunkName, 0, RoleAnalysisOperationMode.NEUTRAL));

            for (String user : users) {
                userChunk.putAll(user, Collections.singletonList(roleId));
            }

        }

        int userChunkSize = userChunk.size();

        handler.enterNewStep("Map Users");
        handler.setOperationCountToProcess(miningRoleTypeChunks.size());
        for (MiningRoleTypeChunk chunk : miningRoleTypeChunks) {
            handler.iterateActualStatus();

            chunk.setFrequency((chunk.getUsers().size() / (double) userChunkSize));
        }

        int memberCount = membersOidSet.size();

        handler.enterNewStep("Prepare User Structure");
        handler.setOperationCountToProcess(userChunk.size());
        for (String key : userChunk.keySet()) {
            handler.iterateActualStatus();

            List<String> roleIds = userChunk.get(key);
            roleIds.retainAll(membersOidSet);
            double frequency = Math.min(roleIds.size() / (double) memberCount, 1);
            PrismObject<UserType> user = cacheUser(modelService, result, userExistCache, key, task);
            String chunkName = "NOT FOUND";
            if (user != null) {
                chunkName = user.getName().toString();
            }

            miningUserTypeChunks.add(new MiningUserTypeChunk(Collections.singletonList(key), roleIds, chunkName, frequency,
                    RoleAnalysisOperationMode.NEUTRAL));

        }

        return new MiningOperationChunk(miningUserTypeChunks, miningRoleTypeChunks);
    }

    @Override
    public MiningOperationChunk prepareUserBasedStructure(@NotNull RoleAnalysisClusterType cluster, ModelService modelService,
            OperationResult result, RoleAnalysisProgressIncrement handler, Task task) {

        Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();
        Map<String, PrismObject<RoleType>> roleExistCache = new HashMap<>();

        List<MiningUserTypeChunk> miningUserTypeChunks = new ArrayList<>();
        List<MiningRoleTypeChunk> miningRoleTypeChunks = new ArrayList<>();

        ListMultimap<String, String> roleChunk = ArrayListMultimap.create();
        Set<String> membersOidSet = new HashSet<>();

        List<ObjectReferenceType> members = cluster.getMember();
        int usersCount = members.size();

        handler.setActive(true);
        handler.enterNewStep("Prepare User Structure");
        handler.setOperationCountToProcess(usersCount);
        for (int i = 0; i < usersCount; i++) {
            handler.iterateActualStatus();

            String userOid = members.get(i).getOid();
            PrismObject<UserType> user = cacheUser(modelService, result, userExistCache, userOid, task);
            if (user == null) {continue;}

            String chunkName = "NOT FOUND";
            if (user.getName() != null) {
                chunkName = user.getName().toString();
            }
            membersOidSet.add(userOid);

            List<String> roleOids = getRolesOidAssignment(user.asObjectable());
            List<String> existingRolesAssignment = new ArrayList<>();
            for (String roleId : roleOids) {
                PrismObject<RoleType> role = cacheRole(modelService, result, roleExistCache, roleId, task);
                if (role == null) {continue;}
                existingRolesAssignment.add(roleId);
                roleChunk.putAll(roleId, Collections.singletonList(user.getOid()));

            }

            miningUserTypeChunks.add(new MiningUserTypeChunk(Collections.singletonList(userOid), existingRolesAssignment, chunkName,
                    0, RoleAnalysisOperationMode.NEUTRAL));

        }

        int roleChunkSize = roleChunk.size();

        handler.enterNewStep("Map Roles");
        handler.setOperationCountToProcess(usersCount);
        for (MiningUserTypeChunk chunk : miningUserTypeChunks) {
            handler.iterateActualStatus();

            chunk.setFrequency((chunk.getRoles().size() / (double) roleChunkSize));
        }

        int memberCount = membersOidSet.size();

        handler.enterNewStep("Prepare Role Structure");
        handler.setOperationCountToProcess(usersCount);
        for (String key : roleChunk.keySet()) {
            handler.iterateActualStatus();

            List<String> userOids = roleChunk.get(key);
            double frequency = Math.min(userOids.size() / (double) memberCount, 1);

            PrismObject<RoleType> role = cacheRole(modelService, result, roleExistCache, key, task);
            String chunkName = "NOT FOUND";
            if (role != null) {
                chunkName = role.getName().toString();
            }

            miningRoleTypeChunks.add(new MiningRoleTypeChunk(Collections.singletonList(key), userOids, chunkName, frequency,
                    RoleAnalysisOperationMode.NEUTRAL));

        }

        return new MiningOperationChunk(miningUserTypeChunks, miningRoleTypeChunks);
    }

    @Override
    public MiningOperationChunk preparePartialRoleBasedStructure(@NotNull RoleAnalysisClusterType cluster,
            ModelService modelService, OperationResult result, RoleAnalysisProgressIncrement handler, Task task) {

        Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();
        Map<String, PrismObject<RoleType>> roleExistCache = new HashMap<>();

        List<MiningUserTypeChunk> miningUserTypeChunks = new ArrayList<>();
        List<MiningRoleTypeChunk> miningRoleTypeChunks = new ArrayList<>();

        List<ObjectReferenceType> rolesElements = cluster.getMember();
        ListMultimap<List<String>, String> userChunk = ArrayListMultimap.create();
        ListMultimap<String, String> roleMap = ArrayListMultimap.create();
        Set<String> membersOidSet = new HashSet<>();

        handler.setActive(true);
        handler.enterNewStep("Map Roles");
        handler.setOperationCountToProcess(rolesElements.size());
        for (ObjectReferenceType objectReferenceType : rolesElements) {
            handler.iterateActualStatus();

            String oid = objectReferenceType.getOid();
            PrismObject<RoleType> role = cacheRole(modelService, result, roleExistCache, oid, task);
            if (role == null) {continue;}
            membersOidSet.add(oid);

            //TODO add filter
            List<PrismObject<UserType>> userMembers = extractRoleMembers(null, result, modelService, oid, task);
            List<String> users = extractOid(userMembers);
            Collections.sort(users);

            userChunk.putAll(users, Collections.singletonList(oid));
            for (String roleId : users) {
                roleMap.putAll(roleId, Collections.singletonList(oid));
            }
        }

        //user //role
        ListMultimap<List<String>, String> roleChunk = ArrayListMultimap.create();
        handler.enterNewStep("Map Users");
        handler.setOperationCountToProcess(roleMap.size());
        for (String key : roleMap.keySet()) {
            handler.iterateActualStatus();

            List<String> values = roleMap.get(key);
            roleChunk.put(values, key);
        }

        int memberCount = membersOidSet.size();

        handler.enterNewStep("Prepare User Structure");
        handler.setOperationCountToProcess(roleChunk.size());
        for (List<String> key : roleChunk.keySet()) {
            handler.iterateActualStatus();

            List<String> users = roleChunk.get(key);
            key.retainAll(membersOidSet);
            int size = key.size();
            double frequency = Math.min(size / (double) memberCount, 1);
            int userSize = users.size();
            String chunkName = "Group (" + userSize + " Users)";
            if (userSize == 1) {
                PrismObject<UserType> user = cacheUser(modelService, result, userExistCache, users.get(0), task);
                chunkName = "NOT FOUND";
                if (user != null) {
                    chunkName = user.getName().toString();
                }
            }
            miningUserTypeChunks.add(new MiningUserTypeChunk(users, key, chunkName, frequency, RoleAnalysisOperationMode.NEUTRAL));
        }
        return new MiningOperationChunk(miningUserTypeChunks, miningRoleTypeChunks);
    }

    @Override
    public MiningOperationChunk preparePartialUserBasedStructure(@NotNull RoleAnalysisClusterType cluster,
            ModelService modelService, OperationResult result, RoleAnalysisProgressIncrement handler, Task task) {
        Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();
        Map<String, PrismObject<RoleType>> roleExistCache = new HashMap<>();

        List<MiningUserTypeChunk> miningUserTypeChunks = new ArrayList<>();
        List<MiningRoleTypeChunk> miningRoleTypeChunks = new ArrayList<>();

        ListMultimap<List<String>, String> userChunk = ArrayListMultimap.create();
        ListMultimap<String, String> roleMap = ArrayListMultimap.create();
        Set<String> membersOidSet = new HashSet<>();

        List<ObjectReferenceType> members = cluster.getMember();

        handler.setActive(true);
        handler.enterNewStep("Map Users");
        handler.setOperationCountToProcess(members.size());
        for (ObjectReferenceType objectReferenceType : members) {
            handler.iterateActualStatus();

            String oid = objectReferenceType.getOid();
            PrismObject<UserType> user = cacheUser(modelService, result, userExistCache, oid, task);
            if (user == null) {
                continue;
            }
            membersOidSet.add(oid);

            List<String> rolesOid = getRolesOidAssignment(user.asObjectable());
            List<String> existingRolesAssignment = new ArrayList<>();
            for (String roleId : rolesOid) {
                PrismObject<RoleType> role = cacheRole(modelService, result, roleExistCache, roleId, task);
                if (role == null) {continue;}
                existingRolesAssignment.add(roleId);
                roleMap.putAll(roleId, Collections.singletonList(oid));
            }

            Collections.sort(existingRolesAssignment);
            userChunk.putAll(existingRolesAssignment, Collections.singletonList(oid));

        }

        //user //role
        ListMultimap<List<String>, String> roleChunk = ArrayListMultimap.create();

        handler.enterNewStep("Map Roles");
        handler.setOperationCountToProcess(roleMap.size());
        for (String key : roleMap.keySet()) {
            handler.iterateActualStatus();

            List<String> values = roleMap.get(key);
            roleChunk.put(values, key);
        }

        int memberCount = membersOidSet.size();

        handler.enterNewStep("Prepare Role Structure");
        handler.setOperationCountToProcess(roleChunk.size());
        for (List<String> key : roleChunk.keySet()) {
            handler.iterateActualStatus();

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
            miningRoleTypeChunks.add(new MiningRoleTypeChunk(roles, key, chunkName, frequency, RoleAnalysisOperationMode.NEUTRAL));
        }
        return new MiningOperationChunk(miningUserTypeChunks, miningRoleTypeChunks);
    }

}

