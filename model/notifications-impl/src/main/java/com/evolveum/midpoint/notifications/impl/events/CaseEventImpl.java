/*
 * Copyright (C) 2020-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
    protected String getCaseOrItemOutcome() {
        return aCase.getOutcome();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(this.getClass(), indent);
        debugDumpCommon(sb, indent);
        return sb.toString();
    }
}
