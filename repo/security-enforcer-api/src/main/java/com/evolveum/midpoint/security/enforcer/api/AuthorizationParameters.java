/*
 * Copyright (c) 2017-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.security.enforcer.api;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrderConstraintsType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asPrismObject;

/**
 * Object-related authorization parameters. The traditional ones.
 *
 * @author semancik
 */
public class AuthorizationParameters<O extends ObjectType, T extends ObjectType>
        implements AbstractAuthorizationParameters {

    public static final AuthorizationParameters<ObjectType,ObjectType> EMPTY =
            new AuthorizationParameters<>(null, null, null, null);

    // ODO specifies authorization object with delta
    private final ObjectDeltaObject<O> odo;
    private final PrismObject<T> target;
    private final QName relation;
    private final List<OrderConstraintsType> orderConstraints;

    private AuthorizationParameters(
            ObjectDeltaObject<O> odo, PrismObject<T> target, QName relation, List<OrderConstraintsType> orderConstraints) {
        this.odo = odo;
        this.target = target;
        this.relation = relation;
        this.orderConstraints = orderConstraints;
    }

    /** Just a convenience method. */
    public static <O extends ObjectType> @NotNull AuthorizationParameters<O, ObjectType> forObject(@Nullable O object) {
        return AuthorizationParameters.Builder.buildObject(asPrismObject(object));
    }

    public ObjectDeltaObject<O> getOdo() {
        return odo;
    }

    public PrismObject<O> getOldObject() {
        if (odo == null) {
            return null;
        }
        return odo.getOldObject();
    }

    public PrismObject<O> getNewObject() {
        if (odo == null) {
            return null;
        }
        return odo.getNewObject();
    }

    public PrismObjectValue<O> getValue() {
        PrismObject<O> object = getAnyObject();
        return object != null ? object.getValue() : null;
    }

    public PrismObject<O> getAnyObject() {
        if (odo == null) {
            return null;
        }
        return odo.getAnyObject();
    }

    public ObjectDelta<O> getDelta() {
        if (odo == null) {
            return null;
        }
        return odo.getObjectDelta();
    }

    public boolean hasDelta() {
        return odo != null && odo.getObjectDelta() != null;
    }

    public PrismObject<T> getTarget() {
        return target;
    }

    public QName getRelation() {
        return relation;
    }

    public List<OrderConstraintsType> getOrderConstraints() {
        return orderConstraints;
    }

    @Override
    public String toString() {
        return "AuthorizationParameters(odo=" + odo + ", target=" + target
                + ", relation=" + relation + ", orderConstraints=" + orderConstraints + ")";
    }

    @Override
    public void shortDump(StringBuilder sb) {
        // slower than plain StringBuilder but less magical and error-prone w.r.t comma handling
        List<String> elements = new ArrayList<>();
        elements.add(shortDumpOdo());
        shortDumpElement(elements, "target", target);
        shortDumpElement(elements, "relation", relation);
        if (orderConstraints != null) {
            StringBuilder sb1 = new StringBuilder("orderConstraints=");
            SchemaDebugUtil.shortDumpOrderConstraintsList(sb1, orderConstraints);
            elements.add(sb1.toString());
        }
        sb.append(
                String.join(", ", elements));
    }

    private String shortDumpOdo() {
        if (odo == null) {
            return "no odo";
        } else {
            return "odo=(" + odo.getOldObject() + ", " + odo.getObjectDelta() + ", " + odo.getNewObject() + ")";
        }
    }

    private void shortDumpElement(List<String> elements, String label, Object o) {
        if (o != null) {
            elements.add(label + "=" + o);
        }
    }

    public static class Builder<O extends ObjectType, T extends ObjectType> {
        private ObjectDeltaObject<O> odo;
        private PrismObject<O> newObject;
        private ObjectDelta<O> delta;
        private PrismObject<O> oldObject;
        private PrismObject<T> target;
        private QName relation;
        private List<OrderConstraintsType> orderConstraints;

        public Builder<O,T> newObject(PrismObject<O> object) {
            if (odo != null) {
                throw new IllegalArgumentException("Odo already set, cannot set object");
            }
            this.newObject = object;
            return this;
        }

        public Builder<O,T> delta(ObjectDelta<O> delta) {
            if (odo != null) {
                throw new IllegalArgumentException("Odo already set, cannot set delta");
            }
            this.delta = delta;
            return this;
        }

        public Builder<O,T> oldObject(PrismObject<O> object) {
            if (odo != null) {
                throw new IllegalArgumentException("Odo already set, cannot set object");
            }
            this.oldObject = object;
            return this;
        }

        public Builder<O,T> odo(ObjectDeltaObject<O> odo) {
            if (oldObject != null) {
                throw new IllegalArgumentException("Old object already set, cannot set ODO");
            }
            if (delta != null) {
                throw new IllegalArgumentException("Delta object already set, cannot set ODO");
            }
            if (newObject != null) {
                throw new IllegalArgumentException("New object already set, cannot set ODO");
            }
            this.odo = odo;
            return this;
        }

        public Builder<O,T> target(PrismObject<T> target) {
            this.target = target;
            return this;
        }

        public Builder<O,T> relation(QName relation) {
            this.relation = relation;
            return this;
        }

        public Builder<O,T> orderConstraints(List<OrderConstraintsType> orderConstraints) {
            this.orderConstraints = orderConstraints;
            return this;
        }

        public AuthorizationParameters<O,T> build() throws SchemaException {
            if (odo == null) {
                if (oldObject == null && delta == null && newObject == null) {
                    return new AuthorizationParameters<>(null, target, relation, orderConstraints);
                } else {
                    // Non-null content, definition can be determined in ObjectDeltaObject constructor
                    ObjectDeltaObject<O> odo = new ObjectDeltaObject<>(oldObject, delta, newObject, null);
                    odo.recomputeIfNeeded(false);
                    return new AuthorizationParameters<>(odo, target, relation, orderConstraints);
                }
            } else {
                return new AuthorizationParameters<>(odo, target, relation, orderConstraints);
            }
        }

        public static <O extends ObjectType> AuthorizationParameters<O,ObjectType> buildObjectAdd(PrismObject<O> object) {
            // TODO: Do we need to create delta here?
            ObjectDeltaObject<O> odo = null;
            if (object != null) {
                odo = new ObjectDeltaObject<>(null, null, object, object.getDefinition());
            }
            return new AuthorizationParameters<>(odo, null, null, null);
        }

        public static <O extends ObjectType> AuthorizationParameters<O,ObjectType> buildObjectDelete(PrismObject<O> object) {
            // TODO: Do we need to create delta here?
            ObjectDeltaObject<O> odo = null;
            if (object != null) {
                odo = new ObjectDeltaObject<>(object, null, null, object.getDefinition());
            }
            return new AuthorizationParameters<>(odo, null, null, null);
        }

        public static <O extends ObjectType> AuthorizationParameters<O,ObjectType> buildObjectDelta(PrismObject<O> object, ObjectDelta<O> delta) throws SchemaException {
            ObjectDeltaObject<O> odo;
            if (delta != null && delta.isAdd()) {
                odo = new ObjectDeltaObject<>(null, delta, object, object.getDefinition());
            } else {
                odo = new ObjectDeltaObject<>(object, delta, null, object.getDefinition());
                odo.recomputeIfNeeded(false);
            }
            return new AuthorizationParameters<>(odo, null, null, null);
        }

        public static <O extends ObjectType> AuthorizationParameters<O,ObjectType> buildObject(PrismObject<O> object) {
            ObjectDeltaObject<O> odo = null;
            if (object != null) {
                odo = new ObjectDeltaObject<>(object, null, object, object.getDefinition());
            }
            return new AuthorizationParameters<>(odo, null, null, null);
        }
    }
}
