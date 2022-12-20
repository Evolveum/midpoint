/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.evaluator;

import java.util.UUID;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.common.stringpolicy.*;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDeltaUtil;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.repo.common.expression.ValuePolicySupplier;
import com.evolveum.midpoint.repo.common.expression.evaluator.AbstractExpressionEvaluator;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.RandomString;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.GenerateExpressionEvaluatorModeType.POLICY;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * Generates a string value based on given value policy. Puts it into zero set. Plus and minus sets are empty.
 *
 * @author semancik
 */
public class GenerateExpressionEvaluator<V extends PrismValue, D extends ItemDefinition<?>>
        extends AbstractExpressionEvaluator<V, D, GenerateExpressionEvaluatorType> {

    public static final int DEFAULT_LENGTH = 8;

    private final ObjectResolver objectResolver;
    private final ValuePolicyProcessor valuePolicyProcessor;

    GenerateExpressionEvaluator(
            QName elementName,
            GenerateExpressionEvaluatorType generateEvaluatorBean,
            D outputDefinition,
            Protector protector,
            ObjectResolver objectResolver,
            ValuePolicyProcessor valuePolicyProcessor) {
        super(elementName, generateEvaluatorBean, outputDefinition, protector);
        this.objectResolver = objectResolver;
        this.valuePolicyProcessor = valuePolicyProcessor;
    }

    @Override
    public PrismValueDeltaSetTriple<V> evaluate(ExpressionEvaluationContext context, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        checkEvaluatorProfile(context);

        ValuePolicyType valuePolicy = getValuePolicy(context, result);

        //noinspection unchecked
        Item<V, D> output = outputDefinition.instantiate();
        ItemPath outputPath = output.getPath(); // actually, a name only

        String stringValue = generateStringValue(valuePolicy, context, outputPath, result);
        addValueToOutputProperty(output, stringValue, context);

        PrismValueDeltaSetTriple<V> outputTriple = ItemDeltaUtil.toDeltaSetTriple(output, null);
        applyValueMetadata(outputTriple, context, result);
        return outputTriple;
    }

    @NotNull
    private String generateStringValue(ValuePolicyType valuePolicy, ExpressionEvaluationContext context, ItemPath outputPath,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        GenerateExpressionEvaluatorModeType mode = defaultIfNull(expressionEvaluatorBean.getMode(), POLICY);
        switch (mode) {
            case POLICY:
                // TODO: generate value based on stringPolicyType (if not null)
                if (valuePolicy != null) {
                    String generatedStringValue = generateStringValueFromPolicy(valuePolicy, context, outputPath, result);
                    if (generatedStringValue != null)
                        return generatedStringValue;
                }
                return new RandomString(DEFAULT_LENGTH).nextString();
            case UUID:
                return UUID.randomUUID().toString();
            default:
                throw new ExpressionEvaluationException("Unknown mode for generate expression: " + mode);
        }
    }

    private void addValueToOutputProperty(Item<V, D> output, String stringValue, ExpressionEvaluationContext context)
            throws ExpressionEvaluationException,
            SchemaException {
        if (output instanceof PrismProperty) {
            Object realValue = ExpressionUtil.convertToOutputValue(stringValue, outputDefinition, protector);
            if (realValue != null) {
                PrismPropertyValue<Object> prismValue = prismContext.itemFactory().createPropertyValue(realValue);
                addInternalOrigin(prismValue, context);
                ((PrismProperty<Object>) output).add(prismValue);
            }
        } else {
            throw new UnsupportedOperationException(
                    "Can only generate values of property, not " + output.getClass());
        }
    }

    @Nullable
    private String generateStringValueFromPolicy(ValuePolicyType valuePolicy, ExpressionEvaluationContext context,
            ItemPath outputPath, OperationResult result)
            throws ExpressionEvaluationException, SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        ObjectBasedValuePolicyOriginResolver<?> originResolver = getOriginResolver(context);
        String generatedValue;
        if (isNotEmptyMinLength(valuePolicy)) {
            generatedValue = valuePolicyProcessor.generate(outputPath, valuePolicy, DEFAULT_LENGTH, true, originResolver,
                    context.getContextDescription(), context.getTask(), result);
        } else {
            generatedValue = valuePolicyProcessor.generate(outputPath, valuePolicy, DEFAULT_LENGTH, false, originResolver,
                    context.getContextDescription(), context.getTask(), result);
        }
        result.computeStatus();
        if (result.isError()) {
            throw new ExpressionEvaluationException("Failed to generate value according to policy: "
                    + valuePolicy.getDescription() + ". " + result.getMessage());
        }
        return generatedValue;
    }

    private boolean isNotEmptyMinLength(ValuePolicyType valuePolicy) {
        StringPolicyType stringPolicy = valuePolicy.getStringPolicy();
        if (stringPolicy == null) {
            return false;
        }
        Integer minLength = stringPolicy.getLimitations().getMinLength();
        return minLength != null && minLength != 0;
    }

    @Nullable
    private ValuePolicyType getValuePolicy(ExpressionEvaluationContext context, OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        ObjectReferenceType specifiedValuePolicyRef = expressionEvaluatorBean.getValuePolicyRef();
        if (specifiedValuePolicyRef != null) {
            ValuePolicyType resolvedPolicy = objectResolver.resolve(specifiedValuePolicyRef, ValuePolicyType.class,
                    null, "resolving value policy reference in generateExpressionEvaluator", context.getTask(), result);
            if (resolvedPolicy != null) {
                return resolvedPolicy;
            }
        }

        ValuePolicySupplier valuePolicySupplier = context.getValuePolicySupplier();
        if (valuePolicySupplier != null) {
            return valuePolicySupplier.get(result);
        }

        return null;
    }

    // determine object from the variables
    @SuppressWarnings("unchecked")
    private <O extends ObjectType> ObjectBasedValuePolicyOriginResolver<O> getOriginResolver(ExpressionEvaluationContext context) throws SchemaException {
        VariablesMap variables = context.getVariables();
        if (variables == null) {
            return null;
        }
        PrismObject<O> object = variables.getValueNew(ExpressionConstants.VAR_PROJECTION);
        if (object != null) {
            return (ObjectBasedValuePolicyOriginResolver<O>) new ShadowValuePolicyOriginResolver((PrismObject<ShadowType>) object, objectResolver);
        }
        object = variables.getValueNew(ExpressionConstants.VAR_FOCUS);
        return (ObjectBasedValuePolicyOriginResolver<O>) new FocusValuePolicyOriginResolver<>((PrismObject<FocusType>) object, objectResolver);
    }

    @Override
    public String shortDebugDump() {
        // Here used to be reference to the value policy. However, although expression evaluators are usually not re-evaluated,
        // it is not always the case. So the evaluator should not keep state related to evaluation itself.
        if (expressionEvaluatorBean.getValuePolicyRef() != null) {
            return "generate:" + expressionEvaluatorBean.getValuePolicyRef();
        } else {
            return "generate";
        }
    }
}
