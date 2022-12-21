/*
 * Copyright (C) 2017-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SimpleObjectResolver;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 */
public class RepoSimpleObjectResolver implements SimpleObjectResolver {

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService cacheRepositoryService;

    @Override
    public <O extends ObjectType> PrismObject<O> getObject(Class<O> expectedType, String oid,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        return cacheRepositoryService.getObject(expectedType, oid, options, result);
    }

    public <O extends ObjectType> void searchIterative(Class<O> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, ResultHandler<O> handler, Object task,
            OperationResult parentResult)
            throws SchemaException {
        cacheRepositoryService.searchObjectsIterative(type, query, handler, options, true, parentResult);
    }

    public static @NotNull RepoSimpleObjectResolver get() {
        return TestSpringBeans.getRepoSimpleObjectResolver();
    }
}
