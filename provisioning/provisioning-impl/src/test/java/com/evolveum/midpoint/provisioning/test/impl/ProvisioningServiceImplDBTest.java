/**
 * 
 */
package com.evolveum.midpoint.provisioning.test.impl;

import static com.evolveum.midpoint.test.IntegrationTestTools.assertSuccess;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayTestTile;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.xml.bind.JAXBException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.util.DerbyController;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
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
		"classpath:application-context-repo-cache.xml",
		"classpath:application-context-configuration-test.xml" })
@DirtiesContext
public class ProvisioningServiceImplDBTest extends AbstractIntegrationTest {
	
	private static final String FILENAME_RESOURCE_DERBY = "src/test/resources/object/resource-derby.xml";
	private static final String RESOURCE_DERBY_OID = "ef2bc95b-76e0-59e2-86d6-999902d3abab";
	private static final String FILENAME_ACCOUNT = "src/test/resources/impl/account-derby.xml";
	private static final String ACCOUNT_NEW_OID = "c0c010c0-d34d-b44f-f11d-333222123456";
	private static final String DB_TABLE_CONNECTOR_TYPE = "org.identityconnectors.databasetable.DatabaseTableConnector";
	
	private static final Trace LOGGER = TraceManager.getTrace(ProvisioningServiceImplDBTest.class);

	private static DerbyController derbyController = new DerbyController();
	
	@Autowired
	private ProvisioningService provisioningService;
	
	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.test.AbstractIntegrationTest#initSystem()
	 */
	
	@Override
	public void initSystem(OperationResult initResult) throws Exception {
		provisioningService.postInit(initResult);
		addResourceFromFile(FILENAME_RESOURCE_DERBY, DB_TABLE_CONNECTOR_TYPE, initResult);
	}
	
	@BeforeClass
	public static void startDb() throws Exception {
		LOGGER.info("------------------------------------------------------------------------------");
		LOGGER.info("START:  ProvisioningServiceImplDBTest");
		LOGGER.info("------------------------------------------------------------------------------");
		derbyController.startCleanServer();
	}

	@AfterClass
	public static void stopDb() throws Exception {
		derbyController.stop();
		LOGGER.info("------------------------------------------------------------------------------");
		LOGGER.info("STOP:  ProvisioningServiceImplDBTest");
		LOGGER.info("------------------------------------------------------------------------------");
	}

	
	@Test
	public void test000Integrity() throws ObjectNotFoundException, SchemaException {
		displayTestTile("test000Integrity");
		
		OperationResult result = new OperationResult(ProvisioningServiceImplDBTest.class.getName()+".test000Integrity");
		
		ResourceType resource = repositoryService.getObject(ResourceType.class, RESOURCE_DERBY_OID, null, result).asObjectable();
		String connectorOid = resource.getConnectorRef().getOid();
		ConnectorType connector = repositoryService.getObject(ConnectorType.class, connectorOid, null, result).asObjectable();
		assertNotNull(connector);
		display("DB Connector",connector);
	}
	
	@Test
	public void test001Connection() throws ObjectNotFoundException, SchemaException {
		displayTestTile("test001Connection");
		OperationResult result = new OperationResult(ProvisioningServiceImplDBTest.class.getName()+".test001Connection");
		
		OperationResult testResult = provisioningService.testResource(RESOURCE_DERBY_OID);
		
		display("Test result",testResult);
		assertSuccess("Test resource failed (result)", testResult);
		
		ResourceType resource = repositoryService.getObject(ResourceType.class, RESOURCE_DERBY_OID, null, result).asObjectable();
		display("Resource after test",resource);
	}
	
	@Test
	public void test002AddAccount() throws Exception {
		displayTestTile("test002AddAccount");
		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplDBTest.class.getName()
				+ ".test002AddAccount");

		AccountShadowType account = parseObjectTypeFromFile(FILENAME_ACCOUNT, AccountShadowType.class);

		System.out.println(SchemaDebugUtil.prettyPrint(account));
		System.out.println(account.asPrismObject().dump());

		// WHEN
		String addedObjectOid = provisioningService.addObject(account.asPrismObject(), null, result);
		
		// THEN
		result.computeStatus();
		display("add object result",result);
		assertSuccess("addObject has failed (result)",result);
		assertEquals(ACCOUNT_NEW_OID, addedObjectOid);

		AccountShadowType accountType =  repositoryService.getObject(AccountShadowType.class, ACCOUNT_NEW_OID,
				new PropertyReferenceListType(), result).asObjectable();
		assertEquals("will", accountType.getName());

		AccountShadowType provisioningAccountType = provisioningService.getObject(AccountShadowType.class, ACCOUNT_NEW_OID,
				new PropertyReferenceListType(), result).asObjectable();
		assertEquals("will", provisioningAccountType.getName());
		
		// Check database content
		
		Connection conn = derbyController.getConnection();
		// Check if it empty
		Statement stmt = conn.createStatement();
		stmt.execute("select * from users");
		ResultSet rs = stmt.getResultSet();
		
		assertTrue("The \"users\" table is empty",rs.next());
		assertEquals("will",rs.getString(DerbyController.COLUMN_LOGIN));
		assertEquals("3lizab3th",rs.getString(DerbyController.COLUMN_PASSWORD));
		assertEquals("Will Turner",rs.getString(DerbyController.COLUMN_FULL_NAME));
		
		assertFalse("The \"users\" table has more than one record",rs.next());
		rs.close();
		stmt.close();
	}
}
