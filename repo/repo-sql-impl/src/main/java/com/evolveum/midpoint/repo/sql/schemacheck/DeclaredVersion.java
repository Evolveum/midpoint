/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.schemacheck;

import org.jetbrains.annotations.NotNull;

/**
 * Version of the schema as declared in the metadata.
 */
class DeclaredVersion {

    enum State {
        /**
         * m_global_metadata table is missing (so the schema is unknown)
         */
        METADATA_TABLE_MISSING,
        /**
         * metadata table is present but the version value is not specified -- this indicates some corruption of the database
         */
        VERSION_VALUE_MISSING,
        /**
         * Everything is OK, version is present in the metadata.
         */
        VERSION_VALUE_PRESENT,
        /**
         * Version value was supplied externally (e.g. using configuration)
         */
        VERSION_VALUE_EXTERNALLY_SUPPLIED
    }

    @NotNull final State state;
    final String version;

    DeclaredVersion(@NotNull State state, String version) {
        this.state = state;
        this.version = version;
    }

    @Override
    public String toString() {
        return "DeclaredVersion{" +
                "state=" + state +
                ", version='" + version + '\'' +
                '}';
    }
}
