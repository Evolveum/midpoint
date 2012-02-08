/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */

package com.evolveum.midpoint.schema.processor;

import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismContext;

/**
 * Object Reference Schema Definition.
 * 
 * Object Reference is a property that describes reference to an object. It is
 * used to represent association between objects. For example reference from
 * User object to Account objects that belong to the user. The reference is a
 * simple uni-directional link using an OID as an identifier.
 * 
 * This type should be used for all object references so the implementations can
 * detect them and automatically resolve them.
 * 
 * This class represents schema definition for object reference. See
 * {@link Definition} for more details.
 * 
 * @author Radovan Semancik
 * 
 */
public class ObjectReferenceDefinition extends PropertyDefinition {

	private static final long serialVersionUID = 2427488779612517600L;
	private Set<QName> targetTypeNames;

	ObjectReferenceDefinition(QName name, QName defaultName, QName typeName, PrismContext prismContext) {
		super(name, defaultName, typeName, prismContext);
	}

	/**
	 * Returns valid XSD object types whose may be the targets of the reference.
	 * 
	 * Corresponds to "targetType" XSD annotation.
	 * 
	 * Returns empty set if not specified. Must not return null.
	 * 
	 * @return set of target type names
	 */
	public Set<QName> getTargetTypeNames() {
		if (targetTypeNames == null) {
			targetTypeNames = new HashSet<QName>();
		}
		return targetTypeNames;
	}

	void setTargetTypeNames(Set<QName> targetTypeNames) {
		this.targetTypeNames = targetTypeNames;
	}
}
