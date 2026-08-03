/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.page.admin.workflow.dto;

import com.evolveum.midpoint.repo.common.policy.TriggerBeanPresentationUtil.AdditionalData;
import com.evolveum.midpoint.repo.common.policy.TriggerBeanPresentationUtil.TriggerWithDataPredicate;
import com.evolveum.midpoint.repo.common.policy.TriggerBeanPresentationUtil.TriggerWithData;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.TreeNode;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EvaluatedPolicyRuleTriggerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EvaluatedPolicyRuleType;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.evolveum.midpoint.repo.common.policy.TriggerBeanPresentationUtil.arrangeForPresentationExt;
import static java.util.Collections.singleton;

/**
 * A list of triggers that should be displayed as a group (e.g. because they are children of a parent trigger).
 *
 * @see EvaluatedTriggerDto
 */
public class EvaluatedTriggerGroupDto implements Serializable {

    /** TODO consider removing, as this seems to be always null */
    public final LocalizableMessage title;

    public static final String F_TITLE = "title";
    public static final String F_TRIGGERS = "triggers";

    @NotNull private final List<EvaluatedTriggerDto> triggers = new ArrayList<>();

    EvaluatedTriggerGroupDto(
            LocalizableMessage title,
            List<TreeNode<TriggerWithData<HighlightingInformation>>> processedTriggers) {
        this.title = title;
        for (TreeNode<TriggerWithData<HighlightingInformation>> processedTrigger : processedTriggers) {
            this.triggers.add(new EvaluatedTriggerDto(processedTrigger));
        }
    }

    public LocalizableMessage getTitle() {
        return title;
    }

    @NotNull
    public List<EvaluatedTriggerDto> getTriggers() {
        return triggers;
    }

    public static EvaluatedTriggerGroupDto initializeFromRules(
            List<EvaluatedPolicyRuleType> rules, boolean highlighted, UniquenessFilter uniquenessFilter) {
        List<TriggerWithData<HighlightingInformation>> triggerWithData = new ArrayList<>();
        for (EvaluatedPolicyRuleType rule : rules) {
            for (EvaluatedPolicyRuleTriggerType trigger : rule.getTrigger()) {
                triggerWithData.add(new TriggerWithData<>(trigger, new HighlightingInformation(highlighted)));
            }
        }
        List<TreeNode<TriggerWithData<HighlightingInformation>>> triggerTrees =
                arrangeForPresentationExt(triggerWithData, uniquenessFilter);
        return new EvaluatedTriggerGroupDto(null, triggerTrees);
    }

    /** Stateful filter that passes only unique triggers - to avoid displaying the same information twice. */
    public static class UniquenessFilter implements TriggerWithDataPredicate<HighlightingInformation> {

        private record AlreadyShownTriggerRecord<AD extends AdditionalData>(
                TriggerWithData<AD> triggerWithData,
                EvaluatedPolicyRuleTriggerType anonymizedTrigger) {
        }

        List<AlreadyShownTriggerRecord<HighlightingInformation>> triggersAlreadyShown = new ArrayList<>();

        public boolean test(TriggerWithData<HighlightingInformation> newTriggerWithData) {
            EvaluatedPolicyRuleTriggerType anonymizedTrigger = newTriggerWithData.trigger().clone().ruleName(null);
            for (AlreadyShownTriggerRecord<HighlightingInformation> triggerAlreadyShown : triggersAlreadyShown) {
                if (triggerAlreadyShown.anonymizedTrigger.equals(anonymizedTrigger)) {
                    triggerAlreadyShown.triggerWithData.additionalData().merge(newTriggerWithData.additionalData());
                    return false;
                }
            }
            triggersAlreadyShown.add(new AlreadyShownTriggerRecord<>(newTriggerWithData, anonymizedTrigger));
            return true;
        }
    }

    public static class HighlightingInformation implements AdditionalData {

        /** Whether the trigger should be highlighted. */
        boolean value;

        HighlightingInformation(boolean value) {
            this.value = value;
        }

        public void merge(AdditionalData other) {
            value = value || ((HighlightingInformation) other).value;
        }
    }

    public static boolean isEmpty(Collection<EvaluatedTriggerGroupDto> groups) {
        // original implementation (after migration from 3.6 to 3.7 is over)
//        return groups.stream()
//                .allMatch(g -> g.getTriggers().isEmpty());

        // temporary implementation for 3.7 - assuming we can display a trigger only if 'message' is not null and/or it has some children
        return groups.stream()
                .allMatch(g -> triggersAreEmpty(g.getTriggers()));

    }

    private static boolean triggersAreEmpty(Collection<EvaluatedTriggerDto> triggers) {
        return triggers.stream()
                .allMatch(t -> triggerIsEmpty(t));
    }

    private static boolean triggerIsEmpty(EvaluatedTriggerDto trigger) {
        return trigger.getMessage() == null && isEmpty(singleton(trigger.getChildren()));
    }
}
