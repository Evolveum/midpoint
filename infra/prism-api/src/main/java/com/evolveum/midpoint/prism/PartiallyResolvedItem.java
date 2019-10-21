/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.path.ItemPath;

import java.util.Objects;

/**
 * TODO description
 *
 * @author semancik
 */
public class PartiallyResolvedItem<V extends PrismValue,D extends ItemDefinition> {

    private Item<V,D> item;
    private ItemPath residualPath;

    public PartiallyResolvedItem(Item<V,D> item, ItemPath residualPath) {
        super();
        this.item = item;
        this.residualPath = residualPath;
    }

    public Item<V,D> getItem() {
        return item;
    }

    public void setItem(Item<V,D> item) {
        this.item = item;
    }

    public ItemPath getResidualPath() {
        return residualPath;
    }

    public void setResidualPath(ItemPath residualPath) {
        this.residualPath = residualPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PartiallyResolvedItem<?, ?> that = (PartiallyResolvedItem<?, ?>) o;
        return Objects.equals(item, that.item) && Objects.equals(residualPath, that.residualPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(item, residualPath);
    }

    @Override
    public String toString() {
        return "PartiallyResolvedItem(item=" + item + ", residualPath=" + residualPath + ")";
    }

}
