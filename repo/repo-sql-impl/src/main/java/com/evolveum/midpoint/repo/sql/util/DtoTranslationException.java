/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.util;

/**
 * @author lazyman
 */
public class DtoTranslationException extends Exception {

    public DtoTranslationException(String message) {
        super(message);
    }

    public DtoTranslationException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
