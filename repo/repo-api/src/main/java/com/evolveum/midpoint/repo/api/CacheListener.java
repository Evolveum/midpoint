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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public interface CacheListener {

	/**
	 * Invalidates given object(s) in all relevant caches.
	 * @param type Type of object (null means all types).
	 * @param oid OID of object (null means all object(s) of given type(s)).
	 * @param clusterwide Whether to distribute this event clusterwide.
	 * @param context Context of the invalidation request (optional).
	 */
	<O extends ObjectType> void invalidate(Class<O> type, String oid, boolean clusterwide, CacheInvalidationContext context);
}
