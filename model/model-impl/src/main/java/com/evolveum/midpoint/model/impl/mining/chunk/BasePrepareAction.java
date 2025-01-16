/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.chunk;

import java.util.*;

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
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

public abstract class BasePrepareAction implements MiningStructure {

    RoleAnalysisProgressIncrement handler;
    Task task;
    OperationResult result;

    DisplayValueOption option;

    public RoleAnalysisCacheOption getUserCacheOption() {
        return userCacheOption;
    }

    public RoleAnalysisCacheOption getRoleCacheOption() {
        return roleCacheOption;
    }

    //TODO - add the correct path for the attributes
    RoleAnalysisCacheOption userCacheOption = generateUserCacheOption();
    RoleAnalysisCacheOption roleCacheOption = generateRoleCacheOption();

    public RoleAnalysisCacheOption generateUserCacheOption() {
        List<RoleAnalysisAttributeDef> itemDef = new ArrayList<>();

        //TODO only for expanded structure
        if (option != null) {
            RoleAnalysisAttributeDef userItemValuePath = option.getUserAnalysisUserDef();
            if (userItemValuePath != null) {
                itemDef.add(userItemValuePath);
            }
        }

        Set<ItemPath> appliedPaths = new HashSet<>();
        for (RoleAnalysisAttributeDef roleAnalysisAttributeDef : itemDef) {
            appliedPaths.add(roleAnalysisAttributeDef.getPath());
        }

        PrismObjectDefinition<UserType> userDef = PrismContext.get().getSchemaRegistry().findObjectDefinitionByType(UserType.COMPLEX_TYPE);
        //TODO this is incorrect, remove after decision
        ItemPath path = ItemPath.create(ObjectType.F_NAME);
        if (!containItemPath(appliedPaths, path)) {
            RoleAnalysisAttributeDef roleAnalysisAttributeDef = createAnalysisAttributeDef(userDef, path);
            itemDef.add(roleAnalysisAttributeDef);
        }

        //TODO assignment?
        path = ItemPath.create(AssignmentHolderType.F_ASSIGNMENT);
        if (!containItemPath(appliedPaths, path)) {
            RoleAnalysisAttributeDef roleAnalysisAttributeDef = createAnalysisAttributeDef(userDef, path);
            itemDef.add(roleAnalysisAttributeDef);
        }

        if (!containItemPath(appliedPaths, path)) {
            path = ItemPath.create(AssignmentHolderType.F_ARCHETYPE_REF);
            RoleAnalysisAttributeDef roleAnalysisAttributeDef = createAnalysisAttributeDef(userDef, path);
            itemDef.add(roleAnalysisAttributeDef);
        }

        return new RoleAnalysisCacheOption(itemDef);
    }

    private RoleAnalysisAttributeDef createAnalysisAttributeDef(PrismObjectDefinition<?> objectDef, ItemPath path) {
        return new RoleAnalysisAttributeDef(path, objectDef.findItemDefinition(path), (Class<? extends FocusType>) objectDef.getCompileTimeClass());
    }

    public RoleAnalysisCacheOption generateRoleCacheOption() {
        List<RoleAnalysisAttributeDef> itemDef = new ArrayList<>();

        //TODO only for expanded structure
        if (option != null) {
            RoleAnalysisAttributeDef roleAnalysisRoleDef = option.getRoleAnalysisRoleDef();
            if (roleAnalysisRoleDef != null) {
                itemDef.add(roleAnalysisRoleDef);
            }
        }

        Set<ItemPath> appliedPaths = new HashSet<>();
        for (RoleAnalysisAttributeDef roleAnalysisAttributeDef : itemDef) {
            appliedPaths.add(roleAnalysisAttributeDef.getPath());
        }

        PrismObjectDefinition<RoleType> roleDef = PrismContext.get().getSchemaRegistry().findObjectDefinitionByType(RoleType.COMPLEX_TYPE);
        //TODO this is incorrect, remove after decision
        ItemPath path = ItemPath.create(ObjectType.F_NAME);
        if (!containItemPath(appliedPaths, path)) {
            RoleAnalysisAttributeDef roleAnalysisAttributeDef = createAnalysisAttributeDef(roleDef, path);
            itemDef.add(roleAnalysisAttributeDef);
        }

        path = ItemPath.create(ObjectType.F_LIFECYCLE_STATE);
        if (!containItemPath(appliedPaths, path)) {
            RoleAnalysisAttributeDef roleAnalysisAttributeDef = createAnalysisAttributeDef(roleDef, path);
            itemDef.add(roleAnalysisAttributeDef);
        }

        path = ItemPath.create(AssignmentHolderType.F_ARCHETYPE_REF);
        if (!containItemPath(appliedPaths, path)) {
            RoleAnalysisAttributeDef roleAnalysisAttributeDef = createAnalysisAttributeDef(roleDef, path);
            itemDef.add(roleAnalysisAttributeDef);
        }

        return new RoleAnalysisCacheOption(itemDef);
    }

    private boolean containItemPath(@NotNull Set<ItemPath> appliedPaths, ItemPath path) {
        for (ItemPath appliedPath : appliedPaths) {
            if (appliedPath.equivalent(path)) {
                return true;
            }
        }
        return false;
    }

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

        return new MiningOperationChunk(new ArrayList<>(), new ArrayList<>());
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
        return new MiningOperationChunk(new ArrayList<>(), new ArrayList<>());
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

            int usersCount = usersOids.size();
            int rolesCount = rolesOids.size();

            double frequency = Math.min(usersCount / allUsersInMiningStructureSize, 1);
            FrequencyItem frequencyType = new FrequencyItem(frequency);

            String chunkName = "'" + rolesCount + "' Roles";
            String iconColor = null;
            if (rolesCount == 1) {
                PrismObject<RoleType> role = roleAnalysisService.cacheRoleTypeObject(
                        roleExistCache, rolesOids.get(0), task, result, getRoleCacheOption());
                chunkName = "NOT FOUND";
                if (role != null) {
                    chunkName = role.getName().toString();
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
    }

    protected void resolveRoleTypeChunkExpanded(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull ListMultimap<String, String> expandRolesMap,
            double allUsersInMiningStructureSize,
            @NotNull Map<String,
                    PrismObject<RoleType>> roleExistCache,
            @NotNull List<MiningRoleTypeChunk> miningRoleTypeChunks) {
        handler.enterNewStep("Process Role Structure");
        handler.setOperationCountToProcess(expandRolesMap.size());

        for (String roleOid : expandRolesMap.keySet()) {
            handler.iterateActualStatus();
            List<String> usersOids = expandRolesMap.get(roleOid);

            int usersCount = usersOids.size();

            double frequency = Math.min(usersCount / allUsersInMiningStructureSize, 1);
            FrequencyItem frequencyType = new FrequencyItem(frequency);

            String chunkName = "NOT FOUND";
            String iconColor = null;
            PrismObject<RoleType> role = roleAnalysisService.cacheRoleTypeObject(
                    roleExistCache, roleOid, task, result, getRoleCacheOption());
            if (role != null) {
                chunkName = role.getName().toString();
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

            int usersCount = usersOids.size();
            int rolesCount = rolesOids.size();

            double frequency = Math.min(rolesCount / (double) allRolesInMiningStructureSize, 1);

            String chunkName = "'" + usersCount + "' Users";
            String iconColor = null;
            if (usersCount == 1) {
                PrismObject<UserType> user = roleAnalysisService.cacheUserTypeObject(
                        userExistCache, usersOids.get(0), task, result, getUserCacheOption());
                chunkName = "NOT FOUND";
                if (user != null) {
                    chunkName = user.getName().toString();
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
    }

    protected void resolveUserTypeChunkExpand(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull ListMultimap<String, String> expandedUsersMap,
            int allRolesInMiningStructureSize,
            @NotNull Map<String, PrismObject<UserType>> userExistCache,
            @NotNull List<MiningUserTypeChunk> miningUserTypeChunks) {

        int compressedUsersSize = expandedUsersMap.size();
        handler.enterNewStep("Process User Structure");
        handler.setOperationCountToProcess(compressedUsersSize);

        for (String userOid : expandedUsersMap.keySet()) {
            handler.iterateActualStatus();
            List<String> rolesOids = expandedUsersMap.get(userOid);

            int rolesCount = rolesOids.size();

            double frequency = Math.min(rolesCount / (double) allRolesInMiningStructureSize, 1);

            String chunkName = "NOT FOUND";
            String iconColor = null;
            PrismObject<UserType> user = roleAnalysisService.cacheUserTypeObject(
                    userExistCache, userOid, task, result, getUserCacheOption());
            if (user != null) {
                chunkName = user.getName().toString();
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

}
