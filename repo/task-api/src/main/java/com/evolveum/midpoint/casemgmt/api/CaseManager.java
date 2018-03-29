/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.casemgmt.api;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;

/**
 * Analogous to WorkflowManager, this component will be responsible for managing cases and their work items.
 * Currently its only responsibility is to distribute case-related events.
 *
 * Eventually it will be placed in its own module distinct from task-api/task-quartz-impl.
 *
 * @author mederly
 */
public interface CaseManager {

	void registerWorkItemListener(CaseWorkItemListener workItemListener);

	void unregisterWorkItemListener(CaseWorkItemListener workItemListener);

	void notifyWorkItemCreated(CaseWorkItemType workItem, CaseType aCase, Task task, OperationResult result);

}
