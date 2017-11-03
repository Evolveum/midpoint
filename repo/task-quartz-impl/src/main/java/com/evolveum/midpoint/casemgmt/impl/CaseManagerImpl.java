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

package com.evolveum.midpoint.casemgmt.impl;

import com.evolveum.midpoint.casemgmt.api.CaseManager;
import com.evolveum.midpoint.casemgmt.api.CaseWorkItemListener;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author mederly
 */
@Service(value = "caseManager")
public class CaseManagerImpl implements CaseManager {

	private static final Trace LOGGER = TraceManager.getTrace(CaseManagerImpl.class);

	private final Set<CaseWorkItemListener> workItemListeners = ConcurrentHashMap.newKeySet();

	@Override
	public void registerWorkItemListener(CaseWorkItemListener workItemListener) {
		workItemListeners.add(workItemListener);
	}

	@Override
	public void unregisterWorkItemListener(CaseWorkItemListener workItemListener) {
		workItemListeners.remove(workItemListener);
	}

	@Override
	public void notifyWorkItemCreated(CaseWorkItemType workItem, CaseType aCase, Task task, OperationResult result) {
		for (CaseWorkItemListener listener : workItemListeners) {
			try {
				listener.onWorkItemCreation(workItem, aCase, task, result);
			} catch (Throwable t) {
				LoggingUtils.logUnexpectedException(LOGGER, "Exception when invoking work item listener; work item = {}", t, workItem);
			}
		}
	}
}
