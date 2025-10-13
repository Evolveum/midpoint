/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.audit.api;

/**
 * @author lazyman
 */
public class AuditServiceFactoryException extends Exception {

    public AuditServiceFactoryException(String s) {
        super(s);
    }

    public AuditServiceFactoryException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
