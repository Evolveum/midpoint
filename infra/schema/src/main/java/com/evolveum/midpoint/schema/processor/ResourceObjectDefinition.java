/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.util.ResourceTypeUtil;

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

/**
 * A definition that describes either an object class (as fetched from the resource, optionally refined by `schemaHandling`),
 * or an object type (as defined in `schemaHandling` part of resource definition).
 *
 * It is used as a common interface to both "raw" and "refined" definitions. (Raw definitions are used e.g. in cases
 * when there is no `schemaHandling` for given object class, or for the resource as a whole.)
 *
 * Note: Before midPoint 4.5, this interface was known as `ObjectClassComplexTypeDefinition`.
 * So the hierarchy was:
 *
 *                          ComplexTypeDefinition
 *                                   ^
 *                                   |
 *                    ObjectClassComplexTypeDefinition
 *                                   ^
 *                                   |
 *                      RefinedObjectClassDefinition
 *
 * Now the hierarchy is like this:
 *
 *                          ComplexTypeDefinition
 *                                   ^
 *                                   |
 *                       ResourceObjectDefinition
 *                                   ^
 *                                   |
 *                +------------------+-------------------+
 *                |                                      |
 *     ResourceObjectClassDefinition  ResourceObjectTypeDefinition
 *
 * This change eliminates e.g. the need to create "artificial" refined object class definitions just to allow
 * model and provisioning modules to work with object classes not described in schema handling. (Confusion stemmed
 * e.g. from the fact that `RefinedObjectClassDefinition` had to have kind/intent. This is now fixed.)
 */
public interface ResourceObjectDefinition
    extends
        ComplexTypeDefinition,
        IdentifiersDefinitionStore,
        AttributeDefinitionStore,
        AssociationDefinitionStore, // no-op for object class definitions
        LayeredDefinition {

    /**
     * Returns the (raw or refined) object class definition.
     *
     * It is either this object itself (for object classes), or the linked object class definition (for object types).
     */
    @NotNull ResourceObjectClassDefinition getObjectClassDefinition();

    /**
     * Returns the raw object class definition.
     */
    @NotNull ResourceObjectClassDefinition getRawObjectClassDefinition();

    /**
     * Returns the name of the object class.
     */
    @NotNull QName getObjectClassName();

    /**
     * Returns the names of auxiliary object classes that are "statically" defined for this object type
     * (or object class, in the future).
     *
     * For dynamically composed definitions ({@link CompositeObjectDefinition} only the statically-defined ones
     * (i.e. those from the structural definition) are returned.
     */
    @Experimental
    Collection<QName> getConfiguredAuxiliaryObjectClassNames();

    /**
     * TODO define semantics (it's different for {@link CompositeObjectDefinition} and the others!
     */
    @NotNull Collection<ResourceObjectDefinition> getAuxiliaryDefinitions();

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
    default @Nullable ResourceAttributeDefinition<?> getDescriptionAttribute() {
        QName name = getDescriptionAttributeName();
        return name != null ? findAttributeDefinitionStrictlyRequired(name) : null;
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
    default @Nullable ResourceAttributeDefinition<?> getNamingAttribute() {
        QName name = getNamingAttributeName();
        return name != null ? findAttributeDefinitionStrictlyRequired(name) : null;
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
    default ResourceAttributeDefinition<?> getDisplayNameAttribute() {
        QName name = getDisplayNameAttributeName();
        return name != null ? findAttributeDefinitionStrictlyRequired(name) : null;
    }

    /**
     * Returns name of the display name attribute.
     *
     * @see #getDisplayNameAttribute()
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
     * Returns compiled patterns denoting protected objects.
     *
     * @see ResourceObjectTypeDefinitionType#getProtected()
     */
    @NotNull Collection<ResourceObjectPattern> getProtectedObjectPatterns();

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
        ResourceActivationDefinitionType activationSchemaHandling = getActivationSchemaHandling();
        if (activationSchemaHandling == null) {
            return null;
        }
        if (QNameUtil.match(ActivationType.F_ADMINISTRATIVE_STATUS, itemName)) {
            return activationSchemaHandling.getAdministrativeStatus();
        } else if (QNameUtil.match(ActivationType.F_VALID_FROM, itemName)) {
            return activationSchemaHandling.getValidFrom();
        } else if (QNameUtil.match(ActivationType.F_VALID_TO, itemName)) {
            return activationSchemaHandling.getValidTo();
        } else if (QNameUtil.match(ActivationType.F_LOCKOUT_STATUS, itemName)) {
            return activationSchemaHandling.getLockoutStatus();
        } else if (QNameUtil.match(ActivationType.F_LOCKOUT_EXPIRATION_TIMESTAMP, itemName)) {
            return null; // todo implement this
        } else {
            throw new IllegalArgumentException("Unknown activation property " + itemName);
        }
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
    @NotNull ObjectQuery createShadowSearchQuery(String resourceOid) throws SchemaException;

    /**
     * Creates a blank {@link ShadowType} object, with the attributes container having appropriate definition.
     */
    PrismObject<ShadowType> createBlankShadow(String resourceOid, String tag);

    /**
     * Returns a prism definition for the prism object/objects carrying the resource object/objects.
     */
    PrismObjectDefinition<ShadowType> getPrismObjectDefinition();

    /**
     * Creates {@link ResourceAttributeContainerDefinition} with this definition as a complex type definition.
     */
    default ResourceAttributeContainerDefinition toResourceAttributeContainerDefinition() {
        return toResourceAttributeContainerDefinition(ShadowType.F_ATTRIBUTES);
    }

    /**
     * Creates {@link ResourceAttributeContainerDefinition} (with given item name) with this definition
     * as a complex type definition.
     */
    default ResourceAttributeContainerDefinition toResourceAttributeContainerDefinition(QName elementName) {
        return ObjectFactory.createResourceAttributeContainerDefinition(elementName, this);
    }

    /**
     * Creates a {@link ResourceAttributeContainer} instance with this definition as its complex type definition.
     */
    default ResourceAttributeContainer instantiate(ItemName itemName) {
        return new ResourceAttributeContainerImpl(
                itemName,
                toResourceAttributeContainerDefinition(itemName));
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

    @Override
    @NotNull ResourceObjectDefinition clone();

    @Override
    @NotNull
    ResourceObjectDefinition deepClone(@NotNull DeepCloneOperation operation);

    //endregion

    //region Other
    /**
     * Returns the "raw" configuration bean for this object type.
     *
     * BEWARE: In the case of inherited object types, this is only the partial information.
     * (Parts inherited from the parents are not returned.)
     */
    @NotNull ResourceObjectTypeDefinitionType getDefinitionBean();

    /**
     * Creates a layer-specific version of this definition.
     */
    ResourceObjectDefinition forLayer(@NotNull LayerType layer);

    /**
     * Replaces a definition for given item name with a provided one.
     */
    void replaceDefinition(@NotNull QName itemName, @Nullable ItemDefinition<?> newDefinition);

    default void replaceDefinition(@NotNull ItemDefinition<?> newDefinition) {
        replaceDefinition(newDefinition.getItemName(), newDefinition);
    }

    /**
     * This is currently used only to pass information about association in the model-impl
     *
     * TODO consider removal!
     */
    String getResourceOid();

    /**
     * Returns true if the type definition matches specified object class name.
     * OC name of `null` matches all definitions.
     */
    default boolean matchesObjectClassName(@Nullable QName name) {
        return name == null || QNameUtil.match(name, getObjectClassName());
    }

    /** Is this definition bound to a specific resource type? If yes, this method returns its identification. */
    @Nullable ResourceObjectTypeIdentification getTypeIdentification();

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
    //endregion
}
