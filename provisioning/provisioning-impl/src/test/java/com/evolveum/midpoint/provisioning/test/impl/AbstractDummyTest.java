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

import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyAttributeDefinition;
import com.evolveum.icf.dummy.resource.DummyObjectClass;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.provisioning.ProvisioningTestUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.impl.ConnectorTypeManager;
import com.evolveum.midpoint.provisioning.test.mock.SynchornizationServiceMock;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowType;
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
	protected static final String ACCOUNT_WILL_ICF_UID = "will";

	protected static final String ACCOUNT_DAEMON_USERNAME = "daemon";
	protected static final String ACCOUNT_DAEMON_OID = "c0c010c0-dddd-dddd-dddd-dddddddae604";
	protected static final String ACCOUNT_DAEMON_FILENAME = TEST_DIR + "account-daemon.xml";

	protected static final String ACCOUNT_DAVIEJONES_USERNAME = "daviejones";

	protected static final String ACCOUNT_MORGAN_FILENAME = TEST_DIR + "account-morgan.xml";
	protected static final String ACCOUNT_MORGAN_OID = "c0c010c0-d34d-b44f-f11d-444400008888";
	protected static final String ACCOUNT_MORGAN_NAME = "morgan";

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
	protected ConnectorTypeManager connectorTypeManager;

	@Autowired(required = true)
	protected SynchornizationServiceMock syncServiceMock;
	
	@Autowired(required = true) 
	protected TaskManager taskManager;

	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		provisioningService.postInit(initResult);
		resource = addResourceFromFile(RESOURCE_DUMMY_FILENAME, IntegrationTestTools.DUMMY_CONNECTOR_TYPE, initResult);
		resourceType = resource.asObjectable();

		dummyResource = DummyResource.getInstance();
		dummyResource.reset();
		dummyResource.populateWithDefaultSchema();
		ProvisioningTestUtil.extendSchema(dummyResource);

		DummyAccount dummyAccountDaemon = new DummyAccount(ACCOUNT_DAEMON_USERNAME);
		dummyAccountDaemon.setEnabled(true);
		dummyAccountDaemon.addAttributeValues("fullname", "Evil Daemon");
		dummyResource.addAccount(dummyAccountDaemon);

		addObjectFromFile(ACCOUNT_DAEMON_FILENAME, AccountShadowType.class, initResult);
	}

	protected <T extends ResourceObjectShadowType> void checkConsistency(Collection<PrismObject<T>> shadows) throws SchemaException {
		for (PrismObject<T> shadow: shadows) {
			checkConsistency(shadow);
		}
	}
	
	protected void checkConsistency(PrismObject<? extends ResourceObjectShadowType> object) throws SchemaException {

		OperationResult result = new OperationResult(TestDummyNegative.class.getName()
				+ ".checkConsistency");
		
		ItemDefinition itemDef = ResourceObjectShadowUtil.getAttributesContainer(object).getDefinition().findAttributeDefinition(ConnectorFactoryIcfImpl.ICFS_UID);
		
		LOGGER.info("item definition: {}", itemDef.dump());
		
		EqualsFilter equal = EqualsFilter.createEqual(new ItemPath(AccountShadowType.F_ATTRIBUTES), itemDef, ACCOUNT_WILL_ICF_UID);
		ObjectQuery query = ObjectQuery.createObjectQuery(equal);
		
		System.out.println("Looking for shadows of \"" + ACCOUNT_WILL_ICF_UID + "\" with filter "
				+ query.dump());
		display("Looking for shadows of \"" + ACCOUNT_WILL_ICF_UID + "\" with filter "
				+ query.dump());

		
		List<PrismObject<AccountShadowType>> objects = repositoryService.searchObjects(AccountShadowType.class, query,
				result);

		
		assertEquals("Wrong number of shadows for ICF UID \"" + ACCOUNT_WILL_ICF_UID + "\"", 1, objects.size());

	}
	
	protected <T> void assertAttribute(ResourceObjectShadowType shadow, String attrName, T... expectedValues) {
		QName attrQname = new QName(ResourceTypeUtil.getResourceNamespace(resource), attrName);
		List<T> actualValues = ResourceObjectShadowUtil.getAttributeValues(shadow, attrQname);
		PrismAsserts.assertSets("attribute "+attrQname+" in " + shadow, actualValues, expectedValues);
	}
	
	protected void assertSchemaSanity(ResourceSchema resourceSchema, ResourceType resourceType) {
		ProvisioningTestUtil.assertDummyResourceSchemaSanityExteded(resourceSchema, resourceType);
	}


}
