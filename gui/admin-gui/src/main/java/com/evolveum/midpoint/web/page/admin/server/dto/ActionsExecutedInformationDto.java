/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
