/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story;

import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TracingProfileType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.ITestContext;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Collection;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * An attempt to test performance of various operations on large groups.
 * E.g. MID-5836 but others as well (in the future).
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestLargeGroups extends AbstractStoryTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "large-groups");

    protected static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy.xml");
    protected static final String RESOURCE_DUMMY_ID = null;
    protected static final String RESOURCE_DUMMY_OID = "b07a7f15-61f6-4769-b697-083b7ab4f995";

    protected static DummyResource dummyResource;
    protected static DummyResourceContoller dummyResourceCtl;
    protected PrismObject<ResourceType> resourceDummy;

    private static final ItemName ATTR_MEMBERS = new ItemName(MidPointConstants.NS_RI, "members");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        // Resources
        dummyResourceCtl = DummyResourceContoller.create(RESOURCE_DUMMY_ID, resourceDummy);
        dummyResourceCtl.extendSchemaPirate();
        dummyResource = dummyResourceCtl.getDummyResource();
        resourceDummy = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_FILE, RESOURCE_DUMMY_OID, initTask, initResult);
        dummyResourceCtl.setResource(resourceDummy);
        dummyResource.setSyncStyle(DummySyncStyle.SMART);
    }

    @Override
    protected TracingProfileType getTestMethodTracingProfile() {
        return createModelLoggingTracingProfile()
                .fileNamePattern(TEST_METHOD_TRACING_FILENAME_PATTERN);
    }

    @Test
    public void test000Sanity() throws Exception {
        Task task = getTestTask();

        OperationResult testResult = modelService.testResource(RESOURCE_DUMMY_OID, task);
        TestUtil.assertSuccess(testResult);
    }

    /**
     * Test retrieval of a large group.
     */
    @Test
    public void test100GetLargeGroup(ITestContext ctx) throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        final int MEMBERS = 20000;

        DummyGroup group1 = dummyResourceCtl.addGroup("group1");
        for (int i = 0; i < MEMBERS; i++) {
            group1.addMember(String.format("member-%09d", i));
        }

        Collection<SelectorOptions<GetOperationOptions>> options = schemaHelper.getOperationOptionsBuilder()
                // MID-5838
                .item(ShadowType.F_ATTRIBUTES, ATTR_MEMBERS).retrieve()
                .build();
        assert100LargeGroupSearch(ctx, options, MEMBERS);

        // Legacy behavior (MID-5838)
        Collection<SelectorOptions<GetOperationOptions>> badOptions = schemaHelper.getOperationOptionsBuilder()
                .item(/*ShadowType.F_ATTRIBUTES,*/ ATTR_MEMBERS).retrieve()
                .build();
        assert100LargeGroupSearch(ctx, badOptions, MEMBERS);
    }

    private void assert100LargeGroupSearch(
            ITestContext ctx, Collection<SelectorOptions<GetOperationOptions>> options, final int MEMBERS)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        ResourceAttributeDefinition<Object> nameDefinition = libraryMidpointFunctions
                .getAttributeDefinition(resourceDummy, dummyResourceCtl.getGroupObjectClass(), SchemaConstants.ICFS_NAME);
        ObjectQuery query = prismContext.queryFor(ShadowType.class)
                .item(ShadowType.F_RESOURCE_REF).ref(resourceDummy.getOid())
                .and().item(ShadowType.F_KIND).eq(ShadowKindType.ENTITLEMENT)
                .and().item(ItemPath.create(ShadowType.F_ATTRIBUTES, SchemaConstants.ICFS_NAME), nameDefinition)
                    .eq("group1")
                .build();

        SearchResultList<PrismObject<ShadowType>> groups = provisioningService
                .searchObjects(ShadowType.class, query, options, task, result);

        assertEquals("Wrong # of groups found", 1, groups.size());
        PrismObject<ShadowType> group = groups.get(0);
        PrismProperty<String> membersProperty = group.findProperty(ItemPath.create(ShadowType.F_ATTRIBUTES, ATTR_MEMBERS));
        assertNotNull("Members attribute was not found", membersProperty);
        assertEquals("Wrong # of members", MEMBERS, membersProperty.size());
    }
}
