/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.path.ItemPath;

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
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((item == null) ? 0 : item.hashCode());
        result = prime * result + ((residualPath == null) ? 0 : residualPath.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PartiallyResolvedItem other = (PartiallyResolvedItem) obj;
        if (item == null) {
            if (other.item != null)
                return false;
        } else if (!item.equals(other.item))
            return false;
        if (residualPath == null) {
            if (other.residualPath != null)
                return false;
        } else if (!residualPath.equivalent(other.residualPath))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "PartiallyResolvedItem(item=" + item + ", residualPath=" + residualPath + ")";
    }

}
