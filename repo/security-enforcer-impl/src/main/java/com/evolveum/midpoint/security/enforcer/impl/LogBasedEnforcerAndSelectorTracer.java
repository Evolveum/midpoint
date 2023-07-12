/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import static com.evolveum.midpoint.security.enforcer.impl.TracingUtil.*;

import com.evolveum.midpoint.schema.selector.eval.SelectorTraceEvent;
import com.evolveum.midpoint.schema.traces.details.AbstractTraceEvent;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.security.enforcer.impl.SecurityTraceEvent.AuthorizationRelated;

import com.evolveum.midpoint.security.enforcer.impl.SecurityTraceEvent.OperationRelated;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.traces.details.ProcessingTracer;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.Nullable;

import java.util.stream.Collectors;

/**
 * Facilitates troubleshooting of authorizations and their components.
 *
 * FIXME preliminary implementation
 */
public class LogBasedEnforcerAndSelectorTracer implements
        ProcessingTracer<AbstractTraceEvent> {

    private static final Trace LOGGER = TraceManager.getTrace(SecurityEnforcerImpl.class);

    /** Additional sink for log messages; used e.g. when diagnosing authorizations via GUI. */
    @Nullable private final SecurityEnforcer.LogCollector logCollector;

    private final boolean traceEnabled = LOGGER.isTraceEnabled();

    LogBasedEnforcerAndSelectorTracer(@Nullable SecurityEnforcer.LogCollector logCollector) {
        this.logCollector = logCollector;
    }

    @Override
    public boolean isEnabled() {
        return traceEnabled || logCollector != null;
    }

    @Override
    public void trace(@NotNull AbstractTraceEvent event) {
        boolean sendToLogCollectorIfDefined;
        String prefix;
        if (event instanceof SelectorTraceEvent selectorEvent) {
            prefix = getSelectorEventPrefix(selectorEvent);
            sendToLogCollectorIfDefined = logCollector != null && logCollector.isSelectorTracingEnabled();
        } else if (event instanceof SecurityTraceEvent securityEvent) {
            prefix = getSecurityEventPrefix(securityEvent);
            sendToLogCollectorIfDefined = true;
        } else {
            throw new IllegalStateException("Unsupported trace event type: " + event);
        }

        logEvent(event, prefix, sendToLogCollectorIfDefined);
    }

    private static String getSelectorEventPrefix(@NotNull SelectorTraceEvent event) {
        String typeMark;
        if (event instanceof SelectorTraceEvent.Start) {
            typeMark = START;
        } else if (event instanceof SelectorTraceEvent.End) {
            typeMark = END;
        } else {
            typeMark = CONT;
        }
        return SEL_SPACE + event.getId() + typeMark;
    }

    private static String getSecurityEventPrefix(@NotNull SecurityTraceEvent event) {
        String extraPrefix;
        String typeMark;
        if (event instanceof SecurityTraceEvent.Start) {
            typeMark = START;
            extraPrefix = "";
        } else if (event instanceof SecurityTraceEvent.End) {
            typeMark = END;
            extraPrefix = "";
        } else {
            typeMark = CONT;
            extraPrefix = INTERIOR_SPACE;
        }

        String prefix;
        if (event instanceof SecurityTraceEvent.PartialFilterOperationRelated<?> p) {
            prefix = extraPrefix + PARTIAL_SEC_SPACE + p.getId() + typeMark;
        } else if (event instanceof OperationRelated<?>) {
            prefix = extraPrefix + SEC + typeMark;
        } else if (event instanceof AuthorizationRelated a) {
            prefix = extraPrefix + AUTZ_SPACE + a.getId() + typeMark;
        } else {
            prefix = "??? " + typeMark;
        }
        return prefix;
    }

    private void logEvent(@NotNull AbstractTraceEvent event, String prefix, boolean sendToLogCollectorIfDefined) {
        var record = event.defaultTraceRecord();
        var nextLines = record.nextLines();
        if (nextLines == null) {
            if (traceEnabled) {
                LOGGER.trace("{} {}", prefix, record.firstLine());
            }
            if (logCollector != null && sendToLogCollectorIfDefined) {
                logCollector.log(prefix + " " + record.firstLine());
            }
        } else {
            if (traceEnabled) {
                LOGGER.trace("{} {}\n{}", prefix, record.firstLine(), nextLines);
            }
            if (logCollector != null && sendToLogCollectorIfDefined) {
                logCollector.log(
                        prefix + " " + record.firstLine() + "\n"
                                + applyPrefixToEachLine(prefix + " ", nextLines));
            }
        }
    }

    private String applyPrefixToEachLine(String prefix, String lines) {
        return lines.lines()
                .map(line -> prefix + line)
                .collect(Collectors.joining("\n"));
    }
}
