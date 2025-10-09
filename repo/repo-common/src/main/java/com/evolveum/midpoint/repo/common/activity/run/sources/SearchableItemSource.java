/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.run.sources;

import com.evolveum.midpoint.repo.common.activity.run.SearchSpecification;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.schema.ContainerableResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;

/**
 * Provides access to items (and their count) based on the search specification.
 *
 * Implementations differ at the level used (model vs repo) and to type of objects,
 * reflected in different APIs (objects, audit records, containerables).
 */
public interface SearchableItemSource {

    /**
     * Counts items according to given search specification.
     */
    Integer count(@NotNull SearchSpecification<?> searchSpecification, @NotNull RunningTask task,
            @NotNull OperationResult result) throws CommonException;

    /**
     * Searches for items according to given search specification.
     */
    <C extends Containerable> void searchIterative(@NotNull SearchSpecification<C> searchSpecification,
            @NotNull ContainerableResultHandler<C> handler, @NotNull RunningTask task, @NotNull OperationResult result)
            throws CommonException;
}
