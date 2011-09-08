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

import java.util.HashSet;
import java.util.List;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.common.result.OperationResultStatus;
import com.evolveum.midpoint.common.string.StringPolicyUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
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
	private static final transient Trace LOGGER = TraceManager.getTrace(PasswordPolicyUtils.class);

	/**
	 * add defined default values
	 * 
	 * @param pp
	 * @return
	 */
	public static void normalize(PasswordPolicyType pp) {
		if (null == pp) {
			throw new IllegalArgumentException("Password policy cannot be null");
		}

		if (null == pp.getStringPolicy()) {
			StringPolicyType sp = new StringPolicyType();
			pp.setStringPolicy(StringPolicyUtils.normalize(sp));
		} else {
			pp.setStringPolicy(StringPolicyUtils.normalize(pp.getStringPolicy()));
		}

		if (null == pp.getLifetime()) {
			PasswordLifeTimeType lt = new PasswordLifeTimeType();
			lt.setExpiration(-1);
			lt.setWarnBeforeExpiration(0);
			lt.setLockAfterExpiration(0);
			lt.setMinPasswordAge(0);
			lt.setPasswordHistoryLength(0);
		}
		return;
	}

	

	/**
	 * Check provided password against provided policy
	 * 
	 * @param password
	 *            - password to check
	 * @param policies
	 *            - Password List of policies used to check
	 * @param result
	 *            - Operation result of password validator.
	 * @return true if password meet all criteria , false if any criteria is not
	 *         met
	 */

	public static boolean validatePassword(String password, List <PasswordPolicyType> policies, OperationResult result) {
		boolean ret=true;
		//iterate through policies 
		for (PasswordPolicyType pp: policies) {
			OperationResult op = validatePassword(password, pp);
			result.addSubresult(op);
			//if one fail then result is failure
			if (ret == true && ! op.isSuccess()) {
				ret = false;
			}
		}
		return ret;
	}
	
	/**
	 * Check provided password against provided policy
	 * 
	 * @param password
	 *            - password to check
	 * @param pp
	 *            - Password policy used to check
	 * @param result
	 *            - Operation result of password validator.
	 * @return true if password meet all criteria , false if any criteria is not
	 *         met
	 */

	public static boolean validatePassword(String password, PasswordPolicyType pp, OperationResult result) {
		OperationResult op = validatePassword(password, pp);
		result.addSubresult(op);
		return op.isSuccess();
	}

	/**
	 * Check provided password against provided policy
	 * 
	 * @param password
	 *            - password to check
	 * @param pp
	 *            - Password policy used
	 * @return - Operation result of this validation
	 */
	public static OperationResult validatePassword(String password, PasswordPolicyType pp) {
		// check input params
		if (null == pp) {
			throw new IllegalArgumentException("No policy provided: NULL");
		}

		if (null == password) {
			throw new IllegalArgumentException("Password for validaiton is null.");
		}

		OperationResult ret = new OperationResult("Password validation against password policy:"
				+ pp.getName());
		normalize(pp);
		LimitationsType lims = pp.getStringPolicy().getLimitations();

		// Test minimal length
		if (lims.getMinLength() > password.length()) {
			ret.addSubresult(new OperationResult("Check global minimal length",
					OperationResultStatus.FATAL_ERROR, "Required minimal size of password is not met."
							+ lims.getMinLength() + ">" + password.length()));
		} else {
			ret.addSubresult(new OperationResult("Check global minimal length",
					OperationResultStatus.SUCCESS, "PASSED"));
		}

		// Test maximal length
		if (lims.getMaxLength() < password.length()) {
			ret.addSubresult(new OperationResult("Check global maximal length",
					OperationResultStatus.FATAL_ERROR, "Required maximal size of password was exceeded."
							+ lims.getMaxLength() + "<" + password.length()));
		} else {
			ret.addSubresult(new OperationResult("Check global maximal length",
					OperationResultStatus.SUCCESS, "PASSED"));
		}

		// Test uniqueness criteria
		HashSet<String> tmp = new HashSet<String>(StringPolicyUtils.stringTokenizer(password));
		if (lims.getMinUniqueChars() > tmp.size()) {
			ret.addSubresult(new OperationResult("Check minimal unique chars",
					OperationResultStatus.FATAL_ERROR,
					"Required minimal count of unique characters of password are not met."
							+ lims.getMinUniqueChars() + ">" + tmp.size()));
		} else {
			ret.addSubresult(new OperationResult("Check minimal unique chars", OperationResultStatus.SUCCESS,
					"PASSED"));
		}

		// check limitation
		HashSet<String> allValidChars = new HashSet<String>(128);
		ArrayList<String> validChars = null;
		ArrayList<String> passwd = StringPolicyUtils.stringTokenizer(password);
		for (StringLimitType l : lims.getLimit()) {
			OperationResult limitResult = new OperationResult("Tested limitation: " + l.getDescription());
			if (null != l.getCharacterClass().getValue()) {
				validChars = StringPolicyUtils.stringTokenizer(l.getCharacterClass().getValue());
			} else {
				validChars = StringPolicyUtils.stringTokenizer(StringPolicyUtils.collectCharacterClass(pp
						.getStringPolicy().getCharacterClass(), l.getCharacterClass().getRef()));
			}
			// memorize validChars
			allValidChars.addAll(validChars);

			// Count how many character for this limitiation are there
			int count = 0;
			for (String s : passwd) {
				if (validChars.contains(s)) {
					count++;
				}
			}

			// Test minimal occurrence
			if (l.getMinOccurs() > count) {
				limitResult.addSubresult(new OperationResult("Check minimal occurence",
						OperationResultStatus.FATAL_ERROR,
						"Required minimal occurence of characters in password is not met." + l.getMinOccurs()
								+ ">" + count));
			} else {
				limitResult.addSubresult(new OperationResult("Check minimal occurence",
						OperationResultStatus.SUCCESS, "PASSED"));
			}

			// Test maximal occurrence
			if (l.getMaxOccurs() < count) {
				limitResult.addSubresult(new OperationResult("Check maximal occurence",
						OperationResultStatus.FATAL_ERROR,
						"Required maximal occurence of characters in password was exceeded."
								+ l.getMaxOccurs() + "<" + count));
			} else {
				limitResult.addSubresult(new OperationResult("Check maximal occurence",
						OperationResultStatus.SUCCESS, "PASSED"));
			}
			// test if first character is valid
			if (l.isMustBeFirst() && !validChars.contains(password.substring(0, 1))) {
				limitResult.addSubresult(new OperationResult("Check valid first char",
						OperationResultStatus.FATAL_ERROR, "First character is not from allowed set "
								+ validChars.toString()));
			} else {
				limitResult.addSubresult(new OperationResult("Check valid first char",
						OperationResultStatus.SUCCESS, "PASSED"));
			}
			limitResult.computeStatus();
			ret.addSubresult(limitResult);
		}

		// Check if there is no invalid character
		StringBuilder sb = new StringBuilder();
		for (String s : passwd) {
			if (!allValidChars.contains(s)) {
				// memorize all invalid characters
				sb.append(s);
			}
		}
		if (sb.length() > 0) {
			ret.addSubresult(new OperationResult("Check if not contain invalid characters",
					OperationResultStatus.FATAL_ERROR, "Not allowed characters [ " + sb
							+ " ] are in password"));
		} else {
			ret.addSubresult(new OperationResult("Check if not contain invalid characters",
					OperationResultStatus.SUCCESS, "PASSED"));
		}

		ret.computeStatus();
		return ret;
	}

}
