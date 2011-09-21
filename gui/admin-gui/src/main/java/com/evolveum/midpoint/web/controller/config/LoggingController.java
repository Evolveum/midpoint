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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.bean.AppenderListItem;
import com.evolveum.midpoint.web.bean.BasicLoggerListItem;
import com.evolveum.midpoint.web.bean.LoggerListItem;
import com.evolveum.midpoint.web.bean.SubsystemLoggerListItem;
import com.evolveum.midpoint.web.component.LoggingManager;
import com.evolveum.midpoint.web.controller.util.ControllerUtil;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.web.util.SelectItemComparator;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AppenderConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ClassLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.FileAppenderConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggingLevelType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SubSystemLoggerConfigurationType;

/**
 * 
 * @author lazyman
 * 
 */
@Controller("logging")
@Scope("session")
public class LoggingController implements Serializable {

	public static final String PAGE_NAVIGATION = "/config/logging?faces-redirect=true";
	public static final String PARAM_APPENDER_ID = "appenderName";
	public static final String PARAM_LOGGER_ID = "loggerId";
	private static final Trace LOGGER = TraceManager.getTrace(LoggingController.class);
	private static final long serialVersionUID = -8739729766074013883L;
	@Autowired(required = true)
	private transient LoggingManager loggingManager;

	private LoggingLevelType rootLoggerLevel;
	private String rootAppender;

	private List<SubsystemLoggerListItem> subsystemLoggers;
	private List<LoggerListItem> loggers;
	private List<AppenderListItem> appenders;

	private boolean selectAllSubsystemLoggers = false;
	private boolean selectAllLoggers = false;
	private boolean selectAllAppenders = false;

	public List<SelectItem> getLevels() {
		List<SelectItem> levels = new ArrayList<SelectItem>();
		for (LoggingLevelType type : LoggingLevelType.values()) {
			levels.add(new SelectItem(type.value()));
		}

		Collections.sort(levels, new SelectItemComparator());

		return levels;
	}

	public void setRootLevelString(String rootLoggerLevel) {
		if (StringUtils.isEmpty(rootLoggerLevel)) {
			rootLoggerLevel = null;
		}
		for (LoggingLevelType level : LoggingLevelType.values()) {
			if (level.value().equals(rootLoggerLevel)) {
				this.rootLoggerLevel = level;
				break;
			}
		}
	}

	public String getRootLevelString() {
		if (rootLoggerLevel == null) {
			return null;
		}

		return rootLoggerLevel.value();
	}

	public String getRootAppender() {
		return rootAppender;
	}

	public void setRootAppender(String rootAppender) {
		this.rootAppender = rootAppender;
	}

	public List<SubsystemLoggerListItem> getSubsystemLoggers() {
		if (subsystemLoggers == null) {
			subsystemLoggers = new ArrayList<SubsystemLoggerListItem>();
		}
		return subsystemLoggers;
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

	public void setSelectAllAppenders(boolean selectAllAppenders) {
		this.selectAllAppenders = selectAllAppenders;
	}

	public void selectAppenderPerformed(ValueChangeEvent evt) {
		this.selectAllAppenders = ControllerUtil.selectPerformed(evt, getAppenders());
	}

	public void selectAllAppendersPerformed(ValueChangeEvent evt) {
		ControllerUtil.selectAllPerformed(evt, getAppenders());
	}

	public List<LoggerListItem> getLoggers() {
		if (loggers == null) {
			loggers = new ArrayList<LoggerListItem>();
		}
		return loggers;
	}

	public boolean isSelectAllSubsystemLoggers() {
		return selectAllSubsystemLoggers;
	}

	public void setSelectAllSubsystemLoggers(boolean selectAllSubsystemLoggers) {
		this.selectAllSubsystemLoggers = selectAllSubsystemLoggers;
	}

	public void selectAllSubsystemLoggersPerformed(ValueChangeEvent evt) {
		ControllerUtil.selectAllPerformed(evt, getSubsystemLoggers());
	}

	public boolean isSelectAllLoggers() {
		return selectAllLoggers;
	}

	public void setSelectAllLoggers(boolean selectAllLoggers) {
		this.selectAllLoggers = selectAllLoggers;
	}

	public void selectAllLoggersPerformed(ValueChangeEvent evt) {
		ControllerUtil.selectAllPerformed(evt, getLoggers());
	}

	public List<SelectItem> getAppenderNames() {
		List<SelectItem> appenders = new ArrayList<SelectItem>();
		for (AppenderListItem item : getAppenders()) {
			appenders.add(new SelectItem(item.getName()));
		}

		Collections.sort(appenders, new SelectItemComparator());

		return appenders;
	}

	public void addAppender() {
		AppenderListItem item = new AppenderListItem();
		item.setEditing(true);

		getAppenders().add(item);
	}

	public void editAppender() {
		String appenderName = FacesUtils.getRequestParameter(PARAM_APPENDER_ID);
		if (StringUtils.isEmpty(appenderName)) {
			FacesUtils.addErrorMessage("Appender id not defined.");
			return;
		}

		for (AppenderListItem item : getAppenders()) {
			if (item.getName().equals(appenderName)) {
				item.setEditing(true);
				break;
			}
		}
	}

	public void deleteAppenders() {
		List<AppenderListItem> items = new ArrayList<AppenderListItem>();
		for (AppenderListItem item : getAppenders()) {
			if (item.isSelected()) {
				items.add(item);
			}
		}
		getAppenders().removeAll(items);
	}

	public void addLogger() {
		int id = 0;
		for (LoggerListItem item : getLoggers()) {
			if (item.getId() >= id) {
				id = item.getId() + 1;
			}
		}
		LoggerListItem item = new LoggerListItem(id);
		item.setEditing(true);

		getLoggers().add(item);
	}

	public void editLogger() {
		String loggerId = FacesUtils.getRequestParameter(PARAM_LOGGER_ID);
		if (StringUtils.isEmpty(loggerId) || !loggerId.matches("[0-9]*")) {
			FacesUtils.addErrorMessage("Logger id not defined.");
			return;
		}

		int id = Integer.parseInt(loggerId);
		for (LoggerListItem item : getLoggers()) {
			if (item.getId() == id) {
				item.setEditing(true);
				break;
			}
		}
	}

	public void deleteLoggers() {
		List<LoggerListItem> items = new ArrayList<LoggerListItem>();
		for (LoggerListItem item : getLoggers()) {
			if (item.isSelected()) {
				items.add(item);
			}
		}
		getLoggers().removeAll(items);
	}

	public void savePerformed() {
		LoggingConfigurationType logging = createConfiguration(getSubsystemLoggers(), getLoggers(),
				getAppenders());
		OperationResult result = new OperationResult("Load Logging Configuration");
		try {
			loggingManager.updateConfiguration(logging, result);
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't update logging configuration", ex);
		} finally {
			result.computeStatus("Couldn't update logging configuration.");
			if (!result.isSuccess()) {
				FacesUtils.addMessage(result);
			}
		}

		initController();
	}

	public void cancelPerformed() {
		// TODO:
	}

	public String initController() {
		getAppenders().clear();
		getLoggers().clear();
		getSubsystemLoggers().clear();

		selectAllAppenders = false;
		selectAllLoggers = false;
		selectAllSubsystemLoggers = false;

		OperationResult result = new OperationResult("Load Logging Configuration");
		try {
			LoggingConfigurationType logging = loggingManager.getConfiguration(result);
			if (logging == null) {
				FacesUtils.addMessage(result);
				return PAGE_NAVIGATION;
			}

			rootLoggerLevel = logging.getRootLoggerLevel();
			rootAppender = logging.getRootLoggerAppender();

			for (AppenderConfigurationType appender : logging.getAppender()) {
				if (!(appender instanceof FileAppenderConfigurationType)) {
					FacesUtils.addWarnMessage("Unknown appender '" + appender.getName() + "'.");
					continue;
				}
				getAppenders().add(createAppenderListItem((FileAppenderConfigurationType) appender));
			}

			int id = 0;
			for (ClassLoggerConfigurationType logger : logging.getClassLogger()) {
				getLoggers().add(createLoggerListItem(id, logger));
				id++;
			}

			id = 0;
			for (SubSystemLoggerConfigurationType logger : logging.getSubSystemLogger()) {
				getSubsystemLoggers().add(createSubsystemLoggerListItem(id, logger));
			}
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't get logging configuration.", ex);
			FacesUtils.addErrorMessage("Couldn't get logging configuration.", ex);
		} finally {
			if (!result.isSuccess()) {
				FacesUtils.addMessage(result);
			}
		}

		return PAGE_NAVIGATION;
	}

	private AppenderListItem createAppenderListItem(FileAppenderConfigurationType appender) {
		AppenderListItem item = new AppenderListItem();
		item.setAppending(appender.isAppend());
		item.setFilePath(appender.getFileName());
		item.setFilePattern(appender.getFilePattern());
		item.setMaxFileSize(appender.getMaxFileSize());
		item.setMaxHistory(appender.getMaxHistory());
		item.setName(appender.getName());
		item.setPattern(appender.getPattern());

		return item;
	}

	private FileAppenderConfigurationType createAppenderType(AppenderListItem item) {
		FileAppenderConfigurationType appender = new FileAppenderConfigurationType();
		appender.setAppend(item.isAppending());
		appender.setFileName(item.getFilePath());
		appender.setFilePattern(item.getFilePattern());
		appender.setMaxFileSize(item.getMaxFileSize());
		appender.setMaxHistory(item.getMaxHistory());
		appender.setName(item.getName());
		appender.setPattern(item.getPattern());

		return appender;
	}

	private LoggerListItem createLoggerListItem(int id, ClassLoggerConfigurationType logger) {
		LoggerListItem item = new LoggerListItem(id);
		item.setAppenders(logger.getAppender());
		item.setLevel(logger.getLevel());
		item.setPackageName(logger.getPackage());

		return item;
	}

	private ClassLoggerConfigurationType createClassLogger(LoggerListItem item,
			List<AppenderListItem> appenders) {
		ClassLoggerConfigurationType logger = new ClassLoggerConfigurationType();
		logger.setLevel(item.getLevel());
		logger.setPackage(item.getPackageName());
		logger.getAppender().addAll(createAppendersForLogger(item, appenders));

		return logger;
	}

	private List<String> createAppendersForLogger(BasicLoggerListItem item, List<AppenderListItem> appenders) {
		Set<String> existingAppenders = new HashSet<String>();
		for (AppenderListItem appender : appenders) {
			existingAppenders.add(appender.getName());
		}

		List<String> appenderNames = new ArrayList<String>();
		for (String appender : item.getAppenders()) {
			if (!existingAppenders.contains(appender)) {
				String name = "unknown";
				if (item instanceof LoggerListItem) {
					name = ((LoggerListItem) item).getPackageName();
				} else if (item instanceof SubsystemLoggerListItem) {
					name = ((SubsystemLoggerListItem) item).getComponentString();
				}

				FacesUtils.addWarnMessage("Ignoring unknown appender '" + appender + "' for logger '" + name
						+ "'.");
				continue;
			}
			appenderNames.add(appender);
		}

		return appenderNames;
	}

	private SubsystemLoggerListItem createSubsystemLoggerListItem(int id,
			SubSystemLoggerConfigurationType logger) {
		SubsystemLoggerListItem item = new SubsystemLoggerListItem(id);
		item.setLevel(logger.getLevel());
		item.setComponent(logger.getComponent());
		item.setAppenders(logger.getAppender());

		return item;
	}

	private SubSystemLoggerConfigurationType createSubsystemLogger(SubsystemLoggerListItem item,
			List<AppenderListItem> appenders) {
		SubSystemLoggerConfigurationType logger = new SubSystemLoggerConfigurationType();
		logger.setLevel(item.getLevel());
		logger.setComponent(item.getComponent());
		item.getAppenders().addAll(createAppendersForLogger(item, appenders));

		return logger;
	}

	private LoggingConfigurationType createConfiguration(List<SubsystemLoggerListItem> subsystemLoggers,
			List<LoggerListItem> loggers, List<AppenderListItem> appenders) {
		LoggingConfigurationType configuration = new LoggingConfigurationType();
		for (AppenderListItem item : appenders) {
			AppenderConfigurationType appender = createAppenderType(item);
			configuration.getAppender().add(appender);
		}
		for (LoggerListItem item : loggers) {
			ClassLoggerConfigurationType logger = createClassLogger(item, appenders);
			configuration.getClassLogger().add(logger);
		}
		for (SubsystemLoggerListItem item : subsystemLoggers) {
			SubSystemLoggerConfigurationType logger = createSubsystemLogger(item, appenders);
			configuration.getSubSystemLogger().add(logger);
		}

		return configuration;
	}
}
