/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.scripting.actions;

import static com.evolveum.midpoint.model.impl.scripting.VariablesUtil.cloneIfNecessary;

import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.model.api.BulkAction;

import com.evolveum.midpoint.xml.ns._public.model.scripting_3.AbstractActionExpressionType;

import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;

import com.evolveum.midpoint.xml.ns._public.model.scripting_3.SearchExpressionType;

import jakarta.annotation.PostConstruct;
import jakarta.xml.bind.JAXBElement;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.PipelineItem;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.model.impl.scripting.helpers.ExpressionHelper;
import com.evolveum.midpoint.model.impl.scripting.helpers.OperationsHelper;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ScriptingExpressionType;

/**
 * Evaluates "search" scripting expression.
 */
@Component
public class SearchExecutor extends BaseActionExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(SearchExecutor.class);

    @Autowired private ExpressionHelper expressionHelper;
    @Autowired private OperationsHelper operationsHelper;
    @Autowired private ExpressionFactory expressionFactory;

    private static final String PARAM_NO_FETCH = "noFetch";

    @PostConstruct
    public void init() {
        actionExecutorRegistry.register(this);
    }

    @Override
    public @NotNull BulkAction getActionType() {
        return BulkAction.SEARCH;
    }

    @Override
    public PipelineData execute(
            AbstractActionExpressionType action, PipelineData input, ExecutionContext context, OperationResult globalResult)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, SecurityViolationException,
            PolicyViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

        var searchBean = (SearchExpressionType) action;
        return executeInternal(searchBean, input, context, globalResult);
    }

    @Override
    public PipelineData execute(
            ActionExpressionType command, PipelineData input, ExecutionContext context, OperationResult globalResult) {
        throw new UnsupportedOperationException("The 'search' action cannot be invoked dynamically");
    }

    private <T extends ObjectType> PipelineData executeInternal(
            SearchExpressionType action, PipelineData input, ExecutionContext context, OperationResult globalResult)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, SecurityViolationException,
            PolicyViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

        Validate.notNull(action.getType());

        List<PipelineItem> data = input.getData();
        if (data.isEmpty()) {
            // TODO fix this brutal hack (with dummyValue)
            PrismContainerValue<?> dummyValue = prismContext.itemFactory().createContainerValue();
            PipelineItem dummyItem = new PipelineItem(dummyValue, PipelineData.newOperationResult(), context.getInitialVariables());
            data = Collections.singletonList(dummyItem);
        }

        final PipelineData outputData = PipelineData.createEmpty();
        final MutableBoolean atLeastOne = new MutableBoolean(false);

        for (PipelineItem item : data) {

            // TODO variables from current item
            // TODO operation result handling (global vs local)
            boolean noFetch = expressionHelper.getArgumentAsBoolean(
                    action.getParameter(), PARAM_NO_FETCH, input, context, false,
                    "search", globalResult);

            Class<T> objectClass =
                    ObjectTypes.getObjectTypeFromTypeQName(action.getType())
                            .getClassDefinition();

            ObjectQuery unresolvedObjectQuery;
            if (action.getQuery() != null) {
                unresolvedObjectQuery = context.getQueryConverter().createObjectQuery(objectClass, action.getQuery());
            } else if (action.getSearchFilter() != null) {
                unresolvedObjectQuery = prismContext.queryFactory().createQuery();
                ObjectFilter filter =
                        prismContext.getQueryConverter().parseFilter(action.getSearchFilter(), objectClass);
                unresolvedObjectQuery.setFilter(filter);
            } else {
                unresolvedObjectQuery = null;
            }
            ObjectQuery objectQuery;
            if (unresolvedObjectQuery != null) {
                VariablesMap variables = new VariablesMap();
                item.getVariables().forEach((name, value) -> variables.put(name, cloneIfNecessary(name, value)));
                objectQuery = ExpressionUtil.evaluateQueryExpressions(
                        unresolvedObjectQuery, variables, context.getExpressionProfile(), expressionFactory,
                        "bulk action query", context.getTask(), globalResult);
            } else {
                objectQuery = null;
            }

            final String variableName = action.getVariable();

            ResultHandler<T> handler = (object, parentResult) -> {
                context.checkTaskStop();
                atLeastOne.setValue(true);
                if (action.getScriptingExpression() != null) {
                    if (variableName != null) {
                        // TODO
                    }
                    JAXBElement<?> childExpression = action.getScriptingExpression();
                    try {
                        PipelineData searchResult = bulkActionsExecutor.execute(
                                (ScriptingExpressionType) childExpression.getValue(),
                                PipelineData.create(object.getValue(), item.getVariables()), context, globalResult);
                        if (!BooleanUtils.isFalse(action.isAggregateOutput())) {
                            outputData.addAllFrom(searchResult);
                        }
                        globalResult.setSummarizeSuccesses(true);
                        globalResult.summarize();
                    } catch (RuntimeException | SchemaException | ConfigurationException | ObjectNotFoundException |
                             ObjectAlreadyExistsException | CommunicationException | SecurityViolationException |
                             PolicyViolationException | ExpressionEvaluationException e) {
                        // todo think about this
                        if (context.isContinueOnAnyError()) {
                            LoggingUtils.logUnexpectedException(
                                    LOGGER, "Exception when evaluating item from search result list.", e);
                        } else {
                            throw new TunnelException(e);
                        }
                    }
                } else {
                    outputData.addValue(object.getValue(), item.getVariables());
                }
                return true;
            };

            var options = operationsHelper.createGetOptions(action.getOptions(), noFetch);
            try {
                modelService.searchObjectsIterative(
                        objectClass, objectQuery, handler, options, context.getTask(), globalResult);
            } catch (TunnelException e) {
                unwrapTunnelException(e);
            }
        }

        if (atLeastOne.isFalse()) {
            context.println("Warning: no matching object found"); // this will be maybe configurable
        }
        return outputData;
    }

    private void unwrapTunnelException(TunnelException e)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, ObjectAlreadyExistsException,
            CommunicationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {
        Throwable originalException = e.getCause();
        if (originalException instanceof ExpressionEvaluationException expressionEvaluationException) {
            throw expressionEvaluationException;
        } else if (originalException instanceof ObjectNotFoundException objectNotFoundException) {
            throw objectNotFoundException;
        } else if (originalException instanceof ObjectAlreadyExistsException objectAlreadyExistsException) {
            throw objectAlreadyExistsException;
        } else if (originalException instanceof SchemaException schemaException) {
            throw schemaException;
        } else if (originalException instanceof CommunicationException communicationException) {
            throw communicationException;
        } else if (originalException instanceof ConfigurationException configurationException) {
            throw configurationException;
        } else if (originalException instanceof SecurityViolationException securityViolationException) {
            throw securityViolationException;
        } else if (originalException instanceof PolicyViolationException policyViolationException) {
            throw policyViolationException;
        } else if (originalException instanceof RuntimeException runtimeException) {
            throw runtimeException;
        } else {
            throw new IllegalStateException("Unexpected exception: "+e+": "+e.getMessage(),e);
        }
    }
}
