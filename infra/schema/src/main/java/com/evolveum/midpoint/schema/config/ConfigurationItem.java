/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

/**
 * Helper class that provides complex information about a configuration item (e.g., a mapping).
 * Currently, the most prominent information is the origin of the item value.
 */
@Experimental
public class ConfigurationItem<T extends Serializable & Cloneable>
        implements ConfigurationItemable<T>, Serializable, Cloneable {

    @Serial private static final long serialVersionUID = 0L;

    /** Used as a convenience marker for {@link #fullDescription()} in error messages. */
    public static final Object DESC = new Object();

    /** The value of the item. Usually, it is a {@link Containerable} but there are exceptions, like {@link ExpressionType}. */
    private final @NotNull T value;

    /**
     * The origin of the item value. If the value comes from various sources, this is the _default_ origin: individual
     * parts may have their own origins: declared either in this field, or in the value metadata.
     *
     * Note that the support for value metadata for origin determination is not implemented yet.
     */
    private final @NotNull ConfigurationItemOrigin origin;

    /** The "container" in which this item is present. */
    private final @Nullable ConfigurationItem<?> parent;

    /** For internal use. */
    protected ConfigurationItem(
            @NotNull ConfigurationItem<? extends T> original) {
        this.value = original.value;
        this.origin = original.origin;
        this.parent = original.parent;
    }

    public ConfigurationItem(
            @NotNull T value,
            @NotNull ConfigurationItemOrigin origin,
            @Nullable ConfigurationItem<?> parent) {
        this.value = value;
        this.origin = origin;
        this.parent = parent;
    }

    /** Simple way of constructing a typed configuration item. To be imported statically. */
    public static @NotNull <T extends Serializable & Cloneable, X extends ConfigurationItem<T>> X configItem(
            @NotNull T value, @NotNull ConfigurationItemOrigin origin, @NotNull Class<X> clazz) {
        return of(value, origin)
                .as(clazz);
    }

    /** Simple way of constructing a typed configuration item. To be imported statically. */
    public static @NotNull <T extends Serializable & Cloneable, X extends ConfigurationItem<T>> X configItem(
            @NotNull T value,
            @NotNull ConfigurationItemOrigin origin,
            @Nullable ConfigurationItem<?> parent,
            @NotNull Class<X> clazz) {
        return new ConfigurationItem<>(value, origin, parent)
                .as(clazz);
    }

    /** Simple way of constructing a typed configuration item. To be imported statically. */
    @Contract("!null, _, _ -> !null; null, _, _ -> null")
    public static <T extends Serializable & Cloneable, X extends ConfigurationItem<T>> X configItemNullable(
            @Nullable T value, @NotNull ConfigurationItemOrigin origin, @NotNull Class<X> clazz) {
        return value != null ? configItem(value, origin, clazz) : null;
    }

    /** Use {@link #configItem(Serializable, ConfigurationItemOrigin, Class)} instead. */
    @Deprecated
    public static @NotNull <T extends Serializable & Cloneable> ConfigurationItem<T> of(
            @NotNull T value, @NotNull ConfigurationItemOrigin origin) {
        return new ConfigurationItem<>(value, origin, null);
    }

    public static @NotNull <T extends Serializable & Cloneable> ConfigurationItem<T> embedded(@NotNull T value) {
        return new ConfigurationItem<>(value, ConfigurationItemOrigin.embedded(value), null);
    }

    @Contract("!null -> !null; null -> null")
    public static <T extends Serializable & Cloneable> ConfigurationItem<T> embeddedNullable(T value) {
        return value != null ?
                new ConfigurationItem<>(value, ConfigurationItemOrigin.embedded(value), null) :
                null;
    }

    private static @NotNull <T extends Serializable & Cloneable> List<ConfigurationItem<T>> ofList(
            @NotNull List<T> items, @NotNull OriginProvider<? super T> originProvider) {
        return items.stream()
                .map(i -> of(i, originProvider.origin(i)))
                .toList();
    }

    // consider adding parent here
    public static @NotNull <T extends Serializable & Cloneable, X extends ConfigurationItem<T>> List<X> ofList(
            @NotNull List<T> items, @NotNull OriginProvider<? super T> originProvider, @NotNull Class<X> clazz) {
        return asList(
                ofList(items, originProvider),
                clazz);
    }

    @Override
    public @NotNull T value() {
        return value;
    }

    public static <T extends Serializable & Cloneable> T value(@Nullable ConfigurationItem<T> item) {
        return item != null ? item.value() : null;
    }

    public @NotNull ConfigurationItemOrigin origin() {
        return origin;
    }

    /** Null-safe variant of {@link #as(Class)}. */
    @Contract("null, _ -> null; !null, _ -> !null")
    protected static <V extends Serializable & Cloneable, CI extends RAW_CI, RAW_CI extends ConfigurationItem<V>> CI as(
            @Nullable RAW_CI value, @NotNull Class<CI> clazz) {
        return value != null ? value.as(clazz) : null;
    }

    public static <T extends Serializable & Cloneable, X extends ConfigurationItem<T>> @NotNull List<X> asList(
            @NotNull List<ConfigurationItem<T>> list, @NotNull Class<X> clazz) {
        return list.stream()
                .map(ci -> ci.as(clazz))
                .toList();
    }

    @Override
    public <X extends ConfigurationItem<T>> @NotNull X as(@NotNull Class<X> clazz) {
        if (clazz.isAssignableFrom(this.getClass())) {
            // We are already there
            return clazz.cast(this);
        }
        try {
            var constructor = clazz.getDeclaredConstructor(ConfigurationItem.class);
            return constructor.newInstance(this);
        } catch (Throwable t) {
            throw SystemException.unexpected(t, "when converting " + this + " to " + clazz);
        }
    }

    @NotNull <C extends Containerable, CI extends ConfigurationItem<C>> List<CI> children(
            @NotNull List<C> beans, Class<CI> type, Object... pathSegments) {
        return beans.stream()
                .map(val ->
                        childWithOrWithoutId(val, pathSegments)
                                .as(type))
                .toList();
    }

    @NotNull <X extends Serializable & Cloneable, CI extends ConfigurationItem<X>> List<CI> childrenPlain(
            @NotNull List<X> beans, Class<CI> type, Object... pathSegments) {
        return beans.stream()
                .map(val ->
                        child(val, pathSegments)
                                .as(type))
                .toList();
    }

    @Override
    @Contract("null, _, _ -> null; !null, _, _ -> !null")
    public <X extends Serializable & Cloneable, CI extends ConfigurationItem<X>> CI child(
            @Nullable X value, @NotNull Class<CI> clazz, Object... pathSegments) {
        var ci = child(value, pathSegments);
        return ci != null ? ci.as(clazz) : null;
    }

    @Contract("null, _ -> null; !null, _ -> !null")
    protected <X extends Serializable & Cloneable> ConfigurationItem<X> child(@Nullable X value, Object... pathSegments) {
        if (value != null) {
            return new ConfigurationItem<>(
                    value,
                    origin().child(pathSegments),
                    this);
        } else {
            return null;
        }
    }

    private <C extends Containerable> @NotNull ConfigurationItem<C> childWithId(@NotNull C value, Object... pathSegments) {
        return new ConfigurationItem<>(
                value,
                origin().child(
                        ItemPath.create(pathSegments).append(value.asPrismContainerValue().getId())),
                this);
    }

    /** TODO better name */
    @Contract("null, _ -> null; !null, _ -> !null")
    <C extends Containerable> ConfigurationItem<C> childWithOrWithoutId(@Nullable C value, Object... pathSegments) {
        if (value != null) {
            if (value.asPrismContainerValue().getId() != null) {
                return childWithId(value, pathSegments);
            } else {
                return child(value, pathSegments);
            }
        } else {
            return null;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (ConfigurationItem<?>) obj;
        return Objects.equals(this.value, that.value)
                && Objects.equals(this.origin, that.origin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, origin);
    }

    /**
     * Intentionally not calling super.clone, as the value is final (and we have to clone it); so we would have
     * to hack this using reflection turning off the `final` flag.
     */
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public ConfigurationItem<T> clone() {
        return new ConfigurationItem<>(
                CloneUtil.cloneCloneable(value),
                origin,
                parent);
    }

    @Override
    public String toString() {
        return fullDescription();
    }

    /** To be overridden in specific subclasses. */
    public @NotNull String localDescription() {
        return value.getClass().getSimpleName();
    }

    @SuppressWarnings("WeakerAccess") // for the future
    public @NotNull String fullDescription() {
        return fullOriginLessDescription() + " " + origin.fullDescription();
    }

    @SuppressWarnings("WeakerAccess") // for the future
    public @NotNull String fullOriginLessDescription() {
        if (parent != null) {
            return localDescription() + " in " + parent.fullOriginLessDescription();
        } else {
            return localDescription();
        }
    }
}
