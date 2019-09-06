/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema;

/**
 * @author Pavol Mederly
 */
public class DeltaConversionOptions {

    private boolean serializeReferenceNames;

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
}
