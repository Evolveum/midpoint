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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggingLevelType;

/**
 * 
 * @author lazyman
 * 
 */
public class LoggerListItem implements Serializable {

	private static final long serialVersionUID = -2575577614131038008L;
	private boolean selected;
	private int id;
	private LoggingLevelType level;
	private List<String> categories;
	private List<String> components;
	private List<String> packages;
	private List<String> appenders;

	public LoggerListItem(int id) {
		this.id = id;
	}
	
	public LoggerListItem cloneItem() {
		LoggerListItem item = new LoggerListItem(getId());
		item.setLevel(getLevel());
		item.getCategories().addAll(getCategories());
		item.getComponents().addAll(getComponents());
		item.getPackages().addAll(getPackages());
		item.getAppenders().addAll(getAppenders());
		
		return item;
	}
	
	public int getId() {
		return id;
	}
	
	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
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
		if (level == null) {
			this.level = null;
			return;
		}
		this.level = LoggingLevelType.fromValue(level);
	}

	public List<String> getCategories() {
		if (categories == null) {
			categories = new ArrayList<String>();
		}
		return categories;
	}

	public void setCategories(List<String> categories) {
		this.categories = categories;
	}

	public List<String> getComponents() {
		if (components == null) {
			components = new ArrayList<String>();
		}
		return components;
	}

	public void setComponents(List<String> components) {
		this.components = components;
	}

	public List<String> getPackages() {
		if (packages == null) {
			packages = new ArrayList<String>();
		}
		return packages;
	}

	public List<String> getAppenders() {
		return appenders;
	}
	
	public void setAppenders(List<String> appenders) {
		this.appenders = appenders;
	}
}
