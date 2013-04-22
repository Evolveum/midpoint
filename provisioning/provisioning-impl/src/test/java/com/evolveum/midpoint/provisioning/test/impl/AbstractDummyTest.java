/**
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */
package com.evolveum.midpoint.provisioning.test.impl;

import static org.testng.AssertJUnit.assertFalse;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.testng.AssertJUnit;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyAttributeDefinition;
import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.icf.dummy.resource.DummyObject;
import com.evolveum.icf.dummy.resource.DummyObjectClass;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.common.InternalsConfig;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.provisioning.ProvisioningTestUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.impl.ConnectorManager;
import com.evolveum.midpoint.provisioning.test.mock.SynchornizationServiceMock;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowEntitlementsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowKindType;

/**
 * @author semancik
 *
 */
public abstract class AbstractDummyTest extends AbstractIntegrationTest {
	
	protected static final String TEST_DIR = "src/test/resources/impl/dummy/";
	
	public static final String RESOURCE_DUMMY_FILENAME = ProvisioningTestUtil.COMMON_TEST_DIR_FILENAME + "resource-dummy.xml";
	public static final String RESOURCE_DUMMY_OID = "ef2bc95b-76e0-59e2-86d6-9999dddddddd";
	
	protected static final String RESOURCE_DUMMY_NONEXISTENT_OID = "ef2bc95b-000-000-000-009900dddddd";

	protected static final String ACCOUNT_WILL_FILENAME = TEST_DIR + "account-will.xml";
	protected static final String ACCOUNT_WILL_OID = "c0c010c0-d34d-b44f-f11d-33322212dddd";
	protected static final String ACCOUNT_WILL_USERNAME = "Will";

	protected static final String ACCOUNT_DAEMON_USERNAME = "daemon";
	protected static final String ACCOUNT_DAEMON_OID = "c0c010c0-dddd-dddd-dddd-dddddddae604";
	protected static final String ACCOUNT_DAEMON_FILENAME = TEST_DIR + "account-daemon.xml";

	protected static final String ACCOUNT_DAVIEJONES_USERNAME = "daviejones";

	protected static final String ACCOUNT_MORGAN_FILENAME = TEST_DIR + "account-morgan.xml";
	protected static final String ACCOUNT_MORGAN_OID = "c0c010c0-d34d-b44f-f11d-444400008888";
	protected static final String ACCOUNT_MORGAN_NAME = "morgan";
	
	protected static final String GROUP_PIRATES_FILENAME = TEST_DIR + "group-pirates.xml";
	protected static final String GROUP_PIRATES_OID = "c0c010c0-d34d-b44f-f11d-3332eeee0000";
	protected static final String GROUP_PIRATES_NAME = "pirates";

	protected static final String FILENAME_ACCOUNT_SCRIPT = TEST_DIR + "account-script.xml";
	protected static final String ACCOUNT_NEW_SCRIPT_OID = "c0c010c0-d34d-b44f-f11d-33322212abcd";
	protected static final String FILENAME_ENABLE_ACCOUNT = TEST_DIR + "modify-will-enable.xml";
	protected static final String FILENAME_DISABLE_ACCOUNT = TEST_DIR + "modify-will-disable.xml";
	protected static final String FILENAME_MODIFY_ACCOUNT = TEST_DIR + "modify-will-fullname.xml";
	protected static final String FILENAME_SCRIPT_ADD = TEST_DIR + "script-add.xml";
	
	protected static final String NOT_PRESENT_OID = "deaddead-dead-dead-dead-deaddeaddead";
	
	private static final Trace LOGGER = TraceManager.getTrace(AbstractDummyTest.class);
	
	protected PrismObject<ResourceType> resource;
	protected ResourceType resourceType;
	protected static DummyResource dummyResource;
	
	@Autowired(required = true)
	protected ProvisioningService provisioningService;

	// Used to make sure that the connector is cached
	@Autowired(required = true)
	protected ConnectorManager connectorManager;

	@Autowired(required = true)
	protected SynchornizationServiceMock syncServiceMock;
	
	@Autowired(required = true) 
	protected TaskManager taskManager;

	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		// We need to switch off the encryption checks. Some values cannot be encrypted as we do
		// not have a definition here
		InternalsConfig.encryptionChecks = false;
		provisioningService.postInit(initResult);
		resource = addResourceFromFile(getResourceDummyFilename(), IntegrationTestTools.DUMMY_CONNECTOR_TYPE, initResult);
		resourceType = resource.asObjectable();

		dummyResource = DummyResource.getInstance();
		dummyResource.reset();
		dummyResource.populateWithDefaultSchema();
		ProvisioningTestUtil.extendSchema(dummyResource);

		DummyAccount dummyAccountDaemon = new DummyAccount(ACCOUNT_DAEMON_USERNAME);
		dummyAccountDaemon.setEnabled(true);
		dummyAccountDaemon.addAttributeValues("fullname", "Evil Daemon");
		dummyResource.addAccount(dummyAccountDaemon);

		addObjectFromFile(ACCOUNT_DAEMON_FILENAME, ShadowType.class, initResult);
	}
	
	protected String getResourceDummyFilename() {
		return RESOURCE_DUMMY_FILENAME;
	}

	protected <T extends ShadowType> void checkConsistency(Collection<PrismObject<T>> shadows) throws SchemaException {
		for (PrismObject<T> shadow: shadows) {
			checkConsistency(shadow);
		}
	}
	
	protected void checkConsistency(PrismObject<? extends ShadowType> object) throws SchemaException {

		OperationResult result = new OperationResult(TestDummyNegative.class.getName()
				+ ".checkConsistency");
		
		ItemDefinition itemDef = ShadowUtil.getAttributesContainer(object).getDefinition().findAttributeDefinition(ConnectorFactoryIcfImpl.ICFS_UID);
		
		LOGGER.info("item definition: {}", itemDef.dump());
		//TODO: matching rule
		EqualsFilter equal = EqualsFilter.createEqual(new ItemPath(ShadowType.F_ATTRIBUTES), itemDef, null, getWillRepoIcfUid());
		ObjectQuery query = ObjectQuery.createObjectQuery(equal);
		
		System.out.println("Looking for shadows of \"" + getWillRepoIcfUid() + "\" with filter "
				+ query.dump());
		display("Looking for shadows of \"" + getWillRepoIcfUid() + "\" with filter "
				+ query.dump());

		
		List<PrismObject<ShadowType>> objects = repositoryService.searchObjects(ShadowType.class, query,
				result);

		
		assertEquals("Wrong number of shadows for ICF UID \"" + getWillRepoIcfUid() + "\"", 1, objects.size());

	}
	
	protected <T> void assertAttribute(ShadowType shadow, String attrName, T... expectedValues) {
		ProvisioningTestUtil.assertAttribute(resource, shadow, attrName, expectedValues);
	}
	
	protected <T> void assertAttribute(ShadowType shadow, QName attrName, T... expectedValues) {
		ProvisioningTestUtil.assertAttribute(resource, shadow, attrName, expectedValues);
	}
	
	protected void assertSchemaSanity(ResourceSchema resourceSchema, ResourceType resourceType) {
		ProvisioningTestUtil.assertDummyResourceSchemaSanityExtended(resourceSchema, resourceType);
	}
	
	protected <T> void assertDummyAccountAttributeValues(String accountId, String attributeName, T... expectedValues) {
		DummyAccount dummyAccount = dummyResource.getAccountByUsername(accountId);
		assertNotNull("No account '"+accountId+"'", dummyAccount);
		assertDummyAttributeValues(dummyAccount, attributeName, expectedValues);
	}
	
	protected <T> void assertDummyAttributeValues(DummyObject object, String attributeName, T... expectedValues) {
		Set<T> attributeValues = (Set<T>) object.getAttributeValues(attributeName, expectedValues[0].getClass());
		assertNotNull("No attribute "+attributeName+" in "+object.getShortTypeName()+" "+object, attributeValues);
		TestUtil.assertSetEquals("Wroung values of attribute "+attributeName+" in "+object.getShortTypeName()+" "+object, attributeValues, expectedValues);
	}
	
	protected String getWillRepoIcfUid() {
		return ACCOUNT_WILL_USERNAME;
	}
	
	protected void assertMember(DummyGroup group, String accountId) {
		Collection<String> members = group.getMembers();
		assertTrue("Account "+accountId+" is not member of group "+group.getName()+", members: "+members, members.contains(accountId));
	}

	protected void assertNoMember(DummyGroup group, String accountId) {
		Collection<String> members = group.getMembers();
		assertFalse("Account "+accountId+" IS member of group "+group.getName()+" while not expecting it, members: "+members, members.contains(accountId));
	}
	
	protected void assertEntitlement(PrismObject<ShadowType> account, String entitlementOid) {
		ShadowType accountType = account.asObjectable();
		ShadowEntitlementsType entitlements = accountType.getEntitlements();
		assertNotNull("No entitlements in "+account, entitlements);
		List<ShadowAssociationType> associations = entitlements.getAssociation();
		assertNotNull("Null entitlement associations in "+account, associations);
		assertFalse("Empty entitlement associations in "+account, associations.isEmpty());
		for (ShadowAssociationType association: associations) {
			if (entitlementOid.equals(association.getOid())) {
				return;
			}
		}
		AssertJUnit.fail("No association for entitlement "+entitlementOid+" in "+account);
	}


}
