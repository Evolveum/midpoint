/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import com.google.common.base.Strings;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.util.StringUtils;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.Collection;

import static com.evolveum.midpoint.schema.config.ConfigurationItem.DESC;

/**
 * For internal use. TODO better name
 */
interface ConfigurationItemable<T extends Serializable & Cloneable> {

    /** See {@link ConfigurationItem#value}. */
    @NotNull T value();

    /** See {@link ConfigurationItem#origin}. */
    @NotNull ConfigurationItemOrigin origin();

    <X extends ConfigurationItem<T>> @NotNull X as(@NotNull Class<X> clazz);

    @Contract("null, _, _ -> null; !null, _, _ -> !null")
    <X extends Serializable & Cloneable, CI extends ConfigurationItem<X>> CI child(
            @Nullable X value, @NotNull Class<CI> clazz, Object... pathSegments);

    @NotNull String fullDescription();

    /**
     * Checks the value, and if it's `false`, emits a {@link ConfigurationException}.
     *
     * Note that {@link ConfigurationItem#DESC} can be used as a placeholder for {@link #fullDescription()} in the `arguments`.
     */
    @Contract("false, _, _ -> fail")
    default void configCheck(boolean value, String template, Object... arguments) throws ConfigurationException {
        if (!value) {
            throw configException(template, arguments);
        }
    }

    default void checkNamespace(@NotNull QName name, String expectedNamespace) throws ConfigurationException {
        String realNamespace = name.getNamespaceURI();
        if (StringUtils.hasLength(realNamespace) && !expectedNamespace.equals(realNamespace)) {
            throw namespaceMismatchException(name, expectedNamespace);
        }
    }

    private @NotNull ConfigurationException namespaceMismatchException(@NotNull QName name, String expectedNamespace) {
        return configException(
                "Expected namespace '%s', but got '%s' (local part: '%s'); in %s",
                expectedNamespace, name.getNamespaceURI(), name.getLocalPart(), DESC);
    }

    /** Just like {@link QNameUtil#enforceNamespace(QName, String)} but throwing {@link ConfigurationException}. */
    default @NotNull QName enforceNamespace(@NotNull QName name, @NotNull String requiredNamespace)
            throws ConfigurationException {
        var namespace = name.getNamespaceURI();
        if (!StringUtils.hasLength(namespace)) {
            return new QName(requiredNamespace, name.getLocalPart());
        } else if (namespace.equals(requiredNamespace)) {
            return name;
        } else {
            throw namespaceMismatchException(name, requiredNamespace);
        }
    }

    default @NotNull String getLocalPart(@NotNull QName name, String expectedNamespace) throws ConfigurationException {
        checkNamespace(name, expectedNamespace);
        return name.getLocalPart();
    }

    default @NotNull ConfigurationException configException(Throwable cause, String template, Object... arguments) {
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i] == DESC) {
                arguments[i] = fullDescription();
            }
        }
        String message = Strings.lenientFormat(template, arguments);
        if (cause != null) {
            return new ConfigurationException(message + ": " + cause.getMessage(), cause);
        } else {
            return new ConfigurationException(message);
        }
    }

    default @NotNull ConfigurationException configException(String template, Object... arguments) {
        return configException(null, template, arguments);
    }

    /** As {@link #configCheck(boolean, String, Object...)}, but checks that the value is not null. */
    @Contract("null, _, _ -> fail")
    default  <V> @NotNull V configNonNull(V value, String template, Object... arguments) throws ConfigurationException {
        configCheck(value != null, template, arguments);
        assert value != null;
        return value;
    }

    /** Even more shortened version. */
    @Contract("null, _ -> fail")
    default <V> @NotNull V nonNull(V value, Object itemDesc) throws ConfigurationException {
        return configNonNull(value, "No %s in %s", itemDesc, DESC);
    }

    default <C extends Collection<?>> @NotNull C nonEmpty(C collection, Object itemDescription) throws ConfigurationException {
        configCheck(collection != null && !collection.isEmpty(), "No %s in %s", itemDescription, DESC);
        assert collection != null; // just to make IDE happy
        return collection;
    }

    default <C extends Collection<?>> @NotNull C nonEmpty(C collection, String template, Object... arguments)
            throws ConfigurationException {
        configCheck(collection != null && !collection.isEmpty(), template, arguments);
        assert collection != null; // just to make IDE happy
        return collection;
    }

    default @NotNull ItemName singleNameRequired(ItemPathType name, Object itemDesc)
            throws ConfigurationException {
        var itemPath = nonNull(name, itemDesc).getItemPath();
        if (itemPath.isSingleName()) {
            return ItemPath.toName(itemPath.first());
        } else {
            throw configException("Expected a single-segment %s, bug got '%s'; in %s", itemDesc, itemPath, DESC);
        }
    }

    default <C> @Nullable C single(Collection<? extends C> collection, String template, Object... arguments) throws ConfigurationException {
        if (collection == null || collection.isEmpty()) {
            return null;
        } else if (collection.size() == 1) {
            return collection.iterator().next();
        } else {
            throw configException(template, arguments);
        }
    }

    default <C extends Containerable> @NotNull OriginProvider<C> originProviderFor(@NotNull ItemPath path) {
        return item -> originFor(path);
    }

    default @NotNull ConfigurationItemOrigin originFor(@NotNull ItemPath path) {
        return origin().child(path);
    }

}
