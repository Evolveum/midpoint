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
	
	private static final Trace LOGGER = TraceManager.getTrace(TestRestService.class);
	
	private final static String ENDPOINT_ADDRESS = "http://localhost:8080/rest";
	private static Server server;
	 
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

		SqlRepositoryServiceImpl repositoryService = (SqlRepositoryServiceImpl) applicationContext
				.getBean("repositoryService");

		PrismContext prismContext = (PrismContext) applicationContext.getBean("prismContext");
		PrismObject<UserType> admin = prismContext.getPrismDomProcessor().parseObject(
				new File(USER_ADMINISTRATOR_FILENAME));
		OperationResult parentResult = new OperationResult("add");
		repositoryService.addObject(admin, RepoAddOptions.createAllowUnencryptedValues(), parentResult);

		PrismObject<UserType> sysConfig = prismContext.getPrismDomProcessor().parseObject(
				new File(SYSTEM_CONFIGURATION_FILENAME));
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
	  public void test001addSystemCOnfiguration() throws Exception{
		  WebClient client = WebClient.create(ENDPOINT_ADDRESS);
		  
		  ClientConfiguration clientConfig = WebClient.getConfig(client);
		  clientConfig.getRequestContext().put(LocalConduit.DIRECT_DISPATCH, Boolean.TRUE);
		  
		  client.accept("application/xml");
		  client.path("/users");
		  
		  String authorizationHeader = "Basic "
			      + org.apache.cxf.common.util.Base64Utility.encode("administrator:5ecr3t".getBytes());
		  client.header("Authorization", authorizationHeader);
		  
		 
		  LOGGER.info("post starting");
		  Response response = client.post(new File(SYSTEM_CONFIGURATION_FILENAME));
		  LOGGER.info("post end");
		  LOGGER.info("response : {} ", response.getStatus());
		  LOGGER.info("response : {} ", response.getStatusInfo().getReasonPhrase());
		  
		  AssertJUnit.assertEquals("Ex[ected 200 but got " + response.getStatus(), 201, response.getStatus());
		
	  }


	
  @Test
  public void test002addUserAdministrator() throws Exception{
	  WebClient client = WebClient.create(ENDPOINT_ADDRESS);
	  
	  ClientConfiguration clientConfig = WebClient.getConfig(client);
	  clientConfig.getRequestContext().put(LocalConduit.DIRECT_DISPATCH, Boolean.TRUE);
	  client.accept("application/xml");
	  client.path("/users");
	  
	  String authorizationHeader = "Basic "
		      + org.apache.cxf.common.util.Base64Utility.encode("administrator:5ecr3t".getBytes());
	  client.header("Authorization", authorizationHeader);
	  
	 
	  LOGGER.info("post starting");
	  Response response = client.post(new File(USER_ADMINISTRATOR_FILENAME));
	  LOGGER.info("post end");
	  LOGGER.info("response : {} ", response.getStatus());
	  LOGGER.info("response : {} ", response.getStatusInfo().getReasonPhrase());
	  
	  AssertJUnit.assertEquals("Ex[ected 200 but got " + response.getStatus(), 201, response.getStatus());
  }
}
