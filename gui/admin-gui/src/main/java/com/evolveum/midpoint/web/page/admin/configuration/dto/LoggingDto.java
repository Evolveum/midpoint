/*
 * Copyright (c) 2010-2015 Evolveum
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AuditingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingComponentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingLevelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SubSystemLoggerConfigurationType;

/**
 * @author lazyman
 * @author katkav
 */
public class LoggingDto implements Serializable {

	public static final String F_ROOT_LEVEL = "rootLevel";
	public static final String F_ROOT_APPENDER = "rootAppender";
	public static final String F_APPENDERS = "appenders";

	private List<AppenderConfiguration> appenders;

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

	private LoggingLevelType rootLevel;
	private String rootAppender;

	private List<LoggerConfiguration> loggers = new ArrayList<>();
	private List<FilterConfiguration> filters = new ArrayList<>();

	private boolean auditLog = false;
	private boolean auditDetails;
	private String auditAppender;

	private boolean advanced;

	private Boolean debug;
	

	public LoggingDto() {
		this(null);
	}

	public LoggingDto(LoggingConfigurationType config) {
		init(config);
	}

	private void init(LoggingConfigurationType config) {
		if (config == null) {
			return;
		}
		rootLevel = config.getRootLoggerLevel();
		rootAppender = config.getRootLoggerAppender();

		for (SubSystemLoggerConfigurationType logger : config.getSubSystemLogger()) {
			filters.add(new FilterConfiguration(logger));
		}

		AuditingConfigurationType auditing = config.getAuditing();
		if (auditing != null) {
			setAuditLog(auditing.isEnabled());
			setAuditDetails(auditing.isDetails());
			setAuditAppender(auditing.getAppender() != null && auditing.getAppender().size() > 0
					? auditing.getAppender().get(0) : null);
		}

		for (ClassLoggerConfigurationType logger : config.getClassLogger()) {

			if (ProfilingDto.LOGGER_PROFILING.equals(logger.getPackage())) {
				continue;
			}
			if (componentMap.containsKey(logger.getPackage())) {
				loggers.add(new ComponentLogger(logger));
			} else if (StandardLogger.isStandardLogger(logger.getPackage())) {
				loggers.add(new StandardLogger(logger));
			} else {
				loggers.add(new ClassLogger(logger));
			}
		}

		loggers.sort((l1, l2) -> String.CASE_INSENSITIVE_ORDER.compare(l1.getName(), l2.getName()));
		filters.sort((f1, f2) -> String.CASE_INSENSITIVE_ORDER.compare(f1.getName(), f2.getName()));
		debug = config.isDebug();
	}

	public LoggingConfigurationType getNewObject() {
		LoggingConfigurationType configuration = new LoggingConfigurationType();
		AuditingConfigurationType audit = new AuditingConfigurationType();
		audit.setEnabled(isAuditLog());
		audit.setDetails(isAuditDetails());
		if (StringUtils.isNotEmpty(getAuditAppender())) {
			audit.getAppender().add(getAuditAppender());
		}
		configuration.setAuditing(audit);
		configuration.setRootLoggerAppender(getRootAppender());
		configuration.setRootLoggerLevel(getRootLevel());

		for (AppenderConfiguration item : getAppenders()) {
			configuration.getAppender().add(item.getConfig());
		}

		for (LoggerConfiguration item : getLoggers()) {

			for (ClassLoggerConfigurationType logger : configuration.getClassLogger()) {
				if (logger.getPackage().equals(item.getName())) {
					throw new IllegalStateException(
							"Logger with name '" + item.getName() + "' is already defined.");
				}
			}

			//TODO : clean up toXmlType() method.. getAppenders() in LogginConfiguration is empty by default..shouldn't it be null?
			if (item.toXmlType() != null){
				configuration.getClassLogger().add(item.toXmlType());
			}

		}

		for (FilterConfiguration item : getFilters()) {

			for (SubSystemLoggerConfigurationType filter : configuration.getSubSystemLogger()) {
				if (filter.getComponent().name().equals(item.getName())) {
					throw new IllegalStateException(
							"Filter with name '" + item.getName() + "' is already defined.");
				}
			}

			configuration.getSubSystemLogger().add(item.toXmlType());
		}

		for (LoggerConfiguration logger : getLoggers()) {
			logger.setEditing(false);
		}
		for (FilterConfiguration filter : getFilters()) {
			filter.setEditing(false);
		}
		for (AppenderConfiguration appender : getAppenders()) {
			appender.setEditing(false);
		}
		configuration.setDebug(debug);
		return configuration;
	}

	public List<LoggerConfiguration> getLoggers() {
		return loggers;
	}

	public List<FilterConfiguration> getFilters() {
		return filters;
	}

	public String getRootAppender() {
		return rootAppender;
	}

	public void setRootAppender(String rootAppender) {
		this.rootAppender = rootAppender;
	}

	public LoggingLevelType getRootLevel() {
		return rootLevel;
	}

	public void setRootLevel(LoggingLevelType rootLevel) {
		this.rootLevel = rootLevel;
	}

	public String getAuditAppender() {
		return auditAppender;
	}

	public void setAuditAppender(String auditAppender) {
		this.auditAppender = auditAppender;
	}

	public boolean isAuditDetails() {
		return auditDetails;
	}

	public void setAuditDetails(boolean auditDetails) {
		this.auditDetails = auditDetails;
	}

	public boolean isAuditLog() {
		return auditLog;
	}

	public void setAuditLog(boolean auditLog) {
		this.auditLog = auditLog;
	}

	public boolean isAdvanced() {
		return advanced;
	}

	public void setAdvanced(boolean advanced) {
		this.advanced = advanced;
	}

	public void setAppenders(List<AppenderConfiguration> appenders) {
		this.appenders = appenders;
	}

	public List<AppenderConfiguration> getAppenders() {
		return appenders;
	}

}
