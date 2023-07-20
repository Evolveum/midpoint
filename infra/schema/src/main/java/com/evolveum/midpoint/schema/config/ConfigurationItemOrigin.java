/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
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
 * TODO make this class a kind-of immutable
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
        return new ConfigurationItemOrigin.Undetermined();
    }

    /** Use with care! Careless use of this origin may render expression profiles ineffective. */
    public static ConfigurationItemOrigin detached() {
        return new ConfigurationItemOrigin.Detached();
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
        return new InObject(originatingObject, path);
    }

    public static ConfigurationItemOrigin inDelta(
            @NotNull ObjectType targetObject, @NotNull ItemPath path) {
        return new InDelta(targetObject, path);
    }

    public abstract ConfigurationItemOrigin child(@NotNull ItemPath path);

    public abstract @NotNull String fullDescription();

    /** Represents an origin we are not currently able to determine exactly. */
    public static class Undetermined extends ConfigurationItemOrigin {
        @Override
        public String toString() {
            return "undetermined";
        }

        @Override
        public ConfigurationItemOrigin child(@NotNull ItemPath path) {
            return this;
        }

        @Override
        public @NotNull String fullDescription() {
            return "(undetermined origin)";
        }
    }

    /** Represents an item that was defined out of context of any prism object. */
    public static class Detached extends ConfigurationItemOrigin {
        @Override
        public String toString() {
            return "detached";
        }

        @Override
        public ConfigurationItemOrigin child(@NotNull ItemPath path) {
            return this;
        }

        @Override
        public @NotNull String fullDescription() {
            return "(detached piece of configuration)";
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

        private InObject(@NotNull ObjectType originatingObject, @NotNull ItemPath path) {
            // explicit nullity check is here for additional safety
            this.originatingObject = Objects.requireNonNull(originatingObject);
            this.path = path;
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

        @Override
        public ConfigurationItemOrigin child(@NotNull ItemPath path) {
            return new InObject(originatingObject, this.path.append(path));
        }

        @Override
        public String toString() {
            return "in " + originatingObject + " @" + path;
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
