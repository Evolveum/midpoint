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

import com.evolveum.midpoint.common.refinery.EnhancedResourceType;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.CachingMetadata;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.XmlSchemaType;

/**
 * Class for caching parsed instances of Resource Schema.
 * 
 * @author Radovan Semancik
 *
 */
@Component
public class ResourceSchemaCache {

	private Map<String,ResourceSchemaCacheEntry> cache;
    @Autowired(required = true)
	private PrismContext prismContext;

    ResourceSchemaCache() {
        cache = new HashMap<String, ResourceSchemaCache.ResourceSchemaCacheEntry>();
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
		
		if (cache.containsKey(oid)) {
			// There is already an entry in the cache. Check serial number
			ResourceSchemaCacheEntry entry = cache.get(oid);
			if (serial != null && !(serial.equals(entry.serialNumber))) {
				// The new object has different serial number.
				// Delete the old entry, there is obviously new version of schema
				cache.remove(oid);
			} else {
				// existing entry that seems to be still valid. Reuse parsed schema
				EnhancedResourceType enhResource = new EnhancedResourceType(resourceType);
				enhResource.setParsedSchema(entry.parsedSchema);
				return enhResource;
			}
		}
		
		// Cache entry does not exist (or was deleted). Parse the schema.
		Element xsdElement = ObjectTypeUtil.findXsdElement(xmlSchemaType);
		ResourceSchema parsedSchema = ResourceSchema.parse(xsdElement, prismContext);
		// Put it into cache
		ResourceSchemaCacheEntry entry = new ResourceSchemaCacheEntry(serial, xsdElement, parsedSchema);
		cache.put(oid,entry);
		
		EnhancedResourceType enhResource = new EnhancedResourceType(resourceType);
		enhResource.setParsedSchema(parsedSchema);
		
		return enhResource;
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
		CachingMetadata cachingMetadata = xmlSchemaType.getCachingMetadata();
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
		EnhancedResourceType enhResource = new EnhancedResourceType(resourceType);
		enhResource.setParsedSchema(entry.parsedSchema);
		
		return enhResource;
	}

	private class ResourceSchemaCacheEntry {
		public String serialNumber;
		public Element xsdElement;
		public ResourceSchema parsedSchema;
		
		private ResourceSchemaCacheEntry(String serialNumber, Element xsdElement, ResourceSchema parsedSchema) {
			super();
			this.serialNumber = serialNumber;
			this.xsdElement = xsdElement;
			this.parsedSchema = parsedSchema;
		}
	}
}
