/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.util.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/**
 *
 *  This filter provides functionality to profiling loggers to act as they don't inherit
 *  rootAppender, thus forwarding profiling and performance logs only into MIDPOINT_PROFILE_LOG.
 *  (we don't want them in MIDPOINT_LOG)
 *
 *  @author shood
 * */
public class ProfilingLogbackFilter extends Filter<ILoggingEvent> {

    /* Class Attributes */

    /* Attributes */
    private FilterReply onMatch = FilterReply.ACCEPT;
    private FilterReply onMismatch = FilterReply.DENY;
    private FilterReply neutralReply = FilterReply.NEUTRAL;

    private final String REQUEST_FILTER_LOGGER_CLASS_NAME = "com.evolveum.midpoint.web.util.MidPointProfilingServletFilter";
    private final String PROFILING_ASPECT_LOGGER = "com.evolveum.midpoint.util.aspect.ProfilingDataManager";

    /* BEHAVIOR */
    @Override
    public FilterReply decide(ILoggingEvent event) {

        if(REQUEST_FILTER_LOGGER_CLASS_NAME.equals(event.getLoggerName())){
            return onMismatch;
        }

        if(PROFILING_ASPECT_LOGGER.equals(event.getLoggerName())){
            return onMismatch;
        }

        return neutralReply;
    }   //decide end


}   //ProfilingLogbackFilter end
