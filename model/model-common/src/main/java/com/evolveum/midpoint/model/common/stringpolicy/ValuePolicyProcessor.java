/*
 * Copyright (c) 2010-2017 Evolveum
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

/**
 * Processor for values that match value policies (mostly passwords).
 * This class is supposed to process the parts of the value policy
 * as defined in the ValuePolicyType. So it will validate the values
 * and generate the values. It is NOT supposed to process
 * more complex credential policies such as password lifetime
 * and history.
 * 
 *  @author mamut
 *  @author semancik 
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.text.StrBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CharacterClassType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CheckExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LimitationsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordLifeTimeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StringLimitType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StringPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;

@Component
public class ValuePolicyProcessor {

	private static final String OP_GENERATE = ValuePolicyProcessor.class.getName() + ".generate";
	private static final transient Trace LOGGER = TraceManager.getTrace(ValuePolicyProcessor.class);

	private static final Random RAND = new Random(System.currentTimeMillis());

	private static final String DOT_CLASS = ValuePolicyProcessor.class.getName() + ".";
	private static final String OPERATION_STRING_POLICY_VALIDATION = DOT_CLASS + "stringPolicyValidation";
	private static final int DEFAULT_MAX_ATTEMPTS = 10;
	
	private ItemPath path;
	
	@Autowired
	private ExpressionFactory expressionFactory;

	public ExpressionFactory getExpressionFactory() {
		return expressionFactory;
	}

	public void setExpressionFactory(ExpressionFactory expressionFactory) {
		this.expressionFactory = expressionFactory;
	}
	
	public void setPath(ItemPath path) {
		this.path = path;
	}
	
	public ItemPath getPath() {
		if (path == null) {
			return SchemaConstants.PATH_PASSWORD_VALUE;
		}
		return path;
	}

	public <O extends ObjectType> String generate(ItemPath path, StringPolicyType policy, int defaultLength, PrismObject<O> object, String shortDesc, Task task, OperationResult result) throws ExpressionEvaluationException, SchemaException, ObjectNotFoundException {
		return generate(path, policy, defaultLength, false, object, shortDesc, task, result);
	}

	public <O extends ObjectType>  String generate(ItemPath path, StringPolicyType policy, int defaultLength, boolean generateMinimalSize,
			PrismObject<O> object, String shortDesc, Task task, OperationResult parentResult) throws ExpressionEvaluationException, SchemaException, ObjectNotFoundException {
		setPath(path);
		OperationResult result = parentResult.createSubresult(OP_GENERATE);
		
		int maxAttempts = DEFAULT_MAX_ATTEMPTS;
		if (policy.getLimitations() != null && policy.getLimitations().getMaxAttempts() != null) {
			maxAttempts = policy.getLimitations().getMaxAttempts(); 
		}
		if (maxAttempts < 1) {
			ExpressionEvaluationException e = new ExpressionEvaluationException("Illegal number of maximum value genaration attemps: "+maxAttempts);
			result.recordFatalError(e);
			throw e;
		}
		String generatedValue = null;
		int attempt = 1;
		for (;;) {
			generatedValue = generateAttempt(policy, defaultLength, generateMinimalSize, result);
			if (result.isError()) {
				throw new ExpressionEvaluationException(result.getMessage());
			}
			if (checkAttempt(generatedValue, policy, object, shortDesc, task, result)) {
				break;
			}
			LOGGER.trace("Generator attempt {}: check failed", attempt);
			if (attempt == maxAttempts) {
				ExpressionEvaluationException e =  new ExpressionEvaluationException("Unable to genarate value, maximum number of attemps exceeded");
				result.recordFatalError(e);
				throw e;
			}
			attempt++;
		}
		
		return generatedValue;
		
	}
	
	public <O extends ObjectType> boolean validateValue(String newValue, ValuePolicyType pp, 
			PrismObject<O> object, String shortDesc, Task task, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		return validateValue(newValue, pp, object, new StringBuilder(), shortDesc, task, parentResult);
	}
	
	public <O extends ObjectType> boolean validateValue(String newValue, ValuePolicyType pp, 
			PrismObject<O> object, StringBuilder message, String shortDesc, Task task, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {

		Validate.notNull(pp, "Value policy must not be null.");

		OperationResult result = parentResult.createSubresult(OPERATION_STRING_POLICY_VALIDATION);
		result.addParam("policyName", pp.getName());
		normalize(pp);

		if (newValue == null && 
				(pp.getMinOccurs() == null || XsdTypeMapper.multiplicityToInteger(pp.getMinOccurs()) == 0)) {
			// No password is allowed
			result.recordSuccess();
			return true;
		}

		if (newValue == null) {
			newValue = "";
		}

		LimitationsType lims = pp.getStringPolicy().getLimitations();

		testMinimalLength(newValue, lims, result, message);
		testMaximalLength(newValue, lims, result, message);

		testMinimalUniqueCharacters(newValue, lims, result, message);
		
		if (lims.getLimit() == null || lims.getLimit().isEmpty()) {
			if (message.toString() == null || message.toString().isEmpty()) {
				result.computeStatus();
			} else {
				result.computeStatus(message.toString());

			}

			return result.isAcceptable();
		}

		// check limitation
		HashSet<String> validChars = null;
		HashSet<String> allValidChars = new HashSet<>();
		List<String> passwd = StringPolicyUtils.stringTokenizer(newValue);
		for (StringLimitType stringLimitationType : lims.getLimit()) {
			OperationResult limitResult = new OperationResult(
					"Tested limitation: " + stringLimitationType.getDescription());

			validChars = getValidCharacters(stringLimitationType.getCharacterClass(), pp);
			int count = countValidCharacters(validChars, passwd);
			allValidChars.addAll(validChars);
			testMinimalOccurence(stringLimitationType, count, limitResult, message);
			testMaximalOccurence(stringLimitationType, count, limitResult, message);
			testMustBeFirst(stringLimitationType, count, limitResult, message, newValue, validChars);

			limitResult.computeStatus();
			result.addSubresult(limitResult);
		}
		testInvalidCharacters(passwd, allValidChars, result, message);
		
		testCheckExpression(newValue, lims, object, shortDesc, task, result, message);

		if (message.toString() == null || message.toString().isEmpty()) {
			result.computeStatus();
		} else {
			result.computeStatus(message.toString());

		}

		return result.isAcceptable();
	}
	
	/**
	 * add defined default values
	 */
	private void normalize(ValuePolicyType pp) {
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
	
	private void testMustBeFirst(StringLimitType stringLimitationType, int count,
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

	private void testMaximalOccurence(StringLimitType stringLimitationType, int count,
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

	private void testMinimalOccurence(StringLimitType stringLimitation, int count,
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

	private int countValidCharacters(Set<String> validChars, List<String> password) {
		int count = 0;
		for (String s : password) {
			if (validChars.contains(s)) {
				count++;
			}
		}
		return count;
	}

	private HashSet<String> getValidCharacters(CharacterClassType characterClassType,
			ValuePolicyType passwordPolicy) {
		if (null != characterClassType.getValue()) {
			return new HashSet<String>(StringPolicyUtils.stringTokenizer(characterClassType.getValue()));
		} else {
			return new HashSet<String>(StringPolicyUtils.stringTokenizer(StringPolicyUtils
					.collectCharacterClass(passwordPolicy.getStringPolicy().getCharacterClass(),
							characterClassType.getRef())));
		}
	}

	private void testMinimalUniqueCharacters(String password, LimitationsType limitations,
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

	private void testMinimalLength(String password, LimitationsType limitations,
			OperationResult result, StringBuilder message) {
		// Test minimal length
		if (limitations.getMinLength() == null) {
			limitations.setMinLength(0);
		}
		if (limitations.getMinLength() > password.length()) {
			String msg = "Required minimal size (" + limitations.getMinLength()
					+ ") is not met (actual length: " + password.length() + ")";
			result.addSubresult(new OperationResult("Check global minimal length",
					OperationResultStatus.FATAL_ERROR, msg));
			message.append(msg);
			message.append("\n");
		}
	}

	private void testMaximalLength(String password, LimitationsType limitations,
			OperationResult result, StringBuilder message) {
		// Test maximal length
		if (limitations.getMaxLength() != null) {
			if (limitations.getMaxLength() < password.length()) {
				String msg = "Required maximal size (" + limitations.getMaxLength()
						+ ") was exceeded (actual length: " + password.length() + ").";
				result.addSubresult(new OperationResult("Check global maximal length",
						OperationResultStatus.FATAL_ERROR, msg));
				message.append(msg);
				message.append("\n");
			}
		}
	}
	
	private void testInvalidCharacters(List<String> password, HashSet<String> validChars,
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
			String msg = "Characters [ " + invalidCharacters + " ] are not allowed in value";
			result.addSubresult(new OperationResult("Check if value does not contain invalid characters",
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
	
	private <O extends ObjectType> void testCheckExpression(String newPassword, LimitationsType lims, PrismObject<O> object,
			String shortDesc, Task task, OperationResult result, StringBuilder message) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {

		List<CheckExpressionType> checkExpressions = lims.getCheckExpression();
		if (checkExpressions.isEmpty()) {
			return;
		}
		for (CheckExpressionType checkExpression: checkExpressions) {
			ExpressionType expressionType = checkExpression.getExpression();
			if (expressionType == null) {
				return;
			}
			if (!checkExpression(newPassword, expressionType, object, shortDesc, task, result)) {
				String msg = checkExpression.getFailureMessage();
				if (msg == null) {
					msg = "Check expression failed";
				}
				result.addSubresult(new OperationResult("Check expression",
						OperationResultStatus.FATAL_ERROR, msg));
				message.append(msg);
				message.append("\n");
			}
		}

	}
	
	private String generateAttempt(StringPolicyType policy, int defaultLength, boolean generateMinimalSize,
			OperationResult result) {

		// if (policy.getLimitations() != null &&
		// policy.getLimitations().getMinLength() != null){
		// generateMinimalSize = true;
		// }
		// setup default values where missing
		// PasswordPolicyUtils.normalize(pp);

		// Optimize usage of limits ass hashmap of limitas and key is set of
		// valid chars for each limitation
		Map<StringLimitType, List<String>> lims = new HashMap<StringLimitType, List<String>>();
		int minLen = defaultLength;
		int maxLen = defaultLength;
		int unique = defaultLength / 2;
		if (policy != null) {
			for (StringLimitType l : policy.getLimitations().getLimit()) {
				if (null != l.getCharacterClass().getValue()) {
					lims.put(l, StringPolicyUtils.stringTokenizer(l.getCharacterClass().getValue()));
				} else {
					lims.put(l, StringPolicyUtils.stringTokenizer(StringPolicyUtils.collectCharacterClass(
							policy.getCharacterClass(), l.getCharacterClass().getRef())));
				}
			}

			// Get global limitations
			minLen = policy.getLimitations().getMinLength() == null ? 0
					: policy.getLimitations().getMinLength().intValue();
			if (minLen != 0 && minLen > defaultLength) {
				defaultLength = minLen;
			}
			maxLen = (policy.getLimitations().getMaxLength() == null ? 0
					: policy.getLimitations().getMaxLength().intValue());
			unique = policy.getLimitations().getMinUniqueChars() == null ? minLen
					: policy.getLimitations().getMinUniqueChars().intValue();

		} 
		// test correctness of definition
		if (unique > minLen) {
			minLen = unique;
			OperationResult reportBug = new OperationResult("Global limitation check");
			reportBug.recordWarning(
					"There is more required uniq characters then definied minimum. Raise minimum to number of required uniq chars.");
		}

		if (minLen == 0 && maxLen == 0) {
			minLen = defaultLength;
			maxLen = defaultLength;
			generateMinimalSize = true;
		}

		if (maxLen == 0) {
			if (minLen > defaultLength) {
				maxLen = minLen;
			} else {
				maxLen = defaultLength;
			}
		}

		// Initialize generator
		StringBuilder password = new StringBuilder();

		/*
		 * ********************************** Try to find best characters to be
		 * first in password
		 */
		Map<StringLimitType, List<String>> mustBeFirst = new HashMap<StringLimitType, List<String>>();
		for (StringLimitType l : lims.keySet()) {
			if (l.isMustBeFirst() != null && l.isMustBeFirst()) {
				mustBeFirst.put(l, lims.get(l));
			}
		}

		// If any limitation was found to be first
		if (!mustBeFirst.isEmpty()) {
			Map<Integer, List<String>> posibleFirstChars = cardinalityCounter(mustBeFirst, null, false, false,
					result);
			int intersectionCardinality = mustBeFirst.keySet().size();
			List<String> intersectionCharacters = posibleFirstChars.get(intersectionCardinality);
			// If no intersection was found then raise error
			if (null == intersectionCharacters || intersectionCharacters.size() == 0) {
				result.recordFatalError(
						"No intersection for required first character sets in value policy:"
								+ policy.getDescription());
				// Log error
				if (LOGGER.isErrorEnabled()) {
					LOGGER.error(
							"Unable to generate value for " + getPath() + ": No intersection for required first character sets in value policy: ["
									+ policy.getDescription()
									+ "] following character limitation and sets are used:");
					for (StringLimitType l : mustBeFirst.keySet()) {
						StrBuilder tmp = new StrBuilder();
						tmp.appendSeparator(", ");
						tmp.appendAll(mustBeFirst.get(l));
						LOGGER.error("L:" + l.getDescription() + " -> [" + tmp + "]");
					}
				}
				// No more processing unrecoverable conflict
				return null; // EXIT
			} else {
				if (LOGGER.isDebugEnabled()) {
					StrBuilder tmp = new StrBuilder();
					tmp.appendSeparator(", ");
					tmp.appendAll(intersectionCharacters);
					LOGGER.trace("Generate first character intersection items [" + tmp + "] into " + getPath() + ".");
				}
				// Generate random char into password from intersection
				password.append(intersectionCharacters.get(RAND.nextInt(intersectionCharacters.size())));
			}
		}

		/*
		 * ************************************** Generate rest to fulfill
		 * minimal criteria
		 */

		boolean uniquenessReached = false;

		// Count cardinality of elements
		Map<Integer, List<String>> chars;
		for (int i = 0; i < minLen; i++) {

			// Check if still unique chars are needed
			if (password.length() >= unique) {
				uniquenessReached = true;
			}
			// Find all usable characters
			chars = cardinalityCounter(lims, StringPolicyUtils.stringTokenizer(password.toString()), false,
					uniquenessReached, result);
			// If something goes badly then go out
			if (null == chars) {
				return null;
			}

			if (chars.isEmpty()) {
				LOGGER.trace("Minimal criterias was met. No more characters");
				break;
			}
			// Find lowest possible cardinality and then generate char
			for (int card = 1; card < lims.keySet().size(); card++) {
				if (chars.containsKey(card)) {
					List<String> validChars = chars.get(card);
					password.append(validChars.get(RAND.nextInt(validChars.size())));
					break;
				}
			}
		}

		// test if maximum is not exceeded
		if (password.length() > maxLen) {
			result.recordFatalError(
					"Unable to meet minimal criteria and not exceed maximxal size of " + getPath() + ".");
			return null;
		}

		/*
		 * *************************************** Generate chars to not exceed
		 * maximal
		 */

		for (int i = 0; i < minLen; i++) {
			// test if max is reached
			if (password.length() == maxLen) {
				// no more characters maximal size is reached
				break;
			}

			if (password.length() >= minLen && generateMinimalSize) {
				// no more characters are needed
				break;
			}

			// Check if still unique chars are needed
			if (password.length() >= unique) {
				uniquenessReached = true;
			}
			// find all usable characters
			chars = cardinalityCounter(lims, StringPolicyUtils.stringTokenizer(password.toString()), true,
					uniquenessReached, result);

			// If something goes badly then go out
			if (null == chars) {
				// we hope this never happend.
				result.recordFatalError(
						"No valid characters to generate, but no all limitation are reached");
				return null;
			}

			// if selection is empty then no more characters and we can close
			// our work
			if (chars.isEmpty()) {
				if (i == 0) {
					password.append(RandomStringUtils.randomAlphanumeric(minLen));

				}
				break;
				// if (!StringUtils.isBlank(password.toString()) &&
				// password.length() >= minLen) {
				// break;
				// }
				// check uf this is a firs cycle and if we need to user some
				// default (alphanum) character class.

			}

			// Find lowest possible cardinality and then generate char
			for (int card = 1; card <= lims.keySet().size(); card++) {
				if (chars.containsKey(card)) {
					List<String> validChars = chars.get(card);
					password.append(validChars.get(RAND.nextInt(validChars.size())));
					break;
				}
			}
		}

		if (password.length() < minLen) {
			result.recordFatalError(
					"Unable to generate value for " + getPath() + " and meet minimal size of " + getPath() + ". Actual lenght: "
							+ password.length() + ", required: " + minLen);
			LOGGER.trace(
					"Unable to generate value for " + getPath() + " and meet minimal size of " + getPath() + ". Actual lenght: {}, required: {}",
					password.length(), minLen);
			return null;
		}

		result.recordSuccess();

		// Shuffle output to solve pattern like output
		StrBuilder sb = new StrBuilder(password.substring(0, 1));
		List<String> shuffleBuffer = StringPolicyUtils.stringTokenizer(password.substring(1));
		Collections.shuffle(shuffleBuffer);
		sb.appendAll(shuffleBuffer);

		return sb.toString();
	}

	private <O extends ObjectType> boolean checkAttempt(String generatedValue, StringPolicyType policy, PrismObject<O> object, String shortDesc, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		LimitationsType limitationsType = policy.getLimitations();
		if (limitationsType == null) {
			return true;
		}
		List<CheckExpressionType> checkExpressionTypes = limitationsType.getCheckExpression();
		if (!checkExpressions(generatedValue, checkExpressionTypes, object, shortDesc, task, result)) {
			LOGGER.trace("Check expression returned false for generated value in {}", shortDesc);
			return false;
		}
		// TODO Check pattern
		return true;
	}
	
	private <O extends ObjectType> boolean checkExpressions(String generatedValue, List<CheckExpressionType> checkExpressionTypes, PrismObject<O> object, String shortDesc, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		for (CheckExpressionType checkExpressionType: checkExpressionTypes) {
			ExpressionType expression = checkExpressionType.getExpression();
			if (!checkExpression(generatedValue, expression, object, shortDesc, task, result)) {
				return false;
			}
		}
		return true;
	}

	public <O extends ObjectType> boolean checkExpression(String generatedValue, ExpressionType checkExpression, PrismObject<O> object, String shortDesc, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		ExpressionVariables variables = new ExpressionVariables();
		variables.addVariableDefinition(ExpressionConstants.VAR_INPUT, generatedValue);
		variables.addVariableDefinition(ExpressionConstants.VAR_OBJECT, object);
		PrismPropertyValue<Boolean> output = ExpressionUtil.evaluateCondition(variables, checkExpression, expressionFactory, shortDesc, task, result);
		return ExpressionUtil.getBooleanConditionOutput(output);
	}
	
	/**
	 * Count cardinality
	 */
	private Map<Integer, List<String>> cardinalityCounter(Map<StringLimitType, List<String>> lims,
			List<String> password, Boolean skipMatchedLims, boolean uniquenessReached, OperationResult op) {
		HashMap<String, Integer> counter = new HashMap<String, Integer>();

		for (StringLimitType l : lims.keySet()) {
			int counterKey = 1;
			List<String> chars = lims.get(l);
			int i = 0;
			if (null != password) {
				i = charIntersectionCounter(lims.get(l), password);
			}
			// If max is exceed then error unable to continue
			if (l.getMaxOccurs() != null && i > l.getMaxOccurs()) {
				OperationResult o = new OperationResult("Limitation check :" + l.getDescription());
				o.recordFatalError(
						"Exceeded maximal value for this limitation. " + i + ">" + l.getMaxOccurs());
				op.addSubresult(o);
				return null;
				// if max is all ready reached or skip enabled for minimal skip
				// counting
			} else if (l.getMaxOccurs() != null && i == l.getMaxOccurs()) {
				continue;
				// other cases minimum is not reached
			} else if ((l.getMinOccurs() == null || i >= l.getMinOccurs()) && !skipMatchedLims) {
				continue;
			}
			for (String s : chars) {
				if (null == password || !password.contains(s) || uniquenessReached) {
					// if (null == counter.get(s)) {
					counter.put(s, counterKey);
					// } else {
					// counter.put(s, counter.get(s) + 1);
					// }
				}
			}
			counterKey++;

		}

		// If need to remove disabled chars (already reached limitations)
		if (null != password) {
			for (StringLimitType l : lims.keySet()) {
				int i = charIntersectionCounter(lims.get(l), password);
				if (l.getMaxOccurs() != null && i > l.getMaxOccurs()) {
					OperationResult o = new OperationResult("Limitation check :" + l.getDescription());
					o.recordFatalError(
							"Exceeded maximal value for this limitation. " + i + ">" + l.getMaxOccurs());
					op.addSubresult(o);
					return null;
				} else if (l.getMaxOccurs() != null && i == l.getMaxOccurs()) {
					// limitation matched remove all used chars
					LOGGER.trace("Skip " + l.getDescription());
					for (String charToRemove : lims.get(l)) {
						counter.remove(charToRemove);
					}
				}
			}
		}

		// Transpone to better format
		Map<Integer, List<String>> ret = new HashMap<Integer, List<String>>();
		for (String s : counter.keySet()) {
			// if not there initialize
			if (null == ret.get(counter.get(s))) {
				ret.put(counter.get(s), new ArrayList<String>());
			}
			ret.get(counter.get(s)).add(s);
		}
		return ret;
	}

	private int charIntersectionCounter(List<String> a, List<String> b) {
		int ret = 0;
		for (String s : b) {
			if (a.contains(s)) {
				ret++;
			}
		}
		return ret;
	}
}
