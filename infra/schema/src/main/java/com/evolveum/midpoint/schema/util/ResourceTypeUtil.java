/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.util;

import java.util.*;
import java.util.stream.Collectors;
import javax.xml.datatype.Duration;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.midpoint.xml.ns._public.connector.icf_1.connector_schema_3.ConfigurationPropertiesType;
import com.evolveum.midpoint.xml.ns._public.connector.icf_1.connector_schema_3.ResultsHandlerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.w3c.dom.Element;

import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.MaintenanceException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;

import static com.evolveum.midpoint.schema.SchemaConstantsGenerated.ICF_C_RESULTS_HANDLER_CONFIGURATION;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.ICF_CONFIGURATION_PROPERTIES_NAME;

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
        return existingConnectorRef.getValue().getObject();
    }

    public static Element getResourceXsdSchemaElement(ResourceType resource) {
        XmlSchemaType xmlSchemaType = resource.getSchema();
        if (xmlSchemaType == null) {
            return null;
        }
        return ObjectTypeUtil.findXsdElement(xmlSchemaType);
    }

    public static Element getResourceXsdSchemaElement(PrismObject<ResourceType> resource) {
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
            throw new IllegalStateException("Internal schema error: " + e.getMessage(), e);
        }
    }

    /**
     * Returns collection of capabilities. Note: there is difference between empty set and null.
     * Empty set means that we know the resource has no capabilities. Null means that the
     * capabilities were not yet determined.
     */
    public static @Nullable CapabilityCollectionType getNativeCapabilitiesCollection(ResourceType resource) {
        if (resource.getCapabilities() == null) {
            // No capabilities, not initialized
            return null;
        } else {
            return resource.getCapabilities().getNative();
        }
    }

    public static boolean hasSchemaGenerationConstraints(ResourceType resource) {
        if (resource == null || resource.getSchema() == null) {
            return false;
        }
        SchemaGenerationConstraintsType generationConstraints = resource.getSchema().getGenerationConstraints();
        if (generationConstraints == null) {
            return false;
        } else {
            List<QName> constraints = generationConstraints.getGenerateObjectClass();
            return constraints != null && !constraints.isEmpty();
        }
    }

    public static @NotNull List<QName> getSchemaGenerationConstraints(ResourceType resource) {
        if (hasSchemaGenerationConstraints(resource)) {
            return resource.getSchema().getGenerationConstraints().getGenerateObjectClass();
        } else {
            return List.of();
        }
    }

    /**
     * Assumes that native capabilities are already cached.
     */
    @Nullable
    public static <T extends CapabilityType> T getEnabledCapability(ResourceType resource, Class<T> capabilityClass) {
        return getEnabledCapability(resource, null, capabilityClass);
    }

    /**
     * Gets an enabled instance of given capability type.
     *
     * (No fetching: assumes that native capabilities are already cached.)
     *
     * Precondition: the bean must be fully expanded (regarding object type inheritance).
     */
    public static <T extends CapabilityType> T getEnabledCapability(
            @NotNull ResourceType resource,
            @Nullable ResourceObjectTypeDefinitionType resourceObjectTypeDefinitionBean,
            @NotNull Class<T> capabilityClass) {
        T capability = getCapabilityInternal(resource, resourceObjectTypeDefinitionBean, capabilityClass);
        if (CapabilityUtil.isCapabilityEnabled(capability)) {
            return capability;
        } else {
            // Disabled or null capability, pretend that it is not there
            return null;
        }
    }

    // object type definition must be fully expanded!
    private static <T extends CapabilityType> T getCapabilityInternal(
            @NotNull ResourceType resource,
            @Nullable ResourceObjectTypeDefinitionType resourceObjectTypeDefinitionBean,
            @NotNull Class<T> capabilityClass) {
        if (resourceObjectTypeDefinitionBean != null) {
            T configuredCapability =
                    CapabilityUtil.getCapability(resourceObjectTypeDefinitionBean.getConfiguredCapabilities(), capabilityClass);
            if (configuredCapability != null) {
                return configuredCapability;
            }
            // No capability at the level of resource object type, continuing at the resource level
        }

        for (ConnectorInstanceSpecificationType additionalConnector : resource.getAdditionalConnector()) {
            T connectorCapability = CapabilityUtil.getCapability(additionalConnector.getCapabilities(), capabilityClass);
            if (CapabilityUtil.isCapabilityEnabled(connectorCapability)) {
                return connectorCapability;
            }
        }

        return CapabilityUtil.getCapability(resource.getCapabilities(), capabilityClass);
    }

    public static <T extends CapabilityType> boolean hasEnabledCapability(
            @NotNull ResourceType resource,
            @NotNull Class<T> capabilityClass) {
        return getEnabledCapability(resource, capabilityClass) != null;
    }

    /**
     * Assumes that native capabilities are already cached.
     */
    @VisibleForTesting
    public static List<CapabilityType> getEnabledCapabilities(@NotNull ResourceType resource) throws SchemaException {
        return CapabilityUtil.getCapabilities(resource.getCapabilities(), false);
    }

    // FIXME ensure that typeDefinitionBean is fully expanded!
    public static boolean isActivationCapabilityEnabled(
            ResourceType resource, ResourceObjectTypeDefinitionType typeDefinitionBean) {
        ActivationCapabilityType activationCap = getEnabledCapability(resource, typeDefinitionBean, ActivationCapabilityType.class);
        return isEnabled(activationCap);
    }

    public static boolean isActivationLockoutStatusCapabilityEnabled(
            ResourceType resource, ResourceObjectTypeDefinitionType typeDefinitionBean) {
        ActivationLockoutStatusCapabilityType lockoutCap = getEffectiveActivationLockoutStatusCapability(resource, typeDefinitionBean);
        return isEnabled(lockoutCap);
    }

    private static boolean isEnabled(CapabilityType capability) {
        return capability != null
                && BooleanUtils.isNotFalse(capability.isEnabled());
    }

    private static ActivationLockoutStatusCapabilityType getEffectiveActivationLockoutStatusCapability(
            ResourceType resource, ResourceObjectTypeDefinitionType typeDefinitionBean) {
        ActivationCapabilityType act = getEnabledCapability(resource, typeDefinitionBean, ActivationCapabilityType.class);
        if (act == null || act.getLockoutStatus() == null || Boolean.FALSE.equals(act.getLockoutStatus().isEnabled())) {
            return null;
        } else {
            return act.getLockoutStatus();
        }
    }

    // FIXME typeDefBean must be fully expanded!
    public static boolean isActivationStatusCapabilityEnabled(ResourceType resource, ResourceObjectTypeDefinitionType typeDefBean) {
        ActivationStatusCapabilityType activationStatusCap = getEffectiveActivationStatusCapability(resource, typeDefBean);
        return isEnabled(activationStatusCap);
    }

    // FIXME typeDefBean must be fully expanded!
    private static ActivationStatusCapabilityType getEffectiveActivationStatusCapability(
            ResourceType resource, ResourceObjectTypeDefinitionType typeDefinitionBean) {
        ActivationCapabilityType act = getEnabledCapability(resource, typeDefinitionBean, ActivationCapabilityType.class);
        if (act == null || act.getStatus() == null || Boolean.FALSE.equals(act.getStatus().isEnabled())) {
            return null;
        } else {
            return act.getStatus();
        }
    }

    // FIXME typeDefBean must be fully expanded!
    public static boolean isActivationValidityFromCapabilityEnabled(
            ResourceType resource, ResourceObjectTypeDefinitionType typeDefBean) {
        ActivationValidityCapabilityType validFromCap = getEffectiveActivationValidFromCapability(resource, typeDefBean);
        return isEnabled(validFromCap);
    }

    private static ActivationValidityCapabilityType getEffectiveActivationValidFromCapability(
            ResourceType resource, ResourceObjectTypeDefinitionType typeDefinitionBean) {
        ActivationCapabilityType act = getEnabledCapability(resource, typeDefinitionBean, ActivationCapabilityType.class);
        if (act == null || act.getValidFrom() == null || Boolean.FALSE.equals(act.getValidFrom().isEnabled())) {
            return null;
        } else {
            return act.getValidFrom();
        }
    }

    // FIXME typeDefinitionBean must be fully expanded!
    public static boolean isActivationValidityToCapabilityEnabled(
            ResourceType resource, ResourceObjectTypeDefinitionType typeDefinitionBean) {
        ActivationValidityCapabilityType validToCap = getEffectiveActivationValidToCapability(resource, typeDefinitionBean);
        return isEnabled(validToCap);
    }

    private static ActivationValidityCapabilityType getEffectiveActivationValidToCapability(
            ResourceType resource, ResourceObjectTypeDefinitionType typeDefinitionBean) {
        ActivationCapabilityType act = getEnabledCapability(resource, typeDefinitionBean, ActivationCapabilityType.class);
        if (act == null || act.getValidTo() == null || Boolean.FALSE.equals(act.getValidTo().isEnabled())) {
            return null;
        } else {
            return act.getValidTo();
        }
    }

    // FIXME typeDefinitionBean must be fully expanded!
    public static boolean isCredentialsCapabilityEnabled(
            ResourceType resource, ResourceObjectTypeDefinitionType typeDefinitionBean) {
        CredentialsCapabilityType credentialsCap = getEnabledCapability(resource, typeDefinitionBean, CredentialsCapabilityType.class);
        return isEnabled(credentialsCap);
    }

    public static boolean isCreateCapabilityEnabled(ResourceType resource) {
        CreateCapabilityType createCap = getEnabledCapability(resource, CreateCapabilityType.class);
        return isEnabled(createCap);
    }

    public static boolean isCountObjectsCapabilityEnabled(ResourceType resource) {
        return getEnabledCapability(resource, CountObjectsCapabilityType.class) != null;
    }

    // FIXME typeDefinitionBean must be fully expanded!
    public static boolean isPasswordCapabilityEnabled(ResourceType resource, ResourceObjectTypeDefinitionType typeDefinitionBean) {
        PasswordCapabilityType passwordCap = getEffectivePasswordCapability(resource, typeDefinitionBean);
        return isEnabled(passwordCap);
    }

    public static boolean isLastLoginTimestampCapabilityEnabled(ResourceType resource, ResourceObjectTypeDefinitionType def) {
        BehaviorCapabilityType cap = getEnabledCapability(resource, def, BehaviorCapabilityType.class);
        if (!CapabilityUtil.isEnabled(cap)) {
            return false;
        }
        return CapabilityUtil.isEnabled(cap.getLastLoginTimestamp());
    }

    private static PasswordCapabilityType getEffectivePasswordCapability(
            ResourceType resource, ResourceObjectTypeDefinitionType typeDefinitionBean) {
        CredentialsCapabilityType cct = getEnabledCapability(resource, typeDefinitionBean, CredentialsCapabilityType.class);
        if (cct == null || cct.getPassword() == null || Boolean.FALSE.equals(cct.getPassword().isEnabled())) {
            return null;
        } else {
            return cct.getPassword();
        }
    }

    public static boolean isLiveSyncCapabilityEnabled(ResourceType resource) {
        return getEnabledCapability(resource, LiveSyncCapabilityType.class) != null;
    }

    public static boolean isScriptCapabilityEnabled(ResourceType resource) {
        return getEnabledCapability(resource, ScriptCapabilityType.class) != null;
    }

    public static <C extends CapabilityType> boolean isCapabilityEnabled(ResourceType resource, Class<C> type) {
        return getEnabledCapability(resource, type) != null;
    }

    public static boolean isTestConnectionCapabilityEnabled(ResourceType resource) {
        return getEnabledCapability(resource, TestConnectionCapabilityType.class) != null;
    }

    public static boolean isAuxiliaryObjectClassCapabilityEnabled(ResourceType resource) {
        return getEnabledCapability(resource, AuxiliaryObjectClassesCapabilityType.class) != null;
    }

    public static boolean isPagedSearchCapabilityEnabled(ResourceType resource) {
        return getEnabledCapability(resource, PagedSearchCapabilityType.class) != null;
    }

    public static boolean isReadCapabilityEnabled(ResourceType resource) {
        return getEnabledCapability(resource, ReadCapabilityType.class) != null;
    }

    public static boolean isUpdateCapabilityEnabled(ResourceType resource) {
        return getEnabledCapability(resource, UpdateCapabilityType.class) != null;
    }

    public static boolean isDeleteCapabilityEnabled(ResourceType resource) {
        return getEnabledCapability(resource, DeleteCapabilityType.class) != null;
    }

    @VisibleForTesting
    public static boolean hasResourceNativeActivationCapability(@NotNull ResourceType resource) {
        return resource.getCapabilities() != null
                && CapabilityUtil.getCapability(resource.getCapabilities().getNative(), ActivationCapabilityType.class) != null;
    }

    @VisibleForTesting
    public static boolean hasResourceNativeActivationStatusCapability(@NotNull ResourceType resource) {
        if (resource.getCapabilities() != null) {
            ActivationCapabilityType activationCapability =
                    CapabilityUtil.getCapability(resource.getCapabilities().getNative(), ActivationCapabilityType.class);
            return CapabilityUtil.getEnabledActivationStatus(activationCapability) != null;
        } else {
            return false;
        }
    }

    @VisibleForTesting
    public static boolean hasResourceNativeActivationLockoutCapability(@NotNull ResourceType resource) {
        if (resource.getCapabilities() != null) {
            ActivationCapabilityType activationCapability =
                    CapabilityUtil.getCapability(resource.getCapabilities().getNative(), ActivationCapabilityType.class);
            return CapabilityUtil.getEnabledActivationLockoutStatus(activationCapability) != null;
        } else {
            return false;
        }
    }

    /**
     * @throws ConfigurationException e.g. if the connectorName matches no configuration
     */
    public static @Nullable PrismContainerValue<ConfigurationPropertiesType> getConfigurationProperties(
            @NotNull ResourceType resource, @Nullable String connectorName) throws ConfigurationException {
        ConnectorConfigurationType config = getConnectorConfiguration(resource, connectorName);
        if (config == null) {
            return null;
        }
        //noinspection unchecked
        PrismContainer<ConfigurationPropertiesType> propertiesContainer =
                config.asPrismContainerValue().findContainer(ICF_CONFIGURATION_PROPERTIES_NAME);
        if (propertiesContainer == null || propertiesContainer.hasNoValues()) {
            return null;
        }
        return propertiesContainer.getValue();
    }

    /**
     * @throws ConfigurationException e.g. if the connectorName matches no configuration
     */
    public static @Nullable ConnectorConfigurationType getConnectorConfiguration(
            @NotNull ResourceType resource, @Nullable String connectorName) throws ConfigurationException {
        if (connectorName == null) {
            return resource.getConnectorConfiguration();
        } else {
            return getAdditionalConnectorSpec(resource, connectorName)
                    .getConnectorConfiguration();
        }
    }

    /**
     * @throws ConfigurationException e.g. if the connectorName matches no configuration
     */
    public static @NotNull ConnectorInstanceSpecificationType getAdditionalConnectorSpec(
            @NotNull ResourceType resource, @NotNull String name) throws ConfigurationException {
        var matching = resource.getAdditionalConnector().stream()
                .filter(s -> name.equals(s.getName()))
                .collect(Collectors.toList());
        return MiscUtil.extractSingletonRequired(
                matching,
                () -> new ConfigurationException("Multiple additional connectors with the name '" + name + "': " + matching),
                () -> new ConfigurationException("No additional connector with the name '" + name + "'"));
    }

    /**
     * @throws ConfigurationException e.g. if the connectorName matches no configuration
     */
    public static @Nullable PrismContainerValue<ResultsHandlerConfigurationType> getResultsHandlerConfiguration(
            @NotNull ResourceType resource, @Nullable String connectorName) throws ConfigurationException {
        ConnectorConfigurationType config = getConnectorConfiguration(resource, connectorName);
        if (config == null) {
            return null;
        }
        //noinspection unchecked
        PrismContainer<ResultsHandlerConfigurationType> handlerContainer =
                config.asPrismContainerValue().findContainer(ICF_C_RESULTS_HANDLER_CONFIGURATION);
        if (handlerContainer == null || handlerContainer.hasNoValues()) {
            return null;
        }
        return handlerContainer.getValue();
    }

    public static PrismContainer<ConnectorConfigurationType> getConfigurationContainer(ResourceType resourceType) {
        return getConfigurationContainer(resourceType.asPrismObject());
    }

    public static PrismContainer<ConnectorConfigurationType> getConfigurationContainer(PrismObject<ResourceType> resource) {
        return resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
    }

    public static boolean isDown(ResourceType resource) {
        return (resource.getOperationalState() != null
                && AvailabilityStatusType.DOWN == resource.getOperationalState().getLastAvailabilityStatus());
    }

    public static AvailabilityStatusType getLastAvailabilityStatus(ResourceType resource) {
        OperationalStateType state = resource.getOperationalState();
        return state != null ? state.getLastAvailabilityStatus() : null;
    }

    public static boolean isUp(@NotNull ResourceType resource) {
        return getLastAvailabilityStatus(resource) == AvailabilityStatusType.UP;
    }

    public static boolean isInMaintenance(ResourceType resource) {
        return getAdministrativeAvailabilityStatus(resource) == AdministrativeAvailabilityStatusType.MAINTENANCE;
    }

    public static void checkNotInMaintenance(ResourceType resource) throws MaintenanceException {
        if (isInMaintenance(resource)) {
            throw new MaintenanceException("Resource " + resource + " is in the maintenance");
        }
    }

    public static AdministrativeAvailabilityStatusType getAdministrativeAvailabilityStatus(ResourceType resource) {
        if (resource == null) {
            return null;
        }
        AdministrativeOperationalStateType adminOpState = resource.getAdministrativeOperationalState();
        return adminOpState != null ? adminOpState.getAdministrativeAvailabilityStatus() : null;
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
    public static ShadowKindType fillDefault(ShadowKindType kind) {
        return kind != null ? kind : ShadowKindType.ACCOUNT;
    }

    @NotNull
    public static String fillDefault(String intent) {
        return intent != null ? intent : SchemaConstants.INTENT_DEFAULT;
    }

    public static ResourceObjectTypeDefinitionType findObjectTypeDefinition(
            PrismObject<ResourceType> resourceObject, @Nullable ShadowKindType kind, @Nullable String intent) {
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

    // TODO specify semantics of this method
    @Deprecated
    @Nullable
    public static ObjectSynchronizationType findObjectSynchronization(
            @Nullable ResourceType resource, @Nullable ShadowKindType kind, @Nullable String intent) {
        if (resource == null) {
            return null;
        }
        for (ObjectSynchronizationType def : getAllSynchronizationBeans(resource)) {
            if (fillDefault(kind).equals(fillDefault(def.getKind()))
                    && fillDefault(intent).equals(fillDefault(def.getIntent()))) {
                return def;
            }
        }
        return null;
    }

    @NotNull
    public static QName fillDefaultFocusType(QName focusType) {
        return focusType != null ? focusType : UserType.COMPLEX_TYPE;
    }

    public static @NotNull ShadowCheckType getShadowConstraintsCheck(ResourceType resource) {
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

        return false;
    }

    public static ErrorSelectorType getConnectorErrorCriticality(ResourceType resourceType) {
        ResourceConsistencyType consistency = resourceType.getConsistency();
        return consistency != null ? consistency.getConnectorErrorCriticality() : null;
    }

    public static boolean isInMaintenance(PrismObject<ResourceType> resource) {
        return isInMaintenance(resource.asObjectable());
    }

    // Moved from GUI. Seems to be quite dependent on GUI conventions (empty values?)
    public static boolean isOutboundDefined(ResourceAttributeDefinitionType attr) {
        if (attr.asPrismContainerValue().isEmpty()) {
            return false;
        }
        return attr.getOutbound() != null
                && (attr.getOutbound().getSource() != null || attr.getOutbound().getExpression() != null);
    }

    // Moved from GUI. Seems to be quite dependent on GUI conventions (empty values?)
    public static boolean isInboundDefined(ResourceAttributeDefinitionType attr) {
        return attr.getInbound() != null
                && CollectionUtils.isNotEmpty(attr.getInbound())
                && (attr.getInbound().get(0).getTarget() != null || attr.getInbound().get(0).getExpression() != null);
    }

    public static boolean isSynchronizationDefined(ResourceType resource) {
        for (ObjectSynchronizationType syncBean : getAllSynchronizationBeans(resource)) {
            if (isEnabled(syncBean) && !syncBean.getReaction().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isEnabled(ObjectSynchronizationType syncBean) {
        return !Boolean.FALSE.equals(syncBean.isEnabled());
    }

    /**
     * FIXME! Delete this method. It no longer works with embedded synchronization configurations.
     */
    @Deprecated
    public static List<ObjectSynchronizationType> getAllSynchronizationBeans(ResourceType resource) {
        List<ObjectSynchronizationType> all = new ArrayList<>();
        if (resource.getSynchronization() != null) {
            all.addAll(resource.getSynchronization().getObjectSynchronization());
        }
        return all;
    }

    public static @Nullable ExpressionType getSynchronizationSorterExpression(@NotNull ResourceType resource) {
        SynchronizationType synchronization = resource.getSynchronization();

        if (synchronization == null) {
            return null;
        }

        ObjectSynchronizationSorterType sorter = synchronization.getObjectSynchronizationSorter();
        return sorter != null ? sorter.getExpression() : null;
    }

    public static boolean isComplete(@NotNull ResourceType resource) {
        return hasSchema(resource) && hasCapabilitiesCached(resource);
    }

    public static boolean hasCapabilitiesCached(ResourceType resource) {
        CapabilitiesType capabilities = resource.getCapabilities();
        return capabilities != null && capabilities.getCachingMetadata() != null;
    }

    public static boolean hasSchema(ResourceType resource) {
        return getResourceXsdSchemaElement(resource) != null;
    }

    public static boolean isAbstract(@NotNull ResourceType resource) {
        Boolean isAbstract = resource.isAbstract();
        if (isAbstract != null) {
            return isAbstract;
        } else {
            return isTemplate(resource);
        }
    }

    public static boolean isTemplate(@NotNull ResourceType resource) {
        return Boolean.TRUE.equals(resource.isTemplate());
    }

    /**
     * Note that this method is not 100% accurate. It's because even if the resource is expanded, currently the `super`
     * item is not removed. This may change in the future.
     */
    public static boolean doesNeedExpansion(ResourceType resource) {
        return resource.getSuper() != null;
    }

    public static boolean isDiscoveryAllowed(@NotNull ResourceType resource) {
        ResourceConsistencyType consistency = resource.getConsistency();
        return consistency == null || !Boolean.FALSE.equals(consistency.isDiscovery());
    }

    public static Duration getGroupingInterval(ResourceType resource) {
        ResourceConsistencyType resourceConsistency = resource.getConsistency();
        return resourceConsistency != null ? resourceConsistency.getOperationGroupingInterval() : null;
    }
}
