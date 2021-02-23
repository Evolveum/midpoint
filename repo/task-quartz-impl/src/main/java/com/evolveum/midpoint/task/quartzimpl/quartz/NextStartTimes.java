/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.quartz;

import org.quartz.Trigger;

public class NextStartTimes {
    protected final Long nextScheduledRun;
    private final Long nextRetry;

    public NextStartTimes(Trigger standardTrigger, Trigger nextRetryTrigger) {
        this.nextScheduledRun = getTime(standardTrigger);
        this.nextRetry = getTime(nextRetryTrigger);
    }

    private Long getTime(Trigger t) {
        return t != null && t.getNextFireTime() != null ? t.getNextFireTime().getTime() : null;
    }

    public Long getNextScheduledRun() {
        return nextScheduledRun;
    }

    public Long getNextRetry() {
        return nextRetry;
    }
}
