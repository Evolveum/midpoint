/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.chunk;

import java.util.*;

import com.evolveum.midpoint.common.mining.objects.chunk.DisplayValueOption;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;

import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.common.mining.objects.handler.RoleAnalysisProgressIncrement;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Nullable;

/**
 * This class is responsible for preparing the chunk structure for role analysis in the Midpoint system.
 * It creates data structures used in the analysis process, such as chunks of user and role data for further processing.
 */
public class CompressedMiningStructure extends BasePrepareAction {

    RoleAnalysisProgressIncrement handler = new RoleAnalysisProgressIncrement("Data Preparation", 4);

    public MiningOperationChunk executeOperation(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisClusterType cluster,
            @Nullable SearchFilterType userSearchFilter,
            @Nullable SearchFilterType roleSearchFilter,
            @Nullable SearchFilterType assignmentSearchFilter,
            boolean fullProcess,
            @NotNull RoleAnalysisProcessModeType mode,
            @NotNull OperationResult result,
            @NotNull Task task) {
        return this.executeAction(roleAnalysisService, cluster,
                userSearchFilter,roleSearchFilter,assignmentSearchFilter,
                fullProcess, mode, handler, task, result, null);
    }

    @Override
    public @NotNull MiningOperationChunk prepareRoleBasedStructure(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisClusterType cluster,
            @Nullable SearchFilterType userSearchFilter,
            @Nullable SearchFilterType roleSearchFilter,
            @Nullable SearchFilterType assignmentSearchFilter,
            @NotNull RoleAnalysisProgressIncrement handler,
            @NotNull Task task,
            @NotNull OperationResult result,
            @Nullable DisplayValueOption option) {
        Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();
        Map<String, PrismObject<RoleType>> roleExistCache = new HashMap<>();
        List<MiningUserTypeChunk> miningUserTypeChunks = new ArrayList<>();
        List<MiningRoleTypeChunk> miningRoleTypeChunks = new ArrayList<>();
        ListMultimap<List<String>, String> userChunk = ArrayListMultimap.create();
        ListMultimap<String, String> roleMap = ArrayListMultimap.create();
        Set<String> membersOidSet = new HashSet<>();

        List<ObjectReferenceType> members = cluster.getMember();
        loadRoleMap(roleAnalysisService, userSearchFilter, members, roleExistCache, userExistCache, membersOidSet, userChunk, roleMap);

        int roleMapSize = roleMap.size();
        ListMultimap<List<String>, String> roleChunk = prepareRoleChunkMap(roleMapSize, roleMap);

        resolveRoleTypeChunk(roleAnalysisService, userChunk, roleMapSize, membersOidSet, roleExistCache, miningRoleTypeChunks);

        resolveUserTypeChunk(
                roleAnalysisService, membersOidSet, membersOidSet.size(), roleChunk, userExistCache, miningUserTypeChunks);

        return new MiningOperationChunk(miningUserTypeChunks, miningRoleTypeChunks);
    }

    @Override
    public @NotNull MiningOperationChunk prepareUserBasedStructure(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisClusterType cluster,
            @Nullable SearchFilterType userSearchFilter,
            @Nullable SearchFilterType roleSearchFilter,
            @Nullable SearchFilterType assignmentSearchFilter,
            @NotNull RoleAnalysisProgressIncrement handler,
            @NotNull Task task,
            @NotNull OperationResult result,
            @Nullable DisplayValueOption option) {
        Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();
        Map<String, PrismObject<RoleType>> roleExistCache = new HashMap<>();
        List<MiningUserTypeChunk> miningUserTypeChunks = new ArrayList<>();
        List<MiningRoleTypeChunk> miningRoleTypeChunks = new ArrayList<>();
        ListMultimap<List<String>, String> userChunk = ArrayListMultimap.create();
        ListMultimap<String, String> roleMap = ArrayListMultimap.create();
        Set<String> membersOidSet = new HashSet<>();

        loadUserChunk(roleAnalysisService, cluster.getMember(), userExistCache, membersOidSet, roleExistCache, roleMap, userChunk);
        int roleMapSize = roleMap.size();
        ListMultimap<List<String>, String> roleChunk = prepareRoleChunkMap(roleMapSize, roleMap);

        resolveUserTypeChunk(roleAnalysisService, membersOidSet, roleMapSize, userChunk, userExistCache, miningUserTypeChunks);

        int memberCount = membersOidSet.size();
        resolveRoleTypeChunk(roleAnalysisService, roleChunk, memberCount, membersOidSet, roleExistCache,
                miningRoleTypeChunks);

        return new MiningOperationChunk(miningUserTypeChunks, miningRoleTypeChunks);
    }

    @Override
    public @NotNull MiningOperationChunk preparePartialRoleBasedStructure(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisClusterType cluster,
            @Nullable SearchFilterType userSearchFilter,
            @Nullable SearchFilterType roleSearchFilter,
            @Nullable SearchFilterType assignmentSearchFilter,
            @NotNull RoleAnalysisProgressIncrement handler,
            @NotNull Task task,
            @NotNull OperationResult result) {
        Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();
        Map<String, PrismObject<RoleType>> roleExistCache = new HashMap<>();
        List<MiningUserTypeChunk> miningUserTypeChunks = new ArrayList<>();
        List<MiningRoleTypeChunk> miningRoleTypeChunks = new ArrayList<>();
        ListMultimap<List<String>, String> userChunk = ArrayListMultimap.create();
        ListMultimap<String, String> roleMap = ArrayListMultimap.create();
        Set<String> membersOidSet = new HashSet<>();

        List<ObjectReferenceType> members = cluster.getMember();
        loadRoleMap(roleAnalysisService, userSearchFilter, members, roleExistCache, userExistCache, membersOidSet, userChunk, roleMap);

        int roleMapSize = roleMap.size();
        ListMultimap<List<String>, String> roleChunk = prepareRoleChunkMap(roleMapSize, roleMap);

        int membersCount = membersOidSet.size();
        resolveUserTypeChunk(roleAnalysisService, membersOidSet, membersCount, roleChunk, userExistCache, miningUserTypeChunks);
        return new MiningOperationChunk(miningUserTypeChunks, miningRoleTypeChunks);
    }

    @Override
    public @NotNull MiningOperationChunk preparePartialUserBasedStructure(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisClusterType cluster,
            @Nullable SearchFilterType userSearchFilter,
            @Nullable SearchFilterType roleSearchFilter,
            @Nullable SearchFilterType assignmentSearchFilter,
            @NotNull RoleAnalysisProgressIncrement handler,
            @NotNull Task task,
            @NotNull OperationResult result) {
        Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();
        Map<String, PrismObject<RoleType>> roleExistCache = new HashMap<>();
        List<MiningUserTypeChunk> miningUserTypeChunks = new ArrayList<>();
        List<MiningRoleTypeChunk> miningRoleTypeChunks = new ArrayList<>();
        ListMultimap<String, String> roleMap = ArrayListMultimap.create();
        Set<String> membersOidSet = new HashSet<>();

        List<ObjectReferenceType> members = cluster.getMember();
        loadPartialUserChunk(
                roleAnalysisService, result, handler, task, members, userExistCache, membersOidSet, roleExistCache, roleMap);
        int roleMapSize = roleMap.size();
        ListMultimap<List<String>, String> roleChunk = prepareRoleChunkMap(roleMapSize, roleMap);

        resolveRoleTypeChunk(
                roleAnalysisService, roleChunk, membersOidSet.size(), membersOidSet, roleExistCache, miningRoleTypeChunks);

        return new MiningOperationChunk(miningUserTypeChunks, miningRoleTypeChunks);
    }

}

