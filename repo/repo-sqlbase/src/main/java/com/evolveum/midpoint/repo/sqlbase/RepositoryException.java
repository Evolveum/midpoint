/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase;

import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.util.exception.CommonException;

/**
 * Base repository exception.
 */
public class RepositoryException extends CommonException {

    public RepositoryException(String message) {
        super(message);
    }

    public RepositoryException(LocalizableMessage localizableMessage) {
        super(localizableMessage);
    }

    public RepositoryException(Throwable cause) {
        super(cause);
    }

    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getErrorTypeMessage() {
        return "Repository error";
    }
}
