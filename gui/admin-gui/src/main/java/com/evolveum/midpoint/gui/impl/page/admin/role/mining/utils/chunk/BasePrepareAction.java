/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.chunk;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.extractOid;
import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.getRolesOidAssignment;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.RoleAnalysisObjectUtils.extractRoleMembers;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.chunk.CacheUtils.cacheRole;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.chunk.CacheUtils.cacheUser;

import java.util.*;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.common.mining.objects.handler.RoleAnalysisProgressIncrement;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisOperationMode;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public abstract class BasePrepareAction implements MiningStructure {

    RoleAnalysisProgressIncrement handler;
    Task task;
    OperationResult result;
    ModelService modelService;

    protected MiningOperationChunk executeAction(ModelService modelService, @NotNull RoleAnalysisClusterType cluster,
            boolean fullProcess, RoleAnalysisProcessModeType mode, RoleAnalysisProgressIncrement handler,
            Task task, OperationResult result) {

        this.handler = handler;
        this.task = task;
        this.result = result;
        this.modelService = modelService;

        if (fullProcess) {
            return resolveFullChunkStructures(cluster, mode);
        } else {
            return resolvePartialChunkStructures(cluster, mode);
        }
    }

    private MiningOperationChunk resolvePartialChunkStructures(@NotNull RoleAnalysisClusterType cluster,
            RoleAnalysisProcessModeType mode) {
        if (mode.equals(RoleAnalysisProcessModeType.USER)) {
            return preparePartialUserBasedStructure(cluster, modelService, handler, task, result);

        } else if (mode.equals(RoleAnalysisProcessModeType.ROLE)) {
            return preparePartialRoleBasedStructure(cluster, modelService, handler, task, result);
        }

        return new MiningOperationChunk(new ArrayList<>(), new ArrayList<>());
    }

    private MiningOperationChunk resolveFullChunkStructures(@NotNull RoleAnalysisClusterType cluster,
            RoleAnalysisProcessModeType mode) {

        if (mode.equals(RoleAnalysisProcessModeType.USER)) {
            return prepareUserBasedStructure(cluster, modelService, handler, task, result);

        } else if (mode.equals(RoleAnalysisProcessModeType.ROLE)) {
            return prepareRoleBasedStructure(cluster, modelService, handler, task, result);
        }
        return new MiningOperationChunk(new ArrayList<>(), new ArrayList<>());
    }

    protected void loadRoleMap(List<ObjectReferenceType> members, Map<String,
            PrismObject<RoleType>> roleExistCache, Set<String> membersOidSet, ListMultimap<List<String>,
            String> userChunk, ListMultimap<String, String> roleMap) {
        handler.setActive(true);
        handler.enterNewStep("Map Roles");
        handler.setOperationCountToProcess(members.size());
        for (ObjectReferenceType member : members) {
            handler.iterateActualStatus();

            String membersOid = member.getOid();
            PrismObject<RoleType> role = cacheRole(modelService, roleExistCache, membersOid, task, result);
            if (role == null) {
                continue;
            }
            membersOidSet.add(membersOid);
            List<PrismObject<UserType>> userMembers = extractRoleMembers(modelService, null, membersOid, task, result);
            List<String> users = extractOid(userMembers);
            Collections.sort(users);

            userChunk.putAll(users, Collections.singletonList(membersOid));
            for (String userOid : users) {
                roleMap.putAll(userOid, Collections.singletonList(membersOid));
            }

        }
    }

    @NotNull
    protected ListMultimap<List<String>, String> prepareRoleChunkMap(int roleMapSize,
            ListMultimap<String, String> roleMap) {
        handler.enterNewStep("Map Role Chunk");
        handler.setOperationCountToProcess(roleMapSize);
        ListMultimap<List<String>, String> roleChunk = ArrayListMultimap.create();
        for (String key : roleMap.keySet()) {
            handler.iterateActualStatus();
            List<String> values = roleMap.get(key);
            roleChunk.put(values, key);
        }
        return roleChunk;
    }
    protected void resolveRoleTypeChunk(ListMultimap<List<String>, String> chunkMap, double membersCount, Set<String> membersOidSet, Map<String,
            PrismObject<RoleType>> roleExistCache,
            List<MiningRoleTypeChunk> miningRoleTypeChunks) {
        handler.enterNewStep("Process Role Structure");
        handler.setOperationCountToProcess(chunkMap.size());
        for (List<String> key : chunkMap.keySet()) {
            handler.iterateActualStatus();

            List<String> roles = chunkMap.get(key);
            key.retainAll(membersOidSet);

            int rolesSize = roles.size();
            String chunkName = "Group (" + rolesSize + " Roles)";
            if (rolesSize == 1) {
                PrismObject<RoleType> role = cacheRole(modelService, roleExistCache, roles.get(0), task, result);
                chunkName = "NOT FOUND";
                if (role != null) {
                    chunkName = role.getName().toString();
                }
            }

            double frequency = Math.min(key.size() / membersCount, 1);

            miningRoleTypeChunks.add(new MiningRoleTypeChunk(roles, key, chunkName, frequency, RoleAnalysisOperationMode.NEUTRAL));

        }
    }

    protected void resolveUserTypeChunk(Set<String> membersOidSet, int mapSize,
            ListMultimap<List<String>, String> propertiesChunk,
            Map<String, PrismObject<UserType>> userExistCache,
            List<MiningUserTypeChunk> miningUserTypeChunks) {
        int roleChunkSize = propertiesChunk.size();
        handler.enterNewStep("Process User Structure");
        handler.setOperationCountToProcess(roleChunkSize);
        for (List<String> key : propertiesChunk.keySet()) {
            handler.iterateActualStatus();

            List<String> users = propertiesChunk.get(key);
            key.retainAll(membersOidSet);
            int size = key.size();
            double frequency = Math.min(size / (double) mapSize, 1);
            int userSize = users.size();
            String chunkName = "Group (" + userSize + " Users)";
            if (userSize == 1) {
                PrismObject<UserType> user = CacheUtils.cacheUser(modelService, userExistCache, users.get(0), task, result);
                chunkName = "NOT FOUND";
                if (user != null) {
                    chunkName = user.getName().toString();
                }
            }
            miningUserTypeChunks.add(new MiningUserTypeChunk(users, key, chunkName, frequency, RoleAnalysisOperationMode.NEUTRAL));

        }
    }

    protected void loadUserChunk(List<ObjectReferenceType> members,
            Map<String, PrismObject<UserType>> userExistCache, Set<String> membersOidSet,
            Map<String, PrismObject<RoleType>> roleExistCache, ListMultimap<String, String> roleMap,
            ListMultimap<List<String>, String> userChunk) {
        handler.setActive(true);
        handler.enterNewStep("Map Users");
        handler.setOperationCountToProcess(members.size());
        for (ObjectReferenceType member : members) {
            handler.iterateActualStatus();

            String membersOid = member.getOid();
            PrismObject<UserType> user = cacheUser(modelService, userExistCache, membersOid, task, result);
            if (user == null) {continue;}
            membersOidSet.add(membersOid);
            List<String> rolesOid = getRolesOidAssignment(user.asObjectable());

            List<String> existingRolesAssignment = new ArrayList<>();
            for (String roleId : rolesOid) {
                PrismObject<RoleType> role = cacheRole(modelService, roleExistCache, roleId, task, result);
                if (role == null) {continue;}
                existingRolesAssignment.add(roleId);
                roleMap.putAll(roleId, Collections.singletonList(membersOid));
            }

            Collections.sort(existingRolesAssignment);
            userChunk.putAll(existingRolesAssignment, Collections.singletonList(membersOid));

        }
    }

}
