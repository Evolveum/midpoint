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

package com.evolveum.midpoint.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
/**
 * Escapes special characters : \|.-^?*+{}()[] 
 * 
 * @author mamut
 *
 */

public class XQueryEscapeStringBuilder extends GenericEscapeStringBuilder {
	
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 8721309397203556565L;
	private static final Map<Character,String> entities = Collections.unmodifiableMap(new HashMap<Character, String>(14) {
		private static final long serialVersionUID = -2403122042276533790L;
	{
			put('\\',"\\");	// 1
			put('|',"|");		// 2
			put('.',".");		// 3
			put('-',"-");		// 4
			put('^',"^");		// 5
			put('?',"?");		// 6
			put('*',"*");		// 7
			put('+',"+");		// 8
			put('{',"{{");		// 9
			put('}',"}}");		//10
			put('(',"(");		//11
			put(')',")");		//12
			put('[',"[");		//13
			put(']',"]");		//14
	}}
	);

	
	@Override
	public EscapeStringBuilder eappend(Object o) {
		return super.append(escape(o.toString()));
	}

	@Override
	public EscapeStringBuilder eappend(String str) {
		return super.append(escape(str));
	}

	private StringBuffer escape(String str) {
		StringBuffer buf = new StringBuffer(str.length() * 2);
		int i;
		for (i = 0; i < str.length(); ++i) {
			char ch = str.charAt(i);
			if (entities.containsKey(ch)) {
				buf.append(entities.get(ch));
			} else {
				buf.append(ch);
			}
		}
		return buf;
	}

}
