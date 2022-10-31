/*
 * Copyright (C) 2018-2022 Evolveum and contributors
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
 * @author skublik
 */
public final class ComponentLoggerType {

    public static final Map<String, LoggingComponentType> COMPONENT_MAP = new HashMap<>();

    static {
        COMPONENT_MAP.put("com.evolveum.midpoint", LoggingComponentType.ALL);
        COMPONENT_MAP.put("com.evolveum.midpoint.model", LoggingComponentType.MODEL);
        COMPONENT_MAP.put("com.evolveum.midpoint.provisioning", LoggingComponentType.PROVISIONING);
        COMPONENT_MAP.put("com.evolveum.midpoint.repo", LoggingComponentType.REPOSITORY);
        COMPONENT_MAP.put("com.evolveum.midpoint.web", LoggingComponentType.WEB);
        COMPONENT_MAP.put("com.evolveum.midpoint.gui", LoggingComponentType.GUI);
        COMPONENT_MAP.put("com.evolveum.midpoint.task", LoggingComponentType.TASKMANAGER);
        COMPONENT_MAP.put("com.evolveum.midpoint.model.sync",
                LoggingComponentType.RESOURCEOBJECTCHANGELISTENER);
        COMPONENT_MAP.put("com.evolveum.midpoint.wf", LoggingComponentType.WORKFLOWS);
        COMPONENT_MAP.put("com.evolveum.midpoint.notifications", LoggingComponentType.NOTIFICATIONS);
        COMPONENT_MAP.put("com.evolveum.midpoint.transport", LoggingComponentType.TRANSPORT);
        COMPONENT_MAP.put("com.evolveum.midpoint.certification", LoggingComponentType.ACCESS_CERTIFICATION);
        COMPONENT_MAP.put("com.evolveum.midpoint.security", LoggingComponentType.SECURITY);
    }

    public static String getPackageByValue(LoggingComponentType value) {
        if (value == null) {
            return null;
        }
        for (Entry<String, LoggingComponentType> entry : COMPONENT_MAP.entrySet()) {
            if (value.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }
}
