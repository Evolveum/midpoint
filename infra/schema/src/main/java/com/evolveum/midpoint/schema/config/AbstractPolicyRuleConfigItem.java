/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.impl.binding.AbstractReferencable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyActionsType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/** Exception from naming rules */
public abstract class AbstractPolicyRuleConfigItem<R extends PolicyRuleType> extends ConfigurationItem<R> {

    @SuppressWarnings("unused") // invoked dynamically
    AbstractPolicyRuleConfigItem(@NotNull ConfigurationItem<R> original) {
        super(original);
    }

    AbstractPolicyRuleConfigItem(@NotNull R value, @NotNull ConfigurationItemOrigin origin) {
        super(value, origin);
    }

    public @Nullable String getName() {
        return value().getName();
    }

    public List<PolicyActionConfigItem<?>> getAllActions() {
        var actionsBean = value().getPolicyActions();
        if (actionsBean == null) {
            return List.of();
        }

        //noinspection unchecked
        PrismContainerValue<PolicyActionsType> actionsPcv = actionsBean.asPrismContainerValue();

        List<PolicyActionConfigItem<?>> rv = new ArrayList<>();
        addActions(rv, actionsPcv, PolicyActionsType.F_ENFORCEMENT);
        addActions(rv, actionsPcv, PolicyActionsType.F_APPROVAL);
        addActions(rv, actionsPcv, PolicyActionsType.F_REMEDIATION);
        addActions(rv, actionsPcv, PolicyActionsType.F_PRUNE);
        addActions(rv, actionsPcv, PolicyActionsType.F_CERTIFICATION);
        addActions(rv, actionsPcv, PolicyActionsType.F_NOTIFICATION);
        addActions(rv, actionsPcv, PolicyActionsType.F_RECORD);
        addActions(rv, actionsPcv, PolicyActionsType.F_SCRIPT_EXECUTION);
        addActions(rv, actionsPcv, PolicyActionsType.F_SUSPEND_TASK);
        return rv;
    }

    private <A extends PolicyActionType> void addActions(
            List<PolicyActionConfigItem<?>> target, PrismContainerValue<PolicyActionsType> actionsPcv, ItemName itemName) {
        //noinspection unchecked,rawtypes
        PrismContainer<A> item = (PrismContainer<A>) (PrismContainer) actionsPcv.findItem(itemName);
        if (item == null) {
            return;
        }
        for (A action : item.getRealValues()) {
            target.add(
                    new PolicyActionConfigItem<>(
                            childWithOrWithoutId(action, itemName)));
        }
    }

    public @NotNull Collection<String> getEventMarksOids() {
        // TODO treat no-OID refs
        return value().getMarkRef().stream()
                .map(AbstractReferencable::getOid)
                .collect(Collectors.toSet());
    }
}
