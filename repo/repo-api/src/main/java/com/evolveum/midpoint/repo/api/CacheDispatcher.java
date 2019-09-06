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

package com.evolveum.midpoint.repo.api;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.UserSessionManagementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface CacheDispatcher {

	void registerCacheListener(CacheListener cacheListener);
	void unregisterCacheListener(CacheListener cacheListener);

	/**
	 * Dispatches "cache entry/entries invalidation" event to all relevant caches, even clusterwide if requested so.
	 * @param type Type of object(s) to be invalidated. Null means 'all types' (implies oid is null as well).
	 * @param oid Object(s) to be invalidated. Null means 'all objects of given type(s)'.
	 * @param clusterwide True if the event has to be distributed clusterwide.
	 * @param context Context of the invalidation request (optional).
	 */
	<O extends ObjectType> void dispatchInvalidation(@Nullable Class<O> type, @Nullable String oid, boolean clusterwide,
			@Nullable CacheInvalidationContext context);

}
