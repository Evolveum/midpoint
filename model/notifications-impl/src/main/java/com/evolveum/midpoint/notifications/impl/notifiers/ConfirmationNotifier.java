/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.notifiers;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.model.impl.expr.ExpressionEnvironment;
import com.evolveum.midpoint.model.impl.expr.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.events.ModelEvent;
import com.evolveum.midpoint.notifications.impl.NotificationFunctionsImpl;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConfirmationNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GeneralNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RegistrationConfirmationMethodType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author katkav
 */
@Component
public class ConfirmationNotifier extends GeneralNotifier {

    private static final Trace LOGGER = TraceManager.getTrace(ConfirmationNotifier.class);

    @Autowired
    private MidpointFunctions midpointFunctions;

    @Autowired
    private NotificationFunctionsImpl notificationsUtil;


    @PostConstruct
    public void init() {
        register(ConfirmationNotifierType.class);
    }

    @Override
    protected boolean quickCheckApplicability(Event event, GeneralNotifierType generalNotifierType,
            OperationResult result) {
        if (!(event instanceof ModelEvent)) {
            logNotApplicable(event, "wrong event type");
            return false;
        } else {
            return true;
        }
    }

    public String getConfirmationLink(UserType userType) {
        throw new UnsupportedOperationException("Please implement in concrete notifier");
    }

    protected String createConfirmationLink(UserType userType, GeneralNotifierType generalNotifierType, OperationResult result) {


        ConfirmationNotifierType userRegistrationNotifier = (ConfirmationNotifierType) generalNotifierType;

        RegistrationConfirmationMethodType confirmationMethod = userRegistrationNotifier.getConfirmationMethod();

        if (confirmationMethod == null) {
            return null;
        }
        ExpressionEnvironment expressionEnv = new ExpressionEnvironment();
        expressionEnv.setCurrentResult(result);
        ModelExpressionThreadLocalHolder.pushExpressionEnvironment(expressionEnv);

        try {

            switch (confirmationMethod) {
                case LINK:
                    String confirmationLink = getConfirmationLink(userType);
                    return confirmationLink;
                case PIN:
                    throw new UnsupportedOperationException("PIN confirmation not supported yes");
    //                return getNonce(userType);
                default:
                    break;
            }

        } finally {
            ModelExpressionThreadLocalHolder.popExpressionEnvironment();
        }

        return null;

    }

    protected UserType getUser(Event event){
        ModelEvent modelEvent = (ModelEvent) event;
        //noinspection unchecked
        PrismObject<UserType> newUser = (PrismObject<UserType>) modelEvent.getFocusContext().getObjectNew();
        return newUser.asObjectable();
    }


    @Override
    protected Trace getLogger() {
        return LOGGER;
    }

    public MidpointFunctions getMidpointFunctions() {
        return midpointFunctions;
    }
}
