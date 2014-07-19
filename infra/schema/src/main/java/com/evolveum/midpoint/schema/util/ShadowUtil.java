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

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Methods that would belong to the ResourceObjectShadowType class but cannot go there
 * because of JAXB.
 * 
 * @author Radovan Semancik
 */
public class ShadowUtil {
	
	public static Collection<ResourceAttribute<?>> getIdentifiers(ShadowType shadowType) {
		return getIdentifiers(shadowType.asPrismObject());
	}
	
	public static Collection<ResourceAttribute<?>> getIdentifiers(PrismObject<? extends ShadowType> shadow) {
		ResourceAttributeContainer attributesContainer = getAttributesContainer(shadow);
		if (attributesContainer == null) {
			return null;
		}
		return attributesContainer.getIdentifiers();	
	}
	
	public static Collection<ResourceAttribute<?>> getSecondaryIdentifiers(ShadowType shadowType) {
		return getSecondaryIdentifiers(shadowType.asPrismObject());
	}
	
	public static Collection<ResourceAttribute<?>> getSecondaryIdentifiers(PrismObject<? extends ShadowType> shadow) {
		ResourceAttributeContainer attributesContainer = getAttributesContainer(shadow);
		if (attributesContainer == null) {
			return null;
		}
		return attributesContainer.getSecondaryIdentifiers();	
	}
	
	public static Collection<ResourceAttribute<?>> getAttributes(ShadowType shadowType) {
		return getAttributes(shadowType.asPrismObject());
	}
	
	public static Collection<ResourceAttribute<?>> getAttributes(PrismObject<? extends ShadowType> shadow) {
		return getAttributesContainer(shadow).getAttributes();	
	}
	
	public static <T> ResourceAttribute<T> getAttribute(PrismObject<? extends ShadowType> shadow, QName attrName) {
		return getAttributesContainer(shadow).findAttribute(attrName);	
	}
	
	public static ResourceAttributeContainer getAttributesContainer(ShadowType shadowType) {
		return getAttributesContainer(shadowType.asPrismObject());
	}
	
	public static ResourceAttributeContainer getAttributesContainer(PrismObject<? extends ShadowType> shadow) {
		return getAttributesContainer(shadow, ShadowType.F_ATTRIBUTES);
	}
	
	public static ResourceAttributeContainer getAttributesContainer(PrismObject<? extends ShadowType> shadow, QName containerName) {
		return getAttributesContainer(shadow.getValue(), containerName);
	}
	
	public static ResourceAttributeContainer getAttributesContainer(PrismContainerValue<?> cval, QName containerName) {
		PrismContainer attributesContainer = cval.findContainer(containerName);
		if (attributesContainer == null) {
			return null;
		}
		if (attributesContainer instanceof ResourceAttributeContainer) {
			return (ResourceAttributeContainer)attributesContainer;
		} else {
			throw new SystemException("Expected that <"+containerName.getLocalPart()+"> will be ResourceAttributeContainer but it is "+attributesContainer.getClass());
		}
	}
	
	public static ResourceAttributeContainer getOrCreateAttributesContainer(PrismObject<? extends ShadowType> shadow, 
			ObjectClassComplexTypeDefinition objectClassDefinition) {
		ResourceAttributeContainer attributesContainer = getAttributesContainer(shadow);
		if (attributesContainer != null) {
			return attributesContainer;
		}
		ResourceAttributeContainer emptyContainer = ResourceAttributeContainer.createEmptyContainer(ShadowType.F_ATTRIBUTES, objectClassDefinition);
		try {
			shadow.add(emptyContainer);
		} catch (SchemaException e) {
			throw new SystemException("Unexpected schema error: "+e.getMessage(), e);
		}
		return emptyContainer;
	}
	
	public static ObjectClassComplexTypeDefinition getObjectClassDefinition(ShadowType shadow) {
		// TODO: maybe we can do something more intelligent here
		ResourceAttributeContainer attributesContainer = getAttributesContainer(shadow);
		return attributesContainer.getDefinition().getComplexTypeDefinition();
	}
	
	public static ObjectClassComplexTypeDefinition getObjectClassDefinition(PrismObject<? extends ShadowType> shadow) {
		// TODO: maybe we can do something more intelligent here
		ResourceAttributeContainer attributesContainer = getAttributesContainer(shadow);
		return attributesContainer.getDefinition().getComplexTypeDefinition();
	}

	
	public static String getResourceOid(ShadowType shadowType) {
		return getResourceOid(shadowType.asPrismObject());
	}
	
	public static String getResourceOid(PrismObject<ShadowType> shadow) {
		PrismReference resourceRef = shadow.findReference(ShadowType.F_RESOURCE_REF);
		if (resourceRef == null) {
			return null;
		}
		return resourceRef.getOid();
	}
	
	public static String getSingleStringAttributeValue(ShadowType shadow, QName attrName) {
		return getSingleStringAttributeValue(shadow.asPrismObject(), attrName);
	}
	

	private static String getSingleStringAttributeValue(PrismObject<ShadowType> shadow, QName attrName) {
		PrismContainer<?> attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
		if (attributesContainer == null) {
			return null;
		}
		PrismProperty<String> attribute = attributesContainer.findProperty(attrName);
		if (attribute == null) {
			return null;
		}
		return attribute.getRealValue();
	}
	
	public static String getMultiStringAttributeValueAsSingle(ShadowType shadow, QName attrName) {
		return getMultiStringAttributeValueAsSingle(shadow.asPrismObject(), attrName);
	}
	

	private static String getMultiStringAttributeValueAsSingle(PrismObject<ShadowType> shadow, QName attrName) {
		PrismContainer<?> attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
		if (attributesContainer == null) {
			return null;
		}
		PrismProperty<String> attribute = attributesContainer.findProperty(attrName);
		if (attribute == null) {
			return null;
		}
		Collection<String> realValues = attribute.getRealValues();
		if (realValues == null || realValues.isEmpty()) {
			return null;
		}
		if (realValues.size() > 1) {
			throw new IllegalStateException("More than one value in attribute "+attrName);
		}
		return realValues.iterator().next();
	}

	public static <T> List<T> getAttributeValues(ShadowType shadowType, QName attrName) {
		return getAttributeValues(shadowType.asPrismObject(), attrName);
	}
	
	public static <T> List<T> getAttributeValues(PrismObject<? extends ShadowType> shadow, QName attrName) {
		PrismContainer<?> attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
		if (attributesContainer == null || attributesContainer.isEmpty()) {
			return null;
		}
		PrismProperty<T> attr = attributesContainer.findProperty(attrName);
		if (attr == null) {
			return null;
		}
		List<T> values = new ArrayList<T>();
		for (PrismPropertyValue<T> pval : attr.getValues()) {
			values.add(pval.getValue());
		}
		if (values.isEmpty()) {
			return null;
		}
		return values;
	}
	
	public static <T> T getAttributeValue(ShadowType shadowType, QName attrName) throws SchemaException {
		return (T) getAttributeValue(shadowType.asPrismObject(), attrName);
	}
	
	public static <T> T getAttributeValue(PrismObject<? extends ShadowType> shadow, QName attrName) throws SchemaException {
		Collection<T> values = getAttributeValues(shadow, attrName);
		if (values == null || values.isEmpty()) {
			return null;
		}
		if (values.size() > 1) {
			throw new SchemaException("Attempt to get single value from multi-valued attribute "+attrName);
		}
		return values.iterator().next();
	}

	public static void setPassword(ShadowType shadowType, ProtectedStringType password) {
		CredentialsType credentialsType = shadowType.getCredentials();
		if (credentialsType == null) {
			credentialsType = new CredentialsType();
			shadowType.setCredentials(credentialsType);
		}
		PasswordType passwordType = credentialsType.getPassword();
		if (passwordType == null) {
			passwordType = new PasswordType();
			credentialsType.setPassword(passwordType);
		}
		passwordType.setValue(password);
	}

	public static ActivationType getOrCreateActivation(ShadowType shadowType) {
		ActivationType activation = shadowType.getActivation();
		if (activation == null) {
			activation = new ActivationType();
			shadowType.setActivation(activation);
		}
		return activation;
	}
	
    /**
     * This is not supposed to be used in production code! It is just for the tests.
     */
	public static void applyResourceSchema(PrismObject<? extends ShadowType> shadow,
			ResourceSchema resourceSchema) throws SchemaException {
		ShadowType shadowType = shadow.asObjectable();
		QName objectClass = shadowType.getObjectClass();
    	ObjectClassComplexTypeDefinition objectClassDefinition = resourceSchema.findObjectClassDefinition(objectClass);
    	applyObjectClass(shadow, objectClassDefinition);
	}
	
	private static void applyObjectClass(PrismObject<? extends ShadowType> shadow, 
			ObjectClassComplexTypeDefinition objectClassDefinition) throws SchemaException {
		PrismContainer<?> attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
		ResourceAttributeContainerDefinition racDef = new ResourceAttributeContainerDefinition(ShadowType.F_ATTRIBUTES,
				objectClassDefinition, objectClassDefinition.getPrismContext());
		attributesContainer.applyDefinition(racDef, true);
	}
	
	public static PrismObjectDefinition<ShadowType> applyObjectClass(PrismObjectDefinition<ShadowType> shadowDefinition, 
			ObjectClassComplexTypeDefinition objectClassDefinition) throws SchemaException {
		PrismObjectDefinition<ShadowType> shadowDefClone = shadowDefinition.cloneWithReplacedDefinition(ShadowType.F_ATTRIBUTES,
				objectClassDefinition.toResourceAttributeContainerDefinition());
		return shadowDefClone;
	}

	/**
	 * Returns intent from the shadow. Backwards compatible with older accountType. May also adjust for default
	 * intent if necessary.
	 */
	public static String getIntent(ShadowType shadow) {
		if (shadow == null) {
			return null;
		}
		String intent = shadow.getIntent();
		if (intent != null) {
			return intent;
		}
		return null;
	}
	
	public static ShadowKindType getKind(ShadowType shadow) {
		if (shadow == null) {
			return null;
		}
		ShadowKindType kind = shadow.getKind();
		if (kind != null) {
			return kind;
		}
		return ShadowKindType.ACCOUNT;
	}


	public static <T> Collection<T> getAttributeValues(ShadowType shadow, QName attributeQname, Class<T> type) {
		ResourceAttributeContainer attributesContainer = getAttributesContainer(shadow);
		if (attributesContainer == null) {
			return null;
		}
		ResourceAttribute<T> attribute = attributesContainer.findAttribute(attributeQname);
		if (attribute == null) {
			return null;
		}
		return attribute.getRealValues(type);
	}

	public static void checkConsistence(PrismObject<? extends ShadowType> shadow, String desc) {
		PrismReference resourceRef = shadow.findReference(ShadowType.F_RESOURCE_REF);
    	if (resourceRef == null) {
    		throw new IllegalStateException("No resourceRef in "+desc);
    	}
    	if (StringUtils.isBlank(resourceRef.getOid())) {
    		throw new IllegalStateException("Null or empty OID in resourceRef in "+desc);
    	}
		ShadowType shadowType = shadow.asObjectable();
    	if (shadowType.getObjectClass() == null) {
    		throw new IllegalStateException("Null objectClass in "+desc);
    	}
    	PrismContainer<ShadowAttributesType> attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
    	if (attributesContainer != null) {
    		if (!(attributesContainer instanceof ResourceAttributeContainer)) {
    			throw new IllegalStateException("The attributes element expected to be ResourceAttributeContainer but it is "
    					+attributesContainer.getClass()+" instead in "+desc);
    		}
    		checkConsistency(attributesContainer.getDefinition(), " container definition in "+desc); 
    	}
    	
    	PrismContainerDefinition<ShadowAttributesType> attributesDefinition = 
    			shadow.getDefinition().findContainerDefinition(ShadowType.F_ATTRIBUTES);
    	checkConsistency(attributesDefinition, " object definition in "+desc);
	}
	
	public static void checkConsistency(PrismContainerDefinition<ShadowAttributesType> attributesDefinition, String desc) {
		if (attributesDefinition == null) {
    		throw new IllegalStateException("No definition for <attributes> in "+desc);
    	}
    	if (!(attributesDefinition instanceof ResourceAttributeContainerDefinition)) {
    		throw new IllegalStateException("The attributes element definition expected to be ResourceAttributeContainerDefinition but it is "
					+attributesDefinition.getClass()+" instead in "+desc);
    	}
	}

    // TODO is this correct?
    public static boolean isAccount(ShadowType shadowType) {
        if (shadowType.getKind() != null) {
            return shadowType.getKind() == ShadowKindType.ACCOUNT;
        } else {
            return true;        // ???
        }
    }
    
    public static boolean isProtected(PrismObject<? extends ShadowType> shadow) {
    	ShadowType shadowType = shadow.asObjectable();
    	Boolean protectedObject = shadowType.isProtectedObject();
    	return (protectedObject != null && protectedObject);
    }
    
    public static boolean isDead(ShadowType shadow){
    	return shadow.isDead() != null && shadow.isDead();
    }

	public static boolean matches(ShadowType shadowType, String resourceOid, ShadowKindType kind, String intent) {
		if (shadowType == null) {
			return false;
		}
		if (!resourceOid.equals(shadowType.getResourceRef().getOid())) {
			return false;
		}
		if (!MiscUtil.equals(kind, shadowType.getKind())) {
			return false;
		}
		if (intent == null) {
			return true;
		}
		return MiscUtil.equals(intent, shadowType.getIntent());
	}
	
}
