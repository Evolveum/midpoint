/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.evaluator.path;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;

/**
 * Returns value set triple derived from specified (or default) source by resolving specified path.
 *
 * @author Radovan Semancik
 */
public class PathExpressionEvaluator<V extends PrismValue, D extends ItemDefinition> implements ExpressionEvaluator<V> {

    private final QName elementName;
    final ItemPath path;
    final PrismContext prismContext;
    final D outputDefinition;
    final Protector protector;

    PathExpressionEvaluator(QName elementName, ItemPath path, D outputDefinition, Protector protector,
            PrismContext prismContext) {
        this.elementName = elementName;
        this.path = path;
        this.outputDefinition = outputDefinition;
        this.prismContext = prismContext;
        this.protector = protector;
    }

    @Override
    public QName getElementName() {
        return elementName;
    }

    @Override
    public PrismValueDeltaSetTriple<V> evaluate(ExpressionEvaluationContext context, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException {
        ExpressionUtil.checkEvaluatorProfileSimple(this, context);

        return new PathExpressionEvaluation<>(this, context)
                .evaluate();
    }

    @Override
    public String shortDebugDump() {
        return "path: "+path;
    }
}
