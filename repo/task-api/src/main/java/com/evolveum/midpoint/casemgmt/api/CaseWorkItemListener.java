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
 * An interface through which external observers can be notified about work item related events.
 * Used e.g. for implementing workflow-related notifications.
 *
 * EXPERIMENTAL. This interface may change in near future.
 *
 * See also WorkItemListener interface (workflow-api).
 *
 * These two might be merged in the future. Until that we provide here the bare minimum needed
 * to implement currently required functionality.
 *
 * @author mederly
 */
public interface CaseWorkItemListener {

    /**
     * This method is called when a work item is created.
	 */
    void onWorkItemCreation(CaseWorkItemType workItem, CaseType aCase, Task task, OperationResult result);
}
