/*
 * Copyright (c) 2014-2017 Evolveum
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

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.BooleanUtils;

import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.util.ItemPathTypeUtil;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class RefinedAssociationDefinition implements Serializable, Visitable {
	private static final long serialVersionUID = 1L;

	private Map<LayerType, PropertyLimitations> limitationsMap;
	
	private ResourceObjectAssociationType resourceObjectAssociationType;
	private RefinedObjectClassDefinition associationTarget;

	public RefinedAssociationDefinition(ResourceObjectAssociationType resourceObjectAssociationType) {
		super();
		this.resourceObjectAssociationType = resourceObjectAssociationType;
	}

	public ResourceObjectAssociationType getResourceObjectAssociationType() {
		return resourceObjectAssociationType;
	}

	public RefinedObjectClassDefinition getAssociationTarget() {
		return associationTarget;
	}

	public void setAssociationTarget(RefinedObjectClassDefinition associationTarget) {
		this.associationTarget = associationTarget;
	}

	public ItemName getName() {
		return ItemPathTypeUtil.asSingleNameOrFail(resourceObjectAssociationType.getRef());
	}

	public ShadowKindType getKind() {
		return resourceObjectAssociationType.getKind();
	}

    public Collection<String> getIntents() {
        return resourceObjectAssociationType.getIntent();
    }

    public QName getAuxiliaryObjectClass() {
		return resourceObjectAssociationType.getAuxiliaryObjectClass();
	}

	public MappingType getOutboundMappingType() {
		return resourceObjectAssociationType.getOutbound();
	}
	
	public List<MappingType> getInboundMappingTypes() {
		return resourceObjectAssociationType.getInbound();
	}

	public boolean isExclusiveStrong() {
		return BooleanUtils.isTrue(resourceObjectAssociationType.isExclusiveStrong());
	}

    public boolean isIgnored() {
    	return false;           // todo implement!
    }
    
    public boolean isIgnored(LayerType layer) {
		QName name = getAssociationAttribute();
		RefinedAttributeDefinition<?> associationAttributeDef = associationTarget.findAttributeDefinition(name);
		if (associationAttributeDef == null) {
			throw new IllegalStateException("No such attribute :" + name
					+ " in kind: " + associationTarget.getKind() + ", intent: " + associationTarget.getIntent()
					+ " as defined for association: " + resourceObjectAssociationType.getDisplayName());
    	}
    	
    	return associationAttributeDef.isIgnored(layer);
    }

    private QName getAssociationAttribute() {
		ResourceObjectAssociationDirectionType direction = resourceObjectAssociationType.getDirection();
		if (ResourceObjectAssociationDirectionType.OBJECT_TO_SUBJECT.equals(direction)) {
			return resourceObjectAssociationType.getAssociationAttribute();
		}

		return resourceObjectAssociationType.getValueAttribute();
	}
    
    public PropertyLimitations getLimitations(LayerType layer) {
		QName name = getAssociationAttribute();
		RefinedAttributeDefinition<?> associationAttributeDef = associationTarget.findAttributeDefinition(name);
		if (associationAttributeDef == null) {
			throw new IllegalStateException("No such attribute :" + name
					+ " in kind: " + associationTarget.getKind() + ", intent: " + associationTarget.getIntent()
					+ " as defined for association: " + resourceObjectAssociationType.getDisplayName());
    	}
    	
    	return associationAttributeDef.getLimitations(layer);
    }
        
    public boolean isTolerant() {
        return BooleanUtils.isNotFalse(resourceObjectAssociationType.isTolerant());
    }

    @NotNull
	public List<String> getTolerantValuePattern() {
		return resourceObjectAssociationType.getTolerantValuePattern();
	}

	@NotNull
	public List<String> getIntolerantValuePattern() {
		return resourceObjectAssociationType.getIntolerantValuePattern();
	}

	public boolean requiresExplicitReferentialIntegrity() {
		return !BooleanUtils.isFalse(getResourceObjectAssociationType().isExplicitReferentialIntegrity());	// because default is TRUE
	}

	public QName getMatchingRule() {
		return getResourceObjectAssociationType().getMatchingRule();
	}

	public String getDisplayName() {
		return resourceObjectAssociationType.getDisplayName();
	}
	
	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

	public RefinedAssociationDefinition clone() {
		RefinedAssociationDefinition clone = new RefinedAssociationDefinition(resourceObjectAssociationType);
		copyValues(clone);
		return clone;
	}

	private void copyValues(RefinedAssociationDefinition clone) {
		clone.associationTarget = this.associationTarget;
	}
}
