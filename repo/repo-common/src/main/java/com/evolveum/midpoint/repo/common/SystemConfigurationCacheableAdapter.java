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

package com.evolveum.midpoint.repo.common;

import com.evolveum.midpoint.repo.api.SystemConfigurationChangeApplier;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author mederly
 */
@Component
public class SystemConfigurationCacheableAdapter implements Cacheable {

	private static final Trace LOGGER = TraceManager.getTrace(SystemConfigurationCacheableAdapter.class);

	@Autowired private CacheRegistry cacheRegistry;
	@Autowired private SystemConfigurationChangeApplier changeApplier;

	@PostConstruct
	public void register() {
		cacheRegistry.registerCacheableService(this);
	}

	@Override
	public void clearCache() {
		try {
			OperationResult result = new OperationResult(SystemConfigurationCacheableAdapter.class.getName() + ".clearCache");
			changeApplier.applySystemConfiguration(true, true, result);
		} catch (Throwable t) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't apply updated system configuration", t);
		}
	}

}
