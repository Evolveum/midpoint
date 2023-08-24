/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting.actions;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.PipelineItem;
import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionParameterValueType;

import org.jetbrains.annotations.NotNull;

/**
 * Abstract executor for assign and unassign actions.
 */
abstract class AssignmentOperationsExecutor<P extends AssignmentOperationsExecutor.Parameters>
        extends AbstractObjectBasedActionExecutor<AssignmentHolderType> {

    private static final String PARAM_RESOURCE = "resource";
    private static final String PARAM_ROLE = "role";
    private static final String PARAM_RELATION = "relation";

    static class Parameters {
        ModelExecuteOptions options;
        boolean dryRun;
    }

    @Override
    public PipelineData execute(
            ActionExpressionType action, PipelineData input, ExecutionContext context, OperationResult globalResult)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, SecurityViolationException,
            PolicyViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

        P parameters = parseParameters(action, input, context, globalResult);
        parameters.options = operationsHelper.getOptions(action, input, context, globalResult);
        parameters.dryRun = operationsHelper.getDryRun(action, input, context, globalResult);

        if (checkParameters(parameters, context)) {
            iterateOverObjects(input, context, globalResult,
                    (object, item, result) ->
                            apply(object.asObjectable(), item, parameters, context, result),
                    (object, exception) ->
                            context.println("Failed to modify " + object + drySuffix(parameters.dryRun) + exceptionSuffix(exception))
            );
        }

        return input;
    }

    abstract boolean checkParameters(P parameters, ExecutionContext context);

    abstract P parseParameters(
            ActionExpressionType action, PipelineData input, ExecutionContext context, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, SecurityViolationException,
            PolicyViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException;

    private void apply(
            AssignmentHolderType object, PipelineItem item, P parameters, ExecutionContext context, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, SecurityViolationException,
            PolicyViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        ObjectDelta<? extends ObjectType> delta = createDelta(object, item, parameters, context, result);
        if (!delta.isEmpty()) {
            operationsHelper.applyDelta(delta, parameters.options, parameters.dryRun, context, result);
            context.println("Modified " + object + optionsSuffix(parameters.options, parameters.dryRun));
        }
    }

    abstract ObjectDelta<? extends ObjectType> createDelta(AssignmentHolderType object, PipelineItem item, P parameters,
            ExecutionContext context, OperationResult result) throws SchemaException, ConfigurationException,
            ObjectNotFoundException, CommunicationException, SecurityViolationException, ExpressionEvaluationException;

    @Override
    protected Class<AssignmentHolderType> getObjectType() {
        return AssignmentHolderType.class;
    }

    @NotNull Collection<ObjectReferenceType> getRolesParameter(
            ActionExpressionType action, PipelineData input, ExecutionContext context, OperationResult globalResult)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, SecurityViolationException,
            PolicyViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        ActionParameterValueType roleParameterValue = expressionHelper.getArgument(
                action.getParameter(), PARAM_ROLE, false, false, getName());
        if (roleParameterValue != null) {
            PipelineData data = expressionHelper.evaluateParameter(roleParameterValue, null, input, context, globalResult);
            // if somebody wants to assign Org, he has to use full reference value (including object type)
            return data.getDataAsReferences(RoleType.COMPLEX_TYPE, AbstractRoleType.class, context, globalResult);
        } else {
            return Collections.emptyList();
        }
    }

    @NotNull Collection<ObjectReferenceType> getResourcesParameter(
            ActionExpressionType action, PipelineData input, ExecutionContext context, OperationResult globalResult)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, SecurityViolationException,
            PolicyViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        ActionParameterValueType resourceParameterValue = expressionHelper.getArgument(
                action.getParameter(), PARAM_RESOURCE, false, false, getName());
        if (resourceParameterValue != null) {
            PipelineData data = expressionHelper
                    .evaluateParameter(resourceParameterValue, null, input, context, globalResult);
            return data.getDataAsReferences(ResourceType.COMPLEX_TYPE, ResourceType.class, context, globalResult);
        } else {
            return Collections.emptyList();
        }
    }

    @NotNull Collection<QName> getRelationsParameter(
            ActionExpressionType action, PipelineData input, ExecutionContext context, OperationResult globalResult)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, SecurityViolationException,
            PolicyViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        Collection<String> relationSpecificationUris = expressionHelper.getArgumentValues(action.getParameter(), PARAM_RELATION,
                false, false, getName(), input, context, String.class, globalResult);
        return relationSpecificationUris.stream()
                .map(uri -> QNameUtil.uriToQName(uri, true))
                .collect(Collectors.toSet());
    }
}
