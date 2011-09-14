/*
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
package com.evolveum.midpoint.schema.util;

import org.w3c.dom.Element;

import com.evolveum.midpoint.schema.EnhancedResourceType;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;

/**
 * Methods that would belong to the ResourceType class but cannot go there
 * because of JAXB.
 * 
 * @author Radovan Semancik
 */
public class ResourceTypeUtil {
	
	public static String getConnectorOid(ResourceType resource) {
		if (resource.getConnectorRef() != null) {
			return resource.getConnectorRef().getOid();
		} else if (resource.getConnector() != null) {
			return resource.getConnector().getOid();
		} else {
			return null;
		}
	}

	/**
	 * The usage of "resolver" is experimental. Let's see if it will be practical ...
	 * @see ObjectResolver
	 */
	public static ConnectorType getConnectorType(ResourceType resource, ObjectResolver resolver) {
		if (resource.getConnector() != null) {
			return resource.getConnector();
		} else if (resource.getConnectorRef() != null) {
			String oid = resource.getConnectorRef().getOid();
			return (ConnectorType)resolver.resolve(oid);
		} else {
			return null;
		}
	}
	
	public static Element getResourceXsdSchema(ResourceType resource) {
		if (resource.getSchema() == null) {
			return null;
		}
		for (Element e : resource.getSchema().getAny()) {
			if (QNameUtil.compareQName(DOMUtil.XSD_SCHEMA_ELEMENT, e)) {
				return e;
			}
		}
		return null;
	}
	
	public static Schema getResourceSchema(ResourceType resource) throws SchemaException {
		Element resourceXsdSchema = getResourceXsdSchema(resource);
		if (resourceXsdSchema == null) {
			return null;
		}
		if (resource instanceof EnhancedResourceType) {
			EnhancedResourceType enh = (EnhancedResourceType) resource;
			if (enh.getParsedSchema() != null) {
				return enh.getParsedSchema();
			} else {
				Schema parsedSchema = Schema.parse(resourceXsdSchema);
				enh.setParsedSchema(parsedSchema);
				return parsedSchema;
			}
		}
		Schema parsedSchema = Schema.parse(resourceXsdSchema);
		return parsedSchema;
	}
	
}
