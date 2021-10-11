/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.evaluator;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDeltaUtil;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IntegerStatType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProportionalExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProportionalStyleType;

/**
 * @author skublik
 *
 */
public class ProportionalExpressionEvaluator<V extends PrismValue, D extends ItemDefinition>
        implements ExpressionEvaluator<V, D> {

    private ProportionalExpressionEvaluatorType proportionalEvaluatorType;
    private D outputDefinition;
    private PrismContext prismContext;

    ProportionalExpressionEvaluator(ProportionalExpressionEvaluatorType proportionalEvaluatorType, D outputDefinition, PrismContext prismContext) {
        this.proportionalEvaluatorType = proportionalEvaluatorType;
        this.outputDefinition = outputDefinition;
        this.prismContext = prismContext;
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
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {

        ProportionalStyleType style = proportionalEvaluatorType.getStyle();

        IntegerStatType integerStatType = context.getVariables().getValue(ExpressionConstants.VAR_INPUT, IntegerStatType.class);
        if(integerStatType == null) {
            throw new IllegalArgumentException("Proportional expression cannot by evaluated without input of type "
                    + IntegerStatType.COMPLEX_TYPE);
        }
        String numbermessage = "";
        Integer totalItems = integerStatType.getDomain();
        Integer actualItems = integerStatType.getValue();

        switch(style) {

            case PERCENTAGE:
                validateInputNumbers(totalItems, actualItems, ProportionalStyleType.PERCENTAGE);

                float percentage = (totalItems==0 ? 0 : actualItems*100.0f/totalItems);
                String format = "%.0f";

                if(percentage < 100.0f && percentage % 10 != 0 && ((percentage % 10) % 1) != 0) {
                    format = "%.1f";
                }
                numbermessage = String.format(format, percentage) + " %";
                integerStatType.setPercentage(percentage);
                break;
            case VALUE_OF_DOMAIN:
                validateInputNumbers(totalItems, actualItems, ProportionalStyleType.VALUE_OF_DOMAIN);

                numbermessage = String.valueOf(actualItems) + " of " + String.valueOf(totalItems);
                break;
            case VALUE_SLASH_DOMAIN:
                validateInputNumbers(totalItems, actualItems, ProportionalStyleType.VALUE_SLASH_DOMAIN);

                numbermessage = String.valueOf(actualItems) + "/" + String.valueOf(totalItems);
                break;
            case VALUE_ONLY:
                if(actualItems == null) {
                    throw new IllegalArgumentException("Proportional expression with " + ProportionalStyleType.VALUE_ONLY.value()
                    +" style cannot by evaluated without value and domain numbers in input of type " + IntegerStatType.COMPLEX_TYPE);
                }
                numbermessage = String.valueOf(actualItems);
                break;
        }

        Item<V, D> output = outputDefinition.instantiate();
        if (output instanceof PrismProperty) {
            ((PrismProperty<String>) output).addRealValue(numbermessage);
        } else {
            throw new UnsupportedOperationException(
                    "Can only generate values of property, not " + output.getClass());
        }

        return ItemDeltaUtil.toDeltaSetTriple(output, null, prismContext);
    }



    private void validateInputNumbers(Integer totalItems, Integer actualItems, ProportionalStyleType style) {
        if(totalItems == null || actualItems == null) {
            throw new IllegalArgumentException("Proportional expression with " + style.value() +" style cannot by evaluated"
                    + " without value and domain numbers in input of type " + IntegerStatType.COMPLEX_TYPE);
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#
     * shortDebugDump()
     */
    @Override
    public String shortDebugDump() {
        return "const:"+proportionalEvaluatorType.getStyle();
    }

    @Override
    public QName getElementName() {
        return IntegerStatType.COMPLEX_TYPE;
    }


}
