/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

import java.util.function.Function;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Generates extension values.
 *
 * Typical use: performance tests.
 *
 * Should be supported by appropriate "regular" extension schema.
 * See e.g. `big-user-extension.xsd` files in `model-impl` and `story` modules.
 */
public class ExtensionValueGenerator {

    private int maxNumberOfExtProperties = 50; // see big-user-extension.xsd
    private String extNs = "http://midpoint.evolveum.com/xml/ns/samples/gen";
    private String extPropertyNameFormat = "prop%04d";
    private Function<Integer, String> valueFunction = (i) -> "val " + i;

    // TODO better name
    public static ExtensionValueGenerator withDefaults() {
        return new ExtensionValueGenerator();
    }

    // TODO better name
    public static ExtensionValueGenerator with() {
        return new ExtensionValueGenerator();
    }

    @SuppressWarnings("unused")
    public int getMaxNumberOfExtProperties() {
        return maxNumberOfExtProperties;
    }

    @SuppressWarnings("unused")
    public ExtensionValueGenerator maxNumberOfExtProperties(int value) {
        this.maxNumberOfExtProperties = value;
        return this;
    }

    @SuppressWarnings("unused")
    public String getExtNs() {
        return extNs;
    }

    @SuppressWarnings("unused")
    public ExtensionValueGenerator extNs(String value) {
        this.extNs = value;
        return this;
    }

    @SuppressWarnings("unused")
    public String getExtPropertyNameFormat() {
        return extPropertyNameFormat;
    }

    @SuppressWarnings("unused")
    public ExtensionValueGenerator extPropertyNameFormat(String value) {
        this.extPropertyNameFormat = value;
        return this;
    }

    @SuppressWarnings("unused")
    public Function<Integer, String> getValueFunction() {
        return valueFunction;
    }

    @SuppressWarnings("unused")
    public ExtensionValueGenerator valueFunction(Function<Integer, String> valueFunction) {
        this.valueFunction = valueFunction;
        return this;
    }

    public void populateExtension(PrismObject<?> object, int numberOfProperties)
            throws SchemaException {
        argCheck(numberOfProperties <= maxNumberOfExtProperties,
                "Too many properties to fill: %s (max: %s)", numberOfProperties, maxNumberOfExtProperties);
        PrismContainer<?> extension = object.getExtension();
        if (extension == null) {
            extension = object.createExtension();
        }
        for (int i = 0; i < numberOfProperties; i++) {
            String propName = String.format(extPropertyNameFormat, i);
            PrismProperty<String> prop = extension.findOrCreateProperty(new ItemName(extNs, propName));
            prop.setRealValue(valueFunction.apply(i));
        }
    }
}
