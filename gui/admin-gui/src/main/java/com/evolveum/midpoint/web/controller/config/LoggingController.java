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
package com.evolveum.midpoint.web.controller.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.web.bean.AppenderListItem;
import com.evolveum.midpoint.web.bean.AppenderType;
import com.evolveum.midpoint.web.bean.LoggerListItem;
import com.evolveum.midpoint.web.controller.util.ControllerUtil;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.web.util.SelectItemComparator;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggingCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggingComponentType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggingLevelType;

/**
 * 
 * @author lazyman
 * 
 */
@Controller("logging")
@Scope("session")
public class LoggingController implements Serializable {

	private static final long serialVersionUID = -8739729766074013883L;
	private List<LoggerListItem> loggers;
	private List<AppenderListItem> appenders;
	private boolean selectAllLoggers = false;
	private boolean selectAllAppenders = false;

	public List<SelectItem> getAppenderNames() {
		List<SelectItem> appenders = new ArrayList<SelectItem>();
		for (AppenderListItem item : getAppenders()) {
			appenders.add(new SelectItem(item.getName()));
		}

		Collections.sort(appenders, new SelectItemComparator());

		return appenders;
	}

	public List<SelectItem> getCategories() {
		List<SelectItem> categories = new ArrayList<SelectItem>();
		for (LoggingCategoryType type : LoggingCategoryType.values()) {
			categories.add(new SelectItem(type.value()));
		}

		Collections.sort(categories, new SelectItemComparator());

		return categories;
	}

	public List<SelectItem> getComponents() {
		List<SelectItem> components = new ArrayList<SelectItem>();
		for (LoggingComponentType type : LoggingComponentType.values()) {
			components.add(new SelectItem(type.value()));
		}

		Collections.sort(components, new SelectItemComparator());

		return components;
	}

	public List<SelectItem> getLevels() {
		List<SelectItem> levels = new ArrayList<SelectItem>();
		for (LoggingLevelType type : LoggingLevelType.values()) {
			levels.add(new SelectItem(type.value()));
		}

		Collections.sort(levels, new SelectItemComparator());

		return levels;
	}

	public List<SelectItem> getTypes() {
		List<SelectItem> types = new ArrayList<SelectItem>();
		for (AppenderType type : AppenderType.values()) {
			types.add(new SelectItem(type.getTitle(), FacesUtils.translateKey(type.getTitle())));
		}

		Collections.sort(types, new SelectItemComparator());

		return types;
	}

	public List<LoggerListItem> getLoggers() {
		if (loggers == null) {
			loggers = new ArrayList<LoggerListItem>();
		}
		return loggers;
	}

	public List<AppenderListItem> getAppenders() {
		if (appenders == null) {
			appenders = new ArrayList<AppenderListItem>();
		}
		return appenders;
	}

	public boolean isSelectAllAppenders() {
		return selectAllAppenders;
	}

	public boolean isSelectAllLoggers() {
		return selectAllLoggers;
	}

	public void setSelectAllAppenders(boolean selectAllAppenders) {
		this.selectAllAppenders = selectAllAppenders;
	}

	public void setSelectAllLoggers(boolean selectAllLoggers) {
		this.selectAllLoggers = selectAllLoggers;
	}

	public void selectLoggerPerformed(ValueChangeEvent evt) {
		this.selectAllLoggers = ControllerUtil.selectPerformed(evt, getLoggers());
	}

	public void selectAllLoggersPerformed(ValueChangeEvent evt) {
		ControllerUtil.selectAllPerformed(evt, getLoggers());
	}

	public void selectAppenderPerformed(ValueChangeEvent evt) {
		this.selectAllAppenders = ControllerUtil.selectPerformed(evt, getAppenders());
	}

	public void selectAllAppendersPerformed(ValueChangeEvent evt) {
		ControllerUtil.selectAllPerformed(evt, getAppenders());
	}

	public void deleteLoggers() {

	}

	public void deleteAppenders() {

	}
}
