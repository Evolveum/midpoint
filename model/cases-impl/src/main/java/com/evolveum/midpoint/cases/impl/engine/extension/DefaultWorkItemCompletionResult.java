/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.cases.impl.engine.extension;

import com.evolveum.midpoint.cases.api.extensions.WorkItemCompletionResult;

public class DefaultWorkItemCompletionResult implements WorkItemCompletionResult {

    @Override
    public boolean shouldCloseOtherWorkItems() {
        // Operators are equivalent: if one completes the item, all items are done. (Moreover, there should be only one item.)
        return true;
    }
}
