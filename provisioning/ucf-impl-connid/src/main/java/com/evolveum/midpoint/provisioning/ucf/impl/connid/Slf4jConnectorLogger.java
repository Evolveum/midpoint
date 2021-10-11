/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import org.identityconnectors.common.logging.Log.Level;
import org.identityconnectors.common.logging.LogSpi;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Logger for ICF Connectors.
 *
 * The ICF connectors will call this class to log messages. It is configured in
 * META-INF/services/org.identityconnectors.common.logging
 *
 * @author Katka Valalikova
 *
 */
public class Slf4jConnectorLogger implements LogSpi {

    @Override
    public void log(Class<?> clazz, String method, Level level, String message, Throwable ex) {
        final Trace logger = TraceManager.getTrace(clazz);
        //Mark all messages from ICF as ICF
        Marker m = MarkerFactory.getMarker("ICF");

        //Translate ICF logging into slf4j
        // OK    -> trace
        // INFO  -> debug
        // WARN  -> warn
        // ERROR -> error
        if (Level.OK.equals(level)) {
            if (null == ex) {
                logger.trace(m, "method: {} msg:{}", method, message);
            } else {
                logger.trace(m, "method: {} msg:{}", new Object[] { method, message }, ex);
            }
        } else if (Level.INFO.equals(level)) {
            if (null == ex) {
                logger.debug(m, "method: {} msg:{}", method, message);
            } else {
                logger.debug(m, "method: {} msg:{}", new Object[] { method, message }, ex);
            }
        } else if (Level.WARN.equals(level)) {
            if (null == ex) {
                logger.warn(m, "method: {} msg:{}", method, message);
            } else {
                logger.warn(m, "method: {} msg:{}", new Object[] { method, message }, ex);
            }
        } else if (Level.ERROR.equals(level)) {
            if (null == ex) {
                logger.error(m, "method: {} msg:{}", method, message);
            } else {
                logger.error(m, "method: {} msg:{}", new Object[] { method, message }, ex);
            }
        }
    }

    //@Override
    // not using override to be able to work with both "old" and current version of connid
    public void log(Class<?> clazz, StackTraceElement caller, Level level, String message, Throwable ex) {
        log(clazz, caller.getMethodName(), level, message, ex);
    }

    @Override
    public boolean isLoggable(Class<?> clazz, Level level) {
        return true;
    }

    //@Override
    // not using override to be able to work with both "old" and current version of connid
    public boolean needToInferCaller(Class<?> clazz, Level level) {
        return false;
    }

}
