/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.chunk;

import com.evolveum.midpoint.common.mining.objects.chunk.DisplayValueOption;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.handler.RoleAnalysisProgressIncrement;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;

import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface MiningStructure {

    /**
     * This method prepares a role-based structure for mining operations.
     *
     * @param roleAnalysisService The role analysis service.
     * @param cluster The cluster representing a group of roles.
     * @param userSearchFilter The additional user filter.
     * @param roleSearchFilter The additional role filter.
     * @param assignmentSearchFilter The additional assignment filter.
     * @param handler The progress handler for role analysis.
     * @param task The task associated with the operation.
     * @param result The result object for tracking the operation's outcome.
     * @param option Display option for preparation chunk structures.
     * @return A MiningOperationChunk containing user and role chunks for further processing.
     */
    @NotNull MiningOperationChunk prepareRoleBasedStructure(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisClusterType cluster,
            @Nullable SearchFilterType userSearchFilter,
            @Nullable SearchFilterType roleSearchFilter,
            @Nullable SearchFilterType assignmentSearchFilter,
            @NotNull RoleAnalysisProgressIncrement handler,
            @NotNull Task task,
            @NotNull OperationResult result,
            @Nullable DisplayValueOption option);

    /**
     * This method prepares a user-based structure for mining operations.
     *
     * @param roleAnalysisService The role analysis service.
     * @param cluster The cluster representing a group of roles.
     * @param userSearchFilter The additional user filter.
     * @param roleSearchFilter The additional role filter.
     * @param assignmentSearchFilter The additional assignment filter.
     * @param handler The progress handler for role analysis.
     * @param task The task associated with the operation.
     * @param result The result object for tracking the operation's outcome.
     * @param option Display option for preparation chunk structures.
     * @return A MiningOperationChunk containing user and role chunks for further processing.
     */
    @NotNull MiningOperationChunk prepareUserBasedStructure(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisClusterType cluster,
            @Nullable SearchFilterType userSearchFilter,
            @Nullable SearchFilterType roleSearchFilter,
            @Nullable SearchFilterType assignmentSearchFilter,
            @NotNull RoleAnalysisProgressIncrement handler,
            @NotNull Task task,
            @NotNull OperationResult result,
            @Nullable DisplayValueOption option);

    /**
     * Prepares a partial role-based structure for mining operations based on the provided parameters.
     * This method is used for partial role analysis where is not nessessary to process all roles like in GUI.
     * It prepares user chunk (MiningUserTypeChunk) structure for role analysis.
     * Role chunk (MiningRoleTypeChunk) structure is empty array.
     *
     * @param roleAnalysisService The role analysis service.
     * @param cluster The cluster representing a group of roles for analysis.
     * @param userSearchFilter The additional user filter.
     * @param roleSearchFilter The additional role filter.
     * @param assignmentSearchFilter The additional assignment filter.
     * @param state The progress handler for monitoring role analysis.
     * @param task The task associated with this operation.
     * @param result The result object for tracking the operation's outcome.
     * @return A MiningOperationChunk containing mining information about user chunk (MiningUserTypeChunk).
     */
    @NotNull MiningOperationChunk preparePartialRoleBasedStructure(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisClusterType cluster,
            @Nullable SearchFilterType userSearchFilter,
            @Nullable SearchFilterType roleSearchFilter,
            @Nullable SearchFilterType assignmentSearchFilter,
            @NotNull RoleAnalysisProgressIncrement state,
            @NotNull Task task,
            @NotNull OperationResult result);

    /**
     * Prepares a partial user-based structure for mining operations based on the provided parameters.
     * This method is used for partial user analysis where is not nessessary to process all users like in GUI.
     * It prepares role chunk (MiningRoleTypeChunk) structure for role analysis.
     * User chunk (MiningUserTypeChunk) structure is empty array.
     *
     * @param roleAnalysisService The role analysis service.
     * @param cluster The cluster representing a group of roles for analysis.
     * @param userSearchFilter The additional user filter.
     * @param roleSearchFilter The additional role filter.
     * @param assignmentSearchFilter The additional assignment filter.
     * @param state The progress handler for monitoring role analysis.
     * @param task The task associated with this operation.
     * @param result The result object for tracking the operation's outcome.
     * @return A MiningOperationChunk containing mining information about role chunk (MiningRoleTypeChunk).
     */
    @NotNull MiningOperationChunk preparePartialUserBasedStructure(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull RoleAnalysisClusterType cluster,
            @Nullable SearchFilterType userSearchFilter,
            @Nullable SearchFilterType roleSearchFilter,
            @Nullable SearchFilterType assignmentSearchFilter,
            @NotNull RoleAnalysisProgressIncrement state,
            @NotNull Task task,
            @NotNull OperationResult result);

}
