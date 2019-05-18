/*
 * Copyright (c) 2010-2019 Evolveum
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

package com.evolveum.midpoint.repo.cache;

import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.cache.CacheType;
import com.evolveum.midpoint.util.caching.CacheConfiguration;
import com.evolveum.midpoint.util.caching.CacheConfiguration.CacheObjectTypeConfiguration;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.cache2k.expiry.Expiry;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 */
public abstract class AbstractGlobalCache {

	private static final Trace LOGGER = TraceManager.getTrace(AbstractGlobalCache.class);

	static final int DEFAULT_TIME_TO_LIVE = 60;                     // see also default-caching-profile.xml in resources

	@Autowired protected CacheConfigurationManager configurationManager;

	boolean supportsObjectType(Class<?> type) {
		CacheType cacheType = getCacheType();
		CacheConfiguration configuration = configurationManager.getConfiguration(cacheType);
		if (configuration != null) {
			return configuration.supportsObjectType(type);
		} else {
			LOGGER.warn("Global cache configuration for {} not found", cacheType);
			return false;
		}
	}

	protected CacheConfiguration getConfiguration() {
		return configurationManager.getConfiguration(getCacheType());
	}

	protected CacheObjectTypeConfiguration getConfiguration(Class<?> type) {
		CacheConfiguration configuration = getConfiguration();
		return configuration != null ? configuration.getForObjectType(type) : null;
	}

	long getCapacity() {
		CacheConfiguration configuration = configurationManager.getConfiguration(getCacheType());
		if (configuration == null) {
			return 0;
		} else if (configuration.getMaxSize() != null) {
			return configuration.getMaxSize();
		} else {
			return Long.MAX_VALUE;
		}
	}

	protected long getExpiryTime(Class<?> type) {
		CacheObjectTypeConfiguration configuration = getConfiguration(type);
		if (configuration == null) {
			return Expiry.NO_CACHE;
		} else if (configuration.getTimeToLive() != null) {
			return System.currentTimeMillis() + configuration.getTimeToLive() * 1000L;
		} else {
			return System.currentTimeMillis() + DEFAULT_TIME_TO_LIVE * 1000L;
		}
	}

	protected abstract CacheType getCacheType();

}
