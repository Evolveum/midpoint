/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.chunk;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.*;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.chunk.CacheUtils.*;

import java.io.Serializable;
import java.util.*;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.utils.Handler;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.MiningOperationChunk;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.MiningRoleTypeChunk;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.MiningUserTypeChunk;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class PrepareChunkStructure implements MiningStructure, Serializable {

    Handler handler = new Handler("Data Preparation", 4);

    public MiningOperationChunk executeOperation(@NotNull RoleAnalysisClusterType cluster, boolean fullProcess,
            RoleAnalysisProcessModeType mode, PageBase pageBase, OperationResult result) {
        if (fullProcess) {
            return resolveFullChunkStructures(cluster, mode, pageBase, result);
        } else {
            return resolvePartialChunkStructures(cluster, mode, pageBase, result);
        }
    }

    private MiningOperationChunk resolvePartialChunkStructures(@NotNull RoleAnalysisClusterType cluster,
            RoleAnalysisProcessModeType mode, PageBase pageBase, OperationResult result) {
        if (mode.equals(RoleAnalysisProcessModeType.USER)) {
            return preparePartialUserBasedStructure(cluster, pageBase, result, handler);

        } else if (mode.equals(RoleAnalysisProcessModeType.ROLE)) {
            return preparePartialRoleBasedStructure(cluster, pageBase, result, handler);
        }

        return new MiningOperationChunk(new ArrayList<>(), new ArrayList<>());
    }

    private MiningOperationChunk resolveFullChunkStructures(@NotNull RoleAnalysisClusterType cluster,
            RoleAnalysisProcessModeType mode, PageBase pageBase, OperationResult result) {

        if (mode.equals(RoleAnalysisProcessModeType.USER)) {
            return prepareUserBasedStructure(cluster, pageBase, result, handler);

        } else if (mode.equals(RoleAnalysisProcessModeType.ROLE)) {
            return prepareRoleBasedStructure(cluster, pageBase, result, handler);
        }
        return new MiningOperationChunk(new ArrayList<>(), new ArrayList<>());
    }

    @Override
    public MiningOperationChunk prepareRoleBasedStructure(@NotNull RoleAnalysisClusterType cluster, PageBase pageBase,
            OperationResult result, Handler handler) {

        Map<String, PrismObject<RoleType>> roleExistCache = new HashMap<>();

        List<MiningUserTypeChunk> miningUserTypeChunks = new ArrayList<>();
        List<MiningRoleTypeChunk> miningRoleTypeChunks = new ArrayList<>();
        ListMultimap<List<String>, String> userChunk = ArrayListMultimap.create();
        ListMultimap<String, String> roleMap = ArrayListMultimap.create();
        List<ObjectReferenceType> members = cluster.getMember();

        Set<String> membersOidSet = new HashSet<>();

        handler.setActive(true);
        handler.setSubTitle("Map Roles");
        handler.setOperationCountToProcess(members.size());
        for (ObjectReferenceType member : members) {
            handler.iterateActualStatus();

            String membersOid = member.getOid();
            PrismObject<RoleType> role = cacheRole(pageBase, result, roleExistCache, membersOid);
            if (role == null) {
                continue;
            }

            membersOidSet.add(membersOid);
            //TODO add filter
            List<PrismObject<UserType>> userMembers = extractRoleMembers(null, result, pageBase, membersOid);
            List<String> users = extractOid(userMembers);
            Collections.sort(users);

            userChunk.putAll(users, Collections.singletonList(membersOid));
            for (String userOid : users) {
                roleMap.putAll(userOid, Collections.singletonList(membersOid));
            }

        }

        handler.setSubTitle("Map Users");
        handler.setOperationCountToProcess(members.size());
        int roleMapSize = roleMap.size();
        ListMultimap<List<String>, String> roleChunk = ArrayListMultimap.create();
        for (String key : roleMap.keySet()) {
            handler.iterateActualStatus();

            List<String> values = roleMap.get(key);
            roleChunk.put(values, key);

        }

        int userChunkSize = userChunk.size();
        handler.setSubTitle("Process Role Structure");
        handler.setOperationCountToProcess(userChunkSize);
        for (List<String> users : userChunk.keySet()) {
            handler.iterateActualStatus();

            List<String> roles = userChunk.get(users);
            int rolesSize = roles.size();
            String chunkName = "Group (" + rolesSize + " Roles)";
            if (rolesSize == 1) {
                PrismObject<RoleType> role = cacheRole(pageBase, result, roleExistCache, roles.get(0));
                chunkName = "NOT FOUND";
                if (role != null) {
                    chunkName = role.getName().toString();
                }
            }

            double frequency = Math.min(users.size() / (double) roleMapSize, 1);

            miningRoleTypeChunks.add(new MiningRoleTypeChunk(roles, users, chunkName, frequency, Status.NEUTRAL));

        }

        int memberCount = membersOidSet.size();
        int roleChunkSize = roleChunk.size();
        handler.setSubTitle("Process User Structure");
        handler.setOperationCountToProcess(roleChunkSize);
        for (List<String> key : roleChunk.keySet()) {
            handler.iterateActualStatus();

            List<String> users = roleChunk.get(key);
            key.retainAll(membersOidSet);
            int size = key.size();
            double frequency = Math.min(size / (double) memberCount, 1);
            int userSize = users.size();
            String chunkName = "Group (" + userSize + " Users)";
            if (userSize == 1) {
                PrismObject<UserType> user = getUserTypeObject(pageBase, users.get(0), result);
                chunkName = "NOT FOUND";
                if (user != null) {
                    chunkName = user.getName().toString();
                }
            }
            miningUserTypeChunks.add(new MiningUserTypeChunk(users, key, chunkName, frequency, Status.NEUTRAL));

        }

        return new MiningOperationChunk(miningUserTypeChunks, miningRoleTypeChunks);
    }

    @Override
    public MiningOperationChunk prepareUserBasedStructure(@NotNull RoleAnalysisClusterType cluster, PageBase pageBase,
            OperationResult result, Handler handler) {

        Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();
        Map<String, PrismObject<RoleType>> roleExistCache = new HashMap<>();

        List<MiningUserTypeChunk> miningUserTypeChunks = new ArrayList<>();
        List<MiningRoleTypeChunk> miningRoleTypeChunks = new ArrayList<>();
        ListMultimap<List<String>, String> userChunk = ArrayListMultimap.create();
        ListMultimap<String, String> roleMap = ArrayListMultimap.create();

        List<ObjectReferenceType> members = cluster.getMember();
        Set<String> membersOidSet = new HashSet<>();

        handler.setActive(true);
        handler.setSubTitle("Map Users");
        handler.setOperationCountToProcess(members.size());
        for (ObjectReferenceType member : members) {
            handler.iterateActualStatus();

            String membersOid = member.getOid();

            PrismObject<UserType> user = cacheUser(pageBase, result, userExistCache, membersOid);
            if (user == null) {continue;}

            membersOidSet.add(membersOid);
            List<String> rolesOid = getRolesOidAssignment(user.asObjectable());

            List<String> existingRolesAssignment = new ArrayList<>();
            for (String roleId : rolesOid) {
                PrismObject<RoleType> role = cacheRole(pageBase, result, roleExistCache, roleId);
                if (role == null) {continue;}
                existingRolesAssignment.add(roleId);
                roleMap.putAll(roleId, Collections.singletonList(membersOid));
            }

            Collections.sort(existingRolesAssignment);
            userChunk.putAll(existingRolesAssignment, Collections.singletonList(membersOid));

        }

        int roleMapSize = roleMap.size();
        handler.setSubTitle("Map Roles");
        handler.setOperationCountToProcess(roleMapSize);
        ListMultimap<List<String>, String> roleChunk = ArrayListMultimap.create();
        for (String key : roleMap.keySet()) {
            handler.iterateActualStatus();
            List<String> values = roleMap.get(key);
            roleChunk.put(values, key);
        }

        int userChunkSize = userChunk.size();
        handler.setSubTitle("Process User Structure");
        handler.setOperationCountToProcess(userChunkSize);
        for (List<String> key : userChunk.keySet()) {
            handler.iterateActualStatus();

            List<String> usersElements = userChunk.get(key);
            int usersSize = usersElements.size();
            String chunkName = "Group (" + usersSize + " Users)";
            if (usersSize == 1) {
                PrismObject<UserType> user = userExistCache.get(usersElements.get(0));
                chunkName = "NOT FOUND";
                if (user != null) {
                    chunkName = user.getName().toString();
                }
            }

            double frequency = Math.min(key.size() / (double) roleMapSize, 1);

            miningUserTypeChunks.add(new MiningUserTypeChunk(usersElements, key, chunkName, frequency, Status.NEUTRAL));

        }

        int memberCount = membersOidSet.size();

        int roleChunkSize = roleChunk.size();

        handler.setSubTitle("Process Role Structure");
        handler.setOperationCountToProcess(roleChunkSize);
        for (List<String> key : roleChunk.keySet()) {
            handler.iterateActualStatus();

            List<String> roles = roleChunk.get(key);
            key.retainAll(membersOidSet);
            int size = key.size();
            double frequency = Math.min(size / (double) memberCount, 1);
            int rolesSize = roles.size();
            String chunkName = "Group (" + rolesSize + " Roles)";
            if (rolesSize == 1) {
                PrismObject<RoleType> role = cacheRole(pageBase, result, roleExistCache, roles.get(0));
                chunkName = "NOT FOUND";
                if (role != null) {
                    chunkName = role.getName().toString();
                }
            }
            miningRoleTypeChunks.add(new MiningRoleTypeChunk(roles, key, chunkName, frequency, Status.NEUTRAL));

        }

        return new MiningOperationChunk(miningUserTypeChunks, miningRoleTypeChunks);
    }

    @Override
    public MiningOperationChunk preparePartialRoleBasedStructure(@NotNull RoleAnalysisClusterType cluster,
            PageBase pageBase, OperationResult result, Handler handler) {

        Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();
        Map<String, PrismObject<RoleType>> roleExistCache = new HashMap<>();

        List<MiningUserTypeChunk> miningUserTypeChunks = new ArrayList<>();
        List<MiningRoleTypeChunk> miningRoleTypeChunks = new ArrayList<>();

        ListMultimap<List<String>, String> userChunk = ArrayListMultimap.create();
        ListMultimap<String, String> roleMap = ArrayListMultimap.create();
        Set<String> membersOidSet = new HashSet<>();

        List<ObjectReferenceType> members = cluster.getMember();
        handler.setActive(true);
        handler.setSubTitle("Map Roles");
        handler.setOperationCountToProcess(members.size());
        for (ObjectReferenceType objectReferenceType : members) {
            handler.iterateActualStatus();

            String oid = objectReferenceType.getOid();

            PrismObject<RoleType> role = cacheRole(pageBase, result, roleExistCache, oid);
            if (role == null) {continue;}
            membersOidSet.add(oid);

            //TODO add filter
            List<PrismObject<UserType>> userMembers = extractRoleMembers(null, result, pageBase, oid);
            List<String> users = extractOid(userMembers);
            Collections.sort(users);

            userChunk.putAll(users, Collections.singletonList(oid));
            for (String roleId : users) {
                roleMap.putAll(roleId, Collections.singletonList(oid));
            }

        }

        int membersCount = membersOidSet.size();
        //user //role
        ListMultimap<List<String>, String> roleChunk = ArrayListMultimap.create();
        handler.setSubTitle("Map Users");
        handler.setOperationCountToProcess(roleMap.size());

        for (String key : roleMap.keySet()) {
            handler.iterateActualStatus();
            List<String> values = roleMap.get(key);
            roleChunk.put(values, key);
        }

        handler.setSubTitle("Prepare User Structure");
        handler.setOperationCountToProcess(roleChunk.size());
        for (List<String> key : roleChunk.keySet()) {
            handler.iterateActualStatus();

            List<String> users = roleChunk.get(key);
            key.retainAll(membersOidSet);
            int size = key.size();
            double frequency = Math.min(size / (double) membersCount, 1);
            int userSize = users.size();
            String chunkName = "Group (" + userSize + " Users)";
            if (userSize == 1) {
                PrismObject<UserType> user = cacheUser(pageBase, result, userExistCache, users.get(0));
                chunkName = "NOT FOUND";
                if (user != null) {
                    chunkName = user.getName().toString();
                }
            }
            miningUserTypeChunks.add(new MiningUserTypeChunk(users, key, chunkName, frequency, Status.NEUTRAL));
        }
        return new MiningOperationChunk(miningUserTypeChunks, miningRoleTypeChunks);
    }

    @Override
    public MiningOperationChunk preparePartialUserBasedStructure(@NotNull RoleAnalysisClusterType cluster,
            PageBase pageBase, OperationResult result, Handler handler) {
        Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();
        Map<String, PrismObject<RoleType>> roleExistCache = new HashMap<>();

        List<MiningUserTypeChunk> miningUserTypeChunks = new ArrayList<>();
        List<MiningRoleTypeChunk> miningRoleTypeChunks = new ArrayList<>();

        ListMultimap<String, String> roleMap = ArrayListMultimap.create();
        Set<String> membersOidSet = new HashSet<>();

        List<ObjectReferenceType> members = cluster.getMember();
        handler.setActive(true);
        handler.setSubTitle("Map Users");
        handler.setOperationCountToProcess(members.size());
        for (ObjectReferenceType objectReferenceType : members) {
            handler.iterateActualStatus();

            String oid = objectReferenceType.getOid();

            PrismObject<UserType> user = cacheUser(pageBase, result, userExistCache, oid);
            if (user == null) {
                continue;
            }
            membersOidSet.add(oid);
            List<String> rolesOid = getRolesOidAssignment(user.asObjectable());

            for (String roleId : rolesOid) {
                PrismObject<RoleType> role = cacheRole(pageBase, result, roleExistCache, roleId);
                if (role == null) {continue;}
                roleMap.putAll(roleId, Collections.singletonList(oid));
            }

        }

        int membersCount = membersOidSet.size();
        //user //role
        ListMultimap<List<String>, String> roleChunk = ArrayListMultimap.create();

        handler.setSubTitle("Map Roles");
        handler.setOperationCountToProcess(roleMap.size());
        for (String key : roleMap.keySet()) {
            handler.iterateActualStatus();

            List<String> values = roleMap.get(key);
            roleChunk.put(values, key);
        }

        handler.setSubTitle("Prepare Role Structure");
        handler.setOperationCountToProcess(roleChunk.size());
        for (List<String> key : roleChunk.keySet()) {
            handler.iterateActualStatus();

            List<String> roles = roleChunk.get(key);
            key.retainAll(membersOidSet);
            int size = key.size();
            double frequency = Math.min(size / (double) membersCount, 1);
            int rolesSize = roles.size();
            String chunkName = "Group (" + rolesSize + " Roles)";
            if (rolesSize == 1) {
                PrismObject<RoleType> role = cacheRole(pageBase, result, roleExistCache, roles.get(0));
                chunkName = "NOT FOUND";
                if (role != null) {
                    chunkName = role.getName().toString();
                }
            }

            miningRoleTypeChunks.add(new MiningRoleTypeChunk(roles, key, chunkName, frequency, Status.NEUTRAL));
        }
        return new MiningOperationChunk(miningUserTypeChunks, miningRoleTypeChunks);
    }

}
