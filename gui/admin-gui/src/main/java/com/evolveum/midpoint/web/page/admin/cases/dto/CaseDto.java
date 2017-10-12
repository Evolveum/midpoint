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

import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.List;

/**
 * DTO representing a particular Case.
 *
 * TODO cleanup a bit
 *
 * @author bpowers
 */
public class CaseDto extends Selectable {

    public static final String F_NAME = "name";
    public static final String F_OBJECT_NAME = "objectName";
    public static final String F_DESCRIPTION = "description";
    public static final String F_EVENT = "event";
    public static final String F_OUTCOME = "outcome";
    public static final String F_CLOSE_TIMESTAMP = "closeTimestamp";
    public static final String F_STATE = "state";

    @NotNull private final CaseType caseInstance;
    private String objectName;

    public CaseDto(@NotNull CaseType _case) {
        this.caseInstance = _case;
        this.objectName = getObjectName(this.caseInstance.getObjectRef());
    }

    // ugly hack (for now) - we extract the name from serialization metadata
    private String getObjectName(ObjectReferenceType ref) {
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
        return caseInstance.getObjectRef().getType();
    }

    public Long getCaseId() {
        return caseInstance.asPrismContainerValue().getId();
    }

    public CaseType getCase() {
        return caseInstance;
    }

    public String getName() {
        return caseInstance.getName().toString();
    }

    public String getDescription() {
        return caseInstance.getDescription();
    }

    public String getEvent() {
        return caseInstance.getEvent().toString();
    }

    public String getOutcome() {
        return caseInstance.getOutcome();
    }

    public XMLGregorianCalendar getCloseTimestamp() {
        return caseInstance.getCloseTimestamp();
    }

    public String getState() {
        return caseInstance.getState();
    }

    public List<CaseWorkItemType> getWorkItems() {
        return caseInstance.getWorkItem();
    }

    public CaseWorkItemType getWorkItem(Long caseWorkItemId) {
        List<CaseWorkItemType> caseWorkItems = caseInstance.getWorkItem();
        for (CaseWorkItemType caseWorkItem : caseWorkItems){
            if (caseWorkItem.getId().equals(caseWorkItemId)) {
                return caseWorkItem;
            }
        }
        return null;
    }
}
