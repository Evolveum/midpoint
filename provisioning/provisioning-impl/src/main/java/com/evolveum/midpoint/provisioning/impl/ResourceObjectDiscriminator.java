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
package com.evolveum.midpoint.provisioning.impl;

import java.util.Collection;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;

/**
 * @author semancik
 *
 */
public class ResourceObjectDiscriminator {
	
	private QName objectClass;
	private Collection<? extends ResourceAttribute<?>> identifiers;
	// This is only cached here, it is not used for comparison
	private RefinedObjectClassDefinition objectClassDefinition;
	
	public ResourceObjectDiscriminator(QName objectClass, Collection<? extends ResourceAttribute<?>> identifiers) {
		super();
		this.objectClass = objectClass;
		this.identifiers = identifiers;
	}
	
	public ResourceObjectDiscriminator(RefinedObjectClassDefinition objectClassDefinition, 
			Collection<? extends ResourceAttribute<?>> identifiers) {
		super();
		this.objectClass = objectClassDefinition.getTypeName();
		this.identifiers = identifiers;
		this.objectClassDefinition = objectClassDefinition;
	}

	public QName getObjectClass() {
		return objectClass;
	}

	public Collection<? extends ResourceAttribute<?>> getIdentifiers() {
		return identifiers;
	}

	public RefinedObjectClassDefinition getObjectClassDefinition() {
		return objectClassDefinition;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((identifiers == null) ? 0 : identifiers.hashCode());
		result = prime * result + ((objectClass == null) ? 0 : objectClass.hashCode());
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
		ResourceObjectDiscriminator other = (ResourceObjectDiscriminator) obj;
		if (identifiers == null) {
			if (other.identifiers != null)
				return false;
		} else if (!identifiers.equals(other.identifiers))
			return false;
		if (objectClass == null) {
			if (other.objectClass != null)
				return false;
		} else if (!objectClass.equals(other.objectClass))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ResourceObjectDiscriminator(" + objectClass + ": " + identifiers + ")";
	}

}
