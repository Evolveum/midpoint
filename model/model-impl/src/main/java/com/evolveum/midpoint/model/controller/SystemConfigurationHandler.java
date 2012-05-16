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

import com.evolveum.midpoint.model.api.hooks.ChangeHook;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemObjectsType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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

import javax.annotation.PostConstruct;
import java.util.Collection;

/**
 * @author semancik
 *
 */
@Component
public class SystemConfigurationHandler implements ChangeHook {
	
	private static final Trace LOGGER = TraceManager.getTrace(SystemConfigurationHandler.class);

    private static final String DOT_CLASS = SystemConfigurationHandler.class + ".";

    public static final String HOOK_URI = "http://midpoint.evolveum.com/model/sysconfig-hook-1";

    @Autowired(required = true)
    private HookRegistry hookRegistry;

    @Autowired(required = true)
    @Qualifier("cacheRepositoryService")
    private transient RepositoryService cacheRepositoryService;

    @PostConstruct
    public void init() {
        hookRegistry.registerChangeHook(HOOK_URI, this);
    }

    public void postInit(PrismObject<SystemConfigurationType> systemConfiguration, OperationResult parentResult) {
        SystemConfigurationType systemConfigurationType = systemConfiguration.asObjectable();
        LoggingConfigurationType loggingConfigType = systemConfigurationType.getLogging();
        if (loggingConfigType != null) {
            LoggingConfigurationManager.configure(loggingConfigType, systemConfiguration.getVersion(), parentResult);
        }
    }

    @Override
    public HookOperationMode preChangePrimary(Collection<ObjectDelta<? extends ObjectType>> changes, Task task, OperationResult result) {
        return HookOperationMode.FOREGROUND;        // nothing to do here
    }

    @Override
    public HookOperationMode preChangeSecondary(Collection<ObjectDelta<? extends ObjectType>> changes, Task task, OperationResult result) {
        return HookOperationMode.FOREGROUND;        // nothing to do here
    }

    @Override
    public HookOperationMode postChange(Collection<ObjectDelta<? extends ObjectType>> changes, Task task, OperationResult parentResult) {

        boolean relatesToSystemConfiguration = false;
        boolean isDeletion = false;     // is last config-related change a deletion?
        for (ObjectDelta<? extends ObjectType> change : changes) {
            if (change.getObjectTypeClass().isAssignableFrom(SystemConfigurationType.class)) {
                relatesToSystemConfiguration = true;
                isDeletion = change.isDelete();
            }
        }

        if (!relatesToSystemConfiguration)
            return HookOperationMode.FOREGROUND;

        OperationResult result = parentResult.createSubresult(DOT_CLASS + "postChange");
        try {
            if (isDeletion) {
                LoggingConfigurationManager.resetCurrentlyUsedVersion();        // because the new config (if any) will have version number probably starting at 1 - so to be sure to read it when it comes
                return HookOperationMode.FOREGROUND;
            }

            /*
             * Because we need to know actual version of the system configuration (generated by repo), we have to re-read
             * current configuration. (At this moment, it is already stored there.)
             */

            PrismObject<SystemConfigurationType> config = cacheRepositoryService.getObject(SystemConfigurationType.class,
                    SystemObjectsType.SYSTEM_CONFIGURATION.value(), result);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("System configuration version read from repo: " + config.getVersion());
            }
            postInit(config, result);
            result.recordSuccessIfUnknown();
            return HookOperationMode.FOREGROUND;

        } catch (ObjectNotFoundException e) {
            String message = "Cannot read system configuration because it does not exist in repository: " + e.getMessage();
            LoggingUtils.logException(LOGGER, message, e);
            result.recordFatalError(message, e);
            return HookOperationMode.ERROR;         // currently has no effect
        } catch (SchemaException e) {
            String message = "Cannot read system configuration because of schema exception: " + e.getMessage();
            LoggingUtils.logException(LOGGER, message, e);
            result.recordFatalError(message, e);
            return HookOperationMode.ERROR;         // currently has no effect
        }
    }

//    /**
//     * This method is no longer sufficient, as we need to store version as generated from repository.
//     * (So we must get it from freshly-read repo object.)
//     *
//     * @param objectDelta
//     * @param task
//     * @param parentResult
//     * @throws SchemaException
//     */
//    @Deprecated
//	public void postChange(ObjectDelta<SystemConfigurationType> objectDelta, Task task, OperationResult parentResult) throws SchemaException {
//		LOGGER.trace("Configuration postChange with delta:\n{}", objectDelta.dump());
//		// TODO
//
//		if (objectDelta.getChangeType() == ChangeType.ADD) {
//            versionCurrentlyApplied = objectDelta.getObjectToAdd().getVersion();
//			SystemConfigurationType systemConfigurationType = objectDelta.getObjectToAdd().asObjectable();
//			LoggingConfigurationType loggingConfigType = systemConfigurationType.getLogging();
//			if (loggingConfigType != null) {
//				LoggingConfigurationManager.configure(loggingConfigType, parentResult);
//			}
//
//		} else if (objectDelta.getChangeType() == ChangeType.MODIFY) {
//            versionCurrentlyApplied = null;         // actually we do not know which version will be applied, so to be sure we reapply it on next occassion
//			PropertyDelta<LoggingConfigurationType> loggingDelta = objectDelta.findPropertyDelta(SystemConfigurationType.F_LOGGING);
//			if (loggingDelta != null) {
//				PrismProperty<LoggingConfigurationType> loggingProperty = loggingDelta.instantiateEmptyProperty();
//				loggingDelta.applyTo(loggingProperty);
//				LoggingConfigurationType newLoggingConfigType = loggingProperty.getRealValue();
//				LoggingConfigurationManager.configure(newLoggingConfigType, parentResult);
//			}
//		} else {
//			// Deleting system config or other strange change? What will we do? panic?
//			// Let's do nothing
//		}
//	}

}
