/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.expression;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

import java.util.Collection;
import java.util.function.Function;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.schema.expression.ExpressionEvaluatorProfile;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.task.api.Task;

/**
 * Simple almost-DTO used to contain all the parameters of expression evaluation.
 *
 * Designed to allow future compatible changes (addition of optional parameters).
 *
 * @author semancik
 */
public class ExpressionEvaluationContext {

    /**
     * Sources for expression evaluation. Can be empty.
     */
    @NotNull private final Collection<Source<?,?>> sources;

    /**
     * One of the sources can be denoted as "default".
     * Interpretation of this information is evaluator-specific.
     */
    private Source<?,?> defaultSource;

    /**
     * Additional variables to be used during the evaluation.
     * May contain item-delta-item objects.
     */
    private ExpressionVariables variables;

    /**
     * Should we skip evaluation to the plus set?
     */
    private boolean skipEvaluationPlus = false;

    /**
     * Should we skip evaluation to the minus set? This can be useful e.g. for delta-less evaluation of conditions.
     */
    private boolean skipEvaluationMinus = false;

    /**
     * Provider of value policy. Currently used in GenerateExpressionEvaluator.
     */
    private ValuePolicyResolver valuePolicyResolver;

    /**
     * Factory for expressions. Necessary e.g. for condition evaluation in transformational evaluators.
     */
    private ExpressionFactory expressionFactory;

    /**
     * Purpose of this field is unknown. TODO Consider removal.
     */
    private PrismObjectDefinition<?> defaultTargetContext;

    /**
     * Purpose of this field is unknown. TODO Consider removal.
     */
    private RefinedObjectClassDefinition refinedObjectClassDefinition;

    /**
     * Yet another field with unclear meaning. Seems to be used as an association name. TODO Clarify.
     */
    private QName mappingQName;

    /**
     * Free-form context description for diagnostic purposes.
     */
    private final String contextDescription;

    /**
     * Task under which the evaluation is being carried out.
     */
    private final Task task;

    /**
     * Converts expression output to expected form. E.g. if object reference is required but
     * the script provided a plain OID. Or if URI is required but enum value or QName was provided.
     * See commit a395bb1572181b95679df6cdacd62384fb5ba480.
     *
     * TODO reconsider if we really need this.
     */
    private Function<Object, Object> additionalConvertor;

    /**
     * If present, this one is used to produce extra variables for given value.
     *
     * The concept of value producer is a bit imprecise as it is currently called for each value
     * in value tuple (see CombinatorialEvaluation and TupleEvaluation). So it works well only
     * if single-source expression are evaluated.
     */
    private VariableProducer<?> variableProducer;

    /**
     * Optional. If not specified then it will be added at the start of expression evaluation.
     * Might be used to override the profile specified in Expression.
     */
    private ExpressionProfile expressionProfile;

    /**
     * Evaluator profile for specific expression evaluator in question. It is computed
     * on the start of expression evaluation.
     */
    private ExpressionEvaluatorProfile expressionEvaluatorProfile;

    public ExpressionEvaluationContext(Collection<Source<?,?>> sources,
            ExpressionVariables variables, String contextDescription, Task task) {
        super();
        this.sources = emptyIfNull(sources);
        this.variables = variables;
        this.contextDescription = contextDescription;
        this.task = task;
    }

    @NotNull
    public Collection<Source<?,?>> getSources() {
        return sources;
    }

    public Source<?,?> getDefaultSource() {
        return defaultSource;
    }

    public void setDefaultSource(Source<?,?> defaultSource) {
        this.defaultSource = defaultSource;
    }

    public ExpressionVariables getVariables() {
        return variables;
    }

    public void setVariables(ExpressionVariables variables) {
        this.variables = variables;
    }

    public boolean isSkipEvaluationPlus() {
        return skipEvaluationPlus;
    }

    public void setSkipEvaluationPlus(boolean skipEvaluationPlus) {
        this.skipEvaluationPlus = skipEvaluationPlus;
    }

    public boolean isSkipEvaluationMinus() {
        return skipEvaluationMinus;
    }

    public void setSkipEvaluationMinus(boolean skipEvaluationMinus) {
        this.skipEvaluationMinus = skipEvaluationMinus;
    }

    public ExpressionProfile getExpressionProfile() {
        return expressionProfile;
    }

    public void setExpressionProfile(ExpressionProfile expressionProfile) {
        this.expressionProfile = expressionProfile;
    }

    ExpressionEvaluatorProfile getExpressionEvaluatorProfile() {
        return expressionEvaluatorProfile;
    }

    void setExpressionEvaluatorProfile(ExpressionEvaluatorProfile expressionEvaluatorProfile) {
        this.expressionEvaluatorProfile = expressionEvaluatorProfile;
    }

    public ValuePolicyResolver getValuePolicyResolver() {
        return valuePolicyResolver;
    }

    public void setValuePolicyResolver(ValuePolicyResolver valuePolicyResolver) {
        this.valuePolicyResolver = valuePolicyResolver;
    }

    public ExpressionFactory getExpressionFactory() {
        return expressionFactory;
    }

    public void setExpressionFactory(ExpressionFactory expressionFactory) {
        this.expressionFactory = expressionFactory;
    }

    public PrismObjectDefinition<?> getDefaultTargetContext() {
        return defaultTargetContext;
    }

    public void setDefaultTargetContext(PrismObjectDefinition<?> defaultTargetContext) {
        this.defaultTargetContext = defaultTargetContext;
    }

    public void setRefinedObjectClassDefinition(RefinedObjectClassDefinition refinedObjectClassDefinition) {
        this.refinedObjectClassDefinition = refinedObjectClassDefinition;
    }

    public QName getMappingQName() {
        return mappingQName;
    }

    public void setMappingQName(QName mappingQName) {
        this.mappingQName = mappingQName;
    }

    public String getContextDescription() {
        return contextDescription;
    }

    public Task getTask() {
        return task;
    }

    public Function<Object, Object> getAdditionalConvertor() {
        return additionalConvertor;
    }

    public void setAdditionalConvertor(Function<Object, Object> additionalConvertor) {
        this.additionalConvertor = additionalConvertor;
    }

    public VariableProducer<?> getVariableProducer() {
        return variableProducer;
    }

    public void setVariableProducer(VariableProducer<?> variableProducer) {
        this.variableProducer = variableProducer;
    }

    public boolean hasDeltas() {
        return hasDeltas(sources) || variables != null && variables.haveDeltas();
    }

    private boolean hasDeltas(Collection<Source<?,?>> sources) {
        for (Source<?,?> source: sources) {
            if (source.getDelta() != null && !source.getDelta().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public ExpressionEvaluationContext shallowClone() {
        ExpressionEvaluationContext clone = new ExpressionEvaluationContext(sources, variables, contextDescription, task);
        clone.skipEvaluationMinus = this.skipEvaluationMinus;
        clone.skipEvaluationPlus = this.skipEvaluationPlus;
        clone.expressionProfile = this.expressionProfile;
        clone.valuePolicyResolver = this.valuePolicyResolver;
        clone.expressionFactory = this.expressionFactory;
        clone.defaultSource = this.defaultSource;
        clone.refinedObjectClassDefinition = this.refinedObjectClassDefinition;
        clone.mappingQName = this.mappingQName;
        clone.additionalConvertor = this.additionalConvertor;
        clone.variableProducer = this.variableProducer;
        return clone;
    }
}
