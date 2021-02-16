/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.common.refinery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AttributeFetchStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AttributeStorageStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import org.jetbrains.annotations.NotNull;

/**
 * @author semancik
 *
 */
public final class LayerRefinedAttributeDefinitionImpl<T> extends AbstractFreezable implements LayerRefinedAttributeDefinition<T> {

    private final RefinedAttributeDefinition<T> refinedAttributeDefinition;
    private final LayerType layer;
    private Boolean overrideCanRead = null;
    private Boolean overrideCanAdd = null;
    private Boolean overrideCanModify = null;

    private LayerRefinedAttributeDefinitionImpl(RefinedAttributeDefinition<T> refinedAttributeDefinition, LayerType layer) {
        this.refinedAttributeDefinition = refinedAttributeDefinition;
        this.layer = layer;
    }

    static <T> LayerRefinedAttributeDefinition<T> wrap(RefinedAttributeDefinition<T> rAttrDef, LayerType layer) {
        if (rAttrDef == null) {
            return null;
        }
        LayerRefinedAttributeDefinitionImpl<T> wrapped = new LayerRefinedAttributeDefinitionImpl<>(rAttrDef, layer);
        if (rAttrDef.isImmutable()) {
            wrapped.freeze();
        }
        return wrapped;
    }

    static List<LayerRefinedAttributeDefinition<?>> wrapCollection(
            Collection<? extends ItemDefinition> defs, LayerType layer) {
        List<LayerRefinedAttributeDefinition<?>> outs = new ArrayList<>(defs.size());
        for (ItemDefinition itemDef: defs) {
            if (itemDef instanceof LayerRefinedAttributeDefinition) {
                outs.add(((LayerRefinedAttributeDefinition<?>) itemDef));
            } else if (itemDef instanceof RefinedAttributeDefinition) {
                outs.add(wrap((RefinedAttributeDefinition<?>) itemDef, layer));
            } else {
                throw new IllegalStateException("Unexpected type of attribute definition: " + itemDef);
            }
        }
        return outs;
    }

    @Override
    public LayerType getLayer() {
        return layer;
    }

    @Override
    public Boolean getOverrideCanRead() {
        return overrideCanRead;
    }

    public void setOverrideCanRead(Boolean overrideCanRead) {
        checkMutable();
        this.overrideCanRead = overrideCanRead;
    }

    @Override
    public Boolean getOverrideCanAdd() {
        return overrideCanAdd;
    }

    public void setOverrideCanAdd(Boolean overrideCanAdd) {
        checkMutable();
        this.overrideCanAdd = overrideCanAdd;
    }

    @Override
    public Boolean getOverrideCanModify() {
        return overrideCanModify;
    }

    public void setOverrideCanModify(Boolean overrideCanModify) {
        checkMutable();
        this.overrideCanModify = overrideCanModify;
    }

    @Override
    public boolean canAdd() {
        if (overrideCanAdd != null) {
            return overrideCanAdd;
        }
        return refinedAttributeDefinition.canAdd(layer);
    }

    @Override
    public PropertyLimitations getLimitations() {
        return refinedAttributeDefinition.getLimitations(layer);
    }

    @Override
    public void accept(Visitor<Definition> visitor) {
        visitor.visit(this);
        refinedAttributeDefinition.accept(visitor);
    }

    // TODO reconsider this
    @Override
    public boolean accept(Visitor<Definition> visitor, SmartVisitation<Definition> visitation) {
        visitor.visit(this);
        refinedAttributeDefinition.accept(visitor, visitation);
        return true;
    }

    @NotNull
    @Override
    public RefinedAttributeDefinition<T> clone() {
        return refinedAttributeDefinition.clone();
    }

    // TODO ????????
    @Override
    public String debugDump(int indent, LayerType layer) {
        return refinedAttributeDefinition.debugDump(indent, layer);
    }

    @Override
    public boolean canRead() {
        if (overrideCanRead != null) {
            return overrideCanRead;
        }
        return refinedAttributeDefinition.canRead(layer);
    }

    @Override
    public boolean isIgnored(LayerType layer) {
        return refinedAttributeDefinition.isIgnored(layer);
    }

    @Override
    public ItemProcessing getProcessing() {
        return refinedAttributeDefinition.getProcessing(layer);
    }


    @Override
    public ItemProcessing getProcessing(LayerType layer) {
        return refinedAttributeDefinition.getProcessing(layer);
    }

    @Override
    public List<SchemaMigration> getSchemaMigrations() {
        return refinedAttributeDefinition.getSchemaMigrations();
    }

    @Override
    public boolean canModify() {
        if (overrideCanModify != null) {
            return overrideCanModify;
        }
        return refinedAttributeDefinition.canModify(layer);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 17;
        result = prime * result + ((layer == null) ? 0 : layer.hashCode());
        result = prime * result + ((refinedAttributeDefinition == null) ? 0 : refinedAttributeDefinition.hashCode());
        return result;
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (getClass() != obj.getClass()) return false;
        LayerRefinedAttributeDefinitionImpl other = (LayerRefinedAttributeDefinitionImpl) obj;
        if (layer != other.layer) return false;
        if (refinedAttributeDefinition == null) {
            if (other.refinedAttributeDefinition != null) return false;
        } else if (!refinedAttributeDefinition.equals(other.refinedAttributeDefinition)) {
            return false;
        }
        return true;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(getDebugDumpClassName()).append("(layer=").append(layer).append(",\n");
        sb.append(refinedAttributeDefinition.debugDump(indent+1, layer));
        return sb.toString();
    }

    /**
     * Return a human readable name of this class suitable for logs.
     */
    public String getDebugDumpClassName() {
        return "LRRAD";
    }

    //region Delegation (automatically generated)

    @Override
    public String getFrameworkAttributeName() {
        return refinedAttributeDefinition.getFrameworkAttributeName();
    }

    @Override
    public boolean isInherited() {
        return refinedAttributeDefinition.isInherited();
    }

    @Override
    public Integer getModificationPriority() {
        return refinedAttributeDefinition.getModificationPriority();
    }

    @Override
    public Boolean getReadReplaceMode() {
        return refinedAttributeDefinition.getReadReplaceMode();
    }

    @Override
    public <DT extends ItemDefinition> DT findItemDefinition(@NotNull ItemPath path, @NotNull Class<DT> clazz) {
        return refinedAttributeDefinition.findItemDefinition(path, clazz);
    }

    @Override
    public boolean isDisplayNameAttribute() {
        return refinedAttributeDefinition.isDisplayNameAttribute();
    }

    @Override
    public ItemDefinition<PrismProperty<T>> deepClone(boolean ultraDeep, Consumer<ItemDefinition> postCloneAction) {
        return refinedAttributeDefinition.deepClone(ultraDeep, postCloneAction);
    }

    @Override
    public void revive(PrismContext prismContext) {
        refinedAttributeDefinition.revive(prismContext);
    }

    @Override
    public Integer getDisplayOrder() {
        return refinedAttributeDefinition.getDisplayOrder();
    }

    @Override
    public String getHelp() {
        return refinedAttributeDefinition.getHelp();
    }

    @Override
    public String getDocumentation() {
        return refinedAttributeDefinition.getDocumentation();
    }

    @Override
    public String getDocumentationPreview() {
        return refinedAttributeDefinition.getDocumentationPreview();
    }

    @Override
    public boolean isRuntimeSchema() {
        return refinedAttributeDefinition.isRuntimeSchema();
    }

    @Override
    public PrismContext getPrismContext() {
        return refinedAttributeDefinition.getPrismContext();
    }

    @Override
    public Class getTypeClassIfKnown() {
        return refinedAttributeDefinition.getTypeClassIfKnown();
    }

    @Override
    public Class getTypeClass() {
        return refinedAttributeDefinition.getTypeClass();
    }

    @Override
    public MutableResourceAttributeDefinition<T> toMutable() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDescription() {
        return refinedAttributeDefinition.getDescription();
    }

    @Override
    public PrismReferenceValue getValueEnumerationRef() {
        return refinedAttributeDefinition.getValueEnumerationRef();
    }

    @Override
    public ResourceAttributeDefinition<T> getAttributeDefinition() {
        return refinedAttributeDefinition.getAttributeDefinition();
    }

    @Override
    public boolean isValidFor(QName elementQName, Class<? extends ItemDefinition> clazz) {
        return refinedAttributeDefinition.isValidFor(elementQName, clazz);
    }

    @Override
    public MappingType getOutboundMappingType() {
        return refinedAttributeDefinition.getOutboundMappingType();
    }

    @Override
    public boolean hasOutboundMapping() {
        return refinedAttributeDefinition.hasOutboundMapping();
    }

    @Override
    public boolean isValidFor(@NotNull QName elementQName,
            @NotNull Class<? extends ItemDefinition> clazz, boolean caseInsensitive) {
        return refinedAttributeDefinition.isValidFor(elementQName, clazz, caseInsensitive);
    }

    @Override
    public List<MappingType> getInboundMappingTypes() {
        return refinedAttributeDefinition.getInboundMappingTypes();
    }

    @Override
    public int getMaxOccurs(LayerType layer) {
        return refinedAttributeDefinition.getMaxOccurs(layer);
    }

    @Override
    public int getMinOccurs(LayerType layer) {
        return refinedAttributeDefinition.getMinOccurs(layer);
    }

    @Override
    public void adoptElementDefinitionFrom(ItemDefinition otherDef) {
        refinedAttributeDefinition.adoptElementDefinitionFrom(otherDef);
    }

    @Override
    public boolean isOptional(LayerType layer) {
        return refinedAttributeDefinition.isOptional(layer);
    }

    @Override
    public boolean isEmphasized() {
        return refinedAttributeDefinition.isEmphasized();
    }

    @Override
    public boolean isMandatory(LayerType layer) {
        return refinedAttributeDefinition.isMandatory(layer);
    }

    @Override
    public boolean isMultiValue(LayerType layer) {
        return refinedAttributeDefinition.isMultiValue(layer);
    }

    @Override
    public boolean isSingleValue(LayerType layer) {
        return refinedAttributeDefinition.isSingleValue(layer);
    }

    @Override
    public boolean isExclusiveStrong() {
        return refinedAttributeDefinition.isExclusiveStrong();
    }

    @Override
    public PropertyLimitations getLimitations(LayerType layer) {
        return refinedAttributeDefinition.getLimitations(layer);
    }

    @Override
    public AttributeFetchStrategyType getFetchStrategy() {
        return refinedAttributeDefinition.getFetchStrategy();
    }

    @Override
    public AttributeStorageStrategyType getStorageStrategy() {
        return refinedAttributeDefinition.getStorageStrategy();
    }

    @Override
    public List<String> getTolerantValuePattern() {
        return refinedAttributeDefinition.getTolerantValuePattern();
    }

    @Override
    public List<String> getIntolerantValuePattern() {
        return refinedAttributeDefinition.getIntolerantValuePattern();
    }

    @Override
    public boolean isVolatilityTrigger() {
        return refinedAttributeDefinition.isVolatilityTrigger();
    }

    @Override
    public String getDisplayName() {
        return refinedAttributeDefinition.getDisplayName();
    }


    @NotNull
    @Override
    public ResourceAttribute<T> instantiate() {
        @NotNull ResourceAttribute<T> resourceAttribute = refinedAttributeDefinition.instantiate();
        resourceAttribute.setDefinition(this);
        return resourceAttribute;
    }

    @NotNull
    @Override
    public ResourceAttribute<T> instantiate(QName name) {
        @NotNull ResourceAttribute<T> resourceAttribute = refinedAttributeDefinition.instantiate(name);
        resourceAttribute.setDefinition(this);
        return resourceAttribute;
    }

    @Override
    public Boolean getReturnedByDefault() {
        return refinedAttributeDefinition.getReturnedByDefault();
    }

    @Override
    public boolean isReturnedByDefault() {
        return refinedAttributeDefinition.isReturnedByDefault();
    }

    @Override
    public boolean isPrimaryIdentifier(ResourceAttributeContainerDefinition objectDefinition) {
        return refinedAttributeDefinition.isPrimaryIdentifier(objectDefinition);
    }

    @Override
    @NotNull
    public ItemName getItemName() {
        return refinedAttributeDefinition.getItemName();
    }

    @Override
    public String getNamespace() {
        return refinedAttributeDefinition.getNamespace();
    }

    @Override
    public int getMinOccurs() {
        return refinedAttributeDefinition.getMinOccurs(layer);
    }

    @Override
    public Collection<? extends DisplayableValue<T>> getAllowedValues() {
        return refinedAttributeDefinition.getAllowedValues();
    }

    @Override
    public int getMaxOccurs() {
        return refinedAttributeDefinition.getMaxOccurs(layer);
    }

    @Override
    public boolean isPrimaryIdentifier(ObjectClassComplexTypeDefinition objectDefinition) {
        return refinedAttributeDefinition.isPrimaryIdentifier(objectDefinition);
    }

    @Override
    public T defaultValue() {
        return refinedAttributeDefinition.defaultValue();
    }

    @Override
    public boolean isTolerant() {
        return refinedAttributeDefinition.isTolerant();
    }

    @Override
    public boolean isSingleValue() {
        return refinedAttributeDefinition.isSingleValue(layer);
    }

    @Override
    public QName getValueType() {
        return refinedAttributeDefinition.getValueType();
    }

    @Override
    public Boolean isSecondaryIdentifierOverride() {
        return refinedAttributeDefinition.isSecondaryIdentifierOverride();
    }

    @Override
    public boolean isMultiValue() {
        return refinedAttributeDefinition.isMultiValue(layer);
    }

    @Override
    @NotNull
    public QName getTypeName() {
        return refinedAttributeDefinition.getTypeName();
    }

    @Override
    public Boolean isIndexed() {
        return refinedAttributeDefinition.isIndexed();
    }

    @Override
    public boolean canAdd(LayerType layer) {
        if (this.layer == layer) {
            if (overrideCanAdd != null) {
                return overrideCanAdd;
            }
        }
        return refinedAttributeDefinition.canAdd(layer);
    }

    @Override
    public boolean isMandatory() {
        return refinedAttributeDefinition.isMandatory(layer);
    }

    @Override
    public boolean isIgnored() {
        return refinedAttributeDefinition.isIgnored(layer);
    }

    @Override
    public boolean isSecondaryIdentifier(ObjectClassComplexTypeDefinition objectDefinition) {
        return refinedAttributeDefinition.isSecondaryIdentifier(objectDefinition);
    }

    @Override
    public QName getMatchingRuleQName() {
        return refinedAttributeDefinition.getMatchingRuleQName();
    }

    @Override
    public boolean isAbstract() {
        return refinedAttributeDefinition.isAbstract();
    }

    @Override
    public boolean isOptional() {
        return refinedAttributeDefinition.isOptional(layer);
    }

    @Override
    public boolean canRead(LayerType layer) {
        if (this.layer == layer) {
            if (overrideCanRead != null) {
                return overrideCanRead;
            }
        }
        return refinedAttributeDefinition.canRead(layer);
    }

    @Override
    public boolean isDeprecated() {
        return refinedAttributeDefinition.isDeprecated();
    }

    @Override
    public String getDeprecatedSince() {
        return refinedAttributeDefinition.getDeprecatedSince();
    }

    @Override
    public boolean isExperimental() {
        return refinedAttributeDefinition.isExperimental();
    }

    @Override
    public String getPlannedRemoval() {
        return refinedAttributeDefinition.getPlannedRemoval();
    }

    @Override
    public boolean isElaborate() {
        return refinedAttributeDefinition.isElaborate();
    }

    @Override
    public boolean isOperational() {
        return refinedAttributeDefinition.isOperational();
    }

    @Override
    public boolean isIndexOnly() {
        return refinedAttributeDefinition.isIndexOnly();
    }

    @Override
    public PropertyDelta<T> createEmptyDelta(ItemPath path) {
        return refinedAttributeDefinition.createEmptyDelta(path);
    }

    @Override
    public boolean canModify(LayerType layer) {
        if (this.layer == layer) {
            if (overrideCanModify != null) {
                return overrideCanModify;
            }
        }
        return refinedAttributeDefinition.canModify(layer);
    }

    @Override
    public boolean isDynamic() {
        return refinedAttributeDefinition.isDynamic();
    }

    @Override
    public String getNativeAttributeName() {
        return refinedAttributeDefinition.getNativeAttributeName();
    }

    @Override
    public RefinedAttributeDefinition<T> deepClone(Map<QName, ComplexTypeDefinition> ctdMap, Map<QName, ComplexTypeDefinition> onThisPath, Consumer<ItemDefinition> postCloneAction) {
        return new LayerRefinedAttributeDefinitionImpl<>(refinedAttributeDefinition.deepClone(ctdMap, onThisPath, postCloneAction), layer);
    }

    @Override
    public QName getSubstitutionHead() {
        return refinedAttributeDefinition.getSubstitutionHead();
    }

    @Override
    public boolean isHeterogeneousListItem() {
        return refinedAttributeDefinition.isHeterogeneousListItem();
    }

    @Override
    public void debugDumpShortToString(StringBuilder sb) {
        refinedAttributeDefinition.debugDumpShortToString(sb);
    }

    //endregion

    //@Override
    //public void setMaxOccurs(int maxOccurs) {
    //    refinedAttributeDefinition.setMaxOccurs(maxOccurs);
    //}

    @Override
    public boolean canBeDefinitionOf(PrismProperty<T> item) {
        return refinedAttributeDefinition.canBeDefinitionOf(item);
    }

    @Override
    public boolean canBeDefinitionOf(PrismValue pvalue) {
        return refinedAttributeDefinition.canBeDefinitionOf(pvalue);
    }

    @Override
    public <A> A getAnnotation(QName qname) {
        return refinedAttributeDefinition.getAnnotation(qname);
    }

    @Override
    public <A> void setAnnotation(QName qname, A value) {
        refinedAttributeDefinition.setAnnotation(qname, value);
    }

    @Override
    public String toString() {
        return (isImmutable() ? "" : "+") + refinedAttributeDefinition + ":" + layer;
    }

    @Override
    public Optional<ComplexTypeDefinition> structuredType() {
        return refinedAttributeDefinition.structuredType();
    }
}
