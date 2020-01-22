/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger;

/**
 * Created by Viliam Repan (lazyman).
 */
public class SchrodingerException extends RuntimeException {

    public SchrodingerException(String message) {
        super(message);
    }

    public SchrodingerException(String message, Throwable cause) {
        super(message, cause);
    }

    public SchrodingerException(Throwable cause) {
        super(cause);
    }
}
