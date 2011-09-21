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

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.bean.AppenderListItem;
import com.evolveum.midpoint.web.bean.LoggerListItem;
import com.evolveum.midpoint.web.bean.SubsystemLoggerListItem;
import com.evolveum.midpoint.web.component.LoggingManager;
import com.evolveum.midpoint.web.controller.util.ControllerUtil;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.web.util.SelectItemComparator;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggingLevelType;

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

	private List<SubsystemLoggerListItem> subsystemLoggers;
	private List<LoggerListItem> loggers;
	private List<AppenderListItem> appenders;

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

	public boolean isSelectAllLoggers() {
		return selectAllLoggers;
	}

	public void setSelectAllLoggers(boolean selectAllLoggers) {
		this.selectAllLoggers = selectAllLoggers;
	}

	public void selectClassLoggerPerformed(ValueChangeEvent evt) {
		this.selectAllLoggers = ControllerUtil.selectPerformed(evt, getLoggers());
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

	// private List<SubSystemLoggerConfigurationType> subSystemLoggers;
	//

	//
	// public List<SelectItem> getComponents() {
	// List<SelectItem> components = new ArrayList<SelectItem>();
	// // for (LoggingComponentType type : LoggingComponentType.values()) {
	// // components.add(new SelectItem(type.value()));
	// // }
	// //
	// // Collections.sort(components, new SelectItemComparator());
	//
	// return components;
	// }

	// public List<SelectItem> getTypes() {
	// List<SelectItem> types = new ArrayList<SelectItem>();
	// for (AppenderType type : AppenderType.values()) {
	// types.add(new SelectItem(type.getTitle(),
	// FacesUtils.translateKey(type.getTitle())));
	// }
	//
	// Collections.sort(types, new SelectItemComparator());
	//
	// return types;
	// }
	//
	// public List<ClassLoggerConfigurationType> getClassLoggers() {
	// // if (classLoggers == null) {
	// // classLoggers = new ArrayList<ClassLoggerConfigurationType>();
	// // }
	// // return classLoggers;
	// return null;
	// }
	//
	// public List<SubSystemLoggerConfigurationType> getSubSystemLoggers() {
	// // if (subSystemLoggers == null) {
	// // subSystemLoggers = new ArrayList<SubSystemLoggerConfigurationType>();
	// // }
	// // return classLoggers;
	// return null;
	// }
	//

	//
	// public void selectAppenderPerformed(ValueChangeEvent evt) {
	// // this.selectAllAppenders = ControllerUtil.selectPerformed(evt,
	// // getAppenders());
	// }
	//
	// public void selectAllAppendersPerformed(ValueChangeEvent evt) {
	// // ControllerUtil.selectAllPerformed(evt, getAppenders());
	// }
	//
	//
	// public void deleteAppenders() {
	// List<AppenderListItem> items = new ArrayList<AppenderListItem>();
	// for (AppenderListItem item : getAppenders()) {
	// if (item.isSelected()) {
	// items.add(item);
	// }
	// }
	// getAppenders().removeAll(items);
	//
	// saveConfiguration();
	// }

	public String initController() {
		getAppenders().clear();
		getLoggers().clear();

		OperationResult result = new OperationResult("Load Logging Configuration");
		LoggingConfigurationType logging = loggingManager.getConfiguration(result);
		if (logging == null) {
			FacesUtils.addMessage(result);
			return PAGE_NAVIGATION;
		}

		// for (AppenderConfigurationType appender : logging.getAppender()) {
		// getAppenders().add(createAppenderListItem(appender));
		// }
		//
		// int id = 0;
		// for (LoggerConfigurationType logger : logging.getLogger()) {
		// getLoggers().add(createLoggerListItem(id, logger));
		// id++;
		// }

		return PAGE_NAVIGATION;
	}

	void saveConfiguration() {
		// LoggingConfigurationType logging = createConfiguration(getLoggers(),
		// getAppenders());
		// OperationResult result = new
		// OperationResult("Load Logging Configuration");
		// try {
		// loggingManager.updateConfiguration(logging, result);
		// } catch (Exception ex) {
		// LoggingUtils.logException(LOGGER,
		// "Couldn't update logging configuration", ex);
		// }
		// result.computeStatus("Couldn't update logging configuration.");
		// FacesUtils.addMessage(result);
		//
		// initController();
	}

	/*
	 * private LoggerListItem createLoggerListItem(int id,
	 * LoggerConfigurationType logger) { LoggerListItem item = new
	 * LoggerListItem(id); item.setAppenders(logger.getAppender());
	 * item.setLevel(logger.getLevel()); // for (LoggingCategoryType category :
	 * logger.getCategory()) { // item.getCategories().add(category.value()); //
	 * } for (LoggingComponentType component : logger.getComponent()) {
	 * item.getComponents().add(component.value()); }
	 * item.getPackages().addAll(logger.getPackage());
	 * 
	 * return item; }
	 * 
	 * private AppenderListItem createAppenderListItem(AppenderConfigurationType
	 * appender) { AppenderListItem item = new AppenderListItem();
	 * item.setName(appender.getName()); item.setPattern(appender.getPattern());
	 * item.setType(AppenderType.CONSOLE);
	 * 
	 * if (appender instanceof RollingFileAppenderConfigurationType) {
	 * RollingFileAppenderConfigurationType file =
	 * (RollingFileAppenderConfigurationType) appender;
	 * item.setFilePath(file.getFilePath());
	 * item.setMaxFileSize(file.getMaxFileSize());
	 * item.setAppending(file.isAppend());
	 * item.setType(AppenderType.ROLLING_FILE);
	 * 
	 * if (appender instanceof NdcRollingFileAppenderConfigurationType) {
	 * item.setType(AppenderType.NDC_ROLLING_FILE); } } else if (appender
	 * instanceof DailyRollingFileAppenderConfigurationType) {
	 * DailyRollingFileAppenderConfigurationType daily =
	 * (DailyRollingFileAppenderConfigurationType) appender;
	 * item.setDatePattern(daily.getDatePattern());
	 * item.setFilePath(daily.getFilePath());
	 * item.setAppending(daily.isAppend());
	 * item.setType(AppenderType.DAILY_ROLLING_FILE);
	 * 
	 * if (appender instanceof NdcDailyRollingFileAppenderConfigurationType) {
	 * item.setType(AppenderType.NDC_DAILY_ROLLING_FILE); } }
	 * 
	 * return item; }
	 * 
	 * private LoggingConfigurationType createConfiguration(List<LoggerListItem>
	 * loggers, List<AppenderListItem> appenders) { LoggingConfigurationType
	 * configuration = new LoggingConfigurationType(); for (AppenderListItem
	 * item : appenders) { AppenderConfigurationType appender =
	 * createAppenderType(item); configuration.getAppender().add(appender); }
	 * for (LoggerListItem item : loggers) { LoggerConfigurationType logger =
	 * createLoggerType(item, configuration);
	 * configuration.getLogger().add(logger); }
	 * 
	 * return configuration; }
	 * 
	 * private AppenderConfigurationType createAppenderType(AppenderListItem
	 * item) { AppenderConfigurationType appender = null;
	 * 
	 * RollingFileAppenderConfigurationType fileAppender = null;
	 * DailyRollingFileAppenderConfigurationType daily = null;
	 * 
	 * switch (item.getType()) { case CONSOLE: appender = new
	 * AppenderConfigurationType(); break; case NDC_ROLLING_FILE: fileAppender =
	 * new NdcRollingFileAppenderConfigurationType(); case ROLLING_FILE: if
	 * (fileAppender == null) { fileAppender = new
	 * RollingFileAppenderConfigurationType(); }
	 * fileAppender.setFilePath(item.getFilePath());
	 * fileAppender.setMaxFileSize(item.getMaxFileSize());
	 * fileAppender.setAppend(item.isAppending());
	 * 
	 * appender = fileAppender; break; case NDC_DAILY_ROLLING_FILE: daily = new
	 * NdcDailyRollingFileAppenderConfigurationType(); case DAILY_ROLLING_FILE:
	 * if (daily == null) { daily = new
	 * DailyRollingFileAppenderConfigurationType(); }
	 * daily.setDatePattern(item.getDatePattern());
	 * daily.setFilePath(item.getFilePath());
	 * daily.setAppend(item.isAppending());
	 * 
	 * appender = daily; break; }
	 * 
	 * appender.setName(item.getName()); appender.setPattern(item.getPattern());
	 * 
	 * return appender; }
	 * 
	 * private LoggerConfigurationType createLoggerType(LoggerListItem item,
	 * LoggingConfigurationType configuration) { LoggerConfigurationType logger
	 * = new LoggerConfigurationType(); for (String category :
	 * item.getCategories()) {
	 * logger.getCategory().add(LoggingCategoryType.fromValue(category)); } for
	 * (String component : item.getComponents()) {
	 * logger.getComponent().add(LoggingComponentType.fromValue(component)); }
	 * logger.getPackage().addAll(item.getPackages());
	 * logger.setLevel(item.getLevel());
	 * 
	 * for (String appender : item.getAppenders()) { if
	 * (!containsAppender(appender, configuration.getAppender())) { continue; }
	 * logger.getAppender().add(appender); }
	 * 
	 * return logger; }
	 * 
	 * private boolean containsAppender(String name,
	 * List<AppenderConfigurationType> appenders) { for
	 * (AppenderConfigurationType appender : appenders) { if
	 * (appender.getName().equals(name)) { return true; } }
	 * 
	 * return false; }
	 */
}
