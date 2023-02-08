/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import static com.evolveum.midpoint.model.test.CommonInitialObjects.*;
import static com.evolveum.midpoint.schema.constants.MidPointConstants.NS_RI;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;

import java.io.File;
import java.util.List;

import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.test.DummyResourceContoller;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.namespace.QName;

/**
 * Tests policy rules attached to projections.
 *
 * (Currently, all projection-related event marks are tested here.)
 */
public class TestProjectionPolicyRules extends AbstractLensTest {

    private static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "lens/policy/projection");

    private static final String ATTR_SAM_ACCOUNT_NAME = "samAccountName";
    private static final String ATTR_MEMBER_OF_ORG = "memberOfOrg";
    private static final ItemName ASSOCIATION_GROUP = new ItemName(NS_RI, "group");
    private static final ItemName ASSOCIATION_ORG = new ItemName(NS_RI, "org");
    private static final QName CUSTOM_ORG_OBJECT_CLASS = new QName(NS_RI, DummyResourceContoller.OBJECTCLASS_ORG_LOCAL_PART);

    private ShadowType wheelShadow;
    private ShadowType topOrgShadow;

    private static final DummyTestResource RESOURCE_DUMMY_EVENT_MARKS = new DummyTestResource(
            TEST_DIR, "resource-dummy-event-marks.xml", "b951c40b-2f57-4f1d-a881-8ba37e973c11", "event-marks",
            controller -> {
                controller.populateWithDefaultSchema();
                controller.addAttrDef(
                        controller.getAccountObjectClass(), ATTR_SAM_ACCOUNT_NAME, String.class, false, false);
                controller.addAttrDef(
                        controller.getAccountObjectClass(), ATTR_MEMBER_OF_ORG, String.class, false, true);
            });

    @BeforeMethod
    public void onNativeOnly() {
        skipIfNotNativeRepository();
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        CommonInitialObjects.addMarks(this, initTask, initResult);

        RESOURCE_DUMMY_EVENT_MARKS.initAndTest(this, initTask, initResult);

        RESOURCE_DUMMY_EVENT_MARKS.controller.addGroup("wheel");
        var groupShadows = provisioningService.searchObjects(
                ShadowType.class,
                Resource.of(RESOURCE_DUMMY_EVENT_MARKS.get())
                        .queryFor(RI_GROUP_OBJECT_CLASS)
                        .build(),
                null, initTask, initResult);
        wheelShadow = MiscUtil.extractSingletonRequired(groupShadows).asObjectable();

        RESOURCE_DUMMY_EVENT_MARKS.controller.addOrgTop();
        var orgShadows = provisioningService.searchObjects(
                ShadowType.class,
                Resource.of(RESOURCE_DUMMY_EVENT_MARKS.get())
                        .queryFor(CUSTOM_ORG_OBJECT_CLASS)
                        .build(),
                null, initTask, initResult);
        topOrgShadow = MiscUtil.extractSingletonRequired(orgShadows).asObjectable();
    }

    /**
     * Check that {@link SystemObjectsType#MARK_PROJECTION_DEACTIVATED} is set correctly.
     */
    @Test
    public void test100DisableAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("a user with an account exists");
        UserType user = createUserWithAccount("test100", null, task, result);

        switchToSimulationMode(task);

        when("user and account are disabled (in simulation mode)");
        ObjectDelta<UserType> disableDelta = deltaFor(UserType.class)
                .item(PATH_ACTIVATION_ADMINISTRATIVE_STATUS).replace(ActivationStatusType.DISABLED)
                .asObjectDelta(user.getOid());

        LensContext<UserType> disableContext = runClockwork(disableDelta, null, task, result);

        then("marks are set correctly");
        // @formatter:off
        assertModelContext(disableContext, "disable context")
                .focusContext()
                    .assertEventMarks(MARK_FOCUS_DEACTIVATED)
                .end()
                .projectionContexts()
                    .single()
                        .assertEventMarks(MARK_PROJECTION_DEACTIVATED);
    }

    /**
     * Check that {{@link SystemObjectsType#MARK_PROJECTION_ACTIVATED} is set correctly.
     */
    @Test
    public void test105EnableAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("a (disabled) user with an account exists");
        UserType user = createUserWithAccount("test105", ActivationStatusType.DISABLED, task, result);

        switchToSimulationMode(task);

        when("user and account are enabled");
        ObjectDelta<UserType> enableDelta = deltaFor(UserType.class)
                .item(PATH_ACTIVATION_ADMINISTRATIVE_STATUS).replace() // intentionally without specifying "ENABLED" explicitly
                .asObjectDelta(user.getOid());
        LensContext<UserType> enableContext = runClockwork(enableDelta, null, task, result);

        then("marks are set correctly");
        // @formatter:off
        assertModelContext(enableContext, "enable context")
                .focusContext()
                    .assertEventMarks(MARK_FOCUS_ACTIVATED)
                .end()
                .projectionContexts()
                    .single()
                        .assertEventMarks(MARK_PROJECTION_ACTIVATED);
        // @formatter:on
    }

    private UserType createUserWithAccount(String name, ActivationStatusType status, Task task, OperationResult result)
            throws CommonException {
        UserType user = new UserType()
                .name(name)
                .activation(new ActivationType().administrativeStatus(status))
                .assignment(new AssignmentType()
                        .construction(new ConstructionType()
                                .resourceRef(RESOURCE_DUMMY_EVENT_MARKS.oid, ResourceType.COMPLEX_TYPE)));
        addObject(user, task, result);
        return repositoryService.getObject(UserType.class, user.getOid(), null, result).asObjectable();
    }

    /**
     * Check that {@link SystemObjectsType#MARK_FOCUS_RENAMED} and {@link SystemObjectsType#MARK_PROJECTION_RENAMED}
     * are set correctly.
     */
    @Test
    public void test110RenameAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("a user with an account exists");
        UserType user = createUserWithAccount("test110", null, task, result);

        switchToSimulationMode(task);

        when("user and account are renamed");
        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_NAME).replace(PolyString.fromOrig("renamed"))
                .asObjectDelta(user.getOid());
        LensContext<UserType> lensContext = runClockwork(delta, null, task, result);

        then("marks are set correctly");
        // @formatter:off
        assertModelContext(lensContext, "context")
                .focusContext()
                    .assertEventMarks(MARK_FOCUS_RENAMED)
                .end()
                .projectionContexts()
                    .single()
                        .assertEventMarks(MARK_PROJECTION_RENAMED, MARK_PROJECTION_IDENTIFIER_CHANGED);
        // @formatter:on
    }

    /**
     * Check that {@link SystemObjectsType#MARK_PROJECTION_IDENTIFIER_CHANGED} is set correctly.
     */
    @Test
    public void test120ChangeNonNamingIdentifier() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("a user with an account exists");
        UserType user = createUserWithAccount("test120", null, task, result);

        switchToSimulationMode(task);

        when("account non-naming identifier is changed");
        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_EMPLOYEE_NUMBER).replace("new-emp-id")
                .asObjectDelta(user.getOid());
        LensContext<UserType> lensContext = runClockwork(delta, null, task, result);

        then("marks are set correctly");
        // @formatter:off
        assertModelContext(lensContext, "context")
                .focusContext()
                    .assertEventMarks()
                .end()
                .projectionContexts()
                    .single()
                        .assertEventMarks(MARK_PROJECTION_IDENTIFIER_CHANGED);
        // @formatter:on
    }

    /**
     * Check that {@link SystemObjectsType#MARK_PROJECTION_ENTITLEMENT_CHANGED} is set correctly.
     */
    @Test
    public void test130ChangeEntitlement() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("a user with an account exists");
        UserType user = createUserWithAccount("test130", null, task, result);

        switchToSimulationMode(task);

        when("account group membership is changed");
        ObjectDelta<ShadowType> delta = Resource.of(RESOURCE_DUMMY_EVENT_MARKS.get())
                .deltaFor(RI_ACCOUNT_OBJECT_CLASS)
                .item(ShadowType.F_ASSOCIATION)
                .add(new ShadowAssociationType()
                        .name(ASSOCIATION_GROUP)
                        .shadowRef(ObjectTypeUtil.createObjectRef(wheelShadow, ORG_DEFAULT)))
                .asObjectDelta(user.getLinkRef().get(0).getOid());
        LensContext<UserType> lensContext = runClockwork(List.of(delta), null, task, result);

        then("marks are set correctly");
        // @formatter:off
        assertModelContext(lensContext, "context")
                .focusContext()
                    .assertEventMarks()
                .end()
                .projectionContexts()
                    .single()
                        .assertEventMarks(MARK_PROJECTION_ENTITLEMENT_CHANGED);
        // @formatter:on
    }

    /**
     * Check that {@link SystemObjectsType#MARK_PROJECTION_ENTITLEMENT_CHANGED} is not set when non-entitlement association
     * is changed.
     */
    @Test
    public void test140ChangeNonEntitlementAssociation() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("a user with an account exists");
        UserType user = createUserWithAccount("test140", null, task, result);

        switchToSimulationMode(task);

        when("account org membership is changed");
        ObjectDelta<ShadowType> delta = Resource.of(RESOURCE_DUMMY_EVENT_MARKS.get())
                .deltaFor(RI_ACCOUNT_OBJECT_CLASS)
                .item(ShadowType.F_ASSOCIATION)
                .add(new ShadowAssociationType()
                        .name(ASSOCIATION_ORG)
                        .shadowRef(ObjectTypeUtil.createObjectRef(topOrgShadow, ORG_DEFAULT)))
                .asObjectDelta(user.getLinkRef().get(0).getOid());
        LensContext<UserType> lensContext = runClockwork(List.of(delta), null, task, result);

        then("'entitlement changed' mark is not present");
        // @formatter:off
        assertModelContext(lensContext, "context")
                .focusContext()
                    .assertEventMarks()
                .end()
                .projectionContexts()
                    .single()
                        .assertEventMarks(); // "org" is not an entitlement
        // @formatter:on
    }

    /**
     * Check that {@link SystemObjectsType#MARK_PROJECTION_PASSWORD_CHANGED} is set correctly.
     */
    @Test
    public void test150ChangeAccountPassword() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("a user with an account exists");
        UserType user = createUserWithAccount("test150", null, task, result);

        switchToSimulationMode(task);

        when("account password is changed");
        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(PATH_PASSWORD_VALUE).replace(protector.encryptString("secret"))
                .asObjectDelta(user.getOid());
        LensContext<UserType> lensContext = runClockwork(delta, null, task, result);

        then("marks are set correctly");
        // @formatter:off
        assertModelContext(lensContext, "context")
                .focusContext()
                    .assertEventMarks()
                .end()
                .projectionContexts()
                    .single()
                        .assertEventMarks(MARK_PROJECTION_PASSWORD_CHANGED);
        // @formatter:on
    }

    /** Switching task to a simulation mode. Otherwise, event marks would not be applied. */
    private static void switchToSimulationMode(Task task) {
        task.setExecutionMode(TaskExecutionMode.SIMULATED_PRODUCTION);
    }
}
