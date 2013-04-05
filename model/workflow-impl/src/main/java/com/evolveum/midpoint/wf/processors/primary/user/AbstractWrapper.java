package com.evolveum.midpoint.wf.processors.primary.user;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.WfConstants;
import com.evolveum.midpoint.wf.WfTaskUtil;
import com.evolveum.midpoint.wf.activiti.ActivitiUtil;
import com.evolveum.midpoint.wf.messages.ProcessEvent;
import com.evolveum.midpoint.wf.processors.primary.PrimaryApprovalProcessWrapper;
import com.evolveum.midpoint.wf.processors.primary.StartProcessInstructionForPrimaryStage;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.runtime.ProcessInstance;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Pavol
 */
public abstract class AbstractWrapper implements PrimaryApprovalProcessWrapper {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractWrapper.class);

    public static final String GENERAL_APPROVAL_PROCESS = "ItemApproval";

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    WfTaskUtil wfTaskUtil;

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
        instruction.addProcessVariable(WfConstants.VARIABLE_MIDPOINT_OBJECT_OID, objectOid);
        instruction.addProcessVariable(WfConstants.VARIABLE_MIDPOINT_OBJECT_BEFORE, fc.getObjectOld());
        instruction.addProcessVariable(WfConstants.VARIABLE_MIDPOINT_OBJECT_AFTER, fc.getObjectNew());
        //spi.addProcessVariable(WfConstants.VARIABLE_MIDPOINT_DELTA, change);
        instruction.addProcessVariable(WfConstants.VARIABLE_MIDPOINT_REQUESTER, requester);
        instruction.addProcessVariable(WfConstants.VARIABLE_MIDPOINT_REQUESTER_OID, task.getOwner().getOid());
        instruction.addProcessVariable(WfConstants.VARIABLE_UTIL, new ActivitiUtil());
        instruction.addProcessVariable(WfConstants.VARIABLE_MIDPOINT_PROCESS_WRAPPER, this.getClass().getName());
        instruction.setWrapper(this);
        instruction.setNoProcess(false);
        instruction.addProcessVariable(WfConstants.VARIABLE_START_TIME, new Date());
    }

    /*
 * In this case, mapping deltaIn -> deltaOut is extremely simple.
 * DeltaIn contains a delta that has to be approved. Workflow answers simply yes/no.
 * Therefore, we either copy DeltaIn to DeltaOut, or generate an empty list of modifications.
 */
    @Override
    public List<ObjectDelta<Objectable>> prepareDeltaOut(ProcessEvent event, Task task, OperationResult result) throws SchemaException {
        List<ObjectDelta<Objectable>> deltaIn = wfTaskUtil.retrieveDeltasToProcess(task);
        if (event.getAnswer() == Boolean.TRUE) {
            return new ArrayList<ObjectDelta<Objectable>>(deltaIn);
        } else if (event.getAnswer() == Boolean.FALSE) {
            return new ArrayList<ObjectDelta<Objectable>>();
        } else {
            throw new IllegalStateException("No wfAnswer variable in process event " + event);      // todo more meaningful message
        }
    }

    @Override
    public String getProcessSpecificDetailsForTask(String instanceId, Map<String, Object> vars) {
        return "not implemented yet";
    }

    @Override
    public String getProcessSpecificDetails(HistoricProcessInstance instance, Map<String, Object> vars) {
        return "not implemented yet";
    }

    @Override
    public String getProcessSpecificDetails(ProcessInstance instance, Map<String, Object> vars, List<org.activiti.engine.task.Task> tasks) {
        return "not implemented yet";
    }


}
