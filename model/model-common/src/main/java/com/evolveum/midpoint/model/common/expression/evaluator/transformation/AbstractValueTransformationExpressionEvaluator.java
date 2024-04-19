/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.evaluator.transformation;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import static com.evolveum.midpoint.util.DebugUtil.debugDumpLazily;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TransformExpressionRelativityModeType.ABSOLUTE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TransformExpressionRelativityModeType.RELATIVE;

import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.common.ModelCommonBeans;
import com.evolveum.midpoint.prism.crypto.Protector;

import org.apache.commons.lang3.BooleanUtils;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.evaluator.AbstractExpressionEvaluator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TransformExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TransformExpressionRelativityModeType;

import org.jetbrains.annotations.NotNull;

/**
 * Evaluates transformational expression: one that transforms input values to output values.
 *
 * Actually, the hard work is delegated to RelativisticEvaluation and AbsoluteEvaluation classes.
 *
 * @param <V> Type of output PrismValues.
 * @param <D> Definition of output values.
 * @param <E> Type of the configuration (evaluator) bean.
 *
 * @author Radovan Semancik
 */
public abstract class AbstractValueTransformationExpressionEvaluator
        <V extends PrismValue, D extends ItemDefinition<?>, E extends TransformExpressionEvaluatorType>
        extends AbstractExpressionEvaluator<V, D, E> {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractValueTransformationExpressionEvaluator.class);

    private static final String OP_EVALUATE = AbstractValueTransformationExpressionEvaluator.class.getName() + ".evaluate";

    /** May be `null` in some low-level tests where {@link ModelCommonBeans} are initialized manually. */
    protected final SecurityContextManager securityContextManager = ModelCommonBeans.get().securityContextManager;

    protected final LocalizationService localizationService;

    protected AbstractValueTransformationExpressionEvaluator(
            QName elementName,
            E expressionEvaluatorType,
            D outputDefinition,
            Protector protector,
            LocalizationService localizationService) {
        super(elementName, expressionEvaluatorType, outputDefinition, protector);
        this.localizationService = localizationService;
    }

    @Override
    public PrismValueDeltaSetTriple<V> evaluate(ExpressionEvaluationContext context, OperationResult parentResult) throws SchemaException,
            ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        OperationResult result = parentResult.subresult(OP_EVALUATE)
                .setMinor()
                .addContext("context", context.getContextDescription())
                .build();
        // trace is provided by the evaluators

        try {
            checkEvaluatorProfile(context);

            PrismValueDeltaSetTriple<V> outputTriple;

            String contextDescription = context.getContextDescription();
            TransformExpressionRelativityModeType relativityMode = defaultIfNull(expressionEvaluatorBean.getRelativityMode(), RELATIVE);
            switch (relativityMode) {
                case ABSOLUTE -> {
                    outputTriple = new SingleShotEvaluation<>(context, result, this).evaluate();
                    LOGGER.trace("Evaluated absolute expression {}, output triple:\n{}",
                            contextDescription, debugDumpLazily(outputTriple, 1));
                }
                case RELATIVE -> {
                    if (context.getSources().isEmpty()) {
                        // Special case. No sources, so there will be no input variables and no combinations. Everything goes to zero set.
                        outputTriple = new SingleShotEvaluation<>(context, result, this).evaluate();
                        LOGGER.trace("Evaluated relative sourceless expression {}, output triple:\n{}",
                                contextDescription, debugDumpLazily(outputTriple, 1));
                    } else {
                        outputTriple = new CombinatorialEvaluation<>(context, result, this).evaluate();
                        LOGGER.trace("Evaluated relative expression {}, output triple:\n{}",
                                contextDescription, debugDumpLazily(outputTriple, 1));
                    }
                }
                default -> throw new AssertionError(relativityMode);
            }
            return outputTriple;

        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    protected boolean isIncludeNullInputs() {
        return BooleanUtils.isNotFalse(expressionEvaluatorBean.isIncludeNullInputs());
    }

    protected boolean isRelative() {
        return expressionEvaluatorBean.getRelativityMode() != ABSOLUTE;
    }

    E getExpressionEvaluatorBean() {
        return expressionEvaluatorBean;
    }

    /** Transforms single value or single values tuple. */
    @NotNull
    protected abstract List<V> transformSingleValue(
            @NotNull ValueTransformationContext valueTransformationContext,
            @NotNull OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException;
}
