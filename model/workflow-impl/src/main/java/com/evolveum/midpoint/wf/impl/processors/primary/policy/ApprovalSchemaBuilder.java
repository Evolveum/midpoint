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
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ApprovalSchema;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ApprovalSchemaImpl;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ReferenceResolver;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.RelationResolver;
import com.evolveum.midpoint.wf.impl.processors.primary.ModelInvocationContext;
import com.evolveum.midpoint.wf.impl.processors.primary.aspect.BasePrimaryChangeAspect;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.*;
import java.util.stream.Stream;

import static java.util.Comparator.naturalOrder;

/**
 * @author mederly
 */
class ApprovalSchemaBuilder {

	class Result {
		@NotNull final ApprovalSchema schema;
		@NotNull final ApprovalSchemaType schemaType;
		@NotNull final SchemaAttachedPolicyRulesType attachedRules;

		public Result(@NotNull ApprovalSchemaType schemaType, @NotNull ApprovalSchema schema,
				@NotNull SchemaAttachedPolicyRulesType attachedRules) {
			this.schema = schema;
			this.schemaType = schemaType;
			this.attachedRules = attachedRules;
		}
	}

	private class Fragment {
		final PrismObject<?> target;					// object to which relations (approved, owner) are resolved
		@NotNull final ApprovalSchemaType schema;
		final EvaluatedPolicyRule policyRule;
		Integer order;

		private Fragment(PrismObject<?> target, @NotNull ApprovalSchemaType schema, EvaluatedPolicyRule policyRule) {
			this.target = target;
			this.schema = schema;
			this.policyRule = policyRule;
		}
	}

	private final List<Fragment> predefinedFragments = new ArrayList<>();
	private final List<Fragment> standardFragments = new ArrayList<>();

	@NotNull private final BasePrimaryChangeAspect primaryChangeAspect;

	ApprovalSchemaBuilder(@NotNull BasePrimaryChangeAspect primaryChangeAspect) {
		this.primaryChangeAspect = primaryChangeAspect;
	}

	// TODO target
	void add(ApprovalSchemaType schema, ApprovalCompositionStrategyType compositionStrategy, PrismObject<?> defaultTarget,
			EvaluatedPolicyRule policyRule) {
		Fragment fragment = new Fragment(defaultTarget, schema, policyRule);
		if (compositionStrategy != null) {
			fragment.order = compositionStrategy.getOrder();
		}
		standardFragments.add(fragment);
	}

	// checks the existence of approvers beforehand, because we don't want to have an empty level
	boolean addPredefined(PrismObject<?> targetObject, @NotNull QName relationName, OperationResult result) {
		RelationResolver resolver = primaryChangeAspect.createRelationResolver(targetObject, result);
		List<ObjectReferenceType> approvers = resolver.getApprovers(Collections.singletonList(relationName));
		if (!approvers.isEmpty()) {
			ApprovalLevelType level = new ApprovalLevelType();
			level.getApproverRef().addAll(approvers);
			addPredefined(targetObject, level);
			return true;
		} else {
			return false;
		}
	}

	void addPredefined(PrismObject<?> targetObject, ApprovalLevelType level) {
		ApprovalSchemaType schema = new ApprovalSchemaType();
		schema.getLevel().add(level);
		addPredefined(targetObject, schema);
	}

	void addPredefined(PrismObject<?> targetObject, ApprovalSchemaType schema) {
		predefinedFragments.add(new Fragment(targetObject, schema, null));
	}

	Result buildSchema(ModelInvocationContext ctx, OperationResult result) {
		sortFragments(predefinedFragments);
		sortFragments(standardFragments);

		ApprovalSchemaType schemaType = new ApprovalSchemaType(ctx.prismContext);
		ApprovalSchemaImpl schema = new ApprovalSchemaImpl(ctx.prismContext);
		SchemaAttachedPolicyRulesType attachedRules = new SchemaAttachedPolicyRulesType();
		Stream.concat(predefinedFragments.stream(), standardFragments.stream())
				.forEach(f -> processFragment(f, schemaType, schema, attachedRules, ctx, result));

		return new Result(schemaType, schema, attachedRules);
	}

	private void processFragment(Fragment fragment, ApprovalSchemaType resultingSchemaType, ApprovalSchemaImpl resultingSchema,
			SchemaAttachedPolicyRulesType attachedRules, ModelInvocationContext ctx, OperationResult result) {
		List<ApprovalLevelType> fragmentLevels = CloneUtil.cloneListMembers(fragment.schema.getLevel());
		if (fragmentLevels.isEmpty()) {
			return;		// probably shouldn't occur
		}
		fragmentLevels.sort(Comparator.comparing(ApprovalLevelType::getOrder, Comparator.nullsLast(naturalOrder())));
		RelationResolver relationResolver = primaryChangeAspect.createRelationResolver(fragment.target, result);
		ReferenceResolver referenceResolver = primaryChangeAspect.createReferenceResolver(ctx.modelContext, ctx.taskFromModel, result);
		int from = resultingSchemaType.getLevel().size() + 1;
		int i = from;
		for (ApprovalLevelType level : fragmentLevels) {
			level.setOrder(i++);
			resultingSchemaType.getLevel().add(level);
			resultingSchema.addLevel(level, relationResolver, referenceResolver);
		}
		if (fragment.policyRule != null) {
			SchemaAttachedPolicyRuleType attachedRule = new SchemaAttachedPolicyRuleType();
			attachedRule.setLevelMin(from);
			attachedRule.setLevelMax(i - 1);
			attachedRule.setRule(fragment.policyRule.toEvaluatedPolicyRuleType());
			attachedRules.getEntry().add(attachedRule);
		}
	}

	private void sortFragments(List<Fragment> fragments) {
		// relying on the fact that the sort algorithm is stable
		fragments.sort(Comparator.comparing(f -> f.order, Comparator.nullsLast(naturalOrder())));
	}

}
