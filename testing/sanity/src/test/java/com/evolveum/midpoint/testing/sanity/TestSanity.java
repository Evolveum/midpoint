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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.testing.sanity;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.ws.Holder;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.test.ldap.OpenDJUnitTestAdapter;
import com.evolveum.midpoint.test.ldap.OpenDJUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1.ModelPortType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.ModelService;


/**
 * Sanity test suite.
 * 
 * It tests the very basic representative test cases. It does not try to be complete. It rather should be quick to execute
 * and pass through the most representative cases. It should test all the system components except for GUI. Therefore the
 * test cases are selected to pass through most of the components.
 * 
 * It is using mock BaseX repository and embedded OpenDJ instance as a testing resource. The BaseX repository is instantiated
 * from the Spring context in the same way as all other components. OpenDJ instance is started explicitly using BeforeClass
 * method. Appropriate resource definition to reach the OpenDJ instance is provided in the test data and is inserted in the
 * repository as part of test initialization.
 * 
 * @author Radovan Semancik
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-provisioning.xml",
		"classpath:application-context-sanity-test.xml"})
public class TestSanity extends OpenDJUnitTestAdapter {

	private static final String FILENAME_RESOURCE_OPENDJ = "src/test/resources/repo/opendj-resource.xml";
	private static final String RESOURCE_OPENDJ_OID = "ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";

	/**
	 * Utility to control embedded OpenDJ instance (start/stop)
	 */
	protected static OpenDJUtil djUtil;
	
	/**
	 * Unmarshalled resource definition to reach the embedded OpenDJ instance.
	 * Used for convenience - the tests method may find it handy.
	 */
	ResourceType resource;
	
	private JAXBContext jaxbctx;
	private Unmarshaller unmarshaller;
	
	/**
	 * The instance of ModelService. This is the interface that we will test.
	 */
	@Autowired(required = true)
	private ModelPortType model;
	
	@Autowired(required = true)
	private RepositoryService repositoryService;
	private static boolean repoInitialized = false;

	public TestSanity() throws JAXBException {
		djUtil = new OpenDJUtil();
		jaxbctx = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		unmarshaller = jaxbctx.createUnmarshaller();
	}

	/**
	 * Initialize embedded OpenDJ instance
	 * @throws Exception
	 */
	@BeforeClass
	public static void startLdap() throws Exception {
		startACleanDJ();
	}

	/**
	 * Shutdown embedded OpenDJ instance
	 * @throws Exception
	 */
	@AfterClass
	public static void stopLdap() throws Exception {
		stopDJ();
	}
		
	@Before
	public void initRepository() throws Exception {
		System.out.println("start initRepository("+this+"): repoInitialized="+repoInitialized);
		if (!repoInitialized) {
			resource = (ResourceType) addObjectFromFile(FILENAME_RESOURCE_OPENDJ);
			repoInitialized = true;
		}
		System.out.println("finish initRepository("+this+"): repoInitialized="+repoInitialized);
	}

	private ObjectType createObjectFromFile(String filePath) throws FileNotFoundException, JAXBException {
		File file = new File(filePath);
		FileInputStream fis = new FileInputStream(file);
		Object object = unmarshaller.unmarshal(fis);
		ObjectType objectType = ((JAXBElement<ObjectType>) object).getValue();
		return objectType;
	}

	private ObjectType addObjectFromFile(String filePath) throws Exception {
		ObjectType object = createObjectFromFile(filePath);
		System.out.println("obj: " + object.getName());
		OperationResult result = new OperationResult(TestSanity.class.getName()
				+ ".addObjectFromFile");
		repositoryService.addObject(object, result);
		return object;
	}
	
	/**
	 * Test integrity of the test setup.
	 * @throws SchemaException 
	 * @throws ObjectNotFoundException 
	 */
	@Test
	public void test000Integrity() throws ObjectNotFoundException, SchemaException {
		assertNotNull(resource);
		assertNotNull(model);
		assertNotNull(repositoryService);
		
		OperationResult result = new OperationResult(TestSanity.class.getName()
				+ ".test000Integrity");
		ObjectType object = repositoryService.getObject(RESOURCE_OPENDJ_OID, null, result);
		assertTrue(object instanceof ResourceType);
		assertEquals(RESOURCE_OPENDJ_OID,object.getOid());
		
		// TODO: test if OpenDJ is running
	}
	
	@Test
	public void test001TestConnection() throws FaultMessage, JAXBException {
		OperationResultType result = new OperationResultType();
		Holder<OperationResultType> holder = new Holder<OperationResultType>(result);
		model.testResource(RESOURCE_OPENDJ_OID, holder);
		
		Document doc = DOMUtil.getDocument();
		Element element = JAXBUtil.jaxbToDom(result, new QName("result"), doc);
		System.out.println(DOMUtil.serializeDOMToString(element));
	}
	
	//TODO: create user
	
	//TODO: assign account to user: should create account on OpenDJ
	//TODO: check with getObject
	
	//TODO: delete user (with the account): should also delete the account on OpenDJ
	
}
