/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.web.bean;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggingComponentType;

/**
 * 
 * @author lazyman
 * 
 */
public class SubsystemLoggerListItem extends BasicLoggerListItem {

	private static final long serialVersionUID = -2575577614131038008L;
	private LoggingComponentType component;

	public SubsystemLoggerListItem(int id) {
		super(id);
	}

	public LoggingComponentType getComponent() {
		return component;
	}

	public void setComponent(LoggingComponentType component) {
		this.component = component;
	}

	public String getComponentString() {
		if (component == null) {
			return null;
		}

		return component.value();
	}

	public void setComponentString(String component) {
		if (StringUtils.isEmpty(component)) {
			this.component = null;
			return;
		}

		this.component = LoggingComponentType.fromValue(component);
	}
}
