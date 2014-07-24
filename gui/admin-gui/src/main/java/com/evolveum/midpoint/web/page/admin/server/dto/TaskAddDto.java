/*
 * Copyright (c) 2010-2013 Evolveum
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

import java.io.Serializable;
import java.util.Date;

import com.evolveum.midpoint.xml.ns._public.common.common_3.MisfireActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ThreadStopActionType;

/**
 * @author lazyman
 */
public class TaskAddDto implements Serializable {

    public static final String F_DRY_RUN = "dryRun";
    public static final String F_KIND = "kind";
    public static final String F_INTENT = "intent";
    public static final String F_RESOURCE = "resource";
    public static final String F_CATEGORY = "category";
    public static final String F_NAME = "name";
    public static final String F_RECURRING = "reccuring";
    public static final String F_BOUND = "bound";
    public static final String F_INTERVAL = "interval";
    public static final String F_CRON = "cron";
    public static final String F_NOT_START_BEFORE = "notStartBefore";
    public static final String F_NOT_START_AFTER = "notStartAfter";
    public static final String F_RUN_UNTIL_NODW_DOWN = "runUntilNodeDown";
    public static final String F_SUSPENDED_STATE = "suspendedState";
    public static final String F_THREAD_STOP = "threadStop";
    public static final String F_MISFIRE_ACTION = "misfireAction";

	private String category;
	private TaskAddResourcesDto resource;
	private String name;
	
	//Scheduling
	private boolean reccuring;
	private boolean bound;
	private Integer interval;
	private String cron;
	private Date notStartBefore;
	private Date notStartAfter;
	
	private boolean runUntilNodeDown;
	
	//Advanced
	private boolean suspendedState;
	private ThreadStopActionType threadStop;
	private MisfireActionType misfireAction = MisfireActionType.EXECUTE_IMMEDIATELY;

    private boolean dryRun;
    private ShadowKindType kind;
    private String intent;
	
	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}
	
	public TaskAddResourcesDto getResource() {
		return resource;
	}

	public void setResource(TaskAddResourcesDto resource) {
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

	public Date getNotStartBefore() {
		return notStartBefore;
	}

	public void setNotStartBefore(Date notStartBefore) {
		this.notStartBefore = notStartBefore;
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

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public ShadowKindType getKind() {
        return kind;
    }

    public void setKind(ShadowKindType kind) {
        this.kind = kind;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }
}
