/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.api;

/**
 * Generic indistinguishable error of a connector framework.
 *
 * Please do not use this exception if possible!
 * Only errors that cannot be categorized or that are not
 * expected at all should use this exception.
 *
 * This is RUNTIME exception. As this error is generic and
 * we cannot distinguish any details, there is no hope that
 * an interface client can do anything with it. So don't even
 * bother catching it.
 *
 * @author Radovan Semancik
 */
public class GenericConnectorException extends RuntimeException {
    private static final long serialVersionUID = 4718501022689239025L;

    public GenericConnectorException() {
    }

    public GenericConnectorException(String message) {
        super(message);
    }

    public GenericConnectorException(Throwable cause) {
        super(cause);
    }

    public GenericConnectorException(String message, Throwable cause) {
        super(message, cause);
    }

}
