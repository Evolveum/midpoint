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

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectActionsExecutedEntryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActionsExecutedInformationType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Pavol Mederly
 */
public class ActionsExecutedInformationDto {

    public static final String F_OBJECTS_TABLE_LINES = "objectsTableLines";

    private ActionsExecutedInformationType actionsExecutedInformationType;

    public ActionsExecutedInformationDto(ActionsExecutedInformationType actionsExecutedInformationType) {
        this.actionsExecutedInformationType = actionsExecutedInformationType;
    }

    public List<ActionsExecutedObjectsTableLineDto> getObjectsTableLines() {
        List<ActionsExecutedObjectsTableLineDto> rv = new ArrayList<>();
        for (ObjectActionsExecutedEntryType entry : actionsExecutedInformationType.getObjectActionsEntry()) {
            rv.add(new ActionsExecutedObjectsTableLineDto(entry));
        }
        Collections.sort(rv);
        return rv;
    }

    public List<ActionsExecutedObjectsTableLineDto> getUniqueObjectsTableLines() {
        List<ActionsExecutedObjectsTableLineDto> rv = new ArrayList<>();
        for (ObjectActionsExecutedEntryType entry : actionsExecutedInformationType.getResultingObjectActionsEntry()) {
            rv.add(new ActionsExecutedObjectsTableLineDto(entry));
        }
        Collections.sort(rv);
        return rv;
    }

}
