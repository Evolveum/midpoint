/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.functions;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author semancik
 *
 */
public class LogExpressionFunctions {

    public static final String EXPRESSION_LOGGER_NAME = "com.evolveum.midpoint.expression";

    public static final Trace LOGGER = TraceManager.getTrace(EXPRESSION_LOGGER_NAME);

    private PrismContext prismContext;

    public LogExpressionFunctions(PrismContext prismContext) {
        super();
        this.prismContext = prismContext;
    }

    public void error(String format, Object... args) {
        LOGGER.error(format, args);
    }

    public void warn(String format, Object... args) {
        LOGGER.warn(format, args);
    }

    public void info(String format, Object... args) {
        LOGGER.info(format, args);
    }

    public void debug(String format, Object... args) {
        LOGGER.debug(format, args);
    }

    public void trace(String format, Object... args) {
        LOGGER.trace(format, args);
    }

}
