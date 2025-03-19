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
                userSearchFilter, roleSearchFilter, assignmentSearchFilter,
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
        Set<String> rolesMembers = new HashSet<>();

        cluster.getMember().forEach(member -> rolesMembers.add(member.getOid()));

        //User as a key, roles as a value
        ListMultimap<String, String> userRolesMap = roleAnalysisService.assignmentRoleMemberSearch(
                userSearchFilter, roleSearchFilter, assignmentSearchFilter,
                rolesMembers, false, task, result, cluster);


        pullMigratedRoles(roleAnalysisService,
                cluster,
                userSearchFilter,
                assignmentSearchFilter,
                task,
                result,
                userRolesMap);

        //Roles as a key, users as a value
        ListMultimap<String, String> roleUserMap = ArrayListMultimap.create();
        Set<String> allRolesInMiningStructure = new HashSet<>();

        //roles key, users value
        ListMultimap<List<String>, String> compressedUsers = ArrayListMultimap.create();
        for (String userOid : userRolesMap.keySet()) {
            List<String> rolesOids = userRolesMap.get(userOid);
            allRolesInMiningStructure.addAll(rolesOids);
            Collections.sort(rolesOids);
            for (String roleOid : rolesOids) {
                roleUserMap.put(roleOid, userOid);
            }
            compressedUsers.put(rolesOids, userOid);
        }

        //users key, roles value
        ListMultimap<List<String>, String> compressedRoles = ArrayListMultimap.create();
        for (String rolesOid : roleUserMap.keySet()) {
            List<String> usersOids = roleUserMap.get(rolesOid);
            Collections.sort(usersOids);
            compressedRoles.put(usersOids, rolesOid);
        }

        int allRolesInMiningStructureSize = allRolesInMiningStructure.size();
        int allUsersInMiningStructureSize = userRolesMap.keySet().size();

        resolveUserTypeChunkCompress(
                roleAnalysisService,
                compressedUsers, allRolesInMiningStructureSize,
                userExistCache,
                miningUserTypeChunks);

        resolveRoleTypeChunkCompress(roleAnalysisService, compressedRoles, allUsersInMiningStructureSize, roleExistCache,
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
        ListMultimap<String, String> userRolesMap = roleAnalysisService.assignmentUserAccessSearch(
                userSearchFilter, roleSearchFilter, assignmentSearchFilter,
                userMember, true, task, result, cluster);
        //Roles as a key, users as a value
        ListMultimap<String, String> roleUserMap = ArrayListMultimap.create();

        Set<String> allRolesInMiningStructure = new HashSet<>();

        List<RoleAnalysisCandidateRoleType> candidateRoles = cluster.getCandidateRoles();
        Set<String> candidateRolesOids =  collectCandidateRolesOidToExclude(candidateRoles);

        //roles key, users value
        ListMultimap<List<String>, String> compressedUsers = ArrayListMultimap.create();
        for (String userOid : userRolesMap.keySet()) {
            List<String> rolesOids = userRolesMap.get(userOid);

            //exclude candidate roles
            List<String> filteredRoles = new ArrayList<>(rolesOids.size());
            for (String roleOid : rolesOids) {
                if (!candidateRolesOids.contains(roleOid)) {
                    filteredRoles.add(roleOid);
                    roleUserMap.put(roleOid, userOid);
                }
            }

            if(filteredRoles.isEmpty()) {
                continue;
            }

            Collections.sort(filteredRoles);

            allRolesInMiningStructure.addAll(filteredRoles);
            compressedUsers.put(filteredRoles, userOid);
        }

        //users key, roles value
        ListMultimap<List<String>, String> compressedRoles = ArrayListMultimap.create();
        for (String rolesOid : roleUserMap.keySet()) {
            List<String> usersOids = roleUserMap.get(rolesOid);
            Collections.sort(usersOids);
            compressedRoles.put(usersOids, rolesOid);
        }

        int allRolesInMiningStructureSize = allRolesInMiningStructure.size();
        int allUsersInMiningStructureSize = userRolesMap.keySet().size();

        resolveUserTypeChunkCompress(
                roleAnalysisService,
                compressedUsers, allRolesInMiningStructureSize,
                userExistCache,
                miningUserTypeChunks);

        resolveRoleTypeChunkCompress(roleAnalysisService, compressedRoles, allUsersInMiningStructureSize, roleExistCache,
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

        List<MiningUserTypeChunk> miningUserTypeChunks = new ArrayList<>();
        List<MiningRoleTypeChunk> miningRoleTypeChunks = new ArrayList<>();
        Set<String> rolesMembers = new HashSet<>();

        cluster.getMember().forEach(member -> rolesMembers.add(member.getOid()));

        //User as a key, roles as a value
        ListMultimap<String, String> userRolesMap = roleAnalysisService.assignmentRoleMemberSearch(
                userSearchFilter, roleSearchFilter, assignmentSearchFilter,
                rolesMembers, false, task, result, cluster);

        Set<String> allRolesInMiningStructure = new HashSet<>();

        //roles key, users value
        ListMultimap<List<String>, String> compressedUsers = ArrayListMultimap.create();
        for (String userOid : userRolesMap.keySet()) {
            List<String> rolesOids = userRolesMap.get(userOid);
            Collections.sort(rolesOids);
            allRolesInMiningStructure.addAll(rolesOids);
            compressedUsers.put(rolesOids, userOid);
        }

        int allRolesInMiningStructureSize = allRolesInMiningStructure.size();

        resolveUserTypeChunkCompress(
                roleAnalysisService,
                compressedUsers, allRolesInMiningStructureSize,
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
        Map<String, PrismObject<RoleType>> roleExistCache = new HashMap<>();

        List<MiningUserTypeChunk> miningUserTypeChunks = new ArrayList<>();
        List<MiningRoleTypeChunk> miningRoleTypeChunks = new ArrayList<>();
        Set<String> userMember = new HashSet<>();

        cluster.getMember().forEach(member -> userMember.add(member.getOid()));

        //Roles as a key, users as a value
        ListMultimap<String, String> rolesUserMap = roleAnalysisService.assignmentUserAccessSearch(
                userSearchFilter, roleSearchFilter, assignmentSearchFilter,
                userMember, false, task, result, cluster);

        Set<String> allUsersInMiningStructure = new HashSet<>();
        //users key, roles value
        ListMultimap<List<String>, String> compressedRoles = ArrayListMultimap.create();
        for (String rolesOid : rolesUserMap.keySet()) {
            List<String> usersOids = rolesUserMap.get(rolesOid);
            Collections.sort(usersOids);
            allUsersInMiningStructure.addAll(usersOids);
            compressedRoles.put(usersOids, rolesOid);
        }

        int allUsersInMiningStructureSize = allUsersInMiningStructure.size();

        resolveRoleTypeChunkCompress(roleAnalysisService, compressedRoles, allUsersInMiningStructureSize, roleExistCache,
                miningRoleTypeChunks);

        return new MiningOperationChunk(miningUserTypeChunks, miningRoleTypeChunks);
    }

}

