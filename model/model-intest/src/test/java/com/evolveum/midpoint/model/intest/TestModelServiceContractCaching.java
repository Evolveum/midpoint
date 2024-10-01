/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.util.Collection;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.util.RawRepoShadow;
import com.evolveum.midpoint.util.exception.ConfigurationException;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachingMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * Uses dummy, blue, and green resources with the caching enabled and actively used (strategy is "use cached or fresh").
 *
 * @author semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestModelServiceContractCaching extends TestModelServiceContract {

    @Override
    protected File getResourceDummyFile() {
        return RESOURCE_DUMMY_CACHING_FILE;
    }

    @Override
    protected File getResourceDummyBlueFile() {
        return RESOURCE_DUMMY_BLUE_CACHING_FILE;
    }

    @Override
    protected File getResourceDummyGreenFile() {
        return RESOURCE_DUMMY_GREEN_CACHING_FILE;
    }

    @Override
    protected void assertShadowRepo(RawRepoShadow shadow, String oid, String username, ResourceType resourceType,
            QName objectClass, MatchingRule<String> nameMatchingRule) throws SchemaException, ConfigurationException {
        super.assertShadowRepo(shadow, oid, username, resourceType, objectClass, nameMatchingRule);
        CachingMetadataType cachingMetadata = shadow.getBean().getCachingMetadata();
        assertNotNull("Missing caching metadata in repo shadow"+shadow, cachingMetadata);
    }

    @Override
    protected void assertRepoShadowAttributes(Collection<Item<?,?>> attributes, int expectedNumberOfIdentifiers) {
        // We can only assert that there are at least the identifiers. But we do not know how many attributes should be there
        assertTrue("Unexpected number of attributes in repo shadow, expected at least "+
        expectedNumberOfIdentifiers+", but was "+attributes.size(), attributes.size() >= expectedNumberOfIdentifiers);
    }

    @Override
    boolean isCached() {
        return true;
    }
}
