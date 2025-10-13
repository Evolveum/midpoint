/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.api;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

/**
 * @author semancik
 */
@FunctionalInterface
public interface RepositoryOperation<O extends ObjectType, R> {

    R run(@NotNull PrismObject<O> object)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException,
            PreconditionViolationException, ConfigurationException;
}
