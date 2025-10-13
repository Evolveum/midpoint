/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.tasks.sources;

import java.util.List;

import com.evolveum.midpoint.repo.api.RepositoryService;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.repo.common.activity.run.SearchSpecification;
import com.evolveum.midpoint.repo.common.activity.run.sources.SearchableItemSource;
import com.evolveum.midpoint.schema.ContainerableResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.CommonException;

/**
 * Searches for container values at the model level.
 *
 * EXPERIMENTAL: Emulates iterative search using regular search. Will be adapted after iterative search
 * is provided at the model level.
 */
@Experimental
@Component
public class ModelContainerableItemSource implements SearchableItemSource {

    @Autowired private ModelService modelService;

    @Autowired private RepositoryService repositoryService;

    @Override
    public Integer count(@NotNull SearchSpecification<?> searchSpecification, @NotNull RunningTask task,
            @NotNull OperationResult result) throws CommonException {
        return modelService.countContainers(
                searchSpecification.getType(),
                searchSpecification.getQuery(),
                searchSpecification.getSearchOptions(),
                task, result);
    }

    @Override
    public <C extends Containerable> void searchIterative(@NotNull SearchSpecification<C> searchSpecification,
            @NotNull ContainerableResultHandler<C> handler, @NotNull RunningTask task, @NotNull OperationResult result)
            throws CommonException {
        if (repositoryService.isNative()) {
            modelService.searchContainersIterative(
                    searchSpecification.getType(),
                    searchSpecification.getQuery(),
                    handler,
                    searchSpecification.getSearchOptions(),
                    task, result);
        } else {
            List<C> items = modelService.searchContainers(
                    searchSpecification.getType(),
                    searchSpecification.getQuery(),
                    searchSpecification.getSearchOptions(),
                    task, result);
            for (C item : items) {
                if (!handler.handle(item, result)) {
                    break;
                }
            }
        }


    }
}
