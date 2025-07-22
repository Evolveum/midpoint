/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;

import static com.evolveum.midpoint.web.AdminGuiTestConstants.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.model.api.trigger.TriggerHandlerRegistry;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.schema.merger.AdminGuiConfigurationMergeManager;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;

import com.evolveum.midpoint.model.api.correlation.CorrelationService;
import com.evolveum.midpoint.model.api.simulation.SimulationResultManager;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.web.util.validation.MidpointFormValidatorRegistry;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.io.IOUtils;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
//import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.util.tester.WicketTester;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.BeforeClass;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.api.factory.wrapper.ItemWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.model.api.interaction.DashboardService;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.security.MidPointApplication;

/**
 * @author lazyman
 * @author semancik
 */
public abstract class AbstractGuiIntegrationTest extends AbstractModelIntegrationTest {

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
    protected static final String USER_ADMINISTRATOR_OID = "00000000-0000-0000-0000-000000000002";

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
    @Autowired protected PrismContext prismContext;
    @Autowired protected ExpressionFactory expressionFactory;
    @Autowired protected RelationRegistry relationRegistry;
    @Autowired protected GuiComponentRegistry registry;
    @Autowired protected SmartIntegrationService smartIntegrationService;

    protected WicketTester tester;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        logger.info("before super init");

        if (!repositoryService.isNative()) {
            // we have to reimport superuser role without assignment to policy type because PolicyType is not
            // supported in non-native repositories and data import for initial objects failed for this role
            try (InputStream is = AbstractIntegrationTest.class.getResourceAsStream("/initial-objects/role/030-role-superuser.xml")) {
                String xml = IOUtils.toString(is);
                PrismObject<RoleType> superuser = prismContext.parseObject(xml);
                RoleType role = superuser.asObjectable();
                List<AssignmentType> assignments = role.getAssignment().stream()
                        .filter(a -> a.getTargetRef() == null || !PolicyType.COMPLEX_TYPE.equals(a.getTargetRef().getType()))
                        .toList();
                role.getAssignment().clear();
                role.getAssignment().addAll(assignments);

                repositoryService.addObject(superuser, RepoAddOptions.createOverwrite(), initResult);
            } catch (IOException ex) {
                throw new RuntimeException("Couldn't save super user role in non-native repository", ex);
            }
        }

        super.initSystem(initTask, initResult);

        logger.info("after super init");

        login(USER_ADMINISTRATOR_USERNAME);
        logger.info("user logged in");

        // cglib used by wicket unsupport java 15+ so we need use byte buddy generation for wicket
        // We can remove this after cglib(wicket) fix issue with java 15+ or when wicket will use byte buddy as default
        System.setProperty("wicket.ioc.useByteBuddy", "true");

        tester = new WicketTester(application, true);
    }

    @BeforeClass
    public void displayLocalizationAndSystemBuildInfo() {
        display("Localization service: " + localizationService);
        display("System build: " + localizationService.translate("midpoint.system.build", null, localizationService.getDefaultLocale()));
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
            public SmartIntegrationService getSmartIntegrationService() {
                return smartIntegrationService;
            }

            @Override
            public DashboardService getDashboardService() {
                return dashboardService;
            }

            @Override
            public Task createSimpleTask(String operationName) {
                MidPointPrincipal user = AuthUtil.getPrincipalUser();
                if (user == null) {
                    throw new IllegalStateException("No authenticated user");
                }
                return WebModelServiceUtils.createSimpleTask(operationName, SchemaConstants.CHANNEL_USER_URI, user.getFocus().asPrismObject(), taskManager);
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
            public CompiledGuiProfile getCompiledGuiProfile() {
                Task task = createSimpleTask("getCompiledGuiProfile");
                try {
                    return getModelInteractionService().getCompiledGuiProfile(task, task.getResult());
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

            @Override
            public <O extends ObjectType> PrismObjectWrapperFactory<O> findObjectWrapperFactory(PrismObjectDefinition<O> objectDef) {
                return registry.getObjectWrapperFactory(objectDef);
            }

            @Override
            public <I extends Item, IW extends ItemWrapper> IW createItemWrapper(I item, ItemStatus status, WrapperContext ctx) throws SchemaException {
                ItemWrapperFactory<IW, ?, ?> factory = registry.findWrapperFactory(item.getDefinition(), null);

                ctx.setCreateIfEmpty(true);
                return factory.createWrapper(null, item, status, ctx);
            }

            @Override
            public <IW extends ItemWrapper, VW extends PrismValueWrapper, PV extends PrismValue> VW createValueWrapper(IW parentWrapper, PV newValue, ValueStatus status, WrapperContext context) throws SchemaException {
                ItemWrapperFactory<IW, VW, PV> factory = registry.findWrapperFactory(parentWrapper, null);

                return factory.createValueWrapper(parentWrapper, newValue, status, context);
            }

            @Override
            public MidpointFormValidatorRegistry getFormValidatorRegistry() {
                return null;
            }

            @Override
            public AdminGuiConfigurationMergeManager getAdminGuiConfigurationMergeManager() {
                return null;
            }

            @Override
            public CorrelationService getCorrelationService() {
                return null;
            }

            @Override
            public SimulationResultManager getSimulationResultManager() {
                return null;
            }

            @Override
            public RoleAnalysisService getRoleAnalysisService() {
                return null;
            }

            @Override
            public TriggerHandlerRegistry getTriggerHandlerRegistry() {
                return null;
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
        assertEquals("Wrong jack subtype", "CAPTAIN", userType.getSubtype().get(0));
        if (locality == null) {
            assertNull("Locality sneaked to user jack", userType.getLocality());
        } else {
            PrismAsserts.assertEqualsPolyString("Wrong jack locality", locality, userType.getLocality());
        }
    }

    protected ItemPath extensionPath(QName qname) {
        return ItemPath.create(ObjectType.F_EXTENSION, qname);
    }

    /**
     * Emulate closing ModalWindow component.
     */
    protected void executeModalWindowCloseCallback(String path) {
        Component component = tester.getComponentFromLastRenderedPage(path);
        /*
        if (!(component instanceof ModalWindow)) {
            fail("path: '" + path + "' is not ModalWindow: " + component.getClass());
        }
        for (Behavior behavior : ((ModalWindow)component).getBehaviors()) {
            if (behavior instanceof AbstractDefaultAjaxBehavior) {
                String name = behavior.getClass().getSimpleName();
                if (name.startsWith("WindowClosedBehavior")) {
                    tester.executeBehavior((AbstractAjaxBehavior) behavior);
                }
            }
        }
        */
    }

    protected void clickOnTab(int order, Class<? extends Page> expectedPage) {
        tester.assertRenderedPage(expectedPage);
        String tabPath = "mainPanel:mainForm:tabPanel:tabs-container:tabs:" + order + ":link";
        tester.clickLink(tabPath);
    }

    protected void clickOnDetailsAssignmentMenu(int order, Class<? extends Page> expectedPage) {
        tester.assertRenderedPage(expectedPage);
        tester.debugComponentTrees("navigation");
        String tabPath = "detailsView:mainForm:navigation:menu:2:navLinkStyle:subNavigation:menu:" + order + ":navLinkStyle:navItemLink";
        tester.clickLink(tabPath);
    }

    protected void clickOnDetailsMenu(int order, Class<? extends Page> expectedPage) {
        tester.assertRenderedPage(expectedPage);
        tester.debugComponentTrees("navigation");
        String tabPath = "detailsView:mainForm:navigation:menu:" + order + ":navLinkStyle:navItemLink";
        tester.clickLink(tabPath);
    }
}
