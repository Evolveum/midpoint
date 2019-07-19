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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.api.Cacheable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachesStateInformationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.api.CacheDispatcher;
import com.evolveum.midpoint.repo.api.CacheListener;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

@Component
public class CacheRegistry implements CacheListener {
	
	private static transient Trace LOGGER = TraceManager.getTrace(CacheListener.class);
	
	private List<Cacheable> cacheableServices = new ArrayList<>();

	@Autowired private CacheDispatcher dispatcher;
	@Autowired private PrismContext prismContext;
	
	@PostConstruct
	public void registerListener() {
		dispatcher.registerCacheListener(this);
	}

	@PreDestroy
	public void unregisterListener() {
		dispatcher.unregisterCacheListener(this);
	}
	
	public synchronized void registerCacheableService(Cacheable cacheableService) {
		if (!cacheableServices.contains(cacheableService)) {
			cacheableServices.add(cacheableService);
		}
	}

	public synchronized void unregisterCacheableService(Cacheable cacheableService) {
		cacheableServices.remove(cacheableService);
	}
	
	public List<Cacheable> getCacheableServices() {
		return cacheableServices;
	}
	
	@Override
	public <O extends ObjectType> void invalidate(Class<O> type, String oid, boolean clusterwide,
			CacheInvalidationContext context) {
		// We currently ignore clusterwide parameter, because it's used by ClusterCacheListener only.
		// So we assume that the invalidation event - from this point on - is propagated only locally.
		for (Cacheable cacheableService : cacheableServices) {
			cacheableService.invalidate(type, oid, context);
		}
	}

	public CachesStateInformationType getStateInformation() {
		CachesStateInformationType rv = new CachesStateInformationType(prismContext);
		cacheableServices.forEach(cacheable -> rv.getEntry().addAll(cacheable.getStateInformation()));
		return rv;
	}
}

