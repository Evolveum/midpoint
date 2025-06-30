/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.AbstractFreezable;
import com.evolveum.midpoint.util.ShortDumpable;

import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Objects;

/**
 * Getter interface to "UCF" part of shadow attribute definitions.
 *
 * Supports delegation to real data store.
 */
public interface ShadowAttributeUcfDefinition extends Serializable, ShortDumpable {

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

    /**
     * Is this attribute returned by default? (I.e. if no specific options are sent to the connector?)
     */
    @Nullable Boolean getReturnedByDefault();

    /**
     *  Returns the description of an Attribute. Can be used to determine the potential use of the Attribute.
     */
    String getNativeDescription();

    interface Delegable extends ShadowAttributeUcfDefinition {

        ShadowAttributeUcfDefinition ucfData();

        default @Nullable Boolean getReturnedByDefault() {
            return ucfData().getReturnedByDefault();
        }

        default String getNativeAttributeName() {
            return ucfData().getNativeAttributeName();
        }

        default String getFrameworkAttributeName() {
            return ucfData().getFrameworkAttributeName();
        }

        default String getNativeDescription() {
            return ucfData().getNativeDescription();
        }
    }

    /** Mutable interface to properties in this class. */
    interface Mutable {

        void setReturnedByDefault(Boolean returnedByDefault);

        void setNativeAttributeName(String nativeAttributeName);

        void setFrameworkAttributeName(String frameworkAttributeName);

        void setNativeDescription(String nativeDescription);

        interface Delegable extends Mutable {

            ShadowAttributeUcfDefinition.Mutable ucfData();

            default void setReturnedByDefault(Boolean returnedByDefault) {
                ucfData().setReturnedByDefault(returnedByDefault);
            }

            default void setNativeAttributeName(String nativeAttributeName) {
                ucfData().setNativeAttributeName(nativeAttributeName);
            }

            default void setFrameworkAttributeName(String frameworkAttributeName) {
                ucfData().setFrameworkAttributeName(frameworkAttributeName);
            }

            default void setNativeDescription(String nativeDescription) {
                ucfData().setNativeDescription(nativeDescription);
            }
        }
    }

    /** Contains real data for UCF aspect of shadow attribute definition. */
    class Data
            extends AbstractFreezable
            implements ShadowAttributeUcfDefinition, ShadowAttributeUcfDefinition.Mutable, Serializable {

        /** Name that is native on the resource. It is provided e.g. as part of ConnId attribute information. */
        private String nativeAttributeName;

        /** Name under which this attribute is seen by the connection framework (like ConnId). */
        private String frameworkAttributeName;

        private Boolean returnedByDefault;

        private String nativeDescription;

        @Override
        public String getNativeAttributeName() {
            return nativeAttributeName;
        }

        public void setNativeAttributeName(String nativeAttributeName) {
            checkMutable();
            this.nativeAttributeName = nativeAttributeName;
        }

        @Override
        public String getFrameworkAttributeName() {
            return frameworkAttributeName;
        }

        public void setFrameworkAttributeName(String frameworkAttributeName) {
            checkMutable();
            this.frameworkAttributeName = frameworkAttributeName;
        }

        @Override
        public void setNativeDescription(String nativeDescription) {
            this.nativeDescription = nativeDescription;
        }

        @Override
        public @Nullable Boolean getReturnedByDefault() {
            return returnedByDefault;
        }

        public String getNativeDescription() {
            return nativeDescription;
        }

        public void setReturnedByDefault(Boolean returnedByDefault) {
            checkMutable();
            this.returnedByDefault = returnedByDefault;
        }

        @Override
        public void shortDump(StringBuilder sb) {
            if (nativeAttributeName != null) {
                sb.append(" native=");
                sb.append(nativeAttributeName);
            }
            if (frameworkAttributeName !=null) {
                sb.append(" framework=");
                sb.append(frameworkAttributeName);
            }
            if (returnedByDefault != null) {
                sb.append(" returnedByDefault=");
                sb.append(returnedByDefault);
            }

            if (nativeDescription != null) {
                sb.append(" nativeDescription=");
                sb.append(nativeDescription);
            }
        }

        void copyFrom(Data source) {
            this.nativeAttributeName = source.nativeAttributeName;
            this.frameworkAttributeName = source.frameworkAttributeName;
            this.returnedByDefault = source.returnedByDefault;
            this.nativeDescription = source.nativeDescription;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Data data = (Data) o;
            return Objects.equals(nativeAttributeName, data.nativeAttributeName)
                    && Objects.equals(frameworkAttributeName, data.frameworkAttributeName)
                    && Objects.equals(returnedByDefault, data.returnedByDefault)
                    && Objects.equals(nativeDescription, data.nativeDescription);
        }

        @Override
        public int hashCode() {
            return Objects.hash(nativeAttributeName, frameworkAttributeName, returnedByDefault, nativeDescription);
        }
    }
}
