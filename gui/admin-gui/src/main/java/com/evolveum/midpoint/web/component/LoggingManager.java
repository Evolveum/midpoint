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
import java.util.Map;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.common.LoggingConfigurationManager;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.model.ObjectTypeCatalog;
import com.evolveum.midpoint.web.model.SystemManager;
import com.evolveum.midpoint.web.controller.config.LoggingController;
import com.evolveum.midpoint.web.controller.util.ControllerUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AppenderConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ClassLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SubSystemLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemObjectsType;

/**
 * 
 * @author lazyman
 * 
 */
public class LoggingManager {

	private static final Trace LOGGER = TraceManager.getTrace(LoggingManager.class);
	@Autowired(required = true)
	private ModelService model;
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
			LOGGER.warn("System configuration is null, LOGGERs won't be updated.");
			return;
		}
	
		this.logging = config;

		updateLoggers(result);
	}

	private synchronized void updateLoggers(OperationResult result) {
		LoggingConfigurationType config = getConfiguration(result);
		//LoggingConfigurationManager.configure(config, result);
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
		final OperationResult getSystemConfigResult = result.createSubresult("Get System Configuration");

		SystemConfigurationType config = null;
		try {
			ObjectType object = model.getObject(SystemConfigurationType.class,
					SystemObjectsType.SYSTEM_CONFIGURATION.value(), new PropertyReferenceListType(),
					getSystemConfigResult);
			config = (SystemConfigurationType) object;

			getSystemConfigResult.recordSuccess();
		} catch (Exception ex) {
			String message = "Couldn't get system configuration";
			LoggingUtils.logException(LOGGER, message, ex);

			getSystemConfigResult.recordFatalError(message, ex);
		} finally {
			getSystemConfigResult.computeStatus();
		}

		if (config == null) {
			config = new SystemConfigurationType();
		}

		return config;
	} 
}
