/*
 * Copyright (c) 2010-2014 Evolveum
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

import com.evolveum.midpoint.model.api.ScriptExecutionException;
import com.evolveum.midpoint.model.impl.scripting.Data;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.helpers.ExpressionHelper;
import com.evolveum.midpoint.model.impl.scripting.helpers.OperationsHelper;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.marshaller.QueryConvertor;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryJaxbConvertor;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ScriptingExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.SearchExpressionType;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;

/**
 * @author mederly
 */
@Component
public class SearchEvaluator extends BaseExpressionEvaluator {

    @Autowired
    private ExpressionHelper expressionHelper;

    @Autowired
    private OperationsHelper operationsHelper;

    private static final String PARAM_NO_FETCH = "noFetch";

    public <T extends ObjectType> Data evaluate(final SearchExpressionType searchExpression, Data input, final ExecutionContext context, final OperationResult result) throws ScriptExecutionException {
        Validate.notNull(searchExpression.getType());

        boolean noFetch = expressionHelper.getArgumentAsBoolean(searchExpression.getParameter(), PARAM_NO_FETCH, input, context, false, "search", result);

        Class<T> objectClass = (Class) ObjectTypes.getObjectTypeFromTypeQName(searchExpression.getType()).getClassDefinition();

        ObjectQuery objectQuery = null;
        if (searchExpression.getQuery() != null) {
            try {
                objectQuery = QueryJaxbConvertor.createObjectQuery(objectClass, searchExpression.getQuery(), prismContext);
            } catch (SchemaException e) {
                throw new ScriptExecutionException("Couldn't parse object query due to schema exception", e);
            }
        } else if (searchExpression.getSearchFilter() != null) {
            // todo resolve variable references in the filter
            objectQuery = new ObjectQuery();
            try {
                ObjectFilter filter = QueryConvertor.parseFilter(searchExpression.getSearchFilter(), objectClass, prismContext);
                objectQuery.setFilter(filter);
            } catch (SchemaException e) {
                throw new ScriptExecutionException("Couldn't parse object filter due to schema exception", e);
            }
        }

        final String variableName = searchExpression.getVariable();

        Data oldVariableValue = null;
        if (variableName != null) {
            oldVariableValue = context.getVariable(variableName);
        }

        final Data outputData = Data.createEmpty();

        final MutableBoolean atLeastOne = new MutableBoolean(false);

        ResultHandler<T> handler = new ResultHandler<T>() {
            @Override
            public boolean handle(PrismObject<T> object, OperationResult parentResult) {
				context.checkTaskStop();
				atLeastOne.setValue(true);
                if (searchExpression.getScriptingExpression() != null) {
                    if (variableName != null) {
                        context.setVariable(variableName, object);
                    }
                    JAXBElement<?> childExpression = searchExpression.getScriptingExpression();
                    try {
                        outputData.addAllFrom(scriptingExpressionEvaluator.evaluateExpression((ScriptingExpressionType) childExpression.getValue(), Data.create(object), context, result));
                        result.setSummarizeSuccesses(true);
                        result.summarize();
                    } catch (ScriptExecutionException e) {
                        throw new SystemException(e);           // todo think about this
                    }
                } else {
                    outputData.addItem(object);
                }
                return true;
            }
        };

        try {
            modelService.searchObjectsIterative(objectClass, objectQuery, handler, operationsHelper.createGetOptions(noFetch), context.getTask(), result);
        } catch (SchemaException | ObjectNotFoundException | SecurityViolationException | CommunicationException | ConfigurationException e) {
            throw new ScriptExecutionException("Couldn't execute searchObjects operation: " + e.getMessage(), e);
        }

        if (atLeastOne.isFalse()) {
            String matching;
            if (objectQuery != null) {
                matching = "matching ";
            } else {
                matching = "";
            }
            context.println("Warning: no " + matching + searchExpression.getType().getLocalPart() + " object found");          // temporary hack, this will be configurable
        }

        if (variableName != null) {
            context.setVariable(variableName, oldVariableValue);
        }
        return outputData;
    }

}
