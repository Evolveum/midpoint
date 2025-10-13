/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
