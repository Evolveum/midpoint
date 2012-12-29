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

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.hooks.ChangeHook;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.LoggingConfigurationManager;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemObjectsType;

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
        applyLoggingConfiguration(systemConfiguration, parentResult);
    }

    private void applyLoggingConfiguration(PrismObject<SystemConfigurationType> systemConfiguration, OperationResult parentResult) {
        SystemConfigurationType systemConfigurationType = systemConfiguration.asObjectable();
        LoggingConfigurationType loggingConfigType = systemConfigurationType.getLogging();
        if (loggingConfigType != null) {
            LoggingConfigurationManager.configure(loggingConfigType, systemConfiguration.getVersion(), parentResult);
        }
    }

//    @Override
//    @Deprecated
//    public void postChange(Collection<ObjectDelta<? extends ObjectType>> changes, Task task, OperationResult parentResult) {
//
//        boolean relatesToSystemConfiguration = false;
//        boolean isDeletion = false;     // is last config-related change a deletion?
//        for (ObjectDelta<? extends ObjectType> change : changes) {
//            if (change.getObjectTypeClass().isAssignableFrom(SystemConfigurationType.class)) {
//                relatesToSystemConfiguration = true;
//                isDeletion = change.isDelete();
//            }
//        }
//
//        if (!relatesToSystemConfiguration) {
//            return;
//        }
//
//        OperationResult result = parentResult.createSubresult(DOT_CLASS + "postChange");
//        try {
//            if (isDeletion) {
//                LoggingConfigurationManager.resetCurrentlyUsedVersion();        // because the new config (if any) will have version number probably starting at 1 - so to be sure to read it when it comes
//                return;
//            }
//
//            /*
//             * Because we need to know actual version of the system configuration (generated by repo), we have to re-read
//             * current configuration. (At this moment, it is already stored there.)
//             */
//
//            PrismObject<SystemConfigurationType> config = cacheRepositoryService.getObject(SystemConfigurationType.class,
//                    SystemObjectsType.SYSTEM_CONFIGURATION.value(), result);
//
//            if (LOGGER.isTraceEnabled()) {
//                LOGGER.trace("System configuration version read from repo: " + config.getVersion());
//            }
//            applyLoggingConfiguration(config, result);
//            result.recordSuccessIfUnknown();
//
//        } catch (ObjectNotFoundException e) {
//            String message = "Cannot read system configuration because it does not exist in repository: " + e.getMessage();
//            LoggingUtils.logException(LOGGER, message, e);
//            result.recordFatalError(message, e);
//        } catch (SchemaException e) {
//            String message = "Cannot read system configuration because of schema exception: " + e.getMessage();
//            LoggingUtils.logException(LOGGER, message, e);
//            result.recordFatalError(message, e);
//        }
//    }

    @Override
    public HookOperationMode invoke(ModelContext context, Task task, OperationResult parentResult) {

        ModelState state = context.getState();
        if (state != ModelState.FINAL) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("sysconfig handler called in state = " + state + ", exiting.");
            }
            return HookOperationMode.FOREGROUND;
        } else {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("sysconfig handler called in state = " + state + ", proceeding.");
            }
        }

        boolean relatesToSystemConfiguration = false;
        boolean isDeletion = false;     // is this config-related change a deletion?
        for (Object o : context.getProjectionContexts()) {      // for some reason, system configuration is treated as a projection, not as a focus
            boolean deletion = false;
            PrismObject object = ((ModelElementContext) o).getObjectNew();
            if (object == null) {
                deletion = true;
                object = ((ModelElementContext) o).getObjectOld();
            }
            if (object == null) {
                LOGGER.warn("Probably invalid projection context: both old and new objects are null");          // if the handler would not work because of this, for us to see the reason
            } else if (object.getCompileTimeClass().isAssignableFrom(SystemConfigurationType.class)) {
                relatesToSystemConfiguration = true;
                isDeletion = deletion;
            }
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("change relates to sysconfig: " + relatesToSystemConfiguration + ", is deletion: " + isDeletion);
        }

        if (!relatesToSystemConfiguration) {
            return HookOperationMode.FOREGROUND;
        }

        OperationResult result = parentResult.createSubresult(DOT_CLASS + "invoke");
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
            applyLoggingConfiguration(config, result);
            result.recordSuccessIfUnknown();

        } catch (ObjectNotFoundException e) {
            String message = "Cannot read system configuration because it does not exist in repository: " + e.getMessage();
            LoggingUtils.logException(LOGGER, message, e);
            result.recordFatalError(message, e);
        } catch (SchemaException e) {
            String message = "Cannot read system configuration because of schema exception: " + e.getMessage();
            LoggingUtils.logException(LOGGER, message, e);
            result.recordFatalError(message, e);
        }

        return HookOperationMode.FOREGROUND;
    }
}
