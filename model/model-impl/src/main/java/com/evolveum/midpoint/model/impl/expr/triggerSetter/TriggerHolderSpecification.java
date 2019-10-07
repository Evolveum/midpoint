/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.expr.triggerSetter;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 *  Specification of an object that should hold a trigger. Currently we support pointing to object by its name or OID.
 */
abstract class TriggerHolderSpecification {

    @NotNull private final Class<? extends ObjectType> type;

    TriggerHolderSpecification(@NotNull Class<? extends ObjectType> type) {
        this.type = type;
    }

    @NotNull
    public Class<? extends ObjectType> getType() {
        return type;
    }

    abstract String getOid();
    abstract ObjectQuery createQuery(PrismContext prismContext);

    static class Named extends TriggerHolderSpecification {
        @NotNull private final String name;

        Named(@NotNull Class<? extends ObjectType> type, @NotNull String name) {
            super(type);
            this.name = name;
        }

        @Override
        String getOid() {
            return null;
        }

        @Override
        ObjectQuery createQuery(PrismContext prismContext) {
            return prismContext.queryFor(getType())
                    .item(ObjectType.F_NAME).eq(name).matchingOrig()
                    .build();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Named)) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }
            Named named = (Named) o;
            return name.equals(named.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), name);
        }

        @Override
        public String toString() {
            return "TriggerHolderSpecification.Named{" +
                    "type=" + getType() +
                    ", name='" + name + '\'' +
                    '}';
        }
    }

    static class Referenced extends TriggerHolderSpecification {
        @NotNull private final String oid;

        Referenced(@NotNull Class<? extends ObjectType> type, @NotNull String oid) {
            super(type);
            this.oid = oid;
        }

        @NotNull
        @Override
        String getOid() {
            return oid;
        }

        @Override
        ObjectQuery createQuery(PrismContext prismContext) {
            return null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Referenced)) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }
            Referenced that = (Referenced) o;
            return oid.equals(that.oid);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), oid);
        }

        @Override
        public String toString() {
            return "TriggerHolderSpecification.Referenced{" +
                    "type=" + getType() +
                    ", oid='" + oid + '\'' +
                    '}';
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof TriggerHolderSpecification))
            return false;
        TriggerHolderSpecification that = (TriggerHolderSpecification) o;
        return type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }
}
