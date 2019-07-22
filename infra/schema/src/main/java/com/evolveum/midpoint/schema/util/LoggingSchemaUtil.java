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

package com.evolveum.midpoint.schema.util;

import ch.qos.logback.classic.Level;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingLevelType;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class LoggingSchemaUtil {

	public static Level toLevel(@NotNull LoggingLevelType level) {
		switch (level) {
			case ALL: return Level.ALL;
			case TRACE: return Level.TRACE;
			case DEBUG: return Level.DEBUG;
			case INFO: return Level.INFO;
			case WARN: return Level.WARN;
			case ERROR: return Level.ERROR;
			case OFF: return Level.OFF;
			default: throw new IllegalArgumentException("level: " + level);
		}
	}
}
