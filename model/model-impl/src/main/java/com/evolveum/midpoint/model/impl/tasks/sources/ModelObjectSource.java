/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.tasks.sources;

import static com.evolveum.midpoint.repo.common.activity.run.sources.RepoObjectSource.getObjectType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.repo.common.activity.run.SearchSpecification;
import com.evolveum.midpoint.repo.common.activity.run.sources.SearchableItemSource;
import com.evolveum.midpoint.schema.ContainerableResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;

/**
 * Provides access to objects at the model level.
 */
@Component
public class ModelObjectSource implements SearchableItemSource {

    @Autowired private ModelObjectResolver modelObjectResolver;

    @Override
    public Integer count(@NotNull SearchSpecification<?> searchSpecification, @NotNull RunningTask task,
            @NotNull OperationResult result) throws CommonException {
        return modelObjectResolver.countObjects(
                getObjectType(searchSpecification.getType()),
                searchSpecification.getQuery(),
                searchSpecification.getSearchOptions(),
                task, result);
    }

    @Override
    public <C extends Containerable> void searchIterative(@NotNull SearchSpecification<C> searchSpecification,
            @NotNull ContainerableResultHandler<C> handler, @NotNull RunningTask task, @NotNull OperationResult result)
            throws CommonException {
        //noinspection unchecked
        modelObjectResolver.searchIterative(
                getObjectType(searchSpecification.getType()),
                searchSpecification.getQuery(),
                searchSpecification.getSearchOptions(),
                (object, localResult) ->
                        handler.handle((C) object.asObjectable(), localResult),
                task, result);
    }
}
