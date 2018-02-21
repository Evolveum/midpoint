/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
