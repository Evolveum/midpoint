/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.util;

import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingLoader;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SystemException;

import org.jetbrains.annotations.NotNull;

/** Temporary. We should cover error handling in a more systematic way. */
public class ErrorHandlingUtil {

    public static void processProjectionNotFoundException(
            @NotNull ObjectNotFoundException e, @NotNull LensProjectionContext projectionContext)
            throws ObjectNotFoundException {
        if (projectionContext.isGone() || projectionContext.isBroken()) {
            // This is not critical. The projection is already marked as gone/broken and we can go on with processing
            // No extra action is needed.
        } else {
            throw e;
        }

    }
    public static void processProjectionNotLoadedException(
            @NotNull MappingLoader.NotLoadedException e,
            @NotNull LensProjectionContext projectionContext) {
        if (projectionContext.isGone() || projectionContext.isBroken()) {
            // This is not critical. The projection is already marked as gone/broken and we can go on with processing
            // No extra action is needed.
        } else {
            // The "not loaded" exception is thrown only if context is gone or broken
            throw SystemException.unexpected(e, "not loaded with context not gone/broken?");
        }
    }
}
