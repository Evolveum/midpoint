/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.cases.impl.engine.helpers;

import java.util.Date;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemEventType;

/**
 *
 */
@Component
public class WorkItemHelper {

    public static void fillInWorkItemEvent(
            WorkItemEventType event,
            MidPointPrincipal currentUser,
            WorkItemId workItemId,
            CaseWorkItemType workItem) {
        if (currentUser != null) {
            event.setInitiatorRef(ObjectTypeUtil.createObjectRef(currentUser.getFocus()));
            event.setAttorneyRef(ObjectTypeUtil.createObjectRef(currentUser.getAttorney()));
        }
        event.setTimestamp(XmlTypeConverter.createXMLGregorianCalendar(new Date()));
        event.setExternalWorkItemId(workItemId.asString());
        event.setWorkItemId(workItemId.id);
        event.setOriginalAssigneeRef(workItem.getOriginalAssigneeRef());
        event.setStageNumber(workItem.getStageNumber());
        event.setEscalationLevel(workItem.getEscalationLevel());
    }

}
