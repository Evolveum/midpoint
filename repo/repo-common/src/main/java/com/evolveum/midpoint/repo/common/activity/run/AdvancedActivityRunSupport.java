/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.run;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.activity.definition.ResourceObjectSetSpecificationImpl;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemPreprocessor;
import com.evolveum.midpoint.repo.common.activity.run.sources.SearchableItemSource;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.SimulationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConfigurationSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationDefinitionType;

/**
 * Advanced features needed for activity run, like
 *
 * 1. calling `modelObjectResolver` for search/count operations,
 * 2. model-level processing of expressions in search queries,
 * 3. authorizations,
 * 4. resolving provisioning definitions in queries,
 * 5. managing simulation result objects.
 */
public interface AdvancedActivityRunSupport {

    /** Returns true if the real support is present. */
    boolean isPresent();

    @NotNull SearchSpecification<?> createSearchSpecificationFromResourceObjectSetSpec(
            @NotNull ResourceObjectSetSpecificationImpl resourceObjectSetSpecification, @NotNull RunningTask task,
            OperationResult result)
            throws SchemaException, ActivityRunException;

    /** Assuming that query has expressions. */
    ObjectQuery evaluateQueryExpressions(
            @NotNull ObjectQuery query, ExpressionProfile expressionProfile,
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

    ItemPreprocessor<ShadowType> createShadowFetchingPreprocessor(
            @NotNull Producer<Collection<SelectorOptions<GetOperationOptions>>> producerOptions,
            @NotNull SchemaService schemaService);

    /**
     * Returns item source suitable for processing items of given type.
     */
    <C extends Containerable> SearchableItemSource getItemSourceFor(Class<C> type);

    /**
     * Creates a simulation result into which the activity will store information about processed objects.
     *
     * The result will be open until the activity realization is done. The realization can span multiple tasks (for distributed
     * activities) and multiple task runs (in the case of suspend/resume actions).
     */
    @NotNull SimulationResult createSimulationResult(
            @Nullable SimulationDefinitionType definition,
            @NotNull Task rootTask,
            @Nullable ConfigurationSpecificationType configurationSpecification,
            OperationResult result)
            throws ConfigurationException;

    /** Provides a {@link SimulationResult} for given simulation result OID. May involve repository get operation. */
    @NotNull SimulationResult getSimulationResult(@NotNull String resultOid, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException;
}
