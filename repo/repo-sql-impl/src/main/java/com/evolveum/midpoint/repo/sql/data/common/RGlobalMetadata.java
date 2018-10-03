/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.repo.sql.helpers.modify.Ignore;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Objects;

/**
 * @author mederly
 */
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
