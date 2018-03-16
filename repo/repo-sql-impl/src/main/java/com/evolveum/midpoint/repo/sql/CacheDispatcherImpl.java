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

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.api.CacheDispatcher;
import com.evolveum.midpoint.repo.api.CacheListener;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

@Component(value="cacheDispatcher")
public class CacheDispatcherImpl implements CacheDispatcher {

	private static final Trace LOGGER = TraceManager.getTrace(CacheDispatcherImpl.class);
	
	private List<CacheListener> cacheListeners = new ArrayList<>();
	
	@Override
	public synchronized void registerCacheListener(CacheListener cacheListener) {
		if (cacheListeners.contains(cacheListener)) {
			LOGGER.warn("Registering listener {} which was already registered.", cacheListener);
			return;
		}
		cacheListeners.add(cacheListener);
	}
	
	@Override
	public synchronized void unregisterCacheListener(CacheListener cacheListener) {
		if (!cacheListeners.contains(cacheListener)) {
			LOGGER.warn("Unregistering listener {} which was already unregistered.", cacheListener);
			return;
		}
		cacheListeners.remove(cacheListener);
	}
	
	@Override
	public <O extends ObjectType> void dispatch(Class<O> type, String oid) { 
		for (CacheListener listenter : cacheListeners) {
			listenter.invalidateCache(type, oid);
		}
	}
}
