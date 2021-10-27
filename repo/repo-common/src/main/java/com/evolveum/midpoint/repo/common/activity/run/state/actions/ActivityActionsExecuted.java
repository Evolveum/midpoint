/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.state.actions;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.run.state.Initializable;
import com.evolveum.midpoint.schema.statistics.ActionsExecutedInformationUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityActionsExecutedType;

/**
 * Must be thread-safe. Accessed e.g. from multiple worker tasks.
 */
public class ActivityActionsExecuted extends Initializable {

    /** Current value. Guarded by this. */
    @NotNull private final ActivityActionsExecutedType value;

    public ActivityActionsExecuted() {
        value = new ActivityActionsExecutedType();
    }

    public synchronized void initialize(ActivityActionsExecutedType initialValue) {
        doInitialize(() -> add(initialValue));
    }

    public synchronized void add(ActivityActionsExecutedType increment) {
        ActionsExecutedInformationUtil.addTo(this.value, increment);
    }

    /** Returns a current value of this statistics. It is copied because of thread safety issues. */
    public synchronized @NotNull ActivityActionsExecutedType getValueCopy() {
        assertInitialized();
        return value.clone();
    }
}
