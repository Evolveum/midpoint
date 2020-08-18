/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.query;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.OrderDirection;

/**
 * @author mederly
 */
public final class ObjectOrderingImpl implements ObjectOrdering {

    private final ItemPath orderBy;
    private final OrderDirection direction;

    private ObjectOrderingImpl(ItemPath orderBy, OrderDirection direction) {
        if (ItemPath.isEmpty(orderBy)) {
            throw new IllegalArgumentException("Null or empty ordering path is not supported.");
        }
        this.orderBy = orderBy;
        this.direction = direction;
    }

    public static ObjectOrderingImpl createOrdering(ItemPath orderBy, OrderDirection direction) {
        return new ObjectOrderingImpl(orderBy, direction);
    }

    public ItemPath getOrderBy() {
        return orderBy;
    }

    public OrderDirection getDirection() {
        return direction;
    }

    @Override
    public String toString() {
        return orderBy.toString() + " " + direction;
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object o) {
        return equals(o, true);
    }

    public boolean equals(Object o, boolean exact) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        ObjectOrderingImpl that = (ObjectOrderingImpl) o;

        if (orderBy != null ? !orderBy.equals(that.orderBy, exact) : that.orderBy != null) {
            return false;
        }
        return direction == that.direction;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + (direction != null ? direction.hashCode() : 0);
        return result;
    }
}
