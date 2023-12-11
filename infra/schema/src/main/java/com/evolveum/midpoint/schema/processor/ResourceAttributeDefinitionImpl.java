/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.prism.util.CloneUtil.toImmutable;
import static com.evolveum.midpoint.prism.util.DefinitionUtil.addNamespaceIfApplicable;
import static com.evolveum.midpoint.util.MiscUtil.argCheck;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.io.Serial;
import java.util.*;
import java.util.function.Consumer;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.annotation.ItemDiagramSpecification;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.impl.PrismPropertyDefinitionImpl;
import com.evolveum.midpoint.prism.impl.delta.PropertyDeltaImpl;
import com.evolveum.midpoint.prism.impl.match.MatchingRuleRegistryImpl;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.util.DefinitionUtil;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * An attribute definition (obtained typically from the connector),
 * optionally refined by information from `schemaHandling` section of a resource definition.
 *
 * The implementation consists of a pair of {@link #rawDefinition} and {@link #customizationBean},
 * plus some auxiliary information for faster access.
 *
 * This class intentionally does NOT inherit from {@link PrismPropertyDefinitionImpl}. Instead, a large part of the required
 * functionality is delegated to {@link #rawDefinition} which inherits from that class.
 *
 * @see RawResourceAttributeDefinition
 */
public class ResourceAttributeDefinitionImpl<T>
        extends AbstractFreezable
        implements ResourceAttributeDefinition<T> {

    @Serial private static final long serialVersionUID = 1L;

    /**
     * Default value for {@link #currentLayer}.
     */
    private static final LayerType DEFAULT_LAYER = LayerType.MODEL;

    /**
     * At what layer do we want to view property limitations ({@link #limitationsMap}).
     */
    @NotNull private final LayerType currentLayer;

    /**
     * Raw definition obtained from the connector (or manually filled-in by the administrator in `schema` part).
     *
     * Always immutable. (The reason is mere simplicity. For example, {@link #limitationsMap} depends
     * on information here, so any updates would mean the need to recompute that map.)
     */
    @NotNull private final RawResourceAttributeDefinition<T> rawDefinition;

    /**
     * Customization from `schemaHandling`. If no matching value is present there, an empty one
     * is created (to avoid nullity checks throughput the code). This is also the case when
     * {@link ResourceAttributeDefinitionImpl} is used to hold a raw attribute definition
     * in {@link ResourceObjectClassDefinition}.
     *
     * Always immutable.
     */
    @NotNull private final ResourceAttributeDefinitionType customizationBean;

    /**
     * Contains attribute limitations (minOccurs, maxOccurs, access, ...) for individual layers.
     *
     * Computed at construction time, then immutable.
     */
    private final Map<LayerType, PropertyLimitations> limitationsMap;

    /**
     * Allows overriding read/add/modify access flags.
     *
     * Mutable by default.
     */
    private final PropertyAccessType accessOverride;

    /**
     * @see ItemDefinition#structuredType()
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private transient @Nullable Optional<ComplexTypeDefinition> structuredType;

    /**
     * Version without customization bean. Throws no checked exceptions.
     */
    private ResourceAttributeDefinitionImpl(@NotNull RawResourceAttributeDefinition<T> rawDefinition) {

        assert rawDefinition.isImmutable();

        this.currentLayer = DEFAULT_LAYER;
        this.rawDefinition = rawDefinition;
        this.customizationBean =
                CloneUtil.toImmutable(
                        new ResourceAttributeDefinitionType());
        try {
            this.limitationsMap = computeLimitationsMap();
        } catch (SchemaException e) {
            // Parsing the empty customization bean should not generate any exceptions.
            throw new SystemException("Unexpected schema exception: " + e.getMessage(), e);
        }
        this.accessOverride = new PropertyAccessType();
    }

    /**
     * "Standard" version (raw + customization).
     *
     * @throws SchemaException If there's a problem with parsing customization bean.
     */
    private ResourceAttributeDefinitionImpl(
            @NotNull RawResourceAttributeDefinition<T> rawDefinition,
            @NotNull ResourceAttributeDefinitionType customizationBean)
            throws SchemaException {
        assert rawDefinition.isImmutable();
        assert customizationBean.isImmutable();

        this.currentLayer = DEFAULT_LAYER;
        this.rawDefinition = rawDefinition;
        this.customizationBean = customizationBean;
        this.limitationsMap = computeLimitationsMap();
        this.accessOverride = new PropertyAccessType();
    }

    /**
     * Version to be used for cloning and similar operations.
     */
    private ResourceAttributeDefinitionImpl(
            @NotNull LayerType layer,
            @NotNull RawResourceAttributeDefinition<T> rawDefinition,
            @NotNull ResourceAttributeDefinitionType customizationBean,
            @NotNull Map<LayerType, PropertyLimitations> limitationsMap,
            @NotNull PropertyAccessType accessOverride) {
        assert rawDefinition.isImmutable();
        assert customizationBean.isImmutable();

        this.currentLayer = layer;
        this.rawDefinition = rawDefinition;
        this.customizationBean = customizationBean;
        this.limitationsMap = limitationsMap;
        this.accessOverride = accessOverride;
    }

    /**
     * This is the main creation point.
     *
     * @throws SchemaException If there's a problem with the customization bean.
     */
    public static <T> ResourceAttributeDefinition<T> create(
            @NotNull RawResourceAttributeDefinition<T> rawDefinition,
            @Nullable ResourceAttributeDefinitionType customizationBean)
            throws SchemaException {

        return new ResourceAttributeDefinitionImpl<>(
                toImmutable(rawDefinition),
                toImmutable(customizationBean != null ?
                        customizationBean : new ResourceAttributeDefinitionType()));
    }

    /**
     * This is the creation point from "raw" form only.
     */
    public static <T> ResourceAttributeDefinition<T> create(
            @NotNull RawResourceAttributeDefinition<T> rawDefinition) {
        return new ResourceAttributeDefinitionImpl<>(
                toImmutable(rawDefinition));
    }

    public @NotNull ResourceAttributeDefinitionImpl<T> forLayer(@NotNull LayerType layer) {
        if (layer == currentLayer) {
            return this;
        } else {
            return new ResourceAttributeDefinitionImpl<>(
                    layer,
                    rawDefinition,
                    customizationBean,
                    limitationsMap,
                    accessOverride.clone() // TODO do we want to preserve also the access override?
            );
        }
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @NotNull
    @Override
    public ResourceAttributeDefinitionImpl<T> clone() {
        return new ResourceAttributeDefinitionImpl<>(
                currentLayer,
                rawDefinition,
                customizationBean,
                limitationsMap,
                accessOverride.clone());
    }

    /**
     * Converts limitations embedded in {@link #rawDefinition} and specified in {@link #customizationBean}
     * to the {@link #limitationsMap}.
     */
    private @NotNull Map<LayerType, PropertyLimitations> computeLimitationsMap() throws SchemaException {
        Map<LayerType, PropertyLimitations> map = new HashMap<>();

        PropertyLimitations schemaLimitations = getOrCreateLimitationsForLayer(map, LayerType.SCHEMA);
        schemaLimitations.setMinOccurs(rawDefinition.getMinOccurs());
        schemaLimitations.setMaxOccurs(rawDefinition.getMaxOccurs());
        schemaLimitations.setProcessing(rawDefinition.getProcessing());
        schemaLimitations.getAccess().setAdd(rawDefinition.canAdd());
        schemaLimitations.getAccess().setModify(rawDefinition.canModify());
        schemaLimitations.getAccess().setRead(rawDefinition.canRead());

        PropertyLimitations previousLimitations = null;
        for (LayerType layer : LayerType.values()) {
            PropertyLimitations limitations = getOrCreateLimitationsForLayer(map, layer);
            if (previousLimitations != null) {
                limitations.setMinOccurs(previousLimitations.getMinOccurs());
                limitations.setMaxOccurs(previousLimitations.getMaxOccurs());
                limitations.setProcessing(previousLimitations.getProcessing());
                limitations.getAccess().setAdd(previousLimitations.getAccess().isAdd());
                limitations.getAccess().setRead(previousLimitations.getAccess().isRead());
                limitations.getAccess().setModify(previousLimitations.getAccess().isModify());
            }
            previousLimitations = limitations;
            // TODO check this as part of MID-7929 resolution
            if (layer != LayerType.SCHEMA) {
                // SCHEMA is a pseudo-layer. It cannot be overridden ... unless specified explicitly
                PropertyLimitationsType genericLimitationsType =
                        MiscSchemaUtil.getLimitationsLabeled(customizationBean.getLimitations(), null);
                if (genericLimitationsType != null) {
                    applyLimitationsBean(limitations, genericLimitationsType);
                }
            }
            PropertyLimitationsType layerLimitationsType =
                    MiscSchemaUtil.getLimitationsLabeled(customizationBean.getLimitations(), layer);
            if (layerLimitationsType != null) {
                applyLimitationsBean(limitations, layerLimitationsType);
            }
        }
        return Collections.unmodifiableMap(map);
    }

    private PropertyLimitations getOrCreateLimitationsForLayer(Map<LayerType, PropertyLimitations> map, LayerType layer) {
        return map.computeIfAbsent(
                layer, (l) -> new PropertyLimitations());
    }

    @Override
    public boolean isTolerant() {
        return !Boolean.FALSE.equals(
                customizationBean.isTolerant());
    }

    @Override
    public Boolean isSecondaryIdentifierOverride() {
        return customizationBean.isSecondaryIdentifier();
    }

    @Override
    public boolean canAdd() {
        return canAdd(currentLayer);
    }

    @Override
    public boolean canAdd(LayerType layer) {
        if (accessOverride.isAdd() != null) {
            return accessOverride.isAdd();
        }
        return limitationsMap.get(layer).canAdd();
    }

    @Override
    public boolean canRead() {
        return canRead(currentLayer);
    }

    @Override
    public boolean canRead(LayerType layer) {
        if (accessOverride.isRead() != null) {
            return accessOverride.isRead();
        }
        return limitationsMap.get(layer).canRead();
    }

    @Override
    public boolean canModify() {
        return canModify(currentLayer);
    }

    @Override
    public boolean canModify(LayerType layer) {
        if (accessOverride.isModify() != null) {
            return accessOverride.isModify();
        }
        return limitationsMap.get(layer).canModify();
    }

    public void setOverrideCanRead(Boolean value) {
        checkMutable();
        accessOverride.setRead(value);
    }

    public void setOverrideCanAdd(Boolean value) {
        checkMutable();
        accessOverride.setAdd(value);
    }

    public void setOverrideCanModify(Boolean value) {
        checkMutable();
        accessOverride.setModify(value);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isIgnored() {
        return isIgnored(currentLayer);
    }

    @Override
    public boolean isEmphasized() {
        if (customizationBean.isEmphasized() != null) {
            return customizationBean.isEmphasized();
        } else {
            return rawDefinition.isEmphasized();
        }
    }

    @Override
    public ItemProcessing getProcessing() {
        return getProcessing(currentLayer);
    }

    @Override
    public boolean isAbstract() {
        return rawDefinition.isAbstract(); // most probably false
    }

    @Override
    public boolean isDeprecated() {
        return rawDefinition.isDeprecated(); // most probably false
    }

    @Override
    public boolean isRemoved() {
        return rawDefinition.isRemoved(); // most probably false
    }

    @Override
    public String getRemovedSince() {
        return rawDefinition.getRemovedSince(); // most probably null
    }

    @Override
    public boolean isExperimental() {
        return rawDefinition.isExperimental(); // most probably false
    }

    @Override
    public String getPlannedRemoval() {
        return rawDefinition.getPlannedRemoval(); // most probably null
    }

    @Override
    public boolean isElaborate() {
        return rawDefinition.isElaborate(); // most probably null
    }

    @Override
    public String getDeprecatedSince() {
        return rawDefinition.getDeprecatedSince(); // most probably null
    }

    @Override
    public ItemProcessing getProcessing(LayerType layer) {
        return limitationsMap.get(layer).getProcessing();
    }

    @Override
    public String getDisplayName() {
        return MiscUtil.orElseGet(
                customizationBean.getDisplayName(),
                rawDefinition::getDisplayName);
    }

    @Override
    public Integer getDisplayOrder() {
        return MiscUtil.orElseGet(
                customizationBean.getDisplayOrder(),
                rawDefinition::getDisplayOrder);
    }

    @Override
    public String getHelp() {
        return MiscUtil.orElseGet(
                customizationBean.getHelp(),
                rawDefinition::getHelp);
    }

    @Override
    public String getDescription() {
        return MiscUtil.orElseGet(
                customizationBean.getDescription(),
                rawDefinition::getDescription);
    }

    @Override
    public RawResourceAttributeDefinition<T> getRawAttributeDefinition() {
        return rawDefinition;
    }

    @Override
    public @Nullable MappingType getOutboundMappingBean() {
        return customizationBean.getOutbound();
    }

    @Override
    public @NotNull List<InboundMappingType> getInboundMappingBeans() {
        return customizationBean.getInbound();
    }

    @Override
    public @Nullable Boolean getReturnedByDefault() {
        return rawDefinition.getReturnedByDefault();
    }

    @Override
    public String getNativeAttributeName() {
        return rawDefinition.getNativeAttributeName();
    }

    @Override
    public String getFrameworkAttributeName() {
        return rawDefinition.getFrameworkAttributeName();
    }

    @Override
    public @NotNull ResourceAttribute<T> instantiate() {
        return instantiate(
                getItemName());
    }

    @Override
    public @NotNull ResourceAttribute<T> instantiate(QName name) {
        name = addNamespaceIfApplicable(name, getItemName());
        return new ResourceAttributeImpl<>(name, this);
    }

    @Override
    public int getMaxOccurs() {
        return getMaxOccurs(currentLayer);
    }

    @Override
    public boolean isOperational() {
        return false;
    }

    @Override
    public boolean isIndexOnly() {
        return getStorageStrategy() == AttributeStorageStrategyType.INDEX_ONLY;
    }

    @Override
    public boolean isInherited() {
        return rawDefinition.isInherited(); // probably false
    }

    @Override
    public boolean isDynamic() {
        return rawDefinition.isDynamic(); // probably false?
    }

    @Override
    public QName getSubstitutionHead() {
        return rawDefinition.getSubstitutionHead(); // probably null
    }

    @Override
    public boolean isHeterogeneousListItem() {
        return rawDefinition.isHeterogeneousListItem(); // probably false
    }

    @Override
    public PrismReferenceValue getValueEnumerationRef() {
        return rawDefinition.getValueEnumerationRef(); // probably false
    }

    @Override
    public boolean isValidFor(
            @NotNull QName elementQName, @NotNull Class<? extends ItemDefinition<?>> clazz, boolean caseInsensitive) {
        //noinspection unchecked,rawtypes
        return clazz.isAssignableFrom(ResourceAttributeDefinitionImpl.class)
                && rawDefinition.isValidFor(elementQName, (Class) ItemDefinition.class, caseInsensitive);
    }

    @Override
    public void adoptElementDefinitionFrom(ItemDefinition<?> otherDef) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <D extends ItemDefinition<?>> D findItemDefinition(@NotNull ItemPath path, @NotNull Class<D> clazz) {
        if (path.isEmpty()) {
            argCheck(clazz.isAssignableFrom(this.getClass()),
                    "Looking for definition of class %s but found %s", clazz, this);
            //noinspection unchecked
            return (D) this;
        } else {
            return null;
        }
    }

    @Override
    public int getMaxOccurs(LayerType layer) {
        return limitationsMap.get(layer).getMaxOccurs();
    }

    @Override
    public @NotNull ItemName getItemName() {
        return rawDefinition.getItemName();
    }

    @Override
    public int getMinOccurs() {
        return getMinOccurs(currentLayer);
    }

    @Override
    public int getMinOccurs(LayerType layer) {
        return limitationsMap.get(layer).getMinOccurs();
    }

    @Override
    public boolean isExclusiveStrong() {
        return Boolean.TRUE.equals(
                customizationBean.isExclusiveStrong());
    }

    @Override
    public PropertyLimitations getLimitations(LayerType layer) {
        return limitationsMap.get(layer);
    }

    @Override
    public String getDocumentation() {
        return MiscUtil.orElseGet(
                customizationBean.getDocumentation(),
                rawDefinition::getDocumentation);
    }

    @Override
    public String getDocumentationPreview() {
        return rawDefinition.getDocumentationPreview(); // probably null
    }

    @Override
    public <A> void setAnnotation(QName qname, A value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable Map<QName, Object> getAnnotations() {
        return rawDefinition.getAnnotations();
    }

    @Override
    public @Nullable List<SchemaMigration> getSchemaMigrations() {
        return rawDefinition.getSchemaMigrations(); // probably none
    }

    @Override
    public List<ItemDiagramSpecification> getDiagrams() {
        return rawDefinition.getDiagrams(); // probably none
    }

    @Override
    public AttributeFetchStrategyType getFetchStrategy() {
        return customizationBean.getFetchStrategy();
    }

    @Override
    public @NotNull AttributeStorageStrategyType getStorageStrategy() {
        if (customizationBean.getStorageStrategy() != null) {
            return customizationBean.getStorageStrategy();
        } else if (rawDefinition.isIndexOnly()) {
            return AttributeStorageStrategyType.INDEX_ONLY;
        } else {
            return AttributeStorageStrategyType.NORMAL;
        }
    }

    @Override
    public Boolean isIndexed() {
        return true; // TODO reconsider
    }

    @Override
    public Boolean isCached() {
        return customizationBean.isCached();
    }

    @Override
    public @Nullable Collection<? extends DisplayableValue<T>> getAllowedValues() {
        return rawDefinition.getAllowedValues();
    }

    @Override
    public @Nullable Collection<? extends DisplayableValue<T>> getSuggestedValues() {
        return rawDefinition.getSuggestedValues();
    }

    @Override
    public @Nullable T defaultValue() {
        return rawDefinition.defaultValue();
    }

    @Override
    public QName getMatchingRuleQName() {
        return MiscUtil.orElseGet(
                customizationBean.getMatchingRule(),
                rawDefinition::getMatchingRuleQName);
    }

    @Override
    public @NotNull MatchingRule<T> getMatchingRule() {
        return MatchingRuleRegistryImpl.instance()
                .getMatchingRuleSafe(getMatchingRuleQName(), getTypeName());
    }

    @Override
    public @NotNull PropertyDelta<T> createEmptyDelta(ItemPath path) {
        return new PropertyDeltaImpl<>(path, this, PrismContext.get());
    }

    @Override
    public @NotNull List<String> getTolerantValuePatterns() {
        return customizationBean.getTolerantValuePattern();
    }

    @Override
    public @NotNull List<String> getIntolerantValuePatterns() {
        return customizationBean.getIntolerantValuePattern();
    }

    @Override
    public boolean isVolatilityTrigger() {
        return Boolean.TRUE.equals(
                customizationBean.isVolatilityTrigger());
    }

    private static void applyLimitationsBean(PropertyLimitations limitations, PropertyLimitationsType layerLimitationsBean) {
        if (layerLimitationsBean.getMinOccurs() != null) {
            limitations.setMinOccurs(
                    DefinitionUtil.parseMultiplicity(layerLimitationsBean.getMinOccurs()));
        }
        if (layerLimitationsBean.getMaxOccurs() != null) {
            limitations.setMaxOccurs(
                    DefinitionUtil.parseMultiplicity(layerLimitationsBean.getMaxOccurs()));
        }
        if (layerLimitationsBean.getProcessing() != null) {
            limitations.setProcessing(
                    MiscSchemaUtil.toItemProcessing(layerLimitationsBean.getProcessing()));
        }
        if (layerLimitationsBean.getAccess() != null) {
            PropertyAccessType accessBean = layerLimitationsBean.getAccess();
            if (accessBean.isAdd() != null) {
                limitations.getAccess().setAdd(accessBean.isAdd());
            }
            if (accessBean.isRead() != null) {
                limitations.getAccess().setRead(accessBean.isRead());
            }
            if (accessBean.isModify() != null) {
                limitations.getAccess().setModify(accessBean.isModify());
            }
        }

    }

    @Override
    public @NotNull MutableRawResourceAttributeDefinition<T> toMutable() {
        throw new UnsupportedOperationException("Refined attribute definition can not be mutated: " + this);
    }

    @Override
    public ResourceAttributeDefinition<T> deepClone(@NotNull DeepCloneOperation operation) {
        // No deep cloning, because the constituents are immutable.
        return clone();
    }

    @Override
    public void revive(PrismContext prismContext) {
        // TODO is this [still] needed?
        rawDefinition.revive(prismContext);
        customizationBean.asPrismContainerValue().revive(prismContext);
    }

    @Override
    public void debugDumpShortToString(StringBuilder sb) {
        // TODO
    }

    @Override
    public boolean canBeDefinitionOf(PrismProperty<T> item) {
        return rawDefinition.canBeDefinitionOf(item);
    }

    @Override
    public boolean canBeDefinitionOf(@NotNull PrismValue pvalue) {
        return rawDefinition.canBeDefinitionOf(pvalue);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(getDebugDumpClassName());
        sb.append(getMutabilityFlag());
        sb.append(": ").append(getItemName()); // TODO needed?
        sb.append(" (").append(PrettyPrinter.prettyPrint(getTypeName())).append(")");

        if (getDisplayName() != null) {
            sb.append(",Disp");
        }
        if (getDescription() != null) {
            sb.append(",Desc");
        }
        if (getOutboundMappingBean() != null) {
            sb.append(",OUT");
        }
        if (!getInboundMappingBeans().isEmpty()) {
            sb.append(",IN");
        }
        if (Boolean.TRUE.equals(getReadReplaceMode())) {
            sb.append(",R+E");
        }
        if (getModificationPriority() != null) {
            sb.append(",P").append(getModificationPriority());
        }
        PropertyAccessType accessOverride = this.accessOverride;
        if (accessOverride != null && !accessOverride.asPrismContainerValue().isEmpty()) {
            sb.append(",AccessOverride: ");
            addOverride(sb, 'R', accessOverride.isRead());
            addOverride(sb, 'A', accessOverride.isAdd());
            addOverride(sb, 'M', accessOverride.isModify());
        }
        var matchingRuleQName = getMatchingRuleQName();
        if (matchingRuleQName != null) {
            sb.append(",MR=").append(PrettyPrinter.prettyPrint(matchingRuleQName));
        }
        return sb.toString();
    }

    private static void addOverride(StringBuilder sb, char op, Boolean value) {
        if (value == null) {
            sb.append(".");
        } else if (value) {
            sb.append(Character.toUpperCase(op));
        } else {
            sb.append(Character.toLowerCase(op));
        }
    }

    /**
     * Return a human readable name of this class suitable for logs.
     */
    public String getDebugDumpClassName() {
        return "RAD";
    }

    @Override
    public String debugDump(int indent) {
        return debugDump(indent, (LayerType) null);
    }

    public String debugDump(int indent, LayerType layer) {
        // TODO reconsider this method
        StringBuilder sb = DebugUtil.createTitleStringBuilder(getClass(), indent);
        sb.append(this);
        if (layer == null) {
            sb.append("\n");
            DebugUtil.debugDumpMapSingleLine(sb, limitationsMap, indent + 1);
        } else {
            PropertyLimitations limitations = limitationsMap.get(layer);
            if (limitations != null) {
                sb.append(limitations);
            }
        }
        return sb.toString();
    }

    @Override
    public Integer getModificationPriority() {
        return customizationBean.getModificationPriority();
    }

    @Override
    public Boolean getReadReplaceMode() {
        return customizationBean.isReadReplaceMode();
    }

    @Override
    public boolean isDisplayNameAttribute() {
        return Boolean.TRUE.equals(
                customizationBean.isDisplayNameAttribute());
    }

    @Override
    public @Nullable ItemCorrelatorDefinitionType getCorrelatorDefinition() {
        return customizationBean.getCorrelator();
    }

    @Override
    public @Nullable ItemChangeApplicationModeType getChangeApplicationMode() {
        return customizationBean.getChangeApplicationMode();
    }

    @Override
    public @Nullable String getLifecycleState() {
        return customizationBean.getLifecycleState();
    }

    @Override
    public Optional<ComplexTypeDefinition> structuredType() {
        //noinspection OptionalAssignedToNull
        if (structuredType == null) {
            this.structuredType = Optional.ofNullable(
                    getPrismContext().getSchemaRegistry().findComplexTypeDefinitionByType(getTypeName()));
        }
        return structuredType;
    }

    @Override
    public void performFreeze() {
        stateCheck(rawDefinition.isImmutable(), "Raw definition is not immutable");
        stateCheck(customizationBean.isImmutable(), "Customization bean is not immutable");
        // accessOverride should be frozen but there's currently no support for that
    }

    @Override
    public PrismContext getPrismContext() {
        return PrismContext.get();
    }

    @Override
    public boolean accept(Visitor<Definition> visitor, SmartVisitation<Definition> visitation) {
        return rawDefinition.accept(visitor, visitation);
    }

    @Override
    public ResourceAttributeDefinition<T> spawnModifyingRaw(
            @NotNull Consumer<RawResourceAttributeDefinition<T>> rawPartCustomizer) {
        try {
            return ResourceAttributeDefinitionImpl.create(
                    RawResourceAttributeDefinition.spawn(rawDefinition, rawPartCustomizer),
                    customizationBean);
        } catch (SchemaException e) {
            // The customization bean should not have any schema problems at this time.
            throw new IllegalStateException("Unexpected schema exception: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ResourceAttributeDefinitionImpl<?> that = (ResourceAttributeDefinitionImpl<?>) o;
        return rawDefinition.equals(that.rawDefinition)
                && customizationBean.equals(that.customizationBean)
                && Objects.equals(limitationsMap, that.limitationsMap)
                && Objects.equals(accessOverride, that.accessOverride);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rawDefinition, customizationBean);
    }

    @Override
    public @NotNull LayerType getCurrentLayer() {
        return currentLayer;
    }

    @Override
    public @NotNull QName getTypeName() {
        return rawDefinition.getTypeName();
    }

    @Override
    public boolean isRuntimeSchema() {
        return false;
    }

    @Override
    public @NotNull Class<T> getTypeClass() {
        return rawDefinition.getTypeClass();
    }

    @Override
    public <A> A getAnnotation(QName qname) {
        return null;
    }

    @Override
    public boolean hasRefinements() {
        return !customizationBean.asPrismContainerValue().isEmpty();
    }

    @Override
    public void accept(Visitor<Definition> visitor) {
        rawDefinition.accept(visitor);
    }
}
