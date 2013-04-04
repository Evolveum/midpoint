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

package com.evolveum.midpoint.wf.processes.addrole;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.api.RepositoryService;
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
import com.evolveum.midpoint.wf.processors.primary.PrimaryApprovalProcessWrapper;
import com.evolveum.midpoint.wf.processors.primary.StartProcessInstructionForPrimaryStage;
import com.evolveum.midpoint.wf.processes.general.Decision;
import com.evolveum.midpoint.wf.processes.general.ApprovalRequest;
import com.evolveum.midpoint.wf.processes.general.ProcessVariableNames;
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
 * Process wrapper that manages role addition approval. It starts one process instance for each role
 * that has to be approved.
 *
 * In the past, we used to start one process instance for ALL roles to be approved. It made BPMN
 * approval process slightly more complex, while allowed to keep information about approval process
 * centralized from the user point of view (available via "single click"). If necessary, we can return
 * to this behavior.
 *
 * Alternatively, it is possible to start one process instance for a set of roles that share the
 * same approval mechanism. However, it is questionable what "the same approval mechanism" means,
 * for example, if there are expressions used to select an approver.
 *
 * @author mederly
 */
@Component
public class AddRoleAssignmentWrapper implements PrimaryApprovalProcessWrapper {

    private static final Trace LOGGER = TraceManager.getTrace(AddRoleAssignmentWrapper.class);

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private WfTaskUtil wfTaskUtil;

    @Override
    public List<StartProcessInstructionForPrimaryStage> prepareProcessesToStart(ModelContext<?,?> modelContext, ObjectDelta<Objectable> change, Task task, OperationResult result) {

        List<ApprovalRequest<AssignmentType>> approvalRequestList = getAssignmentToApproveList(change, result);
        if (approvalRequestList == null) {
            return null;
        }

        return prepareStartProcessInstructions(modelContext, task, result, approvalRequestList);
    }

    private List<ApprovalRequest<AssignmentType>> getAssignmentToApproveList(ObjectDelta<Objectable> change, OperationResult result) {

        List<ApprovalRequest<AssignmentType>> approvalRequestList = new ArrayList<ApprovalRequest<AssignmentType>>();

        /*
         * We either add a user; then the list of roles to be added is given by the assignment property,
         * or we modify a user; then the list of roles is given by the assignment property modification.
         */

        if (change.getChangeType() == ChangeType.ADD) {

            PrismObject<?> prismToAdd = change.getObjectToAdd();
            UserType user = (UserType) prismToAdd.asObjectable();

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Role-related assignments in user add delta (" + user.getAssignment().size() + "): ");
            }

            Iterator<AssignmentType> assignmentTypeIterator = user.getAssignment().iterator();
            while (assignmentTypeIterator.hasNext()) {
                AssignmentType a = assignmentTypeIterator.next();
                ObjectReferenceType ort = a.getTargetRef();
                if (ort != null && RoleType.COMPLEX_TYPE.equals(ort.getType())) {
                    RoleType role = resolveRoleRef(a, result);
                    boolean approvalRequired = shouldRoleBeApproved(role);
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace(" - role: " + role + " (approval required = " + approvalRequired + ")");
                    }
                    if (approvalRequired) {
                        AssignmentType aCopy = a.clone();
                        aCopy.setTarget(role);
                        approvalRequestList.add(createApprovalRequest(aCopy, role));
                        assignmentTypeIterator.remove();
                    }
                }
            }

        } else if (change.getChangeType() == ChangeType.MODIFY) {

            Iterator<? extends ItemDelta> deltaIterator = change.getModifications().iterator();

            while (deltaIterator.hasNext()) {
                ItemDelta delta = deltaIterator.next();
                if (UserType.F_ASSIGNMENT.equals(delta.getName()) && delta.getValuesToAdd() != null && !delta.getValuesToAdd().isEmpty()) {          // todo: what if assignments are modified?

                    Iterator valueIterator = delta.getValuesToAdd().iterator();
                    while (valueIterator.hasNext()) {
                        Object o = valueIterator.next();
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("Assignment to add = " + ((PrismContainerValue) o).dump());
                        }
                        PrismContainerValue<AssignmentType> at = (PrismContainerValue<AssignmentType>) o;
                        ObjectReferenceType ort = at.getValue().getTargetRef();
                        if (ort != null && RoleType.COMPLEX_TYPE.equals(ort.getType())) {
                            RoleType role = resolveRoleRef(at.getValue(), result);
                            boolean approvalRequired = shouldRoleBeApproved(role);
                            if (LOGGER.isTraceEnabled()) {
                                LOGGER.trace(" - role: " + role + " (approval required = " + approvalRequired + ")");
                            }
                            if (approvalRequired) {
                                AssignmentType aCopy = at.asContainerable().clone();
                                aCopy.setTarget(role);
                                approvalRequestList.add(createApprovalRequest(aCopy, role));
                                valueIterator.remove();
                            }
                        }
                    }
                    if (delta.getValuesToAdd().isEmpty()) {         // empty set of values to add is an illegal state
                        delta.resetValuesToAdd();
                        if (delta.getValuesToReplace() == null && delta.getValuesToDelete() == null) {
                            deltaIterator.remove();
                        }
                    }
                }
            }
        } else {
            return null;
        }
        return approvalRequestList;
    }

    private ApprovalRequest<AssignmentType> createApprovalRequest(AssignmentType a, RoleType role) {
        return new ApprovalRequest(a, role.getApprovalSchema(), role.getApproverRef(), role.getApproverExpression(), role.getAutomaticallyApproved());
    }


    // approvalRequestList should contain de-referenced roles
    private List<StartProcessInstructionForPrimaryStage> prepareStartProcessInstructions(ModelContext<?, ?> modelContext, Task task, OperationResult result, List<ApprovalRequest<AssignmentType>> approvalRequestList) {
        List<StartProcessInstructionForPrimaryStage> instructions = new ArrayList<StartProcessInstructionForPrimaryStage>();

        ModelElementContext<UserType> fc = (ModelElementContext<UserType>) modelContext.getFocusContext();
        UserType newUser = fc.getObjectNew() != null ? fc.getObjectNew().asObjectable() : null;

        Validate.notNull(newUser);
        Validate.notNull(newUser.getName());
        String userName = newUser.getName().getOrig();

        for (ApprovalRequest<AssignmentType> approvalRequest : approvalRequestList) {

            StartProcessInstructionForPrimaryStage instruction = new StartProcessInstructionForPrimaryStage();
            instruction.setProcessName(ADD_ROLE_PROCESS);
            instruction.setSimple(true);

            AssignmentType assignmentType = approvalRequest.getItemToApprove();
            RoleType roleType = (RoleType) assignmentType.getTarget();

            Validate.notNull(roleType);
            Validate.notNull(roleType.getName());
            String roleName = roleType.getName().getOrig();

            instruction.setTaskName(new PolyStringType("Workflow for approving adding " + roleName + " to " + userName));
            instruction.addProcessVariable(WfConstants.VARIABLE_PROCESS_NAME, "Adding " + roleName + " to " + userName);
            instruction.addProcessVariable(WfConstants.VARIABLE_START_TIME, new Date());

            instruction.addProcessVariable(ProcessVariableNames.APPROVAL_REQUEST, approvalRequest);
            instruction.addProcessVariable(ProcessVariableNames.APPROVAL_TASK_NAME, "Approve adding " + roleName + " to " + userName);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("assignment to approve = " + approvalRequest);
            }

            String objectOid = null;
            if (fc.getObjectNew() != null && fc.getObjectNew().getOid() != null) {
                objectOid = fc.getObjectNew().getOid();
            } else if (fc.getObjectOld() != null && fc.getObjectOld().getOid() != null) {
                objectOid = fc.getObjectOld().getOid();
            }

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
//            LOGGER.info("+++ requester = " + task.getOwner().dump());
//            LOGGER.info("+++ object old = " + fc.getObjectOld().dump());

//            if (fc.getObjectOld() != null) {
//                resolveRolesAndOrgUnits(fc.getObjectOld(), result);
//            }
//
//            if (fc.getObjectNew() != null) {
//                resolveRolesAndOrgUnits(fc.getObjectNew(), result);
//            }

            if (requester != null) {
                resolveRolesAndOrgUnits(requester, result);
            }

            instruction.addProcessVariable(WfConstants.VARIABLE_MIDPOINT_OBJECT_OID, objectOid);
            instruction.addProcessVariable(WfConstants.VARIABLE_MIDPOINT_OBJECT_BEFORE, fc.getObjectOld());
            instruction.addProcessVariable(WfConstants.VARIABLE_MIDPOINT_OBJECT_AFTER, fc.getObjectNew());
            //spi.addProcessVariable(WfConstants.VARIABLE_MIDPOINT_DELTA, change);
            instruction.addProcessVariable(WfConstants.VARIABLE_MIDPOINT_REQUESTER, requester);
            instruction.addProcessVariable(WfConstants.VARIABLE_MIDPOINT_REQUESTER_OID, task.getOwner().getOid());
            instruction.addProcessVariable(WfConstants.VARIABLE_UTIL, new ActivitiUtil());
            instruction.addProcessVariable(WfConstants.VARIABLE_MIDPOINT_ADDITIONAL_DATA, roleType);
            instruction.addProcessVariable(WfConstants.VARIABLE_MIDPOINT_PROCESS_WRAPPER, this.getClass().getName());

            instruction.setWrapper(this);

            instruction.setExecuteImmediately(ModelExecuteOptions.isExecuteImmediatelyAfterApproval(((LensContext) modelContext).getOptions()));
            instruction.setNoProcess(false);

            ObjectDelta<Objectable> delta = assignmentToDelta(modelContext, approvalRequest, objectOid);
            instruction.setDelta(delta);

            instructions.add(instruction);
        }
        return instructions;
    }

    private ObjectDelta<Objectable> assignmentToDelta(ModelContext<?, ?> modelContext, ApprovalRequest<AssignmentType> approvalRequest, String objectOid) {
        PrismObject<UserType> user = (PrismObject<UserType>) modelContext.getFocusContext().getObjectNew();
        PrismContainerDefinition<AssignmentType> prismContainerDefinition = user.getDefinition().findContainerDefinition(UserType.F_ASSIGNMENT);

        ItemDelta<PrismContainerValue<AssignmentType>> addRoleDelta = new ContainerDelta<AssignmentType>(new ItemPath(), UserType.F_ASSIGNMENT, prismContainerDefinition);
        addRoleDelta.addValueToAdd(approvalRequest.getItemToApprove().asPrismContainerValue().clone());

        // casting to ObjectDelta is an ugly hack, but...
        return (ObjectDelta) ObjectDelta.createModifyDelta(objectOid != null ? objectOid : "?", addRoleDelta, UserType.class, ((LensContext) modelContext).getPrismContext());
    }


    public static final String ADD_ROLE_PROCESS = "ItemApproval";
    public static final String APPROVER_MAIL_ADDRESS = "approverMailAddress";

    //private static final QName ADDITIONAL_INFO = new QName(SchemaConstants.NS_C, "AdditionalInfo");       // todo: change namespace

    private boolean shouldRoleBeApproved(RoleType role) {
        return !role.getApproverRef().isEmpty() || !role.getApproverExpression().isEmpty() || role.getApprovalSchema() != null;
    }

//    private String formatAsAssignmentList(List<ApprovalRequest> assignmentsToApprove) {
//        StringBuffer sb = new StringBuffer();
//        if (assignmentsToApprove.size() > 1) {
//            sb.append("roles ");
//        } else if (assignmentsToApprove.size() == 1) {
//            sb.append("role ");
//        } else {
//            throw new IllegalStateException("No roles to approve.");
//        }
//        boolean first = true;
//        for (ApprovalRequest at : assignmentsToApprove) {
//            if (first) {
//                first = false;
//            } else {
//                sb.append(", ");
//            }
//            sb.append(formatAssignmentBrief(at.getAssignment()));
//        }
//        return sb.toString();
//    }

    private RoleType resolveRoleRef(AssignmentType a, OperationResult result) {
        RoleType role = (RoleType) a.getTarget();
        if (role == null) {
            try {
                role = repositoryService.getObject(RoleType.class, a.getTargetRef().getOid(), result).asObjectable();
            } catch (ObjectNotFoundException e) {
                throw new SystemException(e);
            } catch (SchemaException e) {
                throw new SystemException(e);
            }
            a.setTarget(role);
        }
        return role;
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


//
//        AssignmentsApprovals assignmentsApprovals = (AssignmentsApprovals) event.getVariables().get(ASSIGNMENTS_APPROVALS);
//        if (assignmentsApprovals == null) {
//            throw new IllegalStateException("AssignmentsApprovals is null");           // todo
//        }
//
//        if (context.getState() != ModelState.PRIMARY) {
//            throw new IllegalStateException("Model state is not PRIMARY (it is " + context.getState() + "); task = " + task);
//        }
//
//        List<AssignmentType> assignmentsApproved = new ArrayList<AssignmentType>();
//        List<AssignmentType> assignmentsDisapproved = new ArrayList<AssignmentType>();
//        for (AssignmentsApprovals.AssignmentApproval assignmentApproval : assignmentsApprovals.getAssignmentsApproval()) {
//            if (LOGGER.isDebugEnabled()) {
//                LOGGER.debug("Workflow result: approved=" + assignmentApproval.isApproved() + " for assignment=" + assignmentApproval.getAssignmentType());
//            }
//            if (assignmentApproval.isApproved()) {
//                assignmentsApproved.add(assignmentApproval.getAssignmentType());
//            } else {
//                assignmentsDisapproved.add(assignmentApproval.getAssignmentType());
//            }
//        }
//
//        ObjectDelta<Objectable> change = context.getFocusContext().getPrimaryDelta();
//        if (change.getChangeType() == ChangeType.ADD) {
//
//            PrismObject<?> prismToAdd = change.getObjectToAdd();
//            boolean isUser = prismToAdd.getCompileTimeClass().isAssignableFrom(UserType.class);
//
//            if (!isUser) {
//                throw new IllegalStateException("Object to be added is not User; task = " + task);
//            }
//
//            UserType user = (UserType) prismToAdd.asObjectable();
//            if (LOGGER.isTraceEnabled()) {
//                LOGGER.trace("Assignments (" + user.getAssignment().size() + "): ");
//            }
//            for (AssignmentType a : new ArrayList<AssignmentType>(user.getAssignment())) {      // copy, because we want to modify the list
//                ObjectReferenceType ort = a.getTargetRef();
//                if (RoleType.COMPLEX_TYPE.equals(ort.getType())) {
//                    if (LOGGER.isTraceEnabled()) {
//                        LOGGER.trace(" - assignment: " + a);
//                    }
//                    if (assignmentsApproved.contains(a)) {
//                        if (LOGGER.isTraceEnabled()) {
//                            LOGGER.trace(" --- approved");
//                        }
//                    } else {
//                        if (LOGGER.isTraceEnabled()) {
//                            LOGGER.trace(" --- rejected; will be removed from the list");
//                        }
//                        user.getAssignment().remove(a);
//                    }
//                }
//            }
//        } else if (change.getChangeType() == ChangeType.MODIFY) {
//
//            boolean isUser = change.getObjectTypeClass().isAssignableFrom(UserType.class);
//
//            if (!isUser) {
//                throw new IllegalStateException("Object to be added is not User; task = " + task);
//            }
//
//            for (ItemDelta delta : change.getModifications()) {
//                if (UserType.F_ASSIGNMENT.equals(delta.getName())) {
//                    for (Object o : new ArrayList<Object>(delta.getValuesToAdd())) {
//                        if (LOGGER.isTraceEnabled()) {
//                            LOGGER.trace("Value to add = " + o);
//                        }
//                        PrismContainerValue<AssignmentType> at = (PrismContainerValue<AssignmentType>) o;
//                        ObjectReferenceType ort = at.getValue().getTargetRef();
//                        if (RoleType.COMPLEX_TYPE.equals(ort.getType())) {
//                            if (LOGGER.isTraceEnabled()) {
//                                LOGGER.trace(" - assignment: " + at);
//                            }
//                            if (assignmentsApproved.contains(at.asContainerable())) {
//                                if (LOGGER.isTraceEnabled()) {
//                                    LOGGER.trace(" --- approved");
//                                }
//                            } else {
//                                if (LOGGER.isTraceEnabled()) {
//                                    LOGGER.trace(" --- rejected; will be removed from the list");
//                                }
//                                delta.getValuesToAdd().remove(o);
//                            }
//                        }
//                    }
//                }
//            }
//        } else {
//            throw new IllegalStateException("Operation that has to be continued is neither ADD nor MODIFY; task = " + task);
//        }
//
//    }

    private void addRolesRequested(StringBuffer sb, Map<String, Object> vars) {
        sb.append("The following assignments were requested to be added:\n");
        List<ApprovalRequest<AssignmentType>> approvalRequestList = null; //(List<ApprovalRequest>) vars.get(ASSIGNMENTS_TO_APPROVE);
        if (approvalRequestList == null) {
            sb.append("(the list cannot be obtained)\n\n");
        } else {
            int counter = 1;
            for (ApprovalRequest<AssignmentType> ata : approvalRequestList) {
                sb.append(" ");
                sb.append(counter++);
                sb.append(". ");
                sb.append(formatAssignmentFull(ata.getItemToApprove(), true));
                sb.append("\n");
            }
            sb.append("\n");
        }
    }

    private String formatAssignmentFull(AssignmentType assignment, boolean showOid) {
        StringBuilder sb = new StringBuilder();
        if (assignment.getTarget() != null) {
            sb.append(assignment.getTarget().getName());
        }
        if (showOid || assignment.getTarget() == null) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append("(");
            sb.append(assignment.getTargetRef().getOid());
            sb.append(")");
        }
        if (assignment.getActivation() != null) {
            if (assignment.getActivation().getValidFrom() != null) {
                sb.append(", from ");
                sb.append(formatTime(assignment.getActivation().getValidFrom()));
            }
            if (assignment.getActivation().getValidTo() != null) {
                sb.append(", to ");
                sb.append(formatTime(assignment.getActivation().getValidTo()));
            }
        }
        if (StringUtils.isNotBlank(assignment.getDescription())) {
            sb.append(" (");
            sb.append(assignment.getDescription());
            sb.append(")");
        }
        return sb.toString();
    }

    private String formatAssignmentBrief(AssignmentType assignment) {
        StringBuilder sb = new StringBuilder();
        if (assignment.getTarget() != null) {
            sb.append(assignment.getTarget().getName());
        } else {
            sb.append(assignment.getTargetRef().getOid());
        }
        if (assignment.getActivation() != null && (assignment.getActivation().getValidFrom() != null || assignment.getActivation().getValidTo() != null)) {
            sb.append(" ");
            sb.append("(");
            sb.append(formatTimeIntervalBrief(assignment));
            sb.append(")");
        }
        return sb.toString();
    }

    private void addDecisionsDone(StringBuffer sb, Map<String, Object> vars, boolean detailed) {
        sb.append("Individual decisions done: ");
        List<Decision<AssignmentType>> allDecisions = (List<Decision<AssignmentType>>) vars.get(ProcessVariableNames.ALL_DECISIONS);
        if (allDecisions == null) {
            sb.append("(error - list of decisions is null)\n");        // todo
        } else if (allDecisions.isEmpty()) {
            sb.append("none\n");
        } else {
            if (detailed) {
                sb.append("\n");
                for (Decision<AssignmentType> decision : allDecisions) {
                    sb.append(" - " + (decision.isApproved() ? "APPROVED" : "NOT APPROVED") + " role " +
                            formatAssignmentFull(decision.getApprovalRequest().getItemToApprove(), false) + " by " +
                            decision.getUser() + " on " + decision.getDate() + ", with comment: " + decision.getComment() + "\n");
                }
            } else {
                boolean first = true;
                for (Decision<AssignmentType> decision : allDecisions) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append("; ");
                    }
                    sb.append("role " + formatAssignmentBrief(decision.getApprovalRequest().getItemToApprove()) + " " +
                            (decision.isApproved() ? "approved" : "NOT approved") +
                            " by " + decision.getUser() +
                            (!StringUtils.isEmpty(decision.getComment()) ? ", commenting: " + decision.getComment() : ""));
                }
            }
        }
        sb.append("\n");
    }

    private void addAssignmentsApprovals(StringBuffer sb, Map<String, Object> vars) {

//        AssignmentsApprovals assignmentsApprovals = null; //(AssignmentsApprovals) vars.get(ASSIGNMENTS_APPROVALS);
//        if (LOGGER.isTraceEnabled()) {
//            LOGGER.trace("assignmentsApprovals = " + assignmentsApprovals);
//        }
//        if (assignmentsApprovals == null) {
//            sb.append("(no information on definitive assignments approvals/rejections)\n");
//            return;
//        }
//
//        List<AssignmentsApprovals.AssignmentApproval> approved = new ArrayList<AssignmentsApprovals.AssignmentApproval>();
//        List<AssignmentsApprovals.AssignmentApproval> rejected = new ArrayList<AssignmentsApprovals.AssignmentApproval>();
//
//        for (AssignmentsApprovals.AssignmentApproval assignmentApproval : assignmentsApprovals.getAssignmentsApproval()) {
//            (assignmentApproval.isApproved() ? approved : rejected).add(assignmentApproval);
//        }
//
//        sb.append("Assignments definitively approved: ");
//        listAssignmentsApprovals(sb, approved);
//        sb.append("\n");
//        sb.append("Assignments definitively rejected: ");
//        listAssignmentsApprovals(sb, rejected);
//        sb.append("\n");
    }

//    private void listAssignmentsApprovals(StringBuffer sb, List<AssignmentsApprovals.AssignmentApproval> assignmentApprovals) {
//        if (assignmentApprovals.isEmpty()) {
//            sb.append("none\n");
//        } else {
//            sb.append("\n");
//            for (AssignmentsApprovals.AssignmentApproval approval : assignmentApprovals) {
//                sb.append(" - role " + formatAssignmentFull(approval.getAssignmentType(), false) + "\n");
//            }
//        }
//    }

    @Override
    public String getProcessSpecificDetailsForTask(String instanceId, Map<String, Object> vars) {
        StringBuffer sb = new StringBuffer();
        addDecisionsDone(sb, vars, false);
        return sb.toString();
    }

    // todo this is brutal hack
    @Override
    public String getProcessSpecificDetails(HistoricProcessInstance instance, Map<String, Object> vars) {
        StringBuffer sb = new StringBuffer();
        addRolesRequested(sb, vars);
        addDecisionsDone(sb, vars, true);
        sb.append("----------------------------------------------\n\n");
        addAssignmentsApprovals(sb, vars);
        if (instance.getDeleteReason() != null) {
            sb.append("Reason for process deletion: ");
            sb.append(instance.getDeleteReason());
            sb.append("\n");
        }
        return sb.toString();
    }

    // todo this is brutal hack
    @Override
    public String getProcessSpecificDetails(ProcessInstance instance, Map<String, Object> vars, List<org.activiti.engine.task.Task> tasks) {
        StringBuffer sb = new StringBuffer();
        addRolesRequested(sb, vars);
        addDecisionsDone(sb, vars, true);
        sb.append("----------------------------------------------\n\n");
        addAssignmentsApprovals(sb, vars);
        return sb.toString();
    }

    private void resolveRolesAndOrgUnits(PrismObject<UserType> user, OperationResult result) {
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

    public static String formatTimeIntervalBrief(AssignmentType assignment) {
        StringBuilder sb = new StringBuilder();
        if (assignment != null && assignment.getActivation() != null &&
                (assignment.getActivation().getValidFrom() != null || assignment.getActivation().getValidTo() != null)) {
            if (assignment.getActivation().getValidFrom() != null && assignment.getActivation().getValidTo() != null) {
                sb.append(formatTime(assignment.getActivation().getValidFrom()));
                sb.append("-");
                sb.append(formatTime(assignment.getActivation().getValidTo()));
            } else if (assignment.getActivation().getValidFrom() != null) {
                sb.append("from ");
                sb.append(formatTime(assignment.getActivation().getValidFrom()));
            } else {
                sb.append("to ");
                sb.append(formatTime(assignment.getActivation().getValidTo()));
            }
        }
        return sb.toString();
    }

    private static String formatTime(XMLGregorianCalendar time) {
        DateFormat formatter = DateFormat.getDateInstance();
        return formatter.format(time.toGregorianCalendar().getTime());
    }




}
