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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CapabilitiesType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CapabilityCollectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectSynchronizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceAccountTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SchemaHandlingType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SynchronizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.XmlSchemaType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.CapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.CredentialsCapabilityType;

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
					"resolving connector in " + resource ,parentResult);
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
			PrismProperty<Element> definitionProperty = schemaContainer.findOrCreateProperty(XmlSchemaType.F_DEFINITION);
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

	public static ResourceAccountTypeDefinitionType getResourceAccountTypeDefinitionType(
			ResourceType resource, String accountType) {
		if (resource == null) {
			throw new IllegalArgumentException("The resource is null");
		}
		SchemaHandlingType schemaHandling = resource.getSchemaHandling();
        if (schemaHandling == null || schemaHandling.getAccountType() == null) {
            return null;
        }
		for (ResourceAccountTypeDefinitionType acct: schemaHandling.getAccountType()) {
			if (accountType == null && acct.isDefault()) {
				return acct;
			}
			if (acct.getName().equals(accountType)) {
				return acct;
			}
		}
		return null;
	}

	/**
	 * Returns appropriate object synchronization settings for the class.
	 * Assumes single sync setting for now.
	 */
	public static ObjectSynchronizationType determineSynchronization(ResourceType resource, Class<UserType> type) {
		SynchronizationType synchronization = resource.getSynchronization();
		if (synchronization == null) {
			return null;
		}
		List<ObjectSynchronizationType> objectSynchronizations = synchronization.getObjectSynchronization();
		if (objectSynchronizations.isEmpty()) {
			return null;
		}
		if (objectSynchronizations.size() == 1) {
			return objectSynchronizations.get(0);
		}
		throw new UnsupportedOperationException("Selecting from multiple synchronization settings is not yet supported");
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

}
