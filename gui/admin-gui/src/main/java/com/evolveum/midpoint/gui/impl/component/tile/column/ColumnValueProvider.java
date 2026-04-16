/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.tile.column;

import java.io.Serializable;
import java.util.List;

/**
 * Provides values for column-based UI components, mainly used by {@code ColumnTileTable}.
 *
 * <p>Supports both single-valued and multivalued data representations.
 * By default, a single value is wrapped into a list, but implementations
 * can override {@link #getColumnsValues()} to provide multiple values
 * (e.g. for grouped tile scenarios).</p>
 *
 * <p>Additional helper methods allow defining grouping, display name,
 * expansion state, and item count for UI rendering.</p>
 *
 * @param <PV> type of value provided for columns
 */
public interface ColumnValueProvider<PV extends Serializable> extends Serializable {

    PV getColumnValue();

    //In case of multivalued object
    default List<PV> getColumnsValues() {
        PV value = getColumnValue();
        return value != null ? List.of(value) : List.of();
    }

    default String getKeyValue(){
       return "";
    }

    default String getGroupDisplayName(){
        return "";
    }

    default Integer getCount() {
        return getColumnsValues().size();
    }

    default boolean isExpanded() {
        return false;
    }

    default void setExpanded(boolean expanded) {
    }
}
