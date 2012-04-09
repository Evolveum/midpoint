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
import java.util.Set;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ProtectedStringType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;

/**
 * Methods that would belong to the ResourceObjectShadowType class but cannot go there
 * because of JAXB.
 * 
 * @author Radovan Semancik
 */
public class ResourceObjectShadowUtil {
	
	public static Collection<ResourceAttribute<?>> getIdentifiers(ResourceObjectShadowType shadowType) {
		return getIdentifiers(shadowType.asPrismObject());
	}
	
	public static Collection<ResourceAttribute<?>> getIdentifiers(PrismObject<? extends ResourceObjectShadowType> shadow) {
		return getAttributesContainer(shadow).getIdentifiers();	
	}
	
	public static ResourceAttributeContainer getAttributesContainer(ResourceObjectShadowType shadowType) {
		return getAttributesContainer(shadowType.asPrismObject());
	}
	
	public static ResourceAttributeContainer getAttributesContainer(PrismObject<? extends ResourceObjectShadowType> shadow) {
		PrismContainer attributesContainer = shadow.findContainer(ResourceObjectShadowType.F_ATTRIBUTES);
		if (attributesContainer == null) {
			return null;
		}
		if (attributesContainer instanceof ResourceAttributeContainer) {
			return (ResourceAttributeContainer)attributesContainer;
		} else {
			throw new SystemException("Expected that <attributes> will be ResourceAttributeContainer but it is "+attributesContainer.getClass());
		}
	}
	
	public static ResourceAttributeContainerDefinition getObjectClassDefinition(ResourceObjectShadowType shadow) {
		// TODO: maybe we can do something more intelligent here
		ResourceAttributeContainer attributesContainer = getAttributesContainer(shadow);
		return attributesContainer.getDefinition();
	}

	
	public static String getResourceOid(ResourceObjectShadowType shadowType) {
		PrismObject<ResourceObjectShadowType> shadow = shadowType.asPrismObject();
		PrismReference resourceRef = shadow.findReference(ResourceObjectShadowType.F_RESOURCE_REF);
		if (resourceRef == null) {
			return null;
		}
		return resourceRef.getOid();
	}
	
	public static String getSingleStringAttributeValue(ResourceObjectShadowType shadow, QName attrName) {
		return getSingleStringAttributeValue(shadow.asPrismObject(), attrName);
	}
	

	private static String getSingleStringAttributeValue(PrismObject<ResourceObjectShadowType> shadow, QName attrName) {
		PrismContainer<?> attributesContainer = shadow.findContainer(ResourceObjectShadowType.F_ATTRIBUTES);
		if (attributesContainer == null) {
			return null;
		}
		PrismProperty<String> attribute = attributesContainer.findProperty(attrName);
		if (attribute == null) {
			return null;
		}
		return attribute.getRealValue();
	}

	public static List<Object> getAttributeValues(ResourceObjectShadowType shadowType, QName attrName) {
		return getAttributeValues(shadowType.asPrismObject(), attrName);
	}
	
	public static List<Object> getAttributeValues(PrismObject<ResourceObjectShadowType> shadow, QName attrName) {
		PrismContainer attributesContainer = shadow.findContainer(ResourceObjectShadowType.F_ATTRIBUTES);
		if (attributesContainer == null || attributesContainer.isEmpty()) {
			return null;
		}
		PrismProperty<?> attr = attributesContainer.findProperty(attrName);
		if (attr == null) {
			return null;
		}
		List<Object> values = new ArrayList<Object>();
		for (PrismPropertyValue<?> pval : attr.getValues()) {
			values.add(pval.getValue());
		}
		if (values.isEmpty()) {
			return null;
		}
		return values;
	}

	public static void setPassword(AccountShadowType accountShadowType, ProtectedStringType password) {
		CredentialsType credentialsType = accountShadowType.getCredentials();
		if (credentialsType == null) {
			credentialsType = new CredentialsType();
			accountShadowType.setCredentials(credentialsType);
		}
		PasswordType passwordType = credentialsType.getPassword();
		if (passwordType == null) {
			passwordType = new PasswordType();
			credentialsType.setPassword(passwordType);
		}
		passwordType.setProtectedString(password);
	}

	public static ActivationType getOrCreateActivation(ResourceObjectShadowType shadowType) {
		ActivationType activation = shadowType.getActivation();
		if (activation == null) {
			activation = new ActivationType();
			shadowType.setActivation(activation);
		}
		return activation;
	}
	
}
