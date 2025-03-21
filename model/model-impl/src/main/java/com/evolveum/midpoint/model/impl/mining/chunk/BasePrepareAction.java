/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.chunk;

import java.util.*;

import com.evolveum.midpoint.schema.constants.SchemaConstants;

import com.google.common.collect.ListMultimap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.common.mining.objects.chunk.DisplayValueOption;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.common.mining.objects.handler.RoleAnalysisProgressIncrement;
import com.evolveum.midpoint.common.mining.utils.RoleAnalysisCacheOption;
import com.evolveum.midpoint.common.mining.utils.values.FrequencyItem;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisOperationMode;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import static com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef.getRoleAnalysisArchetypeDef;
import static com.evolveum.midpoint.model.impl.mining.chunk.MiningChunkUtils.*;

public abstract class BasePrepareAction implements MiningStructure {

    RoleAnalysisProgressIncrement handler;
    Task task;
    OperationResult result;

    DisplayValueOption option;
    RoleAnalysisCacheOption userCacheOption = generateUserCacheOption();
    RoleAnalysisCacheOption roleCacheOption = generateRoleCacheOption();

    /**
     * Executes the action for preparing the mining structure based on the specified cluster and mode.
     *
     * @param roleAnalysisService The role analysis service for performing the operation.
     * @param cluster The role analysis cluster to process.
     * @param userSearchFilter The user search filter.
     * @param roleSearchFilter The role search filter.
     * @param assignmentSearchFilter The assignment search filter.
     * @param fullProcess Indicates whether a full process should be performed.
     * @param mode The role analysis process Mode.
     * @param handler The progress increment handler.
     * @param task The task associated with this operation.
     * @param result The operation result.
     * @param option Display options chunk preparation.
     * @return The MiningOperationChunk containing the prepared structure.
     */
    @NotNull
    protected MiningOperationChunk executeAction(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisClusterType cluster,
            @Nullable SearchFilterType userSearchFilter,
            @Nullable SearchFilterType roleSearchFilter,
            @Nullable SearchFilterType assignmentSearchFilter,
            boolean fullProcess,
            @NotNull RoleAnalysisProcessModeType mode,
            @NotNull RoleAnalysisProgressIncrement handler,
            @NotNull Task task,
            @NotNull OperationResult result,
            @Nullable DisplayValueOption option) {

        this.handler = handler;
        this.task = task;
        this.result = result;
        this.option = option;
        this.userCacheOption = generateUserCacheOption();
        this.roleCacheOption = generateRoleCacheOption();

        if (fullProcess) {
            return resolveFullChunkStructures(roleAnalysisService, cluster,
                    userSearchFilter, roleSearchFilter, assignmentSearchFilter,
                    mode, option);
        } else {
            return resolvePartialChunkStructures(roleAnalysisService,
                    userSearchFilter, roleSearchFilter, assignmentSearchFilter,
                    cluster, mode);
        }
    }

    @NotNull
    private MiningOperationChunk resolvePartialChunkStructures(
            @NotNull RoleAnalysisService roleAnalysisService,
            @Nullable SearchFilterType userSearchFilter,
            @Nullable SearchFilterType roleSearchFilter,
            @Nullable SearchFilterType assignmentSearchFilter,
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull RoleAnalysisProcessModeType mode) {
        if (mode.equals(RoleAnalysisProcessModeType.USER)) {
            return preparePartialUserBasedStructure(roleAnalysisService, cluster,
                    userSearchFilter, roleSearchFilter, assignmentSearchFilter,
                    handler, task, result);

        } else if (mode.equals(RoleAnalysisProcessModeType.ROLE)) {
            return preparePartialRoleBasedStructure(roleAnalysisService, cluster,
                    userSearchFilter, roleSearchFilter, assignmentSearchFilter,
                    handler, task, result);
        }

        return new MiningOperationChunk(new ArrayList<>(), new ArrayList<>(), null);
    }

    @NotNull
    private MiningOperationChunk resolveFullChunkStructures(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisClusterType cluster,
            @Nullable SearchFilterType userSearchFilter,
            @Nullable SearchFilterType roleSearchFilter,
            @Nullable SearchFilterType assignmentSearchFilter,
            @NotNull RoleAnalysisProcessModeType mode,
            @Nullable DisplayValueOption option) {
        if (mode.equals(RoleAnalysisProcessModeType.USER)) {
            return prepareUserBasedStructure(roleAnalysisService, cluster,
                    userSearchFilter, roleSearchFilter, assignmentSearchFilter,
                    handler, task, result, option);
        } else if (mode.equals(RoleAnalysisProcessModeType.ROLE)) {
            return prepareRoleBasedStructure(roleAnalysisService, cluster,
                    userSearchFilter, roleSearchFilter, assignmentSearchFilter,
                    handler, task, result, option);
        }
        return new MiningOperationChunk(new ArrayList<>(), new ArrayList<>(), null);
    }

    protected void resolveRoleTypeChunkCompress(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull ListMultimap<List<String>, String> compressedUsers,
            double allUsersInMiningStructureSize,
            @NotNull Map<String,
                    PrismObject<RoleType>> roleExistCache,
            @NotNull List<MiningRoleTypeChunk> miningRoleTypeChunks) {
        handler.enterNewStep("Process Role Structure");
        handler.setOperationCountToProcess(compressedUsers.size());

        for (List<String> usersOids : compressedUsers.keySet()) {
            handler.iterateActualStatus();
            List<String> rolesOids = compressedUsers.get(usersOids);

            processRolesChunk(roleAnalysisService,
                    allUsersInMiningStructureSize,
                    roleExistCache,
                    miningRoleTypeChunks,
                    usersOids,
                    rolesOids);

        }
    }

    private void processRolesChunk(@NotNull RoleAnalysisService roleAnalysisService,
            double allUsersInMiningStructureSize,
            @NotNull Map<String, PrismObject<RoleType>> roleExistCache,
            @NotNull List<MiningRoleTypeChunk> miningRoleTypeChunks,
            @NotNull List<String> usersOids,
            @NotNull List<String> rolesOids) {
        int usersCount = usersOids.size();
        int rolesCount = rolesOids.size();

        double frequency = Math.min(usersCount / allUsersInMiningStructureSize, 1);
        FrequencyItem frequencyType = new FrequencyItem(frequency);

        String chunkName = getRoleChunkName(rolesCount);
        String iconColor = null;
        if (rolesCount == 1) {
            PrismObject<RoleType> role = roleAnalysisService.cacheRoleTypeObject(
                    roleExistCache, rolesOids.get(0), task, result, getRoleCacheOption());
            chunkName = resolveSingleMemberChunkName(role, option);
            if (role != null) {
                iconColor = roleAnalysisService.resolveFocusObjectIconColor(role.asObjectable(), task, result);
            }
        }

        MiningRoleTypeChunk miningRoleTypeChunk = new MiningRoleTypeChunk(
                rolesOids, usersOids, chunkName, frequencyType, RoleAnalysisOperationMode.EXCLUDE);

        if (iconColor != null) {
            miningRoleTypeChunk.setIconColor(iconColor);
        }

        miningRoleTypeChunks
                .add(miningRoleTypeChunk);
    }

    protected void resolveRoleTypeChunkExpanded(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull ListMultimap<String, String> expandRolesMap,
            double allUsersInMiningStructureSize,
            @NotNull Map<String,
                    PrismObject<RoleType>> roleExistCache,
            @NotNull List<MiningRoleTypeChunk> miningRoleTypeChunks, @Nullable DisplayValueOption option) {
        handler.enterNewStep("Process Role Structure");
        handler.setOperationCountToProcess(expandRolesMap.size());

        for (String roleOid : expandRolesMap.keySet()) {
            handler.iterateActualStatus();
            List<String> usersOids = expandRolesMap.get(roleOid);

            int usersCount = usersOids.size();

            double frequency = Math.min(usersCount / allUsersInMiningStructureSize, 1);
            FrequencyItem frequencyType = new FrequencyItem(frequency);

            String iconColor = null;
            PrismObject<RoleType> role = roleAnalysisService.cacheRoleTypeObject(
                    roleExistCache, roleOid, task, result, getRoleCacheOption());
            String chunkName = resolveSingleMemberChunkName(role, option);
            if (role != null) {
                iconColor = roleAnalysisService.resolveFocusObjectIconColor(role.asObjectable(), task, result);
            }

            MiningRoleTypeChunk miningRoleTypeChunk = new MiningRoleTypeChunk(
                    Collections.singletonList(roleOid), usersOids, chunkName, frequencyType, RoleAnalysisOperationMode.EXCLUDE);

            if (iconColor != null) {
                miningRoleTypeChunk.setIconColor(iconColor);
            }

            miningRoleTypeChunks
                    .add(miningRoleTypeChunk);

        }
    }

    protected void resolveUserTypeChunkCompress(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull ListMultimap<List<String>, String> compressedUsers,
            int allRolesInMiningStructureSize,
            @NotNull Map<String, PrismObject<UserType>> userExistCache,
            @NotNull List<MiningUserTypeChunk> miningUserTypeChunks) {

        int compressedUsersSize = compressedUsers.size();
        handler.enterNewStep("Process User Structure");
        handler.setOperationCountToProcess(compressedUsersSize);

        for (List<String> rolesOids : compressedUsers.keySet()) {
            handler.iterateActualStatus();
            List<String> usersOids = compressedUsers.get(rolesOids);

            processUsersChunk(roleAnalysisService, allRolesInMiningStructureSize, userExistCache, miningUserTypeChunks, rolesOids, usersOids);

        }
    }

    private void processUsersChunk(@NotNull RoleAnalysisService roleAnalysisService,
            double allRolesInMiningStructureSize,
            @NotNull Map<String, PrismObject<UserType>> userExistCache,
            @NotNull List<MiningUserTypeChunk> miningUserTypeChunks,
            @NotNull List<String> rolesOids,
            @NotNull List<String> usersOids) {
        int usersCount = usersOids.size();
        int rolesCount = rolesOids.size();

        double frequency = Math.min(rolesCount / allRolesInMiningStructureSize, 1);

        String chunkName = getUserChunkName(usersCount);
        String iconColor = null;
        if (usersCount == 1) {
            PrismObject<UserType> user = roleAnalysisService.cacheUserTypeObject(
                    userExistCache, usersOids.get(0), task, result, getUserCacheOption());
            chunkName = resolveSingleMemberChunkName(user, option);
            if (user != null) {
                iconColor = roleAnalysisService.resolveFocusObjectIconColor(user.asObjectable(), task, result);
            }
        }

        MiningUserTypeChunk miningUserTypeChunk = new MiningUserTypeChunk(
                usersOids, rolesOids, chunkName, new FrequencyItem(frequency), RoleAnalysisOperationMode.EXCLUDE);

        if (iconColor != null) {
            miningUserTypeChunk.setIconColor(iconColor);
        }

        miningUserTypeChunks
                .add(miningUserTypeChunk);
    }

    protected void resolveUserTypeChunkExpand(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull ListMultimap<String, String> expandedUsersMap,
            int allRolesInMiningStructureSize,
            @NotNull Map<String, PrismObject<UserType>> userExistCache,
            @NotNull List<MiningUserTypeChunk> miningUserTypeChunks,
            @Nullable DisplayValueOption option) {

        int compressedUsersSize = expandedUsersMap.size();
        handler.enterNewStep("Process User Structure");
        handler.setOperationCountToProcess(compressedUsersSize);

        for (String userOid : expandedUsersMap.keySet()) {
            handler.iterateActualStatus();
            List<String> rolesOids = expandedUsersMap.get(userOid);

            int rolesCount = rolesOids.size();

            double frequency = Math.min(rolesCount / (double) allRolesInMiningStructureSize, 1);

            String iconColor = null;
            PrismObject<UserType> user = roleAnalysisService.cacheUserTypeObject(
                    userExistCache, userOid, task, result, getUserCacheOption());
            String chunkName = resolveSingleMemberChunkName(user, option);
            if (user != null) {
                iconColor = roleAnalysisService.resolveFocusObjectIconColor(user.asObjectable(), task, result);
            }

            MiningUserTypeChunk miningUserTypeChunk = new MiningUserTypeChunk(
                    Collections.singletonList(userOid), rolesOids, chunkName, new FrequencyItem(frequency), RoleAnalysisOperationMode.EXCLUDE);

            if (iconColor != null) {
                miningUserTypeChunk.setIconColor(iconColor);
            }

            miningUserTypeChunks
                    .add(miningUserTypeChunk);

        }
    }

    protected static @NotNull Set<String> collectCandidateRolesOidToExclude(@NotNull RoleAnalysisService roleAnalysisService, List<RoleAnalysisCandidateRoleType> candidateRoles, @NotNull Task task, @NotNull OperationResult result) {
        Set<String> candidateRolesOids = new HashSet<>();
        if (candidateRoles != null) {
            candidateRoles.forEach(candidateRole -> {
                if (isMigratedOrActiveRole(roleAnalysisService, candidateRole, task, result)) {
                    return;
                }

                String candidateRoleOid = candidateRole.getCandidateRoleRef().getOid();
                candidateRolesOids.add(candidateRoleOid);

            });
        }
        return candidateRolesOids;
    }

    private static boolean isMigratedOrActiveRole(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisCandidateRoleType candidateRole,
            @NotNull Task task,
            @NotNull OperationResult result) {

        RoleAnalysisOperationType operationChannel = Optional.ofNullable(candidateRole.getOperationStatus())
                .map(RoleAnalysisOperationStatusType::getOperationChannel)
                .orElse(null);

        if (Objects.equals(operationChannel, RoleAnalysisOperationType.MIGRATION)) {
            return true;
        }

        ObjectReferenceType candidateRoleRef = candidateRole.getCandidateRoleRef();
        if (candidateRoleRef == null || candidateRoleRef.getOid() == null) {
            return false;
        }

        PrismObject<RoleType> roleTypeObject = roleAnalysisService.getRoleTypeObject(candidateRoleRef.getOid(), task, result);
        if (roleTypeObject == null) {
            return false;
        }

        String lifecycleState = roleTypeObject.asObjectable().getLifecycleState();
        return Objects.equals(lifecycleState, SchemaConstants.LIFECYCLE_ACTIVE);
    }


    protected static void pullMigratedRoles(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisClusterType cluster,
            @Nullable SearchFilterType userSearchFilter,
            @Nullable SearchFilterType assignmentSearchFilter,
            @NotNull Task task,
            @NotNull OperationResult result,
            ListMultimap<String, String> expandUsersMap) {
        Set<String> migratedRoles = new HashSet<>();
        List<ObjectReferenceType> resolvedPattern = cluster.getResolvedPattern();
        if (resolvedPattern == null) {
            return;
        }
        resolvedPattern.forEach(pattern -> migratedRoles.add(pattern.getOid()));

        if (!migratedRoles.isEmpty()) {
            ListMultimap<String, String> userRolesMapMigrated = roleAnalysisService.assignmentRoleMemberSearch(
                    userSearchFilter, null, assignmentSearchFilter,
                    migratedRoles, false, task, result, cluster);
            expandUsersMap.putAll(userRolesMapMigrated);
        }
    }

    /**
     * Generates a {@link RoleAnalysisCacheOption} instance containing cache-specific properties
     * used for managing the expanded user-permission table. This method processes the current
     * {@code option} object to retrieve relevant attribute definitions that influence the
     * display value for specific user.
     * This method is designed to handle an expanded structure, with the specific chunkName attribute selected.
     */
    private @Nullable RoleAnalysisCacheOption generateUserCacheOption() {
        List<RoleAnalysisAttributeDef> itemDef;

        if (option != null) {
            itemDef = new ArrayList<>();
            RoleAnalysisAttributeDef userItemValuePath = option.getUserAnalysisUserDef();
            if (userItemValuePath != null) {
                itemDef.add(userItemValuePath);
            }
            if (!itemDef.isEmpty()) {
                itemDef.add(getRoleAnalysisArchetypeDef(UserType.class));
                return new RoleAnalysisCacheOption(itemDef);
            }
        }

        return null;
    }

    /**
     * Generates a {@link RoleAnalysisCacheOption} instance containing cache-specific properties
     * used for managing the expanded user-permission table. This method processes the current
     * {@code option} object to retrieve relevant attribute definitions that influence the
     * display value for specific role.
     * This method is designed to handle an expanded structure, with the specific chunkName attribute selected.
     */
    private @Nullable RoleAnalysisCacheOption generateRoleCacheOption() {
        List<RoleAnalysisAttributeDef> itemDef;

        //TODO only for expanded structure
        if (option != null) {
            itemDef = new ArrayList<>();
            RoleAnalysisAttributeDef roleAnalysisRoleDef = option.getRoleAnalysisRoleDef();
            if (roleAnalysisRoleDef != null) {
                itemDef.add(roleAnalysisRoleDef);
            }

            if (!itemDef.isEmpty()) {
                itemDef.add(getRoleAnalysisArchetypeDef(RoleType.class));
                return new RoleAnalysisCacheOption(itemDef);
            }
        }

        return null;
    }

    protected RoleAnalysisCacheOption getUserCacheOption() {
        return userCacheOption;
    }

    protected RoleAnalysisCacheOption getRoleCacheOption() {
        return roleCacheOption;
    }
}
