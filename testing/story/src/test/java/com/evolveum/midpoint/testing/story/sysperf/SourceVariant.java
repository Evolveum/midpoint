/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.sysperf;

import com.evolveum.icf.dummy.resource.ConflictException;
import com.evolveum.icf.dummy.resource.DummyObjectClass;
import com.evolveum.icf.dummy.resource.SchemaViolationException;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.DummyTestResource;

import java.io.FileNotFoundException;
import java.net.ConnectException;

import static com.evolveum.midpoint.testing.story.sysperf.TestSystemPerformance.TEST_DIR;

enum SourceVariant {

    MS2_5("ms2-5", "resource-source-ms0002.xml", 1, 1, 5),
    MS20_5("ms20-5", "resource-source-ms0020.xml", 10, 10, 5),
    MS110_5("ms110-5", "resource-source-ms0110.xml", 100, 10, 5),
    MS110_1000("ms110-1000", "resource-source-ms0110.xml", 100, 10, 1000);

    private static final String PROP_SOURCE = "source";

    public static final String RESOURCE_INSTANCE = "source";
    private static final String RESOURCE_OID = "7eb3a16c-3a33-4ef0-8523-98f0ef7291ba";
    public static final String A_SINGLE_NAME = "a-single-%04d";
    public static final String A_MULTI_NAME = "a-multi-%04d";

    private final String name;
    private final String fileName;
    private final int singleValuedAttributes;
    private final int multiValuedAttributes;
    private final int attributeValues;

    SourceVariant(String name, String fileName, int singleValuedAttributes, int multiValuedAttributes, int attributeValues) {
        this.name = name;
        this.fileName = fileName;
        this.singleValuedAttributes = singleValuedAttributes;
        this.multiValuedAttributes = multiValuedAttributes;
        this.attributeValues = attributeValues;
    }

    public String getName() {
        return name;
    }

    public String getFileName() {
        return fileName;
    }

    public int getSingleValuedAttributes() {
        return singleValuedAttributes;
    }

    public int getMultiValuedAttributes() {
        return multiValuedAttributes;
    }

    public int getAttributeValues() {
        return attributeValues;
    }

    public static SourceVariant setup() {
        String configuredName = System.getProperty(PROP_SOURCE, MS2_5.name);
        SourceVariant sourceVariant = fromName(configuredName);
        System.out.println("Source variant: " + sourceVariant);
        return sourceVariant;
    }

    private static SourceVariant fromName(String name) {
        for (SourceVariant value : values()) {
            if (value.name.equals(name)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown source resource variant: " + name);
    }

    public DummyTestResource createDummyTestResource() {
        return new DummyTestResource(TEST_DIR, fileName, RESOURCE_OID, RESOURCE_INSTANCE,
                controller -> {
                    createAttributes(controller, A_SINGLE_NAME, singleValuedAttributes, false);
                    createAttributes(controller, A_MULTI_NAME, multiValuedAttributes, true);
                });
    }

    private void createAttributes(DummyResourceContoller controller, String name, int number, boolean multi)
            throws ConflictException, FileNotFoundException, SchemaViolationException, InterruptedException, ConnectException {
        DummyObjectClass objectClass = controller.getDummyResource().getAccountObjectClass();
        for (int i = 0; i < number; i++) {
            controller.addAttrDef(objectClass, String.format(name, i), String.class, false, multi);
        }
    }
}
