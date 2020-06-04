/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.expression.evaluator;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDeltaUtil;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AsIsExpressionEvaluatorType;

/**
 * @author Radovan Semancik
 */
public class AsIsExpressionEvaluator<V extends PrismValue, D extends ItemDefinition> extends AbstractExpressionEvaluator<V,D,AsIsExpressionEvaluatorType> {

    AsIsExpressionEvaluator(QName elementName, AsIsExpressionEvaluatorType evaluatorBean, D outputDefinition, Protector protector, PrismContext prismContext) {
        super(elementName, evaluatorBean, outputDefinition, protector, prismContext);
    }

    @Override
    public PrismValueDeltaSetTriple<V> evaluate(ExpressionEvaluationContext context, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, SecurityViolationException {

        checkEvaluatorProfile(context);

        Source<V,D> source;
        if (context.getSources().isEmpty()) {
            throw new ExpressionEvaluationException("asIs evaluator cannot work without a source in "+ context.getContextDescription());
        }
        if (context.getSources().size() > 1) {
            //noinspection unchecked
            Source<V,D> defaultSource = (Source<V,D>) context.getDefaultSource();
            if (defaultSource != null) {
                source = defaultSource;
            } else {
                throw new ExpressionEvaluationException("asIs evaluator cannot work with more than one source ("+ context.getSources().size()
                    +" sources specified) without specification of a default source, in "+ context.getContextDescription());
            }
        } else {
            //noinspection unchecked
            source = (Source<V,D>) context.getSources().iterator().next();
        }
        PrismValueDeltaSetTriple<V> sourceTriple = ItemDeltaUtil.toDeltaSetTriple(source.getItemOld(), source.getDelta(),
                prismContext);

        if (sourceTriple == null) {
            return null;
        }
        return ExpressionUtil.toOutputTriple(sourceTriple, outputDefinition, context.getAdditionalConvertor(), source.getResidualPath(),
                protector, prismContext);
    }

    @Override
    public String shortDebugDump() {
        return "asIs";
    }
}
