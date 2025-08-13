/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.state.sync;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.common.activity.run.state.Initializable;
import com.evolveum.midpoint.schema.statistics.ActivitySynchronizationStatisticsUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivitySynchronizationStatisticsType;

/**
 * Must be thread-safe. Accessed e.g. from multiple worker tasks.
 */
public class ActivitySynchronizationStatistics extends Initializable {

    /** Current value. Guarded by this. */
    @NotNull private final ActivitySynchronizationStatisticsType value;

    public ActivitySynchronizationStatistics() {
        value = new ActivitySynchronizationStatisticsType();
    }

    public synchronized void initialize(ActivitySynchronizationStatisticsType initialValue) {
        doInitialize(() -> add(initialValue));
    }

    public synchronized void add(ActivitySynchronizationStatisticsType increment) {
        ActivitySynchronizationStatisticsUtil.addTo(this.value, increment);
    }

    /** Returns a current value of this statistics. It is copied because of thread safety issues. */
    public synchronized @NotNull ActivitySynchronizationStatisticsType getValueCopy() {
        assertInitialized();
        return value.cloneWithoutId();
    }
}
