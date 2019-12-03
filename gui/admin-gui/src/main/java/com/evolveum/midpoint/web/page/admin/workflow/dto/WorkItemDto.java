/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.workflow.dto;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.visualizer.Scene;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.ObjectTreeDeltas;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CaseTypeUtil;
import com.evolveum.midpoint.schema.util.CaseWorkItemUtil;
import com.evolveum.midpoint.schema.util.ApprovalContextUtil;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.component.prism.show.SceneDto;
import com.evolveum.midpoint.web.component.prism.show.SceneUtil;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskChangesDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author lazyman
 * @author mederly
 */
public class WorkItemDto extends Selectable {

    public static final String F_WORK_ITEM = "workItem";
    public static final String F_NAME = "name";
    public static final String F_CREATED_FORMATTED = "createdFormatted";
    public static final String F_CREATED_FORMATTED_FULL = "createdFormattedFull";
    public static final String F_DEADLINE_FORMATTED = "deadlineFormatted";
    public static final String F_DEADLINE_FORMATTED_FULL = "deadlineFormattedFull";
    public static final String F_STARTED_FORMATTED_FULL = "startedFormattedFull";
    public static final String F_ASSIGNEE_OR_CANDIDATES = "assigneeOrCandidates";
    public static final String F_ORIGINAL_ASSIGNEE = "originalAssignee";
    public static final String F_ORIGINAL_ASSIGNEE_FULL = "originalAssigneeFull";
    public static final String F_CURRENT_ASSIGNEES = "currentAssignees";
    public static final String F_CURRENT_ASSIGNEES_FULL = "currentAssigneesFull";
    public static final String F_CANDIDATES = "candidates";
    public static final String F_STAGE_INFO = "stageInfo";
    public static final String F_ESCALATION_LEVEL_INFO = "escalationLevelInfo";
    public static final String F_ESCALATION_LEVEL_NUMBER = "escalationLevelNumber";
    public static final String F_ADDITIONAL_INFORMATION = "additionalInformation";
    public static final String F_TRIGGERS = "triggers";

    public static final String F_OTHER_WORK_ITEMS = "otherWorkItems";
    public static final String F_RELATED_WORKFLOW_REQUESTS = "relatedWorkflowRequests";

    public static final String F_OBJECT_NAME = "objectName";
    public static final String F_TARGET_NAME = "targetName";

    public static final String F_REQUESTER_NAME = "requesterName";
    public static final String F_REQUESTER_FULL_NAME = "requesterFullName";
    public static final String F_APPROVER_COMMENT = "approverComment";

    public static final String F_WORKFLOW_CONTEXT = "workflowContext";          // use with care
    @Deprecated public static final String F_DELTAS = "deltas";
    public static final String F_PROCESS_INSTANCE_ID = "processInstanceId";
    public static final String F_CHANGES = "changes";

    public static final String F_REQUESTER_COMMENT = "requesterComment";
    public static final String F_TASK_OID = "taskOid";
    public static final String F_IN_STAGE_BEFORE_LAST_ONE = "isInStageBeforeLastOne";

    // workItem may or may not contain resolved taskRef;
    // and this task may or may not contain filled-in workflowContext -> and then requesterRef object
    //
    // Depending on expected use (work item list vs. work item details)

    private final CaseWorkItemType workItem;
    private CaseType aCase;
    private List<TaskType> relatedTasks;
    @Deprecated private SceneDto deltas;
    private TaskChangesDto changes;
    private String approverComment;
    private List<EvaluatedTriggerGroupDto> triggers;            // initialized on demand

    private PageBase pageBase;
    private ObjectType focus;

    public WorkItemDto(CaseWorkItemType workItem, PageBase pageBase) {
        this(workItem, null, null, pageBase);
    }

    public WorkItemDto(CaseWorkItemType workItem, CaseType aCase, List<TaskType> relatedTasks, PageBase pageBase) {
        this.workItem = workItem;
        this.aCase = aCase;
        this.relatedTasks = relatedTasks;
        this.pageBase = pageBase;
    }

    public void prepareDeltaVisualization(String sceneNameKey, PrismContext prismContext,
            ModelInteractionService modelInteractionService, Task opTask, OperationResult result) throws SchemaException, ExpressionEvaluationException {
        CaseType aCase = getCase();
        if (aCase == null || aCase.getApprovalContext() == null) {
            return;
        }
        ObjectReferenceType objectRef = aCase.getObjectRef();
        ApprovalContextType actx = aCase.getApprovalContext();
        Scene deltasScene = SceneUtil.visualizeObjectTreeDeltas(actx.getDeltasToApprove(), sceneNameKey, prismContext, modelInteractionService,
                objectRef, opTask, result);
        deltas = new SceneDto(deltasScene);

        ObjectTreeDeltas deltas = ObjectTreeDeltas.fromObjectTreeDeltasType(actx.getDeltasToApprove(), prismContext);
        changes = TaskDto.createChangesToBeApproved(deltas, modelInteractionService,
                prismContext, objectRef, opTask, result);
    }

    @Nullable
    public CaseType getCase() {
        if (aCase == null) {
            aCase = CaseWorkItemUtil.getCase(workItem);
        }
        return aCase;
    }

    public WorkItemId getWorkItemId() {
        return WorkItemId.of(workItem);
    }

    public String getName() {
        return PolyString.getOrig(workItem.getName());  // todo MID-5916
    }

    public String getCreatedFormatted() {
        return WebComponentUtil.getShortDateTimeFormattedValue(workItem.getCreateTimestamp(), pageBase);
    }

    public String getDeadlineFormatted() {
        return WebComponentUtil.getShortDateTimeFormattedValue(workItem.getDeadline(), pageBase);
    }

    public String getCreatedFormattedFull() {
        return WebComponentUtil.getLongDateTimeFormattedValue(workItem.getCreateTimestamp(), pageBase);
    }

    public String getDeadlineFormattedFull() {
        return WebComponentUtil.getLongDateTimeFormattedValue(workItem.getDeadline(), pageBase);
    }

    public Date getCreatedDate() {
        return XmlTypeConverter.toDate(workItem.getCreateTimestamp());
    }

    public Date getDeadlineDate() {
        return XmlTypeConverter.toDate(workItem.getDeadline());
    }

    public Date getStartedDate() {
        return XmlTypeConverter.toDate(CaseTypeUtil.getStartTimestamp(getCase()));
    }

    public String getStartedFormattedFull() {
        return WebComponentUtil.getLongDateTimeFormattedValue(getStartedDate(), pageBase);
    }

    // TODO
    public String getAssigneeOrCandidates() {
        String assignee = getCurrentAssignees();
        if (assignee != null) {
            return assignee;
        } else {
            return getCandidates();
        }
    }

    public String getOriginalAssignee() {
        return WebComponentUtil.getReferencedObjectNames(Collections.singletonList(workItem.getOriginalAssigneeRef()), false);
    }

    public String getOriginalAssigneeFull() {
        return WebComponentUtil.getReferencedObjectDisplayNamesAndNames(Collections.singletonList(workItem.getOriginalAssigneeRef()), false);
    }

    public String getCurrentAssignees() {
        return WebComponentUtil.getReferencedObjectNames(workItem.getAssigneeRef(), false);
    }

    public String getCurrentAssigneesFull() {
        return WebComponentUtil.getReferencedObjectDisplayNamesAndNames(workItem.getAssigneeRef(), false);
    }

    public String getCandidates() {
        return WebComponentUtil.getReferencedObjectNames(workItem.getCandidateRef(), true);
    }

    public String getObjectName() {
        return WebComponentUtil.getName(ApprovalContextUtil.getObjectRef(workItem));
    }

    public ObjectReferenceType getObjectRef() {
        return ApprovalContextUtil.getObjectRef(workItem);
    }

    public ObjectReferenceType getTargetRef() {
        return ApprovalContextUtil.getTargetRef(workItem);
    }

    public String getTargetName() {
        return WebComponentUtil.getName(ApprovalContextUtil.getTargetRef(workItem));
    }

    public ApprovalContextType getApprovalContext() {
        CaseType aCase = getCase();
        return aCase != null ? aCase.getApprovalContext() : null;
    }

    public String getRequesterName() {
        CaseType aCase = getCase();
        return aCase != null ? WebComponentUtil.getName(aCase.getRequestorRef()) : null;
    }

    public String getRequesterFullName() {
        UserType requester = getRequester();
        return requester != null ? PolyString.getOrig(requester.getFullName()) : null;
    }

    public UserType getRequester() {
        CaseType aCase = getCase();
        if (aCase == null) {
            return null;
        }
        return WebComponentUtil.getObjectFromReference(aCase.getRequestorRef(), UserType.class);
    }

    public String getApproverComment() {
        return approverComment;
    }

    public void setApproverComment(String approverComment) {
        this.approverComment = approverComment;
    }

    public CaseWorkItemType getWorkItem() {
        return workItem;
    }

    @Deprecated
    public SceneDto getDeltas() {
        return deltas;
    }

    public QName getTargetType() {
        ObjectReferenceType targetRef = ApprovalContextUtil.getTargetRef(workItem);
        return targetRef != null ? targetRef.getType() : null;
    }

    public QName getObjectType() {
        ObjectReferenceType objectRef = ApprovalContextUtil.getObjectRef(workItem);
        return objectRef != null ? objectRef.getType() : null;
    }

    // all except the current one
    public List<WorkItemDto> getOtherWorkItems() {
        final List<WorkItemDto> rv = new ArrayList<>();
        final CaseType aCase = getCase();
        if (aCase == null || aCase.getApprovalContext() == null) {
            return rv;
        }
        for (CaseWorkItemType workItemType : aCase.getWorkItem()) {
            // todo
//            if (workItemType.getExternalId() == null || workItemType.getExternalId().equals(getWorkItemId())) {
//                continue;
//            }
            rv.add(new WorkItemDto(workItemType, pageBase));
        }
        return rv;
    }

    // all excluding the current task
    public List<ProcessInstanceDto> getRelatedWorkflowRequests() {
        final List<ProcessInstanceDto> rv = new ArrayList<>();
        if (relatedTasks == null) {
            return rv;
        }
        for (TaskType task : relatedTasks) {
            // todo
//            if (task.getApprovalContext() == null || task.getApprovalContext().getCaseOid() == null) {
//                continue;
//            }
//            if (StringUtils.equals(getProcessInstanceId(), task.getApprovalContext().getCaseOid())) {
//                continue;
//            }
            rv.add(new ProcessInstanceDto(aCase, WebComponentUtil.getShortDateTimeFormat(pageBase)));
        }
        return rv;
    }

    public String getProcessInstanceId() {
        return null; // TODO
//        final TaskType task = getTaskType();
//        return task != null && task.getApprovalContext() != null ? task.getApprovalContext().getCaseOid() : null;
    }

    public String getCaseOid() {
        final CaseType aCase = getCase();
        return aCase != null ? aCase.getOid() : null;
    }

    public boolean isInStageBeforeLastOne() {
        return ApprovalContextUtil.isInStageBeforeLastOne(getCase());
    }

    // TODO deduplicate
    public boolean hasHistory() {
        List<DecisionDto> rv = new ArrayList<>();
        ApprovalContextType approvalContext = getApprovalContext();
        if (approvalContext == null) {
            return false;
        }
        // TODO
//        if (!wfContextType.getEvent().isEmpty()) {
//            wfContextType.getEvent().forEach(e -> addIgnoreNull(rv, DecisionDto.create(e, null)));
//        } else {
//            ItemApprovalProcessStateType instanceState = WfContextUtil.getItemApprovalProcessInfo(wfContextType);
//            if (instanceState != null) {
//                instanceState.getDecisions().forEach(d -> addIgnoreNull(rv, DecisionDto.create(d)));
//            }
//        }
        return !rv.isEmpty();
    }

    public String getStageInfo() {
        CaseType aCase = getCase();
        return aCase != null ? ApprovalContextUtil.getStageInfo(aCase) : ApprovalContextUtil.getStageInfo(workItem);
    }

    public String getEscalationLevelInfo() {
        return ApprovalContextUtil.getEscalationLevelInfo(workItem);
    }

    public Integer getEscalationLevelNumber() {
        int number = ApprovalContextUtil.getEscalationLevelNumber(workItem);
        return number > 0 ? number : null;
    }

    public List<EvaluatedTriggerGroupDto> getTriggers() {
        if (triggers == null) {
            triggers = WebComponentUtil.computeTriggers(getApprovalContext(), 0); //TODO how to take stageNumber for TaskType?
        }
        return triggers;
    }

    public List<InformationType> getAdditionalInformation() {
        return workItem.getAdditionalInformation();
    }

    // Expects that we deal with primary changes of the focus (i.e. not of projections)
    // Beware: returns the full object; regardless of the security settings
    public ObjectType getFocus(PageBase pageBase) {
        if (focus != null) {
            return focus;
        }
        ApprovalContextType wfc = getApprovalContext();
        if (wfc == null || wfc.getDeltasToApprove() == null || wfc.getDeltasToApprove().getFocusPrimaryDelta() == null) {
            return null;
        }
        ObjectDeltaType delta = wfc.getDeltasToApprove().getFocusPrimaryDelta();
        if (delta.getChangeType() == ChangeTypeType.ADD) {
            focus = CloneUtil.clone((ObjectType) delta.getObjectToAdd());
        } else if (delta.getChangeType() == ChangeTypeType.MODIFY) {
            String oid = delta.getOid();
            if (oid == null) {
                throw new IllegalStateException("No OID in object modify delta: " + delta);
            }
            if (delta.getObjectType() == null) {
                throw new IllegalStateException("No object type in object modify delta: " + delta);
            }
            Class<? extends ObjectType> clazz = ObjectTypes.getObjectTypeFromTypeQName(delta.getObjectType())
                    .getClassDefinition();
            Task task = pageBase.createSimpleTask("getObject");
            PrismObject<?> object = pageBase.runPrivileged(() ->
                    WebModelServiceUtils.loadObject(clazz, oid, pageBase, task, task.getResult()));
            if (object != null) {
                focus = (ObjectType) object.asObjectable();
                try {
                    ObjectDelta<Objectable> objectDelta = DeltaConvertor.createObjectDelta(delta, pageBase.getPrismContext());
                    objectDelta.applyTo((PrismObject) focus.asPrismObject());
                } catch (SchemaException e) {
                    throw new SystemException("Cannot apply delta to focus object: " + e.getMessage(), e);
                }
                focus = (ObjectType) object.asObjectable();
            }
        } else {
            // DELETE case: nothing to do here
        }
        return focus;
    }

    public String getRequesterComment() {
        OperationBusinessContextType businessContext = ApprovalContextUtil.getBusinessContext(aCase);
        return businessContext != null ? businessContext.getComment() : null;
    }
}
