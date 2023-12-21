/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.chunk;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.getRolesOidAssignment;

import java.util.*;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.common.mining.objects.handler.RoleAnalysisProgressIncrement;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisOperationMode;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public abstract class BasePrepareAction implements MiningStructure {

    RoleAnalysisProgressIncrement handler;
    Task task;
    OperationResult result;

    /**
     * Executes the action for preparing the mining structure based on the specified cluster and mode.
     *
     * @param roleAnalysisService The role analysis service for performing the operation.
     * @param cluster The role analysis cluster to process.
     * @param fullProcess Indicates whether a full process should be performed.
     * @param mode The role analysis process Mode.
     * @param handler The progress increment handler.
     * @param task The task associated with this operation.
     * @param result The operation result.
     * @return The MiningOperationChunk containing the prepared structure.
     */
    @NotNull
    protected MiningOperationChunk executeAction(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisClusterType cluster,
            boolean fullProcess,
            @NotNull RoleAnalysisProcessModeType mode,
            @NotNull RoleAnalysisProgressIncrement handler,
            @NotNull Task task,
            @NotNull OperationResult result) {

        this.handler = handler;
        this.task = task;
        this.result = result;

        if (fullProcess) {
            return resolveFullChunkStructures(roleAnalysisService, cluster, mode);
        } else {
            return resolvePartialChunkStructures(roleAnalysisService, cluster, mode);
        }
    }

    @NotNull
    private MiningOperationChunk resolvePartialChunkStructures(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull RoleAnalysisProcessModeType mode) {
        if (mode.equals(RoleAnalysisProcessModeType.USER)) {
            return preparePartialUserBasedStructure(roleAnalysisService, cluster, handler, task, result);

        } else if (mode.equals(RoleAnalysisProcessModeType.ROLE)) {
            return preparePartialRoleBasedStructure(roleAnalysisService, cluster, handler, task, result);
        }

        return new MiningOperationChunk(new ArrayList<>(), new ArrayList<>());
    }

    @NotNull
    private MiningOperationChunk resolveFullChunkStructures(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull RoleAnalysisProcessModeType mode) {

        if (mode.equals(RoleAnalysisProcessModeType.USER)) {
            return prepareUserBasedStructure(roleAnalysisService, cluster, handler, task, result);

        } else if (mode.equals(RoleAnalysisProcessModeType.ROLE)) {
            return prepareRoleBasedStructure(roleAnalysisService, cluster, handler, task, result);
        }
        return new MiningOperationChunk(new ArrayList<>(), new ArrayList<>());
    }

    protected void loadRoleMap(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull List<ObjectReferenceType> members,
            @NotNull Map<String, PrismObject<RoleType>> roleExistCache,
            @NotNull Map<String, PrismObject<UserType>> userExistCache,
            @NotNull Set<String> membersOidSet,
            @NotNull ListMultimap<List<String>, String> userChunk,
            @NotNull ListMultimap<String, String> roleMap) {
        handler.setActive(true);
        handler.enterNewStep("Map Roles");
        handler.setOperationCountToProcess(members.size());

        for (ObjectReferenceType member : members) {
            handler.iterateActualStatus();

            String membersOid = member.getOid();
            PrismObject<RoleType> role = roleAnalysisService.cacheRoleTypeObject(roleExistCache, membersOid, task, result);
            if (role != null) {
                membersOidSet.add(membersOid);
            }
        }

        ListMultimap<String, String> mapRoleMembers = roleAnalysisService.extractUserTypeMembers(
                userExistCache, null, membersOidSet, task, result);

        for (String clusterMember : membersOidSet) {
            List<String> users = mapRoleMembers.get(clusterMember);
            Collections.sort(users);
            userChunk.putAll(users, Collections.singletonList(clusterMember));
            users.forEach(userOid -> roleMap.putAll(userOid, Collections.singletonList(clusterMember)));
        }
    }

    @NotNull
    protected ListMultimap<List<String>, String> prepareRoleChunkMap(int roleMapSize,
            @NotNull ListMultimap<String, String> roleMap) {
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

    protected void resolveRoleTypeChunk(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull ListMultimap<List<String>, String> chunkMap,
            double membersCount,
            @NotNull Set<String> membersOidSet,
            @NotNull Map<String,
                    PrismObject<RoleType>> roleExistCache,
            @NotNull List<MiningRoleTypeChunk> miningRoleTypeChunks) {
        handler.enterNewStep("Process Role Structure");
        handler.setOperationCountToProcess(chunkMap.size());

        for (List<String> key : chunkMap.keySet()) {
            handler.iterateActualStatus();

            List<String> roles = chunkMap.get(key);
            key.retainAll(membersOidSet);

            int rolesSize = roles.size();
            String chunkName = "'" + rolesSize + "' Roles";
            if (rolesSize == 1) {
                PrismObject<RoleType> role = roleAnalysisService.cacheRoleTypeObject(
                        roleExistCache, roles.get(0), task, result);
                chunkName = "NOT FOUND";
                if (role != null) {
                    chunkName = role.getName().toString();
                }
            }

            double frequency = Math.min(key.size() / membersCount, 1);

            miningRoleTypeChunks
                    .add(new MiningRoleTypeChunk(roles, key, chunkName, frequency, RoleAnalysisOperationMode.EXCLUDE));

        }
    }

    protected void resolveUserTypeChunk(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull Set<String> membersOidSet,
            int mapSize,
            @NotNull ListMultimap<List<String>, String> propertiesChunk,
            @NotNull Map<String, PrismObject<UserType>> userExistCache,
            @NotNull List<MiningUserTypeChunk> miningUserTypeChunks) {
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
            String chunkName = "'" + userSize + "' Users";
            if (userSize == 1) {
                PrismObject<UserType> user = roleAnalysisService.cacheUserTypeObject(
                        userExistCache, users.get(0), task, result);
                chunkName = "NOT FOUND";
                if (user != null) {
                    chunkName = user.getName().toString();
                }
            }
            miningUserTypeChunks
                    .add(new MiningUserTypeChunk(users, key, chunkName, frequency, RoleAnalysisOperationMode.EXCLUDE));

        }
    }

    protected void loadUserChunk(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull List<ObjectReferenceType> members,
            @NotNull Map<String, PrismObject<UserType>> userExistCache,
            @NotNull Set<String> membersOidSet,
            @NotNull Map<String, PrismObject<RoleType>> roleExistCache,
            @NotNull ListMultimap<String, String> roleMap,
            @NotNull ListMultimap<List<String>, String> userChunk) {
        handler.setActive(true);
        handler.enterNewStep("Map Users");
        handler.setOperationCountToProcess(members.size());
        for (ObjectReferenceType member : members) {
            handler.iterateActualStatus();

            String membersOid = member.getOid();
            PrismObject<UserType> user = roleAnalysisService
                    .cacheUserTypeObject(userExistCache, membersOid, task, result);
            if (user == null) {continue;}
            membersOidSet.add(membersOid);
            List<String> rolesOid = getRolesOidAssignment(user.asObjectable());

            List<String> existingRolesAssignment = new ArrayList<>();
            for (String roleId : rolesOid) {
                PrismObject<RoleType> role = roleAnalysisService
                        .cacheRoleTypeObject(roleExistCache, roleId, task, result);
                if (role == null) {continue;}
                existingRolesAssignment.add(roleId);
                roleMap.putAll(roleId, Collections.singletonList(membersOid));
            }

            Collections.sort(existingRolesAssignment);
            userChunk.putAll(existingRolesAssignment, Collections.singletonList(membersOid));

        }
    }

    protected void loadPartialUserChunk(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull OperationResult result,
            @NotNull RoleAnalysisProgressIncrement handler,
            @NotNull Task task,
            @NotNull List<ObjectReferenceType> members,
            @NotNull Map<String, PrismObject<UserType>> userExistCache,
            @NotNull Set<String> membersOidSet,
            @NotNull Map<String, PrismObject<RoleType>> roleExistCache,
            @NotNull ListMultimap<String, String> roleMap) {
        handler.setActive(true);
        handler.enterNewStep("Map Users");
        handler.setOperationCountToProcess(members.size());
        for (ObjectReferenceType member : members) {
            handler.iterateActualStatus();

            String oid = member.getOid();
            PrismObject<UserType> user = roleAnalysisService.cacheUserTypeObject(userExistCache, oid, task, result);
            if (user == null) {
                continue;
            }
            membersOidSet.add(oid);
            List<String> rolesOid = getRolesOidAssignment(user.asObjectable());

            for (String roleId : rolesOid) {
                PrismObject<RoleType> role = roleAnalysisService.cacheRoleTypeObject(roleExistCache, roleId, task, result);
                if (role == null) {continue;}
                roleMap.putAll(roleId, Collections.singletonList(oid));
            }

        }
    }

}
