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
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.faces.component.UIParameter;
import javax.faces.event.ActionEvent;
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
import com.evolveum.midpoint.web.bean.ProfilingLevelType;
import com.evolveum.midpoint.web.bean.SubsystemLoggerListItem;
import com.evolveum.midpoint.web.component.LoggingManager;
import com.evolveum.midpoint.web.controller.util.ControllerUtil;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.web.util.SelectItemComparator;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AppenderConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ClassLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.FileAppenderConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggingComponentType;
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

	private SubsystemLoggerListItem midpointLogger;

	private BasicLoggerListItem profilingLogger = null;
	private List<SelectItem> profilingLevels;
	private String selectedProfilingLevel;

	private List<BasicLoggerListItem> loggers;
	private List<AppenderListItem> appenders;
	private List<SubsystemLoggerListItem> subsystemLoggers;

	private boolean advancedView = false;

	private boolean selectAllLoggers = false;
	private boolean selectAllAppenders = false;

	public List<SelectItem> getLevels() {
		List<SelectItem> levels = new ArrayList<SelectItem>();
		for (LoggingLevelType type : LoggingLevelType.values()) {
			levels.add(new SelectItem(type.value()));
		}

		// Collections.sort(levels, new SelectItemComparator());

		return levels;
	}

	public List<SelectItem> getComponents() {
		List<SelectItem> components = new ArrayList<SelectItem>();
		for (LoggingComponentType component : LoggingComponentType.values()) {
			components.add(new SelectItem(component.value()));
		}

		Collections.sort(components, new SelectItemComparator());

		return components;
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

	public void selectLoggerPerformed(ValueChangeEvent evt) {
		this.selectAllLoggers = ControllerUtil.selectPerformed(evt, getLoggers());
	}

	public void selectAllAppendersPerformed(ValueChangeEvent evt) {
		ControllerUtil.selectAllPerformed(evt, getAppenders());
	}

	public List<BasicLoggerListItem> getLoggers() {
		if (loggers == null) {
			loggers = new ArrayList<BasicLoggerListItem>();
		}
		return loggers;
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

		if (appenders.isEmpty()) {
			appenders.add(new SelectItem(""));
		} else {
			appenders.add(0, new SelectItem(""));
		}

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
		Iterator<AppenderListItem> iterator = getAppenders().iterator();
		while (iterator.hasNext()) {
			AppenderListItem item = iterator.next();
			if (item.isSelected()) {
				iterator.remove();
			}
		}
	}

	public void addLogger() {
		int id = getNewLoggerId(getLoggers());
		LoggerListItem item = new LoggerListItem(id);
		item.setEditing(true);

		getLoggers().add(item);
	}

	public void addSubsystemLogger() {
		int id = getNewLoggerId(getLoggers());
		SubsystemLoggerListItem item = new SubsystemLoggerListItem(id);
		item.setEditing(true);

		getLoggers().add(item);
	}

	private <T extends BasicLoggerListItem> int getNewLoggerId(List<T> loggers) {
		int id = 0;
		for (T item : loggers) {
			if (item.getId() >= id) {
				id = item.getId() + 1;
			}
		}

		return id;
	}

	public void editLogger() {
		BasicLoggerListItem logger = getLogger(FacesUtils.getRequestParameter(PARAM_LOGGER_ID));
		if (logger != null) {
			logger.setEditing(true);
		}
	}

	public void deleteLoggers() {
		Iterator<BasicLoggerListItem> iterator = getLoggers().iterator();
		while (iterator.hasNext()) {
			BasicLoggerListItem item = iterator.next();
			if (item.isSelected()) {
				iterator.remove();
			}
		}
	}

	private BasicLoggerListItem getLogger(String loggerId) {
		if (StringUtils.isEmpty(loggerId) || !loggerId.matches("[0-9]*")) {
			FacesUtils.addErrorMessage("Logger id not defined.");
			return null;
		}

		int id = Integer.parseInt(loggerId);
		for (BasicLoggerListItem item : getLoggers()) {
			if (item.getId() == id) {
				return item;
			}
		}

		for (BasicLoggerListItem item : getSubsystemLoggers()) {
			if (item.getId() == id) {
				return item;
			}
		}

		return null;
	}

	public void deleteLoggerAppender() {
		String appenderName = FacesUtils.getRequestParameter(PARAM_APPENDER_ID);
		if (StringUtils.isEmpty(appenderName)) {
			FacesUtils.addErrorMessage("Appender id not defined.");
			return;
		}

		BasicLoggerListItem logger = getLogger(FacesUtils.getRequestParameter(PARAM_LOGGER_ID));
		if (logger == null) {
			FacesUtils
					.addWarnMessage("Couldn't remove package, because couldn't find logger by internal id.");
			return;
		}

		Iterator<String> iterator = logger.getAppenders().iterator();
		while (iterator.hasNext()) {
			if (iterator.next().equals(appenderName)) {
				iterator.remove();
				break;
			}
		}
	}

	public void addLoggerAppender(ValueChangeEvent event) {
		if (!ControllerUtil.isEventAvailable(event)) {
			return;
		}

		UIParameter parameter = (UIParameter) event.getComponent().findComponent(PARAM_LOGGER_ID);
		if (parameter == null) {
			FacesUtils.addErrorMessage("Couldn't find logger parameter by internal id.");
			return;
		}
		String value = (String) event.getNewValue();
		if (StringUtils.isBlank(value)) {
			return;
		}
		BasicLoggerListItem logger = getLogger(parameter.getValue().toString());
		if (logger == null) {
			FacesUtils.addWarnMessage("Couldn't get logger by internal id.");
			return;
		}
		logger.getAppenders().add(value);
	}

	public void savePerformed() {
		LoggingConfigurationType logging = createConfiguration(getLoggers(), getAppenders());
		OperationResult result = new OperationResult("Load Logging Configuration");
		try {
			loggingManager.updateConfiguration(logging, result);
			result.recordSuccess();

		} catch (Exception ex) {
			result.recordFatalError("Couldn't update logging configuration.", ex);
			LoggingUtils.logException(LOGGER, "Couldn't update logging configuration", ex);
		} finally {
			result.computeStatus("Couldn't update logging configuration.");
			ControllerUtil.printResults(LOGGER, result, "Changes saved sucessfully.");
		}
		initController();
	}

	public void cancelPerformed() {
		initController();
	}

	public String initController() {
		getLoggers().clear();
		getAppenders().clear();

		selectAllAppenders = false;
		selectAllLoggers = false;
		
		OperationResult result = new OperationResult("Load Logging Configuration");

		
		try {
			LoggingConfigurationType logging = loggingManager.getConfiguration(result);
			if (logging == null) {
				result.recordFatalError("Couldn't get logging configuration.");
				LoggingUtils.logException(LOGGER, "Couldn't get logging configuration.",
						new IllegalStateException(), "");
				ControllerUtil.printResults(LOGGER, result, null);
				return PAGE_NAVIGATION;
			}

			rootLoggerLevel = logging.getRootLoggerLevel();
			rootAppender = logging.getRootLoggerAppender();

			for (AppenderConfigurationType appender : logging.getAppender()) {
				if (!(appender instanceof FileAppenderConfigurationType)) {
					result.recordPartialError("Unknown appender '" + appender.getName() + "'.");
					LoggingUtils.logException(LOGGER, "Unknown appender {}.", new IllegalStateException(),
							appender.getName());
					continue;
				}
				getAppenders().add(createAppenderListItem((FileAppenderConfigurationType) appender));
			}

			

			//create loggers list
			createLoggerList(logging);
			
			
			result.recordSuccess();
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't get logging configuration.", ex);
			result.recordFatalError("Couldn't get logging configuration. Reason: " + ex.getMessage(), ex);
			// FacesUtils.addErrorMessage("Couldn't get logging configuration.",
			// ex);
		} finally {
			ControllerUtil.printResults(LOGGER, result, null);
		}
		// initController();

		return PAGE_NAVIGATION;
	}

	private void createLoggerList(LoggingConfigurationType logging) {
		int id = 0;
		List<SubsystemLoggerListItem> subsystemItems = new ArrayList<SubsystemLoggerListItem>();
		
		// create logger item for each defined class or package..if defined
		// logger is for profiling (aspect) create special case
		for (ClassLoggerConfigurationType classLogger : logging.getClassLogger()) {
			if ("PROFILING".equals(classLogger.getPackage())) {
				profilingLogger = createLoggerListItem(id, classLogger);
				id++;
				break;
			}
			getLoggers().add(createLoggerListItem(id, classLogger));
			id++;
		}

		// check if profiling logger is defined in default configuration..if
		// not, create new one with root logger properties
		if (profilingLogger == null) {
			profilingLogger = createLoggerListItem(id, null);
			((LoggerListItem) profilingLogger).setPackageName("PROFILING");
			id++;
		}

		// create subsystem logger item for each of subsystem
		// for component logger ALL, create special case
		for (SubSystemLoggerConfigurationType logger : logging.getSubSystemLogger()) {
			SubsystemLoggerListItem subsystemLoggerItem = createSubsystemLoggerListItem(id, logger);
			// subsystemItems.add(subsystemLoggerItem);
			id++;
			subsystemItems.add(subsystemLoggerItem);
			if (subsystemLoggerItem.getComponent().equals(LoggingComponentType.ALL)) {
				midpointLogger = subsystemLoggerItem;
				id++;
			}
		}

		// check if ALL component logger has been configured, if not, create
		// new default configuration with root logger properties
		if (midpointLogger == null) {
			midpointLogger = createDefaultSubsystemLoggerListItem(id, LoggingComponentType.ALL);
			id++;
		}

		// check configuration of all subsystem loggers, if the logger for
		// certain subsystem is defined, use this configuration, otherwise
		// create new default configuration using root logger properties
		createSubsystemLogerList(subsystemItems, id);
		
	}

	private void createSubsystemLogerList(List<SubsystemLoggerListItem> subsystemLoggers, int id) {

		for (LoggingComponentType loggingComp : LoggingComponentType.values()) {

			// if logger component is all, skip..this is a special case and is
			// processed independently
			if (!loggingComp.equals(LoggingComponentType.ALL)) {
				//if logger for component was defined, used this config
				if (!isDefined(loggingComp, subsystemLoggers)) {
					//else create new
					SubsystemLoggerListItem subsystemItem = createDefaultSubsystemLoggerListItem(id,
							loggingComp);
					getLoggers().add(subsystemItem);
					id++;
				}
			}
		}

	}

	// this method check if the logger for certain subsystem was defined in
	// configuration.
	// if does, the configuration will be used, otherwise a empty configuration
	// will be made..
	private boolean isDefined(LoggingComponentType type, List<SubsystemLoggerListItem> subsystemLoggers) {
		if (subsystemLoggers == null) {
			return false;
		}
		for (SubsystemLoggerListItem subsystemLogger : subsystemLoggers) {
			if (type.equals(subsystemLogger.getComponent())) {
				getLoggers().add(subsystemLogger);
				return true;
			}
		}
		return false;
	}

	public void changeView(ActionEvent evt) {
		advancedView = !advancedView;
	}

//	public String initController() {
//		getAppenders().clear();
//		getLoggers().clear();
//
//		selectAllAppenders = false;
//		selectAllLoggers = false;
//
//		OperationResult result = new OperationResult("Load Logging Configuration");
//		try {
//			LoggingConfigurationType logging = loggingManager.getConfiguration(result);
//			if (logging == null) {
//				result.recordFatalError("Couldn't get logging configuration.");
//				LoggingUtils.logException(LOGGER, "Couldn't get logging configuration.",
//						new IllegalStateException(), "");
//				ControllerUtil.printResults(LOGGER, result, null);
//				return PAGE_NAVIGATION;
//			}
//
//			rootLoggerLevel = logging.getRootLoggerLevel();
//			rootAppender = logging.getRootLoggerAppender();
//
//			for (AppenderConfigurationType appender : logging.getAppender()) {
//				if (!(appender instanceof FileAppenderConfigurationType)) {
//					result.recordPartialError("Unknown appender '" + appender.getName() + "'.");
//					LoggingUtils.logException(LOGGER, "Unknown appender {}.", new IllegalStateException(),
//							appender.getName());
//					// FacesUtils.addWarnMessage("Unknown appender '" +
//					// appender.getName() + "'.");
//					continue;
//				}
//				getAppenders().add(createAppenderListItem((FileAppenderConfigurationType) appender));
//			}
//
//			int id = 0;
//			for (ClassLoggerConfigurationType logger : logging.getClassLogger()) {
//				getLoggers().add(createLoggerListItem(id, logger));
//				id++;
//			}
//
//			for (SubSystemLoggerConfigurationType logger : logging.getSubSystemLogger()) {
//				SubsystemLoggerListItem subsystemLoggerItem = createSubsystemLoggerListItem(id, logger);
//				getLoggers().add(subsystemLoggerItem);
//				id++;
//			}
//			result.recordSuccess();
//		} catch (Exception ex) {
//			LoggingUtils.logException(LOGGER, "Couldn't get logging configuration.", ex);
//			result.recordFatalError("Couldn't get logging configuration. Reason: " + ex.getMessage(), ex);
//			// FacesUtils.addErrorMessage("Couldn't get logging configuration.",
//			// ex);
//		} finally {
//			result.computeStatus("Couldn't init logging configuration.");
//			ControllerUtil.printResults(LOGGER, result, "Changes saved sucessfully.");
//		}
//
//		return PAGE_NAVIGATION;
//	}

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
		if (logger == null) {
			List<String> appenders = new ArrayList<String>();
			appenders.add(rootAppender);
			item.setAppenders(appenders);
			item.setLevel(rootLoggerLevel);
		} else {
			item.setAppenders(logger.getAppender());
			item.setLevel(logger.getLevel());
			item.setPackageName(logger.getPackage());
		}
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

	private SubsystemLoggerListItem createDefaultSubsystemLoggerListItem(int id,
			LoggingComponentType component) {
		SubsystemLoggerListItem item = new SubsystemLoggerListItem(id);
		item.setLevel(rootLoggerLevel);
		item.setComponent(component);
		List<String> appenders = new ArrayList<String>();
		appenders.add(rootAppender);
		item.setAppenders(appenders);
		return item;
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

	/**
	 * This method creates JAXB xml representation of current logging settings
	 * which were created by user on logging page
	 */
	private LoggingConfigurationType createConfiguration(List<BasicLoggerListItem> loggers,
			List<AppenderListItem> appenders) {
		LoggingConfigurationType configuration = new LoggingConfigurationType();
		configuration.setRootLoggerAppender(getRootAppender());
		configuration.setRootLoggerLevel(rootLoggerLevel);
	
		loggers.add(midpointLogger);

		for (AppenderListItem item : appenders) {
			AppenderConfigurationType appender = createAppenderType(item);
			configuration.getAppender().add(appender);
		}
		for (BasicLoggerListItem item : loggers) {
			if (item instanceof LoggerListItem) {
				if (((LoggerListItem) item).getPackageName().equals("PROFILING")) {
					continue;
				}
				ClassLoggerConfigurationType logger = createClassLogger((LoggerListItem) item, appenders);
				configuration.getClassLogger().add(logger);
			} else if (item instanceof SubsystemLoggerListItem) {
				SubSystemLoggerConfigurationType logger = createSubsystemLogger(
						(SubsystemLoggerListItem) item, appenders);
				configuration.getSubSystemLogger().add(logger);
			}
		}

		updateProfilingLogger(profilingLogger);
		ClassLoggerConfigurationType logger = createClassLogger((LoggerListItem) profilingLogger, appenders);
		configuration.getClassLogger().add(logger);

		return configuration;
	}

	public List<SubsystemLoggerListItem> getSubsystemLoggers() {
		if (subsystemLoggers == null) {
			subsystemLoggers = new ArrayList<SubsystemLoggerListItem>();
		}
		return subsystemLoggers;
	}

	public void setSubsystemLoggers(List<SubsystemLoggerListItem> subsystemLoggers) {
		this.subsystemLoggers = subsystemLoggers;
	}

	public boolean isAdvancedView() {
		return advancedView;
	}

	public void setAdvancedView(boolean advancedView) {
		this.advancedView = advancedView;
	}

	public BasicLoggerListItem getProfilingLogger() {
		return profilingLogger;
	}

	public void setProfilingLogger(BasicLoggerListItem profilingLogger) {
		this.profilingLogger = profilingLogger;
	}

	public List<SelectItem> getProfilingLevels() {

		profilingLevels = new ArrayList<SelectItem>();

		for (ProfilingLevelType level : ProfilingLevelType.values()) {
			SelectItem si = new SelectItem(level.getValue());
			profilingLevels.add(si);
		}

		return profilingLevels;
	}

	public void setProfilingLevels(List<SelectItem> profilingLevels) {
		this.profilingLevels = profilingLevels;
	}

	public String getSelectedProfilingLevel() {
		if (profilingLogger != null && profilingLogger.getLevel() != null) {
			selectedProfilingLevel = ProfilingLevelType.fromLoggerLevelType(profilingLogger.getLevel())
					.getValue();
		}
		return selectedProfilingLevel;
	}

	public void setSelectedProfilingLevel(String selectedProfilingLevel) {
		this.selectedProfilingLevel = selectedProfilingLevel;
	}

	private void updateProfilingLogger(BasicLoggerListItem profilingLogger) {
		LoggingLevelType level = ProfilingLevelType.toLoggerLevelType(selectedProfilingLevel);
		profilingLogger.setLevel(level);
	}

	public SubsystemLoggerListItem getMidpointLogger() {
		return midpointLogger;
	}

	public void setMidpointLogger(SubsystemLoggerListItem midpointLogger) {
		this.midpointLogger = midpointLogger;
	}
}
