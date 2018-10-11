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

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FunctionLibraryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

public interface CacheListener {

	
	
	<O extends ObjectType> void invalidateCache(Class<O> type, String oid);
	
	default <O extends ObjectType> boolean isSupportedToBeCleared(Class<O> type, String oid) {
		if (FunctionLibraryType.class.equals(type) || SystemConfigurationType.class.equals(type)) {
			return true;
		}
		
		// this is temporary hack, without checking also oid for blank, the whole caches are cleared
		// what we probably don't want. in this situation, clear cache only when no/blank oid is provided
		// it means, clear cache was called explicitly
		if (ConnectorType.class.equals(type) && StringUtils.isBlank(oid)) {
			return true;
		}
		
		return false;
	}
}
