/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.notifications.impl.notifiers;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SimplePolicyRuleNotifierType;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class SimplePolicyRuleNotifier extends AbstractPolicyRuleNotifier<SimplePolicyRuleNotifierType> {

    @Override
    public @NotNull Class<SimplePolicyRuleNotifierType> getEventHandlerConfigurationType() {
        return SimplePolicyRuleNotifierType.class;
    }
}
