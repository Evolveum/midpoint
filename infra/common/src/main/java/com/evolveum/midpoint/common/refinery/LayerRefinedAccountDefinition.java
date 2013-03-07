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
import com.evolveum.midpoint.xml.ns._public.common.common_2a.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;

/**
 * @author semancik
 *
 */
public class LayerRefinedAccountDefinition extends RefinedAccountDefinition {
	
	private RefinedAccountDefinition refinedAccountDefinition;
	private LayerType layer;
	
	private LayerRefinedAccountDefinition(RefinedAccountDefinition refinedAccountDefinition, LayerType layer) {
		super(refinedAccountDefinition.getPrismContext());
		this.refinedAccountDefinition = refinedAccountDefinition;
		this.layer = layer;
	}
	
	static LayerRefinedAccountDefinition wrap(RefinedAccountDefinition rAccountDef, LayerType layer) {
		return new LayerRefinedAccountDefinition(rAccountDef, layer);
	}
	
	static Collection<? extends LayerRefinedAccountDefinition> wrapCollection(Collection<? extends RefinedAccountDefinition> rAccountDefs, LayerType layer) {
		Collection<LayerRefinedAccountDefinition> outs = new ArrayList<LayerRefinedAccountDefinition>(rAccountDefs.size());
		for (RefinedAccountDefinition rAccountDef: rAccountDefs) {
			outs.add(wrap(rAccountDef, layer));
		}
		return outs;
	}

	public QName getName() {
		return refinedAccountDefinition.getName();
	}

	public void setName(QName name) {
		refinedAccountDefinition.setName(name);
	}

	public QName getTypeName() {
		return refinedAccountDefinition.getTypeName();
	}

	public QName getDefaultName() {
		return refinedAccountDefinition.getDefaultName();
	}

	public QName getNameOrDefaultName() {
		return refinedAccountDefinition.getNameOrDefaultName();
	}

	public void setTypeName(QName typeName) {
		refinedAccountDefinition.setTypeName(typeName);
	}

	public ResourceAttributeDefinition getDescriptionAttribute() {
		return refinedAccountDefinition.getDescriptionAttribute();
	}

	public boolean isIgnored() {
		return refinedAccountDefinition.isIgnored();
	}

	public void setIgnored(boolean ignored) {
		refinedAccountDefinition.setIgnored(ignored);
	}

	public void setDescriptionAttribute(ResourceAttributeDefinition descriptionAttribute) {
		refinedAccountDefinition.setDescriptionAttribute(descriptionAttribute);
	}

	public int getMinOccurs() {
		return refinedAccountDefinition.getMinOccurs();
	}

	public void setMinOccurs(int minOccurs) {
		refinedAccountDefinition.setMinOccurs(minOccurs);
	}

	public LayerRefinedAttributeDefinition getNamingAttribute() {
		return LayerRefinedAttributeDefinition.wrap(refinedAccountDefinition.getNamingAttribute(), layer);
	}

	public int getMaxOccurs() {
		return refinedAccountDefinition.getMaxOccurs();
	}

	public String getNativeObjectClass() {
		return refinedAccountDefinition.getNativeObjectClass();
	}

	public void setAccountType(boolean accountType) {
		refinedAccountDefinition.setAccountType(accountType);
	}

	public void setMaxOccurs(int maxOccurs) {
		refinedAccountDefinition.setMaxOccurs(maxOccurs);
	}

	public Integer getDisplayOrder() {
		return refinedAccountDefinition.getDisplayOrder();
	}

	public Class<ResourceObjectShadowAttributesType> getCompileTimeClass() {
		return refinedAccountDefinition.getCompileTimeClass();
	}

	public boolean isSingleValue() {
		return refinedAccountDefinition.isSingleValue();
	}

	public boolean isDefaultAccountType() {
		return refinedAccountDefinition.isDefaultAccountType();
	}

	public void setDefaultAccountType(boolean defaultAccountType) {
		refinedAccountDefinition.setDefaultAccountType(defaultAccountType);
	}

	public void setCompileTimeClass(Class<ResourceObjectShadowAttributesType> compileTimeClass) {
		refinedAccountDefinition.setCompileTimeClass(compileTimeClass);
	}

	public boolean isMultiValue() {
		return refinedAccountDefinition.isMultiValue();
	}

	public String getAccountTypeName() {
		return refinedAccountDefinition.getAccountTypeName();
	}

	public void setAccountTypeName(String accountTypeName) {
		refinedAccountDefinition.setAccountTypeName(accountTypeName);
	}

	public void setDisplayOrder(Integer displayOrder) {
		refinedAccountDefinition.setDisplayOrder(displayOrder);
	}

	public boolean isMandatory() {
		return refinedAccountDefinition.isMandatory();
	}

	public LayerRefinedAttributeDefinition getDisplayNameAttribute() {
		return LayerRefinedAttributeDefinition.wrap(refinedAccountDefinition.getDisplayNameAttribute(), layer);
	}

	public String getHelp() {
		return refinedAccountDefinition.getHelp();
	}

	public void setComplexTypeDefinition(ComplexTypeDefinition complexTypeDefinition) {
		refinedAccountDefinition.setComplexTypeDefinition(complexTypeDefinition);
	}

	public boolean isOptional() {
		return refinedAccountDefinition.isOptional();
	}

	public void setDisplayNameAttribute(QName displayName) {
		refinedAccountDefinition.setDisplayNameAttribute(displayName);
	}

	public Collection<? extends LayerRefinedAttributeDefinition> getIdentifiers() {
		return LayerRefinedAttributeDefinition.wrapCollection(refinedAccountDefinition.getIdentifiers(), layer);
	}

	public boolean isDynamic() {
		return refinedAccountDefinition.isDynamic();
	}

	public <D extends ItemDefinition> D findItemDefinition(QName name, Class<D> clazz) {
		return refinedAccountDefinition.findItemDefinition(name, clazz);
	}

	public void setHelp(String help) {
		refinedAccountDefinition.setHelp(help);
	}

	public Collection<? extends LayerRefinedAttributeDefinition> getSecondaryIdentifiers() {
		return LayerRefinedAttributeDefinition.wrapCollection(refinedAccountDefinition.getSecondaryIdentifiers(), layer);
	}

	public Class getTypeClass() {
		return refinedAccountDefinition.getTypeClass();
	}

	public void setDynamic(boolean dynamic) {
		refinedAccountDefinition.setDynamic(dynamic);
	}

	public Collection<ResourceObjectPattern> getProtectedAccounts() {
		return refinedAccountDefinition.getProtectedAccounts();
	}

	public boolean isValidFor(QName elementQName, Class<? extends ItemDefinition> clazz) {
		return refinedAccountDefinition.isValidFor(elementQName, clazz);
	}

	public PrismContext getPrismContext() {
		return refinedAccountDefinition.getPrismContext();
	}

	public ObjectClassComplexTypeDefinition getComplexTypeDefinition() {
		return refinedAccountDefinition.getComplexTypeDefinition();
	}

	public <T extends ItemDefinition> T findItemDefinition(ItemPath path, Class<T> clazz) {
		return refinedAccountDefinition.findItemDefinition(path, clazz);
	}

	public ResourceAttributeContainer instantiate() {
		return refinedAccountDefinition.instantiate();
	}

	public void setNamingAttribute(ResourceAttributeDefinition namingAttribute) {
		refinedAccountDefinition.setNamingAttribute(namingAttribute);
	}

	public ResourceAttributeContainer instantiate(QName name) {
		return refinedAccountDefinition.instantiate(name);
	}

	public void setNamingAttribute(QName namingAttribute) {
		refinedAccountDefinition.setNamingAttribute(namingAttribute);
	}

	public ItemDefinition findItemDefinition(QName name) {
		return refinedAccountDefinition.findItemDefinition(name);
	}

	public ItemDefinition findItemDefinition(ItemPath path) {
		return refinedAccountDefinition.findItemDefinition(path);
	}

	public PrismPropertyDefinition findPropertyDefinition(QName name) {
		return refinedAccountDefinition.findPropertyDefinition(name);
	}

	public RefinedAttributeDefinition findAttributeDefinition(QName elementQName) {
		return refinedAccountDefinition.findAttributeDefinition(elementQName);
	}

	public PrismPropertyDefinition findPropertyDefinition(ItemPath path) {
		return refinedAccountDefinition.findPropertyDefinition(path);
	}

	public void setNativeObjectClass(String nativeObjectClass) {
		refinedAccountDefinition.setNativeObjectClass(nativeObjectClass);
	}

	public LayerRefinedAttributeDefinition findAttributeDefinition(String elementLocalname) {
		return LayerRefinedAttributeDefinition.wrap(refinedAccountDefinition.findAttributeDefinition(elementLocalname), layer);
	}

	public PrismReferenceDefinition findReferenceDefinition(QName name) {
		return refinedAccountDefinition.findReferenceDefinition(name);
	}

	public String getDisplayName() {
		return refinedAccountDefinition.getDisplayName();
	}

	public <X extends Containerable> PrismContainerDefinition<X> findContainerDefinition(QName name) {
		return refinedAccountDefinition.findContainerDefinition(name);
	}

	public <X extends Containerable> PrismContainerDefinition<X> findContainerDefinition(String name) {
		return refinedAccountDefinition.findContainerDefinition(name);
	}

	public PrismContainerDefinition findContainerDefinition(ItemPath path) {
		return refinedAccountDefinition.findContainerDefinition(path);
	}

	public void setDisplayName(String displayName) {
		refinedAccountDefinition.setDisplayName(displayName);
	}

	public boolean isRuntimeSchema() {
		return refinedAccountDefinition.isRuntimeSchema();
	}

	public String getDescription() {
		return refinedAccountDefinition.getDescription();
	}

	public void setDescription(String description) {
		refinedAccountDefinition.setDescription(description);
	}

	public String getNamespace() {
		return refinedAccountDefinition.getNamespace();
	}

	public boolean isDefault() {
		return refinedAccountDefinition.isDefault();
	}

	public void setDefault(boolean isDefault) {
		refinedAccountDefinition.setDefault(isDefault);
	}

	public ObjectClassComplexTypeDefinition getObjectClassDefinition() {
		return refinedAccountDefinition.getObjectClassDefinition();
	}

	public void setObjectClassDefinition(ObjectClassComplexTypeDefinition objectClassDefinition) {
		refinedAccountDefinition.setObjectClassDefinition(objectClassDefinition);
	}

	public boolean isAccountType() {
		return refinedAccountDefinition.isAccountType();
	}

	public Collection<? extends LayerRefinedAttributeDefinition> getAttributeDefinitions() {
		return LayerRefinedAttributeDefinition.wrapCollection(refinedAccountDefinition.getAttributeDefinitions(), layer);
	}

	public List<PrismPropertyDefinition> getPropertyDefinitions() {
		return refinedAccountDefinition.getPropertyDefinitions();
	}

	public List<ItemDefinition> getDefinitions() {
		return refinedAccountDefinition.getDefinitions();
	}

	public ResourceType getResourceType() {
		return refinedAccountDefinition.getResourceType();
	}

	public PrismObjectDefinition<AccountShadowType> getObjectDefinition() {
		return refinedAccountDefinition.getObjectDefinition();
	}

	public void setRuntimeSchema(boolean isRuntimeSchema) {
		refinedAccountDefinition.setRuntimeSchema(isRuntimeSchema);
	}

	public void setDisplayNameAttribute(ResourceAttributeDefinition displayName) {
		refinedAccountDefinition.setDisplayNameAttribute(displayName);
	}

	public RefinedAttributeDefinition getAttributeDefinition(QName attributeName) {
		return refinedAccountDefinition.getAttributeDefinition(attributeName);
	}

	public ItemDelta createEmptyDelta(ItemPath path) {
		return refinedAccountDefinition.createEmptyDelta(path);
	}

	public boolean containsAttributeDefinition(QName attributeName) {
		return refinedAccountDefinition.containsAttributeDefinition(attributeName);
	}

	public PrismPropertyDefinition createPropertyDefinition(QName name, QName typeName) {
		return refinedAccountDefinition.createPropertyDefinition(name, typeName);
	}

	public ResourceAttributeDefinition findAttributeDefinition(ItemPath elementPath) {
		return refinedAccountDefinition.findAttributeDefinition(elementPath);
	}

	public PrismPropertyDefinition createPropertyDefinition(QName name, QName typeName, int minOccurs, int maxOccurs) {
		return refinedAccountDefinition.createPropertyDefinition(name, typeName, minOccurs, maxOccurs);
	}

	public <T extends ResourceObjectShadowType> PrismObjectDefinition<T> toShadowDefinition() {
		return refinedAccountDefinition.toShadowDefinition();
	}

	public PrismPropertyDefinition createPropertyDefinition(QName name) {
		return refinedAccountDefinition.createPropertyDefinition(name);
	}

	public PrismPropertyDefinition createPropertyDefinition(String localName, QName typeName) {
		return refinedAccountDefinition.createPropertyDefinition(localName, typeName);
	}

	public PrismPropertyDefinition createPropertyDefinition(String localName, String localTypeName) {
		return refinedAccountDefinition.createPropertyDefinition(localName, localTypeName);
	}

	public PrismPropertyDefinition createPropertyDefinition(String localName, String localTypeName, int minOccurs,
			int maxOccurs) {
		return refinedAccountDefinition.createPropertyDefinition(localName, localTypeName, minOccurs, maxOccurs);
	}

	public PrismContainerDefinition createContainerDefinition(QName name, QName typeName) {
		return refinedAccountDefinition.createContainerDefinition(name, typeName);
	}

	public PrismContainerDefinition createContainerDefinition(QName name, QName typeName, int minOccurs, int maxOccurs) {
		return refinedAccountDefinition.createContainerDefinition(name, typeName, minOccurs, maxOccurs);
	}

	public PrismContainerDefinition<ResourceObjectShadowAttributesType> createContainerDefinition(QName name,
			ComplexTypeDefinition complexTypeDefinition, int minOccurs, int maxOccurs) {
		return refinedAccountDefinition.createContainerDefinition(name, complexTypeDefinition, minOccurs, maxOccurs);
	}

	public boolean isEmpty() {
		return refinedAccountDefinition.isEmpty();
	}

	public PrismObject<AccountShadowType> createBlankShadow() {
		return refinedAccountDefinition.createBlankShadow();
	}

	public ResourceShadowDiscriminator getResourceAccountType() {
		return refinedAccountDefinition.getResourceAccountType();
	}

	public Collection<? extends QName> getNamesOfAttributesWithOutboundExpressions() {
		return refinedAccountDefinition.getNamesOfAttributesWithOutboundExpressions();
	}

	public Collection<? extends QName> getNamesOfAttributesWithInboundExpressions() {
		return refinedAccountDefinition.getNamesOfAttributesWithInboundExpressions();
	}

	public MappingType getCredentialsInbound() {
		return refinedAccountDefinition.getCredentialsInbound();
	}

	public MappingType getCredentialsOutbound() {
		return refinedAccountDefinition.getCredentialsOutbound();
	}

	public ObjectReferenceType getPasswordPolicy() {
		return refinedAccountDefinition.getPasswordPolicy();
	}

	public MappingType getActivationInbound() {
		return refinedAccountDefinition.getActivationInbound();
	}

	public MappingType getActivationOutbound() {
		return refinedAccountDefinition.getActivationOutbound();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((layer == null) ? 0 : layer.hashCode());
		result = prime * result + ((refinedAccountDefinition == null) ? 0 : refinedAccountDefinition.hashCode());
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
		LayerRefinedAccountDefinition other = (LayerRefinedAccountDefinition) obj;
		if (layer != other.layer)
			return false;
		if (refinedAccountDefinition == null) {
			if (other.refinedAccountDefinition != null)
				return false;
		} else if (!refinedAccountDefinition.equals(other.refinedAccountDefinition))
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
		sb.append(refinedAccountDefinition.debugDump(indent+1));
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
