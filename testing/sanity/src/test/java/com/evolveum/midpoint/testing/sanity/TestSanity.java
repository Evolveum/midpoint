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
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.ws.Holder;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
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
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1.ModelPortType;


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

	private static final String FILENAME_SYSTEM_CONFIGURATION = "src/test/resources/repo/system-configuration.xml";
	private static final String SYSTEM_CONFIGURATION_OID = "00000000-0000-0000-0000-000000000001";
	
	private static final String FILENAME_RESOURCE_OPENDJ = "src/test/resources/repo/opendj-resource.xml";
	private static final String RESOURCE_OPENDJ_OID = "ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";
	
	private static final String FILENAME_USER_JACK = "src/test/resources/repo/user-jack.xml";
	private static final String USER_JACK_OID = "c0c010c0-d34d-b33f-f00d-111111111111";
	
	private static final String FILENAME_REQUEST_USER_MODIFY_ADD_ACCOUNT = "src/test/resources/request/user-modify-add-account.xml";

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
		
	// We need this complicated init as we want to initialize repo only once. JUnit will
	// create new class instance for every test, so @Before and @PostInit will not work
	// directly. We also need to init the repo after spring autowire is done, so @BeforeClass won't work either.
	@Before
	public void initRepository() throws Exception {
		if (!repoInitialized) {
			resource = (ResourceType) addObjectFromFile(FILENAME_RESOURCE_OPENDJ);
			addObjectFromFile(FILENAME_SYSTEM_CONFIGURATION);
			repoInitialized = true;
		}
	}

	private <T> T unmarshallJaxbFromFile(String filePath, Class<T> clazz) throws FileNotFoundException, JAXBException {
		File file = new File(filePath);
		FileInputStream fis = new FileInputStream(file);
		Object object = unmarshaller.unmarshal(fis);
		T objectType = ((JAXBElement<T>) object).getValue();
		return objectType;
	}

	private ObjectType addObjectFromFile(String filePath) throws Exception {
		ObjectType object = unmarshallJaxbFromFile(filePath,ObjectType.class);
		System.out.println("obj: " + object.getName());
		OperationResult result = new OperationResult(TestSanity.class.getName()
				+ ".addObjectFromFile");
		repositoryService.addObject(object, result);
		return object;
	}
	
	private void displayJaxb(Object o, QName qname) throws JAXBException {
		Document doc = DOMUtil.getDocument();
		Element element = JAXBUtil.jaxbToDom(o, qname, doc);
		System.out.println(DOMUtil.serializeDOMToString(element));
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
	
	/**
	 * Test the testResource method. Expect a complete success for now.
	 * 
	 * TODO: better check for the returned result. Look inside and check if
	 * all the expected tests were run.
	 * 
	 * @throws FaultMessage
	 * @throws JAXBException
	 */
	@Test
	public void test001TestConnection() throws FaultMessage, JAXBException {
		// GIVEN
		OperationResultType result = new OperationResultType();
		Holder<OperationResultType> holder = new Holder<OperationResultType>(result);
		
		// WHEN
		model.testResource(RESOURCE_OPENDJ_OID, holder);
		
		// THEN
		displayJaxb(result, new QName("result"));
		
		assertSuccess(result.getPartialResults().get(0));
	}
	
	private void assertSuccess(OperationResultType result) {
		assertEquals(OperationResultStatusType.SUCCESS,result.getStatus());
		List<OperationResultType> partialResults = result.getPartialResults();
		for (OperationResultType subResult : partialResults) {
			assertSuccess(subResult);
		}
	}
	
	/**
	 * Attempt to add new user. It is only added to the repository, so check if it is in the repository after
	 * the operation.
	 */
	@Test
	public void test002AddUser() throws FileNotFoundException, JAXBException, FaultMessage, ObjectNotFoundException, SchemaException {		
		// GIVEN
		UserType user = unmarshallJaxbFromFile(FILENAME_USER_JACK,UserType.class);
		
		OperationResultType result = new OperationResultType();
		Holder<OperationResultType> holder = new Holder<OperationResultType>(result);
		
		// WHEN
		String oid = model.addObject(user, holder);
		
		// THEN
		
		assertEquals(USER_JACK_OID, oid);
		
		OperationResult repoResult = new OperationResult("getObject");
		PropertyReferenceListType resolve = new PropertyReferenceListType();
		ObjectType repoObject = repositoryService.getObject(oid, resolve, repoResult);
		assertEquals(USER_JACK_OID,repoObject.getOid());
		UserType repoUser = (UserType)repoObject;
		assertEquals(user.getFullName(), repoUser.getFullName());
		
		//TODO: better checks
	}
	
	@Test
	public void test003AddAccountToUser() throws FileNotFoundException, JAXBException, FaultMessage, ObjectNotFoundException, SchemaException {
		// GIVEN
		
		ObjectModificationType objectChange = unmarshallJaxbFromFile(FILENAME_REQUEST_USER_MODIFY_ADD_ACCOUNT, ObjectModificationType.class);

		OperationResultType result = new OperationResultType();
		Holder<OperationResultType> holder = new Holder<OperationResultType>(result);
		
		
		// WHEN
		model.modifyObject(objectChange, holder);
		
		// THEN
		
		// Check if user object was modified in the repo
		
		OperationResult repoResult = new OperationResult("getObject");
		PropertyReferenceListType resolve = new PropertyReferenceListType();
		ObjectType repoObject = repositoryService.getObject(USER_JACK_OID, resolve, repoResult);
		UserType repoUser = (UserType)repoObject;
		displayJaxb(repoUser, new QName("user"));
		List<ObjectReferenceType> accountRefs = repoUser.getAccountRef();
		assertEquals(1,accountRefs.size());
		ObjectReferenceType accountRef = accountRefs.get(0);
		String shadowOid = accountRef.getOid();
		assertFalse(shadowOid.isEmpty());
		
		// Check if shadow was created in the repo
		
		repoResult = new OperationResult("getObject");
		repoObject = repositoryService.getObject(shadowOid, resolve, repoResult);
		AccountShadowType repoShadow = (AccountShadowType)repoObject;
		displayJaxb(repoShadow, new QName("shadow"));
		assertNotNull(repoShadow);
		assertEquals(RESOURCE_OPENDJ_OID,repoShadow.getResourceRef().getOid());
		
		// TODO: check attributes in the shadow (should be only identifiers)
		
		// TODO: check if account was created in LDAP
	}
	
	//TODO: delete user (with the account): should also delete the account on OpenDJ
	
	//TODO: test for missing/corrupt system configuration
}
