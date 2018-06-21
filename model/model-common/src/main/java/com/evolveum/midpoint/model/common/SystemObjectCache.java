/**
 * Copyright (c) 2016 Evolveum
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
package com.evolveum.midpoint.model.common;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

/**
 * Cache for system object such as SystemConfigurationType. This is a global cache,
 * independent of the request. It will store the system configuration in memory.
 * It will check for system configuration updates in regular interval using the
 * getVersion() method.
 *
 * This supplements the RepositoryCache. RepositoryCache works on per-request
 * (per-operation) basis. The  SystemObjectCache is global. Its goal is to reduce
 * the number of getObject(SystemConfiguration) and the getVersion(SystemConfiguration)
 * calls.
 *
 * In the future: May be used for more objects that are often used and seldom
 * changed, e.g. object templates.
 *
 * TODO: use real repo instead of repo cache
 *
 * @author semancik
 */
@Component
public class SystemObjectCache {

	private static final Trace LOGGER = TraceManager.getTrace(SystemObjectCache.class);

	@Autowired
	@Qualifier("cacheRepositoryService")
	private transient RepositoryService cacheRepositoryService;

	private PrismObject<SystemConfigurationType> systemConfiguration;
	private Long systemConfigurationCheckTimestamp;

	private long getSystemConfigurationExpirationMillis() {
		return 1000;
	}

	public synchronized PrismObject<SystemConfigurationType> getSystemConfiguration(OperationResult result) throws SchemaException {
		try {
			if (!hasValidSystemConfiguration(result)) {
				LOGGER.trace("Cache MISS: reading system configuration from the repository: {}, version {}",
						systemConfiguration, systemConfiguration==null?null:systemConfiguration.getVersion());
				loadSystemConfiguration(result);
			} else {
				LOGGER.trace("Cache HIT: reusing cached system configuration: {}, version {}",
						systemConfiguration, systemConfiguration==null?null:systemConfiguration.getVersion());
			}
		} catch (ObjectNotFoundException e) {
			systemConfiguration = null;
			LOGGER.trace("Cache ERROR: System configuration not found", e);
			result.muteLastSubresultError();
		}
		return systemConfiguration;
	}

	private boolean hasValidSystemConfiguration(OperationResult result) throws ObjectNotFoundException, SchemaException {
		if (systemConfiguration == null) {
			return false;
		}
		if (systemConfiguration.getVersion() == null) {
			return false;
		}
		if (systemConfigurationCheckTimestamp == null) {
			return false;
		}
		if (System.currentTimeMillis() < systemConfigurationCheckTimestamp + getSystemConfigurationExpirationMillis()) {
			return true;
		}
		String repoVersion = cacheRepositoryService.getVersion(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(),
				result);
		if (systemConfiguration.getVersion().equals(repoVersion)) {
			systemConfigurationCheckTimestamp = System.currentTimeMillis();
			return true;
		}
		return false;
	}

	private void loadSystemConfiguration(OperationResult result) throws ObjectNotFoundException, SchemaException {
		Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createReadOnly());
		systemConfiguration = cacheRepositoryService.getObject(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(),
				options, result);
		systemConfigurationCheckTimestamp = System.currentTimeMillis();
		if (systemConfiguration != null && systemConfiguration.getVersion() == null) {
			LOGGER.warn("Retrieved system configuration with null version");
		}
	}

	public synchronized void invalidateCaches() {
		systemConfiguration = null;
	}
}
