/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.impl;

import com.evolveum.midpoint.provisioning.api.LiveSyncToken;
import com.evolveum.midpoint.provisioning.api.LiveSyncTokenStorage;
import com.evolveum.midpoint.schema.result.OperationResult;

import static org.assertj.core.api.Assertions.assertThat;

public class DummyTokenStorageImpl implements LiveSyncTokenStorage {

    private LiveSyncToken currentToken;

    public DummyTokenStorageImpl() {
    }

    public DummyTokenStorageImpl(Object currentToken) {
        this.currentToken = LiveSyncToken.of(currentToken);
    }

    @Override
    public LiveSyncToken getToken() {
        return currentToken;
    }

    @Override
    public void setToken(LiveSyncToken token, OperationResult result) {
        this.currentToken = token;
    }

    public void assertToken(Object expected) {
        assertThat(LiveSyncToken.getValue(currentToken))
                .as("current token value")
                .isEqualTo(expected);
    }
}
