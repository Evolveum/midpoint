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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

import com.evolveum.midpoint.schema.util.LocalizationUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.LocalizableMessageList;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.apache.commons.lang.text.StrBuilder;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CharacterClassType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CheckExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LimitationsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordLifeTimeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProhibitedValueItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProhibitedValuesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StringLimitType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StringPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

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

@Component
public class ValuePolicyProcessor {

	private static final String OP_GENERATE = ValuePolicyProcessor.class.getName() + ".generate";
	private static final transient Trace LOGGER = TraceManager.getTrace(ValuePolicyProcessor.class);

	private static final Random RAND = new Random(System.currentTimeMillis());

	private static final String DOT_CLASS = ValuePolicyProcessor.class.getName() + ".";
	private static final String OPERATION_STRING_POLICY_VALIDATION = DOT_CLASS + "stringPolicyValidation";
	private static final int DEFAULT_MAX_ATTEMPTS = 10;
	
	@Autowired private ExpressionFactory expressionFactory;
	@Autowired private Protector protector;

	static class Context {
		final ItemPath path;

		public Context(ItemPath path) {
			this.path = defaultIfNull(path, SchemaConstants.PATH_PASSWORD_VALUE);
		}
	}

	public ExpressionFactory getExpressionFactory() {
		return expressionFactory;
	}

	// Used in tests
	public void setExpressionFactory(ExpressionFactory expressionFactory) {
		this.expressionFactory = expressionFactory;
	}
	
	public <O extends ObjectType> String generate(ItemPath path, @NotNull ValuePolicyType policy, int defaultLength, boolean generateMinimalSize,
			AbstractValuePolicyOriginResolver<O> originResolver, String shortDesc, Task task, OperationResult parentResult) throws ExpressionEvaluationException, SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
		Context ctx = new Context(path);
		OperationResult result = parentResult.createSubresult(OP_GENERATE);
		
		StringPolicyType stringPolicy = policy.getStringPolicy();
		int maxAttempts = DEFAULT_MAX_ATTEMPTS;
		if (stringPolicy.getLimitations() != null && stringPolicy.getLimitations().getMaxAttempts() != null) {
			maxAttempts = stringPolicy.getLimitations().getMaxAttempts(); 
		}
		if (maxAttempts < 1) {
			ExpressionEvaluationException e = new ExpressionEvaluationException("Illegal number of maximum value generation attempts: "+maxAttempts);
			result.recordFatalError(e);
			throw e;
		}
		String generatedValue;
		int attempt = 1;
		for (;;) {
			generatedValue = generateAttempt(policy, defaultLength, generateMinimalSize, ctx, result);
			if (result.isError()) {
				throw new ExpressionEvaluationException(result.getMessage());
			}
			if (checkAttempt(generatedValue, policy, originResolver, shortDesc, task, result)) {
				break;
			}
			LOGGER.trace("Generator attempt {}: check failed", attempt);
			if (attempt == maxAttempts) {
				ExpressionEvaluationException e =  new ExpressionEvaluationException("Unable to generate value, maximum number of attempts exceeded");
				result.recordFatalError(e);
				throw e;
			}
			attempt++;
		}
		return generatedValue;
	}
	
	public <O extends ObjectType> boolean validateValue(String newValue, ValuePolicyType pp, 
			AbstractValuePolicyOriginResolver<O> originResolver, String shortDesc, Task task, OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
			ConfigurationException, SecurityViolationException {
		return validateValue(newValue, pp, originResolver, new ArrayList<>(), shortDesc, task, parentResult);
	}
	
	public <O extends ObjectType> boolean validateValue(String newValue, ValuePolicyType pp, 
			AbstractValuePolicyOriginResolver<O> originResolver, List<LocalizableMessage> messages, String shortDesc, Task task,
			OperationResult parentResult) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
			CommunicationException, ConfigurationException, SecurityViolationException {
		Validate.notNull(pp, "Value policy must not be null.");

		OperationResult result = parentResult.createSubresult(OPERATION_STRING_POLICY_VALIDATION);
		result.addArbitraryObjectAsParam("policyName", pp.getName());
		normalize(pp);

		if (newValue == null &&
				defaultIfNull(XsdTypeMapper.multiplicityToInteger(pp.getMinOccurs()), 0) == 0) {
			// No value is allowed
			result.recordSuccess();
			return true;
		}

		if (newValue == null) {
			newValue = "";
		}

		LimitationsType lims = pp.getStringPolicy().getLimitations();

		testMinimalLength(newValue, lims, result, messages);
		testMaximalLength(newValue, lims, result, messages);

		testMinimalUniqueCharacters(newValue, lims, result, messages);

		testProhibitedValues(newValue, pp.getProhibitedValues(), originResolver, shortDesc, task, result, messages);
		testCheckExpression(newValue, lims, originResolver, shortDesc, task, result, messages);

		if (!lims.getLimit().isEmpty()) {
			// check limitation
			HashSet<String> validChars;
			HashSet<String> allValidChars = new HashSet<>();
			List<String> characters = StringPolicyUtils.stringTokenizer(newValue);
			for (StringLimitType stringLimitationType : lims.getLimit()) {
				OperationResult limitResult = new OperationResult("Tested limitation: " + stringLimitationType.getDescription());

				validChars = getValidCharacters(stringLimitationType.getCharacterClass(), pp);
				int count = countValidCharacters(validChars, characters);
				allValidChars.addAll(validChars);
				testMinimalOccurrence(stringLimitationType, count, limitResult, messages);
				testMaximalOccurrence(stringLimitationType, count, limitResult, messages);
				testMustBeFirst(stringLimitationType, limitResult, messages, newValue, validChars);

				limitResult.computeStatus();
				result.addSubresult(limitResult);
			}
			testInvalidCharacters(characters, allValidChars, result, messages);
		}

		result.computeStatus();
		if (!result.isSuccess() && !messages.isEmpty()) {
			result.setUserFriendlyMessage(new LocalizableMessageList(messages, LocalizableMessageList.SPACE, null, null));
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
	}
	
	private void testMustBeFirst(StringLimitType stringLimitation, OperationResult result, List<LocalizableMessage> messages,
			String value, Set<String> validFirstChars) {
		if (StringUtils.isNotEmpty(value) && isTrue(stringLimitation.isMustBeFirst()) && !validFirstChars.contains(value.substring(0, 1))) {
			LocalizableMessage msg = new LocalizableMessageBuilder()
					.key("ValuePolicy.firstCharacterNotAllowed")
					.arg(validFirstChars.toString())
					.build();
			result.addSubresult(new OperationResult("Check valid first char", OperationResultStatus.FATAL_ERROR, msg));
			messages.add(msg);
		}
	}

	private void testMaximalOccurrence(StringLimitType stringLimitation, int count, OperationResult result, List<LocalizableMessage> messages) {
		if (stringLimitation.getMaxOccurs() == null) {
			return;
		}
		if (count > stringLimitation.getMaxOccurs()) {
			LocalizableMessage msg = new LocalizableMessageBuilder()
					.key("ValuePolicy.maximalOccurrenceExceeded")
					.arg(stringLimitation.getMaxOccurs())
					.arg(stringLimitation.getDescription())
					.arg(count)
					.build();
			result.addSubresult(new OperationResult("Check maximal occurrence of characters", OperationResultStatus.FATAL_ERROR, msg));
			messages.add(msg);
		}
	}

	private void testMinimalOccurrence(StringLimitType stringLimitation, int count, OperationResult result, List<LocalizableMessage> messages) {
		if (stringLimitation.getMinOccurs() == null) {
			return;
		}
		if (count < stringLimitation.getMinOccurs()) {
			LocalizableMessage msg = new LocalizableMessageBuilder()
					.key("ValuePolicy.minimalOccurrenceNotMet")
					.arg(stringLimitation.getMinOccurs())
					.arg(stringLimitation.getDescription())
					.arg(count)
					.build();
			result.addSubresult(new OperationResult("Check minimal occurrence of characters", OperationResultStatus.FATAL_ERROR, msg));
			messages.add(msg);
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
			return new HashSet<>(StringPolicyUtils.stringTokenizer(characterClassType.getValue()));
		} else {
			return new HashSet<>(StringPolicyUtils.stringTokenizer(StringPolicyUtils
					.collectCharacterClass(passwordPolicy.getStringPolicy().getCharacterClass(),
							characterClassType.getRef())));
		}
	}

	private void testMinimalUniqueCharacters(String password, LimitationsType limitations,
			OperationResult result, List<LocalizableMessage> message) {
		if (limitations.getMinUniqueChars() == null) {
			return;
		}
		HashSet<String> distinctCharacters = new HashSet<>(StringPolicyUtils.stringTokenizer(password));
		if (limitations.getMinUniqueChars() > distinctCharacters.size()) {
			LocalizableMessage msg = new LocalizableMessageBuilder()
					.key("ValuePolicy.minimalUniqueCharactersNotMet")
					.arg(limitations.getMinUniqueChars())
					.arg(distinctCharacters.size())
					.build();
			result.addSubresult(new OperationResult("Check minimal count of unique chars", OperationResultStatus.FATAL_ERROR, msg));
			message.add(msg);
		}
	}

	private void testMinimalLength(String value, LimitationsType limitations, OperationResult result, List<LocalizableMessage> messages) {
		if (limitations.getMinLength() == null) {
			return;
		}
		if (value.length() < limitations.getMinLength()) {
			LocalizableMessage msg = new LocalizableMessageBuilder()
					.key("ValuePolicy.minimalSizeNotMet")
					.arg(limitations.getMinLength())
					.arg(value.length())
					.build();
			result.addSubresult(new OperationResult("Check global minimal length", OperationResultStatus.FATAL_ERROR, msg));
			messages.add(msg);
		}
	}

	private void testMaximalLength(String value, LimitationsType limitations, OperationResult result, List<LocalizableMessage> messages) {
		if (limitations.getMaxLength() == null) {
			return;
		}
		if (value.length() > limitations.getMaxLength()) {
			LocalizableMessage msg = new LocalizableMessageBuilder()
					.key("ValuePolicy.maximalSizeExceeded")
					.arg(limitations.getMaxLength())
					.arg(value.length())
					.build();
			result.addSubresult(new OperationResult("Check global maximal length", OperationResultStatus.FATAL_ERROR, msg));
			messages.add(msg);
		}
	}
	
	private void testInvalidCharacters(List<String> valueCharacters, HashSet<String> validChars, OperationResult result, List<LocalizableMessage> message) {
		StringBuilder invalidCharacters = new StringBuilder();
		for (String character : valueCharacters) {
			if (!validChars.contains(character)) {
				invalidCharacters.append(character);
			}
		}
		if (invalidCharacters.length() > 0) {
			LocalizableMessage msg = new LocalizableMessageBuilder()
					.key("ValuePolicy.charactersNotAllowed")
					.arg(invalidCharacters)
					.build();
			result.addSubresult(new OperationResult("Check if value does not contain invalid characters", OperationResultStatus.FATAL_ERROR, msg));
			message.add(msg);
		}
	}
	
	private <O extends ObjectType> void testCheckExpression(String newPassword, LimitationsType lims,
			AbstractValuePolicyOriginResolver<O> originResolver, String shortDesc, Task task, OperationResult result,
			List<LocalizableMessage> messages) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
			CommunicationException, ConfigurationException, SecurityViolationException {
		for (CheckExpressionType checkExpression: lims.getCheckExpression()) {
			ExpressionType expressionType = checkExpression.getExpression();
			if (expressionType == null) {
				return;
			}
			if (!checkExpression(newPassword, expressionType, originResolver, shortDesc, task, result)) {
				LocalizableMessage msg;
				if (checkExpression.getLocalizableFailureMessage() != null) {
					msg = LocalizationUtil.toLocalizableMessage(checkExpression.getLocalizableFailureMessage());
				} else if (checkExpression.getFailureMessage() != null) {
					msg = LocalizableMessageBuilder.buildFallbackMessage(checkExpression.getFailureMessage());
				} else {
					msg = LocalizableMessageBuilder.buildKey("ValuePolicy.checkExpressionFailed");
				}
				result.addSubresult(new OperationResult("Check expression", OperationResultStatus.FATAL_ERROR, msg));
				messages.add(msg);
			}
		}
	}
	
	private <O extends ObjectType> void testProhibitedValues(String newPassword, ProhibitedValuesType prohibitedValuesType,
			AbstractValuePolicyOriginResolver<O> originResolver, String shortDesc, Task task, OperationResult result,
			List<LocalizableMessage> messages) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
			CommunicationException, ConfigurationException, SecurityViolationException {
		if (prohibitedValuesType == null || originResolver == null) {
			return;
		}
		Consumer<ProhibitedValueItemType> failAction = (prohibitedItemType) -> {
			LocalizableMessage msg = new LocalizableMessageBuilder()
					.key("ValuePolicy.prohibitedValue")
					.build();
			result.addSubresult(new OperationResult("Prohibited value", OperationResultStatus.FATAL_ERROR, msg));
			messages.add(msg);
		};
		checkProhibitedValues(newPassword, prohibitedValuesType, originResolver, failAction, shortDesc, task, result);
	}
	
	private <O extends ObjectType, R extends ObjectType> boolean checkProhibitedValues(String newPassword, ProhibitedValuesType prohibitedValuesType, AbstractValuePolicyOriginResolver<O> originResolver,
			Consumer<ProhibitedValueItemType> failAction, String shortDesc, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {

		if (prohibitedValuesType == null || originResolver == null) {
			return true;
		}
		
		MutableBoolean isAcceptable = new MutableBoolean(true);
		for (ProhibitedValueItemType prohibitedItemType: prohibitedValuesType.getItem()) {
			
			ItemPathType itemPathType = prohibitedItemType.getPath();
			if (itemPathType == null) {
				throw new SchemaException("No item path defined in prohibited item in "+shortDesc);
			}
			ItemPath itemPath = itemPathType.getItemPath();
			
			ResultHandler<R> handler = (object, objectResult) -> {
				
				PrismProperty<Object> objectProperty = object.findProperty(itemPath);
				if (objectProperty == null) {
					return true;
				}
				
				if (isMatching(newPassword, objectProperty)) {
					if (failAction != null) {
						failAction.accept(prohibitedItemType);
					}
					isAcceptable.setValue(false);
					return false;
				}
				
				return true;
			};
			originResolver.resolve(handler, prohibitedItemType.getOrigin(), shortDesc, task, result);			
		}

		return isAcceptable.booleanValue();
	}
	
	private boolean isMatching(String newPassword, PrismProperty<Object> objectProperty) {
		for (Object objectRealValue: objectProperty.getRealValues()) {
			if (objectRealValue instanceof String) {
				if (newPassword.equals(objectRealValue)) {
					return true;
				}
			} else if (objectRealValue instanceof ProtectedStringType) {
				ProtectedStringType newPasswordPs = new ProtectedStringType();
				newPasswordPs.setClearValue(newPassword);
				try {
					if (protector.compare(newPasswordPs, (ProtectedStringType)objectRealValue)) {
						return true;
					}
				} catch (SchemaException | EncryptionException e) {
					throw new SystemException(e);
				}
			} else {
				if (newPassword.equals(objectRealValue.toString())) {
					return true;
				}
			}
		}
		return false;
	}

	private String generateAttempt(ValuePolicyType policy, int defaultLength, boolean generateMinimalSize, Context ctx, OperationResult result) {

		StringPolicyType stringPolicy = policy.getStringPolicy();
		// if (policy.getLimitations() != null &&
		// policy.getLimitations().getMinLength() != null){
		// generateMinimalSize = true;
		// }
		// setup default values where missing
		// PasswordPolicyUtils.normalize(pp);

		// Optimize usage of limits ass hashmap of limitas and key is set of
		// valid chars for each limitation
		Map<StringLimitType, List<String>> lims = new HashMap<>();
		int minLen = defaultLength;
		int maxLen = defaultLength;
		int unique = defaultLength / 2;
		if (stringPolicy != null) {
			for (StringLimitType l : stringPolicy.getLimitations().getLimit()) {
				if (null != l.getCharacterClass().getValue()) {
					lims.put(l, StringPolicyUtils.stringTokenizer(l.getCharacterClass().getValue()));
				} else {
					lims.put(l, StringPolicyUtils.stringTokenizer(StringPolicyUtils.collectCharacterClass(
							stringPolicy.getCharacterClass(), l.getCharacterClass().getRef())));
				}
			}

			// Get global limitations
			minLen = defaultIfNull(stringPolicy.getLimitations().getMinLength(), 0);
			if (minLen != 0 && minLen > defaultLength) {
				defaultLength = minLen;
			}
			maxLen = defaultIfNull(stringPolicy.getLimitations().getMaxLength(), 0);
			unique = defaultIfNull(stringPolicy.getLimitations().getMinUniqueChars(), minLen);
		}
		// test correctness of definition
		if (unique > minLen) {
			minLen = unique;
			OperationResult reportBug = new OperationResult("Global limitation check");
			reportBug.recordWarning("There is more required unique characters then defined minimum. Raise minimum to number of required unique chars.");
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
								+ stringPolicy.getDescription());
				// Log error
				if (LOGGER.isErrorEnabled()) {
					LOGGER.error(
							"Unable to generate value for " + ctx.path + ": No intersection for required first character sets in value policy: ["
									+ stringPolicy.getDescription()
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
					LOGGER.trace("Generate first character intersection items [" + tmp + "] into " + ctx.path + ".");
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
					"Unable to meet minimal criteria and not exceed maximal size of " + ctx.path + ".");
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
					"Unable to generate value for " + ctx.path + " and meet minimal size of " + ctx.path + ". Actual length: "
							+ password.length() + ", required: " + minLen);
			LOGGER.trace(
					"Unable to generate value for " + ctx.path + " and meet minimal size of " + ctx.path + ". Actual length: {}, required: {}",
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

	private <O extends ObjectType> boolean checkAttempt(String generatedValue, ValuePolicyType policy, AbstractValuePolicyOriginResolver<O> originResolver, String shortDesc, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		StringPolicyType stringPolicy = policy.getStringPolicy();
		if (stringPolicy != null) {
			LimitationsType limitationsType = stringPolicy.getLimitations();
			if (limitationsType != null) {
				List<CheckExpressionType> checkExpressionTypes = limitationsType.getCheckExpression();
				if (!checkExpressions(generatedValue, checkExpressionTypes, originResolver, shortDesc, task, result)) {
					LOGGER.trace("Check expression returned false for generated value in {}", shortDesc);
					return false;
				}
			}
		}
		if (!checkProhibitedValues(generatedValue, policy.getProhibitedValues(), originResolver, null, shortDesc, task, result)) {
			LOGGER.trace("Generated value is prohibited in {}", shortDesc);
			return false;
		}
		// TODO Check pattern
		return true;
	}
	
	private <O extends ObjectType> boolean checkExpressions(String generatedValue, List<CheckExpressionType> checkExpressionTypes, AbstractValuePolicyOriginResolver<O> originResolver, String shortDesc, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		for (CheckExpressionType checkExpressionType: checkExpressionTypes) {
			ExpressionType expression = checkExpressionType.getExpression();
			if (!checkExpression(generatedValue, expression, originResolver, shortDesc, task, result)) {
				return false;
			}
		}
		return true;
	}

	private <O extends ObjectType> boolean checkExpression(String generatedValue, ExpressionType checkExpression,
			AbstractValuePolicyOriginResolver<O> originResolver, String shortDesc, Task task, OperationResult result)
			throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
			ConfigurationException, SecurityViolationException {
		ExpressionVariables variables = new ExpressionVariables();
		variables.addVariableDefinition(ExpressionConstants.VAR_INPUT, generatedValue);
		variables.addVariableDefinition(ExpressionConstants.VAR_OBJECT, originResolver == null ? null : originResolver.getObject());
		PrismPropertyValue<Boolean> output = ExpressionUtil.evaluateCondition(variables, checkExpression, expressionFactory, shortDesc, task, result);
		return ExpressionUtil.getBooleanConditionOutput(output);
	}
	
	/**
	 * Count cardinality
	 */
	private Map<Integer, List<String>> cardinalityCounter(Map<StringLimitType, List<String>> lims,
			List<String> password, Boolean skipMatchedLims, boolean uniquenessReached, OperationResult op) {
		HashMap<String, Integer> counter = new HashMap<>();

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
		Map<Integer, List<String>> ret = new HashMap<>();
		for (String s : counter.keySet()) {
			// if not there initialize
			ret.computeIfAbsent(counter.get(s), k -> new ArrayList<>());
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
