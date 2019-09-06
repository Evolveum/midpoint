/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl;

import java.util.HashMap;
import java.util.Map;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
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

	private static final Trace LOGGER = TraceManager.getTrace(ResourceCache.class);

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
			LOGGER.debug("Caching(new): {}", resource);
			cache.put(oid, resource.createImmutableClone());
		} else {
			if (compareVersion(resource.getVersion(), cachedResource.getVersion())) {
				LOGGER.debug("Caching fizzle, resource already cached: {}", resource);
				// We already have equivalent resource, nothing to do
			} else {
				LOGGER.debug("Caching(replace): {}", resource);
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

	public synchronized PrismObject<ResourceType> get(String oid, String requestedVersion, GetOperationOptions options) throws SchemaException {
		if (oid == null) {
			return null;
		}

		PrismObject<ResourceType> cachedResource = cache.get(oid);
		if (cachedResource == null) {
			LOGGER.debug("MISS(not cached) for {}", oid);
			return null;
		}

		if (!compareVersion(requestedVersion, cachedResource.getVersion())) {
			LOGGER.debug("MISS(wrong version) for {}", oid);
			LOGGER.trace("Cached resource version {} does not match requested resource version {}, purging from cache", cachedResource.getVersion(), requestedVersion);
			cache.remove(oid);
			return null;
		}

		if (GetOperationOptions.isReadOnly(options)) {
			try {	// MID-4574
				cachedResource.checkImmutability();
			} catch (IllegalStateException ex) {
				LOGGER.error("Failed immutability test", ex);
				cache.remove(oid);

				return null;
			}
			LOGGER.trace("HIT(read only) for {}", cachedResource);
			return cachedResource;
		} else {
			LOGGER.debug("HIT(returning clone) for {}", cachedResource);
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
