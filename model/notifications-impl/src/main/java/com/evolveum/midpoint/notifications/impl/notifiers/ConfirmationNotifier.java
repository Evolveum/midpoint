/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.notifiers;

import com.evolveum.midpoint.model.common.expression.ModelExpressionEnvironment;
import com.evolveum.midpoint.repo.common.expression.ExpressionEnvironmentThreadLocalHolder;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.notifications.api.events.ModelEvent;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author katkav
 */
@Component
public abstract class ConfirmationNotifier<N extends ConfirmationNotifierType> extends AbstractGeneralNotifier<ModelEvent, N> {

    private static final Trace LOGGER = TraceManager.getTrace(ConfirmationNotifier.class);

    @Autowired private MidpointFunctions midpointFunctions;

    @Override
    public @NotNull Class<ModelEvent> getEventType() {
        return ModelEvent.class;
    }

    String createConfirmationLink(UserType userType, ConfigurationItem<? extends N> config, OperationResult result) {

        RegistrationConfirmationMethodType confirmationMethod = config.value().getConfirmationMethod();
        if (confirmationMethod == null) {
            return null;
        }
        ExpressionEnvironmentThreadLocalHolder.pushExpressionEnvironment(
                new ModelExpressionEnvironment.ExpressionEnvironmentBuilder<>()
                        .currentResult(result)
                        .build());

        try {
            return switch (confirmationMethod) {
                case LINK -> getConfirmationLink(userType);
                case PIN -> throw new UnsupportedOperationException("PIN confirmation not supported yet");
                default -> null;
            };
        } finally {
            ExpressionEnvironmentThreadLocalHolder.popExpressionEnvironment();
        }
    }

    public String getConfirmationLink(UserType user) {
        throw new UnsupportedOperationException("Please implement in concrete notifier");
    }

    protected UserType getUser(ModelEvent event) {
        //noinspection unchecked
        PrismObject<UserType> newUser = (PrismObject<UserType>) event.getFocusContext().getObjectNew();
        return newUser.asObjectable();
    }

    @Override
    protected Trace getLogger() {
        return LOGGER;
    }

    MidpointFunctions getMidpointFunctions() {
        return midpointFunctions;
    }
}
