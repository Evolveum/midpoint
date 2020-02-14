/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.util.statistics;

import com.evolveum.midpoint.util.annotation.Experimental;

/**
 *  EXPERIMENTAL.
 */
@Experimental
public interface OperationsPerformanceMonitor {

    OperationsPerformanceMonitor INSTANCE = OperationsPerformanceMonitorImpl.INSTANCE;

    void clearGlobalPerformanceInformation();

    OperationsPerformanceInformation getGlobalPerformanceInformation();

    /**
     * Starts gathering thread-local performance information, clearing existing (if any).
     */
    void startThreadLocalPerformanceInformationCollection();

    /**
     * Stops gathering thread-local performance information, clearing existing (if any).
     */
    void stopThreadLocalPerformanceInformationCollection();

    OperationsPerformanceInformation getThreadLocalPerformanceInformation();
}
