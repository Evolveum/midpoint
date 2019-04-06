/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web;

import static com.evolveum.midpoint.web.AdminGuiTestConstants.USER_JACK_FAMILY_NAME;
import static com.evolveum.midpoint.web.AdminGuiTestConstants.USER_JACK_FULL_NAME;
import static com.evolveum.midpoint.web.AdminGuiTestConstants.USER_JACK_GIVEN_NAME;
import static com.evolveum.midpoint.web.AdminGuiTestConstants.USER_JACK_OID;
import static com.evolveum.midpoint.web.AdminGuiTestConstants.USER_JACK_USERNAME;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Locale;

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;
import javax.xml.namespace.QName;

import org.apache.wicket.Application;
import org.apache.wicket.ThreadContext;
import org.apache.wicket.protocol.http.WicketFilter;
import org.apache.wicket.util.tester.WicketTester;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.WebApplicationContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.authentication.CompiledUserProfile;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.DescriptorLoader;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author lazyman
 * @author semancik
 */
public abstract class AbstractGuiIntegrationTest extends AbstractModelIntegrationTest {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractGuiIntegrationTest.class);

    public static final File FOLDER_BASIC = new File("./src/test/resources/basic");
    
    public static final String NS_PIRACY = "http://midpoint.evolveum.com/xml/ns/samples/piracy";
    public static final ItemName PIRACY_SHIP = new ItemName(NS_PIRACY, "ship");
    public static final ItemName PIRACY_WEAPON = new ItemName(NS_PIRACY, "weapon");
    public static final ItemName PIRACY_COLORS = new ItemName(NS_PIRACY, "colors");
    public static final ItemName PIRACY_SECRET = new ItemName(NS_PIRACY, "secret");
    public static final ItemName PIRACY_RANT = new ItemName(NS_PIRACY, "rant");
    public static final ItemName PIRACY_TRANSFORM_DESCRIPTION = new ItemName(NS_PIRACY, "transformDescription");
    public static final ItemName PIRACY_TRANSFORMATION_ENABLED = new ItemName(NS_PIRACY, "transformationEnabled");
    public static final ItemName PIRACY_TRANSFORM = new ItemName(NS_PIRACY, "transform");
    public static final ItemName PIRACY_PATTERN = new ItemName(NS_PIRACY, "pattern");
    public static final ItemName PIRACY_REPLACEMENT = new ItemName(NS_PIRACY, "replacement");
    
    protected static final File ORG_MONKEY_ISLAND_FILE = new File(COMMON_DIR, "org-monkey-island.xml");
	protected static final String ORG_GOVERNOR_OFFICE_OID = "00000000-8888-6666-0000-100000000001";
	protected static final String ORG_SCUMM_BAR_OID = "00000000-8888-6666-0000-100000000006";
	protected static final String ORG_SCUMM_BAR_NAME = "F0006";
	protected static final String ORG_SCUMM_BAR_DISPLAY_NAME = "Scumm Bar";
	protected static final String ORG_MINISTRY_OF_OFFENSE_OID = "00000000-8888-6666-0000-100000000003";
    protected static final String ORG_MINISTRY_OF_DEFENSE_OID = "00000000-8888-6666-0000-100000000002";
	protected static final String ORG_MINISTRY_OF_RUM_OID = "00000000-8888-6666-0000-100000000004";
	protected static final String ORG_MINISTRY_OF_RUM_NAME = "F0004";
	protected static final String ORG_SWASHBUCKLER_SECTION_OID = "00000000-8888-6666-0000-100000000005";
	protected static final String ORG_PROJECT_ROOT_OID = "00000000-8888-6666-0000-200000000000";
	protected static final String ORG_SAVE_ELAINE_OID = "00000000-8888-6666-0000-200000000001";
	protected static final String ORG_KIDNAP_AND_MARRY_ELAINE_OID = "00000000-8888-6666-0000-200000000002";
	
	public static final File USER_ADMINISTRATOR_FILE = new File(COMMON_DIR, "user-administrator.xml");
	
	public static final File SYSTEM_CONFIGURATION_FILE = new File(COMMON_DIR, "system-configuration.xml");
	public static final String SYSTEM_CONFIGURATION_OID = SystemObjectsType.SYSTEM_CONFIGURATION.value();
	
	public static final File ROLE_SUPERUSER_FILE = new File(COMMON_DIR, "role-superuser.xml");
	protected static final String ROLE_SUPERUSER_OID = "00000000-0000-0000-0000-000000000004";
	
	public static final File ROLE_ENDUSER_FILE = new File(COMMON_DIR, "role-enduser.xml");
	protected static final String ROLE_ENDUSER_OID = "00000000-0000-0000-0000-000000000008";
	
	public static final File VALUE_POLICY_FILE = new File(COMMON_DIR, "value-policy.xml");
	protected static final String VALUE_POLICY_OID = "00000000-0000-0000-0000-000000000003";
	
	public static final File SECURITY_POLICY_FILE = new File(COMMON_DIR, "value-policy.xml");
	protected static final String SECURITY_POLICY_OID = "00000000-0000-0000-0000-000000000120";

    @Autowired private MidPointApplication application;
    
//    @Autowired private ApplicationContext appContext;
    @Autowired protected PrismContext prismContext;
    @Autowired protected ExpressionFactory expressionFactory;
    @Autowired protected RelationRegistry relationRegistry;
    @Autowired protected GuiComponentRegistry registry;
    
    protected WicketTester tester;
    
    private PrismObject<UserType> userAdministrator = null;

    @Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
    	LOGGER.info("before super init");
		super.initSystem(initTask, initResult);
		
		LOGGER.info("after super init");
	    WebComponentUtil.setStaticallyProvidedRelationRegistry(relationRegistry);
	    
	    PrismObject<SystemConfigurationType> configuration = repoAddObjectFromFile(SYSTEM_CONFIGURATION_FILE, initResult);
	    relationRegistry.applyRelationsConfiguration(configuration.asObjectable());
	    
//	    repoAddObjectFromFile(VALUE_POLICY_FILE, initResult);
	    repoAddObjectFromFile(SECURITY_POLICY_FILE, initResult);
	    repoAddObjectFromFile(ROLE_ENDUSER_FILE, initResult);
	    repoAddObjectFromFile(ROLE_SUPERUSER_FILE, initResult);
	    repoAddObjectFromFile(USER_ADMINISTRATOR_FILE, initResult);
	    
//	    repoAddObjectFromFile(SYSTEM_CONFIGURATION_FILE, initResult);
	    
	  	login(USER_ADMINISTRATOR_USERNAME);
	  	LOGGER.info("user logged in");
	  	
	  	tester = new WicketTester(application);
	}
    
//    @PostConstruct
//    public void setupApplication() throws ServletException {
//    	
//    	display("PostContruct");
//    	for (String key: Application.getApplicationKeys()) {
//    		display("App "+key, Application.get(key));
//    	}
//    	initializeMidPointApplication();
//    	tester = new WicketTester(application);
//    }
    
    private void initializeMidPointApplication() throws ServletException {
//    	WicketFilter wicketFilter = new WicketFilter(application);
//    	MockServletContext servletContext = new MockServletContext();
//    	WebApplicationContext wac = new MockWebApplicationContext(appContext, servletContext);
//    	servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
//    	MockFilterConfig filterConfig = new MockFilterConfig(servletContext, "midpoint");
//		wicketFilter.init(filterConfig);
//		application.setWicketFilter(wicketFilter);
//		application.setServletContext(servletContext);
//    	new DescriptorLoader().loadData(application);
//    	ThreadContext.setApplication(application);
//    	application.initApplication();
//    	ThreadContext.setApplication(null);
	}
    
    @BeforeMethod
    public void beforeMethodApplication() {
//    	ThreadContext.setApplication(application);
    	
    }
    
    @AfterMethod
    public void afterMethodApplication() {
//    	ThreadContext.setApplication(null);
//    	application.internalDestroy();
    }

	@BeforeClass
    public void beforeClass() throws Exception {
        System.out.println("\n>>>>>>>>>>>>>>>>>>>>>>>> START " + getClass().getName() + "<<<<<<<<<<<<<<<<<<<<<<<<");
        LOGGER.info("\n>>>>>>>>>>>>>>>>>>>>>>>> START {} <<<<<<<<<<<<<<<<<<<<<<<<", new Object[]{getClass().getName()});
    }

    @AfterClass
    public void afterClass() {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> FINISH " + getClass().getName() + "<<<<<<<<<<<<<<<<<<<<<<<<");
        LOGGER.info(">>>>>>>>>>>>>>>>>>>>>>>> FINISH {} <<<<<<<<<<<<<<<<<<<<<<<<\n", new Object[]{getClass().getName()});
    }

    @BeforeMethod
    public void beforeMethod(Method method) {
        System.out.println("\n>>>>>>>>>>>>>>>>>>>>>>>> START TEST" + getClass().getName() + "." + method.getName() + "<<<<<<<<<<<<<<<<<<<<<<<<");
        LOGGER.info("\n>>>>>>>>>>>>>>>>>>>>>>>> START {}.{} <<<<<<<<<<<<<<<<<<<<<<<<", new Object[]{getClass().getName(), method.getName()});
    }

    @AfterMethod
    public void afterMethod(Method method) {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> END TEST" + getClass().getName() + "." + method.getName() + "<<<<<<<<<<<<<<<<<<<<<<<<");
        LOGGER.info(">>>>>>>>>>>>>>>>>>>>>>>> END {}.{} <<<<<<<<<<<<<<<<<<<<<<<<", new Object[]{getClass().getName(), method.getName()});
    }

	protected ModelServiceLocator getServiceLocator(final Task pageTask) {
		return new ModelServiceLocator() {

			@Override
			public ModelService getModelService() {
				return modelService;
			}

			@Override
			public ModelInteractionService getModelInteractionService() {
				return modelInteractionService;
			}

			@Override
			public Task createSimpleTask(String operationName) {
				return taskManager.createTaskInstance(operationName);
			}

			@Override
			public PrismContext getPrismContext() {
				return prismContext;
			}

			@Override
			public SecurityEnforcer getSecurityEnforcer() {
				return securityEnforcer;
			}
			
			@Override
			public SecurityContextManager getSecurityContextManager() {
				return securityContextManager;
			}

			@NotNull
			@Override
			public CompiledUserProfile getCompiledUserProfile() {
				Task task = createSimpleTask("getCompiledUserProfile");
				try {
					return getModelInteractionService().getCompiledUserProfile(task, task.getResult());
				} catch (ObjectNotFoundException | SchemaException | CommunicationException | ConfigurationException | SecurityViolationException | ExpressionEvaluationException e) {
					throw new SystemException(e.getMessage(), e);
				}
			}

			@Override
			public Task getPageTask() {
				return pageTask;
			}

			@Override
			public ExpressionFactory getExpressionFactory() {
				return expressionFactory;
			}

			@Override
			public LocalizationService getLocalizationService() {
				return localizationService;
			}

			@Override
			public Locale getLocale() {
				return Locale.US;
			}

			@Override
			public GuiComponentRegistry getRegistry() {
				return registry;
			}

		};
	}


    protected void assertUserJack(PrismObject<UserType> user) {
		assertUserJack(user, USER_JACK_FULL_NAME, USER_JACK_GIVEN_NAME, USER_JACK_FAMILY_NAME);
	}

	protected void assertUserJack(PrismObject<UserType> user, String fullName) {
		assertUserJack(user, fullName, USER_JACK_GIVEN_NAME, USER_JACK_FAMILY_NAME);
	}

	protected void assertUserJack(PrismObject<UserType> user, String fullName, String givenName, String familyName) {
		assertUserJack(user, fullName, givenName, familyName, "Caribbean");
	}

	protected void assertUserJack(PrismObject<UserType> user, String fullName, String givenName, String familyName, String locality) {
		assertUserJack(user, USER_JACK_USERNAME, fullName, givenName, familyName, locality);
	}

	protected void assertUserJack(PrismObject<UserType> user, String name, String fullName, String givenName, String familyName, String locality) {
		assertUser(user, USER_JACK_OID, name, fullName, givenName, familyName, locality);
		UserType userType = user.asObjectable();
		PrismAsserts.assertEqualsPolyString("Wrong jack honorificPrefix", "Cpt.", userType.getHonorificPrefix());
		assertEquals("Wrong jack employeeNumber", "001", userType.getEmployeeNumber());
		assertEquals("Wrong jack employeeType", "CAPTAIN", userType.getEmployeeType().get(0));
		if (locality == null) {
			assertNull("Locality sneaked to user jack", userType.getLocality());
		} else {
			PrismAsserts.assertEqualsPolyString("Wrong jack locality", locality, userType.getLocality());
		}
	}
	
	protected ItemPath extensionPath(QName qname) {
		return ItemPath.create(ObjectType.F_EXTENSION, qname);
	}

	protected Task createSimpleTask(String operation) {
		Task task = taskManager.createTaskInstance(operation);
		task.setChannel(SchemaConstants.CHANNEL_GUI_USER_URI);
		return task;
	}
}
