/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.api;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SingleCacheStateInformationType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface Cacheable {

    void invalidate(Class<?> type, String oid, CacheInvalidationContext context);

    @NotNull
    Collection<SingleCacheStateInformationType> getStateInformation();

    void dumpContent();
}
