/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
