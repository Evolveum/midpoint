/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.tasks.sources;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelAuditService;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.repo.common.activity.run.SearchSpecification;
import com.evolveum.midpoint.repo.common.activity.run.sources.SearchableItemSource;
import com.evolveum.midpoint.schema.ContainerableResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;

import static com.evolveum.midpoint.repo.common.activity.run.sources.RepoAuditItemSource.toAuditResultHandler;

/**
 * Provides access to audit events at the model level.
 */
@Component
public class ModelAuditItemSource implements SearchableItemSource {

    @Autowired private ModelAuditService modelAuditService;

    @Override
    public Integer count(@NotNull SearchSpecification<?> searchSpecification, @NotNull RunningTask task,
            @NotNull OperationResult result) throws CommonException {
        return modelAuditService.countObjects(
                searchSpecification.getQuery(),
                searchSpecification.getSearchOptions(),
                task, result);
    }

    @Override
    public <C extends Containerable> void searchIterative(@NotNull SearchSpecification<C> searchSpecification,
            @NotNull ContainerableResultHandler<C> handler, @NotNull RunningTask task,
            @NotNull OperationResult result)
            throws CommonException {
        modelAuditService.searchObjectsIterative(
                searchSpecification.getQuery(),
                searchSpecification.getSearchOptions(),
                toAuditResultHandler(handler), task, result);
    }
}
