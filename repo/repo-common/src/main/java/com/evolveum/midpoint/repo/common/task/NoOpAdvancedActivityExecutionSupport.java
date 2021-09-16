/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

class NoOpAdvancedActivityExecutionSupport implements AdvancedActivityExecutionSupport {

    public static final NoOpAdvancedActivityExecutionSupport INSTANCE = new NoOpAdvancedActivityExecutionSupport();

    @Override
    public boolean isPresent() {
        return false;
    }

    @Override
    public @NotNull SearchSpecification<?> createSearchSpecificationFromResourceObjectSetSpec(
            @NotNull ResourceObjectSetSpecificationImpl resourceObjectSetSpecification, @NotNull RunningTask task, OperationResult result) {
        throw new UnsupportedOperationException("Resource objects set specification is not supported without model-impl");
    }

    @Override
    public ObjectQuery evaluateQueryExpressions(@NotNull ObjectQuery query, ExpressionProfile expressionProfile,
            @NotNull RunningTask task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        throw new UnsupportedOperationException("Query expressions are not supported without model-impl");
    }

    @Override
    public void applyDefinitionsToQuery(@NotNull SearchSpecification<?> searchSpecification, @NotNull Task task,
            OperationResult result) {
        // no-op
    }

    @Override
    public void checkRawAuthorization(Task task, OperationResult result) {
        throw new UnsupportedOperationException("Couldn't check for 'raw' authorization without model-impl");
    }

    @Override
    public Integer countObjects(@NotNull SearchSpecification<?> searchSpecification, @NotNull RunningTask task,
            @NotNull OperationResult result) throws CommonException {
        throw new UnsupportedOperationException("Model-impl is not available");
    }

    @Override
    public <O extends ObjectType> void searchIterative(@NotNull SearchSpecification<O> searchSpecification,
            @NotNull ResultHandler<O> handler, @NotNull RunningTask task, @NotNull OperationResult result) {
        throw new UnsupportedOperationException("Model-impl is not available");
    }

    @Override
    public ObjectPreprocessor<ShadowType> createShadowFetchingPreprocessor(
            @NotNull SearchBasedActivityExecution<?, ?, ?, ?> activityExecution) {
        throw new UnsupportedOperationException("Model-impl is not available");
    }
}
