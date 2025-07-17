/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;

import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.repo.common.activity.run.buckets.ItemDefinitionProvider;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.work.ObjectSetUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Provides execution logic and/or execution state related to a search-based activity run.
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
 * @param <C> Type of objects processed by the activity
 */
@SuppressWarnings("RedundantThrows")
public interface SearchBasedActivityRunSpecifics<C extends Containerable>
        extends IterativeActivityRunSpecifics {

    //region 1. Search specification formulation and customization
    /**
     * Creates a complete search specification.
     *
     * If the activity is not interested in this kind of customization, it should return null, and the default search
     * specification is then produced by the {@link SearchBasedActivityRun}.
     *
     * Note: freely add {@link CommonException} and {@link ActivityRunException} to the signature of this method if needed.
     */
    default @Nullable SearchSpecification<C> createCustomSearchSpecification(OperationResult result)
            throws SchemaException, ConfigurationException {
        return null;
    }

    // TODO we may consider merging `customizeQuery` and `customizeSearchOptions`

    /**
     * Customizes a query present in the original search specification (usually derived from the task configuration).
     * The activity can either add specific clauses here, or rewrite the query altogether.
     */
    default void customizeQuery(SearchSpecification<C> searchSpecification, OperationResult result) throws CommonException {
    }

    /**
     * Customizes search options present in the original search specification (usually derived from the task configuration).
     * The activity can either add or modify some options in the provided object - if it's not null - or
     * replace the whole object by its own version.
     */
    default void customizeSearchOptions(SearchSpecification<C> searchSpecification, OperationResult result)
            throws CommonException {
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
     * Processes given item that came as part of a request.
     *
     * BEWARE: Item may have been preprocessed, and may be different from the item present in the request.
     */
    boolean processItem(@NotNull C item, @NotNull ItemProcessingRequest<C> request,
            RunningTask workerTask, OperationResult result) throws CommonException, ActivityRunException;
    //endregion
}
