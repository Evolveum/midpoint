/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl;

import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_DIR;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertNotNull;

import java.io.File;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.schemaContext.SchemaContext;
import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.test.TestObject;

/**
 * Tests for "schema context" annotation feature, see https://docs.evolveum.com/midpoint/devel/schema-context-annotations/,
 * using real midPoint schema. (Low-level tests are in `prism-impl`.)
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestSchemaContext extends AbstractInternalModelIntegrationTest {

    private static final File TEST_DIR = new File(TEST_RESOURCES_DIR, "schema-context");

    private static final TestObject<RoleType> ROLE_JUDGE_AUTOASSIGNED = TestObject.file(
            TEST_DIR, "role-judge-autoassigned.xml", "41190e78-f53f-472c-b2e3-b9f49c3ffc69");

    @Test
    public void testResourceObjectContextResolver() throws CommonException {
        PrismObject<ResourceType> resourceObj = getObject(ResourceType.class, RESOURCE_DUMMY_OID);
        Item<?, ?> objectItem = resourceObj.findItem(ItemPath.create(new QName("schemaHandling"), new QName("objectType")));
        PrismValue objectPrismValue = objectItem.getAnyValue();
        ItemDefinition<?> itemDefinition = objectPrismValue.getSchemaContext().getItemDefinition();
        assertNotNull(itemDefinition.findItemDefinition(ItemPath.create(ShadowType.F_ATTRIBUTES, new QName("fullname")), ItemDefinition.class));
    }

    @Test
    public void testShadowConstructionContextResolver() {
        PrismObject<RoleType> roleObj = ROLE_JUDGE_AUTOASSIGNED.get();
        Item<?, ?> objectItem = roleObj.findItem(ItemPath.create(new QName("inducement"), 100L, new QName("construction")));
        PrismValue objectPrismValue = objectItem.getAnyValue();
        ItemDefinition<?> shadow = objectPrismValue.getSchemaContext().getItemDefinition();
        assertNotNull(shadow.findItemDefinition(ItemPath.create(ShadowType.F_ATTRIBUTES, new QName("fullname")), ItemDefinition.class));
    }

    /** Checks the context for `RoleType:autoassign/focus` (related to filters in auto-assignment conditions). #4684/#11865. */
    @Test
    public void testRoleAutoassignContext() {

        when("asking for schema context for autoassignment mapping expression");
        var conditionSchemaContext = ROLE_JUDGE_AUTOASSIGNED.get()
                .findItem(ItemPath.create(
                        RoleType.F_AUTOASSIGN,
                        AutoassignSpecificationType.F_FOCUS,
                        FocalAutoassignSpecificationType.F_MAPPING,
                        12345L,
                        MappingType.F_CONDITION))
                .getAnyValue()
                .getSchemaContext();

        then("it points to a ServiceType");
        assertThat(conditionSchemaContext)
                .as("schema context for condition")
                .isNotNull()
                .extracting(SchemaContext::getItemDefinition)
                .as("referenced type definition in schema context for condition")
                .isNotNull()
                .extracting(ItemDefinition::getTypeName)
                .as("referenced type name in schema context for condition")
                .isEqualTo(ServiceType.COMPLEX_TYPE);
    }
}
