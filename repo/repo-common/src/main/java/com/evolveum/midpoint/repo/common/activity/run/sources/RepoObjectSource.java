/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.sources;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

import com.evolveum.midpoint.repo.api.RepositoryService;

import com.evolveum.midpoint.repo.common.activity.run.SearchSpecification;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.schema.ContainerableResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Provides access to objects at the repository level.
 */
@Component
public class RepoObjectSource implements SearchableItemSource {

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;

    @Override
    public Integer count(@NotNull SearchSpecification<?> searchSpecification, @NotNull RunningTask task,
            @NotNull OperationResult result) throws CommonException {
        return repositoryService.countObjects(
                getObjectType(searchSpecification.getType()),
                searchSpecification.getQuery(),
                searchSpecification.getSearchOptions(),
                result);
    }

    @Override
    public <C extends Containerable> void searchIterative(@NotNull SearchSpecification<C> searchSpecification,
            @NotNull ContainerableResultHandler<C> handler, @NotNull RunningTask task, @NotNull OperationResult result)
            throws CommonException {
        //noinspection unchecked
        repositoryService.searchObjectsIterative(
                getObjectType(searchSpecification.getType()),
                searchSpecification.getQuery(),
                (object, localResult) ->
                        handler.handle((C) object.asObjectable(), localResult),
                searchSpecification.getSearchOptions(),
                true,
                result);
    }

    public static Class<? extends ObjectType> getObjectType(Class<?> type) {
        argCheck(ObjectType.class.isAssignableFrom(type), "Type is not a subtype of ObjectType: %s", type);
        //noinspection unchecked
        return (Class<? extends ObjectType>) type;
    }
}
