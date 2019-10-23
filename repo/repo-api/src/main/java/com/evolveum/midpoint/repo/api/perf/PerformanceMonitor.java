/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.api.perf;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RepositoryStatisticsReportingConfigurationType;

/**
 *  EXPERIMENTAL. Probably temporary.
 */
public interface PerformanceMonitor {

    void clearGlobalPerformanceInformation();

    PerformanceInformation getGlobalPerformanceInformation();

    /**
     * Starts gathering thread-local performance information, clearing existing (if any).
     */
    void startThreadLocalPerformanceInformationCollection();

    /**
     * Stops gathering thread-local performance information, clearing existing (if any).
     */
    void stopThreadLocalPerformanceInformationCollection();

    PerformanceInformation getThreadLocalPerformanceInformation();

    void setConfiguration(RepositoryStatisticsReportingConfigurationType statistics);
}
