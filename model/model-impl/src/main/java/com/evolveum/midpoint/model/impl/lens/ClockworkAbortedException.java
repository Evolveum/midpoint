/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.repo.common.expression.Expression;

/**
 * Indicates that the current clockwork execution should be aborted, e.g. because the focus is gone.
 *
 * This exception is intentionally *UNCHECKED*, because it can get propagated through many layers of the code - even such
 * low-level ones like {@link Expression}. It could get named abstractly like `OperationAbortedException` but that would be
 * too generic to be really useful. Hence, we made it unchecked, so that it won't pollute tens of method signatures
 * in various modules with unnecessary `throws` clauses.
 */
class ClockworkAbortedException extends RuntimeException {

    ClockworkAbortedException() {
        super();
    }
}
