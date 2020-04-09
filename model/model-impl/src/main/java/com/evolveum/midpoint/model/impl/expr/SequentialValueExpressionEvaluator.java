/*
 * Copyright (c) 2015-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.expr;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDeltaUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.evaluator.AbstractExpressionEvaluator;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SequentialValueExpressionEvaluatorType;

/**
 * @author semancik
 *
 */
public class SequentialValueExpressionEvaluator<V extends PrismValue, D extends ItemDefinition> extends AbstractExpressionEvaluator<V, D, SequentialValueExpressionEvaluatorType> {

    RepositoryService repositoryService;

    SequentialValueExpressionEvaluator(QName elementName, SequentialValueExpressionEvaluatorType sequentialValueEvaluatorType,
            D outputDefinition, Protector protector, RepositoryService repositoryService, PrismContext prismContext) {
        super(elementName, sequentialValueEvaluatorType, outputDefinition, protector, prismContext);
        this.repositoryService = repositoryService;
    }

    @Override
    public PrismValueDeltaSetTriple<V> evaluate(ExpressionEvaluationContext context,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, SecurityViolationException {
        checkEvaluatorProfile(context);

        long counter = getSequenceCounter(getExpressionEvaluatorType().getSequenceRef().getOid(), repositoryService, result);

        Object value = ExpressionUtil.convertToOutputValue(counter, outputDefinition, protector);

        Item<V,D> output = outputDefinition.instantiate();
        if (output instanceof PrismProperty) {
            ((PrismProperty<Object>)output).addRealValue(value);
        } else {
            throw new UnsupportedOperationException("Can only generate values of property, not "+output.getClass());
        }

        return ItemDeltaUtil.toDeltaSetTriple(output, null, prismContext);
    }

    public static long getSequenceCounter(String sequenceOid, RepositoryService repositoryService, OperationResult result) throws ObjectNotFoundException, SchemaException {
        ModelContext<? extends FocusType> ctx = ModelExpressionThreadLocalHolder.getLensContext();
        if (ctx == null) {
            throw new IllegalStateException("No lens context");
        }

        Long counter = ctx.getSequenceCounter(sequenceOid);
        if (counter == null) {
            counter = repositoryService.advanceSequence(sequenceOid, result);
            ctx.setSequenceCounter(sequenceOid, counter);
        }

        return counter;
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#shortDebugDump()
     */
    @Override
    public String shortDebugDump() {
        return "squentialValue: "+getExpressionEvaluatorType().getSequenceRef().getOid();
    }

}
