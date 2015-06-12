/*
 * Copyright (c) 2015 Evolveum
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
package com.evolveum.midpoint.provisioning.impl;

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.ucf.api.Operation;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
public class ResourceObjectOperations {
	
	private Collection<Operation> operations = new ArrayList<>();
	private PrismObject<ShadowType> currentShadow = null;
	private RefinedObjectClassDefinition objectClassDefinition = null;
	
	public PrismObject<ShadowType> getCurrentShadow() {
		return currentShadow;
	}
	
	public void setCurrentShadow(PrismObject<ShadowType> currentShadow) {
		this.currentShadow = currentShadow;
	}
	
	public RefinedObjectClassDefinition getObjectClassDefinition() {
		return objectClassDefinition;
	}
	
	public void setObjectClassDefinition(RefinedObjectClassDefinition objectClassDefinition) {
		this.objectClassDefinition = objectClassDefinition;
	}
	
	public Collection<Operation> getOperations() {
		return operations;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((currentShadow == null) ? 0 : currentShadow.hashCode());
		result = prime * result + ((objectClassDefinition == null) ? 0 : objectClassDefinition.hashCode());
		result = prime * result + ((operations == null) ? 0 : operations.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ResourceObjectOperations other = (ResourceObjectOperations) obj;
		if (currentShadow == null) {
			if (other.currentShadow != null)
				return false;
		} else if (!currentShadow.equals(other.currentShadow))
			return false;
		if (objectClassDefinition == null) {
			if (other.objectClassDefinition != null)
				return false;
		} else if (!objectClassDefinition.equals(other.objectClassDefinition))
			return false;
		if (operations == null) {
			if (other.operations != null)
				return false;
		} else if (!operations.equals(other.operations))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ResourceObjectOperations(operations=" + operations + ", currentShadow=" + currentShadow
				+ ", objectClassDefinition=" + objectClassDefinition + ")";
	}

	

}
