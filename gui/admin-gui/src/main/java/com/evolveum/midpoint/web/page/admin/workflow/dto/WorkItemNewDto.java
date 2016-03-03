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

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author lazyman
 */
public class WorkItemNewDto extends Selectable {

    public static final String F_NAME = "name";
    public static final String F_OWNER_OR_CANDIDATES = "ownerOrCandidates";
    public static final String F_CANDIDATES = "candidates";
    public static final String F_ASSIGNEE = "assignee";
    public static final String F_CREATED = "created";

    public static final String F_OBJECT_NAME = "objectName";
    public static final String F_TARGET_NAME = "targetName";
    public static final String F_PROCESS_STARTED = "processStarted";

    protected WorkItemNewType workItem;

    public WorkItemNewDto(WorkItemNewType workItem) {
        this.workItem = workItem;
    }

    public String getName() {
        return workItem.getName();
    }

    public String getWorkItemId() {
        return workItem.getWorkItemId();
    }

    public String getCreated() {
        return WebComponentUtil.formatDate(XmlTypeConverter.toDate(workItem.getWorkItemCreatedTimestamp()));
    }

    public String getProcessStarted() {
        return WebComponentUtil.formatDate(XmlTypeConverter.toDate(workItem.getProcessStartedTimestamp()));
    }

    public String getOwnerOrCandidates() {
        String assignee = getAssignee();
        if (assignee != null) {
            return assignee;
        } else {
            return getCandidates();
        }
    }

    public String getAssignee() {
        return WebComponentUtil.getName(workItem.getAssigneeRef());
    }

    public String getCandidates() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (ObjectReferenceType roleRef : workItem.getCandidateRolesRef()) {
            if (!first) {
                sb.append(", ");
            } else {
                first = false;
            }
            sb.append(WebComponentUtil.getName(roleRef));
            if (RoleType.COMPLEX_TYPE.equals(roleRef.getType())) {
                sb.append(" (role)");
            } else if (OrgType.COMPLEX_TYPE.equals(roleRef.getType())) {
                sb.append(" (org)");
            }
        }
        for (ObjectReferenceType userRef : workItem.getCandidateUsersRef()) {
            if (!first) {
                sb.append(", ");
            } else {
                first = false;
            }
            sb.append(WebComponentUtil.getName(userRef));
            sb.append(" (user)");
        }
        return sb.toString();
    }

    public String getObjectName() {
        return WebComponentUtil.getName(workItem.getObjectRef());
    }

    public String getTargetName() {
        return WebComponentUtil.getName(workItem.getTargetRef());
    }
}
