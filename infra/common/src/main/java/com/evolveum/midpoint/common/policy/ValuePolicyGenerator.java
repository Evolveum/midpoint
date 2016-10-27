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

/**
 * 
 *  @author mamut
 *  
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.text.StrBuilder;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StringLimitType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StringPolicyType;

public class ValuePolicyGenerator {

	private static final transient Trace LOGGER = TraceManager.getTrace(ValuePolicyGenerator.class);

	private static final Random rand = new Random(System.currentTimeMillis());

	public static String generate(StringPolicyType policy, int defaultLength, OperationResult inputResult) {
		return generate(policy, defaultLength, false, inputResult);
	}

	public static String generate(StringPolicyType policy, int defaultLength, boolean generateMinimalSize,
			OperationResult inputResult) {

		if (null == policy) {
			throw new IllegalArgumentException("Provided password policy can not be null.");
		}

		if (null == inputResult) {
			throw new IllegalArgumentException("Provided operation result cannot be null");
		}
		// Define result from generator
		OperationResult generatorResult = new OperationResult("Password generator running policy :"
				+ policy.getDescription());
		inputResult.addSubresult(generatorResult);

		// if (policy.getLimitations() != null &&
		// policy.getLimitations().getMinLength() != null){
		// generateMinimalSize = true;
		// }
		// setup default values where missing
		// PasswordPolicyUtils.normalize(pp);

		// Optimize usage of limits ass hashmap of limitas and key is set of
		// valid chars for each limitation
		Map<StringLimitType, List<String>> lims = new HashMap<StringLimitType, List<String>>();
		for (StringLimitType l : policy.getLimitations().getLimit()) {
			if (null != l.getCharacterClass().getValue()) {
				lims.put(l, StringPolicyUtils.stringTokenizer(l.getCharacterClass().getValue()));
			} else {
				lims.put(
						l,
						StringPolicyUtils.stringTokenizer(StringPolicyUtils.collectCharacterClass(
								policy.getCharacterClass(), l.getCharacterClass().getRef())));
			}
		}

		// Get global limitations
		int minLen = policy.getLimitations().getMinLength() == null ? 0 : policy.getLimitations().getMinLength()
				.intValue();
		if (minLen != 0 && minLen > defaultLength) {
			defaultLength = minLen;
		}
		int maxLen = (policy.getLimitations().getMaxLength() == null ? 0 : policy.getLimitations()
				.getMaxLength().intValue());
		int unique = policy.getLimitations().getMinUniqueChars() == null ? minLen : policy.getLimitations()
				.getMinUniqueChars().intValue();

		// test correctness of definition
		if (unique > minLen) {
			minLen = unique;
			OperationResult reportBug = new OperationResult("Global limitation check");
			reportBug
					.recordWarning("There is more required uniq characters then definied minimum. Raise minimum to number of required uniq chars.");
		}

		if (minLen == 0 && maxLen == 0) {
			minLen = defaultLength;
			maxLen = defaultLength;
			generateMinimalSize = true;
		}
		
		if (maxLen == 0){
			if (minLen > defaultLength){
				maxLen = minLen;
			} else { 
				maxLen = defaultLength;
			}
		}

		// Initialize generator
		StringBuilder password = new StringBuilder();

		/* **********************************
		 * Try to find best characters to be first in password
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
					generatorResult);
			int intersectionCardinality = mustBeFirst.keySet().size();
			List<String> intersectionCharacters = posibleFirstChars.get(intersectionCardinality);
			// If no intersection was found then raise error
			if (null == intersectionCharacters || intersectionCharacters.size() == 0) {
				generatorResult
						.recordFatalError("No intersection for required first character sets in password policy:"
								+ policy.getDescription());
				// Log error
				if (LOGGER.isErrorEnabled()) {
					LOGGER.error("Unable to generate password: No intersection for required first character sets in password policy: ["
							+ policy.getDescription() + "] following character limitation and sets are used:");
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
				password.append(intersectionCharacters.get(rand.nextInt(intersectionCharacters.size())));
			}
		}

		/* **************************************
		 * Generate rest to fulfill minimal criteria
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
					uniquenessReached, generatorResult);
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
					password.append(validChars.get(rand.nextInt(validChars.size())));
					break;
				}
			}
		}

		// test if maximum is not exceeded
		if (password.length() > maxLen) {
			generatorResult
					.recordFatalError("Unable to meet minimal criteria and not exceed maximxal size of password.");
			return null;
		}

		/* ***************************************
		 * Generate chars to not exceed maximal
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
					uniquenessReached, generatorResult);

			// If something goes badly then go out
			if (null == chars) {
				// we hope this never happend.
				generatorResult.recordFatalError("No valid characters to generate, but no all limitation are reached");
				return null;
			}

			// if selection is empty then no more characters and we can close
			// our work
			if (chars.isEmpty()) {
				if (i == 0) {
					password.append(RandomStringUtils.randomAlphanumeric(minLen));
					
				}
				break;
//				if (!StringUtils.isBlank(password.toString()) && password.length() >= minLen) {
//					break;
//				}
				// check uf this is a firs cycle and if we need to user some
				// default (alphanum) character class.
				
			}

			// Find lowest possible cardinality and then generate char
			for (int card = 1; card <= lims.keySet().size(); card++) {
				if (chars.containsKey(card)) {
					List<String> validChars = chars.get(card);
					password.append(validChars.get(rand.nextInt(validChars.size())));
					break;
				}
			}
		}

		if (password.length() < minLen) {
			generatorResult.recordFatalError("Unable to generate password and meet minimal size of password. Password lenght: " 
					+ password.length() + ", required: " + minLen);
			LOGGER.trace("Unable to generate password and meet minimal size of password. Password lenght: {}, required: {}",
					password.length(), minLen);
			return null; 
		}

		generatorResult.recordSuccess();

		// Shuffle output to solve pattern like output
		StrBuilder sb = new StrBuilder(password.substring(0, 1));
		List<String> shuffleBuffer = StringPolicyUtils.stringTokenizer(password.substring(1));
		Collections.shuffle(shuffleBuffer);
		sb.appendAll(shuffleBuffer);

		return sb.toString();
	}

	/******************************************************
	 * Private helper methods
	 ******************************************************/

	/**
	 * Count cardinality
	 */
	private static Map<Integer, List<String>> cardinalityCounter(
			Map<StringLimitType, List<String>> lims, List<String> password, Boolean skipMatchedLims,
			boolean uniquenessReached, OperationResult op) {
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
				o.recordFatalError("Exceeded maximal value for this limitation. " + i + ">" + l.getMaxOccurs());
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
//					if (null == counter.get(s)) {
						counter.put(s, counterKey);
//					} else {
//						counter.put(s, counter.get(s) + 1);
//					}
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
					o.recordFatalError("Exceeded maximal value for this limitation. " + i + ">" + l.getMaxOccurs());
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

	private static int charIntersectionCounter(List<String> a, List<String> b) {
		int ret = 0;
		for (String s : b) {
			if (a.contains(s)) {
				ret++;
			}
		}
		return ret;
	}
}
