/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.expression.evaluator;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDeltaUtil;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AsIsExpressionEvaluatorType;

/**
 * Returns value set triple of the default source. (The same behavior as <path> with empty path.)
 *
 * @author Radovan Semancik
 */
public class AsIsExpressionEvaluator<V extends PrismValue, D extends ItemDefinition<?>>
        extends AbstractExpressionEvaluator<V, D, AsIsExpressionEvaluatorType> {

    AsIsExpressionEvaluator(
            QName elementName, AsIsExpressionEvaluatorType evaluatorBean, D outputDefinition, Protector protector) {
        super(elementName, evaluatorBean, outputDefinition, protector);
    }

    @Override
    public PrismValueDeltaSetTriple<V> evaluate(ExpressionEvaluationContext context, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException {

        checkEvaluatorProfile(context);

        Source<V,D> source;
        if (context.getSources().isEmpty()) {
            throw new ExpressionEvaluationException(
                    "asIs evaluator cannot work without a source in " + context.getContextDescription());
        }
        if (context.getSources().size() > 1) {
            //noinspection unchecked
            Source<V,D> defaultSource = (Source<V,D>) context.getDefaultSource();
            if (defaultSource != null) {
                source = defaultSource;
            } else {
                throw new ExpressionEvaluationException(
                        "asIs evaluator cannot work with more than one source (%d sources specified) without specification of a default source, in %s"
                                .formatted(context.getSources().size(), context.getContextDescription()));
            }
        } else {
            //noinspection unchecked
            source = (Source<V,D>) context.getSources().iterator().next();
        }
        PrismValueDeltaSetTriple<V> outputTriple = ItemDeltaUtil.toDeltaSetTriple(source.getItemOld(), source.getDelta());

        applyValueMetadata(outputTriple, context, result);
        return finishOutputTriple(outputTriple, context.getAdditionalConvertor(), source.getResidualPath());
    }

    @Override
    public String shortDebugDump() {
        return "asIs";
    }
}
