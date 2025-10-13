/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.expression;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class ExpressionSyntaxException extends SchemaException {

    public ExpressionSyntaxException() {
        super();
    }

    public ExpressionSyntaxException(String message, QName propertyName) {
        super(message, propertyName);
    }

    public ExpressionSyntaxException(String message, Throwable cause, QName propertyName) {
        super(message, cause, propertyName);
    }

    public ExpressionSyntaxException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExpressionSyntaxException(String message) {
        super(message);
    }

}
