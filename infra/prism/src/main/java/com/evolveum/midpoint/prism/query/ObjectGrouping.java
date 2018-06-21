/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.prism.query;

import com.evolveum.midpoint.prism.path.ItemPath;

import java.io.Serializable;

/**
 * @author acope
 */
public class ObjectGrouping implements Serializable {

    final private ItemPath groupBy;

    ObjectGrouping(ItemPath groupBy) {
        if (ItemPath.isNullOrEmpty(groupBy)) {
            throw new IllegalArgumentException("Null or empty groupBy path is not supported.");
        }
        this.groupBy = groupBy;
    }

    public static ObjectGrouping createGrouping(ItemPath groupBy) {
        return new ObjectGrouping(groupBy);
    }

    public ItemPath getGroupBy() {
        return groupBy;
    }


    @Override
    public String toString() {
        return groupBy.toString();
    }

    @Override
    public boolean equals(Object o) {
        return equals(o, true);
    }

    public boolean equals(Object o, boolean exact) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        ObjectGrouping that = (ObjectGrouping) o;

        if (groupBy != null ? !groupBy.equals(that.groupBy, exact) : that.groupBy != null)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        return groupBy.hashCode();
    }
}
