/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.impl;

/**
 * Created by Viliam Repan (lazyman).
 */
public class NinjaException extends RuntimeException {

    public NinjaException(String message) {
        super(message);
    }

    public NinjaException(String message, Throwable cause) {
        super(message, cause);
    }
}
