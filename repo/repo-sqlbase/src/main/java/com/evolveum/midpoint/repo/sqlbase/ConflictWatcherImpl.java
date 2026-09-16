/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqlbase;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.ConflictWatcher;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Detects write-write conflicts on objects by tracking expected object versions, like this:
 *
 * - After successful ADD and MODIFY operation, the expected version is updated (ADD sets it; MODIFY increases it)
 * - Before MODIFY operation, it is checked that the expected version matches the current version in the repository.
 *
 * This object is local to the thread that uses it. This means that if a different thread modifies the object in the meanwhile,
 * the current version in the repository will no longer match the expected version (on MODIFY operation start) and so the
 * conflict will be detected.
 *
 * NOTE: This object does not prevent conflicting changes. It just detects them.
 */
public class ConflictWatcherImpl implements ConflictWatcher {

    /** OID of the object we are watching. */
    @NotNull private final String oid;

    /** Was this watcher initialized i.e. is expected version set? */
    private boolean initialized;

    /** Was a conflict already detected? */
    private boolean hasConflict;

    /** What is the expected version of an object? Meaningful only if {@link #initialized} is {@code true}. */
    private int expectedVersion;

    /** Was the object deleted? This skips all future checks. */
    private boolean objectDeleted;

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
        if (initialized) {
            if (current != expectedVersion) {
                hasConflict = true;
            }
        } else {
            initialized = true;
            expectedVersion = current;
        }
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
