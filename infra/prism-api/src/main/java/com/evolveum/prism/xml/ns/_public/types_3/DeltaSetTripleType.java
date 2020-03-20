/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.prism.xml.ns._public.types_3;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.util.CloneUtil;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *  Experimental.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DeltaSetTripleType", propOrder = {
    "zero",
    "plus",
    "minus"
})
public class DeltaSetTripleType implements Serializable, JaxbVisitable, Cloneable {

    @XmlElement
    @Raw
    private final List<Object> zero = new ArrayList<>();

    @XmlElement
    @Raw
    private final List<Object> plus = new ArrayList<>();

    @XmlElement
    @Raw
    private final List<Object> minus = new ArrayList<>();

    public final static QName COMPLEX_TYPE = new QName(PrismConstants.NS_TYPES, "DeltaSetTripleType");
    public final static QName F_ZERO = new QName(PrismConstants.NS_TYPES, "zero");
    public final static QName F_PLUS = new QName(PrismConstants.NS_TYPES, "plus");
    public final static QName F_MINUS = new QName(PrismConstants.NS_TYPES, "minus");

    public List<Object> getZero() {
        return zero;
    }

    public List<Object> getPlus() {
        return plus;
    }

    public List<Object> getMinus() {
        return minus;
    }

    @Override
    public void accept(JaxbVisitor visitor) {
        visitor.visit(this);
        visit(zero, visitor);
        visit(plus, visitor);
        visit(minus, visitor);
    }

    public void visit(List<Object> set, JaxbVisitor visitor) {
        for (Object o : set) {
            if (o instanceof JaxbVisitable) {
                visitor.visit((JaxbVisitable) o);
            }
        }
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public DeltaSetTripleType clone() {
        DeltaSetTripleType clone = new DeltaSetTripleType();
        for (Object v : zero) {
            clone.zero.add(CloneUtil.clone(v));
        }
        for (Object v : plus) {
            clone.plus.add(CloneUtil.clone(v));
        }
        for (Object v : minus) {
            clone.minus.add(CloneUtil.clone(v));
        }
        return clone;
    }

    @Override
    public String toString() {
        return "DeltaSetTripleType{" +
                "zero=" + zero +
                ", plus=" + plus +
                ", minus=" + minus +
                '}';
    }

    public static <V extends PrismValue> DeltaSetTripleType fromDeltaSetTriple(PrismValueDeltaSetTriple<V> triple, PrismContext prismContext) {
        DeltaSetTripleType rv = new DeltaSetTripleType();
        for (V v : triple.getZeroSet()) {
            rv.zero.add(v.getRealValueOrRawType(prismContext));
        }
        for (V v : triple.getPlusSet()) {
            rv.plus.add(v.getRealValueOrRawType(prismContext));
        }
        for (V v : triple.getMinusSet()) {
            rv.minus.add(v.getRealValueOrRawType(prismContext));
        }
        return rv;
    }
}
