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

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class CacheRegistry {

	private List<Cacheable> cacheableServices = new ArrayList<>();
	
	public void registerCacheableService(Cacheable cacheableService) {
		cacheableServices.add(cacheableService);
	}
	
	public List<Cacheable> getCacheableServices() {
		return cacheableServices;
	}
	
	public void clearAllCaches() {
		for (Cacheable cacheableService : cacheableServices) {
			cacheableService.clearCache();
		}
	}
	
	public void notifyListeners(Class type, String oid) {
		for (Cacheable cacheable : cacheableServices) {
			cacheable.notify();
		}
	}
}

