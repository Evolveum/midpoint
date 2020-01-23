/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.prism.xml.ns._public.types_3;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.util.CloneUtil;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *  Experimental.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ItemType", propOrder = {
    "name",
    "value"
})
public class ItemType implements Serializable, JaxbVisitable {

    protected QName name;

    @Raw
    protected final List<Object> value = new ArrayList<>();

    public final static QName COMPLEX_TYPE = new QName(PrismConstants.NS_TYPES, "ItemType");
    public final static QName F_NAME = new QName(PrismConstants.NS_TYPES, "name");
    public final static QName F_VALUE = new QName(PrismConstants.NS_TYPES, "value");

    public QName getName() {
        return name;
    }

    public void setName(QName name) {
        this.name = name;
    }

    public List<Object> getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ItemType))
            return false;
        ItemType itemType = (ItemType) o;
        return Objects.equals(name, itemType.name) &&
                Objects.equals(getValue(), itemType.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, getValue());
    }

    @Override
    public void accept(JaxbVisitor visitor) {
        visitor.visit(this);
        for (Object o : getValue()) {
            if (o instanceof JaxbVisitable) {
                visitor.visit((JaxbVisitable) o);
            }
        }
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public ItemType clone() {
        ItemType clone = new ItemType();
        clone.name = name;
        for (Object v : getValue()) {
            clone.getValue().add(CloneUtil.clone(v));
        }
        return clone;
    }

    @Override
    public String toString() {
        return "ItemType{" +
                "name=" + name +
                ", value=" + value +
                '}';
    }

    public static ItemType fromItem(Item<?, ?> item) {
        if (item != null) {
            ItemType rv = new ItemType();
            rv.setName(item.getElementName());
            if (item instanceof PrismContainer && (item.getDefinition() == null || ((PrismContainerDefinition) item.getDefinition()).getCompileTimeClass() == null)) {
                // a special case -- item.getRealValues() does not work here (TODO generalize this)
                for (PrismContainerValue<?> value : ((PrismContainer<?>) item).getValues()) {
                    rv.value.add(new RawType(value, null, item.getPrismContext()));
                }
            } else {
                rv.value.addAll(item.getRealValues());
            }
            return rv;
        } else {
            return null;
        }
    }
}
