/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.scripting.expressions;

import static com.evolveum.midpoint.model.impl.scripting.VariablesUtil.cloneIfNecessary;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import jakarta.xml.bind.JAXBElement;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.BooleanUtils;
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
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ScriptingExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.SearchExpressionType;

/**
 * Evaluates "search" scripting expression.
 */
@Component
public class SearchEvaluator extends BaseExpressionEvaluator {

    private static final Trace LOGGER = TraceManager.getTrace(SearchEvaluator.class);

    @Autowired private ExpressionHelper expressionHelper;
    @Autowired private OperationsHelper operationsHelper;
    @Autowired private ExpressionFactory expressionFactory;

    private static final String PARAM_NO_FETCH = "noFetch";

    public <T extends ObjectType> PipelineData evaluate(SearchExpressionType searchExpression, PipelineData input,
            ExecutionContext context, OperationResult globalResult)
            throws ScriptExecutionException, SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, SecurityViolationException, ExpressionEvaluationException {
        Validate.notNull(searchExpression.getType());

        ExpressionProfile expressionProfile = MiscSchemaUtil.getExpressionProfile();

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
            boolean noFetch = expressionHelper.getArgumentAsBoolean(searchExpression.getParameter(), PARAM_NO_FETCH, input, context, false, "search", globalResult);

            Class<T> objectClass =
                    ObjectTypes.getObjectTypeFromTypeQName(searchExpression.getType())
                            .getClassDefinition();

            ObjectQuery unresolvedObjectQuery = null;
            if (searchExpression.getQuery() != null) {
                try {
                    unresolvedObjectQuery = context.getQueryConverter().createObjectQuery(objectClass, searchExpression.getQuery());
                } catch (SchemaException e) {
                    throw new ScriptExecutionException("Couldn't parse object query. Reason: " + e.getMessage(), e);
                }
            } else if (searchExpression.getSearchFilter() != null) {
                unresolvedObjectQuery = prismContext.queryFactory().createQuery();
                try {
                    ObjectFilter filter = prismContext.getQueryConverter().parseFilter(searchExpression.getSearchFilter(), objectClass);
                    unresolvedObjectQuery.setFilter(filter);
                } catch (SchemaException e) {
                    throw new ScriptExecutionException("Couldn't parse object query. Reason: " + e.getMessage(), e);
                }
            }
            ObjectQuery objectQuery;
            if (unresolvedObjectQuery != null) {
                VariablesMap variables = new VariablesMap();
                item.getVariables().forEach((name, value) -> variables.put(name, cloneIfNecessary(name, value)));
                try {
                    objectQuery = ExpressionUtil.evaluateQueryExpressions(
                            unresolvedObjectQuery, variables, expressionProfile, expressionFactory,
                            "bulk action query", context.getTask(), globalResult);
                } catch (CommonException e) {
                    // TODO continue on any error?
                    throw new ScriptExecutionException("Couldn't evaluate expressions in object query: " + e.getMessage(), e);
                }
            } else {
                objectQuery = null;
            }

            final String variableName = searchExpression.getVariable();

            ResultHandler<T> handler = (object, parentResult) -> {
                context.checkTaskStop();
                atLeastOne.setValue(true);
                if (searchExpression.getScriptingExpression() != null) {
                    if (variableName != null) {
                        // TODO
                    }
                    JAXBElement<?> childExpression = searchExpression.getScriptingExpression();
                    try {
                        PipelineData expressionResult = scriptingExpressionEvaluator.evaluateExpression(
                                (ScriptingExpressionType) childExpression.getValue(),
                                PipelineData.create(object.getValue(), item.getVariables()), context, globalResult);
                        if (!BooleanUtils.isFalse(searchExpression.isAggregateOutput())) {
                            outputData.addAllFrom(expressionResult);
                        }
                        globalResult.setSummarizeSuccesses(true);
                        globalResult.summarize();
                    } catch (ScriptExecutionException | SchemaException | ConfigurationException | ObjectNotFoundException | CommunicationException | SecurityViolationException | ExpressionEvaluationException e) {
                        // todo think about this
                        if (context.isContinueOnAnyError()) {
                            LoggingUtils.logUnexpectedException(LOGGER, "Exception when evaluating item from search result list.", e);
                        } else {
                            throw new SystemException(e);
                        }
                    }
                } else {
                    outputData.addValue(object.getValue(), item.getVariables());
                }
                return true;
            };

            try {
                Collection<SelectorOptions<GetOperationOptions>> options = operationsHelper.createGetOptions(searchExpression.getOptions(), noFetch);
                modelService.searchObjectsIterative(objectClass, objectQuery, handler, options, context.getTask(), globalResult);
            } catch (SchemaException | ObjectNotFoundException | SecurityViolationException | CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
                // TODO continue on any error?
                throw new ScriptExecutionException("Couldn't execute searchObjects operation: " + e.getMessage(), e);
            }
        }

        if (atLeastOne.isFalse()) {
            context.println("Warning: no matching object found");          // temporary hack, this will be configurable
        }
        return outputData;
    }

}
