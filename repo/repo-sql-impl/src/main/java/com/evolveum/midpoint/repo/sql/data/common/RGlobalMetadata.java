/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.repo.sql.helpers.modify.Ignore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Ignore
@Table(name = RGlobalMetadata.TABLE_NAME)
public class RGlobalMetadata {

    public static final String TABLE_NAME = "m_global_metadata";
    public static final String DATABASE_SCHEMA_VERSION = "databaseSchemaVersion";

    private String name;
    private String value;

    @Id
    @Column(name = "name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(name = "value")
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
