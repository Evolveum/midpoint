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

import com.evolveum.midpoint.common.diff.CalculateXmlDiff;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectContainerType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;
import java.io.File;
import java.sql.Connection;

import javax.sql.DataSource;
import javax.xml.bind.JAXBElement;

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

/**
 * 
 * @author sleepwalker
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "../../../../../application-context-repository.xml",
		"../../../../../application-context-repository-test.xml" })
public class RepositoryResourceTest {

	org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RepositoryResourceTest.class);

	@Autowired(required = true)
	private RepositoryPortType repositoryService;
	@Autowired(required = true)
	private DataSource dataSource;

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

	public RepositoryResourceTest() {
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
	}

	@After
	public void tearDown() {
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

	@Test
	public void testResource() throws Exception {
		final String resourceOid = "aae7be60-df56-11df-8608-0002a5d5c51b";
		try {
			ObjectContainerType objectContainer = new ObjectContainerType();
			ObjectListType objects = repositoryService.listObjects(QNameUtil.qNameToUri(SchemaConstants.I_RESOURCE_TYPE), new PagingType());
			int oldSize = objects.getObject().size();
			
			ResourceType resource = ((JAXBElement<ResourceType>) JAXBUtil.unmarshal(new File(
					"src/test/resources/aae7be60-df56-11df-8608-0002a5d5c51b.xml"))).getValue();
			objectContainer.setObject(resource);
			repositoryService.addObject(objectContainer);
			ObjectContainerType retrievedObjectContainer = repositoryService.getObject(resourceOid,
					new PropertyReferenceListType());
			
			assertEquals(resource.getOid(), ((ResourceType) (retrievedObjectContainer.getObject())).getOid());
			
			objects = repositoryService.listObjects(
					QNameUtil.qNameToUri(SchemaConstants.I_RESOURCE_TYPE), new PagingType());
			
			assertEquals(oldSize + 1 , objects.getObject().size());

			boolean oidTest = false;
			for ( ObjectType o: objects.getObject()) {
				if ( resourceOid.equals(o.getOid())) {
					oidTest = true;
				}
			}
			assertTrue(oidTest);
			
			
		} finally {
			repositoryService.deleteObject(resourceOid);
		}
	}

	@Test
	public void testResourceModification() throws Exception {
		final String resourceOid = "aae7be60-df56-11df-8608-0002a5d5c51b";
		try {
			ObjectContainerType objectContainer = new ObjectContainerType();

			ResourceType resource = ((JAXBElement<ResourceType>) JAXBUtil.unmarshal(new File(
					"src/test/resources/aae7be60-df56-11df-8608-0002a5d5c51b.xml"))).getValue();
			objectContainer.setObject(resource);
			repositoryService.addObject(objectContainer);
			ObjectContainerType retrievedObjectContainer = repositoryService.getObject(resourceOid,
					new PropertyReferenceListType());
			assertEquals(resource.getOid(), ((ResourceType) (retrievedObjectContainer.getObject())).getOid());
			ResourceType modifiedResource = ((JAXBElement<ResourceType>) JAXBUtil.unmarshal(new File(
					"src/test/resources/resource-modified-removed-tags.xml"))).getValue();
			ObjectModificationType objectModificationType = CalculateXmlDiff.calculateChanges(new File(
					"src/test/resources/aae7be60-df56-11df-8608-0002a5d5c51b.xml"), new File(
					"src/test/resources/resource-modified-removed-tags.xml"));
			repositoryService.modifyObject(objectModificationType);
			retrievedObjectContainer = repositoryService.getObject(resourceOid,
					new PropertyReferenceListType());
			assertEquals(modifiedResource.getOid(),
					((ResourceType) (retrievedObjectContainer.getObject())).getOid());
			assertEquals(modifiedResource.getSchemaHandling(),
					((ResourceType) (retrievedObjectContainer.getObject())).getSchemaHandling());
			assertEquals(modifiedResource.getConfiguration(),
					((ResourceType) (retrievedObjectContainer.getObject())).getConfiguration());
			assertEquals(modifiedResource.getSchema(),
					((ResourceType) (retrievedObjectContainer.getObject())).getSchema());
			assertEquals(modifiedResource.getScripts(),
					((ResourceType) (retrievedObjectContainer.getObject())).getScripts());
		} finally {
			repositoryService.deleteObject(resourceOid);
		}
	}
}
