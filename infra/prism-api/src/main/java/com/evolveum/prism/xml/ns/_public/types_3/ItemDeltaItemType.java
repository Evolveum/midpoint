/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */


package com.evolveum.prism.xml.ns._public.types_3;

import com.evolveum.midpoint.prism.JaxbVisitable;
import com.evolveum.midpoint.prism.JaxbVisitor;
import com.evolveum.midpoint.prism.Raw;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * THIS IS NOT A GENERATED CLASS.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ItemDeltaItemType", propOrder = {
    "oldItem",
    "delta",
    "newItem"
})
public class ItemDeltaItemType implements Serializable, Cloneable, JaxbVisitable {

	public static final QName COMPLEX_TYPE = new QName("http://prism.evolveum.com/xml/ns/public/types-3", "ItemDeltaItemType");
	public static final QName F_OLD_ITEM = new QName("http://prism.evolveum.com/xml/ns/public/types-3", "oldItem");
	public static final QName F_DELTA = new QName("http://prism.evolveum.com/xml/ns/public/types-3", "delta");
	public static final QName F_NEW_ITEM = new QName("http://prism.evolveum.com/xml/ns/public/types-3", "newItem");

    @XmlElement
    private ItemType oldItem;

    @XmlElement
    @NotNull
    private final List<ItemDeltaType> delta = new ArrayList<>();

    @XmlElement
    private ItemType newItem;

    public ItemType getOldItem() {
        return oldItem;
    }

    public void setOldItem(ItemType oldItem) {
        this.oldItem = oldItem;
    }

    @NotNull
    public List<ItemDeltaType> getDelta() {
        return delta;
    }

    public ItemType getNewItem() {
        return newItem;
    }

    public void setNewItem(ItemType newItem) {
        this.newItem = newItem;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ItemDeltaItemType))
            return false;
        ItemDeltaItemType that = (ItemDeltaItemType) o;
        return Objects.equals(oldItem, that.oldItem) &&
                delta.equals(that.delta) &&
                Objects.equals(newItem, that.newItem);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oldItem, delta, newItem);
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public ItemDeltaItemType clone() {
        ItemDeltaItemType clone = new ItemDeltaItemType();
        if (oldItem != null) {
            clone.oldItem = oldItem.clone();
        }
        for (ItemDeltaType d : delta) {
            if (d != null) {
                clone.delta.add(d.clone());
            }
        }
        if (newItem != null) {
            clone.newItem = newItem.clone();
        }
        return clone;
    }

    @Override
    public String toString() {
        return "ItemDeltaItemType{" +
                "oldItem=" + oldItem +
                ", delta=" + delta +
                ", newItem=" + newItem +
                '}';
    }

    @Override
	public void accept(JaxbVisitor visitor) {
		visitor.visit(this);
		if (oldItem != null) {
			oldItem.accept(visitor);
		}
        for (ItemDeltaType d : delta) {
            if (d != null) {
                visitor.visit(d);
            }
        }
		if (newItem != null) {
			newItem.accept(visitor);
		}
	}
}
