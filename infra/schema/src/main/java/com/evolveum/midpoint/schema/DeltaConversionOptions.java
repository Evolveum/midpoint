/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.util.annotation.Experimental;

/**
 * Options used when serializing deltas to "bean" form (ObjectDeltaType).
 */
public class DeltaConversionOptions {

    /**
     * Should we serialize target names in object references?
     */
    private boolean serializeReferenceNames;

    /**
     * Works around characters that cannot be serialized in XML by replacing them with appropriate
     * form.
     *
     * The result will not be machine processable, so it should be used with caution: for example
     * only for logging, tracing, or maybe auditing purposes.
     *
     * See analogous option in {@link com.evolveum.midpoint.prism.SerializationOptions}.
     */
    @Experimental
    private boolean escapeInvalidCharacters;

    public boolean isSerializeReferenceNames() {
        return serializeReferenceNames;
    }

    public void setSerializeReferenceNames(boolean serializeReferenceNames) {
        this.serializeReferenceNames = serializeReferenceNames;
    }

    public static boolean isSerializeReferenceNames(DeltaConversionOptions options) {
        return options != null && options.isSerializeReferenceNames();
    }

    public static DeltaConversionOptions createSerializeReferenceNames() {
        DeltaConversionOptions options = new DeltaConversionOptions();
        options.setSerializeReferenceNames(true);
        return options;
    }

    public boolean isEscapeInvalidCharacters() {
        return escapeInvalidCharacters;
    }

    public void setEscapeInvalidCharacters(boolean escapeInvalidCharacters) {
        this.escapeInvalidCharacters = escapeInvalidCharacters;
    }

    public static boolean isEscapeInvalidCharacters(DeltaConversionOptions options) {
        return options != null && options.isEscapeInvalidCharacters();
    }
}
