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

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.test.DummyResourceContoller;

import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

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
 * (Currently, all projection-related event tags are tested here.)
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

    private static final DummyTestResource RESOURCE_DUMMY_TAGS = new DummyTestResource(
            TEST_DIR, "resource-dummy-tags.xml", "b951c40b-2f57-4f1d-a881-8ba37e973c11", "tags",
            controller -> {
                controller.populateWithDefaultSchema();
                controller.addAttrDef(
                        controller.getAccountObjectClass(), ATTR_SAM_ACCOUNT_NAME, String.class, false, false);
                controller.addAttrDef(
                        controller.getAccountObjectClass(), ATTR_MEMBER_OF_ORG, String.class, false, true);
            });

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        CommonInitialObjects.addTags(this, initResult);

        RESOURCE_DUMMY_TAGS.initAndTest(this, initTask, initResult);

        RESOURCE_DUMMY_TAGS.controller.addGroup("wheel");
        var groupShadows = provisioningService.searchObjects(
                ShadowType.class,
                Resource.of(RESOURCE_DUMMY_TAGS.get())
                        .queryFor(RI_GROUP_OBJECT_CLASS)
                        .build(),
                null, initTask, initResult);
        wheelShadow = MiscUtil.extractSingletonRequired(groupShadows).asObjectable();

        RESOURCE_DUMMY_TAGS.controller.addOrgTop();
        var orgShadows = provisioningService.searchObjects(
                ShadowType.class,
                Resource.of(RESOURCE_DUMMY_TAGS.get())
                        .queryFor(CUSTOM_ORG_OBJECT_CLASS)
                        .build(),
                null, initTask, initResult);
        topOrgShadow = MiscUtil.extractSingletonRequired(orgShadows).asObjectable();
    }

    /**
     * Check that {@link SystemObjectsType#TAG_PROJECTION_DISABLED} and {@link SystemObjectsType#TAG_PROJECTION_ENABLED}
     * are set correctly.
     */
    @Test
    public void test100DisableAndEnableAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("a user with an account exists");
        UserType user = createUserWithAccount("test100", task, result);

        when("user and account are disabled");
        ObjectDelta<UserType> disableDelta = deltaFor(UserType.class)
                .item(PATH_ACTIVATION_ADMINISTRATIVE_STATUS).replace(ActivationStatusType.DISABLED)
                .asObjectDelta(user.getOid());
        LensContext<UserType> disableContext = runClockwork(disableDelta, null, task, result);

        then("tags are set correctly");
        // @formatter:off
        assertModelContext(disableContext, "disable context")
                .focusContext()
                    .assertEventTags(TAG_FOCUS_DISABLED)
                .end()
                .projectionContexts()
                    .single()
                        .assertEventTags(TAG_PROJECTION_DISABLED);

        when("user and account are enabled");
        ObjectDelta<UserType> enableDelta = deltaFor(UserType.class)
                .item(PATH_ACTIVATION_ADMINISTRATIVE_STATUS).replace() // intentionally without specifying "ENABLED" explicitly
                .asObjectDelta(user.getOid());
        LensContext<UserType> enableContext = runClockwork(enableDelta, null, task, result);

        then("tags are set correctly");
        // @formatter:off
        assertModelContext(enableContext, "enable context")
                .focusContext()
                    .assertEventTags(TAG_FOCUS_ENABLED)
                .end()
                .projectionContexts()
                    .single()
                        .assertEventTags(TAG_PROJECTION_ENABLED);
        // @formatter:on
    }

    private UserType createUserWithAccount(String name, Task task, OperationResult result) throws CommonException {
        UserType user = new UserType()
                .name(name)
                .assignment(new AssignmentType()
                        .construction(new ConstructionType()
                                .resourceRef(RESOURCE_DUMMY_TAGS.oid, ResourceType.COMPLEX_TYPE)));
        addObject(user, task, result);
        return repositoryService.getObject(UserType.class, user.getOid(), null, result).asObjectable();
    }

    /**
     * Check that {@link SystemObjectsType#TAG_FOCUS_NAME_CHANGED} and {@link SystemObjectsType#TAG_PROJECTION_NAME_CHANGED}
     * are set correctly.
     */
    @Test
    public void test110RenameAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("a user with an account exists");
        UserType user = createUserWithAccount("test110", task, result);

        when("user and account are renamed");
        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_NAME).replace(PolyString.fromOrig("renamed"))
                .asObjectDelta(user.getOid());
        LensContext<UserType> lensContext = runClockwork(delta, null, task, result);

        then("tags are set correctly");
        if (areTagsSupported()) {
            // @formatter:off
            assertModelContext(lensContext, "context")
                    .focusContext()
                        .assertEventTags(TAG_FOCUS_NAME_CHANGED)
                    .end()
                    .projectionContexts()
                        .single()
                            .assertEventTags(TAG_PROJECTION_NAME_CHANGED, TAG_PROJECTION_IDENTIFIER_CHANGED);
            // @formatter:on
        }
    }

    /**
     * Check that {@link SystemObjectsType#TAG_PROJECTION_IDENTIFIER_CHANGED} is set correctly.
     */
    @Test
    public void test120ChangeNonNamingIdentifier() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("a user with an account exists");
        UserType user = createUserWithAccount("test120", task, result);

        when("account non-naming identifier is changed");
        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_EMPLOYEE_NUMBER).replace("new-emp-id")
                .asObjectDelta(user.getOid());
        LensContext<UserType> lensContext = runClockwork(delta, null, task, result);

        then("tags are set correctly");
        if (areTagsSupported()) {
            // @formatter:off
            assertModelContext(lensContext, "context")
                    .focusContext()
                        .assertEventTags()
                    .end()
                    .projectionContexts()
                        .single()
                            .assertEventTags(TAG_PROJECTION_IDENTIFIER_CHANGED);
            // @formatter:on
        }
    }

    /**
     * Check that {@link SystemObjectsType#TAG_PROJECTION_ENTITLEMENT_CHANGED} is set correctly.
     */
    @Test
    public void test130ChangeEntitlement() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("a user with an account exists");
        UserType user = createUserWithAccount("test130", task, result);

        when("account group membership is changed");
        ObjectDelta<ShadowType> delta = Resource.of(RESOURCE_DUMMY_TAGS.get())
                .deltaFor(RI_ACCOUNT_OBJECT_CLASS)
                .item(ShadowType.F_ASSOCIATION)
                .add(new ShadowAssociationType()
                        .name(ASSOCIATION_GROUP)
                        .shadowRef(ObjectTypeUtil.createObjectRef(wheelShadow, ORG_DEFAULT)))
                .asObjectDelta(user.getLinkRef().get(0).getOid());
        LensContext<UserType> lensContext = runClockwork(List.of(delta), null, task, result);

        then("tags are set correctly");
        if (areTagsSupported()) {
            // @formatter:off
            assertModelContext(lensContext, "context")
                    .focusContext()
                        .assertEventTags()
                    .end()
                    .projectionContexts()
                        .single()
                            .assertEventTags(TAG_PROJECTION_ENTITLEMENT_CHANGED);
            // @formatter:on
        }
    }

    /**
     * Check that {@link SystemObjectsType#TAG_PROJECTION_ENTITLEMENT_CHANGED} is not set when non-entitlement association
     * is changed.
     */
    @Test
    public void test140ChangeNonEntitlementAssociation() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("a user with an account exists");
        UserType user = createUserWithAccount("test140", task, result);

        when("account org membership is changed");
        ObjectDelta<ShadowType> delta = Resource.of(RESOURCE_DUMMY_TAGS.get())
                .deltaFor(RI_ACCOUNT_OBJECT_CLASS)
                .item(ShadowType.F_ASSOCIATION)
                .add(new ShadowAssociationType()
                        .name(ASSOCIATION_ORG)
                        .shadowRef(ObjectTypeUtil.createObjectRef(topOrgShadow, ORG_DEFAULT)))
                .asObjectDelta(user.getLinkRef().get(0).getOid());
        LensContext<UserType> lensContext = runClockwork(List.of(delta), null, task, result);

        then("'entitlement changed' tag is not present");
        if (areTagsSupported()) {
            // @formatter:off
            assertModelContext(lensContext, "context")
                    .focusContext()
                        .assertEventTags()
                    .end()
                    .projectionContexts()
                        .single()
                            .assertEventTags(); // "org" is not an entitlement
            // @formatter:on
        }
    }

    /**
     * Check that {@link SystemObjectsType#TAG_PROJECTION_PASSWORD_CHANGED} is set correctly.
     */
    @Test
    public void test150ChangeAccountPassword() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("a user with an account exists");
        UserType user = createUserWithAccount("test150", task, result);

        when("account password is changed");
        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(PATH_PASSWORD_VALUE).replace(protector.encryptString("secret"))
                .asObjectDelta(user.getOid());
        LensContext<UserType> lensContext = runClockwork(delta, null, task, result);

        then("tags are set correctly");
        if (areTagsSupported()) {
            // @formatter:off
            assertModelContext(lensContext, "context")
                    .focusContext()
                        .assertEventTags()
                    .end()
                    .projectionContexts()
                        .single()
                            .assertEventTags(TAG_PROJECTION_PASSWORD_CHANGED);
            // @formatter:on
        }
    }
}
