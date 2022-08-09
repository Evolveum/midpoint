/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl;

import static org.testng.AssertJUnit.*;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.util.DerbyController;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PasswordCapabilityType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestDBTable extends AbstractIntegrationTest {

    protected static final File TEST_DIR = new File("src/test/resources/db");

    private static final File RESOURCE_DERBY_FILE = new File(TEST_DIR, "resource-derby.xml");
    private static final String RESOURCE_DERBY_OID = "ef2bc95b-76e0-59e2-86d6-999902d3abab";
    private static final File ACCOUNT_WILL_FILE = new File(TEST_DIR, "account-derby.xml");
    private static final String ACCOUNT_WILL_OID = "c0c010c0-d34d-b44f-f11d-333222123456";
    private static final String ACCOUNT_WILL_USERNAME = "will";
    private static final String ACCOUNT_WILL_FULLNAME = "Will Turner";
    private static final String ACCOUNT_WILL_PASSWORD = "3lizab3th";
    private static final String DB_TABLE_CONNECTOR_TYPE = "org.identityconnectors.databasetable.DatabaseTableConnector";

    private final DerbyController derbyController = new DerbyController();

    @Autowired
    private ProvisioningService provisioningService;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        // We need to switch off the encryption checks. Some values cannot be encrypted as we do
        // not have a definition here
        InternalsConfig.encryptionChecks = false;
        provisioningService.postInit(initResult);
        addResourceFromFile(RESOURCE_DERBY_FILE, DB_TABLE_CONNECTOR_TYPE, initResult);
    }

    @BeforeClass
    public void startDb() throws Exception {
        logger.info("------------------------------------------------------------------------------");
        logger.info("START:  ProvisioningServiceImplDBTest");
        logger.info("------------------------------------------------------------------------------");
        derbyController.startCleanServer();
    }

    @AfterClass
    public void stopDb() throws Exception {
        derbyController.stop();
        logger.info("------------------------------------------------------------------------------");
        logger.info("STOP:  ProvisioningServiceImplDBTest");
        logger.info("------------------------------------------------------------------------------");
    }

    @Test
    public void test000Integrity() throws ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();

        ResourceType resource = repositoryService.getObject(ResourceType.class, RESOURCE_DERBY_OID, null, result).asObjectable();
        String connectorOid = resource.getConnectorRef().getOid();
        ConnectorType connector = repositoryService.getObject(ConnectorType.class, connectorOid, null, result).asObjectable();
        assertNotNull(connector);
        display("DB Connector", connector);
    }

    @Test
    public void test001Connection() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        OperationResult testResult = provisioningService.testResource(RESOURCE_DERBY_OID, task, result);

        display("Test result", testResult);
        TestUtil.assertSuccess("Test resource failed (result)", testResult);

        ResourceType resource = repositoryService.getObject(ResourceType.class, RESOURCE_DERBY_OID, null, result).asObjectable();
        display("Resource after test", resource);
        displayValue("Resource after test (XML)", PrismTestUtil.serializeToXml(resource));

        CapabilityCollectionType nativeCapabilities = resource.getCapabilities().getNative();
        CredentialsCapabilityType credentialsCapabilityType = CapabilityUtil.getCapability(nativeCapabilities, CredentialsCapabilityType.class);
        assertNotNull("No credentials capability", credentialsCapabilityType);
        PasswordCapabilityType passwordCapabilityType = credentialsCapabilityType.getPassword();
        assertNotNull("No password in credentials capability", passwordCapabilityType);
        assertEquals("Wrong password capability ReturnedByDefault", Boolean.FALSE, passwordCapabilityType.isReturnedByDefault());
    }

    @Test
    public void test002AddAccount() throws Exception {
        given();
        OperationResult result = createOperationResult();

        ShadowType account = parseObjectType(ACCOUNT_WILL_FILE, ShadowType.class);

        System.out.println(SchemaDebugUtil.prettyPrint(account));
        System.out.println(account.asPrismObject().debugDump());

        Task task = taskManager.createTaskInstance();
        when();
        String addedObjectOid = provisioningService.addObject(account.asPrismObject(), null, null, task, result);

        then();
        result.computeStatus();
        display("add object result", result);
        TestUtil.assertSuccess("addObject has failed (result)", result);
        assertEquals(ACCOUNT_WILL_OID, addedObjectOid);

        ShadowType accountType = repositoryService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, result).asObjectable();
        PrismAsserts.assertEqualsPolyString("Name not equal.", ACCOUNT_WILL_USERNAME, accountType.getName());

        ShadowType provisioningAccountType = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result).asObjectable();
        PrismAsserts.assertEqualsPolyString("Name not equal.", ACCOUNT_WILL_USERNAME, provisioningAccountType.getName());

        // Check database content

        Connection conn = derbyController.getConnection();
        // Check if empty
        Statement stmt = conn.createStatement();
        stmt.execute("select * from users");
        ResultSet rs = stmt.getResultSet();

        assertTrue("The \"users\" table is empty", rs.next());
        assertEquals(ACCOUNT_WILL_USERNAME, rs.getString(DerbyController.COLUMN_LOGIN));
        assertEquals(ACCOUNT_WILL_PASSWORD, rs.getString(DerbyController.COLUMN_PASSWORD));
        assertEquals(ACCOUNT_WILL_FULLNAME, rs.getString(DerbyController.COLUMN_FULL_NAME));

        assertFalse("The \"users\" table has more than one record", rs.next());
        rs.close();
        stmt.close();
    }

    // MID-1234
    @Test(enabled = false)
    public void test005GetAccount() throws Exception {
        given();
        OperationResult result = createOperationResult();

        Task task = taskManager.createTaskInstance();
        when();
        PrismObject<ShadowType> account = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);

        then();
        result.computeStatus();
        display(result);
        TestUtil.assertSuccess(result);

        PrismAsserts.assertEqualsPolyString("Name not equal.", ACCOUNT_WILL_USERNAME, account.asObjectable().getName());

        assertNotNull("No credentials", account.asObjectable().getCredentials());
        assertNotNull("No password", account.asObjectable().getCredentials().getPassword());
        assertNotNull("No password value", account.asObjectable().getCredentials().getPassword().getValue());
        ProtectedStringType password = account.asObjectable().getCredentials().getPassword().getValue();
        displayValue("Password", password);
        String clearPassword = protector.decryptString(password);
        assertEquals("Wrong password", ACCOUNT_WILL_PASSWORD, clearPassword);
    }
}
