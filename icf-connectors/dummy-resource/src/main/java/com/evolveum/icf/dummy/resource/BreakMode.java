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
public enum BreakMode {

    NONE,
    NETWORK,
    IO,
    SCHEMA,
    CONFLICT, // results in AlreadyExists exceptions
    GENERIC,
    UNSUPPORTED,
    RUNTIME,
    ASSERTION_ERROR;

}
