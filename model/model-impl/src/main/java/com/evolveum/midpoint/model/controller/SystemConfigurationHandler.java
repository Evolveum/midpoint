/**
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model.controller;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.LoggingConfigurationManager;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemConfigurationType;

/**
 * @author semancik
 *
 */
@Component
public class SystemConfigurationHandler {
	
	private static final Trace LOGGER = TraceManager.getTrace(SystemConfigurationHandler.class);
	
	public void postInit(PrismObject<SystemConfigurationType> systemConfiguration, OperationResult parentResult) {
		SystemConfigurationType systemConfigurationType = systemConfiguration.asObjectable();
		LoggingConfigurationType loggingConfigType = systemConfigurationType.getLogging();
		if (loggingConfigType != null) {
			LoggingConfigurationManager.configure(loggingConfigType, parentResult);
		}
	}
	
	public void postChange(ObjectDelta<SystemConfigurationType> objectDelta, Task task, OperationResult parentResult) throws SchemaException {
		LOGGER.trace("Configuration postChange with delta:\n{}", objectDelta.dump());
		// TODO
		
		if (objectDelta.getChangeType() == ChangeType.ADD) {
			SystemConfigurationType systemConfigurationType = objectDelta.getObjectToAdd().asObjectable();
			LoggingConfigurationType loggingConfigType = systemConfigurationType.getLogging();
			if (loggingConfigType != null) {
				LoggingConfigurationManager.configure(loggingConfigType, parentResult);
			}
		
		} else if (objectDelta.getChangeType() == ChangeType.MODIFY) {
			PropertyDelta<LoggingConfigurationType> loggingDelta = objectDelta.findPropertyDelta(SystemConfigurationType.F_LOGGING);
			if (loggingDelta != null) {
				PrismProperty<LoggingConfigurationType> loggingProperty = loggingDelta.instantiateEmptyProperty();
				loggingDelta.applyTo(loggingProperty);
				LoggingConfigurationType newLoggingConfigType = loggingProperty.getRealValue();
				LoggingConfigurationManager.configure(newLoggingConfigType, parentResult);
			}
		} else {
			// Deleting system config or other strange change? What will we do? panic?
			// Let's do nothing
		}		
	}

}
