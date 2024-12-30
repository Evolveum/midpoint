package com.evolveum.midpoint.schema.processor.deleg;

import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.util.AbstractShadow;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityType;

public interface ResourceObjectDefinitionDelegator extends ResourceObjectDefinition {

    ResourceObjectDefinition delegate();

    @Override
    @NotNull
    default BasicResourceInformation getBasicResourceInformation() {
        return delegate().getBasicResourceInformation();
    }

    @Override
    default @NotNull List<? extends ShadowSimpleAttributeDefinition<?>> getSimpleAttributeDefinitions() {
        return delegate().getSimpleAttributeDefinitions();
    }

//    @Override
//    default @NotNull Collection<? extends ShadowAttributeDefinition<?, ?>> getShadowItemDefinitions() {
//        return delegate().getShadowItemDefinitions();
//    }

    @Override
    default <T> @Nullable ShadowSimpleAttributeDefinition<T> findSimpleAttributeDefinition(QName name, boolean caseInsensitive) {
        return delegate().findSimpleAttributeDefinition(name, caseInsensitive);
    }

    @Override
    default <T> ShadowSimpleAttributeDefinition<T> findSimpleAttributeDefinition(String name) {
        return delegate().findSimpleAttributeDefinition(name);
    }

    @Override
    default @NotNull Collection<? extends ShadowSimpleAttributeDefinition<?>> getPrimaryIdentifiers() {
        return delegate().getPrimaryIdentifiers();
    }

    @Override
    default boolean isPrimaryIdentifier(QName attrName) {
        return delegate().isPrimaryIdentifier(attrName);
    }

    @Override
    default @NotNull Collection<? extends ShadowSimpleAttributeDefinition<?>> getSecondaryIdentifiers() {
        return delegate().getSecondaryIdentifiers();
    }

    @Override
    default boolean isSecondaryIdentifier(QName attrName) {
        return delegate().isSecondaryIdentifier(attrName);
    }

    @Override
    default ShadowSimpleAttributeDefinition<?> getDescriptionAttribute() {
        return delegate().getDescriptionAttribute();
    }

    @Override
    default ShadowSimpleAttributeDefinition<?> getDisplayNameAttribute() {
        return delegate().getDisplayNameAttribute();
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
    @Nullable
    default ResourceLastLoginTimestampDefinitionType getLastLoginTimestampDefinition() {
        return delegate().getLastLoginTimestampDefinition();
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
    default AbstractShadow createBlankShadowWithTag(String tag) {
        return delegate().createBlankShadowWithTag(tag);
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
    default @NotNull List<? extends ShadowReferenceAttributeDefinition> getReferenceAttributeDefinitions() {
        return delegate().getReferenceAttributeDefinitions();
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
    default void replaceAttributeDefinition(@NotNull QName itemName, @Nullable ItemDefinition<?> newDefinition) {
        delegate().replaceAttributeDefinition(itemName, newDefinition);
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
    default @NotNull ShadowMarkingRules getShadowMarkingRules() {
        return delegate().getShadowMarkingRules();
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
    }

    @Override
    default @NotNull Collection<QName> getConfiguredAuxiliaryObjectClassNames() {
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
    default @NotNull NativeObjectClassDefinition getNativeObjectClassDefinition() {
        return delegate().getNativeObjectClassDefinition();
    }

    @Override
    default @NotNull ObjectQuery createShadowSearchQuery(String resourceOid) {
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

    @Override
    default @NotNull String getShortIdentification() {
        return delegate().getShortIdentification();
    }

    @Override
    @Nullable
    default ItemName resolveFrameworkName(@NotNull String frameworkName) {
        return delegate().resolveFrameworkName(frameworkName);
    }

    @Override
    default ItemInboundDefinition getSimpleAttributeInboundDefinition(ItemName itemName) throws SchemaException {
        return delegate().getSimpleAttributeInboundDefinition(itemName);
    }

    @Override
    default ItemInboundDefinition getReferenceAttributeInboundDefinition(ItemName itemName) throws SchemaException {
        return delegate().getReferenceAttributeInboundDefinition(itemName);
    }

    @Override
    @NotNull
    default FocusSpecification getFocusSpecification() {
        return delegate().getFocusSpecification();
    }

    @Override
    default @NotNull Collection<? extends SynchronizationReactionDefinition> getSynchronizationReactions() {
        return delegate().getSynchronizationReactions();
    }

    @Override
    default CorrelationDefinitionType getCorrelation() {
        return delegate().getCorrelation();
    }
}
