/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.chunk;

import java.util.*;

import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.common.mining.objects.chunk.DisplayValueOption;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.common.mining.objects.handler.RoleAnalysisProgressIncrement;
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
            @Nullable SearchFilterType userSearchFilter,
            @Nullable SearchFilterType roleSearchFilter,
            @Nullable SearchFilterType assignmentSearchFilter,
            boolean fullProcess,
            @NotNull RoleAnalysisProcessModeType mode,
            @NotNull OperationResult result,
            @NotNull Task task,
            @Nullable DisplayValueOption option) {
        return this.executeAction(roleAnalysisService,
                cluster,
                userSearchFilter,
                roleSearchFilter,
                assignmentSearchFilter,
                fullProcess,
                mode,
                handler,
                task,
                result,
                option);
    }

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
        Set<String> roleMembers = new HashSet<>();

        cluster.getMember().forEach(member -> roleMembers.add(member.getOid()));

        //User as a key, roles as a value
        ListMultimap<String, String> expandUsersMap = roleAnalysisService.assignmentRoleMemberSearch(
                userSearchFilter, roleSearchFilter, assignmentSearchFilter,
                roleMembers, false, task, result);

        //role key, users value
        ListMultimap<String, String> expandRolesMap = ArrayListMultimap.create();
        for (String userOid : expandUsersMap.keySet()) {
            List<String> rolesOids = expandUsersMap.get(userOid);
            for (String roleOid : rolesOids) {
                expandRolesMap.put(roleOid, userOid);
            }
        }

        int allRolesInMiningStructureSize = expandRolesMap.keySet().size();
        int allUsersInMiningStructureSize = expandUsersMap.keySet().size();

        resolveUserTypeChunkExpand(
                roleAnalysisService,
                expandUsersMap, allRolesInMiningStructureSize,
                userExistCache,
                miningUserTypeChunks);

        resolveRoleTypeChunkExpanded(roleAnalysisService, expandRolesMap, allUsersInMiningStructureSize, roleExistCache,
                miningRoleTypeChunks);

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
        Set<String> userMember = new HashSet<>();

        cluster.getMember().forEach(member -> userMember.add(member.getOid()));

        //User as a key, roles as a value
        ListMultimap<String, String> expandUsersMap = roleAnalysisService.assignmentUserAccessSearch(
                userSearchFilter, roleSearchFilter, assignmentSearchFilter,
                userMember, true, task, result);

        //role key, users value
        ListMultimap<String, String> expandRolesMap = ArrayListMultimap.create();
        for (String userOid : expandUsersMap.keySet()) {
            List<String> rolesOids = expandUsersMap.get(userOid);
            for (String roleOid : rolesOids) {
                expandRolesMap.put(roleOid, userOid);
            }
        }

        int allRolesInMiningStructureSize = expandRolesMap.keySet().size();
        int allUsersInMiningStructureSize = expandUsersMap.keySet().size();

        resolveUserTypeChunkExpand(
                roleAnalysisService,
                expandUsersMap, allRolesInMiningStructureSize,
                userExistCache,
                miningUserTypeChunks);

        resolveRoleTypeChunkExpanded(roleAnalysisService, expandRolesMap, allUsersInMiningStructureSize, roleExistCache,
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
        Set<String> rolesMembers = new HashSet<>();

        cluster.getMember().forEach(member -> rolesMembers.add(member.getOid()));

        //User as a key, roles as a value
        ListMultimap<String, String> expandedUsersMap = roleAnalysisService.assignmentRoleMemberSearch(
                userSearchFilter, roleSearchFilter, assignmentSearchFilter,
                rolesMembers, false, task, result);

        Set<String> allRolesInMiningStructure = new HashSet<>();
        for (String userOid : expandedUsersMap.keySet()) {
            List<String> rolesOids = expandedUsersMap.get(userOid);
            allRolesInMiningStructure.addAll(rolesOids);
        }

        int allRolesInMiningStructureSize = allRolesInMiningStructure.size();

        resolveUserTypeChunkExpand(
                roleAnalysisService,
                expandedUsersMap, allRolesInMiningStructureSize,
                userExistCache,
                miningUserTypeChunks);

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
        Set<String> userMember = new HashSet<>();

        cluster.getMember().forEach(member -> userMember.add(member.getOid()));

        //Roles as a key, users as a value
        ListMultimap<String, String> expandeRolesMap = roleAnalysisService.assignmentUserAccessSearch(
                userSearchFilter, roleSearchFilter, assignmentSearchFilter,
                userMember, false, task, result);

        Set<String> allUsersInMiningStructure = new HashSet<>();
        for (String roleOid : expandeRolesMap.keySet()) {
            allUsersInMiningStructure.addAll(expandeRolesMap.get(roleOid));
        }

        int allUsersInMiningStructureSize = allUsersInMiningStructure.size();

        resolveRoleTypeChunkExpanded(roleAnalysisService, expandeRolesMap, allUsersInMiningStructureSize, roleExistCache,
                miningRoleTypeChunks);

        return new MiningOperationChunk(miningUserTypeChunks, miningRoleTypeChunks);
    }

}

