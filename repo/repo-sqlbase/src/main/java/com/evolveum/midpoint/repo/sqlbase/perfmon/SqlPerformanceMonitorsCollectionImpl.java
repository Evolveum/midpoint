/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.perfmon;

import java.util.Collection;
import java.util.HashSet;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.api.SqlPerformanceMonitorsCollection;
import com.evolveum.midpoint.repo.api.perf.PerformanceInformation;
import com.evolveum.midpoint.repo.api.perf.PerformanceMonitor;
import com.evolveum.midpoint.util.annotation.Experimental;

/**
 * Maintains a collection of SQL performance monitors, typically one for the repository and one for SQL audit service.
 * <p>
 * Temporary implementation, created as a reaction to splitting single monitor into two in 4.2.
 * It will be replaced by something more serious in the future.
 * <p>
 * BEWARE: All public classes operating on monitors must be synchronized.
 */
@Experimental
public class SqlPerformanceMonitorsCollectionImpl implements SqlPerformanceMonitorsCollection {

    @NotNull private final Collection<PerformanceMonitor> monitors = new HashSet<>();

    @Override
    public synchronized void register(PerformanceMonitor monitor) {
        monitors.add(monitor);
    }

    @Override
    public synchronized void deregister(PerformanceMonitor monitor) {
        monitors.remove(monitor);
    }

    @Override
    public synchronized PerformanceInformation getGlobalPerformanceInformation() {
        PerformanceInformationImpl information = new PerformanceInformationImpl();
        for (PerformanceMonitor monitor : monitors) {
            information.mergeDistinct(monitor.getGlobalPerformanceInformation());
        }
        return information;
    }

    @Override
    public synchronized PerformanceInformation getThreadLocalPerformanceInformation() {
        PerformanceInformationImpl information = new PerformanceInformationImpl();
        for (PerformanceMonitor monitor : monitors) {
            information.mergeDistinct(monitor.getThreadLocalPerformanceInformation());
        }
        return information;
    }

    @Override
    public synchronized void startThreadLocalPerformanceInformationCollection() {
        for (PerformanceMonitor monitor : monitors) {
            monitor.startThreadLocalPerformanceInformationCollection();
        }
    }
}
