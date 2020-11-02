/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.api;

import com.evolveum.midpoint.repo.api.perf.PerformanceInformation;
import com.evolveum.midpoint.util.annotation.Experimental;

/**
 * Used to access performance monitors. A temporary solution.
 */
@Experimental
public interface SqlPerformanceMonitorsCollection {

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
