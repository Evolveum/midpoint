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
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AttributeFetchStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceActivationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceBidirectionalMappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 * @author mederly
 *
 * Work in-progress. TODO fix this mess
 *
 */
public class LayerRefinedObjectClassDefinition extends RefinedObjectClassDefinition {
	
	private RefinedObjectClassDefinition refinedObjectClassDefinition;
	private LayerType layer;
    /**
     * Keeps layer-specific information on resource object attributes.
     * This list is lazily evaluated.
     */
    private List<LayerRefinedAttributeDefinition> layerRefinedAttributeDefinitions;
	
	private LayerRefinedObjectClassDefinition(RefinedObjectClassDefinition refinedAccountDefinition, LayerType layer) {
		super(new QName("fake"), refinedAccountDefinition.getPrismContext());
		this.refinedObjectClassDefinition = refinedAccountDefinition;
		this.layer = layer;
	}
	
	static LayerRefinedObjectClassDefinition wrap(RefinedObjectClassDefinition rAccountDef, LayerType layer) {
		if (rAccountDef == null) {
			return null;
		}
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

    @Override
	public QName getTypeName() {
		return refinedObjectClassDefinition.getTypeName();
	}

    @Override
	public void setTypeName(QName typeName) {
		refinedObjectClassDefinition.setTypeName(typeName);
	}

    @Override
	public boolean isIgnored() {
		return refinedObjectClassDefinition.isIgnored();
	}

    @Override
	public void setIgnored(boolean ignored) {
		refinedObjectClassDefinition.setIgnored(ignored);
	}

    @Override
    public ResourceAttributeDefinition getDescriptionAttribute() {
        return substituteLayerRefinedAttributeDefinition(refinedObjectClassDefinition.getDescriptionAttribute());
    }

    @Override
    public void setDescriptionAttribute(ResourceAttributeDefinition descriptionAttribute) {
		refinedObjectClassDefinition.setDescriptionAttribute(descriptionAttribute);
	}

    private LayerRefinedAttributeDefinition substituteLayerRefinedAttributeDefinition(ResourceAttributeDefinition attributeDef) {
        LayerRefinedAttributeDefinition rAttrDef = findAttributeDefinition(attributeDef.getName());
        return rAttrDef;
    }

    private Collection<LayerRefinedAttributeDefinition> substituteLayerRefinedAttributeDefinitionCollection(Collection<? extends RefinedAttributeDefinition> attributes) {
        Collection<LayerRefinedAttributeDefinition> retval = new ArrayList<>();
        for (RefinedAttributeDefinition rad : attributes) {
            retval.add(substituteLayerRefinedAttributeDefinition(rad));
        }
        return retval;
    }

    @Override
    public LayerRefinedAttributeDefinition getNamingAttribute() {
        return substituteLayerRefinedAttributeDefinition(refinedObjectClassDefinition.getNamingAttribute());
	}

    @Override
	public String getNativeObjectClass() {
		return refinedObjectClassDefinition.getNativeObjectClass();
	}

    @Override
	public Integer getDisplayOrder() {
		return refinedObjectClassDefinition.getDisplayOrder();
	}

    @Override
	public boolean isDefaultInAKind() {
		return refinedObjectClassDefinition.isDefaultInAKind();
	}

    @Override
	public void setDefaultInAKind(boolean defaultAccountType) {
		refinedObjectClassDefinition.setDefaultInAKind(defaultAccountType);
	}

    @Override
	public ShadowKindType getKind() {
		return refinedObjectClassDefinition.getKind();
	}

    @Override
	public void setKind(ShadowKindType kind) {
		refinedObjectClassDefinition.setKind(kind);
	}

    @Override
	public AttributeFetchStrategyType getPasswordFetchStrategy() {
		return refinedObjectClassDefinition.getPasswordFetchStrategy();
	}

    @Override
	public String getIntent() {
		return refinedObjectClassDefinition.getIntent();
	}

    @Override
	public void setIntent(String accountTypeName) {
		refinedObjectClassDefinition.setIntent(accountTypeName);
	}

    @Override
	public void setDisplayOrder(Integer displayOrder) {
		refinedObjectClassDefinition.setDisplayOrder(displayOrder);
	}

    @Override
	public LayerRefinedAttributeDefinition getDisplayNameAttribute() {
        return substituteLayerRefinedAttributeDefinition(refinedObjectClassDefinition.getDisplayNameAttribute());
	}

    @Override
	public String getHelp() {
		return refinedObjectClassDefinition.getHelp();
	}

    @Override
	public void setDisplayNameAttribute(QName displayName) {
		refinedObjectClassDefinition.setDisplayNameAttribute(displayName);
	}

    @Override
	public Collection<? extends LayerRefinedAttributeDefinition> getIdentifiers() {
        return substituteLayerRefinedAttributeDefinitionCollection(refinedObjectClassDefinition.getIdentifiers());
	}

    @Override
    public <D extends ItemDefinition> D findItemDefinition(QName name, Class<D> clazz) {
		D findItemDefinition = refinedObjectClassDefinition.findItemDefinition(name, clazz);
		return (D) LayerRefinedAttributeDefinition.wrap((RefinedAttributeDefinition) findItemDefinition, layer);
	}

    @Override
	public void setHelp(String help) {
		refinedObjectClassDefinition.setHelp(help);
	}

    @Override
	public Collection<? extends LayerRefinedAttributeDefinition> getSecondaryIdentifiers() {
		return LayerRefinedAttributeDefinition.wrapCollection(refinedObjectClassDefinition.getSecondaryIdentifiers(), layer);
	}

    @Override
	public Class getTypeClass() {
		return refinedObjectClassDefinition.getTypeClass();
	}

    @Override
	public Collection<ResourceObjectPattern> getProtectedObjectPatterns() {
		return refinedObjectClassDefinition.getProtectedObjectPatterns();
	}

    @Override
	public PrismContext getPrismContext() {
		return refinedObjectClassDefinition.getPrismContext();
	}

    @Override
	public void setNamingAttribute(ResourceAttributeDefinition namingAttribute) {
		refinedObjectClassDefinition.setNamingAttribute(namingAttribute);
	}

    @Override
	public ResourceAttributeContainer instantiate(QName name) {
		return refinedObjectClassDefinition.instantiate(name);
	}

    @Override
    public void setNamingAttribute(QName namingAttribute) {
		refinedObjectClassDefinition.setNamingAttribute(namingAttribute);
	}

    @Override
	public PrismPropertyDefinition findPropertyDefinition(QName name) {
        LayerRefinedAttributeDefinition def = findAttributeDefinition(name);
        if (def != null) {
            return def;
        } else {
            // actually, can there be properties other than attributes? [mederly]
		    return LayerRefinedAttributeDefinition.wrap((RefinedAttributeDefinition) refinedObjectClassDefinition.findPropertyDefinition(name), layer);
        }
	}

    @Override
	public LayerRefinedAttributeDefinition findAttributeDefinition(QName elementQName) {
        for (LayerRefinedAttributeDefinition definition : getAttributeDefinitions()) {
            if (QNameUtil.match(definition.getName(), elementQName)) {
                return definition;
            }
        }
        return null;
	}

    @Override
	public void setNativeObjectClass(String nativeObjectClass) {
		refinedObjectClassDefinition.setNativeObjectClass(nativeObjectClass);
	}

    @Override
	public LayerRefinedAttributeDefinition findAttributeDefinition(String elementLocalname) {
		return findAttributeDefinition(new QName(getResourceNamespace(), elementLocalname));        // todo or should we use ns-less matching?
	}

    @Override
	public String getDisplayName() {
		return refinedObjectClassDefinition.getDisplayName();
	}

    @Override
	public void setDisplayName(String displayName) {
		refinedObjectClassDefinition.setDisplayName(displayName);
	}

    @Override
	public List<? extends ItemDefinition> getDefinitions() {
		return getAttributeDefinitions();
	}

    @Override
	public String getDescription() {
		return refinedObjectClassDefinition.getDescription();
	}

    @Override
	public void setDescription(String description) {
		refinedObjectClassDefinition.setDescription(description);
	}

    @Override
	public boolean isDefault() {
		return refinedObjectClassDefinition.isDefault();
	}

    @Override
	public void setDefault(boolean isDefault) {
		refinedObjectClassDefinition.setDefault(isDefault);
	}

    @Override
	public ObjectClassComplexTypeDefinition getObjectClassDefinition() {
		return refinedObjectClassDefinition.getObjectClassDefinition();
	}

    @Override
	public void setObjectClassDefinition(ObjectClassComplexTypeDefinition objectClassDefinition) {
		refinedObjectClassDefinition.setObjectClassDefinition(objectClassDefinition);
	}

    @Override
	public List<? extends LayerRefinedAttributeDefinition> getAttributeDefinitions() {
        if (layerRefinedAttributeDefinitions == null) {
            layerRefinedAttributeDefinitions = LayerRefinedAttributeDefinition.wrapCollection(refinedObjectClassDefinition.getAttributeDefinitions(), layer);
        }
		return layerRefinedAttributeDefinitions;
	}

    @Override
    public ResourceType getResourceType() {
		return refinedObjectClassDefinition.getResourceType();
	}

    @Override
	public PrismObjectDefinition<ShadowType> getObjectDefinition() {
		return refinedObjectClassDefinition.getObjectDefinition();
	}

    @Override
	public void setDisplayNameAttribute(ResourceAttributeDefinition displayName) {
		refinedObjectClassDefinition.setDisplayNameAttribute(displayName);
	}

    @Override
	public LayerRefinedAttributeDefinition getAttributeDefinition(QName attributeName) {
        // todo should there be any difference between findAttributeDefinition and getAttributeDefinition? [mederly]
		return findAttributeDefinition(attributeName);
	}

    @Override
	public boolean containsAttributeDefinition(QName attributeName) {
		return refinedObjectClassDefinition.containsAttributeDefinition(attributeName);
	}

    @Override
	public PrismPropertyDefinition createPropertyDefinition(String localName, QName typeName) {
        throw new UnsupportedOperationException("property definition cannot be created in LayerRefinedObjectClassDefinition");
		// return refinedObjectClassDefinition.createPropertyDefinition(localName, typeName);
	}

    @Override
	public boolean isEmpty() {
		return refinedObjectClassDefinition.isEmpty();
	}

    @Override
	public PrismObject<ShadowType> createBlankShadow() {
		return refinedObjectClassDefinition.createBlankShadow();
	}

    @Override
	public ResourceShadowDiscriminator getShadowDiscriminator() {
		return refinedObjectClassDefinition.getShadowDiscriminator();
	}

    @Override
	public Collection<? extends QName> getNamesOfAttributesWithOutboundExpressions() {
		return refinedObjectClassDefinition.getNamesOfAttributesWithOutboundExpressions();
	}

    @Override
    public Collection<? extends QName> getNamesOfAttributesWithInboundExpressions() {
		return refinedObjectClassDefinition.getNamesOfAttributesWithInboundExpressions();
	}

    @Override
	public List<MappingType> getCredentialsInbound() {
		return refinedObjectClassDefinition.getCredentialsInbound();
	}

    @Override
	public MappingType getCredentialsOutbound() {
		return refinedObjectClassDefinition.getCredentialsOutbound();
	}

    @Override
	public ObjectReferenceType getPasswordPolicy() {
		return refinedObjectClassDefinition.getPasswordPolicy();
	}

    @Override
	public Class<?> getCompileTimeClass() {
		return refinedObjectClassDefinition.getCompileTimeClass();
	}

    @Override
	public QName getExtensionForType() {
		return refinedObjectClassDefinition.getExtensionForType();
	}

    @Override
    public boolean isContainerMarker() {
		return refinedObjectClassDefinition.isContainerMarker();
	}

    @Override
	public boolean isIdentifier(QName attrName) {
		return refinedObjectClassDefinition.isIdentifier(attrName);
	}

    @Override
	public boolean isObjectMarker() {
		return refinedObjectClassDefinition.isObjectMarker();
	}

    @Override
	public boolean isXsdAnyMarker() {
		return refinedObjectClassDefinition.isXsdAnyMarker();
	}

    @Override
	public QName getSuperType() {
		return refinedObjectClassDefinition.getSuperType();
	}

    @Override
	public boolean isSecondaryIdentifier(QName attrName) {
		return refinedObjectClassDefinition.isSecondaryIdentifier(attrName);
	}

    @Override
	public boolean isRuntimeSchema() {
		return refinedObjectClassDefinition.isRuntimeSchema();
	}

    @Override
	public Collection<RefinedAssociationDefinition> getAssociations() {
		return refinedObjectClassDefinition.getAssociations();
	}

    @Override
	public Collection<RefinedAssociationDefinition> getAssociations(ShadowKindType kind) {
		return refinedObjectClassDefinition.getAssociations(kind);
	}

    @Override
    public ResourceAttributeContainerDefinition toResourceAttributeContainerDefinition() {
		ResourceAttributeContainerDefinition resourceAttributeContainerDefinition = refinedObjectClassDefinition.toResourceAttributeContainerDefinition();
		resourceAttributeContainerDefinition.setComplexTypeDefinition(this);
		return resourceAttributeContainerDefinition;
	}

    @Override
    public ResourceAttributeContainerDefinition toResourceAttributeContainerDefinition(QName elementName) {
		ResourceAttributeContainerDefinition resourceAttributeContainerDefinition = refinedObjectClassDefinition.toResourceAttributeContainerDefinition(elementName);
		resourceAttributeContainerDefinition.setComplexTypeDefinition(this);
		return resourceAttributeContainerDefinition;
	}

    @Override
    public ResourceActivationDefinitionType getActivationSchemaHandling() {
		return refinedObjectClassDefinition.getActivationSchemaHandling();
	}

    @Override
	public ResourceBidirectionalMappingType getActivationBidirectionalMappingType(QName propertyName) {
		return refinedObjectClassDefinition.getActivationBidirectionalMappingType(propertyName);
	}

    @Override
	public AttributeFetchStrategyType getActivationFetchStrategy(QName propertyName) {
		return refinedObjectClassDefinition.getActivationFetchStrategy(propertyName);
	}

    @Override
	public Collection<RefinedAssociationDefinition> getEntitlementAssociations() {
		return refinedObjectClassDefinition.getEntitlementAssociations();
	}

    @Override
	public boolean isAbstract() {
		return refinedObjectClassDefinition.isAbstract();
	}

    @Override
	public boolean isDeprecated() {
		return refinedObjectClassDefinition.isDeprecated();
	}

    @Override
	public String getDocumentation() {
		return refinedObjectClassDefinition.getDocumentation();
	}

    @Override
	public String getDocumentationPreview() {
		return refinedObjectClassDefinition.getDocumentationPreview();
	}

    @Override
	public RefinedAssociationDefinition findAssociation(QName name) {
		return refinedObjectClassDefinition.findAssociation(name);
	}

    @Override
	public RefinedAssociationDefinition findEntitlementAssociation(QName name) {
		return refinedObjectClassDefinition.findEntitlementAssociation(name);
	}

    @Override
	public Collection<? extends QName> getNamesOfAssociationsWithOutboundExpressions() {
		return refinedObjectClassDefinition.getNamesOfAssociationsWithOutboundExpressions();
	}

    @Override
	public String getDocClassName() {
		return refinedObjectClassDefinition.getDocClassName();
	}

    @Override
	public boolean matches(ShadowType shadowType) {
		return refinedObjectClassDefinition.matches(shadowType);
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
		return debugDump(indent, layer);
	}

	/**
     * Return a human readable name of this class suitable for logs.
     */
    @Override
    protected String getDebugDumpClassName() {
        return "LRObjectClassDef";
    }

    @Override
	public String getHumanReadableName() {
		return refinedObjectClassDefinition.getHumanReadableName();
	}

    @Override
    public LayerRefinedObjectClassDefinition clone() {
        return wrap(refinedObjectClassDefinition.clone(), this.layer);
    }

    @Override
    public void setAssociations(Collection<RefinedAssociationDefinition> associations) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected String getResourceNamespace() {
        return refinedObjectClassDefinition.getResourceNamespace();
    }

    @Override
    public void add(RefinedAttributeDefinition refinedAttributeDefinition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void parseAssociations(RefinedResourceSchema rSchema) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected String debugDump(int indent, LayerType layer) {
        return refinedObjectClassDefinition.debugDump(indent, layer);
    }
}
