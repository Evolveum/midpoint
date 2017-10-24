/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.cases.dto;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.util.CaseWorkItemUtil;
import com.evolveum.midpoint.schema.util.WorkItemTypeUtil;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemOutputType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

/**
 * DTO representing a particular CaseWorkItem.
 *
 * TODO cleanup a bit
 *
 * @author bpowers
 */
public class CaseWorkItemDto extends Selectable {

    public static final String F_NAME = "name";
    public static final String F_OBJECT_NAME = "objectName";
    public static final String F_ASSIGNEES = "assignees";
    public static final String F_ORIGINAL_ASSIGNEE = "originalAssignee";
    public static final String F_DESCRIPTION = "description";
    public static final String F_OPEN_TIMESTAMP = "openTimestamp";
    public static final String F_CLOSE_TIMESTAMP = "closeTimestamp";
    public static final String F_DEADLINE = "deadline";
    public static final String F_STATE = "state";
    public static final String F_COMMENT = "comment";
    public static final String F_OUTCOME = "outcome";
    public static final String F_PROOF = "proof";

    @NotNull private final CaseWorkItemType workItem;

    private CaseType _case;
    private String objectName;

    public CaseWorkItemDto(@NotNull CaseWorkItemType workItem) {
        this._case = CaseWorkItemUtil.getCase(workItem);
        this.workItem = workItem;
        this.objectName = getName(this._case.getObjectRef());
    }

    // ugly hack (for now) - we extract the name from serialization metadata
    private String getName(ObjectReferenceType ref) {
        if (ref == null) {
            return null;
        }
        String name = ref.getTargetName() != null ? ref.getTargetName().getOrig() : null;
        if (name == null) {
            return "(" + ref.getOid() + ")";
        } else {
            return name.trim();
        }
    }

    public String getObjectName() {
        return objectName;
    }

    public QName getObjectType() {
        return _case.getObjectRef().getType();
    }

    public Long getCaseId() {
        return _case.asPrismContainerValue().getId();
    }

    public CaseType getCase() {
        return _case;
    }

    public String getOutcome() {
        return WorkItemTypeUtil.getOutcome(workItem);
    }

    public String getComment() {
        return WorkItemTypeUtil.getComment(workItem);
    }

    public void setComment(String value) {
        if (workItem.getOutput() == null) {
            workItem.beginOutput().comment(value);
        } else {
            workItem.getOutput().comment(value);
        }
    }

    public byte[] getProof() {
        return WorkItemTypeUtil.getProof(workItem);
    }

    public void setProof(byte[] value) {
        if (workItem.getOutput() == null) {
            workItem.beginOutput().proof(value);
        } else {
            workItem.getOutput().proof(value);
        }
    }

    public long getWorkItemId() {
        return workItem.getId();
    }

    public String getAssignees() {
        return WebComponentUtil.getReferencedObjectNames(workItem.getAssigneeRef(), false);
    }

    public String getOriginalAssignee() {
//        return WebComponentUtil.getReferencedObjectNames(Collections.singletonList(workItem.getOriginalAssigneeRef()), false);
        return WebComponentUtil.getName(workItem.getOriginalAssigneeRef());
    }

    public String getName() {
        return workItem.getName();
    }

    public AbstractWorkItemOutputType getOutput() {
        return workItem.getOutput();
    }

    public String getDescription() {
        return _case.getDescription();
    }

    public XMLGregorianCalendar getCloseTimestamp() {
        return workItem.getCloseTimestamp();
    }

    public XMLGregorianCalendar getOpenTimestamp() {
        return _case.getMetadata().getCreateTimestamp();
    }

    public XMLGregorianCalendar getDeadline() {
        return workItem.getDeadline();
    }

    public String getState() {
        return _case.getState();
    }
}
