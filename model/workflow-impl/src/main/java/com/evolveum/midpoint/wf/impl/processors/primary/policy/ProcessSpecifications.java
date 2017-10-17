/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.wf.impl.processors.primary.policy;

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.util.PrismPrettyPrinter;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalPolicyActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyActionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfProcessSpecificationType;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * Maintains "process specifications" i.e. recipes how to
 *  - analyze incoming deltas (WfProcessSpecificationType.deltaFrom),
 *  - create approval processes
 *  - fill-in their approval schema (list of approval actions with policy rules)
 *
 * @author mederly
 */
class ProcessSpecifications implements DebugDumpable {

	private final List<ProcessSpecification> specifications = new ArrayList<>();
	private final PrismContext prismContext;

	private ProcessSpecifications(PrismContext prismContext) {
		// use createFromRules instead
		this.prismContext = prismContext;
	}

	class ProcessSpecification implements DebugDumpable {
		final WfProcessSpecificationType basicSpec;
		final List<Pair<ApprovalPolicyActionType, EvaluatedPolicyRule>> actionsWithRules;

		ProcessSpecification(Map.Entry<WfProcessSpecificationType, List<Pair<ApprovalPolicyActionType, EvaluatedPolicyRule>>> entry) {
			this.basicSpec = entry.getKey();
			this.actionsWithRules = entry.getValue();
		}

		@Override
		public String debugDump(int indent) {
			StringBuilder sb = new StringBuilder();
			DebugUtil.debugDumpLabelLn(sb, "process specification", indent);
			PrismPrettyPrinter.debugDumpValue(sb, indent+1, basicSpec, prismContext, ApprovalPolicyActionType.F_PROCESS_SPECIFICATION, PrismContext.LANG_YAML);
			sb.append("\n");
			DebugUtil.debugDumpLabelLn(sb, "actions with rules", indent);
			for (Pair<ApprovalPolicyActionType, EvaluatedPolicyRule> actionWithRule : actionsWithRules) {
				DebugUtil.debugDumpLabelLn(sb, "action", indent+1);
				PrismPrettyPrinter.debugDumpValue(sb, indent+2, actionWithRule.getKey(), prismContext, PolicyActionsType.F_APPROVAL, PrismContext.LANG_YAML);
				sb.append("\n");
				if (actionWithRule.getValue() != null) {
					sb.append("\n").append(actionWithRule.getValue().debugDump(indent + 2));
				}
			}
			return sb.toString();
		}
	}

	static ProcessSpecifications createFromRules(List<EvaluatedPolicyRule> rules, PrismContext prismContext) {
		// Step 1: plain list of approval actions -> map: process-spec -> list of related actions/rules ("collected")
		LinkedHashMap<WfProcessSpecificationType, List<Pair<ApprovalPolicyActionType, EvaluatedPolicyRule>>> collected = new LinkedHashMap<>();
		for (EvaluatedPolicyRule rule : rules) {
			for (ApprovalPolicyActionType approvalAction : rule.getEnabledActions(ApprovalPolicyActionType.class)) {
				WfProcessSpecificationType spec = approvalAction.getProcessSpecification();
				collected.computeIfAbsent(spec, s -> new ArrayList<>()).add(new ImmutablePair<>(approvalAction, rule));
			}
		}
		// Step 2: resolve references
		for (WfProcessSpecificationType spec : new HashSet<>(collected.keySet())) {     // cloned to avoid concurrent modification exception
			if (spec != null && spec.getRef() != null) {
				List<Map.Entry<WfProcessSpecificationType, List<Pair<ApprovalPolicyActionType, EvaluatedPolicyRule>>>> matching = collected.entrySet().stream()
						.filter(e -> e.getKey() != null && spec.getRef().equals(e.getKey().getName()))
						.collect(Collectors.toList());
				if (matching.isEmpty()) {
					throw new IllegalStateException("Process specification named '"+spec.getRef()+"' referenced from an approval action couldn't be found");
				} else if (matching.size() > 1) {
					throw new IllegalStateException("More than one process specification named '"+spec.getRef()+"' referenced from an approval action: " + matching);
				} else {
					// move all actions/rules to the referenced process specification
					List<Pair<ApprovalPolicyActionType, EvaluatedPolicyRule>> referencedSpecActions = matching.get(0).getValue();
					referencedSpecActions.addAll(collected.get(spec));
					collected.remove(spec);
				}
			}
		}

		// Step 3: include other actions
		for (WfProcessSpecificationType spec : collected.keySet()) {
			if (spec == null) {
				continue;
			}
			for (String actionToInclude : spec.getIncludeAction()) {
				// TODO
			}
			
		}

		// todo distribute non-process-attached actions to process-attached ones

		// Step 4: sorts process specifications and wraps into ProcessSpecification objects
		ProcessSpecifications rv = new ProcessSpecifications(prismContext);
		collected.entrySet().stream()
				.sorted((ps1, ps2) -> {
					WfProcessSpecificationType key1 = ps1.getKey();
					WfProcessSpecificationType key2 = ps2.getKey();
					if (key1 == null) {
						return key2 == null ? 0 : 1;       // non-empty (key2) records first
					} else if (key2 == null) {
						return -1;                          // non-empty (key1) record first
					}
					int order1 = defaultIfNull(key1.getOrder(), Integer.MAX_VALUE);
					int order2 = defaultIfNull(key2.getOrder(), Integer.MAX_VALUE);
					return Integer.compare(order1, order2);
				}).forEach(e -> rv.specifications.add(rv.new ProcessSpecification(e)));
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
}
