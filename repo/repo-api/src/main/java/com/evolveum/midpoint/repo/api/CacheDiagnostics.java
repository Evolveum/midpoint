/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.api;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SingleCacheStateInformationType;

/**
 * Methods providing diagnostics on a cache - reporting the state and dumping the content.
 *
 * @see CacheInvalidationListener
 */
public interface CacheDiagnostics {

    /** Returns information about cache size and performance. */
    @NotNull Collection<SingleCacheStateInformationType> getStateInformation();

    /** Dumps the cache content into special log file. */
    void dumpContent();
}
