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

import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

import com.evolveum.midpoint.schema.EnhancedResourceType;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
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
	
	private ResourceSchemaCache() {
		super();
		cache = new HashMap<String, ResourceSchemaCache.ResourceSchemaCacheEntry>();
	}
	
	public ResourceType put(ResourceType resourceType) throws SchemaException {
		if (resourceType.getSchema() == null) {
			// nothing to cache
			return resourceType;
		}
		if (resourceType.getOid() == null) {
			// no key to cache under
			return resourceType;
		}
		
		XmlSchemaType xmlSchemaType = resourceType.getSchema();
		Element xsdElement = ObjectTypeUtil.findXsdElement(xmlSchemaType);
		Schema parsedSchema = Schema.parse(xsdElement);
		String serial = null;
		if (xmlSchemaType.getCachingMetadata()!=null) {
			serial = xmlSchemaType.getCachingMetadata().getSerialNumber();
		}
		ResourceSchemaCacheEntry entry = new ResourceSchemaCacheEntry(serial, xsdElement, parsedSchema);
		cache.put(resourceType.getOid(),entry);
		
		EnhancedResourceType enhResource = new EnhancedResourceType(resourceType);
		enhResource.setParsedSchema(parsedSchema);
		
		return enhResource;
	}

	private class ResourceSchemaCacheEntry {
		public String serialNumber;
		public Element xsdElement;
		public Schema parsedSchema;
		
		private ResourceSchemaCacheEntry(String serialNumber, Element xsdElement, Schema parsedSchema) {
			super();
			this.serialNumber = serialNumber;
			this.xsdElement = xsdElement;
			this.parsedSchema = parsedSchema;
		}
	}
}
