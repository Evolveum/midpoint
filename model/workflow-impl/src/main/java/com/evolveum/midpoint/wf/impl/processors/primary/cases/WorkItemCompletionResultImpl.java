/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
