/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.wf.processors.primary.user;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.StartProcessInstruction;
import com.evolveum.midpoint.wf.WfTaskUtil;
import com.evolveum.midpoint.wf.activiti.ActivitiUtil;
import com.evolveum.midpoint.wf.api.ProcessInstance;
import com.evolveum.midpoint.wf.messages.ProcessEvent;
import com.evolveum.midpoint.wf.processes.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.processes.StringHolder;
import com.evolveum.midpoint.wf.processes.general.Constants;
import com.evolveum.midpoint.wf.processes.general.Decision;
import com.evolveum.midpoint.wf.processes.general.ProcessVariableNames;
import com.evolveum.midpoint.wf.processors.ChangeProcessor;
import com.evolveum.midpoint.wf.processors.primary.PrimaryApprovalProcessWrapper;
import com.evolveum.midpoint.wf.processors.primary.StartProcessInstructionForPrimaryStage;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import org.springframework.beans.factory.annotation.Autowired;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Pavol
 */
public abstract class AbstractWrapper implements PrimaryApprovalProcessWrapper {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractWrapper.class);

    public static final String GENERAL_APPROVAL_PROCESS = "ItemApproval";
    private static final String DEFAULT_PROCESS_INSTANCE_DETAILS_PANEL_NAME = Constants.DEFAULT_PANEL_NAME;

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    WfTaskUtil wfTaskUtil;

    ChangeProcessor changeProcessor;

    String getObjectOid(ModelContext<?,?> modelContext) {
        ModelElementContext<UserType> fc = (ModelElementContext<UserType>) modelContext.getFocusContext();
        String objectOid = null;
        if (fc.getObjectNew() != null && fc.getObjectNew().getOid() != null) {
            return fc.getObjectNew().getOid();
        } else if (fc.getObjectOld() != null && fc.getObjectOld().getOid() != null) {
            return fc.getObjectOld().getOid();
        } else {
            return null;
        }
    }

    PrismObject<UserType> getRequester(Task task, OperationResult result) {
        // let's get fresh data (not the ones read on user login)
        PrismObject<UserType> requester = null;
        try {
            requester = ((PrismObject<UserType>) repositoryService.getObject(UserType.class, task.getOwner().getOid(), result));
        } catch (ObjectNotFoundException e) {
            LoggingUtils.logException(LOGGER, "Couldn't get data about task requester (" + task.getOwner() + "), because it does not exist in repository anymore. Using cached data.", e);
            requester = task.getOwner().clone();
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Couldn't get data about task requester (" + task.getOwner() + "), due to schema exception. Using cached data.", e);
            requester = task.getOwner().clone();
        }

        if (requester != null) {
            resolveRolesAndOrgUnits(requester, result);
        }

        return requester;
    }

    void resolveRolesAndOrgUnits(PrismObject<UserType> user, OperationResult result) {
        for (AssignmentType assignmentType : user.asObjectable().getAssignment()) {
            if (assignmentType.getTargetRef() != null && assignmentType.getTarget() == null) {
                QName type = assignmentType.getTargetRef().getType();
                if (RoleType.COMPLEX_TYPE.equals(type) || OrgType.COMPLEX_TYPE.equals(type)) {
                    String oid = assignmentType.getTargetRef().getOid();
                    try {
                        PrismObject<ObjectType> o = repositoryService.getObject(ObjectType.class, oid, result);
                        assignmentType.setTarget(o.asObjectable());
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("Resolved {} to {} in {}", new Object[]{oid, o, user});
                        }
                    } catch (ObjectNotFoundException e) {
                        LoggingUtils.logException(LOGGER, "Couldn't resolve reference to {} in {}", e, oid, user);
                    } catch (SchemaException e) {
                        LoggingUtils.logException(LOGGER, "Couldn't resolve reference to {} in {}", e, oid, user);
                    }
                }
            }
        }
    }

    void prepareCommonInstructionAttributes(StartProcessInstructionForPrimaryStage instruction, ModelContext<?,?> modelContext, String objectOid, PrismObject<UserType> requester, Task task) {
        ModelElementContext<UserType> fc = (ModelElementContext<UserType>) modelContext.getFocusContext();

        instruction.addProcessVariable(CommonProcessVariableNames.VARIABLE_MIDPOINT_REQUESTER_OID, task.getOwner().getOid());
        if (objectOid != null) {
            instruction.addProcessVariable(CommonProcessVariableNames.VARIABLE_MIDPOINT_OBJECT_OID, objectOid);
        }

        instruction.addProcessVariable(CommonProcessVariableNames.VARIABLE_UTIL, new ActivitiUtil());
        instruction.addProcessVariable(CommonProcessVariableNames.VARIABLE_MIDPOINT_PROCESS_WRAPPER, this.getClass().getName());
        instruction.addProcessVariable(CommonProcessVariableNames.VARIABLE_MIDPOINT_CHANGE_PROCESSOR, changeProcessor.getClass().getName());
        instruction.addProcessVariable(CommonProcessVariableNames.VARIABLE_START_TIME, new Date());
        instruction.setWrapper(this);
        instruction.setNoProcess(false);
    }

    public void setDeltaProcessVariable(StartProcessInstruction instruction, ObjectDelta delta) {
        try {
            instruction.addProcessVariable(CommonProcessVariableNames.VARIABLE_MIDPOINT_DELTA, new StringHolder(DeltaConvertor.toObjectDeltaTypeXml(delta)));
        } catch(JAXBException e) {
            throw new SystemException("Couldn't store primary delta into the process variable due to JAXB exception", e);
        } catch (SchemaException e) {
            throw new SystemException("Couldn't store primary delta into the process variable due to schema exception", e);
        }
    }


    /*
     * In this case, mapping deltaIn -> deltaOut is extremely simple.
     * DeltaIn contains a delta that has to be approved. Workflow answers simply yes/no.
     * Therefore, we either copy DeltaIn to DeltaOut, or generate an empty list of modifications.
     */

    @Override
    public List<ObjectDelta<Objectable>> prepareDeltaOut(ProcessEvent event, Task task, OperationResult result) throws SchemaException {
        List<ObjectDelta<Objectable>> deltaIn = wfTaskUtil.retrieveDeltasToProcess(task);
        if (Boolean.TRUE.equals(event.getAnswer())) {
            return new ArrayList<ObjectDelta<Objectable>>(deltaIn);
        } else {
            return new ArrayList<ObjectDelta<Objectable>>();
        }
    }

    /*
     * Default implementation of getApprovedBy expects that we are using general item approval process.
     */

    @Override
    public List<ObjectReferenceType> getApprovedBy(ProcessEvent event) {
        List<ObjectReferenceType> retval = new ArrayList<ObjectReferenceType>();
        if (!Boolean.TRUE.equals(event.getAnswer())) {
            return retval;
        }
        List<Decision> allDecisions = (List<Decision>) event.getVariable(ProcessVariableNames.ALL_DECISIONS);
        for (Decision decision : allDecisions) {
            if (decision.isApproved()) {
                ObjectReferenceType approverRef = new ObjectReferenceType();
                approverRef.setOid(decision.getApproverOid());
                retval.add(approverRef);
            }
        }

        return retval;
    }

    @Override
    public ChangeProcessor getChangeProcessor() {
        return changeProcessor;
    }

    @Override
    public void setChangeProcessor(ChangeProcessor changeProcessor) {
        this.changeProcessor = changeProcessor;
    }

    @Override
    public String getProcessInstanceDetailsPanelName(ProcessInstance processInstance) {
        return DEFAULT_PROCESS_INSTANCE_DETAILS_PANEL_NAME;
    }
}
