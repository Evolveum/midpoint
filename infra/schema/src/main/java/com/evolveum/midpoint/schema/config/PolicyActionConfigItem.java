/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyActionType;

public class PolicyActionConfigItem<A extends PolicyActionType> extends ConfigurationItem<A> {

    @SuppressWarnings("unused") // called dynamically
    public PolicyActionConfigItem(@NotNull ConfigurationItem<A> original) {
        super(original);
    }

    public PolicyActionConfigItem(@NotNull A value, @NotNull ConfigurationItemOrigin origin) {
        super(value, origin);
    }

    public static <A extends PolicyActionType> PolicyActionConfigItem<A> of(
            @NotNull A bean, @NotNull ConfigurationItemOrigin origin) {
        return new PolicyActionConfigItem<>(bean, origin);
    }

    public static <A extends PolicyActionType> PolicyActionConfigItem<A> of(
            @NotNull A bean,
            @NotNull OriginProvider<? super A> originProvider) {
        return new PolicyActionConfigItem<>(bean, originProvider.origin(bean));
    }

    @Override
    public @NotNull String localDescription() {
        return "policy action";
    }

    @Override
    public PolicyActionConfigItem<A> clone() {
        return new PolicyActionConfigItem<>(super.clone());
    }

    public @Nullable String getName() {
        return value().getName();
    }

    public @Nullable ExpressionConfigItem getCondition() {
        return child(
                value().getCondition(),
                ExpressionConfigItem.class,
                PolicyActionType.F_CONDITION);
    }

    /** Preliminary implementation, will probably change. */
    public @NotNull String getTypeName() {
        return value().getClass().getSimpleName();
    }
}
