/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.schema.result.OperationResult;

@FunctionalInterface
public interface ExpressionEnvironmentSupplier {

    ExpressionEnvironment get(Task task, OperationResult result);
}
