/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.icf.dummy.resource;

/**
 * @author semancik
 *
 */
public class ObjectAlreadyExistsException extends Exception {

    public ObjectAlreadyExistsException() {
        super();
    }

    public ObjectAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }

    public ObjectAlreadyExistsException(String message) {
        super(message);
    }

    public ObjectAlreadyExistsException(Throwable cause) {
        super(cause);
    }

}
