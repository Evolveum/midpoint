/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.provisioning.ucf.api;

import java.io.Serializable;
import java.util.Collection;

import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;

/**
 * @author semancik
 *
 */
public class AttributesToReturn implements Serializable {
	private static final long serialVersionUID = 157146351122133667L;

	// TODO consider adding "fetchEntitlements" flag here
	private boolean returnDefaultAttributes = true;
	private boolean returnPasswordExplicit = false;
	private boolean returnAdministrativeStatusExplicit = false;
	private boolean returnLockoutStatusExplicit = false;
	private boolean returnValidFromExplicit = false;
	private boolean returnValidToExplicit = false;
	Collection<? extends ResourceAttributeDefinition> attributesToReturn = null;
	
	public boolean isReturnDefaultAttributes() {
		return returnDefaultAttributes;
	}
	
	public void setReturnDefaultAttributes(boolean returnDefaultAttributes) {
		this.returnDefaultAttributes = returnDefaultAttributes;
	}
	
	public Collection<? extends ResourceAttributeDefinition> getAttributesToReturn() {
		return attributesToReturn;
	}
	
	public void setAttributesToReturn(Collection<? extends ResourceAttributeDefinition> attributesToReturn) {
		this.attributesToReturn = attributesToReturn;
	}

	public boolean isReturnPasswordExplicit() {
		return returnPasswordExplicit;
	}

	public void setReturnPasswordExplicit(boolean returnPasswordExplicit) {
		this.returnPasswordExplicit = returnPasswordExplicit;
	}

	public boolean isReturnAdministrativeStatusExplicit() {
		return returnAdministrativeStatusExplicit;
	}

	public void setReturnAdministrativeStatusExplicit(boolean returnAdministrativeStatusExplicit) {
		this.returnAdministrativeStatusExplicit = returnAdministrativeStatusExplicit;
	}

	public boolean isReturnLockoutStatusExplicit() {
		return returnLockoutStatusExplicit;
	}

	public void setReturnLockoutStatusExplicit(boolean returnLockoutStatusExplicit) {
		this.returnLockoutStatusExplicit = returnLockoutStatusExplicit;
	}

	public boolean isReturnValidFromExplicit() {
		return returnValidFromExplicit;
	}

	public void setReturnValidFromExplicit(boolean returnValidFromExplicit) {
		this.returnValidFromExplicit = returnValidFromExplicit;
	}

	public boolean isReturnValidToExplicit() {
		return returnValidToExplicit;
	}

	public void setReturnValidToExplicit(boolean returnValidToExplicit) {
		this.returnValidToExplicit = returnValidToExplicit;
	}

	@Override
	public String toString() {
		return "AttributesToReturn(returnDefaultAttributes=" + returnDefaultAttributes + ", returnPasswordExplicit="
				+ returnPasswordExplicit
				+ ", returnAdministrativeStatusExplicit="+ returnAdministrativeStatusExplicit
				+ ", returnValidFromExplicit="+ returnValidFromExplicit
				+ ", returnValidToExplicit="+ returnValidToExplicit
				+ ", attributesToReturn=" + attributesToReturn + ")";
	}

}
