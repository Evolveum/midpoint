/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import com.evolveum.midpoint.schema.selector.eval.SelectorTraceEvent;
import com.evolveum.midpoint.schema.selector.eval.SelectorTraceEvent.End;
import com.evolveum.midpoint.schema.selector.eval.SelectorTraceEvent.Start;
import com.evolveum.midpoint.schema.traces.details.ProcessingTracer;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.security.enforcer.impl.TracingUtil.*;

/**
 * FIXME preliminary implementation
 */
class LogBasedSelectorTracer implements ProcessingTracer<SelectorTraceEvent> {

    private static final Trace LOGGER = TraceManager.getTrace(SecurityEnforcerImpl.class);

    private final boolean enabled = LOGGER.isTraceEnabled();

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void trace(@NotNull SelectorTraceEvent event) {

        String typeMark;
        if (event instanceof Start) {
            typeMark = START;
        } else if (event instanceof End) {
            typeMark = END;
        } else {
            typeMark = CONT;
        }

        String prefix = SEL_SPACE + event.getId() + typeMark;

        var record = event.defaultTraceRecord();
        var nextLines = record.nextLines();
        if (nextLines == null) {
            LOGGER.trace("{} {}", prefix, record.firstLine());
        } else {
            LOGGER.trace("{} {}\n{}", prefix, record.firstLine(), nextLines);
        }
    }
}
