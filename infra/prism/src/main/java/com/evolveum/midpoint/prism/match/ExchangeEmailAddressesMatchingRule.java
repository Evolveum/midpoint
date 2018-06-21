/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.prism.match;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.util.DOMUtil;
import org.apache.commons.lang.StringUtils;

import javax.xml.namespace.QName;
import java.util.regex.Pattern;

/**
 * A specific matching rule for Microsoft Exchange EmailAddresses attribute consisting of SMTP:/smtp: prefix and email address.
 * It considers the case in the prefix but ignores the case in the email address.
 *
 * @author Pavol Mederly
 *
 */
public class ExchangeEmailAddressesMatchingRule implements MatchingRule<String> {

	public static final QName NAME = new QName(PrismConstants.NS_MATCHING_RULE, "exchangeEmailAddresses");

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
		a = a.trim();
		b = b.trim();
		if (a.equals(b)) {
			return true;
		}
		String aPrefix = getPrefix(a);
		String aSuffix = getSuffix(a);
		String bPrefix = getPrefix(b);
		String bSuffix = getSuffix(b);
		return StringUtils.equals(aPrefix, bPrefix) && StringUtils.equalsIgnoreCase(aSuffix, bSuffix);
	}

	private String getPrefix(String a) {
		int i = a.indexOf(':');
		if (i < 0) {
			return null;
		} else {
			return a.substring(0, i);
		}
	}

	private String getSuffix(String a) {
		int i = a.indexOf(':');
		if (i < 0) {
			return a;
		} else {
			return a.substring(i+1);
		}
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.match.MatchingRule#normalize(java.lang.Object)
	 */
	@Override
	public String normalize(String original) {
		String prefix = getPrefix(original);
		String suffix = StringUtils.lowerCase(getSuffix(original));
		if (prefix == null) {
			return suffix;
		} else {
			return prefix + ":" + suffix;
		}
	}

	@Override
	public boolean matchRegex(String a, String regex) {
		if (a == null) {
			return false;
		}
		return Pattern.matches(regex, a);			// we ignore case-insensitiveness of the email address
	}

}
