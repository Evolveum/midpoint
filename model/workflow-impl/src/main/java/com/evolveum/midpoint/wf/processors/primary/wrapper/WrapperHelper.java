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

package com.evolveum.midpoint.wf.processors.primary.wrapper;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.jobs.WfTaskUtil;
import com.evolveum.midpoint.wf.messages.ProcessEvent;
import com.evolveum.midpoint.wf.processes.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.processes.StringHolder;
import com.evolveum.midpoint.wf.processes.itemApproval.Decision;
import com.evolveum.midpoint.wf.processes.itemApproval.ProcessVariableNames;
import com.evolveum.midpoint.wf.processors.primary.PcpChildJobCreationInstruction;
import com.evolveum.midpoint.wf.processors.primary.PcpJob;
import com.evolveum.midpoint.wf.processors.primary.PrimaryChangeProcessor;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mederly
 */
@Component
public class WrapperHelper {

    private static final Trace LOGGER = TraceManager.getTrace(WrapperHelper.class);

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private WfTaskUtil wfTaskUtil;

    //region ========================================================================== Jobs-related methods
    public void prepareCommonInstructionAttributes(PrimaryChangeProcessor changeProcessor, PrimaryApprovalProcessWrapper wrapper, PcpChildJobCreationInstruction instruction, ModelContext<?> modelContext, String objectOid, PrismObject<UserType> requester) throws SchemaException {

        instruction.setRequesterOidInProcess(requester);
        instruction.setObjectOidInProcess(objectOid);

        instruction.setExecuteApprovedChangeImmediately(ModelExecuteOptions.isExecuteImmediatelyAfterApproval(((LensContext) modelContext).getOptions()));

        instruction.addProcessVariable(CommonProcessVariableNames.VARIABLE_MIDPOINT_PROCESS_WRAPPER, wrapper.getClass().getName());
        instruction.addTaskVariable(wfTaskUtil.getWfProcessWrapperPropertyDefinition(), wrapper.getClass().getName());

        if (instruction.isExecuteApprovedChangeImmediately()) {
            // actually, context should be emptied anyway; but to be sure, let's do it here as well
            instruction.addTaskModelContext(changeProcessor.contextCopyWithNoDelta((LensContext) modelContext));
            instruction.setExecuteModelOperationHandler(true);
        }
    }

    public void setDeltaProcessAndTaskVariables(PcpChildJobCreationInstruction instruction, ObjectDelta delta) {
        try {
            instruction.addProcessVariable(CommonProcessVariableNames.VARIABLE_MIDPOINT_DELTA, new StringHolder(DeltaConvertor.toObjectDeltaTypeXml(delta)));
        } catch(JAXBException e) {
            throw new SystemException("Couldn't store primary delta into the process variable due to JAXB exception", e);
        } catch (SchemaException e) {
            throw new SystemException("Couldn't store primary delta into the process variable due to schema exception", e);
        }

        try {
            instruction.addTaskDeltasVariable(wfTaskUtil.getWfDeltaToProcessPropertyDefinition(), delta);
        } catch (SchemaException e) {
            throw new SystemException("Couldn't store primary delta into the task variable due to schema exception", e);
        }
    }
    //endregion

    //region ========================================================================== Default implementation of wrapper methods
    /**
     * Prepares deltaOut from deltaIn, based on process instance variables.
     * (Default implementation of the method from PrimaryApprovalProcessWrapper.)
     *
     * In the default case, mapping deltaIn -> deltaOut is extremely simple.
     * DeltaIn contains a delta that has to be approved. Workflow answers simply yes/no.
     * Therefore, we either copy DeltaIn to DeltaOut, or generate an empty list of modifications.
     */
    public List<ObjectDelta<Objectable>> prepareDeltaOut(ProcessEvent event, PcpJob pcpJob, OperationResult result) throws SchemaException {
        List<ObjectDelta<Objectable>> deltaIn = pcpJob.retrieveDeltasToProcess();
        if (CommonProcessVariableNames.isApproved(event.getAnswer())) {
            return new ArrayList<>(deltaIn);
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * Default implementation of getApprovedBy; it expects that we are using general item approval process.
     */
    public List<ObjectReferenceType> getApprovedBy(ProcessEvent event) {
        List<ObjectReferenceType> retval = new ArrayList<ObjectReferenceType>();
        if (!CommonProcessVariableNames.isApproved(event.getAnswer())) {
            return retval;
        }
        List<Decision> allDecisions = (List<Decision>) event.getVariable(ProcessVariableNames.ALL_DECISIONS);
        for (Decision decision : allDecisions) {
            if (decision.isApproved()) {
                retval.add(MiscSchemaUtil.createObjectReference(decision.getApproverOid(), SchemaConstants.C_USER_TYPE));
            }
        }

        return retval;
    }
    //endregion

    //region ========================================================================== Miscellaneous
    public String getObjectOid(ModelContext<?> modelContext) {
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

    public PrismObject<UserType> getRequester(Task task, OperationResult result) {
        // let's get fresh data (not the ones read on user login)
        PrismObject<UserType> requester = null;
        try {
            requester = ((PrismObject<UserType>) repositoryService.getObject(UserType.class, task.getOwner().getOid(), null, result));
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

    public ObjectType resolveObjectRef(AssignmentType a, OperationResult result) {

        if (a == null) {
            return null;
        }

        ObjectType object = a.getTarget();
        if (object == null) {
            if (a.getTargetRef() == null || a.getTargetRef().getOid() == null) {
                return null;
            }
            try {
                object = repositoryService.getObject(ObjectType.class, a.getTargetRef().getOid(), null, result).asObjectable();
            } catch (ObjectNotFoundException|SchemaException e) {
                throw new SystemException(e);
            }
            a.setTarget(object);
        }
        return object;
    }

    public void resolveRolesAndOrgUnits(PrismObject<UserType> user, OperationResult result) {
        for (AssignmentType assignmentType : user.asObjectable().getAssignment()) {
            if (assignmentType.getTargetRef() != null && assignmentType.getTarget() == null) {
                QName type = assignmentType.getTargetRef().getType();
                if (RoleType.COMPLEX_TYPE.equals(type) || OrgType.COMPLEX_TYPE.equals(type)) {
                    String oid = assignmentType.getTargetRef().getOid();
                    try {
                        PrismObject<ObjectType> o = repositoryService.getObject(ObjectType.class, oid, null, result);
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
    //endregion

}
