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

/**
 * It was simply not possible to provide an empty default value for logback property. So this is the workaround.
 * See https://stackoverflow.com/questions/44671972/empty-default-string-for-property-in-logback-xml.
 *
 * Even
 *   <if condition='isDefined("midpoint.logging.console.prefix")'>
 *       <then> ... </then>
 *       <else>
 *           <property name="prefix" value=""/>
 *       </else>
 *   </if>
 * does not work, because the "" cannot be used as a property value.
 *
 * So, the property definer is a workaround.
 *
 * @author mederly
 */
public class LogbackPropertyDefinerForConsolePrefix extends PropertyDefinerBase {

	@Override
	public String getPropertyValue() {
		String value = System.getProperty(MidpointConfiguration.MIDPOINT_LOGGING_CONSOLE_PREFIX_PROPERTY);
		return value != null ? value : "";
	}
}
