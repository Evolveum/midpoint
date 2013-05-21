/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.repo.sql.data.common.id;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class RContainerId implements Serializable {

    private String oid;
    private Long id;

    public RContainerId() {
    }

    public RContainerId(String oid) {
        this(0L, oid);
    }

    public RContainerId(Long id, String oid) {
        this.id = id;
        this.oid = oid;
    }

    public Long getId() {
        return id;
    }

    public String getOid() {
        return oid;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RContainerId that = (RContainerId) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (oid != null ? !oid.equals(that.oid) : that.oid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = oid != null ? oid.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "RContainerId{" + oid + ", " + id + "}";
    }
}
