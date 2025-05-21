/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import static com.evolveum.midpoint.util.DebugUtil.*;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.util.PrismPrettyPrinter;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

public class EvaluatedActivityPolicyRule implements DebugDumpable {

    private final @NotNull ActivityPolicyType policy;

    private final @NotNull ActivityPath path;

    private final List<EvaluatedActivityPolicyRuleTrigger<?>> triggers = new ArrayList<>();

    /**
     * Whether the rule was enforced (i.e. the action was taken).
     */
    private boolean enforced;

    private ActivityPolicyStateType currentState;

    public EvaluatedActivityPolicyRule(@NotNull ActivityPolicyType policy, @NotNull ActivityPath path) {
        this.policy = policy;
        this.path = path;
    }

    public String getRuleId() {
        return ActivityPolicyUtils.createIdentifier(path, policy);
    }

    public String getName() {
        return policy.getName();
    }

    @NotNull
    public ActivityPolicyType getPolicy() {
        return policy;
    }

    public boolean containsAction(Class<? extends ActivityPolicyActionType> policyActionType) {
        return getActions(policy.getPolicyActions()).stream()
                .anyMatch(policyActionType::isInstance);
    }

    private List<ActivityPolicyActionType> getActions(ActivityPolicyActionsType actions) {
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

    @NotNull
    public List<EvaluatedActivityPolicyRuleTrigger<?>> getTriggers() {
        return triggers;
    }

    public void setTriggers(List<EvaluatedActivityPolicyRuleTrigger<?>> triggers) {
        this.triggers.clear();

        if (triggers != null) {
            this.triggers.addAll(triggers);
        }
    }

    public ActivityPolicyStateType getCurrentState() {
        return currentState;
    }

    public void setCurrentState(ActivityPolicyStateType currentState) {
        this.currentState = currentState;
    }

    public boolean isTriggered() {
        return !triggers.isEmpty() || (currentState != null && !currentState.getTriggers().isEmpty());
    }

    public boolean isEnforced() {
        return enforced || (currentState != null && currentState.isEnforced());
    }

    public void enforced() {
        enforced = true;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        debugDumpLabelLn(sb, "EvaluatedActivityPolicyRule " + (getName() != null ? getName() + " " : "") + "(triggers: " + triggers.size() + ")", indent);
        debugDumpWithLabelLn(sb, "name", getName(), indent + 1);
        debugDumpLabelLn(sb, "policyRuleType", indent + 1);
        indentDebugDump(sb, indent + 2);
        PrismPrettyPrinter.debugDumpValue(sb, indent + 2, policy, ActivityPolicyType.COMPLEX_TYPE, PrismContext.LANG_XML);
        sb.append('\n');
        debugDumpWithLabelLn(sb, "triggers", triggers, indent + 1);
        return sb.toString();
    }

    @Override
    public String toString() {
        return "EvaluatedActivityPolicyRule{" +
                "policy=" + policy.getName() +
                ", triggers=" + triggers.size() +
                '}';
    }
}
