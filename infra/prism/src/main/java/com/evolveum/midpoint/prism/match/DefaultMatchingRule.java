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
package com.evolveum.midpoint.prism.match;

import java.util.regex.Pattern;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Matchable;
import com.evolveum.midpoint.prism.PrismConstants;

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
		if (a instanceof Matchable && b instanceof Matchable) {
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

	@Override
	public boolean matchRegex(T a, String regex) {
		String valueToMatch = null;
		if (a instanceof Matchable){
			return ((Matchable) a).matches(regex);
		} else if (a instanceof String){
			valueToMatch = (String) a;
		} else if (a instanceof Integer){
			valueToMatch = Integer.toString((Integer) a);
		} else {
			valueToMatch = String.valueOf(a);
		}

		return Pattern.matches(regex, valueToMatch);
	}

}
