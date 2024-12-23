/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.AbstractFreezable;

import com.evolveum.midpoint.util.DebugDumpable;

import com.evolveum.midpoint.util.DebugUtil;

import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.io.Serializable;

/**
 * Getter interface to "UCF" part of resource object class definition.
 */
public interface NativeObjectClassUcfDefinition {

    /**
     * Returns the native object class name.
     *
     * Native object class is the name of the object class as it is seen by the resource itself.
     * The name of the object class used in midPoint may be constrained by XSD or other syntax and therefore
     * may be "mangled" to conform to such syntax. The _native object class_ value will contain original,
     * un-mangled name (if available).
     *
     * Returns null if there is no native object class.
     *
     * The exception should be never thrown unless there is some bug in the code. The validation of model
     * consistency should be done at the time of schema parsing.
     *
     * @return native object class
     */
    String getNativeObjectClassName();

    /**
     * Is this an auxiliary object class, i.e., a class that can be attached to an object that already holds the structural
     * object class? This is originally an LDAP concept, but it may be applicable to other systems as well.
     */
    boolean isAuxiliary();

    /** Is this an embedded object? These are meant to be passed "by value" in reference attributes. */
    boolean isEmbedded();

    /**
     * Indicates whether definition is the default account definition.
     * (This feature is present for "dumb" resource definition that are completely without `schemaHandling` part.)
     *
     * This is a way how a resource connector may suggest applicable object classes.
     *
     * Currently the only use of this flag is that ConnId `pass:[__ACCOUNT__]` is declared
     * as a default for the kind of `ACCOUNT`.
     *
     * Originally, this property was called `defaultInAKind` and marked the object class as being default
     * for given kind. At that time, the kind was part of object class definition. This is no longer the case,
     * therefore also this property is renamed - and is available only for account-like object classes.
     * In the future we may put those things (kind + default-in-a-kind) back, if needed.
     */
    boolean isDefaultAccountDefinition();

    /**
     * Returns name of the naming attribute.
     *
     * @see #getNamingAttributeName()
     */
    @Nullable QName getNamingAttributeName();

    /**
     * Returns the definition of display name attribute.
     *
     * Display name attribute specifies which resource attribute should be used
     * as title when displaying objects of a specific resource object class. It
     * must point to an attribute of String type. If not present, primary
     * identifier should be used instead (but this method does not handle this
     * default behavior).
     *
     * Returns null if there is no display name attribute.
     *
     * The exception should be never thrown unless there is some bug in the
     * code. The validation of model consistency should be done at the time of
     * schema parsing.
     *
     * NOTE: Currently seems to be not used. (Neither not set nor used.)
     */
    @Nullable QName getDisplayNameAttributeName();

    /**
     * Returns the name of the description attribute.
     *
     * Currently seems to be unused.
     */
    @Nullable QName getDescriptionAttributeName();

    QName getPrimaryIdentifierName();

    QName getSecondaryIdentifierName();

    interface Delegable extends NativeObjectClassUcfDefinition {

        NativeObjectClassUcfDefinition ucfData();

        @Override
        default String getNativeObjectClassName() {
            return ucfData().getNativeObjectClassName();
        }

        @Override
        default boolean isAuxiliary() {
            return ucfData().isAuxiliary();
        }

        @Override
        default boolean isEmbedded() {
            return ucfData().isEmbedded();
        }

        @Override
        default boolean isDefaultAccountDefinition() {
            return ucfData().isDefaultAccountDefinition();
        }

        @Override
        default @Nullable QName getNamingAttributeName() {
            return ucfData().getNamingAttributeName();
        }

        @Override
        default @Nullable QName getDisplayNameAttributeName() {
            return ucfData().getDisplayNameAttributeName();
        }

        @Override
        default @Nullable QName getDescriptionAttributeName() {
            return ucfData().getDescriptionAttributeName();
        }

        @Override
        default QName getPrimaryIdentifierName() {
            return ucfData().getPrimaryIdentifierName();
        }

        @Override
        default QName getSecondaryIdentifierName() {
            return ucfData().getSecondaryIdentifierName();
        }
    }

    interface Mutable {

        void setNativeObjectClassName(String value);
        void setAuxiliary(boolean value);
        void setEmbedded(boolean value);
        void setDefaultAccountDefinition(boolean value);
        void setNamingAttributeName(QName value);
        void setDisplayNameAttributeName(QName value);
        void setDescriptionAttributeName(QName value);
        void setPrimaryIdentifierName(QName value);
        void setSecondaryIdentifierName(QName value);

        interface Delegable extends Mutable {

            Mutable ucfData();

            default void setNativeObjectClassName(String value) {
                ucfData().setNativeObjectClassName(value);
            }

            default void setAuxiliary(boolean value) {
                ucfData().setAuxiliary(value);
            }

            default void setEmbedded(boolean value) {
                ucfData().setEmbedded(value);
            }

            default void setDefaultAccountDefinition(boolean value) {
                ucfData().setDefaultAccountDefinition(value);
            }

            @Override
            default void setNamingAttributeName(QName value) {
                ucfData().setNamingAttributeName(value);
            }

            @Override
            default void setDisplayNameAttributeName(QName value) {
                ucfData().setDisplayNameAttributeName(value);
            }

            @Override
            default void setDescriptionAttributeName(QName value) {
                ucfData().setDescriptionAttributeName(value);
            }

            @Override
            default void setPrimaryIdentifierName(QName value) {
                ucfData().setPrimaryIdentifierName(value);
            }
            @Override
            default void setSecondaryIdentifierName(QName value) {
                ucfData().setSecondaryIdentifierName(value);
            }
        }
    }

    class Data
            extends AbstractFreezable
            implements NativeObjectClassUcfDefinition, NativeObjectClassUcfDefinition.Mutable, Serializable, DebugDumpable {

        private String nativeObjectClassName;
        private boolean auxiliary;
        private boolean embedded;
        private boolean defaultAccountDefinition;

        private QName namingAttributeName;
        private QName displayNameAttributeName;
        private QName descriptionAttributeName;

        private QName primaryIdentifierName;
        private QName secondaryIdentifierName;

        @Override
        public String getNativeObjectClassName() {
            return nativeObjectClassName;
        }

        @Override
        public void setNativeObjectClassName(String value) {
            checkMutable();
            this.nativeObjectClassName = value;
        }

        @Override
        public boolean isAuxiliary() {
            return auxiliary;
        }

        @Override
        public void setAuxiliary(boolean value) {
            checkMutable();
            this.auxiliary = value;
        }

        @Override
        public boolean isEmbedded() {
            return embedded;
        }

        @Override
        public void setEmbedded(boolean value) {
            this.embedded = value;
        }

        @Override
        public boolean isDefaultAccountDefinition() {
            return defaultAccountDefinition;
        }

        @Override
        public void setDefaultAccountDefinition(boolean value) {
            checkMutable();
            this.defaultAccountDefinition = value;
        }

        @Override
        public @Nullable QName getNamingAttributeName() {
            return namingAttributeName;
        }

        public void setNamingAttributeName(QName namingAttributeName) {
            checkMutable();
            this.namingAttributeName = namingAttributeName;
        }

        @Override
        public @Nullable QName getDisplayNameAttributeName() {
            return displayNameAttributeName;
        }

        public void setDisplayNameAttributeName(QName displayNameAttributeName) {
            checkMutable();
            this.displayNameAttributeName = displayNameAttributeName;
        }

        @Override
        public @Nullable QName getDescriptionAttributeName() {
            return descriptionAttributeName;
        }

        public void setDescriptionAttributeName(QName descriptionAttributeName) {
            checkMutable();
            this.descriptionAttributeName = descriptionAttributeName;
        }

        @Override
        public QName getPrimaryIdentifierName() {
            return primaryIdentifierName;
        }

        @Override
        public void setPrimaryIdentifierName(QName value) {
            checkMutable();
            this.primaryIdentifierName = value;
        }

        @Override
        public QName getSecondaryIdentifierName() {
            return secondaryIdentifierName;
        }

        @Override
        public void setSecondaryIdentifierName(QName value) {
            checkMutable();
            this.secondaryIdentifierName = value;
        }

        void copyFrom(NativeObjectClassUcfDefinition source) {
            this.nativeObjectClassName = source.getNativeObjectClassName();
            this.auxiliary = source.isAuxiliary();
            this.embedded = source.isEmbedded();
            this.defaultAccountDefinition = source.isDefaultAccountDefinition();
            this.namingAttributeName = source.getNamingAttributeName();
            this.displayNameAttributeName = source.getDisplayNameAttributeName();
            this.descriptionAttributeName = source.getDescriptionAttributeName();
            this.primaryIdentifierName = source.getPrimaryIdentifierName();
            this.secondaryIdentifierName = source.getSecondaryIdentifierName();
        }

        @Override
        public String debugDump(int indent) {
            var sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
            DebugUtil.debugDumpWithLabelLn(sb, "nativeObjectClassName", nativeObjectClassName, indent + 1);
            DebugUtil.debugDumpWithLabelLn(sb, "auxiliary", auxiliary, indent + 1);
            DebugUtil.debugDumpWithLabelLn(sb, "embedded", embedded, indent + 1);
            DebugUtil.debugDumpWithLabelLn(sb, "defaultAccountDefinition", defaultAccountDefinition, indent + 1);
            DebugUtil.debugDumpWithLabelLn(sb, "namingAttributeName", namingAttributeName, indent + 1);
            DebugUtil.debugDumpWithLabelLn(sb, "displayNameAttributeName", displayNameAttributeName, indent + 1);
            DebugUtil.debugDumpWithLabelLn(sb, "descriptionAttributeName", descriptionAttributeName, indent + 1);
            DebugUtil.debugDumpWithLabelLn(sb, "primaryIdentifierName", primaryIdentifierName, indent + 1);
            DebugUtil.debugDumpWithLabel(sb, "secondaryIdentifierName", secondaryIdentifierName, indent + 1);
            return sb.toString();
        }
    }
}
