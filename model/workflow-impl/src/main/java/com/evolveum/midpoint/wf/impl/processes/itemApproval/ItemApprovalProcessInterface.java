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

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processors.primary.PcpChildWfTaskCreationInstruction;
import com.evolveum.midpoint.wf.impl.tasks.WfTaskCreationInstruction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author mederly
 */
@Component
public class ItemApprovalProcessInterface {

	private static final Trace LOGGER = TraceManager.getTrace(ItemApprovalProcessInterface.class);

	@Autowired private PrismContext prismContext;

	private static final String PROCESS_DEFINITION_KEY = "ItemApproval";

    public void prepareStartInstruction(WfTaskCreationInstruction instruction) {
        instruction.setProcessName(PROCESS_DEFINITION_KEY);

        if (LOGGER.isDebugEnabled() && instruction instanceof PcpChildWfTaskCreationInstruction) {
			PcpChildWfTaskCreationInstruction instr = (PcpChildWfTaskCreationInstruction) instruction;
			LOGGER.debug("About to start approval process instance '{}'", instr.getProcessInstanceName());
			if (instr.getProcessContent() instanceof ItemApprovalSpecificContent) {
				ItemApprovalSpecificContent iasc = (ItemApprovalSpecificContent) instr.getProcessContent();
				LOGGER.debug("Approval schema XML:\n{}", PrismUtil.serializeQuietlyLazily(prismContext, iasc.approvalSchemaType));
				LOGGER.debug("Attached rules:\n{}", PrismUtil.serializeQuietlyLazily(prismContext, iasc.policyRules));
			}
		}
    }

}
