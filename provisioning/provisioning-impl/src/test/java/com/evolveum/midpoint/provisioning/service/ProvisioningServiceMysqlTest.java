/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.provisioning.service;

import org.junit.AfterClass;
import com.evolveum.midpoint.test.util.DerbyManager;
import java.io.FileReader;
import org.dbunit.dataset.xml.XmlDataSet;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.operation.DatabaseOperation;
import org.springframework.jdbc.datasource.DataSourceUtils;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;
import java.io.File;
import java.sql.Connection;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.junit.Test;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationalResultType;
import com.evolveum.midpoint.xml.ns._public.provisioning.resource_object_change_listener_1.ResourceObjectChangeListenerPortType;
import org.dbunit.dataset.Column;
import org.dbunit.dataset.ITable;
import static org.junit.Assert.*;
import org.junit.BeforeClass;

import static org.mockito.Mockito.*;

/**
 *
 * DISABLED for now. The test infrastructure is not finished yet.
 *
 * @author elek
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:application-context-provisioning.xml", "classpath:application-context-repository-test.xml"})
public class ProvisioningServiceMysqlTest {

    private static DerbyManager derby = new DerbyManager();

    @BeforeClass
    public static void startDb() throws Exception {
        derby.createDatabase();
    }

    @AfterClass
    public static void stopDb() throws Exception {
        derby.deleteDatabase();
    }
    @Autowired(required = true)
    private DataSource dataSource;
    @Autowired(required = true)
    private RepositoryPortType repositoryService;

    @Before
    public void setUp() throws Exception {
        addDataToDB(new FlatXmlDataSet(new File("target/test-data/dbunit/empty-dataset.xml")));
    }

    @After
    public void tearDown() {
        //addDataToDB("target/test-data/dbunit/empty-dataset.xml");
    }

    private void addDataToDB(IDataSet dataSet) throws Exception {


        Connection con = DataSourceUtils.getConnection(dataSource);
        IDatabaseConnection connection = new DatabaseConnection(con);
        // initialize your dataset here
        try {
            DatabaseOperation.CLEAN_INSERT.execute(connection, dataSet);
        } finally {
            connection.close();
            con.close();
        }

    }

    @Test
    @Ignore
    public void syncronizeWithoutStateObject() throws Exception {
        addDataToDB(new XmlDataSet(new FileReader(new File("src/test/resources/test-dataset_withoutstate.xml"))));
        //GIVEN
        //OperationalResultType opResult = new OperationalResultType();
        ProvisioningService service = new ProvisioningService();

        ResourceObjectChangeListenerPortType roclpt = mock(ResourceObjectChangeListenerPortType.class);
        service.setObjectChangeListener(roclpt);

        Connection con = DataSourceUtils.getConnection(dataSource);
        IDatabaseConnection connection = new DatabaseConnection(con);
        try {
            IDataSet databaseDataSet = connection.createDataSet();
            ITable accounts = databaseDataSet.getTable("Accounts");
            ITable resourceStates = databaseDataSet.getTable("ResourcesStates");
            assertEquals(0, accounts.getRowCount());
            assertEquals(0, resourceStates.getRowCount());

            //Create Sample Request
            service.setRepositoryPort(repositoryService);

            //WHEN
            service.synchronize("aae7be60-df56-11df-8608-0002a5d5c51b");

            //THEN
            resourceStates = databaseDataSet.getTable("ResourcesStates");
            accounts = databaseDataSet.getTable("Accounts");
            assertEquals("1", accounts.getValue(0, "name"));
            assertTrue(((String)resourceStates.getValue(0, "state")).contains(">3<"));
        } finally {
            connection.close();
            con.close();
        }
    }

    @Test
    @Ignore
    public void syncronizeWithThePreviousStateObject() throws Exception {
        addDataToDB(new XmlDataSet(new FileReader(new File("src/test/resources/test-dataset.xml"))));
        //GIVEN
        //OperationalResultType opResult = new OperationalResultType();
        ProvisioningService service = new ProvisioningService();

        ResourceObjectChangeListenerPortType roclpt = mock(ResourceObjectChangeListenerPortType.class);
        service.setObjectChangeListener(roclpt);
        Connection con = DataSourceUtils.getConnection(dataSource);
        IDatabaseConnection connection = new DatabaseConnection(con);
        try {
            IDataSet databaseDataSet = connection.createDataSet();
            ITable accounts = databaseDataSet.getTable("Accounts");
            ITable resourceStates = databaseDataSet.getTable("ResourcesStates");
            assertEquals(0, accounts.getRowCount());
            assertEquals(1, resourceStates.getRowCount());
            assertTrue(((String)resourceStates.getValue(0, "state")).contains(">1<"));

            //Create Sample Request
            service.setRepositoryPort(repositoryService);

            //WHEN
            service.synchronize("aae7be60-df56-11df-8608-0002a5d5c51b");

            //THEN
            resourceStates = databaseDataSet.getTable("ResourcesStates");
            accounts = databaseDataSet.getTable("Accounts");
            assertEquals("1", accounts.getValue(0, "name"));
            assertTrue(((String)resourceStates.getValue(0, "state")).contains(">3<"));
        } finally {
            connection.close();
            con.close();
        }
    }

    @Test
    @Ignore
    public void syncronizeAlreadyExistingObject() throws Exception {
        addDataToDB(new XmlDataSet(new FileReader(new File("src/test/resources/test-dataset_withaccount.xml"))));
        //GIVEN
        ProvisioningService service = new ProvisioningService();

        ResourceObjectChangeListenerPortType roclpt = mock(ResourceObjectChangeListenerPortType.class);
        service.setObjectChangeListener(roclpt);
        Connection con = DataSourceUtils.getConnection(dataSource);
        IDatabaseConnection connection = new DatabaseConnection(con);
        try {
            IDataSet databaseDataSet = connection.createDataSet();
            ITable accounts = databaseDataSet.getTable("Accounts");
            ITable accountAttributes = databaseDataSet.getTable("AccountAttributes");
            assertEquals("value2", accountAttributes.getValue(2, "attrvalue"));

            ITable resourceStates = databaseDataSet.getTable("ResourcesStates");
            assertEquals(1, accounts.getRowCount());
            assertEquals(1, resourceStates.getRowCount());
            assertTrue(((String)resourceStates.getValue(0, "state")).contains(">1<"));

            //Create Sample Request
            service.setRepositoryPort(repositoryService);

            //WHEN
            service.synchronize("aae7be60-df56-11df-8608-0002a5d5c51b");

            //THEN
            resourceStates = databaseDataSet.getTable("ResourcesStates");
            accounts = databaseDataSet.getTable("Accounts");
            accountAttributes = databaseDataSet.getTable("AccountAttributes");
            assertEquals("value1", accountAttributes.getValue(2, "attrvalue"));
            assertEquals("1", accounts.getValue(0, "name"));
            assertTrue(((String)resourceStates.getValue(0, "state")).contains(">3<"));
        } finally {
            connection.close();
            con.close();
        }
    }

    private void printRepoContent() throws Exception {
        Connection con = DataSourceUtils.getConnection(dataSource);
        IDatabaseConnection connection = new DatabaseConnection(con);
        try {
            IDataSet databaseDataSet = connection.createDataSet();
            for (String tableName : databaseDataSet.getTableNames()) {
                ITable table = databaseDataSet.getTable(tableName);
                System.out.println("*********Table name: " + tableName);
                for (int i = 0; i < table.getRowCount(); i++) {
                    System.out.println("*************Row nr."+i);
                    for (Column column : table.getTableMetaData().getColumns()) {
                        System.out.println("****************"+column.getColumnName()+": " + table.getValue(i, column.getColumnName()));
                    }
                }
            }
           } finally {
            connection.close();
            con.close();
        }
    }
}
