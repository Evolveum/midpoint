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

import ch.qos.logback.classic.Level;
import org.apache.commons.lang.Validate;

/**
 *
 * @author lazyman
 *
 */
public class LoggingUtils {

    /**
     * Standard way of logging exception: message is presented at ERROR level, stack trace on DEBUG.
     */
	public static void logException(Trace LOGGER, String message, Throwable ex, Object... objects) {
        logExceptionInternal(Level.ERROR, Level.DEBUG, LOGGER, message, ex, objects);
	}

    /**
     * When logging unexpected exception, we always want to see the stack trace (so everything is logged on ERROR level)
     */
    public static void logUnexpectedException(Trace LOGGER, String message, Throwable ex, Object... objects) {
        logExceptionInternal(Level.ERROR, Level.ERROR, LOGGER, message, ex, objects);
    }

    /**
     * Non-critical exceptions (warnings, with details as debug)
     */
    public static void logExceptionAsWarning(Trace LOGGER, String message, Throwable ex, Object... objects) {
        logExceptionInternal(Level.WARN, Level.DEBUG, LOGGER, message, ex, objects);
    }

    /**
     * Exceptions that shouldn't be even visible on INFO level.
     */
	public static void logExceptionOnDebugLevel(Trace LOGGER, String message, Throwable ex, Object... objects) {
        logExceptionInternal(Level.DEBUG, Level.TRACE, LOGGER, message, ex, objects);
	}

    private static void logExceptionInternal(Level first, Level second, Trace LOGGER, String message, Throwable ex, Object... objects) {
        Validate.notNull(LOGGER, "Logger can't be null.");
        Validate.notNull(ex, "Exception can't be null.");

        List<Object> args = new ArrayList<>();
        args.addAll(Arrays.asList(objects));
        args.add(ex.getMessage() + " (" + ex.getClass() + ")");

        if (!first.equals(second)) {
            log(LOGGER, first, message + ", reason: {}", args.toArray());
        }
        // Add exception to the list. It will be the last argument without {} in the message,
        // therefore the stack trace will get logged
        args.add(ex);
        log(LOGGER, second, message + ".", args.toArray());
    }

    private static void log(Trace logger, Level level, String message, Object[] arguments) {
        if (level == Level.ERROR) {
            logger.error(message, arguments);
        } else if (level == Level.WARN) {
            logger.warn(message, arguments);
        } else if (level == Level.INFO) {
            logger.info(message, arguments);
        } else if (level == Level.DEBUG) {
            logger.debug(message, arguments);
        } else if (level == Level.TRACE) {
            logger.trace(message, arguments);
        } else {
            throw new IllegalArgumentException("Unknown level: " + level);
        }
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
