/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.activity.definition.ResourceObjectSetSpecificationImpl;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemPreprocessor;
import com.evolveum.midpoint.repo.common.activity.run.sources.SearchableItemSource;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.SimulationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ConfigurationSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationDefinitionType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

class NoOpAdvancedActivityRunSupport implements AdvancedActivityRunSupport {

    public static final NoOpAdvancedActivityRunSupport INSTANCE = new NoOpAdvancedActivityRunSupport();

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
    public ItemPreprocessor<ShadowType> createShadowFetchingPreprocessor(
            @NotNull Producer<Collection<SelectorOptions<GetOperationOptions>>> producerOptions,
            @NotNull SchemaService schemaService) {
        throw noModelAvailableException();
    }

    @Override
    public <C extends Containerable> SearchableItemSource getItemSourceFor(Class<C> type) {
        throw noModelAvailableException();
    }

    @Override
    public @NotNull SimulationResult createSimulationResult(
            @Nullable SimulationDefinitionType definition,
            @NotNull String rootTaskOid,
            @Nullable ConfigurationSpecificationType configurationSpecification,
            OperationResult result) {
        throw noModelAvailableException();
    }

    @Override
    public @NotNull SimulationResult getSimulationResult(
            @NotNull String resultOid, @NotNull OperationResult result) {
        throw noModelAvailableException();
    }

    private UnsupportedOperationException noModelAvailableException() {
        return new UnsupportedOperationException("Model-impl is not available");
    }
}
