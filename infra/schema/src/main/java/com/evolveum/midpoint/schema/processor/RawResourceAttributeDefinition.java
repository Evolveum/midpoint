/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismPropertyDefinition;

public interface RawResourceAttributeDefinition<T>
        extends PrismPropertyDefinition<T> {

    /**
     * Is this attribute returned by default? (I.e. if no specific options are sent to the connector?)
     */
    @Nullable Boolean getReturnedByDefault();

    /**
     * Is this attribute returned by default? (I.e. if no specific options are sent to the connector?)
     */
    default boolean isReturnedByDefault() {
        return !Boolean.FALSE.equals(
                getReturnedByDefault());
    }

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
    String getNativeAttributeName();

    /**
     * Returns name of the attribute as given in the connector framework.
     * This is not used for any significant logic. It is mostly for diagnostics.
     *
     * @return name of the attribute as given in the connector framework.
     */
    String getFrameworkAttributeName();

    /** TODO what to do with this? */
    @NotNull
    @Override
    ResourceAttribute<T> instantiate();

    /** TODO what to do with this? */
    @NotNull
    @Override
    ResourceAttribute<T> instantiate(QName name);

}
