/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.evaluator;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.wss4j.common.ext.WSPasswordCallback;

import java.io.IOException;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author Igor Farinic
 * @author Radovan Semancik
 */
public class PasswordCallback implements CallbackHandler {

    private static final Trace LOGGER = TraceManager.getTrace(PasswordCallback.class);

    private final PasswordAuthenticationEvaluatorImpl passwordAuthenticationEvaluatorImpl;

    public PasswordCallback(PasswordAuthenticationEvaluatorImpl passwordAuthenticationEvaluatorImpl) {
        this.passwordAuthenticationEvaluatorImpl = passwordAuthenticationEvaluatorImpl;
    }

    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        LOGGER.trace("Invoked PasswordCallback with {} callbacks: {}", callbacks.length, callbacks);
        WSPasswordCallback pc = (WSPasswordCallback) callbacks[0];

        String username = pc.getIdentifier();
        String wssPasswordType = pc.getType();
        LOGGER.trace("Username: '{}', Password type: {}", username, wssPasswordType);

        try {
            ConnectionEnvironment connEnv = ConnectionEnvironment.create(SchemaConstants.CHANNEL_WEB_SERVICE_URI);
            pc.setPassword(passwordAuthenticationEvaluatorImpl.getAndCheckUserPassword(connEnv, username));
        } catch (Exception e) {
            LOGGER.trace("Exception in password callback: {}: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            throw new PasswordCallbackException("Authentication failed");
        }
   }
}
