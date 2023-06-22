/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import static com.evolveum.midpoint.security.enforcer.impl.TracingUtil.*;

import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.security.enforcer.impl.SecurityTraceEvent.AuthorizationRelated;

import com.evolveum.midpoint.security.enforcer.impl.SecurityTraceEvent.OperationRelated;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.traces.details.ProcessingTracer;
import com.evolveum.midpoint.security.enforcer.impl.SecurityTraceEvent.End;
import com.evolveum.midpoint.security.enforcer.impl.SecurityTraceEvent.Start;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.Nullable;

/**
 * Facilitates troubleshooting of authorizations and their components.
 *
 * FIXME preliminary implementation
 */
public class LogBasedEnforcerTracer implements ProcessingTracer<SecurityTraceEvent> {

    private static final Trace LOGGER = TraceManager.getTrace(SecurityEnforcerImpl.class);

    /** Additional sink for log messages; used e.g. when diagnosing authorizations via GUI. */
    @Nullable private final SecurityEnforcer.LogCollector logCollector;

    private final boolean traceEnabled = LOGGER.isTraceEnabled();

    LogBasedEnforcerTracer(@Nullable SecurityEnforcer.LogCollector logCollector) {
        this.logCollector = logCollector;
    }

    @Override
    public boolean isEnabled() {
        return traceEnabled || logCollector != null;
    }

    @Override
    public void trace(@NotNull SecurityTraceEvent event) {
        String extraPrefix;
        String typeMark;
        if (event instanceof Start) {
            typeMark = START;
            extraPrefix = "";
        } else if (event instanceof End) {
            typeMark = END;
            extraPrefix = "";
        } else {
            typeMark = CONT;
            extraPrefix = INTERIOR_SPACE;
        }

        String prefix;
        if (event instanceof SecurityTraceEvent.PartialFilterOperationRelated<?>) {
            prefix = extraPrefix + PARTIAL_SEC + typeMark;
        } else if (event instanceof OperationRelated<?>) {
            prefix = extraPrefix + SEC + typeMark;
        } else if (event instanceof AuthorizationRelated a) {
            prefix = extraPrefix + AUTZ_SPACE + a.getId() + typeMark;
        } else {
            prefix = "??? " + typeMark;
        }

        var record = event.defaultTraceRecord();
        var nextLines = record.nextLines();
        if (nextLines == null) {
            if (traceEnabled) {
                LOGGER.trace("{} {}", prefix, record.firstLine());
            }
            if (logCollector != null) {
                logCollector.log(prefix + " " + record.firstLine());
            }
        } else {
            if (traceEnabled) {
                LOGGER.trace("{} {}\n{}", prefix, record.firstLine(), nextLines);
            }
            if (logCollector != null) {
                logCollector.log(prefix + " " + record.firstLine() + "\n" + nextLines);
            }
        }
    }
}
