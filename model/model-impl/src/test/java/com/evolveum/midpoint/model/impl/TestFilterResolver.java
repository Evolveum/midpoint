/*
 * Copyright (c) 2016-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.io.File;
import java.util.List;

import com.evolveum.midpoint.prism.path.ItemPath;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDependencyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaHandlingType;

@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestFilterResolver extends AbstractInternalModelIntegrationTest {

    protected static final File TEST_DIR = new File("src/test/resources/common");

    private static final String RESOURCE_DUMMY_DEPENDECY_FILTER_OID = "10000000-0000-0000-dep0-000000000004";
    private static final File RESOURCE_DUMMY_DEPENDENCY_FILTER_FILE = new File(TEST_DIR, "resource-dummy-dependency-filter.xml");

    @Test
    public void test001resolveDependencyFilter() throws Exception {
        // WHEN
        importObjectFromFile(RESOURCE_DUMMY_DEPENDENCY_FILTER_FILE);

        // THEN
        PrismObject<ResourceType> resourceDummyResolvedFilter = getObject(ResourceType.class,
                RESOURCE_DUMMY_DEPENDECY_FILTER_OID);
        assertNotNull(resourceDummyResolvedFilter, "Something unexpected happened. No resource found");

        PrismContainer<ResourceObjectTypeDefinitionType> objectType = resourceDummyResolvedFilter.findContainer(
                ItemPath.create(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE));
        assertEquals(objectType.size(), 1, "Unexpected object type definitions in resource.");

        PrismContainerValue<ResourceObjectTypeDefinitionType> objectTypeDef = objectType.getValues().iterator().next();
        List<ResourceObjectTypeDependencyType> resourceDependencies = objectTypeDef.asContainerable().getDependency();
        assertEquals(resourceDependencies.size(), 1, "Unexpected dependency definitions in resource.");

        ResourceObjectTypeDependencyType resourceDependency = resourceDependencies.iterator().next();
        ObjectReferenceType dependencyRef = resourceDependency.getResourceRef();
        assertNotNull(dependencyRef, "No dependency reference found in the resource, something is wrong");
        assertEquals(dependencyRef.getOid(), RESOURCE_DUMMY_OID, "Unexpected oid in resolved reference.");


    }
}
