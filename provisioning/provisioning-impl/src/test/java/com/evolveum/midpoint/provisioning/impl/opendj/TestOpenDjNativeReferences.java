/*
 * Copyright (c) 2016-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.impl.opendj;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.schema.processor.ResourceObjectClassDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;

/**
 * Same as {@link TestOpenDj} but with native references provided by LDAP connector.
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestOpenDjNativeReferences extends TestOpenDj {

    private static final File RESOURCE_FILE = new File(TEST_DIR, "resource-opendj-native-references.xml");

    @Override
    protected File getResourceOpenDjFile() {
        return RESOURCE_FILE;
    }

    @Override
    protected boolean hasNativeReferences() {
        return true;
    }

    @Override
    protected void assertMemberOfAttributeBare(ResourceObjectClassDefinition accountClassDefBare) {
        // TODO implement
    }

    @Override
    protected void assertMemberOfAttributeRefined(ResourceObjectDefinition accountDef) {
        // TODO implement
    }
}
