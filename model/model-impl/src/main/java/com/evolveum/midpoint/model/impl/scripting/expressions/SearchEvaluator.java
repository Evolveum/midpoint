/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.model.impl.scripting.expressions;

import com.evolveum.midpoint.model.api.PipelineItem;
import com.evolveum.midpoint.model.api.ScriptExecutionException;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.model.impl.scripting.helpers.ExpressionHelper;
import com.evolveum.midpoint.model.impl.scripting.helpers.OperationsHelper;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.marshaller.QueryConvertor;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryJaxbConvertor;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ScriptingExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.SearchExpressionType;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author mederly
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
		    throws ScriptExecutionException {
        Validate.notNull(searchExpression.getType());

	    List<PipelineItem> data = input.getData();
	    if (data.isEmpty()) {
	    	// TODO fix this brutal hack (with dummyValue)
		    PrismContainerValue<ObjectType> dummyValue = new PrismContainerValue<>(prismContext);
		    PipelineItem dummyItem = new PipelineItem(dummyValue, PipelineData.newOperationResult());
		    PipelineItem.copyClonedVariables(context.getInitialVariables(), dummyItem.getVariables());
		    data = Collections.singletonList(dummyItem);
	    }

	    final PipelineData outputData = PipelineData.createEmpty();
	    final MutableBoolean atLeastOne = new MutableBoolean(false);

	    for (PipelineItem item : data) {

	    	// TODO variables from current item
		    // TODO operation result handling (global vs local)
		    boolean noFetch = expressionHelper.getArgumentAsBoolean(searchExpression.getParameter(), PARAM_NO_FETCH, input, context, false, "search", globalResult);

		    @SuppressWarnings({ "unchecked", "raw" })
		    Class<T> objectClass = (Class<T>) ObjectTypes.getObjectTypeFromTypeQName(searchExpression.getType()).getClassDefinition();

		    ObjectQuery unresolvedObjectQuery = null;
		    if (searchExpression.getQuery() != null) {
			    try {
				    unresolvedObjectQuery = QueryJaxbConvertor.createObjectQuery(objectClass, searchExpression.getQuery(), prismContext);
			    } catch (SchemaException e) {
				    throw new ScriptExecutionException("Couldn't parse object query due to schema exception", e);
			    }
		    } else if (searchExpression.getSearchFilter() != null) {
			    unresolvedObjectQuery = new ObjectQuery();
			    try {
				    ObjectFilter filter = QueryConvertor.parseFilter(searchExpression.getSearchFilter(), objectClass, prismContext);
				    unresolvedObjectQuery.setFilter(filter);
			    } catch (SchemaException e) {
				    throw new ScriptExecutionException("Couldn't parse object filter due to schema exception", e);
			    }
		    }
		    ObjectQuery objectQuery;
		    if (unresolvedObjectQuery != null) {
			    ExpressionVariables variables = new ExpressionVariables();
			    item.getVariables().forEach((name, value) -> variables.addVariableDefinition(new QName(name), value));
			    try {
				    objectQuery = ExpressionUtil
						    .evaluateQueryExpressions(unresolvedObjectQuery, variables, expressionFactory, prismContext,
								    "bulk action query", context.getTask(), globalResult);
			    } catch (SchemaException | ObjectNotFoundException | ExpressionEvaluationException e) {
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
					    outputData.addAllFrom(scriptingExpressionEvaluator.evaluateExpression(
							    (ScriptingExpressionType) childExpression.getValue(),
							    PipelineData.create(object.getValue(), item.getVariables()), context, globalResult));
					    globalResult.setSummarizeSuccesses(true);
					    globalResult.summarize();
				    } catch (ScriptExecutionException e) {
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
