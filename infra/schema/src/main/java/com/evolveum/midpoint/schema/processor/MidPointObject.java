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
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.schema.processor;

/**
 * Common supertype for all identity objects. Defines basic properties that each
 * object must have to live in our system (identifier, name).
 * 
 * Objects consists of identifier and name (see definition below) and a set of
 * properties represented as XML elements in the object's body. The attributes
 * are represented as first-level XML elements (tags) of the object XML
 * representation and may be also contained in other tags (e.g. extension,
 * attributes). The QName (namespace and local name) of the element holding the
 * property is considered to be a property name.
 * 
 * This class is named MidPointObject instead of Object to avoid confusion with
 * java.lang.Object.
 * 
 * @author Radovan Semancik
 * 
 */
public class MidPointObject extends PropertyContainer {

	/**
	 * Returns Object ID (OID).
	 * 
	 * May return null if the object does not have an OID.
	 * 
	 * @return Object ID (OID)
	 */
	public String getOid() {
		throw new IllegalStateException("not implemented yet.");
	}
}
