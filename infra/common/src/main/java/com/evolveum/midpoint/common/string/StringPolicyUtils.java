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
 * Portions Copyrighted 2011 Peter Prochazka
 */
package com.evolveum.midpoint.common.string;

import java.util.ArrayList;
import java.util.HashSet;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrBuilder;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.common.result.OperationResultStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_1.CharacterClassType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LimitationsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.StringLimitType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.StringPolicyType;

/**
 * 
 * @author mamut
 * 
 */
public class StringPolicyUtils {

	private static final transient Trace logger = TraceManager.getTrace(StringPolicyUtils.class);

	private static final String ASCII7_CHARS = " !\"#$%&'()*+,-.01234567890:;<=>?"
			+ "@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_" + "`abcdefghijklmnopqrstuvwxyz{|}~";

	public static StringPolicyType normalize(StringPolicyType sp) {
		if (null == sp) {
			throw new IllegalArgumentException("Providide string policy cannot be null");
		}

		if (null == sp.getLimitations()) {
			LimitationsType sl = new LimitationsType();
			sl.setCheckAgainstDictionary(false);
			sl.setCheckPattern("");
			sl.setMaxLength(-1);
			sl.setMinLength(0);
			sl.setMinUniqueChars(0);
			sp.setLimitations(sl);
		}

		// Add default char class
		if (null == sp.getCharacterClass()) {
			CharacterClassType cct = new CharacterClassType();
			cct.setValue(ASCII7_CHARS);
			sp.setCharacterClass(cct);
		}

		return sp;
	}
	
	/**
	 * Prepare usable list of strings for generator
	 * 
	 */

	
	public static String collectCharacterClass(CharacterClassType cc, QName ref) {
		StrBuilder l = new StrBuilder();
		if (null == cc) {
			throw new IllegalArgumentException("Character class cannot be null");
		}

		if (null != cc.getValue() && (null == ref || ref.equals(cc.getName()))) {
			l.append(cc.getValue());
		} else if (null != cc.getCharacterClass() && !cc.getCharacterClass().isEmpty()) {
			// Process all sub lists
			for (CharacterClassType subClass : cc.getCharacterClass()) {
				// If we found requested name or no name defined
				if (null == ref || ref.equals(cc.getName())) {
					l.append(collectCharacterClass(subClass, null));
				} else {
					l.append(collectCharacterClass(subClass, ref));
				}
			}
		}
		// Remove duplicity in return;
		HashSet<String> h = new HashSet<String>();
		for (String s : l.toString().split("")) {
			h.add(s);
		}
		return new StrBuilder().appendAll(h).toString();
	}
	
	/**
	 * Convert string to array 
	 * @param in
	 * @return ArrayList
	 */
	public static ArrayList<String> stringTokenizer(String in) {
		ArrayList<String> l = new ArrayList<String>();
		String a[] = in.split("");
		// Add all to list , STrat at 1 because a[0] contains ""
		for (int i = 1; i < a.length; i++) {
			l.add(a[i]);
		}
		return l;
	}
}
