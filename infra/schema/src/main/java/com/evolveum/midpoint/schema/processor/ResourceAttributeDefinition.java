/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * Information about a resource attribute that is available from the connector and
 * optionally refined by configuration in resource `schemaHandling` section.
 *
 * For clarity, the information is categorized into:
 *
 * - data obtainable from the resource: {@link RawResourceAttributeDefinition},
 * - full information available (this interface)
 *
 * This class represents schema definition for resource object attribute. See
 * {@link Definition} for more details.
 *
 * @see ResourceAttribute
 */
public interface ResourceAttributeDefinition<T>
        extends PrismPropertyDefinition<T>,
        RawResourceAttributeDefinition<T>,
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
     */
    default ResourceAttributeDefinition<T> spawnModifyingRaw(
            @NotNull Consumer<MutableRawResourceAttributeDefinition<T>> rawPartCustomizer) {
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
}
