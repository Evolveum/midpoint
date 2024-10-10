/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.chunk;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.getRolesOidAssignment;

import java.util.*;

import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.common.mining.objects.chunk.DisplayValueOption;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.common.mining.objects.handler.RoleAnalysisProgressIncrement;
import com.evolveum.midpoint.common.mining.utils.values.FrequencyItem;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisOperationMode;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
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
            PrismObject<RoleType> role = roleAnalysisService.cacheRoleTypeObject(roleExistCache, memberOid, task, result, getRoleCacheOption());

            if (role != null) {
                RoleType roleObject = role.asObjectable();
                String lifecycleState = roleObject.getLifecycleState();

                if (lifecycleState == null || lifecycleState.equals("active")) {
                    membersOidSet.add(memberOid);

                }
            }
        }

        ListMultimap<String, String> mapRoleMembers = roleAnalysisService
                .extractUserTypeMembers(userExistCache, userSearchFilter, membersOidSet, task, result);

        for (String clusterMember : membersOidSet) {
            List<String> users = mapRoleMembers.get(clusterMember);

            PrismObject<RoleType> role = roleExistCache.get(clusterMember);

            if (role == null) {
                continue;
            }

            String chunkName = "unknown";
            String iconColor = roleAnalysisService.resolveFocusObjectIconColor(role.asObjectable(), task, result);

            if (option != null && option.getRoleAnalysisRoleDef() != null) {
                RoleAnalysisAttributeDef roleAnalysisRoleDef = option.getRoleAnalysisRoleDef();
                ItemPath path = roleAnalysisRoleDef.getPath();
                String value = roleAnalysisRoleDef.resolveSingleValueItem(role, path);
                if (value != null) {
                    chunkName = value;
                }
            } else if (role.getName() != null) {
                chunkName = role.getName().toString();
            }

            MiningRoleTypeChunk miningRoleTypeChunk = new MiningRoleTypeChunk(
                    Collections.singletonList(clusterMember),
                    users,
                    chunkName,
                    new FrequencyItem(0),
                    RoleAnalysisOperationMode.EXCLUDE
            );

            if (iconColor != null) {
                miningRoleTypeChunk.setIconColor(iconColor);
            }

            miningRoleTypeChunks.add(miningRoleTypeChunk);

            users.forEach(user -> userChunk.putAll(user, Collections.singletonList(clusterMember)));
        }

        int userChunkSize = userChunk.size();

        handler.enterNewStep("Map Frequency");
        handler.setOperationCountToProcess(miningRoleTypeChunks.size());
        for (MiningRoleTypeChunk chunk : miningRoleTypeChunks) {
            handler.iterateActualStatus();
            chunk.setFrequency(new FrequencyItem((chunk.getUsers().size() / (double) userChunkSize)));
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
                    .cacheUserTypeObject(userExistCache, key, task, result, null);

            String chunkName = "unknown";
            String iconColor = null;

            if (user != null) {
                iconColor = roleAnalysisService.resolveFocusObjectIconColor(user.asObjectable(), task, result);
            }

            if (user != null && option != null && option.getUserAnalysisUserDef() != null) {
                RoleAnalysisAttributeDef roleAnalysisUserDef = option.getUserAnalysisUserDef();
                ItemPath path = roleAnalysisUserDef.getPath();
                String value = roleAnalysisUserDef.resolveSingleValueItem(user, path);
                if (value != null) {
                    chunkName = value;
                }
            } else if (user != null && user.getName() != null) {
                chunkName = user.getName().toString();
            }

            MiningUserTypeChunk miningUserTypeChunk = new MiningUserTypeChunk(Collections.singletonList(key),
                    roleIds,
                    chunkName,
                    new FrequencyItem(frequency),
                    RoleAnalysisOperationMode.EXCLUDE);

            if (iconColor != null) {
                miningUserTypeChunk.setIconColor(iconColor);
            }

            miningUserTypeChunks.add(miningUserTypeChunk);

        }

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
            PrismObject<UserType> user = roleAnalysisService.cacheUserTypeObject(userExistCache, userOid, task, result, null);
            if (user == null) {
                continue;
            }

            String chunkName = "unknown";
            String iconColor = roleAnalysisService.resolveFocusObjectIconColor(user.asObjectable(), task, result);

            if (option != null && option.getUserAnalysisUserDef() != null) {
                RoleAnalysisAttributeDef roleAnalysisUserDef = option.getUserAnalysisUserDef();
                ItemPath path = roleAnalysisUserDef.getPath();
                String value = roleAnalysisUserDef.resolveSingleValueItem(user, path);
                if (value != null) {
                    chunkName = value;
                }
            } else if (user.getName() != null) {
                chunkName = user.getName().toString();
            }

            membersOidSet.add(userOid);

            List<String> rolesOidAssignment = getRolesOidAssignment(user.asObjectable());
            List<String> existingRolesAssignment = new ArrayList<>();
            for (String roleId : rolesOidAssignment) {
                PrismObject<RoleType> role = roleAnalysisService.cacheRoleTypeObject(roleExistCache, roleId, task, result, getRoleCacheOption());
                if (role == null) {
                    continue;
                }

                RoleType roleObject = role.asObjectable();
                String lifecycleState = roleObject.getLifecycleState();

                if (lifecycleState == null || lifecycleState.equals("active")) {
                    existingRolesAssignment.add(roleId);
                    roleChunk.putAll(roleId, Collections.singletonList(user.getOid()));
                }

            }

            MiningUserTypeChunk miningUserTypeChunk = new MiningUserTypeChunk(Collections.singletonList(userOid),
                    existingRolesAssignment,
                    chunkName,
                    new FrequencyItem(0),
                    RoleAnalysisOperationMode.EXCLUDE);

            if (iconColor != null) {
                miningUserTypeChunk.setIconColor(iconColor);
            }

            miningUserTypeChunks.add(miningUserTypeChunk);

        }

        int roleChunkSize = roleChunk.size();

        handler.enterNewStep("Map Roles");
        handler.setOperationCountToProcess(membersCount);
        for (MiningUserTypeChunk chunk : miningUserTypeChunks) {
            handler.iterateActualStatus();
            chunk.setFrequency(new FrequencyItem((chunk.getRoles().size() / (double) roleChunkSize)));
        }

        int memberCount = membersOidSet.size();

        handler.enterNewStep("Prepare Role Structure");
        handler.setOperationCountToProcess(membersCount);
        for (String key : roleChunk.keySet()) {
            handler.iterateActualStatus();

            List<String> usersOidList = roleChunk.get(key);
            double frequency = Math.min(usersOidList.size() / (double) memberCount, 1);

            PrismObject<RoleType> role = roleAnalysisService
                    .cacheRoleTypeObject(roleExistCache, key, task, result, getRoleCacheOption());
            String chunkName = "unknown";
            String iconColor = null;
            if (role != null) {
                iconColor = roleAnalysisService.resolveFocusObjectIconColor(role.asObjectable(), task, result);
            }

            if (role != null && option != null && option.getRoleAnalysisRoleDef() != null) {
                RoleAnalysisAttributeDef roleAnalysisRoleDef = option.getRoleAnalysisRoleDef();
                ItemPath path = roleAnalysisRoleDef.getPath();
                String value = roleAnalysisRoleDef.resolveSingleValueItem(role, path);
                if (value != null) {
                    chunkName = value;
                }
            } else if (role != null) {
                chunkName = role.getName().toString();
            }

            MiningRoleTypeChunk miningRoleTypeChunk = new MiningRoleTypeChunk(Collections.singletonList(key),
                    usersOidList,
                    chunkName,
                    new FrequencyItem(frequency),
                    RoleAnalysisOperationMode.EXCLUDE);
            if (iconColor != null) {
                miningRoleTypeChunk.setIconColor(iconColor);
            }

            miningRoleTypeChunks.add(miningRoleTypeChunk);

        }

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
        List<ObjectReferenceType> members = cluster.getMember();
        Set<String> membersOidSet = new HashSet<>();
        loadRoleMap(roleAnalysisService, userSearchFilter, members, roleExistCache, userExistCache, membersOidSet, userChunk, roleMap);

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

