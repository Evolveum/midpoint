/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.utils.JaccardSorter;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClusterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class MiningOperationChunk implements Serializable {
    List<MiningUserTypeChunk> miningUserTypeChunks = new ArrayList<>();
    List<MiningRoleTypeChunk> miningRoleTypeChunks = new ArrayList<>();

    public MiningOperationChunk(@NotNull ClusterType clusterType,
            PageBase pageBase, ClusterObjectUtils.Mode mode, OperationResult operationResult, boolean chunk) {

        if (chunk) {
            getChunkedMiningClusterStructure(clusterType, pageBase, mode, operationResult);
        } else {
            getMiningClusterStructure(clusterType, pageBase, mode, operationResult);
        }
    }

    private void getMiningClusterStructure(@NotNull ClusterType cluster,
            PageBase pageBase, ClusterObjectUtils.Mode mode, OperationResult operationResult) {

        ListMultimap<String, String> roleChunk = ArrayListMultimap.create();

        if (mode.equals(ClusterObjectUtils.Mode.USER)) {
            List<String> members = cluster.getElements();
            int usersCount = members.size();
            for (String membersOid : members) {
                PrismObject<UserType> user = getUserTypeObject(pageBase, membersOid, operationResult);

                if(user == null){
                    continue;
                }

                List<String> rolesOid = getRolesOid(user.asObjectable());
                int size = rolesOid.size();
                double frequency = Math.min(size / (double) usersCount, 1);
                String chunkName = "Unknown User";
                if (user.getName() != null) {
                    chunkName = user.getName().toString();
                }
                miningUserTypeChunks.add(new MiningUserTypeChunk(Collections.singletonList(membersOid), rolesOid, chunkName,
                        frequency, Status.NEUTRAL));

                for (String roleId : rolesOid) {
                    roleChunk.putAll(roleId, Collections.singletonList(user.getOid()));
                }
            }

            for (String key : roleChunk.keySet()) {
                List<String> strings = roleChunk.get(key);
                double frequency = Math.min(strings.size() / (double) usersCount, 1);

                PrismObject<RoleType> role = getRoleTypeObject(pageBase, key, operationResult);
                String chunkName = "Unknown Role";
                if (role != null) {
                    chunkName = role.getName().toString();
                }

                miningRoleTypeChunks.add(new MiningRoleTypeChunk(Collections.singletonList(key), strings, chunkName, frequency, Status.NEUTRAL));
            }

        } else if (mode.equals(ClusterObjectUtils.Mode.ROLE)) {
            List<String> rolesElements = cluster.getElements();

            ListMultimap<String, String> userChunk = ArrayListMultimap.create();

            int rolesCount = rolesElements.size();
            for (String rolesOid : rolesElements) {
                List<PrismObject<UserType>> userMembers = extractRoleMembers(pageBase, rolesOid);
                List<String> users = extractOid(userMembers);

                int size = users.size();
                double frequency = Math.min(size / (double) rolesCount, 1);

                PrismObject<RoleType> role = getRoleTypeObject(pageBase, rolesOid, operationResult);
                String chunkName = "Unknown Role";
                if (role != null) {
                    chunkName = role.getName().toString();
                }

                miningRoleTypeChunks.add(new MiningRoleTypeChunk(Collections.singletonList(rolesOid),
                        users, chunkName, frequency, Status.NEUTRAL));

                for (String user : users) {
                    userChunk.putAll(user, Collections.singletonList(rolesOid));
                }
            }

            for (String key : userChunk.keySet()) {
                List<String> strings = userChunk.get(key);
                double frequency = Math.min(strings.size() / (double) rolesCount, 1);
                PrismObject<UserType> user = getUserTypeObject(pageBase, key, operationResult);
                String chunkName = "Unknown User";
                if (user != null) {
                    chunkName = user.getName().toString();
                }

                miningUserTypeChunks.add(new MiningUserTypeChunk(Collections.singletonList(key), strings, chunkName, frequency, Status.NEUTRAL));
            }
        }

    }

    private void getChunkedMiningClusterStructure(@NotNull ClusterType cluster,
            PageBase pageBase, ClusterObjectUtils.Mode mode, OperationResult operationResult) {

        //this set of roles List<String> has users String...
        ListMultimap<List<String>, String> userChunk = ArrayListMultimap.create();
        ListMultimap<String, String> roleMap = ArrayListMultimap.create();
        if (mode.equals(ClusterObjectUtils.Mode.USER)) {
            List<String> members = cluster.getElements();
            for (String membersOid : members) {
                PrismObject<UserType> user = getUserTypeObject(pageBase, membersOid, operationResult);
                if(user == null){
                    continue;
                }
                List<String> rolesOid = getRolesOid(user.asObjectable());
                Collections.sort(rolesOid);
                userChunk.putAll(rolesOid, Collections.singletonList(membersOid));
                for (String roleId : rolesOid) {
                    roleMap.putAll(roleId, Collections.singletonList(membersOid));
                }
            }

            int usersCount = members.size();

            //user //role
            ListMultimap<List<String>, String> roleChunk = ArrayListMultimap.create();
            for (String key : roleMap.keySet()) {
                List<String> values = roleMap.get(key);
                roleChunk.put(values, key);
            }

            for (List<String> key : userChunk.keySet()) {
                List<String> usersElements = userChunk.get(key);
                int size = key.size();
                double frequency = Math.min(size / (double) usersCount, 1);
                int usersSize = usersElements.size();
                String chunkName = "Group (" + usersSize + " Users)";
                if (usersSize == 1) {
                    PrismObject<UserType> user = getUserTypeObject(pageBase, usersElements.get(0), operationResult);
                    chunkName = "Unknown User";
                    if (user != null) {
                        chunkName = user.getName().toString();
                    }
                }
                miningUserTypeChunks.add(new MiningUserTypeChunk(usersElements, key, chunkName, frequency, Status.NEUTRAL));
            }

            for (List<String> key : roleChunk.keySet()) {
                List<String> roles = roleChunk.get(key);
                int size = key.size();
                double frequency = Math.min(size / (double) usersCount, 1);
                int rolesSize = roles.size();
                String chunkName = "Group (" + rolesSize + " Roles)";
                if (rolesSize == 1) {
                    PrismObject<RoleType> role = getRoleTypeObject(pageBase, roles.get(0), operationResult);
                    chunkName = "Unknown Role";
                    if (role != null) {
                        chunkName = role.getName().toString();
                    }
                }
                miningRoleTypeChunks.add(new MiningRoleTypeChunk(roles, key, chunkName, frequency, Status.NEUTRAL));
            }

        } else if (mode.equals(ClusterObjectUtils.Mode.ROLE)) {
            List<String> rolesElements = cluster.getElements();

            for (String membersOid : rolesElements) {

                List<PrismObject<UserType>> userMembers = extractRoleMembers(pageBase, membersOid);
                List<String> users = extractOid(userMembers);
                Collections.sort(users);

                userChunk.putAll(users, Collections.singletonList(membersOid));
                for (String roleId : users) {
                    roleMap.putAll(roleId, Collections.singletonList(membersOid));
                }
            }

            int rolesCount = rolesElements.size();

            //user //role
            ListMultimap<List<String>, String> roleChunk = ArrayListMultimap.create();
            for (String key : roleMap.keySet()) {
                List<String> values = roleMap.get(key);
                roleChunk.put(values, key);
            }

            for (List<String> key : userChunk.keySet()) {
                List<String> roles = userChunk.get(key);
                int size = key.size();
                double frequency = Math.min(size / (double) rolesCount, 1);
                int rolesSize = roles.size();
                String chunkName = "Group (" + rolesSize + " Roles)";
                if (rolesSize == 1) {
                    PrismObject<RoleType> role = getRoleTypeObject(pageBase, roles.get(0), operationResult);
                    chunkName = "Unknown Role";
                    if (role != null) {
                        chunkName = role.getName().toString();
                    }

                }
                miningRoleTypeChunks.add(new MiningRoleTypeChunk(roles, key, chunkName, frequency, Status.NEUTRAL));
            }

            for (List<String> key : roleChunk.keySet()) {
                List<String> users = roleChunk.get(key);
                int size = key.size();
                double frequency = Math.min(size / (double) rolesCount, 1);
                int userSize = users.size();
                String chunkName = "Group (" + userSize + " Users)";
                if (userSize == 1) {
                    PrismObject<UserType> user = getUserTypeObject(pageBase, users.get(0), operationResult);
                    chunkName = "Unknown User";
                    if (user != null) {
                        chunkName = user.getName().toString();
                    }

                }
                miningUserTypeChunks.add(new MiningUserTypeChunk(users, key, chunkName, frequency, Status.NEUTRAL));
            }
        }

    }

    public List<MiningUserTypeChunk> getMiningUserTypeChunks() {
        return JaccardSorter.sortU(miningUserTypeChunks);
    }

    public List<MiningRoleTypeChunk> getMiningRoleTypeChunks() {
        return JaccardSorter.sortR(miningRoleTypeChunks);
    }
}
