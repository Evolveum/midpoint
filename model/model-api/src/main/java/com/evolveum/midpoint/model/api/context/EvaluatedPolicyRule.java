/**
 * Copyright (c) 2016-2017 Evolveum
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

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.TreeNode;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author semancik
 *
 */
public interface EvaluatedPolicyRule extends DebugDumpable, Serializable {

	@NotNull
	Collection<EvaluatedPolicyRuleTrigger<?>> getTriggers();

	default boolean isTriggered() {
		return !getTriggers().isEmpty();
	}

	/**
	 * Returns all triggers, even those that were indirectly collected via situation policy rules.
	 */
	@NotNull
	Collection<EvaluatedPolicyRuleTrigger<?>> getAllTriggers();

	String getName();

	PolicyRuleType getPolicyRule();

	PolicyConstraintsType getPolicyConstraints();

	PolicyActionsType getActions();

	AssignmentPath getAssignmentPath();

	/**
	 * Object that "directly owns" the rule. TODO. [consider if really needed]
	 */
	@Nullable
	ObjectType getDirectOwner();

	// TODO consider removing
	String getPolicySituation();

	Collection<PolicyExceptionType> getPolicyExceptions();

	void addToEvaluatedPolicyRuleTypes(Collection<EvaluatedPolicyRuleType> rules, PolicyRuleExternalizationOptions options);

	boolean isGlobal();

	String toShortString();

	List<TreeNode<LocalizableMessage>> extractMessages();
}
