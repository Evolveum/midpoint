package com.evolveum.midpoint.model.importer;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.ws.Holder;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.model.ModelWebService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.test.ldap.OpenDJUnitTestAdapter;
import com.evolveum.midpoint.test.ldap.OpenDJUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;
import com.evolveum.midpoint.xml.schema.SchemaConstants;

@Ignore
//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(locations = { "classpath:application-context-provisioning.xml",
//		"classpath:application-context-provisioning-test.xml",
//		"classpath:application-context-model.xml"})
public class ImportFromResourceIntegrationTest extends OpenDJUnitTestAdapter {
	
	private static final String FILENAME_RESOURCE_OPENDJ = "src/test/resources/importer/opendj-resource.xml";
	private static final String FILENAME_RESOURCE_OPENDJ_OID = "ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";

	protected static OpenDJUtil djUtil = new OpenDJUtil();
	private JAXBContext jaxbctx;
	private Unmarshaller unmarshaller;
	
	@Autowired(required = true)
	private RepositoryService repositoryService;
	@Autowired(required = true)
	private ModelWebService model;
	
	public ImportFromResourceIntegrationTest() throws JAXBException {
//		jaxbctx = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
//		unmarshaller = jaxbctx.createUnmarshaller();
	}
	
	@BeforeClass
    public static void startLdap() throws Exception{
//        startACleanDJ();
    }
    
    @AfterClass
    public static void stopLdap() throws Exception{
//        stopDJ();
    }
	
	@Before
	public void setUp() throws Exception {
//		addObjectFromFile(FILENAME_RESOURCE_OPENDJ);
	}

	@After
	public void tearDown() throws Exception {
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
		OperationResult result = new OperationResult(ImportFromResourceIntegrationTest.class.getName()
				+ ".addObjectFromFile");
		repositoryService.addObject(object, result);
		return object;
	}

	@Test
	public void testLaunchImportFromResource() throws FaultMessage {
		
		Holder<OperationResultType> resultHolder = new Holder<OperationResultType>(new OperationResultType());
		model.launchImportFromResource(FILENAME_RESOURCE_OPENDJ_OID, SchemaConstants.ICFS_ACCOUNT, resultHolder);
		
		// TODO: check results
	}

}
