/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter.refinedschema;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.prism.PrismSchemaAsserter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import javax.xml.namespace.QName;

import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author Radovan Semancik
 */
public class RefinedResourceSchemaAsserter<RA> extends PrismSchemaAsserter<RA> {

    public RefinedResourceSchemaAsserter(RefinedResourceSchema schema) {
        super(schema);
    }

    public RefinedResourceSchemaAsserter(RefinedResourceSchema schema, String detail) {
        super(schema, detail);
    }

    public RefinedResourceSchemaAsserter(RefinedResourceSchema schema, RA returnAsserter, String detail) {
        super(schema, returnAsserter, detail);
    }

    public static RefinedResourceSchemaAsserter<Void> forRefinedResourceSchema(RefinedResourceSchema schema) {
        return new RefinedResourceSchemaAsserter<>(schema);
    }

    public static RefinedResourceSchemaAsserter<Void> forResource(PrismObject<ResourceType> resource) throws SchemaException {
        RefinedResourceSchema refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource, resource.getPrismContext());
        assertNotNull("No refined schema for "+resource, refinedSchema);
        return new RefinedResourceSchemaAsserter<>(refinedSchema, resource.toString());
    }

    public static RefinedResourceSchemaAsserter<Void> forResource(PrismObject<ResourceType> resource, String details) throws SchemaException {
        RefinedResourceSchema refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource, resource.getPrismContext());
        assertNotNull("No refined schema for "+resource+" ("+details+")", refinedSchema);
        return new RefinedResourceSchemaAsserter<>(refinedSchema, resource.toString()+" ("+details+")");
    }

    public RefinedResourceSchema getSchema() {
        return (RefinedResourceSchema) super.getSchema();
    }

    public RefinedResourceSchemaAsserter<RA> assertNamespace(String expected) {
        super.assertNamespace(expected);
        return this;
    }

    public ObjectClassComplexTypeDefinitionAsserter<RefinedResourceSchemaAsserter<RA>> objectClass(QName ocQname) {
        ObjectClassComplexTypeDefinition objectClassDefinition = getSchema().findObjectClassDefinition(ocQname);
        ObjectClassComplexTypeDefinitionAsserter<RefinedResourceSchemaAsserter<RA>> asserter = new ObjectClassComplexTypeDefinitionAsserter<>(objectClassDefinition, this, desc());
        copySetupTo(asserter);
        return asserter;
    }

    public ObjectClassComplexTypeDefinitionAsserter<RefinedResourceSchemaAsserter<RA>> objectClass(String ocName) {
        ObjectClassComplexTypeDefinition objectClassDefinition = getSchema().findObjectClassDefinition(ocName);
        ObjectClassComplexTypeDefinitionAsserter<RefinedResourceSchemaAsserter<RA>> asserter = new ObjectClassComplexTypeDefinitionAsserter<>(objectClassDefinition, this, desc());
        copySetupTo(asserter);
        return asserter;
    }

    public ObjectClassComplexTypeDefinitionAsserter<RefinedResourceSchemaAsserter<RA>> defaultDefinition(ShadowKindType kind) {
        RefinedObjectClassDefinition objectClassDefinition = getSchema().getDefaultRefinedDefinition(kind);
        ObjectClassComplexTypeDefinitionAsserter<RefinedResourceSchemaAsserter<RA>> asserter = new ObjectClassComplexTypeDefinitionAsserter<>(objectClassDefinition, this, "default definition for kind " + kind + " in " + desc());
        copySetupTo(asserter);
        return asserter;
    }

    public ObjectClassComplexTypeDefinitionAsserter<RefinedResourceSchemaAsserter<RA>> defaultAccountDefinition() {
        RefinedObjectClassDefinition objectClassDefinition = getSchema().getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
        ObjectClassComplexTypeDefinitionAsserter<RefinedResourceSchemaAsserter<RA>> asserter = new ObjectClassComplexTypeDefinitionAsserter<>(objectClassDefinition, this, "default account definition in " + desc());
        copySetupTo(asserter);
        return asserter;
    }

    protected String desc() {
        return descWithDetails("refined schema");
    }

    public RefinedResourceSchemaAsserter<RA> display() {
        display(desc());
        return this;
    }

    public RefinedResourceSchemaAsserter<RA> display(String message) {
        IntegrationTestTools.display(message, getSchema());
        return this;
    }
}
