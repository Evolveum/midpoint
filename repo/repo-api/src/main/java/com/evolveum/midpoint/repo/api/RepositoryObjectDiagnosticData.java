/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.api;

public class RepositoryObjectDiagnosticData {

    private long storedObjectSize;

    public RepositoryObjectDiagnosticData(long storedObjectSize) {
        this.storedObjectSize = storedObjectSize;
    }

    public long getStoredObjectSize() {
        return storedObjectSize;
    }

    public void setStoredObjectSize(long storedObjectSize) {
        this.storedObjectSize = storedObjectSize;
    }
}
