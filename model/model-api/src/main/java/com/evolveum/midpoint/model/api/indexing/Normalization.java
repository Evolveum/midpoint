/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.indexing;

import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.schema.result.OperationResult;

import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.util.exception.*;

import org.jetbrains.annotations.NotNull;

public interface Normalization {

    @NotNull String getName();

    boolean isDefault();

    ItemName getIndexItemName();

    ItemPath getIndexItemPath();

    @NotNull PrismPropertyDefinition<String> getIndexItemDefinition();

    @NotNull String normalize(@NotNull String input, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ConfigurationException, ObjectNotFoundException;
}
