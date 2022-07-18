/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.api;

import java.util.Collection;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

@Deprecated
public interface CacheListener extends CacheInvalidationListener {

    @Override
    default Collection<CacheInvalidationEventSpecification> getEventSpecifications() {
        return CacheInvalidationEventSpecification.ALL_AVAILABLE_EVENTS;
    }

}
