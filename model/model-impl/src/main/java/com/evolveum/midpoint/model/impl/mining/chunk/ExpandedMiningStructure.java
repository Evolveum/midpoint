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

/**
 * This class is responsible for preparing the expanded structure for role analysis in the Midpoint system.
 * It creates data structures used in the analysis process, such as users and roles data for further processing.
 */
public class ExpandedMiningStructure extends BasePrepareAction {

    RoleAnalysisProgressIncrement handler = new RoleAnalysisProgressIncrement("Data Preparation", 3);

    public MiningOperationChunk executeOperation(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisClusterType cluster,
            boolean fullProcess,
            @NotNull RoleAnalysisProcessModeType mode,
            @NotNull OperationResult result,
            @NotNull Task task) {
        return this.executeAction(roleAnalysisService, cluster, fullProcess, mode, handler, task, result);
    }

    public @NotNull MiningOperationChunk prepareRoleBasedStructure(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull RoleAnalysisProgressIncrement handler,
            @NotNull Task task,
            @NotNull OperationResult result) {

        Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();
        Map<String, PrismObject<RoleType>> roleExistCache = new HashMap<>();
        List<MiningUserTypeChunk> miningUserTypeChunks = new ArrayList<>();
        List<MiningRoleTypeChunk> miningRoleTypeChunks = new ArrayList<>();
        ListMultimap<String, String> userChunk = ArrayListMultimap.create();
        List<ObjectReferenceType> members = cluster.getMember();
        Set<String> membersOidSet = new HashSet<>();

        int membersCount = members.size();
        handler.setActive(true);
        handler.enterNewStep("Prepare Role Structure");
        handler.setOperationCountToProcess(membersCount);

        for (ObjectReferenceType member : members) {
            handler.iterateActualStatus();
            String memberOid = member.getOid();
            PrismObject<RoleType> role = roleAnalysisService.cacheRoleTypeObject(roleExistCache, memberOid, task, result);

            if (role != null) {
                membersOidSet.add(memberOid);
            }
        }

        ListMultimap<String, String> mapRoleMembers = roleAnalysisService
                .extractUserTypeMembers(userExistCache, null, membersOidSet, task, result);

        for (String clusterMember : membersOidSet) {
            List<String> users = mapRoleMembers.get(clusterMember);

            PrismObject<RoleType> roleTypePrismObject = roleExistCache.get(clusterMember);
            String chunkName = roleTypePrismObject.getName().toString();

            miningRoleTypeChunks.add(new MiningRoleTypeChunk(
                    Collections.singletonList(clusterMember),
                    users,
                    chunkName,
                    0,
                    RoleAnalysisOperationMode.EXCLUDE
            ));

            users.forEach(user -> userChunk.putAll(user, Collections.singletonList(clusterMember)));
        }

        int userChunkSize = userChunk.size();

        handler.enterNewStep("Map Frequency");
        handler.setOperationCountToProcess(miningRoleTypeChunks.size());
        for (MiningRoleTypeChunk chunk : miningRoleTypeChunks) {
            handler.iterateActualStatus();
            chunk.setFrequency((chunk.getUsers().size() / (double) userChunkSize));
        }

        int memberCount = membersOidSet.size();

        handler.enterNewStep("Prepare User Structure");
        handler.setOperationCountToProcess(userChunk.keySet().size());
        for (String key : userChunk.keySet()) {
            handler.iterateActualStatus();

            List<String> roleIds = userChunk.get(key);
            roleIds.retainAll(membersOidSet);
            double frequency = Math.min(roleIds.size() / (double) memberCount, 1);
            PrismObject<UserType> user = roleAnalysisService
                    .cacheUserTypeObject(userExistCache, key, task, result);
            String chunkName = "NOT FOUND";
            if (user != null) {
                chunkName = user.getName().toString();
            }

            miningUserTypeChunks.add(new MiningUserTypeChunk(Collections.singletonList(key), roleIds, chunkName, frequency,
                    RoleAnalysisOperationMode.EXCLUDE));

        }

        return new MiningOperationChunk(miningUserTypeChunks, miningRoleTypeChunks);
    }

    @Override
    public @NotNull MiningOperationChunk prepareUserBasedStructure(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull RoleAnalysisProgressIncrement handler,
            @NotNull Task task,
            @NotNull OperationResult result) {

        Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();
        Map<String, PrismObject<RoleType>> roleExistCache = new HashMap<>();

        List<MiningUserTypeChunk> miningUserTypeChunks = new ArrayList<>();
        List<MiningRoleTypeChunk> miningRoleTypeChunks = new ArrayList<>();

        ListMultimap<String, String> roleChunk = ArrayListMultimap.create();
        List<ObjectReferenceType> members = cluster.getMember();
        Set<String> membersOidSet = new HashSet<>();

        int membersCount = members.size();
        handler.setActive(true);
        handler.enterNewStep("Prepare User Structure");
        handler.setOperationCountToProcess(membersCount);
        for (ObjectReferenceType member : members) {
            handler.iterateActualStatus();

            String userOid = member.getOid();
            PrismObject<UserType> user = roleAnalysisService.cacheUserTypeObject(userExistCache, userOid, task, result);
            if (user == null) {
                continue;
            }

            String chunkName = "NOT FOUND";
            if (user.getName() != null) {
                chunkName = user.getName().toString();
            }
            membersOidSet.add(userOid);

            List<String> rolesOidAssignment = getRolesOidAssignment(user.asObjectable());
            List<String> existingRolesAssignment = new ArrayList<>();
            for (String roleId : rolesOidAssignment) {
                PrismObject<RoleType> role = roleAnalysisService.cacheRoleTypeObject(roleExistCache, roleId, task, result);
                if (role == null) {
                    continue;
                }
                existingRolesAssignment.add(roleId);
                roleChunk.putAll(roleId, Collections.singletonList(user.getOid()));

            }

            miningUserTypeChunks.add(new MiningUserTypeChunk(Collections.singletonList(userOid), existingRolesAssignment,
                    chunkName, 0, RoleAnalysisOperationMode.EXCLUDE));

        }

        int roleChunkSize = roleChunk.size();

        handler.enterNewStep("Map Roles");
        handler.setOperationCountToProcess(membersCount);
        for (MiningUserTypeChunk chunk : miningUserTypeChunks) {
            handler.iterateActualStatus();
            chunk.setFrequency((chunk.getRoles().size() / (double) roleChunkSize));
        }

        int memberCount = membersOidSet.size();

        handler.enterNewStep("Prepare Role Structure");
        handler.setOperationCountToProcess(membersCount);
        for (String key : roleChunk.keySet()) {
            handler.iterateActualStatus();

            List<String> usersOidList = roleChunk.get(key);
            double frequency = Math.min(usersOidList.size() / (double) memberCount, 1);

            PrismObject<RoleType> role = roleAnalysisService
                    .cacheRoleTypeObject(roleExistCache, key, task, result);
            String chunkName = "NOT FOUND";
            if (role != null) {
                chunkName = role.getName().toString();
            }

            miningRoleTypeChunks.add(new MiningRoleTypeChunk(Collections.singletonList(key), usersOidList, chunkName, frequency,
                    RoleAnalysisOperationMode.EXCLUDE));

        }

        return new MiningOperationChunk(miningUserTypeChunks, miningRoleTypeChunks);
    }

    @Override
    public @NotNull MiningOperationChunk preparePartialRoleBasedStructure(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull RoleAnalysisProgressIncrement handler,
            @NotNull Task task,
            @NotNull OperationResult result) {

        Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();
        Map<String, PrismObject<RoleType>> roleExistCache = new HashMap<>();

        List<MiningUserTypeChunk> miningUserTypeChunks = new ArrayList<>();
        List<MiningRoleTypeChunk> miningRoleTypeChunks = new ArrayList<>();

        ListMultimap<List<String>, String> userChunk = ArrayListMultimap.create();
        ListMultimap<String, String> roleMap = ArrayListMultimap.create();
        List<ObjectReferenceType> members = cluster.getMember();
        Set<String> membersOidSet = new HashSet<>();
        loadRoleMap(roleAnalysisService, members, roleExistCache, userExistCache, membersOidSet, userChunk, roleMap);

        //user //role
        ListMultimap<List<String>, String> roleChunk = prepareRoleChunkMap(roleMap.size(), roleMap);

        int memberCount = membersOidSet.size();
        resolveUserTypeChunk(roleAnalysisService, membersOidSet, memberCount, roleChunk, userExistCache, miningUserTypeChunks);

        return new MiningOperationChunk(miningUserTypeChunks, miningRoleTypeChunks);
    }

    @Override
    public @NotNull MiningOperationChunk preparePartialUserBasedStructure(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull RoleAnalysisProgressIncrement handler,
            @NotNull Task task,
            @NotNull OperationResult result) {
        Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();
        Map<String, PrismObject<RoleType>> roleExistCache = new HashMap<>();

        List<MiningUserTypeChunk> miningUserTypeChunks = new ArrayList<>();
        List<MiningRoleTypeChunk> miningRoleTypeChunks = new ArrayList<>();

        ListMultimap<List<String>, String> userChunk = ArrayListMultimap.create();
        ListMultimap<String, String> roleMap = ArrayListMultimap.create();
        List<ObjectReferenceType> members = cluster.getMember();
        Set<String> membersOidSet = new HashSet<>();

        loadUserChunk(roleAnalysisService, members, userExistCache, membersOidSet, roleExistCache, roleMap, userChunk);
        //user //role
        ListMultimap<List<String>, String> roleChunk = prepareRoleChunkMap(roleMap.size(), roleMap);

        int memberCount = membersOidSet.size();
        resolveRoleTypeChunk(
                roleAnalysisService, roleChunk, memberCount, membersOidSet, roleExistCache, miningRoleTypeChunks);
        return new MiningOperationChunk(miningUserTypeChunks, miningRoleTypeChunks);
    }

}

