/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter.refinedschema;

import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.test.asserter.prism.PrismSchemaAsserter;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author Radovan Semancik
 */
public class RefinedResourceSchemaAsserter<RA> extends PrismSchemaAsserter<RA> {

    public RefinedResourceSchemaAsserter(ResourceSchema schema) {
        super(schema);
    }

    public RefinedResourceSchemaAsserter(ResourceSchema schema, String detail) {
        super(schema, detail);
    }

    public RefinedResourceSchemaAsserter(ResourceSchema schema, RA returnAsserter, String detail) {
        super(schema, returnAsserter, detail);
    }

    public static RefinedResourceSchemaAsserter<Void> forRefinedResourceSchema(ResourceSchema schema) {
        return new RefinedResourceSchemaAsserter<>(schema);
    }

    public static RefinedResourceSchemaAsserter<Void> forResource(PrismObject<ResourceType> resource)
            throws SchemaException, ConfigurationException {
        ResourceSchema refinedSchema = ResourceSchemaFactory.getCompleteSchema(resource);
        assertNotNull("No refined schema for "+resource, refinedSchema);
        return new RefinedResourceSchemaAsserter<>(refinedSchema, resource.toString());
    }

    public static RefinedResourceSchemaAsserter<Void> forResource(PrismObject<ResourceType> resource, String details)
            throws SchemaException, ConfigurationException {
        ResourceSchema refinedSchema = ResourceSchemaFactory.getCompleteSchema(resource);
        assertNotNull("No refined schema for "+resource+" ("+details+")", refinedSchema);
        return new RefinedResourceSchemaAsserter<>(refinedSchema, resource +" ("+details+")");
    }

    public ResourceSchema getSchema() {
        return (ResourceSchema) super.getSchema();
    }

    public RefinedResourceSchemaAsserter<RA> assertNamespace(String expected) {
        super.assertNamespace(expected);
        return this;
    }

    public ResourceObjectDefinitionAsserter<RefinedResourceSchemaAsserter<RA>> objectClass(QName ocQname) {
        ResourceObjectDefinition objectClassDefinition = getSchema().findDefinitionForObjectClass(ocQname);
        ResourceObjectDefinitionAsserter<RefinedResourceSchemaAsserter<RA>> asserter = new ResourceObjectDefinitionAsserter<>(objectClassDefinition, this, desc());
        copySetupTo(asserter);
        return asserter;
    }

    public ResourceObjectDefinitionAsserter<RefinedResourceSchemaAsserter<RA>> objectClass(String ocName) {
        ResourceObjectDefinition objectClassDefinition =
                getSchema().findDefinitionForObjectClass(toRiQName(ocName));
        ResourceObjectDefinitionAsserter<RefinedResourceSchemaAsserter<RA>> asserter = new ResourceObjectDefinitionAsserter<>(objectClassDefinition, this, desc());
        copySetupTo(asserter);
        return asserter;
    }

    @NotNull
    private QName toRiQName(String ocName) {
        return new QName(MidPointConstants.NS_RI, ocName);
    }

    public ResourceObjectDefinitionAsserter<RefinedResourceSchemaAsserter<RA>> defaultAccountDefinition() {
        ResourceObjectDefinition objectClassDefinition = getSchema().findDefaultDefinitionForKind(ShadowKindType.ACCOUNT);
        ResourceObjectDefinitionAsserter<RefinedResourceSchemaAsserter<RA>> asserter = new ResourceObjectDefinitionAsserter<>(objectClassDefinition, this, "default account definition in " + desc());
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
        PrismTestUtil.display(message, getSchema());
        return this;
    }
}
