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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalPolicyActionType;
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
class ProcessSpecifications {

	private final List<ProcessSpecification> specifications = new ArrayList<>();

	private ProcessSpecifications() {
		// use createFromRules instead
	}

	static class ProcessSpecification {
		final WfProcessSpecificationType basicSpec;
		final List<Pair<ApprovalPolicyActionType, EvaluatedPolicyRule>> actionsWithRules;

		ProcessSpecification(Map.Entry<WfProcessSpecificationType, List<Pair<ApprovalPolicyActionType, EvaluatedPolicyRule>>> entry) {
			this.basicSpec = entry.getKey();
			this.actionsWithRules = entry.getValue();
		}
	}

	static ProcessSpecifications createFromRules(List<EvaluatedPolicyRule> rules) {
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
			if (spec.getRef() != null) {
				List<Map.Entry<WfProcessSpecificationType, List<Pair<ApprovalPolicyActionType, EvaluatedPolicyRule>>>> matching = collected.entrySet().stream()
						.filter(e -> spec.getRef().equals(e.getKey().getName()))
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
			for (String actionToInclude : spec.getIncludeAction()) {
				// TODO
			}
			
		}

		// todo distribute non-process-attached actions to process-attached ones

		// Step 4: sorts process specifications and wraps into ProcessSpecification objects
		ProcessSpecifications rv = new ProcessSpecifications();
		collected.entrySet().stream()
				.sorted((ps1, ps2) -> {
					int order1 = defaultIfNull(ps1.getKey() != null ? ps1.getKey().getOrder() : null, Integer.MAX_VALUE);
					int order2 = defaultIfNull(ps2.getKey() != null ? ps2.getKey().getOrder() : null, Integer.MAX_VALUE);
					return Integer.compare(order1, order2);
				}).forEach(e -> rv.specifications.add(new ProcessSpecification(e)));
		return rv;
	}

	Collection<ProcessSpecification> getSpecifications() {
		return specifications;
	}

}
