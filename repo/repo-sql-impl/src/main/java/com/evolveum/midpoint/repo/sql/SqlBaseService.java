/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.repo.sql.perf.SqlPerformanceMonitorImpl;

/**
 * Common supertype for SQL-based repository-like services.
 */
public abstract class SqlBaseService {

    private SqlPerformanceMonitorImpl performanceMonitor;

    public abstract SqlRepositoryConfiguration sqlConfiguration();

    public synchronized SqlPerformanceMonitorImpl getPerformanceMonitor() {
        if (performanceMonitor == null) {
            SqlRepositoryConfiguration config = sqlConfiguration();
            performanceMonitor = new SqlPerformanceMonitorImpl(
                    config.getPerformanceStatisticsLevel(), config.getPerformanceStatisticsFile());
        }

        return performanceMonitor;
    }

    public void destroy() {
        if (performanceMonitor != null) {
            performanceMonitor.shutdown();
        }
    }
}
