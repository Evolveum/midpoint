/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.model.impl.util;

import org.apache.cxf.Bus;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.interceptor.AbstractLoggingInterceptor;
import org.apache.cxf.interceptor.InterceptorProvider;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Created by Viliam Repan (lazyman).
 */
public class LoggingFeature extends AbstractFeature {

    public static final String MESSAGE_LOGGER = "org.apache.cxf.services.midpoint";

    private static final Logger LOG = Logger.getLogger(MESSAGE_LOGGER);

    private static final LoggingInInterceptor IN = new LoggingInInterceptor(AbstractLoggingInterceptor.DEFAULT_LIMIT);

    private static final LoggingOutInterceptor OUT = new LoggingOutInterceptor(AbstractLoggingInterceptor.DEFAULT_LIMIT);

    @Override
    protected void initializeProvider(InterceptorProvider provider, Bus bus) {
        provider.getInInterceptors().add(IN);
        provider.getInFaultInterceptors().add(IN);
        provider.getOutInterceptors().add(OUT);
        provider.getOutFaultInterceptors().add(OUT);
    }

    private static void logMessage(Logger logger, String message) {
        if (!logger.isLoggable(Level.FINE)) {
            return;
        }

        LogRecord lr = new LogRecord(Level.FINE, message);
        lr.setSourceClassName(logger.getName());
        lr.setSourceMethodName(null);
        lr.setLoggerName(logger.getName());
        logger.log(lr);
    }

    public static class LoggingInInterceptor extends org.apache.cxf.interceptor.LoggingInInterceptor {

        public LoggingInInterceptor(int lim) {
            super(lim);
        }

        @Override
        protected void log(Logger logger, String message) {
            logMessage(logger, message);
        }

        @Override
        protected Logger getLogger() {
            return LOG;
        }
    }

    public static class LoggingOutInterceptor extends org.apache.cxf.interceptor.LoggingOutInterceptor {

        public LoggingOutInterceptor(int lim) {
            super(lim);
        }

        @Override
        protected void log(Logger logger, String message) {
            logMessage(logger, message);
        }

        @Override
        protected Logger getLogger() {
            return LOG;
        }
    }
}
