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
package com.evolveum.midpoint.common.policy;

import java.util.ArrayList;

import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.LimitationsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.PasswordLifeTimeType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProtectedStringType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.StringLimitType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.StringPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ValuePolicyType;

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
	public static void normalize(ValuePolicyType pp) {
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

	public static boolean validatePassword(String password, List <ValuePolicyType> policies, OperationResult result) {
		boolean ret=true;
		//iterate through policies 
		for (ValuePolicyType pp: policies) {
			OperationResult op = validatePassword(password, pp);
			result.addSubresult(op);
			//if one fail then result is failure
			if (ret == true && ! op.isSuccess()) {
				ret = false;
			}
		}
		return ret;
	}
	
	public static boolean validatePassword(String password, List<PrismObject<ValuePolicyType>> policies) {
		boolean ret=true;
		//iterate through policies 
		for (PrismObject<ValuePolicyType> pp: policies) {
			OperationResult op = validatePassword(password, pp.asObjectable());
//			result.addSubresult(op);
			//if one fail then result is failure
			if (ret == true && ! op.isSuccess()) {
				ret = false;
			}
		}
		return ret;
	}
	
	public static boolean validatePassword(ProtectedStringType password, List<PrismObject<ValuePolicyType>> policies) {
		boolean ret=true;
		//iterate through policies 
		for (PrismObject<ValuePolicyType> pp: policies) {
			OperationResult op = validatePassword(password.getClearValue(), pp.asObjectable());
//			result.addSubresult(op);
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

	public static boolean validatePassword(String password, ValuePolicyType pp, OperationResult result) {
		OperationResult op = validatePassword(password, pp);
		result.addSubresult(op);
		return op.isSuccess();
	}
	
	public static boolean validatePassword(ProtectedStringType password, ValuePolicyType pp) {
		
		OperationResult op = validatePassword(password.getClearValue(), pp);
//		result.addSubresult(op);
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
	public static OperationResult validatePassword(String password, ValuePolicyType pp) {
		// check input params
//		if (null == pp) {
//			throw new IllegalArgumentException("No policy provided: NULL");
//		}
//
//		if (null == password) {
//			throw new IllegalArgumentException("Password for validaiton is null.");
//		}
		
		Validate.notNull(pp, "Password policy must not be null.");
		Validate.notNull(password, "Password to validate must not be null.");

		OperationResult ret = new OperationResult("Password validation against password policy:"
				+ pp.getName());
		normalize(pp);
		LimitationsType lims = pp.getStringPolicy().getLimitations();

		StringBuilder message = new StringBuilder();
		
		// Test minimal length
		if (lims.getMinLength() == null){
			lims.setMinLength(0);
		}
		if (lims.getMinLength() > password.length()) {
			String msg = "Required minimal size (" + lims.getMinLength() + ") of password is not met (password length: "
							+ password.length() + ")";
			ret.addSubresult(new OperationResult("Check global minimal length", OperationResultStatus.FATAL_ERROR,
					msg));
			message.append(msg);
			message.append("\n");
		} else {
			ret.addSubresult(new OperationResult("Check global minimal length. Minimal length of password OK.",
					OperationResultStatus.SUCCESS, "PASSED"));
		}

		// Test maximal length
		if (lims.getMaxLength() != null) {
			if (lims.getMaxLength() < password.length()) {
				String msg = "Required maximal size (" + lims.getMaxLength()
						+ ") of password was exceeded (password length: " + password.length() + ").";
				ret.addSubresult(new OperationResult("Check global maximal length", OperationResultStatus.FATAL_ERROR,
						msg));
				message.append(msg);
				message.append("\n");
			} else {
				ret.addSubresult(new OperationResult("Check global maximal length. Maximal length of password OK.",
						OperationResultStatus.SUCCESS, "PASSED"));
			}
		}
		// Test uniqueness criteria
		HashSet<String> tmp = new HashSet<String>(StringPolicyUtils.stringTokenizer(password));
		if (lims.getMinUniqueChars() != null) {
			if (lims.getMinUniqueChars() > tmp.size()) {
				String msg = "Required minimal count of unique characters ("
						+ lims.getMinUniqueChars()
						+ ") in password are not met (unique characters in password " + tmp.size() + ")";
				ret.addSubresult(new OperationResult("Check minimal count of unique chars",
						OperationResultStatus.FATAL_ERROR, msg));
				message.append(msg);
				message.append("\n");
			} else {
				ret.addSubresult(new OperationResult(
						"Check minimal count of unique chars. Password satisfies minimal required unique characters.",
						OperationResultStatus.SUCCESS, "PASSED"));
			}
		}

		// check limitation
		HashSet<String> allValidChars = new HashSet<String>(128);
		ArrayList<String> validChars = null;
		ArrayList<String> passwd = StringPolicyUtils.stringTokenizer(password);
		
		if (lims.getLimit() == null || lims.getLimit().isEmpty()){
			ret.computeStatus(message.toString());
			
			return ret;
		}
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
			if (l.getMinOccurs() == null){
				l.setMinOccurs(0);
			}
			if (l.getMinOccurs() > count) {
				String msg = "Required minimal occurence (" + l.getMinOccurs()
						+ ") of characters in password is not met (occurence of characters in password "
						+ count + ").";
				limitResult.addSubresult(new OperationResult("Check minimal occurence of characters",
						OperationResultStatus.FATAL_ERROR, msg));
				message.append(msg);
				message.append("\n");
			} else {
				limitResult.addSubresult(new OperationResult("Check minimal occurence of characters in password OK.",
						OperationResultStatus.SUCCESS, "PASSED"));
			}

			// Test maximal occurrence
			if (l.getMaxOccurs() != null) {

				if (l.getMaxOccurs() < count) {
					String msg = "Required maximal occurence (" + l.getMaxOccurs()
							+ ") of characters in password was exceeded (occurence of characters in password "
							+ count + ").";
					limitResult.addSubresult(new OperationResult("Check maximal occurence of characters",
							OperationResultStatus.FATAL_ERROR, msg));
					message.append(msg);
					message.append("\n");
				} else {
					limitResult.addSubresult(new OperationResult(
							"Check maximal occurence of characters in password OK.", OperationResultStatus.SUCCESS,
							"PASSED"));
				}
			}
			// test if first character is valid
			if (l.isMustBeFirst() == null){
				l.setMustBeFirst(false);
			}
			if (l.isMustBeFirst() && !validChars.contains(password.substring(0, 1))) {
				String msg = "First character is not from allowed set. Allowed set: "
						+ validChars.toString();
				limitResult.addSubresult(new OperationResult("Check valid first char",
						OperationResultStatus.FATAL_ERROR, msg));
				message.append(msg);
				message.append("\n");
			} else {
				limitResult.addSubresult(new OperationResult("Check valid first char in password OK.",
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
			String msg = "Characters [ " + sb
					+ " ] are not allowed to be use in password";
			ret.addSubresult(new OperationResult("Check if password does not contain invalid characters",
					OperationResultStatus.FATAL_ERROR, msg));
			message.append(msg);
			message.append("\n");
		} else {
			ret.addSubresult(new OperationResult("Check if password does not contain invalid characters OK.",
					OperationResultStatus.SUCCESS, "PASSED"));
		}

		ret.computeStatus(message.toString());
		
		return ret;
	}

}
