/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Description of an origin of a configuration item (expression, mapping, and so on).
 * Necessary e.g. for the derivation of an {@link ExpressionProfile}.
 *
 * == Open questions
 *
 * We implicitly assume that the prism objects (of which configuration items are parts) come from the repository, where they
 * were stored according to the respective authorizations. (It is, after all, a necessary condition to use the origin as a basis
 * for expression profile determination!)
 *
 * But, then, what about (full) objects coming not from the repository but from external sources?
 *
 * TODO make this class a kind-of immutable (currently, we do have full objects here)
 */
@Experimental
public abstract class ConfigurationItemOrigin implements Serializable {

    @Serial private static final long serialVersionUID = 0L;

    /**
     * TEMPORARY until the implementation is complete.
     *
     * We may search for occurrences of this method to find all the places where we need to implement the origin determination.
     *
     * Use with care! Careless use of this origin may render expression profiles ineffective.
     */
    public static ConfigurationItemOrigin undetermined() {
        return new ConfigurationItemOrigin.Undetermined(false);
    }

    /** Undetermined but safe, because it is never used to determine the expression profile. */
    public static ConfigurationItemOrigin undeterminedSafe() {
        return new ConfigurationItemOrigin.Undetermined(true);
    }

    /** Use with care! Careless use of this origin may render expression profiles ineffective. */
    public static ConfigurationItemOrigin external(@NotNull String channelUri) {
        return new External(channelUri);
    }

    public static ConfigurationItemOrigin rest() {
        return external(SchemaConstants.CHANNEL_REST_URI);
    }

    public static ConfigurationItemOrigin user() {
        return external(SchemaConstants.CHANNEL_USER_URI);
    }

    /** Use with care! Careless use of this origin may render expression profiles ineffective. */
    public static ConfigurationItemOrigin generated() {
        return new ConfigurationItemOrigin.Generated();
    }

    /** Intentionally fails for detached objects. */
    public static ConfigurationItemOrigin embedded(@NotNull Object value) {
        return embedded(value, item -> {
            throw new IllegalArgumentException(
                    "Value is not a part of an object: %s".formatted(
                            MiscUtil.getValueWithClass(value)));
        });
    }

    public static ConfigurationItemOrigin embedded(
            @NotNull Object value, @NotNull OriginProvider<? super PrismValue> providerForDetachedValues) {
        if (value instanceof ObjectType object) {
            return inObject(object, ItemPath.EMPTY_PATH);
        }
        PrismValue prismValue;
        if (value instanceof PrismValue pv) {
            prismValue = pv;
        } else if (value instanceof Containerable containerable) {
            prismValue = containerable.asPrismContainerValue();
        } else if (value instanceof Referencable referencable) {
            prismValue = referencable.asReferenceValue();
        } else {
            throw new IllegalArgumentException(
                    "Unsupported value: %s".formatted(
                            MiscUtil.getValueWithClass(value)));
        }
        Objectable rootObjectable = prismValue.getRootObjectable();
        if (rootObjectable == null) {
            return providerForDetachedValues.origin(prismValue);
        }
        if (!(rootObjectable instanceof ObjectType rootObject)) {
            throw new IllegalArgumentException(
                    "Value is not a part of an object of type ObjectType but of %s: %s".formatted(
                            MiscUtil.getValueWithClass(rootObjectable),
                            MiscUtil.getValueWithClass(value)));
        }
        return inObject(rootObject, prismValue.getPath());
    }

    public static ConfigurationItemOrigin inObject(
            @NotNull ObjectType originatingObject, @NotNull ItemPath path) {
        return new InObject(originatingObject, path, true);
    }

    public static ConfigurationItemOrigin inObjectApproximate(
            @NotNull ObjectType originatingObject, @NotNull ItemPath knownPath) {
        return new InObject(originatingObject, knownPath, false);
    }

    public static ConfigurationItemOrigin inDelta(
            @NotNull ObjectType targetObject, @NotNull ItemPath path) {
        return new InDelta(targetObject, path);
    }

    public ConfigurationItemOrigin child(Object... pathSegments) {
        return child(ItemPath.create(pathSegments));
    }

    public abstract ConfigurationItemOrigin child(@NotNull ItemPath path);

    /** Returns the description of the origin, e.g. "in role:xxx(xxx) @assignment[102]/condition". */
    public abstract @NotNull String fullDescription();

    public @NotNull ConfigurationItemOrigin toApproximate() {
        return this; // usually this is the case
    }

    /** Represents an origin we are not currently able to determine exactly. */
    public static class Undetermined extends ConfigurationItemOrigin {

        /**
         * Safe means that it is safe to use this origin, as is is NEVER used to determine the expression profile.
         * So, the possible damage is limited to imprecise information in error or diagnostic messages.
         *
         * OTOH, _unsafe_ means that this is a temporary situation during the migration to full implementation of expression
         * profiles. Such cases should be found and fixed.
         */
        private final boolean safe;

        Undetermined(boolean safe) {
            this.safe = safe;
        }

        @Override
        public String toString() {
            return safe ? "undetermined" : "undetermined (unsafe)";
        }

        @Override
        public ConfigurationItemOrigin child(@NotNull ItemPath path) {
            return this;
        }

        @Override
        public @NotNull String fullDescription() {
            return safe ? "(undetermined origin)" : "(undetermined origin; unsafe)";
        }

        public boolean isSafe() {
            return safe;
        }
    }

    /** Represents an item that was defined out of context of any prism object. */
    public static class External extends ConfigurationItemOrigin {
        @NotNull private final String channelUri;

        public External(@NotNull String channelUri) {
            this.channelUri = channelUri;
        }

        @Override
        public String toString() {
            return "detached(" + channelUri + ")";
        }

        @Override
        public ConfigurationItemOrigin child(@NotNull ItemPath path) {
            return this;
        }

        @Override
        public @NotNull String fullDescription() {
            return "(external piece of configuration; from channel " + channelUri + ")";
        }
    }

    /** Represents an item that was generated by the system. Generally, these should be trusted. */
    public static class Generated extends ConfigurationItemOrigin {
        @Override
        public String toString() {
            return "generated";
        }

        @Override
        public ConfigurationItemOrigin child(@NotNull ItemPath path) {
            return this;
        }

        @Override
        public @NotNull String fullDescription() {
            return "(generated piece of configuration)";
        }
    }

    /** A typical case: an item that is a part of a prism object. */
    public static class InObject extends ConfigurationItemOrigin {

        private final @NotNull ObjectType originatingObject;
        private final @NotNull ItemPath path;

        /**
         * If `false`, we know the position only approximately. The {@link #path} is then all that we know;
         * the value can be anywhere in that subtree.
         */
        private final boolean precise;

        private InObject(
                @NotNull ObjectType originatingObject, @NotNull ItemPath path, boolean precise) {
            // explicit nullity check is here for additional safety
            this.originatingObject = Objects.requireNonNull(originatingObject);
            this.path = path;
            this.precise = precise;
        }

        @Override
        public @NotNull ConfigurationItemOrigin toApproximate() {
            if (precise) {
                return new InObject(originatingObject, path, false);
            } else {
                return this;
            }
        }

        public @NotNull ObjectType getOriginatingObject() {
            return originatingObject;
        }

        public @NotNull PrismObject<? extends ObjectType> getOriginatingPrismObject() {
            return originatingObject.asPrismObject();
        }

        public @NotNull String getOriginatingObjectOid() {
            return originatingObject.getOid();
        }

        public @NotNull ItemPath getPath() {
            return path;
        }

        public boolean isPrecise() {
            return precise;
        }

        @Override
        public ConfigurationItemOrigin child(@NotNull ItemPath path) {
            return precise ?
                    new InObject(originatingObject, this.path.append(path), true) :
                    this;
        }

        @Override
        public String toString() {
            return "in " + originatingObject + " @" + path + (precise ? "" : " (approximate)");
        }

        @Override
        public @NotNull String fullDescription() {
            return toString();
        }
    }

    /**
     * An item that is a part of a delta (of unspecified provenience) that is targeting a given object.
     *
     * As far as expression profiles are concerned, this is very similar to {@link InObject}: the profile is determined
     * by looking at the object.
     */
    public static class InDelta extends ConfigurationItemOrigin {

        private final @NotNull ObjectType targetObject;
        private final @NotNull ItemPath path;

        private InDelta(@NotNull ObjectType targetObject, @NotNull ItemPath path) {
            // explicit nullity check is here for additional safety
            this.targetObject = Objects.requireNonNull(targetObject);
            this.path = path;
        }

        public @NotNull ObjectType getTargetObject() {
            return targetObject;
        }

        public @NotNull PrismObject<? extends ObjectType> getTargetPrismObject() {
            return targetObject.asPrismObject();
        }

        public @NotNull ItemPath getPath() {
            return path;
        }

        @Override
        public ConfigurationItemOrigin child(@NotNull ItemPath path) {
            return new InDelta(targetObject, this.path.append(path));
        }

        @Override
        public String toString() {
            return "in delta targeted to " + targetObject + " @" + path;
        }

        @Override
        public @NotNull String fullDescription() {
            return toString();
        }
    }
}
