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

import javax.xml.bind.JAXBElement;

import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.Schema;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.CapabilitiesType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceAccountTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SchemaHandlingType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_1.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_1.CredentialsCapabilityType;

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
			return (ConnectorType) resolver.resolve(resource.getConnectorRef(),
					"resolving connector in "+ObjectTypeUtil.toShortString(resource) ,parentResult);
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

	public static boolean compareConfiguration(ResourceConfigurationType a, ResourceConfigurationType b) {
		if (a == b) {
			// Short-cut. If they are the same instance they must be the same
			return true;
		}
		if (a == null && b == null) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		return JAXBUtil.compareAny(a.getAny(), b.getAny());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> T getCapability(Collection<Object> capabilities, Class<T> capabilityClass) {
		for (Object cap : capabilities) {
			if (cap instanceof JAXBElement) {
				JAXBElement jaxe = (JAXBElement) cap;
				if (capabilityClass.isAssignableFrom(jaxe.getDeclaredType())) {
					return (T) jaxe.getValue();
				}
			} else if (capabilityClass.isAssignableFrom(cap.getClass())) {
				return (T) cap;
			}
		}
		return null;
	}

	public static <T> T getEffectiveCapability(ResourceType resource, Class<T> capabilityClass) {
		if (resource.getCapabilities() != null) {
			return getCapability(resource.getCapabilities().getAny(), capabilityClass);
		} else if (resource.getNativeCapabilities() != null) {
			return getCapability(resource.getNativeCapabilities().getAny(), capabilityClass);
		} else {
			// No capabilities at all
			return null;
		}
	}

	public static List<Object> listEffectiveCapabilities(ResourceType resource) {
		if (resource.getCapabilities() != null) {
			return resource.getCapabilities().getAny();
		} else if (resource.getNativeCapabilities() != null) {
			return resource.getNativeCapabilities().getAny();
		} else {
			return new ArrayList<Object>();
		}
	}

	public static String getCapabilityDisplayName(Object capability) {
		// TODO: look for schema annotation
		String className = null;
		if (capability instanceof JAXBElement) {
			className = ((JAXBElement) capability).getDeclaredType().getSimpleName();
		} else {
			className = capability.getClass().getSimpleName();
		}
		if (className.endsWith("CapabilityType")) {
			return className.substring(0, className.length() - "CapabilityType".length());
		}
		return className;
	}

	public static CapabilitiesType getEffectiveCapabilities(ResourceType resource) {
		if (resource.getCapabilities() != null) {
			return resource.getCapabilities();
		}
		if (resource.getNativeCapabilities() != null) {
			return resource.getNativeCapabilities();
		}
		
		return new CapabilitiesType();
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
		if (resource.getNativeCapabilities() != null) {
			activationCapability = ResourceTypeUtil.getCapability(resource.getNativeCapabilities().getAny(),
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

}
