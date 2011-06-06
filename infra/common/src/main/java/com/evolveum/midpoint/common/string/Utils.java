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

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.common.result.OperationResultStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_1.CharacterClassType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LimitationsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.StringPolicyType;

/**
 * 
 * @author mamut
 * 
 */
public class Utils {
	private static final transient Trace logger = TraceManager.getTrace(Utils.class);
	
	public static StringPolicyType initialize(StringPolicyType sp){
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

		if (null == sp.getCharacterClass()) {
			CharacterClassType cct = new CharacterClassType();
			cct.setValue(" !\"#$%&'()*+,-.01234567890:;<=>?" + "@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_"
					+ "`abcdefghijklmnopqrstuvwxyz{|}~");

			sp.setCharacterClass(cct);
		}
		return sp;
	}

}
