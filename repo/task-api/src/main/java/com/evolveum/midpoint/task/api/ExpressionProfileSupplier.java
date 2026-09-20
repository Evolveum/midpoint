/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.task.api;

import java.io.Serializable;

import org.jspecify.annotations.NullMarked;

import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.MidPointTrustDescriptor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SecurityViolationException;

/**
 * Supplies an {@link ExpressionProfile}, typically by engaging `ExpressionProfileManager` in higher layers,
 * or by providing a pre-configured profile.
 *
 * NOTE: The placement in `task-api` is a bit unfortunate, but it cannot be lower because of the use of the {@link Task}.
 * And it cannot be much higher because it is referenced by `repo-test-util`. This is something to be reconsidered.
 */
@NullMarked
public interface ExpressionProfileSupplier extends Serializable {

    /**
     * Provides an expression profile in the given context.
     *
     * @throws SecurityViolationException If the profile cannot be determined (for whatever reasons).
     */
    ExpressionProfile getExpressionProfile(MidPointTrustDescriptor trustDescriptor, Task task, OperationResult result)
            throws SecurityViolationException;
}
