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

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

/**
 * @author semancik
 *
 */
public class PolyStringStrictMatchingRule implements MatchingRule<PolyString> {
	
	public static final QName NAME = new QName(PrismConstants.NS_MATCHING_RULE, "polyStringStrict");

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.match.MatchingRule#getName()
	 */
	@Override
	public QName getName() {
		return NAME;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.match.MatchingRule#isSupported(java.lang.Class, javax.xml.namespace.QName)
	 */
	@Override
	public boolean isSupported(QName xsdType) {
		return (PolyStringType.COMPLEX_TYPE.equals(xsdType));
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.match.MatchingRule#match(java.lang.Object, java.lang.Object)
	 */
	@Override
	public boolean match(PolyString a, PolyString b) {
		if (a == null && b == null) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		return MiscUtil.equals(a.getOrig(), b.getOrig()) && MiscUtil.equals(a.getNorm(), b.getNorm());  
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.match.MatchingRule#normalize(java.lang.Object)
	 */
	@Override
	public PolyString normalize(PolyString original) {
		return original;
	}

}
