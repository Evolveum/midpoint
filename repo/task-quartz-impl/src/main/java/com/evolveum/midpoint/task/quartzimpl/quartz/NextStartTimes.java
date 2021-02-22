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
