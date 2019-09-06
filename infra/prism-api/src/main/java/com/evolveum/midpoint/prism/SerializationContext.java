/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism;

/**
 * Everything we want to maintain during the serialization process.
 * (First of all, processing options.)
 *
 * @author Pavol Mederly
 */
public class SerializationContext implements Cloneable {

    private SerializationOptions options;

    public SerializationContext(SerializationOptions options) {
        this.options = options;
    }

    public SerializationOptions getOptions() {
        return options;
    }

    public void setOptions(SerializationOptions options) {
        this.options = options;
    }

    public static boolean isSerializeReferenceNames(SerializationContext ctx) {
        return ctx != null && SerializationOptions.isSerializeReferenceNames(ctx.getOptions());
    }

    public static boolean isSerializeReferenceNamesForNullOids(SerializationContext ctx) {
        return ctx != null && SerializationOptions.isSerializeReferenceNamesForNullOids(ctx.getOptions());
    }

    public static boolean isSerializeCompositeObjects(SerializationContext ctx) {
        return ctx != null && SerializationOptions.isSerializeCompositeObjects(ctx.getOptions());
    }

    public static boolean isSerializeForExport(SerializationContext ctx) {
        return ctx != null && SerializationOptions.isSerializeForExport(ctx.getOptions());
    }

    public static SerializationContext forOptions(SerializationOptions options) {
        return new SerializationContext(options);
    }

    @Override
    public SerializationContext clone() {
        SerializationContext clone;
        try {
            clone = (SerializationContext) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
        if (options != null) {
            clone.options = options.clone();
        }
        return clone;
    }
}
