/*
 * Copyright (c) 2010-2013 Evolveum
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.util.DOMUtil;

/**
 * String matching rule that ignores the case.
 *
 * @author Radovan Semancik
 *
 */
public class StringIgnoreCaseMatchingRule implements MatchingRule<String> {

	public static final QName NAME = new QName(PrismConstants.NS_MATCHING_RULE, "stringIgnoreCase");

	@Override
	public QName getName() {
		return NAME;
	}

	@Override
	public boolean isSupported(QName xsdType) {
		return (DOMUtil.XSD_STRING.equals(xsdType));
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.model.match.MatchingRule#match(java.lang.Object, java.lang.Object)
	 */
	@Override
	public boolean match(String a, String b) {
		if (a == null && b == null) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		return StringUtils.equalsIgnoreCase(a, b);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.match.MatchingRule#normalize(java.lang.Object)
	 */
	@Override
	public String normalize(String original) {
		return StringUtils.lowerCase(original);
	}

	@Override
	public boolean matchRegex(String a, String regex) {
		if (a == null){
			return false;
		}

		Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(a);
		return matcher.matches();
	}

}
