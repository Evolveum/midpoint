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
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.repo.test;

import static org.testng.AssertJUnit.assertEquals;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeClass;
import org.testng.Assert;
import java.io.File;
import java.util.List;

import javax.xml.bind.JAXBElement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ObjectTypes;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserTemplateType;

/**
 *
 * @author Igor Farinic
 */
@ContextConfiguration(locations = {"../../../../../application-context-repository.xml", "classpath:application-context-repository-test.xml"})
public class RepositoryUserTemplateTest extends AbstractTestNGSpringContextTests {

    @Autowired(required = true)
    private RepositoryService repositoryService;

    public RepositoryService getRepositoryService() {
        return repositoryService;
    }

    public void setRepositoryService(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    public RepositoryUserTemplateTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @BeforeMethod
	public void setUp() {
    }

    @AfterMethod
	public void tearDown() {
    }

    @Test
    public void testUserTemplate() throws Exception {
    	String userTemplateOid = "c0c010c0-d34d-b33f-f00d-777111111111";
        try {
        	//add object
            UserTemplateType userTemplate = ((JAXBElement<UserTemplateType>) JAXBUtil.unmarshal(new File("src/test/resources/user-template.xml"))).getValue();
            repositoryService.addObject(userTemplate, new OperationResult("test"));
            
            //get object
            ObjectType retrievedObject = repositoryService.getObject(userTemplateOid, new PropertyReferenceListType(), new OperationResult("test"));
            assertEquals(userTemplate.getPropertyConstruction().get(0).getProperty().getTextContent(), ((UserTemplateType) retrievedObject).getPropertyConstruction().get(0).getProperty().getTextContent());
            assertEquals(userTemplate.getAccountConstruction().get(0).getResourceRef().getOid(), ((UserTemplateType) retrievedObject).getAccountConstruction().get(0).getResourceRef().getOid());
            
            //list object
            List<UserTemplateType> objects = repositoryService.listObjects(UserTemplateType.class, new PagingType(), new OperationResult("test"));
            assertEquals(1, objects.size());
            assertEquals(userTemplateOid, objects.get(0).getOid());
            
			// delete object
			repositoryService.deleteObject(userTemplateOid, new OperationResult("test"));
			try {
				repositoryService.getObject(userTemplateOid, new PropertyReferenceListType(), new OperationResult("test"));
				Assert.fail("Object with oid " + userTemplateOid + " was not deleted");
			} catch (ObjectNotFoundException ex) {
				//ignore
			}
		} finally {
			// to be sure try to delete the object as part of cleanup
			try {
				repositoryService.deleteObject(userTemplateOid, new OperationResult("test"));
			} catch (Exception ex) {
				// ignore exceptions during cleanup
			}
		}
    }

}
