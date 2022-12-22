/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.evaluator.path;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.evaluator.AbstractExpressionEvaluator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * Returns value set triple derived from specified (or default) source by resolving specified path.
 *
 * @author Radovan Semancik
 */
public class PathExpressionEvaluator<V extends PrismValue, D extends ItemDefinition<?>>
        extends AbstractExpressionEvaluator<V, D, ItemPathType> {

    PathExpressionEvaluator(QName elementName, ItemPathType path, D outputDefinition, Protector protector) {
        super(elementName, path, outputDefinition, protector);
    }

    @Override
    public PrismValueDeltaSetTriple<V> evaluate(ExpressionEvaluationContext context, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException, CommunicationException {
        ExpressionUtil.checkEvaluatorProfileSimple(this, context);

        return new PathExpressionEvaluation<>(this, context)
                .evaluate(result);
    }

    public ItemPath getPath() {
        return expressionEvaluatorBean.getItemPath();
    }

    @Override
    public String shortDebugDump() {
        return "path: " + getPath();
    }
}
