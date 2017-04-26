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
package com.evolveum.midpoint.provisioning.impl.opendj;

import java.io.File;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.match.DistinguishedNameMatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.impl.ProvisioningTestUtil;
import com.evolveum.midpoint.provisioning.impl.ResourceManager;
import com.evolveum.midpoint.provisioning.impl.mock.SynchornizationServiceMock;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
public abstract class AbstractOpenDjTest extends AbstractIntegrationTest {
	
	protected static final String TEST_DIR_NAME = "src/test/resources/impl/opendj";
	protected static final File TEST_DIR = new File(TEST_DIR_NAME);
	
	protected static final File RESOURCE_OPENDJ_FILE = new File(ProvisioningTestUtil.COMMON_TEST_DIR_FILE, "resource-opendj.xml");
	protected static final File RESOURCE_OPENDJ_INITIALIZED_FILE = new File(ProvisioningTestUtil.COMMON_TEST_DIR_FILE, "resource-opendj-initialized.xml");
	protected static final String RESOURCE_OPENDJ_OID = "ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";
	protected static final String RESOURCE_OPENDJ_NS = "http://midpoint.evolveum.com/xml/ns/public/resource/instance/ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";
	
	protected static final File RESOURCE_OPENDJ_BAD_CREDENTIALS_FILE = new File(TEST_DIR, "resource-opendj-bad-credentials.xml");
	protected static final String RESOURCE_OPENDJ_BAD_CREDENTIALS_OID = "8bc3ff5a-ef5d-11e4-8bba-001e8c717e5b";
	
	protected static final File RESOURCE_OPENDJ_BAD_BIND_DN_FILE = new File(TEST_DIR, "resource-opendj-bad-bind-dn.xml");
	protected static final String RESOURCE_OPENDJ_BAD_BIND_DN_OID = "d180258a-ef5f-11e4-8737-001e8c717e5b";
	
	protected static final File ACCOUNT1_FILE = new File (TEST_DIR_NAME, "account1.xml");
	protected static final File ACCOUNT1_REPO_FILE = new File(TEST_DIR_NAME, "account1-repo.xml");
	protected static final String ACCOUNT1_OID = "dbb0c37d-9ee6-44a4-8d39-016dbce1cccc";
	
	protected static final File ACCOUNT_WILL_FILE = new File(TEST_DIR, "account-will.xml");
	protected static final String ACCOUNT_WILL_OID = "c0c010c0-d34d-b44f-f11d-333222123456";
	protected static final String ACCOUNT_WILL_DN = "uid=will,ou=People,dc=example,dc=com";
	
	protected static final File ACCOUNT_BAD_FILE = new File(TEST_DIR, "account-bad.xml");
	protected static final String ACCOUNT_BAD_OID = "dbb0c37d-9ee6-44a4-8d39-016dbce1ffff";
	
	protected static final File ACCOUNT_JACK_FILE = new File(TEST_DIR, "account-jack.xml");
	protected static final File ACCOUNT_JACK_REPO_FILE = new File(TEST_DIR, "account-jack-repo.xml");
	protected static final String ACCOUNT_JACK_OID = "c0c010c0-d34d-b44f-f11d-333222444555";
	protected static final String ACCOUNT_JACK_NAME = "jack";
	protected static final File ACCOUNT_JACK_CHANGE_FILE = new File(TEST_DIR, "account-jack-change.xml");
	
	protected static final String ACCOUNT_BARBOSSA_DN = "uid=hbarbossa,ou=People,dc=example,dc=com";
	
	protected static final File ACCOUNT_MODIFY_PASSWORD_FILE = new File(TEST_DIR_NAME, "account-modify-password.xml");
	protected static final String ACCOUNT_MODIFY_PASSWORD_OID = "c0c010c0-d34d-b44f-f11d-333222444566";
	
	protected static final File ACCOUNT_SPARROW_FILE = new File(TEST_DIR_NAME, "account-sparrow.xml");
	protected static final File ACCOUNT_SPARROW_REPO_FILE = new File(TEST_DIR_NAME, "account-sparrow-repo.xml");
	protected static final String ACCOUNT_SPARROW_OID = "c0c010c0-d34d-b44f-f11d-333222654321";
	
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
	
	protected static final File ACCOUNT_POSIX_MCMUTTON_FILE = new File (TEST_DIR, "account-posix-mcmutton.xml");
	protected static final String ACCOUNT_POSIX_MCMUTTON_OID = "3a1902a4-14d8-11e5-b0b5-001e8c717e5b";
	protected static final String ACCOUNT_POSIX_MCMUTTON_DN = "uid=mcmutton,ou=People,dc=example,dc=com";
	protected static final File ACCOUNT_POSIX_MCMUTTON_CHANGE_FILE = new File (TEST_DIR, "account-posix-mcmutton-change.xml");

	protected static final File ACCOUNT_POSIX_VANHELGEN_LDIF_FILE = new File(TEST_DIR, "vanhelgen.ldif");
	
	protected static final String REQUEST_DISABLE_ACCOUNT_SIMULATED_FILENAME = TEST_DIR_NAME + "/disable-account-simulated.xml";
	
	protected static final String ACCOUNT_NO_SN_FILENAME = TEST_DIR_NAME + "/account-opendj-no-sn.xml";
	protected static final String ACCOUNT_NO_SN_OID = "c0c010c0-d34d-beef-f33d-113222123444";
	
	protected static final File ACCOUNT_MORGAN_FILE = new File(TEST_DIR, "account-morgan.xml");
	protected static final String ACCOUNT_MORGAN_OID = "8dfcf05e-c571-11e3-abbd-001e8c717e5b";
	protected static final String ACCOUNT_MORGAN_DN = "uid=morgan,ou=People,dc=example,dc=com";
	
	protected static final File GROUP_SWASHBUCKLERS_FILE = new File(TEST_DIR, "group-swashbucklers.xml");
	protected static final String GROUP_SWASHBUCKLERS_OID = "3d96846e-c570-11e3-a80f-001e8c717e5b";
	protected static final String GROUP_SWASHBUCKLERS_DN = "cn=swashbucklers,ou=groups,dc=example,dc=com";

	protected static final File GROUP_SPECIALISTS_FILE = new File(TEST_DIR, "group-specialists.xml");
	protected static final String GROUP_SPECIALISTS_OID = "3da6ddca-cc0b-11e5-9b3f-2b7f453dbfb3";
	protected static final String GROUP_SPECIALISTS_DN = "cn=specialists,ou=specialgroups,dc=example,dc=com";

	protected static final File GROUP_CORSAIRS_FILE = new File(TEST_DIR, "group-corsairs.xml");
	protected static final String GROUP_CORSAIRS_OID = "70a1f3ee-4b5b-11e5-95d0-001e8c717e5b";
	protected static final String GROUP_CORSAIRS_DN = "cn=corsairs,ou=groups,dc=example,dc=com";
	
	protected static final String NON_EXISTENT_OID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
	
	public static final String RESOURCE_NS = "http://midpoint.evolveum.com/xml/ns/public/resource/instance/ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";
	public static final QName RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS = new QName(RESOURCE_NS, "inetOrgPerson");
	public static final QName RESOURCE_OPENDJ_GROUP_OBJECTCLASS = new QName(RESOURCE_NS, "groupOfUniqueNames");
	public static final QName RESOURCE_OPENDJ_POSIX_ACCOUNT_OBJECTCLASS = new QName(RESOURCE_NS, "posixAccount");
	
	protected static final File QUERY_COMPLEX_FILTER_FILE = new File(TEST_DIR, "query-complex-filter.xml");
	protected static final File QUERY_ALL_ACCOUNTS_FILE = new File(TEST_DIR, "query-filter-all-accounts.xml");
	protected static final File QUERY_VANHELGEN_FILE = new File(TEST_DIR, "query-vanhelgen.xml");
	
	protected static final String OBJECT_CLASS_INETORGPERSON_NAME = "inetOrgPerson";
	protected static final String GROUP_MEMBER_ATTR_NAME = "uniqueMember";
	
	protected static final QName ASSOCIATION_GROUP_NAME = new QName(RESOURCE_OPENDJ_NS, "group");
	
	private static final Trace LOGGER = TraceManager.getTrace(AbstractOpenDjTest.class);
	
	protected MatchingRule<String> dnMatchingRule;
	
	protected PrismObject<ResourceType> resource;
	protected ResourceType resourceType;
	protected PrismObject<ConnectorType> connector;
	
	@Autowired(required = true)
	protected ProvisioningService provisioningService;

	// Used to make sure that the connector is cached
	@Autowired(required = true)
	protected ResourceManager resourceManager;

	@Autowired(required = true)
	protected SynchornizationServiceMock syncServiceMock;
	
	@Autowired(required = true)
	protected MatchingRuleRegistry matchingRuleRegistry;

	protected File getResourceOpenDjFile() {
		return RESOURCE_OPENDJ_FILE;
	}
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		// We need to switch off the encryption checks. Some values cannot be encrypted as we do
		// not have a definition here
		InternalsConfig.encryptionChecks = false;
		provisioningService.postInit(initResult);
		resource = addResourceFromFile(getResourceOpenDjFile(), IntegrationTestTools.CONNECTOR_LDAP_TYPE, initResult);
		repoAddShadowFromFile(ACCOUNT_BAD_FILE, initResult);
		
		dnMatchingRule = matchingRuleRegistry.getMatchingRule(DistinguishedNameMatchingRule.NAME, DOMUtil.XSD_STRING);
	}
	
	protected <T> void assertAttribute(ShadowType shadow, String attrName, T... expectedValues) {
		assertAttribute(resource, shadow, attrName, expectedValues);
	}
	
	protected <T> void assertAttribute(PrismObject<ShadowType> shadow, String attrName, T... expectedValues) {
		assertAttribute(resource, shadow.asObjectable(), attrName, expectedValues);
	}

	protected <T> void assertAttribute(ShadowType shadow, QName attrName, T... expectedValues) {
		assertAttribute(resource, shadow, attrName, expectedValues);
	}
	
	protected QName getPrimaryIdentifierQName() {
		return new QName(ResourceTypeUtil.getResourceNamespace(resourceType), OpenDJController.RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME);
	}

	protected QName getSecondaryIdentifierQName() {
		return new QName(ResourceTypeUtil.getResourceNamespace(resourceType), OpenDJController.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER_LOCAL_NAME);
	}
}
