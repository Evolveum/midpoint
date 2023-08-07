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

import com.evolveum.midpoint.prism.util.CloneUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

import com.google.common.base.Strings;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SystemException;

import org.jetbrains.annotations.Nullable;

/**
 * Helper class that provides complex information about a configuration item (e.g., a mapping).
 * Currently, the most prominent information is the origin of the item value.
 */
@Experimental
public class ConfigurationItem<T extends Serializable & Cloneable> implements ConfigurationItemable<T>, Serializable, Cloneable {

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

    /** For internal use. */
    protected ConfigurationItem(
            @NotNull ConfigurationItem<T> original) {
        this.value = original.value;
        this.origin = original.origin;
    }

    public ConfigurationItem(
            @NotNull T value,
            @NotNull ConfigurationItemOrigin origin) {
        this.value = value;
        this.origin = origin;
    }

    public static @NotNull <T extends Serializable & Cloneable> ConfigurationItem<T> of(
            @NotNull T value, @NotNull ConfigurationItemOrigin origin) {
        return new ConfigurationItem<>(value, origin);
    }

    public static @NotNull <T extends Serializable & Cloneable> ConfigurationItem<T> embedded(@NotNull T value) {
        return new ConfigurationItem<>(value, ConfigurationItemOrigin.embedded(value));
    }

    public static @NotNull <T extends Serializable & Cloneable> List<ConfigurationItem<T>> ofListEmbedded(@NotNull List<T> items) {
        return ofList(items, OriginProvider.embedded());
    }

    public static @NotNull <T extends Serializable & Cloneable> List<ConfigurationItem<T>> ofList(
            @NotNull List<T> items, @NotNull OriginProvider<? super T> originProvider) {
        return items.stream()
                .map(i -> of(i, originProvider.origin(i)))
                .toList();
    }

    @Override
    public @NotNull T value() {
        return value;
    }

    public @NotNull ConfigurationItemOrigin origin() {
        return origin;
    }

    public @NotNull ConfigurationItemOrigin originFor(@NotNull ItemPath path) {
        return origin.child(path);
    }

    public <C extends Containerable> @NotNull OriginProvider<C> originProviderFor(@NotNull ItemPath path) {
        return item -> originFor(path);
    }

    /** Null-safe variant of {@link #as(Class)}. */
    @Contract("null, _ -> null; !null, _ -> !null")
    protected static <V extends Serializable & Cloneable, CI extends RAW_CI, RAW_CI extends ConfigurationItem<V>> CI as(
            @Nullable RAW_CI value, @NotNull Class<CI> clazz) {
        return value != null ? value.as(clazz) : null;
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

    @Contract("null, _, _ -> null; !null, _, _ -> !null")
    protected <X extends Serializable & Cloneable, CI extends ConfigurationItem<X>> CI child(
            @Nullable X value, @NotNull Class<CI> clazz, Object... pathSegments) {
        var ci = child(value, pathSegments);
        return ci != null ? ci.as(clazz) : null;
    }

    @Contract("null, _ -> null; !null, _ -> !null")
    protected <X extends Serializable & Cloneable> ConfigurationItem<X> child(@Nullable X value, Object... pathSegments) {
        if (value != null) {
            return of(
                    value,
                    origin().child(pathSegments));
        } else {
            return null;
        }
    }

    private <C extends Containerable> @NotNull ConfigurationItem<C> childWithId(@NotNull C value, Object... pathSegments) {
        return of(
                value,
                origin().child(
                        ItemPath.create(pathSegments).append(value.asPrismContainerValue().getId())));
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
                origin);
    }

    @Override
    public String toString() {
        return "ConfigurationItem[" +
                "value=" + value + ", " +
                "origin=" + origin + ']';
    }

    /** To be overridden in specific subclasses. */
    public @NotNull String localDescription() {
        return value.getClass().getSimpleName();
    }

    @SuppressWarnings("WeakerAccess") // for the future
    public @NotNull String fullDescription() {
        return localDescription() + " in " + origin.fullDescription();
    }

    /**
     * Checks the value, and if it's `false`, emits a {@link ConfigurationException}.
     *
     * Note that {@link #DESC} can be used as a placeholder for {@link #fullDescription()} in the `arguments`.
     * */
    public void configCheck(boolean value, String template, Object... arguments) throws ConfigurationException {
        if (!value) {
            for (int i = 0; i < arguments.length; i++) {
                if (arguments[i] == DESC) {
                    arguments[i] = fullDescription();
                }
            }
            throw new ConfigurationException(
                    Strings.lenientFormat(template, arguments));
        }
    }

    /** As {@link #configCheck(boolean, String, Object...)}, but checks that the value is not null. */
    @Contract("null, _, _ -> fail")
    public <V> @NotNull V configNonNull(V value, String template, Object... arguments) throws ConfigurationException {
        configCheck(value != null, template, arguments);
        assert value != null;
        return value;
    }
}
