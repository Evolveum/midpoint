/*
 * Copyright (c) 2010-2013 Evolveum
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

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.evolveum.midpoint.util.logging.impl.TraceImpl;

/**
 * Factory for trace instances.
 */
public class TraceManager {

    private static final String PERFORMANCE_ADVISOR = "PERFORMANCE_ADVISOR";

    private static Logger LOGGER = org.slf4j.LoggerFactory.getLogger(TraceManager.class);

    public static Trace getTrace(Class clazz) {
        Logger logger = org.slf4j.LoggerFactory.getLogger(clazz);
        return new TraceImpl(logger);
    }

    public static Trace getTrace(String loggerName) {
        Logger logger = org.slf4j.LoggerFactory.getLogger(loggerName);
        return new TraceImpl(logger);
    }

    public static Trace getPerformanceAdvisorTrace() {
        Logger logger = org.slf4j.LoggerFactory.getLogger(PERFORMANCE_ADVISOR);
        return new TraceImpl(logger);
    }

    public static ILoggerFactory getILoggerFactory() {
    	return LoggerFactory.getILoggerFactory();
    }
}
