/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.processors.primary.policy;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.util.*;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.context.AssociatedPolicyRule;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.util.PrismPrettyPrinter;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalPolicyActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyActionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfProcessSpecificationType;

/**
 * Maintains "process specifications" i.e. recipes how to
 *  - analyze incoming deltas (WfProcessSpecificationType.deltaFrom),
 *  - create approval processes
 *  - fill-in their approval schema (list of approval actions with policy rules)
 *
 * TODO find better names
 */
public class ProcessSpecifications implements DebugDumpable {

    private final List<ProcessSpecification> specifications = new ArrayList<>();
    private final PrismContext prismContext;

    // use createFromRules instead
    private ProcessSpecifications(PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    public class ProcessSpecification implements DebugDumpable {
        final WfProcessSpecificationType basicSpec;
        final List<ApprovalActionWithRule> actionsWithRules;

        private ProcessSpecification(Map.Entry<WfProcessSpecificationType, List<ApprovalActionWithRule>> entry) {
            this.basicSpec = entry.getKey();
            this.actionsWithRules = entry.getValue();
        }

        @Override
        public String debugDump(int indent) {
            StringBuilder sb = new StringBuilder();
            DebugUtil.debugDumpLabelLn(sb, "process specification", indent);
            PrismPrettyPrinter.debugDumpValue(
                    sb, indent+1, basicSpec,
                    prismContext, ApprovalPolicyActionType.F_PROCESS_SPECIFICATION, PrismContext.LANG_YAML);
            sb.append("\n");
            DebugUtil.debugDumpLabelLn(sb, "actions with rules", indent);
            for (ApprovalActionWithRule actionWithRule : actionsWithRules) {
                DebugUtil.debugDumpLabelLn(sb, "action", indent+1);
                PrismPrettyPrinter.debugDumpValue(
                        sb, indent+2, actionWithRule.approvalAction,
                        prismContext, PolicyActionsType.F_APPROVAL, PrismContext.LANG_YAML);
                sb.append("\n");
                sb.append("\n").append(actionWithRule.policyRule.debugDump(indent + 2));
            }
            return sb.toString();
        }
    }

    static ProcessSpecifications createFromRules(List<AssociatedPolicyRule> rules, PrismContext prismContext)
            throws ConfigurationException {
        // Step 1: plain list of approval actions -> map: process-spec -> list of related actions/rules ("collected")
        LinkedHashMap<WfProcessSpecificationType, List<ApprovalActionWithRule>> collectedSpecifications = new LinkedHashMap<>();
        for (AssociatedPolicyRule rule : rules) {
            for (ApprovalPolicyActionType approvalAction : rule.getEnabledActions(ApprovalPolicyActionType.class)) {
                WfProcessSpecificationType spec = approvalAction.getProcessSpecification();
                collectedSpecifications
                        .computeIfAbsent(spec, s -> new ArrayList<>())
                        .add(new ApprovalActionWithRule(approvalAction, rule));
            }
        }
        // Step 2: resolve references
        // specifications are cloned to avoid concurrent modification exception
        for (WfProcessSpecificationType spec : new HashSet<>(collectedSpecifications.keySet())) {
            if (spec == null) {
                continue;
            }
            String ref = spec.getRef();
            if (ref != null) {
                List<Map.Entry<WfProcessSpecificationType, List<ApprovalActionWithRule>>> matching =
                        collectedSpecifications.entrySet().stream()
                                .filter(e -> e.getKey() != null && ref.equals(e.getKey().getName()))
                                .collect(Collectors.toList());
                if (matching.isEmpty()) {
                    throw new IllegalStateException(
                            "Process specification named '" + ref + "' referenced from an approval action couldn't be found");
                } else if (matching.size() > 1) {
                    throw new IllegalStateException(
                            "More than one process specification named '" + ref + "' referenced from an approval action: " + matching);
                } else {
                    // move all actions/rules to the referenced process specification
                    List<ApprovalActionWithRule> referencedSpecActions = matching.get(0).getValue();
                    referencedSpecActions.addAll(collectedSpecifications.get(spec));
                    collectedSpecifications.remove(spec);
                }
            }
        }

        Map<String, ApprovalActionWithRule> actionsMap = null;

        // Step 3: include other actions
        for (Map.Entry<WfProcessSpecificationType, List<ApprovalActionWithRule>> processSpecificationEntry :
                collectedSpecifications.entrySet()) {
            WfProcessSpecificationType spec = processSpecificationEntry.getKey();
            if (spec == null || spec.getIncludeAction().isEmpty() && spec.getIncludeActionIfPresent().isEmpty()) {
                continue;
            }
            if (actionsMap == null) {
                actionsMap = createActionsMap(collectedSpecifications.values());
            }
            for (String actionToInclude : spec.getIncludeAction()) {
                processActionToInclude(actionToInclude, actionsMap, processSpecificationEntry, true);
            }
            for (String actionToInclude : spec.getIncludeActionIfPresent()) {
                processActionToInclude(actionToInclude, actionsMap, processSpecificationEntry, false);
            }
        }

        // Step 4: sorts process specifications and wraps into ProcessSpecification objects
        ProcessSpecifications rv = new ProcessSpecifications(prismContext);
        collectedSpecifications.entrySet().stream()
                .sorted((ps1, ps2) -> {
                    WfProcessSpecificationType key1 = ps1.getKey();
                    WfProcessSpecificationType key2 = ps2.getKey();
                    if (key1 == null) {
                        return key2 == null ? 0 : 1; // non-empty (key2) records first
                    } else if (key2 == null) {
                        return -1; // non-empty (key1) record first
                    }
                    int order1 = defaultIfNull(key1.getOrder(), Integer.MAX_VALUE);
                    int order2 = defaultIfNull(key2.getOrder(), Integer.MAX_VALUE);
                    return Integer.compare(order1, order2);
                }).forEach(e -> rv.specifications.add(rv.new ProcessSpecification(e)));
        return rv;
    }

    private static void processActionToInclude(
            String actionToInclude, Map<String, ApprovalActionWithRule> actionsMap,
            Map.Entry<WfProcessSpecificationType, List<ApprovalActionWithRule>> processSpecificationEntry,
            boolean mustBePresent) throws ConfigurationException {
        ApprovalActionWithRule actionWithRule = actionsMap.get(actionToInclude);
        if (actionWithRule != null) {
            processSpecificationEntry.getValue().add(actionWithRule);
        } else if (mustBePresent) {
            throw new ConfigurationException("Approval action '" + actionToInclude + "' cannot be found");
        }
    }

    private static Map<String, ApprovalActionWithRule> createActionsMap(
            Collection<List<ApprovalActionWithRule>> allActionsWithRules) {
        Map<String, ApprovalActionWithRule> rv = new HashMap<>();
        for (List<ApprovalActionWithRule> actionsWithRules : allActionsWithRules) {
            for (ApprovalActionWithRule actionWithRule : actionsWithRules) {
                String actionName = actionWithRule.approvalAction.getName();
                if (actionName != null) {
                    rv.put(actionName, actionWithRule);
                }
            }
        }
        return rv;
    }

    Collection<ProcessSpecification> getSpecifications() {
        return specifications;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpWithLabel(sb, "Process specifications", specifications, indent);
        return sb.toString();
    }

    static class ApprovalActionWithRule {
        @NotNull final ApprovalPolicyActionType approvalAction;
        @NotNull final AssociatedPolicyRule policyRule;

        ApprovalActionWithRule(@NotNull ApprovalPolicyActionType approvalAction, @NotNull AssociatedPolicyRule policyRule) {
            this.approvalAction = approvalAction;
            this.policyRule = policyRule;
        }
    }
}
