/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.query.definition;

/**
 * In current version this is just a marker class - there's nothing to be said about standard collections yet.
 */
public class CollectionSpecification {

    public String toString() {
        return "StdCol";
    }

    public String getShortInfo() {
        return "[]";
    }
}
