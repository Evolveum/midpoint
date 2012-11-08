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

import com.evolveum.midpoint.web.component.util.Editable;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.LoggingComponentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.LoggingLevelType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SubSystemLoggerConfigurationType;

import org.apache.commons.lang.Validate;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class FilterConfiguration extends Selectable implements Editable {

    private boolean editing;
    private LoggingLevelType level;
    private List<String> appenders = new ArrayList<String>();
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
