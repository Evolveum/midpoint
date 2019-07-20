/*
 * Copyright (c) 2010-2019 Evolveum
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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.Marker;

/**
 * Overrides logging level for given loggers, based on thread-specific configuration.
 *
 * It is used to temporarily elevate logging levels when gathering logs for tracing purposes.
 * Other expected use is to allow task-specific logging.
 */
public class LevelOverrideTurboFilter extends TurboFilter {

	private static final ThreadLocal<LoggingLevelOverrideConfiguration> configurationThreadLocal = new ThreadLocal<>();

	@Override
	public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
		LoggingLevelOverrideConfiguration configuration = configurationThreadLocal.get();
		if (configuration != null) {
			for (LoggingLevelOverrideConfiguration.Entry entry : configuration.getEntries()) {
				if (entry.getLevel() == null) {
					continue;   // suspicious
				}
				for (String nameToOverride : entry.getLoggers()) {
					if (loggerMatches(logger, nameToOverride)) {
						// "greater" means more coarse; e.g. WARN is greater than DEBUG
						return level != null && level.isGreaterOrEqual(entry.getLevel()) ? FilterReply.ACCEPT : FilterReply.DENY;
					}
				}
			}
		}
		return FilterReply.NEUTRAL;
	}

	private boolean loggerMatches(Logger logger, String prefix) {
		String loggerName = logger.getName();
		return prefix != null && loggerName != null &&
				loggerName.startsWith(prefix) &&
				(loggerName.length() == prefix.length() || loggerName.charAt(prefix.length()) == '.');
	}

	// In the future we might have active multiple overriding sources (e.g. tracing + per-task-logging)
	// But for now the source is only one.
	public static boolean isActive() {
		return configurationThreadLocal.get() != null;
	}

	public static void overrideLogging(LoggingLevelOverrideConfiguration configuration) {
		configurationThreadLocal.set(configuration);
	}

	public static void cancelLoggingOverride() {
		configurationThreadLocal.remove();
	}
}
