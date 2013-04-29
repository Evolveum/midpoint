/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.provisioning.impl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CachingMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.XmlSchemaType;

/**
 * Class for caching ResourceType instances with a parsed schemas.
 * 
 * @author Radovan Semancik
 *
 */
@Component
public class ResourceCache {

	private Map<String,PrismObject<ResourceType>> cache;
    @Autowired(required = true)
	private PrismContext prismContext;

    ResourceCache() {
        cache = new HashMap<String, PrismObject<ResourceType>>();
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
			cache.put(oid, resource.clone());
		} else {
			if (compareVersion(resource.getVersion(), cachedResource.getVersion())) {
				// We already have equivalent resource, nothing to do
				return;
			} else {
				cache.put(oid, resource.clone());
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

	public synchronized PrismObject<ResourceType> get(PrismObject<ResourceType> resource) throws SchemaException {
		return get(resource.getOid(), resource.getVersion());
	}
	
	public synchronized PrismObject<ResourceType> get(String oid, String version) throws SchemaException {
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
		
		return cachedResource.clone();
	}

}
