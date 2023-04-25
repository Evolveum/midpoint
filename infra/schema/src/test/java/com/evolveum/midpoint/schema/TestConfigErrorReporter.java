/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.util.ItemRefinedDefinitionTypeUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

public class TestConfigErrorReporter extends AbstractSchemaTest {

    public static final File TEST_DIR = new File("src/test/resources/config-error-reporter");

    private static final File RESOURCE_1 = new File(TEST_DIR, "resource-1.xml");
    private static final File OBJECT_TEMPLATE_1 = new File(TEST_DIR, "object-template-1.xml");

    @Test
    public void testReportMissingItemRefInResource() throws Exception {
        PrismContext prismContext = PrismTestUtil.getPrismContext();
        ResourceType resource = (ResourceType) prismContext.parseObject(RESOURCE_1).asObjectable();

        when("ref-less object type attribute is accessed");
        try {
            ItemRefinedDefinitionTypeUtil.getRef(
                    resource.getSchemaHandling().getObjectType().get(0).getAttribute().get(0));
        } catch (ConfigurationException e) {
            displayExpectedException(e);
            assertThat(e)
                    .hasMessage("No 'ref' in attribute definition with ID 10 in the definition of type ACCOUNT/test in resource:null(dummy)");
        }

        when("ref-less object class attribute is accessed");
        try {
            ItemRefinedDefinitionTypeUtil.getRef(
                    resource.getSchemaHandling().getObjectClass().get(0).getAttribute().get(0));
        } catch (ConfigurationException e) {
            displayExpectedException(e);
            assertThat(e)
                    .hasMessage("No 'ref' in attribute definition with ID 123 in the refined definition of class AccountObjectClass in resource:null(dummy)");
        }
    }

    @Test
    public void testReportMissingItemRefInObjectTemplate() throws Exception {
        PrismContext prismContext = PrismTestUtil.getPrismContext();
        ObjectTemplateType template = (ObjectTemplateType) prismContext.parseObject(OBJECT_TEMPLATE_1).asObjectable();

        when("ref-less item definition is accessed");
        try {
            ItemRefinedDefinitionTypeUtil.getRef(
                    template.getItem().get(0));
        } catch (ConfigurationException e) {
            displayExpectedException(e);
            assertThat(e)
                    .hasMessage("No 'ref' in item definition with ID 20 in objectTemplate:null(dummy)");
        }
    }
}
