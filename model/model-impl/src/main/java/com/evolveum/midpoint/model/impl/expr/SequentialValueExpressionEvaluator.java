/*
 * Copyright (c) 2015-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.expr;

import java.util.Collection;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.SequenceHelper;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDeltaUtil;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.evaluator.AbstractExpressionEvaluator;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SequenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SequentialValueExpressionEvaluatorType;

/**
 * Returns current value of a given sequence object. The value is returned in the zero set. Plus and minus sets are empty.
 * The value for a given sequence OID is stored in the model context, so it is returned each time this evaluator (with given
 * sequence OID) is invoked.
 *
 * @author semancik
 */
public class SequentialValueExpressionEvaluator<V extends PrismValue, D extends ItemDefinition>
        extends AbstractExpressionEvaluator<V, D, SequentialValueExpressionEvaluatorType> {

    @NotNull private final String sequenceOid;
    private final RepositoryService repositoryService;

    SequentialValueExpressionEvaluator(QName elementName, @NotNull String sequenceOid,
            SequentialValueExpressionEvaluatorType sequentialValueEvaluatorType, D outputDefinition,
            Protector protector, RepositoryService repositoryService, PrismContext prismContext) {
        super(elementName, sequentialValueEvaluatorType, outputDefinition, protector, prismContext);
        this.sequenceOid = sequenceOid;
        this.repositoryService = repositoryService;
    }

    @Override
    public PrismValueDeltaSetTriple<V> evaluate(ExpressionEvaluationContext context,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, SecurityViolationException {
        checkEvaluatorProfile(context);

        long counterValue = getSequenceCounterValue(sequenceOid, repositoryService, result);

        Object value = ExpressionUtil.convertToOutputValue(counterValue, outputDefinition, protector);
        Item<V, D> output = addValueToOutputProperty(value);

        return ItemDeltaUtil.toDeltaSetTriple(output, null, prismContext);
    }

    /**
     * Returns sequence counter value for this clockwork run.
     *
     * Because mappings are evaluated repeatedly, the value is obtained from the repository only for the first time.
     * Then it is stored in model context to be reused as needed.
     */
    static long getSequenceCounterValue(String sequenceOid, RepositoryService repositoryService, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        ModelContext<? extends FocusType> ctx = ModelExpressionThreadLocalHolder.getLensContextRequired();

        Long alreadyObtainedValue = ctx.getSequenceCounter(sequenceOid);
        if (alreadyObtainedValue != null) {
            return alreadyObtainedValue;
        } else {
            long freshValue;
            if (!isAdvanceSequenceSafe()) {
                freshValue = repositoryService.advanceSequence(sequenceOid, result);

            } else {
                Collection<SelectorOptions<GetOperationOptions>> options = GetOperationOptions.createReadOnlyCollection();
                SequenceType seq = repositoryService.getObject(SequenceType.class, sequenceOid, options, result).asObjectable();
                freshValue = SequenceHelper.advanceSequence(seq);
            }

            ctx.setSequenceCounter(sequenceOid, freshValue);

            return freshValue;
        }
    }

    private static boolean isAdvanceSequenceSafe() {
        boolean isAdvanceSequenceSafe = false;

        return isAdvanceSequenceSafe;
    }

    @NotNull
    private Item<V, D> addValueToOutputProperty(Object value) throws SchemaException {
        //noinspection unchecked
        Item<V,D> output = outputDefinition.instantiate();
        if (output instanceof PrismProperty) {
            ((PrismProperty<Object>)output).addRealValue(value);
        } else {
            throw new UnsupportedOperationException("Can only provide values of property, not "+output.getClass());
        }
        return output;
    }

    @Override
    public String shortDebugDump() {
        return "sequentialValue: "+ sequenceOid;
    }
}
