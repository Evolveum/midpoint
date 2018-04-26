/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.provisioning.impl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * Class for caching ResourceType instances with a parsed schemas.
 *
 * @author Radovan Semancik
 *
 */
@Component
public class ResourceCache {

	private Map<String,PrismObject<ResourceType>> cache;

    ResourceCache() {
        cache = new HashMap<>();
    }

	public synchronized void put(PrismObject<ResourceType> resource) throws SchemaException {
		String oid = resource.getOid();
		if (oid == null) {
			throw new SchemaException("Attempt to cache "+resource+" without an OID");
		}

		String version = resource.getVersion();
		if (version == null) {
			throw new SchemaException("Attempt to cache "+resource+" without version");
		}

		PrismObject<ResourceType> cachedResource = cache.get(oid);
		if (cachedResource == null) {
			cache.put(oid, resource.createImmutableClone());
		} else {
			if (compareVersion(resource.getVersion(), cachedResource.getVersion())) {
				// We already have equivalent resource, nothing to do
			} else {
				cache.put(oid, resource.createImmutableClone());
			}
		}
	}

	private boolean compareVersion(String version1, String version2) {
		if (version1 == null && version2 == null) {
			return true;
		}
		if (version1 == null || version2 == null) {
			return false;
		}
		return version1.equals(version2);
	}

	public synchronized PrismObject<ResourceType> get(PrismObject<ResourceType> resource, GetOperationOptions options) throws SchemaException {
		return get(resource.getOid(), resource.getVersion(), options);
	}

	public synchronized PrismObject<ResourceType> get(String oid, String version, GetOperationOptions options) throws SchemaException {
		if (oid == null) {
			return null;
		}

		PrismObject<ResourceType> cachedResource = cache.get(oid);
		if (cachedResource == null) {
			return null;
		}

		if (!compareVersion(version, cachedResource.getVersion())) {
			return null;
		}

		if (GetOperationOptions.isReadOnly(options)) {
			try {
				cachedResource.checkImmutability();
			} catch (IllegalStateException ex) {
				// todo still need proper fix https://jira.evolveum.com/browse/MID-4574
				return null;
			}

			return cachedResource;
		} else {
			return cachedResource.clone();
		}
	}

	/**
	 * Returns currently cached version. FOR DIAGNOSTICS ONLY.
	 */
	public synchronized String getVersion(String oid) {
		if (oid == null) {
			return null;
		}
		PrismObject<ResourceType> cachedResource = cache.get(oid);
		if (cachedResource == null) {
			return null;
		}
		return cachedResource.getVersion();
	}

	public synchronized void remove(String oid) {
		cache.remove(oid);
	}

}
