/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.simulation;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;
import static com.evolveum.midpoint.util.MiscUtil.argNonNull;

import java.io.Serializable;
import java.util.Objects;

import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.schema.util.SimulationMetricReferenceTypeUtil;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.BuiltInSimulationMetricType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationMetricReferenceType;

import org.jetbrains.annotations.Nullable;

/**
 * Parsed form of {@link SimulationMetricReferenceType}, suitable e.g. as a map key.
 *
 * Primitive implementation, for now.
 */
public abstract class SimulationMetricReference implements Serializable {

    public static SimulationMetricReference fromBean(@NotNull SimulationMetricReferenceType bean) {
        ObjectReferenceType markRef = bean.getEventMarkRef();
        BuiltInSimulationMetricType builtIn = bean.getBuiltIn();
        String identifier = bean.getIdentifier();
        int count = (markRef != null ? 1 : 0)
                + (builtIn != null ? 1 : 0)
                + (identifier != null ? 1 : 0);
        argCheck(count == 1,
                "Exactly one of (eventMarkRef, builtIn, identifier) must be specified: %s of them are", count);
        if (markRef != null) {
            return new Mark(argNonNull(markRef.getOid(), () -> "eventMarkRef without OID"));
        } else if (builtIn != null) {
            return new BuiltIn(builtIn);
        } else {
            assert identifier != null;
            return new Explicit(identifier);
        }
    }

    public static SimulationMetricReference.Mark forMark(@NotNull String oid) {
        return new SimulationMetricReference.Mark(oid);
    }

    public static SimulationMetricReference.Explicit forExplicit(@NotNull String identifier) {
        return new SimulationMetricReference.Explicit(identifier);
    }

    public static SimulationMetricReference.BuiltIn forBuiltIn(@NotNull BuiltInSimulationMetricType builtIn) {
        return new SimulationMetricReference.BuiltIn(builtIn);
    }

    public abstract @NotNull SimulationMetricReferenceType toBean();

    public abstract boolean matches(@Nullable SimulationMetricReferenceType ref);

    public static class Mark extends SimulationMetricReference {

        @NotNull private final String oid;

        public Mark(@NotNull String oid) {
            this.oid = oid;
        }

        public @NotNull String getOid() {
            return oid;
        }

        public @NotNull ObjectReferenceType getRef() {
            return ObjectTypeUtil.createObjectRef(oid, ObjectTypes.MARK);
        }

        @Override
        public @NotNull SimulationMetricReferenceType toBean() {
            return SimulationMetricReferenceTypeUtil.forEventMarkOid(oid);
        }

        @Override
        public boolean matches(@Nullable SimulationMetricReferenceType ref) {
            return ref != null && oid.equals(Referencable.getOid(ref.getEventMarkRef()));
        }

        @Override
        public String toString() {
            return "Mark{" +
                    "oid='" + oid + '\'' +
                    "}";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Mark mark = (Mark) o;
            return oid.equals(mark.oid);
        }

        @Override
        public int hashCode() {
            return Objects.hash(oid);
        }
    }

    public static class Explicit extends SimulationMetricReference {
        @NotNull private final String identifier;

        public Explicit(@NotNull String identifier) {
            this.identifier = identifier;
        }

        public @NotNull String getIdentifier() {
            return identifier;
        }

        @Override
        public @NotNull SimulationMetricReferenceType toBean() {
            return SimulationMetricReferenceTypeUtil.forIdentifier(identifier);
        }

        @Override
        public boolean matches(@Nullable SimulationMetricReferenceType ref) {
            return ref != null && identifier.equals(ref.getIdentifier());
        }

        @Override
        public String toString() {
            return "Explicit{" +
                    "identifier='" + identifier + '\'' +
                    "}";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Explicit explicit = (Explicit) o;
            return identifier.equals(explicit.identifier);
        }

        @Override
        public int hashCode() {
            return Objects.hash(identifier);
        }
    }

    public static class BuiltIn extends SimulationMetricReference {

        public static final SimulationMetricReference.BuiltIn ADDED = new BuiltIn(BuiltInSimulationMetricType.ADDED);
        public static final SimulationMetricReference.BuiltIn MODIFIED = new BuiltIn(BuiltInSimulationMetricType.MODIFIED);
        public static final SimulationMetricReference.BuiltIn DELETED = new BuiltIn(BuiltInSimulationMetricType.DELETED);

        @NotNull private final BuiltInSimulationMetricType builtIn;

        BuiltIn(@NotNull BuiltInSimulationMetricType builtIn) {
            this.builtIn = builtIn;
        }

        public @NotNull BuiltInSimulationMetricType getBuiltIn() {
            return builtIn;
        }

        @Override
        public @NotNull SimulationMetricReferenceType toBean() {
            return SimulationMetricReferenceTypeUtil.forBuiltIn(builtIn);
        }

        @Override
        public boolean matches(@Nullable SimulationMetricReferenceType ref) {
            return ref != null && builtIn == ref.getBuiltIn();
        }

        @Override
        public String toString() {
            return "BuiltIn{" +
                    "identifier=" + builtIn +
                    "}";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            BuiltIn builtIn = (BuiltIn) o;
            return this.builtIn == builtIn.builtIn;
        }

        @Override
        public int hashCode() {
            return Objects.hash(builtIn);
        }
    }
}
