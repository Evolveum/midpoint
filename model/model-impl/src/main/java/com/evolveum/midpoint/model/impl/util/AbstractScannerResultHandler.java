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
package com.evolveum.midpoint.model.impl.util;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeResultHandler;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public abstract class AbstractScannerResultHandler<O extends ObjectType> extends
		AbstractSearchIterativeResultHandler<O> {

	protected XMLGregorianCalendar lastScanTimestamp;
	protected XMLGregorianCalendar thisScanTimestamp;

	public AbstractScannerResultHandler(Task coordinatorTask, String taskOperationPrefix,
			String processShortName, String contextDesc, TaskManager taskManager) {
		super(coordinatorTask, taskOperationPrefix, processShortName, contextDesc, taskManager);
	}

	public XMLGregorianCalendar getLastScanTimestamp() {
		return lastScanTimestamp;
	}

	public void setLastScanTimestamp(XMLGregorianCalendar lastScanTimestamp) {
		this.lastScanTimestamp = lastScanTimestamp;
	}

	public XMLGregorianCalendar getThisScanTimestamp() {
		return thisScanTimestamp;
	}

	public void setThisScanTimestamp(XMLGregorianCalendar thisScanTimestamp) {
		this.thisScanTimestamp = thisScanTimestamp;
	}



}
