/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.other;

import com.evolveum.midpoint.repo.api.RepositoryPerformanceMonitor;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.schema.util.DiagnosticContextHolder;
import com.evolveum.midpoint.schema.cache.CacheUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Utility methods related to operations and cache performance monitoring.
 */
public class MonitoringUtil {

    private static final Trace REPO_CACHE_LOGGER = TraceManager.getTrace(RepositoryCache.class);
    private static final Trace PERFORMANCE_ADVISOR = TraceManager.getPerformanceAdvisorTrace();

    public static Long repoOpStart() {
        RepositoryPerformanceMonitor monitor = DiagnosticContextHolder.get(RepositoryPerformanceMonitor.class);
        if (monitor == null) {
            return null;
        } else {
            return System.currentTimeMillis();
        }
    }

    public static void repoOpEnd(Long startTime) {
        RepositoryPerformanceMonitor monitor = DiagnosticContextHolder.get(RepositoryPerformanceMonitor.class);
        if (monitor != null) {
            monitor.recordRepoOperation(System.currentTimeMillis() - startTime);
        }
    }

    public static void log(String message, boolean info, Object... params) {
        CacheUtil.log(REPO_CACHE_LOGGER, PERFORMANCE_ADVISOR, message, info, params);
    }
}
