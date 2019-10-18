/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.util.logging;

import com.evolveum.midpoint.util.statistics.OperationExecutionLogger;
import org.slf4j.MDC;
import org.slf4j.Marker;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * This class allow output for given MDC value and given level
 * implments logback turbofilter feature
 * <p>
 * If given value of MDC is found and also level of message reach given level
 * then onMatch action is done
 * else onMissmatch actionis done
 *
 * <p>
 * Action values:
 * ACCEPT - bypass basic selection rule and follow processing
 * NEUTRAL - follow processing
 * DENY - stop processing
 * <p>
 * Level values:OFF,ERROR,WARN,INFO,DEBUG,TRACE
 *
 * @author mamut
 *
 */
public class MDCLevelTurboFilter extends TurboFilter {

    private FilterReply onMatch = FilterReply.ACCEPT;
    private FilterReply onMismatch = FilterReply.NEUTRAL;
    private String mdcKey;
    private String mdcValue;
    private Level level = Level.OFF;

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {

        if (null == mdcKey || null == mdcValue) {
            return FilterReply.NEUTRAL;
        }
        //First compare levels
        if (level.isGreaterOrEqual(borderLevel())) {
            //Second test MDCvalue match current MDC->key => value
            if (mdcValue.equals(MDC.get(mdcKey))) {
                //if midpoint clas then process
                if (logger.getName().contains("com.evolveum.midpoint")) {
                    return onMatch;
                // if PROFILING then skip
                } else if (OperationExecutionLogger.PROFILING_LOGGER_NAME.equals(logger.getName())) {
                    return FilterReply.NEUTRAL;
                // if external class then move to TRACE
                } else {
                    if (level.isGreaterOrEqual(Level.DEBUG)) {
                        level = Level.TRACE;
                    }
                }

            } else {
                return onMismatch;
            }
        }
        return FilterReply.NEUTRAL;
    }

    /**
     * @param action the action to set on success
     */
    public void setOnMatch(String action) {
        if ("NEUTRAL".equals(action)) {
            this.onMatch = FilterReply.NEUTRAL;
        } else if ("ACCEPT".equals(action)) {
            this.onMatch = FilterReply.ACCEPT;
        } else {
            this.onMatch = FilterReply.DENY;
        }
    }

    /**
     * @param action the onMismatch to set on failure
     */
    public void setOnMismatch(String action) {
        if ("NEUTRAL".equals(action)) {
            this.onMismatch = FilterReply.NEUTRAL;
        } else if ("ACCEPT".equals(action)) {
            this.onMismatch = FilterReply.ACCEPT;
        } else {
            this.onMismatch = FilterReply.DENY;
        }
    }

    /**
     * @param mdcKey the mdcKey to watch
     */
    public void setMDCKey(String mdcKey) {
        System.out.println("MDCkey = " + mdcKey);
        this.mdcKey = mdcKey;
    }

    /**
     * @param mdcValue the mdcValue to match with MDCkey
     */
    public void setMDCValue(String mdcValue) {
        System.out.println("MDCvalue = " + mdcValue);
        this.mdcValue = mdcValue;
    }

    /**
     * @param loggingLevel the level to breach
     */
    public void setLevel(String loggingLevel) {
        String level = loggingLevel.toUpperCase();
        if ("OFF".equals(level)) {
            this.level = Level.OFF;
        } else if ("ERROR".equals(level)) {
            this.level = Level.ERROR;
        } else if ("WARN".equals(level)) {
            this.level = Level.WARN;
        } else if ("INFO".equals(level)) {
            this.level = Level.INFO;
        } else if ("DEBUG".equals(level)) {
            this.level = Level.DEBUG;
        } else if ("TRACE".equals(level)) {
            this.level = Level.TRACE;
        } else {
            this.level = Level.ALL;
        }
    }

    /**
     * @return the level
     */
    private Level borderLevel() {
        return level;
    }

    @Override
    public void start() {
    }

}
