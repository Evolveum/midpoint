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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggingComponentType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggingLevelType;

/**
 * 
 * @author lazyman
 * 
 */
public class SubsystemLoggerListItem extends SelectableBean {

	private static final long serialVersionUID = -2575577614131038008L;
	private int id;
	private LoggingLevelType level;
	private LoggingComponentType component;
	private List<String> appenders;

	public SubsystemLoggerListItem(int id) {
		this.id = id;
	}

	public SubsystemLoggerListItem cloneItem() {
		SubsystemLoggerListItem item = new SubsystemLoggerListItem(getId());
		item.setLevel(getLevel());
		item.setComponent(getComponent());
		item.getAppenders().addAll(getAppenders());

		return item;
	}

	public int getId() {
		return id;
	}

	public LoggingLevelType getLevel() {
		return level;
	}

	public void setLevel(LoggingLevelType level) {
		this.level = level;
	}

	public String getLevelString() {
		if (level == null) {
			return null;
		}
		return level.value();
	}

	public void setLevelString(String level) {
		if (StringUtils.isEmpty(level)) {
			this.level = null;
			return;
		}
		this.level = LoggingLevelType.fromValue(level);
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

	public List<String> getAppenders() {
		if (appenders == null) {
			appenders = new ArrayList<String>();
		}
		return appenders;
	}

	public void setAppenders(List<String> appenders) {
		this.appenders = appenders;
	}
}
