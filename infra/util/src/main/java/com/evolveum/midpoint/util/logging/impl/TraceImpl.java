/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.util.logging.impl;

import org.slf4j.Logger;
import org.slf4j.Marker;

import com.evolveum.midpoint.util.logging.Trace;

/**
 * Sample Class Doc
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
public class TraceImpl implements Trace {

    public static final String code_id = "$Id$";
    private Logger LOGGER;

    public TraceImpl(Logger LOGGER) {
        this.LOGGER = LOGGER;
    }

    @Override
    public String getName() {
        return LOGGER.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return LOGGER.isTraceEnabled();
    }

    @Override
    public void trace(String msg) {
        LOGGER.trace(msg);
    }

    @Override
    public void trace(String format, Object arg) {
        LOGGER.trace(format, arg);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        LOGGER.trace(format, arg1, arg2);
    }

    @Override
    public void trace(String format, Object[] argArray) {
        LOGGER.trace(format, argArray);
    }

    @Override
    public void trace(String msg, Throwable t) {
        LOGGER.trace(msg, t);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return LOGGER.isTraceEnabled(marker);
    }

    @Override
    public void trace(Marker marker, String msg) {
        LOGGER.trace(marker, msg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        LOGGER.trace(marker, format, arg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        LOGGER.trace(format, arg1, arg2);
    }

    @Override
    public void trace(Marker marker, String format, Object[] argArray) {
        LOGGER.trace(marker, format, argArray);
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        LOGGER.trace(marker, msg, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return LOGGER.isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        LOGGER.debug(msg);
    }

    @Override
    public void debug(String format, Object arg) {
        LOGGER.debug(format, arg);
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        LOGGER.debug(format, arg1, arg2);
    }

    @Override
    public void debug(String format, Object[] argArray) {
        LOGGER.debug(format, argArray);
    }

    @Override
    public void debug(String msg, Throwable t) {
        LOGGER.debug(msg, t);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return LOGGER.isDebugEnabled(marker);
    }

    @Override
    public void debug(Marker marker, String msg) {
        LOGGER.debug(marker, msg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        LOGGER.debug(marker, format, arg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        LOGGER.debug(marker, format, arg1, arg2);
    }

    @Override
    public void debug(Marker marker, String format, Object[] argArray) {
        LOGGER.debug(marker, format, argArray);
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        LOGGER.debug(msg, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return LOGGER.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        LOGGER.info(msg);
    }

    @Override
    public void info(String format, Object arg) {
        LOGGER.info(format, arg);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        LOGGER.info(format, arg1, arg2);
    }

    @Override
    public void info(String format, Object[] argArray) {
        LOGGER.info(format, argArray);
    }

    @Override
    public void info(String msg, Throwable t) {
        LOGGER.info(msg, t);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return LOGGER.isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, String msg) {
        LOGGER.info(marker, msg);
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        LOGGER.info(marker, format, arg);
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        LOGGER.info(marker, format, arg1, arg2);
    }

    @Override
    public void info(Marker marker, String format, Object[] argArray) {
        LOGGER.info(marker, format, argArray);
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        LOGGER.info(marker, msg, t);
    }

    @Override
    public boolean isWarnEnabled() {
        return LOGGER.isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        LOGGER.warn(msg);
    }

    @Override
    public void warn(String format, Object arg) {
        LOGGER.warn(format, arg);
    }

    @Override
    public void warn(String format, Object[] argArray) {
        LOGGER.warn(format, argArray);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
         LOGGER.warn(format, arg1, arg2);
    }

    @Override
    public void warn(String msg, Throwable t) {
        LOGGER.warn(msg, t);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return LOGGER.isWarnEnabled(marker);
    }

    @Override
    public void warn(Marker marker, String msg) {
        LOGGER.warn(marker, msg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        LOGGER.warn(marker, format, arg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        LOGGER.warn(marker, format, arg1, arg2);
    }

    @Override
    public void warn(Marker marker, String format, Object[] argArray) {
        LOGGER.warn(marker, format, argArray);
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        LOGGER.warn(marker, msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return LOGGER.isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        LOGGER.error(msg);
    }

    @Override
    public void error(String format, Object arg) {
        LOGGER.error(format, arg);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        LOGGER.error(format, arg1, arg2);
    }

    @Override
    public void error(String format, Object[] argArray) {
        LOGGER.error(format, argArray);
    }

    @Override
    public void error(String msg, Throwable t) {
        LOGGER.error(msg, t);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
       return LOGGER.isErrorEnabled(marker);
    }

    @Override
    public void error(Marker marker, String msg) {
        LOGGER.error(marker, msg);
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        LOGGER.error(marker, format, arg);
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        LOGGER.error(marker, format, arg1, arg2);
    }

    @Override
    public void error(Marker marker, String format, Object[] argArray) {
        LOGGER.error(marker, format, argArray);
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        LOGGER.error(marker, msg, t);
    }
}
