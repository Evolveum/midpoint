/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.chunk;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.getRolesOidAssignment;
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
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class PrepareChunkStructure extends BasePrepareAction {

    RoleAnalysisProgressIncrement handler = new RoleAnalysisProgressIncrement("Data Preparation", 4);

    public MiningOperationChunk executeOperation(@NotNull RoleAnalysisClusterType cluster, boolean fullProcess,
            RoleAnalysisProcessModeType mode, ModelService modelService, OperationResult result, Task task) {
        return this.executeAction(modelService, cluster, fullProcess, mode, handler, task, result);
    }

    @Override
    public MiningOperationChunk prepareRoleBasedStructure(@NotNull RoleAnalysisClusterType cluster, ModelService modelService,
            RoleAnalysisProgressIncrement handler, Task task, OperationResult result) {
        Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();
        Map<String, PrismObject<RoleType>> roleExistCache = new HashMap<>();
        List<MiningUserTypeChunk> miningUserTypeChunks = new ArrayList<>();
        List<MiningRoleTypeChunk> miningRoleTypeChunks = new ArrayList<>();
        ListMultimap<List<String>, String> userChunk = ArrayListMultimap.create();
        ListMultimap<String, String> roleMap = ArrayListMultimap.create();
        Set<String> membersOidSet = new HashSet<>();

        List<ObjectReferenceType> members = cluster.getMember();
        loadRoleMap(members, roleExistCache, membersOidSet, userChunk, roleMap);

        int roleMapSize = roleMap.size();
        ListMultimap<List<String>, String> roleChunk = prepareRoleChunkMap(roleMapSize, roleMap);

        resolveRoleTypeChunk(userChunk, roleMapSize, membersOidSet, roleExistCache, miningRoleTypeChunks);

        resolveUserTypeChunk(membersOidSet, membersOidSet.size(), roleChunk, userExistCache, miningUserTypeChunks);

        return new MiningOperationChunk(miningUserTypeChunks, miningRoleTypeChunks);
    }

    @Override
    public MiningOperationChunk prepareUserBasedStructure(@NotNull RoleAnalysisClusterType cluster, ModelService modelService,
            RoleAnalysisProgressIncrement handler, Task task, OperationResult result) {
        Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();
        Map<String, PrismObject<RoleType>> roleExistCache = new HashMap<>();
        List<MiningUserTypeChunk> miningUserTypeChunks = new ArrayList<>();
        List<MiningRoleTypeChunk> miningRoleTypeChunks = new ArrayList<>();
        ListMultimap<List<String>, String> userChunk = ArrayListMultimap.create();
        ListMultimap<String, String> roleMap = ArrayListMultimap.create();
        Set<String> membersOidSet = new HashSet<>();

        loadUserChunk(cluster.getMember(), userExistCache, membersOidSet, roleExistCache, roleMap, userChunk);
        int roleMapSize = roleMap.size();
        ListMultimap<List<String>, String> roleChunk = prepareRoleChunkMap(roleMapSize, roleMap);

        resolveUserTypeChunk(membersOidSet, roleMapSize, userChunk, userExistCache, miningUserTypeChunks);

        int memberCount = membersOidSet.size();
        resolveRoleTypeChunk(roleChunk, memberCount, membersOidSet, roleExistCache,
                miningRoleTypeChunks);

        return new MiningOperationChunk(miningUserTypeChunks, miningRoleTypeChunks);
    }

    @Override
    public MiningOperationChunk preparePartialRoleBasedStructure(@NotNull RoleAnalysisClusterType cluster,
            ModelService modelService, RoleAnalysisProgressIncrement handler, Task task, OperationResult result) {
        Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();
        Map<String, PrismObject<RoleType>> roleExistCache = new HashMap<>();
        List<MiningUserTypeChunk> miningUserTypeChunks = new ArrayList<>();
        List<MiningRoleTypeChunk> miningRoleTypeChunks = new ArrayList<>();
        ListMultimap<List<String>, String> userChunk = ArrayListMultimap.create();
        ListMultimap<String, String> roleMap = ArrayListMultimap.create();
        Set<String> membersOidSet = new HashSet<>();

        List<ObjectReferenceType> members = cluster.getMember();
        loadRoleMap(members, roleExistCache, membersOidSet, userChunk, roleMap);

        int roleMapSize = roleMap.size();
        ListMultimap<List<String>, String> roleChunk = prepareRoleChunkMap(roleMapSize, roleMap);

        int membersCount = membersOidSet.size();
        resolveUserTypeChunk(membersOidSet, membersCount, roleChunk, userExistCache, miningUserTypeChunks);
        return new MiningOperationChunk(miningUserTypeChunks, miningRoleTypeChunks);
    }

    @Override
    public MiningOperationChunk preparePartialUserBasedStructure(@NotNull RoleAnalysisClusterType cluster,
            ModelService modelService, RoleAnalysisProgressIncrement handler, Task task, OperationResult result) {
        Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();
        Map<String, PrismObject<RoleType>> roleExistCache = new HashMap<>();
        List<MiningUserTypeChunk> miningUserTypeChunks = new ArrayList<>();
        List<MiningRoleTypeChunk> miningRoleTypeChunks = new ArrayList<>();
        ListMultimap<String, String> roleMap = ArrayListMultimap.create();
        Set<String> membersOidSet = new HashSet<>();

        List<ObjectReferenceType> members = cluster.getMember();
        loadPartialUserChunk(modelService, result, handler, task, members, userExistCache, membersOidSet, roleExistCache, roleMap);
        int roleMapSize = roleMap.size();
        ListMultimap<List<String>, String> roleChunk = prepareRoleChunkMap(roleMapSize, roleMap);

        resolveRoleTypeChunk(roleChunk, membersOidSet.size(), membersOidSet, roleExistCache, miningRoleTypeChunks);

        return new MiningOperationChunk(miningUserTypeChunks, miningRoleTypeChunks);
    }

    private static void loadPartialUserChunk(ModelService modelService, OperationResult result, RoleAnalysisProgressIncrement handler,
            Task task, List<ObjectReferenceType> members, Map<String, PrismObject<UserType>> userExistCache, Set<String> membersOidSet, Map<String, PrismObject<RoleType>> roleExistCache, ListMultimap<String, String> roleMap) {
        handler.setActive(true);
        handler.enterNewStep("Map Users");
        handler.setOperationCountToProcess(members.size());
        for (ObjectReferenceType member : members) {
            handler.iterateActualStatus();

            String oid = member.getOid();
            PrismObject<UserType> user = cacheUser(modelService, userExistCache, oid, task, result);
            if (user == null) {
                continue;
            }
            membersOidSet.add(oid);
            List<String> rolesOid = getRolesOidAssignment(user.asObjectable());

            for (String roleId : rolesOid) {
                PrismObject<RoleType> role = cacheRole(modelService, roleExistCache, roleId, task, result);
                if (role == null) {continue;}
                roleMap.putAll(roleId, Collections.singletonList(oid));
            }

        }
    }

}
