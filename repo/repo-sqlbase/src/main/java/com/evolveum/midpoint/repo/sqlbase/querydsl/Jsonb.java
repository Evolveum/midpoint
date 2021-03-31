/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.querydsl;

/**
 * Type representing JSONB columns in PostgreSQL database as a wrapped string.
 */
public class Jsonb {
    public final String value;

    public Jsonb(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "JSONB " + value;
    }
}
