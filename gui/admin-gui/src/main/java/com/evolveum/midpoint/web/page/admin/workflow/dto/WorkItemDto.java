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

package com.evolveum.midpoint.web.page.admin.workflow.dto;

import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.wf.api.WorkItem;

import java.text.DateFormat;

/**
 * @author lazyman
 */
public class WorkItemDto extends Selectable {

    public static final String F_NAME = "name";
    public static final String F_OWNER_OR_CANDIDATES = "ownerOrCandidates";
    public static final String F_CREATED = "created";

    WorkItem workItem;

    public WorkItemDto(WorkItem workItem) {
        this.workItem = workItem;
    }

    public String getName() {
        return workItem.getName();
    }

    public String getOwner() {
        return workItem.getAssignee();
    }

    public WorkItem getWorkItem() {
        return workItem;
    }

    public String getCandidates() {
        return workItem.getCandidates();
    }

    public String getCreated() {
        if (workItem.getCreateTime() != null) {
            return WebMiscUtil.getFormatedDate(workItem.getCreateTime());
        } else {
            return null;
        }
    }

    public String getOwnerOrCandidates() {
        if (workItem.getAssigneeName() != null) {
            return workItem.getAssigneeName();
        } else if (workItem.getAssignee() != null) {
            return workItem.getAssignee();      // todo ???
        } else if (workItem.getCandidates() != null) {
            return workItem.getCandidates();
        } else {
            return null;
        }
    }
}
