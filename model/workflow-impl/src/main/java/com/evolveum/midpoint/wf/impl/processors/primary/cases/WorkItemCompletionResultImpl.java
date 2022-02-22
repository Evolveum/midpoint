/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.processors.primary.cases;

import com.evolveum.midpoint.cases.api.extensions.WorkItemCompletionResult;

public class WorkItemCompletionResultImpl implements WorkItemCompletionResult {

    private final boolean shouldCloseOtherWorkItems;

    public WorkItemCompletionResultImpl(boolean shouldCloseOtherWorkItems) {
        this.shouldCloseOtherWorkItems = shouldCloseOtherWorkItems;
    }

    @Override
    public boolean shouldCloseOtherWorkItems() {
        return shouldCloseOtherWorkItems;
    }
}
