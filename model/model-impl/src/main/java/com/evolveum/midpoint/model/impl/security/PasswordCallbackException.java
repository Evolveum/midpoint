/*
 * Copyright (c) 2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.security;

import java.io.IOException;

/**
 * Hack to correctly process exceptions from a password callback;
 * If this exception is thrown then the event was already audited.
 *
 * This is deliberately a subclass of IOException because CXF will
 * only handle IOExceptions correctly.
 *
 * @author semancik
 */
public class PasswordCallbackException extends IOException {

    public PasswordCallbackException() {
        super();
    }

    public PasswordCallbackException(String message, Throwable cause) {
        super(message, cause);
    }

    public PasswordCallbackException(String message) {
        super(message);
    }

    public PasswordCallbackException(Throwable cause) {
        super(cause);
    }

}
