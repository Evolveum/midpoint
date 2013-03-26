/**
 * Copyright (c) 2013 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */
package com.evolveum.midpoint.common.refinery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.ResourceObjectPattern;
import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AttributeFetchStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowType;
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
		super(refinedAccountDefinition.getPrismContext());
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

	public QName getName() {
		return refinedObjectClassDefinition.getName();
	}

	public void setName(QName name) {
		refinedObjectClassDefinition.setName(name);
	}

	public QName getTypeName() {
		return refinedObjectClassDefinition.getTypeName();
	}

	public QName getDefaultName() {
		return refinedObjectClassDefinition.getDefaultName();
	}

	public QName getNameOrDefaultName() {
		return refinedObjectClassDefinition.getNameOrDefaultName();
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

	public int getMinOccurs() {
		return refinedObjectClassDefinition.getMinOccurs();
	}

	public void setMinOccurs(int minOccurs) {
		refinedObjectClassDefinition.setMinOccurs(minOccurs);
	}

	public LayerRefinedAttributeDefinition getNamingAttribute() {
		return LayerRefinedAttributeDefinition.wrap(refinedObjectClassDefinition.getNamingAttribute(), layer);
	}

	public int getMaxOccurs() {
		return refinedObjectClassDefinition.getMaxOccurs();
	}

	public String getNativeObjectClass() {
		return refinedObjectClassDefinition.getNativeObjectClass();
	}

	public void setMaxOccurs(int maxOccurs) {
		refinedObjectClassDefinition.setMaxOccurs(maxOccurs);
	}

	public Integer getDisplayOrder() {
		return refinedObjectClassDefinition.getDisplayOrder();
	}

	public Class<ResourceObjectShadowAttributesType> getCompileTimeClass() {
		return refinedObjectClassDefinition.getCompileTimeClass();
	}

	public boolean isSingleValue() {
		return refinedObjectClassDefinition.isSingleValue();
	}

	public boolean isDefaultInAKind() {
		return refinedObjectClassDefinition.isDefaultInAKind();
	}

	public void setDefaultInAKind(boolean defaultAccountType) {
		refinedObjectClassDefinition.setDefaultInAKind(defaultAccountType);
	}

	public void setCompileTimeClass(Class<ResourceObjectShadowAttributesType> compileTimeClass) {
		refinedObjectClassDefinition.setCompileTimeClass(compileTimeClass);
	}

	public boolean isMultiValue() {
		return refinedObjectClassDefinition.isMultiValue();
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

	public boolean isMandatory() {
		return refinedObjectClassDefinition.isMandatory();
	}

	public LayerRefinedAttributeDefinition getDisplayNameAttribute() {
		return LayerRefinedAttributeDefinition.wrap(refinedObjectClassDefinition.getDisplayNameAttribute(), layer);
	}

	public String getHelp() {
		return refinedObjectClassDefinition.getHelp();
	}

	public void setComplexTypeDefinition(ComplexTypeDefinition complexTypeDefinition) {
		refinedObjectClassDefinition.setComplexTypeDefinition(complexTypeDefinition);
	}

	public boolean isOptional() {
		return refinedObjectClassDefinition.isOptional();
	}

	public void setDisplayNameAttribute(QName displayName) {
		refinedObjectClassDefinition.setDisplayNameAttribute(displayName);
	}

	public Collection<? extends LayerRefinedAttributeDefinition> getIdentifiers() {
		return LayerRefinedAttributeDefinition.wrapCollection(refinedObjectClassDefinition.getIdentifiers(), layer);
	}

	public boolean isDynamic() {
		return refinedObjectClassDefinition.isDynamic();
	}

	public <D extends ItemDefinition> D findItemDefinition(QName name, Class<D> clazz) {
		return refinedObjectClassDefinition.findItemDefinition(name, clazz);
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

	public void setDynamic(boolean dynamic) {
		refinedObjectClassDefinition.setDynamic(dynamic);
	}

	public Collection<ResourceObjectPattern> getProtectedObjectPatterns() {
		return refinedObjectClassDefinition.getProtectedObjectPatterns();
	}

	public boolean isValidFor(QName elementQName, Class<? extends ItemDefinition> clazz) {
		return refinedObjectClassDefinition.isValidFor(elementQName, clazz);
	}

	public PrismContext getPrismContext() {
		return refinedObjectClassDefinition.getPrismContext();
	}

	public ObjectClassComplexTypeDefinition getComplexTypeDefinition() {
		return refinedObjectClassDefinition.getComplexTypeDefinition();
	}

	public <T extends ItemDefinition> T findItemDefinition(ItemPath path, Class<T> clazz) {
		return refinedObjectClassDefinition.findItemDefinition(path, clazz);
	}

	public ResourceAttributeContainer instantiate() {
		return refinedObjectClassDefinition.instantiate();
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

	public ItemDefinition findItemDefinition(QName name) {
		return refinedObjectClassDefinition.findItemDefinition(name);
	}

	public ItemDefinition findItemDefinition(ItemPath path) {
		return refinedObjectClassDefinition.findItemDefinition(path);
	}

	public PrismPropertyDefinition findPropertyDefinition(QName name) {
		return refinedObjectClassDefinition.findPropertyDefinition(name);
	}

	public RefinedAttributeDefinition findAttributeDefinition(QName elementQName) {
		return refinedObjectClassDefinition.findAttributeDefinition(elementQName);
	}

	public PrismPropertyDefinition findPropertyDefinition(ItemPath path) {
		return refinedObjectClassDefinition.findPropertyDefinition(path);
	}

	public void setNativeObjectClass(String nativeObjectClass) {
		refinedObjectClassDefinition.setNativeObjectClass(nativeObjectClass);
	}

	public LayerRefinedAttributeDefinition findAttributeDefinition(String elementLocalname) {
		return LayerRefinedAttributeDefinition.wrap(refinedObjectClassDefinition.findAttributeDefinition(elementLocalname), layer);
	}

	public PrismReferenceDefinition findReferenceDefinition(QName name) {
		return refinedObjectClassDefinition.findReferenceDefinition(name);
	}

	public String getDisplayName() {
		return refinedObjectClassDefinition.getDisplayName();
	}

	public <X extends Containerable> PrismContainerDefinition<X> findContainerDefinition(QName name) {
		return refinedObjectClassDefinition.findContainerDefinition(name);
	}

	public <X extends Containerable> PrismContainerDefinition<X> findContainerDefinition(String name) {
		return refinedObjectClassDefinition.findContainerDefinition(name);
	}

	public PrismContainerDefinition findContainerDefinition(ItemPath path) {
		return refinedObjectClassDefinition.findContainerDefinition(path);
	}

	public void setDisplayName(String displayName) {
		refinedObjectClassDefinition.setDisplayName(displayName);
	}

	public boolean isRuntimeSchema() {
		return refinedObjectClassDefinition.isRuntimeSchema();
	}

	public String getDescription() {
		return refinedObjectClassDefinition.getDescription();
	}

	public void setDescription(String description) {
		refinedObjectClassDefinition.setDescription(description);
	}

	public String getNamespace() {
		return refinedObjectClassDefinition.getNamespace();
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

	public List<PrismPropertyDefinition> getPropertyDefinitions() {
		return refinedObjectClassDefinition.getPropertyDefinitions();
	}

	public List<ItemDefinition> getDefinitions() {
		return refinedObjectClassDefinition.getDefinitions();
	}

	public ResourceType getResourceType() {
		return refinedObjectClassDefinition.getResourceType();
	}

	public PrismObjectDefinition<AccountShadowType> getObjectDefinition() {
		return refinedObjectClassDefinition.getObjectDefinition();
	}

	public void setRuntimeSchema(boolean isRuntimeSchema) {
		refinedObjectClassDefinition.setRuntimeSchema(isRuntimeSchema);
	}

	public void setDisplayNameAttribute(ResourceAttributeDefinition displayName) {
		refinedObjectClassDefinition.setDisplayNameAttribute(displayName);
	}

	public RefinedAttributeDefinition getAttributeDefinition(QName attributeName) {
		return refinedObjectClassDefinition.getAttributeDefinition(attributeName);
	}

	public ItemDelta createEmptyDelta(ItemPath path) {
		return refinedObjectClassDefinition.createEmptyDelta(path);
	}

	public boolean containsAttributeDefinition(QName attributeName) {
		return refinedObjectClassDefinition.containsAttributeDefinition(attributeName);
	}

	public PrismPropertyDefinition createPropertyDefinition(QName name, QName typeName) {
		return refinedObjectClassDefinition.createPropertyDefinition(name, typeName);
	}

	public ResourceAttributeDefinition findAttributeDefinition(ItemPath elementPath) {
		return refinedObjectClassDefinition.findAttributeDefinition(elementPath);
	}

	public PrismPropertyDefinition createPropertyDefinition(QName name, QName typeName, int minOccurs, int maxOccurs) {
		return refinedObjectClassDefinition.createPropertyDefinition(name, typeName, minOccurs, maxOccurs);
	}

	public <T extends ResourceObjectShadowType> PrismObjectDefinition<T> toShadowDefinition() {
		return refinedObjectClassDefinition.toShadowDefinition();
	}

	public PrismPropertyDefinition createPropertyDefinition(QName name) {
		return refinedObjectClassDefinition.createPropertyDefinition(name);
	}

	public PrismPropertyDefinition createPropertyDefinition(String localName, QName typeName) {
		return refinedObjectClassDefinition.createPropertyDefinition(localName, typeName);
	}

	public PrismPropertyDefinition createPropertyDefinition(String localName, String localTypeName) {
		return refinedObjectClassDefinition.createPropertyDefinition(localName, localTypeName);
	}

	public PrismPropertyDefinition createPropertyDefinition(String localName, String localTypeName, int minOccurs,
			int maxOccurs) {
		return refinedObjectClassDefinition.createPropertyDefinition(localName, localTypeName, minOccurs, maxOccurs);
	}

	public PrismContainerDefinition createContainerDefinition(QName name, QName typeName) {
		return refinedObjectClassDefinition.createContainerDefinition(name, typeName);
	}

	public PrismContainerDefinition createContainerDefinition(QName name, QName typeName, int minOccurs, int maxOccurs) {
		return refinedObjectClassDefinition.createContainerDefinition(name, typeName, minOccurs, maxOccurs);
	}

	public PrismContainerDefinition<ResourceObjectShadowAttributesType> createContainerDefinition(QName name,
			ComplexTypeDefinition complexTypeDefinition, int minOccurs, int maxOccurs) {
		return refinedObjectClassDefinition.createContainerDefinition(name, complexTypeDefinition, minOccurs, maxOccurs);
	}

	public boolean isEmpty() {
		return refinedObjectClassDefinition.isEmpty();
	}

	public PrismObject<AccountShadowType> createBlankShadow() {
		return refinedObjectClassDefinition.createBlankShadow();
	}

	public ResourceShadowDiscriminator getResourceAccountType() {
		return refinedObjectClassDefinition.getResourceAccountType();
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

	public MappingType getActivationInbound() {
		return refinedObjectClassDefinition.getActivationInbound();
	}

	public MappingType getActivationOutbound() {
		return refinedObjectClassDefinition.getActivationOutbound();
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
        return "LRAccDef";
    }
	
}
