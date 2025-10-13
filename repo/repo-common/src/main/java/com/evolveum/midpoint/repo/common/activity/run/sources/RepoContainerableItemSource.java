/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.run.sources;

import java.util.List;

import com.evolveum.midpoint.repo.common.activity.run.SearchSpecification;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ContainerableResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.CommonException;

/**
 * Searches for container values at the repository level.
 *
 * EXPERIMENTAL: Emulates iterative search using regular search. Will be adapted after iterative search
 * is provided at the model level.
 */
@Experimental
@Component
class RepoContainerableItemSource implements SearchableItemSource {

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;

    @Override
    public Integer count(@NotNull SearchSpecification<?> searchSpecification, @NotNull RunningTask task,
            @NotNull OperationResult result) throws CommonException {
        return repositoryService.countContainers(
                searchSpecification.getType(),
                searchSpecification.getQuery(),
                searchSpecification.getSearchOptions(),
                result);
    }

    @Override
    public <C extends Containerable> void searchIterative(@NotNull SearchSpecification<C> searchSpecification,
            @NotNull ContainerableResultHandler<C> handler, @NotNull RunningTask task, @NotNull OperationResult result)
            throws CommonException {
        if (repositoryService.isNative()) {
            repositoryService.searchContainersIterative(
                    searchSpecification.getType(),
                    searchSpecification.getQuery(),
                    handler,
                    searchSpecification.getSearchOptions(),
                    result);
        } else {
            List<C> items = repositoryService.searchContainers(
                    searchSpecification.getType(),
                    searchSpecification.getQuery(),
                    searchSpecification.getSearchOptions(),
                    result);
            for (C item : items) {
                if (!handler.handle(item, result)) {
                    break;
                }
            }
        }
    }
}
