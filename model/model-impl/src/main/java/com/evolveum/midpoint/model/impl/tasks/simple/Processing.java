/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.tasks.simple;

import com.evolveum.midpoint.util.exception.ScriptExecutionException;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Specifies the processing of a simple task:
 * - how object type, query, and search options are specified
 * - how resulting objects are handled
 */
public abstract class Processing<O extends ObjectType, EC extends ExecutionContext> {

    @NotNull protected final EC ctx;

    protected Processing(@NotNull EC ctx) {
        this.ctx = ctx;
    }

    /**
     * Creates object type. The default is to use object type that was configured in the task,
     * or ObjectType if there was none.
     *
     * For handlers that are limited to a specific type this method MUST be overridden.
     */
    protected Class<? extends O> determineObjectType(Class<? extends O> configuredType) {
        if (configuredType != null) {
            return configuredType;
        } else {
            //noinspection unchecked
            return (Class<O>) ObjectType.class;
        }
    }

    /**
     * Creates search query. The default is to use query that was configured in the task.
     *
     * @param configuredQuery Query that was configured in the task object.
     */
    protected ObjectQuery createQuery(ObjectQuery configuredQuery) throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        return configuredQuery;
    }

    /**
     * Creates search options. The default is to use options specified in the task.
     */
    protected Collection<SelectorOptions<GetOperationOptions>> createSearchOptions(
            Collection<SelectorOptions<GetOperationOptions>> configuredOptions) {
        return configuredOptions;
    }

    /**
     * Handles an object found. Must be overridden.
     * For simplicity we avoid returning a boolean flag. We expect that stopping the processing
     * for simple tasks is driven solely by the configuration (based e.g. on the exceptions thrown).
     */
    protected abstract void handleObject(PrismObject<O> object, RunningTask workerTask, OperationResult result)
            throws CommonException, PreconditionViolationException;
}
