/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util;

import java.util.*;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;

import com.evolveum.midpoint.common.mining.objects.analysis.cache.AttributeAnalysisCache;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;

import com.google.common.collect.ListMultimap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.mechanism.DataPoint;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.object.RoleAnalysisAttributeDefConvert;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.object.ExtensionProperties;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * Utility class for performing clustering operations in the context of role analysis.
 * Provides methods for creating, preparing, and processing data points used in clustering.
 */
public class ClusteringUtils {

    private static final Trace LOGGER = TraceManager.getTrace(ClusteringUtils.class);
    public static final String LOAD_DATA_STEP = "Loading data";
    public static final String PREPARING_DATA_POINTS_STEP = "Preparing data points";

    static Collection<SelectorOptions<GetOperationOptions>> defaultOptions = GetOperationOptionsBuilder.create().raw().build();

    /**
     * Retrieves existing role OIDs from the model service.
     *
     * @param modelService The model service for accessing role data.
     * @param task The task associated with the operation.
     * @param result The operation result.
     * @return A set of existing role OIDs.
     */
    @NotNull
    public static Set<String> getExistingActiveRolesOidsSet(@NotNull ModelService modelService,
            @NotNull Task task,
            @NotNull OperationResult result) {
        Set<String> existingRolesOidsSet = new HashSet<>();
        ResultHandler<RoleType> roleTypeHandler = (object, parentResult) -> {
            try {
                RoleType roleObject = object.asObjectable();
                String lifecycleState = roleObject.getLifecycleState();

                if (lifecycleState == null || lifecycleState.equals("active")) {
                    existingRolesOidsSet.add(object.getOid());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return true;
        };

        try {
            modelService.searchObjectsIterative(RoleType.class, null, roleTypeHandler,
                    defaultOptions, task, result);
        } catch (SchemaException | ObjectNotFoundException | ExpressionEvaluationException |
                CommunicationException | ConfigurationException | SecurityViolationException e) {
            LOGGER.error("Couldn't search  RoleType ", e);
        }
        return existingRolesOidsSet;

    }

    /**
     * Prepares data points based on the provided chunk map.
     *
     * @param chunkMap A list multimap mapping roles to users.
     * @return A list of DataPoint instances.
     */
    public static @NotNull List<DataPoint> prepareDataPoints(@NotNull ListMultimap<List<String>, String> chunkMap) {
        List<DataPoint> dataPoints = new ArrayList<>();

        for (List<String> points : chunkMap.keySet()) {
            List<String> elements = chunkMap.get(points);
            dataPoints.add(new DataPoint(new HashSet<>(elements), new HashSet<>(points)));
        }
        return dataPoints;
    }

    public static @NotNull List<DataPoint> prepareDataPointsUserModeRules(
            @NotNull ListMultimap<List<String>, String> chunkMap,
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull List<RoleAnalysisAttributeDefConvert> roleAnalysisAttributeDefConverts,
            @NotNull Task task) {
        List<DataPoint> dataPoints = new ArrayList<>();

        for (List<String> points : chunkMap.keySet()) {
            for (String element : chunkMap.get(points)) {
                PrismObject<UserType> userTypeObject = roleAnalysisService.getUserTypeObject(element, task, task.getResult());

                if (userTypeObject != null) {
                    ExtensionProperties extensionProperties = new ExtensionProperties();

                    for (RoleAnalysisAttributeDefConvert rule : roleAnalysisAttributeDefConverts) {

                        //TODO
                        RoleAnalysisAttributeDef roleAnalysisItemDef = rule.getRoleAnalysisItemDef();
                        ItemPath path = roleAnalysisItemDef.getPath();
                        boolean isContainer = roleAnalysisItemDef.isContainer();

                        if (isContainer) {
                            Set<String> allValues = roleAnalysisItemDef.resolveMultiValueItem(userTypeObject, path);
                            for (String value : allValues) {
                                extensionProperties.addProperty(rule, value);
                            }
                        } else {
                            String value = roleAnalysisItemDef.resolveSingleValueItem(userTypeObject, path);
                            extensionProperties.addProperty(rule, value);

                        }
                    }

                    dataPoints.add(new DataPoint(new HashSet<>(Collections.singleton(element)),
                            new HashSet<>(points),
                            extensionProperties));
                }
            }
        }
        return dataPoints;
    }

    public static @NotNull List<DataPoint> prepareDataPointsRoleModeRules(
            @NotNull ListMultimap<List<String>, String> chunkMap,
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull List<RoleAnalysisAttributeDefConvert> roleAnalysisAttributeDefConverts,
            @NotNull Task task) {
        List<DataPoint> dataPoints = new ArrayList<>();

        for (List<String> points : chunkMap.keySet()) {
            for (String element : chunkMap.get(points)) {
                PrismObject<RoleType> roleType = roleAnalysisService.getRoleTypeObject(element, task, task.getResult());

                if (roleType != null) {
                    ExtensionProperties extensionProperties = new ExtensionProperties();

                    for (RoleAnalysisAttributeDefConvert rule : roleAnalysisAttributeDefConverts) {

                        //TODO
                        RoleAnalysisAttributeDef roleAnalysisItemDef = rule.getRoleAnalysisItemDef();
                        ItemPath path = roleAnalysisItemDef.getPath();
                        boolean isContainer = roleAnalysisItemDef.isContainer();

                        if (isContainer) {
                            Set<String> allValues = roleAnalysisItemDef.resolveMultiValueItem(roleType, path);
                            for (String value : allValues) {
                                extensionProperties.addProperty(rule, value);
                            }
                        } else {
                            String value = roleAnalysisItemDef.resolveSingleValueItem(roleType, path);
                            extensionProperties.addProperty(rule, value);

                        }

                    }

                    dataPoints.add(new DataPoint(new HashSet<>(Collections.singleton(element)),
                            new HashSet<>(points),
                            extensionProperties));
                }
            }
        }
        return dataPoints;
    }

    @NotNull
    public static ListMultimap<List<String>, String> loadRoleBasedMultimapData(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull Boolean isIndirect,
            @Nullable SearchFilterType userSearchFilter,
            @Nullable SearchFilterType roleSearchFilter,
            @Nullable SearchFilterType assignmentSearchFilter,
            @NotNull AttributeAnalysisCache attributeAnalysisCache,
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull RoleAnalysisSessionType sessionObject) {

        if (Boolean.TRUE.equals(isIndirect)) {
            return roleAnalysisService.prepareMembershipChunkMapRolesAsKey(
                    userSearchFilter, roleSearchFilter, assignmentSearchFilter, RoleAnalysisProcessModeType.ROLE, attributeAnalysisCache, task, result, sessionObject);
        }

        return roleAnalysisService.prepareAssignmentChunkMapRolesAsKey(
                userSearchFilter, roleSearchFilter, assignmentSearchFilter, RoleAnalysisProcessModeType.ROLE, attributeAnalysisCache, task, result, sessionObject);
    }

    @NotNull
    public static ListMultimap<List<String>, String> loadUserBasedMultimapData(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull Boolean isIndirect,
            @Nullable SearchFilterType userSearchFilter,
            @Nullable SearchFilterType roleSearchFilter,
            @Nullable SearchFilterType assignmentSearchFilter,
            @NotNull AttributeAnalysisCache attributeAnalysisCache,
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull RoleAnalysisSessionType sessionObject) {

        if (Boolean.TRUE.equals(isIndirect)) {
            return roleAnalysisService.prepareMembershipChunkMapRolesAsKey(
                    userSearchFilter, roleSearchFilter, assignmentSearchFilter, RoleAnalysisProcessModeType.USER, attributeAnalysisCache, task, result, sessionObject);
        }

        return roleAnalysisService.prepareAssignmentChunkMapRolesAsKey(
                userSearchFilter, roleSearchFilter, assignmentSearchFilter, RoleAnalysisProcessModeType.USER, attributeAnalysisCache, task, result, sessionObject);
    }

}
