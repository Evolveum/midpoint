/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.schemacheck;

import org.jetbrains.annotations.NotNull;

/**
 * Compliance of the database structure. (Not regarding schema version in the metadata.)
 */
class DataStructureCompliance {

    enum State {
        /**
         * State is fully compliant (verified by hibernate).
         */
        COMPLIANT,
        /**
         * The database is empty - no tables present.
         */
        NO_TABLES,
        /**
         *  Some tables exist but the schema is not compliant.
         */
        NOT_COMPLIANT
    }

    /**
     * State of the tables.
     */
    @NotNull final State state;

    /**
     * Exception that was the result of the validation.
     */
    final Exception validationException;

    DataStructureCompliance(@NotNull State state, Exception validationException) {
        this.state = state;
        this.validationException = validationException;
    }

    @Override
    public String toString() {
        return "DataStructureCompliance{" +
                "state=" + state +
                ", exception=" + validationException +
                '}';
    }
}
