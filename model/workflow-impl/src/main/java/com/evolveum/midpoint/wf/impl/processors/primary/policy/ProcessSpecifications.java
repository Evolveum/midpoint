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

/**
 * Maintains "process specifications" i.e. recipes how to
 *  - analyze incoming deltas (WfProcessSpecificationType.deltaFrom),
 *  - create approval processes
 *  - fill-in their approval schema (list of approval actions with policy rules)
 *
 * @author mederly
 */
public class ProcessSpecifications {

	private final LinkedHashMap<WfProcessSpecificationType, List<Pair<ApprovalPolicyActionType, EvaluatedPolicyRule>>> specifications = new LinkedHashMap<>();

	static class ProcessSpecification {
		final WfProcessSpecificationType basicSpec;
		final List<Pair<ApprovalPolicyActionType, EvaluatedPolicyRule>> actionsWithRules;

		public ProcessSpecification(Map.Entry<WfProcessSpecificationType, List<Pair<ApprovalPolicyActionType, EvaluatedPolicyRule>>> entry) {
			this.basicSpec = entry.getKey();
			this.actionsWithRules = entry.getValue();
		}
	}

	static ProcessSpecifications createFromRules(List<EvaluatedPolicyRule> rules) {
		LinkedHashMap<WfProcessSpecificationType, List<Pair<ApprovalPolicyActionType, EvaluatedPolicyRule>>> collected = new LinkedHashMap<>();
		for (EvaluatedPolicyRule rule : rules) {
			for (ApprovalPolicyActionType approvalAction : rule.getActions().getApproval()) {
				WfProcessSpecificationType spec = approvalAction.getProcessSpecification();
				collected.computeIfAbsent(spec, s -> new ArrayList<>()).add(new ImmutablePair<>(approvalAction, rule));
			}
		}
		// todo resolve references
		// todo distribute non-process-attached actions to process-attached ones
		ProcessSpecifications rv = new ProcessSpecifications();
		collected.entrySet().stream()
				.sorted((ps1, ps2) -> {
					Integer order1 = ps1.getKey() != null ? ps1.getKey().getOrder() : null;
					Integer order2 = ps2.getKey() != null ? ps2.getKey().getOrder() : null;
					if (order1 != null || order2 != null) {
						return Comparator.nullsLast(Comparator.<Integer>naturalOrder()).compare(order1, order2);
					} else if (ps1.getKey() != null && ps2.getKey() == null) {
						return -1;
					} else if (ps1.getKey() == null && ps2.getKey() != null) {
						return 1;
					} else {
						return 0;
					}
				}).forEach(e -> rv.specifications.put(e.getKey(), e.getValue()));
		return rv;
	}

	Collection<ProcessSpecification> getSpecifications() {
		return specifications.entrySet().stream()
				.map(ProcessSpecification::new)
				.collect(Collectors.toList());
	}

}
