/*
 * Copyright (C) 2020-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.api;

import com.evolveum.midpoint.repo.api.perf.PerformanceInformation;
import com.evolveum.midpoint.repo.api.perf.PerformanceMonitor;
import com.evolveum.midpoint.util.annotation.Experimental;

/**
 * Used to access performance monitors. A temporary solution.
 */
@Experimental
public interface SqlPerformanceMonitorsCollection {

    void register(PerformanceMonitor monitor);
    void deregister(PerformanceMonitor monitor);
    /**
     * Returns global performance information aggregated from all monitors.
     */
    PerformanceInformation getGlobalPerformanceInformation();

    /**
     * Returns thread-local performance information aggregated from all monitors.
     */
    PerformanceInformation getThreadLocalPerformanceInformation();

    /**
     * Starts collection of thread local performance information in all monitors.
     */
    void startThreadLocalPerformanceInformationCollection();
}
