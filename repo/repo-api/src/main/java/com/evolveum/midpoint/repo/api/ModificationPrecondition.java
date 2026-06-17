/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.api;

import java.util.Collection;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * A precondition that is checked before a modification is committed into the repository. It can e.g. check an object version,
 * or a specific object state. For even more dynamic executions please see {@link RepositoryService#modifyObjectDynamically(Class,
 * String, Collection, RepositoryService.ModificationsSupplier, RepoModifyOptions, OperationResult)}.
 *
 * @see RepositoryService#modifyObject(Class, String, Collection, ModificationPrecondition, RepoModifyOptions, OperationResult)}
 * @see VersionPrecondition
 */
@FunctionalInterface
public interface ModificationPrecondition<T extends ObjectType> {

    /**
     * Violation of the precondition can be reported either by returning false or by throwing
     * {@link PreconditionViolationException} directly.
     *
     * The former method is easier while the latter one gives a possibility to provide a custom exception message.
     */
    boolean holds(PrismObject<T> object) throws PreconditionViolationException;
}
