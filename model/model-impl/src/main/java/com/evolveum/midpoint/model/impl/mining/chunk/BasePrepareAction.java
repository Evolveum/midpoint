/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.chunk;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.getRolesOidAssignment;

import java.util.*;

import com.evolveum.midpoint.common.mining.objects.chunk.DisplayValueOption;
import com.evolveum.midpoint.common.mining.utils.RoleAnalysisCacheOption;
import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.common.mining.utils.values.FrequencyItem;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;

import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.common.mining.objects.handler.RoleAnalysisProgressIncrement;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisOperationMode;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Nullable;

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

        //TODO this is incorrect, remove after decision
        ItemPath path = ItemPath.create(UserType.F_NAME);
        if(!containItemPath(appliedPaths, path)) {
            RoleAnalysisAttributeDef roleAnalysisAttributeDef = new RoleAnalysisAttributeDef(path, false, UserType.class);
            itemDef.add(roleAnalysisAttributeDef);
        }

        path = ItemPath.create(UserType.F_ASSIGNMENT);
        if(!containItemPath(appliedPaths, path)) {
            RoleAnalysisAttributeDef roleAnalysisAttributeDef = new RoleAnalysisAttributeDef(path, true, null);
            itemDef.add(roleAnalysisAttributeDef);
        }

        if(!containItemPath(appliedPaths, path)) {
            path = ItemPath.create(UserType.F_ARCHETYPE_REF);
            RoleAnalysisAttributeDef roleAnalysisAttributeDef = new RoleAnalysisAttributeDef(path, false, ArchetypeType.class);
            itemDef.add(roleAnalysisAttributeDef);
        }

        return new RoleAnalysisCacheOption(itemDef);
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

        //TODO this is incorrect, remove after decision
        ItemPath path = ItemPath.create(RoleType.F_NAME);
        if(!containItemPath(appliedPaths, path)) {
            RoleAnalysisAttributeDef roleAnalysisAttributeDef = new RoleAnalysisAttributeDef(path, false, RoleType.class);
            itemDef.add(roleAnalysisAttributeDef);
        }

        path = ItemPath.create(RoleType.F_LIFECYCLE_STATE);
        if(!containItemPath(appliedPaths, path)) {
            RoleAnalysisAttributeDef roleAnalysisAttributeDef = new RoleAnalysisAttributeDef(path, false, RoleType.class);
            itemDef.add(roleAnalysisAttributeDef);
        }

        path = ItemPath.create(RoleType.F_ARCHETYPE_REF);
        if(!containItemPath(appliedPaths, path)) {
            RoleAnalysisAttributeDef roleAnalysisAttributeDef = new RoleAnalysisAttributeDef(path, false, ArchetypeType.class);
            itemDef.add(roleAnalysisAttributeDef);
        }

        return new RoleAnalysisCacheOption(itemDef);
    }

    private boolean containItemPath(@NotNull Set<ItemPath> appliedPaths, ItemPath path) {
        for (ItemPath appliedPath : appliedPaths) {
            if(appliedPath.equivalent(path)) {
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
     * @param objectFilter The additional user filter.
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
            SearchFilterType objectFilter,
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
            return resolveFullChunkStructures(roleAnalysisService, cluster, objectFilter, mode, option);
        } else {
            return resolvePartialChunkStructures(roleAnalysisService, objectFilter, cluster, mode);
        }
    }

    @NotNull
    private MiningOperationChunk resolvePartialChunkStructures(
            @NotNull RoleAnalysisService roleAnalysisService,
            @Nullable SearchFilterType objectFilter,
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull RoleAnalysisProcessModeType mode) {
        if (mode.equals(RoleAnalysisProcessModeType.USER)) {
            return preparePartialUserBasedStructure(roleAnalysisService, cluster, objectFilter, handler, task, result);

        } else if (mode.equals(RoleAnalysisProcessModeType.ROLE)) {
            return preparePartialRoleBasedStructure(roleAnalysisService, cluster, objectFilter, handler, task, result);
        }

        return new MiningOperationChunk(new ArrayList<>(), new ArrayList<>());
    }

    @NotNull
    private MiningOperationChunk resolveFullChunkStructures(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisClusterType cluster,
            SearchFilterType objectFilter,
            @NotNull RoleAnalysisProcessModeType mode,
            @Nullable DisplayValueOption option) {
        if (mode.equals(RoleAnalysisProcessModeType.USER)) {
            return prepareUserBasedStructure(roleAnalysisService, cluster, objectFilter, handler, task, result, option);
        } else if (mode.equals(RoleAnalysisProcessModeType.ROLE)) {
            return prepareRoleBasedStructure(roleAnalysisService, cluster, objectFilter, handler, task, result, option);
        }
        return new MiningOperationChunk(new ArrayList<>(), new ArrayList<>());
    }

    protected void loadRoleMap(
            @NotNull RoleAnalysisService roleAnalysisService,
            @Nullable SearchFilterType objectFilter,
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
            PrismObject<RoleType> role = roleAnalysisService.cacheRoleTypeObject(roleExistCache, membersOid, task, result, getRoleCacheOption());
            if (role != null) {
                RoleType roleObject = role.asObjectable();
                String lifecycleState = roleObject.getLifecycleState();

                if (lifecycleState == null || lifecycleState.equals("active")) {
                    membersOidSet.add(membersOid);
                }
            }
        }

        ListMultimap<String, String> mapRoleMembers = roleAnalysisService.extractUserTypeMembers(
                userExistCache, objectFilter, membersOidSet, task, result);

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
            String iconColor = null;
            if (rolesSize == 1) {
                PrismObject<RoleType> role = roleAnalysisService.cacheRoleTypeObject(
                        roleExistCache, roles.get(0), task, result, getRoleCacheOption());
                chunkName = "NOT FOUND";
                if (role != null) {
                    chunkName = role.getName().toString();
                    iconColor = roleAnalysisService.resolveFocusObjectIconColor(role.asObjectable(), task, result);
                }
            }

            double frequency = Math.min(key.size() / membersCount, 1);
            FrequencyItem frequencyType = new FrequencyItem(frequency);

            MiningRoleTypeChunk miningRoleTypeChunk = new MiningRoleTypeChunk(roles, key, chunkName, frequencyType, RoleAnalysisOperationMode.EXCLUDE);

            if (iconColor != null) {
                miningRoleTypeChunk.setIconColor(iconColor);
            }

            miningRoleTypeChunks
                    .add(miningRoleTypeChunk);

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
            String iconColor = null;
            if (userSize == 1) {
                PrismObject<UserType> user = roleAnalysisService.cacheUserTypeObject(
                        userExistCache, users.get(0), task, result, getUserCacheOption());
                chunkName = "NOT FOUND";
                if (user != null) {
                    chunkName = user.getName().toString();
                    iconColor = roleAnalysisService.resolveFocusObjectIconColor(user.asObjectable(), task, result);
                }
            }

            MiningUserTypeChunk miningUserTypeChunk = new MiningUserTypeChunk(users, key, chunkName, new FrequencyItem(frequency), RoleAnalysisOperationMode.EXCLUDE);

            if (iconColor != null) {
                miningUserTypeChunk.setIconColor(iconColor);
            }

            miningUserTypeChunks
                    .add(miningUserTypeChunk);

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
                    .cacheUserTypeObject(userExistCache, membersOid, task, result, getUserCacheOption());
            if (user == null) {continue;}
            membersOidSet.add(membersOid);
            List<String> rolesOid = getRolesOidAssignment(user.asObjectable());

            List<String> existingRolesAssignment = new ArrayList<>();
            for (String roleId : rolesOid) {
                PrismObject<RoleType> role = roleAnalysisService
                        .cacheRoleTypeObject(roleExistCache, roleId, task, result, getRoleCacheOption());
                if (role == null) {continue;}
                RoleType roleObject = role.asObjectable();
                String lifecycleState = roleObject.getLifecycleState();

                if (lifecycleState == null || lifecycleState.equals("active")) {
                    existingRolesAssignment.add(roleId);
                    roleMap.putAll(roleId, Collections.singletonList(membersOid));
                }
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
            PrismObject<UserType> user = roleAnalysisService.cacheUserTypeObject(userExistCache, oid, task, result, getUserCacheOption());
            if (user == null) {
                continue;
            }
            membersOidSet.add(oid);
            List<String> rolesOid = getRolesOidAssignment(user.asObjectable());

            for (String roleId : rolesOid) {
                PrismObject<RoleType> role = roleAnalysisService.cacheRoleTypeObject(roleExistCache, roleId, task, result, getRoleCacheOption());
                if (role == null) {continue;}

                RoleType roleObject = role.asObjectable();
                String lifecycleState = roleObject.getLifecycleState();

                if (lifecycleState == null || lifecycleState.equals("active")) {
                    roleMap.putAll(roleId, Collections.singletonList(oid));
                }
            }

        }
    }

}
