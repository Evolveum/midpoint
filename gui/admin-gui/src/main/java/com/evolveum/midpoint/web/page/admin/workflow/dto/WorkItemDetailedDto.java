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
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.model.delta.ContainerValueDto;
import com.evolveum.midpoint.web.component.model.delta.DeltaDto;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.WorkItemType;

/**
 * @author lazyman
 */
public class WorkItemDetailedDto extends Selectable {

    public static final String F_DELTA = "delta";
    public static final String F_REQUESTER = "requester";
    public static final String F_OBJECT_OLD = "objectOld";
    public static final String F_OBJECT_NEW = "objectNew";
    public static final String F_RELATED_OBJECT = "relatedObject";

    WorkItemType workItem;
    DeltaDto deltaDto;

    public WorkItemDetailedDto(WorkItemType workItem, PrismContext prismContext) throws SchemaException {
        this.workItem = workItem;
        if (workItem.getObjectDelta() != null) {
            deltaDto = new DeltaDto(DeltaConvertor.createObjectDelta(workItem.getObjectDelta(), prismContext));
        }
    }

    public String getName() {
        return PolyString.getOrig(workItem.getName());
    }

    public String getOwner() {
        return workItem.getAssigneeRef() != null ? workItem.getAssigneeRef().getOid() : null;
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

    public ContainerValueDto getObjectOld() {
        if (workItem.getObjectOld() != null) {
            return new ContainerValueDto(workItem.getObjectOld().asPrismObject());
        } else {
            return null;
        }
    }

    public ContainerValueDto getObjectNew() {
        if (workItem.getObjectNew() != null) {
            return new ContainerValueDto(workItem.getObjectNew().asPrismObject());
        } else {
            return null;
        }
    }

    public ContainerValueDto getRelatedObject() {
        if (workItem.getRelatedObject() != null) {
            return new ContainerValueDto(workItem.getRelatedObject().asPrismObject());
        } else {
            return null;
        }
    }
}
