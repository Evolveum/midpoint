/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting.actions;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.PipelineItem;
import com.evolveum.midpoint.model.api.ScriptExecutionException;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.RelationTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionParameterValueType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 *
 */
public abstract class AssignmentOperationsExecutor extends BaseActionExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentOperationsExecutor.class);

    static final String UNASSIGN_NAME = "unassign";
    static final String ASSIGN_NAME = "assign";
    private static final String PARAM_RESOURCE = "resource";
    private static final String PARAM_ROLE = "role";
    private static final String PARAM_RELATION = "relation";

    @PostConstruct
    public void init() {
        scriptingExpressionEvaluator.registerActionExecutor(getName(), this);
    }

    protected abstract String getName();

    @Override
    public PipelineData execute(ActionExpressionType expression, PipelineData input, ExecutionContext context, OperationResult globalResult) throws ScriptExecutionException {

        ModelExecuteOptions executionOptions = getOptions(expression, input, context, globalResult);
        boolean dryRun = getParamDryRun(expression, input, context, globalResult);

        ActionParameterValueType resourceParameterValue = expressionHelper.getArgument(expression.getParameter(), PARAM_RESOURCE, false, false, getName());
        ActionParameterValueType roleParameterValue = expressionHelper.getArgument(expression.getParameter(), PARAM_ROLE, false, false, getName());
        Collection<String> relationSpecificationUris = expressionHelper.getArgumentValues(expression.getParameter(), PARAM_RELATION, false, false, getName(), input, context, String.class, globalResult);

        Collection<QName> relationSpecifications;
        if (relationSpecificationUris.isEmpty()) {
            QName defaultRelation = ObjectUtils.defaultIfNull(prismContext.getDefaultRelation(), RelationTypes.MEMBER.getRelation());
            relationSpecifications = Collections.singleton(defaultRelation);
        } else {
            relationSpecifications = relationSpecificationUris.stream()
                    .map(uri -> QNameUtil.uriToQName(uri, true))
                    .collect(Collectors.toSet());
        }
        assert !relationSpecifications.isEmpty();

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
            throw new ScriptExecutionException("Nothing to " + getName() + ": neither resource nor role specified");
        }

        if (CollectionUtils.isEmpty(resources) && CollectionUtils.isEmpty(roles)) {
            LOGGER.warn("No resources and no roles to unassign in a scripting expression");
            context.println("Warning: no resources and no roles to unassign");        // TODO some better handling?
            return input;
        }

        for (PipelineItem item : input.getData()) {
            PrismValue value = item.getValue();
            OperationResult result = operationsHelper.createActionResult(item, this, context, globalResult);
            context.checkTaskStop();
            if (value instanceof PrismObjectValue && ((PrismObjectValue) value).asObjectable() instanceof AssignmentHolderType) {
                @SuppressWarnings({"unchecked", "raw"})
                PrismObject<? extends ObjectType> prismObject = ((PrismObjectValue) value).asPrismObject();
                AssignmentHolderType objectType = (AssignmentHolderType) prismObject.asObjectable();
                long started = operationsHelper.recordStart(context, objectType);
                Throwable exception = null;
                try {
                    operationsHelper.applyDelta(createDelta(objectType, resources, roles, relationSpecifications), executionOptions, dryRun, context, result);
                    operationsHelper.recordEnd(context, objectType, started, null);
                } catch (Throwable ex) {
                    operationsHelper.recordEnd(context, objectType, started, ex);
                    exception = processActionException(ex, getName(), value, context);
                }
                context.println(createConsoleMessage(prismObject, executionOptions, dryRun, exception));
            } else {
                //noinspection ThrowableNotThrown
                processActionException(new ScriptExecutionException("Item is not a PrismObject of AssignmentHolderType"), getName(), value, context);
            }
            operationsHelper.trimAndCloneResult(result, globalResult, context);
        }
        return input;           // TODO updated objects?
    }

    @NotNull
    private String createConsoleMessage(PrismObject<? extends ObjectType> object, ModelExecuteOptions executionOptions,
            boolean dryRun, Throwable exception) {
        return (exception != null ? "Attempted to modify " : "Modified ") + object
                + optionsSuffix(executionOptions, dryRun) + exceptionSuffix(exception);
    }

    protected abstract ObjectDelta<? extends ObjectType> createDelta(AssignmentHolderType object, Collection<ObjectReferenceType> resources,
          Collection<ObjectReferenceType> roles, Collection<QName> relationSpecifications) throws SchemaException;
}
