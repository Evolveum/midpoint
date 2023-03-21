/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.processors.primary.policy;

import static java.util.Comparator.naturalOrder;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.TriggeredPolicyRulesStorageStrategyType.FULL;

import java.util.*;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.context.AssociatedPolicyRule;
import com.evolveum.midpoint.model.api.context.PolicyRuleExternalizationOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ApprovalSchemaHelper;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ReferenceResolver;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.RelationResolver;
import com.evolveum.midpoint.wf.impl.processors.ModelInvocationContext;
import com.evolveum.midpoint.wf.impl.processors.primary.aspect.BasePrimaryChangeAspect;
import com.evolveum.midpoint.wf.impl.processors.primary.policy.ProcessSpecifications.ProcessSpecification;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Creates complete approval schema and related information ({@link Result}) from partial information collected from
 * individual policy rules via {@link #add(ApprovalSchemaType, ApprovalPolicyActionType, PrismObject, AssociatedPolicyRule)}
 * and {@link #addPredefined(PrismObject, RelationKindType, OperationResult)} methods.
 */
class ApprovalSchemaBuilder {

    private ProcessSpecification processSpecification;

    void setProcessSpecification(ProcessSpecification processSpecification) {
        this.processSpecification = processSpecification;
    }

    static class Result {
        @NotNull final ApprovalSchemaType schema;
        @NotNull final SchemaAttachedPolicyRulesType attachedRules;
        @Nullable final ProcessSpecification processSpecification;
        @Nullable final LocalizableMessageTemplateType approvalDisplayName;

        Result(@NotNull ApprovalSchemaType schema,
                @NotNull SchemaAttachedPolicyRulesType attachedRules,
                @Nullable ProcessSpecification processSpecification,
                @Nullable LocalizableMessageTemplateType approvalDisplayName) {
            this.schema = schema;
            this.attachedRules = attachedRules;
            this.processSpecification = processSpecification;
            this.approvalDisplayName = approvalDisplayName;
        }
    }

    private static class Fragment {
        // object to which relations (approved, owner) are resolved
        // TODO test this thoroughly in presence of non-direct rules and merged schemas
        final PrismObject<?> target;
        @NotNull final ApprovalSchemaType schema;
        final AssociatedPolicyRule policyRule;
        final ApprovalCompositionStrategyType compositionStrategy;
        final LocalizableMessageTemplateType approvalDisplayName;

        private Fragment(
                ApprovalCompositionStrategyType compositionStrategy,
                PrismObject<?> target,
                @NotNull ApprovalSchemaType schema,
                AssociatedPolicyRule policyRule,
                LocalizableMessageTemplateType approvalDisplayName) {
            this.compositionStrategy = compositionStrategy;
            this.target = target;
            this.schema = schema;
            this.policyRule = policyRule;
            this.approvalDisplayName = approvalDisplayName;
        }

        private boolean isMergeableWith(Fragment other) {
            return isMergeable()
                    && other.isMergeable()
                    && hasSameExplicitOrderAs(other);
        }

        public boolean isExclusive() {
            return compositionStrategy != null && BooleanUtils.isTrue(compositionStrategy.isExclusive());
        }

        private boolean isMergeable() {
            return compositionStrategy != null && BooleanUtils.isTrue(compositionStrategy.isMergeable());
        }

        private boolean hasSameExplicitOrderAs(Fragment other) {
            Integer myOrder = getOrder();
            return myOrder != null && myOrder.equals(other.getOrder());
        }

        public Integer getOrder() {
            return compositionStrategy != null ? compositionStrategy.getOrder() : null;
        }
    }

    private final List<Fragment> predefinedFragments = new ArrayList<>();
    private final List<Fragment> standardFragments = new ArrayList<>();
    private final List<Fragment> addOnFragments = new ArrayList<>(); // fragments to be merged into other ones

    private final Set<Integer> exclusiveOrders = new HashSet<>();
    private final Set<Integer> nonExclusiveOrders = new HashSet<>();

    @NotNull private final BasePrimaryChangeAspect primaryChangeAspect;
    @NotNull private final ApprovalSchemaHelper approvalSchemaHelper;

    ApprovalSchemaBuilder(@NotNull BasePrimaryChangeAspect primaryChangeAspect, @NotNull ApprovalSchemaHelper approvalSchemaHelper) {
        this.primaryChangeAspect = primaryChangeAspect;
        this.approvalSchemaHelper = approvalSchemaHelper;
    }

    // TODO target
    void add(
            ApprovalSchemaType schema,
            ApprovalPolicyActionType approvalAction,
            PrismObject<?> defaultTarget,
            AssociatedPolicyRule policyRule) throws SchemaException {
        ApprovalCompositionStrategyType compositionStrategy = approvalAction.getCompositionStrategy();
        Fragment fragment = new Fragment(
                compositionStrategy, defaultTarget, schema, policyRule, approvalAction.getApprovalDisplayName());
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
    boolean addPredefined(PrismObject<?> targetObject, RelationKindType relationKind, OperationResult result) {
        RelationResolver resolver = primaryChangeAspect.createRelationResolver(targetObject, result);
        Collection<QName> relations = SchemaService.get().relationRegistry().getAllRelationsFor(relationKind);
        List<ObjectReferenceType> approvers = resolver.getApprovers(relations);
        if (!approvers.isEmpty()) {
            ApprovalStageDefinitionType stageDef = new ApprovalStageDefinitionType();
            stageDef.getApproverRef().addAll(approvers);
            addPredefined(targetObject, stageDef);
            return true;
        } else {
            return false;
        }
    }

    private void addPredefined(PrismObject<?> targetObject, ApprovalStageDefinitionType stageDef) {
        ApprovalSchemaType schema = new ApprovalSchemaType();
        schema.getStage().add(stageDef);
        addPredefined(targetObject, schema);
    }

    private void addPredefined(PrismObject<?> targetObject, ApprovalSchemaType schema) {
        predefinedFragments.add(
                new Fragment(null, targetObject, schema, null, null));
    }

    Result buildSchema(ModelInvocationContext<?> ctx, OperationResult result) throws SchemaException {
        sortFragments(predefinedFragments);
        sortFragments(standardFragments);
        List<Fragment> allFragments = new ArrayList<>();
        allFragments.addAll(predefinedFragments);
        allFragments.addAll(standardFragments);

        ApprovalSchemaType schemaBean = new ApprovalSchemaType();
        SchemaAttachedPolicyRulesType attachedRules = new SchemaAttachedPolicyRulesType();

        LocalizableMessageTemplateType approvalDisplayName = null;
        if (processSpecification != null
                && processSpecification.basicSpec != null
                && processSpecification.basicSpec.getApprovalDisplayName() != null) {
            approvalDisplayName = processSpecification.basicSpec.getApprovalDisplayName();
        }
        for (Fragment fragment : allFragments) {
            if (approvalDisplayName == null && fragment.approvalDisplayName != null) {
                approvalDisplayName = fragment.approvalDisplayName;
            }
        }

        int i = 0;
        while (i < allFragments.size()) {
            List<Fragment> fragmentMergeGroup = getMergeGroup(allFragments, i);
            i += fragmentMergeGroup.size();
            checkExclusivity(fragmentMergeGroup);
            processFragmentGroup(fragmentMergeGroup, schemaBean, attachedRules, ctx, result);
        }

        return new Result(schemaBean, attachedRules, processSpecification, approvalDisplayName);
    }

    private void checkExclusivity(List<Fragment> fragmentMergeGroup) {
        boolean isExclusive = fragmentMergeGroup.stream().anyMatch(Fragment::isExclusive);
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
        return new ArrayList<>(fragments.subList(i, j)); // result should be modifiable independently on the master list
    }

    private void processFragmentGroup(
            List<Fragment> fragments,
            ApprovalSchemaType resultingSchemaType,
            SchemaAttachedPolicyRulesType attachedRules,
            ModelInvocationContext<?> ctx,
            OperationResult result)
            throws SchemaException {
        Fragment firstFragment = fragments.get(0);
        appendAddOnFragments(fragments);
        List<ApprovalStageDefinitionType> fragmentStageDefs = cloneAndMergeStages(fragments);
        if (fragmentStageDefs.isEmpty()) {
            return; // probably shouldn't occur
        }
        fragmentStageDefs.sort(Comparator.comparing(this::getNumber, Comparator.nullsLast(naturalOrder())));
        RelationResolver relationResolver = primaryChangeAspect.createRelationResolver(firstFragment.target, result);
        ReferenceResolver referenceResolver = primaryChangeAspect.createReferenceResolver(ctx.modelContext, ctx.task, result);
        int from = getStages(resultingSchemaType).size() + 1;
        int i = from;
        for (ApprovalStageDefinitionType stageDef : fragmentStageDefs) {
            stageDef.asPrismContainerValue().setId(null);       // to avoid ID collision
            stageDef.setNumber(i++);
            approvalSchemaHelper.prepareStage(stageDef, relationResolver, referenceResolver);
            resultingSchemaType.getStage().add(stageDef);
        }
        if (firstFragment.policyRule != null) {
            List<EvaluatedPolicyRuleType> rules = new ArrayList<>();
            firstFragment.policyRule.addToEvaluatedPolicyRuleBeans(
                    rules,
                    new PolicyRuleExternalizationOptions(FULL, false),
                    null,
                    firstFragment.policyRule.getNewOwner());
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
        return s.getNumber();
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
        return schema.getStage();
    }

    private List<ApprovalStageDefinitionType> cloneAndMergeStages(List<Fragment> fragments) throws SchemaException {
        if (fragments.size() == 1) {
            return CloneUtil.cloneCollectionMembers(getStages(fragments.get(0).schema));
        }
        ApprovalStageDefinitionType resultingStageDef = new ApprovalStageDefinitionType();
        fragments.sort((f1, f2) ->
            Comparator.nullsLast(Comparator.<Integer>naturalOrder())
                .compare(f1.compositionStrategy.getMergePriority(), f2.compositionStrategy.getMergePriority()));
        for (Fragment fragment : fragments) {
            mergeStageDefFromFragment(resultingStageDef, fragment);
        }
        return Collections.singletonList(resultingStageDef);
    }

    private void mergeStageDefFromFragment(ApprovalStageDefinitionType resultingStageDef, Fragment fragment)
            throws SchemaException {
        List<ApprovalStageDefinitionType> stages = getStages(fragment.schema);
        if (stages.size() != 1) {
            throw new IllegalStateException(
                    "Couldn't merge approval schema fragment with stage count of not 1: " + fragment.schema);
        }
        ApprovalStageDefinitionType stageDefToMerge = stages.get(0);
        List<QName> overwriteItems = fragment.compositionStrategy.getMergeOverwriting();
        //noinspection unchecked
        resultingStageDef.asPrismContainerValue().mergeContent(
                stageDefToMerge.asPrismContainerValue(), overwriteItems);
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
