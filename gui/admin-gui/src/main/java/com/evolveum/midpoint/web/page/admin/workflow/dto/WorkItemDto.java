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
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang.StringUtils;

import java.util.List;

/**
 * @author lazyman
 */
public class WorkItemDto extends Selectable {

    public static final String F_NAME = "name";
    public static final String F_OWNER_OR_CANDIDATES = "ownerOrCandidates";
    public static final String F_CANDIDATES = "candidates";
    public static final String F_ASSIGNEE = "assignee";
    public static final String F_CREATED = "created";

    public static final String F_OBJECT_NAME = "objectName";
    public static final String F_TARGET_NAME = "targetName";
    public static final String F_PROCESS_STARTED = "processStarted";

    protected WorkItemType workItem;

    public WorkItemDto(WorkItemType workItem) {
        this.workItem = workItem;
    }

    public String getName() {
        return PolyString.getOrig(workItem.getName());
    }

//    public String getOwner() {
//        return workItem.getAssigneeRef() != null ? workItem.getAssigneeRef().getOid() : null;
//    }

    public WorkItemType getWorkItem() {
        return workItem;
    }

    public String getCreated() {
        if (workItem.getMetadata() != null && workItem.getMetadata().getCreateTimestamp() != null) {
            return WebComponentUtil.formatDate(XmlTypeConverter.toDate(workItem.getMetadata().getCreateTimestamp()));
        } else {
            return null;
        }
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
        if (workItem.getAssignee() != null) {
            if (workItem.getAssignee().getName() != null) {
                return workItem.getAssignee().getName().getOrig();
            } else {
                return workItem.getAssignee().getOid();
            }
        } else if (workItem.getAssigneeRef() != null) {
            return workItem.getAssigneeRef().getOid();
        } else {
            return null;
        }
    }

    // what an ugly method :| TODO rework some day [also add users]
    public String getCandidates() {
        StringBuilder retval = new StringBuilder();
        boolean first = true;
        boolean referenceOnly = false;
        // we assume that either all roles have full reference information, or none of them
        for (AbstractRoleType roleType : workItem.getCandidateRoles()) {
            if (!first) {
                retval.append(", ");
            } else {
                first = false;
            }
            if (roleType.getOid() == null) {        // no object information, only reference is present
                referenceOnly = true;
                break;
            }
            retval.append(PolyString.getOrig(roleType.getName()));
            if (roleType instanceof RoleType) {
                retval.append(" (role)");
            } else if (roleType instanceof OrgType) {
                retval.append(" (org)");
            }
        }
        if (referenceOnly) {
            // start again
            retval = new StringBuilder();
            first = true;
            for (ObjectReferenceType roleRef : workItem.getCandidateRolesRef()) {
                if (!first) {
                    retval.append(", ");
                } else {
                    first = false;
                }
                retval.append(roleRef.getOid());
                if (RoleType.COMPLEX_TYPE.equals(roleRef.getType())) {
                    retval.append(" (role)");
                } else if (OrgType.COMPLEX_TYPE.equals(roleRef.getType())) {
                    retval.append(" (org)");
                }
            }
        }
        return retval.toString();
    }

    public String getWorkItemId() {
        return workItem.getWorkItemId();
    }
}
