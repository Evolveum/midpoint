/*
 * Copyright (c) 2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.intest;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.util.List;

import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachingMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
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
	protected void assertShadowRepo(PrismObject<ShadowType> shadow, String oid, String username, ResourceType resourceType,
            QName objectClass, MatchingRule<String> nameMatchingRule) throws SchemaException {
		super.assertShadowRepo(shadow, oid, username, resourceType, objectClass, nameMatchingRule);
		CachingMetadataType cachingMetadata = shadow.asObjectable().getCachingMetadata();
		assertNotNull("Missing caching metadata in repo shadow"+shadow, cachingMetadata);
	}

	@Override
	protected void assertRepoShadowAttributes(List<Item<?,?>> attributes, int expectedNumberOfIdentifiers) {
		// We can only assert that there are at least the identifiers. But we do not know how many attributes should be there
		assertTrue("Unexpected number of attributes in repo shadow, expected at least "+
		expectedNumberOfIdentifiers+", but was "+attributes.size(), attributes.size() >= expectedNumberOfIdentifiers);
	}
}
