/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import java.util.Collection;

import com.evolveum.midpoint.schema.util.task.work.ObjectSetUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.task.work.ItemDefinitionProvider;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

/**
 * Provides execution logic and/or execution state related to a search-based activity execution.
 *
 * Main responsibilities:
 *
 * 1. search specification formulation and customization,
 * 2. object processing.
 *
 * Note that we *do not* allow customizing the type of objects being processed (e.g. by restricting them to {@link ShadowType}).
 * The reason is that the type has to be known a little ahead e.g. to interpret the configured object query.
 * The correct way how to prescribe object type is to use {@link ObjectSetUtil#assumeObjectType(ObjectSetType, QName)} method
 * when constructing the work definition.
 *
 * @param <O> Type of objects processed by the activity
 */
@SuppressWarnings("RedundantThrows")
public interface SearchBasedActivityExecutionSpecifics<O extends ObjectType>
        extends IterativeActivityExecutionSpecifics {

    //region 1. Search specification formulation and customization
    /**
     * Creates a complete search specification.
     *
     * If the activity is not interested in this kind of customization, it should return null, and the default search
     * specification is then produced by the {@link SearchBasedActivityExecution}.
     *
     * Note: freely add {@link CommonException} and {@link ActivityExecutionException} to the signature of this method if needed.
     */
    default @Nullable SearchSpecification<O> createSearchSpecification(OperationResult result) {
        return null;
    }

    /**
     * Customizes a query present in the original search specification (usually derived from the task configuration).
     * The activity can either add specific clauses here, or rewrite the query altogether.
     */
    default ObjectQuery customizeQuery(ObjectQuery configuredQuery, OperationResult result) throws CommonException {
        return configuredQuery;
    }

    /**
     * Customizes search options present in the original search specification (usually derived from the task configuration).
     * The activity can either add or modify some options, or replace the whole options by its own version.
     */
    default Collection<SelectorOptions<GetOperationOptions>> customizeSearchOptions(
            Collection<SelectorOptions<GetOperationOptions>> configuredOptions, OperationResult result) throws CommonException {
        return configuredOptions;
    }

    /**
     * @return True if the activity requires direct repository access. Note that the framework does not check the authorizations
     * of the user running the activity in such cases. If an autz check is needed, the activity is responsible for that.
     */
    default boolean doesRequireDirectRepositoryAccess() {
        return false;
    }

    /**
     * Returns a provider of definitions for runtime items (e.g. attributes) that are needed in bucket filters.
     * Usually needed for activities that work with resource objects.
     */
    default ItemDefinitionProvider createItemDefinitionProvider() {
        return null;
    }
    //endregion

    //region 2. Object processing
    /**
     * Processes given object that came as part of a request.
     *
     * BEWARE: Object may have been preprocessed, and may be different from the object present in the request.
     */
    boolean processObject(@NotNull PrismObject<O> object, @NotNull ItemProcessingRequest<PrismObject<O>> request,
            RunningTask workerTask, OperationResult result) throws CommonException, ActivityExecutionException;
    //endregion
}
