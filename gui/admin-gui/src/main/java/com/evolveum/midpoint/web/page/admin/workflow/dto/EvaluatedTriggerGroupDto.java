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

import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EvaluatedPolicyRuleTriggerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EvaluatedPolicyRuleType;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mederly
 */
public class EvaluatedTriggerGroupDto implements Serializable {

	public final LocalizableMessage title;

	public static final String F_TITLE = "title";
	public static final String F_TRIGGERS = "triggers";

	@NotNull private final List<EvaluatedTriggerDto> triggers = new ArrayList<>();

	public EvaluatedTriggerGroupDto(LocalizableMessage title) {
		this.title = title;
	}

	public LocalizableMessage getTitle() {
		return title;
	}

	@NotNull
	public List<EvaluatedTriggerDto> getTriggers() {
		return triggers;
	}

	public static EvaluatedTriggerGroupDto createFrom(List<EvaluatedPolicyRuleType> rules, boolean highlighted, List<AlreadyShownTriggerRecord> triggersAlreadyShown) {
		EvaluatedTriggerGroupDto group = new EvaluatedTriggerGroupDto(null);
		for (EvaluatedPolicyRuleType rule : rules) {
			for (EvaluatedPolicyRuleTriggerType trigger : rule.getTrigger()) {
				EvaluatedTriggerDto.create(group.getTriggers(), trigger, highlighted, triggersAlreadyShown);
			}
		}
		return group;
	}
}
