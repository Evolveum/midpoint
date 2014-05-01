package com.evolveum.midpoint.testing.rest;


import static com.evolveum.midpoint.test.util.TestUtil.displayTestTile;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.fail;

import java.io.File;

import javax.ws.rs.core.Response;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.local.LocalConduit;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.impl.ProvisioningServiceImpl;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sql.SqlRepositoryServiceImpl;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@ContextConfiguration(locations = { "classpath:ctx-rest-test.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestRestService {
	
	private static final String REPO_DIR_NAME = "src/test/resources/repo/";
	
	public static final String USER_ADMINISTRATOR_FILENAME = REPO_DIR_NAME + "user-administrator.xml";
	
	public static final String RESOURCE_OPENDJ_FILENAME = REPO_DIR_NAME + "reosurce-opendj.xml";
	public static final String RESOURCE_OPENDJ_OID = REPO_DIR_NAME + "ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";
	
	public static final String USER_TEMPLATE_FILENAME = REPO_DIR_NAME + "user-template.xml";
	public static final String USER_TEMPLATE_OID = REPO_DIR_NAME + "c0c010c0-d34d-b33f-f00d-777111111111";
	
	public static final String ACCOUT_CHUCK_FILENAME = REPO_DIR_NAME + "account-chuck.xml";
	public static final String ACCOUT_CHUCK_OID = REPO_DIR_NAME + "a0c010c0-d34d-b33f-f00d-111111111666";
	
	public static final String SYSTEM_CONFIGURATION_FILENAME = REPO_DIR_NAME + "system-configuration.xml";
	
	private static final Trace LOGGER = TraceManager.getTrace(TestRestService.class);
	
	private final static String ENDPOINT_ADDRESS = "http://localhost:8080/rest";
	private static Server server;
	
	private static RepositoryService repositoryService;
	private static ProvisioningService provisioning;
	 
	@BeforeClass
	public static void initialize() throws Exception {
	     startServer();
	}
	 
	private static void startServer() throws Exception {
	     ApplicationContext applicationContext = new ClassPathXmlApplicationContext("ctx-rest-test-main.xml");
	     LOGGER.info("Spring context initialized.");

	     JAXRSServerFactoryBean sf = (JAXRSServerFactoryBean) applicationContext.getBean("restService");
	        
	     sf.setAddress(ENDPOINT_ADDRESS);
	 
	     server = sf.create();
	     
	     repositoryService = (SqlRepositoryServiceImpl) applicationContext.getBean("repositoryService");
         provisioning = (ProvisioningServiceImpl) applicationContext.getBean("provisioningService");
        
         PrismContext prismContext = (PrismContext) applicationContext.getBean("prismContext");
         PrismObject<UserType> admin = prismContext.getPrismDomProcessor().parseObject(new File(USER_ADMINISTRATOR_FILENAME));
         OperationResult parentResult = new OperationResult("add");
         repositoryService.addObject(admin, RepoAddOptions.createAllowUnencryptedValues(), parentResult);
     
         PrismObject<UserType> sysConfig = prismContext.getPrismDomProcessor().parseObject(new File(SYSTEM_CONFIGURATION_FILENAME));
         repositoryService.addObject(sysConfig, RepoAddOptions.createAllowUnencryptedValues(), parentResult);
	}
	 
	 
	 
	@AfterClass
	public static void destroy() throws Exception {
	   server.stop();
	   server.destroy();
	}


	public TestRestService() {
		super();
	}

	
	@Test
	public void test001getUserAdministrator(){
		displayTestTile(this, "test001getUserAdministrator");
		
		WebClient client = prepareClient(true);
		
		client.path("/users/"+ SystemObjectsType.USER_ADMINISTRATOR.value());
		  
		  Response response = client.get();
		  
		  assertEquals("Expected 200 but got " + response.getStatus(), 200, response.getStatus());
		  UserType userType = response.readEntity(UserType.class);
		  assertNotNull("Returned entity in body must not be null.", userType);
		  LOGGER.info("Returned entity: {}", userType.asPrismObject().dump());
	}
	
	@Test
	public void test002getNonExistingUser(){
		displayTestTile(this, "test002getNonExistingUser");
		
		WebClient client = prepareClient(true);
		
		client.path("/users/12345");
	
	@Test
	public void test002getNonExistingUser(){
		displayTestTile(this, "test002getNonExistingUser");
		
		WebClient client = prepareClient(true);
		
		client.path("/users/12345");
		  
		  Response response = client.get();
		  
		  assertEquals("Expected 404 but got " + response.getStatus(), 404, response.getStatus());
		  
	}
	
	@Test
	public void test003getNonAuthneticated(){
		displayTestTile(this, "test003getNonAuthneticated");
		
		WebClient client = prepareClient(false);
		
		client.path("/users/12345");
		  
		  Response response = client.get();
		  
		  assertEquals("Expected 401 but got " + response.getStatus(), 401, response.getStatus());
		  
	}
	
	@Test
	  public void test401addSystemConfigurationOverwrite() throws Exception{
		displayTestTile(this, "test101addSystemConfigurationOverwrite");
		
		WebClient client = prepareClient(true);
		  client.path("/systemConfigurations");
		  
		
		  
		  client.query("options", "override");
		 
		  LOGGER.info("post starting");
		  Response response = client.post(new File(SYSTEM_CONFIGURATION_FILENAME));
		  LOGGER.info("post end");
//		  Response response = client.get();
		  LOGGER.info("response : {} ", response.getStatus());
		  LOGGER.info("response : {} ", response.getStatusInfo().getReasonPhrase());
		  
		  assertEquals("Expected 200 but got " + response.getStatus(), 201, response.getStatus());
		  String location = response.getHeaderString("Location");
		  assertEquals(ENDPOINT_ADDRESS + "/systemConfigurations/"+SystemObjectsType.SYSTEM_CONFIGURATION.value(), location);
		  
		
	  }


	
  @Test
  public void test102addUserTemplate() throws Exception{
	  displayTestTile(this, "test102addUserTemplate");
	  
	  WebClient client = prepareClient(true);
	  
	  client.path("/objectTemplates");
	 
	  LOGGER.info("post starting");
	  Response response = client.post(new File(USER_TEMPLATE_FILENAME));
	  LOGGER.info("post end");
	  LOGGER.info("response : {} ", response.getStatus());
	  LOGGER.info("response : {} ", response.getStatusInfo().getReasonPhrase());
	  
	  assertEquals("Expected 201 but got " + response.getStatus(), 201, response.getStatus());
	
	
  }
  
  @Test
  public void test103addUserBadTargetCollection() throws Exception{
	  displayTestTile(this, "test103addUserBadTargetCollection");
	  
	  WebClient client = prepareClient(true);
	  
	  client.path("/objectTemplates");
	 
	  LOGGER.info("post starting");
	  Response response = client.post(new File(USER_ADMINISTRATOR_FILENAME));
	  LOGGER.info("post end");
	  LOGGER.info("response : {} ", response.getStatus());
	  LOGGER.info("response : {} ", response.getStatusInfo().getReasonPhrase());
	  
	  assertEquals("Expected 400 but got " + response.getStatus(), 400, response.getStatus());
	
	
  }
  
  @Test
  public void test104addAccountRaw() throws Exception{
	  displayTestTile(this, "test104addAccountRaw");
	  
	  WebClient client = prepareClient(true);
	  
	  client.path("/shadows");
	  client.query("options", "raw");
	 
	  LOGGER.info("post starting");
	  Response response = client.post(new File(ACCOUT_CHUCK_FILENAME));
	  LOGGER.info("post end");
	  LOGGER.info("response : {} ", response.getStatus());
	  LOGGER.info("response : {} ", response.getStatusInfo().getReasonPhrase());
	  
	  assertEquals("Expected 201 but got " + response.getStatus(), 201, response.getStatus());
	
	  OperationResult parentResult = new OperationResult("get");
	  try{
	  provisioning.getObject(ShadowType.class, ACCOUT_CHUCK_OID, SelectorOptions.createCollection(GetOperationOptions.createDoNotDiscovery()), null, parentResult);
	  fail("expected object not found exception but haven't got one.");
	  } catch (ObjectNotFoundException ex){
		  //this is OK..we expect objet not found, because accout was added with the raw options which indicates, that it was created only in the repository
	  }
	
  }
  
	private WebClient prepareClient(boolean authenticate) {
		WebClient client = WebClient.create(ENDPOINT_ADDRESS);

		ClientConfiguration clientConfig = WebClient.getConfig(client);

		clientConfig.getRequestContext().put(LocalConduit.DIRECT_DISPATCH,
				Boolean.TRUE);

		client.accept("application/xml");

		if (authenticate) {
			String authorizationHeader = "Basic "
					+ org.apache.cxf.common.util.Base64Utility
							.encode("administrator:5ecr3t".getBytes());
			client.header("Authorization", authorizationHeader);
		}
		return client;
	
  }
}
