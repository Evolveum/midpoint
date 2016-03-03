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

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.model.delta.ContainerValueDto;
import com.evolveum.midpoint.web.component.model.delta.DeltaDto;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType;
import com.evolveum.midpoint.xml.ns.model.workflow.common_forms_3.GeneralChangeApprovalWorkItemContents;

/**
 * @author lazyman
 */
public class WorkItemDetailedDto extends WorkItemDto {

    public static final String F_DELTA = "delta";
    public static final String F_REQUESTER = "requester";
    public static final String F_OBJECT_OLD = "objectOld";
    public static final String F_OBJECT_NEW = "objectNew";
    public static final String F_RELATED_OBJECT = "relatedObject";

    DeltaDto deltaDto;

    public WorkItemDetailedDto(WorkItemType workItem, PrismContext prismContext) throws SchemaException {
        super(workItem);
        if (workItem.getContents() instanceof GeneralChangeApprovalWorkItemContents) {
            GeneralChangeApprovalWorkItemContents wic = (GeneralChangeApprovalWorkItemContents) workItem.getContents();
            if (wic.getObjectDelta() != null) {
                deltaDto = new DeltaDto(DeltaConvertor.createObjectDelta(wic.getObjectDelta(), prismContext));
            }
        }
        if (deltaDto == null) { // TODO!!!!
            deltaDto = new DeltaDto(ObjectDelta.createEmptyDelta(ObjectType.class, "dummy", prismContext, ChangeType.MODIFY));
        }
    }

    public WorkItemType getWorkItem() {
        return workItem;
    }

    public DeltaDto getDelta() {
        return deltaDto;
    }

    public ContainerValueDto getRequester() {
        if (workItem.getRequester() != null) {
            return new ContainerValueDto(workItem.getRequester().asPrismObject());
        } else {
            return null;
        }
    }

    private GeneralChangeApprovalWorkItemContents getWic() {
        if (workItem.getContents() instanceof GeneralChangeApprovalWorkItemContents) {
            return (GeneralChangeApprovalWorkItemContents) workItem.getContents();
        } else {
            return null;
        }
    }

    public ContainerValueDto getObjectOld() {
        if (getWic() != null && getWic().getObjectOld() != null) {
            return new ContainerValueDto(getWic().getObjectOld().asPrismObject());
        } else {
            return null;
        }
    }

    public ContainerValueDto getObjectNew() {
        if (getWic() != null && getWic().getObjectNew() != null) {
            return new ContainerValueDto(getWic().getObjectNew().asPrismObject());
        } else {
            return null;
        }
    }

    public ContainerValueDto getRelatedObject() {
        if (getWic() != null && getWic().getRelatedObject() != null) {
            return new ContainerValueDto(getWic().getRelatedObject().asPrismObject());
        } else {
            return null;
        }
    }

    public ObjectType getContents() {
        if (workItem != null) {
            return workItem.getContents();
        } else {
            return null;
        }
    }
}
