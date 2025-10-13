/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.repo.sql.helpers.modify.Ignore;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.Objects;

@Entity
@Ignore
public class RGlobalMetadata {

    public static final String TABLE_NAME = "m_global_metadata";
    public static final String DATABASE_SCHEMA_VERSION = "databaseSchemaVersion";

    private String name;
    private String value;

    @Id
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof RGlobalMetadata))
            return false;
        RGlobalMetadata that = (RGlobalMetadata) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value);
    }

    @Override
    public String toString() {
        return "RGlobalMetadata{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
