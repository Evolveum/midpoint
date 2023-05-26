/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.expression.evaluator;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import jakarta.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.StaticExpressionUtil;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDeltaUtil;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;

/**
 * Always returns zero set with literal value (values) specified in the evaluator. Plus and minus sets are empty.
 *
 * Note: using evaluatorElements as "expressionEvaluatorBean" is a bit strange. It is because I am too lazy to find more
 * appropriate name for the field. Moreover, for all other uses it really _is_ the expression evaluator bean. So leaving
 * fixing this to the future. [pmed]
 */
public class LiteralExpressionEvaluator<V extends PrismValue, D extends ItemDefinition<?>>
        extends AbstractExpressionEvaluator<V, D, Collection<JAXBElement<?>>> {

    private Item<V, D> literalItem;
    private boolean literalItemParsed;

    LiteralExpressionEvaluator(
            QName elementName, Collection<JAXBElement<?>> evaluatorElements, D outputDefinition, Protector protector) {
        super(elementName, evaluatorElements, outputDefinition, protector);
    }

    @Override
    public PrismValueDeltaSetTriple<V> evaluate(ExpressionEvaluationContext context, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException {

        ExpressionUtil.checkEvaluatorProfileSimple(this, context);

        // Cached parsed value is defensively cloned so it can be further changed.
        Item<V, D> output = CloneUtil.clone(parseLiteralItem(context));

        for (V value : output.getValues()) {
            addInternalOrigin(value, context);
        }

        PrismValueDeltaSetTriple<V> outputTriple = ItemDeltaUtil.toDeltaSetTriple(output, null);
        applyValueMetadata(outputTriple, context, result);
        return outputTriple;
    }

    private Item<V, D> parseLiteralItem(ExpressionEvaluationContext context) throws SchemaException {
        if (!literalItemParsed) {
            Function<Object, Object> additionalConvertor = context.getAdditionalConvertor();
            if (additionalConvertor == null) {
                // simple case, we use the definition as declared
                literalItem = StaticExpressionUtil.parseValueElements(
                        expressionEvaluatorBean, outputDefinition, context.getContextDescription());
            } else {
                literalItem = parseAndConvert(context);
            }
            literalItemParsed = true;
        }
        return literalItem;
    }

    private Item<V, D> parseAndConvert(ExpressionEvaluationContext context) throws SchemaException {
        //noinspection unchecked
        Item<V, D> item = outputDefinition.instantiate();
        List<Object> values = StaticExpressionUtil.parseValueElements(
                expressionEvaluatorBean, context.getContextDescription());
        Function<Object, Object> convertor = context.getAdditionalConvertor();
        for (Object value : values) {
            PrismValue prismValue = prismContext.itemFactory().createValue(convertor.apply(value));
            //noinspection unchecked
            item.add((V) prismValue);
        }
        return item;
    }

    /*
    TODO: Original questionable code that didn't support additional convertor.
     Shouldn't convertor go into makeExpression already? Is should not change for one expression like variables.
    private Item<V, D> parseLiteralItem(String contextDescription) throws SchemaException {
        if (!literalItemParsed) {
            literalItem = StaticExpressionUtil.parseValueElements(expressionEvaluatorBean, outputDefinition, contextDescription);
            literalItemParsed = true;
        }
        return literalItem;
    }
    */

    @Override
    public String shortDebugDump() {
        // TODO previously shortDebugDump called parseLiteralItem, but to get the right value
        //  it needs ExpressionEvaluationContext (corner cases like additional converters).
        if (!literalItemParsed) {
            return "literal: not-parsed-yet";
        } else if (literalItem == null || literalItem.hasNoValues()) {
            return "literal: no values";
        } else if (literalItem.size() == 1) {
            return "literal: " + literalItem.getAnyValue();
        } else {
            return "literal: " + literalItem.getValues();
        }
    }
}
