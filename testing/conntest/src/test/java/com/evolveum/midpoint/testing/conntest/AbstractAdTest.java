/**
 * Copyright (c) 2015 Evolveum
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
package com.evolveum.midpoint.testing.conntest;

import static org.testng.AssertJUnit.assertNotNull;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;

import java.io.File;
import java.util.Collection;

import javax.xml.namespace.QName;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * @author semancik
 *
 */
@Listeners({com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class})
public abstract class AbstractAdTest extends AbstractLdapTest {

	protected static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "ad");

	protected ConnectorHostType connectorHostType;

	protected static final File ROLE_PIRATES_FILE = new File(TEST_DIR, "role-pirate.xml");
	protected static final String ROLE_PIRATES_OID = "5dd034e8-41d2-11e5-a123-001e8c717e5b";

	protected static final File ROLE_META_ORG_FILE = new File(TEST_DIR, "role-meta-org.xml");
	protected static final String ROLE_META_ORG_OID = "f2ad0ace-45d7-11e5-af54-001e8c717e5b";

	public static final String ATTRIBUTE_LOCKOUT_LOCKED_NAME = "lockedByIntruder";
	public static final String ATTRIBUTE_LOCKOUT_RESET_TIME_NAME = "loginIntruderResetTime";
	public static final String ATTRIBUTE_GROUP_MEMBERSHIP_NAME = "groupMembership";
	public static final String ATTRIBUTE_EQUIVALENT_TO_ME_NAME = "equivalentToMe";
	public static final String ATTRIBUTE_SECURITY_EQUALS_NAME = "securityEquals";

	protected static final String ACCOUNT_JACK_UID = "jack";
	protected static final String ACCOUNT_JACK_PASSWORD = "qwe123";

	private static final String GROUP_PIRATES_NAME = "pirates";
	private static final String GROUP_MELEE_ISLAND_NAME = "Mêlée Island";

	protected static final int NUMBER_OF_ACCOUNTS = 4;
	protected static final int LOCKOUT_EXPIRATION_SECONDS = 65;
	private static final String ASSOCIATION_GROUP_NAME = "group";

	protected String jackAccountOid;
	protected String groupPiratesOid;
	protected long jackLockoutTimestamp;
	private String accountBarbossaOid;
	private String orgMeleeIslandOid;
	protected String groupMeleeOid;

	@Override
	public String getStartSystemCommand() {
		return null;
	}

	@Override
	public String getStopSystemCommand() {
		return null;
	}

	@Override
	protected File getBaseDir() {
		return TEST_DIR;
	}

	@Override
	protected String getResourceOid() {
		return "188ec322-4bd7-11e5-b919-001e8c717e5b";
	}

	@Override
	protected File getResourceFile() {
		return new File(getBaseDir(), "resource-medusa.xml");
	}

	protected String getConnectorHostOid() {
		return "08e687b6-4bd7-11e5-8484-001e8c717e5b";
	}

	protected abstract File getConnectorHostFile();

	@Override
	protected String getSyncTaskOid() {
		return null;
	}

	@Override
	protected boolean useSsl() {
		return true;
	}

	@Override
	protected String getLdapSuffix() {
		return "dc=win,dc=evolveum,dc=com";
	}

	@Override
	protected String getLdapBindDn() {
		return "CN=IDM Administrator,CN=Users,DC=win,DC=evolveum,DC=com";
	}

	@Override
	protected String getLdapBindPassword() {
		return "Secret123";
	}

	@Override
	protected int getSearchSizeLimit() {
		return -1;
	}

	@Override
	public String getPrimaryIdentifierAttributeName() {
		return "GUID";
	}

	@Override
	protected QName getAccountObjectClass() {
		return new QName(MidPointConstants.NS_RI,  "AccountObjectClass");
	}

	@Override
	protected String getLdapGroupObjectClass() {
		return "groupOfNames";
	}

	@Override
	protected String getLdapGroupMemberAttribute() {
		return "member";
	}

	private QName getAssociationGroupQName() {
		return new QName(MidPointConstants.NS_RI, ASSOCIATION_GROUP_NAME);
	}

	@Override
	protected boolean isImportResourceAtInit() {
		return false;
	}

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

//		binaryAttributeDetector.addBinaryAttribute("GUID");

		// Connector host
		connectorHostType = repoAddObjectFromFile(getConnectorHostFile(), ConnectorHostType.class, initResult).asObjectable();

		// Users
		repoAddObjectFromFile(USER_BARBOSSA_FILE, initResult);
		repoAddObjectFromFile(USER_GUYBRUSH_FILE, initResult);

		// Roles
//		repoAddObjectFromFile(ROLE_PIRATES_FILE, RoleType.class, initResult);
//		repoAddObjectFromFile(ROLE_META_ORG_FILE, RoleType.class, initResult);

	}

	@Test
    public void test000Sanity() throws Exception {
//		assertLdapPassword(ACCOUNT_JACK_UID, ACCOUNT_JACK_PASSWORD);
//		assertEDirGroupMember(ACCOUNT_JACK_UID, GROUP_PIRATES_NAME);
//		cleanupDelete(toDn(USER_BARBOSSA_USERNAME));
//		cleanupDelete(toDn(USER_CPTBARBOSSA_USERNAME));
//		cleanupDelete(toDn(USER_GUYBRUSH_USERNAME));
//		cleanupDelete(toGroupDn("Mêlée Island"));
	}

	@Test
    public void test001ConnectorHostDiscovery() throws Exception {
		final String TEST_NAME = "test001ConnectorHostDiscovery";
        TestUtil.displayTestTitle(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modelService.discoverConnectors(connectorHostType, task, result);

        // THEN
 		result.computeStatus();
 		TestUtil.assertSuccess(result);

 		SearchResultList<PrismObject<ConnectorType>> connectors = modelService.searchObjects(ConnectorType.class, null, null, task, result);

 		boolean found = false;
 		for (PrismObject<ConnectorType> connector: connectors) {
 			if (CONNECTOR_AD_TYPE.equals(connector.asObjectable().getConnectorType())) {
 				display("Found connector", connector);
 				found = true;
 			}
 		}
 		assertTrue("AD Connector not found", found);
	}

	@Test
    public void test002ImportResource() throws Exception {
		final String TEST_NAME = "test002ImportResource";
        TestUtil.displayTestTitle(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        resource = importAndGetObjectFromFile(ResourceType.class, getResourceFile(), getResourceOid(), task, result);

        // THEN
 		result.computeStatus();
 		TestUtil.assertSuccess(result);

 		resourceType = resource.asObjectable();
	}

	@Test
    public void test020Schema() throws Exception {
		final String TEST_NAME = "test020Schema";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        ResourceSchema resourceSchema = RefinedResourceSchema.getResourceSchema(resource, prismContext);
        display("Resource schema", resourceSchema);

        RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resource);
        display("Refined schema", refinedSchema);
        accountObjectClassDefinition = refinedSchema.findObjectClassDefinition(getAccountObjectClass());
        assertNotNull("No definition for object class "+getAccountObjectClass(), accountObjectClassDefinition);
        display("Account object class def", accountObjectClassDefinition);

        ResourceAttributeDefinition<String> cnDef = accountObjectClassDefinition.findAttributeDefinition("cn");
        PrismAsserts.assertDefinition(cnDef, new QName(MidPointConstants.NS_RI, "cn"), DOMUtil.XSD_STRING, 0, 1);
        assertTrue("cn read", cnDef.canRead());
    	assertFalse("cn modify", cnDef.canModify());
    	assertFalse("cn add", cnDef.canAdd());

        ResourceAttributeDefinition<String> userPrincipalNameDef = accountObjectClassDefinition.findAttributeDefinition("userPrincipalName");
        PrismAsserts.assertDefinition(userPrincipalNameDef, new QName(MidPointConstants.NS_RI, "userPrincipalName"), DOMUtil.XSD_STRING, 0, 1);
        assertTrue("o read", userPrincipalNameDef.canRead());
        assertTrue("o modify", userPrincipalNameDef.canModify());
        assertTrue("o add", userPrincipalNameDef.canAdd());

	}


	@Test
    public void test050Capabilities() throws Exception {
		final String TEST_NAME = "test050Capabilities";
        TestUtil.displayTestTitle(this, TEST_NAME);

        Collection<Object> nativeCapabilitiesCollection = ResourceTypeUtil.getNativeCapabilitiesCollection(resourceType);
        display("Native capabilities", nativeCapabilitiesCollection);

        assertTrue("No native activation capability", ResourceTypeUtil.hasResourceNativeActivationCapability(resourceType));
        assertTrue("No native activation status capability", ResourceTypeUtil.hasResourceNativeActivationStatusCapability(resourceType));
        assertTrue("No native lockout capability", ResourceTypeUtil.hasResourceNativeActivationLockoutCapability(resourceType));
        assertTrue("No native credentias capability", ResourceTypeUtil.isCredentialsCapabilityEnabled(resourceType));
	}



}
