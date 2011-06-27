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

import java.util.List;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBException;
import javax.xml.ws.Holder;

import org.apache.commons.lang.Validate;
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
import com.evolveum.midpoint.web.model.ObjectTypeCatalog;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AppenderConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggerConfigurationType;
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
	private ObjectTypeCatalog objectTypeCatalog;
	@Autowired(required = true)
	private ModelPortType model;
	private LoggingConfigurationType logging;

	@PostConstruct
	public void init() {
		LOGGER.info("Initializing Logging Manager.");
		OperationResult result = new OperationResult("Init Logging Manager");
		updateLogger(result);
		LOGGER.info(result.debugDump());
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

	public void updateLogger(OperationResult result) {
		LoggingConfigurationType config = getConfiguration(result);

		List<AppenderConfigurationType> appenders = config.getAppender();
		List<LoggerConfigurationType> loggers = config.getLogger();
		// TODO: update logger configuration
	}

	private LoggingConfigurationType saveConfiguration(LoggingConfigurationType logging,
			OperationResult result) {
		Validate.notNull(logging, "Logging configuration can't be null.");

		final OperationResult saveConfigResult = new OperationResult("Save System Configuration");

		OperationResultType resultTypeHolder = new OperationResultType();
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
