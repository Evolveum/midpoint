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

package com.evolveum.midpoint.wf.impl.processes.itemApproval;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.util.LocalizationUtil;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TODO
 *
 * Generator of default 'additional information' section for specific constraints (exclusion, assignment, ...).
 *
 * @author mederly
 */
class AdditionalInformationGenerator {

	@NotNull
	List<InformationType> getDefaultAdditionalInformation(Task wfTask, int order) {
		// later here may be more rules (stage amalgamation)
		List<SchemaAttachedPolicyRuleType> attachedRules = WfContextUtil.getAttachedPolicyRules(wfTask.getWorkflowContext(), order);
		List<EvaluatedPolicyRuleTriggerType> triggers = new ArrayList<>();
		attachedRules.forEach(rule -> collectPrimitiveTriggers(triggers, rule.getRule()));

		List<InformationType> rv = new ArrayList<>();
		generateAssignmentModificationMessages(rv, extractTriggers(triggers, PolicyConstraintKindType.ASSIGNMENT_MODIFICATION));
		generateObjectModificationMessages(rv, extractTriggers(triggers, PolicyConstraintKindType.OBJECT_MODIFICATION));
		generateExclusionMessages(rv, extractTriggers(triggers, PolicyConstraintKindType.EXCLUSION));
		generateOtherMessages(rv, triggers);
		return rv;
	}

	private void collectPrimitiveTriggers(List<EvaluatedPolicyRuleTriggerType> triggers, EvaluatedPolicyRuleType attachedRule) {
		for (EvaluatedPolicyRuleTriggerType trigger : attachedRule.getTrigger()) {
			if (trigger instanceof EvaluatedSituationTriggerType) {
				((EvaluatedSituationTriggerType) trigger).getSourceRule().forEach(r -> collectPrimitiveTriggers(triggers, r));
			} else {
				triggers.add(trigger);
			}
		}
	}

	private List<EvaluatedPolicyRuleTriggerType> extractTriggers(List<EvaluatedPolicyRuleTriggerType> triggers,
			PolicyConstraintKindType kind) {
		List<EvaluatedPolicyRuleTriggerType> rv = new ArrayList<>();
		for (Iterator<EvaluatedPolicyRuleTriggerType> iterator = triggers.iterator(); iterator.hasNext(); ) {
			EvaluatedPolicyRuleTriggerType trigger = iterator.next();
			if (trigger.getConstraintKind() == kind) {
				rv.add(trigger);
				iterator.remove();
			}
		}
		return rv;
	}

	@SuppressWarnings("unused")
	private void generateAssignmentModificationMessages(List<InformationType> infoList, List<EvaluatedPolicyRuleTriggerType> triggers) {
		// Nothing to do here. The information about assignments to be added/removed is obvious from the delta.
	}

	@SuppressWarnings("unused")
	private void generateObjectModificationMessages(List<InformationType> infoList, List<EvaluatedPolicyRuleTriggerType> triggers) {
		// Nothing to do here. The information about assignments to be added/removed is obvious from the delta.
	}

	private void generateExclusionMessages(List<InformationType> infoList, List<EvaluatedPolicyRuleTriggerType> triggers) {
		if (triggers.isEmpty()) {
			return;
		}
		InformationType info = new InformationType();
		info.setTitle(LocalizationUtil.createForKey("AdditionalInformationGenerator.exclusionsTitle"));
		for (EvaluatedPolicyRuleTriggerType trigger : triggers) {
			EvaluatedExclusionTriggerType exclusion = (EvaluatedExclusionTriggerType) trigger;
			InformationPartType part = new InformationPartType();
			StringBuilder sb = new StringBuilder();
			sb.append(LocalizationUtil.resolve("AdditionalInformationGenerator.assignmentOf",
					getObjectTypeAndName(exclusion.getDirectOwnerRef(), exclusion.getDirectOwnerDisplayName())));
			String thisPathInfo = getObjectPathIfRelevant(exclusion.getAssignmentPath());
			if (thisPathInfo != null) {
				sb.append(" (").append(thisPathInfo).append(")");
			}
			sb.append(" ").append(LocalizationUtil.resolve("AdditionalInformationGenerator.isInConflictWithAssignmentOf",
					getObjectTypeAndName(exclusion.getConflictingObjectRef(), exclusion.getConflictingObjectDisplayName())));
			String conflictingPathInfo = getObjectPathIfRelevant(exclusion.getConflictingObjectPath());
			if (conflictingPathInfo != null) {
				sb.append(" (").append(conflictingPathInfo).append(")");
			}
			sb.append(".");
			part.setText(LocalizationUtil.createForFallbackMessage(sb.toString()));   // TODO localizable
			info.getPart().add(part);
		}
		infoList.add(info);
	}

	// We simply show all messages from triggers.
	private void generateOtherMessages(List<InformationType> infoList, List<EvaluatedPolicyRuleTriggerType> triggers) {
		if (triggers.isEmpty()) {
			return;
		}
		InformationType info = new InformationType();
		info.setTitle(LocalizationUtil.createForKey("AdditionalInformationGenerator.notes"));
		for (EvaluatedPolicyRuleTriggerType trigger : triggers) {
			InformationPartType part = new InformationPartType();
			part.setText(trigger.getMessage());
			info.getPart().add(part);
		}
		infoList.add(info);
	}

//	private Stream<String> getRuleMessages(EvaluatedPolicyRuleType rule) {
//		if (rule == null) {
//			return null;
//		}
//		return rule.getTrigger().stream()
//				.flatMap(this::getTriggerMessages);
//	}
//
//	private Stream<String> getTriggerMessages(EvaluatedPolicyRuleTriggerType t) {
//		if (t instanceof EvaluatedSituationTriggerType) {
//			return ((EvaluatedSituationTriggerType) t).getSourceRule().stream()
//					.flatMap(this::getRuleMessages);
//		} else {
//			return t.getMessage() != null ? Stream.of(t.getMessage()) : Stream.empty();
//		}
//	}

	private String getObjectTypeAndName(ObjectReferenceType ref, PolyStringType displayNamePoly) {
		String name = PolyString.getOrig(ref.getTargetName());
		String displayName = PolyString.getOrig(displayNamePoly);
		String typeName = ObjectTypes.getDisplayNameForTypeName(ref.getType(), null);

		StringBuilder sb = new StringBuilder();
		if (typeName != null) {
			sb.append(typeName.toLowerCase()).append(" ");
		}
		if (name != null && displayName != null) {
			sb.append(displayName).append(" (").append(name).append(")");
		} else if (name != null) {
			sb.append(name);
		} else if (displayName != null) {
			sb.append(displayName);
		} else {
			sb.append(ref.getOid());
		}
		return sb.toString();
	}

	private String getObjectPathIfRelevant(AssignmentPathType path) {
		if (path == null) {
			return null;
		}
		List<AssignmentPathSegmentType> matchingOrder = path.getSegment().stream()
				.filter(seg -> seg.isMatchingOrder()).collect(Collectors.toList());
		if (matchingOrder.size() < 2) {
			return null;
		}
		return matchingOrder.stream()
				.map(seg -> getDisplayableName(seg.getTargetRef(), seg.getTargetDisplayName()))
				.collect(Collectors.joining(" -> "));
	}

	private String getDisplayableName(ObjectReferenceType ref, PolyStringType displayName) {
		if (displayName != null) {
			return displayName.getOrig();
		} else if (ref == null) {
			return null;
		} else if (ref.getTargetName() != null) {
			return ref.getTargetName().getOrig();
		} else {
			return ref.getOid();
		}
	}

}
