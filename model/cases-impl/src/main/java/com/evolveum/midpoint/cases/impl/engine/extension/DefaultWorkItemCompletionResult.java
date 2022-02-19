/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
