/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.prism.xml.ns._public.types_3;

import java.io.Serializable;
import java.util.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.JaxbVisitable;
import com.evolveum.midpoint.prism.JaxbVisitor;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.util.CloneUtil;

/**
 * Experimental.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ObjectDeltaObjectType", propOrder = {
        "oldObject",
        "delta",
        "newObject"
})
public class ObjectDeltaObjectType implements Serializable, JaxbVisitable {

    protected ObjectType oldObject;
    protected ObjectDeltaType delta;
    protected ObjectType newObject;

    public static final QName COMPLEX_TYPE = new QName(PrismConstants.NS_TYPES, "ObjectDeltaObjectType");
    public static final QName F_OLD_OBJECT = new QName(PrismConstants.NS_TYPES, "oldObject");
    public static final QName F_DELTA = new QName(PrismConstants.NS_TYPES, "delta");
    public static final QName F_NEW_OBJECT = new QName(PrismConstants.NS_TYPES, "newObject");

    public ObjectType getOldObject() {
        return oldObject;
    }

    public void setOldObject(ObjectType oldObject) {
        this.oldObject = oldObject;
    }

    public ObjectDeltaType getDelta() {
        return delta;
    }

    public void setDelta(ObjectDeltaType delta) {
        this.delta = delta;
    }

    public ObjectType getNewObject() {
        return newObject;
    }

    public void setNewObject(ObjectType newObject) {
        this.newObject = newObject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof ObjectDeltaObjectType)) { return false; }
        ObjectDeltaObjectType that = (ObjectDeltaObjectType) o;
        return Objects.equals(oldObject, that.oldObject) &&
                Objects.equals(delta, that.delta) &&
                Objects.equals(newObject, that.newObject);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oldObject, delta, newObject);
    }

    @Override
    public void accept(JaxbVisitor visitor) {
        visitor.visit(this);
        if (oldObject != null) {
            oldObject.accept(visitor);
        }
        if (delta != null) {
            delta.accept(visitor);
        }
        if (newObject != null) {
            newObject.accept(visitor);
        }
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public ObjectDeltaObjectType clone() {
        ObjectDeltaObjectType clone = new ObjectDeltaObjectType();
        if (oldObject != null) {
            clone.setOldObject(CloneUtil.clone(oldObject));
        }
        if (delta != null) {
            clone.setDelta(delta.clone());
        }
        if (newObject != null) {
            clone.setOldObject(CloneUtil.clone(newObject));
        }
        return clone;
    }
}
