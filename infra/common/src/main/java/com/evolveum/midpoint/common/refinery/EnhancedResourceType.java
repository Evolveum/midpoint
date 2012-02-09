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
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.common.refinery;

import com.evolveum.midpoint.prism.Schema;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;

/**
 * @author Radovan Semancik
 * 
 */
public class EnhancedResourceType extends ResourceType {

	private static final long serialVersionUID = -3858765689306267363L;
	private Schema parsedSchema;
	private RefinedResourceSchema refinedSchema;

	public EnhancedResourceType() {
		super();
	}

	public EnhancedResourceType(ResourceType resourceType) {
		setConfiguration(resourceType.getConfiguration());
		setConnector(resourceType.getConnector());
		setConnectorRef(resourceType.getConnectorRef());
		setDescription(resourceType.getDescription());
		setExtension(resourceType.getExtension());
		setName(resourceType.getName());
		setNamespace(resourceType.getNamespace());
		setOid(resourceType.getOid());
		setSchema(resourceType.getSchema());
		setSchemaHandling(resourceType.getSchemaHandling());
		setNativeCapabilities(resourceType.getNativeCapabilities());
		setCapabilities(resourceType.getCapabilities());
		setScripts(resourceType.getScripts());
		setSynchronization(resourceType.getSynchronization());
		setVersion(resourceType.getVersion());
	}

	public Schema getParsedSchema() {
		return parsedSchema;
	}

	public void setParsedSchema(Schema parsedSchema) {
		this.parsedSchema = parsedSchema;
	}

	public RefinedResourceSchema getRefinedSchema() {
		return refinedSchema;
	}

	public void setRefinedSchema(RefinedResourceSchema refinedSchema) {
		this.refinedSchema = refinedSchema;
	}
	
}
