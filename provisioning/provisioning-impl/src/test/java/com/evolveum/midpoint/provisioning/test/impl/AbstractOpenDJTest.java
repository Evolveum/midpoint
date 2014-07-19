/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.provisioning.test.impl;

import java.io.File;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.common.InternalsConfig;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.ProvisioningTestUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.impl.ConnectorManager;
import com.evolveum.midpoint.provisioning.test.mock.SynchornizationServiceMock;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
public abstract class AbstractOpenDJTest extends AbstractIntegrationTest {
	
	protected static final String TEST_DIR_NAME = "src/test/resources/impl/opendj";
	protected static final File TEST_DIR = new File(TEST_DIR_NAME);
	
	protected static final String RESOURCE_OPENDJ_FILENAME = ProvisioningTestUtil.COMMON_TEST_DIR_FILENAME + "resource-opendj.xml";
	protected static final String RESOURCE_OPENDJ_INITIALIZED_FILENAME = ProvisioningTestUtil.COMMON_TEST_DIR_FILENAME + "resource-opendj-initialized.xml";
	protected static final String RESOURCE_OPENDJ_OID = "ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";
	
	protected static final String ACCOUNT1_FILENAME = TEST_DIR_NAME + "/account1.xml";
	protected static final String ACCOUNT1_REPO_FILENAME = TEST_DIR_NAME + "/account1-repo.xml";
	protected static final String ACCOUNT1_OID = "dbb0c37d-9ee6-44a4-8d39-016dbce1cccc";
	
	protected static final String ACCOUNT_NEW_FILENAME = TEST_DIR_NAME + "/account-new.xml";
	protected static final String ACCOUNT_NEW_OID = "c0c010c0-d34d-b44f-f11d-333222123456";
	protected static final String ACCOUNT_NEW_DN = "uid=will,ou=People,dc=example,dc=com";
	
	protected static final String ACCOUNT_BAD_FILENAME = TEST_DIR_NAME + "/account-bad.xml";
	protected static final String ACCOUNT_BAD_OID = "dbb0c37d-9ee6-44a4-8d39-016dbce1ffff";
	
	protected static final String ACCOUNT_MODIFY_FILENAME = TEST_DIR_NAME + "/account-modify.xml";
	protected static final String ACCOUNT_MODIFY_REPO_FILENAME = TEST_DIR_NAME + "/account-modify-repo.xml";
	protected static final String ACCOUNT_MODIFY_OID = "c0c010c0-d34d-b44f-f11d-333222444555";
	
	protected static final String ACCOUNT_MODIFY_PASSWORD_FILENAME = TEST_DIR_NAME + "/account-modify-password.xml";
	protected static final String ACCOUNT_MODIFY_PASSWORD_OID = "c0c010c0-d34d-b44f-f11d-333222444566";
	
	protected static final String ACCOUNT_DELETE_FILENAME = TEST_DIR_NAME + "/account-delete.xml";
	protected static final String ACCOUNT_DELETE_REPO_FILENAME = TEST_DIR_NAME + "/account-delete-repo.xml";
	protected static final String ACCOUNT_DELETE_OID = "c0c010c0-d34d-b44f-f11d-333222654321";
	
	protected static final String ACCOUNT_SEARCH_ITERATIVE_FILENAME = TEST_DIR_NAME + "/account-search-iterative.xml";
	protected static final String ACCOUNT_SEARCH_ITERATIVE_OID = "c0c010c0-d34d-b44f-f11d-333222666666";
	
	protected static final String ACCOUNT_SEARCH_FILENAME = TEST_DIR_NAME + "/account-search.xml";
	protected static final String ACCOUNT_SEARCH_OID = "c0c010c0-d34d-b44f-f11d-333222777777";
	
	protected static final String ACCOUNT_NEW_WITH_PASSWORD_FILENAME = TEST_DIR_NAME + "/account-new-with-password.xml";;
	protected static final String ACCOUNT_NEW_WITH_PASSWORD_OID = "c0c010c0-d34d-b44f-f11d-333222124422";
	
	protected static final File ACCOUNT_NEW_DISABLED_FILE = new File (TEST_DIR, "account-new-disabled.xml");
	protected static final String ACCOUNT_NEW_DISABLED_OID = "c0c010c0-d34d-b44f-f11d-d3d2d2d2d4d2";

	protected static final File ACCOUNT_NEW_ENABLED_FILE = new File (TEST_DIR, "account-new-enabled.xml");
	protected static final String ACCOUNT_NEW_ENABLED_OID = "c0c010c0-d34d-b44f-f11d-d3d2d2d2d4d3";

	protected static final String ACCOUNT_DISABLE_SIMULATED_FILENAME = TEST_DIR_NAME + "/account-disable-simulated-opendj.xml";
	protected static final String ACCOUNT_DISABLE_SIMULATED_OID = "dbb0c37d-9ee6-44a4-8d39-016dbce1aaaa";
	
	protected static final String REQUEST_DISABLE_ACCOUNT_SIMULATED_FILENAME = TEST_DIR_NAME + "/disable-account-simulated.xml";
	
	protected static final String ACCOUNT_NO_SN_FILENAME = TEST_DIR_NAME + "/account-opendj-no-sn.xml";
	protected static final String ACCOUNT_NO_SN_OID = "c0c010c0-d34d-beef-f33d-113222123444";
	
	protected static final File ACCOUNT_MORGAN_FILE = new File(TEST_DIR, "account-morgan.xml");
	protected static final String ACCOUNT_MORGAN_OID = "8dfcf05e-c571-11e3-abbd-001e8c717e5b";
	protected static final String ACCOUNT_MORGAN_DN = "uid=morgan,ou=People,dc=example,dc=com";
	
	protected static final File GROUP_SWASHBUCKLERS_FILE = new File(TEST_DIR, "group-swashbucklers.xml");
	protected static final String GROUP_SWASHBUCKLERS_OID = "3d96846e-c570-11e3-a80f-001e8c717e5b";
	protected static final String GROUP_SWASHBUCKLERS_DN = "cn=swashbucklers,ou=Groups,dc=example,dc=com";
	
	protected static final String NON_EXISTENT_OID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
	
	protected static final String RESOURCE_NS = "http://midpoint.evolveum.com/xml/ns/public/resource/instance/ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";
	protected static final QName RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS = new QName(RESOURCE_NS,"AccountObjectClass");
	protected static final String LDAP_CONNECTOR_TYPE = "org.identityconnectors.ldap.LdapConnector";
		
	private static final Trace LOGGER = TraceManager.getTrace(AbstractOpenDJTest.class);
	
	protected PrismObject<ResourceType> resource;
	protected ResourceType resourceType;
	protected PrismObject<ConnectorType> connector;
	
	@Autowired(required = true)
	protected ProvisioningService provisioningService;

	// Used to make sure that the connector is cached
	@Autowired(required = true)
	protected ConnectorManager connectorManager;

	@Autowired(required = true)
	protected SynchornizationServiceMock syncServiceMock;

	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		// We need to switch off the encryption checks. Some values cannot be encrypted as we do
		// not have a definition here
		InternalsConfig.encryptionChecks = false;
		provisioningService.postInit(initResult);
		PrismObject<ResourceType> resource = addResourceFromFile(RESOURCE_OPENDJ_FILENAME, LDAP_CONNECTOR_TYPE, initResult);
//		addObjectFromFile(FILENAME_ACCOUNT1);
		repoAddObjectFromFile(ACCOUNT_BAD_FILENAME, ShadowType.class, initResult);
	}
	
	protected <T> void assertAttribute(ShadowType shadow, String attrName, T... expectedValues) {
		ProvisioningTestUtil.assertAttribute(resource, shadow, attrName, expectedValues);
	}

	protected <T> void assertAttribute(ShadowType shadow, QName attrName, T... expectedValues) {
		ProvisioningTestUtil.assertAttribute(resource, shadow, attrName, expectedValues);
	}
}
