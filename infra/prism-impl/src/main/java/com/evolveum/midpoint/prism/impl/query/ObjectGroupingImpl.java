/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.query;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectGrouping;

/**
 * @author acope
 */
public class ObjectGroupingImpl implements ObjectGrouping {

    private final ItemPath groupBy;

    ObjectGroupingImpl(ItemPath groupBy) {
        if (ItemPath.isEmpty(groupBy)) {
            throw new IllegalArgumentException("Null or empty groupBy path is not supported.");
        }
        this.groupBy = groupBy;
    }

    public static ObjectGroupingImpl createGrouping(ItemPath groupBy) {
        return new ObjectGroupingImpl(groupBy);
    }

    public ItemPath getGroupBy() {
        return groupBy;
    }

    @Override
    public String toString() {
        return groupBy.toString();
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass") // it does, actually
    @Override
    public boolean equals(Object o) {
        return equals(o, true);
    }

    public boolean equals(Object o, boolean exact) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        ObjectGroupingImpl that = (ObjectGroupingImpl) o;
        if (groupBy != null ? !groupBy.equals(that.groupBy, exact) : that.groupBy != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return groupBy.hashCode();
    }
}
