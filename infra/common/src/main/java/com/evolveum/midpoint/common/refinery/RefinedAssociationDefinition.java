/*
 * Copyright (c) 2014 Evolveum
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

import org.apache.commons.lang.BooleanUtils;

import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectAssociationDirectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import java.io.Serializable;
import java.util.Collection;

public class RefinedAssociationDefinition implements Serializable {
	
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

	public QName getName() {
		return resourceObjectAssociationType.getRef();
	}
	
	public ShadowKindType getKind() {
		return resourceObjectAssociationType.getKind();
	}
	
    public Collection<String> getIntents() {
        return resourceObjectAssociationType.getIntent();
    }

    public MappingType getOutboundMappingType() {
		return resourceObjectAssociationType.getOutbound();
	}
	
	public boolean isExclusiveStrong() {
		return BooleanUtils.isTrue(resourceObjectAssociationType.isExclusiveStrong());
	}

    public boolean isIgnored() {
        return false;           // todo implement!
    }

    public boolean isTolerant() {
        return BooleanUtils.isNotFalse(resourceObjectAssociationType.isTolerant());
    }

}
