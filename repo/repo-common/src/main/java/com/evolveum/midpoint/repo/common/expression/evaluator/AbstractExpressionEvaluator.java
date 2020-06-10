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

import org.jetbrains.annotations.NotNull;

/**
 * @param <E> evaluator bean (configuration) type
 * @author Radovan Semancik
 */
public abstract class AbstractExpressionEvaluator<V extends PrismValue, D extends ItemDefinition, E>
        implements ExpressionEvaluator<V> {

    /**
     * Qualified name of the evaluator.
     */
    @NotNull private final QName elementName;

    /**
     * Bean (i.e. configuration) for the evaluator.
     * In some cases it can be null - e.g. for implicit asIs evaluator.
     */
    protected final E expressionEvaluatorBean;

    /**
     * Definition of the output item.
     * TODO why it is important to be here?
     */
    @NotNull protected final D outputDefinition;

    protected final Protector protector;
    @NotNull protected final PrismContext prismContext;

    public AbstractExpressionEvaluator(@NotNull QName elementName, E expressionEvaluatorBean, @NotNull D outputDefinition,
            Protector protector, @NotNull PrismContext prismContext) {
        this.elementName = elementName;
        this.expressionEvaluatorBean = expressionEvaluatorBean;
        this.outputDefinition = outputDefinition;
        this.prismContext = prismContext;
        this.protector = protector;
    }

    @Override
    public @NotNull QName getElementName() {
        return elementName;
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

    public @NotNull PrismContext getPrismContext() {
        return prismContext;
    }
}
