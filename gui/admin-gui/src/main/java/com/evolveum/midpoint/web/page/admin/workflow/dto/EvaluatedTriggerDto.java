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

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author mederly
 */
public class EvaluatedTriggerDto implements Serializable {

	public static final String F_MESSAGE = "message";
	public static final String F_CHILDREN = "children";

	@NotNull private final EvaluatedPolicyRuleTriggerType trigger;
	@NotNull private final EvaluatedTriggerGroupDto children;
	private boolean highlighted;

	private EvaluatedTriggerDto(@NotNull EvaluatedPolicyRuleTriggerType trigger, boolean highlighted) {
		this.trigger = trigger;
		this.children = new EvaluatedTriggerGroupDto(null);
		this.highlighted = highlighted;
	}

	@NotNull
	public EvaluatedPolicyRuleTriggerType getTrigger() {
		return trigger;
	}

	public LocalizableMessageType getMessage() {
		return trigger.getMessage();
	}

	@NotNull
	public EvaluatedTriggerGroupDto getChildren() {
		return children;
	}

	// copies logic from EvaluatedPolicyRuleImpl.createMessageTreeNode
	public static void create(List<EvaluatedTriggerDto> resultList, EvaluatedPolicyRuleTriggerType trigger, boolean highlighted,
			List<AlreadyShownTriggerRecord> triggersAlreadyShown) {
		boolean hidden = Boolean.TRUE.equals(trigger.isHidden());
		boolean isFinal = Boolean.TRUE.equals(trigger.isFinal());
		if (!hidden) {
			EvaluatedTriggerDto newTriggerDto = new EvaluatedTriggerDto(trigger, highlighted);
			if (triggersAlreadyShown != null && alreadyShown(triggersAlreadyShown, newTriggerDto)) {
				return;
			}
			resultList.add(newTriggerDto);
			resultList = newTriggerDto.getChildren().getTriggers();
		}
		if (!isFinal) { // it's possible that this was pre-filtered e.g. by policy enforcer hook
			for (EvaluatedPolicyRuleTriggerType innerTrigger : getChildTriggers(trigger)) {
				create(resultList, innerTrigger, highlighted, triggersAlreadyShown);
			}
		}
	}

	public static void sort(List<EvaluatedTriggerDto> triggers) {
		// TODO
	}

	private static boolean alreadyShown(List<AlreadyShownTriggerRecord> triggersAlreadyShown, EvaluatedTriggerDto newTriggerDto) {
		EvaluatedPolicyRuleTriggerType anonymizedTrigger = newTriggerDto.trigger.clone().ruleName(null);
		for (AlreadyShownTriggerRecord alreadyShown : triggersAlreadyShown) {
			if (alreadyShown.anonymizedTrigger.equals(anonymizedTrigger)) {
				if (newTriggerDto.highlighted) {
					alreadyShown.originalTriggerDto.highlighted = true;
				}
				return true;
			}
		}
		triggersAlreadyShown.add(new AlreadyShownTriggerRecord(newTriggerDto, anonymizedTrigger));
		return false;
	}

	private static List<EvaluatedPolicyRuleTriggerType> getChildTriggers(EvaluatedPolicyRuleTriggerType trigger) {
		if (trigger instanceof EvaluatedEmbeddingTriggerType) {
			return ((EvaluatedEmbeddingTriggerType) trigger).getEmbedded();
		} else if (trigger instanceof EvaluatedSituationTriggerType) {
			List<EvaluatedPolicyRuleTriggerType> rv = new ArrayList<>();
			for (EvaluatedPolicyRuleType rule : ((EvaluatedSituationTriggerType) trigger).getSourceRule()) {
				rv.addAll(rule.getTrigger());
			}
			return rv;
		} else {
			return Collections.emptyList();
		}
	}

	public boolean isHighlighted() {
		return highlighted;
	}
}
