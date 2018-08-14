/*
 * Copyright (c) 2010-2018 Evolveum
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

package com.evolveum.midpoint.common;

import ch.qos.logback.core.PropertyDefinerBase;
import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;

import java.util.TimeZone;

/**
 * @see LogbackPropertyDefinerForConsolePrefix
 *
 * @author mederly
 */
public class LogbackPropertyDefinerForConsoleTimezone extends PropertyDefinerBase {
	@Override
	public String getPropertyValue() {
		String value = System.getProperty(MidpointConfiguration.MIDPOINT_LOGGING_CONSOLE_TIMEZONE_PROPERTY);
		return value != null ? value : TimeZone.getDefault().getID();
	}
}
