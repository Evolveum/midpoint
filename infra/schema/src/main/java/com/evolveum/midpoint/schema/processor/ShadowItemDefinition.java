/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.schema.simulation.ExecutionModeProvider;
import com.evolveum.midpoint.schema.util.SimulationUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Information about a resource attribute or association.
 *
 * . It is based on a "native" part, available from the connector (or from simulated associations capability definition);
 * see {@link NativeShadowItemDefinition}.
 * . This part is then optionally refined by the configuration in resource `schemaHandling` section.
 *
 * For the time being, it does not extend {@link ItemDefinition} because of typing complications:
 * {@link ShadowItem} cannot extend {@link Item}.
 *
 * @see ResourceAttributeDefinition
 * @see ShadowAssociationDefinition
 *
 * @param <I> item that is created by the instantiation of this definition
 * @param <R> real value stored in I
 */
public interface ShadowItemDefinition<I extends ShadowItem<?, ?>, R>
        extends
        PrismItemBasicDefinition,
        PrismItemAccessDefinition,
        PrismItemMiscDefinition,
        PrismPresentationDefinition,
        ShadowItemUcfDefinition,
        ShadowItemLayeredDefinition,
        LayeredDefinition {

    /**
     * When set to true, allows to preserve attribute values that are set outside midPoint.
     *
     * @see ItemRefinedDefinitionType#isTolerant()
     */
    boolean isTolerant();

    /**
     * Returns configured fetch strategy.
     *
     * @see ResourceItemDefinitionType#getFetchStrategy()
     */
    @NotNull AttributeFetchStrategyType getFetchStrategy();

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

    /**
     * Creates a view of the current definition for a given layer.
     * (May return even the original object e.g. if the layer matches the current one.)
     */
    @NotNull ShadowItemDefinition<I, R> forLayer(@NotNull LayerType layer);

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
     * Gets the (configured) attribute description.
     *
     * @see ResourceAttributeDefinitionType#getDescription()
     */
    String getDescription();

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

    /** TODO */
    @Nullable ItemChangeApplicationModeType getChangeApplicationMode();

    /** TODO */
    @Nullable String getLifecycleState();

    /** TODO */
    default boolean isVisible(@NotNull TaskExecutionMode taskExecutionMode) {
        return SimulationUtil.isVisible(getLifecycleState(), taskExecutionMode);
    }

    default boolean isVisible(@NotNull ExecutionModeProvider executionModeProvider) {
        return SimulationUtil.isVisible(getLifecycleState(), executionModeProvider);
    }

    /** Note that attributes must always have static Java type. */
    @NotNull Class<R> getTypeClass();

    /**
     * Is this attribute returned by default? (I.e. if no specific options are sent to the connector?)
     */
    default boolean isReturnedByDefault() {
        return !Boolean.FALSE.equals(
                getReturnedByDefault());
    }

    /** Returns `true` if there are any refinements (like in `schemaHandling`). */
    boolean hasRefinements();

    boolean isIndexOnly();

    @NotNull I instantiate() throws SchemaException;

    @NotNull I instantiate(QName itemName) throws SchemaException;

    String getHumanReadableDescription();

    ItemPath getStandardPath();

    /** If `true`, the item does not exist on the resource, but is simulated by midPoint. */
    boolean isSimulated();
}
