/*
 * Copyright (c) 2016-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui;

import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.midpoint.gui.api.prism.*;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.impl.factory.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.impl.factory.ShadowWrapperFactoryImpl;
import com.evolveum.midpoint.gui.impl.factory.WrapperContext;
import com.evolveum.midpoint.gui.impl.prism.*;
import com.evolveum.midpoint.gui.test.TestMidPointSpringApplication;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.web.AbstractInitializedGuiIntegrationTest;
import com.evolveum.midpoint.web.AdminGuiTestConstants;
import com.evolveum.midpoint.web.WrapperTestUtil;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.web.AdminGuiTestConstants.*;
import static org.testng.AssertJUnit.*;

/**
 * @author semancik
 */
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@SpringBootTest(classes = TestMidPointSpringApplication.class)
public class TestIntegrationObjectWrapperFactory extends AbstractInitializedGuiIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/wrapper");

    protected static final File ROLE_PROP_READ_ALL_MODIFY_SOME_USER_FILE = new File(TEST_DIR, "role-prop-read-all-modify-some-user.xml");
    protected static final String ROLE_PROP_READ_ALL_MODIFY_SOME_USER_OID = "00000000-0000-0000-0000-00000000ae05";

    protected static final File ROLE_PROP_READ_SOME_MODIFY_SOME_USER_FILE = new File(TEST_DIR, "role-prop-read-some-modify-some-user.xml");
    protected static final String ROLE_PROP_READ_SOME_MODIFY_SOME_USER_OID = "00000000-0000-0000-0000-00000000ae08";

    private static final String USER_WALLY_NAME = "wally";
    private static final String USER_WALLY_FULLNAME = "Wally B. Feed";

    public static final String GROUP_DUMMY_MAPMAKERS_NAME = "mapmakers";

    private static final String USER_NEWMAN_USERNAME = "newman";
    private static final String USER_NEWMAN_GIVEN_NAME = "John";
    private static final String USER_NEWMAN_FAMILY_NAME = "Newman";
    private static final String USER_NEWMAN_EMPLOYEE_NUMBER = "N00001";
    private static final String USER_NEWMAN_SHIP = "Nova";

    private static final List<ItemPath> BASIC_USER_CONTAINERS_PATHS = Arrays.asList(
            UserType.F_EXTENSION,
            UserType.F_METADATA,
            UserType.F_ASSIGNMENT,
            UserType.F_ACTIVATION,
            UserType.F_TRIGGER,
            UserType.F_CREDENTIALS,
            UserType.F_ADMIN_GUI_CONFIGURATION);
    private static final List<ItemPath> BASIC_SHADOW_CONTAINERS_PATHS = Arrays.asList(
            ShadowType.F_EXTENSION,
            ShadowType.F_METADATA,
            ShadowType.F_PENDING_OPERATION,
            ShadowType.F_ATTRIBUTES,
            ShadowType.F_TRIGGER,
            ShadowType.F_ASSOCIATION,
            ShadowType.F_ACTIVATION,
            ShadowType.F_CREDENTIALS);
    private static final List<ItemPath> BASIC_ORG_CONTAINERS_PATHS = Arrays.asList(
            OrgType.F_EXTENSION,
            OrgType.F_METADATA,
            OrgType.F_ASSIGNMENT,
            OrgType.F_ACTIVATION,
            OrgType.F_INDUCEMENT,
            OrgType.F_AUTHORIZATION,
//            OrgType.F_EXCLUSION,
            OrgType.F_CONDITION,
            OrgType.F_ADMIN_GUI_CONFIGURATION,
            OrgType.F_DATA_PROTECTION,
            OrgType.F_TRIGGER,
            OrgType.F_AUTOASSIGN,
            ShadowType.F_CREDENTIALS);

    private String userWallyOid;
    private String accountWallyOid;


    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAddObjectFromFile(ROLE_PROP_READ_ALL_MODIFY_SOME_USER_FILE, initResult);
        repoAddObjectFromFile(ROLE_PROP_READ_SOME_MODIFY_SOME_USER_FILE, initResult);

        PrismObject<SystemConfigurationType> systemConfig = parseObject(SYSTEM_CONFIGURATION_FILE);

//        LOGGER.info("adding system config page");
        addObject(systemConfig, ModelExecuteOptions.createOverwrite(), initTask, initResult);

    }

    @Test
    public void test100CreateWrapperUserJack() throws Exception {
        final String TEST_NAME = "test100CreateWrapperUserJack";
        Task task = taskManager.createTaskInstance(TEST_NAME);

        OperationResult result = task.getResult();

        // WHEN
        when(TEST_NAME);
        assertLoggedInUserOid(USER_ADMINISTRATOR_OID);

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        PrismObject<UserType> userOld = user.clone();

        PrismObjectWrapperFactory<UserType> factory = getServiceLocator(task).findObjectWrapperFactory(user.getDefinition());
        WrapperContext context = new WrapperContext(task, result);
        PrismObjectWrapper<UserType> objectWrapper = factory.createObjectWrapper(user, ItemStatus.NOT_CHANGED, context);

        // THEN
        then(TEST_NAME);

        IntegrationTestTools.display("Wrapper after", objectWrapper);

        WrapperTestUtil.assertWrapper(objectWrapper, getString("prismContainer.mainPanelDisplayName"), "user description", user, userOld, ItemStatus.NOT_CHANGED);
        assertContainersPaths(objectWrapper, BASIC_USER_CONTAINERS_PATHS);

        assertEquals("wrong number of containers in "+objectWrapper, 1, objectWrapper.getValues().size());
        PrismObjectValueWrapper<UserType> mainContainerValueWrapper = objectWrapper.getValue();
        WrapperTestUtil.assertPropertyWrapperByName(mainContainerValueWrapper, UserType.F_NAME, PrismTestUtil.createPolyString(USER_JACK_USERNAME));
        WrapperTestUtil.assertPropertyWrapperByName(mainContainerValueWrapper, UserType.F_TIMEZONE, null);
        Item ship = userOld.findItem(ItemPath.create(UserType.F_EXTENSION, PIRACY_SHIP));
        assertNotNull("Ship is null", ship);
        WrapperTestUtil.assertPropertyWrapper(mainContainerValueWrapper, extensionPath(PIRACY_SHIP), AdminGuiTestConstants.USER_JACK_SHIP);

        PrismContainerWrapper<ActivationType> activationContainerWrapper = objectWrapper.findContainer(UserType.F_ACTIVATION);
        WrapperTestUtil.assertWrapper(activationContainerWrapper, getString("ActivationType.activation"), UserType.F_ACTIVATION, user, ItemStatus.NOT_CHANGED);
        assertEquals("wrong number of containers in "+activationContainerWrapper, 1, activationContainerWrapper.getValues().size());
        PrismContainerValueWrapper<ActivationType> activationContainerValueWrapper = activationContainerWrapper.getValues().iterator().next();
        WrapperTestUtil.assertPropertyWrapperByName(activationContainerValueWrapper, ActivationType.F_ADMINISTRATIVE_STATUS, ActivationStatusType.ENABLED);
        WrapperTestUtil.assertPropertyWrapperByName(activationContainerValueWrapper, ActivationType.F_LOCKOUT_STATUS, null);

        assertEquals("Wrong main container wrapper readOnly", Boolean.FALSE, (Boolean)objectWrapper.isReadOnly());

        assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_NAME, true);
        assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_GIVEN_NAME, true);
        assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_FULL_NAME, true);
        assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_ADDITIONAL_NAME, false); // not visible, because it is empty
        assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_LOCALITY, true);

        assertItemWrapperProcessing(mainContainerValueWrapper, extensionPath(PIRACY_WEAPON), null);
        assertItemWrapperProcessing(mainContainerValueWrapper, extensionPath(PIRACY_COLORS), ItemProcessing.AUTO);
        assertItemWrapperProcessing(mainContainerValueWrapper, extensionPath(PIRACY_SECRET), ItemProcessing.IGNORE);
        assertItemWrapperProcessing(mainContainerValueWrapper, extensionPath(PIRACY_RANT), ItemProcessing.MINIMAL);

        // WHEN
        mainContainerValueWrapper.setShowEmpty(true);

        // THEN
        assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_NAME, true);
        assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_GIVEN_NAME, true); // emphasized
        assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_FULL_NAME, true); // emphasized
        assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_ADDITIONAL_NAME, true); // empty
        assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_LOCALITY, true); // empty

        ObjectDelta<UserType> objectDelta = objectWrapper.getObjectDelta();
        display("Delta", objectDelta);
        assertTrue("non-empty delta produced from wrapper: "+objectDelta, objectDelta.isEmpty());
    }

    private void assertContainersPaths(PrismObjectWrapper<?> objectWrapper, Collection<ItemPath> expectedPaths) {
        Set<UniformItemPath> expectedUniformPaths = expectedPaths.stream()
                .map(p -> prismContext.toUniformPath(p))
                .collect(Collectors.toSet());

        List<ItemWrapper> containerWrappers = objectWrapper.getValue().getItems().stream()
                .filter(w -> w instanceof PrismContainerWrapper).collect(Collectors.toList());

        Set<UniformItemPath> realUniformPaths = containerWrappers.stream()
                .map(c -> prismContext.toUniformPath(c.getPath()))
                .collect(Collectors.toSet());
        assertEquals("wrong container paths in "+objectWrapper, expectedUniformPaths, realUniformPaths);
    }

    /**
     * Create wrapper for brand new empty user.
     */
    @Test
    public void test110CreateWrapperUserNewEmpty() throws Exception {
        final String TEST_NAME = "test110CreateWrapperUserNew";
        PrismObject<UserType> user = getUserDefinition().instantiate();

        // WHEN
        when(TEST_NAME);

        Task task = taskManager.createTaskInstance(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObjectWrapperFactory<UserType> factory = getServiceLocator(task).findObjectWrapperFactory(user.getDefinition());
        WrapperContext context = new WrapperContext(task, result);
        context.setCreateIfEmpty(true);
        context.setShowEmpty(true);

        PrismObjectWrapper<UserType> objectWrapper = factory.createObjectWrapper(user, ItemStatus.ADDED, context);

        // THEN
        then(TEST_NAME);

        IntegrationTestTools.display("Wrapper after", objectWrapper);

        WrapperTestUtil.assertWrapper(objectWrapper, getString("prismContainer.mainPanelDisplayName"), "user description", user, getUserDefinition().instantiate(), ItemStatus.ADDED);
        assertContainersPaths(objectWrapper, BASIC_USER_CONTAINERS_PATHS);

        assertEquals("wrong number of containers in "+objectWrapper, 1, objectWrapper.getValues().size());
        PrismContainerValueWrapper<UserType> mainContainerValueWrapper = objectWrapper.getValue();
        WrapperTestUtil.assertPropertyWrapperByName(mainContainerValueWrapper, UserType.F_NAME, null);
        WrapperTestUtil.assertPropertyWrapperByName(mainContainerValueWrapper, UserType.F_TIMEZONE, null);

        PrismContainerWrapper<ActivationType> activationContainerWrapper = objectWrapper.findContainer(UserType.F_ACTIVATION);
        WrapperTestUtil.assertWrapper(activationContainerWrapper, getString("ActivationType.activation"), UserType.F_ACTIVATION, user, ItemStatus.ADDED);
        assertEquals("wrong number of containers in "+activationContainerWrapper, 1, activationContainerWrapper.getValues().size());
        PrismContainerValueWrapper<ActivationType> activationContainerValueWrapper = activationContainerWrapper.getValues().iterator().next();
        WrapperTestUtil.assertPropertyWrapperByName(activationContainerValueWrapper, ActivationType.F_ADMINISTRATIVE_STATUS, null);
        WrapperTestUtil.assertPropertyWrapperByName(activationContainerValueWrapper, ActivationType.F_LOCKOUT_STATUS, null);

        assertEquals("Wrong main container wrapper readOnly", Boolean.FALSE, (Boolean)objectWrapper.isReadOnly());

        assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_NAME, true);
        assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_GIVEN_NAME, true);
        assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_FULL_NAME, true);
        assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_ADDITIONAL_NAME, true);

        assertItemWrapperProcessing(mainContainerValueWrapper, extensionPath(PIRACY_WEAPON), null);
        assertItemWrapperProcessing(mainContainerValueWrapper, extensionPath(PIRACY_COLORS), ItemProcessing.AUTO);
        assertItemWrapperProcessing(mainContainerValueWrapper, extensionPath(PIRACY_SECRET), ItemProcessing.IGNORE);
        assertItemWrapperProcessing(mainContainerValueWrapper, extensionPath(PIRACY_RANT), ItemProcessing.MINIMAL);

        // WHEN
        mainContainerValueWrapper.setShowEmpty(false);

        // THEN
        assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_NAME, true);
        assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_GIVEN_NAME, true); // emphasized
        assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_FULL_NAME, true); // emphasized
        assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_ADDITIONAL_NAME, false); // empty
        assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_LOCALITY, false); // empty

        ObjectDelta<UserType> objectDelta = objectWrapper.getObjectDelta();
        display("Delta", objectDelta);
        assertTrue("non-add delta produced from wrapper: "+objectDelta, objectDelta.isAdd());
        PrismObject<UserType> objectToAdd = objectDelta.getObjectToAdd();
        assertTrue("non-empty object in add delta produced from wrapper: "+objectDelta, objectToAdd.isEmpty());
    }

    /**
     * Create wrapper for brand new user, but "fill in" some data.
     */
    @Test
    public void test112CreateWrapperUserNewman() throws Exception {
        final String TEST_NAME = "test112CreateWrapperUserNewman";
        PrismObject<UserType> user = getUserDefinition().instantiate();

        // WHEN
        when(TEST_NAME);

        Task task = taskManager.createTaskInstance(TEST_NAME);
        OperationResult result = task.getResult();

        ModelServiceLocator modelServiceLocator = getServiceLocator(task);
        PrismObjectWrapperFactory<UserType> factory = modelServiceLocator.findObjectWrapperFactory(user.getDefinition());
        WrapperContext context = new WrapperContext(task, result);
        PrismObjectWrapper<UserType> objectWrapper = factory.createObjectWrapper(user,
                ItemStatus.ADDED, context);

        PrismObjectValueWrapper<UserType> mainContainerValueWrapper = objectWrapper.getValue();

        WrapperTestUtil.fillInPropertyWrapper(modelServiceLocator, mainContainerValueWrapper, UserType.F_NAME, PrismTestUtil.createPolyString(USER_NEWMAN_USERNAME));
        WrapperTestUtil.fillInPropertyWrapper(modelServiceLocator, mainContainerValueWrapper, UserType.F_GIVEN_NAME, PrismTestUtil.createPolyString(USER_NEWMAN_GIVEN_NAME));
        WrapperTestUtil.fillInPropertyWrapper(modelServiceLocator, mainContainerValueWrapper, UserType.F_FAMILY_NAME, PrismTestUtil.createPolyString(USER_NEWMAN_FAMILY_NAME));
        WrapperTestUtil.fillInPropertyWrapper(modelServiceLocator, mainContainerValueWrapper, UserType.F_EMPLOYEE_NUMBER, USER_NEWMAN_EMPLOYEE_NUMBER);
        WrapperTestUtil.fillInPropertyWrapper(modelServiceLocator, mainContainerValueWrapper, extensionPath(PIRACY_SHIP), USER_NEWMAN_SHIP);

        // THEN
        then(TEST_NAME);

        IntegrationTestTools.display("Wrapper after", objectWrapper);

        WrapperTestUtil.assertWrapper(objectWrapper, getString("prismContainer.mainPanelDisplayName"), "user description", user, getUserDefinition().instantiate(), ItemStatus.ADDED);
        assertContainersPaths(objectWrapper, BASIC_USER_CONTAINERS_PATHS);

        WrapperTestUtil.assertWrapper(objectWrapper, getString("prismContainer.mainPanelDisplayName"), (ItemPath)null, user, ItemStatus.ADDED);
        assertEquals("wrong number of containers in "+objectWrapper, 1, objectWrapper.getValues().size());
        WrapperTestUtil.assertPropertyWrapperByName(mainContainerValueWrapper, UserType.F_NAME, PrismTestUtil.createPolyString(USER_NEWMAN_USERNAME));
        WrapperTestUtil.assertPropertyWrapperByName(mainContainerValueWrapper, UserType.F_TIMEZONE, null);
        WrapperTestUtil.assertPropertyWrapper(mainContainerValueWrapper, extensionPath(PIRACY_SHIP), USER_NEWMAN_SHIP);

        PrismContainerWrapper<ActivationType> activationContainerWrapper = objectWrapper.findContainer(UserType.F_ACTIVATION);
        WrapperTestUtil.assertWrapper(activationContainerWrapper, getString("ActivationType.activation"), UserType.F_ACTIVATION, user, ItemStatus.ADDED);
        assertEquals("wrong number of containers in "+activationContainerWrapper, 1, activationContainerWrapper.getValues().size());

        assertEquals("Wrong main container wrapper readOnly", Boolean.FALSE, (Boolean)objectWrapper.isReadOnly());

        assertItemWrapperProcessing(mainContainerValueWrapper, extensionPath(PIRACY_WEAPON), null);
        assertItemWrapperProcessing(mainContainerValueWrapper, extensionPath(PIRACY_COLORS), ItemProcessing.AUTO);
        assertItemWrapperProcessing(mainContainerValueWrapper, extensionPath(PIRACY_SECRET), ItemProcessing.IGNORE);
        assertItemWrapperProcessing(mainContainerValueWrapper, extensionPath(PIRACY_RANT), ItemProcessing.MINIMAL);

        assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_NAME, true);
        assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_GIVEN_NAME, true); // emphasized
        assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_FULL_NAME, true); // emphasized
        assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_ADDITIONAL_NAME, true); // empty
        assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_LOCALITY, true); // empty

        // WHEN
        mainContainerValueWrapper.setShowEmpty(false);

        // THEN
        assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_NAME, true);
        assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_GIVEN_NAME, true);
        assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_FULL_NAME, true);
        assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_ADDITIONAL_NAME, false); // not visible, because it is empty

        ObjectDelta<UserType> objectDelta = objectWrapper.getObjectDelta();
        display("Delta", objectDelta);
        assertTrue("non-add delta produced from wrapper: "+objectDelta, objectDelta.isAdd());
        PrismObject<UserType> objectToAdd = objectDelta.getObjectToAdd();
        PrismAsserts.assertPropertyValue(objectToAdd, UserType.F_NAME, PrismTestUtil.createPolyString(USER_NEWMAN_USERNAME));
        PrismAsserts.assertPropertyValue(objectToAdd, UserType.F_GIVEN_NAME, PrismTestUtil.createPolyString(USER_NEWMAN_GIVEN_NAME));
        PrismAsserts.assertPropertyValue(objectToAdd, UserType.F_FAMILY_NAME, PrismTestUtil.createPolyString(USER_NEWMAN_FAMILY_NAME));
        PrismAsserts.assertPropertyValue(objectToAdd, UserType.F_EMPLOYEE_NUMBER, USER_NEWMAN_EMPLOYEE_NUMBER);
        PrismAsserts.assertPropertyValue(objectToAdd, extensionPath(PIRACY_SHIP), USER_NEWMAN_SHIP);
        PrismAsserts.assertItems(objectToAdd, 5);
    }

    @Test
    public void test102CreateWrapperUserEmpty() throws Exception {
        final String TEST_NAME = "test102CreateWrapperUserEmpty";
        PrismObject<UserType> user = getUser(USER_EMPTY_OID);
        PrismObject<UserType> userOld = user.clone();

        // WHEN
        when(TEST_NAME);

        Task task = taskManager.createTaskInstance(TEST_NAME);
        OperationResult result = task.getResult();

        ModelServiceLocator modelServiceLocator = getServiceLocator(task);
        PrismObjectWrapperFactory<UserType> factory = modelServiceLocator.findObjectWrapperFactory(user.getDefinition());
        WrapperContext context = new WrapperContext(task, result);

        PrismObjectWrapper<UserType> objectWrapper = factory.createObjectWrapper(user, ItemStatus.NOT_CHANGED, context);

        // THEN
        then(TEST_NAME);

        IntegrationTestTools.display("Wrapper after", objectWrapper);

        WrapperTestUtil.assertWrapper(objectWrapper, getString("prismContainer.mainPanelDisplayName"), "user description", user, userOld, ItemStatus.NOT_CHANGED);
        assertContainersPaths(objectWrapper, BASIC_USER_CONTAINERS_PATHS);

        WrapperTestUtil.assertWrapper(objectWrapper, getString("prismContainer.mainPanelDisplayName"), (ItemPath)null, user, ItemStatus.NOT_CHANGED);
        assertEquals("wrong number of containers in "+objectWrapper, 1, objectWrapper.getValues().size());
        PrismContainerValueWrapper<UserType> mainContainerValueWrapper = objectWrapper.getValue();
        WrapperTestUtil.assertPropertyWrapperByName(mainContainerValueWrapper, UserType.F_NAME, PrismTestUtil.createPolyString(USER_EMPTY_USERNAME));
        WrapperTestUtil.assertPropertyWrapperByName(mainContainerValueWrapper, UserType.F_TIMEZONE, null);

        // Not sure about this
//        ContainerWrapper<ActivationType> activationContainerWrapper = objectWrapper.findContainer(ItemPath.create(UserType.F_ACTIVATION));
//        assertNull("Unexpected activation "+activationContainerWrapper, activationContainerWrapper);

        assertEquals("Wrong main container wrapper readOnly", Boolean.FALSE, (Boolean)objectWrapper.isReadOnly());

        assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_NAME, true);
        assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_GIVEN_NAME, true); // emphasized
        assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_FULL_NAME, true); // emphasized
        assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_ADDITIONAL_NAME, false); // empty
        assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_LOCALITY, false); // empty

        // WHEN
        mainContainerValueWrapper.setShowEmpty(true);

        // THEN
        assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_NAME, true);
        assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_GIVEN_NAME, true); // emphasized
        assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_FULL_NAME, true); // emphasized
        assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_ADDITIONAL_NAME, true); // empty
        assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_LOCALITY, true); // empty

        ObjectDelta<UserType> objectDelta = objectWrapper.getObjectDelta();
        display("Delta", objectDelta);
        assertTrue("non-empty delta produced from wrapper: "+objectDelta, objectDelta.isEmpty());
    }


    @Test
    public void test150CreateWrapperShadow() throws Exception {
        final String TEST_NAME = "test150CreateWrapperShadow";
        PrismObject<ShadowType> shadow = getShadowModel(accountJackOid);
        shadow.findReference(ShadowType.F_RESOURCE_REF).getValue().setObject(resourceDummy);
        display("Shadow", shadow);
        PrismObject<ShadowType> shadowOld = shadow.clone();

        // WHEN
        when(TEST_NAME);
        Task task = taskManager.createTaskInstance(TEST_NAME);
        OperationResult result = task.getResult();

        ModelServiceLocator modelServiceLocator = getServiceLocator(task);
        PrismObjectWrapperFactory<ShadowType> factory = modelServiceLocator.findObjectWrapperFactory(shadow.getDefinition());
        assertTrue("Wrong object facotry found, expexted shadow factory but got " + factory.getClass().getSimpleName(), factory instanceof ShadowWrapperFactoryImpl);
        WrapperContext context = new WrapperContext(task, result);

        PrismObjectWrapper<ShadowType> objectWrapper = factory.createObjectWrapper(shadow, ItemStatus.NOT_CHANGED, context);
        assertTrue("Wrong wrapper created. Expected ShadowWrapper but got " + objectWrapper.getClass().getSimpleName(), objectWrapper instanceof  ShadowWrapper);
        ShadowWrapper shadowWrapper = (ShadowWrapper) objectWrapper;


        // THEN
        then(TEST_NAME);
        display("Wrapper after", objectWrapper);

        WrapperTestUtil.assertWrapper(objectWrapper, getString("prismContainer.mainPanelDisplayName"), "shadow description", shadow, shadowOld, ItemStatus.NOT_CHANGED);
        assertContainersPaths(objectWrapper, BASIC_SHADOW_CONTAINERS_PATHS);

        PrismContainerWrapper<ShadowAttributesType> attributesContainerWrapper = objectWrapper.findContainer(ShadowType.F_ATTRIBUTES);
        assertEquals("wrong number of values in "+attributesContainerWrapper, 1, attributesContainerWrapper.getValues().size());
        PrismContainer<ShadowAttributesType> attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
        WrapperTestUtil.assertWrapper(attributesContainerWrapper, "Attributes", ShadowType.F_ATTRIBUTES,
                attributesContainer, false, ItemStatus.NOT_CHANGED);
        PrismContainerValueWrapper<ShadowAttributesType> attributesContainerValueWrapper = attributesContainerWrapper.getValue();
        WrapperTestUtil.assertPropertyWrapperByName(attributesContainerValueWrapper, dummyResourceCtl.getAttributeFullnameQName(), USER_JACK_FULL_NAME);
        WrapperTestUtil.assertPropertyWrapperByName(attributesContainerValueWrapper, SchemaConstants.ICFS_NAME, USER_JACK_USERNAME);
        assertEquals("wrong number of items in "+attributesContainerWrapper, 18, attributesContainerValueWrapper.getItems().size());

        PrismContainerWrapper<ActivationType> activationContainerWrapper = objectWrapper.findContainer(ShadowType.F_ACTIVATION);
        assertEquals("wrong number of values in "+activationContainerWrapper, 1, activationContainerWrapper.getValues().size());
        PrismContainerValueWrapper<ActivationType> activationContainerValueWrapper = activationContainerWrapper.getValues().iterator().next();
        WrapperTestUtil.assertWrapper(activationContainerWrapper, getString("ShadowType.activation"), UserType.F_ACTIVATION, shadow, ItemStatus.NOT_CHANGED);
        WrapperTestUtil.assertPropertyWrapperByName(activationContainerValueWrapper, ActivationType.F_ADMINISTRATIVE_STATUS, ActivationStatusType.ENABLED);
        WrapperTestUtil.assertPropertyWrapperByName(activationContainerValueWrapper, ActivationType.F_LOCKOUT_STATUS, null);

        assertEquals("Wrong attributes container wrapper readOnly", Boolean.FALSE, (Boolean)attributesContainerValueWrapper.isReadOnly());

        PrismPropertyWrapper<String> fullnameWrapper = attributesContainerValueWrapper.findProperty(ItemPath.create(dummyResourceCtl.getAttributeFullnameQName()));
        assertEquals("Wrong attribute fullname readOnly", Boolean.FALSE, (Boolean)fullnameWrapper.isReadOnly()); // Is this OK?
        assertEquals("Wrong attribute fullname visible", Boolean.TRUE, (Boolean)fullnameWrapper.isVisible(attributesContainerValueWrapper, null));
        display("fullname attribute definition", fullnameWrapper);
        assertEquals("Wrong attribute fullname definition.canRead", Boolean.TRUE, (Boolean)fullnameWrapper.canRead());
        assertEquals("Wrong attribute fullname definition.canAdd", Boolean.TRUE, (Boolean)fullnameWrapper.canAdd());
        assertEquals("Wrong attribute fullname definition.canModify", Boolean.TRUE, (Boolean)fullnameWrapper.canModify());
        // MID-3144
        if (fullnameWrapper.getDisplayOrder() == null || fullnameWrapper.getDisplayOrder() < 100 || fullnameWrapper.getDisplayOrder() > 400) {
            AssertJUnit.fail("Wrong fullname definition.displayOrder: " + fullnameWrapper.getDisplayOrder());
        }
        assertEquals("Wrong attribute fullname definition.displayName", "Full Name", fullnameWrapper.getDisplayName());

        ObjectDelta<ShadowType> objectDelta = objectWrapper.getObjectDelta();
        display("Delta", objectDelta);
        assertTrue("non-empty delta produced from wrapper: "+objectDelta, objectDelta.isEmpty());

    }

    private <O extends ObjectType> PrismObjectWrapper<O> createObjectWrapper(Task task, PrismObject<O> object, ItemStatus status) throws SchemaException {
        OperationResult result = task.getResult();

        ModelServiceLocator modelServiceLocator = getServiceLocator(task);
        PrismObjectWrapperFactory<O> factory = modelServiceLocator.findObjectWrapperFactory(object.getDefinition());
        WrapperContext context = new WrapperContext(task, result);
        if (ItemStatus.NOT_CHANGED == status) {
            context.setCreateIfEmpty(true);
            context.setShowEmpty(true);
        }

        PrismObjectWrapper<O> objectWrapper = factory.createObjectWrapper(object, status, context);
        return objectWrapper;

    }

    @Test
    public void test160CreateWrapperOrgScummBar() throws Exception {
        PrismObject<OrgType> org = getObject(OrgType.class, ORG_SCUMM_BAR_OID);
        PrismObject<OrgType> orgOld = org.clone();

        // WHEN
        when();
        Task task = getTestTask();
        PrismObjectWrapper<OrgType> objectWrapper = createObjectWrapper(task, org, ItemStatus.NOT_CHANGED);

        // THEN
        then();
        IntegrationTestTools.display("Wrapper after", objectWrapper);

        WrapperTestUtil.assertWrapper(objectWrapper, getString("prismContainer.mainPanelDisplayName"), "org description", org, orgOld, ItemStatus.NOT_CHANGED);
        assertContainersPaths(objectWrapper, BASIC_ORG_CONTAINERS_PATHS);

        WrapperTestUtil.assertWrapper(objectWrapper, getString("prismContainer.mainPanelDisplayName"), (ItemPath)null, org, ItemStatus.NOT_CHANGED);
        assertEquals("wrong number of containers in "+objectWrapper, 1, objectWrapper.getValues().size());
        PrismContainerValueWrapper<OrgType> mainContainerValueWrapper = objectWrapper.getValue();
        WrapperTestUtil.assertPropertyWrapperByName(mainContainerValueWrapper, OrgType.F_NAME, PrismTestUtil.createPolyString(ORG_SCUMM_BAR_NAME));
        WrapperTestUtil.assertPropertyWrapperByName(mainContainerValueWrapper, OrgType.F_TIMEZONE, null);

        PrismContainerWrapper<ActivationType> activationContainerWrapper = mainContainerValueWrapper.findContainer(OrgType.F_ACTIVATION);
        WrapperTestUtil.assertWrapper(activationContainerWrapper, getString("ActivationType.activation"), OrgType.F_ACTIVATION, org, ItemStatus.NOT_CHANGED);
        assertEquals("wrong number of containers in "+activationContainerWrapper, 1, activationContainerWrapper.getValues().size());
        PrismContainerValueWrapper<ActivationType> activationContainerValueWrapper = activationContainerWrapper.getValues().iterator().next();
        WrapperTestUtil.assertPropertyWrapperByName(activationContainerValueWrapper, ActivationType.F_ADMINISTRATIVE_STATUS, ActivationStatusType.ENABLED);
        WrapperTestUtil.assertPropertyWrapperByName(activationContainerValueWrapper, ActivationType.F_LOCKOUT_STATUS, null);

        assertEquals("Wrong main container wrapper readOnly", Boolean.FALSE, (Boolean)mainContainerValueWrapper.isReadOnly());

        assertItemWrapperFullConrol(mainContainerValueWrapper, OrgType.F_NAME, true);
        assertItemWrapperFullConrol(mainContainerValueWrapper, OrgType.F_IDENTIFIER, true);
        assertItemWrapperFullConrol(mainContainerValueWrapper, OrgType.F_RISK_LEVEL, false); // not visible, because it is empty
        assertItemWrapperFullConrol(mainContainerValueWrapper, OrgType.F_LOCALITY, true);

        assertItemWrapperProcessing(mainContainerValueWrapper, extensionPath(PIRACY_TRANSFORM_DESCRIPTION), null);
        PrismContainerWrapper<Containerable> transformContainerWrapper = mainContainerValueWrapper.findContainer(extensionPath(PIRACY_TRANSFORM));
        assertEquals("Wrong processing in item wrapper for "+PIRACY_TRANSFORM, ItemProcessing.MINIMAL, transformContainerWrapper.getProcessing());

//        ContainerWrapper<Containerable> transformContainerWrapper = objectWrapper.findContainer(ItemPath.create(PIRACY_TRANSFORM));
//        assertEquals("Wrong processing in item wrapper for "+PIRACY_TRANSFORM, ItemProcessing.MINIMAL, transformContainerWrapper.getProcessing());

        // WHEN
        mainContainerValueWrapper.setShowEmpty(true);

        // THEN
        assertItemWrapperFullConrol(mainContainerValueWrapper, OrgType.F_NAME, true);
        assertItemWrapperFullConrol(mainContainerValueWrapper, OrgType.F_IDENTIFIER, true);
        assertItemWrapperFullConrol(mainContainerValueWrapper, OrgType.F_RISK_LEVEL, true); // empty
        assertItemWrapperFullConrol(mainContainerValueWrapper, OrgType.F_LOCALITY, true);

        ObjectDelta<OrgType> objectDelta = objectWrapper.getObjectDelta();
        display("Delta", objectDelta);
        assertTrue("non-empty delta produced from wrapper: "+objectDelta, objectDelta.isEmpty());
    }

    @Test
    public void test220AssignRoleLandluberToWally() throws Exception {
        final String TEST_NAME = "test220AssignRoleLandluberToWally";

        Task task = getTestTask();
        OperationResult result = task.getResult();

        DummyGroup mapmakers = new DummyGroup(GROUP_DUMMY_MAPMAKERS_NAME);
        dummyResource.addGroup(mapmakers);

        PrismObject<UserType> user = createUser(USER_WALLY_NAME, USER_WALLY_FULLNAME, true);
        addObject(user);
        userWallyOid = user.getOid();
        assignRole(userWallyOid, ROLE_MAPMAKER_OID, task, result);

        // preconditions
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(userWallyOid);
        display("User after change execution", userAfter);
        accountWallyOid = getSingleLinkOid(userAfter);

        PrismObject<ShadowType> shadow = getShadowModel(accountWallyOid);
        shadow.findReference(ShadowType.F_RESOURCE_REF).getValue().setObject(resourceDummy);
        display("Shadow", shadow);
        PrismObject<ShadowType> shadowOld = shadow.clone();

        DummyGroup dummyGroup = dummyResource.getGroupByName(GROUP_DUMMY_MAPMAKERS_NAME);
        assertNotNull("No group on dummy resource", dummyGroup);
        display("Group", dummyGroup);
        assertGroupMember(dummyGroup, USER_WALLY_NAME);

        // WHEN
        when(TEST_NAME);

        PrismObjectWrapper<ShadowType> objectWrapper = createObjectWrapper(task, shadow, ItemStatus.NOT_CHANGED);
        assertTrue("Wrong wrapper created. Expected ShadowWrapper but was " + objectWrapper.getClass().getSimpleName(), objectWrapper instanceof ShadowWrapper);
        ShadowWrapper shadowWrapper = (ShadowWrapper) objectWrapper;

        // THEN
        then(TEST_NAME);

        display("Wrapper after", shadowWrapper);

        WrapperTestUtil.assertWrapper(shadowWrapper, getString("prismContainer.mainPanelDisplayName"), "shadow description", shadow, shadowOld, ItemStatus.NOT_CHANGED);
        assertContainersPaths(objectWrapper, BASIC_SHADOW_CONTAINERS_PATHS);

        PrismContainerWrapper<ShadowAttributesType> attributesContainerWrapper = objectWrapper.findContainer(ShadowType.F_ATTRIBUTES);
        WrapperTestUtil.assertWrapper(attributesContainerWrapper, "Attributes", ShadowType.F_ATTRIBUTES, shadow.findContainer(ShadowType.F_ATTRIBUTES),
                false, ItemStatus.NOT_CHANGED);
        assertEquals("wrong number of containers in "+attributesContainerWrapper, 1, attributesContainerWrapper.getValues().size());
        PrismContainerValueWrapper<ShadowAttributesType> attributesContainerValueWrapper = attributesContainerWrapper.getValues().iterator().next();
        WrapperTestUtil.assertPropertyWrapperByName(attributesContainerValueWrapper, dummyResourceCtl.getAttributeFullnameQName(), USER_WALLY_FULLNAME);
        WrapperTestUtil.assertPropertyWrapperByName(attributesContainerValueWrapper, SchemaConstants.ICFS_NAME, USER_WALLY_NAME);
        assertEquals("wrong number of items in "+attributesContainerWrapper, 18, attributesContainerValueWrapper.getItems().size());

        PrismContainerWrapper<ActivationType> activationContainerWrapper = objectWrapper.findContainer(ShadowType.F_ACTIVATION);
        WrapperTestUtil.assertWrapper(activationContainerWrapper, getString("ShadowType.activation"), UserType.F_ACTIVATION, shadow, ItemStatus.NOT_CHANGED);
        assertEquals("wrong number of containers in "+activationContainerWrapper, 1, activationContainerWrapper.getValues().size());
        PrismContainerValueWrapper<ActivationType> activationContainerValueWrapper = activationContainerWrapper.getValues().iterator().next();
        WrapperTestUtil.assertPropertyWrapperByName(activationContainerValueWrapper, ActivationType.F_ADMINISTRATIVE_STATUS, ActivationStatusType.ENABLED);
        WrapperTestUtil.assertPropertyWrapperByName(activationContainerValueWrapper, ActivationType.F_LOCKOUT_STATUS, null);

        //TODO: fix
        PrismContainerWrapper<ShadowAssociationType> associationContainerWrapper = objectWrapper.findContainer(ShadowType.F_ASSOCIATION);
        assertNotNull("No association container wrapper", associationContainerWrapper);
        assertTrue("Wrong type of group association property wrapper: "+associationContainerWrapper.getClass(), associationContainerWrapper instanceof ShadowAssociationWrapperImpl);
        assertEquals("wrong number of items in "+associationContainerWrapper, 1, associationContainerWrapper.getValues().size());
        PrismReferenceWrapper groupAssociationWrapper = (PrismReferenceWrapper) associationContainerWrapper.findReference(RESOURCE_DUMMY_ASSOCIATION_GROUP_QNAME);
        assertNotNull("No group association property wrapper", groupAssociationWrapper);
        List<PrismValueWrapper> groupAssociationValues = groupAssociationWrapper.getValues();
        assertEquals("wrong number of values in "+groupAssociationWrapper, 1, groupAssociationValues.size());
        PrismValueWrapper groupAssociationValue = groupAssociationValues.get(0);
        PrismReferenceValue groupAssociationValuePVal = (PrismReferenceValue) groupAssociationValue.getNewValue();
        display("groupAssociationValuePVal", groupAssociationValuePVal);
        assertEquals("wrong number of values in "+groupAssociationValue, ValueStatus.NOT_CHANGED, groupAssociationValue.getStatus());
        assertEquals("Wrong group association name", RESOURCE_DUMMY_ASSOCIATION_GROUP_QNAME, groupAssociationWrapper.getItemName());
        assertEquals("Wrong group association value", GROUP_DUMMY_MAPMAKERS_NAME, groupAssociationValuePVal.asReferencable().getTargetName().getOrig());
//        PrismContainer<ShadowIdentifiersType> groupAssociationValueIdentifiers = groupAssociationValuePVal.findContainer(ShadowAssociationType.F_IDENTIFIERS);
//        PrismProperty<String> groupAssociationUidProp = groupAssociationValueIdentifiers.findProperty(new QName(null,"uid"));
//        PrismAsserts.assertPropertyValue(groupAssociationValuePVal.asReferencable().getTargetName(), GROUP_DUMMY_MAPMAKERS_NAME);
    }

    @Test
    public void test240OrgScummBarModifyTransformDescription() throws Exception {
        PrismObject<OrgType> org = getObject(OrgType.class, ORG_SCUMM_BAR_OID);

        Task task = getTestTask();

        PrismObjectWrapper<OrgType> objectWrapper = createObjectWrapper(task, org, ItemStatus.NOT_CHANGED);
        IntegrationTestTools.display("Wrapper before", objectWrapper);

        PrismObjectValueWrapper<OrgType> mainContainerValueWrapper = objectWrapper.getValue();

        modifyPropertyWrapper(getServiceLocator(task), mainContainerValueWrapper, extensionPath(PIRACY_TRANSFORM_DESCRIPTION), "Whatever");

        IntegrationTestTools.display("Wrapper after", objectWrapper);

        // WHEN
        when();
        ObjectDelta<OrgType> objectDelta = objectWrapper.getObjectDelta();

        // THEN
        then();
        display("Delta", objectDelta);
        ItemPath ahoyPath = ItemPath.create(ObjectType.F_EXTENSION, PIRACY_TRANSFORM_DESCRIPTION);
        PrismAsserts.assertPropertyReplace(objectDelta, ahoyPath, "Whatever");
        PrismAsserts.assertModifications(objectDelta, 1);

        OperationResult result = task.getResult();
        executeChanges(objectDelta, null, task, result);

        assertSuccess(result);

        PrismObject<OrgType> orgAfter = getObject(OrgType.class, ORG_MINISTRY_OF_RUM_OID);
        display("Org after", orgAfter);

    }

    @Test
    public void test241OrgScummBarModifyTransformProperties() throws Exception {
        final String TEST_NAME = "test241OrgScummBarModifyTransformProperties";
        PrismObject<OrgType> org = getObject(OrgType.class, ORG_SCUMM_BAR_OID);

        Task task = taskManager.createTaskInstance(TEST_NAME);

        PrismObjectWrapper<OrgType> objectWrapper = createObjectWrapper(task, org, ItemStatus.NOT_CHANGED);
        IntegrationTestTools.display("Wrapper before", objectWrapper);

        PrismContainerValueWrapper<OrgType> mainContainerValueWrapper = objectWrapper.getValue();
        PrismContainerWrapper<Containerable> transformContainerWrapper = mainContainerValueWrapper.findContainer(extensionPath(PIRACY_TRANSFORM));
        List<PrismContainerValueWrapper<Containerable>> transformValueWrappers = transformContainerWrapper.getValues();
        assertEquals("Unexpecter number of transform value wrappers", 3, transformValueWrappers.size());

        PrismContainerValueWrapper<Containerable> valueWrapperA = findTransformValueWrapper(transformValueWrappers, "A");
        assertNotNull("No A value wrapper", valueWrapperA);
        display("A value wrapper", valueWrapperA);
        modifyTransformProp(valueWrapperA, PIRACY_REPLACEMENT, "Ahoy");

        IntegrationTestTools.display("Wrapper after", objectWrapper);

        // WHEN
        when(TEST_NAME);
        ObjectDelta<OrgType> objectDelta = objectWrapper.getObjectDelta();

        // THEN
        then(TEST_NAME);
        display("Delta", objectDelta);
        ItemPath ahoyPath = ItemPath.create(ObjectType.F_EXTENSION, PIRACY_TRANSFORM, valueWrapperA.getNewValue().getId(), PIRACY_REPLACEMENT);
        PrismAsserts.assertPropertyReplace(objectDelta, ahoyPath, "Ahoy");
        PrismAsserts.assertModifications(objectDelta, 1);

        OperationResult result = task.getResult();
        executeChanges(objectDelta, null, task, result);

        assertSuccess(result);

        PrismObject<OrgType> orgAfter = getObject(OrgType.class, ORG_MINISTRY_OF_RUM_OID);
        display("Org after", orgAfter);

    }

    @Test
    public void test242OrgScummBarAddTransform() throws Exception {
        final String TEST_NAME = "test242OrgScummBarAddTransform";
        PrismObject<OrgType> org = getObject(OrgType.class, ORG_SCUMM_BAR_OID);

        Task task = taskManager.createTaskInstance(TEST_NAME);
        PrismObjectWrapper<OrgType> objectWrapper = createObjectWrapper(task, org, ItemStatus.NOT_CHANGED);
        IntegrationTestTools.display("Wrapper before", objectWrapper);

        PrismContainerValueWrapper<OrgType> mainContainerValueWrapper = objectWrapper.getValue();
        PrismContainerWrapper<Containerable> transformContainerWrapper = mainContainerValueWrapper.findContainer(extensionPath(PIRACY_TRANSFORM));
        List<PrismContainerValueWrapper<Containerable>> transformValueWrappers = transformContainerWrapper.getValues();
        assertEquals("Unexpecter number of transform value wrappers", 3, transformValueWrappers.size());

        ModelServiceLocator modelServiceLocator = getServiceLocator(task);
        WrapperContext context = new WrapperContext(task, task.getResult());
        context.setShowEmpty(true);
        context.setCreateIfEmpty(true);

        PrismContainerValueWrapper<Containerable> newContainerValueWrapper =  modelServiceLocator.createValueWrapper(transformContainerWrapper, transformContainerWrapper.getItem().createNewValue(), ValueStatus.ADDED, context);
        transformContainerWrapper.getValues().add(newContainerValueWrapper);
        modifyTransformProp(newContainerValueWrapper, PIRACY_PATTERN, "D");
        modifyTransformProp(newContainerValueWrapper, PIRACY_REPLACEMENT, "Doubloon");

        IntegrationTestTools.display("Wrapper after", objectWrapper);

        // WHEN
        when(TEST_NAME);
        ObjectDelta<OrgType> objectDelta = objectWrapper.getObjectDelta();

        // THEN
        then(TEST_NAME);
        display("Delta", objectDelta);
        ItemPath transformPath = ItemPath.create(ObjectType.F_EXTENSION, PIRACY_TRANSFORM);
        PrismAsserts.assertModifications(objectDelta, 1);
        ContainerDelta<Containerable> transfromDelta = (ContainerDelta) objectDelta.getModifications().iterator().next();
        assertTrue("Wrong container delta path. Expected "+transformPath+" but was "+transfromDelta.getPath(), transfromDelta.getPath().equivalent(transformPath));
        PrismAsserts.assertNoDelete(transfromDelta);
        PrismAsserts.assertNoReplace(transfromDelta);
        Collection<PrismContainerValue<Containerable>> valuesToAdd = transfromDelta.getValuesToAdd();
        assertEquals("Wrong number of values to add", 1, valuesToAdd.size());
        PrismContainerValue<Containerable> containerValueToAdd = valuesToAdd.iterator().next();
        assertEquals("Unexpected number of items in value to add", 2, containerValueToAdd.size());
        PrismProperty<String> patternProp = (PrismProperty) containerValueToAdd.findItem(PIRACY_PATTERN);
        PrismAsserts.assertPropertyValue(patternProp, "D");
        PrismProperty<String> replacementProp = (PrismProperty) containerValueToAdd.findItem(PIRACY_REPLACEMENT);
        PrismAsserts.assertPropertyValue(replacementProp, "Doubloon");

        OperationResult result = task.getResult();
        executeChanges(objectDelta, null, task, result);

        assertSuccess(result);

        PrismObject<OrgType> orgAfter = getObject(OrgType.class, ORG_MINISTRY_OF_RUM_OID);
        display("Org after", orgAfter);


    }

    /**
     * Ministry of rum has no extension container.
     */
    @Test
    public void test250OrgMinistryOrRumModifyTransformDescription() throws Exception {
        final String TEST_NAME = "test250OrgMinistryOrRumModifyTransformDescription";
        PrismObject<OrgType> org = getObject(OrgType.class, ORG_MINISTRY_OF_RUM_OID);

        Task task = taskManager.createTaskInstance(TEST_NAME);

        PrismObjectWrapper<OrgType> objectWrapper = createObjectWrapper(task, org, ItemStatus.NOT_CHANGED);
        IntegrationTestTools.display("Wrapper before", objectWrapper);

        PrismContainerValueWrapper<OrgType> mainContainerValueWrapper = objectWrapper.getValue();

        modifyPropertyWrapper(getServiceLocator(task), mainContainerValueWrapper, extensionPath(PIRACY_TRANSFORM_DESCRIPTION), "Whatever");

        IntegrationTestTools.display("Wrapper after", objectWrapper);

        // WHEN
        when(TEST_NAME);
        ObjectDelta<OrgType> objectDelta = objectWrapper.getObjectDelta();

        // THEN
        then(TEST_NAME);
        display("Delta", objectDelta);
        PrismAsserts.assertModifications(objectDelta, 1);

        ContainerDelta containerDelta = (ContainerDelta) objectDelta.getModifications().iterator().next();
        Collection<PrismContainerValue<ExtensionType>> valuesToAdd = containerDelta.getValuesToAdd();
        assertEquals("Unexpected values to add in extension delta", 1, valuesToAdd.size());
        PrismContainerValue<ExtensionType> extension = valuesToAdd.iterator().next();
        PrismProperty piracytransform = extension.findProperty(PIRACY_TRANSFORM_DESCRIPTION);
        assertEquals("Unexpected value in piracy transform attribute", "Whatever", piracytransform.getRealValue());

        OperationResult result = task.getResult();
        executeChanges(objectDelta, null, task, result);

        assertSuccess(result);

        PrismObject<OrgType> orgAfter = getObject(OrgType.class, ORG_MINISTRY_OF_RUM_OID);
        display("Org after", orgAfter);

    }

    private void modifyPropertyWrapper(ModelServiceLocator modelServiceLocator, PrismContainerValueWrapper<OrgType> mainContainerValueWrapper, ItemPath propPath, String newValue) throws SchemaException {
        PrismPropertyWrapper propertyWrapper = mainContainerValueWrapper.findProperty(propPath);
        List<PrismPropertyValueWrapper> values = propertyWrapper.getValues();
        if (values.size() == 1) {
            values.get(0).setRealValue(newValue);
        } else if (values.isEmpty()) {
            PrismPropertyValue<String> pval = prismContext.itemFactory().createPropertyValue(newValue);
            WrapperContext context = new WrapperContext(modelServiceLocator.getPageTask(), modelServiceLocator.getPageTask().getResult());
            context.setShowEmpty(true);
            context.setCreateIfEmpty(true);
            PrismValueWrapper newValueWrapper = modelServiceLocator.createValueWrapper(propertyWrapper, pval, ValueStatus.ADDED, context);
            newValueWrapper.setRealValue(newValue);
            propertyWrapper.getItem().add(pval);
            propertyWrapper.getValues().add(newValueWrapper);
        } else {
            throw new IllegalArgumentException("Cannot use on multivalue props");
        }
    }

    private PrismContainerValueWrapper<Containerable> findTransformValueWrapper(
            List<PrismContainerValueWrapper<Containerable>> transformValueWrappers, String pattern) throws SchemaException {
        for (PrismContainerValueWrapper<Containerable> transformValueWrapper: transformValueWrappers) {
            PrismPropertyWrapper<String> patternPropWrapper = transformValueWrapper.findProperty(PIRACY_PATTERN);
            PrismProperty<String> patternProperty = patternPropWrapper.getItem();
            if (pattern.equals(patternProperty.getRealValue())) {
                return transformValueWrapper;
            }
        }
        return null;
    }

    private void modifyTransformProp(PrismContainerValueWrapper<Containerable> transformValueWrapper, ItemName prop, String newReplacement) throws SchemaException {
        PrismPropertyWrapper<String> replacementPropWrapper = transformValueWrapper.findProperty(prop);
        List<PrismPropertyValueWrapper<String>> values = replacementPropWrapper.getValues();
        PrismPropertyValue<String> prismValue = (PrismPropertyValue<String>) values.get(0).getNewValue();
        prismValue.setValue(newReplacement);
    }

    /**
     * MID-3126
     */
    @Test
    public void test800EditSchemaJackPropReadAllModifySomeUser() throws Exception {
        final String TEST_NAME = "test800EditSchemaJackPropReadAllModifySomeUser";
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_PROP_READ_ALL_MODIFY_SOME_USER_OID);
        login(USER_JACK_USERNAME);

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("user before", user);

        // WHEN
        when(TEST_NAME);

        PrismObjectWrapper<UserType> objectWrapper = createObjectWrapper(getTestTask(), user, ItemStatus.NOT_CHANGED);

        // THEN
        then(TEST_NAME);

        IntegrationTestTools.display("Wrapper after", objectWrapper);
        assertEquals("Wrong object wrapper readOnly", Boolean.FALSE, (Boolean)objectWrapper.isReadOnly());

        PrismContainerValueWrapper<UserType> mainContainerValueWrapper = objectWrapper.getValue();

        PrismPropertyWrapper<PolyString> nameWrapper = mainContainerValueWrapper.findProperty(UserType.F_NAME);
        assertEquals("Wrong name readOnly", Boolean.TRUE, (Boolean)nameWrapper.isReadOnly()); // Is this OK?
        assertEquals("Wrong name visible", Boolean.TRUE, (Boolean)nameWrapper.isVisible(mainContainerValueWrapper, null));
        assertEquals("Wrong name definition.canRead", Boolean.TRUE, (Boolean)nameWrapper.canRead());
        assertEquals("Wrong name definition.canAdd", Boolean.FALSE, (Boolean)nameWrapper.canAdd());
        assertEquals("Wrong name definition.canModify", Boolean.FALSE, (Boolean)nameWrapper.canModify());

        PrismPropertyWrapper<PolyString> givenNameWrapper = mainContainerValueWrapper.findProperty(UserType.F_GIVEN_NAME);
        assertEquals("Wrong givenName readOnly", Boolean.TRUE, (Boolean)givenNameWrapper.isReadOnly()); // Is this OK?
        assertEquals("Wrong givenName visible", Boolean.TRUE, (Boolean)givenNameWrapper.isVisible(mainContainerValueWrapper, null));
        assertEquals("Wrong givenName definition.canRead", Boolean.TRUE, (Boolean)givenNameWrapper.canRead());
        assertEquals("Wrong givenName definition.canAdd", Boolean.FALSE, (Boolean)givenNameWrapper.canAdd());
        assertEquals("Wrong givenName definition.canModify", Boolean.FALSE, (Boolean)givenNameWrapper.canModify());

        PrismPropertyWrapper<PolyString> fullNameWrapper = mainContainerValueWrapper.findProperty(UserType.F_FULL_NAME);
        assertEquals("Wrong fullName readOnly", Boolean.FALSE, (Boolean)fullNameWrapper.isReadOnly());
        assertEquals("Wrong fullName visible", Boolean.TRUE, (Boolean)fullNameWrapper.isVisible(mainContainerValueWrapper, null));
        assertEquals("Wrong fullName definition.canRead", Boolean.TRUE, (Boolean)fullNameWrapper.canRead());
        assertEquals("Wrong fullName definition.canAdd", Boolean.FALSE, (Boolean)fullNameWrapper.canAdd());
        assertEquals("Wrong fullName definition.canModify", Boolean.TRUE, (Boolean)fullNameWrapper.canModify());

        PrismPropertyWrapper<String> additionalNameWrapper = mainContainerValueWrapper.findProperty(UserType.F_ADDITIONAL_NAME);
        assertEquals("Wrong additionalName readOnly", Boolean.TRUE, (Boolean)additionalNameWrapper.isReadOnly()); // Is this OK?
        assertEquals("Wrong additionalName visible", Boolean.FALSE, (Boolean)additionalNameWrapper.isVisible(mainContainerValueWrapper, null)); // not visible, because it is empty
        assertEquals("Wrong additionalName definition.canRead", Boolean.TRUE, (Boolean)additionalNameWrapper.canRead());
        assertEquals("Wrong additionalName definition.canAdd", Boolean.FALSE, (Boolean)additionalNameWrapper.canAdd());
        assertEquals("Wrong additionalName definition.canModify", Boolean.FALSE, (Boolean)additionalNameWrapper.canModify());

        PrismPropertyWrapper<String> localityNameWrapper = mainContainerValueWrapper.findProperty(UserType.F_LOCALITY);
        assertEquals("Wrong locality readOnly", Boolean.TRUE, (Boolean)localityNameWrapper.isReadOnly());
        assertEquals("Wrong locality visible", Boolean.TRUE, (Boolean)localityNameWrapper.isVisible(mainContainerValueWrapper, null));
        assertEquals("Wrong locality definition.canRead", Boolean.TRUE, (Boolean)localityNameWrapper.canRead());
        assertEquals("Wrong locality definition.canAdd", Boolean.FALSE, (Boolean)localityNameWrapper.canAdd());
        assertEquals("Wrong locality definition.canModify", Boolean.FALSE, (Boolean)localityNameWrapper.canModify());

        // WHEN
        mainContainerValueWrapper.setShowEmpty(true);

        // THEN
        additionalNameWrapper = mainContainerValueWrapper.findProperty(UserType.F_ADDITIONAL_NAME);
        assertEquals("Wrong additionalName visible", Boolean.TRUE, (Boolean)additionalNameWrapper.isVisible(mainContainerValueWrapper, null)); // visible, because show empty

    }

    /**
     * MID-3126
     */
    @Test
    public void test802EditSchemaJackPropReadSomeModifySomeUser() throws Exception {
        final String TEST_NAME = "test800EditSchemaJackPropReadAllModifySomeUser";
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_PROP_READ_SOME_MODIFY_SOME_USER_OID);
        login(USER_JACK_USERNAME);

        Task task = getTestTask();
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("user before", user);

        // WHEN
        when(TEST_NAME);

        PrismObjectWrapper<UserType> objectWrapper = createObjectWrapper(task, user, ItemStatus.NOT_CHANGED);

        // THEN
        then(TEST_NAME);

        IntegrationTestTools.display("Wrapper after", objectWrapper);
        assertEquals("Wrong object wrapper readOnly", Boolean.FALSE, (Boolean)objectWrapper.isReadOnly());

        PrismContainerValueWrapper<UserType> mainContainerValueWrapper = objectWrapper.getValue();
        PrismPropertyWrapper nameWrapper = mainContainerValueWrapper.findProperty(UserType.F_NAME);
        assertEquals("Wrong name readOnly", Boolean.TRUE, (Boolean)nameWrapper.isReadOnly());
        assertEquals("Wrong name visible", Boolean.TRUE, (Boolean)nameWrapper.isVisible(mainContainerValueWrapper, null));
        assertEquals("Wrong name definition.canRead", Boolean.TRUE, (Boolean)nameWrapper.canRead());
        assertEquals("Wrong name definition.canAdd", Boolean.FALSE, (Boolean)nameWrapper.canAdd());
        assertEquals("Wrong name definition.canModify", Boolean.FALSE, (Boolean)nameWrapper.canModify());

        //no access to given name, wrapper should not be even generated
        PrismPropertyWrapper givenNameWrapper = mainContainerValueWrapper.findProperty(UserType.F_GIVEN_NAME);
        assertNull("Unexpected givenName wrapper ", givenNameWrapper);
//        assertEquals("Wrong givenName readOnly", Boolean.TRUE, (Boolean)givenNameWrapper.isReadOnly());
//        // Emphasized property. But the role given no access to this. Therefore is should not be visible.
//        // MID-3206
//        assertEquals("Wrong givenName visible", Boolean.FALSE, (Boolean)givenNameWrapper.isVisible(mainContainerValueWrapper, null));
//        assertEquals("Wrong givenName definition.canRead", Boolean.FALSE, (Boolean)givenNameWrapper.canRead());
//        assertEquals("Wrong givenName definition.canAdd", Boolean.FALSE, (Boolean)givenNameWrapper.canAdd());
//        assertEquals("Wrong givenName definition.canModify", Boolean.FALSE, (Boolean)givenNameWrapper.canModify());

        PrismPropertyWrapper fullNameWrapper = mainContainerValueWrapper.findProperty(UserType.F_FULL_NAME);
        assertEquals("Wrong fullName readOnly", Boolean.FALSE, (Boolean)fullNameWrapper.isReadOnly());
        assertEquals("Wrong fullName visible", Boolean.TRUE, (Boolean)fullNameWrapper.isVisible(mainContainerValueWrapper, null));
        assertEquals("Wrong fullName definition.canRead", Boolean.TRUE, (Boolean)fullNameWrapper.canRead());
        assertEquals("Wrong fullName definition.canAdd", Boolean.FALSE, (Boolean)fullNameWrapper.canAdd());
        assertEquals("Wrong fullName definition.canModify", Boolean.TRUE, (Boolean)fullNameWrapper.canModify());

        // not created because of unsufficient authZ
        PrismPropertyWrapper additionalNameWrapper = mainContainerValueWrapper.findProperty(UserType.F_ADDITIONAL_NAME);
        assertNull("Unexpeced aditional name wrraper", additionalNameWrapper);
//        assertEquals("Wrong additionalName readOnly", Boolean.FALSE, (Boolean)additionalNameWrapper.isReadOnly());
//        assertEquals("Wrong additionalName visible", Boolean.FALSE, (Boolean)additionalNameWrapper.isVisible(mainContainerValueWrapper, null));
//        assertEquals("Wrong additionalName definition.canRead", Boolean.FALSE, (Boolean)additionalNameWrapper.canRead());
//        assertEquals("Wrong additionalName definition.canAdd", Boolean.FALSE, (Boolean)additionalNameWrapper.canAdd());
//        assertEquals("Wrong additionalName definition.canModify", Boolean.TRUE, (Boolean)additionalNameWrapper.canModify());

        // no access to property, should not be generated
        PrismPropertyWrapper localityNameWrapper = mainContainerValueWrapper.findProperty(UserType.F_LOCALITY);
        assertNull("Unexpected locality wrapper", localityNameWrapper);

//        assertEquals("Wrong locality readOnly", Boolean.TRUE, (Boolean)localityNameWrapper.isReadOnly()); // Is this OK?
//        assertEquals("Wrong locality visible", Boolean.FALSE, (Boolean)localityNameWrapper.isVisible(mainContainerValueWrapper, null));
//        assertEquals("Wrong locality definition.canRead", Boolean.FALSE, (Boolean)localityNameWrapper.canRead());
//        assertEquals("Wrong locality definition.canAdd", Boolean.FALSE, (Boolean)localityNameWrapper.canAdd());
//        assertEquals("Wrong locality definition.canModify", Boolean.FALSE, (Boolean)localityNameWrapper.canModify());
    }

    private <C extends Containerable> void assertItemWrapperFullConrol(PrismContainerValueWrapper<C> containerWrapper, ItemName propName,
                                                                       boolean visible) throws SchemaException {
        ItemWrapper itemWrapper = containerWrapper.findItem(propName, ItemWrapper.class);
        assertEquals("Wrong "+propName+" readOnly", Boolean.FALSE, (Boolean)itemWrapper.isReadOnly());
        assertEquals("Wrong "+propName+" visible", visible, itemWrapper.isVisible(containerWrapper, w -> ItemVisibility.AUTO));
        assertEquals("Wrong "+propName+" definition.canRead", Boolean.TRUE, (Boolean)itemWrapper.canRead());
        assertEquals("Wrong "+propName+" definition.canAdd", Boolean.TRUE, (Boolean)itemWrapper.canAdd());
        assertEquals("Wrong "+propName+" definition.canModify", Boolean.TRUE, (Boolean)itemWrapper.canModify());
    }

    private <F extends FocusType> void assertItemWrapperProcessing(PrismContainerValueWrapper<F> containerWrapper,
                                                                   ItemPath propName, ItemProcessing expectedProcessing) throws SchemaException {
        ItemWrapper itemWrapper = containerWrapper.findItem(propName, ItemWrapper.class);
        if (expectedProcessing == ItemProcessing.IGNORE) {
            assertNull("Unexpected ignored item wrapper for "+propName, itemWrapper);
        } else {
            assertEquals("Wrong processing in item wrapper for "+propName, expectedProcessing, itemWrapper.getProcessing());
        }
    }


    private void cleanupAutzTest(String userOid) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
            ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException, IOException {
        login(userAdministrator);
        unassignAllRoles(userOid);
    }

    private String getString(String key) {
        return localizationService.translate(key, null, Locale.US, key);
    }
}
