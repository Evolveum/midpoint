/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.stringpolicy;

import com.evolveum.midpoint.model.api.validator.StringLimitationResult;
import com.evolveum.midpoint.model.common.ModelCommonBeans;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.LocalizationUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringTranslationType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

/**
 * Checks string values against the string policy.
 *
 * Provides both "brief" {@link #checkExpressionsAndProhibitions(String, OperationResult)} and "full" checks
 * ({@link #checkFully(String, OperationResult)}); the former are to be used e.g. to check generated values
 * for aspects that we cannot guarantee when generating.
 *
 * NOTE: Not using {@link ModelCommonBeans} because of low-level tests where that component is not available.
 */
class ValueChecker {

    private static final Trace LOGGER = TraceManager.getTrace(ValueChecker.class);

    @NotNull private final StringPolicy stringPolicy;

    @Nullable private final ProhibitedValuesType prohibitedValues;
    private final ExpressionProfile expressionProfile; // TODO make not null
    @Nullable private final ObjectBasedValuePolicyOriginResolver<?> originResolver;
    @NotNull private final String shortDesc;
    @NotNull private final Protector protector;
    @NotNull private final ExpressionFactory expressionFactory;
    @NotNull private final Task task;

    ValueChecker(
            @NotNull StringPolicy stringPolicy,
            @Nullable ProhibitedValuesType prohibitedValues,
            ExpressionProfile expressionProfile,
            @Nullable ObjectBasedValuePolicyOriginResolver<?> originResolver,
            @NotNull String shortDesc,
            @NotNull Protector protector,
            @NotNull ExpressionFactory expressionFactory,
            @NotNull Task task) {
        this.stringPolicy = stringPolicy;
        this.prohibitedValues = prohibitedValues;
        this.expressionProfile = expressionProfile;
        this.originResolver = originResolver;
        this.shortDesc = shortDesc;
        this.protector = protector;
        this.expressionFactory = expressionFactory;
        this.task = task;
    }

    boolean checkExpressionsAndProhibitions(@NotNull String value, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        return new Operation(value)
                .executeForExpressionsAndProhibitions(result);
    }

    List<StringLimitationResult> checkFully(@NotNull String value, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        return new Operation(value)
                .executeFully(result);
    }

    /** Single "value check" operation. */
    class Operation {

        /** Value being checked. */
        @NotNull private final String value;

        /** Value decomposed into characters. */
        @NotNull private final Set<Character> distinctCharacters;

        /** Result of the operation. Used only for {@link #executeFully(OperationResult)}. */
        @NotNull private final List<StringLimitationResult> resultList = new ArrayList<>();

        Operation(@NotNull String value) {
            this.value = value;
            this.distinctCharacters = StringPolicyUtils.stringAsCharacters(value);
        }

        boolean executeForExpressionsAndProhibitions(OperationResult result)
                throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
                CommunicationException, ConfigurationException, SecurityViolationException {
            if (!checkExpressions(result)) {
                LOGGER.trace("Check expression returned false for value in {}", shortDesc);
                return false;
            }
            if (!checkProhibitedValues(null, result)) {
                LOGGER.trace("Value is prohibited in {}", shortDesc);
                return false;
            }
            return true;
        }

        List<StringLimitationResult> executeFully(OperationResult result)
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException {

            testLength();
            testMinimalUniqueCharacters();
            testProhibitedValues(result);
            testCheckExpression(result);
            testCharacterClasses();

            return resultList;
        }

        private <R extends ObjectType> boolean checkProhibitedValues(
                Consumer<ProhibitedValueItemType> failAction, OperationResult result)
                throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
                CommunicationException, ConfigurationException, SecurityViolationException {

            if (prohibitedValues == null || originResolver == null) {
                return true;
            }

            MutableBoolean isAcceptable = new MutableBoolean(true);

            for (ProhibitedValueItemType prohibitedValue : prohibitedValues.getItem()) {

                ItemPathType itemPathType = prohibitedValue.getPath();
                if (itemPathType == null) {
                    throw new SchemaException("No item path defined in prohibited item in " + shortDesc);
                }
                ItemPath itemPath = itemPathType.getItemPath();

                ResultHandler<R> handler = (object, lResult) -> {

                    // Not providing own operation result, as the processing is minimal here.

                    PrismProperty<Object> objectProperty = object.findProperty(itemPath);
                    if (objectProperty == null) {
                        return true;
                    }

                    if (isMatching(value, objectProperty)) {
                        if (failAction != null) {
                            failAction.accept(prohibitedValue);
                        }
                        isAcceptable.setValue(false);
                        return false;
                    }

                    return true;
                };
                originResolver.resolve(prohibitedValue, handler, shortDesc, task, result);
            }

            return isAcceptable.booleanValue();
        }

        private boolean isMatching(String value, PrismProperty<Object> objectProperty) {
            for (Object objectRealValue: objectProperty.getRealValues()) {
                if (objectRealValue instanceof String) {
                    if (value.equals(objectRealValue)) {
                        return true;
                    }
                } else if (objectRealValue instanceof ProtectedStringType) {
                    ProtectedStringType newPasswordPs = new ProtectedStringType().clearValue(value);
                    try {
                        if (protector.compareCleartext(newPasswordPs, (ProtectedStringType) objectRealValue)) {
                            return true;
                        }
                    } catch (SchemaException | EncryptionException e) {
                        throw new SystemException(e);
                    }
                } else {
                    if (value.equals(objectRealValue.toString())) {
                        return true;
                    }
                }
            }
            return false;
        }

        /** See also {@link #testCharacterClass(StringPolicy.CharacterClassLimitation)}. */
        private void testLength() {
            int requiredMinLength = stringPolicy.getMinLength();
            Integer requiredMaxLength = stringPolicy.getDeclaredMaxLength();
            int requiredMaxLengthEffective = stringPolicy.getEffectiveMaxLength();

            if (requiredMinLength == 0 && requiredMaxLength == null) {
                return;
            }

            StringLimitationResult limResult = new StringLimitationResult();

            PolyStringType name = new PolyStringType("characters");
            PolyStringTranslationType translation = new PolyStringTranslationType();
            translation.setKey("ValuePolicy.characters");
            name.setTranslation(translation);
            limResult.setName(name);

            limResult.setMinOccurs(requiredMinLength);
            if (value.length() < requiredMinLength) {
                limResult.recordFailure(new LocalizableMessageBuilder()
                        .key("ValuePolicy.minimalSizeNotMet")
                        .arg(requiredMinLength)
                        .arg(value.length())
                        .build());
            }

            limResult.setMaxOccurs(requiredMaxLength);
            if (value.length() > requiredMaxLengthEffective) {
                limResult.recordFailure(new LocalizableMessageBuilder()
                        .key("ValuePolicy.maximalSizeExceeded")
                        .arg(requiredMaxLengthEffective)
                        .arg(value.length())
                        .build());
            }
            addResult(limResult);
        }

        private void testMinimalUniqueCharacters() {
            int requiredMinUniqueChars = stringPolicy.getMinUniqueChars();
            if (requiredMinUniqueChars == 0) {
                return;
            }

            StringLimitationResult limResult = new StringLimitationResult();

            PolyStringType name = new PolyStringType("unique characters");
            PolyStringTranslationType translation = new PolyStringTranslationType();
            translation.setKey("ValuePolicy.uniqueCharacters");
            name.setTranslation(translation);
            limResult.setName(name);

            limResult.setMinOccurs(requiredMinUniqueChars);

            if (distinctCharacters.size() < requiredMinUniqueChars) {
                limResult.recordFailure(new LocalizableMessageBuilder()
                        .key("ValuePolicy.minimalUniqueCharactersNotMet")
                        .arg(requiredMinUniqueChars)
                        .arg(distinctCharacters.size())
                        .build());
            }
            addResult(limResult);
        }

        private void testProhibitedValues(OperationResult result)
                throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
                CommunicationException, ConfigurationException, SecurityViolationException {

            if (prohibitedValues == null || originResolver == null) {
                return;
            }

            StringLimitationResult limResult = new StringLimitationResult();
            PolyStringType name = new PolyStringType("prohibited value");
            PolyStringTranslationType translation = new PolyStringTranslationType();
            translation.setKey("ValuePolicy.prohibitedValueName");
            name.setTranslation(translation);
            limResult.setName(name);
            PolyStringType help = new PolyStringType("");
            PolyStringTranslationType helpTranslation = new PolyStringTranslationType();
            helpTranslation.setKey("ValuePolicy.prohibitedValue");
            help.setTranslation(helpTranslation);
            limResult.setHelp(help);

            checkProhibitedValues(
                    item -> limResult.recordFailure(new LocalizableMessageBuilder()
                            .key("ValuePolicy.prohibitedValue")
                            .build()),
                    result);

            addResult(limResult);
        }

        private void testCheckExpression(OperationResult result)
                throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
                ConfigurationException, SecurityViolationException {

            for (CheckExpressionType checkExpression : stringPolicy.getCheckExpressions()) {
                ExpressionType expression = checkExpression.getExpression();
                if (expression == null) {
                    continue;
                }
                StringLimitationResult limResult = new StringLimitationResult();
                PolyStringType name = null;
                if (checkExpression.getDisplay() != null) {
                    name = checkExpression.getDisplay().getLabel();
                    limResult.setHelp(checkExpression.getDisplay().getHelp());
                }
                if (name == null) {
                    name = new PolyStringType("Check expression");
                    PolyStringTranslationType translation = new PolyStringTranslationType();
                    translation.setKey("ValuePolicy.checkExpression");
                    name.setTranslation(translation);
                }
                limResult.setName(name);

                if (!checkExpression(expression, result)) {
                    LocalizableMessage msg;
                    if (checkExpression.getLocalizableFailureMessage() != null) {
                        msg = LocalizationUtil.toLocalizableMessage(checkExpression.getLocalizableFailureMessage());
                    } else if (checkExpression.getFailureMessage() != null) {
                        msg = LocalizableMessageBuilder.buildFallbackMessage(checkExpression.getFailureMessage());
                    } else {
                        msg = LocalizableMessageBuilder.buildKey("ValuePolicy.checkExpressionFailed");
                    }
                    limResult.recordFailure(msg);
                }

                addResult(limResult);
            }
        }

        private boolean checkExpressions(OperationResult result)
                throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
                CommunicationException, ConfigurationException, SecurityViolationException {
            for (CheckExpressionType checkExpression : stringPolicy.getCheckExpressions()) {
                if (!checkExpression(checkExpression.getExpression(), result)) {
                    return false;
                }
            }
            return true;
        }

        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        private boolean checkExpression(
                ExpressionType expression, OperationResult result)
                throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
                ConfigurationException, SecurityViolationException {
            VariablesMap variables = new VariablesMap();

            PrismPropertyDefinition<Object> inputDef = PrismContext.get().definitionFactory().newPropertyDefinition(
                    new ItemName(SchemaConstants.NS_C, ExpressionConstants.VAR_INPUT),
                    PrimitiveType.STRING.getQname());
            variables.addVariableDefinition(ExpressionConstants.VAR_INPUT, value, inputDef);

            PrismObject<? extends ObjectType> object = null;
            PrismObjectDefinition<? extends ObjectType> objectDef = null;
            if (originResolver != null) {
                object = originResolver.getObject();
                if (object != null) {
                    objectDef = object.getDefinition();
                }
            }
            if (objectDef == null) {
                objectDef = PrismContext.get().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ObjectType.class);
            }
            variables.addVariableDefinition(ExpressionConstants.VAR_OBJECT, object, objectDef);

            return ExpressionUtil.evaluateConditionDefaultFalse(
                    variables, expression,
                    expressionProfile, expressionFactory,
                    shortDesc, task, result);
        }

        private void testCharacterClasses() {
            var classLimitations = stringPolicy.getCharacterClassLimitations();
            if (classLimitations.isEmpty()) {
                return;
            }

            HashSet<Character> allValidChars = new HashSet<>();
            for (var classLimitation : classLimitations) {
                CharacterClass characterClass = classLimitation.characterClass();
                testCharacterClass(classLimitation);
                allValidChars.addAll(characterClass.characters);
            }
            testInvalidCharacters(allValidChars);
        }

        /** See also {@link #testLength()}. */
        private void testCharacterClass(StringPolicy.CharacterClassLimitation classLimitation) {

            int requiredMinOccurrences = classLimitation.minOccurrences();
            Integer requiredMaxOccurrences = classLimitation.declaredMaxOccurrences();
            int requiredMaxOccurrencesEffective = classLimitation.effectiveMaxOccurrences();
            boolean mustBeFirst = classLimitation.mustBeFirst();

            if (requiredMinOccurrences == 0 && requiredMaxOccurrences == null && !mustBeFirst) {
                return;
            }

            String limitationDescription = classLimitation.getDescription();
            var characterClass = classLimitation.characterClass();
            int count = characterClass.countOccurrences(value);

            StringLimitationResult limResult = new StringLimitationResult();

            PolyStringType name = classLimitation.getName();
            if (name == null) {
                name = new PolyStringType(Objects.requireNonNullElse(limitationDescription, "limitation"));
                if (limitationDescription != null) {
                    PolyStringTranslationType translation = new PolyStringTranslationType();
                    translation.setKey(limitationDescription);
                    name.setTranslation(translation);
                }
            }
            limResult.setName(name);
            PolyStringType help = new PolyStringType(characterClass.getCharactersAsString());
            limResult.setHelp(help);

            if (requiredMinOccurrences > 0) {
                limResult.setMinOccurs(requiredMinOccurrences);
            }
            limResult.setMaxOccurs(requiredMaxOccurrences);
            limResult.setMustBeFirst(mustBeFirst);

            if (count < requiredMinOccurrences) {
                limResult.recordFailure(new LocalizableMessageBuilder()
                        .key("ValuePolicy.minimalOccurrenceNotMet")
                        .arg(requiredMinOccurrences)
                        .arg(limitationDescription)
                        .arg(count)
                        .build());
            }
            if (count > requiredMaxOccurrencesEffective) {
                limResult.recordFailure(new LocalizableMessageBuilder()
                        .key("ValuePolicy.maximalOccurrenceExceeded")
                        .arg(requiredMaxOccurrences)
                        .arg(limitationDescription)
                        .arg(count)
                        .build());
            }
            if (mustBeFirst && !value.isEmpty() && !characterClass.characters.contains(value.charAt(0))) {
                LocalizableMessage msg = new LocalizableMessageBuilder()
                        .key("ValuePolicy.firstCharacterNotAllowed")
                        .arg(characterClass.getCharactersAsString())
                        .build();
                limResult.recordFailure(msg);
            }

            addResult(limResult);
        }

        private void testInvalidCharacters(Set<Character> validChars) {

            StringBuilder invalidCharacters = new StringBuilder();
            for (Character character : distinctCharacters) {
                if (!validChars.contains(character)) {
                    invalidCharacters.append(character);
                }
            }

            StringLimitationResult limResult = new StringLimitationResult();
            PolyStringType name = new PolyStringType("invalid characters");
            PolyStringTranslationType translation = new PolyStringTranslationType();
            translation.setKey("ValuePolicy.invalidCharacters");
            name.setTranslation(translation);
            limResult.setName(name);
            PolyStringType help = new PolyStringType(StringPolicyUtils.charactersAsString(validChars));
            limResult.setHelp(help);

            if (!invalidCharacters.isEmpty()) {
                LocalizableMessage msg = new LocalizableMessageBuilder()
                        .key("ValuePolicy.charactersNotAllowed")
                        .arg(invalidCharacters)
                        .build();
                limResult.recordFailure(msg);
            }

            addResult(limResult);
        }

        private void addResult(StringLimitationResult limResult) {
            resultList.add(limResult);
        }
    }
}
