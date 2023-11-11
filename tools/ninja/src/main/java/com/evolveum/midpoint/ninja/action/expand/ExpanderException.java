/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.expand;

public class ExpanderException extends RuntimeException {

    public ExpanderException(String message) {
        super(message);
    }

    public ExpanderException(String message, Throwable cause) {
        super(message, cause);
    }
}
