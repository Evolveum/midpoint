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

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SystemException;

import com.google.common.base.Strings;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;

/**
 * Helper class that provides complex information about a configuration item (e.g., a mapping).
 * Currently, the most prominent information is the origin of the item value.
 */
@Experimental
public class ConfigurationItem<T extends Serializable> implements Serializable {

    @Serial private static final long serialVersionUID = 0L;

    private final @NotNull T value;
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
            @NotNull T item, @NotNull ConfigurationItemOrigin origin) {
        return new ConfigurationItem<>(item, origin);
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

    public <X extends ConfigurationItem<T>> @NotNull X as(@NotNull Class<X> clazz) {
        try {
            var constructor = clazz.getDeclaredConstructor(ConfigurationItem.class);
            return constructor.newInstance(this);
        } catch (Throwable t) {
            throw SystemException.unexpected(t, "when converting " + this + " to " + clazz);
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

    public @NotNull String fullDescription() {
        return localDescription() + " in " + origin.fullDescription();
    }

    /** The last "%s" is filled with full description */
    public void configCheck(boolean value, String template, Object... arguments) throws ConfigurationException {
        if (!value) {
            Object[] augmentedArguments = new Object[arguments.length + 1];
            System.arraycopy(arguments, 0, augmentedArguments, 0, arguments.length);
            augmentedArguments[arguments.length] = fullDescription();

            throw new ConfigurationException(
                    Strings.lenientFormat(template, augmentedArguments));
        }
    }

    @Contract("null, _, _ -> fail")
    public <V> @NotNull V configNonNull(V value, String template, Object... arguments) throws ConfigurationException {
        configCheck(value != null, template, arguments);
        assert value != null;
        return value;
    }
}
