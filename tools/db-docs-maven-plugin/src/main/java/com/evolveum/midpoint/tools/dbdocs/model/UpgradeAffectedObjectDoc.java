/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.tools.dbdocs.model;

/**
 * Documentation model for one schema object affected by an upgrade change.
 */
public final class UpgradeAffectedObjectDoc {

    private final String objectName;
    private final String changeType;
    private final String description;

    /**
     * Creates an affected-object description from an {@code @affects} annotation.
     */
    public UpgradeAffectedObjectDoc(String objectName, String changeType, String description) {
        this.objectName = objectName;
        this.changeType = changeType;
        this.description = description;
    }

    public String objectName() {
        return objectName;
    }

    public String changeType() {
        return changeType;
    }

    public String description() {
        return description;
    }
}
