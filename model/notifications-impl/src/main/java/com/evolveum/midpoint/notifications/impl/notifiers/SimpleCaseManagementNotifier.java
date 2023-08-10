/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.notifiers;

import com.evolveum.midpoint.notifications.api.EventProcessingContext;
import com.evolveum.midpoint.notifications.api.events.CaseEvent;
import com.evolveum.midpoint.notifications.api.events.CaseManagementEvent;
import com.evolveum.midpoint.notifications.api.events.SimpleObjectRef;
import com.evolveum.midpoint.notifications.api.events.WorkItemEvent;
import com.evolveum.midpoint.notifications.impl.events.WorkItemAllocationEventImpl;
import com.evolveum.midpoint.notifications.impl.events.WorkItemCustomEventImpl;
import com.evolveum.midpoint.notifications.impl.events.WorkItemEventImpl;
import com.evolveum.midpoint.notifications.impl.events.WorkItemLifecycleEventImpl;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.cases.ApprovalContextUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Default implementation of a notifier dealing with case management events (related to both work items and cases).
 */
@Component
public class SimpleCaseManagementNotifier extends AbstractGeneralNotifier<CaseManagementEvent, SimpleWorkflowNotifierType> {

    private static final Trace LOGGER = TraceManager.getTrace(SimpleCaseManagementNotifier.class);

    @Override
    public @NotNull Class<CaseManagementEvent> getEventType() {
        return CaseManagementEvent.class;
    }

    @Override
    public @NotNull Class<SimpleWorkflowNotifierType> getEventHandlerConfigurationType() {
        return SimpleWorkflowNotifierType.class;
    }

    @Override
    protected UserType getDefaultRecipient(CaseManagementEvent event, OperationResult result) {
        @Nullable SimpleObjectRef recipientRef;
        if (event instanceof CaseEvent) {
            recipientRef = event.getRequester();
        } else if (event instanceof WorkItemEvent) {
            recipientRef = ((WorkItemEvent) event).getAssignee();
        } else {
            return null;
        }
        ObjectType recipient = functions.getObject(recipientRef, false, result);
        if (recipient instanceof UserType) {
            return (UserType) recipient;
        } else {
            return null;
        }
    }

    @Override
    protected String getSubject(
            ConfigurationItem<? extends SimpleWorkflowNotifierType> configuration,
            String transport,
            @NotNull EventProcessingContext<? extends CaseManagementEvent> ctx,
            OperationResult result) {
        return getTitle(ctx.event());
    }

    private String getTitle(@NotNull CaseManagementEvent event) {
        if (event instanceof CaseEvent) {
            return event.isAdd() ?
                    getCaseTitle(event) + " has been opened" :
                    getCaseTitle(event) + " has been closed";
        } else if (event instanceof WorkItemEventImpl) {
            return getSubjectFromWorkItemEvent((WorkItemEventImpl) event);
        } else {
            throw new UnsupportedOperationException("Unsupported event type for event=" + event);
        }
    }

    private String getCaseTitle(@NotNull CaseManagementEvent event) {
        if (event.isApproval()) {
            return "An approval case";
        } else if (event.isManualProvisioning()) {
            return "A manual provisioning";
        } else if (event.isCorrelation()) {
            return "A correlation case";
        } else {
            return "A case";
        }
    }

    private String getSubjectFromWorkItemEvent(WorkItemEventImpl event) {
        if (event instanceof WorkItemLifecycleEventImpl) {
            if (event.isAdd()) {
                return "A new work item has been created";
            } else if (event.isDelete()) {
                if (event.getOperationKind() == WorkItemOperationKindType.COMPLETE) {
                    return "Work item has been completed";
                } else {
                    return "Work item has been cancelled";
                }
            } else {
                throw new UnsupportedOperationException("workItemLifecycle event with MODIFY operation is not supported");
            }
        } else if (event instanceof WorkItemAllocationEventImpl) {
            if (event.isAdd()) {
                return "Work item has been allocated to you";
            } else if (event.isModify()) {
                if (event.getOperationKind() == null) {
                    throw new IllegalStateException("Missing operationKind in " + event);
                }
                String rv = "Work item will be automatically " + getOperationPastTenseVerb(event.getOperationKind());
                if (event.getTimeBefore() != null) { // should always be
                    rv += " in " + DurationFormatUtils.formatDurationWords(
                            event.getTimeBefore().getTimeInMillis(new Date()), true, true);
                }
                return rv;
            } else {
                return "Work item has been " + getOperationPastTenseVerb(event.getOperationKind());
            }
        } else if (event instanceof WorkItemCustomEventImpl) {
            return "A notification about work item";
        } else {
            throw new UnsupportedOperationException("Unsupported event type for event=" + event);
        }
    }

    @Override
    protected String getBody(
            ConfigurationItem<? extends SimpleWorkflowNotifierType> configuration,
            String transport,
            @NotNull EventProcessingContext<? extends CaseManagementEvent> ctx,
            OperationResult result) {

        boolean techInfo = Boolean.TRUE.equals(configuration.value().isShowTechnicalInformation());

        StringBuilder body = new StringBuilder();
        var event = ctx.event();

        body.append(getTitle(event));
        body.append("\n\n");

        appendGeneralInformation(body, event); // case name, work item name, stage, escalation level

        if (event instanceof WorkItemEventImpl) {
            WorkItemEventImpl workItemEvent = (WorkItemEventImpl) event;
            appendAssigneeInformation(body, workItemEvent, result);
            appendResultAndOriginInformation(body, workItemEvent, result);
            appendDeadlineInformation(body, workItemEvent);
        } else {
            appendResultInformation(body, event, true);
        }
        body.append("\nNotification created on: ").append(new Date()).append("\n\n");

        if (techInfo) {
            body.append("----------------------------------------\n");
            body.append("Technical information:\n\n");
            if (event instanceof WorkItemEventImpl workItemEvent) {
                body.append("WorkItem:\n")
                        .append(PrismUtil.serializeQuietly(prismContext, workItemEvent.getWorkItem()))
                        .append("\n");
            }
            if (event.getApprovalContext() != null) {
                body.append("Approval context:\n")
                        .append(PrismUtil.serializeQuietly(prismContext, event.getApprovalContext()));
            }
            if (event.getManualProvisioningContext() != null) {
                body.append("Manual provisioning context:\n")
                        .append(PrismUtil.serializeQuietly(prismContext, event.getManualProvisioningContext()));
            }
            if (event.getCorrelationContext() != null) {
                body.append("Correlation context:\n")
                        .append(PrismUtil.serializeQuietly(prismContext, event.getCorrelationContext()));
            }
        }
        return body.toString();
    }

    private void appendGeneralInformation(StringBuilder sb, CaseManagementEvent event) {
        sb.append("Case name: ").append(event.getCaseName()).append("\n");
        if (event instanceof WorkItemEventImpl workItemEvent) {
            sb.append("Work item: ").append(workItemEvent.getWorkItemName()).append("\n");
            appendStageInformation(sb, event);
            appendEscalationInformation(sb, workItemEvent);
        } else {
            appendStageInformation(sb, event);
        }
        sb.append("\n");
    }

    private boolean appendResultInformation(StringBuilder body, CaseManagementEvent event, boolean emptyLineAfter) {
        if (event.isDelete() && event.isResultKnown()) {
            body.append("Result: ")
                    .append(event.getStatusAsText())
                    .append("\n");
            if (emptyLineAfter) {
                body.append("\n");
            }
            return true;
        } else {
            return false;
        }
    }

    private void appendDeadlineInformation(StringBuilder sb, WorkItemEventImpl event) {
        CaseWorkItemType workItem = event.getWorkItem();
        if (!isDone(event) && workItem.getDeadline() != null) {
            appendDeadlineInformation(sb, workItem);
        }
    }

    private void appendDeadlineInformation(StringBuilder sb, AbstractWorkItemType workItem) {
        XMLGregorianCalendar deadline = workItem.getDeadline();
        long before = XmlTypeConverter.toMillis(deadline) - System.currentTimeMillis();
        long beforeRounded = Math.round((double) before / 60000.0) * 60000L;
        String beforeWords = DurationFormatUtils.formatDurationWords(Math.abs(beforeRounded), true, true);
        String beforePhrase;
        if (beforeRounded > 0) {
            beforePhrase = " (in " + beforeWords + ")";
        } else if (beforeRounded < 0) {
            beforePhrase = " (" + beforeWords + " ago)";
        } else {
            beforePhrase = "";
        }
        sb.append("Deadline: ").append(valueFormatter.formatDateTime(deadline)).append(beforePhrase).append("\n");
        sb.append("\n");
    }

    private void appendResultAndOriginInformation(StringBuilder sb, WorkItemEventImpl event, OperationResult result) {
        boolean atLeastOne = appendResultInformation(sb, event, false);
        WorkItemEventCauseInformationType cause = event.getCause();
        if (cause != null && cause.getType() == WorkItemEventCauseTypeType.TIMED_ACTION) {
            sb.append("Reason: ");
            if (cause.getDisplayName() != null) {
                sb.append(cause.getDisplayName()).append(" (timed action)");
            } else if (cause.getName() != null) {
                sb.append(cause.getName()).append(" (timed action)");
            } else {
                sb.append("Timed action");
            }
            sb.append("\n");
            atLeastOne = true;
        } else {
            SimpleObjectRef initiator = event.getInitiator();
            if (initiator != null && !isCancelled(event)) {
                UserType initiatorFull = (UserType) functions.getObject(initiator, true, result);
                sb.append("Carried out by: ").append(valueFormatter.formatUserName(initiatorFull, initiator.getOid())).append("\n");
                atLeastOne = true;
            }
        }
        if (atLeastOne) {
            sb.append("\n");
        }
    }

    private void appendAssigneeInformation(StringBuilder sb, WorkItemEventImpl event, OperationResult result) {
        CaseWorkItemType workItem = event.getWorkItem();
        ObjectReferenceType originalAssignee = workItem.getOriginalAssigneeRef();
        List<ObjectReferenceType> currentAssignees = workItem.getAssigneeRef();
        boolean atLeastOne = false;
        if (showOriginalAssignee(originalAssignee, currentAssignees)) {
            UserType originalAssigneeObject = (UserType) functions.getObject(originalAssignee, true, result);
            sb.append("Originally allocated to: ").append(
                    valueFormatter.formatUserName(originalAssigneeObject, originalAssignee.getOid())).append("\n");
            atLeastOne = true;
        }
        if (!workItem.getAssigneeRef().isEmpty()) {
            sb.append("Allocated to");
            if (event.getOperationKind() == WorkItemOperationKindType.DELEGATE) {
                sb.append(event.isAdd() ? " (after delegation)" : " (before delegation)");
            } else if (event.getOperationKind() == WorkItemOperationKindType.ESCALATE) {
                sb.append(event.isAdd() ? " (after escalation)" : " (before escalation)");
            }
            sb.append(": ");
            sb.append(workItem.getAssigneeRef().stream()
                    .map(ref -> valueFormatter.formatUserName(ref, result))
                    .collect(Collectors.joining(", ")));
            sb.append("\n");
            atLeastOne = true;
        }
        if (atLeastOne) {
            sb.append("\n");
        }
    }

    private boolean showOriginalAssignee(ObjectReferenceType originalAssignee, List<ObjectReferenceType> currentAssignees) {
        if (originalAssignee == null) {
            return false; // nothing to show
        }
        if (currentAssignees.size() != 1) {
            return true;
        }
        return !Objects.equals(originalAssignee.getOid(), currentAssignees.get(0).getOid());
    }

    // a bit of heuristics...
    @SuppressWarnings("SimplifiableIfStatement")
    private boolean isDone(WorkItemEventImpl event) {
        if (event instanceof WorkItemLifecycleEventImpl) {
            return event.isDelete();
        } else if (event instanceof WorkItemAllocationEventImpl) {
            return event.isDelete() &&
                    (event.getOperationKind() == null || event.getOperationKind() == WorkItemOperationKindType.CANCEL
                            || event.getOperationKind() == WorkItemOperationKindType.COMPLETE);
        } else {
            return false;
        }
    }

    private boolean isCancelled(WorkItemEventImpl event) {
        return (event instanceof WorkItemLifecycleEventImpl || event instanceof WorkItemAllocationEventImpl)
                && event.isDelete()
                && (event.getOperationKind() == null || event.getOperationKind() == WorkItemOperationKindType.CANCEL);
    }

    private void appendEscalationInformation(StringBuilder sb, WorkItemEventImpl workItemEvent) {
        String info = ApprovalContextUtil.getEscalationLevelInfo(workItemEvent.getWorkItem());
        if (info != null) {
            sb.append("Escalation level: ").append(info).append("\n");
        }
    }

    private void appendStageInformation(StringBuilder sb, CaseManagementEvent caseManagementEvent) {
        if (caseManagementEvent.doesUseStages()) {
            String info = ApprovalContextUtil.getStageInfo(caseManagementEvent.getCase());
            if (info != null) {
                sb.append("Stage: ").append(info).append("\n");
            }
        }
    }

    @Override
    protected Trace getLogger() {
        return LOGGER;
    }

    private String getOperationPastTenseVerb(WorkItemOperationKindType operationKind) {
        if (operationKind == null) {
            return "cancelled"; // OK?
        }
        return switch (operationKind) {
            case CLAIM -> "claimed";
            case RELEASE -> "released";
            case COMPLETE -> "completed";
            case DELEGATE -> "delegated";
            case ESCALATE -> "escalated";
            case CANCEL -> "cancelled";
        };
    }
}
