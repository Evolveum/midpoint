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
 *  Generator for values that match value policies (mostly passwords).
 * Mishmash or old and new code. Only partially refactored.
 * Still need to align with ValuePolicyGenerator and the utils.
 * 
 *  @author mamut
 *  @author semancik 
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.text.StrBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.common.expression.Expression;
import com.evolveum.midpoint.model.common.expression.ExpressionFactory;
import com.evolveum.midpoint.model.common.expression.ExpressionUtil;
import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LimitationsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StringLimitType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StringPolicyType;

@Component
public class ValuePolicyGenerator {

	private static final String OP_GENERATE = ValuePolicyGenerator.class.getName() + ".generate";
	private static final transient Trace LOGGER = TraceManager.getTrace(ValuePolicyGenerator.class);

	private static final Random RAND = new Random(System.currentTimeMillis());

	private static final int DEFAULT_MAX_ATTEMPTS = 10;
	
	@Autowired
	private ExpressionFactory expressionFactory;

	public ExpressionFactory getExpressionFactory() {
		return expressionFactory;
	}

	public void setExpressionFactory(ExpressionFactory expressionFactory) {
		this.expressionFactory = expressionFactory;
	}

	public <O extends ObjectType> String generate(StringPolicyType policy, int defaultLength, PrismObject<O> object, String shortDesc, Task task, OperationResult result) throws ExpressionEvaluationException, SchemaException, ObjectNotFoundException {
		return generate(policy, defaultLength, false, object, shortDesc, task, result);
	}

	public <O extends ObjectType>  String generate(StringPolicyType policy, int defaultLength, boolean generateMinimalSize,
			PrismObject<O> object, String shortDesc, Task task, OperationResult parentResult) throws ExpressionEvaluationException, SchemaException, ObjectNotFoundException {
		
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
						"No intersection for required first character sets in password policy:"
								+ policy.getDescription());
				// Log error
				if (LOGGER.isErrorEnabled()) {
					LOGGER.error(
							"Unable to generate password: No intersection for required first character sets in password policy: ["
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
					LOGGER.trace("Generate first character intersection items [" + tmp + "] into password.");
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
					"Unable to meet minimal criteria and not exceed maximxal size of password.");
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
					"Unable to generate password and meet minimal size of password. Password lenght: "
							+ password.length() + ", required: " + minLen);
			LOGGER.trace(
					"Unable to generate password and meet minimal size of password. Password lenght: {}, required: {}",
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
		ExpressionType checkExpression = limitationsType.getCheckExpression();
		if (checkExpression != null && !checkExpression(generatedValue, checkExpression, object, shortDesc, task, result)) {
			LOGGER.trace("Check expression returned false for generated value in {}", shortDesc);
			return false;
		}
		// TODO Check pattern
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
