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
package com.evolveum.midpoint.prism.match;

import javax.xml.namespace.QName;

/**
 * Interface for generic matching rules. The responsibility of a matching rule is to decide if
 * two objects of the same type match. This may seem a simple thing to do but the details may get
 * quite complex. E.g. comparing string in case sensitive or insensitive manner, comparing PolyStrings, etc.
 * 
 * @author Radovan Semancik
 *
 */
public interface MatchingRule<T> {

	/**
	 * QName that identifies the rule. This QName may be used to refer to this specific matching rule,
	 * it is an matching rule identifier.
	 */
	QName getName();
	
	/**
	 * Returns true if the rule can be applied to the specified XSD type.
	 */
	boolean isSupported(QName xsdType);
	
	/**
	 * Matches two objects. 
	 */
	boolean match(T a, T b);
	
	/**
	 * Returns a normalized version of the value.
	 * For normalized version the following holds:
	 * if A matches B then normalize(A) == normalize(B)
	 */
	T normalize(T original);
}
