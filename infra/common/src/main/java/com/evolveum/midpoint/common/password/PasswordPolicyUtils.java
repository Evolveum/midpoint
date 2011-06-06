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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;

import javax.xml.namespace.QName;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.common.result.OperationResultStatus;
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
public class PasswordPolicyUtils {
	private static final transient Trace logger = TraceManager.getTrace(PasswordPolicyUtils.class);
	private static final long GENERATOR_TIMEOUT = 10000; // ms

	/**
	 * 
	 * @param pp
	 * @return
	 * @throws PasswordPolicyException
	 */
	public static PasswordPolicyType initialize(PasswordPolicyType pp) {
		if (null == pp) {
			throw new IllegalArgumentException("Password policy cannot be null");
		}

		if (null == pp.getStringPolicy()) {
			StringPolicyType sp = new StringPolicyType();
			pp.setStringPolicy(com.evolveum.midpoint.common.string.Utils.initialize(sp));

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

	public static OperationResult validatePassword(String password, PasswordPolicyType pp) {
		return null;
	}

	/**
	 * Generate password on provided password policy
	 * 
	 * @param pp
	 *            password policy
	 * @return Generated password string
	 * @throws PasswordPolicyException
	 */
	public static String generatePassword(PasswordPolicyType pp, OperationResult generationResult) {

		// If no police defined use default values
		if (null == pp) {
			throw new IllegalArgumentException("Provided password policy is NULL");
		}
		// Add missing and default parts
		pp = initialize(pp);

		// Get global borders
		int minSize = pp.getStringPolicy().getLimitations().getMinLength();
		int maxSize = pp.getStringPolicy().getLimitations().getMaxLength();
		int uniqChars = pp.getStringPolicy().getLimitations().getMinUniqueChars(); // TODO

		if (uniqChars > minSize) {
			minSize = uniqChars;
		}

		// test if there no basic logical error
		if (-1 != maxSize && minSize > maxSize) {

			generationResult.recordFatalError("Minimal size (" + minSize
					+ ") cannot be bigger then maximal size (" + maxSize + ") in password policy:"
					+ (null == pp.getName() ? "Unamed policy" : pp.getName()));

			// no more processing
			return null;
		}

		// find limitation which need to be run as first
		ArrayList<StringLimitType> limits = shuffleLimitations(pp.getStringPolicy().getLimitations());

		StringBuilder password = new StringBuilder();
		Random r = new Random(System.currentTimeMillis());

		int countedUniqueChars = -1;
		long timeOut = System.currentTimeMillis() + GENERATOR_TIMEOUT;
		// password.append("a!0emef"); // TODO remove only for testing

		// Do while not meet uniqness criteria
		while (countedUniqueChars <= uniqChars && System.currentTimeMillis() < timeOut) {
			// process all limitation to generate password
			for (StringLimitType l : limits) {
				normalizeLimit(l);
				logger.debug("Processing limit: " + l.getDescription());

				// test if requirements meet
				OperationResult op = isAlreadyMeetThis(pp, l, password.toString());

				if (op.isSuccess()) {
					// next iteration
					continue;
				} else if (op.getStatus() == OperationResultStatus.WARNING) {
					// Generate character based on policy
					ArrayList<String> validChars = extractValidChars(pp, l);
					password.append(validChars.get(r.nextInt(validChars.size())));
				} else {
					generationResult.recordPartialError("Some limitation are not meetable.");
					generationResult.addSubresult(op);
				}
			}

			int maxErrorCounter = maxSize * 100;
			// Generate remaining chars to meet minimal randomly
			while (password.length() < minSize) {
				// select one randomly and try to add
				StringLimitType l = limits.get(r.nextInt(limits.size()));
				ArrayList<String> validChars = extractValidChars(pp, l);
				password.append(validChars.get(r.nextInt(validChars.size())));

				// validate if others are not exceeding after this one
				for (StringLimitType vl : limits) {
					if (!isAlreadyMeetThis(pp, vl, password.toString()).isSuccess()) {
						// If one is exceeded after this remove character
						password.deleteCharAt(password.length() - 1);
						break;
					}
				}
				maxErrorCounter--;
				if (maxErrorCounter == 0) {
					generationResult
							.recordFatalError("Generator not be able generate new password based on provided password policy: "
									+ pp.getName());
					break;
				}
			}

			// UNIQNESS TESTING AND REGENERATING
			// Get Unique chars;
			ArrayList<String> tmp = new ArrayList(new HashSet<String>(tokenizeString(password.toString())));
			countedUniqueChars = tmp.size();

			// no more processing is need password is OK
			if (tmp.size() >= uniqChars) {
				break;
			}

			// remove all duplicities
			for (String i : tmp) {
				// character are on two different places
				if (password.indexOf(i) != password.lastIndexOf(i)) {
					// delete it on second place
					password.deleteCharAt(password.lastIndexOf(i));
				}
			}
		}

		if (System.currentTimeMillis() >= timeOut) {
			generationResult
					.recordFatalError("Generator surender generation of password based on password police:"
							+ pp.getName() + " after " + GENERATOR_TIMEOUT + " ms");
			return null;
		}

		// If there is any limitation based error skip processing
		if (generationResult.getStatus() == OperationResultStatus.PARTIAL_ERROR) {
			return null;
		}

		// Check if maximal size not exceeded
		if (maxSize != -1 && maxSize < password.length()) {
			String msg = "Maximal size (" + maxSize
					+ ") of password was exceeded because generated password size was (" + password.length()
					+ ") based on password policy:" + (null == pp.getName() ? "Unamed policy" : pp.getName());

			logger.error(msg);
			// unable to get valid password clear generated and return NULL
			generationResult.recordFatalError(msg);
			return null;
		}

		// Check if minimal criteria was meet
		if (minSize <= password.length()) {
			generationResult.recordSuccess();
			return password.toString();
		}
		// Look like minimal criteria not meet.
		return null;
	}

	/**
	 * Normalize and add default values
	 * 
	 * @param l
	 */
	private static void normalizeLimit(StringLimitType l) {
		if (null == l) {
			throw new IllegalArgumentException();
		}
		if (null == l.getMaxOccurs()) {
			l.setMaxOccurs(-1);
		}

		if (null == l.getMinOccurs()) {
			l.setMinOccurs(0);
		}
	}

	/**
	 * Check if provided password meet required single limitation
	 * 
	 * @param pp
	 * @param l
	 * @param password
	 * @return OperationResult FATAL - if password exceed limitation Warning -
	 *         If password not reach limitation Success - if all requirements
	 *         met
	 */
	private static OperationResult isAlreadyMeetThis(PasswordPolicyType pp, StringLimitType l, String password) {
		// Get Valid chars for this policy
		ArrayList<String> validChars = extractValidChars(pp, l);

		OperationResult result = new OperationResult("Password Policy ["
				+ (null != pp.getName() ? pp.getName() : "Unamed password policy")
				+ "] single limit validation for [" + l.getDescription() + "]");

		int counter = 0;
		if (password.length() != 0) {
			// count how many characters from password is there
			for (String letter : tokenizeString(password)) {
				if (validChars.contains(letter)) {
					counter++;
				}
			}
			// If first characters is required and not there from valid chars
			if (l.isMustBeFirst() && !validChars.contains(password.substring(0, 1))) {
				result.recordFatalError("Provided password not contain required character on first place. "
						+ password.substring(0, 1) + "***** ∉  " + listToString(validChars));
				return result;
			}
		}

		// if number of characters not meet minimal criteria
		if (counter < l.getMinOccurs()) {
			result.recordStatus(
					OperationResultStatus.WARNING,
					"Provided password not contain enought characters from set" + listToString(validChars)
							+ " recognized count of charactes: " + counter + " but required "
							+ l.getMinOccurs());
			return result;

			// if number of characters not exceed maximum
		} else if (counter > l.getMaxOccurs() && l.getMaxOccurs() != -1) {
			result.recordFatalError("Provided password contains more characters (" + counter
					+ ") then required maximum " + l.getMaxOccurs());
			return result;
		}

		logger.debug("Criteria for password was met. -> " + l.getDescription());
		result.recordSuccess();
		return result;
	}

	/**
	 * Extract validchars for specified limitation from policy
	 * 
	 * @param pp
	 *            - password policy
	 * @param l
	 *            - limitation
	 * @return Array of unique valid characters
	 */

	private static ArrayList<String> extractValidChars(PasswordPolicyType pp, StringLimitType l)
			throws IllegalArgumentException {
		if (null == pp || null == l) {
			throw new IllegalArgumentException("Provided password policy or limitation is null.");
		}
		ArrayList<String> validChars = new ArrayList<String>();
		// Is any character class defined in limitation?
		if (null != l.getCharacterClass()) {
			// Is it referenced or included
			if (null != l.getCharacterClass().getRef()) {
				// It's included
				logger.debug("Characters are referenced to : "
						+ l.getCharacterClass().getRef().getLocalPart());
				validChars = tokenizeCharacterClass(pp.getStringPolicy().getCharacterClass(), l
						.getCharacterClass().getRef());
			} else {
				// it's included
				logger.debug("Characters are included :" + l.getCharacterClass().getValue());
				validChars = tokenizeCharacterClass(l.getCharacterClass(), null);
			}
		} else {
			// use whole class
			logger.debug("Cahracters are not included and not referenced use all from policy.");
			validChars = tokenizeCharacterClass(pp.getStringPolicy().getCharacterClass(), null);
		}
		if (validChars.size() == 0) {
			logger.error("No valid chars found in " + pp.getName() + "for " + l.getDescription());
		}
		logger.debug("Valid chars are:" + listToString(validChars));
		return validChars;
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
			if (l.isMustBeFirst() == true) {
				partA.add(l);
			} else {
				partB.add(l);
			}
			// If there is more characters of this type required just multiply
			// limits
			if (l.getMinOccurs() > 1) {
				for (int i = 1; i < l.getMinOccurs(); i++) {
					partB.add(l);
				}
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
		if (null == cc) {
			throw new IllegalArgumentException("Character class cannot be null");
		}

		if (null != cc.getValue() && (null == ref || ref.equals(cc.getName()))) {
			String a[] = cc.getValue().split("");
			// Add all to list
			for (int i = 1; i < a.length; i++) {
				l.add(a[i]);
			}
		} else if (null != cc.getCharacterClass() && !cc.getCharacterClass().isEmpty()) {
			// Process all sub lists
			for (CharacterClassType subClass : cc.getCharacterClass()) {
				// If we found requested name or no name defined
				if (null == ref || ref.equals(cc.getName())) {
					l.addAll(tokenizeCharacterClass(subClass, null));
				} else {
					l.addAll(tokenizeCharacterClass(subClass, ref));
				}
			}
		}
		// Remove duplicity in return;
		return new ArrayList<String>(new HashSet<String>(l));
	}

	private static ArrayList<String> tokenizeString(String in) {
		ArrayList<String> l = new ArrayList<String>();
		String a[] = in.split("");
		// Add all to list
		for (int i = 0; i < a.length; i++) {
			if (!"".equals(a[i])) {
				l.add(a[i]);
			}
		}
		return l;
	}

	private static String listToString(ArrayList<String> l) {
		if (l.size() == 0) {
			return "[ ]";
		}
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (String i : l) {
			sb.append(i);
			sb.append(" ,");
		}
		sb.append("]");
		return sb.toString();
	}
}
