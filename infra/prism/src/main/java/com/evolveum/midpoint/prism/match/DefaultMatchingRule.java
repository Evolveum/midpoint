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

import com.evolveum.midpoint.prism.Matchable;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.util.MiscUtil;

/**
 * Default matching rule used as a fall-back if no explicit matching rule is specified.
 * It is simply using java equals() method to match values.
 * 
 * @author Radovan Semancik
 */
public class DefaultMatchingRule<T> implements MatchingRule<T> {
	
	public static final QName NAME = new QName(PrismConstants.NS_MATCHING_RULE, "default");

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.model.match.MatchingRule#getUrl()
	 */
	@Override
	public QName getName() {
		return NAME;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.model.match.MatchingRule#isSupported(java.lang.Class, javax.xml.namespace.QName)
	 */
	@Override
	public boolean isSupported(QName xsdType) {
		// We support everything. We are the default.
		return true;
	}
	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.model.match.MatchingRule#match(java.lang.Object, java.lang.Object)
	 */
	@Override
	public boolean match(T a, T b) {
		if (a == null && b == null) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		if (a instanceof Matchable){
			return ((Matchable)a).match((Matchable)b);
		}
		// Just use plain java equals() method
		return a.equals(b);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.match.MatchingRule#normalize(java.lang.Object)
	 */
	@Override
	public T normalize(T original) {
		return original;
	}
	
}
