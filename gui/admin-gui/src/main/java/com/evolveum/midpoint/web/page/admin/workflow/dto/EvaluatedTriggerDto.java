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
import com.evolveum.midpoint.util.TreeNode;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * @author mederly
 */
public class EvaluatedTriggerDto implements Serializable {

	public static final String F_MESSAGE = "message";
	public static final String F_CHILDREN = "children";

	@NotNull private final EvaluatedPolicyRuleTriggerType trigger;
	@NotNull private final EvaluatedTriggerGroupDto children;
	private boolean highlighted;

	public EvaluatedTriggerDto(TreeNode<EvaluatedPolicyRuleUtil.AugmentedTrigger<EvaluatedTriggerGroupDto.HighlightingInformation>> node) {
		this.trigger = node.getUserObject().trigger;
		this.children = new EvaluatedTriggerGroupDto(null, node.getChildren());
		this.highlighted = node.getUserObject().additionalData.value;
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

	public boolean isHighlighted() {
		return highlighted;
	}
}
