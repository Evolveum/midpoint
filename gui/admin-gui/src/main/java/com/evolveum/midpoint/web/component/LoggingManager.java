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
package com.evolveum.midpoint.web.component;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.ws.Holder;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.SystemPropertyUtils;

import com.evolveum.midpoint.api.logging.LoggingUtils;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.logging.impl.NdcFilteringDailyRollingFileAppender;
import com.evolveum.midpoint.logging.impl.NdcFilteringRollingFileAppender;
import com.evolveum.midpoint.web.controller.util.ControllerUtil;
import com.evolveum.midpoint.web.model.ObjectTypeCatalog;
import com.evolveum.midpoint.web.model.SystemManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AppenderConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.DailyRollingFileAppenderConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggingComponentType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.NdcDailyRollingFileAppenderConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.NdcRollingFileAppenderConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.RollingFileAppenderConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1_wsdl.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1_wsdl.ModelPortType;

/**
 * 
 * @author lazyman
 * 
 */
public class LoggingManager {

	private static final Trace LOGGER = TraceManager.getTrace(LoggingManager.class);
	@Autowired(required = true)
	private ModelPortType model;
	@Autowired
	private ObjectTypeCatalog catalog;
	private LoggingConfigurationType logging;

	public void init() {
		LOGGER.info("Initializing Logging Manager.");
		OperationResult result = new OperationResult("Init Logging Manager");
		updateLoggers(result);
		result.computeStatus("Logging manager initialization finished with errors/warnings.");
		LOGGER.info(result.dump());
	}

	public LoggingConfigurationType getConfiguration(OperationResult result) {
		SystemConfigurationType system = getSystemConfiguration(result);
		if (system != null) {
			logging = system.getLogging();
		}
		if (logging == null) {
			logging = new LoggingConfigurationType();
		}

		return logging;
	}

	public void updateConfiguration(LoggingConfigurationType logging, OperationResult result) {
		// TODO: save configuration
		LoggingConfigurationType config = saveConfiguration(logging, result);
		if (config == null) {
			LOGGER.warn("System configuration is null, loggers won't be updated.");
			return;
		}
		this.logging = config;

		updateLoggers(result);
	}

	private synchronized void updateLoggers(OperationResult result) {
		LoggingConfigurationType config = getConfiguration(result);

		List<AppenderConfigurationType> appendersConf = config.getAppender();
		List<LoggerConfigurationType> loggersConf = config.getLogger();

		// cleanup log4j
		cleanupLog4j();

		// create new appenders
		Map<String, Appender> appenders = createLog4jAppendersFromConfiguration(appendersConf);

		// set new logger configuration
		udpateLog4jLoggersFromConfiguration(loggersConf, appenders);

		configureLog4jNdcFiltering(loggersConf);
	}

	private void cleanupLog4j() {
		LogManager.shutdown();
	}

	private void udpateLog4jLoggersFromConfiguration(List<LoggerConfigurationType> loggersConf,
			Map<String, Appender> appenders) {
		for (LoggerConfigurationType loggerConf : loggersConf) {
			for (String pckg : loggerConf.getPackage()) {
				Logger logger = Logger.getLogger(pckg);
				logger.setLevel(Level.toLevel(StringUtils.upperCase(loggerConf.getLevel().value())));
				logger.removeAllAppenders();
				for (String appenderName : loggerConf.getAppender()) {
					logger.addAppender(appenders.get(appenderName));
				}
			}
		}
	}

	private void configureLog4jNdcFiltering(List<LoggerConfigurationType> loggersConf) {
		// we have to iterate through loggersConf and set it to Log4j Appenders
		// following ugly hack is because we can control only Log4j Appenders,
		// but not Log4j loggers
		for (LoggerConfigurationType loggerConf : loggersConf) {

			for (String pckg : loggerConf.getPackage()) {
				Enumeration<Appender> appenders = Logger.getLogger(pckg).getAllAppenders();
				if (null != appenders) {
					while (appenders.hasMoreElements()) {
						Appender appender = (Appender) appenders.nextElement();
						List<String> components = new ArrayList<String>();
						for (LoggingComponentType lct : loggerConf.getComponent()) {
							components.add(lct.name());
						}

						if (appender instanceof NdcFilteringRollingFileAppender) {
							NdcFilteringRollingFileAppender ndcAppender = (NdcFilteringRollingFileAppender) appender;
							ndcAppender.addLoggerConfiguration(loggerConf.getPackage(), components);
						}
						if (appender instanceof NdcFilteringDailyRollingFileAppender) {
							NdcFilteringDailyRollingFileAppender ndcAppender = (NdcFilteringDailyRollingFileAppender) appender;
							ndcAppender.addLoggerConfiguration(loggerConf.getPackage(), components);
						}
					}
				}
			}
		}
	}

	private Map<String, Appender> createLog4jAppendersFromConfiguration(
			List<AppenderConfigurationType> appendersConf) {

		Map<String, Appender> appenders = new HashMap<String, Appender>();

		for (AppenderConfigurationType appenderConf : appendersConf) {
			if (appenderConf instanceof NdcRollingFileAppenderConfigurationType) {
				NdcFilteringRollingFileAppender appender = new NdcFilteringRollingFileAppender();
				NdcRollingFileAppenderConfigurationType appenderCnf = (NdcRollingFileAppenderConfigurationType) appenderConf;
				appender.setName(appenderCnf.getName());
				appender.setLayout(new PatternLayout(appenderCnf.getPattern()));
				appender.setMaxFileSize(Integer.valueOf(appenderCnf.getMaxFileSize()).toString() + "KB");
				appender.setAppend(appenderCnf.isAppend());
				appender.setFile(SystemPropertyUtils.resolvePlaceholders(appenderCnf.getFilePath()));
				appender.activateOptions();
				appenders.put(appender.getName(), appender);
				continue;
			}
			if (appenderConf instanceof NdcDailyRollingFileAppenderConfigurationType) {
				NdcFilteringDailyRollingFileAppender appender = new NdcFilteringDailyRollingFileAppender();
				NdcDailyRollingFileAppenderConfigurationType appenderCnf = (NdcDailyRollingFileAppenderConfigurationType) appenderConf;
				appender.setName(appenderCnf.getName());
				appender.setLayout(new PatternLayout(appenderCnf.getPattern()));
				appender.setDatePattern(appenderCnf.getDatePattern());
				appender.setAppend(appenderCnf.isAppend());
				appender.setFile(SystemPropertyUtils.resolvePlaceholders(appenderCnf.getFilePath()));
				appender.activateOptions();
				appenders.put(appender.getName(), appender);
				continue;
			}
			if (appenderConf instanceof RollingFileAppenderConfigurationType) {
				RollingFileAppender appender = new RollingFileAppender();
				RollingFileAppenderConfigurationType appenderCnf = (RollingFileAppenderConfigurationType) appenderConf;
				appender.setName(appenderCnf.getName());
				appender.setLayout(new PatternLayout(appenderCnf.getPattern()));
				appender.setMaxFileSize(Integer.valueOf(appenderCnf.getMaxFileSize()).toString() + "KB");
				appender.setAppend(appenderCnf.isAppend());
				appender.setFile(SystemPropertyUtils.resolvePlaceholders(appenderCnf.getFilePath()));
				appender.activateOptions();
				appenders.put(appender.getName(), appender);
				continue;
			}
			if (appenderConf instanceof DailyRollingFileAppenderConfigurationType) {
				DailyRollingFileAppender appender = new DailyRollingFileAppender();
				DailyRollingFileAppenderConfigurationType appenderCnf = (DailyRollingFileAppenderConfigurationType) appenderConf;
				appender.setName(appenderCnf.getName());
				appender.setLayout(new PatternLayout(appenderCnf.getPattern()));
				appender.setDatePattern(appenderCnf.getDatePattern());
				appender.setAppend(appenderCnf.isAppend());
				appender.setFile(SystemPropertyUtils.resolvePlaceholders(appenderCnf.getFilePath()));
				appender.activateOptions();
				appenders.put(appender.getName(), appender);
				continue;
			}
			if (appenderConf instanceof AppenderConfigurationType) {
				// console
				ConsoleAppender appender = new ConsoleAppender();
				appender.setName(appenderConf.getName());
				appender.setLayout(new PatternLayout(appenderConf.getPattern()));
				appenders.put(appender.getName(), appender);
				continue;
			}
		}

		return appenders;
	}

	private LoggingConfigurationType saveConfiguration(LoggingConfigurationType logging,
			OperationResult result) {
		Validate.notNull(logging, "Logging configuration can't be null.");

		// final OperationResult saveConfigResult = new
		// OperationResult("Save System Configuration");
		SystemManager manager = ControllerUtil.getSystemManager(catalog);
		if (manager.updateLoggingConfiguration(logging)) {
			return logging;
		}

		return null;

	}

	private SystemConfigurationType getSystemConfiguration(OperationResult result) {
		final OperationResult getSystemConfigResult = new OperationResult("Get System Configuration");

		OperationResultType resultTypeHolder = new OperationResultType();
		resultTypeHolder.setOperation(getSystemConfigResult.getOperation());

		SystemConfigurationType config = null;
		try {
			ObjectType object = model.getObject(SystemObjectsType.SYSTEM_CONFIGURATION.value(),
					new PropertyReferenceListType(), new Holder<OperationResultType>(resultTypeHolder));
			config = (SystemConfigurationType) object;

			getSystemConfigResult.recordSuccess();
		} catch (FaultMessage ex) {
			String message = "Couldn't get system configuration";
			LoggingUtils.logException(LOGGER, message, ex);

			getSystemConfigResult.recordFatalError(message, ex);
		} finally {
			OperationResult opResult = OperationResult.createOperationResult(resultTypeHolder);
			getSystemConfigResult.getSubresults().addAll(opResult.getSubresults());

			result.addSubresult(getSystemConfigResult);
		}

		if (config == null) {
			config = new SystemConfigurationType();
		}

		return config;
	}
}
