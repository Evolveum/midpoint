/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
