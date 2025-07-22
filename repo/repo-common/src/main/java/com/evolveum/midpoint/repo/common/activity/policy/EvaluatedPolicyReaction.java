/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import static com.evolveum.midpoint.util.DebugUtil.debugDumpLabelLn;
import static com.evolveum.midpoint.util.DebugUtil.debugDumpWithLabelLn;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class EvaluatedPolicyReaction implements DebugDumpable {

    private @NotNull EvaluatedActivityPolicyRule rule;

    private @NotNull PolicyReactionType reaction;

    private boolean enforced;

    public EvaluatedPolicyReaction(@NotNull EvaluatedActivityPolicyRule rule, @NotNull PolicyReactionType reaction) {
        this.rule = rule;
        this.reaction = reaction;
    }

    public String getName() {
        return reaction.getName();
    }

    public @NotNull EvaluatedActivityPolicyRule getRule() {
        return rule;
    }

    public boolean hasThreshold() {
        return reaction.getThreshold() != null;
    }

    public boolean isWithinThreshold() {
        PolicyThresholdType threshold = reaction.getThreshold();
        if (threshold == null) {
            return true;
        }

        ThresholdEvaluator thresholdEvaluator = rule.getThresholdValueType().getEvaluator();

        return thresholdEvaluator.evaluate(threshold, rule.getThresholdValue());
    }

    public boolean isEnforced() {
        return enforced || rule.isReactionEnforced(getReactionIdentifier());
    }

    private String getReactionIdentifier() {
        return reaction.getId() != null ? Long.toString(reaction.getId()) : null;
    }

    public void enforced() {
        enforced = true;
    }

    public <T extends ActivityPolicyActionType> T getAction(Class<T> policyActionType) {
        return getActions().stream()
                .filter(policyActionType::isInstance)
                .map(policyActionType::cast)
                .findFirst()
                .orElse(null);
    }

    @NotNull
    public List<ActivityPolicyActionType> getActions() {
        ActivityPolicyActionsType actions = reaction.getAction();
        if (actions == null) {
            return List.of();
        }

        List<ActivityPolicyActionType> result = new ArrayList<>();

        addAction(result, actions.getNotification());
        addAction(result, actions.getRestartActivity());
        addAction(result, actions.getSkipActivity());
        addAction(result, actions.getSuspendTask());

        return result;
    }

    private void addAction(List<ActivityPolicyActionType> actions, ActivityPolicyActionType action) {
        if (action != null) {
            actions.add(action);
        }
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();

        debugDumpLabelLn(sb, "EvaluatedPolicyReaction " + (getName() != null ? getName() + " " : ""), indent);
        debugDumpWithLabelLn(sb, "rule", getRule().getName(), indent + 1);

        return sb.toString();
    }

    public EvaluatedActivityPolicyReactionType toPolicyReactionType() {
        EvaluatedActivityPolicyReactionType r = new EvaluatedActivityPolicyReactionType();
        r.setRef(getReactionIdentifier());
        r.setReactionName(reaction.getName());
        r.setEnforced(enforced); // todo is enforced flag needed? [viliam]
        // todo reaction message if there's something... [viliam]

        return r;
    }
}
