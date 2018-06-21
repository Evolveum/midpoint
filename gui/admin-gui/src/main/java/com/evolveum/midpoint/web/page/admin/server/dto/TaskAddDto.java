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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.evolveum.midpoint.xml.ns._public.common.common_3.MisfireActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ThreadStopActionType;

import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
public class TaskAddDto implements Serializable {

    public static final String F_DRY_RUN = "dryRun";
    public static final String F_FOCUS_TYPE = "focusType";
    public static final String F_KIND = "kind";
    public static final String F_INTENT = "intent";
    public static final String F_OBJECT_CLASS = "objectClass";
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
    private QName focusType;
    private ShadowKindType kind;
    private String intent;
    private String objectClass;
    private List<QName> objectClassList;

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

    public QName getFocusType() {
		return focusType;
	}

    public void setFocusType(QName focusType) {
		this.focusType = focusType;
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

    public String getObjectClass() {
        return objectClass;
    }

    public void setObjectClass(String objectClass) {
        this.objectClass = objectClass;
    }

    public List<QName> getObjectClassList() {
        if(objectClassList == null){
            objectClassList = new ArrayList<>();
        }

        return objectClassList;
    }

    public void setObjectClassList(List<QName> objectClassList) {
        this.objectClassList = objectClassList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskAddDto)) return false;

        TaskAddDto that = (TaskAddDto) o;

        if (bound != that.bound) return false;
        if (dryRun != that.dryRun) return false;
        if (reccuring != that.reccuring) return false;
        if (runUntilNodeDown != that.runUntilNodeDown) return false;
        if (suspendedState != that.suspendedState) return false;
        if (category != null ? !category.equals(that.category) : that.category != null) return false;
        if (cron != null ? !cron.equals(that.cron) : that.cron != null) return false;
        if (intent != null ? !intent.equals(that.intent) : that.intent != null) return false;
        if (interval != null ? !interval.equals(that.interval) : that.interval != null) return false;
        if (kind != that.kind) return false;
        if (misfireAction != that.misfireAction) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (notStartAfter != null ? !notStartAfter.equals(that.notStartAfter) : that.notStartAfter != null)
            return false;
        if (notStartBefore != null ? !notStartBefore.equals(that.notStartBefore) : that.notStartBefore != null)
            return false;
        if (focusType != null ? !focusType.equals(that.focusType) : that.focusType != null) return false;
        if (objectClass != null ? !objectClass.equals(that.objectClass) : that.objectClass != null) return false;
        if (objectClassList != null ? !objectClassList.equals(that.objectClassList) : that.objectClassList != null)
            return false;
        if (resource != null ? !resource.equals(that.resource) : that.resource != null) return false;
        if (threadStop != that.threadStop) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = category != null ? category.hashCode() : 0;
        result = 31 * result + (resource != null ? resource.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (reccuring ? 1 : 0);
        result = 31 * result + (bound ? 1 : 0);
        result = 31 * result + (interval != null ? interval.hashCode() : 0);
        result = 31 * result + (cron != null ? cron.hashCode() : 0);
        result = 31 * result + (notStartBefore != null ? notStartBefore.hashCode() : 0);
        result = 31 * result + (notStartAfter != null ? notStartAfter.hashCode() : 0);
        result = 31 * result + (runUntilNodeDown ? 1 : 0);
        result = 31 * result + (suspendedState ? 1 : 0);
        result = 31 * result + (threadStop != null ? threadStop.hashCode() : 0);
        result = 31 * result + (misfireAction != null ? misfireAction.hashCode() : 0);
        result = 31 * result + (dryRun ? 1 : 0);
        result = 31 * result + (kind != null ? kind.hashCode() : 0);
        result = 31 * result + (intent != null ? intent.hashCode() : 0);
        result = 31 * result + (focusType != null ? focusType.hashCode() : 0);
        result = 31 * result + (objectClass != null ? objectClass.hashCode() : 0);
        result = 31 * result + (objectClassList != null ? objectClassList.hashCode() : 0);
        return result;
    }
}
