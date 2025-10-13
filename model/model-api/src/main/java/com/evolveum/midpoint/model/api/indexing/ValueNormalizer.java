/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.api.indexing;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;

import org.jetbrains.annotations.NotNull;

/**
 * Normalizes a (string) value for the purpose of custom property indexing.
 */
public interface ValueNormalizer {

    @NotNull String normalize(@NotNull Object input, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ConfigurationException, ObjectNotFoundException;

}
