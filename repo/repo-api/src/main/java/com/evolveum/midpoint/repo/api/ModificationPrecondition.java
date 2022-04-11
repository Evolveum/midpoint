/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
