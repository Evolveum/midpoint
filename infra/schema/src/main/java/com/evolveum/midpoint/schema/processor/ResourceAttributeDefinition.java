/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.schema.util.SimulationUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Information about a resource attribute that is available from the connector and
 * optionally refined by configuration in resource `schemaHandling` section.
 *
 * This class represents schema definition for resource object attribute. See
 * {@link Definition} for more details.
 *
 * @see ResourceAttribute
 */
public interface ResourceAttributeDefinition<T>
        extends PrismPropertyDefinition<T>,
        LayeredDefinition {

    /**
     * Returns limitations (cardinality, access rights, processing) for given layer.
     *
     * These are obtained from resource and/or explicitly configured.
     *
     * @see ResourceItemDefinitionType#getLimitations()
     */
    PropertyLimitations getLimitations(LayerType layer);

    /**
     * Gets the level of processing for specified layer.
     *
     * @see Definition#getProcessing()
     */
    ItemProcessing getProcessing(LayerType layer);

    /**
     * Is the attribute ignored (at specified layer)?
     */
    default boolean isIgnored(LayerType layer) {
        return getProcessing(layer) == ItemProcessing.IGNORE;
    }

    /**
     * Gets `maxOccurs` limitation for given layer.
     *
     * @see ItemDefinition#getMaxOccurs()
     */
    int getMaxOccurs(LayerType layer);

    /**
     * Gets `minOccurs` limitation for given layer.
     */
    int getMinOccurs(LayerType layer);

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    default boolean isOptional(LayerType layer) {
        return getMinOccurs(layer) == 0;
    }

    default boolean isMandatory(LayerType layer) {
        return !isOptional(layer);
    }

    default boolean isMultiValue(LayerType layer) {
        int maxOccurs = getMaxOccurs(layer);
        return maxOccurs < 0 || maxOccurs > 1;
    }

    default boolean isSingleValue(LayerType layer) {
        return getMaxOccurs(layer) == 1;
    }

    /**
     * Is adding allowed (at specified layer)?
     *
     * @see PrismItemAccessDefinition#canAdd()
     */
    boolean canAdd(LayerType layer);

    /**
     * Is reading allowed (at specified layer)?
     *
     * @see PrismItemAccessDefinition#canRead()
     */
    boolean canRead(LayerType layer);

    /**
     * Is modification allowed (at specified layer)?
     *
     * @see PrismItemAccessDefinition#canModify()
     */
    boolean canModify(LayerType layer);

    /**
     * Returns configured fetch strategy.
     *
     * @see ResourceItemDefinitionType#getFetchStrategy()
     */
    AttributeFetchStrategyType getFetchStrategy();

    /**
     * Returns configured storage strategy.
     *
     * @see ResourceItemDefinitionType#getStorageStrategy()
     */
    @NotNull AttributeStorageStrategyType getStorageStrategy();

    /**
     * If present, it overrides the inclusion/exclusion of this item in/from the shadow caching.
     * Please use the {@link #isEffectivelyCached(ResourceObjectDefinition)} method
     * to determine the effective caching status.
     */
    Boolean isCached();

    /**
     * Returns `true` if this attribute is effectively cached, given provided object type/class definition.
     *
     * Precondition: the definition must be attached to a resource.
     *
     * NOTE: Ignores the default caching turned on by read capability with `cachingOnly` = `true`.
     */
    default boolean isEffectivelyCached(@NotNull ResourceObjectDefinition objectDefinition) {

        var cachingPolicy = objectDefinition.getEffectiveShadowCachingPolicy();
        if (cachingPolicy.getCachingStrategy() != CachingStrategyType.PASSIVE) {
            // Caching is disabled. Individual overriding of caching status is not relevant.
            return false;
        }

        var override = isCached();
        if (override != null) {
            return override;
        }

        var scope = cachingPolicy.getScope();
        var attributesScope = Objects.requireNonNullElse(
                scope != null ? scope.getAttributes() : null,
                ShadowItemsCachingScopeType.MAPPED);

        return switch (attributesScope) {
            case ALL -> true;
            case NONE -> false;
            case MAPPED -> hasOutboundMapping() || getInboundMappingBeans().isEmpty();
        };
    }

    /**
     * Is this attribute so-called volatility trigger, i.e. may its changes cause changes in other attributes?
     *
     * @see ResourceItemDefinitionType#isVolatilityTrigger()
     */
    boolean isVolatilityTrigger();

    /**
     * Determines the order in which this attribute should be modified (in very special cases).
     *
     * @see ResourceItemDefinitionType#getModificationPriority()
     */
    Integer getModificationPriority();

    /**
     * Should be this attribute modified solely in "replace" mode?
     *
     * @see ResourceItemDefinitionType#isReadReplaceMode()
     */
    Boolean getReadReplaceMode();

    // The following are just overrides with more specific return types

    @Override
    @NotNull ResourceAttribute<T> instantiate();

    @Override
    @NotNull ResourceAttribute<T> instantiate(QName name);

    /**
     * Creates a new {@link ResourceAttribute} from given {@link PrismProperty}.
     * Used in the process of "definition application" in `applyDefinitions` and similar methods.
     *
     * Assumes that the original property is correctly constructed, i.e. it has no duplicate values.
     */
    default @NotNull ResourceAttribute<T> instantiateFrom(@NotNull PrismProperty<?> property) throws SchemaException {
        //noinspection unchecked
        ResourceAttribute<T> attribute = instantiateFromRealValues((Collection<T>) property.getRealValues());
        attribute.setIncomplete(property.isIncomplete());
        return attribute;
    }

    /**
     * Creates a new {@link ResourceAttribute} from given real values, converting them if necessary.
     *
     * Assumes that the values contain no duplicates and no nulls.
     */
    default @NotNull ResourceAttribute<T> instantiateFromRealValues(@NotNull Collection<T> realValues) throws SchemaException {
        ResourceAttribute<T> attribute = instantiate();
        attribute.addNormalizedValues(realValues, this);
        return attribute;
    }

    default @NotNull ResourceAttribute<T> instantiateFromRealValue(@NotNull T realValue) throws SchemaException {
        return instantiateFromRealValues(List.of(realValue));
    }

    @Override
    @NotNull ResourceAttributeDefinition<T> clone();

    @Override
    ResourceAttributeDefinition<T> deepClone(@NotNull DeepCloneOperation operation);

    @NotNull MutableRawResourceAttributeDefinition<T> toMutable();

    /**
     * Provides a debug dump respective to the given layer.
     *
     * TODO reconsider this method
     */
    String debugDump(int indent, LayerType layer);

    /**
     * Creates a copy of the definition, with a given customizer applied to the _raw_ part of the definition.
     *
     * Should be used only in special cases, near the resource schema construction time.
     * (So various alternate implementations of this interface need not support this method.)
     *
     * May not preserve all information (like access override flags).
     *
     * TODO is this needed?
     */
    default ResourceAttributeDefinition<T> spawnModifyingRaw(
            @NotNull Consumer<RawResourceAttributeDefinition<T>> rawPartCustomizer) {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a view of the current definition for a given layer.
     * (May return even the original object e.g. if the layer matches the current one.)
     */
    @NotNull ResourceAttributeDefinition<T> forLayer(@NotNull LayerType layer);

    /**
     * Provides a value that will override {@link #canRead(LayerType)} return values (for all layers).
     * Used e.g. when applying authorizations on the definition.
     */
    void setOverrideCanRead(Boolean value);

    /**
     * Provides a value that will override {@link #canAdd(LayerType)} return values (for all layers).
     * Used e.g. when applying authorizations on the definition.
     */
    void setOverrideCanAdd(Boolean value);

    /**
     * Provides a value that will override {@link #canModify(LayerType)} return values (for all layers).
     * Used e.g. when applying authorizations on the definition.
     */
    void setOverrideCanModify(Boolean value);

    /**
     * When set to true, allows to preserve attribute values that are set outside midPoint.
     *
     * @see ItemRefinedDefinitionType#isTolerant()
     */
    boolean isTolerant();

    /**
     * Is this attribute designated as a secondary identifier via `schemaHandling`?
     *
     * @see ResourceAttributeDefinitionType#isSecondaryIdentifier()
     */
    Boolean isSecondaryIdentifierOverride();

    /**
     * Gets the (configured) attribute description.
     *
     * @see ResourceAttributeDefinitionType#getDescription()
     */
    String getDescription();

    /**
     * Gets the original (raw) attribute definition.
     */
    RawResourceAttributeDefinition<T> getRawAttributeDefinition();

    /**
     * Gets the outbound mapping, if defined.
     *
     * @see ResourceAttributeDefinitionType#getOutbound()
     */
    @Nullable MappingType getOutboundMappingBean();

    default boolean hasOutboundMapping() {
        return getOutboundMappingBean() != null;
    }

    /**
     * Gets the inbound mappings (if any).
     *
     * @see ResourceAttributeDefinitionType#getInbound()
     */
    @NotNull List<InboundMappingType> getInboundMappingBeans();

    /**
     * Drives behavior of strong and normal mappings for this attribute.
     *
     * @see ResourceItemDefinitionType#isExclusiveStrong()
     */
    boolean isExclusiveStrong();

    /**
     * Gets patterns for values that are "tolerated" on the resource.
     *
     * @see #isTolerant()
     * @see ResourceItemDefinitionType#getTolerantValuePattern()
     */
    @NotNull List<String> getTolerantValuePatterns();

    /**
     * Gets patterns for values that are not "tolerated" on the resource.
     *
     * @see #isTolerant()
     * @see ResourceItemDefinitionType#getIntolerantValuePattern()
     */
    @NotNull List<String> getIntolerantValuePatterns();

    /**
     * Is this attribute configured to serve as a display name?
     *
     * @see ResourceItemDefinitionType#isDisplayNameAttribute()
     */
    boolean isDisplayNameAttribute();

    /** @see ItemRefinedDefinitionType#getCorrelator() */
    @Nullable ItemCorrelatorDefinitionType getCorrelatorDefinition();

    /** TODO */
    @Nullable ItemChangeApplicationModeType getChangeApplicationMode();

    /** TODO */
    @Nullable String getLifecycleState();

    /** TODO */
    default boolean isVisible(@NotNull TaskExecutionMode taskExecutionMode) {
        return SimulationUtil.isVisible(getLifecycleState(), taskExecutionMode);
    }

    /** Note that attributes must always have static Java type. */
    @Override
    @NotNull Class<T> getTypeClass();

    /**
     * Is this attribute returned by default? (I.e. if no specific options are sent to the connector?)
     */
    @Nullable Boolean getReturnedByDefault();

    /**
     * Is this attribute returned by default? (I.e. if no specific options are sent to the connector?)
     */
    default boolean isReturnedByDefault() {
        return !Boolean.FALSE.equals(
                getReturnedByDefault());
    }

    /**
     * Returns native attribute name.
     *
     * Native name of the attribute is a name as it is used on the resource or
     * as seen by the connector. It is used for diagnostics purposes and may be
     * used by the connector itself. As the attribute names in XSD have to
     * comply with XML element name limitations, this may be the only way how to
     * determine original attribute name.
     *
     * Returns null if native attribute name is not set or unknown.
     *
     * The name should be the same as the one used by the resource, if the
     * resource supports naming of attributes. E.g. in case of LDAP this
     * annotation should contain "cn", "givenName", etc. If the resource is not
     * that flexible, the native attribute names may be hardcoded (e.g.
     * "username", "homeDirectory") or may not be present at all.
     *
     * @return native attribute name
     */
    String getNativeAttributeName();

    /**
     * Returns name of the attribute as given in the connector framework.
     * This is not used for any significant logic. It is mostly for diagnostics.
     *
     * @return name of the attribute as given in the connector framework.
     */
    String getFrameworkAttributeName();

    /** Returns `true` if there are any refinements (like in `schemaHandling`). */
    boolean hasRefinements();

    /** Creates a normalization-aware version of this definition. */
    default <N> @NotNull NormalizationAwareResourceAttributeDefinition<N> toNormalizationAware() {
        return new NormalizationAwareResourceAttributeDefinition<>(this);
    }

    /** Returns the standard path where this attribute can be found in shadows. E.g. for searching. */
    default @NotNull ItemPath getStandardPath() {
        return ItemPath.create(ShadowType.F_ATTRIBUTES, getItemName());
    }

    /** Creates an empty delta for this attribute against its standard path. */
    default @NotNull PropertyDelta<T> createEmptyDelta() {
        return createEmptyDelta(getStandardPath());
    }
}
