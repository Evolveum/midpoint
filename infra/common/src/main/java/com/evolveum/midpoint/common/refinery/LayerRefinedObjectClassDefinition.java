/*
 * Copyright (c) 2010-2013 Evolveum
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

import com.evolveum.midpoint.common.ResourceObjectPattern;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AttributeFetchStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceActivationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceBidirectionalMappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowKindType;

/**
 * @author semancik
 *
 */
public class LayerRefinedObjectClassDefinition extends RefinedObjectClassDefinition {
	
	private RefinedObjectClassDefinition refinedObjectClassDefinition;
	private LayerType layer;
	
	private LayerRefinedObjectClassDefinition(RefinedObjectClassDefinition refinedAccountDefinition, LayerType layer) {
		super(new QName("fake"), refinedAccountDefinition.getPrismContext());
		this.refinedObjectClassDefinition = refinedAccountDefinition;
		this.layer = layer;
	}
	
	static LayerRefinedObjectClassDefinition wrap(RefinedObjectClassDefinition rAccountDef, LayerType layer) {
		return new LayerRefinedObjectClassDefinition(rAccountDef, layer);
	}
	
	static Collection<? extends LayerRefinedObjectClassDefinition> wrapCollection(Collection<? extends RefinedObjectClassDefinition> rAccountDefs, LayerType layer) {
		Collection<LayerRefinedObjectClassDefinition> outs = new ArrayList<LayerRefinedObjectClassDefinition>(rAccountDefs.size());
		for (RefinedObjectClassDefinition rAccountDef: rAccountDefs) {
			outs.add(wrap(rAccountDef, layer));
		}
		return outs;
	}

	public LayerType getLayer() {
		return layer;
	}

	public QName getTypeName() {
		return refinedObjectClassDefinition.getTypeName();
	}

	public QName getDefaultName() {
		return refinedObjectClassDefinition.getDefaultName();
	}

	public void setTypeName(QName typeName) {
		refinedObjectClassDefinition.setTypeName(typeName);
	}

	public ResourceAttributeDefinition getDescriptionAttribute() {
		return refinedObjectClassDefinition.getDescriptionAttribute();
	}

	public boolean isIgnored() {
		return refinedObjectClassDefinition.isIgnored();
	}

	public void setIgnored(boolean ignored) {
		refinedObjectClassDefinition.setIgnored(ignored);
	}

	public void setDescriptionAttribute(ResourceAttributeDefinition descriptionAttribute) {
		refinedObjectClassDefinition.setDescriptionAttribute(descriptionAttribute);
	}

	public LayerRefinedAttributeDefinition getNamingAttribute() {
		return LayerRefinedAttributeDefinition.wrap(refinedObjectClassDefinition.getNamingAttribute(), layer);
	}

	public String getNativeObjectClass() {
		return refinedObjectClassDefinition.getNativeObjectClass();
	}

	public Integer getDisplayOrder() {
		return refinedObjectClassDefinition.getDisplayOrder();
	}

	public boolean isDefaultInAKind() {
		return refinedObjectClassDefinition.isDefaultInAKind();
	}

	public void setDefaultInAKind(boolean defaultAccountType) {
		refinedObjectClassDefinition.setDefaultInAKind(defaultAccountType);
	}
	
	public ShadowKindType getKind() {
		return refinedObjectClassDefinition.getKind();
	}

	public void setKind(ShadowKindType kind) {
		refinedObjectClassDefinition.setKind(kind);
	}

	public AttributeFetchStrategyType getPasswordFetchStrategy() {
		return refinedObjectClassDefinition.getPasswordFetchStrategy();
	}

	public String getIntent() {
		return refinedObjectClassDefinition.getIntent();
	}

	public void setIntent(String accountTypeName) {
		refinedObjectClassDefinition.setIntent(accountTypeName);
	}

	public void setDisplayOrder(Integer displayOrder) {
		refinedObjectClassDefinition.setDisplayOrder(displayOrder);
	}

	public LayerRefinedAttributeDefinition getDisplayNameAttribute() {
		return LayerRefinedAttributeDefinition.wrap(refinedObjectClassDefinition.getDisplayNameAttribute(), layer);
	}

	public String getHelp() {
		return refinedObjectClassDefinition.getHelp();
	}

	public void setDisplayNameAttribute(QName displayName) {
		refinedObjectClassDefinition.setDisplayNameAttribute(displayName);
	}

	public Collection<? extends LayerRefinedAttributeDefinition> getIdentifiers() {
		return LayerRefinedAttributeDefinition.wrapCollection(refinedObjectClassDefinition.getIdentifiers(), layer);
	}

	public <D extends ItemDefinition> D findItemDefinition(QName name, Class<D> clazz) {
		D findItemDefinition = refinedObjectClassDefinition.findItemDefinition(name, clazz);
		return (D) LayerRefinedAttributeDefinition.wrap((RefinedAttributeDefinition) findItemDefinition, layer);
	}

	public void setHelp(String help) {
		refinedObjectClassDefinition.setHelp(help);
	}

	public Collection<? extends LayerRefinedAttributeDefinition> getSecondaryIdentifiers() {
		return LayerRefinedAttributeDefinition.wrapCollection(refinedObjectClassDefinition.getSecondaryIdentifiers(), layer);
	}

	public Class getTypeClass() {
		return refinedObjectClassDefinition.getTypeClass();
	}

	public Collection<ResourceObjectPattern> getProtectedObjectPatterns() {
		return refinedObjectClassDefinition.getProtectedObjectPatterns();
	}

	public PrismContext getPrismContext() {
		return refinedObjectClassDefinition.getPrismContext();
	}

	public void setNamingAttribute(ResourceAttributeDefinition namingAttribute) {
		refinedObjectClassDefinition.setNamingAttribute(namingAttribute);
	}

	public ResourceAttributeContainer instantiate(QName name) {
		return refinedObjectClassDefinition.instantiate(name);
	}

	public void setNamingAttribute(QName namingAttribute) {
		refinedObjectClassDefinition.setNamingAttribute(namingAttribute);
	}

	public PrismPropertyDefinition findPropertyDefinition(QName name) {
		return LayerRefinedAttributeDefinition.wrap((RefinedAttributeDefinition) refinedObjectClassDefinition.findPropertyDefinition(name), layer);
	}

	public RefinedAttributeDefinition findAttributeDefinition(QName elementQName) {
		RefinedAttributeDefinition attributeDefinition = refinedObjectClassDefinition.findAttributeDefinition(elementQName);
		return LayerRefinedAttributeDefinition.wrap(attributeDefinition, layer);
	}

	public void setNativeObjectClass(String nativeObjectClass) {
		refinedObjectClassDefinition.setNativeObjectClass(nativeObjectClass);
	}

	public LayerRefinedAttributeDefinition findAttributeDefinition(String elementLocalname) {
		return LayerRefinedAttributeDefinition.wrap(refinedObjectClassDefinition.findAttributeDefinition(elementLocalname), layer);
	}

	public String getDisplayName() {
		return refinedObjectClassDefinition.getDisplayName();
	}

	public void setDisplayName(String displayName) {
		refinedObjectClassDefinition.setDisplayName(displayName);
	}
	
	public List<? extends ItemDefinition> getDefinitions() {
		return LayerRefinedAttributeDefinition.wrapCollection(refinedObjectClassDefinition.getDefinitions(), layer);
	}

	public String getDescription() {
		return refinedObjectClassDefinition.getDescription();
	}

	public void setDescription(String description) {
		refinedObjectClassDefinition.setDescription(description);
	}

	public boolean isDefault() {
		return refinedObjectClassDefinition.isDefault();
	}

	public void setDefault(boolean isDefault) {
		refinedObjectClassDefinition.setDefault(isDefault);
	}

	public ObjectClassComplexTypeDefinition getObjectClassDefinition() {
		return refinedObjectClassDefinition.getObjectClassDefinition();
	}

	public void setObjectClassDefinition(ObjectClassComplexTypeDefinition objectClassDefinition) {
		refinedObjectClassDefinition.setObjectClassDefinition(objectClassDefinition);
	}

	public Collection<? extends LayerRefinedAttributeDefinition> getAttributeDefinitions() {
		return LayerRefinedAttributeDefinition.wrapCollection(refinedObjectClassDefinition.getAttributeDefinitions(), layer);
	}

	public ResourceType getResourceType() {
		return refinedObjectClassDefinition.getResourceType();
	}

	public PrismObjectDefinition<ShadowType> getObjectDefinition() {
		return refinedObjectClassDefinition.getObjectDefinition();
	}

	public void setDisplayNameAttribute(ResourceAttributeDefinition displayName) {
		refinedObjectClassDefinition.setDisplayNameAttribute(displayName);
	}

	public RefinedAttributeDefinition getAttributeDefinition(QName attributeName) {
		return refinedObjectClassDefinition.getAttributeDefinition(attributeName);
	}

	public boolean containsAttributeDefinition(QName attributeName) {
		return refinedObjectClassDefinition.containsAttributeDefinition(attributeName);
	}

	public PrismPropertyDefinition createPropertyDefinition(String localName, QName typeName) {
		return refinedObjectClassDefinition.createPropertyDefinition(localName, typeName);
	}

	public boolean isEmpty() {
		return refinedObjectClassDefinition.isEmpty();
	}

	public PrismObject<ShadowType> createBlankShadow() {
		return refinedObjectClassDefinition.createBlankShadow();
	}

	public ResourceShadowDiscriminator getShadowDiscriminator() {
		return refinedObjectClassDefinition.getShadowDiscriminator();
	}

	public Collection<? extends QName> getNamesOfAttributesWithOutboundExpressions() {
		return refinedObjectClassDefinition.getNamesOfAttributesWithOutboundExpressions();
	}

	public Collection<? extends QName> getNamesOfAttributesWithInboundExpressions() {
		return refinedObjectClassDefinition.getNamesOfAttributesWithInboundExpressions();
	}

	public MappingType getCredentialsInbound() {
		return refinedObjectClassDefinition.getCredentialsInbound();
	}

	public MappingType getCredentialsOutbound() {
		return refinedObjectClassDefinition.getCredentialsOutbound();
	}

	public ObjectReferenceType getPasswordPolicy() {
		return refinedObjectClassDefinition.getPasswordPolicy();
	}

	

	public Class<?> getCompileTimeClass() {
		return refinedObjectClassDefinition.getCompileTimeClass();
	}

	public QName getExtensionForType() {
		return refinedObjectClassDefinition.getExtensionForType();
	}

	public boolean isContainerMarker() {
		return refinedObjectClassDefinition.isContainerMarker();
	}

	public boolean isIdentifier(QName attrName) {
		return refinedObjectClassDefinition.isIdentifier(attrName);
	}

	public boolean isObjectMarker() {
		return refinedObjectClassDefinition.isObjectMarker();
	}

	public boolean isXsdAnyMarker() {
		return refinedObjectClassDefinition.isXsdAnyMarker();
	}

	public QName getSuperType() {
		return refinedObjectClassDefinition.getSuperType();
	}

	public boolean isSecondaryIdentifier(QName attrName) {
		return refinedObjectClassDefinition.isSecondaryIdentifier(attrName);
	}

	public boolean isRuntimeSchema() {
		return refinedObjectClassDefinition.isRuntimeSchema();
	}

	public Collection<ResourceObjectAssociationType> getAssociations() {
		return refinedObjectClassDefinition.getAssociations();
	}

	public Collection<ResourceObjectAssociationType> getAssociations(ShadowKindType kind) {
		return refinedObjectClassDefinition.getAssociations(kind);
	}

	public ResourceAttributeContainerDefinition toResourceAttributeContainerDefinition() {
		ResourceAttributeContainerDefinition resourceAttributeContainerDefinition = refinedObjectClassDefinition.toResourceAttributeContainerDefinition();
		resourceAttributeContainerDefinition.setComplexTypeDefinition(this);
		return resourceAttributeContainerDefinition;
	}

	public ResourceAttributeContainerDefinition toResourceAttributeContainerDefinition(QName elementName) {
		ResourceAttributeContainerDefinition resourceAttributeContainerDefinition = refinedObjectClassDefinition.toResourceAttributeContainerDefinition(elementName);
		resourceAttributeContainerDefinition.setComplexTypeDefinition(this);
		return resourceAttributeContainerDefinition;
	}

	public ResourceActivationDefinitionType getActivationSchemaHandling() {
		return refinedObjectClassDefinition.getActivationSchemaHandling();
	}

	public ResourceBidirectionalMappingType getActivationBidirectionalMappingType(QName propertyName) {
		return refinedObjectClassDefinition.getActivationBidirectionalMappingType(propertyName);
	}

	public AttributeFetchStrategyType getActivationFetchStrategy(QName propertyName) {
		return refinedObjectClassDefinition.getActivationFetchStrategy(propertyName);
	}

	public Collection<ResourceObjectAssociationType> getEntitlementAssociations() {
		return refinedObjectClassDefinition.getEntitlementAssociations();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((layer == null) ? 0 : layer.hashCode());
		result = prime * result + ((refinedObjectClassDefinition == null) ? 0 : refinedObjectClassDefinition.hashCode());
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
		LayerRefinedObjectClassDefinition other = (LayerRefinedObjectClassDefinition) obj;
		if (layer != other.layer)
			return false;
		if (refinedObjectClassDefinition == null) {
			if (other.refinedObjectClassDefinition != null)
				return false;
		} else if (!refinedObjectClassDefinition.equals(other.refinedObjectClassDefinition))
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
		sb.append(refinedObjectClassDefinition.debugDump(indent+1));
		return sb.toString();
	}

	@Override
	public String dump() {
		return debugDump();
	}
	
	/**
     * Return a human readable name of this class suitable for logs.
     */
    @Override
    protected String getDebugDumpClassName() {
        return "LRObjectClassDef";
    }
	
}
