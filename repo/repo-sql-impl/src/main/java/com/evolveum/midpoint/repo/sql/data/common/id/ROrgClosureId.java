/*
 * Copyright (c) 2010-2014 Evolveum
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
public class ROrgClosureId implements Serializable {

    private String ancestorOid;
    private String descendantOid;

    public String getAncestorOid() {
        return ancestorOid;
    }

    public void setAncestorOid(String ancestorOid) {
        this.ancestorOid = ancestorOid;
    }

    public String getDescendantOid() {
        return descendantOid;
    }

    public void setDescendantOid(String descendantOid) {
        this.descendantOid = descendantOid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ROrgClosureId that = (ROrgClosureId) o;

        if (ancestorOid != null ? !ancestorOid.equals(that.ancestorOid) : that.ancestorOid != null) return false;
        if (descendantOid != null ? !descendantOid.equals(that.descendantOid) : that.descendantOid != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = ancestorOid != null ? ancestorOid.hashCode() : 0;
        result = 31 * result + (descendantOid != null ? descendantOid.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ROrgClosureId{" +
                "ancestorOid='" + ancestorOid + '\'' +
                ", descendantOid='" + descendantOid + '\'' +
                '}';
    }
}
