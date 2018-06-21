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

import com.evolveum.midpoint.web.component.util.Editable;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingComponentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingLevelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SubSystemLoggerConfigurationType;

import org.apache.commons.lang.Validate;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class FilterConfiguration extends Selectable implements Editable {

    private boolean editing;
    private LoggingLevelType level;
    private List<String> appenders = new ArrayList<>();
    private LoggingComponentType component;

    public FilterConfiguration(SubSystemLoggerConfigurationType config) {
        Validate.notNull(config, "Subsystem logger configuration must not be null.");

        component = config.getComponent();
        setLevel(config.getLevel());
        setAppenders(config.getAppender());
    }

    public List<String> getAppenders() {
        return appenders;
    }

    public void setAppenders(List<String> appenders) {
        this.appenders = appenders;
    }

    public LoggingLevelType getLevel() {
        return level;
    }

    public void setLevel(LoggingLevelType level) {
        this.level = level;
    }

    public boolean isEditing() {
        return editing;
    }

    public void setEditing(boolean editing) {
        this.editing = editing;
    }

    public String getName() {
        if (component == null) {
            return null;
        }
        return component.value();
    }

    public LoggingComponentType getComponent() {
        return component;
    }

    public void setName(String name) {
        this.component = LoggingComponentType.valueOf(name);
    }

    public SubSystemLoggerConfigurationType toXmlType() {
        SubSystemLoggerConfigurationType type = new SubSystemLoggerConfigurationType();
        type.setComponent(component);
        type.setLevel(getLevel());
        type.getAppender().addAll(getAppenders());
        return type;
    }
}
