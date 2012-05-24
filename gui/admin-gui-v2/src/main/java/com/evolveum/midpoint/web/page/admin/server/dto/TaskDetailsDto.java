/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.server.dto;

import java.io.Serializable;
import java.util.Date;

import com.evolveum.midpoint.xml.ns._public.common.common_1.MisfireActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ThreadStopActionType;

/**
 * @author lazyman
 */
public class TaskDetailsDto implements Serializable {

	private String type;
	private TaskDetailsResourcesDto resource;
	private String name;
	
	//Scheduling
	private boolean reccuring;
	private boolean bound;
	private Integer interval;
	private String cron;
	private Date notStopBefore;
	private Date notStartAfter;
	
	private boolean runUntilNodeDown;
	
	//Advanced
	private boolean suspendedState;
	private ThreadStopActionType threadStop;
	private MisfireActionType misfireAction = MisfireActionType.EXECUTE_IMMEDIATELY;
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public TaskDetailsResourcesDto getResource() {
		return resource;
	}

	public void setResource(TaskDetailsResourcesDto resource) {
		this.resource = resource;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Boolean getReccuring() {
		return reccuring;
	}

	public void setReccuring(Boolean reccuring) {
		this.reccuring = reccuring;
	}

	public Boolean getBound() {
		return bound;
	}

	public void setBound(Boolean bound) {
		this.bound = bound;
	}

	public Integer getInterval() {
		return interval;
	}

	public void setInterval(Integer interval) {
		this.interval = interval;
	}

	public String getCron() {
		return cron;
	}

	public void setCron(String cron) {
		this.cron = cron;
	}

	public Date getNotStopBefore() {
		return notStopBefore;
	}

	public void setNotStopBefore(Date notStopBefore) {
		this.notStopBefore = notStopBefore;
	}

	public Date getNotStartAfter() {
		return notStartAfter;
	}

	public void setNotStartAfter(Date notStartAfter) {
		this.notStartAfter = notStartAfter;
	}

	public Boolean getRunUntilNodeDown() {
		return runUntilNodeDown;
	}

	public void setRunUntilNodeDown(Boolean runUntilNodeDown) {
		this.runUntilNodeDown = runUntilNodeDown;
	}

	public Boolean getSuspendedState() {
		return suspendedState;
	}

	public void setSuspendedState(Boolean suspendedState) {
		this.suspendedState = suspendedState;
	}
	
	public ThreadStopActionType getThreadStop() {
		return threadStop;
	}

	public void setThreadStop(ThreadStopActionType threadStop) {
		this.threadStop = threadStop;
	}

	public MisfireActionType getMisfireAction() {
		return misfireAction;
	}

	public void setMisfireAction(MisfireActionType misfireAction) {
		this.misfireAction = misfireAction;
	}
    
}
