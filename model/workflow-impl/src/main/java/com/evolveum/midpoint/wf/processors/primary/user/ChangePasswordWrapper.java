/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.wf.processors.primary.user;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.WfConstants;
import com.evolveum.midpoint.wf.WfTaskUtil;
import com.evolveum.midpoint.wf.activiti.ActivitiUtil;
import com.evolveum.midpoint.wf.messages.ProcessEvent;
import com.evolveum.midpoint.wf.processes.general.ApprovalRequest;
import com.evolveum.midpoint.wf.processes.general.Decision;
import com.evolveum.midpoint.wf.processes.general.ProcessVariableNames;
import com.evolveum.midpoint.wf.processors.primary.PrimaryApprovalProcessWrapper;
import com.evolveum.midpoint.wf.processors.primary.PrimaryChangeProcessor;
import com.evolveum.midpoint.wf.processors.primary.StartProcessInstructionForPrimaryStage;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.runtime.ProcessInstance;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.text.DateFormat;
import java.util.*;

/**
 * This is a preliminary version of 'password approval' process wrapper. The idea is that in some cases, a user may request
 * changing (resetting) his password, but if not enough authentication information is available, the change may be
 * subject to manual approval.
 *
 * Exact conditions should be coded into getApprovalRequestList method. Currently, we only test for ANY password change
 * request and push it into approval process.
 *
 * DO NOT USE THIS WRAPPER IN PRODUCTION UNLESS YOU KNOW WHAT YOU ARE DOING :)
 *
 * @author mederly
 */
@Component
public class ChangePasswordWrapper extends AbstractUserWrapper {

    private static final Trace LOGGER = TraceManager.getTrace(ChangePasswordWrapper.class);

    @Autowired
    private PrismContext prismContext;

    @Override
    public List<StartProcessInstructionForPrimaryStage> prepareProcessesToStart(ModelContext<?,?> modelContext, ObjectDelta<Objectable> change, Task task, OperationResult result) {

        List<ApprovalRequest<String>> approvalRequestList = new ArrayList<ApprovalRequest<String>>();
        List<StartProcessInstructionForPrimaryStage> instructions = new ArrayList<StartProcessInstructionForPrimaryStage>();

        if (change.getChangeType() != ChangeType.MODIFY) {
            return null;
        }

        Iterator<? extends ItemDelta> deltaIterator = change.getModifications().iterator();

        ItemPath passwordPath = new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE);
        while (deltaIterator.hasNext()) {
            ItemDelta delta = deltaIterator.next();

            // this needs to be customized and enhanced; e.g. to start wf process only when not enough authentication info is present in the request
            // also, what if we replace whole 'credentials' container?
            if (passwordPath.equivalent(delta.getPath())) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Found password-changing delta, moving it into approval request. Delta = " + delta.debugDump());
                }
                ApprovalRequest<String> approvalRequest = createApprovalRequest(delta);
                approvalRequestList.add(approvalRequest);
                instructions.add(createStartProcessInstruction(modelContext, delta, approvalRequest, task, result));
                deltaIterator.remove();
            }
        }
        return instructions;
    }

    private ApprovalRequest<String> createApprovalRequest(ItemDelta delta) {

        ObjectReferenceType approverRef = new ObjectReferenceType();
        approverRef.setOid(SystemObjectsType.USER_ADMINISTRATOR.value());
        approverRef.setType(UserType.COMPLEX_TYPE);

        List<ObjectReferenceType> approvers = new ArrayList<ObjectReferenceType>();
        approvers.add(approverRef);

        return new ApprovalRequest("Password change", null, approvers, null, null);
    }

    private StartProcessInstructionForPrimaryStage createStartProcessInstruction(ModelContext<?, ?> modelContext, ItemDelta delta, ApprovalRequest approvalRequest, Task task, OperationResult result) {

        String userName = getUserName(modelContext);
        String objectOid = getObjectOid(modelContext);
        PrismObject<UserType> requester = getRequester(task, result);

        StartProcessInstructionForPrimaryStage instruction = new StartProcessInstructionForPrimaryStage();

        prepareCommonInstructionAttributes(instruction, modelContext, objectOid, requester, task);

        instruction.setProcessName(GENERAL_APPROVAL_PROCESS);
        instruction.setSimple(true);

        instruction.setTaskName(new PolyStringType("Workflow for approving password change for " + userName));
        instruction.addProcessVariable(WfConstants.VARIABLE_PROCESS_NAME, "Changing password for " + userName);
        instruction.addProcessVariable(WfConstants.VARIABLE_START_TIME, new Date());

        instruction.addProcessVariable(ProcessVariableNames.APPROVAL_REQUEST, approvalRequest);
        instruction.addProcessVariable(ProcessVariableNames.APPROVAL_TASK_NAME, "Approve changing password for " + userName);

        instruction.setExecuteImmediately(ModelExecuteOptions.isExecuteImmediatelyAfterApproval(((LensContext) modelContext).getOptions()));
        instruction.setDelta(itemDeltaToObjectDelta(objectOid, delta));

        return instruction;
    }

    private ObjectDelta<Objectable> itemDeltaToObjectDelta(String objectOid, ItemDelta delta) {
        return (ObjectDelta<Objectable>) (ObjectDelta) ObjectDelta.createModifyDelta(objectOid, delta, UserType.class, prismContext);
    }

}
