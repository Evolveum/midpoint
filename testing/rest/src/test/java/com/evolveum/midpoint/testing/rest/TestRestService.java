package com.evolveum.midpoint.testing.rest;


import static org.testng.AssertJUnit.*;
import static com.evolveum.midpoint.test.util.TestUtil.displayTestTile;
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
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sql.SqlRepositoryServiceImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

@ContextConfiguration(locations = { "classpath:ctx-rest-test.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestRestService {
	
	private static final String REPO_DIR_NAME = "src/test/resources/repo/";
	
	public static final String USER_ADMINISTRATOR_FILENAME = REPO_DIR_NAME + "user-administrator.xml";
	
	public static final String RESOURCE_OPENDJ_FILENAME = REPO_DIR_NAME + "reosurce-opendj.xml";
	public static final String RESOURCE_OPENDJ_OID = REPO_DIR_NAME + "ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";
	
	public static final String USER_TEMPLATE_FILENAME = REPO_DIR_NAME + "user-template.xml";
	public static final String USER_TEMPLATE_OID = REPO_DIR_NAME + "c0c010c0-d34d-b33f-f00d-777111111111";
	
	public static final String SYSTEM_CONFIGURATION_FILENAME = REPO_DIR_NAME + "system-configuration.xml";
//	public static final String SYSTEM_CONFIGURATION_OID = REPO_DIR_NAME + "c0c010c0-d34d-b33f-f00d-777111111111";
	
	private static final Trace LOGGER = TraceManager.getTrace(TestRestService.class);
//	private final static String ENDPOINT_ADDRESS = "local://rest";
//	private static Server server;
	
	
	private final static String ENDPOINT_ADDRESS = "http://localhost:8080/rest";
//	private final static String WADL_ADDRESS = ENDPOINT_ADDRESS + "?_wadl";
	private static Server server;
	
	private static RepositoryService repositoryService;
	 
	@BeforeClass
	public static void initialize() throws Exception {
	     startServer();
//	     waitForWADL();
	}
	 
	private static void startServer() throws Exception {
	     ApplicationContext applicationContext = new ClassPathXmlApplicationContext("ctx-rest-test-main.xml");
	        LOGGER.info("Spring context initialized.");

	        JAXRSServerFactoryBean sf = (JAXRSServerFactoryBean) applicationContext.getBean("restService");
	        
	     sf.setAddress(ENDPOINT_ADDRESS);
	 
	     server = sf.create();
	     
	     repositoryService = (SqlRepositoryServiceImpl) applicationContext.getBean("repositoryService");
     
     
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
		
		WebClient client = prepareClient();
		
		client.path("/users/"+ SystemObjectsType.USER_ADMINISTRATOR.value());
		  
		  Response response = client.get();
		  
		  assertEquals("Expected 200 but got " + response.getStatus(), 200, response.getStatus());
		  UserType userType = response.readEntity(UserType.class);
		  assertNotNull("Returned entity in body must not be null.", userType);
		  LOGGER.info("Returned entity: {}", userType.asPrismObject().dump());
	}
	
	@Test
	  public void test101addSystemConfigurationOverwrite() throws Exception{
		displayTestTile(this, "test101addSystemConfigurationOverwrite");
		
		WebClient client = prepareClient();
		  client.path("/systemConfigurations");
		  
		
		  
		  client.query("options", "override");
		 
		  LOGGER.info("post starting");
		  Response response = client.post(new File(SYSTEM_CONFIGURATION_FILENAME));
		  LOGGER.info("post end");
		  LOGGER.info("response : {} ", response.getStatus());
		  LOGGER.info("response : {} ", response.getStatusInfo().getReasonPhrase());
		  
		  assertEquals("Expected 200 but got " + response.getStatus(), 201, response.getStatus());
		  String location = response.getHeaderString("Location");
		  assertEquals(ENDPOINT_ADDRESS + "/systemConfigurations/"+SystemObjectsType.SYSTEM_CONFIGURATION.value(), location);
		  
		
	  }


	
  @Test
  public void test102addUserTemplate() throws Exception{
	  displayTestTile(this, "test102addUserTemplate");
	  
	  WebClient client = prepareClient();
	  
	  client.path("/objectTemplates");
	 
	  LOGGER.info("post starting");
	  Response response = client.post(new File(USER_TEMPLATE_FILENAME));
	  LOGGER.info("post end");
	  LOGGER.info("response : {} ", response.getStatus());
	  LOGGER.info("response : {} ", response.getStatusInfo().getReasonPhrase());
	  
	  assertEquals("Expected 200 but got " + response.getStatus(), 201, response.getStatus());
	
	
  }
  
	private WebClient prepareClient() {
		WebClient client = WebClient.create(ENDPOINT_ADDRESS);

		ClientConfiguration clientConfig = WebClient.getConfig(client);

		clientConfig.getRequestContext().put(LocalConduit.DIRECT_DISPATCH,
				Boolean.TRUE);

		client.accept("application/xml");

		String authorizationHeader = "Basic "
				+ org.apache.cxf.common.util.Base64Utility
						.encode("administrator:5ecr3t".getBytes());
		client.header("Authorization", authorizationHeader);

		return client;

  }
}
