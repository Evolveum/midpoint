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

import com.evolveum.midpoint.web.component.model.delta.ContainerValueDto;
import com.evolveum.midpoint.web.component.model.delta.DeltaDto;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.wf.api.WorkItemDetailed;

/**
 * @author lazyman
 */
public class WorkItemDetailedDto extends Selectable {

    public static final String F_DELTA = "delta";
    public static final String F_REQUESTER = "requester";
    public static final String F_OBJECT_OLD = "objectOld";
    public static final String F_OBJECT_NEW = "objectNew";
    public static final String F_ADDITIONAL_DATA = "additionalData";

    WorkItemDetailed workItem;

    public WorkItemDetailedDto(WorkItemDetailed workItem) {
        this.workItem = workItem;
    }

    public String getName() {
        return workItem.getName();
    }

    public String getOwner() {
        return workItem.getAssignee();
    }

    public WorkItemDetailed getWorkItem() {
        return workItem;
    }

    public String getCandidates() {
        return workItem.getCandidates();
    }

    public DeltaDto getDelta() {
        return workItem.getObjectDelta() != null ? new DeltaDto(workItem.getObjectDelta()) : null;
    }

    public ContainerValueDto getRequester() {
        return new ContainerValueDto(workItem.getRequester());
    }

    public ContainerValueDto getObjectOld() {
        return new ContainerValueDto(workItem.getObjectOld());
    }

    public ContainerValueDto getObjectNew() {
        return new ContainerValueDto(workItem.getObjectNew());
    }

    public ContainerValueDto getAdditionalData() {
        return new ContainerValueDto(workItem.getAdditionalData());
    }
}
