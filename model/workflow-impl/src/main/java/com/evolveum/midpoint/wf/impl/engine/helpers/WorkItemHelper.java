/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.engine.helpers;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.WorkItemAllocationChangeOperationInfo;
import com.evolveum.midpoint.wf.api.WorkItemOperationSourceInfo;
import com.evolveum.midpoint.wf.impl.engine.EngineInvocationContext;
import com.evolveum.midpoint.wf.impl.processors.primary.PrimaryChangeProcessor;
import com.evolveum.midpoint.wf.impl.util.MiscHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 *
 */
@Component
public class WorkItemHelper {

    private static final Trace LOGGER = TraceManager.getTrace(WorkItemHelper.class);

    @Autowired private PrismContext prismContext;
    @Autowired private PrimaryChangeProcessor primaryChangeProcessor;
    @Autowired private MiscHelper miscHelper;
    @Autowired private TriggerHelper triggerHelper;

    public static void fillInWorkItemEvent(WorkItemEventType event, MidPointPrincipal currentUser, WorkItemId workItemId,
            CaseWorkItemType workItem, PrismContext prismContext) {
        if (currentUser != null) {
            event.setInitiatorRef(ObjectTypeUtil.createObjectRef(currentUser.getFocus(), prismContext));
            event.setAttorneyRef(ObjectTypeUtil.createObjectRef(currentUser.getAttorney(), prismContext));
        }
        event.setTimestamp(XmlTypeConverter.createXMLGregorianCalendar(new Date()));
        event.setExternalWorkItemId(workItemId.asString());
        event.setWorkItemId(workItemId.id);
        event.setOriginalAssigneeRef(workItem.getOriginalAssigneeRef());
        event.setStageNumber(workItem.getStageNumber());
        event.setEscalationLevel(workItem.getEscalationLevel());
    }

    public void recordWorkItemClosure(EngineInvocationContext ctx, CaseWorkItemType workItem, boolean realClosure,
            WorkItemEventCauseInformationType causeInformation, OperationResult result) throws SchemaException {
        // this might be cancellation because of:
        //  (1) user completion of this task
        //  (2) timed completion of this task
        //  (3) user completion of another task
        //  (4) timed completion of another task
        //  (5) process stop/deletion
        //
        // Actually, when the source is (4) timed completion of another task, it is quite probable that this task
        // would be closed for the same reason. For a user it would be misleading if we would simply view this task
        // as 'cancelled', while, in fact, it is e.g. approved/rejected because of a timed action.

        LOGGER.trace("+++ recordWorkItemClosure ENTER: workItem={}, ctx={}, realClosure={}", workItem, ctx, realClosure);
        WorkItemOperationKindType operationKind = realClosure ? WorkItemOperationKindType.COMPLETE : WorkItemOperationKindType.CANCEL;

        MidPointPrincipal user;
        try {
            user = SecurityUtil.getPrincipal();
        } catch (SecurityViolationException e) {
            throw new SystemException("Couldn't determine current user: " + e.getMessage(), e);
        }

        ObjectReferenceType userRef = user != null ? user.toObjectReference() : workItem.getPerformerRef();    // partial fallback

        WorkItemId workItemId = WorkItemId.create(ctx.getCaseOid(), workItem.getId());

        AbstractWorkItemOutputType output = workItem.getOutput();
        if (realClosure || output != null) {
            WorkItemCompletionEventType event = new WorkItemCompletionEventType(prismContext);
            fillInWorkItemEvent(event, user, workItemId, workItem, prismContext);
            event.setCause(causeInformation);
            event.setOutput(output);
            ctx.addEvent(event);
            ObjectDeltaType additionalDelta = output instanceof WorkItemResultType && ((WorkItemResultType) output).getAdditionalDeltas() != null ?
                    ((WorkItemResultType) output).getAdditionalDeltas().getFocusPrimaryDelta() : null;
            if (additionalDelta != null) {
                ctx.updateDelta(additionalDelta);
            }
        }

        // We don't pass userRef (initiator) to the audit method. It does need the whole object (not only the reference),
        // so it fetches it directly from the security enforcer (logged-in user). This could change in the future.
        AuditEventRecord auditEventRecord = primaryChangeProcessor.prepareWorkItemDeletedAuditRecord(workItem, causeInformation,
                ctx.getCurrentCase(), result);
        ctx.addAuditRecord(auditEventRecord);
        try {
            List<ObjectReferenceType> assigneesAndDeputies = miscHelper.getAssigneesAndDeputies(workItem, ctx.getTask(), result);
            WorkItemAllocationChangeOperationInfo operationInfo =
                    new WorkItemAllocationChangeOperationInfo(operationKind, assigneesAndDeputies, null);
            WorkItemOperationSourceInfo sourceInfo = new WorkItemOperationSourceInfo(userRef, causeInformation, null);
            if (workItem.getAssigneeRef().isEmpty()) {
                ctx.prepareNotification(new DelayedNotification.ItemDeletion(ctx.getCurrentCase(), workItem, operationInfo, sourceInfo, null));
            } else {
                for (ObjectReferenceType assigneeOrDeputy : assigneesAndDeputies) {
                    ctx.prepareNotification(new DelayedNotification.ItemDeletion(ctx.getCurrentCase(), workItem, operationInfo, sourceInfo, assigneeOrDeputy));
                }
            }
            ctx.prepareNotification(new DelayedNotification.AllocationChangeCurrent(ctx.getCurrentCase(), workItem, operationInfo, sourceInfo, null));
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't prepare notifications for work item complete event", e);
        }

        triggerHelper.removeTriggersForWorkItem(ctx.getCurrentCase(), workItem.getId(), result);

        LOGGER.trace("--- recordWorkItemClosure EXIT: workItem={}, ctx={}, realClosure={}", workItem, ctx, realClosure);
    }
}
