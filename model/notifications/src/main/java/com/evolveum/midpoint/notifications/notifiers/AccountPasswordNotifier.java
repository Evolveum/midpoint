/*
 * Copyright (c) 2013 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.notifications.notifiers;

import com.evolveum.midpoint.common.crypto.EncryptionException;
import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.notifications.events.AccountEvent;
import com.evolveum.midpoint.notifications.events.Event;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountPasswordNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.GeneralNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author mederly
 */
@Component
public class AccountPasswordNotifier extends GeneralNotifier {

    private static final Trace LOGGER = TraceManager.getTrace(AccountPasswordNotifier.class);
    private static final Integer LEVEL_TECH_INFO = 10;

    @Autowired
    private MidpointFunctions midpointFunctions;

    @PostConstruct
    public void init() {
        register(AccountPasswordNotifierType.class);
    }

    @Override
    protected boolean checkApplicability(Event event, GeneralNotifierType generalNotifierType, OperationResult result) {
        if (!(event instanceof AccountEvent)) {
            LOGGER.trace("AccountPasswordNotifier was called with incompatible notification event; class = " + event.getClass());
            return false;
        }
        if (!event.isSuccess()) {
            LOGGER.trace("Operation was not successful, exiting.");
            return false;
        }

        AccountEvent accountEvent = (AccountEvent) event;
        ObjectDelta<? extends ShadowType> delta = accountEvent.getAccountOperationDescription().getObjectDelta();
        if (delta == null) {    // should not occur
            LOGGER.trace("Object delta is null, exiting. Event = " + event);
            return false;
        }
        return getPasswordFromDelta(delta) != null;
    }

    private String getPasswordFromDelta(ObjectDelta<? extends ShadowType> delta) {
        try {
            return midpointFunctions.getPlaintextAccountPasswordFromDelta(delta);
        } catch (EncryptionException e) {
            LoggingUtils.logException(LOGGER, "Couldn't decrypt password from shadow delta: {}", e, delta.debugDump());
            return null;
        }
    }

    @Override
    protected String getSubject(Event event, GeneralNotifierType generalNotifierType, String transport, OperationResult result) {
        return "Account password notification";
    }

    @Override
    protected String getBody(Event event, GeneralNotifierType generalNotifierType, String transport, OperationResult result) {

        StringBuilder body = new StringBuilder();

        AccountEvent accountEvent = (AccountEvent) event;

        ResourceOperationDescription rod = accountEvent.getAccountOperationDescription();
        ObjectDelta<ShadowType> delta = (ObjectDelta<ShadowType>) rod.getObjectDelta();

        body.append("Password for account ");
        if (rod.getCurrentShadow() != null && rod.getCurrentShadow().asObjectable().getName() != null) {
            body.append(rod.getCurrentShadow().asObjectable().getName() + " ");
        }
        body.append("on " + rod.getResource().asObjectable().getName());
        body.append(" is: " + getPasswordFromDelta(delta));
        return body.toString();
    }

}
