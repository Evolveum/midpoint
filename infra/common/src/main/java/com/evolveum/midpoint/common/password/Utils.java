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
package com.evolveum.midpoint.common.password;

import java.awt.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.string.StringPolicyException;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.util.result.OperationResult;
import com.evolveum.midpoint.util.result.OperationResultStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_1.CharacterClassType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LimitationsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PasswordLifeTimeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PasswordPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.StringLimitType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.StringPolicyType;

/**
 * 
 * @author mamut
 * 
 */
public class Utils {
	private static final transient Trace logger = TraceManager.getTrace(Utils.class);

	public static PasswordPolicyType initialize(PasswordPolicyType pp) throws PasswordPolicyException {
		if (null == pp) {
			throw new PasswordPolicyException(
					new OperationResult("Password Policy Initialize ", OperationResultStatus.FATAL_ERROR,
							"PPU-001", "Provided password policcy cannot be null."));
		}

		if (null == pp.getStringPolicy()) {
			StringPolicyType sp = new StringPolicyType();
			try {
				pp.setStringPolicy(com.evolveum.midpoint.common.string.Utils.initialize(sp));
			} catch (StringPolicyException spe) {
				throw new PasswordPolicyException(spe.getResult());
			}
		}

		if (null == pp.getLifetime()) {
			PasswordLifeTimeType lt = new PasswordLifeTimeType();
			lt.setExpiration(-1);
			lt.setWarnBeforeExpiration(0);
			lt.setLockAfterExpiration(0);
			lt.setMinPasswordAge(0);
			lt.setPasswordHistoryLength(0);
		}
		return pp;
	}

	public static String generatePassword() throws PasswordPolicyException {
		return generatePassword(null);
	}

	public static String generatePassword(PasswordPolicyType pp) throws PasswordPolicyException {

		// If no police defined use default values
		if (null == pp) {
			pp = new PasswordPolicyType();
			initialize(pp);
		}
		pp.getStringPolicy();

		ArrayList<String> validChars = tokenizeCharacterClass(pp.getStringPolicy().getCharacterClass());
		int minSize = pp.getStringPolicy().getLimitatitions().getMinLength();
		int maxSize = pp.getStringPolicy().getLimitatitions().getMaxLength();
		int uniqChars = pp.getStringPolicy().getLimitatitions().getMinUniqueChars();

		if (uniqChars > minSize) {
			minSize = uniqChars;
		}

		if (-1 != maxSize && minSize > maxSize) {
			throw new PasswordPolicyException(new OperationResult("PPG", OperationResultStatus.FATAL_ERROR,
					"PPG-001", "Minimal size (" + minSize + ") cannot be bigger then maximal size ("
							+ maxSize + ") in password policy:"
							+ (null == pp.getName() ? "Unamed policy" : pp.getName())));
		}

		// find limitation which need to be run as first
		ArrayList<StringLimitType> limits = shuffleLimitations(pp.getStringPolicy().getLimitatitions());

		StringBuilder password = new StringBuilder();
		
		for (StringLimitType l : limits) {
			if (isAlreadyMeetThis(pp,l, password.toString())) {

			} else {
				password.append(passwordgenerateCharFrom(pp, l));
			}
		}
		// Check if maximal size not exceeded
		if (maxSize != -1 && maxSize < password.length()) {
			String msg = "Maximal size (" + maxSize
			+ ") of password was exceeded because generated password size was ("
			+ password.length() + ") based on password policy:"
			+ (null == pp.getName() ? "Unamed policy" : pp.getName()) + ".To fix it please make policy less restrictive";
			logger.error(msg);
			throw new PasswordPolicyException(new OperationResult("PPG", OperationResultStatus.FATAL_ERROR,"PPG-002",msg));
		}

		return null;
	}
	/*
	 * Generate one character based on this policy
	 */
	private static String passwordgenerateCharFrom(PasswordPolicyType pp, StringLimitType l) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * Check if any character not meet policy
	 */
	private static boolean isAlreadyMeetThis(PasswordPolicyType pp, StringLimitType l, String password) {
		// Get Valid chars for this policy
		ArrayList<String> validChars;
		if (null == l.getCharacterClass() || null != l.getCharacterClass() || StringUtils.isEmpty(l.getCharacterClass().getValue())) {
			validChars = tokenizeCharacterClass(l.getCharacterClass());
		}
		
		return false;
	}

	/*
	 * Parse out limitations and add them shuffled to list
	 */
	private static ArrayList<StringLimitType> shuffleLimitations(LimitationsType lims) {
		if (null == lims || lims.getLimit().isEmpty()) {
			return new ArrayList<StringLimitType>();
		}
		ArrayList<StringLimitType> partA = new ArrayList<StringLimitType>();
		ArrayList<StringLimitType> partB = new ArrayList<StringLimitType>();

		// Split to which can be first and which not
		for (StringLimitType l : lims.getLimit()) {
			if (l.isCanBeFirst() == true) {
				partA.add(l);
			} else {
				partB.add(l);
			}
		}
		// Shuffle both
		Collections.shuffle(partA, new Random(System.currentTimeMillis()));
		Collections.shuffle(partB, new Random(System.currentTimeMillis() - 4555));

		// Add limitations which cannot be first to end
		partA.addAll(partB);
		return partA;
	}

	/*
	 * Prepare usable list of strings for generator
	 */
	private static ArrayList<String> tokenizeCharacterClass(CharacterClassType cc, QName ref) {
		ArrayList<String> l = new ArrayList<String>();

		if (null != cc.getValue() && (cc.getName() == ref || null == ref)) {
			String a[] = cc.getValue().split("");
			// Add all to list
			for (int i = 0; i < a.length; i++) {
				l.add(a[i]);
			}
		} else if (null != cc.getCharacterClass() && !cc.getCharacterClass().isEmpty()) {
			// Process all sub lists
			for (CharacterClassType subClass : cc.getCharacterClass()) {
				// If we found requested name or no name defined
				if (cc.getName() == ref || null == ref) {
					l.addAll(tokenizeCharacterClass(subClass, null));
				} else {
					l.addAll(tokenizeCharacterClass(subClass, ref));
				}
			}
		}
		// Remove duplicity in return;
		return new ArrayList<String>(new HashSet<String>(l));
	}

	private static ArrayList<String> tokenizeCharacterClass(CharacterClassType cc) {

		return tokenizeCharacterClass(cc, null);
	}
}
