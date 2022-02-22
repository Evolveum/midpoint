/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
