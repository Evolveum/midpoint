/**
 * Copyright (c) 2015-2016 Evolveum
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
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AttributeFetchStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceActivationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceBidirectionalMappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PagedSearchCapabilityType;

/**
 * Used to represent combined definition of structural and auxiliary object classes.
 * 
 * @author semancik
 *
 */
public class CompositeRefinedObjectClassDefinition extends RefinedObjectClassDefinition {

	private RefinedObjectClassDefinition structuralObjectClassDefinition;
	private Collection<RefinedObjectClassDefinition> auxiliaryObjectClassDefinitions;
	
	public CompositeRefinedObjectClassDefinition(RefinedObjectClassDefinition structuralObjectClassDefinition, Collection<RefinedObjectClassDefinition> auxiliaryObjectClassDefinitions) {
		super(structuralObjectClassDefinition.getTypeName(), structuralObjectClassDefinition.getPrismContext());
		this.structuralObjectClassDefinition = structuralObjectClassDefinition;
		this.auxiliaryObjectClassDefinitions = auxiliaryObjectClassDefinitions;
	}

	public RefinedObjectClassDefinition getStructuralObjectClassDefinition() {
		return structuralObjectClassDefinition;
	}
		
	public Collection<RefinedObjectClassDefinition> getAuxiliaryObjectClassDefinitions() {
		return auxiliaryObjectClassDefinitions;
	}
	
	public Class<?> getCompileTimeClass() {
		return structuralObjectClassDefinition.getCompileTimeClass();
	}

	public boolean isContainerMarker() {
		return structuralObjectClassDefinition.isContainerMarker();
	}

	public boolean isIdentifier(QName attrName) {
		return structuralObjectClassDefinition.isIdentifier(attrName);
	}

	public boolean isObjectMarker() {
		return structuralObjectClassDefinition.isObjectMarker();
	}

	public boolean isIgnored() {
		return structuralObjectClassDefinition.isIgnored();
	}

	public boolean isAbstract() {
		return structuralObjectClassDefinition.isAbstract();
	}

	public QName getSuperType() {
		return structuralObjectClassDefinition.getSuperType();
	}

	public boolean isSecondaryIdentifier(QName attrName) {
		return structuralObjectClassDefinition.isSecondaryIdentifier(attrName);
	}

	public boolean isDeprecated() {
		return structuralObjectClassDefinition.isDeprecated();
	}

	public boolean isInherited() {
		return structuralObjectClassDefinition.isInherited();
	}

	public Integer getDisplayOrder() {
		return structuralObjectClassDefinition.getDisplayOrder();
	}

	public ResourceAttributeDefinition<?> getDescriptionAttribute() {
		return structuralObjectClassDefinition.getDescriptionAttribute();
	}

	public String getHelp() {
		return structuralObjectClassDefinition.getHelp();
	}

	public RefinedAttributeDefinition<?> getNamingAttribute() {
		return structuralObjectClassDefinition.getNamingAttribute();
	}

	public QName getTypeName() {
		return structuralObjectClassDefinition.getTypeName();
	}

	public String getNativeObjectClass() {
		return structuralObjectClassDefinition.getNativeObjectClass();
	}

	public String getDocumentation() {
		return structuralObjectClassDefinition.getDocumentation();
	}

	public boolean isDefaultInAKind() {
		return structuralObjectClassDefinition.isDefaultInAKind();
	}

	public String getDocumentationPreview() {
		return structuralObjectClassDefinition.getDocumentationPreview();
	}

	public String getIntent() {
		return structuralObjectClassDefinition.getIntent();
	}

	public ShadowKindType getKind() {
		return structuralObjectClassDefinition.getKind();
	}

	public boolean isRuntimeSchema() {
		return structuralObjectClassDefinition.isRuntimeSchema();
	}

	public RefinedAttributeDefinition<?> getDisplayNameAttribute() {
		return structuralObjectClassDefinition.getDisplayNameAttribute();
	}

	@Override
	public Collection<? extends RefinedAttributeDefinition<?>> getIdentifiers() {
		return structuralObjectClassDefinition.getIdentifiers();
	}

	@Override
	public Collection<? extends RefinedAttributeDefinition<?>> getSecondaryIdentifiers() {
		return structuralObjectClassDefinition.getSecondaryIdentifiers();
	}
	
	@Override
	public Collection<? extends RefinedAttributeDefinition<?>> getAllIdentifiers() {
		return structuralObjectClassDefinition.getAllIdentifiers();
	}

	public boolean isAuxiliary() {
		return structuralObjectClassDefinition.isAuxiliary();
	}

	public Collection<RefinedAssociationDefinition> getAssociations() {
		return structuralObjectClassDefinition.getAssociations();
	}

	public Collection<RefinedAssociationDefinition> getAssociations(ShadowKindType kind) {
		return structuralObjectClassDefinition.getAssociations(kind);
	}

	public Collection<RefinedAssociationDefinition> getEntitlementAssociations() {
		return structuralObjectClassDefinition.getEntitlementAssociations();
	}

	public Collection<QName> getNamesOfAssociations() {
		return structuralObjectClassDefinition.getNamesOfAssociations();
	}

	public boolean isEmpty() {
		return structuralObjectClassDefinition.isEmpty();
	}

	public Collection<ResourceObjectPattern> getProtectedObjectPatterns() {
		return structuralObjectClassDefinition.getProtectedObjectPatterns();
	}

	public String getDocClassName() {
		return structuralObjectClassDefinition.getDocClassName();
	}

	public String getDisplayName() {
		return structuralObjectClassDefinition.getDisplayName();
	}

	public String getDescription() {
		return structuralObjectClassDefinition.getDescription();
	}

	public boolean isDefault() {
		return structuralObjectClassDefinition.isDefault();
	}

	public ResourceType getResourceType() {
		return structuralObjectClassDefinition.getResourceType();
	}

	public PrismObjectDefinition<ShadowType> getObjectDefinition() {
		return structuralObjectClassDefinition.getObjectDefinition();
	}

	public ObjectClassComplexTypeDefinition getObjectClassDefinition() {
		return structuralObjectClassDefinition.getObjectClassDefinition();
	}

	public ResourceObjectReferenceType getBaseContext() {
		return structuralObjectClassDefinition.getBaseContext();
	}

	public List<MappingType> getPasswordInbound() {
		return structuralObjectClassDefinition.getPasswordInbound();
	}

	public MappingType getPasswordOutbound() {
		return structuralObjectClassDefinition.getPasswordOutbound();
	}

	public AttributeFetchStrategyType getPasswordFetchStrategy() {
		return structuralObjectClassDefinition.getPasswordFetchStrategy();
	}

	public ObjectReferenceType getPasswordPolicy() {
		return structuralObjectClassDefinition.getPasswordPolicy();
	}

	public ResourceActivationDefinitionType getActivationSchemaHandling() {
		return structuralObjectClassDefinition.getActivationSchemaHandling();
	}

	public ResourceBidirectionalMappingType getActivationBidirectionalMappingType(QName propertyName) {
		return structuralObjectClassDefinition.getActivationBidirectionalMappingType(propertyName);
	}

	public AttributeFetchStrategyType getActivationFetchStrategy(QName propertyName) {
		return structuralObjectClassDefinition.getActivationFetchStrategy(propertyName);
	}

	public boolean matches(ShadowType shadowType) {
		return structuralObjectClassDefinition.matches(shadowType);
	}

	public <T extends CapabilityType> T getEffectiveCapability(Class<T> capabilityClass) {
		return structuralObjectClassDefinition.getEffectiveCapability(capabilityClass);
	}

	public PagedSearchCapabilityType getPagedSearches() {
		return structuralObjectClassDefinition.getPagedSearches();
	}

	public boolean isPagedSearchEnabled() {
		return structuralObjectClassDefinition.isPagedSearchEnabled();
	}

	public boolean isObjectCountingEnabled() {
		return structuralObjectClassDefinition.isObjectCountingEnabled();
	}

	@Override
	public <T extends ItemDefinition> T findItemDefinition(QName name, Class<T> clazz, boolean caseInsensitive) {
		T itemDef = structuralObjectClassDefinition.findItemDefinition(name, clazz, caseInsensitive);
		if (itemDef == null && auxiliaryObjectClassDefinitions != null) {
			for(RefinedObjectClassDefinition auxiliaryObjectClassDefinition: auxiliaryObjectClassDefinitions) {
				itemDef = auxiliaryObjectClassDefinition.findItemDefinition(name, clazz, caseInsensitive);
				if (itemDef != null) {
					break;
				}
			}
		}
		return itemDef;
	}
	
	
	@Override
	public Collection<? extends RefinedAttributeDefinition<?>> getAttributeDefinitions() {
		if (auxiliaryObjectClassDefinitions == null || auxiliaryObjectClassDefinitions.isEmpty()) {
			return structuralObjectClassDefinition.getAttributeDefinitions();
		}
		Collection<? extends RefinedAttributeDefinition<?>> defs = new ArrayList<>();
		defs.addAll((Collection)structuralObjectClassDefinition.getAttributeDefinitions());
		for(RefinedObjectClassDefinition auxiliaryObjectClassDefinition: auxiliaryObjectClassDefinitions) {
			for (RefinedAttributeDefinition auxRAttrDef: auxiliaryObjectClassDefinition.getAttributeDefinitions()) {
				boolean add = true;
				for (RefinedAttributeDefinition def: defs) {
					if (def.getName().equals(auxRAttrDef.getName())) {
						add = false;
						break;
					}
				}
				if (add) {
					((Collection)defs).add(auxRAttrDef);
				}
			}
		}
		return defs;
	}
	
	@Override
	public PrismContext getPrismContext() {
		return structuralObjectClassDefinition.getPrismContext();
	}

	@Override
	public CompositeRefinedObjectClassDefinition clone() {
		RefinedObjectClassDefinition structuralObjectClassDefinitionClone = structuralObjectClassDefinition.clone();
		Collection<RefinedObjectClassDefinition> auxiliaryObjectClassDefinitionsClone = null;
		if (this.auxiliaryObjectClassDefinitions != null) {
			auxiliaryObjectClassDefinitionsClone = new ArrayList<>(this.auxiliaryObjectClassDefinitions.size());
			for(RefinedObjectClassDefinition auxiliaryObjectClassDefinition: this.auxiliaryObjectClassDefinitions) {
				auxiliaryObjectClassDefinitionsClone.add(auxiliaryObjectClassDefinition.clone());
			}
		}
		return new CompositeRefinedObjectClassDefinition(structuralObjectClassDefinitionClone, auxiliaryObjectClassDefinitionsClone);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((auxiliaryObjectClassDefinitions == null) ? 0 : auxiliaryObjectClassDefinitions.hashCode());
		result = prime * result
				+ ((structuralObjectClassDefinition == null) ? 0 : structuralObjectClassDefinition.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		CompositeRefinedObjectClassDefinition other = (CompositeRefinedObjectClassDefinition) obj;
		if (auxiliaryObjectClassDefinitions == null) {
			if (other.auxiliaryObjectClassDefinitions != null) {
				return false;
			}
		} else if (!auxiliaryObjectClassDefinitions.equals(other.auxiliaryObjectClassDefinitions)) {
			return false;
		}
		if (structuralObjectClassDefinition == null) {
			if (other.structuralObjectClassDefinition != null) {
				return false;
			}
		} else if (!structuralObjectClassDefinition.equals(other.structuralObjectClassDefinition)) {
			return false;
		}
		return true;
	}

	@Override
    public String debugDump() {
        return debugDump(0);
    }
    
    @Override
    public String debugDump(int indent) {
    	return debugDump(indent, null);
    }

    protected String debugDump(int indent, LayerType layer) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append(INDENT_STRING);
        }
        sb.append(getDebugDumpClassName()).append(": ");
        sb.append(SchemaDebugUtil.prettyPrint(getTypeName()));
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "structural", structuralObjectClassDefinition, indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "auxiliary", auxiliaryObjectClassDefinitions, indent + 1);
        return sb.toString();
    }
    
    /**
     * Return a human readable name of this class suitable for logs.
     */
    @Override
    protected String getDebugDumpClassName() {
        return "crOCD";
    }

	public String getHumanReadableName() {
		if (getDisplayName() != null) {
			return getDisplayName();
		} else {
			return getKind()+":"+getIntent();
		}
	}
	
	@Override
	public String toString() {
		if (auxiliaryObjectClassDefinitions == null || auxiliaryObjectClassDefinitions.isEmpty()) {
			return getDebugDumpClassName() + " ("+getTypeName()+")";
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append(getDebugDumpClassName()).append("(").append(getTypeName());
			for (RefinedObjectClassDefinition auxiliaryObjectClassDefinition: auxiliaryObjectClassDefinitions) {
				sb.append("+").append(auxiliaryObjectClassDefinition.getTypeName());
			}
			sb.append(")");
			return sb.toString();
		}
	}
}
