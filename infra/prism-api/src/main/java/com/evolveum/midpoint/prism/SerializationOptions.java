/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.util.annotation.Experimental;

/**
 *
 */
// FIXME: Create explicit default instance
public class SerializationOptions implements Cloneable {

    private boolean serializeCompositeObjects;
    private boolean serializeReferenceNames;
    private boolean serializeReferenceNamesForNullOids;

    /**
     * Should we skip index-only items? Their values will be omitted and they will be marked as incomplete.
     */
    private boolean skipIndexOnly;

    /**
     * Should we skip values marked as transient?
     */
    private boolean skipTransient;

    private ItemNameQualificationStrategy itemNameQualificationStrategy;

    /**
     * Makes the serialized form "standalone". Currently this means that values for items that are not present in the
     * schema registry (like attributes or connector configuration properties) will get xsi:type information.
     */
    private boolean serializeForExport;

    /**
     * Works around characters that cannot be serialized in XML by replacing them with acceptable form.
     *
     * Because the result is not machine processable it should be used with caution; for example only
     * for logging, tracing, or maybe auditing purposes.
     */
    @Experimental
    private boolean escapeInvalidCharacters;

    /**
     * Works around data types that cannot be serialized in a standard way, like `AuditEventRecord`.
     * Until we find serious way how to deal with these, this option enables their serialization as plain
     * string values. We expect that the receiving side will either treat such values appropriately,
     * or will simply ignore them.
     *
     * Because the result is not machine processable it should be used with caution; for example only
     * for logging, tracing, or maybe auditing purposes.
     */
    @Experimental
    private boolean serializeUnsupportedTypesAsString;

//    private NameQualificationStrategy itemTypeQualificationStrategy;
//    private NameQualificationStrategy itemPathQualificationStrategy;
//    private NameQualificationStrategy genericQualificationStrategy;

    public boolean isSerializeReferenceNames() {
        return serializeReferenceNames;
    }

    public void setSerializeReferenceNames(boolean serializeReferenceNames) {
        this.serializeReferenceNames = serializeReferenceNames;
    }

    public SerializationOptions serializeReferenceNames(boolean value) {
        setSerializeReferenceNames(value);
        return this;
    }

    public static SerializationOptions createSerializeReferenceNames() {
        return new SerializationOptions().serializeReferenceNames(true);
    }

    public static boolean isSerializeReferenceNames(SerializationOptions options) {
        return options != null && options.isSerializeReferenceNames();
    }

    public boolean isSerializeReferenceNamesForNullOids() {
        return serializeReferenceNamesForNullOids;
    }

    public void setSerializeReferenceNamesForNullOids(boolean serializeReferenceNames) {
        this.serializeReferenceNamesForNullOids = serializeReferenceNames;
    }

    public SerializationOptions serializeReferenceNamesForNullOids(boolean value) {
        setSerializeReferenceNamesForNullOids(value);
        return this;
    }

    public static SerializationOptions createSerializeReferenceNamesForNullOids() {
        return new SerializationOptions().serializeReferenceNamesForNullOids(true);
    }

    public static boolean isSerializeReferenceNamesForNullOids(SerializationOptions options) {
        return options != null && options.isSerializeReferenceNamesForNullOids();
    }

    public boolean isSerializeCompositeObjects() {
        return serializeCompositeObjects;
    }

    public void setSerializeCompositeObjects(boolean serializeCompositeObjects) {
        this.serializeCompositeObjects = serializeCompositeObjects;
    }

    public SerializationOptions serializeCompositeObjects(boolean value) {
        setSerializeCompositeObjects(value);
        return this;
    }

    public static SerializationOptions createSerializeCompositeObjects() {
        return new SerializationOptions().serializeCompositeObjects(true);
    }

    public static boolean isSerializeCompositeObjects(SerializationOptions options) {
        return options != null && options.isSerializeCompositeObjects();
    }

    public boolean isSerializeForExport() {
        return serializeForExport;
    }

    public void setSerializeForExport(boolean serializeForExport) {
        this.serializeForExport = serializeForExport;
    }

    public SerializationOptions serializeForExport(boolean value) {
        setSerializeForExport(value);
        return this;
    }

    public static SerializationOptions createSerializeForExport() {
        return new SerializationOptions().serializeForExport(true);
    }

    public static boolean isSerializeForExport(SerializationOptions options) {
        return options != null && options.isSerializeForExport();
    }

    public boolean isEscapeInvalidCharacters() {
        return escapeInvalidCharacters;
    }

    public void setEscapeInvalidCharacters(boolean escapeInvalidCharacters) {
        this.escapeInvalidCharacters = escapeInvalidCharacters;
    }

    public SerializationOptions escapeInvalidCharacters(boolean value) {
        setEscapeInvalidCharacters(value);
        return this;
    }

    public static SerializationOptions createEscapeInvalidCharacters() {
        return new SerializationOptions().escapeInvalidCharacters(true);
    }

    public static boolean isEscapeInvalidCharacters(SerializationOptions options) {
        return options != null && options.isEscapeInvalidCharacters();
    }

    public boolean isSerializeUnsupportedTypesAsString() {
        return serializeUnsupportedTypesAsString;
    }

    public void setSerializeUnsupportedTypesAsString(boolean value) {
        serializeUnsupportedTypesAsString = value;
    }

    public SerializationOptions serializeUnsupportedTypesAsString(boolean value) {
        setSerializeUnsupportedTypesAsString(value);
        return this;
    }

    public static boolean isSerializeUnsupportedTypesAsString(SerializationOptions options) {
        return options != null && options.isSerializeUnsupportedTypesAsString();
    }

    public void setSkipIndexOnly(boolean skipIndexOnly) {
        this.skipIndexOnly = skipIndexOnly;
    }

    public SerializationOptions skipIndexOnly(boolean value) {
        setSkipIndexOnly(value);
        return this;
    }

    public static SerializationOptions createSkipIndexOnly() {
        return new SerializationOptions().skipIndexOnly(true);
    }

    public boolean isSkipIndexOnly() {
        return skipIndexOnly;
    }

    public void setSkipTransient(boolean skipTransient) {
        this.skipTransient = skipTransient;
    }

    public SerializationOptions skipTransient(boolean value) {
        setSkipTransient(value);
        return this;
    }

    public static SerializationOptions createSkipTransient() {
        return new SerializationOptions().skipTransient(true);
    }

    public boolean isSkipTransient() {
        return skipTransient;
    }

    public static SerializationOptions createQualifiedNames() {
        SerializationOptions opts = new SerializationOptions();
        opts.itemNameQualificationStrategy = ItemNameQualificationStrategy.ALWAYS_USE_FULL_URI;
        return opts;
    }

    public static boolean isFullItemNameUris(SerializationOptions opts) {
        return opts != null && opts.itemNameQualificationStrategy != ItemNameQualificationStrategy.ALWAYS_USE_FULL_URI;
    }

    public static boolean isUseNsProperty(SerializationOptions opts) {
        return opts == null || opts.itemNameQualificationStrategy == null || opts.itemNameQualificationStrategy == ItemNameQualificationStrategy.USE_NS_PROPERTY;
    }

    public static SerializationOptions getOptions(SerializationContext sc) {
        return sc != null ? sc.getOptions() : null;
    }

    @Override
    protected SerializationOptions clone() {
        SerializationOptions clone;
        try {
            clone = (SerializationOptions) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
        return clone;
    }

    @Override
    public String toString() {
        return "SerializationOptions{" +
                "serializeCompositeObjects=" + serializeCompositeObjects +
                ", serializeReferenceNames=" + serializeReferenceNames +
                ", serializeReferenceNamesForNullOids=" + serializeReferenceNamesForNullOids +
                ", skipIndexOnly=" + skipIndexOnly +
                ", itemNameQualificationStrategy=" + itemNameQualificationStrategy +
                ", serializeForExport=" + serializeForExport +
                ", escapeInvalidCharacters=" + escapeInvalidCharacters +
                '}';
    }
}
