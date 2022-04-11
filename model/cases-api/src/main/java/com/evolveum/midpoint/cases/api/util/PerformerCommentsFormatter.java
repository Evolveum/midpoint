/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.cases.api.util;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemCompletionEventType;
import org.jetbrains.annotations.NotNull;

/**
 * Ensures formatting of performers (approvers, reviewers) comments before storing them into metadata.
 * May optimize repository accesses by caching performers information retrieved.
 */
public interface PerformerCommentsFormatter {

    PerformerCommentsFormatter EMPTY = new EmptyPerformerCommentsFormatterImpl();

    String formatComment(@NotNull AbstractWorkItemType workItem, Task task, OperationResult result);

    String formatComment(@NotNull WorkItemCompletionEventType event, Task task, OperationResult result);

    /**
     * Simple "no-op" formatter to be used when no real implementation is available.
     */
    class EmptyPerformerCommentsFormatterImpl implements PerformerCommentsFormatter {

        @Override
        public String formatComment(@NotNull AbstractWorkItemType workItem, Task task, OperationResult result) {
            return workItem.getOutput() != null ? workItem.getOutput().getComment() : null;
        }

        @Override
        public String formatComment(@NotNull WorkItemCompletionEventType event, Task task, OperationResult result) {
            return event.getOutput() != null ? event.getOutput().getComment() : null;
        }
    }
}
