/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.notifiers;

import com.evolveum.midpoint.model.common.expression.ModelExpressionEnvironment;
import com.evolveum.midpoint.repo.common.expression.ExpressionEnvironmentThreadLocalHolder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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
    public Class<ModelEvent> getEventType() {
        return ModelEvent.class;
    }

    public String getConfirmationLink(UserType userType, String channel) {
        throw new UnsupportedOperationException("Please implement in concrete notifier");
    }

    String createConfirmationLink(UserType userType, N config, String channel, OperationResult result) {

        RegistrationConfirmationMethodType confirmationMethod = config.getConfirmationMethod();
        if (confirmationMethod == null) {
            return null;
        }
        ExpressionEnvironmentThreadLocalHolder.pushExpressionEnvironment(
                new ModelExpressionEnvironment.ExpressionEnvironmentBuilder<>()
                        .currentResult(result)
                        .build());

        try {
            switch (confirmationMethod) {
                case LINK:
                    return getConfirmationLink(userType, channel);
                case PIN:
                    throw new UnsupportedOperationException("PIN confirmation not supported yes");
    //                return getNonce(userType);
                default:
                    return null;
            }
        } finally {
            ExpressionEnvironmentThreadLocalHolder.popExpressionEnvironment();
        }
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
