/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.configuration.component;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingComponentType;

/**
 *  @author skublik
 * */
public final class ComponentLoggerType {

	public static final Map<String, LoggingComponentType> componentMap = new HashMap<>();

	static {
		componentMap.put("com.evolveum.midpoint", LoggingComponentType.ALL);
		componentMap.put("com.evolveum.midpoint.model", LoggingComponentType.MODEL);
		componentMap.put("com.evolveum.midpoint.provisioning", LoggingComponentType.PROVISIONING);
		componentMap.put("com.evolveum.midpoint.repo", LoggingComponentType.REPOSITORY);
		componentMap.put("com.evolveum.midpoint.web", LoggingComponentType.WEB);
		componentMap.put("com.evolveum.midpoint.gui", LoggingComponentType.GUI);
		componentMap.put("com.evolveum.midpoint.task", LoggingComponentType.TASKMANAGER);
		componentMap.put("com.evolveum.midpoint.model.sync",
				LoggingComponentType.RESOURCEOBJECTCHANGELISTENER);
		componentMap.put("com.evolveum.midpoint.wf", LoggingComponentType.WORKFLOWS);
		componentMap.put("com.evolveum.midpoint.notifications", LoggingComponentType.NOTIFICATIONS);
		componentMap.put("com.evolveum.midpoint.certification", LoggingComponentType.ACCESS_CERTIFICATION);
		componentMap.put("com.evolveum.midpoint.security", LoggingComponentType.SECURITY);
	}

	public static String getPackageByValue(LoggingComponentType value) {
		if (value == null) {
			return null;
		}
		for (Entry<String, LoggingComponentType> entry : componentMap.entrySet()) {
			if (value.equals(entry.getValue())) {
				return entry.getKey();
			}
		}
		return null;
	}
}
