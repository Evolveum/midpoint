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
package com.evolveum.midpoint.provisioning.impl.dummy;

import static org.testng.AssertJUnit.assertTrue;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang.StringUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.icf.dummy.resource.DummyPrivilege;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.match.StringIgnoreCaseMatchingRule;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.provisioning.impl.ProvisioningTestUtil;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachingMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationProvisioningScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Almost the same as TestDummy but this is using a caching configuration.
 * 
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestDummyCaching extends TestDummy {
	
	public static final File TEST_DIR = new File("src/test/resources/impl/dummy-caching/");
	public static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy.xml");

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
	}

	@Override
	protected File getResourceDummyFilename() {
		return RESOURCE_DUMMY_FILE;
	}
		
	@Override
	protected void checkRepoAccountShadowWill(PrismObject<ShadowType> shadowRepo, XMLGregorianCalendar start, XMLGregorianCalendar end) {
		// Sometimes there are 6 and sometimes 7 attributes. Treasure is not returned by default. It is not normally in the cache.
		// So do not check for number of attributes here. Check for individual values.
		checkRepoAccountShadowWillBasic(shadowRepo, start, end, null);
		
		assertRepoShadowCachedAttributeValue(shadowRepo, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Flying Dutchman");
		// this is shadow, values are normalized
		// MID-3484
//		assertRepoShadowCachedAttributeValue(shadowRepo, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "sword", "love");
		assertRepoShadowCachedAttributeValue(shadowRepo, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, 42);
	}
	
	/**
	 * We do not know what the timestamp should be. But some timestamp should be there.
	 */
	@Override
	protected void assertRepoCachingMetadata(PrismObject<ShadowType> shadowFromRepo) {
		CachingMetadataType cachingMetadata = shadowFromRepo.asObjectable().getCachingMetadata();
		assertNotNull("No caching metadata in "+shadowFromRepo, cachingMetadata);
		
		assertNotNull("Missing retrieval timestamp in caching metadata in "+shadowFromRepo, 
				cachingMetadata.getRetrievalTimestamp());
	}
	
	@Override
	protected void assertRepoCachingMetadata(PrismObject<ShadowType> shadowFromRepo, XMLGregorianCalendar start, XMLGregorianCalendar end) {
		CachingMetadataType cachingMetadata = shadowFromRepo.asObjectable().getCachingMetadata();
		assertNotNull("No caching metadata in "+shadowFromRepo, cachingMetadata);
		
		TestUtil.assertBetween("Wrong retrieval timestamp in caching metadata in "+shadowFromRepo, 
				start, end, cachingMetadata.getRetrievalTimestamp());
	}
	
	@Override
	protected void checkRepoAccountShadow(PrismObject<ShadowType> repoShadow) {
		ProvisioningTestUtil.checkRepoShadow(repoShadow, ShadowKindType.ACCOUNT, null);
	}
	
	@Override
	protected void checkRepoEntitlementShadow(PrismObject<ShadowType> repoShadow) {
		ProvisioningTestUtil.checkRepoShadow(repoShadow, ShadowKindType.ENTITLEMENT, null);
	}
	
	@Override
	protected void assertRepoShadowAttributes(List<Item<?,?>> attributes, int expectedNumberOfIdentifiers) {
		// We can only assert that there are at least the identifiers. But we do not know how many attributes should be there
		assertTrue("Unexpected number of attributes in repo shadow, expected at least "+
		expectedNumberOfIdentifiers+", but was "+attributes.size(), attributes.size() >= expectedNumberOfIdentifiers);
	}
	
	@Override
	protected void assertSyncOldShadow(PrismObject<? extends ShadowType> oldShadow, String repoName) {
		assertSyncOldShadow(oldShadow, repoName, null);
	}
	
	@Override
	protected <T> void assertRepoShadowCachedAttributeValue(PrismObject<ShadowType> shadowRepo, String attrName, T... attrValues) {
		assertAttribute(shadowRepo.asObjectable(), attrName, attrValues);
	}

}
