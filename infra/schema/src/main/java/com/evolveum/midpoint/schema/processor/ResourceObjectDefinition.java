/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntryOrEmpty;
import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.*;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SystemException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityType;

import static com.evolveum.midpoint.prism.Referencable.getOid;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

/**
 * A definition that describes either an object class (as fetched from the resource, optionally refined by `schemaHandling`),
 * or an object type (as defined in `schemaHandling` part of resource definition).
 *
 * Since 4.9, it is no longer a {@link ComplexTypeDefinition}; see {@link ShadowItemsComplexTypeDefinition} for explanation.
 *
 *                       ResourceObjectDefinition
 *                                   ^
 *                                   |
 *                +------------------+-------------------+
 *                |                                      |
 *     ResourceObjectClassDefinition  ResourceObjectTypeDefinition
 */
public interface ResourceObjectDefinition
    extends
        IdentifiersDefinitionStore,
        AttributeDefinitionStore,
        AssociationDefinitionStore,
        LayeredDefinition,
        FrameworkNameResolver,
        ResourceObjectInboundDefinition,
        TypeDefinition {

    /**
     * The basic information about the resource (like name, OID, selected configuration beans).
     * Replaces the hard-coded resource OID; necessary also for determination of default values for some features,
     * e.g., shadow caching, and useful for diagnostics.
     */
    @NotNull BasicResourceInformation getBasicResourceInformation();

    /** The resource OID, if known. */
    default String getResourceOid() {
        return getBasicResourceInformation().oid();
    }

    /** Returns the [structural] object class definition. */
    @NotNull ResourceObjectClassDefinition getObjectClassDefinition();

    /** Returns the [structural] native object class definition, typically obtained from the connector. */
    @NotNull NativeObjectClassDefinition getNativeObjectClassDefinition();

    /**
     * Returns the name of the object class. Always fully qualified; currently with the {@link SchemaConstants#NS_RI} namespace.
     */
    @NotNull QName getObjectClassName();

    @NotNull default String getObjectClassLocalName() {
        return QNameUtil.getLocalPartCheckingNamespace(getObjectClassName(), SchemaConstants.NS_RI);
    }

    /**
     * Returns the names of auxiliary object classes that are "statically" defined for this object type
     * (or object class, in the future).
     *
     * For dynamically composed definitions ({@link CompositeObjectDefinition} only the statically-defined ones
     * (i.e. those from the structural definition) are returned.
     */
    @NotNull Collection<QName> getConfiguredAuxiliaryObjectClassNames();

    /**
     * TODO define semantics (it's different for {@link CompositeObjectDefinition} and the others!
     */
    @NotNull Collection<? extends ResourceObjectDefinition> getAuxiliaryDefinitions();

    /**
     * TODO define semantics (it's different for {@link CompositeObjectDefinition} and the others!
     */
    boolean hasAuxiliaryObjectClass(QName expectedObjectClassName);

    /**
     * Free-form textual description of the object. It is supposed to describe
     * the object or a construct that it is attached to.
     *
     * @see ResourceObjectTypeDefinitionType#getDescription()
     */
    String getDescription();

    //region Special attributes (description, name, display name)
    /**
     * Returns the definition of description attribute of a resource object.
     *
     * Returns null if there is no description attribute.
     *
     * The exception should be never thrown unless there is some bug in the
     * code. The validation of model consistency should be done at the time of
     * schema parsing.
     *
     * NOTE: Currently seems to be not used. (Neither not set nor used.)
     */
    default @Nullable ShadowSimpleAttributeDefinition<?> getDescriptionAttribute() {
        QName name = getDescriptionAttributeName();
        return name != null ? findSimpleAttributeDefinitionStrictlyRequired(name) : null;
    }

    /**
     * Returns name of the description attribute.
     *
     * @see #getDescriptionAttribute()
     */
    @Nullable QName getDescriptionAttributeName();

    /**
     * Returns the attribute used as a visible name of the resource object.
     */
    default @Nullable ShadowSimpleAttributeDefinition<?> getNamingAttribute() {
        QName name = getNamingAttributeName();
        return name != null ? findSimpleAttributeDefinitionStrictlyRequired(name) : null;
    }

    /**
     * Returns name of the naming attribute.
     *
     * @see #getNamingAttributeName()
     */
    @Nullable QName getNamingAttributeName();

    /**
     * Returns the definition of display name attribute.
     *
     * Display name attribute specifies which resource attribute should be used
     * as title when displaying objects of a specific resource object class. It
     * must point to an attribute of String type. If not present, primary
     * identifier should be used instead (but this method does not handle this
     * default behavior).
     *
     * Returns null if there is no display name attribute.
     *
     * The exception should be never thrown unless there is some bug in the
     * code. The validation of model consistency should be done at the time of
     * schema parsing.
     *
     * NOTE: Currently seems to be not used. (Neither not set nor used.)
     */
    default ShadowSimpleAttributeDefinition<?> getDisplayNameAttribute() {
        QName name = getDisplayNameAttributeName();
        return name != null ? findSimpleAttributeDefinitionStrictlyRequired(name) : null;
    }

    /**
     * Returns name of the display name attribute.
     */
    @Nullable QName getDisplayNameAttributeName();
    //endregion

    //region Fetching various information from the definition bean

    /**
     * Returns the delineation of the set of objects belonging to this object type.
     *
     * Note that this tells only about information stored right in the definition bean, i.e. legacy configuration
     * is not provided here. The complete picture is provided by {@link SynchronizationPolicy#getDelineation()}.
     */
    @NotNull ResourceObjectTypeDelineation getDelineation();

    /**
     * The definition of base context (resource object container). This object will be used
     * as a base for searches for objects of this type.
     *
     * @see ResourceObjectTypeDefinitionType#getBaseContext()
     */
    ResourceObjectReferenceType getBaseContext();

    /**
     * Definition of search hierarchy scope. It specifies how "deep" the search should go into the object hierarchy.
     *
     * @see ResourceObjectTypeDefinitionType#getSearchHierarchyScope()
     */
    SearchHierarchyScope getSearchHierarchyScope();

    /**
     * Returns compiled patterns denoting protected objects or other kinds of marks.
     *
     * Use only in the parsed state.
     *
     * @see ResourceObjectTypeDefinitionType#getProtected()
     * @see ResourceObjectTypeDefinitionType#getMarking()
     */
    @NotNull ShadowMarkingRules getShadowMarkingRules();

    /**
     * @see ResourceObjectTypeDefinitionType#getAuxiliaryObjectClassMappings()
     */
    ResourceBidirectionalMappingAndDefinitionType getAuxiliaryObjectClassMappings();

    /**
     * @see ResourceObjectTypeDefinitionType#getSecurityPolicyRef()
     */
    ObjectReferenceType getSecurityPolicyRef();

    /**
     * @see ResourceObjectTypeDefinitionType#getMultiplicity()
     */
    ResourceObjectMultiplicityType getObjectMultiplicity();

    /**
     * @see ResourceObjectTypeDefinitionType#getProjection()
     */
    ProjectionPolicyType getProjectionPolicy();

    /**
     * See {@link ResourceObjectTypeDefinitionType#getCredentials()}
     */
    @Nullable ResourcePasswordDefinitionType getPasswordDefinition();

    /**
     * See {@link ResourceObjectTypeDefinitionType#getBehavior()}
     */
    @Nullable ResourceLastLoginTimestampDefinitionType getLastLoginTimestampDefinition();

    /**
     * TODO Rarely used, consider removing from the interface
     */
    default @Nullable AttributeFetchStrategyType getPasswordFetchStrategy() {
        ResourcePasswordDefinitionType password = getPasswordDefinition();
        if (password == null) {
            return AttributeFetchStrategyType.IMPLICIT;
        }
        if (password.getFetchStrategy() == null) {
            return AttributeFetchStrategyType.IMPLICIT;
        }
        return password.getFetchStrategy();
    }

    default @NotNull List<MappingType> getPasswordInbound() {
        ResourcePasswordDefinitionType password = getPasswordDefinition();
        return password != null ? password.getInbound() : List.of();
    }

    default @NotNull List<MappingType> getPasswordOutbound() {
        ResourcePasswordDefinitionType password = getPasswordDefinition();
        return password != null ? password.getOutbound() : List.of();
    }

    /**
     * @see ResourceObjectTypeDefinitionType#getActivation()
     */
    ResourceActivationDefinitionType getActivationSchemaHandling();

    /**
     * TODO Rarely used, consider removing from the interface
     */
    default ResourceBidirectionalMappingType getActivationBidirectionalMappingType(ItemName itemName) {
        return ResourceObjectDefinitionUtil.getActivationBidirectionalMappingType(
                getActivationSchemaHandling(), itemName);
    }

    /**
     * TODO Rarely used, consider removing from the interface
     */
    default @Nullable AttributeFetchStrategyType getActivationFetchStrategy(ItemName itemName) {
        ResourceBidirectionalMappingType biType = getActivationBidirectionalMappingType(itemName);
        if (biType == null) {
            return AttributeFetchStrategyType.IMPLICIT;
        }
        if (biType.getFetchStrategy() == null) {
            return AttributeFetchStrategyType.IMPLICIT;
        }
        return biType.getFetchStrategy();
    }

    default @Nullable AttributeFetchStrategyType getLastLoginTimestampFetchStrategy() {
        ResourceLastLoginTimestampDefinitionType definition = getLastLoginTimestampDefinition();
        if (definition == null) {
            return AttributeFetchStrategyType.IMPLICIT;
        }
        if (definition.getFetchStrategy() == null) {
            return AttributeFetchStrategyType.IMPLICIT;
        }
        return definition.getFetchStrategy();
    }

    /**
     * Specifies volatility of this type of resource objects, i.e. whether such an object
     * can change when midPoint is not looking.
     *
     * @see ResourceObjectTypeDefinitionType#getVolatility()
     */
    @NotNull ResourceObjectVolatilityType getVolatility();

    /**
     * Returns the phases in which inbound mappings are evaluated by default.
     *
     * @see ResourceObjectTypeDefinitionType#getMappingsEvaluation()
     */
    @Nullable DefaultInboundMappingEvaluationPhasesType getDefaultInboundMappingEvaluationPhases();

    /**
     * What lifecycle state is the (refined) object class or object type definition?
     * Raw object class definition should always return `null` here.
     *
     * @see ResourceObjectTypeDefinitionType#getLifecycleState()
     */
    @Nullable String getLifecycleState();
    //endregion

    //region Creating artifacts (shadow, query, PCD, instances, ...)
    /**
     * Creates a query for obtaining shadows related to this object class or object type.
     *
     * The current implementations return either:
     *
     * - a combination of resource OID + object class name, or
     * - a combination of resource OID + kind + intent.
     */
    @NotNull ObjectQuery createShadowSearchQuery(String resourceOid);

    // TODO why are all three methods below named "createBlankShadow", but only the 3rd one adds the kind/intent?
    //  The reason is that the first two are called from the context where there should be no kind/intent present.
    //  But that stinks. Something is broken here. We should define what "blank shadow" is. E.g., should aux OCs be there?

    /**
     * Creates a blank, empty shadow.
     * It contains only the object class name and resource OID.
     *
     * Kind/intent are NOT set, because the definition may be a "default type definition for given object class"
     * (which is sadly still supported); and we do not want to create typed shadows in such cases.
     *
     * {@link ShadowBuilder#withDefinition(ResourceObjectDefinition)} provides kind and intent.
     */
    default AbstractShadow createBlankShadow() {
        try {
            var shadowBean = getPrismObjectDefinition().instantiate().asObjectable();
            shadowBean.setObjectClass(getObjectClassName());
            var resourceOid = getResourceOid();
            if (resourceOid != null) {
                shadowBean.resourceRef(resourceOid, ResourceType.COMPLEX_TYPE);
            }
            return AbstractShadow.of(shadowBean);
        } catch (SchemaException e) {
            throw SystemException.unexpected(e, "while instantiating shadow from " + this);
        }
    }

    /** As {@link #createBlankShadow()} but with the specified primary identifier. */
    default AbstractShadow createBlankShadowWithPrimaryId(@NotNull Object primaryIdentifierValue) throws SchemaException {
        var shadow = createBlankShadow();
        ShadowUtil.addPrimaryIdentifierValue(shadow.getBean(), primaryIdentifierValue);
        return shadow;
    }

    /** As {@link #createBlankShadow()} but having the correct resource OID, kind/intent (if applicable), and tag set. */
    default AbstractShadow createBlankShadowWithTag(String tag) {
        var shadow = createBlankShadow();
        shadow.getBean().tag(tag);
        return shadow;
    }

    /**
     * Returns a prism definition for the prism object/objects carrying the resource object/objects.
     */
    PrismObjectDefinition<ShadowType> getPrismObjectDefinition();

    /**
     * Creates {@link ShadowAttributesContainerDefinition} with this definition as a complex type definition.
     */
    default @NotNull ShadowAttributesContainerDefinition toShadowAttributesContainerDefinition() {
        return new ShadowAttributesContainerDefinitionImpl(ShadowType.F_ATTRIBUTES, getAttributesComplexTypeDefinition());
    }

    default @NotNull ShadowAssociationsContainerDefinition toShadowAssociationsContainerDefinition() {
        return new ShadowAssociationsContainerDefinitionImpl(ShadowType.F_ASSOCIATIONS, getAssociationsComplexTypeDefinition());
    }

    //endregion

    //region Capabilities
    /**
     * Checks the presence of capability in:
     *
     * 1. resource object definition (applicable only to resource object _type_ definitions),
     * 2. additional connectors in resource (only if enabled there),
     * 3. the main connector.
     *
     * Returns the present capability, but only if it's enabled.
     */
    <T extends CapabilityType> T getEnabledCapability(@NotNull Class<T> capabilityClass, ResourceType resource);
    //endregion

    //region Diagnostics and administration

    void trimAttributesTo(@NotNull Collection<ItemPath> paths);
    /**
     * Executes some basic checks on this object type.
     * Moved from `validateObjectClassDefinition()` method in {@link ResourceTypeUtil}.
     *
     * TODO review this method
     */
    void validate() throws SchemaException;

    /**
     * Return a human readable name of this class suitable for logs.
     */
    String getDebugDumpClassName();

    /**
     * TODO
     */
    String getHumanReadableName();

    /** Very short identification, like the object class local name or the kind/intent pair. */
    @NotNull String getShortIdentification();

    /**
     * Returns a mutable definition.
     *
     * BEWARE, the mutable {@link CompositeObjectDefinition} is significantly slower than its immutable counterpart.
     * See MID-9156.
     */
    @NotNull ResourceObjectDefinition clone();

    /**
     * Returns a mutable definition.
     *
     * BEWARE, the mutable {@link CompositeObjectDefinition} is significantly slower than its immutable counterpart.
     * See MID-9156.
     */
    @NotNull
    ResourceObjectDefinition deepClone(@NotNull DeepCloneOperation operation);

    //endregion

    //region Other
    /**
     * Returns the configuration bean for this object type or class.
     */
    @NotNull ResourceObjectTypeDefinitionType getDefinitionBean();

    /**
     * Creates a layer-specific mutable version of this definition.
     */
    @NotNull ResourceObjectDefinition forLayerMutable(@NotNull LayerType layer);

    /**
     * As {@link #forLayerMutable(LayerType)} but returns immutable definition.
     */
    @NotNull ResourceObjectDefinition forLayerImmutable(@NotNull LayerType layer);

    /**
     * Replaces a definition for given item name with a provided one.
     */
    void replaceAttributeDefinition(@NotNull QName itemName, @Nullable ItemDefinition<?> newDefinition);

    default void replaceAttributeDefinition(@NotNull ItemDefinition<?> newDefinition) {
        replaceAttributeDefinition(newDefinition.getItemName(), newDefinition);
    }

    /**
     * Returns true if the type definition matches specified object class name.
     * OC name of `null` matches all definitions.
     */
    default boolean matchesObjectClassName(@Nullable QName name) {
        return name == null || QNameUtil.match(name, getObjectClassName());
    }

    /** Is this definition bound to a specific resource type? If yes, this method returns its identification. */
    @Nullable ResourceObjectTypeIdentification getTypeIdentification();

    default @NotNull ResourceObjectDefinitionIdentification getIdentification() {
        return ResourceObjectDefinitionIdentification.create(
                getObjectClassLocalName(), getTypeIdentification());
    }

    /** Is this definition bound to a specific resource type? If yes, this method returns its definition. */
    @Nullable ResourceObjectTypeDefinition getTypeDefinition();

    /**
     * Returns true if this definition can be considered as a default for the specified kind.
     *
     * Normally, for a type definition it means that it is marked as "default for a kind" and has the specified kind.
     * But there is a special case of {@link ResourceObjectClassDefinition} with
     * {@link ResourceObjectClassDefinition#isDefaultAccountDefinition()} being `true`. It is considered to be
     * the default for {@link ShadowKindType#ACCOUNT}.
     *
     * Use with care!
     */
    @Experimental
    boolean isDefaultFor(@NotNull ShadowKindType kind);

    static void assertAttached(ResourceObjectDefinition resourceObjectDefinition) {
        if (resourceObjectDefinition != null) {
            resourceObjectDefinition.assertAttached();
        }
    }

    default void assertAttached() {
        stateCheck(
                getBasicResourceInformation() != null,
                "Object definition %s is not attached to a resource", this);
    }
    //endregion

    /** Call {@link #getPrismObjectDefinition()} for the cached version. */
    default @NotNull PrismObjectDefinition<ShadowType> toPrismObjectDefinition() {
        return ObjectFactory.constructObjectDefinition(
                toShadowAttributesContainerDefinition(),
                toShadowAssociationsContainerDefinition());
    }

    default @NotNull ResourceObjectDefinition composite(Collection<? extends ResourceObjectDefinition> auxiliaryDefinitions) {
        if (auxiliaryDefinitions.isEmpty()) {
            return this;
        } else {
            return CompositeObjectDefinition.of(this, auxiliaryDefinitions);
        }
    }

//    /** Returns both attribute and association definitions. */
//    @NotNull Collection<? extends ShadowAttributeDefinition<?, ?>> getShadowItemDefinitions();

    default @NotNull ShadowAttributesComplexTypeDefinition getAttributesComplexTypeDefinition() {
        return ShadowAttributesComplexTypeDefinitionImpl.of(this);
    }

    default @NotNull ShadowAttributesComplexTypeDefinition getSimpleAttributesComplexTypeDefinition() {
        return ShadowSimpleAttributesComplexTypeDefinitionImpl.of(this);
    }

    default @NotNull ShadowAttributesComplexTypeDefinition getReferenceAttributesComplexTypeDefinition() {
        return ShadowReferenceAttributesComplexTypeDefinitionImpl.of(this);
    }

    default @NotNull ShadowAssociationsComplexTypeDefinition getAssociationsComplexTypeDefinition() {
        return ShadowAssociationsComplexTypeDefinitionImpl.of(this);
    }

    @Override
    default @Nullable Class<?> getCompileTimeClass() {
        return null;
    }

    @Override
    default @Nullable QName getSuperType() {
        return null;
    }

    @Override
    default @NotNull Collection<TypeDefinition> getStaticSubTypes() {
        return List.of();
    }

    @Override
    default Integer getInstantiationOrder() {
        return null;
    }

    @Override
    default boolean canRepresent(QName typeName) {
        return QNameUtil.match(typeName, getTypeName());
    }

    @Override
    @NotNull
    default List<? extends ShadowReferenceAttributeDefinition> getReferenceAttributeDefinitions() {
        return AttributeDefinitionStore.super.getReferenceAttributeDefinitions();
    }

    @Override
    default ShadowReferenceAttributeDefinition findReferenceAttributeDefinition(QName name) {
        return AttributeDefinitionStore.super.findReferenceAttributeDefinition(name);
    }

    default @NotNull ResourceObjectIdentification.WithPrimary createPrimaryIdentification(@NotNull Object identifierRealValue)
            throws SchemaException {
        return ResourceObjectIdentification.withPrimary(
                this,
                getPrimaryIdentifierRequired().instantiateFromRealValue(identifierRealValue),
                List.of());
    }

    default @NotNull S_FilterEntryOrEmpty queryFor() {
        return PrismContext.get().queryFor(ShadowType.class, new Resource.ResourceItemDefinitionResolver(this));
    }

    default Collection<? extends ShadowAssociationDefinition> getAssociationDefinitionsFor(@NotNull ItemName refAttrName) {
        return getAssociationDefinitions().stream()
                .filter(assocDef -> assocDef.getReferenceAttributeDefinition().getItemName().equals(refAttrName))
                .toList();
    }

    /**
     * Returns shadow caching policy determined by the application of resource-level definitions down to the specific
     * object type/class definition (using bean merging).
     *
     * The returned value has all the defaults applied.
     *
     * Throws an exception for unattached raw object class definitions.
     */
    @NotNull ShadowCachingPolicyType getEffectiveShadowCachingPolicy();

    default boolean isCachingEnabled() {
        return Objects.requireNonNull(getEffectiveShadowCachingPolicy().getCachingStrategy()) == CachingStrategyType.PASSIVE;
    }

    default boolean isActivationCached() {
        return isCachingEnabled()
                && getEffectiveShadowCachingPolicy().getScope().getActivation() != ShadowItemsCachingScopeType.NONE;
    }

    default boolean isAuxiliaryObjectClassPropertyCached() {
        return isCachingEnabled()
                && getEffectiveShadowCachingPolicy().getScope().getAuxiliaryObjectClasses() != ShadowItemsCachingScopeType.NONE;
    }

    default boolean areCredentialsCached() {

        // For simplicity, we first check the legacy password caching strategy.
        //
        // Although it is not logical at first sight, it is much easier than checking if the credentials caching is explicitly
        // enabled/disabled via the modern way.

        var legacy = getLegacyPasswordCachingStrategy();
        if (legacy != null) {
            return legacy != CachingStrategyType.NONE;
        }

        return false; // temporary, see MID-10050

//        return isCachingEnabled()
//                && getEffectiveShadowCachingPolicy().getScope().getCredentials() != ShadowItemsCachingScopeType.NONE;
    }

    private CachingStrategyType getLegacyPasswordCachingStrategy() {
        ResourcePasswordDefinitionType passwordDefinition = getPasswordDefinition();
        if (passwordDefinition == null) {
            return null;
        }
        CachingPolicyType passwordCachingPolicy = passwordDefinition.getCaching();
        if (passwordCachingPolicy == null) {
            return null;
        }
        return passwordCachingPolicy.getCachingStrategy();
    }

    default boolean isEffectivelyCached(ItemPath itemPath) {
        if (itemPath.startsWith(ShadowType.F_ATTRIBUTES)) {
            var attrDef = findAttributeDefinition(itemPath.rest().asSingleNameOrFail());
            return attrDef != null && attrDef.isEffectivelyCached(this);
        } else if (itemPath.startsWith(ShadowType.F_ASSOCIATIONS)) {
            var assocDef = findAssociationDefinition(itemPath.rest().asSingleNameOrFail());
            return assocDef != null && assocDef.getReferenceAttributeDefinition().isEffectivelyCached(this);
        } else if (itemPath.startsWith(ShadowType.F_ACTIVATION)) {
            return isActivationCached(); // FIXME what about sub-items that are always stored in the shadow?
        } else if (itemPath.startsWith(ShadowType.F_CREDENTIALS)) {
            return areCredentialsCached(); // FIXME what about sub-items that are always stored in the shadow?
        } else {
            return itemPath.equivalent(ShadowType.F_AUXILIARY_OBJECT_CLASS);
        }
    }

    default @Nullable String getDefaultOperationPolicyOid(@NotNull TaskExecutionMode mode) throws ConfigurationException {
        var bean = getDefinitionBean();
        var oids = bean.getDefaultOperationPolicy().stream()
                .filter(policy -> SimulationUtil.isVisible(policy.getLifecycleState(), mode))
                .map(policy -> getOid(policy.getPolicyRef()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return MiscUtil.extractSingleton(
                oids,
                () -> new ConfigurationException(
                        "Multiple OIDs for default operation policy in %s for %s: %s".formatted(this, mode, oids)));
    }
}
