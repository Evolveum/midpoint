/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.util.exception;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.LocalizableMessage;

/**
 * Error regarding schema.
 *
 * E.g. Object class violation, missing object class, inconsistent schema, etc.
 *
 * @author Radovan Semancik
 *
 */
public class SchemaException extends CommonException {
    private static final long serialVersionUID = -6016220825724355014L;

    private QName propertyName;

    public SchemaException() {
        super();
    }

    public SchemaException(String message, Throwable cause) {
        super(message, cause);
    }

    public SchemaException(LocalizableMessage userFriendlyMessage, Throwable cause) {
        super(userFriendlyMessage, cause);
    }

    public SchemaException(String message, Throwable cause, QName propertyName) {
        super(message, cause);
        this.propertyName = propertyName;
    }

    public SchemaException(String message) {
        super(message);
    }

    public SchemaException(LocalizableMessage userFriendlyMessage) {
        super(userFriendlyMessage);
    }

    public SchemaException(String message, QName propertyName) {
        super(message);
        this.propertyName = propertyName;
    }

    @Override
    public String getErrorTypeMessage() {
        return "Schema problem";
    }

    public QName getPropertyName() {
        return propertyName;
    }

}
