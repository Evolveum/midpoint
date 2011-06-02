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
package com.evolveum.midpoint.web.controller.other;

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AppenderConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectContainerType;
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
	private static final String SYSTEM_CONFIGURATION_OID = "SystemConfiguration";
	@Autowired(required = true)
	private ModelPortType model;

	@PostConstruct
	public void init() {
		updateLogger();
	}

	public void updateLogger() {
		LoggingConfigurationType config = null;
		try {
			ObjectContainerType container = model.getObject(SYSTEM_CONFIGURATION_OID,
					new PropertyReferenceListType());
			if (container == null) {
				LOGGER.error("Couldn't get system configuration, reason: Empty container.");
				return;
			}
			SystemConfigurationType system = (SystemConfigurationType) container.getObject();
			config = system.getLogging();
		} catch (FaultMessage ex) {
			LOGGER.error("Couldn't get system configuration, reason: " + ex.getMessage());
			// TODO: error handling
			return;
		}

		if (config == null) {
			LOGGER.warn("Logging configuration was not found in system configuration.");
			return;
		}

		List<AppenderConfigurationType> appenders = config.getAppender();
		List<LoggerConfigurationType> loggers = config.getLogger();
		// TODO: update logger configuration
	}
}
