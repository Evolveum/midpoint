/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.schema.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;







import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AvailabilityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CapabilitiesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CapabilityCollectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSynchronizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDependencyStrictnessType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDependencyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaHandlingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.XmlSchemaType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationLockoutStatusCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CreateCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.DeleteCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.UpdateCapabilityType;
import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;

/**
 * Methods that would belong to the ResourceType class but cannot go there
 * because of JAXB.
 * 
 * @author Radovan Semancik
 */
public class ResourceTypeUtil {

	public static String getConnectorOid(PrismObject<ResourceType> resource) {
		return getConnectorOid(resource.asObjectable());
	}
	
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
	 * The usage of "resolver" is experimental. Let's see if it will be
	 * practical ...
	 * 
	 * @see ObjectResolver
	 */
	public static ConnectorType getConnectorType(ResourceType resource, ObjectResolver resolver, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		if (resource.getConnector() != null) {
			return resource.getConnector();
		} else if (resource.getConnectorRef() != null) {
			return resolver.resolve(resource.getConnectorRef(), ConnectorType.class,
					null, "resolving connector in " + resource ,parentResult);
		} else {
			return null;
		}
	}

	public static Element getResourceXsdSchema(ResourceType resource) {
		XmlSchemaType xmlSchemaType = resource.getSchema();
		if (xmlSchemaType == null) {
			return null;
		}
		return ObjectTypeUtil.findXsdElement(xmlSchemaType);
	}
	
	public static Element getResourceXsdSchema(PrismObject<ResourceType> resource) {
		PrismContainer<XmlSchemaType> xmlSchema = resource.findContainer(ResourceType.F_SCHEMA);
		if (xmlSchema == null) {
			return null;
		}
		return ObjectTypeUtil.findXsdElement(xmlSchema);
	}
	
	public static void setResourceXsdSchema(ResourceType resourceType, Element xsdElement) {
		PrismObject<ResourceType> resource = resourceType.asPrismObject();
		setResourceXsdSchema(resource, xsdElement);
	}
	
	public static void setResourceXsdSchema(PrismObject<ResourceType> resource, Element xsdElement) {
		try {
			PrismContainer<XmlSchemaType> schemaContainer = resource.findOrCreateContainer(ResourceType.F_SCHEMA);
			PrismProperty<SchemaDefinitionType> definitionProperty = schemaContainer.findOrCreateProperty(XmlSchemaType.F_DEFINITION);
			ObjectTypeUtil.setXsdSchemaDefinition(definitionProperty, xsdElement);
		} catch (SchemaException e) {
			// Should not happen
			throw new IllegalStateException("Internal schema error: "+e.getMessage(),e);
		}
		
	}

	/**
	 * Returns collection of capabilities. Note: there is difference between empty set and null.
	 * Empty set means that we know the resource has no capabilities. Null means that the
	 * capabilities were not yet determined.
	 */
	public static Collection<Object> getNativeCapabilitiesCollection(ResourceType resource) {
		if (resource.getCapabilities() == null) {
			// No capabilities, not initialized
			return null;
		}
		CapabilityCollectionType nativeCap = resource.getCapabilities().getNative();
		if (nativeCap == null) {
			return null;
		}
		return nativeCap.getAny();
	}
	
	public static boolean hasSchemaGenerationConstraints(ResourceType resource){
		if (resource == null){
			return false;
		}
		
		if (resource.getSchema() == null){
			return false;
		}
		
		if (resource.getSchema().getGenerationConstraints() == null){
			return false;
		}
		
		List<QName> constainst = resource.getSchema().getGenerationConstraints().getGenerateObjectClass();
		
		if (constainst == null){
			return false;
		}
		
		return !constainst.isEmpty();
	}
	
	public static List<QName> getSchemaGenerationConstraints(ResourceType resource){
	
		if (hasSchemaGenerationConstraints(resource)){
			return resource.getSchema().getGenerationConstraints().getGenerateObjectClass();
		}
		return null;
	}

	public static List<QName> getSchemaGenerationConstraints(PrismObject<ResourceType> resource){
		if (resource == null){
			return null;
		}
		return getSchemaGenerationConstraints(resource.asObjectable());
	}

	/**
	 * Assumes that native capabilities are already cached. 
	 */
	public static <T extends CapabilityType> T getEffectiveCapability(ResourceType resource, Class<T> capabilityClass) {
		if (resource.getCapabilities() == null) {
			return null;
		}
		if (resource.getCapabilities().getConfigured() != null) {
			T configuredCapability = CapabilityUtil.getCapability(resource.getCapabilities().getConfigured().getAny(), capabilityClass);
			if (configuredCapability != null) {
				if (CapabilityUtil.isCapabilityEnabled(configuredCapability)) {
					return configuredCapability;
				} else {
					// Disabled capability, pretend that it is not there
					return null;
				}
			}
			// No configured capability entry, fallback to native capability
		}
		if (resource.getCapabilities().getNative() != null) {
			T nativeCapability = CapabilityUtil.getCapability(resource.getCapabilities().getNative().getAny(), capabilityClass);
			if (nativeCapability == null) {
				return null;
			}
			if (CapabilityUtil.isCapabilityEnabled(nativeCapability)) {
				return nativeCapability;
			} else {
				// Disabled capability, pretend that it is not there
				return null;
			}
		}
		return null;
	}
	
	public static <T extends CapabilityType> boolean hasEffectiveCapability(ResourceType resource, Class<T> capabilityClass) {
		return getEffectiveCapability(resource, capabilityClass) != null;
	}
	
	/**
	 * Assumes that native capabilities are already cached. 
	 */
	public static List<Object> getEffectiveCapabilities(ResourceType resource) throws SchemaException {
		if (resource.getCapabilities() == null) {
			return new ArrayList<Object>();
		}
		if (resource.getCapabilities().getConfigured() != null) {
			List<Object> effectiveCapabilities = new ArrayList<Object>();
			for (Object configuredCapability: resource.getCapabilities().getConfigured().getAny()) {
				if (CapabilityUtil.isCapabilityEnabled(configuredCapability)) {
					effectiveCapabilities.add(configuredCapability);
				}
			}
			if (resource.getCapabilities().getNative() != null) {
				for (Object nativeCapability: resource.getCapabilities().getNative().getAny()) {
					if (CapabilityUtil.isCapabilityEnabled(nativeCapability) && 
							!CapabilityUtil.containsCapabilityWithSameElementName(resource.getCapabilities().getConfigured().getAny(), nativeCapability)) {
						effectiveCapabilities.add(nativeCapability);
					}
				}
			}
			return effectiveCapabilities;
		} else if (resource.getCapabilities().getNative() != null) {
			return resource.getCapabilities().getNative().getAny();
		} else {
			return new ArrayList<Object>();
		}		
	}

	public static boolean hasActivationCapability(ResourceType resource) {
		return (getEffectiveCapability(resource, ActivationCapabilityType.class)!=null);
	}
	
	public static boolean hasCredentialsCapability(ResourceType resource) {
		return (getEffectiveCapability(resource, CredentialsCapabilityType.class)!=null);
	}
	
	public static boolean hasReadCapability(ResourceType resource){
		ReadCapabilityType readCap = getEffectiveCapability(resource, ReadCapabilityType.class);
		if (readCap == null){
			return false;
		}
		
		if (readCap.isEnabled() == null){
			return true;
		}
		
		return readCap.isEnabled();
	}
	
	public static boolean hasCreateCapability(ResourceType resource){
		CreateCapabilityType createCap = getEffectiveCapability(resource, CreateCapabilityType.class);
		if (createCap == null){
			return false;
		}
		
		if (createCap.isEnabled() == null){
			return true;
		}
		
		return createCap.isEnabled();
	}
	public static boolean hasUpdateCapability(ResourceType resource){
		UpdateCapabilityType updateCap = getEffectiveCapability(resource, UpdateCapabilityType.class);
		if (updateCap == null){
			return false;
		}
		
		if (updateCap.isEnabled() == null){
			return true;
		}
		
		return updateCap.isEnabled();
	}
	public static boolean hasDeleteCapability(ResourceType resource){
		DeleteCapabilityType deleteCap = getEffectiveCapability(resource, DeleteCapabilityType.class);
		if (deleteCap == null){
			return false;
		}
		
		if (deleteCap.isEnabled() == null){
			return true;
		}
		
		return deleteCap.isEnabled();
	}
	
	public static boolean hasResourceNativeActivationCapability(ResourceType resource) {
		ActivationCapabilityType activationCapability = null;
		// check resource native capabilities. if resource cannot do
		// activation, it sholud be null..
		if (resource.getCapabilities() != null && resource.getCapabilities().getNative() != null) {
			activationCapability = CapabilityUtil.getCapability(resource.getCapabilities().getNative().getAny(),
					ActivationCapabilityType.class);
		}
		if (activationCapability == null) {
			return false;
		}
		return true;
	}
	
	public static boolean hasResourceNativeActivationLockoutCapability(ResourceType resource) {
		ActivationCapabilityType activationCapability = null;
		// check resource native capabilities. if resource cannot do
		// activation, it sholud be null..
		if (resource.getCapabilities() != null && resource.getCapabilities().getNative() != null) {
			activationCapability = CapabilityUtil.getCapability(resource.getCapabilities().getNative().getAny(),
					ActivationCapabilityType.class);
		}
		if (activationCapability == null) {
			return false;
		}
		
		ActivationLockoutStatusCapabilityType lockoutStatus = activationCapability.getLockoutStatus();
		if (lockoutStatus == null) {
			return false;
		}
		
		return true;
	}
	
	public static boolean hasResourceConfiguredActivationCapability(ResourceType resource) {
		if (resource.getCapabilities() == null) {
			return false;
		}
		if (resource.getCapabilities().getConfigured() != null) {
			ActivationCapabilityType configuredCapability = CapabilityUtil.getCapability(resource.getCapabilities().getConfigured().getAny(), ActivationCapabilityType.class);
			if (configuredCapability != null) {
				return true;
			}
			// No configured capability entry, fallback to native capability
		}
		return false;
	}

	public static ResourceObjectTypeDefinitionType getResourceObjectTypeDefinitionType (
			ResourceType resource, ShadowKindType kind, String intent) {
		if (resource == null) {
			throw new IllegalArgumentException("The resource is null");
		}
		SchemaHandlingType schemaHandling = resource.getSchemaHandling();
        if (schemaHandling == null) {
            return null;
        }
        if (kind == null) {
        	kind = ShadowKindType.ACCOUNT;
        }
        for (ResourceObjectTypeDefinitionType objType: schemaHandling.getObjectType()) {
			if (objType.getKind() == kind || (objType.getKind() == null && kind == ShadowKindType.ACCOUNT)) {
				if (intent == null && objType.isDefault()) {
					return objType;
				}
				if (objType.getIntent() != null && objType.getIntent().equals(intent)) {
					return objType;
				}
				if (objType.getIntent() == null && objType.isDefault() && intent != null && intent.equals(SchemaConstants.INTENT_DEFAULT)) {
					return objType;
				}
			}
		}
		return null;
	}

	public static PrismContainer<Containerable> getConfigurationContainer(ResourceType resourceType) {
		return getConfigurationContainer(resourceType.asPrismObject());
	}
	
	public static PrismContainer<Containerable> getConfigurationContainer(PrismObject<ResourceType> resource) {
		return resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
	}
	
	public static String getResourceNamespace(PrismObject<ResourceType> resource) {
		return getResourceNamespace(resource.asObjectable());
	}
	
	public static String getResourceNamespace(ResourceType resourceType) {
		if (resourceType.getNamespace() != null) {
			return resourceType.getNamespace();
		}
		return MidPointConstants.NS_RI;
	}

	public static boolean isSynchronizationOpportunistic(ResourceType resourceType) {
		SynchronizationType synchronization = resourceType.getSynchronization();
		if (synchronization == null) {
			return false;
		}
		if (synchronization.getObjectSynchronization().isEmpty()) {
			return false;
		}
		ObjectSynchronizationType objectSynchronizationType = synchronization.getObjectSynchronization().iterator().next();
		if (objectSynchronizationType.isEnabled() != null && !objectSynchronizationType.isEnabled()) {
			return false;
		}
		Boolean isOpportunistic = objectSynchronizationType.isOpportunistic();
		return isOpportunistic == null || isOpportunistic;
	}

	public static int getDependencyOrder(ResourceObjectTypeDependencyType dependency) {
		if (dependency.getOrder() == 0) {
			return 0;
		} else {
			return dependency.getOrder();
		}
	}

	public static ResourceObjectTypeDependencyStrictnessType getDependencyStrictness(
			ResourceObjectTypeDependencyType dependency) {
		if (dependency.getStrictness() == null) {
			return ResourceObjectTypeDependencyStrictnessType.STRICT;
		} else {
			return dependency.getStrictness();
		}
	}
	
	public static boolean isDown(ResourceType resource){
		return (resource.getOperationalState() != null && AvailabilityStatusType.DOWN == resource.getOperationalState().getLastAvailabilityStatus());
	}

}
