/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql;

public class SerializationRelatedException extends RuntimeException {

    public SerializationRelatedException(String message) {
        super(message);
    }

    public SerializationRelatedException(Throwable ex) {
        super(ex);
    }
}
