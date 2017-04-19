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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationLockoutStatusCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationStatusCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationValidityCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.AddRemoveAttributeValuesCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.AuxiliaryObjectClassesCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CountObjectsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CreateCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.DeleteCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.LiveSyncCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PagedSearchCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PasswordCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ScriptCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.TestConnectionCapabilityType;
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
					null, "resolving connector in " + resource, null, parentResult);		// TODO task
		} else {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static PrismObject<ConnectorType> getConnectorIfPresent(PrismObject<ResourceType> resource) {
		PrismReference existingConnectorRef = resource.findReference(ResourceType.F_CONNECTOR_REF);
		if (existingConnectorRef == null || existingConnectorRef.isEmpty()) {
			return null;
		}
		return (PrismObject<ConnectorType>) existingConnectorRef.getValue().getObject();
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
	@Nullable
	public static <T extends CapabilityType> T getEffectiveCapability(ResourceType resource, Class<T> capabilityClass) {
		return getEffectiveCapability(resource, null, capabilityClass);
	}

	/**
	 * Assumes that native capabilities are already cached.
	 */
	public static <T extends CapabilityType> T getEffectiveCapability(ResourceType resource,
																	  ResourceObjectTypeDefinitionType resourceObjectTypeDefinitionType,
																	  Class<T> capabilityClass) {
		T capability = getEffectiveCapabilityInternal(resource, resourceObjectTypeDefinitionType, capabilityClass);
		if (CapabilityUtil.isCapabilityEnabled(capability)) {
			return capability;
		} else {
			// Disabled or null capability, pretend that it is not there
			return null;
		}
	}

	private static <T extends CapabilityType> T getEffectiveCapabilityInternal(ResourceType resource,
																			   ResourceObjectTypeDefinitionType resourceObjectTypeDefinitionType,
																			   Class<T> capabilityClass) {
		if (resourceObjectTypeDefinitionType != null && resourceObjectTypeDefinitionType.getConfiguredCapabilities() != null) {
			T configuredCapability = CapabilityUtil.getCapability(resourceObjectTypeDefinitionType.getConfiguredCapabilities().getAny(), capabilityClass);
			if (configuredCapability != null) {
				return configuredCapability;
			}
			// No capability at the level of resource object type, continuing at the resource level
		}

		// TODO: additional connector capabilities
		
		return CapabilityUtil.getEffectiveCapability(resource.getCapabilities(), capabilityClass);
	}
	
	public static <T extends CapabilityType> boolean hasEffectiveCapability(ResourceType resource, Class<T> capabilityClass) {
		return getEffectiveCapability(resource, capabilityClass) != null;
	}

	/**
	 * Assumes that native capabilities are already cached.
	 */
	public static List<Object> getAllCapabilities(ResourceType resource) throws SchemaException {
		return getEffectiveCapabilities(resource, true);
	}

	/**
	 * Assumes that native capabilities are already cached. 
	 */
	public static List<Object> getEffectiveCapabilities(ResourceType resource) throws SchemaException {
		return getEffectiveCapabilities(resource, false);
	}

	private static List<Object> getEffectiveCapabilities(ResourceType resource, boolean includeDisabled) throws SchemaException {
		List<Object> rv = new ArrayList<>();
		if (resource.getCapabilities() == null) {
			return rv;
		}
		List<Object> configuredCaps = resource.getCapabilities().getConfigured() != null ? resource.getCapabilities().getConfigured().getAny() : Collections.emptyList();
		List<Object> nativeCaps = resource.getCapabilities().getNative() != null ? resource.getCapabilities().getNative().getAny() : Collections.emptyList();
		for (Object configuredCapability : configuredCaps) {
			if (includeDisabled || CapabilityUtil.isCapabilityEnabled(configuredCapability)) {
				rv.add(configuredCapability);
			}
		}
		for (Object nativeCapability: nativeCaps) {
			if (!CapabilityUtil.containsCapabilityWithSameElementName(configuredCaps, nativeCapability)) {
				if (includeDisabled || CapabilityUtil.isCapabilityEnabled(nativeCapability)) {
					rv.add(nativeCapability);
				}
			}
		}
		return rv;
	}

	public static boolean isActivationCapabilityEnabled(ResourceType resource) {
		return getEffectiveCapability(resource, ActivationCapabilityType.class) != null;
	}
	
	public static boolean isActivationLockoutStatusCapabilityEnabled(ResourceType resource) {
		return getEffectiveActivationLockoutStatusCapability(resource) != null;
	}

	public static ActivationLockoutStatusCapabilityType getEffectiveActivationLockoutStatusCapability(ResourceType resource) {
		ActivationCapabilityType act = getEffectiveCapability(resource, ActivationCapabilityType.class);
		if (act == null || act.getLockoutStatus() == null || Boolean.FALSE.equals(act.getLockoutStatus().isEnabled())) {
			return null;
		} else {
			return act.getLockoutStatus();
		}
	}

	public static ActivationStatusCapabilityType getEffectiveActivationStatusCapability(ResourceType resource) {
		ActivationCapabilityType act = getEffectiveCapability(resource, ActivationCapabilityType.class);
		if (act == null || act.getStatus() == null || Boolean.FALSE.equals(act.getStatus().isEnabled())) {
			return null;
		} else {
			return act.getStatus();
		}
	}

	public static boolean isActivationStatusCapabilityEnabled(ResourceType resource) {
		return getEffectiveActivationStatusCapability(resource) != null;
	}

	public static ActivationValidityCapabilityType getEffectiveActivationValidFromCapability(ResourceType resource) {
		ActivationCapabilityType act = getEffectiveCapability(resource, ActivationCapabilityType.class);
		if (act == null || act.getValidFrom() == null || Boolean.FALSE.equals(act.getValidFrom().isEnabled())) {
			return null;
		} else {
			return act.getValidFrom();
		}
	}

	public static ActivationValidityCapabilityType getEffectiveActivationValidToCapability(ResourceType resource) {
		ActivationCapabilityType act = getEffectiveCapability(resource, ActivationCapabilityType.class);
		if (act == null || act.getValidTo() == null || Boolean.FALSE.equals(act.getValidTo().isEnabled())) {
			return null;
		} else {
			return act.getValidTo();
		}
	}

	public static boolean isActivationValidityCapabilityEnabled(ResourceType resource) {
		return getEffectiveActivationValidFromCapability(resource) != null || getEffectiveActivationValidToCapability(resource) != null;
	}
	
	public static boolean isCredentialsCapabilityEnabled(ResourceType resource) {
		return getEffectiveCapability(resource, CredentialsCapabilityType.class) != null;
	}
	
	public static boolean isCreateCapabilityEnabled(ResourceType resource){
		return getEffectiveCapability(resource, CreateCapabilityType.class) != null;
	}
	
	public static boolean isCountObjectsCapabilityEnabled(ResourceType resource){
		return getEffectiveCapability(resource, CountObjectsCapabilityType.class) != null;
	}
	
	public static boolean isPasswordCapabilityEnabled(ResourceType resource){
		return getEffectivePasswordCapability(resource) != null;
	}

	public static PasswordCapabilityType getEffectivePasswordCapability(ResourceType resource) {
		CredentialsCapabilityType cct = getEffectiveCapability(resource, CredentialsCapabilityType.class);
		if (cct == null || cct.getPassword() == null || Boolean.FALSE.equals(cct.getPassword().isEnabled())) {
			return null;
		} else {
			return cct.getPassword();
		}
	}

	public static boolean isLiveSyncCapabilityEnabled(ResourceType resource) {
		return getEffectiveCapability(resource, LiveSyncCapabilityType.class) != null;
	}
	
	public static boolean isScriptCapabilityEnabled(ResourceType resource) {
		return getEffectiveCapability(resource, ScriptCapabilityType.class) != null;
	}
	
	public static boolean isTestConnectionCapabilityEnabled(ResourceType resource) {
		return getEffectiveCapability(resource, TestConnectionCapabilityType.class) != null;
	}
	
	public static boolean isAuxiliaryObjectClassCapabilityEnabled(ResourceType resource) {
		return getEffectiveCapability(resource, AuxiliaryObjectClassesCapabilityType.class) != null;
	}
	
	public static boolean isPagedSearchCapabilityEnabled(ResourceType resource) {
		return getEffectiveCapability(resource, PagedSearchCapabilityType.class) != null;
	}
	
	public static boolean isReadCapabilityEnabled(ResourceType resource){
		return getEffectiveCapability(resource, ReadCapabilityType.class) != null;
	}
	
	public static boolean isUpdateCapabilityEnabled(ResourceType resource){
		return getEffectiveCapability(resource, UpdateCapabilityType.class) != null;
	}
	
	public static boolean isAddRemoveAttributesValuesCapabilityEnabled(ResourceType resource){
		return getEffectiveCapability(resource, AddRemoveAttributeValuesCapabilityType.class) != null;
	}
	
	public static boolean isDeleteCapabilityEnabled(ResourceType resource){
		return getEffectiveCapability(resource, DeleteCapabilityType.class) != null;
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
	
	public static boolean hasResourceNativeActivationStatusCapability(ResourceType resource) {
		ActivationCapabilityType activationCapability = null;
		if (resource.getCapabilities() != null && resource.getCapabilities().getNative() != null) {
			activationCapability = CapabilityUtil.getCapability(resource.getCapabilities().getNative().getAny(),
					ActivationCapabilityType.class);
		}
		return CapabilityUtil.getEffectiveActivationStatus(activationCapability) != null;
	}
	
	public static boolean hasResourceNativeActivationLockoutCapability(ResourceType resource) {
		ActivationCapabilityType activationCapability = null;
		// check resource native capabilities. if resource cannot do
		// activation, it sholud be null..
		if (resource.getCapabilities() != null && resource.getCapabilities().getNative() != null) {
			activationCapability = CapabilityUtil.getCapability(resource.getCapabilities().getNative().getAny(),
					ActivationCapabilityType.class);
		}
		return CapabilityUtil.getEffectiveActivationLockoutStatus(activationCapability) != null;
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

	public static PrismContainer<ConnectorConfigurationType> getConfigurationContainer(ResourceType resourceType) {
		return getConfigurationContainer(resourceType.asPrismObject());
	}
	
	public static PrismContainer<ConnectorConfigurationType> getConfigurationContainer(PrismObject<ResourceType> resource) {
		return resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
	}

	@NotNull
	public static String getResourceNamespace(PrismObject<ResourceType> resource) {
		return getResourceNamespace(resource.asObjectable());
	}

	@NotNull
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
	
	public static boolean isForceLoadDependentShadow(ResourceObjectTypeDependencyType dependency){
		Boolean force = dependency.isForceLoad();
		if (force == null){
			return false;
		}
		
		return force;
	}
	
	public static boolean isDown(ResourceType resource){
		return (resource.getOperationalState() != null && AvailabilityStatusType.DOWN == resource.getOperationalState().getLastAvailabilityStatus());
	}
	
	public static AvailabilityStatusType getLastAvailabilityStatus(ResourceType resource){
		if (resource.getOperationalState() == null) {
			return null;
		}
		
		if (resource.getOperationalState().getLastAvailabilityStatus() == null) {
			return null;
		}
		
		return resource.getOperationalState().getLastAvailabilityStatus();
		
	}

	public static boolean isAvoidDuplicateValues(ResourceType resource) {
		if (resource.getConsistency() == null) {
			return false;
		}
		if (resource.getConsistency().isAvoidDuplicateValues() == null) {
			return false;
		}
		return resource.getConsistency().isAvoidDuplicateValues();
	}

	public static boolean isCaseIgnoreAttributeNames(ResourceType resource) {
		if (resource.getConsistency() == null) {
			return false;
		}
		if (resource.getConsistency().isCaseIgnoreAttributeNames() == null) {
			return false;
		}
		return resource.getConsistency().isCaseIgnoreAttributeNames();
	}

	// always returns non-null value
	public static List<ObjectReferenceType> getOwnerRef(ResourceType resource) {
		if (resource.getBusiness() == null) {
			return new ArrayList<>();
		}
		return resource.getBusiness().getOwnerRef();
	}

	// always returns non-null value
	public static List<ObjectReferenceType> getApproverRef(ResourceType resource) {
		if (resource.getBusiness() == null) {
			return new ArrayList<>();
		}
		return resource.getBusiness().getApproverRef();
	}

	@NotNull
	public static Collection<Class<? extends CapabilityType>> getNativeCapabilityClasses(ResourceType resource) {
		Set<Class<? extends CapabilityType>> rv = new HashSet<>();
		if (resource.getCapabilities() == null || resource.getCapabilities().getNative() == null) {
			return rv;
		}
		for (Object o : resource.getCapabilities().getNative().getAny()) {
			rv.add(CapabilityUtil.asCapabilityType(o).getClass());
		}
		return rv;
	}

	@NotNull
	public static ShadowKindType fillDefault(ShadowKindType kind) {
		return kind != null ? kind : ShadowKindType.ACCOUNT;
	}

	@NotNull
	public static String fillDefault(String intent) {
		return intent != null ? intent : SchemaConstants.INTENT_DEFAULT;
	}

	public static ResourceObjectTypeDefinitionType findObjectTypeDefinition(PrismObject<ResourceType> resourceObject, @Nullable ShadowKindType kind,
			@Nullable String intent) {
		if (resourceObject == null || resourceObject.asObjectable().getSchemaHandling() == null) {
			return null;
		}
		for (ResourceObjectTypeDefinitionType def : resourceObject.asObjectable().getSchemaHandling().getObjectType()) {
			if (fillDefault(kind).equals(fillDefault(def.getKind())) && fillDefault(intent).equals(fillDefault(def.getIntent()))) {
				return def;
			}
		}
		return null;
	}

	@Nullable
	public static ObjectSynchronizationType findObjectSynchronization(@Nullable ResourceType resource, @Nullable ShadowKindType kind, @Nullable String intent) {
		if (resource == null || resource.getSynchronization() == null) {
			return null;
		}
		for (ObjectSynchronizationType def : resource.getSynchronization().getObjectSynchronization()) {
			if (fillDefault(kind).equals(fillDefault(def.getKind())) && fillDefault(intent).equals(fillDefault(def.getIntent()))) {
				return def;
			}
		}
		return null;
	}

	@NotNull
	public static QName fillDefaultFocusType(QName focusType) {
		return focusType != null ? focusType : UserType.COMPLEX_TYPE;
	}
}
