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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;
import static com.evolveum.midpoint.web.AdminGuiTestConstants.*;

import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.DescriptorLoader;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AdminGuiConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.apache.wicket.Application;
import org.apache.wicket.ThreadContext;
import org.apache.wicket.protocol.http.WicketFilter;
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

import java.io.File;
import java.lang.reflect.Method;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;
import javax.xml.namespace.QName;

/**
 * @author lazyman
 * @author semancik
 */
public abstract class AbstractGuiIntegrationTest extends AbstractModelIntegrationTest {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractGuiIntegrationTest.class);

    public static final File FOLDER_BASIC = new File("./src/test/resources/basic");
    
    public static final String NS_PIRACY = "http://midpoint.evolveum.com/xml/ns/samples/piracy";
    public static final QName PIRACY_WEAPON = new QName(NS_PIRACY, "weapon");
    public static final QName PIRACY_COLORS = new QName(NS_PIRACY, "colors");
    public static final QName PIRACY_SECRET = new QName(NS_PIRACY, "secret");
    public static final QName PIRACY_RANT = new QName(NS_PIRACY, "rant");
    public static final QName PIRACY_TRANSFORM_DESCRIPTION = new QName(NS_PIRACY, "transformDescription");
    public static final QName PIRACY_TRANSFORMATION_ENABLED = new QName(NS_PIRACY, "transformationEnabled");
    public static final QName PIRACY_TRANSFORM = new QName(NS_PIRACY, "transform");
    public static final QName PIRACY_PATTERN = new QName(NS_PIRACY, "pattern");
    public static final QName PIRACY_REPLACEMENT = new QName(NS_PIRACY, "replacement");
    
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

    private MidPointApplication application;
    
    @Autowired private ApplicationContext appContext;
    @Autowired protected PrismContext prismContext;
    @Autowired protected ExpressionFactory expressionFactory;

    @Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
	}
    
    @PostConstruct
    public void setupApplication() throws ServletException {
    	
    	display("PostContruct");
    	Set<String> applicationKeys = Application.getApplicationKeys();
    	for (String key: Application.getApplicationKeys()) {
    		display("App "+key, Application.get(key));
    	}
    	
    	application = (MidPointApplication) Application.get("midpoint");
    	if (application == null) {
    		application = createInitializedMidPointApplication();
    	}
    }
    
    private MidPointApplication createInitializedMidPointApplication() throws ServletException {
		MidPointApplication application = new MidPointApplication();
    	WicketFilter wicketFilter = new WicketFilter(application);
    	MockServletContext servletContext = new MockServletContext();
    	WebApplicationContext wac = new MockWebApplicationContext(appContext, servletContext);
    	servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
    	MockFilterConfig filterConfig = new MockFilterConfig(servletContext, "midpoint");
    	filterConfig.addInitParameter("applicationClassName", MidPointApplication.class.getName());
		wicketFilter.init(filterConfig);
		application.setWicketFilter(wicketFilter);
		application.setServletContext(servletContext);
    	new DescriptorLoader().loadData(application);
    	ThreadContext.setApplication(application);
    	application.initApplication();
    	ThreadContext.setApplication(null);
    	return application;
	}
    
    @BeforeMethod
    public void beforeMethodApplication() {
    	ThreadContext.setApplication(application);
    }
    
    @AfterMethod
    public void afterMethodApplication() {
    	ThreadContext.setApplication(null);
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
			public AdminGuiConfigurationType getAdminGuiConfiguration() {
				Task task = createSimpleTask("getAdminGuiConfiguration");
				try {
					return getModelInteractionService().getAdminGuiConfiguration(task, task.getResult());
				} catch (ObjectNotFoundException | SchemaException e) {
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
		return new ItemPath(ObjectType.F_EXTENSION, qname);
	}

	protected Task createSimpleTask(String operation) {
		Task task = taskManager.createTaskInstance(operation);
		task.setChannel(SchemaConstants.CHANNEL_GUI_USER_URI);
		return task;
	}
}
