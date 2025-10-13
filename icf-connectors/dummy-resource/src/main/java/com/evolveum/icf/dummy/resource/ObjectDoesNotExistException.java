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
public class ObjectDoesNotExistException extends Exception {

    public ObjectDoesNotExistException() {
    }

    public ObjectDoesNotExistException(String message) {
        super(message);
    }

    public ObjectDoesNotExistException(Throwable cause) {
        super(cause);
    }

    public ObjectDoesNotExistException(String message, Throwable cause) {
        super(message, cause);
    }

}
