/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.scriptExecutor;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.repo.api.query.CompleteQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
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

        // @formatter:off
        TaskType newTask = super.createEmptyTask(result)
                .beginAssignment()
                    .targetRef(SystemObjectsType.ARCHETYPE_ITERATIVE_BULK_ACTION_TASK.value(), ArchetypeType.COMPLEX_TYPE)
                .<TaskType>end()
                .beginActivity()
                    .beginWork()
                        .beginIterativeScripting()
                            .objects(
                                    createObjectSet(result))
                            .scriptExecutionRequest(executeScript)
                        .<WorkDefinitionsType>end()
                    .<ActivityDefinitionType>end()
                .end();
        // @formatter:on

        return customizeTask(newTask, result);
    }

    private @NotNull ObjectSetType createObjectSet(OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        CompleteQuery<?> completeQuery = createCompleteQuery(result);
        return new ObjectSetType(PrismContext.get())
                .type(getTypeName(completeQuery))
                .query(getQueryBean(completeQuery));
    }

    private @NotNull CompleteQuery<?> createCompleteQuery(OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {

        QueryBasedObjectSet objectSet = new QueryBasedObjectSet(actx, result);
        objectSet.collect();
        return objectSet.asQuery();
    }

    private @NotNull QName getTypeName(@NotNull CompleteQuery<?> completeQuery) {
        QName typeName = schemaRegistry.determineTypeForClass(completeQuery.getType());
        if (typeName == null || !schemaRegistry.isAssignableFrom(ObjectType.class, typeName)) {
            throw new IllegalStateException("Type for class " + completeQuery.getType() + " is unknown or not an ObjectType: " + typeName);
        }
        return typeName;
    }

    private QueryType getQueryBean(@NotNull CompleteQuery<?> completeQuery) throws SchemaException {
        return beans.prismContext.getQueryConverter()
                .createQueryType(completeQuery.getQuery());
    }
}
