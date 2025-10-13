/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
