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

package com.evolveum.midpoint.notifications.api.events;

import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

/**
 * @author mederly
 */
abstract public class CaseManagementEvent extends BaseEvent {

    @NotNull protected final CaseType aCase;
    @NotNull private final ChangeType changeType;

    public CaseManagementEvent(@NotNull LightweightIdentifierGenerator lightweightIdentifierGenerator,
		    @NotNull ChangeType changeType, @NotNull CaseType aCase) {
        super(lightweightIdentifierGenerator);
        this.changeType = changeType;
		this.aCase = aCase;
    }

	@Override
    public boolean isStatusType(EventStatusType eventStatusType) {
        // temporary implementation
        return eventStatusType == EventStatusType.SUCCESS || eventStatusType == EventStatusType.ALSO_SUCCESS || eventStatusType == EventStatusType.IN_PROGRESS;
    }

    @NotNull
    public ChangeType getChangeType() {
        return changeType;
    }

	@NotNull
	public CaseType getCase() {
		return aCase;
	}

	@Override
    public boolean isOperationType(EventOperationType eventOperationType) {
        return changeTypeMatchesOperationType(changeType, eventOperationType);
    }

	@Override
	public String toString() {
		return "CaseManagementEvent{" +
				"aCase=" + aCase +
				", changeType=" + changeType +
				'}';
	}

	@Override
	protected void debugDumpCommon(StringBuilder sb, int indent) {
		super.debugDumpCommon(sb, indent);
		DebugUtil.debugDumpWithLabelLn(sb, "case", getCase().asPrismContainerValue(), indent + 1);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "changeType", changeType, indent + 1);
	}
}