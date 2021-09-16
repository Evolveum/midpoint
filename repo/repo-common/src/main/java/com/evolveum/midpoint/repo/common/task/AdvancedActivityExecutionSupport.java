/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

/**
 * Advanced features needed for activity execution, like
 *
 * 1. calling `modelObjectResolver` for search/count operations,
 * 2. model-level processing of expressions in search queries,
 * 3. authorizations,
 * 4. resolving provisioning definitions in queries.
 */
public interface AdvancedActivityExecutionSupport {

    /** Returns true if the real support is present. */
    boolean isPresent();

    @NotNull SearchSpecification<?> createSearchSpecificationFromResourceObjectSetSpec(
            @NotNull ResourceObjectSetSpecificationImpl resourceObjectSetSpecification, @NotNull RunningTask task,
            OperationResult result)
            throws SchemaException, ActivityExecutionException;

    /** Assuming that query has expressions. */
    ObjectQuery evaluateQueryExpressions(@NotNull ObjectQuery query, ExpressionProfile expressionProfile,
            @NotNull RunningTask task, OperationResult result) throws CommonException;

    /**
     * Applies definitions to query. (Currently supported for provisioning definitions.)
     */
    void applyDefinitionsToQuery(@NotNull SearchSpecification<?> searchSpecification, @NotNull Task task,
            OperationResult result) throws CommonException;

    /**
     * Checks if the principal has an authorization to issue direct repo calls
     * even if they are not required by the activity implementation.
     */
    void checkRawAuthorization(Task task, OperationResult result) throws CommonException;

    /** Counts objects in "advanced way" (e.g. using model object resolver). */
    Integer countObjects(@NotNull SearchSpecification<?> searchSpecification, @NotNull RunningTask task,
            @NotNull OperationResult result) throws CommonException;

    /** Searches for objects in "advanced way" (e.g. using model object resolver). */
    <O extends ObjectType> void searchIterative(@NotNull SearchSpecification<O> searchSpecification,
            @NotNull ResultHandler<O> handler, @NotNull RunningTask task, @NotNull OperationResult result)
            throws CommonException;

    ObjectPreprocessor<ShadowType> createShadowFetchingPreprocessor(
            @NotNull SearchBasedActivityExecution<?, ?, ?, ?> activityExecution);
}
