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

import com.evolveum.midpoint.xml.ns._public.common.common_1.ClassLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggingComponentType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SubSystemLoggerConfigurationType;
import org.apache.commons.lang.Validate;

import java.util.List;

import javax.swing.LayoutStyle.ComponentPlacement;

/**
 * @author lazyman
 */
public class ComponentLogger extends LoggerConfiguration {

    private LoggingComponentType component;

    public ComponentLogger(SubSystemLoggerConfigurationType config) {
        Validate.notNull(config, "Subsystem logger configuration must not be null.");
//        Validate.notNull(config.getComponent(), "Subsystem component is not defined.");

        component = config.getComponent();
        setLevel(config.getLevel());
        setAppenders(config.getAppender());
    }

    @Override
    public String getName() {
        if (component == null) {
            return null;
        }
        return component.value();
    }
    
    public LoggingComponentType getComponent(){
    	return component;
    }

	@Override
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
