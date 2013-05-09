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
	
	private boolean returnDefaultAttributes = true;
	private boolean returnPasswordExplicit = false;
	private boolean returnAdministrativeStatusExplicit = false;
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

	@Override
	public String toString() {
		return "AttributesToReturn(returnDefaultAttributes=" + returnDefaultAttributes + ", returnPasswordExplicit="
				+ returnPasswordExplicit + ", returnAdministrativeStatusExplicit="
						+ returnAdministrativeStatusExplicit + ", attributesToReturn=" + attributesToReturn + ")";
	}

}
