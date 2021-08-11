/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.mapping;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.mapping.ItemRelationResolver;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Extension of {@link ItemRelationResolver}, this is a common contract for resolver
 * that helps with navigating over complex (non-single) item paths for both query
 * and application of delta modification.
 */
public interface SqaleItemRelationResolver<
        Q extends FlexibleRelationalPathBase<R>, R,
        TQ extends FlexibleRelationalPathBase<TR>, TR>
        extends ItemRelationResolver<Q, R, TQ, TR> {

    /**
     * Resolves current query context to a new context (mapping is always part of context).
     * The information about the resolved item is captured in the instance resolver already
     * in a manner that is specific for various types of resolution (JOIN or nested mapping).
     * Optional {@link ItemPath} is provided for cases when container ID is necessary.
     *
     * Return value `null` indicates that the modification using the resolver should be ignored
     * by the repository.
     */
    SqaleUpdateContext<?, ?, ?> resolve(SqaleUpdateContext<?, Q, R> context, ItemPath itemPath)
            throws RepositoryException;
}
