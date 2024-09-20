/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.util.cases;

import static java.util.Collections.emptyList;

import static com.evolveum.midpoint.prism.polystring.PolyString.getOrig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.datatype.XMLGregorianCalendar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * TODO clean up these formatting methods
 */
public class ApprovalContextUtil {

    @Nullable
    public static String getStageInfo(CaseType aCase) {
        if (aCase == null || isClosed(aCase)) {
            return null;
        }
        return getStageInfo(aCase.getStageNumber(), getStageCount(aCase.getApprovalContext()), getStageName(aCase), getStageDisplayName(aCase));
    }

    @Nullable
    public static String getWorkItemStageInfo(CaseWorkItemType workItem) {
        if (workItem == null) {
            return null;
        }
        CaseType aCase = CaseTypeUtil.getCase(workItem);
        return getStageInfo(workItem.getStageNumber(), getStageCount(aCase.getApprovalContext()),
                getWorkItemStageName(workItem), getWorkItemStageDisplayName(workItem));
    }

    @Nullable
    public static String getStageInfo(CaseWorkItemType workItem) {
        if (workItem == null) {
            return null;
        }
        return getStageInfo(CaseTypeUtil.getCase(workItem));
    }

    public static String getWorkItemStageName(CaseWorkItemType workItem) {
        if (workItem == null) {
            return "";
        }
        CaseType aCase = CaseTypeUtil.getCaseRequired(workItem);
        if (aCase.getApprovalContext() == null || workItem.getStageNumber() == null) {
            return "";
        }
        ApprovalStageDefinitionType def = getStageDefinition(aCase.getApprovalContext(), workItem.getStageNumber());
        return def != null ? def.getName() : "";
    }

    public static String getWorkItemStageDisplayName(CaseWorkItemType workItem) {
        if (workItem == null) {
            return "";
        }
        CaseType aCase = CaseTypeUtil.getCaseRequired(workItem);
        if (aCase.getApprovalContext() == null || workItem.getStageNumber() == null) {
            return "";
        }
        ApprovalStageDefinitionType def = getStageDefinition(aCase.getApprovalContext(), workItem.getStageNumber());
        return def != null ? def.getDisplayName() : "";
    }

    public static String getStageName(CaseType aCase) {
        ApprovalStageDefinitionType def = getCurrentStageDefinition(aCase);
        return def != null ? def.getName() : null;
    }

    public static String getStageDisplayName(CaseType aCase) {
        ApprovalStageDefinitionType def = getCurrentStageDefinition(aCase);
        return def != null ? def.getDisplayName() : null;
    }

    public static ApprovalSchemaType getApprovalSchema(ApprovalContextType wfc) {
        return wfc != null ? wfc.getApprovalSchema() : null;
    }

    public static Integer getStageCount(ApprovalContextType wfc) {
        ApprovalSchemaType schema = getApprovalSchema(wfc);
        return schema != null ? schema.getStage().size() : null;
    }

    public static String getStageDisplayName(CaseWorkItemType workItem) {
        return getStageDisplayName(CaseTypeUtil.getCase(workItem));
    }

    // wfc is used to retrieve approval schema (if needed)
    public static String getStageInfo(Integer stageNumber, Integer stageCount, String stageName, String stageDisplayName) {
        String name = stageDisplayName != null ? stageDisplayName : stageName;
        if (name == null && stageNumber == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (name != null) {
            sb.append(name);
        }
        appendNumber(stageNumber, stageCount, sb);
        return sb.toString();
    }

    @Nullable
    public static String getEscalationLevelInfo(AbstractWorkItemType workItem) {
        if (workItem == null) {
            return null;
        }
        return getEscalationLevelInfo(workItem.getEscalationLevel());
    }

    // TODO move to better place
    public static String getEscalationLevelInfo(WorkItemEscalationLevelType e) {
        if (e == null || e.getNumber() == null || e.getNumber() == 0) {
            return null;
        }
        String name = e.getDisplayName() != null ? e.getDisplayName() : e.getName();
        if (name != null) {
            return name + " (" + e.getNumber() + ")";
        } else {
            return String.valueOf(e.getNumber());
        }
    }

    public static boolean isClosed(ApprovalContextType wfc) {
        return CaseTypeUtil.isClosed(getCase(wfc));
    }

    public static boolean isClosed(CaseType aCase) {
        return CaseTypeUtil.isClosed(aCase);
    }

    private static CaseType getCase(ApprovalContextType wfc) {
        PrismContainerable<?> parent = wfc != null ? wfc.asPrismContainerValue().getParent() : null;
        if (parent == null) {
            return null;
        } else if (!(parent instanceof PrismContainer<?>)) {
            throw new IllegalStateException("Expected PrismContainer as a parent of workflow context, got: " + parent);
        }
        PrismContainerValue<?> grandParent = ((PrismContainer<?>) parent).getParent();
        if (grandParent == null) {
            return null;
        }
        Containerable c = grandParent.asContainerable();
        if (!(c instanceof CaseType)) {
            throw new IllegalStateException("Expected CaseType as a grandparent of workflow context, got: " + c);
        } else {
            return (CaseType) c;
        }
    }

    @Nullable
    public static String getCompleteStageInfo(CaseType aCase) {
        if (aCase == null || isClosed(aCase)) {
            return null;
        }
        Integer stageNumber = aCase.getStageNumber();
        String stageName = getStageName(aCase);
        String stageDisplayName = getStageDisplayName(aCase);
        if (stageNumber == null && stageName == null && stageDisplayName == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (stageName != null && stageDisplayName != null) {
            sb.append(stageName).append(" (").append(stageDisplayName).append(")");
        } else if (stageName != null) {
            sb.append(stageName);
        } else if (stageDisplayName != null) {
            sb.append(stageDisplayName);
        }
        appendNumber(stageNumber, getStageCount(aCase.getApprovalContext()), sb);
        return sb.toString();
    }

    public static void appendNumber(Integer stageNumber, Integer stageCount, StringBuilder sb) {
        if (stageNumber != null) {
            boolean parentheses = !sb.isEmpty();
            if (parentheses) {
                sb.append(" (");
            }
            sb.append(stageNumber);
            if (stageCount != null) {
                sb.append("/").append(stageCount);
            }
            if (parentheses) {
                sb.append(")");
            }
        }
    }

    @NotNull
    public static List<SchemaAttachedPolicyRuleType> getAttachedPolicyRules(ApprovalContextType actx, int order) {
        if (actx == null || actx.getPolicyRules() == null) {
            return emptyList();
        }
        return actx.getPolicyRules().getEntry().stream()
                .filter(e -> e.getStageMax() != null
                        && order >= e.getStageMin() && order <= e.getStageMax())
                .collect(Collectors.toList());
    }

    public static @NotNull ApprovalStageDefinitionType getCurrentStageDefinitionRequired(CaseType aCase) {
        int stageNumber = getStageNumberRequired(aCase);
        return MiscUtil.requireNonNull(
                getStageDefinition(
                        getApprovalContextRequired(aCase), stageNumber),
                () -> new IllegalStateException("No definition for stage number " + stageNumber + " in " + aCase));
    }

    public static ApprovalStageDefinitionType getCurrentStageDefinition(CaseType aCase) {
        if (aCase == null || aCase.getStageNumber() == null) {
            return null;
        }
        return getStageDefinition(aCase.getApprovalContext(), aCase.getStageNumber());
    }

    public static int getStageNumberRequired(@NotNull CaseType aCase) {
        return MiscUtil.requireNonNull(
                aCase.getStageNumber(),
                () -> new IllegalStateException("No stage number in " + aCase));
    }

    // expects already normalized definition (using non-deprecated items, numbering stages from 1 to N)
    public static ApprovalStageDefinitionType getStageDefinition(ApprovalContextType actx, int stageNumber) {
        if (actx == null || actx.getApprovalSchema() == null) {
            return null;
        }
        ApprovalSchemaType approvalSchema = actx.getApprovalSchema();
        List<ApprovalStageDefinitionType> stages = approvalSchema.getStage().stream()
                .filter(level -> level.getNumber() != null && level.getNumber() == stageNumber)
                .collect(Collectors.toList());
        if (stages.size() > 1) {
            throw new IllegalStateException("More than one level with order of " + stageNumber + ": " + stages);
        } else if (stages.isEmpty()) {
            return null;
        } else {
            return stages.get(0);
        }
    }

    // we must be strict here; in case of suspicion, throw an exception
    @SuppressWarnings("unchecked")
    public static <T extends CaseEventType> List<T> getEventsForCurrentStage(@NotNull CaseType aCase, @NotNull Class<T> clazz) {
        ApprovalContextType wfc = aCase.getApprovalContext();
        if (wfc == null) {
            throw new IllegalArgumentException("No workflow context in case " + aCase);
        }
        if (aCase.getStageNumber() == null) {
            throw new IllegalArgumentException("No stage number in workflow context");
        }
        int stageNumber = aCase.getStageNumber();
        return aCase.getEvent().stream()
                .filter(e -> clazz.isAssignableFrom(e.getClass()) && e.getStageNumber() != null && stageNumber == e.getStageNumber())
                .map(e -> (T) e)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public static <T extends CaseEventType> List<T> getEvents(@NotNull CaseType aCase, @NotNull Class<T> clazz) {
        return aCase.getEvent().stream()
                .filter(e -> clazz.isAssignableFrom(e.getClass()))
                .map(e -> (T) e)
                .collect(Collectors.toList());
    }

    public static <T extends WorkItemEventType> List<T> getWorkItemEvents(@NotNull CaseType aCase, long workItemId, Class<T> clazz) {
        return aCase.getEvent().stream()
                .filter(e -> clazz.isAssignableFrom(e.getClass()) && workItemId == ((WorkItemEventType) e).getWorkItemId())
                .map(e -> (T) e)
                .collect(Collectors.toList());
    }

    public static String getBriefDiagInfo(CaseType aCase) {
        if (aCase == null) {
            return "null";
        }
        return "process: " + aCase.getName() + ", stage: " + aCase.getStageNumber();
    }

    // expects normalized definition
    public static String getStageDiagName(ApprovalStageDefinitionType level) {
        return level.getNumber() + ":" + level.getName()
                + (level.getDisplayName() != null ? " (" + level.getDisplayName() + ")" : "");
    }

    public static void normalizeStages(ApprovalSchemaType schema) {
        // Sorting uses set(..) method which is not available on prism structures. So we do sort on a copy (ArrayList).
        List<ApprovalStageDefinitionType> stages = getSortedStages(schema);
        for (int i = 0; i < stages.size(); i++) {
            stages.get(i).setNumber(i + 1);
        }
        schema.getStage().clear();
        schema.getStage().addAll(CloneUtil.cloneCollectionMembers(stages));
    }

    @NotNull
    private static List<ApprovalStageDefinitionType> getSortedStages(ApprovalSchemaType schema) {
        List<ApprovalStageDefinitionType> stages = new ArrayList<>(schema.getStage());
        stages.sort(Comparator.comparing(ApprovalContextUtil::getNumber, Comparator.nullsLast(Comparator.naturalOrder())));
        return stages;
    }

    public static List<ApprovalStageDefinitionType> sortAndCheckStages(ApprovalSchemaType schema) {
        List<ApprovalStageDefinitionType> stages = getSortedStages(schema);
        for (int i = 0; i < stages.size(); i++) {
            ApprovalStageDefinitionType stage = stages.get(i);
            Integer number = getNumber(stage);
            if (number == null || number != i + 1) {
                throw new IllegalArgumentException("Missing or wrong number of stage #" + (i + 1) + ": " + number);
            }
            stage.setNumber(number);
        }
        return stages;
    }

    private static Integer getNumber(ApprovalStageDefinitionType stage) {
        return stage.getNumber();
    }

    public static OperationBusinessContextType getBusinessContext(CaseType aCase) {
        if (aCase == null) {
            return null;
        }
        for (CaseEventType event : aCase.getEvent()) {
            if (event instanceof CaseCreationEventType) {
                return ((CaseCreationEventType) event).getBusinessContext();
            }
        }
        return null;
    }

    // TODO take from the workflow context!
    public static String getStageInfoTODO(Integer stageNumber) {
        return getStageInfo(stageNumber, null, null, null);
    }

    public static ApprovalContextType getApprovalContext(CaseWorkItemType workItem) {
        return CaseTypeUtil.getCaseRequired(workItem).getApprovalContext();
    }

    public static CaseType getCase(ApprovalSchemaExecutionInformationType info) {
        if (info == null || info.getCaseRef() == null || info.getCaseRef().asReferenceValue().getObject() == null) {
            return null;
        }
        return (CaseType) info.getCaseRef().asReferenceValue().getObject().asObjectable();
    }

    public static ApprovalContextType getApprovalContext(ApprovalSchemaExecutionInformationType info) {
        CaseType aCase = getCase(info);
        return aCase != null ? aCase.getApprovalContext() : null;
    }

    public static ObjectReferenceType getObjectRef(CaseWorkItemType workItem) {
        return CaseTypeUtil.getCaseRequired(workItem).getObjectRef();
    }

    public static ObjectReferenceType getObjectRef(PrismContainerValue<CaseWorkItemType> workItem) {
        return getObjectRef(workItem.asContainerable());
    }

    public static ObjectReferenceType getTargetRef(CaseWorkItemType workItem) {
        return CaseTypeUtil.getCaseRequired(workItem).getTargetRef();
    }

    public static ObjectReferenceType getTargetRef(PrismContainerValue<CaseWorkItemType> workItem) {
        return getTargetRef(workItem.asContainerable());
    }

    public static ObjectReferenceType getRequesterRef(CaseWorkItemType workItem) {
        return CaseTypeUtil.getCaseRequired(workItem).getRequestorRef();
    }

    public static ObjectReferenceType getRequesterRef(PrismContainerValue<CaseWorkItemType> workItem) {
        return getRequesterRef(workItem.asContainerable());
    }

    public static XMLGregorianCalendar getStartTimestamp(CaseWorkItemType workItem) {
        return CaseTypeUtil.getStartTimestamp(CaseTypeUtil.getCase(workItem));
    }

    public static XMLGregorianCalendar getStartTimestamp(PrismContainerValue<CaseWorkItemType> workItem) {
        return getStartTimestamp(workItem.asContainerable());
    }

    public static Integer getEscalationLevelNumber(WorkItemEventType event) {
        return WorkItemTypeUtil.getEscalationLevelNumber(event.getEscalationLevel());
    }

    // TODO better place
    @NotNull
    public static WorkItemEventCauseInformationType createCause(AbstractWorkItemActionType action) {
        WorkItemEventCauseInformationType cause = new WorkItemEventCauseInformationType();
        cause.setType(WorkItemEventCauseTypeType.TIMED_ACTION);
        if (action != null) {
            cause.setName(action.getName());
            cause.setDisplayName(action.getDisplayName());
        }
        return cause;
    }

    // TODO better place
    @Nullable
    public static WorkItemOperationKindType getOperationKind(AbstractWorkItemActionType action) {
        WorkItemOperationKindType operationKind;
        if (action instanceof EscalateWorkItemActionType) {
            operationKind = WorkItemOperationKindType.ESCALATE;
        } else if (action instanceof DelegateWorkItemActionType) {
            operationKind = WorkItemOperationKindType.DELEGATE;
        } else if (action instanceof CompleteWorkItemActionType) {
            operationKind = WorkItemOperationKindType.COMPLETE;
        } else {
            // shouldn't occur
            operationKind = null;
        }
        return operationKind;
    }

    @NotNull
    public static WorkItemEscalationLevelType createEscalationLevelInformation(DelegateWorkItemActionType delegateAction) {
        String escalationLevelName;
        String escalationLevelDisplayName;
        if (delegateAction instanceof EscalateWorkItemActionType) {
            escalationLevelName = ((EscalateWorkItemActionType) delegateAction).getEscalationLevelName();
            escalationLevelDisplayName = ((EscalateWorkItemActionType) delegateAction).getEscalationLevelDisplayName();
            if (escalationLevelName == null && escalationLevelDisplayName == null) {
                escalationLevelName = delegateAction.getName();
                escalationLevelDisplayName = delegateAction.getDisplayName();
            }
        } else {
            // TODO ... a warning here?
            escalationLevelName = escalationLevelDisplayName = null;
        }
        return new WorkItemEscalationLevelType().name(escalationLevelName).displayName(escalationLevelDisplayName);
    }

    public static WorkItemDelegationEventType createDelegationEvent(
            WorkItemEscalationLevelType newEscalation,
            List<ObjectReferenceType> assigneesBefore, List<ObjectReferenceType> delegatedTo,
            @NotNull WorkItemDelegationMethodType method,
            WorkItemEventCauseInformationType causeInformation) {
        WorkItemDelegationEventType event;
        if (newEscalation != null) {
            WorkItemEscalationEventType escEvent = new WorkItemEscalationEventType();
            escEvent.setNewEscalationLevel(newEscalation);
            event = escEvent;
        } else {
            event = new WorkItemDelegationEventType();
        }
        event.getAssigneeBefore().addAll(assigneesBefore);
        event.getDelegatedTo().addAll(delegatedTo);
        event.setDelegationMethod(method);
        event.setCause(causeInformation);
        return event;
    }

    public static boolean isInStageBeforeLastOne(CaseType aCase) {
        if (aCase == null || aCase.getStageNumber() == null) {
            return false;
        }
        ApprovalContextType actx = aCase.getApprovalContext();
        if (actx == null) {
            return false;
        }
        return aCase.getStageNumber() < actx.getApprovalSchema().getStage().size();
    }

    public static String getProcessName(ApprovalSchemaExecutionInformationType info) {
        return info != null ? PolyString.getOrig(ObjectTypeUtil.getName(info.getCaseRef())) : null;
    }

    public static String getTargetName(ApprovalSchemaExecutionInformationType info) {
        CaseType aCase = getCase(info);
        return aCase != null ? getOrig(ObjectTypeUtil.getName(aCase.getTargetRef())) : null;
    }

    public static String getOutcome(ApprovalSchemaExecutionInformationType info) {
        CaseType aCase = getCase(info);
        return aCase != null ? aCase.getOutcome() : null;
    }

    public static List<EvaluatedPolicyRuleType> getAllRules(SchemaAttachedPolicyRulesType policyRules) {
        List<EvaluatedPolicyRuleType> rv = new ArrayList<>();
        if (policyRules == null) {
            return rv;
        }
        for (SchemaAttachedPolicyRuleType entry : policyRules.getEntry()) {
            if (entry == null) {
                continue;
            }
            if (!rv.contains(entry.getRule())) {
                rv.add(entry.getRule());
            }
        }
        return rv;
    }

    public static List<List<EvaluatedPolicyRuleType>> getRulesPerStage(ApprovalContextType actx) {
        List<List<EvaluatedPolicyRuleType>> rv = new ArrayList<>();
        if (actx == null || actx.getPolicyRules() == null) {
            return rv;
        }
        List<SchemaAttachedPolicyRuleType> entries = actx.getPolicyRules().getEntry();
        for (int i = 0; i < actx.getApprovalSchema().getStage().size(); i++) {
            rv.add(getRulesForStage(entries, i + 1));
        }
        return rv;
    }

    @NotNull
    private static List<EvaluatedPolicyRuleType> getRulesForStage(List<SchemaAttachedPolicyRuleType> entries, int stageNumber) {
        List<EvaluatedPolicyRuleType> rulesForStage = new ArrayList<>();
        for (SchemaAttachedPolicyRuleType entry : entries) {
            if (entry.getStageMin() != null && stageNumber >= entry.getStageMin()
                    && entry.getStageMax() != null && stageNumber <= entry.getStageMax()) {
                rulesForStage.add(entry.getRule());
            }
        }
        return rulesForStage;
    }

    @NotNull
    public static List<EvaluatedPolicyRuleType> getRulesForStage(ApprovalContextType actx, Integer stageNumber) {
        if (actx == null || actx.getPolicyRules() == null || stageNumber == null) {
            return emptyList();
        }
        return getRulesForStage(actx.getPolicyRules().getEntry(), stageNumber);
    }

    public static @NotNull ApprovalContextType getApprovalContextRequired(@NotNull CaseType currentCase) {
        return MiscUtil.requireNonNull(
                currentCase.getApprovalContext(),
                () -> new IllegalStateException("No approval context in " + currentCase));
    }

    public static String getRequesterComment(CaseType aCase) {
        OperationBusinessContextType businessContext = getBusinessContext(aCase);
        return businessContext != null ? businessContext.getComment() : null;
    }
}
