/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import static org.testng.Assert.*;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.xml.namespace.QName;

import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.spi.Connector;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.NativeResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/** Tests reference filtering during schema generation and object conversion. */
public class TestConnIdReferenceFiltering {

    @BeforeClass
    public void initPrism() throws Exception {
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    /** An excluded reference is omitted from both the schema and the converted object. */
    @Test
    public void excludesReferencesToUnselectedClasses() throws Exception {
        var selected = List.of(name("AccessGrant"));
        var schema = parse(selected, false);
        var grant = schema.findObjectClassDefinition(name("AccessGrant"));
        assertNotNull(grant);
        assertNull(schema.findObjectClassDefinition(name("App")));
        assertTrue(grant.getReferenceAttributeDefinitions().isEmpty());
        assertNotNull(grant.findSimpleAttributeDefinition(name("resource_id")));
        assertTrue(schema.getReferenceTypeDefinitions().isEmpty());
        assertNotNull(ResourceSchemaFactory.nativeToBare(schema));

        var conversion = conversion(schema, selected, reference("App"));
        conversion.execute();
        var shadow = conversion.getUcfResourceObjectIfSuccess().getBean();
        assertTrue(ShadowUtil.getReferenceAttributes(shadow).isEmpty());
        assertEquals(ShadowUtil.getSimpleAttribute(shadow, name("resource_id")).getRealValue(), "app-1");
    }

    /** Selecting both classes keeps the reference in the schema and the converted object. */
    @Test
    public void preservesReferencesToSelectedClasses() throws Exception {
        var selected = List.of(name("AccessGrant"), name("App"));
        var schema = parse(selected, false);
        assertReferencePresent(schema);

        var conversion = conversion(schema, selected, reference("App"));
        conversion.execute();
        assertEquals(ShadowUtil.getReferenceAttributes(conversion.getUcfResourceObjectIfSuccess().getBean()).size(), 1);
    }

    /** Without generation constraints, all reference targets are included. */
    @Test
    public void preservesReferencesWithoutGenerationConstraints() throws Exception {
        var schema = parse(List.of(), false);
        assertReferencePresent(schema);

        var conversion = conversion(schema, List.of(), reference("App"));
        conversion.execute();
        assertEquals(ShadowUtil.getReferenceAttributes(conversion.getUcfResourceObjectIfSuccess().getBean()).size(), 1);
    }

    /** Class selection also works with legacy midPoint names. */
    @Test
    public void appliesConstraintsUsingLegacyObjectClassNames() throws Exception {
        var schema = parse(List.of(name("CustomAccessGrantObjectClass")), true);
        assertTrue(Objects.requireNonNull(schema.findObjectClassDefinition(name("CustomAccessGrantObjectClass")))
                .getReferenceAttributeDefinitions().isEmpty());
        assertNotNull(ResourceSchemaFactory.nativeToBare(schema));
    }

    /** Embedded attributes keep their existing parsing behavior. */
    @Test
    public void preservesEmbeddedAttributesWhenTargetIsExcluded() throws Exception {
        var schema = parse(List.of(name("AccessGrant")), false, true);
        assertNotNull(Objects.requireNonNull(schema.findObjectClassDefinition(name("AccessGrant")))
                .findReferenceAttributeDefinition(name("app")));
    }

    /** Filtering references must not hide unknown scalar attributes. */
    @Test
    public void stillRejectsUnknownSimpleAttributes() throws Exception {
        var conversion = conversion(List.of(name("AccessGrant")), true, AttributeBuilder.build("unexpected", "value"));
        assertRejected(conversion, "unexpected");
    }

    /** A reference missing from the connector schema is still an error. */
    @Test
    public void stillRejectsUndeclaredReferencesToIncludedClasses() throws Exception {
        var conversion = conversion(List.of(), false, reference("App"));
        assertRejected(conversion, "app");
    }

    /** Mixed or empty values cannot be safely identified as excluded references. */
    @Test
    public void stillRejectsMixedAndEmptyUnknownAttributes() throws Exception {
        var mixed = AttributeBuilder.build("app", List.of(referenceValue("App"), "invalid"));
        assertRejected(conversion(List.of(name("AccessGrant")), true, mixed), "app");
        assertRejected(conversion(List.of(name("AccessGrant")), true, AttributeBuilder.build("app", List.of())), "app");
    }

    /** Checks that conversion reports an error for the named attribute. */
    private void assertRejected(ConnIdToUcfObjectConversion conversion, String name) throws Exception {
        conversion.execute();
        var failure = expectThrows(SchemaException.class, conversion::getUcfResourceObjectIfSuccess);
        assertTrue(failure.getMessage().contains("'" + name + "'"), failure.getMessage());
    }

    /**
     * Creates an AccessGrant object and the schema used to convert it.
     * The declared flag controls whether the connector schema defines the app reference.
     */
    private ConnIdToUcfObjectConversion conversion(Collection<QName> selected, boolean declared, Attribute extra)
            throws Exception {
        var nativeSchema = new ConnIdSchemaParser(createSchema(declared, false), selected, false).parse().parsedSchema();
        return conversion(nativeSchema, selected, extra);
    }

    private ConnIdToUcfObjectConversion conversion(NativeResourceSchema nativeSchema, Collection<QName> selected, Attribute extra)
            throws Exception {
        var schema = ResourceSchemaFactory.parseCompleteSchema(new ResourceType().name("test"), nativeSchema);
        var object = new ConnectorObjectBuilder().setObjectClass(new ObjectClass("AccessGrant"))
                .setUid("grant-1").setName("grant-1")
                .addAttribute("resource_id", "app-1").addAttribute(extra).build();
        ConnectorContext context = new ConnectorContext() {
            public Boolean getConfiguredLegacySchema() {
                return false;
            }

            public boolean isLegacySchema() {
                return false;
            }

            public String getHumanReadableName() {
                return "test";
            }

            public boolean isObjectClassExcludedFromSchema(QName className) {
                return !selected.isEmpty() && !selected.contains(className);
            }
        };
        return new ConnIdToUcfObjectConversion(object, schema.findDefinitionForObjectClassRequired(name("AccessGrant")), context, schema);
    }

    private Attribute reference(String target) {
        return AttributeBuilder.build("app", referenceValue(target));
    }

    private ConnectorObjectReference referenceValue(String target) {
        return new ConnectorObjectReference(new ConnectorObjectIdentification(new ObjectClass(target), Set.of(new Uid("app-1"))));
    }

    private void assertReferencePresent(NativeResourceSchema schema) throws Exception {
        assertNotNull(schema.findObjectClassDefinition(name("App")));
        assertNotNull(Objects.requireNonNull(schema.findObjectClassDefinition(name("AccessGrant")))
                .findReferenceAttributeDefinition(name("app")));
        assertFalse(schema.getReferenceTypeDefinitions().isEmpty());
        assertNotNull(ResourceSchemaFactory.nativeToBare(schema));
    }

    private NativeResourceSchema parse(Collection<QName> selected, boolean legacy) throws Exception {
        return parse(selected, legacy, false);
    }

    private NativeResourceSchema parse(Collection<QName> selected, boolean legacy, boolean embedded) throws Exception {
        return new ConnIdSchemaParser(createSchema(true, embedded), selected, legacy).parse().parsedSchema();
    }

    /** Builds the two-class fixture shared by schema and conversion tests. */
    private Schema createSchema(boolean declared, boolean embedded) {
        var grant = new ObjectClassInfoBuilder();
        grant.setType("AccessGrant");
        grant.addAttributeInfo(AttributeInfoBuilder.build(Name.NAME));
        grant.addAttributeInfo(AttributeInfoBuilder.build("resource_id"));
        if (declared) {
            grant.addAttributeInfo(new AttributeInfoBuilder("app", embedded ? EmbeddedObject.class : ConnectorObjectReference.class)
                    .setReferencedObjectClassName("App")
                    .setRoleInReference(AttributeInfo.RoleInReference.SUBJECT.toString())
                    .build());
        }
        var app = new ObjectClassInfoBuilder();
        app.setType("App");
        app.addAttributeInfo(AttributeInfoBuilder.build(Name.NAME));
        var builder = new SchemaBuilder(Connector.class);
        builder.defineObjectClass(grant.build());
        builder.defineObjectClass(app.build());
        return builder.build();
    }

    private QName name(String local) {
        return new QName(SchemaConstants.NS_RI, local);
    }
}
