/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.util.ShortDumpable;

import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * "Getter" interface to "UCF" part of resource attribute and association definitions.
 *
 * Supports delegation to real data store.
 */
public interface ResourceItemUcfDefinition extends Serializable, ShortDumpable {

    /**
     * Is this attribute returned by default? (I.e. if no specific options are sent to the connector?)
     */
    @Nullable Boolean getReturnedByDefault();

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

    interface Delegable extends ResourceItemUcfDefinition {

        ResourceItemUcfDefinition ucfData();

        default @Nullable Boolean getReturnedByDefault() {
            return ucfData().getReturnedByDefault();
        }

        default String getNativeAttributeName() {
            return ucfData().getNativeAttributeName();
        }

        default String getFrameworkAttributeName() {
            return ucfData().getFrameworkAttributeName();
        }
    }

    /** Mutable interface to properties in this class. */
    interface Mutable {

        void setReturnedByDefault(Boolean returnedByDefault);

        void setNativeAttributeName(String nativeAttributeName);

        void setFrameworkAttributeName(String frameworkAttributeName);

        interface Delegable extends Mutable {

            ResourceItemUcfDefinition.Mutable ucfData();

            default void setReturnedByDefault(Boolean returnedByDefault) {
                ucfData().setReturnedByDefault(returnedByDefault);
            }

            default void setNativeAttributeName(String nativeAttributeName) {
                ucfData().setNativeAttributeName(nativeAttributeName);
            }

            default void setFrameworkAttributeName(String frameworkAttributeName) {
                ucfData().setFrameworkAttributeName(frameworkAttributeName);
            }
        }
    }
}
