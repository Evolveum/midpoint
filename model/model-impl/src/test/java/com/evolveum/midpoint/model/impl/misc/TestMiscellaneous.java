/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.misc;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.prism.polystring.PolyString.getOrig;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateItemDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

/**
 * A place for tests that fit nowhere else.
 */
@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMiscellaneous extends AbstractInternalModelIntegrationTest {

    protected static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "misc");

    private static final TestResource<ObjectTemplateType> TEMPLATE_A1 =
            new TestResource<>(TEST_DIR, "template-a1.xml", "c2f74e53-2623-4832-8285-f30e225cab98");
    private static final TestResource<ObjectTemplateType> TEMPLATE_A2 =
            new TestResource<>(TEST_DIR, "template-a2.xml", "bd277f88-5765-4a9b-9424-32e35757e409");
    private static final TestResource<ObjectTemplateType> TEMPLATE_A3 =
            new TestResource<>(TEST_DIR, "template-a3.xml", "244cab1c-8424-4231-bf24-adbf9ea60765");

    private static final TestResource<ObjectTemplateType> TEMPLATE_B1 =
            new TestResource<>(TEST_DIR, "template-b1.xml", "adf4f05f-7dd6-440e-bfe9-880280f73283");
    private static final TestResource<ObjectTemplateType> TEMPLATE_B2A =
            new TestResource<>(TEST_DIR, "template-b2a.xml", "0ec39991-6ba7-4025-8470-8f8d09687f8c");
    private static final TestResource<ObjectTemplateType> TEMPLATE_B2B =
            new TestResource<>(TEST_DIR, "template-b2b.xml", "c0726932-4cd1-42b8-ace3-0fb08c6ac83d");

    @Test
    public void test100CyclicTemplateReferences() throws CommonException, EncryptionException, IOException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("three cyclic templates");
        repoAdd(TEMPLATE_A1, result);
        repoAdd(TEMPLATE_A2, result);
        repoAdd(TEMPLATE_A3, result);

        when("one of them is resolved");
        try {
            archetypeManager.getExpandedObjectTemplate(TEMPLATE_A1.oid, result);
            fail("unexpected success");
        } catch (ConfigurationException e) {
            then("exception is raised");
            displayExpectedException(e);
            assertThat(e).hasMessage("A cycle in object template references: c2f74e53-2623-4832-8285-f30e225cab98 -> "
                    + "bd277f88-5765-4a9b-9424-32e35757e409 -> 244cab1c-8424-4231-bf24-adbf9ea60765");
        }
    }

    @Test
    public void test110RegularInclusionResolution() throws CommonException, EncryptionException, IOException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("a template with two others");
        repoAdd(TEMPLATE_B1, result);
        repoAdd(TEMPLATE_B2A, result);
        repoAdd(TEMPLATE_B2B, result);

        when("the main one is resolved");
        ObjectTemplateType expanded = archetypeManager.getExpandedObjectTemplate(TEMPLATE_B1.oid, result);

        then("the result has correct name");
        displayDumpable("expanded version", expanded);
        assertThat(getOrig(expanded.getName())).as("name").isEqualTo("b1");

        and("there are three merged item definitions - displayName, name, extension/resourceName");
        // Note that this is not officially supported - for now. But it works.
        List<ObjectTemplateItemDefinitionType> items = expanded.getItem();
        assertThat(items).as("item definitions").hasSize(3);

        and("displayName has 3 mappings");
        List<ObjectTemplateItemDefinitionType> displayNameDefs =
                items.stream()
                        .filter(i -> i.getRef().getItemPath().equivalent(RoleType.F_DISPLAY_NAME))
                        .collect(Collectors.toList());
        assertThat(displayNameDefs).as("displayName definitions").hasSize(1);
        ObjectTemplateItemDefinitionType displayNameDef = displayNameDefs.get(0);
        // One from each template
        assertThat(displayNameDef.getMapping()).as("mappings for displayName").hasSize(3);

        and("name has no mappings");
        List<ObjectTemplateItemDefinitionType> nameDefs =
                items.stream()
                        .filter(i -> i.getRef().getItemPath().equivalent(RoleType.F_NAME))
                        .collect(Collectors.toList());
        assertThat(nameDefs).as("name definitions").hasSize(1);
        ObjectTemplateItemDefinitionType nameDef = nameDefs.get(0);
        assertThat(nameDef.getMapping()).as("mappings for name").hasSize(0);

        and("extension/resourceName has 3 mappings");
        ItemPath resourceNamePath = ItemPath.create(ObjectType.F_EXTENSION, EXT_RESOURCE_NAME);
        List<ObjectTemplateItemDefinitionType> resourceNameDefs =
                items.stream()
                        .filter(i -> i.getRef().getItemPath().equivalent(resourceNamePath))
                        .collect(Collectors.toList());
        assertThat(resourceNameDefs).as("ext/resourceName definitions").hasSize(1);
        ObjectTemplateItemDefinitionType resourceNameDef = resourceNameDefs.get(0);
        // One from each template
        assertThat(resourceNameDef.getMapping()).as("mappings for ext/resourceName").hasSize(3);

        and("all segments are qualified");
        List<?> refValueSegments = resourceNameDef.getRef().getItemPath().getSegments();
        assertThat(refValueSegments).as("ref path segments").hasSize(2);
        String firstSegmentNamespace = ItemPath.toName(refValueSegments.get(0)).getNamespaceURI();
        String secondSegmentNamespace = ItemPath.toName(refValueSegments.get(1)).getNamespaceURI();
        assertThat(firstSegmentNamespace).as("first segment namespace").isEqualTo(SchemaConstants.NS_C);
        assertThat(secondSegmentNamespace).as("second segment namespace").isEqualTo(NS_PIRACY);
    }
}
