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

package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.schema.util.PolicyRuleTypeUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EvaluatedSituationTriggerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicySituationPolicyConstraintType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author mederly
 */
public class EvaluatedSituationTrigger extends EvaluatedPolicyRuleTrigger<PolicySituationPolicyConstraintType> {

	@NotNull private final Collection<EvaluatedPolicyRule> sourceRules;

	public EvaluatedSituationTrigger(@NotNull PolicySituationPolicyConstraintType constraint,
			LocalizableMessage message, LocalizableMessage shortMessage, @NotNull Collection<EvaluatedPolicyRule> sourceRules) {
		super(PolicyConstraintKindType.SITUATION, constraint, message, shortMessage);
		this.sourceRules = sourceRules;
	}

	@NotNull
	public Collection<EvaluatedPolicyRule> getSourceRules() {
		return sourceRules;
	}

	// lists all source rules (recursively)
	@NotNull
	public Collection<EvaluatedPolicyRule> getAllSourceRules() {
		List<EvaluatedPolicyRule> rv = new ArrayList<>();
		for (EvaluatedPolicyRule sourceRule : sourceRules) {
			rv.add(sourceRule);
			for (EvaluatedPolicyRuleTrigger trigger : sourceRule.getTriggers()) {
				if (trigger instanceof EvaluatedSituationTrigger) {
					rv.addAll(((EvaluatedSituationTrigger) trigger).getAllSourceRules());
				}
			}
		}
		return rv;
	}

	public Collection<EvaluatedPolicyRuleTrigger<?>> getAllTriggers() {
		List<EvaluatedPolicyRuleTrigger<?>> rv = new ArrayList<>();
		rv.add(this);
		getAllSourceRules().forEach(r -> rv.addAll(r.getTriggers()));
		return rv;
	}

	@Override
	public String toDiagShortcut() {
		return super.toDiagShortcut()
			+ sourceRules.stream()
					.map(sr -> PolicyRuleTypeUtil.toShortString(sr.getPolicyConstraints()))
					.distinct()
					.collect(Collectors.joining("+", "(", ")"));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof EvaluatedSituationTrigger))
			return false;
		if (!super.equals(o))
			return false;
		EvaluatedSituationTrigger that = (EvaluatedSituationTrigger) o;
		return Objects.equals(sourceRules, that.sourceRules);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), sourceRules);
	}

	@Override
	protected void debugDumpSpecific(StringBuilder sb, int indent) {
		// cannot debug dump in details, as we might go into infinite loop
		DebugUtil.debugDumpWithLabel(sb, "sourceRules", sourceRules.stream().map(Object::toString).collect(Collectors.toList()), indent + 1);
	}

	@Override
	public EvaluatedSituationTriggerType toEvaluatedPolicyRuleTriggerType(PolicyRuleExternalizationOptions options) {
		EvaluatedSituationTriggerType rv = new EvaluatedSituationTriggerType();
		fillCommonContent(rv);
		if (!options.isRespectFinalFlag() || !isFinal()) {
			sourceRules.forEach(r -> r.addToEvaluatedPolicyRuleTypes(rv.getSourceRule(), options));
		}
		return rv;
	}
}
