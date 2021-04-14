/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.util.exception;

import com.evolveum.midpoint.util.LocalizableMessage;

/**
 * Object with specified criteria (OID) has not been found in the repository.
 *
 * @author Radovan Semancik
 */
public class ObjectNotFoundException extends CommonException {
    private static final long serialVersionUID = -9003686713018111855L;

    private String oid = null;

    public ObjectNotFoundException() {
        super();
    }

    public ObjectNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ObjectNotFoundException(LocalizableMessage userFriendlyMessage, Throwable cause) {
        super(userFriendlyMessage, cause);
    }

    public ObjectNotFoundException(String message, Throwable cause, String oid) {
        super(message, cause);
        this.oid = oid;
    }

    public ObjectNotFoundException(String message) {
        super(message);
    }

    public ObjectNotFoundException(LocalizableMessage userFriendlyMessage) {
        super(userFriendlyMessage);
    }

    public ObjectNotFoundException(Class<?> type, String oid) {
        this("Object of type '" + type.getSimpleName() + "' with OID '" + oid + "' was not found.",
                oid);
    }

    public ObjectNotFoundException(String message, String oid) {
        super(message);
        this.oid = oid;
    }

    public ObjectNotFoundException(Throwable cause) {
        super(cause);
    }

    public String getOid() {
        return oid;
    }

    @Override
    public String getErrorTypeMessage() {
        return "Object not found";
    }
}
