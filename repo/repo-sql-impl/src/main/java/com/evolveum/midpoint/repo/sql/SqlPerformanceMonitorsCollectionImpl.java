/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql;

import java.util.Collection;
import java.util.HashSet;

import com.evolveum.midpoint.repo.api.SqlPerformanceMonitorsCollection;
import com.evolveum.midpoint.repo.api.perf.PerformanceInformation;
import com.evolveum.midpoint.repo.sql.perf.PerformanceInformationImpl;
import com.evolveum.midpoint.repo.sql.perf.SqlPerformanceMonitorImpl;
import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

/**
 * Maintains a collection of SQL performance monitors, typically one for the repository and one for SQL audit service.
 *
 * Temporary implementation, created as a reaction to splitting single monitor into two in 4.2.
 * It will be replaced by something more serious in the future.
 *
 * BEWARE: All public classes operating on monitors must be synchronized.
 */
@Experimental
@Component
public class SqlPerformanceMonitorsCollectionImpl implements SqlPerformanceMonitorsCollection {

    @NotNull private final Collection<SqlPerformanceMonitorImpl> monitors = new HashSet<>();

    public synchronized void register(SqlPerformanceMonitorImpl monitor) {
        monitors.add(monitor);
    }

    public synchronized void deregister(SqlPerformanceMonitorImpl monitor) {
        monitors.remove(monitor);
    }

    @Override
    public synchronized PerformanceInformation getGlobalPerformanceInformation() {
        PerformanceInformationImpl information = new PerformanceInformationImpl();
        for (SqlPerformanceMonitorImpl monitor : monitors) {
            information.mergeDistinct(monitor.getGlobalPerformanceInformation());
        }
        return information;
    }

    @Override
    public synchronized PerformanceInformation getThreadLocalPerformanceInformation() {
        PerformanceInformationImpl information = new PerformanceInformationImpl();
        for (SqlPerformanceMonitorImpl monitor : monitors) {
            information.mergeDistinct(monitor.getThreadLocalPerformanceInformation());
        }
        return information;
    }

    @Override
    public synchronized void startThreadLocalPerformanceInformationCollection() {
        for (SqlPerformanceMonitorImpl monitor : monitors) {
            monitor.startThreadLocalPerformanceInformationCollection();
        }
    }
}
