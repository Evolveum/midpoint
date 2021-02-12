/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sqlbase;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.ConflictWatcher;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author mederly
 */
public class ConflictWatcherImpl implements ConflictWatcher {

    @NotNull private final String oid;
    private boolean initialized;
    private boolean hasConflict;
    private int expectedVersion;
    private boolean objectDeleted;              // skip all future checks

    public ConflictWatcherImpl(@NotNull String oid) {
        this.oid = oid;
    }

    public <T extends ObjectType> void afterAddObject(@NotNull String oid, @NotNull PrismObject<T> object) {
        if (notRelevant(oid)) {
            return;
        }
        String version = object.getVersion();
        if (version == null) {
            throw new IllegalStateException("No version in object " + object);
        }
        expectedVersion = Integer.parseInt(version);
        initialized = true;
        //System.out.println(Thread.currentThread().getName() + ": afterAddObject: " + this);
    }

    public void afterDeleteObject(String oid) {
        if (this.oid.equals(oid)) {
            objectDeleted = true;
        }
    }

    public <T extends ObjectType> void beforeModifyObject(PrismObject<T> object) {
        if (notRelevant(object.getOid())) {
            return;
        }
        checkExpectedVersion(object.getVersion());
    }

    public void afterModifyObject(String oid) {
        if (notRelevant(oid)) {
            return;
        }
        expectedVersion++;
        //System.out.println(Thread.currentThread().getName() + ": afterModifyObject: " + this);
    }

    public void afterGetVersion(String oid, String currentRepoVersion) {
        if (notRelevant(oid)) {
            return;
        }
        checkExpectedVersion(currentRepoVersion);
    }

    public <T extends ObjectType> void afterGetObject(PrismObject<T> object) {
        if (notRelevant(object.getOid())) {
            return;
        }
        checkExpectedVersion(object.getVersion());
    }

    private boolean notRelevant(@NotNull String oid) {
        return objectDeleted || hasConflict || !this.oid.equals(oid);
    }

    private void checkExpectedVersion(@NotNull String currentRepoVersion) {
        int current = Integer.parseInt(currentRepoVersion);
        //System.out.println(Thread.currentThread().getName() + ": checkExpectedVersion: current=" + current + " in " + this);
        if (initialized) {
            if (current != expectedVersion) {
                hasConflict = true;
            }
        } else {
            initialized = true;
            expectedVersion = current;
        }
        //System.out.println(Thread.currentThread().getName() + ": checkExpectedVersion finishing: current=" + current + " in " + this);
    }

    @Override
    @NotNull
    public String getOid() {
        return oid;
    }

    @Override
    public boolean hasConflict() {
        return hasConflict;
    }

    // for testing purposes
    public boolean isInitialized() {
        return initialized;
    }

    // for testing purposes
    public int getExpectedVersion() {
        return expectedVersion;
    }

    // for testing purposes
    public boolean isObjectDeleted() {
        return objectDeleted;
    }

    @Override
    public void setExpectedVersion(String version) {
        if (initialized) {
            throw new IllegalStateException("Already initialized: " + this);
        }
        if (StringUtils.isNotEmpty(version)) {
            initialized = true;
            expectedVersion = Integer.parseInt(version);
        }
    }

    @Override
    public String toString() {
        return "ConflictWatcherImpl{" +
                "oid='" + oid + '\'' +
                ", initialized=" + initialized +
                ", expectedVersion=" + expectedVersion +
                ", hasConflict=" + hasConflict +
                ", objectDeleted=" + objectDeleted +
                '}';
    }
}
