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
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.repo.common.expression.ValuePolicyResolver;
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

/**
 * @author semancik
 *
 */
public class GenerateExpressionEvaluator<V extends PrismValue, D extends ItemDefinition> extends AbstractExpressionEvaluator<V, D, GenerateExpressionEvaluatorType> {

    public static final int DEFAULT_LENGTH = 8;

    private ObjectResolver objectResolver;
    private ValuePolicyProcessor valuePolicyGenerator;
    private ValuePolicyType elementValuePolicy;

    GenerateExpressionEvaluator(QName elementName, GenerateExpressionEvaluatorType generateEvaluatorType, D outputDefinition,
            Protector protector, ObjectResolver objectResolver, ValuePolicyProcessor valuePolicyGenerator, PrismContext prismContext) {
        super(elementName, generateEvaluatorType, outputDefinition, protector, prismContext);
        this.objectResolver = objectResolver;
        this.valuePolicyGenerator = valuePolicyGenerator;
    }

    private boolean isNotEmptyMinLength(ValuePolicyType policy) {
        StringPolicyType stringPolicy = policy.getStringPolicy();
        if (stringPolicy == null) {
            return false;
        }
        Integer minLength = stringPolicy.getLimitations().getMinLength();
        if (minLength != null) {
            if (minLength.intValue() == 0) {
                return false;
            }
            return true;
        }
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.evolveum.midpoint.common.expression.ExpressionEvaluator#evaluate(java
     * .util.Collection, java.util.Map, boolean, java.lang.String,
     * com.evolveum.midpoint.schema.result.OperationResult)
     */
    @Override
    public PrismValueDeltaSetTriple<V> evaluate(ExpressionEvaluationContext context, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        checkEvaluatorProfile(context);

        ValuePolicyType valuePolicyType = null;

        ObjectReferenceType generateEvaluatorValuePolicyRef = expressionEvaluatorBean.getValuePolicyRef();
        if (generateEvaluatorValuePolicyRef != null) {
            if (expressionEvaluatorBean.getValuePolicyRef() != null) {
                valuePolicyType = objectResolver.resolve(generateEvaluatorValuePolicyRef, ValuePolicyType.class,
                        null, "resolving value policy reference in generateExpressionEvaluator", context.getTask(), result);
            }

        }

        // if (elementStringPolicy == null) {
        // if the policy was changed, the most fresh copy is needed, therefore
        // it must be resolved all time the value is generated..if it was not
        // resolved each time, the cached policy would be used and so bad values
        // would be generated
        if (valuePolicyType == null) {
            ValuePolicyResolver valuePolicyResolver = context.getValuePolicyResolver();
            if (valuePolicyResolver != null) {
                valuePolicyType = valuePolicyResolver.resolve(result);
            }
        }

        elementValuePolicy = valuePolicyType;
        // } else {
        // stringPolicyType = elementStringPolicy;
        // }

        //
        String stringValue = null;
        GenerateExpressionEvaluatorModeType mode = expressionEvaluatorBean.getMode();
        Item<V, D> output = outputDefinition.instantiate();
        if (mode == null || mode == GenerateExpressionEvaluatorModeType.POLICY) {

            ObjectBasedValuePolicyOriginResolver<?> originResolver = getOriginResolver(context);

            // TODO: generate value based on stringPolicyType (if not null)
            if (valuePolicyType != null) {
                if (isNotEmptyMinLength(valuePolicyType)) {
                    stringValue = valuePolicyGenerator.generate(output.getPath(), valuePolicyType, DEFAULT_LENGTH, true, originResolver,
                            context.getContextDescription(), context.getTask(), result);
                } else {
                    stringValue = valuePolicyGenerator.generate(output.getPath(), valuePolicyType, DEFAULT_LENGTH, false, originResolver,
                            context.getContextDescription(), context.getTask(), result);
                }
                result.computeStatus();
                if (result.isError()) {
                    throw new ExpressionEvaluationException("Failed to generate value according to policy: "
                            + valuePolicyType.getDescription() + ". " + result.getMessage());
                }
            }

            if (stringValue == null) {
                int length = DEFAULT_LENGTH;
                RandomString randomString = new RandomString(length);
                stringValue = randomString.nextString();
            }

        } else if (mode == GenerateExpressionEvaluatorModeType.UUID) {
            UUID randomUUID = UUID.randomUUID();
            stringValue = randomUUID.toString();

        } else {
            throw new ExpressionEvaluationException("Unknown mode for generate expression: " + mode);
        }

        Object value = ExpressionUtil.convertToOutputValue(stringValue, outputDefinition, protector);


        if (output instanceof PrismProperty) {
            ((PrismProperty<Object>) output).addRealValue(value);
        } else {
            throw new UnsupportedOperationException(
                    "Can only generate values of property, not " + output.getClass());
        }

        return ItemDeltaUtil.toDeltaSetTriple(output, null, prismContext);
    }

    // determine object from the variables
    @SuppressWarnings("unchecked")
    private <O extends ObjectType> ObjectBasedValuePolicyOriginResolver<O> getOriginResolver(ExpressionEvaluationContext params) throws SchemaException {
        ExpressionVariables variables = params.getVariables();
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
        if (elementValuePolicy != null) {
            return "generate: " + elementValuePolicy;
        } else {
            return "generate";
        }
    }
}
