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

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBException;
import javax.xml.ws.Holder;

import org.apache.commons.lang.Validate;
import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.api.logging.LoggingUtils;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.diff.CalculateXmlDiff;
import com.evolveum.midpoint.common.diff.DiffException;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.logging.impl.NdcFilteringDailyRollingFileAppender;
import com.evolveum.midpoint.logging.impl.NdcFilteringRollingFileAppender;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AppenderConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.FileAppenderConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggingComponentType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1.ModelPortType;

/**
 * 
 * @author lazyman
 * 
 */
@Component
@Scope
@DependsOn(value = "initialSetup")
public class LoggingManager {

	private static final Trace LOGGER = TraceManager.getTrace(LoggingManager.class);
	@Autowired(required = true)
	private ModelPortType model;
	private LoggingConfigurationType logging;

	@PostConstruct
	public void init() {
		LOGGER.info("Initializing Logging Manager.");
		OperationResult result = new OperationResult("Init Logging Manager");
		updateLogger(result);
		result.computeStatus();
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

		updateLogger(result);
	}

	public synchronized void updateLogger(OperationResult result) {
		LoggingConfigurationType config = getConfiguration(result);

		List<AppenderConfigurationType> appendersConf = config.getAppender();
		List<LoggerConfigurationType> loggersConf = config.getLogger();

		// clear appenders configurations
		clearLog4jAppendersConfiguration(loggersConf);

		// create new appenders
		Map<String, Appender> appenders = createLog4jAppendersFromConfiguration(appendersConf);

		// set new logger configuration
		udpateLog4jLoggersFromConfiguration(loggersConf, appenders);

		configureLog4jNdcFiltering(loggersConf);
	}

	private void udpateLog4jLoggersFromConfiguration(List<LoggerConfigurationType> loggersConf, Map<String, Appender> appenders) {
		for (LoggerConfigurationType loggerConf : loggersConf) {
			for (String pckg : loggerConf.getPackage()) {
				Logger logger = Logger.getLogger(pckg);
				logger.setLevel(logger.getLevel());
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

	private void clearLog4jAppendersConfiguration(List<LoggerConfigurationType> loggersConf) {
		for (LoggerConfigurationType loggerConf : loggersConf) {
			for (String pckg : loggerConf.getPackage()) {
				Enumeration appenders = Logger.getRootLogger().getLogger(pckg).getAllAppenders();
				if (null != appenders) {
					while (appenders.hasMoreElements()) {
						Appender appender = (Appender) appenders.nextElement();
						if (appender instanceof NdcFilteringRollingFileAppender) {
							NdcFilteringRollingFileAppender ndcAppender = (NdcFilteringRollingFileAppender) appender;
							ndcAppender.resetLoggerConfiguration();
						}
						if (appender instanceof NdcFilteringDailyRollingFileAppender) {
							NdcFilteringDailyRollingFileAppender ndcAppender = (NdcFilteringDailyRollingFileAppender) appender;
							ndcAppender.resetLoggerConfiguration();
						}

					}
				}
			}
		}
	}

	private Map<String, Appender> createLog4jAppendersFromConfiguration(List<AppenderConfigurationType> appendersConf) {
		
		Map<String, Appender> appenders = new HashMap<String, Appender>();
		
		for (AppenderConfigurationType appenderConf : appendersConf) {
			if (appenderConf instanceof FileAppenderConfigurationType) {
				FileAppender appender = new FileAppender();
				appender.setName(appenderConf.getName());
				appender.setLayout(new PatternLayout(appenderConf.getPattern()));
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

		final OperationResult saveConfigResult = new OperationResult("Save System Configuration");

		OperationResultType resultTypeHolder = saveConfigResult.createOperationResultType();
		try {
			SystemConfigurationType oldSystem = getSystemConfiguration(result);
			SystemConfigurationType newSystem = (SystemConfigurationType) JAXBUtil.clone(oldSystem);
			newSystem.setLogging(logging);

			ObjectModificationType change = CalculateXmlDiff.calculateChanges(oldSystem, newSystem);
			change.setOid(SystemObjectsType.SYSTEM_CONFIGURATION.value());
			model.modifyObject(change, new Holder<OperationResultType>(resultTypeHolder));
			saveConfigResult.recordSuccess();

			return logging;
		} catch (JAXBException ex) {
			String message = "Couldn't clone system configuration";
			LoggingUtils.logException(LOGGER, message, ex);
			saveConfigResult.recordFatalError(message, ex);
		} catch (DiffException ex) {
			String message = "Couldn't create diff for system configuration";
			LoggingUtils.logException(LOGGER, message, ex);
			saveConfigResult.recordFatalError(message, ex);
		} catch (FaultMessage ex) {
			String message = "Couldn't get system configuration";
			LoggingUtils.logException(LOGGER, message, ex);
			saveConfigResult.recordFatalError(message, ex);
		} finally {
			OperationResult opResult = OperationResult.createOperationResult(resultTypeHolder);
			saveConfigResult.getSubresults().addAll(opResult.getSubresults());

			result.addSubresult(saveConfigResult);
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
