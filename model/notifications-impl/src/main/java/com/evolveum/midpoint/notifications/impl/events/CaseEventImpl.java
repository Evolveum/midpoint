/*
 * Copyright (C) 2020-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.notifications.impl.events;

import com.evolveum.midpoint.notifications.api.events.CaseEvent;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventCategoryType;

/**
 * Event related to the case as a whole.
 */
public class CaseEventImpl extends CaseManagementEventImpl implements CaseEvent {

    public CaseEventImpl(LightweightIdentifierGenerator lightweightIdentifierGenerator, ChangeType changeType, CaseType aCase) {
        super(lightweightIdentifierGenerator, changeType, aCase.getApprovalContext(), aCase);
    }

    @Override
    public boolean isCategoryType(EventCategoryType eventCategory) {
        return eventCategory == EventCategoryType.WORKFLOW_PROCESS_EVENT
                || eventCategory == EventCategoryType.WORKFLOW_EVENT;
    }

    @Override
    public String getCaseOrItemOutcome() {
        return aCase.getOutcome();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(this.getClass(), indent);
        debugDumpCommon(sb, indent);
        return sb.toString();
    }
}
