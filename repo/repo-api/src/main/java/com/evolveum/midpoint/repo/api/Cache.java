/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.api;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SingleCacheStateInformationType;

/**
 * Defines basic contract for local caches (various caching components or services).
 */
public interface Cache {

    void invalidate(Class<?> type, String oid, CacheInvalidationContext context);

    @NotNull
    Collection<SingleCacheStateInformationType> getStateInformation();

    void dumpContent();
}
