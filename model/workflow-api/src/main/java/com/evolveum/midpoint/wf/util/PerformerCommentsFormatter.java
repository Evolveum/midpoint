/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.util;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemCompletionEventType;
import org.jetbrains.annotations.NotNull;

/**
 * Ensures formatting of performers (approvers, reviewers) comments before storing them into metadata.
 * May optimize repository accesses by caching performers information retrieved.
 *
 * @author mederly
 */
public interface PerformerCommentsFormatter {

    String formatComment(@NotNull AbstractWorkItemType workItem, Task task, OperationResult result);

    String formatComment(@NotNull WorkItemCompletionEventType event, Task task, OperationResult result);

}
