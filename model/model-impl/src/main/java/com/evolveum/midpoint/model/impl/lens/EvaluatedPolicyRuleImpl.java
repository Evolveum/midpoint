/**
 * Copyright (c) 2016 Evolveum
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
package com.evolveum.midpoint.model.impl.lens;

import java.util.Collection;

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.PolicyConstraintKind;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyActionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;

/**
 * @author semancik
 *
 */
public class EvaluatedPolicyRuleImpl implements EvaluatedPolicyRule {
	private static final long serialVersionUID = 1L;

	private PolicyRuleType policyRuleType;

	public EvaluatedPolicyRuleImpl(PolicyRuleType policyRuleType) {
		super();
		this.policyRuleType = policyRuleType;
	}

	@Override
	public String getName() {
		if (policyRuleType == null) {
			return null;
		}
		return policyRuleType.getName();
	}
	
	@Override
	public PolicyConstraintsType getPolicyConstraints() {
		return policyRuleType.getPolicyConstraints();
	}

	@Override
	public Collection<PolicyConstraintKind> getTriggeredConstraintKinds() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PolicyActionsType getActions() {
		return policyRuleType.getPolicyActions();
	}
	
	@Override
	public String getPolicySituation() {
		// TODO default situations depending on getTriggeredConstraintKinds
		return policyRuleType.getPolicySituation();
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.debugDumpLabelLn(sb, "EvaluatedPolicyRule", indent);
		DebugUtil.debugDumpWithLabelLn(sb, "name", getName(), indent + 1);
		DebugUtil.debugDumpWithLabel(sb, "policyRuleType", policyRuleType.toString(), indent + 1);
		return sb.toString();
	}

}
