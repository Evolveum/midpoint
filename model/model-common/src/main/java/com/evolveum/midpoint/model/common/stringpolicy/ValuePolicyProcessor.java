/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.stringpolicy;

import java.util.*;
import java.util.function.Consumer;

import com.evolveum.midpoint.model.api.validator.StringLimitationResult;
import com.evolveum.midpoint.prism.MutablePrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrimitiveType;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.util.LocalizationUtil;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.LocalizableMessageList;
import com.evolveum.midpoint.util.LocalizableMessageListBuilder;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringTranslationType;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.text.StrBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProhibitedValueItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProhibitedValuesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StringLimitType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StringPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import static com.evolveum.midpoint.util.MiscUtil.or0;

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
    private static final Trace LOGGER = TraceManager.getTrace(ValuePolicyProcessor.class);

    private static final Random RAND = new Random(System.currentTimeMillis());
    private static final String ALPHANUMERIC_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";

    private static final String DOT_CLASS = ValuePolicyProcessor.class.getName() + ".";
    private static final String OPERATION_STRING_POLICY_VALIDATION = DOT_CLASS + "stringPolicyValidation";
    private static final int DEFAULT_MAX_ATTEMPTS = 10;

    @Autowired private PrismContext prismContext;
    @Autowired private ExpressionFactory expressionFactory;
    @Autowired private Protector protector;

    private static class Context {
        @NotNull private final ItemPath path;

        private Context(@NotNull ItemPath path) {
            this.path = path;
        }
    }

    public ExpressionFactory getExpressionFactory() {
        return expressionFactory;
    }

    public ValuePolicyProcessor() {
    }

    @VisibleForTesting
    public ValuePolicyProcessor(ExpressionFactory expressionFactory) {
        this.prismContext = PrismContext.get();
        this.expressionFactory = expressionFactory;
        this.protector = prismContext.getDefaultProtector();
    }

    public String generate(ItemPath path, ValuePolicyType policy, int defaultLength, boolean generateMinimalSize,
            ObjectBasedValuePolicyOriginResolver<?> originResolver, String shortDesc, Task task, OperationResult parentResult)
            throws ExpressionEvaluationException, SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        Context ctx = new Context(path != null ? path : SchemaConstants.PATH_PASSWORD_VALUE);
        OperationResult result = parentResult.createSubresult(OP_GENERATE);

        if (policy == null) {
            // let's create some default policy
            policy = new ValuePolicyType()
                    .stringPolicy(
                            new StringPolicyType()
                                    .limitations(
                                            new LimitationsType()
                                                    .maxLength(defaultLength)
                                                    .minLength(defaultLength)));
        }

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
            // TODO: this needs to be determined from ValuePolicyType archetype
            ExpressionProfile expressionProfile = MiscSchemaUtil.getExpressionProfile();
            if (checkAttempt(generatedValue, policy, expressionProfile, originResolver, shortDesc, task, result)) {
                break;
            }
            LOGGER.trace("Generator attempt {}: check failed", attempt);
            if (attempt == maxAttempts) {
                ExpressionEvaluationException e =  new ExpressionEvaluationException("Unable to generate value, maximum number of attempts ("+maxAttempts+") exceeded");
                result.recordFatalError(e);
                throw e;
            }
            attempt++;
        }
        return generatedValue;
    }

    public List<StringLimitationResult> validateValue(String newValue, ValuePolicyType pp,
            ObjectBasedValuePolicyOriginResolver<?> originResolver, String shortDesc, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        return validateValue(newValue, pp, originResolver, new ArrayList<>(), shortDesc, task, parentResult);
    }

    public List<StringLimitationResult> validateValue(String newValue, ValuePolicyType pp,
            ObjectBasedValuePolicyOriginResolver<?> originResolver, List<LocalizableMessage> messages, String shortDesc, Task task,
            OperationResult parentResult) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        //TODO: do we want to throw exception when no value policy defined??
        Validate.notNull(pp, "Value policy must not be null.");

        OperationResult result = parentResult.createSubresult(OPERATION_STRING_POLICY_VALIDATION);
        result.addArbitraryObjectAsParam("policyName", pp.getName());
        List<StringLimitationResult> limitations = new ArrayList<>();
        try {
            normalize(pp);

            if (newValue == null) {
                newValue = "";
            }

            LimitationsType lims = pp.getStringPolicy().getLimitations();
            CollectionUtils.addIgnoreNull(limitations, testLength(newValue, lims, result, messages));

            CollectionUtils.addIgnoreNull(limitations, testMinimalUniqueCharacters(newValue, lims, result, messages));

            CollectionUtils.addIgnoreNull(
                    limitations, testProhibitedValues(newValue, pp.getProhibitedValues(), originResolver, shortDesc, task, result, messages));

            // TODO: this needs to be determined from ValuePolicyType archetype
            ExpressionProfile expressionProfile = MiscSchemaUtil.getExpressionProfile();
            limitations.addAll(testCheckExpression(newValue, lims, expressionProfile, originResolver, shortDesc, task, result, messages));

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
                    StringLimitationResult limitation = null;
                    limitation = testMinimalOccurrence(stringLimitationType, count, limitResult, messages, limitation);
                    limitation = testMaximalOccurrence(stringLimitationType, count, limitResult, messages, limitation);
                    limitation = testMustBeFirst(stringLimitationType, limitResult, messages, newValue, validChars,limitation);

                    if (limitation != null) {
                        PolyStringType name = stringLimitationType.getName();
                        if (name == null) {
                            name =  new PolyStringType(stringLimitationType.getDescription());
                            PolyStringTranslationType translation = new PolyStringTranslationType();
                            translation.setKey(stringLimitationType.getDescription());
                            name.setTranslation(translation);
                        }
                        PolyStringType help =  new PolyStringType(getCharsetAsString(validChars));
                        limitation.setHelp(help);
                        limitation.setName(name);
                        limitations.add(limitation);
                    }

                    limitResult.computeStatus();
                    result.addSubresult(limitResult);
                }
                CollectionUtils.addIgnoreNull(limitations, testInvalidCharacters(characters, allValidChars, result, messages));
            }
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }

        if (!result.isSuccess() && !messages.isEmpty()) {
            result.setUserFriendlyMessage(
                    new LocalizableMessageListBuilder()
                            .messages(messages)
                            .separator(LocalizableMessageList.SPACE)
                            .buildOptimized());
        }
        return limitations;
    }

    private String getCharsetAsString(HashSet<String> validChars) {
        StringBuilder sb = new StringBuilder();
        validChars.forEach(validChar -> sb.append(validChar));
        return sb.toString();
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
    }

    private StringLimitationResult testMustBeFirst(StringLimitType stringLimitation, OperationResult result, List<LocalizableMessage> messages,
            String value, Set<String> validFirstChars, StringLimitationResult limitation) {
        if (stringLimitation.isMustBeFirst() == null) {
            return limitation;
        }
        if (limitation == null) {
            limitation = new StringLimitationResult();
            limitation.setMustBeFirst(stringLimitation.isMustBeFirst());
            limitation.setSuccess(StringUtils.isNotEmpty(value));
        } else {
            limitation.setMustBeFirst(stringLimitation.isMustBeFirst());
        }
        if (StringUtils.isNotEmpty(value) && isTrue(stringLimitation.isMustBeFirst()) && !validFirstChars.contains(value.substring(0, 1))) {
            LocalizableMessage msg = new LocalizableMessageBuilder()
                    .key("ValuePolicy.firstCharacterNotAllowed")
                    .arg(validFirstChars.toString())
                    .build();
            result.addSubresult(new OperationResult("Check valid first char", OperationResultStatus.FATAL_ERROR, msg));
            messages.add(msg);
            limitation.setSuccess(false);
        }
        return limitation;
    }

    private StringLimitationResult testMaximalOccurrence(StringLimitType stringLimitation, int count, OperationResult result, List<LocalizableMessage> messages, StringLimitationResult limitation) {
        if (stringLimitation.getMaxOccurs() == null) {
            return limitation;
        }
        if (limitation == null) {
            limitation = new StringLimitationResult();
            limitation.setMaxOccurs(stringLimitation.getMaxOccurs());
            limitation.setSuccess(true);
        } else {
            limitation.setMaxOccurs(stringLimitation.getMaxOccurs());
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
            limitation.setSuccess(false);
        }
        return limitation;
    }

    private StringLimitationResult testMinimalOccurrence(StringLimitType stringLimitation, int count, OperationResult result, List<LocalizableMessage> messages, StringLimitationResult limitation) {
        if (stringLimitation.getMinOccurs() == null) {
            return limitation;
        }
        if (limitation == null) {
            limitation = new StringLimitationResult();
            limitation.setMinOccurs(stringLimitation.getMinOccurs());
            limitation.setSuccess(true);
        } else {
            limitation.setMinOccurs(stringLimitation.getMinOccurs());
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
            limitation.setSuccess(false);
        }
        return limitation;
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

    private StringLimitationResult testMinimalUniqueCharacters(String password, LimitationsType limitations,
            OperationResult result, List<LocalizableMessage> message) {
        if (limitations.getMinUniqueChars() == null) {
            return null;
        }
        HashSet<String> distinctCharacters = new HashSet<>(StringPolicyUtils.stringTokenizer(password));
        StringLimitationResult limitation = new StringLimitationResult();
        limitation.setMinOccurs(limitations.getMinUniqueChars());
        PolyStringType name =  new PolyStringType("unique characters");
        PolyStringTranslationType translation = new PolyStringTranslationType();
        translation.setKey("ValuePolicy.uniqueCharacters");
        name.setTranslation(translation);
        limitation.setName(name);
        limitation.setSuccess(true);
        if (limitations.getMinUniqueChars() > distinctCharacters.size()) {
            LocalizableMessage msg = new LocalizableMessageBuilder()
                    .key("ValuePolicy.minimalUniqueCharactersNotMet")
                    .arg(limitations.getMinUniqueChars())
                    .arg(distinctCharacters.size())
                    .build();
            result.addSubresult(new OperationResult("Check minimal count of unique chars", OperationResultStatus.FATAL_ERROR, msg));
            message.add(msg);
            limitation.setSuccess(false);
        }
        return limitation;
    }

    private StringLimitationResult testLength(String value, LimitationsType limitations, OperationResult result, List<LocalizableMessage> messages) {
        if (limitations.getMinLength() == null && limitations.getMaxLength() == null) {
            return null;
        }
        StringLimitationResult limitation = new StringLimitationResult();
        limitation.setMinOccurs(limitations.getMinLength());
        limitation.setMaxOccurs(limitations.getMaxLength());
        PolyStringType name =  new PolyStringType("characters");
        PolyStringTranslationType translation = new PolyStringTranslationType();
        translation.setKey("ValuePolicy.characters");
        name.setTranslation(translation);
        limitation.setName(name);
        limitation.setSuccess(true);
        if (limitations.getMinLength() != null && value.length() < limitations.getMinLength()) {
            LocalizableMessage msg = new LocalizableMessageBuilder()
                    .key("ValuePolicy.minimalSizeNotMet")
                    .arg(limitations.getMinLength())
                    .arg(value.length())
                    .build();
            result.addSubresult(new OperationResult("Check global minimal length", OperationResultStatus.FATAL_ERROR, msg));
            messages.add(msg);
            limitation.setSuccess(false);
        }

        if (limitations.getMaxLength() != null && value.length() > limitations.getMaxLength()) {
            LocalizableMessage msg = new LocalizableMessageBuilder()
                    .key("ValuePolicy.maximalSizeExceeded")
                    .arg(limitations.getMaxLength())
                    .arg(value.length())
                    .build();
            result.addSubresult(new OperationResult("Check global maximal length", OperationResultStatus.FATAL_ERROR, msg));
            messages.add(msg);
            limitation.setSuccess(false);
        }

        return limitation;
    }

    private StringLimitationResult testInvalidCharacters(List<String> valueCharacters, HashSet<String> validChars, OperationResult result, List<LocalizableMessage> message) {
        StringBuilder invalidCharacters = new StringBuilder();
        for (String character : valueCharacters) {
            if (!validChars.contains(character)) {
                invalidCharacters.append(character);
            }
        }
        StringLimitationResult limitation = new StringLimitationResult();
        PolyStringType name =  new PolyStringType("invalid characters");
        PolyStringTranslationType translation = new PolyStringTranslationType();
        translation.setKey("ValuePolicy.invalidCharacters");
        name.setTranslation(translation);
        limitation.setName(name);
        PolyStringType help =  new PolyStringType(getCharsetAsString(validChars));
        limitation.setHelp(help);
        limitation.setSuccess(true);
        if (invalidCharacters.length() > 0) {
            LocalizableMessage msg = new LocalizableMessageBuilder()
                    .key("ValuePolicy.charactersNotAllowed")
                    .arg(invalidCharacters)
                    .build();
            result.addSubresult(new OperationResult("Check if value does not contain invalid characters", OperationResultStatus.FATAL_ERROR, msg));
            message.add(msg);
            limitation.setSuccess(false);
        }
        return limitation;
    }

    private List<StringLimitationResult> testCheckExpression(String newPassword, LimitationsType lims,
            ExpressionProfile expressionProfile, ObjectBasedValuePolicyOriginResolver<?> originResolver, String shortDesc,
            Task task, OperationResult result, List<LocalizableMessage> messages) throws SchemaException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        List<StringLimitationResult> limitations =  new ArrayList<>();
        for (CheckExpressionType checkExpression: lims.getCheckExpression()) {
            ExpressionType expressionType = checkExpression.getExpression();
            if (expressionType == null) {
                continue;
            }
            StringLimitationResult limitation = new StringLimitationResult();
            PolyStringType name = null;
            if (checkExpression.getDisplay() != null) {
                name = checkExpression.getDisplay().getLabel();
                limitation.setHelp(checkExpression.getDisplay().getHelp());
            }
            if (name == null){
                name =  new PolyStringType("Check expression");
                PolyStringTranslationType translation = new PolyStringTranslationType();
                translation.setKey("ValuePolicy.checkExpression");
                name.setTranslation(translation);
            }
            limitation.setName(name);
            limitation.setSuccess(true);
            if (!checkExpression(newPassword, expressionType, expressionProfile, originResolver, shortDesc, task, result)) {
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
                limitation.setSuccess(false);
            }
            limitations.add(limitation);
        }
        return limitations;
    }

    private StringLimitationResult testProhibitedValues(String newPassword, ProhibitedValuesType prohibitedValuesType,
            ObjectBasedValuePolicyOriginResolver<?> originResolver, String shortDesc, Task task, OperationResult result,
            List<LocalizableMessage> messages) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        if (prohibitedValuesType == null || originResolver == null) {
            return null;
        }
        StringLimitationResult limitation = new StringLimitationResult();
        PolyStringType name =  new PolyStringType("prohibited value");
        PolyStringTranslationType translation = new PolyStringTranslationType();
        translation.setKey("ValuePolicy.prohibitedValueName");
        name.setTranslation(translation);
        limitation.setName(name);
        PolyStringType help =  new PolyStringType("");
        PolyStringTranslationType helpTranslation = new PolyStringTranslationType();
        helpTranslation.setKey("ValuePolicy.prohibitedValue");
        help.setTranslation(helpTranslation);
        limitation.setHelp(help);
        limitation.setSuccess(true);
        Consumer<ProhibitedValueItemType> failAction = (prohibitedItemType) -> {
            LocalizableMessage msg = new LocalizableMessageBuilder()
                    .key("ValuePolicy.prohibitedValue")
                    .build();
            result.addSubresult(new OperationResult("Prohibited value", OperationResultStatus.FATAL_ERROR, msg));
            messages.add(msg);
            limitation.setSuccess(false);
        };
        checkProhibitedValues(newPassword, prohibitedValuesType, originResolver, failAction, shortDesc, task, result);
        return limitation;
    }

    private <O extends ObjectType, R extends ObjectType> boolean checkProhibitedValues(
            String newPassword,
            ProhibitedValuesType prohibitedValuesType,
            ObjectBasedValuePolicyOriginResolver<O> originResolver,
            Consumer<ProhibitedValueItemType> failAction,
            String shortDesc,
            Task task,
            OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {

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
            originResolver.resolve(prohibitedItemType, handler, shortDesc, task, result);
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
                    if (protector.compareCleartext(newPasswordPs, (ProtectedStringType)objectRealValue)) {
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
            int allLimitations = 0;
            int ignoredLimitations = 0;
            for (StringLimitType l : stringPolicy.getLimitations().getLimit()) {
                allLimitations++;
                if (Boolean.TRUE.equals(l.isIgnoreWhenGenerating())) {
                    ignoredLimitations++;
                    if (or0(l.getMinOccurs()) > 0) {
                        result.recordFatalError(
                                "Character class is marked as ignored for generation, but has non-zero min occurrences");
                        return null;
                    }
                    continue;
                }
                if (null != l.getCharacterClass().getValue()) {
                    lims.put(l, StringPolicyUtils.stringTokenizer(l.getCharacterClass().getValue()));
                } else {
                    lims.put(l, StringPolicyUtils.stringTokenizer(StringPolicyUtils.collectCharacterClass(
                            stringPolicy.getCharacterClass(), l.getCharacterClass().getRef())));
                }
            }
            if (allLimitations > 0 && ignoredLimitations == allLimitations) {
                result.recordFatalError("Couldn't generate the value, all character classes are marked as ignored for generation");
                return null;
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
            maxLen = Math.max(minLen, defaultLength);
        }

        // Initialize generator
        StringBuilder password = new StringBuilder();

        /*
         * ********************************** Try to find best characters to be
         * first in password
         */
        Map<StringLimitType, List<String>> mustBeFirst = new HashMap<>();
        for (Map.Entry<StringLimitType, List<String>> entry : lims.entrySet()) {
            final StringLimitType key = entry.getKey();
            if (key.isMustBeFirst() != null && key.isMustBeFirst()) {
                mustBeFirst.put(key, entry.getValue());
            }
        }

        // If any limitation was found to be first
        if (!mustBeFirst.isEmpty()) {
            Map<Integer, List<String>> possibleFirstChars = cardinalityCounter(mustBeFirst, null, false,
                    false, result);
            int intersectionCardinality = mustBeFirst.keySet().size();
            List<String> intersectionCharacters = possibleFirstChars.get(intersectionCardinality);
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
                    for (Map.Entry<StringLimitType, List<String>> entry : mustBeFirst.entrySet()) {
                        StrBuilder tmp = new StrBuilder();
                        tmp.appendSeparator(", ");
                        tmp.appendAll(entry.getValue());
                        LOGGER.error("L:" + entry.getKey().getDescription() + " -> [" + tmp + "]");
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

        //fake limit with all alphanumeric character, because of number of unique char
        if(lims.isEmpty()){
            StringLimitType fakeLimit = new StringLimitType();
            CharacterClassType charClass = new CharacterClassType();
            charClass.setValue(ALPHANUMERIC_CHARS);
            fakeLimit.setCharacterClass(charClass);
            fakeLimit.setMustBeFirst(false);
            fakeLimit.setMaxOccurs(maxLen);
            fakeLimit.setMinOccurs(minLen);
            lims.put(fakeLimit, StringPolicyUtils.stringTokenizer(ALPHANUMERIC_CHARS));
        }
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

    private <O extends ObjectType> boolean checkAttempt(String generatedValue, ValuePolicyType policy,
            ExpressionProfile expressionProfile, ObjectBasedValuePolicyOriginResolver<O> originResolver, String shortDesc,
            Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        StringPolicyType stringPolicy = policy.getStringPolicy();
        if (stringPolicy != null) {
            LimitationsType limitationsType = stringPolicy.getLimitations();
            if (limitationsType != null) {
                List<CheckExpressionType> checkExpressionTypes = limitationsType.getCheckExpression();
                if (!checkExpressions(generatedValue, checkExpressionTypes, expressionProfile, originResolver, shortDesc, task, result)) {
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

    private <O extends ObjectType> boolean checkExpressions(String generatedValue, List<CheckExpressionType> checkExpressionTypes,
            ExpressionProfile expressionProfile, ObjectBasedValuePolicyOriginResolver<O> originResolver, String shortDesc,
            Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        for (CheckExpressionType checkExpressionType: checkExpressionTypes) {
            ExpressionType expression = checkExpressionType.getExpression();
            if (!checkExpression(generatedValue, expression, expressionProfile, originResolver, shortDesc, task, result)) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private <O extends ObjectType> boolean checkExpression(String generatedValue, ExpressionType checkExpression,
            ExpressionProfile expressionProfile, ObjectBasedValuePolicyOriginResolver<O> originResolver, String shortDesc,
            Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        VariablesMap variables = new VariablesMap();

        MutablePrismPropertyDefinition<Object> defInput = prismContext.definitionFactory().createPropertyDefinition(
                new ItemName(SchemaConstants.NS_C, ExpressionConstants.VAR_INPUT), PrimitiveType.STRING.getQname());
        variables.addVariableDefinition(ExpressionConstants.VAR_INPUT, generatedValue, defInput);

        PrismObject<O> object = null;
        PrismObjectDefinition<O> objectDef = null;
        if (originResolver != null) {
            object = originResolver.getObject();
            if (object != null) {
                objectDef = object.getDefinition();
            }
        }
        if (objectDef == null) {
            //noinspection unchecked
            objectDef = (PrismObjectDefinition<O>) prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ObjectType.class);
        }
        variables.addVariableDefinition(ExpressionConstants.VAR_OBJECT, object, objectDef);

        return ExpressionUtil.evaluateConditionDefaultFalse(variables, checkExpression,
                expressionProfile, expressionFactory, shortDesc, task, result);
    }

    /**
     * Count cardinality
     */
    private Map<Integer, List<String>> cardinalityCounter(Map<StringLimitType, List<String>> lims,
            List<String> password, Boolean skipMatchedLims, boolean uniquenessReached, OperationResult op) {
        HashMap<String, Integer> counter = new HashMap<>();

        for (Map.Entry<StringLimitType, List<String>> entry : lims.entrySet()) {
            final StringLimitType key = entry.getKey();
            int counterKey = 1;
            List<String> chars = entry.getValue();
            int i;
            if (password != null) {
                i = charIntersectionCounter(entry.getValue(), password);
            } else {
                i = 0;
            }
            // If max is exceed then error unable to continue
            if (key.getMaxOccurs() != null && i > key.getMaxOccurs()) {
                OperationResult o = new OperationResult("Limitation check :" + key.getDescription());
                o.recordFatalError(
                    "Exceeded maximal value for this limitation. " + i + ">" + key.getMaxOccurs());
                op.addSubresult(o);
                return null;
                // if max is all ready reached or skip enabled for minimal skip
                // counting
            } else if (key.getMaxOccurs() != null && i == key.getMaxOccurs()) {
                continue;
                // other cases minimum is not reached
            } else if ((key.getMinOccurs() == null || i >= key.getMinOccurs()) && !skipMatchedLims) {
                continue;
            }
            for (String s : chars) {
                if (null == password || !password.contains(s) || uniquenessReached) {
                    counter.put(s, counterKey);
                }
            }
            counterKey++;       // TODO this is suspicious
        }

        // If need to remove disabled chars (already reached limitations)
        if (password != null) {
            for (Map.Entry<StringLimitType, List<String>> entry : lims.entrySet()) {
                StringLimitType l = entry.getKey();
                int i = charIntersectionCounter(entry.getValue(), password);
                if (l.getMaxOccurs() != null && i > l.getMaxOccurs()) {
                    OperationResult o = new OperationResult("Limitation check :" + l.getDescription());
                    o.recordFatalError(
                            "Exceeded maximal value for this limitation. " + i + ">" + l.getMaxOccurs());
                    op.addSubresult(o);
                    return null;
                } else if (l.getMaxOccurs() != null && i == l.getMaxOccurs()) {
                    // limitation matched remove all used chars
                    LOGGER.trace("Skip " + l.getDescription());
                    for (String charToRemove : entry.getValue()) {
                        counter.remove(charToRemove);
                    }
                }
            }
        }

        // Transpose to better format
        Map<Integer, List<String>> ret = new HashMap<>();
        for (Map.Entry<String, Integer> entry : counter.entrySet()) {
            // if not there initialize
            ret.computeIfAbsent(entry.getValue(), k -> new ArrayList<>());
            ret.get(entry.getValue()).add(entry.getKey());
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
