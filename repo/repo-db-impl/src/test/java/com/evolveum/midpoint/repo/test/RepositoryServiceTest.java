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

package com.evolveum.midpoint.repo.test;

import com.evolveum.midpoint.common.Utils;
import com.evolveum.midpoint.model.SimpleDomainObject;
import com.evolveum.midpoint.repo.*;
import com.evolveum.midpoint.repo.spring.GenericDao;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.ObjectNotFoundFaultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.IllegalArgumentFaultType;
import java.io.File;
import java.math.BigInteger;
import java.sql.Connection;
import java.util.UUID;
import javax.sql.DataSource;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.operation.DatabaseOperation;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import static org.junit.Assert.*;

//TODO: update tests to start its own testing database
/**
 * Test of spring application context initialization
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"../../../../../application-context-repository.xml", "../../../../../application-context-repository-test.xml"})
public class RepositoryServiceTest {

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RepositoryServiceTest.class);
    @Autowired(required = true)
    GenericDao genericDao;
    @Autowired(required = true)
    private DataSource dataSource;
    @Autowired(required = true)
    private RepositoryPortType repositoryService;

    public RepositoryPortType getRepositoryService() {
        return repositoryService;
    }

    public void setRepositoryService(RepositoryPortType repositoryService) {
        this.repositoryService = repositoryService;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public GenericDao getGenericDao() {
        return genericDao;
    }

    public void setGenericDao(GenericDao genericDao) {
        this.genericDao = genericDao;
    }

    public RepositoryServiceTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        addDataToDB("src/test/resources/empty-dataset.xml");
        addDataToDB("src/test/resources/full-dataset.xml");
    }

    @After
    public void tearDown() {
        addDataToDB("src/test/resources/empty-dataset.xml");
    }

    private void addDataToDB(String filename) {
        try {
            Connection con = DataSourceUtils.getConnection(getDataSource());
            IDatabaseConnection connection = new DatabaseConnection(con);
            // initialize your dataset here
            IDataSet dataSet = new FlatXmlDataSet(new File(filename));
            try {
                DatabaseOperation.CLEAN_INSERT.execute(connection, dataSet);
            } finally {
                connection.close();
                con.close();
            }
        } catch (Exception ex) {
            logger.info("[RepositoryServiceTest] AddDataToDb failed");
            logger.info("Exception was {}", ex);
        }
    }

    private UserType addUserForTest() throws FaultMessage {
        ObjectContainerType userContainer = new ObjectContainerType();
        UserType user = new UserType();
        user.setFamilyName("aaa");
        user.setFullName("aaa");
        user.setGivenName("aaa");
        user.setName("aaa");
        user.setVersion("0.1");
        user.setExtension(null);

        userContainer.setObject(user);
        //RepositoryService instance = new RepositoryService();
        //instance.setGenericDao(genericDao);
        String result = repositoryService.addObject(userContainer);
        assertNotNull(result);
        assertNotSame("", result);

        user.setOid(result);
        return user;
    }

    /**
     * Test of addUser method, of class RepositoryService.
     */
    @Test
    public void testAddUser() throws FaultMessage {
        addUserForTest();
    }

    /**
     * Test of lookupUser method, of class RepositoryService.
     */
    @Test
    public void testLookupUser() throws FaultMessage {
        UserType testUser = addUserForTest();
        String oid = testUser.getOid();
        //RepositoryService instance = new RepositoryService();
        //instance.setGenericDao(genericDao);
        ObjectContainerType result = repositoryService.getObject(oid, new PropertyReferenceListType());
        assertEquals(oid, result.getObject().getOid());
    }

    /**
     * Test of listUsers method, of class RepositoryService.
     */
//    @Test
//    public void testListUsers() throws FaultMessage {
//
//        //RepositoryService instance = new RepositoryService();
//        //instance.setGenericDao(genericDao);
//        ObjectListType result = repositoryService.listObjects(Utils.getObjectType("UserType"), new PagingType());
//
//        assertNotNull(result.getObject());
//        assertTrue(0 < result.getObject().size());
//    }
    /**
     * Test of modifyUser method, of class RepositoryService.
     */
//    @Test
//    public void testModifyUser() {
//        UserType testUser = addUserForTest();
//
//        ObjectChangeType changes = new ObjectChangeType();
//        PropertyChangeType change = new PropertyChangeType();
//        change.setChangeType(PropertyChangeTypeType.replace);
////        change.setProperty("fullName");
//        CoreDocumentImpl doc = new CoreDocumentImpl();
//        ElementImpl element = new ElementImpl(doc, "fullName");
//        element.setTextContent("haluz");
//        change.getAny().add(element);
//
//        changes.setOid(testUser.getOid());
//        changes.getChange().add(change);
//
//        RepositoryService instance = new RepositoryService();
//        instance.setGenericDao(genericDao);
//
//        instance.modifyUser(changes);
//
//        SimpleDomainObject user = genericDao.findById(UUID.fromString(testUser.getOid()));
//        assertEquals("haluz", ((User)user).getFullName());
//    }
    /**
     * Test of deleteUser method, of class RepositoryService.
     */
    @Test
    public void testDeleteUser() throws FaultMessage {
        UserType testUser = addUserForTest();

        RepositoryService instance = new RepositoryService();
        instance.setGenericDao(genericDao);

        String oid = testUser.getOid();
        instance.deleteObject(oid);
        SimpleDomainObject returnedUser = genericDao.findById(UUID.fromString(oid));
        assertNull(returnedUser);
    }

    @Test
    public void testGetObject() throws Exception {
        PropertyReferenceListType property = new PropertyReferenceListType();

        ObjectContainerType objectContainer = repositoryService.getObject("92c058ac-97ce-4d44-a443-4d9b6ad56c1e", property);
        UserType userType = (UserType) objectContainer.getObject();
        assertNotNull(userType);
        assertNotNull(userType.getAccountRef());
        assertEquals("9de528cb-ada2-4b76-8c4b-122241706987", userType.getAccountRef().get(0).getOid());
        System.out.println("type " + userType.getAccountRef().get(0).getType());

        assertEquals(1, userType.getAccountRef().size());

    }

    @Test
    public void testGetAccountObject() throws FaultMessage {
        PropertyReferenceListType property = new PropertyReferenceListType();

        ObjectContainerType objectContainer = repositoryService.getObject("9de528cb-ada2-4b76-8c4b-122241706987", property);
        AccountShadowType accountType = (AccountShadowType) objectContainer.getObject();
        assertNotNull(accountType);
        assertEquals("cptjack", accountType.getName());
        assertNotNull(accountType.getResourceRef());
        assertEquals("d0db5be9-cb93-401f-b6c1-86ffffe4cd5e", accountType.getResourceRef().getOid());

    }

    @Test
    public void testGetResourceObject() throws FaultMessage {
        PropertyReferenceListType property = new PropertyReferenceListType();

        ObjectContainerType objectContainer = repositoryService.getObject("d0db5be9-cb93-401f-b6c1-86ffffe4cd5e", property);
        ResourceType resourceType = (ResourceType) objectContainer.getObject();
        assertNotNull(resourceType);
        assertEquals("Localhost OpenDS90", resourceType.getName());
        assertEquals("http://midpoint.evolveum.com/schema/resources/instances/ldap/localhostOpenDS", resourceType.getNamespace());
        assertNotNull(resourceType.getSchema());

    }

    @Test
    public void testGetResourceAccessConfigurationObject() throws FaultMessage {
        PropertyReferenceListType property = new PropertyReferenceListType();

        ObjectContainerType objectContainer = repositoryService.getObject("d0db5be9-cb93-401f-b6c1-86ffffe4cd5f", property);
        ResourceAccessConfigurationType resourceType = (ResourceAccessConfigurationType) objectContainer.getObject();
        assertNotNull(resourceType);
        assertEquals("Identity Connector Integration", resourceType.getName());
        assertEquals("http://midpoint.evolveum.com/xml/ns/public/resource/idconnector/resourceaccessconfiguration-1.xsd", resourceType.getNamespace());
        assertNotNull(resourceType.getSchema());

    }

    @Test
    public void testGetObjectDoesNotExits() {
        try {
            ObjectContainerType objectContainer = repositoryService.getObject("00000000-0000-0000-0000-000000000000", null);
            AccountShadowType accountType = (AccountShadowType) objectContainer.getObject();
            assertNull(accountType);
        } catch (FaultMessage ex) {
            if (ex.getFaultInfo() instanceof ObjectNotFoundFaultType) {
                return;
            }
        }

        fail("Expected exception was not thrown");


    }

    @Test
    public void testListObjects() throws FaultMessage {
        ObjectListType objectListType = repositoryService.listObjects(Utils.getObjectType("AccountType"), new PagingType());
        assertNotNull(objectListType.getObject());
        assertEquals(1, objectListType.getObject().size());

        AccountShadowType account = (AccountShadowType) objectListType.getObject().get(0);
        assertNotNull(account.getAttributes());

    }

    @Test
    public void testListUsers() throws FaultMessage {
        PagingType pagingType = new PagingType();
        pagingType.setMaxSize(BigInteger.valueOf(5));
        pagingType.setOffset(BigInteger.valueOf(-1));

        pagingType.setOrderBy(Utils.fillPropertyReference("name"));
        pagingType.setOrderDirection(OrderDirectionType.ASCENDING);
        ObjectListType objectListType = repositoryService.listObjects(Utils.getObjectType("UserType"), pagingType);
        assertNotNull(objectListType.getObject());
//        assertEquals(1, objectListType.getObject().size());

//        for (ObjectType objType : objectListType.getObject()){
//            System.out.println(objType.getName());
//        }

    }

    @Test
    public void testListObjectDoesNotExist() {
        try {
            repositoryService.listObjects("Abcd", new PagingType());
        } catch (FaultMessage ex) {
            if (ex.getFaultInfo() instanceof IllegalArgumentFaultType) {
                return;
            }
        }
        fail();
    }

    @Test
    public void testDeleteObject() {
        try {
            repositoryService.deleteObject("92c058ac-97ce-4d44-a443-4d9b6ad56c1e");
        } catch (FaultMessage ex) {
            logger.info("[RepositoryServiceTest] deleteObject failed");
            fail("Unexpected exception was thrown" + ex.getMessage());
        }

        assertNull(genericDao.findById(UUID.fromString("92c058ac-97ce-4d44-a443-4d9b6ad56c1e")));

    }

    @Test
    public void testDeleteObjectDoesNotExist() {
        RepositoryService instance = new RepositoryService();
        instance.setGenericDao(genericDao);
        try {
            instance.deleteObject("00000000-0000-0000-0000-000000000000");
        } catch (FaultMessage ex) {
            if (ex.getFaultInfo() instanceof ObjectNotFoundFaultType) {
                return;
            }
        }

        fail("Expected exception was not thrown");
    }

    @Test
    public void testListAccountShadowOwner() throws FaultMessage {
        UserContainerType userContainer = repositoryService.listAccountShadowOwner("9de528cb-ada2-4b76-8c4b-122241706987");
        UserType userType = userContainer.getUser();
        assertNotNull(userType);
        assertEquals("92c058ac-97ce-4d44-a443-4d9b6ad56c1e", userType.getOid());

    }

    @Test
    public void testListAccountShadowDoeasNotExistOwner() throws Exception {
        UserContainerType userContainer = repositoryService.listAccountShadowOwner("00000000-0000-0000-0000-000000000000");
        UserType userType = userContainer.getUser();
        assertNull(userType);
    }

    @Test
    public void testListResourceObjectShadows() throws FaultMessage {
        ResourceObjectShadowListType result = repositoryService.listResourceObjectShadows("d0db5be9-cb93-401f-b6c1-86ffffe4cd5e", Utils.getObjectType("AccountType"));
        assertNotNull(result.getObject());
        assertEquals(1, result.getObject().size());
        assertEquals("cptjack", result.getObject().get(0).getName());
    }

    @Test
    public void testListResourceDoesNotExistObjectShadows() {
        try {
            repositoryService.listResourceObjectShadows("00000000-0000-0000-0000-000000000000", Utils.getObjectType("AccountType"));
        } catch (FaultMessage ex) {
            if (ex.getFaultInfo() instanceof ObjectNotFoundFaultType) {
                return;
            }
        }
        fail();
    }
}
