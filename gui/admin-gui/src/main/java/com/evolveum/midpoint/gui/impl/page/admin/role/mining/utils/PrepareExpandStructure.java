/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.MiningOperationChunk;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.MiningRoleTypeChunk;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.MiningUserTypeChunk;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.*;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.getRoleTypeObject;

public class PrepareExpandStructure implements MiningStructure, Serializable {

    public MiningOperationChunk executeOperation(@NotNull RoleAnalysisClusterType cluster, boolean fullProcess,
            RoleAnalysisProcessModeType mode, PageBase pageBase, OperationResult result, String state) {
        if (fullProcess) {
            return resolveFullStructures(cluster, mode, pageBase, result, state);
        } else {
            return resolvePartialStructures(cluster, mode, pageBase, result, state);
        }
    }

    private MiningOperationChunk resolvePartialStructures(@NotNull RoleAnalysisClusterType cluster,
            RoleAnalysisProcessModeType mode, PageBase pageBase, OperationResult result, String state) {

        if (mode.equals(RoleAnalysisProcessModeType.USER)) {
            return preparePartialRoleBasedStructure(cluster, pageBase, result, state);

        } else if (mode.equals(RoleAnalysisProcessModeType.ROLE)) {
            return preparePartialUserBasedStructure(cluster, pageBase, result, state);
        }

        return new MiningOperationChunk(new ArrayList<>(), new ArrayList<>());
    }

    private MiningOperationChunk resolveFullStructures(@NotNull RoleAnalysisClusterType cluster,
            RoleAnalysisProcessModeType mode, PageBase pageBase, OperationResult result, String state) {

        if (mode.equals(RoleAnalysisProcessModeType.USER)) {
            return prepareUserBasedStructure(cluster, pageBase, result, state);
        } else if (mode.equals(RoleAnalysisProcessModeType.ROLE)) {
            return prepareRoleBasedStructure(cluster, pageBase, result, state);
        }

        return new MiningOperationChunk(new ArrayList<>(), new ArrayList<>());
    }

    @Override
    public MiningOperationChunk prepareRoleBasedStructure(@NotNull RoleAnalysisClusterType cluster, PageBase pageBase,
            OperationResult result, String state) {
        List<MiningUserTypeChunk> miningUserTypeChunks = new ArrayList<>();
        List<MiningRoleTypeChunk> miningRoleTypeChunks = new ArrayList<>();

        List<ObjectReferenceType> rolesElements = cluster.getMember();

        ListMultimap<String, String> userChunk = ArrayListMultimap.create();

        int rolesCount = rolesElements.size();
        for (int i = 0; i < rolesElements.size(); i++) {
            String rolesOid = rolesElements.get(i).getOid();
            List<PrismObject<UserType>> userMembers = extractRoleMembers(result, pageBase, rolesOid);
            List<String> users = extractOid(userMembers);

            int size = users.size();
            double frequency = Math.min(size / (double) rolesCount, 1);

            PrismObject<RoleType> role = getRoleTypeObject(pageBase, rolesOid, result);
            String chunkName = "NOT FOUND";
            if (role != null) {
                chunkName = role.getName().toString();
            }

            miningRoleTypeChunks.add(new MiningRoleTypeChunk(Collections.singletonList(rolesOid),
                    users, chunkName, frequency, Status.NEUTRAL));

            for (String user : users) {
                userChunk.putAll(user, Collections.singletonList(rolesOid));
            }

            state = i + "/" + rolesCount + " ((1 & 2)/4 mapping elements)";
            System.out.println(state);
        }

        int counter = 0;
        int userChunkSize = userChunk.size();
        for (String key : userChunk.keySet()) {
            List<String> strings = userChunk.get(key);
            double frequency = Math.min(strings.size() / (double) rolesCount, 1);
            PrismObject<UserType> user = getUserTypeObject(pageBase, key, result);
            String chunkName = "NOT FOUND";
            if (user != null) {
                chunkName = user.getName().toString();
            }

            miningUserTypeChunks.add(new MiningUserTypeChunk(Collections.singletonList(key), strings, chunkName, frequency,
                    Status.NEUTRAL));
            state = counter + "/" + userChunkSize + " (3/4 mapping points)";
            System.out.println(state);
            counter++;
        }

        return new MiningOperationChunk(miningUserTypeChunks, miningRoleTypeChunks);

    }

    @Override
    public MiningOperationChunk prepareUserBasedStructure(@NotNull RoleAnalysisClusterType cluster, PageBase pageBase,
            OperationResult result, String state) {
        List<MiningUserTypeChunk> miningUserTypeChunks = new ArrayList<>();
        List<MiningRoleTypeChunk> miningRoleTypeChunks = new ArrayList<>();

        ListMultimap<String, String> roleChunk = ArrayListMultimap.create();

        List<ObjectReferenceType> members = cluster.getMember();
        int usersCount = members.size();
        for (int i = 0; i < members.size(); i++) {
            String membersOid = members.get(i).getOid();
            PrismObject<UserType> user = getUserTypeObject(pageBase, membersOid, result);

            if (user == null) {
                continue;
            }

            List<String> rolesOid = getRolesOid(user.asObjectable());
            int size = rolesOid.size();
            double frequency = Math.min(size / (double) usersCount, 1);
            String chunkName = "NOT FOUND";
            if (user.getName() != null) {
                chunkName = user.getName().toString();
            }
            miningUserTypeChunks.add(new MiningUserTypeChunk(Collections.singletonList(membersOid), rolesOid, chunkName,
                    frequency, Status.NEUTRAL));

            for (String roleId : rolesOid) {
                roleChunk.putAll(roleId, Collections.singletonList(user.getOid()));
            }

            state = i + "/" + usersCount + " ((1 & 2)/4 mapping elements)";
            System.out.println(state);
        }

        int roleChunkSize = roleChunk.size();
        int counter = 0;
        for (String key : roleChunk.keySet()) {
            List<String> strings = roleChunk.get(key);
            double frequency = Math.min(strings.size() / (double) usersCount, 1);

            PrismObject<RoleType> role = getRoleTypeObject(pageBase, key, result);
            String chunkName = "NOT FOUND";
            if (role != null) {
                chunkName = role.getName().toString();
            }

            miningRoleTypeChunks.add(new MiningRoleTypeChunk(Collections.singletonList(key), strings, chunkName, frequency,
                    Status.NEUTRAL));
            state = counter + "/" + roleChunkSize + " (3/4 mapping points)";
            System.out.println(state);
            counter++;

        }

        return new MiningOperationChunk(miningUserTypeChunks, miningRoleTypeChunks);

    }

    @Override
    public MiningOperationChunk preparePartialRoleBasedStructure(@NotNull RoleAnalysisClusterType cluster, PageBase pageBase,
            OperationResult result, String state) {
        List<MiningUserTypeChunk> miningUserTypeChunks = new ArrayList<>();
        List<MiningRoleTypeChunk> miningRoleTypeChunks = new ArrayList<>();
        List<ObjectReferenceType> rolesElements = cluster.getMember();

        ListMultimap<List<String>, String> userChunk = ArrayListMultimap.create();
        ListMultimap<String, String> roleMap = ArrayListMultimap.create();

        for (ObjectReferenceType objectReferenceType : rolesElements) {
            String oid = objectReferenceType.getOid();

            List<PrismObject<UserType>> userMembers = extractRoleMembers(result, pageBase, oid);
            List<String> users = extractOid(userMembers);
            Collections.sort(users);

            userChunk.putAll(users, Collections.singletonList(oid));
            for (String roleId : users) {
                roleMap.putAll(roleId, Collections.singletonList(oid));
            }
        }

        int rolesCount = rolesElements.size();

        //user //role
        ListMultimap<List<String>, String> roleChunk = ArrayListMultimap.create();
        for (String key : roleMap.keySet()) {
            List<String> values = roleMap.get(key);
            roleChunk.put(values, key);
        }

        for (List<String> key : roleChunk.keySet()) {
            List<String> users = roleChunk.get(key);
            int size = key.size();
            double frequency = Math.min(size / (double) rolesCount, 1);
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
    public MiningOperationChunk preparePartialUserBasedStructure(@NotNull RoleAnalysisClusterType cluster, PageBase pageBase,
            OperationResult result, String state) {
        List<MiningUserTypeChunk> miningUserTypeChunks = new ArrayList<>();
        List<MiningRoleTypeChunk> miningRoleTypeChunks = new ArrayList<>();
        ListMultimap<List<String>, String> userChunk = ArrayListMultimap.create();
        ListMultimap<String, String> roleMap = ArrayListMultimap.create();

        List<ObjectReferenceType> members = cluster.getMember();
        for (ObjectReferenceType objectReferenceType : members) {
            String oid = objectReferenceType.getOid();
            PrismObject<UserType> user = getUserTypeObject(pageBase, oid, result);
            if (user == null) {
                continue;
            }
            List<String> rolesOid = getRolesOid(user.asObjectable());
            Collections.sort(rolesOid);
            userChunk.putAll(rolesOid, Collections.singletonList(oid));
            for (String roleId : rolesOid) {
                roleMap.putAll(roleId, Collections.singletonList(oid));
            }
        }

        int usersCount = members.size();

        //user //role
        ListMultimap<List<String>, String> roleChunk = ArrayListMultimap.create();
        for (String key : roleMap.keySet()) {
            List<String> values = roleMap.get(key);
            roleChunk.put(values, key);
        }

        for (List<String> key : roleChunk.keySet()) {
            List<String> roles = roleChunk.get(key);
            int size = key.size();
            double frequency = Math.min(size / (double) usersCount, 1);
            int rolesSize = roles.size();
            String chunkName = "Group (" + rolesSize + " Roles)";
            if (rolesSize == 1) {
                PrismObject<RoleType> role = getRoleTypeObject(pageBase, roles.get(0), result);
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

