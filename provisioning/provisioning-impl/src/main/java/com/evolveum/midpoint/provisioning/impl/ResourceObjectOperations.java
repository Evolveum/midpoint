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
	private ProvisioningContext resourceObjectContext = null;
	
	public PrismObject<ShadowType> getCurrentShadow() {
		return currentShadow;
	}
	
	public void setCurrentShadow(PrismObject<ShadowType> currentShadow) {
		this.currentShadow = currentShadow;
	}
	
	public ProvisioningContext getResourceObjectContext() {
		return resourceObjectContext;
	}

	public void setResourceObjectContext(ProvisioningContext resourceObjectContext) {
		this.resourceObjectContext = resourceObjectContext;
	}

	public Collection<Operation> getOperations() {
		return operations;
	}

	@Override
	public String toString() {
		return "ResourceObjectOperations(operations=" + operations + ", currentShadow=" + currentShadow
				+ ", ctx=" + resourceObjectContext + ")";
	}

	

}
