/*
 * Copyright (c) 2010-2015 Evolveum
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

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
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

/**
 * @author semancik
 *
 */
public class LayerRefinedAttributeDefinition<T> extends RefinedAttributeDefinition<T> {
	
	private RefinedAttributeDefinition<T> refinedAttributeDefinition;
	private LayerType layer;
	private Boolean overrideCanRead = null;
	private Boolean overrideCanAdd = null;
	private Boolean overrideCanModify = null;

	private LayerRefinedAttributeDefinition(RefinedAttributeDefinition<T> refinedAttributeDefinition, LayerType layer) {
		super(refinedAttributeDefinition, refinedAttributeDefinition.getPrismContext());
		this.refinedAttributeDefinition = refinedAttributeDefinition;
		this.layer = layer;
	}

	static <T> LayerRefinedAttributeDefinition<T> wrap(RefinedAttributeDefinition<T> rAttrDef, LayerType layer) {
		if (rAttrDef == null) {
			return null;
		}
		return new LayerRefinedAttributeDefinition<T>(rAttrDef, layer);
	}
	
	static List<LayerRefinedAttributeDefinition> wrapCollection(
			Collection<? extends ItemDefinition> defs, LayerType layer) {
		List outs = new ArrayList<LayerRefinedAttributeDefinition>(defs.size());
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

	public LayerType getLayer() {
		return layer;
	}

	@Override
	public ResourceAttribute instantiate() {
		return refinedAttributeDefinition.instantiate();
	}

	@Override
	public ResourceAttribute instantiate(QName name) {
		return refinedAttributeDefinition.instantiate(name);
	}

	@Override
	public boolean isIdentifier(ResourceAttributeContainerDefinition objectDefinition) {
		return refinedAttributeDefinition.isIdentifier(objectDefinition);
	}

	@Override
	public boolean isIdentifier(ObjectClassComplexTypeDefinition objectDefinition) {
		return refinedAttributeDefinition.isIdentifier(objectDefinition);
	}

	@Override
	public void setNativeAttributeName(String nativeAttributeName) {
		refinedAttributeDefinition.setNativeAttributeName(nativeAttributeName);
	}

	@Override
	public boolean isSecondaryIdentifier(ObjectClassComplexTypeDefinition objectDefinition) {
		return refinedAttributeDefinition.isSecondaryIdentifier(objectDefinition);
	}

	@Override
	public boolean isTolerant() {
		return refinedAttributeDefinition.isTolerant();
	}

	@Override
	public void setTolerant(boolean tolerant) {
		refinedAttributeDefinition.setTolerant(tolerant);
	}
	
	public Boolean getOverrideCanRead() {
		return overrideCanRead;
	}

	public void setOverrideCanRead(Boolean overrideCanRead) {
		this.overrideCanRead = overrideCanRead;
	}

	public Boolean getOverrideCanAdd() {
		return overrideCanAdd;
	}

	public void setOverrideCanAdd(Boolean overrideCanAdd) {
		this.overrideCanAdd = overrideCanAdd;
	}

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
	public boolean canAdd(LayerType layer) {
		return refinedAttributeDefinition.canAdd(layer);
	}

	@Override
	public boolean canRead() {
		if (overrideCanRead != null) {
			return overrideCanRead;
		}
		return refinedAttributeDefinition.canRead(layer);
	}

	@Override
	public boolean canRead(LayerType layer) {
		return refinedAttributeDefinition.canRead(layer);
	}

	@Override
	public boolean canModify() {
		if (overrideCanModify != null) {
			return overrideCanModify;
		}
		return refinedAttributeDefinition.canModify(layer);
	}

	@Override
	public void setName(QName name) {
		refinedAttributeDefinition.setName(name);
	}

	@Override
	public boolean canModify(LayerType layer) {
		return refinedAttributeDefinition.canModify(layer);
	}

	@Override
	public void setReadOnly() {
		refinedAttributeDefinition.setReadOnly();
	}

	@Override
	public void setTypeName(QName typeName) {
		refinedAttributeDefinition.setTypeName(typeName);
	}

	@Override
	public QName getValueType() {
		return refinedAttributeDefinition.getValueType();
	}

	@Override
	public String getNamespace() {
		return refinedAttributeDefinition.getNamespace();
	}

	@Override
	public Boolean isIndexed() {
		return refinedAttributeDefinition.isIndexed();
	}

	@Override
	public void setMinOccurs(int minOccurs) {
		refinedAttributeDefinition.setMinOccurs(minOccurs);
	}

	@Override
	public void setMaxOccurs(int maxOccurs) {
		refinedAttributeDefinition.setMaxOccurs(maxOccurs);
	}

	@Override
	public void setCanRead(boolean read) {
		refinedAttributeDefinition.setCanRead(read);
	}

	@Override
	public void setCanModify(boolean update) {
		refinedAttributeDefinition.setCanModify(update);
	}

	@Override
	public void setIndexed(Boolean indexed) {
		refinedAttributeDefinition.setIndexed(indexed);
	}

	@Override
	public Integer getDisplayOrder() {
		return refinedAttributeDefinition.getDisplayOrder();
	}

	@Override
	public boolean isSingleValue() {
		return refinedAttributeDefinition.isSingleValue(layer);
	}

	@Override
	public void setCanAdd(boolean create) {
		refinedAttributeDefinition.setCanAdd(create);
	}

	@Override
	public PropertyDelta<T> createEmptyDelta(ItemPath path) {
		return refinedAttributeDefinition.createEmptyDelta(path);
	}

	@Override
	public boolean isMultiValue() {
		return refinedAttributeDefinition.isMultiValue(layer);
	}

	@Override
	public boolean isIgnored() {
		return refinedAttributeDefinition.isIgnored(layer);
	}

	@Override
	public boolean isIgnored(LayerType layer) {
		return refinedAttributeDefinition.isIgnored(layer);
	}

	@Override
	public void setDisplayOrder(Integer displayOrder) {
		refinedAttributeDefinition.setDisplayOrder(displayOrder);
	}

	@Override
	public boolean isMandatory() {
		return refinedAttributeDefinition.isMandatory(layer);
	}

	@Override
	public void setIgnored(boolean ignored) {
		refinedAttributeDefinition.setIgnored(ignored);
	}

	@Override
	public boolean isOptional() {
		return refinedAttributeDefinition.isOptional(layer);
	}

	@Override
	public void setHelp(String help) {
		refinedAttributeDefinition.setHelp(help);
	}

	@Override
	public String getDisplayName() {
		return refinedAttributeDefinition.getDisplayName();
	}

	@Override
	public boolean isDynamic() {
		return refinedAttributeDefinition.isDynamic();
	}

	@Override
	public void setDisplayName(String displayName) {
		refinedAttributeDefinition.setDisplayName(displayName);
	}

	@Override
	public String getDescription() {
		return refinedAttributeDefinition.getDescription();
	}

	@Override
	public PrismContext getPrismContext() {
		return refinedAttributeDefinition.getPrismContext();
	}

	@Override
	public void setDescription(String description) {
		refinedAttributeDefinition.setDescription(description);
	}

	@Override
	public Class<T> getTypeClass() {
		return refinedAttributeDefinition.getTypeClass();
	}

	@Override
	public ResourceAttributeDefinition<T> getAttributeDefinition() {
		return refinedAttributeDefinition.getAttributeDefinition();
	}

	@Override
	public void setAttributeDefinition(ResourceAttributeDefinition<T> attributeDefinition) {
		refinedAttributeDefinition.setAttributeDefinition(attributeDefinition);
	}

	@Override
	public void setDynamic(boolean dynamic) {
		refinedAttributeDefinition.setDynamic(dynamic);
	}

	@Override
	public boolean isValidFor(QName elementQName, Class<? extends ItemDefinition> clazz) {
		return isValidFor(elementQName, clazz, false);
	}

	@Override
	public boolean isValidFor(QName elementQName, Class<? extends ItemDefinition> clazz, boolean caseInsensitive) {
		return refinedAttributeDefinition.isValidFor(elementQName, clazz, caseInsensitive);
	}

	public Boolean getReturnedByDefault() {
		return refinedAttributeDefinition.getReturnedByDefault();
	}

	public boolean isAbstract() {
		return refinedAttributeDefinition.isAbstract();
	}

	public boolean isDeprecated() {
		return refinedAttributeDefinition.isDeprecated();
	}

	public boolean isOperational() {
		return refinedAttributeDefinition.isOperational();
	}

	public String getDocumentation() {
		return refinedAttributeDefinition.getDocumentation();
	}

	public String getDocumentationPreview() {
		return refinedAttributeDefinition.getDocumentationPreview();
	}

	public boolean isRuntimeSchema() {
		return refinedAttributeDefinition.isRuntimeSchema();
	}

	public boolean isReturnedByDefault() {
		return refinedAttributeDefinition.isReturnedByDefault();
	}

	public String getDocClassName() {
		return refinedAttributeDefinition.getDocClassName();
	}

	public boolean isExlusiveStrong() {
		return refinedAttributeDefinition.isExlusiveStrong();
	}

	public AttributeFetchStrategyType getFetchStrategy() {
		return refinedAttributeDefinition.getFetchStrategy();
	}

	public List<String> getTolerantValuePattern() {
		return refinedAttributeDefinition.getTolerantValuePattern();
	}

	public List<String> getIntolerantValuePattern() {
		return refinedAttributeDefinition.getIntolerantValuePattern();
	}

	@Override
	public MappingType getOutboundMappingType() {
		return refinedAttributeDefinition.getOutboundMappingType();
	}

	@Override
	public void setOutboundMappingType(MappingType outboundMappingType) {
		refinedAttributeDefinition.setOutboundMappingType(outboundMappingType);
	}

	@Override
	public boolean hasOutboundMapping() {
		return refinedAttributeDefinition.hasOutboundMapping();
	}

	@Override
	public List<MappingType> getInboundMappingTypes() {
		return refinedAttributeDefinition.getInboundMappingTypes();
	}

	@Override
	public void setInboundMappingTypes(List<MappingType> inboundAssignmentTypes) {
		refinedAttributeDefinition.setInboundMappingTypes(inboundAssignmentTypes);
	}

	@Override
	public QName getName() {
		return refinedAttributeDefinition.getName();
	}

	@Override
	public QName getTypeName() {
		return refinedAttributeDefinition.getTypeName();
	}

	@Override
	public String getNativeAttributeName() {
		return refinedAttributeDefinition.getNativeAttributeName();
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
	public int getMaxOccurs(LayerType layer) {
		return refinedAttributeDefinition.getMaxOccurs(layer);
	}

	@Override
	public int getMinOccurs() {
		return refinedAttributeDefinition.getMinOccurs(layer);
	}

	@Override
	public int getMinOccurs(LayerType layer) {
		return refinedAttributeDefinition.getMinOccurs(layer);
	}

	@Override
	public PropertyLimitations getLimitations(LayerType layer) {
		return refinedAttributeDefinition.getLimitations(layer);
	}
	
	public PropertyLimitations getLimitations() {
		return refinedAttributeDefinition.getLimitations(layer);
	}

	@Override
	public String getHelp() {
		return refinedAttributeDefinition.getHelp();
	}

	public QName getMatchingRuleQName() {
		return refinedAttributeDefinition.getMatchingRuleQName();
	}

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
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		LayerRefinedAttributeDefinition other = (LayerRefinedAttributeDefinition) obj;
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
    @Override
    protected String getDebugDumpClassName() {
        return "LRRAD";
    }
	
}
