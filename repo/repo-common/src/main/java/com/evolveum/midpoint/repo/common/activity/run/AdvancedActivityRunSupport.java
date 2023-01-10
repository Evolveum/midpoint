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
import com.evolveum.midpoint.task.api.AggregatedObjectProcessingListener;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Advanced features needed for activity run, like
 *
 * 1. calling `modelObjectResolver` for search/count operations,
 * 2. model-level processing of expressions in search queries,
 * 3. authorizations,
 * 4. resolving provisioning definitions in queries.
 */
public interface AdvancedActivityRunSupport {

    /** Returns true if the real support is present. */
    boolean isPresent();

    @NotNull SearchSpecification<?> createSearchSpecificationFromResourceObjectSetSpec(
            @NotNull ResourceObjectSetSpecificationImpl resourceObjectSetSpecification, @NotNull RunningTask task,
            OperationResult result)
            throws SchemaException, ActivityRunException;

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

    ItemPreprocessor<ShadowType> createShadowFetchingPreprocessor(
            @NotNull Producer<Collection<SelectorOptions<GetOperationOptions>>> producerOptions,
            @NotNull SchemaService schemaService);

    /**
     * Returns item source suitable for processing items of given type.
     */
    <C extends Containerable> SearchableItemSource getItemSourceFor(Class<C> type);

    /** Creates a simulation result into which the activity will store information about processed objects. */
    @NotNull ObjectReferenceType createSimulationResult(OperationResult result);

    /** TODO better name */
    @NotNull AggregatedObjectProcessingListener getObjectProcessingListener(ObjectReferenceType simulationResultRef);
}
