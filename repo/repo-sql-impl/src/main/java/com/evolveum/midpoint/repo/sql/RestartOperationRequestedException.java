/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
