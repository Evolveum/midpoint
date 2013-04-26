/*
 * Copyright (c) 2013 Evolveum
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
 * Portions Copyrighted 2013 [name of copyright owner]
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
