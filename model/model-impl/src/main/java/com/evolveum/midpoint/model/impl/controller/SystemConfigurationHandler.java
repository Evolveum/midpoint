/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.impl.controller;

import com.evolveum.midpoint.common.LoggingConfigurationManager;
import com.evolveum.midpoint.common.ProfilingConfigurationManager;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.hooks.ChangeHook;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SystemConfigurationTypeUtil;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.InternalsConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringNormalizerConfigurationType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author semancik
 *
 */
@Component
public class SystemConfigurationHandler implements ChangeHook {

	private static final Trace LOGGER = TraceManager.getTrace(SystemConfigurationHandler.class);

    private static final String DOT_CLASS = SystemConfigurationHandler.class + ".";

    public static final String HOOK_URI = "http://midpoint.evolveum.com/model/sysconfig-hook-1";

    @Autowired private HookRegistry hookRegistry;
    @Autowired private PrismContext prismContext;

    @Autowired
    @Qualifier("cacheRepositoryService")
    private transient RepositoryService cacheRepositoryService;

    @PostConstruct
    public void init() {
        hookRegistry.registerChangeHook(HOOK_URI, this);
    }

    private void applyLoggingConfiguration(LoggingConfigurationType loggingConfig, String version, OperationResult parentResult) throws SchemaException {
        if (loggingConfig != null) {
            LoggingConfigurationManager.configure(loggingConfig, version, parentResult);
        }
    }

    @Override
    public <O extends ObjectType> HookOperationMode invoke(@NotNull ModelContext<O> context, @NotNull Task task, @NotNull OperationResult parentResult) {

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

        if (context.getFocusClass() != SystemConfigurationType.class) {
        	LOGGER.trace("invoke() EXITING: Changes not related to systemConfiguration");
            return HookOperationMode.FOREGROUND;
        }
        ModelContext<SystemConfigurationType> confContext = (ModelContext<SystemConfigurationType>)context;
        ModelElementContext<SystemConfigurationType> focusContext = confContext.getFocusContext();

        boolean isDeletion = false;     // is this config-related change a deletion?
        PrismObject<SystemConfigurationType> object = focusContext.getObjectNew();
        if (object == null) {
        	isDeletion = true;
            object = focusContext.getObjectOld();
        }
        if (object == null) {
            LOGGER.warn("Probably invalid projection context: both old and new objects are null");          // if the handler would not work because of this, for us to see the reason
        }

        LOGGER.trace("change relates to sysconfig, is deletion: {}", isDeletion);

        OperationResult result = parentResult.createSubresult(DOT_CLASS + "invoke");
        try {
            if (isDeletion) {
                LoggingConfigurationManager.resetCurrentlyUsedVersion();        // because the new config (if any) will have version number probably starting at 1 - so to be sure to read it when it comes
                LOGGER.trace("invoke() EXITING because operation is DELETION");
                return HookOperationMode.FOREGROUND;
            }

            /*
             * Because we need to know actual version of the system configuration (generated by repo), we have to re-read
             * current configuration. (At this moment, it is already stored there.)
             */

            PrismObject<SystemConfigurationType> config = cacheRepositoryService.getObject(SystemConfigurationType.class,
                    SystemObjectsType.SYSTEM_CONFIGURATION.value(), null, result);

            LOGGER.trace("invoke() SystemConfig from repo: {}, ApplyingLoggingConfiguration", config.getVersion());

            SystemConfigurationType configType = config.asObjectable();
            SecurityUtil.setRemoteHostAddressHeaders(configType);

            applyLoggingConfiguration(ProfilingConfigurationManager.checkSystemProfilingConfiguration(config), configType.getVersion(), result);
            applyPrismConfiguration(configType);

			cacheRepositoryService.applyFullTextSearchConfiguration(config.asObjectable().getFullTextSearch());
            SystemConfigurationTypeUtil.applyOperationResultHandling(config.asObjectable());

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

    private void applyPrismConfiguration(SystemConfigurationType configType) {
    	PolyStringNormalizerConfigurationType normalizerConfig = null;
		InternalsConfigurationType internals = configType.getInternals();
		if (internals != null) {
			normalizerConfig = internals.getPolyStringNormalizer();
		}
		try {
			prismContext.configurePolyStringNormalizer(normalizerConfig);
			LOGGER.trace("Applied PolyString normalizer configuration {}", DebugUtil.shortDumpLazily(normalizerConfig));
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
			LOGGER.error("Error applying polystring normalizer configuration: "+e.getMessage(), e);
			throw new SystemException("Error applying polystring normalizer configuration: "+e.getMessage(), e);
		}
	}

	@Override
    public void invokeOnException(@NotNull ModelContext context, @NotNull Throwable throwable, @NotNull Task task, @NotNull OperationResult result) {
        // do nothing
    }
}
