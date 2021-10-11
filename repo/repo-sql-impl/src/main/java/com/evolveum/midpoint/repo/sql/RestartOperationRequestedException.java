/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql;

/**
 *
 */
public class RestartOperationRequestedException extends SerializationRelatedException {

    private final boolean forbidNoFetchExtensionValueAddition;

    public RestartOperationRequestedException(String message) {
        super(message);
        forbidNoFetchExtensionValueAddition = false;
    }

    public RestartOperationRequestedException(String message, boolean forbidNoFetchExtensionValueAddition) {
        super(message);
        this.forbidNoFetchExtensionValueAddition = forbidNoFetchExtensionValueAddition;
    }

    public boolean isForbidNoFetchExtensionValueAddition() {
        return forbidNoFetchExtensionValueAddition;
    }
}
