/*
 * Copyright (c) 2015-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.dummy;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.schema.processor.BareResourceSchema;
import com.evolveum.midpoint.test.DummyDefaultScenario;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * The same as {@link TestDummy} but with the dummy resource providing the associations natively.
 *
 * Currently does not pass, because there is no support for updating native associations yet.
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestDummyNativeAssociations extends TestDummy {

    public static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy-native-associations.xml");

    @Override
    protected File getResourceDummyFile() {
        return RESOURCE_DUMMY_FILE;
    }

    public TestDummyNativeAssociations() {
        nativeAssociations = true;
    }

    @Override
    protected void extraDummyResourceInit() throws Exception {
        super.extraDummyResourceInit();
        DummyDefaultScenario.on(dummyResourceCtl)
                .initialize();
    }

    @Override
    protected void assertBareSchemaSanity(BareResourceSchema resourceSchema, ResourceType resourceType) throws Exception {
        // schema is extended (account has +2 associations), displayOrders are changed
        dummyResourceCtl.assertDummyResourceSchemaSanityExtended(
                resourceSchema, resourceType, false, 21);
    }
}
