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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.Validate;

/**
 * 
 * @author lazyman
 * 
 */
public class LoggingUtils {

	public static void logException(Trace LOGGER, String message, Throwable ex, Object... objects) {
		Validate.notNull(LOGGER, "Logger can't be null.");
		Validate.notNull(ex, "Exception can't be null.");
		
		List<Object> args = new ArrayList<Object>();
		args.addAll(Arrays.asList(objects));
		args.add(ex.getMessage() + " (" + ex.getClass() + ")");

		LOGGER.error(message + ", reason: {}", args.toArray());
		// Add exception to the list. It will be the last argument without {} in the message,
		// therefore the stack trace will get logged
		args.add(ex);
		LOGGER.debug(message + ".", args.toArray());
	}
	
	public static void logErrorOnDebugLevel(Trace LOGGER, String message, Throwable ex, Object... objects) {
		Validate.notNull(LOGGER, "Logger can't be null.");
		Validate.notNull(ex, "Exception can't be null.");
		
		List<Object> args = new ArrayList<Object>();
		args.addAll(Arrays.asList(objects));
		args.add(ex.getMessage());

		LOGGER.debug(message + ", reason: {}", args.toArray());
		// Add exception to the list. It will be the last argument without {} in the message,
		// therefore the stack trace will get logged
		args.add(ex);
		LOGGER.trace(message + ".", args.toArray());
	}

	public static void logStackTrace(final Trace LOGGER, String message) {
		if (LOGGER.isTraceEnabled()) {
			if (message != null) {
				LOGGER.trace(message+":\n{}", dumpStackTrace(LoggingUtils.class));
			} else {
				LOGGER.trace("{}", dumpStackTrace(LoggingUtils.class));
			}
		}
	}
		
	public static String dumpStackTrace(Class... classesToSkip) {
		StackTraceElement[] fullStack = Thread.currentThread().getStackTrace();
		String immediateClass = null;
		String immediateMethod = null;
		boolean firstFrameLogged = false;
		StringBuilder sb = new StringBuilder();
		OUTER: for (StackTraceElement stackElement: fullStack) {
			if (!firstFrameLogged) {
				if (stackElement.getClassName().equals(Thread.class.getName())) {
					// skip call to thread.getStackTrace();
					continue;
				}
				if (classesToSkip != null) {
					for (Class classToSkip: classesToSkip) {
						if (stackElement.getClassName().equals(classToSkip.getName())) {
							continue OUTER;
						}
					}
				}
			}
			firstFrameLogged = true;

			sb.append(stackElement.toString());
			sb.append("\n");
		}
		return sb.toString();
	}
}
