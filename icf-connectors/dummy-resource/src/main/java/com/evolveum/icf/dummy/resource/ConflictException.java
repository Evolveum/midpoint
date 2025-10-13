/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.icf.dummy.resource;

/**
 * @author semancik
 *
 */
public class ConflictException extends Exception {

    public ConflictException() {
        super();
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(Throwable cause) {
        super(cause);
    }

}
