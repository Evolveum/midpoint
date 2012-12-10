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
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeClass;
import org.testng.Assert;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.bind.JAXBElement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserTemplateType;

/**
 *
 * @author Igor Farinic
 */
@ContextConfiguration(locations = {"../../../../../ctx-repository.xml",
        "classpath:ctx-repo-cache.xml",
		"classpath:ctx-configuration-basex-test.xml"})
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
    
    @BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		DebugUtil.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
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

    @SuppressWarnings("unchecked")
	@Test
    public void testUserTemplate() throws Exception {
    	String userTemplateOid = "c0c010c0-d34d-b33f-f00d-777111111111";
        try {
        	//add object
            PrismObject<UserTemplateType> userTemplate = PrismTestUtil.parseObject(new File("src/test/resources/user-template.xml"));
            repositoryService.addObject(userTemplate, new OperationResult("test"));
            
            //get object
            PrismObject<UserTemplateType> retrievedObject = repositoryService.getObject(UserTemplateType.class, userTemplateOid, new OperationResult("test"));
            PrismAsserts.assertEquals(userTemplate, retrievedObject);
            
            //list object
            List<PrismObject<UserTemplateType>> objects = repositoryService.searchObjects(UserTemplateType.class, null, new PagingType(), new OperationResult("test"));
            assertEquals(1, objects.size());
            assertEquals(userTemplateOid, objects.get(0).getOid());
            
			// delete object
			repositoryService.deleteObject(UserTemplateType.class, userTemplateOid, new OperationResult("test"));
			try {
				repositoryService.getObject(ObjectType.class, userTemplateOid, new OperationResult("test"));
				Assert.fail("Object with oid " + userTemplateOid + " was not deleted");
			} catch (ObjectNotFoundException ex) {
				//ignore
			}
		} finally {
			// to be sure try to delete the object as part of cleanup
			try {
				repositoryService.deleteObject(UserTemplateType.class, userTemplateOid, new OperationResult("test"));
			} catch (Exception ex) {
				// ignore exceptions during cleanup
			}
		}
    }

}
