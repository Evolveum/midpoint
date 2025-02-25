/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.util.PrismPrettyPrinter;
import com.evolveum.midpoint.util.DebugDumpable;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPolicyActionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyActionType;

import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.util.DebugUtil.*;

public class EvaluatedActivityPolicyRule implements DebugDumpable {

    private final @NotNull ActivityPolicyType policy;

    private final List<EvaluatedActivityPolicyRuleTrigger<?>> triggers = new ArrayList<>();

    public EvaluatedActivityPolicyRule(@NotNull ActivityPolicyType policy) {
        this.policy = policy;
    }

    @Nullable
    public String getName() {
        return policy.getName();
    }

    public ActivityPolicyType getPolicy() {
        return policy;
    }

    public boolean containsAction(Class<? extends PolicyActionType> policyActionType) {
        return getActions(policy.getPolicyActions()).stream()
                .anyMatch(policyActionType::isInstance);
    }

    private List<PolicyActionType> getActions(ActivityPolicyActionsType actions) {
        if (actions == null) {
            return List.of();
        }

        List<PolicyActionType> result = new ArrayList<>();

        addAction(result, actions.getNotification());
        addAction(result, actions.getSuspendTask());

        return result;
    }

    private void addAction(List<PolicyActionType> actions, PolicyActionType action) {
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
}
