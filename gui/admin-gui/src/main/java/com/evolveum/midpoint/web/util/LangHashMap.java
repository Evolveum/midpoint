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
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.web.util;

import java.io.Serializable;
import java.util.HashMap;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.web.controller.Language;

/**
 * Like normal HashMap, but if it can't translate key (can't find value) returns
 * key with '!' on both sides of key.
 */
public class LangHashMap extends HashMap<String, String> implements Serializable {

	private static final long serialVersionUID = -2959760125355416304L;
	private static final Trace TRACE = TraceManager.getTrace(Language.class);

	@Override
	public String get(Object key) {
		if (key == null) {
			TRACE.warn("Key for translation can't be null.");
			return null;
		}

		if (!containsKey(key)) {
			TRACE.warn("Can't find key '" + key + "' in resource for translation.");
			return "!" + key + "!";
		}

		return super.get(key.toString());
	}
}
