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

package com.evolveum.midpoint.model.impl.scripting.actions;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.api.ScriptExecutionException;
import com.evolveum.midpoint.model.api.PipelineItem;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;

import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionParameterValueType;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author mederly
 */
@Component
public class AssignExecutor extends BaseActionExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(AssignExecutor.class);

    private static final String NAME = "assign";
    private static final String PARAM_RESOURCE = "resource";
    private static final String PARAM_ROLE = "role";

    @PostConstruct
    public void init() {
        scriptingExpressionEvaluator.registerActionExecutor(NAME, this);
    }

    @Override
    public PipelineData execute(ActionExpressionType expression, PipelineData input, ExecutionContext context, OperationResult globalResult) throws ScriptExecutionException {

        ModelExecuteOptions executionOptions = getOptions(expression, input, context, globalResult);
        boolean dryRun = getParamDryRun(expression, input, context, globalResult);

        ActionParameterValueType resourceParameterValue = expressionHelper.getArgument(expression.getParameter(), PARAM_RESOURCE, false, false, NAME);
        ActionParameterValueType roleParameterValue = expressionHelper.getArgument(expression.getParameter(), PARAM_ROLE, false, false, NAME);

	    Collection<ObjectReferenceType> resources;
        try {
	        if (resourceParameterValue != null) {
		        PipelineData data = expressionHelper
				        .evaluateParameter(resourceParameterValue, null, input, context, globalResult);
		        resources = data.getDataAsReferences(ResourceType.COMPLEX_TYPE, ResourceType.class, context, globalResult);
	        } else {
		        resources = null;
	        }
        } catch (CommonException e) {
        	throw new ScriptExecutionException("Couldn't evaluate '" + PARAM_RESOURCE + "' parameter of a scripting expression: " + e.getMessage(), e);
        }

        Collection<ObjectReferenceType> roles;
        try {
	        if (roleParameterValue != null) {
		        PipelineData data = expressionHelper.evaluateParameter(roleParameterValue, null, input, context, globalResult);
		        roles = data.getDataAsReferences(RoleType.COMPLEX_TYPE, AbstractRoleType.class, context, globalResult);        // if somebody wants to assign Org, he has to use full reference value (including object type)
	        } else {
		        roles = null;
	        }
        } catch (CommonException e) {
	        throw new ScriptExecutionException("Couldn't evaluate '" + PARAM_ROLE + "' parameter of a scripting expression: " + e.getMessage(), e);
        }

        if (resources == null && roles == null) {
            throw new ScriptExecutionException("Nothing to assign: neither resource nor role specified");
        }

        if (CollectionUtils.isEmpty(resources) && CollectionUtils.isEmpty(roles)) {
        	LOGGER.warn("No resources and no roles to assign in a scripting expression");
        	context.println("Warning: no resources and no roles to assign");        // TODO some better handling?
	        return input;
        }

        for (PipelineItem item : input.getData()) {
            PrismValue value = item.getValue();
            OperationResult result = operationsHelper.createActionResult(item, this, context, globalResult);
            context.checkTaskStop();
            if (value instanceof PrismObjectValue && ((PrismObjectValue) value).asObjectable() instanceof FocusType) {
                @SuppressWarnings({"unchecked", "raw"})
                PrismObject<? extends ObjectType> prismObject = ((PrismObjectValue) value).asPrismObject();
                ObjectType objectType = prismObject.asObjectable();
                long started = operationsHelper.recordStart(context, objectType);
                Throwable exception = null;
                try {
                    operationsHelper.applyDelta(createDelta(objectType, resources, roles), executionOptions, dryRun, context, result);
                    operationsHelper.recordEnd(context, objectType, started, null);
                } catch (Throwable ex) {
                    operationsHelper.recordEnd(context, objectType, started, ex);
					exception = processActionException(ex, NAME, value, context);
                }
                context.println((exception != null ? "Attempted to modify " : "Modified ") + prismObject.toString() + optionsSuffix(executionOptions, dryRun) + exceptionSuffix(exception));
            } else {
				//noinspection ThrowableNotThrown
				processActionException(new ScriptExecutionException("Item is not a PrismObject of FocusType"), NAME, value, context);
            }
            operationsHelper.trimAndCloneResult(result, globalResult, context);
        }
        return input;           // TODO updated objects?
    }

    private ObjectDelta<? extends ObjectType> createDelta(ObjectType objectType, Collection<ObjectReferenceType> resources, Collection<ObjectReferenceType> roles) throws ScriptExecutionException {

        List<AssignmentType> assignments = new ArrayList<>();

        if (roles != null) {
            for (ObjectReferenceType roleRef : roles) {
                AssignmentType assignmentType = new AssignmentType();
                assignmentType.setTargetRef(roleRef);
                assignments.add(assignmentType);
            }
        }

        if (resources != null) {
            for (ObjectReferenceType resourceRef : resources) {
                AssignmentType assignmentType = new AssignmentType();
                ConstructionType constructionType = new ConstructionType();
                constructionType.setResourceRef(resourceRef);
                assignmentType.setConstruction(constructionType);
                assignments.add(assignmentType);
            }
        }

        ObjectDelta<? extends ObjectType> delta = ObjectDelta.createEmptyModifyDelta(objectType.getClass(), objectType.getOid(), prismContext);
        try {
            delta.addModificationAddContainer(FocusType.F_ASSIGNMENT, assignments.toArray(new AssignmentType[0]));
        } catch (SchemaException e) {
            throw new ScriptExecutionException("Couldn't prepare modification to add resource/role assignments", e);
        }
        return delta;
    }
}
