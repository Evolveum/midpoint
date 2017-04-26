/*
 * Copyright (c) 2010-2017 Evolveum
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

/**
 * 
 */
package com.evolveum.midpoint.provisioning.impl.csv;

import static com.evolveum.midpoint.test.IntegrationTestTools.getAttributeValue;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;

import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * The test of Provisioning service on the API level. The test is using CSV resource.
 * 
 * @author Radovan Semancik
 * 
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestCsvUsername extends AbstractCsvTest {

	private static final File RESOURCE_CSV_USERNAME_FILE = new File(TEST_DIR, "resource-csv-username.xml");
	private static final String RESOURCE_CSV_USERNAME_OID = "ef2bc95b-76e0-59e2-86d6-9999cccccccc";
	
	private static final File ACCOUNT_JACK_FILE = new File(TEST_DIR, "account-jack-username.xml");;
	private static final String ACCOUNT_JACK_OID = "2db718b6-243a-11e7-a9e5-bbb2545f80ed";
	private static final String ACCOUNT_JACK_USERNAME = "jack";
	
	private static final File CSV_SOURCE_FILE = new File(TEST_DIR, "midpoint-username.csv");

	protected static final String ATTR_USERNAME = "username";
	protected static final QName ATTR_USERNAME_QNAME = new QName(RESOURCE_NS, ATTR_USERNAME);
	
	private static final Trace LOGGER = TraceManager.getTrace(TestCsvUsername.class);

	@Override
	protected File getResourceFile() {
		return RESOURCE_CSV_USERNAME_FILE;
	}

	@Override
	protected String getResourceOid() {
		return RESOURCE_CSV_USERNAME_OID;
	}

	@Override
	protected File getSourceCsvFile() {
		return CSV_SOURCE_FILE;
	}
	
	@Override
	protected File getAccountJackFile() {
		return ACCOUNT_JACK_FILE;
	}

	@Override
	protected String getAccountJackOid() {
		return ACCOUNT_JACK_OID;
	}	

	@Override
	protected void assertAccountDefinition(ObjectClassComplexTypeDefinition accountDef) {

		assertEquals("Unexpected number of definitions", 4, accountDef.getDefinitions().size());
		
		ResourceAttributeDefinition<String> usernameDef = accountDef.findAttributeDefinition(ATTR_USERNAME);
		assertNotNull("No definition for username", usernameDef);
		assertEquals(1, usernameDef.getMaxOccurs());
		assertEquals(1, usernameDef.getMinOccurs());
		assertTrue("No username create", usernameDef.canAdd());
		assertTrue("No username update", usernameDef.canModify());
		assertTrue("No username read", usernameDef.canRead());
		
	}

	@Override
	protected void assertAccountJackAttributes(ShadowType shadowType) {
		assertEquals("Wrong username", ACCOUNT_JACK_USERNAME, getAttributeValue(shadowType, ATTR_USERNAME_QNAME));
        assertEquals("Wrong firstname", ACCOUNT_JACK_FIRSTNAME, getAttributeValue(shadowType, ATTR_FIRSTNAME_QNAME));
        assertEquals("Wrong lastname", ACCOUNT_JACK_LASTNAME, getAttributeValue(shadowType, ATTR_LASTNAME_QNAME));
	}

	@Override
	protected void assertAccountJackAttributesRepo(ShadowType repoShadowType) {
        assertEquals("Wrong identifier (repo)", ACCOUNT_JACK_USERNAME, getAttributeValue(repoShadowType, ATTR_USERNAME_QNAME));
	}
}
