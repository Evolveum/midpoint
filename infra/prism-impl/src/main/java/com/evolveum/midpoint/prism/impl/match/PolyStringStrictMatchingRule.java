/*
 * Copyright (c) 2010-2018 Evolveum
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
package com.evolveum.midpoint.prism.impl.match;

import java.util.regex.Pattern;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.prism.xml.ns._public.types_4.PolyStringType;

/**
 * @author semancik
 *
 */
public class PolyStringStrictMatchingRule implements MatchingRule<PolyString> {

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.match.MatchingRule#getName()
	 */
	@Override
	public QName getName() {
		return PrismConstants.POLY_STRING_STRICT_MATCHING_RULE_NAME;
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

	@Override
	public boolean matchRegex(PolyString a, String regex) {
		if (a == null){
			return false;
		}

		return Pattern.matches(regex, a.getOrig());
	}

}
