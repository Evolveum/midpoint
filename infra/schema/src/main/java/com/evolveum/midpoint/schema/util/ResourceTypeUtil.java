/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.exception.MaintenanceException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.BooleanUtils;
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
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
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
        return xmlSchema != null ? ObjectTypeUtil.findXsdElement(xmlSchema) : null;
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

        for (ConnectorInstanceSpecificationType additionalConnector: resource.getAdditionalConnector()) {
            T connectorCapability = CapabilityUtil.getEffectiveCapability(additionalConnector.getCapabilities(), capabilityClass);
            if (CapabilityUtil.isCapabilityEnabled(connectorCapability)) {
                return connectorCapability;
            }
        }

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

//    public static boolean isActivationCapabilityEnabled(ResourceType resource) {
//        return getEffectiveCapability(resource, ActivationCapabilityType.class) != null;
//    }

    public static boolean isActivationCapabilityEnabled(ResourceType resource, ResourceObjectTypeDefinitionType resourceObjectTypeDefinitionType) {
        ActivationCapabilityType activationCap = getEffectiveCapability(resource, resourceObjectTypeDefinitionType, ActivationCapabilityType.class);
        return isEnabled(activationCap);
    }

    public static boolean isActivationLockoutStatusCapabilityEnabled(ResourceType resource, ResourceObjectTypeDefinitionType resourceObjectTypeDefinitionType) {


        ActivationLockoutStatusCapabilityType lockoutCap = getEffectiveActivationLockoutStatusCapability(resource);
        return isEnabled(lockoutCap);
    }

    private static boolean isEnabled(CapabilityType capability) {
        if (capability == null) {
            return false;
        }
        return BooleanUtils.isNotFalse(capability.isEnabled());
    }

    public static ActivationLockoutStatusCapabilityType getEffectiveActivationLockoutStatusCapability(ResourceType resource) {
        ActivationCapabilityType act = getEffectiveCapability(resource, ActivationCapabilityType.class);
        if (act == null || act.getLockoutStatus() == null || Boolean.FALSE.equals(act.getLockoutStatus().isEnabled())) {
            return null;
        } else {
            return act.getLockoutStatus();
        }
    }



    public static boolean isActivationStatusCapabilityEnabled(ResourceType resource, ResourceObjectTypeDefinitionType resourceObjectTypeDefinitionType) {
        ActivationStatusCapabilityType activationStatusCap = getEffectiveActivationStatusCapability(resource, resourceObjectTypeDefinitionType);
        return isEnabled(activationStatusCap);
    }

    public static ActivationStatusCapabilityType getEffectiveActivationStatusCapability(ResourceType resource, ResourceObjectTypeDefinitionType resourceObjectTypeDefinitionType) {
        ActivationCapabilityType act = getEffectiveCapability(resource, resourceObjectTypeDefinitionType, ActivationCapabilityType.class);
        if (act == null || act.getStatus() == null || Boolean.FALSE.equals(act.getStatus().isEnabled())) {
            return null;
        } else {
            return act.getStatus();
        }
    }

    public static boolean isActivationValidityFromCapabilityEnabled(ResourceType resource, ResourceObjectTypeDefinitionType resourceObjectTypeDefinitionType) {
        ActivationValidityCapabilityType validFromCap = getEffectiveActivationValidFromCapability(resource, resourceObjectTypeDefinitionType);
        return isEnabled(validFromCap);
    }

    public static ActivationValidityCapabilityType getEffectiveActivationValidFromCapability(ResourceType resource, ResourceObjectTypeDefinitionType resourceObjectTypeDefinitionType) {
        ActivationCapabilityType act = getEffectiveCapability(resource, resourceObjectTypeDefinitionType, ActivationCapabilityType.class);
        if (act == null || act.getValidFrom() == null || Boolean.FALSE.equals(act.getValidFrom().isEnabled())) {
            return null;
        } else {
            return act.getValidFrom();
        }
    }

    public static boolean isActivationValidityToCapabilityEnabled(ResourceType resource, ResourceObjectTypeDefinitionType resourceObjectTypeDefinitionType) {
        ActivationValidityCapabilityType validToCap = getEffectiveActivationValidToCapability(resource, resourceObjectTypeDefinitionType);
        return isEnabled(validToCap);
    }

    public static ActivationValidityCapabilityType getEffectiveActivationValidToCapability(ResourceType resource, ResourceObjectTypeDefinitionType resourceObjectTypeDefinitionType) {
        ActivationCapabilityType act = getEffectiveCapability(resource, resourceObjectTypeDefinitionType, ActivationCapabilityType.class);
        if (act == null || act.getValidTo() == null || Boolean.FALSE.equals(act.getValidTo().isEnabled())) {
            return null;
        } else {
            return act.getValidTo();
        }
    }


    public static boolean isCredentialsCapabilityEnabled(ResourceType resource, ResourceObjectTypeDefinitionType resourceObjectTypeDefinitionType) {
        CredentialsCapabilityType credentialsCap = getEffectiveCapability(resource, resourceObjectTypeDefinitionType, CredentialsCapabilityType.class);
        return isEnabled(credentialsCap);
    }

    public static boolean isCreateCapabilityEnabled(ResourceType resource){
        CreateCapabilityType createCap = getEffectiveCapability(resource, CreateCapabilityType.class);
        return isEnabled(createCap);
    }

    public static boolean isCountObjectsCapabilityEnabled(ResourceType resource){
        return getEffectiveCapability(resource, CountObjectsCapabilityType.class) != null;
    }

//    public static boolean isPasswordCapabilityEnabled(ResourceType resource){
//        return isPasswordCapabilityEnabled(resource, null);
//    }

    public static boolean isPasswordCapabilityEnabled(ResourceType resource, ResourceObjectTypeDefinitionType def){
        PasswordCapabilityType passwordCap = getEffectivePasswordCapability(resource, def);
        return isEnabled(passwordCap);
    }

    public static PasswordCapabilityType getEffectivePasswordCapability(ResourceType resource, ResourceObjectTypeDefinitionType def) {
        CredentialsCapabilityType cct = getEffectiveCapability(resource, def, CredentialsCapabilityType.class);
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

    public static <C extends CapabilityType> boolean isCapabilityEnabled(ResourceType resource, Class<C> type) {
        return getEffectiveCapability(resource, type) != null;
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

    /**
     * AddRemoveAttributeValuesCapabilityType capability is deprecated.
     *  Use addRemoveAttributeValues element of Update capability instead.
     */
    @Deprecated // TODO remove in 4.2
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
        return CapabilityUtil.getEnabledActivationStatus(activationCapability) != null;
    }

    public static boolean hasResourceNativeActivationLockoutCapability(ResourceType resource) {
        ActivationCapabilityType activationCapability = null;
        // check resource native capabilities. if resource cannot do
        // activation, it sholud be null..
        if (resource.getCapabilities() != null && resource.getCapabilities().getNative() != null) {
            activationCapability = CapabilityUtil.getCapability(resource.getCapabilities().getNative().getAny(),
                    ActivationCapabilityType.class);
        }
        return CapabilityUtil.getEnabledActivationLockoutStatus(activationCapability) != null;
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
        // TODO review the code below
        for (ResourceObjectTypeDefinitionType objType: schemaHandling.getObjectType()) {
            if (objType.getKind() == kind || (objType.getKind() == null && kind == ShadowKindType.ACCOUNT)) {
                if (intent == null && Boolean.TRUE.equals(objType.isDefault())) {
                    return objType;
                }
                if (objType.getIntent() != null && objType.getIntent().equals(intent)) {
                    return objType;
                }
                if (objType.getIntent() == null && Boolean.TRUE.equals(objType.isDefault()) && intent != null && intent.equals(SchemaConstants.INTENT_DEFAULT)) {
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

    public static AdministrativeAvailabilityStatusType getAdministrativeAvailabilityStatus(ResourceType resource) {
        if (resource == null)
            return null;

        if (resource.getAdministrativeOperationalState() == null) {
            return null;
        }

        if (resource.getAdministrativeOperationalState().getAdministrativeAvailabilityStatus() == null) {
            return null;
        }

        return resource.getAdministrativeOperationalState().getAdministrativeAvailabilityStatus();
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

    public static ShadowCheckType getShadowConstraintsCheck(ResourceType resource) {
        ResourceConsistencyType consistency = resource.getConsistency();
        if (consistency == null) {
            return ShadowCheckType.NONE;
        }
        ShadowCheckType shadowCheckType = consistency.getShadowConstraintsCheck();
        if (shadowCheckType == null) {
            return ShadowCheckType.NONE;
        }
        return shadowCheckType;
    }

    public static boolean isValidateSchema(ResourceType resource) {
        ResourceConsistencyType consistency = resource.getConsistency();
        return consistency != null && Boolean.TRUE.equals(consistency.isValidateSchema());
    }

    // TODO: maybe later move to ResourceSchema?
    public static void validateSchema(ResourceSchema resourceSchema, PrismObject<ResourceType> resource) throws SchemaException {

        Set<QName> objectClassNames = new HashSet<>();

        for (ObjectClassComplexTypeDefinition objectClassDefinition: resourceSchema.getObjectClassDefinitions()) {
            QName typeName = objectClassDefinition.getTypeName();
            if (objectClassNames.contains(typeName)) {
                throw new SchemaException("Duplicate definition of object class "+typeName+" in resource schema of "+resource);
            }
            objectClassNames.add(typeName);

            validateObjectClassDefinition(objectClassDefinition, resource);
        }
    }

    public static void validateObjectClassDefinition(ObjectClassComplexTypeDefinition objectClassDefinition,
            PrismObject<ResourceType> resource) throws SchemaException {
        Set<QName> attributeNames = new HashSet<>();
        for (ResourceAttributeDefinition<?> attributeDefinition: objectClassDefinition.getAttributeDefinitions()) {
            QName attrName = attributeDefinition.getItemName();
            if (attributeNames.contains(attrName)) {
                throw new SchemaException("Duplicate definition of attribute "+attrName+" in object class "+objectClassDefinition.getTypeName()+" in resource schema of "+resource);
            }
            attributeNames.add(attrName);
        }

        Collection<? extends ResourceAttributeDefinition<?>> primaryIdentifiers = objectClassDefinition.getPrimaryIdentifiers();
        Collection<? extends ResourceAttributeDefinition<?>> secondaryIdentifiers = objectClassDefinition.getSecondaryIdentifiers();

        if (primaryIdentifiers.isEmpty() && secondaryIdentifiers.isEmpty()) {
            throw new SchemaException("No identifiers in definition of object class "+objectClassDefinition.getTypeName()+" in resource schema of "+resource);
        }
    }

    public static RecordPendingOperationsType getRecordPendingOperations(ResourceType resourceType) {
        ResourceConsistencyType consistencyType = resourceType.getConsistency();
        if (consistencyType == null) {
            return RecordPendingOperationsType.ASYNCHRONOUS;
        }
        RecordPendingOperationsType recordPendingOperations = consistencyType.getRecordPendingOperations();
        if (recordPendingOperations == null) {
            return RecordPendingOperationsType.ASYNCHRONOUS;
        }
        return recordPendingOperations;
    }

    public static boolean isRefreshOnRead(ResourceType resource) {
        ResourceConsistencyType consistency = resource.getConsistency();
        if (consistency == null) {
            return false;
        }

        Boolean refreshOnRead = consistency.isRefreshOnRead();
        if (refreshOnRead != null) {
            return refreshOnRead;
        }

        // legacy way (misspelled property)
        Boolean reshreshOnRead = consistency.isReshreshOnRead();
        if (reshreshOnRead != null) {
            return reshreshOnRead;
        }

        return false;
    }

    public static ErrorSelectorType getConnectorErrorCriticality(ResourceType resourceType) {
        ResourceConsistencyType consistency = resourceType.getConsistency();
        return consistency != null ? consistency.getConnectorErrorCriticality() : null;
    }

    public static void checkNotInMaintenance(PrismObject<ResourceType> resource) throws MaintenanceException {
        if (isInMaintenance(resource)) {
            throw new MaintenanceException("Resource " + resource + " is in the maintenance");
        }
    }

    public static boolean isInMaintenance(PrismObject<ResourceType> resource) {
        return isInMaintenance(resource.asObjectable());
    }

    public static boolean isInMaintenance(ResourceType resource) {
        if (resource == null) {
            return false;
        }

        AdministrativeOperationalStateType administrativeOperationalState = resource.getAdministrativeOperationalState();
        if (administrativeOperationalState == null) {
            return false;
        }

        AdministrativeAvailabilityStatusType administrativeAvailabilityStatus = administrativeOperationalState.getAdministrativeAvailabilityStatus();
        if (administrativeAvailabilityStatus == null) {
            return false;
        }

        if (AdministrativeAvailabilityStatusType.MAINTENANCE == administrativeAvailabilityStatus) {
            return true;
        }

        return false;
    }
}
