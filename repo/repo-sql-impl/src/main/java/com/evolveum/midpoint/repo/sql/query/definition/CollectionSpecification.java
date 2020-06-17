/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.definition;

/**
 * In current version this is just a marker class - there's nothing to be said about standard collections yet.
 *
 * @author mederly
 */
public class CollectionSpecification {

    public String toString() {
        return "StdCol";
    }

    public String getShortInfo() {
        return "[]";
    }
}
