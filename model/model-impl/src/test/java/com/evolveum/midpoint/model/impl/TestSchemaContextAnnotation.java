package com.evolveum.midpoint.model.impl;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;

import static org.testng.Assert.assertNotNull;

/**
 * Created by Dominik.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestSchemaContextAnnotation extends AbstractInternalModelIntegrationTest {

    protected static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "common");

    private static final DummyTestResource DUMMY_RESOURCE_SOURCE = new DummyTestResource(
            TEST_DIR, "resource-dummy.xml", "10000000-0000-0000-0000-000000000004", "source");

    @Test
    public void testResourceObjectContextResolver() {
        PrismObject<ResourceType> resourceObj = DUMMY_RESOURCE_SOURCE.get();
        Item<?, ?> objectItem = resourceObj.findItem(ItemPath.create(new QName("schemaHandling"), new QName("objectType")));
        PrismValue objectPrismValue = objectItem.getAnyValue();

        assertNotNull(objectPrismValue.getSchemaContext().getItemDefinition().findItemDefinition(ItemPath.create(ShadowType.F_ATTRIBUTES, new QName("fullname")), ItemDefinition.class));
    }
}
