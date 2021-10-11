/*
 * Copyright (c) 2016-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.context;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import com.evolveum.midpoint.prism.PrismContext;
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
public interface EvaluatedPolicyRule extends DebugDumpable, Serializable, Cloneable {

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

    PolicyThresholdType getPolicyThreshold();

    // returns statically defined actions; consider using getEnabledActions() instead
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

    void addToEvaluatedPolicyRuleTypes(Collection<EvaluatedPolicyRuleType> rules, PolicyRuleExternalizationOptions options,
            Predicate<EvaluatedPolicyRuleTrigger<?>> triggerSelector, PrismContext prismContext);

    boolean isGlobal();

    String toShortString();

    List<TreeNode<LocalizableMessage>> extractMessages();

    List<TreeNode<LocalizableMessage>> extractShortMessages();

    // BEWARE: enabled actions can be queried only after computeEnabledActions has been called
    // todo think again about this

    boolean containsEnabledAction();

    boolean containsEnabledAction(Class<? extends PolicyActionType> clazz);

    Collection<PolicyActionType> getEnabledActions();

    <T extends PolicyActionType> List<T> getEnabledActions(Class<T> clazz);

    <T extends PolicyActionType> T getEnabledAction(Class<T> clazz);

    // use only if you know what you're doing
    void addTrigger(@NotNull EvaluatedPolicyRuleTrigger<?> trigger);

    //experimental
    String getPolicyRuleIdentifier();
}
