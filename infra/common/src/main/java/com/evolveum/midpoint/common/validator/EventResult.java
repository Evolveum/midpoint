/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.common.validator;

/**
 * @author semancik
 *
 */
public final class  EventResult {

    public enum EventResultStatus {

        /**
         * Continue normal processing.
         */
        CONTINUE,

        /**
         * Skip the rest of processing of this object, continue with the next object.
         */
        SKIP_OBJECT,

        /**
         * Stop processing.
         */
        STOP;
    }

    private EventResultStatus status;
    private String reason;

    private EventResult(EventResultStatus status, String reason) {
        super();
        this.status = status;
        this.reason = reason;
    }

    public static EventResult cont() {
        return new EventResult(EventResultStatus.CONTINUE,null);
    }

    public static EventResult skipObject() {
        return new EventResult(EventResultStatus.SKIP_OBJECT,null);
    }

    public static EventResult skipObject(String reason) {
        return new EventResult(EventResultStatus.SKIP_OBJECT, reason);
    }

    public static EventResult stop() {
        return new EventResult(EventResultStatus.STOP,null);
    }

    public static EventResult stop(String reason) {
        return new EventResult(EventResultStatus.STOP,reason);
    }

    public EventResultStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public boolean isCont() {
        return status==EventResultStatus.CONTINUE;
    }

    public boolean isStop() {
        return status==EventResultStatus.STOP;
    }

}
