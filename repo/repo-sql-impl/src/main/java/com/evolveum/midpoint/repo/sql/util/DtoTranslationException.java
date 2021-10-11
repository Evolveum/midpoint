/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
