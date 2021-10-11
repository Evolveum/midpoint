/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.util.exception;

import com.evolveum.midpoint.util.LocalizableMessage;

/**
 * Exceptional concurrency state or operation invocation.
 *
 * This exception is thrown in case of race conditions and similar conflicting concurrency conditions.
 * It is also thrown in an attempt to acquire already acquired locks and similar cases.
 *
 * This condition is implemented as exception in a hope that it will help avoid silently ignoring the
 * concurrency problems and that the developers will be forced to handle the condition.
 * It is much easier to ignore a return value than to ignore an exception.
 *
 * @author Radovan Semancik
 *
 */
public class ConcurrencyException extends CommonException {
    private static final long serialVersionUID = 1L;

    public ConcurrencyException() {
    }

    public ConcurrencyException(String message) {
        super(message);
    }

    public ConcurrencyException(LocalizableMessage userFriendlyMessage) {
        super(userFriendlyMessage);
    }

    public ConcurrencyException(Throwable cause) {
        super(cause);
    }

    public ConcurrencyException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConcurrencyException(LocalizableMessage userFriendlyMessage, Throwable cause) {
        super(userFriendlyMessage, cause);
    }

    @Override
    public String getErrorTypeMessage() {
        return "Concurrency exception";
    }



}
