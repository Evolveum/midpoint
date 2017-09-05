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

package com.evolveum.midpoint.web.page.admin.configuration.dto;

import java.util.Map.Entry;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingComponentType;

import org.apache.commons.lang.Validate;

/**
 * @author lazyman
 */
public class ComponentLogger extends LoggerConfiguration {

	private LoggingComponentType component;

	public ComponentLogger(ClassLoggerConfigurationType config) {
		Validate.notNull(config, "Component logger configuration must not be null.");

		component = LoggingDto.componentMap.get(config.getPackage());

		setLevel(config.getLevel());
		setAppenders(config.getAppender());
	}

	@Override
	public String getName() {
		if (component == null) {
			return null;
		}
		return getPackageByValue(component);
	}

	public LoggingComponentType getComponent() {
		return component;
	}

	public void setComponent(LoggingComponentType component) {
		this.component = component;
	}

	@Override
	public void setName(String name) {

	}

	public ClassLoggerConfigurationType toXmlType() {
		ClassLoggerConfigurationType type = new ClassLoggerConfigurationType();
		type.setPackage(getPackageByValue(component));
		type.setLevel(getLevel());
		if (!(getAppenders().isEmpty())){
        	type.getAppender().addAll(getAppenders());
        }
		return type;
	}

	private static String getPackageByValue(LoggingComponentType value) {
		if (value == null) {
			return null;
		}
		for (Entry<String, LoggingComponentType> entry : LoggingDto.componentMap.entrySet()) {
			if (value.equals(entry.getValue())) {
				return entry.getKey();
			}
		}
		return null;
	}
}
