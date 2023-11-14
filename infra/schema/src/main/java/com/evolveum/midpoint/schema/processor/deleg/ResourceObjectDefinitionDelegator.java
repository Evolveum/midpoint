package com.evolveum.midpoint.schema.processor.deleg;

import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.processor.ResourceAssociationDefinition;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.processor.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.deleg.ComplexTypeDefinitionDelegator;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.exception.SchemaException;

public interface ResourceObjectDefinitionDelegator extends ComplexTypeDefinitionDelegator, ResourceObjectDefinition {

    @Override
    ResourceObjectDefinition delegate();

    @Override
    @Nullable
    default BasicResourceInformation getBasicResourceInformation() {
        return delegate().getBasicResourceInformation();
    }

    @Override
    default @NotNull List<? extends ResourceAttributeDefinition<?>> getAttributeDefinitions() {
        return delegate().getAttributeDefinitions();
    }

    @Override
    default @Nullable ResourceAttributeDefinition<?> findAttributeDefinition(QName name, boolean caseInsensitive) {
        return delegate().findAttributeDefinition(name, caseInsensitive);
    }

    @Override
    default ResourceAttributeDefinition<?> findAttributeDefinition(String name) {
        return delegate().findAttributeDefinition(name);
    }

    @Override
    default @NotNull Collection<? extends ResourceAttributeDefinition<?>> getPrimaryIdentifiers() {
        return delegate().getPrimaryIdentifiers();
    }

    @Override
    default boolean isPrimaryIdentifier(QName attrName) {
        return delegate().isPrimaryIdentifier(attrName);
    }

    @Override
    default @NotNull Collection<? extends ResourceAttributeDefinition<?>> getSecondaryIdentifiers() {
        return delegate().getSecondaryIdentifiers();
    }

    @Override
    default boolean isSecondaryIdentifier(QName attrName) {
        return delegate().isSecondaryIdentifier(attrName);
    }

    @Override
    default ResourceAttributeDefinition<?> getDescriptionAttribute() {
        return delegate().getDescriptionAttribute();
    }

    @Override
    default ResourceAttributeDefinition<?> getNamingAttribute() {
        return delegate().getNamingAttribute();
    }

    @Override
    default ResourceAttributeDefinition<?> getDisplayNameAttribute() {
        return delegate().getDisplayNameAttribute();
    }

    @Override
    default ResourceAttributeContainerDefinition toResourceAttributeContainerDefinition() {
        return delegate().toResourceAttributeContainerDefinition();
    }

    @Override
    default ResourceAttributeContainerDefinition toResourceAttributeContainerDefinition(QName elementName) {
        return delegate().toResourceAttributeContainerDefinition(elementName);
    }

    @Override
    default PrismObjectDefinition<ShadowType> getPrismObjectDefinition() {
        return delegate().getPrismObjectDefinition();
    }

    @Override
    default @Nullable ResourcePasswordDefinitionType getPasswordDefinition() {
        return delegate().getPasswordDefinition();
    }

    @Override
    default @NotNull ResourceObjectClassDefinition getObjectClassDefinition() {
        return delegate().getObjectClassDefinition();
    }

    @Override
    @NotNull
    default QName getObjectClassName() {
        return delegate().getObjectClassName();
    }

    @Override
    @Nullable
    default QName getDescriptionAttributeName() {
        return delegate().getDescriptionAttributeName();
    }

    @Override
    @Nullable
    default QName getNamingAttributeName() {
        return delegate().getNamingAttributeName();
    }

    @Override
    @Nullable
    default QName getDisplayNameAttributeName() {
        return delegate().getDisplayNameAttributeName();
    }

    @Override
    default PrismObject<ShadowType> createBlankShadow(String resourceOid, String tag) {
        return delegate().createBlankShadow(resourceOid, tag);
    }

    @Override
    default <T extends CapabilityType> T getEnabledCapability(@NotNull Class<T> capabilityClass, ResourceType resource) {
        return delegate().getEnabledCapability(capabilityClass, resource);
    }

    @Override
    default String getHumanReadableName() {
        return delegate().getHumanReadableName();
    }

    @Override
    @NotNull
    default Collection<ResourceAssociationDefinition> getAssociationDefinitions() {
        return delegate().getAssociationDefinitions();
    }
    @Override
    @NotNull
    default Collection<QName> getPrimaryIdentifiersNames() {
        return delegate().getPrimaryIdentifiersNames();
    }

    @Override
    @NotNull
    default Collection<QName> getSecondaryIdentifiersNames() {
        return delegate().getSecondaryIdentifiersNames();
    }

    @Override
    default String getDebugDumpClassName() {
        return delegate().getDebugDumpClassName();
    }

    @Override
    default @NotNull ResourceObjectDefinition forLayerMutable(@NotNull LayerType layer) {
        return delegate().forLayerMutable(layer);
    }

    @Override
    default @NotNull ResourceObjectDefinition forLayerImmutable(@NotNull LayerType layer) {
        return delegate().forLayerImmutable(layer);
    }

    @Override
    default ObjectReferenceType getSecurityPolicyRef() {
        return delegate().getSecurityPolicyRef();
    }

    @Override
    default void replaceDefinition(@NotNull QName itemName, @Nullable ItemDefinition<?> newDefinition) {
        delegate().replaceDefinition(itemName, newDefinition);
    }

    @Override
    @NotNull
    default LayerType getCurrentLayer() {
        return delegate().getCurrentLayer();
    }

    @Override
    default String getDescription() {
        return delegate().getDescription();
    }

    @Override
    default ResourceObjectMultiplicityType getObjectMultiplicity() {
        return delegate().getObjectMultiplicity();
    }

    @Override
    default ProjectionPolicyType getProjectionPolicy() {
        return delegate().getProjectionPolicy();
    }

    @Override
    default boolean hasAuxiliaryObjectClass(QName expectedObjectClassName) {
        return delegate().hasAuxiliaryObjectClass(expectedObjectClassName);
    }

    @Override
    default ResourceBidirectionalMappingAndDefinitionType getAuxiliaryObjectClassMappings() {
        return delegate().getAuxiliaryObjectClassMappings();
    }

    @Override
    default @NotNull Collection<ResourceObjectPattern> getProtectedObjectPatterns() {
        return delegate().getProtectedObjectPatterns();
    }

    @Override
    default ResourceActivationDefinitionType getActivationSchemaHandling() {
        return delegate().getActivationSchemaHandling();
    }

    @Override
    @NotNull
    default ResourceObjectTypeDelineation getDelineation() {
        return delegate().getDelineation();
    }

    @Override
    default ResourceObjectReferenceType getBaseContext() {
        return delegate().getBaseContext();
    }

    @Override
    default SearchHierarchyScope getSearchHierarchyScope() {
        return delegate().getSearchHierarchyScope();
    }

    @Override
    @NotNull
    default ResourceObjectVolatilityType getVolatility() {
        return delegate().getVolatility();
    }

    @Override
    @Nullable
    default DefaultInboundMappingEvaluationPhasesType getDefaultInboundMappingEvaluationPhases() {
        return delegate().getDefaultInboundMappingEvaluationPhases();
    }

    @Override
    @Nullable
    default String getLifecycleState() {
        return delegate().getLifecycleState();
    };

    @Override
    default Collection<QName> getConfiguredAuxiliaryObjectClassNames() {
        return delegate().getConfiguredAuxiliaryObjectClassNames();
    }

    @Override
    default void validate() throws SchemaException {
        delegate().validate();
    }

    @Override
    default @NotNull ResourceObjectTypeDefinitionType getDefinitionBean() {
        return delegate().getDefinitionBean();
    }

    @Override
    default @NotNull ResourceObjectClassDefinition getRawObjectClassDefinition() {
        return delegate().getRawObjectClassDefinition();
    }

    @Override
    default @NotNull ObjectQuery createShadowSearchQuery(String resourceOid) throws SchemaException {
        return delegate().createShadowSearchQuery(resourceOid);
    }

    @Override
    @Nullable
    default ResourceObjectTypeIdentification getTypeIdentification() {
        return delegate().getTypeIdentification();
    }

    @Override
    @Nullable
    default ResourceObjectTypeDefinition getTypeDefinition() {
        return delegate().getTypeDefinition();
    }

    @Override
    default boolean isDefaultFor(@NotNull ShadowKindType kind) {
        return delegate().isDefaultFor(kind);
    }

    @Override
    default @NotNull ShadowCachingPolicyType getEffectiveShadowCachingPolicy() {
        return delegate().getEffectiveShadowCachingPolicy();
    }
}
