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

package com.evolveum.midpoint.web.page.admin.workflow.dto;

import com.evolveum.midpoint.model.api.util.EvaluatedPolicyRuleUtil;
import com.evolveum.midpoint.model.api.util.EvaluatedPolicyRuleUtil.AugmentedTrigger;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.TreeNode;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EvaluatedPolicyRuleTriggerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EvaluatedPolicyRuleType;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.evolveum.midpoint.model.api.util.EvaluatedPolicyRuleUtil.arrangeForPresentationExt;
import static java.util.Collections.singleton;

/**
 * @author mederly
 */
public class EvaluatedTriggerGroupDto implements Serializable {

	public final LocalizableMessage title;

	public static final String F_TITLE = "title";
	public static final String F_TRIGGERS = "triggers";

	@NotNull private final List<EvaluatedTriggerDto> triggers = new ArrayList<>();

	EvaluatedTriggerGroupDto(LocalizableMessage title,
			List<TreeNode<AugmentedTrigger<HighlightingInformation>>> processedTriggers) {
		this.title = title;
		for (TreeNode<AugmentedTrigger<HighlightingInformation>> processedTrigger : processedTriggers) {
			this.triggers.add(new EvaluatedTriggerDto(processedTrigger));
		}
	}

	public LocalizableMessage getTitle() {
		return title;
	}

	@NotNull
	public List<EvaluatedTriggerDto> getTriggers() {
		return triggers;
	}

	public static EvaluatedTriggerGroupDto initializeFromRules(List<EvaluatedPolicyRuleType> rules, boolean highlighted,
			UniquenessFilter uniquenessFilter) {
		List<AugmentedTrigger<HighlightingInformation>> augmentedTriggers = new ArrayList<>();
		for (EvaluatedPolicyRuleType rule : rules) {
			for (EvaluatedPolicyRuleTriggerType trigger : rule.getTrigger()) {
				augmentedTriggers.add(new AugmentedTrigger<>(trigger, new HighlightingInformation(highlighted)));
			}
		}
		List<TreeNode<AugmentedTrigger<HighlightingInformation>>> triggerTrees = arrangeForPresentationExt(augmentedTriggers, uniquenessFilter);
		return new EvaluatedTriggerGroupDto(null, triggerTrees);
	}

	public static class UniquenessFilter implements EvaluatedPolicyRuleUtil.AdditionalFilter<HighlightingInformation> {

		private static class AlreadyShownTriggerRecord<AD extends EvaluatedPolicyRuleUtil.AdditionalData> {
			final AugmentedTrigger<AD> augmentedTrigger;
			final EvaluatedPolicyRuleTriggerType anonymizedTrigger;

			AlreadyShownTriggerRecord(AugmentedTrigger<AD> augmentedTrigger,
					EvaluatedPolicyRuleTriggerType anonymizedTrigger) {
				this.augmentedTrigger = augmentedTrigger;
				this.anonymizedTrigger = anonymizedTrigger;
			}
		}

		List<AlreadyShownTriggerRecord<HighlightingInformation>> triggersAlreadyShown = new ArrayList<>();

		@Override
		public boolean accepts(AugmentedTrigger<HighlightingInformation> newAugmentedTrigger) {
			EvaluatedPolicyRuleTriggerType anonymizedTrigger = newAugmentedTrigger.trigger.clone().ruleName(null);
			for (AlreadyShownTriggerRecord<HighlightingInformation> alreadyShown : triggersAlreadyShown) {
				if (alreadyShown.anonymizedTrigger.equals(anonymizedTrigger)) {
					alreadyShown.augmentedTrigger.additionalData.merge(newAugmentedTrigger.additionalData);
					return false;
				}
			}
			triggersAlreadyShown.add(new AlreadyShownTriggerRecord<>(newAugmentedTrigger, anonymizedTrigger));
			return true;
		}
	}

	public static class HighlightingInformation implements EvaluatedPolicyRuleUtil.AdditionalData {
		boolean value;

		HighlightingInformation(boolean value) {
			this.value = value;
		}

		public void merge(EvaluatedPolicyRuleUtil.AdditionalData other) {
			value = value | ((HighlightingInformation) other).value;
		}
	}

	public static boolean isEmpty(Collection<EvaluatedTriggerGroupDto> groups) {
		// original implementation (after migration from 3.6 to 3.7 is over)
//		return groups.stream()
//				.allMatch(g -> g.getTriggers().isEmpty());

		// temporary implementation for 3.7 - assuming we can display a trigger only if 'message' is not null and/or it has some children
		return groups.stream()
				.allMatch(g -> triggersAreEmpty(g.getTriggers()));

	}

	private static boolean triggersAreEmpty(Collection<EvaluatedTriggerDto> triggers) {
		return triggers.stream()
				.allMatch(t -> triggerIsEmpty(t));
	}

	private static boolean triggerIsEmpty(EvaluatedTriggerDto trigger) {
		return trigger.getMessage() == null && isEmpty(singleton(trigger.getChildren()));
	}
}
