/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.stringpolicy;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.validator.StringLimitationResult;
import com.evolveum.midpoint.model.common.ModelCommonBeans;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageList;
import com.evolveum.midpoint.util.LocalizableMessageListBuilder;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProhibitedValuesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StringPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;

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

    private static final String DOT_CLASS = ValuePolicyProcessor.class.getName() + ".";
    private static final String OPERATION_STRING_POLICY_VALIDATION = DOT_CLASS + "stringPolicyValidation";

    @Autowired private ModelCommonBeans beans;
    @Autowired private ExpressionFactory expressionFactory;
    @Autowired private Protector protector;

    public ExpressionFactory getExpressionFactory() {
        return expressionFactory;
    }

    public ValuePolicyProcessor() {
    }

    @VisibleForTesting
    public ValuePolicyProcessor(ExpressionFactory expressionFactory) {
        this.expressionFactory = expressionFactory;
        this.protector = PrismContext.get().getDefaultProtector();
    }

    /**
     * Generates a value meeting the policy and/or the default length
     * (which is used when no minimal length nor minimal unique characters are specified).
     */
    public String generate(
            ItemPath path,
            ValuePolicyType valuePolicy,
            int defaultLength,
            ObjectBasedValuePolicyOriginResolver<?> originResolver,
            String shortDesc,
            Task task,
            OperationResult parentResult)
            throws ExpressionEvaluationException, SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        OperationResult result = parentResult.createSubresult(OP_GENERATE);
        try {

            var stringPolicy = getCompiledStringPolicy(valuePolicy);
            var generator = new ValueGenerator(stringPolicy, defaultLength);
            var checker = getValueChecker(stringPolicy, valuePolicy, originResolver, shortDesc, task);

            int maxAttempts = stringPolicy.getMaxGenerationAttempts();
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                String generatedValue;
                try {
                    generatedValue = generator.generate();
                } catch (ValueGenerator.GenerationException e) {
                    throw new ExpressionEvaluationException(e.getMessage(), e);
                }
                if (checker.checkExpressionsAndProhibitions(generatedValue, result)) {
                    return generatedValue;
                }
                LOGGER.trace("Generator attempt {} of {}: check failed", attempt, maxAttempts);
            }
            ItemPath targetItemPath = Objects.requireNonNullElse(path, SchemaConstants.PATH_PASSWORD_VALUE);
            throw new ExpressionEvaluationException(
                    "Unable to generate value for %s, maximum number of attempts (%d) exceeded".formatted(
                            targetItemPath, maxAttempts));
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    private static @NotNull StringPolicy getCompiledStringPolicy(@Nullable ValuePolicyType valuePolicy)
            throws ConfigurationException {
        StringPolicyType stringPolicyBean = valuePolicy != null ? valuePolicy.getStringPolicy() : null;
        var stringPolicyConfigItem = ConfigurationItem.embeddedNullable(stringPolicyBean);
        return StringPolicy.compile(stringPolicyConfigItem);
    }

    @NotNull
    private ValueChecker getValueChecker(
            StringPolicy stringPolicy, ValuePolicyType valuePolicy,
            ObjectBasedValuePolicyOriginResolver<?> originResolver,
            String shortDesc, Task task) {
        ProhibitedValuesType prohibitedValues = valuePolicy != null ? valuePolicy.getProhibitedValues() : null;
        // TODO: this needs to be determined from ValuePolicyType archetype
        ExpressionProfile expressionProfile = MiscSchemaUtil.getExpressionProfile();
        return new ValueChecker(
                stringPolicy, prohibitedValues, expressionProfile, originResolver, shortDesc, protector, expressionFactory, task);
    }

    public List<StringLimitationResult> validateValue(
            String newValue,
            ValuePolicyType valuePolicy,
            ObjectBasedValuePolicyOriginResolver<?> originResolver,
            String shortDesc,
            Task task,
            OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        //TODO: do we want to throw exception when no value policy defined??
        Validate.notNull(valuePolicy, "Value policy must not be null.");

        OperationResult result = parentResult.createSubresult(OPERATION_STRING_POLICY_VALIDATION);
        result.addArbitraryObjectAsParam("policy", valuePolicy);
        try {
            var stringPolicy = getCompiledStringPolicy(valuePolicy);
            var checker = getValueChecker(stringPolicy, valuePolicy, originResolver, shortDesc, task);

            List<StringLimitationResult> limResults = checker.checkFully(emptyIfNull(newValue), result);
            putResultsIntoOperationResult(result, limResults);
            return limResults;

        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    private void putResultsIntoOperationResult(OperationResult result, List<StringLimitationResult> limResults) {
        List<LocalizableMessage> allFailureMessages = new ArrayList<>();
        for (StringLimitationResult limResult : limResults) {
            if (limResult.isSuccess()) {
                continue;
            }
            var opName = beans.localizationService.translate(limResult.getName().toPolyString());
            for (LocalizableMessage message : limResult.getMessages()) {
                // There should be at least one message
                result.addSubresult(
                        new OperationResult(
                                opName, // TODO create separate keys for this? (operation names should not be free texts)
                                OperationResultStatus.FATAL_ERROR,
                                message));
                allFailureMessages.add(message);
            }
        }
        if (!allFailureMessages.isEmpty()) {
            result.setUserFriendlyMessage(
                    new LocalizableMessageListBuilder()
                            .messages(allFailureMessages)
                            .separator(LocalizableMessageList.SPACE)
                            .buildOptimized());
        }
    }
}
