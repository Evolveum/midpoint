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

import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EvaluatedSituationTriggerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author mederly
 */
public class EvaluatedCompositeTrigger extends EvaluatedPolicyRuleTrigger<PolicyConstraintsType> {

	@NotNull private final Collection<EvaluatedPolicyRuleTrigger<?>> innerTriggers;

	public EvaluatedCompositeTrigger(@NotNull PolicyConstraintKindType kind, @NotNull PolicyConstraintsType constraint,
			LocalizableMessage message, @NotNull Collection<EvaluatedPolicyRuleTrigger<?>> innerTriggers) {
		super(kind, constraint, message);
		this.innerTriggers = innerTriggers;
	}

	@NotNull
	public Collection<EvaluatedPolicyRuleTrigger<?>> getInnerTriggers() {
		return innerTriggers;
	}

	@Override
	public String toDiagShortcut() {
		return super.toDiagShortcut()
			+ innerTriggers.stream()
					.map(trigger -> trigger.toDiagShortcut())
					.distinct()
					.collect(Collectors.joining("+", "(", ")"));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof EvaluatedCompositeTrigger))
			return false;
		if (!super.equals(o))
			return false;
		EvaluatedCompositeTrigger that = (EvaluatedCompositeTrigger) o;
		return Objects.equals(innerTriggers, that.innerTriggers);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), innerTriggers);
	}

	@Override
	protected void debugDumpSpecific(StringBuilder sb, int indent) {
		DebugUtil.debugDumpWithLabel(sb, "innerTriggers", innerTriggers, indent + 1);
	}

	@Override
	public EvaluatedSituationTriggerType toEvaluatedPolicyRuleTriggerType(EvaluatedPolicyRule owningRule) {
		EvaluatedSituationTriggerType rv = new EvaluatedSituationTriggerType();
		fillCommonContent(rv, owningRule);
		//innerTriggers.forEach(r -> rv.getSourceRule().add(r.toEvaluatedPolicyRuleType()));
		//TODO
		return rv;
	}
}
