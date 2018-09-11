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

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.common.LoggingConfigurationManager;
import com.evolveum.midpoint.common.ProfilingConfigurationManager;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeApplier;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SystemConfigurationTypeUtil;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.InternalsConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringNormalizerConfigurationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * @author mederly
 */
@Component
public class SystemConfigurationChangeApplierImpl implements SystemConfigurationChangeApplier {

	private static final Trace LOGGER = TraceManager.getTrace(SystemConfigurationChangeApplierImpl.class);

	@Autowired private RepositoryService repositoryService;
	@Autowired private PrismContext prismContext;
	@Autowired private RelationRegistry relationRegistry;

	private String lastVersionApplied = null;

	public synchronized void applySystemConfiguration(boolean ignoreVersion, boolean allowNotFound,
			OperationResult result) throws SchemaException {
		LOGGER.trace("Applying system configuration: lastVersionApplied = {}, ignoreVersion = {}", lastVersionApplied,
				ignoreVersion);

		Collection<SelectorOptions<GetOperationOptions>> options = GetOperationOptions.createReadOnlyCollection();
		PrismObject<SystemConfigurationType> configurationObject;
		try {
			configurationObject = repositoryService
					.getObject(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(),
							options, result);
		} catch (ObjectNotFoundException e) {
			if (allowNotFound) {
				LOGGER.debug("System configuration not found");
				result.muteLastSubresultError();
			} else {
				LOGGER.warn("System configuration not found");
			}
			lastVersionApplied = null;
			return;
		}

		String currentVersion = configurationObject.getVersion();
		if (!ignoreVersion && lastVersionApplied != null && lastVersionApplied.equals(currentVersion)) {
			LOGGER.trace("Last version applied ({}) is the same as the current version ({}), skipping application",
					lastVersionApplied, currentVersion);
			return;
		}

		SystemConfigurationType configuration = configurationObject.asObjectable();

		// This value is reset to null on exception in applyXXX method. This causes repeated application of sysconfig,
		// because this method is called also from the cluster management thread.
		lastVersionApplied = currentVersion;

		applyLoggingConfiguration(configurationObject, result);
		applyRemoteHostAddressHeadersConfiguration(configuration);
		applyPolyStringNormalizerConfiguration(configuration);
		applyFullTextSearchConfiguration(configuration);
		applyRelationsConfiguration(configuration);
		applyOperationResultHandlingConfiguration(configuration);

		if (lastVersionApplied != null) {
			LOGGER.trace("System configuration version {} applied successfully", lastVersionApplied);
		} else {
			LOGGER.warn("There was a problem during application of the system configuration");
		}
	}

	private void applyLoggingConfiguration(PrismObject<SystemConfigurationType> configuration, OperationResult result) {
		try {
			LoggingConfigurationType loggingWithProfiling = ProfilingConfigurationManager
					.checkSystemProfilingConfiguration(configuration);
			if (loggingWithProfiling != null) {
				LoggingConfigurationManager.configure(loggingWithProfiling, configuration.getVersion(), result);
			}
		} catch (Throwable t) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't apply logging configuration", t);
			lastVersionApplied = null;
		}
	}

	private void applyRemoteHostAddressHeadersConfiguration(SystemConfigurationType configurationBean) {
		try {
			SecurityUtil.setRemoteHostAddressHeaders(configurationBean);
		} catch (Throwable t) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't apply configuration of remote host address headers", t);
			lastVersionApplied = null;
		}
	}

	private void applyPolyStringNormalizerConfiguration(SystemConfigurationType configType) {
		try {
			PolyStringNormalizerConfigurationType normalizerConfig = null;
			InternalsConfigurationType internals = configType.getInternals();
			if (internals != null) {
				normalizerConfig = internals.getPolyStringNormalizer();
			}
			prismContext.configurePolyStringNormalizer(normalizerConfig);
			LOGGER.trace("Applied PolyString normalizer configuration {}", DebugUtil.shortDumpLazily(normalizerConfig));
		} catch (Throwable t) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't apply PolyString normalizer configuration", t);
			lastVersionApplied = null;
		}
	}

	private void applyFullTextSearchConfiguration(SystemConfigurationType configuration) {
		try {
			repositoryService.applyFullTextSearchConfiguration(configuration.getFullTextSearch());
		} catch (Throwable t) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't apply fulltext search configuration", t);
			lastVersionApplied = null;
		}
	}

	private void applyRelationsConfiguration(SystemConfigurationType configuration) {
		try {
			relationRegistry.applyRelationsConfiguration(configuration);
		} catch (Throwable t) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't apply relations configuration", t);
			lastVersionApplied = null;
		}
	}

	private void applyOperationResultHandlingConfiguration(SystemConfigurationType configuration) {
		try {
			SystemConfigurationTypeUtil.applyOperationResultHandling(configuration);
		} catch (Throwable t) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't apply operation result handling configuration", t);
			lastVersionApplied = null;
		}
	}
}
