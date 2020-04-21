/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class CaseEventUtil {

    public static boolean completedByUserAction(WorkItemEventType event) {
        WorkItemEventCauseInformationType cause = event.getCause();
        return event.getInitiatorRef() != null &&
                (cause == null ||
                        cause.getType() == null ||
                        cause.getType() == WorkItemEventCauseTypeType.USER_ACTION);
    }

    public static List<CaseEventType> getEventsForStage(CaseType aCase, int stageNumber) {
        return aCase.getEvent().stream()
                .filter(e -> java.util.Objects.equals(e.getStageNumber(), stageNumber))
                .collect(Collectors.toList());
    }
}
