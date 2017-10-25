/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.common.refinery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AttributeFetchStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import org.jetbrains.annotations.NotNull;

/**
 * @author semancik
 *
 */
public class LayerRefinedAttributeDefinitionImpl<T> implements LayerRefinedAttributeDefinition<T> {

	private RefinedAttributeDefinition<T> refinedAttributeDefinition;
	private LayerType layer;
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
		return new LayerRefinedAttributeDefinitionImpl<T>(rAttrDef, layer);
	}

	static List<LayerRefinedAttributeDefinition<?>> wrapCollection(
			Collection<? extends ItemDefinition> defs, LayerType layer) {
		List outs = new ArrayList<LayerRefinedAttributeDefinition<?>>(defs.size());
		for (ItemDefinition itemDef: defs) {
            if (itemDef instanceof LayerRefinedAttributeDefinition) {
                outs.add(itemDef);
            } else if (itemDef instanceof RefinedAttributeDefinition) {
                outs.add(wrap((RefinedAttributeDefinition)itemDef, layer));
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
		this.overrideCanRead = overrideCanRead;
	}

	@Override
	public Boolean getOverrideCanAdd() {
		return overrideCanAdd;
	}

	public void setOverrideCanAdd(Boolean overrideCanAdd) {
		this.overrideCanAdd = overrideCanAdd;
	}

	@Override
	public Boolean getOverrideCanModify() {
		return overrideCanModify;
	}

	public void setOverrideCanModify(Boolean overrideCanModify) {
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
	public boolean canModify() {
		if (overrideCanModify != null) {
			return overrideCanModify;
		}
		return refinedAttributeDefinition.canModify(layer);
	}

	//	@Override
//	public boolean isValidFor(QName elementQName, Class clazz) {
//		return isValidFor(elementQName, clazz, false);
//	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((layer == null) ? 0 : layer.hashCode());
		result = prime * result + ((refinedAttributeDefinition == null) ? 0 : refinedAttributeDefinition.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
//		if (!super.equals(obj))
//			return false;
		if (getClass() != obj.getClass())
			return false;
		LayerRefinedAttributeDefinitionImpl other = (LayerRefinedAttributeDefinitionImpl) obj;
		if (layer != other.layer)
			return false;
		if (refinedAttributeDefinition == null) {
			if (other.refinedAttributeDefinition != null)
				return false;
		} else if (!refinedAttributeDefinition.equals(other.refinedAttributeDefinition))
			return false;
		return true;
	}

	@Override
	public String debugDump() {
		return debugDump(0);
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
    protected String getDebugDumpClassName() {
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
	public <T extends ItemDefinition> T findItemDefinition(@NotNull ItemPath path, @NotNull Class<T> clazz) {
		return refinedAttributeDefinition.findItemDefinition(path, clazz);
	}

	@Override
	public boolean isDisplayNameAttribute() {
		return refinedAttributeDefinition.isDisplayNameAttribute();
	}

	@Override
	public ItemDefinition<PrismProperty<T>> deepClone(boolean ultraDeep) {
		return refinedAttributeDefinition.deepClone(ultraDeep);
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
	public boolean isExlusiveStrong() {
		return refinedAttributeDefinition.isExlusiveStrong();
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
		return refinedAttributeDefinition.instantiate();
	}

	@NotNull
	@Override
	public ResourceAttribute<T> instantiate(QName name) {
		return refinedAttributeDefinition.instantiate(name);
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
	public boolean isIdentifier(ResourceAttributeContainerDefinition objectDefinition) {
		return refinedAttributeDefinition.isIdentifier(objectDefinition);
	}

	@Override
	@NotNull
	public QName getName() {
		return refinedAttributeDefinition.getName();
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
	public boolean isIdentifier(ObjectClassComplexTypeDefinition objectDefinition) {
		return refinedAttributeDefinition.isIdentifier(objectDefinition);
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
	public boolean isSecondaryIdentifier() {
		return refinedAttributeDefinition.isSecondaryIdentifier();
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
	public boolean isOperational() {
		return refinedAttributeDefinition.isOperational();
	}

	@Override
	public PropertyDelta<T> createEmptyDelta(ItemPath path) {
		return refinedAttributeDefinition.createEmptyDelta(path);
	}

	@Override
	public boolean canModify(LayerType layer) {
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
	public RefinedAttributeDefinition<T> deepClone(Map<QName, ComplexTypeDefinition> ctdMap, Map<QName, ComplexTypeDefinition> onThisPath) {
		return new LayerRefinedAttributeDefinitionImpl<>(refinedAttributeDefinition.deepClone(ctdMap, onThisPath), layer);
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

	@Override
	public void setMaxOccurs(int maxOccurs) {
		refinedAttributeDefinition.setMaxOccurs(maxOccurs);
	}
}
