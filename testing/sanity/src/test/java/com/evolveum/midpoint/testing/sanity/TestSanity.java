package com.evolveum.midpoint.testing.sanity;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.test.ldap.OpenDJUnitTestAdapter;
import com.evolveum.midpoint.test.ldap.OpenDJUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
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
@ContextConfiguration(locations = { "classpath:application-context-provisioning.xml",
		"classpath:application-context-provisioning-test.xml",
		"classpath:application-context-model.xml"})
public class TestSanity extends OpenDJUnitTestAdapter {

	private static final String FILENAME_RESOURCE_OPENDJ = "src/test/resources/ucf/opendj-resource.xml";
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
	private ModelService model;
	
	@Autowired(required = true)
	private RepositoryService repositoryService;

	public TestSanity() throws JAXBException {
		djUtil = new OpenDJUtil();
		jaxbctx = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		unmarshaller = jaxbctx.createUnmarshaller();
	}

	/**
	 * Initialize embedded OpenDJ instace
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

		resource = (ResourceType) addObjectFromFile(FILENAME_RESOURCE_OPENDJ);
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
	
	@Test
	void testFoo() {
		//TODO
	}
}
