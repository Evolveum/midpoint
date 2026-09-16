/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.tools.dbdocs.model;

import java.nio.file.Path;
import java.util.List;

/**
 * Documentation model for an annotated upgrade-script change.
 */
public final class UpgradeChangeDoc {

    private final Path sourceFile;
    private final DocMetadata metadata;
    private final String changeNumber;
    private final List<UpgradeAffectedObjectDoc> affectedObjects;

    public UpgradeChangeDoc(
            Path sourceFile,
            DocMetadata metadata,
            String changeNumber,
            List<UpgradeAffectedObjectDoc> affectedObjects) {
        this.sourceFile = sourceFile;
        this.metadata = metadata != null ? metadata : DocMetadata.EMPTY;
        this.changeNumber = changeNumber;
        this.affectedObjects = List.copyOf(affectedObjects);
    }

    public Path sourceFile() {
        return sourceFile;
    }

    public DocMetadata metadata() {
        return metadata;
    }

    public String changeNumber() {
        return changeNumber;
    }

    public int numericChangeNumber() {
        if (changeNumber == null) {
            return -1;
        }

        try {
            return Integer.parseInt(changeNumber);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public List<UpgradeAffectedObjectDoc> affectedObjects() {
        return affectedObjects;
    }
}
