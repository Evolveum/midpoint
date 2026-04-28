/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.mcp.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.util.ValueMetadataTypeUtil;
import com.evolveum.midpoint.schema.util.cases.ApprovalContextUtil;
import com.evolveum.midpoint.schema.util.cases.CaseTypeUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemOutputType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTreeDeltasType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemCompletionEventType;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang3.StringUtils;

/**
 * Normalized workflow view for {@link CaseType} on top of {@link MidpointMcpAttributeProjector} output.
 */
final class MidpointMcpCaseProjector {

    private static final int MAX_CHILD_CASES = 20;

    private MidpointMcpCaseProjector() {}

    static void enrichExplain(
            LinkedHashMap<String, Object> values,
            PrismObject<CaseType> caseObject,
            PrismContext prismContext,
            ModelService modelService,
            Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
                    ConfigurationException, ExpressionEvaluationException {

        CaseType aCase = caseObject.asObjectable();
        List<CaseWorkItemType> workItems = aCase.getWorkItem() != null ? aCase.getWorkItem() : List.of();
        int totalWi = workItems.size();
        int activeWi = (int) workItems.stream().filter(wi -> wi.getCloseTimestamp() == null).count();

        List<Map<String, Object>> childSummaries = loadChildCaseSummaries(prismContext, modelService, aCase.getOid(), task, result);
        int activeInChildren =
                childSummaries.stream().mapToInt(c -> toInt(c.get("activeWorkItemCount"))).sum();

        Map<String, Object> objectRefNorm = resolveSingleRef(aCase.getObjectRef(), modelService, task, result);
        if (objectRefNorm != null) {
            values.put("objectRef", objectRefNorm);
        }

        values.put("request", buildRequestView(aCase));
        values.put("currentStep", buildCurrentStep(aCase, activeWi, activeInChildren));
        values.put("workItems", buildWorkItemViews(workItems, modelService, task, result));
        values.put("decisionHistory", buildDecisionHistory(aCase, workItems, modelService, task, result));
        values.put("childCases", childSummaries);

        String oneLine = buildOneLineSummary(aCase, activeWi, activeInChildren, childSummaries.size());
        String expl = buildExplanation(aCase, activeWi, activeInChildren, workItems, childSummaries);
        values.put("summary", oneLine);
        values.put("explanation", expl);

        values.put("workItemCount", totalWi);
        values.put("activeWorkItemCount", activeWi);
        if (ValueMetadataTypeUtil.getCreateTimestamp(aCase) != null) {
            values.put(
                    "created",
                    ValueMetadataTypeUtil.getCreateTimestamp(aCase).toXMLFormat());
        }
        if (aCase.getCloseTimestamp() != null) {
            values.put("closed", aCase.getCloseTimestamp().toXMLFormat());
        }
    }

    private static int toInt(Object o) {
        if (o instanceof Number n) {
            return n.intValue();
        }
        return 0;
    }

    private static Map<String, Object> buildRequestView(CaseType aCase) {
        Map<String, Object> m = new LinkedHashMap<>();
        String type = deriveChangeTypeLabel(aCase);
        if (type != null) {
            m.put("type", type);
        }
        String summary = aCase.getName() != null ? aCase.getName().getOrig() : null;
        if (summary != null) {
            m.put("summary", summary);
        }
        return m.isEmpty() ? null : m;
    }

    private static String deriveChangeTypeLabel(CaseType aCase) {
        if (aCase.getApprovalContext() == null || aCase.getApprovalContext().getDeltasToApprove() == null) {
            return null;
        }
        ObjectTreeDeltasType deltas = aCase.getApprovalContext().getDeltasToApprove();
        if (deltas.getFocusPrimaryDelta() != null) {
            ChangeTypeType ct = deltas.getFocusPrimaryDelta().getChangeType();
            return ct != null ? ct.name() : null;
        }
        return null;
    }

    private static Map<String, Object> buildCurrentStep(CaseType aCase, int activeWi, int activeInChildren) {
        Map<String, Object> step = new LinkedHashMap<>();
        boolean closed = CaseTypeUtil.isClosed(aCase);
        String state = aCase.getState() != null ? aCase.getState().toString() : null;
        String stateLocal = state != null && state.contains("#") ? state.substring(state.lastIndexOf('#') + 1) : state;
        String outcome = uriLastSegment(aCase.getOutcome());

        if (closed) {
            step.put("active", false);
            if ("reject".equalsIgnoreCase(outcome) || "rejected".equalsIgnoreCase(outcome)) {
                step.put("name", "Rejected");
            } else if ("approve".equalsIgnoreCase(outcome) || "approved".equalsIgnoreCase(outcome)
                    || "success".equalsIgnoreCase(outcome)) {
                step.put("name", "Completed");
            } else {
                step.put("name", outcome != null ? outcome : "Completed");
            }
            return step;
        }

        if ("executing".equalsIgnoreCase(stateLocal)) {
            step.put("name", "Execution");
            step.put("active", true);
            return step;
        }
        if ("closing".equalsIgnoreCase(stateLocal)) {
            step.put("name", "Processing");
            step.put("active", true);
            return step;
        }

        if (activeWi > 0) {
            String stage = ApprovalContextUtil.getStageName(aCase);
            if (StringUtils.isBlank(stage)) {
                stage = "Approval";
            }
            step.put("name", stage);
            step.put("active", true);
            return step;
        }

        if (activeInChildren > 0) {
            step.put("name", "Approval (child case)");
            step.put("active", true);
            return step;
        }

        step.put("name", stateLocal != null ? StringUtils.capitalize(stateLocal) : "Processing");
        step.put("active", !closed);
        return step;
    }

    private static List<Map<String, Object>> buildWorkItemViews(
            List<CaseWorkItemType> workItems,
            ModelService modelService,
            Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
                    ConfigurationException, ExpressionEvaluationException {

        List<Map<String, Object>> out = new ArrayList<>();
        for (CaseWorkItemType wi : workItems) {
            Map<String, Object> row = new LinkedHashMap<>();
            if (wi.getId() != null) {
                row.put("id", String.valueOf(wi.getId()));
            }
            if (wi.getName() != null) {
                row.put("name", wi.getName().getOrig());
            }
            boolean open = wi.getCloseTimestamp() == null;
            row.put("state", open ? "open" : "closed");
            if (wi.getCreateTimestamp() != null) {
                row.put("created", wi.getCreateTimestamp().toXMLFormat());
            }
            if (wi.getDeadline() != null) {
                row.put("deadline", wi.getDeadline().toXMLFormat());
            }
            if (wi.getCloseTimestamp() != null) {
                row.put("completed", wi.getCloseTimestamp().toXMLFormat());
            }
            ObjectReferenceType assignee = wi.getAssigneeRef() != null && !wi.getAssigneeRef().isEmpty()
                    ? wi.getAssigneeRef().getFirst()
                    : null;
            Map<String, Object> assigneeView = resolveRef(assignee, modelService, task, result);
            if (assigneeView != null) {
                row.put("assignee", assigneeView);
            }
            List<Map<String, Object>> cand = new ArrayList<>();
            if (wi.getCandidateRef() != null) {
                for (ObjectReferenceType cr : wi.getCandidateRef()) {
                    Map<String, Object> cv = resolveRef(cr, modelService, task, result);
                    if (cv != null) {
                        cand.add(cv);
                    }
                }
            }
            if (!cand.isEmpty()) {
                row.put("candidates", cand);
            }
            if (wi.getOutput() != null) {
                AbstractWorkItemOutputType o = wi.getOutput();
                if (o.getOutcome() != null) {
                    row.put("outcome", uriLastSegment(o.getOutcome()));
                }
                if (StringUtils.isNotBlank(o.getComment())) {
                    row.put("comment", o.getComment());
                }
            }
            out.add(row);
        }
        return out;
    }

    private static List<Map<String, Object>> buildDecisionHistory(
            CaseType aCase,
            List<CaseWorkItemType> workItems,
            ModelService modelService,
            Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
                    ConfigurationException, ExpressionEvaluationException {

        List<Map<String, Object>> rows = new ArrayList<>();
        for (CaseWorkItemType wi : workItems) {
            if (wi.getCloseTimestamp() == null || wi.getOutput() == null) {
                continue;
            }
            ObjectReferenceType actor = wi.getPerformerRef() != null ? wi.getPerformerRef() : firstAssignee(wi);
            Map<String, Object> actorView = resolveRef(actor, modelService, task, result);
            if (actorView == null && actor != null) {
                actorView = Map.of("oid", actor.getOid());
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("actor", actorView);
            if (wi.getOutput().getOutcome() != null) {
                row.put("decision", uriLastSegment(wi.getOutput().getOutcome()));
            }
            row.put("outcome", "SUCCESS");
            if (StringUtils.isNotBlank(wi.getOutput().getComment())) {
                row.put("comment", wi.getOutput().getComment());
            }
            row.put("timestamp", wi.getCloseTimestamp().toXMLFormat());
            rows.add(row);
        }

        Set<String> seenTimestamps = new LinkedHashSet<>();
        for (Map<String, Object> row : rows) {
            Object t = row.get("timestamp");
            if (t != null) {
                seenTimestamps.add(String.valueOf(t));
            }
        }

        if (aCase.getEvent() != null) {
            for (var ev : aCase.getEvent()) {
                if (ev instanceof WorkItemCompletionEventType wce && wce.getOutput() != null) {
                    String tsXml = wce.getTimestamp().toXMLFormat();
                    if (seenTimestamps.contains(tsXml)) {
                        continue;
                    }
                    Map<String, Object> row = new LinkedHashMap<>();
                    ObjectReferenceType actor = wce.getAttorneyRef() != null ? wce.getAttorneyRef() : wce.getInitiatorRef();
                    row.put("actor", resolveRef(actor, modelService, task, result));
                    if (wce.getOutput().getOutcome() != null) {
                        row.put("decision", uriLastSegment(wce.getOutput().getOutcome()));
                    }
                    row.put("outcome", "SUCCESS");
                    if (StringUtils.isNotBlank(wce.getOutput().getComment())) {
                        row.put("comment", wce.getOutput().getComment());
                    }
                    row.put("timestamp", tsXml);
                    rows.add(row);
                    seenTimestamps.add(tsXml);
                }
            }
        }

        rows.sort(Comparator.comparing(r -> String.valueOf(r.get("timestamp"))));
        return rows;
    }

    private static ObjectReferenceType firstAssignee(CaseWorkItemType wi) {
        if (wi.getAssigneeRef() == null || wi.getAssigneeRef().isEmpty()) {
            return null;
        }
        return wi.getAssigneeRef().getFirst();
    }

    private static List<Map<String, Object>> loadChildCaseSummaries(
            PrismContext prismContext,
            ModelService modelService,
            String parentOid,
            Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
                    ConfigurationException, ExpressionEvaluationException {

        if (StringUtils.isBlank(parentOid)) {
            return List.of();
        }
        ObjectQuery query = prismContext.queryFor(CaseType.class)
                .item(CaseType.F_PARENT_REF)
                .ref(parentOid)
                .maxSize(MAX_CHILD_CASES)
                .build();
        SearchResultList<PrismObject<CaseType>> children = modelService.searchObjects(CaseType.class, query, null, task, result);
        List<Map<String, Object>> out = new ArrayList<>();
        for (PrismObject<CaseType> ch : children) {
            CaseType c = ch.asObjectable();
            List<CaseWorkItemType> wis = c.getWorkItem() != null ? c.getWorkItem() : List.of();
            int total = wis.size();
            int active = (int) wis.stream().filter(w -> w.getCloseTimestamp() == null).count();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("oid", c.getOid());
            if (c.getName() != null) {
                m.put("name", c.getName().getOrig());
            }
            if (c.getState() != null) {
                m.put("state", uriLastSegment(c.getState().toString()));
            }
            m.put("workItemCount", total);
            m.put("activeWorkItemCount", active);
            out.add(m);
        }
        return out;
    }

    private static String buildOneLineSummary(
            CaseType aCase, int activeWi, int activeInChildren, int childCount) {
        if (CaseTypeUtil.isClosed(aCase)) {
            String o = uriLastSegment(aCase.getOutcome());
            if ("reject".equalsIgnoreCase(o) || "rejected".equalsIgnoreCase(o)) {
                return "Case closed: rejected.";
            }
            return "Case closed: completed.";
        }
        if (activeWi > 0) {
            return activeWi == 1
                    ? "One approval work item is open."
                    : activeWi + " parallel approval work items are open.";
        }
        if (activeInChildren > 0) {
            return "Approval is active in " + (childCount == 1 ? "a child case" : childCount + " child cases")
                    + " (" + activeInChildren + " open work item(s)).";
        }
        return "Case is open; no pending work items on this case object.";
    }

    private static String buildExplanation(
            CaseType aCase,
            int activeWi,
            int activeInChildren,
            List<CaseWorkItemType> workItems,
            List<Map<String, Object>> childSummaries) {

        StringBuilder sb = new StringBuilder();
        String req = aCase.getName() != null ? aCase.getName().getOrig() : "This case";
        sb.append(req).append(". ");

        if (CaseTypeUtil.isClosed(aCase)) {
            sb.append("The workflow has finished with outcome ")
                    .append(uriLastSegment(aCase.getOutcome()))
                    .append(".");
            return sb.toString();
        }

        String stage = ApprovalContextUtil.getStageName(aCase);
        if (activeWi > 0) {
            sb.append("Current stage: ")
                    .append(StringUtils.isNotBlank(stage) ? stage : "approval")
                    .append(". ");
            sb.append(activeWi).append(" approver(s) must still act.");
        } else if (activeInChildren > 0) {
            sb.append("This case has no local work items; pending approvals are on ")
                    .append(childSummaries.size())
                    .append(" child case(s) (")
                    .append(activeInChildren)
                    .append(" open work item(s) total).");
        } else {
            sb.append("No open work items are attached to this case object.");
        }

        long decided = workItems.stream().filter(w -> w.getCloseTimestamp() != null).count();
        if (decided > 0) {
            sb.append(" ").append(decided).append(" work item(s) were already completed.");
        }
        return sb.toString().trim();
    }

    private static Map<String, Object> resolveSingleRef(
            ObjectReferenceType ref, ModelService modelService, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
                    ConfigurationException, ExpressionEvaluationException {
        if (ref == null || StringUtils.isBlank(ref.getOid())) {
            return null;
        }
        return resolveRef(ref, modelService, task, result);
    }

    private static Map<String, Object> resolveRef(
            ObjectReferenceType ref, ModelService modelService, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
                    ConfigurationException, ExpressionEvaluationException {

        if (ref == null || StringUtils.isBlank(ref.getOid())) {
            return null;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("oid", ref.getOid());
        QName tt = ref.getType();
        if (tt != null) {
            ObjectTypes ot = ObjectTypes.getObjectTypeFromTypeQNameIfKnown(tt);
            m.put("type", ot != null ? ot.getRestType() : tt.getLocalPart());
        }
        String nm = polyOrig(ref.getTargetName());
        if (StringUtils.isNotBlank(nm)) {
            m.put("name", nm);
            return m;
        }
        if (modelService != null && task != null && result != null) {
            Class<? extends ObjectType> clazz =
                    tt != null ? ObjectTypes.getObjectTypeClassIfKnown(tt) : ObjectType.class;
            if (clazz == null) {
                clazz = ObjectType.class;
            }
            try {
                PrismObject<? extends ObjectType> obj = modelService.getObject(clazz, ref.getOid(), null, task, result);
                if (obj.getName() != null) {
                    m.put("name", obj.getName().getOrig());
                }
                ObjectTypes ot = ObjectTypes.getObjectTypeIfKnown(obj.getCompileTimeClass());
                if (ot != null) {
                    m.put("type", ot.getRestType());
                }
            } catch (ObjectNotFoundException | SecurityViolationException e) {
                // keep oid only
            }
        }
        return m;
    }

    private static String polyOrig(PolyStringType ps) {
        return ps != null ? ps.getOrig() : null;
    }

    private static String uriLastSegment(Object uri) {
        if (uri == null) {
            return null;
        }
        String s = uri.toString();
        int hash = s.lastIndexOf('#');
        if (hash >= 0 && hash < s.length() - 1) {
            return s.substring(hash + 1);
        }
        int slash = s.lastIndexOf('/');
        if (slash >= 0 && slash < s.length() - 1) {
            return s.substring(slash + 1);
        }
        return s;
    }

    /**
     * Compact {@code currentStep} for search hits (no child-case query). Explain uses full {@link #buildCurrentStep}
     * with child aggregation.
     */
    static Map<String, Object> compactCurrentStepForSearch(CaseType aCase) {
        List<CaseWorkItemType> workItems = aCase.getWorkItem() != null ? aCase.getWorkItem() : List.of();
        int activeWi = (int) workItems.stream().filter(wi -> wi.getCloseTimestamp() == null).count();
        Map<String, Object> step = new LinkedHashMap<>();
        if (CaseTypeUtil.isClosed(aCase)) {
            step.put("active", false);
            step.put("name", "Completed");
            return step;
        }
        if (activeWi > 0) {
            String stage = ApprovalContextUtil.getStageName(aCase);
            step.put("name", StringUtils.isNotBlank(stage) ? stage : "Approval");
            step.put("active", true);
        } else {
            step.put("name", "Open");
            step.put("active", false);
        }
        return step;
    }

    /** Paths allowed in filters but not supported for {@code orderBy} on cases. */
    static boolean isOrderByBlockedForCases(String path) {
        if (path == null) {
            return false;
        }
        String p = path.trim();
        return p.startsWith("request.")
                || p.startsWith("currentStep.")
                || p.startsWith("workItems.")
                || p.startsWith("decisionHistory.")
                || p.equals("created")
                || p.equals("closed");
    }
}
