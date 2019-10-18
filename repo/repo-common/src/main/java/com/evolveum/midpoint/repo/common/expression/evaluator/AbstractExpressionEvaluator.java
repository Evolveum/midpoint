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
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.util.exception.SecurityViolationException;

/**
 * @param <E> evaluator prism type
 * @author Radovan Semancik
 */
public abstract class AbstractExpressionEvaluator<V extends PrismValue, D extends ItemDefinition, E> implements ExpressionEvaluator<V,D> {

    private final QName elementName;
    private final E expressionEvaluatorType;
    protected final PrismContext prismContext;
    protected final D outputDefinition;
    protected final Protector protector;

    public AbstractExpressionEvaluator(QName elementName, E expressionEvaluatorType, D outputDefinition, Protector protector, PrismContext prismContext) {
        this.elementName = elementName;
        this.expressionEvaluatorType = expressionEvaluatorType;
        this.outputDefinition = outputDefinition;
        this.prismContext = prismContext;
        this.protector = protector;
    }

    @Override
    public QName getElementName() {
        return elementName;
    }

    protected E getExpressionEvaluatorType() {
        return expressionEvaluatorType;
    }

    protected PrismContext getPrismContext() {
        return prismContext;
    }

    protected D getOutputDefinition() {
        return outputDefinition;
    }

    protected Protector getProtector() {
        return protector;
    }

    /**
     * Check expression profile. Throws security exception if the execution is not allowed by the profile.
     *
     * This implementation works only for simple evaluators that do not have any profile settings.
     * Complex evaluators should override this method.
     *
     * @throws SecurityViolationException expression execution is not allowed by the profile.
     */
    protected void checkEvaluatorProfile(ExpressionEvaluationContext context) throws SecurityViolationException {
        ExpressionUtil.checkEvaluatorProfileSimple(this, context);
    }

}
