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
 * Portions Copyrighted 2011 Igor Farinic
 * 
 */

package com.evolveum.midpoint.repo.test;

import static org.junit.Assert.*;

import java.io.File;

import javax.xml.bind.JAXBElement;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.ExpectedException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectContainerType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;

/**
 * 
 * @author Igor Farinic
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "../../../../../application-context-repository.xml",
		"../../../../../application-context-repository-test.xml" })
public class RepositoryTest {

	@Autowired(required = true)
	private RepositoryPortType repositoryService;

	public RepositoryPortType getRepositoryService() {
		return repositoryService;
	}

	public void setRepositoryService(RepositoryPortType repositoryService) {
		this.repositoryService = repositoryService;
	}

	@BeforeClass
	public static void setUpClass() throws Exception {
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
	}

	@Before
	public void setUp() {
	}

	@After
	public void tearDown() {
	}

	//FIXME: change expected exception to ObjectAlreadyExistsException when repo is switched to new interface
	@Test
	@ExpectedException(value=FaultMessage.class)
	public void addObjectThatAlreadyExists() throws Exception {
		String oid = "c0c010c0-d34d-b33f-f00d-111111111111";
		try {
			// store user
			ObjectContainerType objectContainer = new ObjectContainerType();
			UserType user = ((JAXBElement<UserType>) JAXBUtil.unmarshal(new File(
					"src/test/resources/user.xml"))).getValue();
			objectContainer.setObject(user);
			repositoryService.addObject(objectContainer);
			
			//try to store the same object again, exception is expected
			repositoryService.addObject(objectContainer);
		} finally {
			// to be sure try to delete the object as part of cleanup
			try {
				repositoryService.deleteObject(oid);
			} catch (Exception ex) {
				// ignore exceptions during cleanup
			}
		}

	}

	@Test
	@ExpectedException(value=FaultMessage.class)
	public void getObjectThatDoNotExists() throws Exception {
		String oid = "c0c010c0-d34d-b33f-f00d-111111111234";
		//try to get not existing object, exception is expected
		repositoryService.getObject(oid, null);
	}
	
	@Test
	public void listObjectsNoObjectsOfThatTypeReturnsEmptyList() throws Exception {
		ObjectListType retrievedList = repositoryService.listObjects(QNameUtil.qNameToUri(SchemaConstants.I_RESOURCE_TYPE), null);
		assertNotNull(retrievedList);
		assertEquals(0, retrievedList.getObject().size());
		assertEquals(0, retrievedList.getCount().intValue());
	}	
	
	
	
}
