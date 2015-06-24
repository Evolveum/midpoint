/**
 * Copyright (c) 2015 Evolveum
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

import java.util.Collection;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
public class ProvisioningContext {
	
	private ResourceManager resourceManager;
	private ConnectorManager connectorManager;
	private OperationResult parentResult;
	
	private String resourceOid;
	private PrismObject<ShadowType> originalShadow;
	private ResourceShadowDiscriminator shadowCoordinates;
	private Collection<QName> additionalAuxiliaryObjectClassQNames;
	
	private RefinedObjectClassDefinition objectClassDefinition;
	private Task task;
	
	private ResourceType resource;
	private ConnectorInstance connector;
	private RefinedResourceSchema refinedSchema;
	
	public ProvisioningContext(ConnectorManager connectorManager, ResourceManager resourceManager, OperationResult parentResult) {
		super();
		this.connectorManager = connectorManager;
		this.resourceManager = resourceManager;
		this.parentResult = parentResult;
	}
	
	public String getResourceOid() {
		return resourceOid;
	}

	public void setResourceOid(String resourceOid) {
		this.resourceOid = resourceOid;
		this.resource = null;
		this.connector = null;
		this.refinedSchema = null;
	}
	
	public ResourceShadowDiscriminator getShadowCoordinates() {
		return shadowCoordinates;
	}
	
	public void setShadowCoordinates(ResourceShadowDiscriminator shadowCoordinates) {
		this.shadowCoordinates = shadowCoordinates;
	}

	public PrismObject<ShadowType> getOriginalShadow() {
		return originalShadow;
	}

	public void setOriginalShadow(PrismObject<ShadowType> originalShadow) {
		this.originalShadow = originalShadow;
	}
	
	public Collection<QName> getAdditionalAuxiliaryObjectClassQNames() {
		return additionalAuxiliaryObjectClassQNames;
	}

	public void setAdditionalAuxiliaryObjectClassQNames(Collection<QName> additionalAuxiliaryObjectClassQNames) {
		this.additionalAuxiliaryObjectClassQNames = additionalAuxiliaryObjectClassQNames;
	}

	public ResourceType getResource() throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		if (resource == null) {
			if (resourceOid == null) {
				throw new SchemaException("Null resource OID "+getDesc());
			}
			resource = resourceManager.getResource(resourceOid, parentResult).asObjectable();
		}
		return resource;
	}
	
	public RefinedResourceSchema getRefinedSchema() throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException {
		if (refinedSchema == null) {
			refinedSchema = ProvisioningUtil.getRefinedSchema(getResource());
		}
		return refinedSchema;
	}
	
	public RefinedObjectClassDefinition getObjectClassDefinition() throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException {
		if (objectClassDefinition == null) {
			if (originalShadow != null) {
				objectClassDefinition = getRefinedSchema().determineCompositeObjectClassDefinition(originalShadow, additionalAuxiliaryObjectClassQNames);
			} else if (shadowCoordinates != null && !shadowCoordinates.isWildcard()) {
				objectClassDefinition = getRefinedSchema().determineCompositeObjectClassDefinition(shadowCoordinates);
			}
		}
		return objectClassDefinition;
	}
	
	public Task getTask() {
		return task;
	}

	public void setTask(Task task) {
		this.task = task;
	}
	
	public String getChannel() {
		return task==null?null:task.getChannel();
	}

	public ConnectorInstance getConnector(OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		if (connector == null) {
			connector = getConnectorInstance(parentResult);
		}
		return connector;
	}
	
	public boolean isWildcard() {
		return (shadowCoordinates == null && originalShadow == null) || (shadowCoordinates != null && shadowCoordinates.isWildcard());
	}
	
	/**
	 * Creates a context for a different object class on the same resource.
	 */
	public ProvisioningContext spawn(ShadowKindType kind, String intent) {
		ProvisioningContext ctx = spawnSameResource();
		ctx.shadowCoordinates = new ResourceShadowDiscriminator(resourceOid, kind, intent);
		return ctx;
	}
		
	/**
	 * Creates a context for a different object class on the same resource.
	 */
	public ProvisioningContext spawn(QName objectClassQName) throws SchemaException {
		ProvisioningContext ctx = spawnSameResource();
		ctx.shadowCoordinates = new ResourceShadowDiscriminator(resourceOid, null, null);
		ctx.shadowCoordinates.setObjectClass(objectClassQName);
		return ctx;
	}
	
	/**
	 * Creates a context for a different object class on the same resource.
	 */
	public ProvisioningContext spawn(PrismObject<ShadowType> shadow) throws SchemaException {
		ProvisioningContext ctx = spawnSameResource();
		ctx.setOriginalShadow(shadow);
		return ctx;
	}
		
//	/**
//	 * Creates a context for a different object class on the same resource.
//	 */
//	public ProvisioningContext spawn(RefinedObjectClassDefinition objectClassDefinition) throws SchemaException {
//		ProvisioningContext ctx = spawnSameResource();
//		ctx.setObjectClassDefinition(objectClassDefinition);
//		return ctx;
//	}
	
	private ProvisioningContext spawnSameResource() {
		ProvisioningContext ctx = new ProvisioningContext(connectorManager, resourceManager, parentResult);
		ctx.task = this.task;
		ctx.resourceOid = this.resourceOid;
		ctx.resource = this.resource;
		ctx.connector = this.connector;
		ctx.refinedSchema = this.refinedSchema;
		return ctx;
	}

	public void assertDefinition(String message) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException {
		if (getObjectClassDefinition() == null) {
			throw new SchemaException(message + " " + getDesc());
		}
	}
	
	public void assertDefinition() throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException {
		assertDefinition("Cannot locate object class definition");
	}
		
	public String getDesc() {
		if (originalShadow != null) {
			return "for " + originalShadow + " in " + (resource==null?("resource "+resourceOid):resource);
		} else if (shadowCoordinates != null && !shadowCoordinates.isWildcard()) {
			return "for " + shadowCoordinates + " in " + (resource==null?("resource "+resourceOid):resource);
		} else {
			return "for wildcard in " + (resource==null?("resource "+resourceOid):resource);
		}
	}

	private ConnectorInstance getConnectorInstance(OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		OperationResult connectorResult = parentResult.createMinorSubresult(ProvisioningContext.class.getName() + ".getConnectorInstance");
		try {
			ConnectorInstance connector = connectorManager.getConfiguredConnectorInstance(getResource().asPrismObject(), false, parentResult);
			connectorResult.recordSuccess();
			return connector;
		} catch (ObjectNotFoundException | SchemaException |  CommunicationException | ConfigurationException e){
			connectorResult.recordPartialError("Could not get connector instance " + getDesc() + ": " +  e.getMessage(),  e);
			throw e;
		}
	}

	@Override
	public String toString() {
		return "ProvisioningContext("+getDesc()+")";
	}
	
}
