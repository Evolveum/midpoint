/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.schema.result.OperationResult;

@FunctionalInterface
public interface ExpressionEnvironmentSupplier {

    ExpressionEnvironment get(Task task, OperationResult result);
}
