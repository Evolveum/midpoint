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
import com.evolveum.midpoint.xml.ns._public.common.common_3.LimitationsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordLifeTimeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StringLimitType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StringPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * 
 * @author mamut
 * 
 */
public class PasswordPolicyUtils {
	private static final transient Trace LOGGER = TraceManager.getTrace(PasswordPolicyUtils.class);

    private static final String DOT_CLASS = PasswordPolicyUtils.class.getName() + ".";
    private static final String OPERATION_PASSWORD_VALIDATION = DOT_CLASS + "passwordValidation";

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

        OperationResult ret = new OperationResult(OPERATION_PASSWORD_VALIDATION);
        ret.addParam("policyName", pp.getName());
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
		} 
//		else {
//			ret.addSubresult(new OperationResult("Check global minimal length. Minimal length of password OK.",
//					OperationResultStatus.SUCCESS, "PASSED"));
//		}

		// Test maximal length
		if (lims.getMaxLength() != null) {
			if (lims.getMaxLength() < password.length()) {
				String msg = "Required maximal size (" + lims.getMaxLength()
						+ ") of password was exceeded (password length: " + password.length() + ").";
				ret.addSubresult(new OperationResult("Check global maximal length", OperationResultStatus.FATAL_ERROR,
						msg));
				message.append(msg);
				message.append("\n");
			} 
//			else {
//				ret.addSubresult(new OperationResult("Check global maximal length. Maximal length of password OK.",
//						OperationResultStatus.SUCCESS, "PASSED"));
//			}
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
			} 
//			else {
//				ret.addSubresult(new OperationResult(
//						"Check minimal count of unique chars. Password satisfies minimal required unique characters.",
//						OperationResultStatus.SUCCESS, "PASSED"));
//			}
		}

		// check limitation
		HashSet<String> allValidChars = new HashSet<String>(128);
		ArrayList<String> validChars = null;
		ArrayList<String> passwd = StringPolicyUtils.stringTokenizer(password);
		
		if (lims.getLimit() == null || lims.getLimit().isEmpty()){
			if (message.toString() == null || message.toString().isEmpty()){
				ret.computeStatus();
			} else {
				ret.computeStatus(message.toString());
				
			}
			
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

			// Count how many character for this limitation are there
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
				String msg = "Required minimal occurrence (" + l.getMinOccurs()
						+ ") of characters ("+l.getDescription()+") in password is not met (occurrence of characters in password "
						+ count + ").";
				limitResult.addSubresult(new OperationResult("Check minimal occurrence of characters",
						OperationResultStatus.FATAL_ERROR, msg));
				message.append(msg);
				message.append("\n");
			} 
//			else {
//				limitResult.addSubresult(new OperationResult("Check minimal occurrence of characters in password OK.",
//						OperationResultStatus.SUCCESS, "PASSED"));
//			}

			// Test maximal occurrence
			if (l.getMaxOccurs() != null) {

				if (l.getMaxOccurs() < count) {
					String msg = "Required maximal occurrence (" + l.getMaxOccurs()
							+ ") of characters ("+l.getDescription()+") in password was exceeded (occurrence of characters in password "
							+ count + ").";
					limitResult.addSubresult(new OperationResult("Check maximal occurrence of characters",
							OperationResultStatus.FATAL_ERROR, msg));
					message.append(msg);
					message.append("\n");
				} 
//				else {
//					limitResult.addSubresult(new OperationResult(
//							"Check maximal occurrence of characters in password OK.", OperationResultStatus.SUCCESS,
//							"PASSED"));
//				}
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
			} 
//			else {
//				limitResult.addSubresult(new OperationResult("Check valid first char in password OK.",
//						OperationResultStatus.SUCCESS, "PASSED"));
//			}
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
		} 
//		else {
//			ret.addSubresult(new OperationResult("Check if password does not contain invalid characters OK.",
//					OperationResultStatus.SUCCESS, "PASSED"));
//		}

		if (message.toString() == null || message.toString().isEmpty()){
			ret.computeStatus();
		} else {
			ret.computeStatus(message.toString());
			
		}
		
		return ret;
	}

}
