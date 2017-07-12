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

package com.evolveum.midpoint.web.page.admin.server.dto;

import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.web.page.admin.server.handlers.dto.HandlerDtoEditableState;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MisfireActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScheduleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ThreadStopActionType;

import java.io.Serializable;
import java.util.Date;

/**
 * @author mederly
 */
public class TaskEditableState implements Serializable, Cloneable {

	String name;
	String description;

	boolean bound;
	boolean recurring;
	Integer interval;
	String cronSpecification;
	Date notStartBefore;
	Date notStartAfter;
	MisfireActionType misfireActionType;
	ThreadStopActionType threadStopActionType;

	Integer workerThreads;

	HandlerDtoEditableState handlerSpecificState;

	public TaskEditableState clone() {
		TaskEditableState clone = new TaskEditableState();
		clone.name = name;
		clone.description = description;
		clone.bound = bound;
		clone.recurring = recurring;
		clone.interval = interval;
		clone.cronSpecification = cronSpecification;
		clone.notStartBefore = CloneUtil.clone(notStartBefore);
		clone.notStartAfter = CloneUtil.clone(notStartAfter);
		clone.misfireActionType = misfireActionType;
		clone.threadStopActionType = threadStopActionType;
		clone.workerThreads = workerThreads;
		clone.handlerSpecificState = CloneUtil.clone(handlerSpecificState);
		return clone;
	}

	public ScheduleType getScheduleType() {
		ScheduleType rv = new ScheduleType();
		rv.setInterval(interval);
		rv.setCronLikePattern(cronSpecification);
		rv.setEarliestStartTime(MiscUtil.asXMLGregorianCalendar(notStartBefore));
		rv.setLatestStartTime(MiscUtil.asXMLGregorianCalendar(notStartAfter));
		rv.setMisfireAction(misfireActionType);
		return rv;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public boolean isBound() {
		return bound;
	}

	public boolean isRecurring() {
		return recurring;
	}

	public Integer getInterval() {
		return interval;
	}

	public String getCronSpecification() {
		return cronSpecification;
	}

	public Date getNotStartBefore() {
		return notStartBefore;
	}

	public Date getNotStartAfter() {
		return notStartAfter;
	}

	public MisfireActionType getMisfireActionType() {
		return misfireActionType;
	}

	public ThreadStopActionType getThreadStopActionType() {
		return threadStopActionType;
	}

	public Integer getWorkerThreads() {
		return workerThreads;
	}

	public HandlerDtoEditableState getHandlerSpecificState() {
		return handlerSpecificState;
	}
}
