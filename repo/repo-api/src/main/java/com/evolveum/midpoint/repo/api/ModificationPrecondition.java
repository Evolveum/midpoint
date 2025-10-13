/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.api;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

@FunctionalInterface
public interface ModificationPrecondition<T extends ObjectType> {

    /**
     * Problem can be reported either by returning false or by throwing PreconditionViolationException directly.
     * The former method is easier while the latter one gives a possibility to provide a custom exception message.
     */
    boolean holds(PrismObject<T> object) throws PreconditionViolationException;
}
