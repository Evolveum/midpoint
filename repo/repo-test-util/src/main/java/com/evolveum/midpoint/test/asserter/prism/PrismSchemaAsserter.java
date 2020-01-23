/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter.prism;

import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import static org.testng.AssertJUnit.assertEquals;

/**
 * @author semancik
 */
public class PrismSchemaAsserter<RA> extends AbstractAsserter<RA> {

    private PrismSchema schema;

    public PrismSchemaAsserter(PrismSchema schema) {
        super();
        this.schema = schema;
    }

    public PrismSchemaAsserter(PrismSchema schema, String detail) {
        super(detail);
        this.schema = schema;
    }

    public PrismSchemaAsserter(PrismSchema schema, RA returnAsserter, String detail) {
        super(returnAsserter, detail);
        this.schema = schema;
    }

    public static <O extends ObjectType> PrismSchemaAsserter<Void> forPrismSchema(PrismSchema schema) {
        return new PrismSchemaAsserter<>(schema);
    }

    public PrismSchema getSchema() {
        return schema;
    }

    public PrismSchemaAsserter<RA> assertNamespace(String expected) {
        assertEquals("Wrong namespace in "+desc(), expected, schema.getNamespace());
        return this;
    }

    protected String desc() {
        return descWithDetails(schema);
    }

    public PrismSchemaAsserter<RA> display() {
        display(desc());
        return this;
    }

    public PrismSchemaAsserter<RA> display(String message) {
        IntegrationTestTools.display(message, schema);
        return this;
    }
}
