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
import com.evolveum.midpoint.model.api.context.PolicyRuleExternalizationOptions;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.*;
import com.evolveum.midpoint.wf.impl.processors.primary.ModelInvocationContext;
import com.evolveum.midpoint.wf.impl.processors.primary.aspect.BasePrimaryChangeAspect;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.BooleanUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.*;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.TriggeredPolicyRulesStorageStrategyType.FULL;
import static java.util.Comparator.naturalOrder;

/**
 * @author mederly
 */
class ApprovalSchemaBuilder {

	class Result {
		@NotNull final ApprovalSchemaType schemaType;
		@NotNull final SchemaAttachedPolicyRulesType attachedRules;

		public Result(@NotNull ApprovalSchemaType schemaType,
				@NotNull SchemaAttachedPolicyRulesType attachedRules) {
			this.schemaType = schemaType;
			this.attachedRules = attachedRules;
		}
	}

	private class Fragment {
		// object to which relations (approved, owner) are resolved
		// TODO test this thoroughly in presence of non-direct rules and merged schemas
		final PrismObject<?> target;
		@NotNull final ApprovalSchemaType schema;
		final EvaluatedPolicyRule policyRule;
		final ApprovalCompositionStrategyType compositionStrategy;

		private Fragment(ApprovalCompositionStrategyType compositionStrategy, PrismObject<?> target,
				@NotNull ApprovalSchemaType schema, EvaluatedPolicyRule policyRule) {
			this.compositionStrategy = compositionStrategy;
			this.target = target;
			this.schema = schema;
			this.policyRule = policyRule;
		}

		private boolean isMergeableWith(Fragment other) {
			return compositionStrategy != null && BooleanUtils.isTrue(compositionStrategy.isMergeable())
					&& other.compositionStrategy != null && BooleanUtils.isTrue(other.compositionStrategy.isMergeable())
					&& compositionStrategy.getOrder() != null && compositionStrategy.getOrder().equals(other.compositionStrategy.getOrder());
		}

		public boolean isExclusive() {
			return compositionStrategy != null && BooleanUtils.isTrue(compositionStrategy.isExclusive());
		}

		public Integer getOrder() {
			return compositionStrategy != null ? compositionStrategy.getOrder() : null;
		}
	}

	private final List<Fragment> predefinedFragments = new ArrayList<>();
	private final List<Fragment> standardFragments = new ArrayList<>();
	private final List<Fragment> addOnFragments = new ArrayList<>();			// fragments to be merged into other ones

	private final Set<Integer> exclusiveOrders = new HashSet<>();
	private final Set<Integer> nonExclusiveOrders = new HashSet<>();

	@NotNull private final BasePrimaryChangeAspect primaryChangeAspect;
	@NotNull private final ApprovalSchemaHelper approvalSchemaHelper;

	ApprovalSchemaBuilder(@NotNull BasePrimaryChangeAspect primaryChangeAspect, ApprovalSchemaHelper approvalSchemaHelper) {
		this.primaryChangeAspect = primaryChangeAspect;
		this.approvalSchemaHelper = approvalSchemaHelper;
	}

	// TODO target
	void add(ApprovalSchemaType schema, ApprovalCompositionStrategyType compositionStrategy, PrismObject<?> defaultTarget,
			EvaluatedPolicyRule policyRule) throws SchemaException {
		Fragment fragment = new Fragment(compositionStrategy, defaultTarget, schema, policyRule);
		if (isAddOnFragment(compositionStrategy)) {
			if (compositionStrategy.getOrder() != null) {
				throw new SchemaException("Both order and mergeIntoOrder/mergeIntoAll are set for " + schema);
			}
			addOnFragments.add(fragment);
		} else {
			standardFragments.add(fragment);
		}
	}

	private boolean isAddOnFragment(ApprovalCompositionStrategyType cs) {
		return cs != null && (!cs.getMergeIntoOrder().isEmpty() || BooleanUtils.isTrue(cs.isMergeIntoAll()));
	}

	// checks the existence of approvers beforehand, because we don't want to have an empty stage
	boolean addPredefined(PrismObject<?> targetObject, @NotNull QName relationName, OperationResult result) {
		RelationResolver resolver = primaryChangeAspect.createRelationResolver(targetObject, result);
		List<ObjectReferenceType> approvers = resolver.getApprovers(Collections.singletonList(relationName));
		if (!approvers.isEmpty()) {
			ApprovalStageDefinitionType stageDef = new ApprovalStageDefinitionType();
			stageDef.getApproverRef().addAll(approvers);
			addPredefined(targetObject, stageDef);
			return true;
		} else {
			return false;
		}
	}

	void addPredefined(PrismObject<?> targetObject, ApprovalStageDefinitionType stageDef) {
		ApprovalSchemaType schema = new ApprovalSchemaType();
		schema.getStage().add(stageDef);
		addPredefined(targetObject, schema);
	}

	void addPredefined(PrismObject<?> targetObject, ApprovalSchemaType schema) {
		predefinedFragments.add(new Fragment(null, targetObject, schema, null));
	}

	Result buildSchema(ModelInvocationContext ctx, OperationResult result) throws SchemaException {
		sortFragments(predefinedFragments);
		sortFragments(standardFragments);
		List<Fragment> allFragments = new ArrayList<>();
		allFragments.addAll(predefinedFragments);
		allFragments.addAll(standardFragments);

		ApprovalSchemaType schemaType = new ApprovalSchemaType(ctx.prismContext);
		SchemaAttachedPolicyRulesType attachedRules = new SchemaAttachedPolicyRulesType();

		int i = 0;
		while(i < allFragments.size()) {
			List<Fragment> fragmentMergeGroup = getMergeGroup(allFragments, i);
			i += fragmentMergeGroup.size();
			checkExclusivity(fragmentMergeGroup);
			processFragmentGroup(fragmentMergeGroup, schemaType, attachedRules, ctx, result);
		}

		return new Result(schemaType, attachedRules);
	}

	private void checkExclusivity(List<Fragment> fragmentMergeGroup) {
		boolean isExclusive = fragmentMergeGroup.stream().anyMatch(f -> f.isExclusive());
		Integer order = fragmentMergeGroup.get(0).getOrder();
		if (isExclusive) {
			if (exclusiveOrders.contains(order) || nonExclusiveOrders.contains(order)) {
				throw new IllegalStateException("Exclusivity violation for schema fragments with the order of " + order);
			}
			exclusiveOrders.add(order);
		} else {
			if (exclusiveOrders.contains(order)) {
				throw new IllegalStateException("Exclusivity violation for schema fragments with the order of " + order);
			}
			nonExclusiveOrders.add(order);
		}
	}

	private List<Fragment> getMergeGroup(List<Fragment> fragments, int i) {
		int j = i+1;
		while (j < fragments.size() && fragments.get(i).isMergeableWith(fragments.get(j))) {
			j++;
		}
		return new ArrayList<>(fragments.subList(i, j));		// result should be modifiable independently on the master list
	}

	private void processFragmentGroup(List<Fragment> fragments, ApprovalSchemaType resultingSchemaType,
			SchemaAttachedPolicyRulesType attachedRules, ModelInvocationContext ctx, OperationResult result)
			throws SchemaException {
		Fragment firstFragment = fragments.get(0);
		appendAddOnFragments(fragments);
		List<ApprovalStageDefinitionType> fragmentStageDefs = cloneAndMergeStages(fragments);
		if (fragmentStageDefs.isEmpty()) {
			return;		// probably shouldn't occur
		}
		fragmentStageDefs.sort(Comparator.comparing(s -> getNumber(s), Comparator.nullsLast(naturalOrder())));
		RelationResolver relationResolver = primaryChangeAspect.createRelationResolver(firstFragment.target, result);
		ReferenceResolver referenceResolver = primaryChangeAspect.createReferenceResolver(ctx.modelContext, ctx.taskFromModel, result);
		int from = getStages(resultingSchemaType).size() + 1;
		int i = from;
		for (ApprovalStageDefinitionType stageDef : fragmentStageDefs) {
			stageDef.setOrder(null);
			stageDef.setNumber(i++);
			approvalSchemaHelper.prepareStage(stageDef, relationResolver, referenceResolver);
			resultingSchemaType.getStage().add(stageDef);
		}
		if (firstFragment.policyRule != null) {
			List<EvaluatedPolicyRuleType> rules = new ArrayList<>();
			firstFragment.policyRule.addToEvaluatedPolicyRuleTypes(rules, new PolicyRuleExternalizationOptions(FULL,
					false, true));
			for (EvaluatedPolicyRuleType rule : rules) {
				SchemaAttachedPolicyRuleType attachedRule = new SchemaAttachedPolicyRuleType();
				attachedRule.setStageMin(from);
				attachedRule.setStageMax(i - 1);
				attachedRule.setRule(rule);
				attachedRules.getEntry().add(attachedRule);
			}
		}
	}

	private Integer getNumber(ApprovalStageDefinitionType s) {
		return s.getNumber() != null ? s.getNumber() : s.getOrder();
	}

	private void appendAddOnFragments(List<Fragment> fragments) {
		Integer order = fragments.get(0).getOrder();
		if (order == null) {
			return;
		}
		for (Fragment addOnFragment : addOnFragments) {
			ApprovalCompositionStrategyType cs = addOnFragment.compositionStrategy;
			if (BooleanUtils.isTrue(cs.isMergeIntoAll()) || cs.getMergeIntoOrder().contains(order)) {
				fragments.add(addOnFragment);
			}
		}
	}

	private List<ApprovalStageDefinitionType> getStages(ApprovalSchemaType schema) {
		return !schema.getStage().isEmpty() ? schema.getStage() : schema.getLevel();
	}

	private List<ApprovalStageDefinitionType> cloneAndMergeStages(List<Fragment> fragments) throws SchemaException {
		if (fragments.size() == 1) {
			return CloneUtil.cloneCollectionMembers(getStages(fragments.get(0).schema));
		}
		PrismContext prismContext = primaryChangeAspect.getChangeProcessor().getPrismContext();
		ApprovalStageDefinitionType resultingStageDef = new ApprovalStageDefinitionType(prismContext);
		fragments.sort((f1, f2) ->
			Comparator.nullsLast(Comparator.<Integer>naturalOrder())
				.compare(f1.compositionStrategy.getMergePriority(), f2.compositionStrategy.getMergePriority()));
		for (Fragment fragment : fragments) {
			mergeStageDefFromFragment(resultingStageDef, fragment);
		}
		return Collections.singletonList(resultingStageDef);
	}

	private void mergeStageDefFromFragment(ApprovalStageDefinitionType resultingStageDef, Fragment fragment) throws SchemaException {
		List<ApprovalStageDefinitionType> stages = getStages(fragment.schema);
		if (stages.size() != 1) {
			throw new IllegalStateException("Couldn't merge approval schema fragment with stage count of not 1: " + fragment.schema);
		}
		ApprovalStageDefinitionType stageDefToMerge = stages.get(0);
		List<QName> overwriteItems = fragment.compositionStrategy.getMergeOverwriting();
		resultingStageDef.asPrismContainerValue().mergeContent(stageDefToMerge.asPrismContainerValue(), overwriteItems);
	}

	private void sortFragments(List<Fragment> fragments) {
		fragments.forEach(f -> {
			if (f.compositionStrategy != null && BooleanUtils.isTrue(f.compositionStrategy.isMergeable())
					&& f.compositionStrategy.getOrder() == null) {
				throw new IllegalStateException("Mergeable composition strategy with no order: "
						+ f.compositionStrategy + " in " + f.policyRule);
			}
		});

		// relying on the fact that the sort algorithm is stable
		fragments.sort((f1, f2) -> {
			ApprovalCompositionStrategyType s1 = f1.compositionStrategy;
			ApprovalCompositionStrategyType s2 = f2.compositionStrategy;
			Integer o1 = s1 != null ? s1.getOrder() : null;
			Integer o2 = s2 != null ? s2.getOrder() : null;
			if (o1 == null || o2 == null) {
				return MiscUtil.compareNullLast(o1, o2);
			}
			int c = Integer.compare(o1, o2);
			if (c != 0) {
				return c;
			}
			// non-mergeable first
			boolean m1 = BooleanUtils.isTrue(s1.isMergeable());
			boolean m2 = BooleanUtils.isTrue(s2.isMergeable());
			if (m1 && !m2) {
				return 1;
			} else if (!m1 && m2) {
				return -1;
			} else {
				return 0;
			}
		});
	}

}
