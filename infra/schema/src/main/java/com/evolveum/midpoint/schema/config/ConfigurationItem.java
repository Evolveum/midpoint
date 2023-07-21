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

import com.google.common.base.Strings;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SystemException;

/**
 * Helper class that provides complex information about a configuration item (e.g., a mapping).
 * Currently, the most prominent information is the origin of the item value.
 */
@Experimental
public class ConfigurationItem<T extends Serializable> implements ConfigurationItemable<T>, Serializable {

    @Serial private static final long serialVersionUID = 0L;

    /** Used as a convenience marker for {@link #fullDescription()} in error messages. */
    public static final Object DESC = new Object();

    /** The value of the item. Usually, it is a {@link Containerable}. */
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

    public static @NotNull <T extends Serializable> ConfigurationItem<T> of(
            @NotNull T value, @NotNull ConfigurationItemOrigin origin) {
        return new ConfigurationItem<>(value, origin);
    }

    public static @NotNull <T extends Serializable> ConfigurationItem<T> embedded(@NotNull T value) {
        return new ConfigurationItem<>(value, ConfigurationItemOrigin.embedded(value));
    }

    public static @NotNull <T extends Serializable> List<ConfigurationItem<T>> ofListEmbedded(@NotNull List<T> items) {
        return ofList(items, OriginProvider.embedded());
    }

    public static @NotNull <T extends Serializable> List<ConfigurationItem<T>> ofList(
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

    protected <X extends Serializable> @NotNull ConfigurationItem<X> child(@NotNull X value, Object... pathSegments) {
        return of(
                value,
                origin().child(pathSegments));
    }

    protected <X extends Containerable> @NotNull ConfigurationItem<X> childWithId(@NotNull X value, Object... pathSegments) {
        return of(
                value,
                origin().child(
                        ItemPath.create(pathSegments).append(value.asPrismContainerValue().getId())));
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
