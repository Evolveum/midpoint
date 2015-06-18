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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
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
@Component
public class ProvisioningContextFactory {

	@Autowired(required = true)
	private ResourceManager resourceManager;
	
	@Autowired(required = true)
	private ConnectorManager connectorManager;
	
	public ProvisioningContext create(PrismObject<ShadowType> shadow, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		ProvisioningContext ctx = new ProvisioningContext();
		ctx.setTask(task);
		ResourceType resource = getResource(shadow, parentResult);
		ctx.setResource(resource);
		RefinedResourceSchema refinedSchema = ProvisioningUtil.getRefinedSchema(resource);
		ctx.setRefinedSchema(refinedSchema);
		RefinedObjectClassDefinition objectClassDefinition = refinedSchema.determineCompositeObjectClassDefinition(shadow);
		ctx.setObjectClassDefinition(objectClassDefinition);
		ConnectorInstance connector = getConnectorInstance(resource, parentResult);
		ctx.setConnector(connector);
		return ctx;
	}
	
	public ProvisioningContext createAndAssertDefinition(PrismObject<ShadowType> shadow, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		ProvisioningContext ctx = create(shadow, task, parentResult);
		ctx.assertDefinition("Cannot locate object class definition for "+shadow+" in "+ctx.getResource());
		return ctx;
	}
	
	public ProvisioningContext create(ResourceShadowDiscriminator coords, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		ProvisioningContext ctx = new ProvisioningContext();
		ctx.setTask(task);
		ResourceType resource = getResource(coords.getResourceOid(), parentResult);
		ctx.setResource(resource);
		RefinedResourceSchema refinedSchema = ProvisioningUtil.getRefinedSchema(resource);
		ctx.setRefinedSchema(refinedSchema);
		RefinedObjectClassDefinition objectClassDefinition = refinedSchema.determineCompositeObjectClassDefinition(coords);
		ctx.setObjectClassDefinition(objectClassDefinition);
		ConnectorInstance connector = getConnectorInstance(resource, parentResult);
		ctx.setConnector(connector);
		return ctx;
	}
	
	public ProvisioningContext createAndAssertDefinition(ResourceShadowDiscriminator coords, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		ProvisioningContext ctx = create(coords, task, parentResult);
		ctx.assertDefinition("Cannot locate object class definition for "+coords+" in "+ctx.getResource());
		return ctx;
	}
	
	private ResourceType getResource(String resourceOid, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		return resourceManager.getResource(resourceOid, parentResult).asObjectable();
	}
	
	private ResourceType getResource(PrismObject<ShadowType> shadow, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		String resourceOid = ShadowUtil.getResourceOid(shadow.asObjectable());
		if (resourceOid == null) {
			throw new SchemaException("Shadow " + shadow + " does not have an resource OID");
		}
		return getResource(ShadowUtil.getResourceOid(shadow.asObjectable()), parentResult);
	}
	
	ConnectorInstance getConnectorInstance(ResourceType resource, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		OperationResult connectorResult = parentResult.createMinorSubresult(ShadowCache.class.getName() + ".getConnectorInstance");
		try {
			ConnectorInstance connector = connectorManager.getConfiguredConnectorInstance(resource.asPrismObject(), false, parentResult);
			connectorResult.recordSuccess();
			return connector;
		} catch (ObjectNotFoundException | SchemaException |  CommunicationException | ConfigurationException e){
			connectorResult.recordPartialError("Could not get connector instance. " + e.getMessage(),  e);
			throw e;
		}
	}
}
