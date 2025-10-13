/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
