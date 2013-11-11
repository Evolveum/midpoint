package com.evolveum.midpoint.testing.rest;

import java.io.File;

import javax.ws.rs.core.Response;

import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.http.HTTPConduit;
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
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.repo.sql.SqlRepositoryServiceImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
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
	 
	@BeforeClass
	public static void initialize() throws Exception {
	     startServer();
//	     waitForWADL();
	}
	 
	private static void startServer() throws Exception {
//	     JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
	     ApplicationContext applicationContext = new ClassPathXmlApplicationContext("ctx-rest-test-main.xml");
	        LOGGER.info("Spring context initialized.");

//	        ModelRestService restService = (ModelRestService) applicationContext.getBean("modelRestService");

	        JAXRSServerFactoryBean sf = (JAXRSServerFactoryBean) applicationContext.getBean("restService");
	        
//	     sf.setResourceClasses(MyJaxrsResource.class);
//	         
//	     List<Object> providers = new ArrayList<Object>();
//	     // add custom providers if any
//	     sf.setProviders(providers);
//	         
//	     sf.setResourceProvider(MyJaxrsResource.class.class,
//	                            new SingletonResourceProvider(new MyJaxrsResource(), true));
	     sf.setAddress(ENDPOINT_ADDRESS);
	 
	     server = sf.create();
	     
	     SqlRepositoryServiceImpl repositoryService = (SqlRepositoryServiceImpl) applicationContext.getBean("repositoryService");
     
     
     PrismContext prismContext = (PrismContext) applicationContext.getBean("prismContext");
     PrismObject<UserType> admin = prismContext.getPrismDomProcessor().parseObject(new File(USER_ADMINISTRATOR_FILENAME));
     OperationResult parentResult = new OperationResult("add");
     repositoryService.addObject(admin, RepoAddOptions.createAllowUnencryptedValues(), parentResult);
     
     PrismObject<UserType> sysConfig = prismContext.getPrismDomProcessor().parseObject(new File(SYSTEM_CONFIGURATION_FILENAME));
     repositoryService.addObject(sysConfig, RepoAddOptions.createAllowUnencryptedValues(), parentResult);
	}
	 
	// Optional step - may be needed to ensure that by the time individual
	// tests start running the endpoint has been fully initialized
//	private static void waitForWADL() throws Exception {
//	    WebClient client = WebClient.create(WADL_ADDRESS);
//	    // wait for 20 secs or so
//	    for (int i = 0; i < 20; i++) {
//	        Thread.currentThread().sleep(1000);
//	        Response response = client.get();
//	        if (response.getStatus() == 200) {
//	            break;
//	        }
//	    }
	    // no WADL is available yet - throw an exception or give tests a chance to run anyway
//	}
	 
	 
	@AfterClass
	public static void destroy() throws Exception {
	   server.stop();
	   server.destroy();
	}
	
	
//	@Autowired
//	ModelRestService restService;
//	
//	public ModelRestService getRestService() {
//		return restService;
//	}
//	
	public TestRestService() {
		super();
	}
//	
////	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
////		LOGGER.trace("initSystem");
////		super.initSystem(initTask, initResult);
////		
//////		repoAddObjectFromFile(USER_ADMINISTRATOR_FILENAME, UserType.class, initResult);
////
////		// This should discover the connectors
////		LOGGER.trace("initSystem: trying modelService.postInit()");
////		modelService.postInit(initResult);
////		LOGGER.trace("initSystem: modelService.postInit() done");
////
////		// We need to add config after calling postInit() so it will not be
////		// applied.
////		// we want original logging configuration from the test logback config
////		// file, not
////		// the one from the system config.
////		repoAddObjectFromFile(SYSTEM_CONFIGURATION_FILENAME, SystemConfigurationType.class, initResult);
////
////		// Add broken connector before importing resources
////		// addObjectFromFile(CONNECTOR_BROKEN_FILENAME, initResult);
////
////		// Need to import instead of add, so the (dynamic) connector reference
////		// will be resolved
////		// correctly
////		importObjectFromFile(RESOURCE_OPENDJ_FILENAME, initResult);
////		// importObjectFromFile(RESOURCE_DERBY_FILENAME, initResult);
////		// importObjectFromFile(RESOURCE_BROKEN_FILENAME, initResult);
////
////		repoAddObjectFromFile(SAMPLE_CONFIGURATION_OBJECT_FILENAME, GenericObjectType.class, initResult);
////		repoAddObjectFromFile(USER_TEMPLATE_FILENAME, ObjectTemplateType.class, initResult);
////		// addObjectFromFile(ROLE_SAILOR_FILENAME, initResult);
////		// addObjectFromFile(ROLE_PIRATE_FILENAME, initResult);
////		repoAddObjectFromFile(ROLE_CAPTAIN_FILENAME, RoleType.class, initResult);
////		
////		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
////		startServer();
////	}
//	 
//	@BeforeClass
//	public static void initialize() throws Exception {
//	     startServer();
//	}
//	 
//	private static void startServer() throws Exception {
////	     JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
////	     sf.setResourceClasses(ModelRestService.class);
////	         
////	     List<Object> providers = new ArrayList<Object>();
////	     // add custom providers if any
////	     
////	     sf.setProviders(providers);
////	     
//	     LOGGER.info("Start to initialize model context - rest service");
//	     
//	     
//	     
////	     <import resource="classpath*:ctx-task.xml"/>
////	     <import resource="ctx-model.xml"/>
////	     <import resource="ctx-provisioning.xml"/>
////	     <import resource="ctx-rest-test.xml"/>
////	     <import resource="ctx-audit.xml"/>
////	     <import resource="ctx-common.xml"/>
////	     <import resource="classpath*:ctx-repository.xml"/>
////	     <import resource="ctx-repo-cache.xml"/>
////	     <import resource="ctx-configuration-test.xml"/>
////	     
////	     ApplicationContext applicationContext = new ClassPathXmlApplicationContext("ctx-audit.xml", "ctx-common.xml", "ctx-repo-cache.xml", "classpath*:ctx-repository.xml", "ctx-task.xml", "ctx-provisioning.xml", "/ctx-model.xml");
//	        
//	     ApplicationContext applicationContext = new ClassPathXmlApplicationContext("ctx-rest-test-main.xml");
//	        LOGGER.info("Spring context initialized.");
//
////	        ModelRestService restService = (ModelRestService) applicationContext.getBean("modelRestService");
//
//	        JAXRSServerFactoryBean sf = (JAXRSServerFactoryBean) applicationContext.getBean("restService");
//	        
////	        SqlRepositoryServiceImpl repositoryService = (SqlRepositoryServiceImpl) applicationContext.getBean("repositoryService");
////	        
////	        
////	        PrismContext prismContext = (PrismContext) applicationContext.getBean("prismContext");
////	        PrismObject<UserType> admin = prismContext.getPrismDomProcessor().parseObject(new File(USER_ADMINISTRATOR_FILENAME));
////	        OperationResult parentResult = new OperationResult("add");
////	        repositoryService.addObject(admin, null, parentResult);
////	        sf.setAddress(ENDPOINT_ADDRESS);
////	        PrismContext prismContext = (PrismContext) applicationContext.getBean("prismContext");
////	        PrismTestUtil.createInitializedPrismContext();
//         
////	     sf.setResourceProvider(ModelRestService.class,
////	                            new SingletonResourceProvider(restService, true));
//	     sf.setAddress(ENDPOINT_ADDRESS);
//	 
//	     server = sf.create();
//	}
//	 
//	@AfterClass
//	public static void destroy() throws Exception {
//	   server.stop();
//	   server.destroy();
//	}
	 
	
	@Test
	  public void test001addSystemCOnfiguration() throws Exception{
		  WebClient client = WebClient.create(ENDPOINT_ADDRESS);
		  
		  ClientConfiguration clientConfig = WebClient.getConfig(client);
		  
//		  
//		  
//		  clientConfig.getRequestContext().put("Authorization", authorizationHeader);
		  clientConfig.getRequestContext().put(LocalConduit.DIRECT_DISPATCH, Boolean.TRUE);
//		  AuthorizationPolicy authPolicy = new AuthorizationPolicy();
//		  authPolicy.setUserName("administrator");
//		  authPolicy.setPassword("secret");
		  
		  client.accept("application/xml");
		  client.path("/users");
		  
		  String authorizationHeader = "Basic "
			      + org.apache.cxf.common.util.Base64Utility.encode("administrator:5ecr3t".getBytes());
		  client.header("Authorization", authorizationHeader);
		  
		  client.query("options", "override");
		 
		  LOGGER.info("post starting");
		  Response response = client.post(new File(SYSTEM_CONFIGURATION_FILENAME));
		  LOGGER.info("post end");
//		  Response response = client.get();
		  LOGGER.info("response : {} ", response.getStatus());
		  LOGGER.info("response : {} ", response.getStatusInfo().getReasonPhrase());
		  
		  AssertJUnit.assertEquals("Expected 200 but got " + response.getStatus(), 201, response.getStatus());
		
	  }


	
//  @Test
  public void test002addUserAdministrator() throws Exception{
	  WebClient client = WebClient.create(ENDPOINT_ADDRESS);
	  
	  ClientConfiguration clientConfig = WebClient.getConfig(client);
	  
//	  
//	  
//	  clientConfig.getRequestContext().put("Authorization", authorizationHeader);
	  clientConfig.getRequestContext().put(LocalConduit.DIRECT_DISPATCH, Boolean.TRUE);
//	  AuthorizationPolicy authPolicy = new AuthorizationPolicy();
//	  authPolicy.setUserName("administrator");
//	  authPolicy.setPassword("secret");
	  
	  client.accept("application/xml");
	  client.path("/users");
	  
	  String authorizationHeader = "Basic "
		      + org.apache.cxf.common.util.Base64Utility.encode("administrator:5ecr3t".getBytes());
	  client.header("Authorization", authorizationHeader);
	  
	 
	  LOGGER.info("post starting");
	  Response response = client.post(new File(USER_ADMINISTRATOR_FILENAME));
	  LOGGER.info("post end");
//	  Response response = client.get();
	  LOGGER.info("response : {} ", response.getStatus());
	  LOGGER.info("response : {} ", response.getStatusInfo().getReasonPhrase());
	  
	  AssertJUnit.assertEquals("Expected 200 but got " + response.getStatus(), 201, response.getStatus());
	
	
  }
}
