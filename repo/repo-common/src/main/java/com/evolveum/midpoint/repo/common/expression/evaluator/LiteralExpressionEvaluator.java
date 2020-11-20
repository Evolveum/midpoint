/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.expression.evaluator;

import java.util.Collection;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.StaticExpressionUtil;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
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
public class LiteralExpressionEvaluator<V extends PrismValue,D extends ItemDefinition>
        extends AbstractExpressionEvaluator<V, D, Collection<JAXBElement<?>>> {

    private Item<V, D> literalItem;
    private boolean literalItemParsed;

    LiteralExpressionEvaluator(QName elementName, Collection<JAXBElement<?>> evaluatorElements,
            D outputDefinition, Protector protector, PrismContext prismContext) {
        super(elementName, evaluatorElements, outputDefinition, protector, prismContext);
    }

    @Override
    public PrismValueDeltaSetTriple<V> evaluate(ExpressionEvaluationContext context, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException {

        ExpressionUtil.checkEvaluatorProfileSimple(this, context);

        Item<V,D> output = CloneUtil.clone(parseLiteralItem(context.getContextDescription()));

        for (V value : output.getValues()) {
            addInternalOrigin(value, context);
        }

        PrismValueDeltaSetTriple<V> outputTriple = ItemDeltaUtil.toDeltaSetTriple(output, null, prismContext);
        applyValueMetadata(outputTriple, context, result);
        return outputTriple;
    }

    private Item<V, D> parseLiteralItem(String contextDescription) throws SchemaException {
        if (!literalItemParsed) {
            literalItem = StaticExpressionUtil.parseValueElements(expressionEvaluatorBean, outputDefinition, contextDescription);
            literalItemParsed = true;
        }
        return literalItem;
    }

    @Override
    public String shortDebugDump() {
        try {
            return "literal: " + shortDebugDump(parseLiteralItem("shortDebugDump"));
        } catch (SchemaException e) {
            return "literal: couldn't parse: " + expressionEvaluatorBean;
        }
    }

    private String shortDebugDump(Item<V, D> item) {
        if (item == null || item.hasNoValues()) {
            return "no values";
        } else if (item.size() == 1) {
            return String.valueOf(item.getAnyValue());
        } else {
            return String.valueOf(item.getValues());
        }
    }
}
