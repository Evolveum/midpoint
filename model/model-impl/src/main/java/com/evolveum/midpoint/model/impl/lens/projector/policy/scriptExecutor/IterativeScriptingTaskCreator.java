/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.scriptExecutor;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRef;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStatusType.RUNNABLE;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Creates an iterative scripting task.
 */
class IterativeScriptingTaskCreator extends ScriptingTaskCreator {

    @NotNull private final PolicyRuleScriptExecutor beans;
    @NotNull private final LensContext<?> context;

    IterativeScriptingTaskCreator(@NotNull PolicyRuleScriptExecutor policyRuleScriptExecutor, @NotNull LensContext<?> context) {
        beans = policyRuleScriptExecutor;
        this.context = context;
    }

    public TaskType create(ExecuteScriptType executeScript, ScriptExecutionPolicyActionType action, Task task,
            OperationResult result) throws SecurityViolationException, ObjectNotFoundException, SchemaException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        if (executeScript.getInput() != null) {
            throw new UnsupportedOperationException("Explicit input with iterative task execution is not supported yet.");
        }

        MidPointPrincipal principal = beans.securityContextManager.getPrincipal();
        if (principal == null) {
            throw new SecurityViolationException("No current user");
        }
        AsynchronousScriptExecutionType asynchronousExecution = action.getAsynchronous();
        TaskType newTask;
        if (asynchronousExecution.getTaskTemplateRef() != null) {
            newTask = beans.modelObjectResolver.resolve(asynchronousExecution.getTaskTemplateRef(), TaskType.class,
                    null, "task template", task, result);
        } else {
            newTask = new TaskType(beans.prismContext);
            newTask.setName(PolyStringType.fromOrig("Execute script"));
            newTask.setRecurrence(TaskRecurrenceType.SINGLE);
        }
        newTask.setName(PolyStringType.fromOrig(newTask.getName().getOrig() + " " + (int) (Math.random() * 10000)));
        newTask.setOid(null);
        newTask.setTaskIdentifier(null);
        newTask.setOwnerRef(createObjectRef(principal.getFocus(), beans.prismContext));
        newTask.setExecutionStatus(RUNNABLE);
        newTask.setHandlerUri(ModelPublicConstants.ITERATIVE_SCRIPT_EXECUTION_TASK_HANDLER_URI);

        //noinspection unchecked
        PrismPropertyDefinition<QName> objectTypeDef = beans.prismContext.getSchemaRegistry()
                .findPropertyDefinitionByElementName(SchemaConstants.MODEL_EXTENSION_OBJECT_TYPE);
        PrismProperty<QName> objectTypeProp = objectTypeDef.instantiate();
        objectTypeProp.setRealValue(AssignmentHolderType.COMPLEX_TYPE);
        newTask.asPrismObject().addExtensionItem(objectTypeProp);

        //noinspection unchecked
        PrismPropertyDefinition<QueryType> queryDef = beans.prismContext.getSchemaRegistry()
                .findPropertyDefinitionByElementName(SchemaConstants.MODEL_EXTENSION_OBJECT_QUERY);
        PrismProperty<QueryType> queryProp = queryDef.instantiate();
        queryProp.setRealValue(createQuery(action));
        newTask.asPrismObject().addExtensionItem(queryProp);

        //noinspection unchecked
        PrismPropertyDefinition<ExecuteScriptType> executeScriptDef = beans.prismContext.getSchemaRegistry()
                .findPropertyDefinitionByElementName(SchemaConstants.SE_EXECUTE_SCRIPT);
        PrismProperty<ExecuteScriptType> executeScriptProp = executeScriptDef.instantiate();
        executeScriptProp.setRealValue(executeScript.clone());
        newTask.asPrismObject().addExtensionItem(executeScriptProp);

        return newTask;
    }

    private QueryType createQuery(ScriptExecutionPolicyActionType action) throws SchemaException {
        ObjectFilter filter = createFilter(action);
        SearchFilterType filterBean = beans.prismContext.getQueryConverter().createSearchFilterType(filter);
        return new QueryType()
                .filter(filterBean);
    }

    private ObjectFilter createFilter(ScriptExecutionPolicyActionType action) {
        ScriptExecutionObjectType objectSpec = action.getObject();
        if (objectSpec == null) {
            return createCurrentFilter();
        } else {
            List<ObjectFilter> filters = new ArrayList<>();
            if (objectSpec.getCurrentObject() != null) {
                filters.add(createCurrentFilter()); // TODO selector
            }
            for (LinkSourceObjectSelectorType linkSource : objectSpec.getLinkSource()) {
                filters.add(createLinkSourceFilter(linkSource));
            }
            if (!objectSpec.getLinkTarget().isEmpty()) {
                throw new UnsupportedOperationException("Link targets are not supported yet");
            }
            if (filters.isEmpty()) {
                return createCurrentFilter();
            } else if (filters.size() == 1) {
                return filters.get(0);
            } else {
                return beans.prismContext.queryFactory().createOr(filters);
            }
        }
    }

    // TODO selector
    private ObjectFilter createLinkSourceFilter(LinkSourceObjectSelectorType linkSource) {
        return beans.prismContext.queryFor(AssignmentHolderType.class)
                .item(AssignmentHolderType.F_ROLE_MEMBERSHIP_REF).ref(getFocusOid())
                .buildFilter();
    }

    private ObjectFilter createCurrentFilter() {
        return beans.prismContext.queryFor(AssignmentHolderType.class)
                .id(getFocusOid())
                .buildFilter();
    }

    private String getFocusOid() {
        return context.getFocusContextRequired().getOid();
    }
}
