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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CharacterClassType;
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

	public static boolean validatePassword(String password, List<String> historyEntries, List<ValuePolicyType> policies,
			OperationResult result) {
		boolean ret = true;
		// iterate through policies
		for (ValuePolicyType pp : policies) {
			OperationResult op = validatePassword(password, historyEntries, pp);
			result.addSubresult(op);
			// if one fail then result is failure
			if (ret == true && !op.isSuccess()) {
				ret = false;
			}
		}
		return ret;
	}

	public static boolean validatePassword(String password, List<String> historyEntries, List<PrismObject<ValuePolicyType>> policies) {
		boolean ret = true;
		// iterate through policies
		for (PrismObject<ValuePolicyType> pp : policies) {
			OperationResult op = validatePassword(password, historyEntries, pp.asObjectable());
			// result.addSubresult(op);
			// if one fail then result is failure
			if (ret == true && !op.isSuccess()) {
				ret = false;
			}
		}
		return ret;
	}

	public static boolean validatePassword(ProtectedStringType password, List<String> historyEntries,
			List<PrismObject<ValuePolicyType>> policies) {
		boolean ret = true;
		// iterate through policies
		for (PrismObject<ValuePolicyType> pp : policies) {
			OperationResult op = validatePassword(password.getClearValue(), historyEntries, pp.asObjectable());
			// result.addSubresult(op);
			// if one fail then result is failure
			if (ret == true && !op.isSuccess()) {
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

	public static boolean validatePassword(String password, List<String> historyEntries, ValuePolicyType pp, OperationResult result) {
		OperationResult op = validatePassword(password, historyEntries, pp);
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
	public static OperationResult validatePassword(String password, List<String> historyEntries, ValuePolicyType pp) {

		Validate.notNull(pp, "Password policy must not be null.");

		OperationResult ret = new OperationResult(OPERATION_PASSWORD_VALIDATION);
		ret.addParam("policyName", pp.getName());
		normalize(pp);

		if (password == null && pp.getMinOccurs() != null
				&& XsdTypeMapper.multiplicityToInteger(pp.getMinOccurs()) == 0) {
			// No password is allowed
			ret.recordSuccess();
			return ret;
		}

		if (password == null) {
			password = "";
		}

		LimitationsType lims = pp.getStringPolicy().getLimitations();

		StringBuilder message = new StringBuilder();

		testMinimalLength(password, lims, ret, message);
		testMaximalLength(password, lims, ret, message);

		testMinimalUniqueCharacters(password, lims, ret, message);
		testPasswordHistoryEntries(password, historyEntries, ret, message);
		

		if (lims.getLimit() == null || lims.getLimit().isEmpty()) {
			if (message.toString() == null || message.toString().isEmpty()) {
				ret.computeStatus();
			} else {
				ret.computeStatus(message.toString());

			}

			return ret;
		}

		// check limitation
		HashSet<String> validChars = null;
		HashSet<String> allValidChars = new HashSet<>();
		List<String> passwd = StringPolicyUtils.stringTokenizer(password);
		for (StringLimitType stringLimitationType : lims.getLimit()) {
			OperationResult limitResult = new OperationResult(
					"Tested limitation: " + stringLimitationType.getDescription());

			validChars = getValidCharacters(stringLimitationType.getCharacterClass(), pp);
			int count = countValidCharacters(validChars, passwd);
			allValidChars.addAll(validChars);
			testMinimalOccurence(stringLimitationType, count, limitResult, message);
			testMaximalOccurence(stringLimitationType, count, limitResult, message);
			testMustBeFirst(stringLimitationType, count, limitResult, message, password, validChars);

			limitResult.computeStatus();
			ret.addSubresult(limitResult);
		}
		testInvalidCharacters(passwd, allValidChars, ret, message);

		if (message.toString() == null || message.toString().isEmpty()) {
			ret.computeStatus();
		} else {
			ret.computeStatus(message.toString());

		}

		return ret;
	}

	private static void testPasswordHistoryEntries(String password, List<String> historyEntries,
			OperationResult result, StringBuilder message) {

		if (historyEntries == null || historyEntries.isEmpty()) {
			return;
		}
		
		if (historyEntries.contains(password)) {
			String msg = "Password couldn't be changed to the same value. Please select another password.";
			result.addSubresult(new OperationResult("Check if password does not contain invalid characters",
					OperationResultStatus.FATAL_ERROR, msg));
			message.append(msg);
			message.append("\n");
		}
		
	}

	private static void testInvalidCharacters(List<String> password, HashSet<String> validChars,
			OperationResult result, StringBuilder message) {

		// Check if there is no invalid character
		StringBuilder invalidCharacters = new StringBuilder();
		for (String s : password) {
			if (!validChars.contains(s)) {
				// memorize all invalid characters
				invalidCharacters.append(s);
			}
		}
		if (invalidCharacters.length() > 0) {
			String msg = "Characters [ " + invalidCharacters + " ] are not allowed in password";
			result.addSubresult(new OperationResult("Check if password does not contain invalid characters",
					OperationResultStatus.FATAL_ERROR, msg));
			message.append(msg);
			message.append("\n");
		}
		// else {
		// ret.addSubresult(new OperationResult("Check if password does not
		// contain invalid characters OK.",
		// OperationResultStatus.SUCCESS, "PASSED"));
		// }

	}

	private static void testMustBeFirst(StringLimitType stringLimitationType, int count,
			OperationResult limitResult, StringBuilder message, String password, Set<String> validChars) {
		// test if first character is valid
		if (stringLimitationType.isMustBeFirst() == null) {
			stringLimitationType.setMustBeFirst(false);
		}
		// we check mustBeFirst only for non-empty passwords
		if (StringUtils.isNotEmpty(password) && stringLimitationType.isMustBeFirst()
				&& !validChars.contains(password.substring(0, 1))) {
			String msg = "First character is not from allowed set. Allowed set: " + validChars.toString();
			limitResult.addSubresult(
					new OperationResult("Check valid first char", OperationResultStatus.FATAL_ERROR, msg));
			message.append(msg);
			message.append("\n");
		}
		// else {
		// limitResult.addSubresult(new OperationResult("Check valid first char
		// in password OK.",
		// OperationResultStatus.SUCCESS, "PASSED"));
		// }

	}

	private static void testMaximalOccurence(StringLimitType stringLimitationType, int count,
			OperationResult limitResult, StringBuilder message) {
		// Test maximal occurrence
		if (stringLimitationType.getMaxOccurs() != null) {

			if (stringLimitationType.getMaxOccurs() < count) {
				String msg = "Required maximal occurrence (" + stringLimitationType.getMaxOccurs()
						+ ") of characters (" + stringLimitationType.getDescription()
						+ ") in password was exceeded (occurrence of characters in password " + count + ").";
				limitResult.addSubresult(new OperationResult("Check maximal occurrence of characters",
						OperationResultStatus.FATAL_ERROR, msg));
				message.append(msg);
				message.append("\n");
			}
			// else {
			// limitResult.addSubresult(new OperationResult(
			// "Check maximal occurrence of characters in password OK.",
			// OperationResultStatus.SUCCESS,
			// "PASSED"));
			// }
		}

	}

	private static void testMinimalOccurence(StringLimitType stringLimitation, int count,
			OperationResult result, StringBuilder message) {
		// Test minimal occurrence
		if (stringLimitation.getMinOccurs() == null) {
			stringLimitation.setMinOccurs(0);
		}
		if (stringLimitation.getMinOccurs() > count) {
			String msg = "Required minimal occurrence (" + stringLimitation.getMinOccurs()
					+ ") of characters (" + stringLimitation.getDescription()
					+ ") in password is not met (occurrence of characters in password " + count + ").";
			result.addSubresult(new OperationResult("Check minimal occurrence of characters",
					OperationResultStatus.FATAL_ERROR, msg));
			message.append(msg);
			message.append("\n");
		}
	}

	private static int countValidCharacters(Set<String> validChars, List<String> password) {
		int count = 0;
		for (String s : password) {
			if (validChars.contains(s)) {
				count++;
			}
		}
		return count;
	}

	private static HashSet<String> getValidCharacters(CharacterClassType characterClassType,
			ValuePolicyType passwordPolicy) {
		if (null != characterClassType.getValue()) {
			return new HashSet<String>(StringPolicyUtils.stringTokenizer(characterClassType.getValue()));
		} else {
			return new HashSet<String>(StringPolicyUtils.stringTokenizer(StringPolicyUtils
					.collectCharacterClass(passwordPolicy.getStringPolicy().getCharacterClass(),
							characterClassType.getRef())));
		}
	}

	private static void testMinimalUniqueCharacters(String password, LimitationsType limitations,
			OperationResult result, StringBuilder message) {
		// Test uniqueness criteria
		HashSet<String> tmp = new HashSet<String>(StringPolicyUtils.stringTokenizer(password));
		if (limitations.getMinUniqueChars() != null) {
			if (limitations.getMinUniqueChars() > tmp.size()) {
				String msg = "Required minimal count of unique characters (" + limitations.getMinUniqueChars()
						+ ") in password are not met (unique characters in password " + tmp.size() + ")";
				result.addSubresult(new OperationResult("Check minimal count of unique chars",
						OperationResultStatus.FATAL_ERROR, msg));
				message.append(msg);
				message.append("\n");
			}

		}
	}

	private static void testMinimalLength(String password, LimitationsType limitations,
			OperationResult result, StringBuilder message) {
		// Test minimal length
		if (limitations.getMinLength() == null) {
			limitations.setMinLength(0);
		}
		if (limitations.getMinLength() > password.length()) {
			String msg = "Required minimal size (" + limitations.getMinLength()
					+ ") of password is not met (password length: " + password.length() + ")";
			result.addSubresult(new OperationResult("Check global minimal length",
					OperationResultStatus.FATAL_ERROR, msg));
			message.append(msg);
			message.append("\n");
		}
	}

	private static void testMaximalLength(String password, LimitationsType limitations,
			OperationResult result, StringBuilder message) {
		// Test maximal length
		if (limitations.getMaxLength() != null) {
			if (limitations.getMaxLength() < password.length()) {
				String msg = "Required maximal size (" + limitations.getMaxLength()
						+ ") of password was exceeded (password length: " + password.length() + ").";
				result.addSubresult(new OperationResult("Check global maximal length",
						OperationResultStatus.FATAL_ERROR, msg));
				message.append(msg);
				message.append("\n");
			}
		}
	}

	private static void buildMessageAndResult(StringBuilder messageBuilder, String message,
			String operationSummary, OperationResult result) {
		messageBuilder.append(message);
		messageBuilder.append("\n");
		result.addSubresult(new OperationResult("Check global maximal length",
				OperationResultStatus.FATAL_ERROR, message));
	}

}
