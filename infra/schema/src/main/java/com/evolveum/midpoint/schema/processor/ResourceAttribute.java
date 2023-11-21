/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Resource Object Attribute is a Property of Resource Object. All that applies
 * to property applies also to attribute, e.g. only a whole attributes can be
 * changed, they may be simple or complex types, they should be representable in
 * XML, etc. In addition, attribute definition may have some annotations that
 * suggest its purpose and use on the Resource.
 *
 * Resource Object Attribute understands resource-specific annotations such as
 * native attribute name.
 *
 * Resource Object Attribute is mutable.
 *
 * @author Radovan Semancik
 */
public interface ResourceAttribute<T> extends PrismProperty<T> {

    ResourceAttributeDefinition<T> getDefinition();

    /**
     * Returns native attribute name.
     *
     * Native name of the attribute is a name as it is used on the resource or
     * as seen by the connector. It is used for diagnostics purposes and may be
     * used by the connector itself. As the attribute names in XSD have to
     * comply with XML element name limitations, this may be the only way how to
     * determine original attribute name.
     *
     * Returns null if native attribute name is not set or unknown.
     *
     * The name should be the same as the one used by the resource, if the
     * resource supports naming of attributes. E.g. in case of LDAP this
     * annotation should contain "cn", "givenName", etc. If the resource is not
     * that flexible, the native attribute names may be hardcoded (e.g.
     * "username", "homeDirectory") or may not be present at all.
     *
     * @return native attribute name
     */
    default String getNativeAttributeName() {
        var definition = getDefinition();
        return definition != null ? definition.getNativeAttributeName() : null;
    }

    /** Returns self to be usable in chained calls. */
    default @NotNull ResourceAttribute<T> forceDefinitionFrom(ResourceObjectDefinition objectDefinition) throws SchemaException {
        var attrDef = objectDefinition.findAttributeDefinitionRequired(getElementName());
        //noinspection unchecked
        forceDefinition((ResourceAttributeDefinition<T>) attrDef);
        return this;
    }

    default void forceDefinition(@NotNull ResourceAttributeDefinition<T> attributeDefinition) throws SchemaException {
        applyDefinition(attributeDefinition, true);
    }

    /**
     * Forces the definition, potentially different from the current one.
     * Executes the normalization.
     *
     * This may include conversion of values from {@link String} to {@link PolyString},
     * if string normalizer is used.
     *
     * The returned value may be the same as this instance, or it may be a new instance.
     * (For example, if the original value is immutable, or if the type change occurs.)
     */
    @NotNull <T2> ResourceAttribute<T2> forceDefinitionWithNormalization(@NotNull ResourceAttributeDefinition<T2> newDefinition)
            throws SchemaException;

    @Override
    ResourceAttribute<T> clone();
}
