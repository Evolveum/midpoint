/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
