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
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CachingMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.XmlSchemaType;

/**
 * Class for caching parsed instances of Resource Schema.
 * 
 * @author Radovan Semancik
 *
 */
@Component
public class ResourceCache {

	private Map<String,ResourceSchemaCacheEntry> cache;
    @Autowired(required = true)
	private PrismContext prismContext;

    ResourceCache() {
        cache = new HashMap<String, ResourceCache.ResourceSchemaCacheEntry>();
    }
	
	public synchronized ResourceType put(ResourceType resourceType) throws SchemaException {
		if (resourceType.getSchema() == null) {
			// nothing to cache
			return resourceType;
		}
		String oid = resourceType.getOid();
		if (oid == null) {
			// no key to cache under
			return resourceType;
		}
		
		XmlSchemaType xmlSchemaType = resourceType.getSchema();
		String serial = null;
		if (xmlSchemaType.getCachingMetadata()!=null) {
			serial = xmlSchemaType.getCachingMetadata().getSerialNumber();
		}
		
		if (serial == null) {
			// No caching metadata or no serial number. We can't do anything with cache
			// in this case. Just return the original resource.
			return resourceType;
		}
		
		if (cache.containsKey(oid)) {
			// There is already an entry in the cache. Check serial number
			ResourceSchemaCacheEntry entry = cache.get(oid);
			if (serial.equals(entry.serialNumber) && compareVersion(resourceType.getVersion(), entry.version)) {
				// Same serial number and version. We have a hit
				RefinedResourceSchema.setParsedResourceSchemaConditional(resourceType, entry.parsedSchema);
				return resourceType;
			} else {
				// The new object has different serial number.
				// Delete the old entry, there is obviously new version of schema
				cache.remove(oid);				
			}
		}
		
		// Cache entry does not exist (or was deleted). Parse the schema.
		Element xsdElement = ResourceTypeUtil.getResourceXsdSchema(resourceType);
		if (xsdElement == null) {
			throw new IllegalArgumentException("No resource schema in " + resourceType + ", cannot put in cache");
		}
		ResourceSchema parsedSchema = ResourceSchema.parse(xsdElement, resourceType.toString(), prismContext);
		// Put it into cache
		ResourceSchemaCacheEntry entry = new ResourceSchemaCacheEntry(serial, resourceType.getVersion(), xsdElement, parsedSchema);
		cache.put(oid,entry);
		
		RefinedResourceSchema.setParsedResourceSchemaConditional(resourceType,parsedSchema);
		return resourceType;
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

	public synchronized ResourceType get(ResourceType resourceType) throws SchemaException {
		if (resourceType.getSchema() == null) {
			// nothing to cache
			return resourceType;
		}
		String oid = resourceType.getOid();
		if (oid == null) {
			// no key to cache under
			return resourceType;
		}
		XmlSchemaType xmlSchemaType = resourceType.getSchema();
		if (xmlSchemaType == null) {
			// No schema, nothing to cache
			return resourceType;
		}
		
		ResourceSchemaCacheEntry entry = cache.get(oid);
		if (entry == null) {
			// No cache entry. So let's create it
			return put(resourceType);
		}
				
		// Check metadata to see if we can reuse what is in the cache
		CachingMetadataType cachingMetadata = xmlSchemaType.getCachingMetadata();
		if (cachingMetadata != null) {			
			if (cachingMetadata.getSerialNumber()!=null) {
				if (!cachingMetadata.getSerialNumber().equals(entry.serialNumber)) {
					// cache entry is not usable. serial number mismatch
					// therefore add the new entry to cache
					return put(resourceType);
				}
			}
		} // No metadata or serial number - we can reuse cache forever

		// We have entry that matches. Create appropriate resource
		RefinedResourceSchema.setParsedResourceSchemaConditional(resourceType, entry.parsedSchema);
		return resourceType;
	}

	private class ResourceSchemaCacheEntry {
		public String serialNumber;
		public String version;
		public Element xsdElement;
		public ResourceSchema parsedSchema;
		
		private ResourceSchemaCacheEntry(String serialNumber, String version, Element xsdElement, ResourceSchema parsedSchema) {
			super();
			this.serialNumber = serialNumber;
			this.version = version;
			this.xsdElement = xsdElement;
			this.parsedSchema = parsedSchema;
		}
	}
}
