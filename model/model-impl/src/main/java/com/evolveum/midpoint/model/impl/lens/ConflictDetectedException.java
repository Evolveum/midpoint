/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.repo.api.PreconditionViolationException;

/**
 * TODO better name
 *
 * An expected conflict was detected by repository, throwing {@link PreconditionViolationException}.
 * It is then converted to this one.
 *
 * Should occur on focus objects only. (For now.)
 */
public class ConflictDetectedException extends Exception {

    public ConflictDetectedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConflictDetectedException(Throwable cause) {
        super(cause);
    }
}
