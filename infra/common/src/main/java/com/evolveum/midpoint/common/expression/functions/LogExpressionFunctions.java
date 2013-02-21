/**
 * Copyright (c) 2013 Evolveum
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
 * Portions Copyrighted 2013 [name of copyright owner]
 */
package com.evolveum.midpoint.common.expression.functions;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author semancik
 *
 */
public class LogExpressionFunctions {
	
	public static String EXPRESSION_LOGGER_NAME = "com.evolveum.midpoint.expression";
	
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
