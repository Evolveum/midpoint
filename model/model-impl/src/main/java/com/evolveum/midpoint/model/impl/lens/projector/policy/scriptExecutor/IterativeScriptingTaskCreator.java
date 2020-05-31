/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.scriptExecutor;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.repo.api.query.CompleteQuery;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;

/**
 * Creates an iterative scripting task.
 */
class IterativeScriptingTaskCreator extends ScriptingTaskCreator {

    @NotNull private final SchemaRegistry schemaRegistry;

    IterativeScriptingTaskCreator(@NotNull ActionContext actx) {
        super(actx);
        this.schemaRegistry = beans.prismContext.getSchemaRegistry();
    }

    @Override
    public TaskType createTask(ExecuteScriptType executeScript, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        if (executeScript.getInput() != null) {
            throw new UnsupportedOperationException("Explicit input with iterative task execution is not supported yet.");
        }

        TaskType newTask = createArchetypedTask(result);
        CompleteQuery<?> completeQuery = createCompleteQuery(result);

        setObjectTypeInTask(newTask, completeQuery);
        setQueryInTask(newTask, completeQuery);
        setScriptInTask(newTask, executeScript);

        return customizeTask(newTask, result);
    }

    @NotNull
    private TaskType createArchetypedTask(OperationResult result) throws ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        return super.createEmptyTask(result)
                .beginAssignment()
                    .targetRef(SystemObjectsType.ARCHETYPE_ITERATIVE_BULK_ACTION_TASK.value(), ArchetypeType.COMPLEX_TYPE)
                .end();
    }

    @NotNull
    private CompleteQuery<?> createCompleteQuery(OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {

        QueryBasedObjectSet objectSet = new QueryBasedObjectSet(actx, result);
        objectSet.collect();
        return objectSet.asQuery();
    }

    private void setQueryInTask(TaskType taskBean, CompleteQuery<?> union) throws SchemaException {
        QueryType queryBean = beans.prismContext.getQueryConverter().createQueryType(union.getQuery());
        //noinspection unchecked
        PrismPropertyDefinition<QueryType> queryDef = beans.prismContext.getSchemaRegistry()
                .findPropertyDefinitionByElementName(SchemaConstants.MODEL_EXTENSION_OBJECT_QUERY);
        PrismProperty<QueryType> queryProp = queryDef.instantiate();
        queryProp.setRealValue(queryBean);
        taskBean.asPrismObject().addExtensionItem(queryProp);
    }

    private void setObjectTypeInTask(TaskType taskBean, CompleteQuery<?> union)
            throws SchemaException {
        QName typeName = schemaRegistry.determineTypeForClass(union.getType());
        if (typeName == null || !schemaRegistry.isAssignableFrom(ObjectType.class, typeName)) {
            throw new IllegalStateException("Type for class " + union.getType() + " is unknown or not an ObjectType: " + typeName);
        }

        //noinspection unchecked
        PrismPropertyDefinition<QName> objectTypeDef = beans.prismContext.getSchemaRegistry()
                .findPropertyDefinitionByElementName(SchemaConstants.MODEL_EXTENSION_OBJECT_TYPE);
        PrismProperty<QName> objectTypeProp = objectTypeDef.instantiate();
        objectTypeProp.setRealValue(typeName);
        taskBean.asPrismObject().addExtensionItem(objectTypeProp);
    }
}
