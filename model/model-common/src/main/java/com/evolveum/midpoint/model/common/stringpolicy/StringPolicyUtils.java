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
package com.evolveum.midpoint.model.common.stringpolicy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.text.StrBuilder;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CharacterClassType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LimitationsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StringPolicyType;

/**
 * 
 * @author mamut
 * 
 */
public class StringPolicyUtils {

	private static final transient Trace LOGGER = TraceManager.getTrace(StringPolicyUtils.class);

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
	
	/**z
	 * Convert string to array 
	 * @param in
	 * @return ArrayList
	 */
	public static List<String> stringTokenizer(String in) {
		List<String> l = new ArrayList<String>();
		for (String a: in.split("")) {
			if (!a.isEmpty()) {
				l.add(a);
			}
		}
		return l;
	}
}
