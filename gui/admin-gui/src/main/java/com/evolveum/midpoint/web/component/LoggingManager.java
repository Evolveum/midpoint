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
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.diff.CalculateXmlDiff;
import com.evolveum.midpoint.common.diff.DiffException;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.web.util.Utils;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AppenderConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1.ModelPortType;

/**
 * 
 * @author lazyman
 * 
 */
@Component
@Scope
public class LoggingManager {

	private static final Trace LOGGER = TraceManager.getTrace(LoggingManager.class);
	public static final String SYSTEM_CONFIGURATION_OID = "00000000-0000-0000-0000-000000000002";
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
		logging = system.getLogging();
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

		OperationResultType resultType = result.createOperationResultType();
		try {
			SystemConfigurationType oldSystem = getSystemConfiguration(result);
			SystemConfigurationType newSystem = (SystemConfigurationType) JAXBUtil.clone(oldSystem);
			newSystem.setLogging(logging);

			ObjectModificationType change = CalculateXmlDiff.calculateChanges(oldSystem, newSystem);
			change.setOid(SYSTEM_CONFIGURATION_OID);
			model.modifyObject(change, new Holder<OperationResultType>(resultType));

			return logging;
		} catch (JAXBException ex) {
			Utils.logException(LOGGER, "Couldn't clone system configuration", ex);
			// TODO: result error handling
		} catch (DiffException ex) {
			Utils.logException(LOGGER, "Couldn't create diff for system configuration", ex);
			// TODO: result error handling
		} catch (FaultMessage ex) {
			Utils.logException(LOGGER, "Couldn't get system configuration", ex);
			// TODO: result error handling
		}

		return null;
	}

	private SystemConfigurationType getSystemConfiguration(OperationResult result) {
		OperationResultType resultType = result.createOperationResultType();
		SystemConfigurationType config = new SystemConfigurationType();
		try {
			ObjectType object = model.getObject(SYSTEM_CONFIGURATION_OID, new PropertyReferenceListType(),
					new Holder<OperationResultType>(resultType));
			config = (SystemConfigurationType) object;
		} catch (FaultMessage ex) {
			Utils.logException(LOGGER, "Couldn't get system configuration", ex);
			// TODO: result error handling
		}

		return config;
	}
}
