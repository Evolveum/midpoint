/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.connid;

/**
 * This exception exists only for the purpose of passing checked exceptions where they are not allowed.
 * This comes handy e.g. in callbacks. This exception must never be shown to "outside".
 *
 * @author Radovan Semancik
 *
 */
public class IntermediateException extends RuntimeException {

    public IntermediateException() {
        super();
    }

    public IntermediateException(String message, Throwable cause) {
        super(message, cause);
    }

    public IntermediateException(String message) {
        super(message);
    }

    public IntermediateException(Throwable cause) {
        super(cause);
    }

}
