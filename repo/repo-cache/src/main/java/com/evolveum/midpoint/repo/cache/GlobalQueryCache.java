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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.cache.CacheType;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.cache2k.Cache2kBuilder;
import org.cache2k.expiry.ExpiryPolicy;
import org.cache2k.processor.EntryProcessor;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;

/**
 *
 */
@Component
public class GlobalQueryCache extends AbstractGlobalCache {

	private static final Trace LOGGER = TraceManager.getTrace(GlobalQueryCache.class);

	private static final String CACHE_NAME = "queryCache";

	private org.cache2k.Cache<QueryKey, SearchResultList> cache;

	public void initialize() {
		if (cache != null) {
			LOGGER.warn("Global query cache was already initialized -- ignoring this request.");
			return;
		}
		long capacity = getCapacity();
		if (capacity == 0) {
			LOGGER.warn("Capacity for " + getCacheType() + " is set to 0; this cache will be disabled (until system restart)");
			cache = null;
		} else {
			cache = new Cache2kBuilder<QueryKey, SearchResultList>() {}
					.name(CACHE_NAME)
					.entryCapacity(capacity)
					.expiryPolicy(getExpirePolicy())
					.build();
			LOGGER.info("Created global repository query cache with a capacity of {} queries", capacity);
		}
	}

	private ExpiryPolicy<QueryKey, SearchResultList> getExpirePolicy() {
		return (key, value, loadTime, oldEntry) -> getExpiryTime(key.getType());
	}

	@PreDestroy
	public void destroy() {
		if (cache != null) {
			cache.close();
			cache = null;
		}
	}

	public boolean isAvailable() {
		return cache != null;
	}

	public <T extends ObjectType> SearchResultList<PrismObject<T>> get(QueryKey key) {
		//noinspection unchecked
		return cache != null ? cache.peek(key) : null;
	}

	public void remove(QueryKey cacheKey) {
		if (cache != null) {
			cache.remove(cacheKey);
		}
	}

	public <T extends ObjectType> void put(QueryKey key, SearchResultList<PrismObject<T>> cacheObject) {
		if (cache != null) {
			cache.put(key, cacheObject);
		}
	}

	public void invokeAll(EntryProcessor<QueryKey, SearchResultList, Void> entryProcessor) {
		if (cache != null) {
			cache.invokeAll(cache.keys(), entryProcessor);
		}
	}

	@Override
	protected CacheType getCacheType() {
		return CacheType.GLOBAL_REPO_QUERY_CACHE;
	}

	public int size() {
		return cache.asMap().size();
	}
}
