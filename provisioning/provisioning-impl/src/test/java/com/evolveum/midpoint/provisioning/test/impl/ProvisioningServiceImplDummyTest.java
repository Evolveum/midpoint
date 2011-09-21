/**
 * 
 */
package com.evolveum.midpoint.provisioning.test.impl;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static com.evolveum.midpoint.test.IntegrationTestTools.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.xml.bind.JAXBException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.DummyResource;

import com.evolveum.midpoint.common.DebugUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.util.DerbyController;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;

/**
 * 
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = { "classpath:application-context-provisioning.xml",
		"classpath:application-context-provisioning-test.xml",
		"classpath:application-context-task.xml",
		"classpath:application-context-repository.xml",
		"classpath:application-context-configuration-test.xml" })
public class ProvisioningServiceImplDummyTest extends AbstractIntegrationTest {
	
	private static final String FILENAME_RESOURCE_DUMMY = "src/test/resources/impl/resource-dummy.xml";
	private static final String RESOURCE_DUMMY_OID = "ef2bc95b-76e0-59e2-86d6-9999dddddddd";
	private static final String FILENAME_ACCOUNT = "src/test/resources/impl/account-dummy.xml";
	private static final String ACCOUNT_NEW_OID = "c0c010c0-d34d-b44f-f11d-333222123456";
	private static final String DUMMY_CONNECTOR_TYPE = "com.evolveum.icf.dummy.DummyConnector";
	
	private static final Trace LOGGER = TraceManager.getTrace(ProvisioningServiceImplDummyTest.class);

	private static DummyResource dummyResource;
	
	@Autowired
	private ProvisioningService provisioningService;
	
	
	/**
	 * @throws JAXBException
	 */
	public ProvisioningServiceImplDummyTest() throws JAXBException {
		super();
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.test.AbstractIntegrationTest#initSystem()
	 */
	
	@Override
	public void initSystem(OperationResult initResult) throws Exception {
		provisioningService.postInit(initResult);
		addResourceFromFile(FILENAME_RESOURCE_DUMMY, DUMMY_CONNECTOR_TYPE, initResult);
	}
	
	@BeforeClass
	public static void startDb() throws Exception {
		dummyResource = DummyResource.getInstance();
	}

	
	@Test
	public void test000Integrity() throws ObjectNotFoundException, SchemaException {
		displayTestTile("test000Integrity");
		
		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()+".test000Integrity");
		
		ResourceType resource = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);
		String connectorOid = resource.getConnectorRef().getOid();
		ConnectorType connector = repositoryService.getObject(ConnectorType.class, connectorOid, null, result);
		assertNotNull(connector);
		display("Dummy Connector",connector);
	}
	
	@Test
	public void test001Connection() throws ObjectNotFoundException, SchemaException {
		displayTestTile("test001Connection");
		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()+".test001Connection");
		
		OperationResult testResult = provisioningService.testResource(RESOURCE_DUMMY_OID);
		
		display("Test result",testResult);
		assertSuccess("Test resource failed (result)", testResult);
		
		ResourceType resource = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);
		display("Resource after test",resource);
	}
	
	@Test
	public void test002AddAccount() throws Exception {
		displayTestTile("test002AddAccount");
		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()
				+ ".test002AddAccount");

		AccountShadowType account = unmarshallJaxbFromFile(FILENAME_ACCOUNT, AccountShadowType.class);

		System.out.println(DebugUtil.prettyPrint(account));
		System.out.println(DOMUtil.serializeDOMToString(JAXBUtil.jaxbToDom(account,
				SchemaConstants.I_ACCOUNT, DOMUtil.getDocument())));

		// WHEN
		String addedObjectOid = provisioningService.addObject(account, null, result);
		
		// THEN
		display("add object result",result);
		assertSuccess("addObject has failed (result)",result);
		assertEquals(ACCOUNT_NEW_OID, addedObjectOid);

		AccountShadowType accountType =  repositoryService.getObject(AccountShadowType.class, ACCOUNT_NEW_OID,
				new PropertyReferenceListType(), result);
		assertEquals("will", accountType.getName());

		AccountShadowType provisioningAccountType = provisioningService.getObject(AccountShadowType.class, ACCOUNT_NEW_OID,
				new PropertyReferenceListType(), result);
		assertEquals("will", provisioningAccountType.getName());
		
	}
}
