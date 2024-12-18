/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.prism.util.DefinitionUtil.addNamespaceIfApplicable;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.io.Serial;
import java.util.*;
import javax.xml.namespace.QName;

import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.annotation.ItemDiagramSpecification;
import com.evolveum.midpoint.prism.delta.ItemMerger;
import com.evolveum.midpoint.prism.impl.PrismPropertyDefinitionImpl;
import com.evolveum.midpoint.prism.key.NaturalKeyDefinition;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.util.DefinitionUtil;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Base implementation of {@link ShadowSimpleAttributeDefinitionImpl} and {@link ShadowReferenceAttributeDefinitionImpl}.
 *
 * The implementation consists of a pair of {@link #nativeDefinition} and {@link #customizationBean},
 * plus some auxiliary information for faster access.
 *
 * This class intentionally does NOT inherit from {@link PrismPropertyDefinitionImpl}. Instead, a large part of the required
 * functionality is delegated to {@link #nativeDefinition} which inherits from that class.
 *
 * @see NativeShadowAttributeDefinition
 */
public abstract class ShadowAttributeDefinitionImpl<
        V extends PrismValue,
        D extends ShadowAttributeDefinition<V, D, RV, SA>,
        RV,
        SA extends ShadowAttribute<V, D, RV, SA>,
        ND extends NativeShadowAttributeDefinition>
        extends AbstractFreezable
        implements ShadowAttributeDefinition<V, D, RV, SA>, ShadowItemLifecycleDefinitionDefaults {

    @Serial private static final long serialVersionUID = 1L;

    /**
     * Default value for {@link #currentLayer}.
     */
    private static final LayerType DEFAULT_LAYER = LayerType.MODEL;

    /**
     * At what layer do we want to view property limitations ({@link #limitationsMap}).
     */
    @NotNull final LayerType currentLayer;

    /**
     * Native (raw) definition. It can come from these sources:
     *
     * . from the connector, as part of the native schema;
     * . manually filled-in by the administrator in `schema` part of the resource definition (in XSD form);
     * . *special for associations*: derived from the simulated association definition (legacy or capability format).
     *
     * The third case is a bit of hack, but it's needed to keep the code simple. The native definition is accessed from
     * too many places in this module. Note that it will never be serialized into the XSD schema in the resource definition
     * in repository.
     *
     * Always immutable. (The reason is mere simplicity. For example, {@link #limitationsMap} depends
     * on information here, so any updates would mean the need to recompute that map.)
     */
    @NotNull final ND nativeDefinition;

    /**
     * Customization from `schemaHandling`. If no matching value is present there, an empty one
     * is created (to avoid nullity checks throughput the code). This is also the case when
     * {@link ShadowAttributeDefinitionImpl} is used to hold a raw attribute definition
     * in {@link ResourceObjectClassDefinition}.
     *
     * Always immutable.
     */
    @NotNull final ResourceItemDefinitionType customizationBean;

    /**
     * Contains attribute limitations (minOccurs, maxOccurs, access, ...) for individual layers.
     *
     * Computed at construction time, then immutable.
     */
    final Map<LayerType, PropertyLimitations> limitationsMap;

    /**
     * Allows overriding read/add/modify access flags.
     *
     * Mutable by default.
     */
    final PropertyAccessType accessOverride;

    /**
     * @see ItemDefinition#structuredType()
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private transient @Nullable Optional<ComplexTypeDefinition> structuredType;

    /**
     * "Standard" constructor version (raw + customization).
     *
     * @throws SchemaException If there's a problem with parsing customization bean.
     */
    ShadowAttributeDefinitionImpl(
            @NotNull ND nativeDefinition, @NotNull ResourceItemDefinitionType customizationBean, boolean forcedIgnored)
            throws SchemaException {
        assert nativeDefinition.isImmutable();

        this.currentLayer = DEFAULT_LAYER;
        this.nativeDefinition = nativeDefinition;
        this.customizationBean = CloneUtil.toImmutable(customizationBean);
        this.limitationsMap = computeLimitationsMap(forcedIgnored);
        this.accessOverride = new PropertyAccessType();
    }

    /**
     * Version to be used for cloning and similar operations.
     */
    ShadowAttributeDefinitionImpl(
            @NotNull LayerType layer,
            @NotNull ND nativeDefinition,
            @NotNull ResourceItemDefinitionType customizationBean,
            @NotNull Map<LayerType, PropertyLimitations> limitationsMap,
            @NotNull PropertyAccessType accessOverride) {
        assert nativeDefinition.isImmutable();
        assert customizationBean.isImmutable();

        this.currentLayer = layer;
        this.nativeDefinition = nativeDefinition;
        this.customizationBean = customizationBean;
        this.limitationsMap = limitationsMap;
        this.accessOverride = accessOverride;
    }

    /**
     * Converts limitations embedded in {@link #nativeDefinition} and specified in {@link #customizationBean}
     * to the {@link #limitationsMap}.
     */
    private @NotNull Map<LayerType, PropertyLimitations> computeLimitationsMap(boolean forcedIgnored) throws SchemaException {
        Map<LayerType, PropertyLimitations> map = new HashMap<>();

        PropertyLimitations schemaLimitations = getOrCreateLimitationsForLayer(map, LayerType.SCHEMA);
        schemaLimitations.setMinOccurs(nativeDefinition.getMinOccurs());
        schemaLimitations.setMaxOccurs(nativeDefinition.getMaxOccurs());
        schemaLimitations.setProcessing(forcedIgnored ? ItemProcessing.IGNORE : nativeDefinition.getProcessing());
        schemaLimitations.getAccess().setAdd(nativeDefinition.canAdd());
        schemaLimitations.getAccess().setModify(nativeDefinition.canModify());
        schemaLimitations.getAccess().setRead(nativeDefinition.canRead());

        PropertyLimitations previousLimitations = null;
        for (LayerType layer : LayerType.values()) {
            PropertyLimitations limitations = getOrCreateLimitationsForLayer(map, layer);
            if (previousLimitations != null) {
                limitations.setMinOccurs(previousLimitations.getMinOccurs());
                limitations.setMaxOccurs(previousLimitations.getMaxOccurs());
                limitations.setProcessing(forcedIgnored ? ItemProcessing.IGNORE : previousLimitations.getProcessing());
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
                    applyLimitationsBean(limitations, genericLimitationsType, forcedIgnored);
                }
            }
            PropertyLimitationsType layerLimitationsType =
                    MiscSchemaUtil.getLimitationsLabeled(customizationBean.getLimitations(), layer);
            if (layerLimitationsType != null) {
                applyLimitationsBean(limitations, layerLimitationsType, forcedIgnored);
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

    @Override
    public DisplayHint getDisplayHint() {
        if (customizationBean.getDisplayHint() != null) {
            return MiscSchemaUtil.toDisplayHint(customizationBean.getDisplayHint());
        } else {
            return nativeDefinition.getDisplayHint();
        }
    }

    @Override
    public boolean isEmphasized() {
        if (customizationBean.isEmphasized() != null) {
            return customizationBean.isEmphasized();
        } else {
            return nativeDefinition.isEmphasized();
        }
    }

    @Override
    public ItemProcessing getProcessing() {
        return getProcessing(currentLayer);
    }

    @Override
    public ItemProcessing getProcessing(LayerType layer) {
        return limitationsMap.get(layer).getProcessing();
    }

    @Override
    public String getDisplayName() {
        return MiscUtil.orElseGet(
                customizationBean.getDisplayName(),
                nativeDefinition::getDisplayName);
    }

    @Override
    public Integer getDisplayOrder() {
        return MiscUtil.orElseGet(
                customizationBean.getDisplayOrder(),
                nativeDefinition::getDisplayOrder);
    }

    @Override
    public String getHelp() {
        return MiscUtil.orElseGet(
                customizationBean.getHelp(),
                nativeDefinition::getHelp);
    }

    @Override
    public String getDescription() {
        return customizationBean.getDescription();
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
        return nativeDefinition.getReturnedByDefault();
    }

    @Override
    public String getNativeAttributeName() {
        return nativeDefinition.getNativeAttributeName();
    }

    @Override
    public String getFrameworkAttributeName() {
        return nativeDefinition.getFrameworkAttributeName();
    }

    @Override
    public @NotNull SA instantiate() {
        return instantiate(
                getItemName());
    }

    @Override
    public @NotNull SA instantiate(QName name) {
        return instantiateFromQualifiedName(
                addNamespaceIfApplicable(name, getItemName()));
    }

    abstract SA instantiateFromQualifiedName(QName name);

    @Override
    public int getMaxOccurs() {
        return getMaxOccurs(currentLayer);
    }

    @Override
    public int getMaxOccurs(LayerType layer) {
        return limitationsMap.get(layer).getMaxOccurs();
    }

    @Override
    public @NotNull ItemName getItemName() {
        return nativeDefinition.getItemName();
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
                nativeDefinition::getDocumentation);
    }

    @Override
    public String getDocumentationPreview() {
        return nativeDefinition.getDocumentationPreview(); // probably null
    }

    @Override
    public @NotNull AttributeFetchStrategyType getFetchStrategy() {
        return Objects.requireNonNullElse(customizationBean.getFetchStrategy(), AttributeFetchStrategyType.IMPLICIT);
    }

    @Override
    public @NotNull AttributeStorageStrategyType getStorageStrategy() {
        if (customizationBean.getStorageStrategy() != null) {
            return customizationBean.getStorageStrategy();
        } else {
            return AttributeStorageStrategyType.NORMAL;
        }
    }

    @Override
    public boolean isIndexOnly() {
        return getStorageStrategy() == AttributeStorageStrategyType.INDEX_ONLY;
    }

    @Override
    public Boolean isCached() {
        return customizationBean.isCached();
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
        var legacy = Boolean.TRUE.equals(customizationBean.isVolatilityTrigger());
        var modern = isVolatilityTriggerModern();
        return legacy || modern;
    }

    private boolean isVolatilityTriggerModern() {
        var volatility = customizationBean.getVolatility();
        if (volatility == null) {
            return false;
        }
        return volatility.getOutgoing().stream().anyMatch(
                s -> s.getOperation().isEmpty() || s.getOperation().contains(ChangeTypeType.MODIFY));
    }

    @Override
    public boolean isVolatileOnAddOperation() {
        var volatility = customizationBean.getVolatility();
        if (volatility == null) {
            return false;
        }
        return volatility.getIncoming().stream().anyMatch(
                s -> s.getOperation().isEmpty() || s.getOperation().contains(ChangeTypeType.ADD));
    }

    @Override
    public boolean isVolatileOnModifyOperation() {
        var volatility = customizationBean.getVolatility();
        if (volatility == null) {
            return false;
        }
        return volatility.getIncoming().stream().anyMatch(
                s -> s.getOperation().isEmpty() || s.getOperation().contains(ChangeTypeType.MODIFY));
    }

    private static void applyLimitationsBean(
            PropertyLimitations limitations, PropertyLimitationsType layerLimitationsBean, boolean forcedIgnored) {
        if (layerLimitationsBean.getMinOccurs() != null) {
            limitations.setMinOccurs(
                    DefinitionUtil.parseMultiplicity(layerLimitationsBean.getMinOccurs()));
        }
        if (layerLimitationsBean.getMaxOccurs() != null) {
            limitations.setMaxOccurs(
                    DefinitionUtil.parseMultiplicity(layerLimitationsBean.getMaxOccurs()));
        }
        if (forcedIgnored) {
            limitations.setProcessing(ItemProcessing.IGNORE);
        } else if (layerLimitationsBean.getProcessing() != null) {
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
        if (hasOutboundMapping()) {
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
        extendToString(sb);
        return sb.toString();
    }

    protected abstract void extendToString(StringBuilder sb);

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
    public abstract String getDebugDumpClassName();

    public String debugDump(int indent) {
        return debugDump(indent, (LayerType) null);
    }

    public String debugDump(int indent, LayerType layer) {
        // TODO reconsider this method
        StringBuilder sb = DebugUtil.createTitleStringBuilder(getClass(), indent);
        sb.append(" ").append(this);
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
    public @Nullable ItemChangeApplicationModeType getChangeApplicationMode() {
        return customizationBean.getChangeApplicationMode();
    }

    @Override
    public @Nullable String getLifecycleState() {
        return customizationBean.getLifecycleState();
    }

    public Optional<ComplexTypeDefinition> structuredType() {
        //noinspection OptionalAssignedToNull
        if (structuredType == null) {
            this.structuredType = Optional.ofNullable(
                    PrismContext.get().getSchemaRegistry().findComplexTypeDefinitionByType(getTypeName()));
        }
        return structuredType;
    }

    @Override
    public void performFreeze() {
        stateCheck(nativeDefinition.isImmutable(), "Raw definition is not immutable");
        stateCheck(customizationBean.isImmutable(), "Customization bean is not immutable");
        // accessOverride should be frozen but there's currently no support for that
    }

    public boolean accept(Visitor<Definition> visitor, SmartVisitation<Definition> visitation) {
        //return nativeDefinition.accept(visitor, visitation);
        return true; // FIXME
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ShadowAttributeDefinitionImpl<?, ?, ?, ?, ?> that)) {
            return false;
        }
        return currentLayer == that.currentLayer
                && Objects.equals(nativeDefinition, that.nativeDefinition)
                && Objects.equals(customizationBean, that.customizationBean)
                && Objects.equals(limitationsMap, that.limitationsMap)
                && Objects.equals(accessOverride, that.accessOverride)
                && Objects.equals(structuredType, that.structuredType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currentLayer, nativeDefinition, customizationBean, limitationsMap, accessOverride, structuredType);
    }

    @Override
    public @NotNull LayerType getCurrentLayer() {
        return currentLayer;
    }

    public boolean isRuntimeSchema() {
        return true;
    }

    public <A> A getAnnotation(QName qname) {
        return null;
    }

    @Override
    public boolean hasRefinements() {
        return !customizationBean.asPrismContainerValue().isEmpty();
    }

    public void accept(Visitor<Definition> visitor) {
        //FIXME nativeDefinition.accept(visitor);
    }


    @Override
    public void shortDump(StringBuilder sb) {
        sb.append(this);
    }

    public boolean isAbstract() {
        return false;
    }

    public boolean isElaborate() {
        return false;
    }

    public boolean isOperational() {
        return false;
    }

    public boolean isInherited() {
        return false;
    }

    public boolean isDynamic() {
        return false;
    }

    public QName getSubstitutionHead() {
        return null;
    }

    public boolean isHeterogeneousListItem() {
        return false;
    }

    public PrismReferenceValue getValueEnumerationRef() {
        return null;
    }

    public List<SchemaMigration> getSchemaMigrations() {
        return null;
    }

    public List<ItemDiagramSpecification> getDiagrams() {
        return null;
    }

    public <A> void setAnnotation(QName qname, A value) {
        throw new UnsupportedOperationException();
    }

    public Map<QName, Object> getAnnotations() {
        return null;
    }

    public Boolean isIndexed() {
        if (getStorageStrategy() == AttributeStorageStrategyType.NOT_INDEXED) {
            return false;
        } else {
            // Returning 'null' which means default setting, depending on whether the specific type is supported (generic repo).
            // For native repo, all attributes (of supported types) are indexed, regardless of this value.
            return null;
        }
    }

    public boolean isOptionalCleanup() {
        return false; // TODO is this ok?
    }

    public boolean isAlwaysUseForEquals() {
        return true;
    }

    public @Nullable String getMergerIdentifier() {
        return null;
    }

    public @Nullable ItemMerger getMergerInstance(@NotNull MergeStrategy strategy, @Nullable OriginMarker originMarker) {
        return null;
    }

    public @Nullable List<QName> getNaturalKeyConstituents() {
        return List.of();
    }

    public @Nullable NaturalKeyDefinition getNaturalKeyInstance() {
        return null;
    }

    @Override
    public abstract @NotNull ShadowAttributeDefinitionImpl<V, D, RV, SA, ND> clone();
}
