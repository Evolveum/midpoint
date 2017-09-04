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
package com.evolveum.midpoint.model.common.expression.functions;

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
