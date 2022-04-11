/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.schemacheck;

import org.jetbrains.annotations.NotNull;

/**
 * Current state of the database schema as determined by the SchemaChecker.
 */
class SchemaState {

    /**
     * Compliance of the data structure. (Not regarding schema version in the metadata.)
     */
    @NotNull final DataStructureCompliance dataStructureCompliance;

    /**
     * Version as declared in the metadata.
     */
    @NotNull final DeclaredVersion declaredVersion;

    SchemaState(@NotNull DataStructureCompliance dataStructureCompliance, @NotNull DeclaredVersion declaredVersion) {
        this.dataStructureCompliance = dataStructureCompliance;
        this.declaredVersion = declaredVersion;
    }

    @Override
    public String toString() {
        return "SchemaState{" +
                "dataStructureCompliance=" + dataStructureCompliance +
                ", declaredVersion=" + declaredVersion +
                '}';
    }
}
