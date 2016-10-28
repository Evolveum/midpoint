/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.wf.impl.processes.itemApproval;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.common.LightweightObjectRef;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.apache.commons.lang.Validate;

public class PrepareApprover implements JavaDelegate {

    private static final Trace LOGGER = TraceManager.getTrace(PrepareApprover.class);

    public void execute(DelegateExecution execution) {

        LightweightObjectRef approverRef = (LightweightObjectRef) execution.getVariable(ProcessVariableNames.APPROVER_REF);
        Validate.notNull(approverRef, "approverRef is undefined");

        String assignee = null;
        String candidateGroups = null;
        if (approverRef.getType() == null || UserType.COMPLEX_TYPE.equals(approverRef.getType())) {
            assignee = approverRef.getOid();
        } else if (RoleType.COMPLEX_TYPE.equals(approverRef.getType())) {
            candidateGroups = MiscDataUtil.ROLE_PREFIX + ":" + approverRef.getOid();
        } else if (OrgType.COMPLEX_TYPE.equals(approverRef.getType())) {
            candidateGroups = MiscDataUtil.ORG_PREFIX + ":" + approverRef.getOid();
        } else {
            throw new IllegalStateException("Unsupported type of the approver: " + approverRef.getType());
        }

        execution.setVariableLocal(ProcessVariableNames.ASSIGNEE, assignee);
        execution.setVariableLocal(ProcessVariableNames.CANDIDATE_GROUPS, candidateGroups);
    }
}
