/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.expression;

import java.util.Collection;
import java.util.function.Function;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.schema.expression.ExpressionEvaluatorProfile;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

/**
 * Simple DTO used to contain all the parameters of expression execution.
 *
 * Designed to allow future compatible changes (addition of optional parameters).
 *
 * @author semancik
 *
 */
public class ExpressionEvaluationContext {

    private Collection<Source<?,?>> sources;
    private Source<?,?> defaultSource;
    private ExpressionVariables variables;
    private boolean skipEvaluationPlus = false;
    private boolean skipEvaluationMinus = false;
    private ValuePolicyResolver valuePolicyResolver;
    private ExpressionFactory expressionFactory;
    private PrismObjectDefinition<?> defaultTargetContext;
    private RefinedObjectClassDefinition refinedObjectClassDefinition;
    private QName mappingQName;
    private String contextDescription;
    private Task task;
    private Function<Object, Object> additionalConvertor;
    private VariableProducer variableProducer;

    /**
     * Optional. If not specified then it will be added at the star of evaluation.
     * Might be used to override the profile.
     */
    private ExpressionProfile expressionProfile;
    private ExpressionEvaluatorProfile expressionEvaluatorProfile;

    public ExpressionEvaluationContext(Collection<Source<?,?>> sources,
            ExpressionVariables variables, String contextDescription, Task task) {
        super();
        this.sources = sources;
        this.variables = variables;
        this.contextDescription = contextDescription;
        this.task = task;
    }

    public Collection<Source<?,?>> getSources() {
        return sources;
    }

    public void setSources(Collection<Source<?,?>> sources) {
        this.sources = sources;
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

    public ExpressionEvaluatorProfile getExpressionEvaluatorProfile() {
        return expressionEvaluatorProfile;
    }

    public void setExpressionEvaluatorProfile(ExpressionEvaluatorProfile expressionEvaluatorProfile) {
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

    public RefinedObjectClassDefinition getRefinedObjectClassDefinition() {
        return refinedObjectClassDefinition;
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

    public void setContextDescription(String contextDescription) {
        this.contextDescription = contextDescription;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public Function<Object, Object> getAdditionalConvertor() {
        return additionalConvertor;
    }

    public void setAdditionalConvertor(Function<Object, Object> additionalConvertor) {
        this.additionalConvertor = additionalConvertor;
    }

    public VariableProducer getVariableProducer() {
        return variableProducer;
    }

    public void setVariableProducer(VariableProducer variableProducer) {
        this.variableProducer = variableProducer;
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
