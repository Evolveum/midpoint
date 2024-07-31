package com.evolveum.midpoint.model.impl;

import static org.testng.Assert.assertNotNull;

import java.io.File;
import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Created by Dominik.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestSchemaContext extends AbstractInternalModelIntegrationTest {

    private static final DummyTestResource DUMMY_RESOURCE_SOURCE = new DummyTestResource(
            new File(MidPointTestConstants.TEST_RESOURCES_DIR, "common"), "resource-dummy.xml", "10000000-0000-0000-0000-000000000004", "source");

    private static final TestObject<RoleType> ROLE_JUDGE = TestObject.file(
            new File(MidPointTestConstants.TEST_RESOURCES_DIR, "lens"), "role-judge.xml", "12345111-1111-2222-1111-121212111111");

    @Test
    public void testResourceObjectContextResolver() {
        PrismObject<ResourceType> resourceObj = DUMMY_RESOURCE_SOURCE.get();
        Item<?, ?> objectItem = resourceObj.findItem(ItemPath.create(new QName("schemaHandling"), new QName("objectType")));
        PrismValue objectPrismValue = objectItem.getAnyValue();
        ItemDefinition<?> itemDefinition = objectPrismValue.getSchemaContext().getItemDefinition();
        assertNotNull(itemDefinition.findItemDefinition(ItemPath.create(ShadowType.F_ATTRIBUTES, new QName("fullname")), ItemDefinition.class));
    }

    @Test
    public void testShadowConstructionContextResolver() {
        PrismObject<RoleType> roleObj = ROLE_JUDGE.get();
        Item<?, ?> objectItem = roleObj.findItem(ItemPath.create(new QName("inducement"), 100L, new QName("construction")));
        PrismValue objectPrismValue = objectItem.getAnyValue();
        ItemDefinition<?> shadow = objectPrismValue.getSchemaContext().getItemDefinition();
        assertNotNull(shadow.findItemDefinition(ItemPath.create(ShadowType.F_ATTRIBUTES, new QName("fullname")), ItemDefinition.class));
    }
}
