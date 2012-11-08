/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.configuration.dto;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.evolveum.midpoint.xml.ns._public.common.common_2a.ClassLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.LoggingComponentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SubSystemLoggerConfigurationType;

import org.apache.commons.lang.Validate;

/**
 * @author lazyman
 */
public class ComponentLogger extends LoggerConfiguration {

	private LoggingComponentType component;

	public ComponentLogger(ClassLoggerConfigurationType config) {
		Validate.notNull(config, "Component logger configuration must not be null.");
		// Validate.notNull(config.getComponent(),
		// "Subsystem component is not defined.");

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
		type.getAppender().addAll(getAppenders());
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
