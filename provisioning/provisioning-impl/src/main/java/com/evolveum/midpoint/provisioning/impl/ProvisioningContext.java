/**
 * Copyright (c) 2015-2017 Evolveum
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

import com.evolveum.midpoint.common.refinery.*;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.StateReporter;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author semancik
 *
 */
public class ProvisioningContext extends StateReporter {

	private static final Trace LOGGER = TraceManager.getTrace(ProvisioningContext.class);
	
	private ResourceManager resourceManager;
	private OperationResult parentResult;
	private Collection<SelectorOptions<GetOperationOptions>> getOperationOptions;
	
	private PrismObject<ShadowType> originalShadow;
	private ResourceShadowDiscriminator shadowCoordinates;
	private Collection<QName> additionalAuxiliaryObjectClassQNames;
	private boolean useRefinedDefinition = true;
	
	private RefinedObjectClassDefinition objectClassDefinition;

	private ResourceType resource;
	private Map<Class<? extends CapabilityType>,ConnectorInstance> connectorMap;
	private RefinedResourceSchema refinedSchema;
	
	public ProvisioningContext(ResourceManager resourceManager, OperationResult parentResult) {
		this.resourceManager = resourceManager;
		this.parentResult = parentResult;
	}
	
	public void setResourceOid(String resourceOid) {
		super.setResourceOid(resourceOid);
		this.resource = null;
		this.connectorMap = null;
		this.refinedSchema = null;
	}
	
	public Collection<SelectorOptions<GetOperationOptions>> getGetOperationOptions() {
		return getOperationOptions;
	}

	public void setGetOperationOptions(Collection<SelectorOptions<GetOperationOptions>> getOperationOptions) {
		this.getOperationOptions = getOperationOptions;
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

	public boolean isUseRefinedDefinition() {
		return useRefinedDefinition;
	}

	public void setUseRefinedDefinition(boolean useRefinedDefinition) {
		this.useRefinedDefinition = useRefinedDefinition;
	}

	public ResourceType getResource() throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		if (resource == null) {
			if (getResourceOid() == null) {
				throw new SchemaException("Null resource OID "+getDesc());
			}
			GetOperationOptions options = GetOperationOptions.createReadOnly();
			resource = resourceManager.getResource(getResourceOid(), options, parentResult).asObjectable();
			updateResourceName();
		}
		return resource;
	}

	private void updateResourceName() {
		if (resource != null && resource.getName() != null) {
			super.setResourceName(resource.getName().getOrig());
		}
	}

	public RefinedResourceSchema getRefinedSchema() throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException {
		if (refinedSchema == null) {
			refinedSchema = ProvisioningUtil.getRefinedSchema(getResource());
		}
		return refinedSchema;
	}
	
	public RefinedObjectClassDefinition getObjectClassDefinition() throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException {
		if (objectClassDefinition == null) {
			if (useRefinedDefinition) {
				if (originalShadow != null) {
					objectClassDefinition = getRefinedSchema().determineCompositeObjectClassDefinition(originalShadow, additionalAuxiliaryObjectClassQNames);
				} else if (shadowCoordinates != null && !shadowCoordinates.isWildcard()) {
					objectClassDefinition = getRefinedSchema().determineCompositeObjectClassDefinition(shadowCoordinates);
				}
			} else {
				if (shadowCoordinates.getObjectClass() == null) {
					throw new IllegalStateException("No objectclass");
				}
				ObjectClassComplexTypeDefinition origObjectClassDefinition = getRefinedSchema().getOriginalResourceSchema().findObjectClassDefinition(shadowCoordinates.getObjectClass());
				if (origObjectClassDefinition == null) {
					throw new SchemaException("No object class definition for "+shadowCoordinates.getObjectClass()+" in original resource schema for "+getResource());
				} else {
					objectClassDefinition = RefinedObjectClassDefinitionImpl.parseFromSchema(origObjectClassDefinition, getResource(), getRefinedSchema(), getResource().asPrismObject().getPrismContext(),
						"objectclass "+origObjectClassDefinition+" in "+getResource());
				}
			}
		}
		return objectClassDefinition;
	}

	// we don't use additionalAuxiliaryObjectClassQNames as we don't know if they are initialized correctly [med] TODO: reconsider this
	public CompositeRefinedObjectClassDefinition computeCompositeObjectClassDefinition(@NotNull Collection<QName> auxObjectClassQNames)
			throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException {
		RefinedObjectClassDefinition structuralObjectClassDefinition = getObjectClassDefinition();
		if (structuralObjectClassDefinition == null) {
			return null;
		}
		Collection<RefinedObjectClassDefinition> auxiliaryObjectClassDefinitions = new ArrayList<>(auxObjectClassQNames.size());
		for (QName auxObjectClassQName : auxObjectClassQNames) {
			RefinedObjectClassDefinition auxObjectClassDef = refinedSchema.getRefinedDefinition(auxObjectClassQName);
			if (auxObjectClassDef == null) {
				throw new SchemaException("Auxiliary object class " + auxObjectClassQName + " specified in " + this + " does not exist");
			}
			auxiliaryObjectClassDefinitions.add(auxObjectClassDef);
		}
		return new CompositeRefinedObjectClassDefinitionImpl(structuralObjectClassDefinition, auxiliaryObjectClassDefinitions);
	}

	public RefinedObjectClassDefinition computeCompositeObjectClassDefinition(PrismObject<ShadowType> shadow)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
		return computeCompositeObjectClassDefinition(shadow.asObjectable().getAuxiliaryObjectClass());
	}

	public String getChannel() {
		return getTask()==null?null:getTask().getChannel();
	}
	
	public <T extends CapabilityType> ConnectorInstance getConnector(Class<T> capabilityClass, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		if (connectorMap == null) {
			connectorMap = new HashMap<>();
		}
		ConnectorInstance connector = connectorMap.get(capabilityClass);
		if (connector == null) {
			connector = getConnectorInstance(capabilityClass, parentResult);
			connectorMap.put(capabilityClass, connector);
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
		ctx.shadowCoordinates = new ResourceShadowDiscriminator(getResourceOid(), kind, intent);
		return ctx;
	}
		
	/**
	 * Creates a context for a different object class on the same resource.
	 */
	public ProvisioningContext spawn(QName objectClassQName) throws SchemaException {
		ProvisioningContext ctx = spawnSameResource();
		ctx.shadowCoordinates = new ResourceShadowDiscriminator(getResourceOid(), null, null);
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
		ProvisioningContext ctx = new ProvisioningContext(resourceManager, parentResult);
		ctx.setTask(this.getTask());
		ctx.setResourceOid(getResourceOid());
		ctx.resource = this.resource;
		ctx.updateResourceName();					// TODO eliminate this mess - check if we need StateReporter any more
		ctx.connectorMap = this.connectorMap;
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
			return "for " + originalShadow + " in " + (resource==null?("resource "+getResourceOid()):resource);
		} else if (shadowCoordinates != null && !shadowCoordinates.isWildcard()) {
			return "for " + shadowCoordinates + " in " + (resource==null?("resource "+getResourceOid()):resource);
		} else {
			return "for wildcard in " + (resource==null?("resource "+getResourceOid()):resource);
		}
	}

	private <T extends CapabilityType> ConnectorInstance getConnectorInstance(Class<T> capabilityClass, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		OperationResult connectorResult = parentResult.createMinorSubresult(ProvisioningContext.class.getName() + ".getConnectorInstance");
		try {
			ConnectorInstance connector = resourceManager.getConfiguredConnectorInstance(getResource().asPrismObject(), capabilityClass, false, parentResult);
			connectorResult.recordSuccess();
			return connector;
		} catch (ObjectNotFoundException | SchemaException e){
			connectorResult.recordPartialError("Could not get connector instance " + getDesc() + ": " +  e.getMessage(),  e);
			// Wrap those exceptions to a configuration exception. In the context of the provisioning operation we really cannot throw
			// ObjectNotFoundException exception. If we do that then the consistency code will interpret that as if the resource object
			// (shadow) is missing. But that's wrong. We do not have connector therefore we do not know anything about the shadow. We cannot
			// throw ObjectNotFoundException here.
			throw new ConfigurationException(e.getMessage(), e);
		} catch (CommunicationException | ConfigurationException | SystemException e){
			connectorResult.recordPartialError("Could not get connector instance " + getDesc() + ": " +  e.getMessage(),  e);
			throw e;
		}
	}
	
	@Override
	public String toString() {
		return "ProvisioningContext("+getDesc()+")";
	}

}
