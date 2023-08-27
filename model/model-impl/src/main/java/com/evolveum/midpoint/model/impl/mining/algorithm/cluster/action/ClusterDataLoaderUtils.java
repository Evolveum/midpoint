/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action;

import java.io.Serializable;
import java.util.*;

import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.mechanism.DataPoint;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.getRolesOidAssignment;

public class ClusterDataLoaderUtils implements Serializable {

    @NotNull
    protected static Set<String> getExistingRolesOidsSet(OperationResult result, @NotNull RepositoryService repoService) {
        Set<String> existingRolesOidsSet = new HashSet<>();
        ResultHandler<RoleType> roleTypeHandler = (object, parentResult) -> {
            try {
                existingRolesOidsSet.add(object.getOid());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return true;
        };

        try {
            repoService.searchObjectsIterative(RoleType.class, null, roleTypeHandler,
                    null, true, result);
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
        return existingRolesOidsSet;
    }

    @NotNull
    protected static ListMultimap<List<String>, String> getUserBasedRoleToUserMap(OperationResult result,
            RepositoryService repoService, int minProperties, int maxProperties, SearchFilterType userQuery,
            Set<String> existingRolesOidsSet) {
        ListMultimap<List<String>, String> roleToUserMap = ArrayListMultimap.create();

        ResultHandler<UserType> resultHandler = (object, parentResult) -> {
            try {
                List<String> properties = getRolesOidAssignment(object.asObjectable());
                int propertiesCount = properties.size();
                if (minProperties <= propertiesCount && maxProperties >= propertiesCount) {
                    properties.retainAll(existingRolesOidsSet);
                    Collections.sort(properties);
                    roleToUserMap.putAll(properties, Collections.singletonList(object.asObjectable().getOid()));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return true;
        };

        ObjectQuery objectQuery;
        try {
            objectQuery = PrismContext.get().getQueryConverter().createObjectQuery(UserType.class, userQuery);
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
        try {
            repoService.searchObjectsIterative(UserType.class, objectQuery, resultHandler, null,
                    true, result);
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
        return roleToUserMap;
    }

    @NotNull
    protected static ListMultimap<String, String> getRoleBasedRoleToUserMap(OperationResult result,
            @NotNull RepositoryService repoService, SearchFilterType userQuery, Set<String> existingRolesOidsSet) {
        ListMultimap<String, String> roleToUserMap = ArrayListMultimap.create();

        ResultHandler<UserType> resultHandler = (object, parentResult) -> {
            try {
                UserType properties = object.asObjectable();
                List<String> members = getRolesOidAssignment(properties);
                members.retainAll(existingRolesOidsSet);
                for (String roleId : members) {
                    roleToUserMap.putAll(roleId, Collections.singletonList(properties.getOid()));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return true;
        };

        ObjectQuery objectQuery;
        try {
            objectQuery = PrismContext.get().getQueryConverter().createObjectQuery(UserType.class, userQuery);
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }

        try {
            repoService.searchObjectsIterative(UserType.class, objectQuery, resultHandler, null,
                    true, result);
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
        return roleToUserMap;
    }

    @NotNull
    protected static ListMultimap<List<String>, String> getRoleBasedUserToRoleMap(int minProperties, int maxProperties,
            ListMultimap<String, String> roleToUserMap) {
        ListMultimap<List<String>, String> userToRoleMap = ArrayListMultimap.create();
        for (String member : roleToUserMap.keySet()) {
            List<String> properties = roleToUserMap.get(member);
            int propertiesCount = properties.size();
            if (minProperties <= propertiesCount && maxProperties >= propertiesCount) {
                userToRoleMap.put(properties, member);
            }
        }
        return userToRoleMap;
    }

    public static List<DataPoint> prepareDataPoints(ListMultimap<List<String>, String> chunkMap) {
        List<DataPoint> dataPoints = new ArrayList<>();

        for (List<String> points : chunkMap.keySet()) {
            List<String> elements = chunkMap.get(points);

            dataPoints.add(new DataPoint(new HashSet<>(elements), new HashSet<>(points)));

        }
        return dataPoints;
    }

}
