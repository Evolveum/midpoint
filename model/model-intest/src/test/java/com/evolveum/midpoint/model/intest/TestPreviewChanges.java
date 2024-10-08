/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.model.api.context.*;
import com.evolveum.midpoint.model.api.visualizer.ModelContextVisualization;
import com.evolveum.midpoint.model.api.visualizer.Visualization;
import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.model.test.TestSimulationResult;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.InfraItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.*;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static com.evolveum.midpoint.model.test.CommonInitialObjects.MARK_PROJECTION_ACTIVATED;
import static com.evolveum.midpoint.model.test.CommonInitialObjects.MARK_PROJECTION_RESOURCE_OBJECT_AFFECTED;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;
import static com.evolveum.midpoint.schema.internals.InternalsConfig.isShadowCachingFullByDefault;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRef;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRefWithFullObject;
import static com.evolveum.midpoint.test.DummyResourceContoller.*;
import static com.evolveum.midpoint.test.util.MidPointAsserts.assertSerializable;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType.ACCOUNT;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

/**
 * Tests "preview changes" as well as a couple of unrelated features of {@link ModelInteractionService}.
 *
 * Some of the changes are also executed in simulation mode (new in 4.7).
 *
 * @author semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestPreviewChanges extends AbstractInitializedModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/preview");

    // LEMON dummy resource has a STRICT dependency on default dummy resource
    private static final DummyTestResource RESOURCE_DUMMY_LEMON = new DummyTestResource(TEST_DIR,
            "resource-dummy-lemon.xml", "10000000-0000-0000-0000-000000000504", "lemon",
            DummyResourceContoller::extendSchemaPirate);

    /** Has both inbound and outbound mappings. */
    private static final DummyTestResource RESOURCE_DUMMY_BIDIRECTIONAL = new DummyTestResource(TEST_DIR,
            "resource-dummy-bidirectional.xml", "40a0478a-42fe-43b5-b3c4-6b98c77a40e7", "bidirectional",
            DummyResourceContoller::extendSchemaPirate);

    /** For various ad-hoc purposes. */
    private static final DummyTestResource RESOURCE_DUMMY_MISC = new DummyTestResource(TEST_DIR,
            "resource-dummy-misc.xml", "6b71a242-2306-4bbc-a46e-9444c2f74823", "misc",
            DummyResourceContoller::extendSchemaPirate);

    private static final TestObject<UserType> USER_ROGERS = TestObject.file(TEST_DIR, "user-rogers.xml", "c0c010c0-d34d-b33f-f00d-11d2d2d2d22d");
    private static final File ACCOUNT_ROGERS_DUMMY_DEFAULT_FILE = new File(TEST_DIR, "account-rogers-dummy-default.xml");
    private static final File ACCOUNT_ROGERS_DUMMY_LEMON_FILE = new File(TEST_DIR, "account-rogers-dummy-lemon.xml");

    // The following are used in the last part of this class
    private static final TestObject<SecurityPolicyType> SECURITY_POLICY = TestObject.file(TEST_DIR, "security-policy.xml", "a013bf3e-68b2-42b7-923e-c4d55e40e486");
    private static final TestObject<ValuePolicyType> VALUE_POLICY_PASSWORD = TestObject.file(TEST_DIR, "value-policy-password.xml", "fd0d70ea-ef5a-4e20-8bf0-3b367ff85f1c");
    private static final TestObject<UserType> USER_JOE = TestObject.file(TEST_DIR, "user-joe.xml", "33f7ec34-4d5e-4640-8224-eb7d55ed86fa");
    private static final TestObject<RoleType> ROLE_SIMPLE = TestObject.file(TEST_DIR, "role-simple.xml", "e681fa58-cf53-452d-99d0-4f1377a06a54");
    private static final DummyTestResource RESOURCE_SIMPLE = new DummyTestResource(TEST_DIR, "resource-simple.xml", "64d8b7f9-28a0-43a9-bec7-d5a1a327a740",
            "resource-preview-simple", TestPreviewChanges::createSimpleAttributeDefinitions);

    private String accountGuybrushOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        CommonInitialObjects.addMarks(this, initTask, initResult);

        RESOURCE_DUMMY_LEMON.initAndTest(this, initTask, initResult);
        RESOURCE_DUMMY_MISC.initAndTest(this, initTask, initResult);
        RESOURCE_DUMMY_BIDIRECTIONAL.initAndTest(this, initTask, initResult);

        // Elaine is in inconsistent state. Account attributes do not match the mappings.
        // We do not want that here, as it would add noise to preview operations.
        reconcileUser(USER_ELAINE_OID, initTask, initResult);

        // Jack is not very consistent either. For example, the activation/effectiveStatus is not set.
        recomputeUser(USER_JACK_OID, initTask, initResult);

        RESOURCE_SIMPLE.initAndTest(this, initTask, initResult);
    }

    private static void createSimpleAttributeDefinitions(DummyResourceContoller controller) {

    }

    /**
     * Previews the account creation (in a variety of ways: single user delta, separate user/account deltas variations).
     */
    @Test
    public void test100ModifyUserAddAccountBundle() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        ObjectSource<PrismObject<ShadowType>> accountSource = () -> {
            try {
                return PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);
            } catch (SchemaException | IOException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        };

        Checkers checkers = new Checkers(
                modelContext -> assertAddAccount(modelContext, false),
                simulationResult -> assertAddAccount(simulationResult));

        modifyUserAddAccountImplicit(accountSource, checkers);
        modifyUserAddAccountExplicit(accountSource, checkers);
        modifyUserAddAccountImplicitExplicitSame(accountSource, checkers);
        modifyUserAddAccountImplicitExplicitSameReverse(accountSource, checkers);
        modifyUserAddAccountImplicitExplicitEqual(accountSource, checkers);
        modifyUserAddAccountImplicitExplicitEqualReverse(accountSource, checkers);
        modifyUserAddAccountImplicitExplicitNotEqual(accountSource);
        modifyUserAddAccountImplicitExplicitNotEqualReverse(accountSource);
    }

    /**
     * As {@link #test100ModifyUserAddAccountBundle()} but the account has no attributes here.
     * They should be computed by mappings.
     */
    @Test
    public void test101ModifyUserAddAccountNoAttributesBundle() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        ObjectSource<PrismObject<ShadowType>> accountSource = () -> {
            try {
                PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);
                account.removeContainer(ShadowType.F_ATTRIBUTES);
                return account;
            } catch (SchemaException | IOException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        };

        Checkers checkers = new Checkers(
                modelContext -> assertAddAccount(modelContext, true),
                simulationResult -> assertAddAccount(simulationResult));

        modifyUserAddAccountImplicit(accountSource, checkers);
        modifyUserAddAccountExplicit(accountSource, checkers);
        modifyUserAddAccountImplicitExplicitSame(accountSource, checkers);
        modifyUserAddAccountImplicitExplicitSameReverse(accountSource, checkers);
        modifyUserAddAccountImplicitExplicitEqual(accountSource, checkers);
        modifyUserAddAccountImplicitExplicitEqualReverse(accountSource, checkers);
        modifyUserAddAccountImplicitExplicitNotEqual(accountSource);
        modifyUserAddAccountImplicitExplicitNotEqualReverse(accountSource);
    }

    /** Adds an account by adding a `linkRef` with embedded shadow object. */
    private void modifyUserAddAccountImplicit(
            ObjectSource<PrismObject<ShadowType>> accountSource,
            Checkers checkers) throws Exception {
        Task task = createPlainTask("modifyUserAddAccountImplicit");
        OperationResult result = task.getResult();

        PrismObject<ShadowType> account = accountSource.get();
        ObjectDelta<UserType> userDelta = userLinkRefAddDelta(account);
        doPreview(
                List.of(userDelta),
                checkers, task, result);
    }

    /** Adds an account by providing explicit {@link ShadowType} `ADD` delta (along with the empty user delta). */
    private void modifyUserAddAccountExplicit(
            ObjectSource<PrismObject<ShadowType>> accountSource, Checkers checkers) throws Exception {
        Task task = createPlainTask("modifyUserAddAccountExplicit");
        OperationResult result = task.getResult();

        PrismObject<ShadowType> account = accountSource.get();
        ObjectDelta<UserType> userDelta = emptyUserDelta();
        ObjectDelta<ShadowType> shadowDelta = shadowAddDelta(account);
        doPreview(
                List.of(userDelta, shadowDelta),
                checkers, task, result);
    }

    /**
     * Adds an account by adding a `linkRef` with the shadow object and providing the shadow `ADD` delta with the same object.
     */
    private void modifyUserAddAccountImplicitExplicitSame(
            ObjectSource<PrismObject<ShadowType>> accountSource, Checkers checkers) throws Exception {
        Task task = createPlainTask("modifyUserAddAccountImplicitExplicitSame");
        OperationResult result = task.getResult();

        PrismObject<ShadowType> account = accountSource.get();
        ObjectDelta<UserType> userDelta = userLinkRefAddDelta(account);
        ObjectDelta<ShadowType> shadowDelta = shadowAddDelta(account);
        doPreview(
                List.of(userDelta, shadowDelta),
                checkers, task, result);
    }

    /**
     * Adds an account by adding a `linkRef` with the shadow object and providing the shadow `ADD` delta with the same object.
     * (In the reverse order - account delta first.)
     */
    private void modifyUserAddAccountImplicitExplicitSameReverse(
            ObjectSource<PrismObject<ShadowType>> accountSource, Checkers checkers) throws Exception {
        Task task = createPlainTask("modifyUserAddAccountImplicitExplicitSameReverse");
        OperationResult result = task.getResult();

        PrismObject<ShadowType> account = accountSource.get();
        ObjectDelta<UserType> userDelta = userLinkRefAddDelta(account);
        ObjectDelta<ShadowType> shadowDelta = shadowAddDelta(account);
        doPreview(
                List.of(shadowDelta, userDelta),
                checkers, task, result);
    }

    /**
     * Adds an account by adding a `linkRef` with the shadow object and providing the shadow `ADD` delta with the object clone.
     */
    private void modifyUserAddAccountImplicitExplicitEqual(
            ObjectSource<PrismObject<ShadowType>> accountSource, Checkers checkers) throws Exception {
        Task task = createPlainTask("modifyUserAddAccountImplicitExplicitEqual");
        OperationResult result = task.getResult();

        PrismObject<ShadowType> account = accountSource.get();
        ObjectDelta<UserType> userDelta = userLinkRefAddDelta(account.clone());
        ObjectDelta<ShadowType> shadowDelta = shadowAddDelta(account);
        doPreview(
                List.of(userDelta, shadowDelta),
                checkers, task, result);
    }

    /**
     * Adds an account by adding a `linkRef` with the shadow object and providing the shadow `ADD` delta with the object clone.
     * (In the reverse order - account delta first.)
     */
    private void modifyUserAddAccountImplicitExplicitEqualReverse(
            ObjectSource<PrismObject<ShadowType>> accountSource, Checkers checkers) throws Exception {
        Task task = createPlainTask("modifyUserAddAccountImplicitExplicitEqual");
        OperationResult result = task.getResult();

        PrismObject<ShadowType> account = accountSource.get();
        ObjectDelta<UserType> userDelta = userLinkRefAddDelta(account.clone());
        ObjectDelta<ShadowType> shadowDelta = shadowAddDelta(account);
        doPreview(
                List.of(shadowDelta, userDelta),
                checkers, task, result);
    }

    /**
     * Adds an account by adding a `linkRef` with the shadow object and providing the shadow `ADD` delta with a different object.
     * It should fail.
     */
    private void modifyUserAddAccountImplicitExplicitNotEqual(
            ObjectSource<PrismObject<ShadowType>> accountSource) throws Exception {
        Task task = createPlainTask("modifyUserAddAccountImplicitExplicitNotEqual");
        OperationResult result = task.getResult();

        PrismObject<ShadowType> account = accountSource.get();
        ObjectDelta<UserType> userDelta = userLinkRefAddDelta(account.clone());
        account.asObjectable().setDescription("aye!"); // Let's make the account different. This should cause the preview to fail
        ObjectDelta<ShadowType> shadowDelta = shadowAddDelta(account);

        doPreviewFail(
                List.of(userDelta, shadowDelta),
                task, result);
    }

    /**
     * Adds an account by adding a `linkRef` with the shadow object and providing the shadow `ADD` delta with a different object.
     * (In the reverse order - account delta first.) It should fail.
     */
    private void modifyUserAddAccountImplicitExplicitNotEqualReverse(
            ObjectSource<PrismObject<ShadowType>> accountSource) throws Exception {
        Task task = createPlainTask("modifyUserAddAccountImplicitExplicitNotEqualReverse");
        OperationResult result = task.getResult();

        PrismObject<ShadowType> account = accountSource.get();
        ObjectDelta<UserType> userDelta = userLinkRefAddDelta(account.clone());
        account.asObjectable().setDescription("aye!"); // Let's make the account different. This should cause the preview to fail
        ObjectDelta<ShadowType> shadowDelta = shadowAddDelta(account);

        doPreviewFail(
                List.of(shadowDelta, userDelta),
                task, result);
    }

    private ObjectDelta<UserType> userLinkRefAddDelta(PrismObject<ShadowType> account) throws SchemaException {
        return deltaFor(UserType.class)
                .item(UserType.F_LINK_REF)
                .add(createObjectRefWithFullObject(account))
                .asObjectDelta(USER_JACK_OID);
    }

    private ObjectDelta<UserType> emptyUserDelta() {
        return prismContext.deltaFactory().object().createEmptyModifyDelta(UserType.class, USER_JACK_OID);
    }

    private ObjectDelta<ShadowType> shadowAddDelta(PrismObject<ShadowType> account) {
        return account.createAddDelta();
    }

    private void doPreview(
            Collection<ObjectDelta<? extends ObjectType>> deltas,
            Checkers checkers, Task task, OperationResult result)
            throws CommonException {
        display("Input deltas: ", deltas);

        when("changes are previewed");
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, null, task, result);

        then("the resulting context is OK");
        displayDumpable("Preview context", modelContext);
        checkers.ctxChecker.check(modelContext);

        if (areMarksSupported()) {
            when("changes are simulated");
            var simResult = executeWithSimulationResult(deltas, task, result);

            then("the result is OK");
            checkers.simChecker.check(simResult);
        }

        assertSuccess(result);
    }

    private void doPreviewFail(
            Collection<ObjectDelta<? extends ObjectType>> deltas, Task task, OperationResult result)
            throws PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException,
            ObjectAlreadyExistsException, CommunicationException, ConfigurationException,
            SecurityViolationException {
        display("Input deltas: ", deltas);

        try {
            when("changes are previewed");
            modelInteractionService.previewChanges(deltas, null, task, result);
            AssertJUnit.fail("Expected exception, but it haven't come");
        } catch (SchemaException e) {
            then("exception should be thrown");
            displayExpectedException(e);
        }

        result.computeStatus();
        TestUtil.assertFailure(result);
    }

    private void assertAddAccount(ModelContext<UserType> modelContext, boolean expectFullNameDelta) {
        assertNotNull("Null model context", modelContext);

        assertSerializable(modelContext);

        ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
        assertNotNull("Null model focus context", focusContext);
        assertNull("Unexpected focus primary delta: " + focusContext.getPrimaryDelta(), focusContext.getPrimaryDelta());
        assertSideEffectiveDeltasOnly(focusContext.getSummarySecondaryDelta(), "focus secondary delta", ActivationStatusType.ENABLED);

        Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
        assertNotNull("Null model projection context list", projectionContexts);
        assertEquals("Unexpected number of projection contexts", 1, projectionContexts.size());
        ModelProjectionContext accContext = projectionContexts.iterator().next();
        assertNotNull("Null model projection context", accContext);

        assertEquals("Wrong policy decision", SynchronizationPolicyDecision.ADD, accContext.getSynchronizationPolicyDecision());
        ObjectDelta<ShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
        assertEquals(ChangeType.ADD, accountPrimaryDelta.getChangeType());
        PrismObject<ShadowType> accountToAddPrimary = accountPrimaryDelta.getObjectToAdd();
        assertNotNull("No object in account primary add delta", accountToAddPrimary);
        getDummyResourceController();
        assertEquals(RI_ACCOUNT_OBJECT_CLASS,
                accountToAddPrimary.findProperty(ShadowType.F_OBJECT_CLASS).getRealValue());
        PrismReference resourceRef = accountToAddPrimary.findReference(ShadowType.F_RESOURCE_REF);
        assertEquals(getDummyResourceObject().getOid(), resourceRef.getOid());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSummarySecondaryDelta();
        assertEquals(ChangeType.MODIFY, accountSecondaryDelta.getChangeType());
        PropertyDelta<String> fullNameDelta = accountSecondaryDelta.findPropertyDelta(
                dummyResourceCtl.getAttributeFullnamePath());
        if (expectFullNameDelta) {
            assertNotNull("No full name delta in account secondary delta", fullNameDelta);
            PrismAsserts.assertReplace(fullNameDelta, "Jack Sparrow");
            PrismAsserts.assertOrigin(fullNameDelta, OriginType.OUTBOUND);
        } else {
            assertNull("Unexpected full name delta in account secondary delta", fullNameDelta);
        }

        PrismObject<ShadowType> accountNew = accContext.getObjectNew();
        IntegrationTestTools.assertIcfsNameAttribute(accountNew, "jack");
        IntegrationTestTools.assertAttribute(accountNew, dummyResourceCtl.getAttributeFullnameQName(), "Jack Sparrow");
    }

    private void assertAddAccount(TestSimulationResult simResult) {
        try {
            // @formatter:off
            assertProcessedObjects(simResult, "after")
                    .display()
                    .by().objectType(UserType.class).changeType(ChangeType.MODIFY).find()
                        .assertEventMarks()
                        .delta()
                            .assertModifiedExclusive(
                                    UserType.F_LINK_REF,
                                    InfraItemName.METADATA)
                        .end()
                    .end()
                    .by().objectType(ShadowType.class).changeType(ChangeType.ADD).find()
                        .assertEventMarks(MARK_PROJECTION_ACTIVATED, MARK_PROJECTION_RESOURCE_OBJECT_AFFECTED)
                        .delta()
                            .objectToAdd()
                                .asShadow()
                                    .assertKind(ACCOUNT)
                                    .assertIntent("default")
                                    .attributes()
                                        .assertValue(DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_QNAME, "Jack Sparrow");
            // @formatter:on
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    /** This is not a test for previewing changes, but about compiling a GUI profile. */
    @Test
    public void test130GetCompiledGuiProfile() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        CompiledGuiProfile compiledGuiProfile = modelInteractionService.getCompiledGuiProfile(task, result);

        // THEN
        assertSuccess(result);

        assertCompiledGuiProfile(compiledGuiProfile)
                .assertAdditionalMenuLinks(0)
                .assertUserDashboardLinks(1)
                .assertUserDashboardWidgets(0)
                .assertObjectCollectionViews(3);

        RichHyperlinkType link = compiledGuiProfile.getUserDashboardLink().get(0);
        assertEquals("Bad link label", "Foo", link.getLabel());
        assertEquals("Bad link targetUrl", "/foo", link.getTargetUrl());

        assertEquals("Bad timezone targetUrl", "Jamaica", compiledGuiProfile.getDefaultTimezone());

        assertSerializable(compiledGuiProfile);
    }

    @Test
    public void test150GetGuybrushRefinedObjectClassDef() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<ShadowType> shadow = getShadowModel(ACCOUNT_SHADOW_GUYBRUSH_OID);

        // WHEN
        ResourceObjectDefinition rOCDef =
                modelInteractionService.getEditObjectClassDefinition(
                        shadow, getDummyResourceObject(), AuthorizationPhaseType.REQUEST, task, result);

        // THEN
        assertSuccess(result);

        displayDumpable("Refined object class", rOCDef);
        assertNotNull("Null config", rOCDef);

        display("Password credentials outbound", rOCDef.getPasswordOutbound());
        assertNotNull("Assert not null", rOCDef.getPasswordOutbound());
    }

    /** Previews deletion of an account (by deleting `linkRef` with full object). */
    @Test
    public void test200ModifyUserGuybrushDeleteAccount() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_GUYBRUSH_DUMMY_FILE);

        ObjectDelta<UserType> userDelta = deltaFor(UserType.class)
                .item(UserType.F_LINK_REF)
                .delete(createObjectRefWithFullObject(account))
                .asObjectDelta(USER_GUYBRUSH_OID);

        Collection<ObjectDelta<? extends ObjectType>> deltas = List.of(userDelta);
        display("Input deltas: ", deltas);

        // WHEN
        when();
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, null, task, result);

        // THEN
        then();
        displayDumpable("Preview context", modelContext);
        assertNotNull("Null model context", modelContext);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
        assertNotNull("Null model focus context", focusContext);
        assertNull("Unexpected focus primary delta: " + focusContext.getPrimaryDelta(), focusContext.getPrimaryDelta());
        assertSideEffectiveDeltasOnly("focus secondary delta", focusContext.getSummarySecondaryDelta());

        Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
        assertNotNull("Null model projection context list", projectionContexts);
        assertEquals("Unexpected number of projection contexts", 1, projectionContexts.size());
        ModelProjectionContext accContext = projectionContexts.iterator().next();
        assertNotNull("Null model projection context", accContext);

        assertEquals("Wrong policy decision", SynchronizationPolicyDecision.DELETE, accContext.getSynchronizationPolicyDecision());
        ObjectDelta<ShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
        assertEquals(ChangeType.DELETE, accountPrimaryDelta.getChangeType());

        assertSerializable(modelContext);
    }

    /** Previews adding an account (by simply creating a shadow). There should be no focus context in this case. */
    @Test
    public void test210JackAddAccount() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);
        ObjectDelta<ShadowType> accountDelta = shadowAddDelta(account);
        Collection<ObjectDelta<? extends ObjectType>> deltas = List.of(accountDelta);
        display("Input deltas: ", deltas);

        // WHEN
        when();
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, null, task, result);

        // THEN
        then();
        displayDumpable("Preview context", modelContext);
        assertNotNull("Null model context", modelContext);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
        assertNull("Unexpected model focus context", focusContext);

        Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
        assertNotNull("Null model projection context list", projectionContexts);
        assertEquals("Unexpected number of projection contexts", 1, projectionContexts.size());
        ModelProjectionContext accContext = projectionContexts.iterator().next();
        assertNotNull("Null model projection context", accContext);

        // Decision does not matter now
//        assertEquals("Wrong policy decision", SynchronizationPolicyDecision.ADD, accContext.getSynchronizationPolicyDecision());
        ObjectDelta<ShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
        assertEquals(ChangeType.ADD, accountPrimaryDelta.getChangeType());
        PrismObject<ShadowType> accountToAddPrimary = accountPrimaryDelta.getObjectToAdd();
        assertNotNull("No object in account primary add delta", accountToAddPrimary);
        assertEquals(RI_ACCOUNT_OBJECT_CLASS,
                accountToAddPrimary.findProperty(ShadowType.F_OBJECT_CLASS).getRealValue());
        PrismReference resourceRef = accountToAddPrimary.findReference(ShadowType.F_RESOURCE_REF);
        assertEquals(getDummyResourceObject().getOid(), resourceRef.getOid());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSummarySecondaryDelta();
        PrismAsserts.assertModifications(accountSecondaryDelta, 1);

        assertSerializable(modelContext);
    }

    /** Simulates adding an account (by simply creating a shadow). There should be no focus context in this case. */
    @Test
    public void test211JackAddAccountSimulated() throws Exception {
        skipIfNotNativeRepository();

        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        ObjectDelta<ShadowType> delta =
                shadowAddDelta(
                        PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE));

        when();
        var simResult = executeWithSimulationResult(List.of(delta), task, result);

        then();
        assertProcessedObjects(simResult, "after")
                .display()
                .by().objectType(ShadowType.class).changeType(ChangeType.ADD).find().end()
                .assertSize(1);
    }

    /** Links an existing account by adding a `linkRef`. */
    @Test
    public void test212ModifyUserAddAccountRef() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        ObjectDelta<UserType> userDelta = deltaFor(UserType.class)
                .item(UserType.F_LINK_REF)
                .add(createObjectRef(ACCOUNT_SHADOW_GUYBRUSH_OID, ObjectTypes.SHADOW))
                .asObjectDelta(USER_GUYBRUSH_OID);
        Collection<ObjectDelta<? extends ObjectType>> deltas = List.of(userDelta);
        displayDumpable("Input deltas: ", userDelta);

        // WHEN
        when();
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, null, task, result);

        // THEN
        then();
        displayDumpable("Preview context", modelContext);
        assertNotNull("Null model context", modelContext);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
        assertNotNull("Null model focus context", focusContext);
        assertNull("Unexpected focus primary delta: " + focusContext.getPrimaryDelta(), focusContext.getPrimaryDelta());

        assertSideEffectiveDeltasOnly("focus secondary delta", focusContext.getSummarySecondaryDelta());

        Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
        assertNotNull("Null model projection context list", projectionContexts);
        assertEquals("Unexpected number of projection contexts", 1, projectionContexts.size());
        ModelProjectionContext accContext = projectionContexts.iterator().next();
        assertNotNull("Null model projection context", accContext);

        assertEquals("Wrong policy decision", SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());
        ObjectDelta<ShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
        assertNull("Unexpected account primary delta", accountPrimaryDelta);

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSummarySecondaryDelta();
        assertEquals("Unexpected size of account secondary delta: " + accountSecondaryDelta, 2, accountSecondaryDelta.getModifications().size());
        PrismAsserts.assertPropertyAdd(accountSecondaryDelta, DUMMY_ACCOUNT_ATTRIBUTE_DRINK_PATH, "rum");
        PrismAsserts.assertPropertyAdd(accountSecondaryDelta, DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_PATH, "Arr!");

        assertSerializable(modelContext);
    }

    /**
     * MID-3079
     */
    @Test
    public void test220PreviewJackAssignRolePirate() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        ObjectDelta<UserType> delta = createAssignmentAssignmentHolderDelta(UserType.class, USER_JACK_OID,
                ROLE_PIRATE_OID, RoleType.COMPLEX_TYPE, null, null, null, true);

        // WHEN
        when();
        ModelContext<UserType> modelContext =
                modelInteractionService.previewChanges(List.of(delta), null, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertPreviewJackAssignRolePirate(modelContext);
        assertSerializable(modelContext);
    }

    /**
     * MID-5762
     */
    @Test
    public void test221PreviewJackAssignRolePirateReconcile() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        assertUserBefore(USER_JACK_OID)
                .links()
                .assertNoLiveLinks();

        ObjectDelta<UserType> delta = createAssignmentAssignmentHolderDelta(UserType.class, USER_JACK_OID,
                ROLE_PIRATE_OID, RoleType.COMPLEX_TYPE, null, null, null, true);

        // WHEN
        when();
        ModelContext<UserType> modelContext =
                modelInteractionService.previewChanges(List.of(delta), executeOptions().reconcile(), task, result);

        // THEN
        then();
        assertSuccess(result);

        assertPreviewJackAssignRolePirate(modelContext);
        assertSerializable(modelContext);
    }

    private void assertPreviewJackAssignRolePirate(ModelContext<UserType> modelContext) {
        displayDumpable("Preview context", modelContext);
        assertNotNull("Null model context", modelContext);

        ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
        assertNotNull("Model focus context missing", focusContext);

        Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
        assertNotNull("Null model projection context list", projectionContexts);
        assertEquals("Unexpected number of projection contexts", 1, projectionContexts.size());
        ModelProjectionContext accContext = projectionContexts.iterator().next();
        assertNotNull("Null model projection context", accContext);

        ObjectDelta<ShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
        assertNull("Unexpected account primary delta", accountPrimaryDelta);

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSummarySecondaryDelta();
        assertEquals(ChangeType.MODIFY, accountSecondaryDelta.getChangeType());

        assertAccountItemModify(accountSecondaryDelta,
                SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS,
                null, // old
                null, // add
                null, // delete
                new ActivationStatusType[] { ActivationStatusType.ENABLED });  // replace

        assertAccountDefaultDummyAttributeModify(accountSecondaryDelta,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME,
                null, // old
                new String[] { ROLE_PIRATE_WEAPON }, // add
                null, // delete
                null);  // replace

        assertAccountDefaultDummyAttributeModify(accountSecondaryDelta,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME,
                null, // old
                new String[] { ROLE_PIRATE_TITLE }, // add
                null, // delete
                null);  // replace

        assertAccountDefaultDummyAttributeModify(accountSecondaryDelta,
                DUMMY_ACCOUNT_ATTRIBUTE_SEA_NAME,
                null, // old
                null, // add
                null, // delete
                new String[] { "jack sailed The Seven Seas, immediately , role , with this The Seven Seas while focused on  (in Pirate)" });  // replace

        assertAccountDefaultDummyAttributeModify(accountSecondaryDelta,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME,
                null, // old
                new String[] { "Jack Sparrow is the best pirate Caribbean has ever seen" }, // add
                null, // delete
                null);  // replace

        assertAccountDefaultDummyAttributeModify(accountSecondaryDelta,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME,
                null, // old
                new String[] { RESOURCE_DUMMY_QUOTE }, // add
                null, // delete
                null);  // replace

        assertAccountDefaultDummyAttributeModify(accountSecondaryDelta,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
                null, // old
                new String[] { RESOURCE_DUMMY_DRINK }, // add
                null, // delete
                null);  // replace

        PrismAsserts.assertModifications(accountSecondaryDelta, 15);
    }

    /**
     * Make sure that Guybrush has an existing account and that it is properly populated.
     * We will use this setup in following tests.
     */
    @Test
    public void test230GuybrushAssignAccountDummy() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        createAssignmentAssignmentHolderDelta(UserType.class, USER_GUYBRUSH_OID,
                ROLE_PIRATE_OID, RoleType.COMPLEX_TYPE, null, null, null, true);

        // WHEN
        when();
        modifyUserReplace(USER_GUYBRUSH_OID, ItemPath.create(UserType.F_EXTENSION, PIRACY_WEAPON), task, result,
                "tongue");
        assignAccountToUser(USER_GUYBRUSH_OID, RESOURCE_DUMMY_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        accountGuybrushOid = assertUserAfter(USER_GUYBRUSH_OID)
                .singleLink()
                .getOid();

        DummyAccount dummyAccount = assertDummyAccount(null, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME, true);
        displayDumpable("Dummy account after", dummyAccount);
        dummyAccount.addAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Great Pirate");
        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_OID);
    }

    /**
     * Let's preview direct modification of account attribute. Make sure that there is no focus delta.
     * MID-5762
     */
    @Test
    public void test231PreviewGuybrushModifyAccountFullName() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<ShadowType> accountDelta = prismContext.deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
                accountGuybrushOid, dummyResourceCtl.getAttributeFullnamePath(), "Mighty Pirate Guybrush Threepwood");

        // WHEN
        when();
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(MiscSchemaUtil.createCollection(accountDelta),
                null, task, result);

        // THEN
        then();
        displayDumpable("Preview context", modelContext);
        assertNotNull("Null model context", modelContext);

        assertSuccess(result);

        ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
        assertNotNull("Model focus context missing", focusContext);
        assertNull("Unexpected focus primary delta", focusContext.getPrimaryDelta());
        assertNull("Unexpected focus secondary delta", focusContext.getSummarySecondaryDelta());

        Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
        assertNotNull("Null model projection context list", projectionContexts);
        assertEquals("Unexpected number of projection contexts", 1, projectionContexts.size());
        ModelProjectionContext accContext = projectionContexts.iterator().next();
        assertNotNull("Null model projection context", accContext);
        assertNotNull("Missing account primary delta", accContext.getPrimaryDelta());
        assertNull("Unexpected account secondary delta", accContext.getSummarySecondaryDelta());
        assertEquals(ChangeType.MODIFY, accContext.getPrimaryDelta().getChangeType());

        assertAccountDefaultDummyAttributeModify(accContext.getPrimaryDelta(),
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME,
                // Note: the fullName mapping is not strong
                isShadowCachingFullByDefault() ? new String[] { "Guybrush Threepwood" } : null, // old
                null, // add
                null, // delete
                new String[] { "Mighty Pirate Guybrush Threepwood" });  // replace

        PrismAsserts.assertModifications(accContext.getPrimaryDelta(), 1);
        assertSerializable(modelContext);
    }

    /**
     * Let's preview direct modification of account attribute. Make sure that there is no focus primary delta.
     * But there will be focus secondary delta as ship has inbound mapping.
     * MID-5762
     */
    @Test
    public void test232PreviewGuybrushModifyAccountShip() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<ShadowType> accountDelta = prismContext.deltaFactory().object().createModificationReplaceProperty(
                ShadowType.class, accountGuybrushOid, DUMMY_ACCOUNT_ATTRIBUTE_SHIP_PATH, "The Mad Monkey");

        // WHEN
        when();
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(MiscSchemaUtil.createCollection(accountDelta),
                null, task, result);

        // THEN
        then();
        displayDumpable("Preview context", modelContext);
        assertNotNull("Null model context", modelContext);

        assertSuccess(result);

        ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
        assertNotNull("Model focus context missing", focusContext);
        assertNull("Unexpected focus primary delta", focusContext.getPrimaryDelta());
        assertNotNull("Missing focus secondary delta", focusContext.getSummarySecondaryDelta());

        Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
        assertNotNull("Null model projection context list", projectionContexts);
        assertEquals("Unexpected number of projection contexts", 1, projectionContexts.size());
        ModelProjectionContext accContext = projectionContexts.iterator().next();
        assertNotNull("Null model projection context", accContext);
        assertNotNull("Missing account primary delta", accContext.getPrimaryDelta());
        assertNull("Unexpected account secondary delta", accContext.getSummarySecondaryDelta());
        assertEquals(ChangeType.MODIFY, accContext.getPrimaryDelta().getChangeType());

        assertAccountDefaultDummyAttributeModify(accContext.getPrimaryDelta(),
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME,
                null, // old
                null, // add
                null, // delete
                new String[] { "The Mad Monkey" });  // replace

        PrismAsserts.assertModifications(accContext.getPrimaryDelta(), 1);
        assertSerializable(modelContext);
    }

    /**
     * MID-3079
     */
    @Test
    public void test233PreviewGuybrushAddRolePirate() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        ObjectDelta<UserType> delta = createAssignmentAssignmentHolderDelta(UserType.class, USER_GUYBRUSH_OID,
                ROLE_PIRATE_OID, RoleType.COMPLEX_TYPE, null, null, null, true);

        // WHEN
        when();
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(MiscSchemaUtil.createCollection(delta),
                null, task, result);

        // THEN
        then();
        displayDumpable("Preview context", modelContext);
        assertNotNull("Null model context", modelContext);

        assertSuccess(result);

        ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
        assertNotNull("Model focus context missing", focusContext);

        Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
        assertNotNull("Null model projection context list", projectionContexts);
        assertEquals("Unexpected number of projection contexts", 1, projectionContexts.size());
        ModelProjectionContext accContext = projectionContexts.iterator().next();
        assertNotNull("Null model projection context", accContext);

        ObjectDelta<ShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
        assertNull("Unexpected account primary delta", accountPrimaryDelta);

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSummarySecondaryDelta();
        assertEquals(ChangeType.MODIFY, accountSecondaryDelta.getChangeType());

        assertAccountDefaultDummyAttributeModify(accountSecondaryDelta,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME,
                new String[] { "Great Pirate" }, // old
                new String[] { ROLE_PIRATE_TITLE }, // add
                null, // delete
                null);  // replace

        assertAccountDefaultDummyAttributeModify(accountSecondaryDelta,
                DUMMY_ACCOUNT_ATTRIBUTE_SEA_NAME,
                null, // old
                null, // add
                null, // delete
                new String[] { "guybrush sailed The Seven Seas, immediately , role , with this The Seven Seas while focused on  (in Pirate)" });  // replace

        assertAccountDefaultDummyAttributeModify(accountSecondaryDelta,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME,
                null, // old
                new String[] { "Guybrush Threepwood is the best pirate Melee Island has ever seen" }, // add
                null, // delete
                null);  // replace

        PrismAsserts.assertModifications(accountSecondaryDelta, 3);
        assertSerializable(modelContext);
    }

    /**
     * MID-3079
     */
    @Test
    public void test234PreviewGuybrushAddRolePirateRecon() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        ObjectDelta<UserType> delta = createAssignmentAssignmentHolderDelta(UserType.class, USER_GUYBRUSH_OID,
                ROLE_PIRATE_OID, RoleType.COMPLEX_TYPE, null, null, null, true);

        ModelExecuteOptions options = executeOptions().reconcile();

        // WHEN
        when();
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(MiscSchemaUtil.createCollection(delta),
                options, task, result);

        // THEN
        then();
        displayDumpable("Preview context", modelContext);
        assertNotNull("Null model context", modelContext);

        assertSuccess(result);

        ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
        assertNotNull("Model focus context missing", focusContext);

        Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
        assertNotNull("Null model projection context list", projectionContexts);
        assertEquals("Unexpected number of projection contexts", 1, projectionContexts.size());
        ModelProjectionContext accContext = projectionContexts.iterator().next();
        assertNotNull("Null model projection context", accContext);

        ObjectDelta<ShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
        assertNull("Unexpected account primary delta", accountPrimaryDelta);

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSummarySecondaryDelta();
        assertEquals(ChangeType.MODIFY, accountSecondaryDelta.getChangeType());

        assertAccountDefaultDummyAttributeModify(accountSecondaryDelta,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME,
                new String[] { "Great Pirate" }, // old
                new String[] { ROLE_PIRATE_TITLE }, // add
                null, // delete
                null);  // replace

        assertAccountDefaultDummyAttributeModify(accountSecondaryDelta,
                DUMMY_ACCOUNT_ATTRIBUTE_SEA_NAME,
                null, // old
                null, // add
                null, // delete
                new String[] { "guybrush sailed The Seven Seas, immediately , role , with this The Seven Seas while focused on  (in Pirate)" });  // replace

        assertAccountDefaultDummyAttributeModify(accountSecondaryDelta,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME,
                null, // old
                new String[] { "Guybrush Threepwood is the best pirate Melee Island has ever seen" }, // add
                null, // delete
                null);  // replace

        PrismAsserts.assertModifications(accountSecondaryDelta, 3);
        assertSerializable(modelContext);
    }

    /**
     * MID-3079
     */
    @Test
    public void test236PreviewGuybrushAddRoleSailor() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        ObjectDelta<UserType> delta = createAssignmentAssignmentHolderDelta(UserType.class, USER_GUYBRUSH_OID,
                ROLE_SAILOR_OID, RoleType.COMPLEX_TYPE, null, null, null, true);

        // WHEN
        when();
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(MiscSchemaUtil.createCollection(delta),
                null, task, result);

        // THEN
        then();
        displayDumpable("Preview context", modelContext);
        assertNotNull("Null model context", modelContext);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
        assertNotNull("Model focus context missing", focusContext);

        Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
        assertNotNull("Null model projection context list", projectionContexts);
        assertEquals("Unexpected number of projection contexts", 1, projectionContexts.size());
        ModelProjectionContext accContext = projectionContexts.iterator().next();
        assertNotNull("Null model projection context", accContext);

        ObjectDelta<ShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
        assertNull("Unexpected account primary delta", accountPrimaryDelta);

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSummarySecondaryDelta();
        assertEquals(ChangeType.MODIFY, accountSecondaryDelta.getChangeType());

        assertAccountDefaultDummyAttributeModify(accountSecondaryDelta,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
                new String[] { RESOURCE_DUMMY_DRINK }, // old
                new String[] { ROLE_SAILOR_DRINK }, // add
                null, // delete
                null);  // replace

        PrismAsserts.assertModifications(accountSecondaryDelta, 1);
        assertSerializable(modelContext);
    }

    /**
     * MID-3845
     */
    @Test
    public void test238PreviewGuybrushAddRoleSailorOwner() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        ObjectDelta<UserType> delta = createAssignmentAssignmentHolderDelta(UserType.class, USER_GUYBRUSH_OID,
                ROLE_SAILOR_OID, RoleType.COMPLEX_TYPE, SchemaConstants.ORG_OWNER, null, null, true);

        // WHEN
        when();
        var options = ModelExecuteOptions.create().firstClickOnly(); // to avoid assignments evaluation in later stages
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(List.of(delta), options, task, result);

        // THEN
        then();
        displayDumpable("Preview context", modelContext);
        assertNotNull("Null model context", modelContext);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
        assertNotNull("Model focus context missing", focusContext);

        DeltaSetTriple<? extends EvaluatedAssignment> evaluatedAssignmentTriple = modelContext.getEvaluatedAssignmentTriple();
        displayDumpable("evaluatedAssignmentTriple", evaluatedAssignmentTriple);

        assertEquals("Wrong # of evaluated assignments in plus set", 1, evaluatedAssignmentTriple.getPlusSet().size());
        EvaluatedAssignment evaluatedAssignment = evaluatedAssignmentTriple.getPlusSet().iterator().next();
        assertNotNull("Target of evaluated assignment is null", evaluatedAssignment.getTarget());
        assertEquals("Wrong # of zero-set roles in evaluated assignment", 1, evaluatedAssignment.getRoles().getZeroSet().size());
        assertSerializable(modelContext);
    }

    @Test
    public void test239GuybrushUnAssignAccountDummy() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        // WHEN
        when();
        unassignAccountFromUser(USER_GUYBRUSH_OID, RESOURCE_DUMMY_OID, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNoDummyAccount(null, USER_GUYBRUSH_USERNAME);
    }

    /**
     * Make sure that Guybrush has an existing account and that it is properly populated.
     * We will use this setup in following tests.
     */
    @Test
    public void test240GuybrushAssignAccountDummyRelative() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        // WHEN
        when();
        assignAccountToUser(USER_GUYBRUSH_OID, RESOURCE_DUMMY_RELATIVE_OID, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        DummyAccount dummyAccount = assertDummyAccount(RESOURCE_DUMMY_RELATIVE_NAME, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME, true);
        displayDumpable("Dummy account after", dummyAccount);
        dummyAccount.addAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Great Pirate");
        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_RELATIVE_OID);
    }

    /**
     * MID-3079
     * Relative operation on a relative resource. The account is not retrieved.
     * There are no old values at all.
     */
    @Test
    public void test242PreviewGuybrushAddRolePirateRelative() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        ObjectDelta<UserType> delta = createAssignmentAssignmentHolderDelta(UserType.class, USER_GUYBRUSH_OID,
                ROLE_PIRATE_RELATIVE_OID, RoleType.COMPLEX_TYPE, null, null, null, true);

        // WHEN
        when();
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(MiscSchemaUtil.createCollection(delta),
                null, task, result);

        // THEN
        then();
        displayDumpable("Preview context", modelContext);
        assertNotNull("Null model context", modelContext);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
        assertNotNull("Model focus context missing", focusContext);

        Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
        assertNotNull("Null model projection context list", projectionContexts);
        assertEquals("Unexpected number of projection contexts", 1, projectionContexts.size());
        ModelProjectionContext accContext = projectionContexts.iterator().next();
        assertNotNull("Null model projection context", accContext);

        ObjectDelta<ShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
        assertNull("Unexpected account primary delta", accountPrimaryDelta);

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSummarySecondaryDelta();
        assertEquals(ChangeType.MODIFY, accountSecondaryDelta.getChangeType());

        assertAccountDummyAttributeModify(accountSecondaryDelta,
                RESOURCE_DUMMY_RELATIVE_NAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME,
                isShadowCachingFullByDefault() ? new String[] { "tongue" } : null, // old
                new String[] { ROLE_PIRATE_WEAPON }, // add
                null, // delete
                null);  // replace

        assertAccountDummyAttributeModify(accountSecondaryDelta,
                RESOURCE_DUMMY_RELATIVE_NAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME,
                null, // old
                new String[] { ROLE_PIRATE_TITLE }, // add
                null, // delete
                null);  // replace

        assertAccountDummyAttributeModify(accountSecondaryDelta,
                RESOURCE_DUMMY_RELATIVE_NAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME,
                null, // old
                new String[] { "Guybrush Threepwood is the best pirate Melee Island has ever seen" }, // add
                null, // delete
                null);  // replace

        PrismAsserts.assertModifications(accountSecondaryDelta, 3);
        assertSerializable(modelContext);
    }

    /**
     * MID-3079
     */
    @Test
    public void test244PreviewGuybrushAddRolePirateRelativeRecon() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        ObjectDelta<UserType> delta = createAssignmentAssignmentHolderDelta(UserType.class, USER_GUYBRUSH_OID,
                ROLE_PIRATE_RELATIVE_OID, RoleType.COMPLEX_TYPE, null, null, null, true);

        // WHEN
        when();
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(MiscSchemaUtil.createCollection(delta),
                executeOptions().reconcile(), task, result);

        // THEN
        then();
        displayDumpable("Preview context", modelContext);
        assertNotNull("Null model context", modelContext);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
        assertNotNull("Model focus context missing", focusContext);

        Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
        assertNotNull("Null model projection context list", projectionContexts);
        assertEquals("Unexpected number of projection contexts", 1, projectionContexts.size());
        ModelProjectionContext accContext = projectionContexts.iterator().next();
        assertNotNull("Null model projection context", accContext);

        ObjectDelta<ShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
        assertNull("Unexpected account primary delta", accountPrimaryDelta);

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSummarySecondaryDelta();
        assertEquals(ChangeType.MODIFY, accountSecondaryDelta.getChangeType());

        assertAccountDummyAttributeModify(accountSecondaryDelta,
                RESOURCE_DUMMY_RELATIVE_NAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME,
                new String[] { "tongue" }, // old
                new String[] { ROLE_PIRATE_WEAPON }, // add
                null, // delete
                null);  // replace

        assertAccountDummyAttributeModify(accountSecondaryDelta,
                RESOURCE_DUMMY_RELATIVE_NAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME,
                new String[] { "Great Pirate" }, // old
                new String[] { ROLE_PIRATE_TITLE }, // add
                null, // delete
                null);  // replace

        assertAccountDummyAttributeModify(accountSecondaryDelta,
                RESOURCE_DUMMY_RELATIVE_NAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME,
                null, // old
                new String[] { "Guybrush Threepwood is the best pirate Melee Island has ever seen" }, // add
                null, // delete
                null);  // replace

        PrismAsserts.assertModifications(accountSecondaryDelta, 3);
        assertSerializable(modelContext);
    }

    @Test
    public void test249GuybrushUnAssignAccountDummyRelative() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        // WHEN
        when();
        unassignAccountFromUser(USER_GUYBRUSH_OID, RESOURCE_DUMMY_RELATIVE_OID, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNoDummyAccount(RESOURCE_DUMMY_RELATIVE_NAME, USER_GUYBRUSH_USERNAME);
    }

    @SuppressWarnings("SameParameterValue")
    private <T> void assertAccountDefaultDummyAttributeModify(ObjectDelta<ShadowType> accountDelta,
            String attrName,
            T[] expectedOldValues, T[] expectedAddValues, T[] expectedDeleteValues, T[] expectedReplaceValues) {
        ItemPath itemPath = getDummyResourceController().getAttributePath(attrName);
        assertAccountItemModify(accountDelta, itemPath, expectedOldValues, expectedAddValues, expectedDeleteValues, expectedReplaceValues);
    }

    @SuppressWarnings("SameParameterValue")
    private <T> void assertAccountDummyAttributeModify(ObjectDelta<ShadowType> accountDelta,
            String dummyName, String attrName,
            T[] expectedOldValues, T[] expectedAddValues, T[] expectedDeleteValues, T[] expectedReplaceValues) {
        ItemPath itemPath = getDummyResourceController(dummyName).getAttributePath(attrName);
        assertAccountItemModify(accountDelta, itemPath, expectedOldValues, expectedAddValues, expectedDeleteValues, expectedReplaceValues);
    }

    private <T> void assertAccountItemModify(ObjectDelta<ShadowType> accountDelta,
            ItemPath itemPath,
            T[] expectedOldValues, T[] expectedAddValues, T[] expectedDeleteValues, T[] expectedReplaceValues) {
        PropertyDelta<T> attrDelta = accountDelta.findPropertyDelta(itemPath);
        assertNotNull("No delta for " + itemPath + " in " + accountDelta, attrDelta);
        PrismAsserts.assertPropertyDelta(attrDelta, expectedOldValues, expectedAddValues, expectedDeleteValues, expectedReplaceValues);
    }

    // MAPPING TESTS
    // following tests mostly check correct functions of mappings

    // the test3xx is testing mappings with default dummy resource. It has NORMAL mappings.

    /**
     * Changing ACCOUNT fullname (replace delta), no user changes.
     */
    @Test
    public void test300ModifyElaineAccountDummyReplace() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        ObjectDelta<ShadowType> accountDelta = createModifyAccountShadowReplaceAttributeDelta(
                ACCOUNT_SHADOW_ELAINE_DUMMY_OID, getDummyResourceObject(), DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME,
                "Elaine Threepwood");
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(accountDelta);
        display("Input deltas: ", deltas);

        // WHEN
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, null, task, result);

        // THEN
        displayDumpable("Preview context", modelContext);
        assertNotNull("Null model context", modelContext);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
        assertNotNull("Null model focus context", focusContext);
        assertNull("Unexpected focus primary delta: " + focusContext.getPrimaryDelta(), focusContext.getPrimaryDelta());

        assertSideEffectiveDeltasOnly("focus secondary delta", focusContext.getSummarySecondaryDelta());

        Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
        assertNotNull("Null model projection context list", projectionContexts);
        assertEquals("Unexpected number of projection contexts", 3, projectionContexts.size());

        ModelProjectionContext accContext = findAccountContext(modelContext, RESOURCE_DUMMY_OID);
        assertNotNull("Null model projection context", accContext);

        assertEquals("Wrong policy decision", SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());
        ObjectDelta<ShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
        assertNotNull("No account primary delta", accountPrimaryDelta);
        PrismAsserts.assertModifications(accountPrimaryDelta, 1);
        PrismAsserts.assertPropertyReplace(accountPrimaryDelta,
                getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME),
                "Elaine Threepwood");

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSummarySecondaryDelta();
        assertNull("Unexpected account secondary delta: " + accountSecondaryDelta, accountSecondaryDelta);
        assertSerializable(modelContext);
    }

    /**
     * Changing ACCOUNT fullname (add/delete delta), no user changes.
     */
    @Test
    public void test301ModifyElaineAccountDummyDeleteAdd() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        ObjectDelta<ShadowType> accountDelta = createModifyAccountShadowEmptyDelta(ACCOUNT_SHADOW_ELAINE_DUMMY_OID);
        PropertyDelta<String> fullnameDelta = createAttributeAddDelta(getDummyResourceObject(),
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Elaine Threepwood");
        fullnameDelta.addRealValuesToDelete("Elaine Marley");
        accountDelta.addModification(fullnameDelta);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(accountDelta);
        display("Input deltas: ", deltas);

        // WHEN
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, null, task, result);

        // THEN
        displayDumpable("Preview context", modelContext);
        assertNotNull("Null model context", modelContext);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
        assertNotNull("Null model focus context", focusContext);
        assertNull("Unexpected focus primary delta: " + focusContext.getPrimaryDelta(), focusContext.getPrimaryDelta());

        assertSideEffectiveDeltasOnly("focus secondary delta", focusContext.getSummarySecondaryDelta());

        Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
        assertNotNull("Null model projection context list", projectionContexts);
        assertEquals("Unexpected number of projection contexts", 3, projectionContexts.size());

        ModelProjectionContext accContext = findAccountContext(modelContext, RESOURCE_DUMMY_OID);
        assertNotNull("Null model projection context", accContext);

        assertEquals("Wrong policy decision", SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());
        ObjectDelta<ShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
        assertNotNull("No account primary delta", accountPrimaryDelta);
        PrismAsserts.assertModifications(accountPrimaryDelta, 1);
        PrismAsserts.assertPropertyAdd(accountPrimaryDelta, DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_PATH, "Elaine Threepwood");
        PrismAsserts.assertPropertyDelete(accountPrimaryDelta, DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_PATH, "Elaine Marley");

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSummarySecondaryDelta();
        assertNull("Unexpected account secondary delta: " + accountSecondaryDelta, accountSecondaryDelta);
        assertSerializable(modelContext);
    }

    /**
     * Changing ACCOUNT fullname (replace delta), no user changes.
     * Attempt to make a change to a single-valued attribute for which there is already a strong mapping.
     * As it cannot have both values (from the delta and from the mapping) the preview should fail.
     */
    @Test
    public void test400ModifyElaineAccountDummyRedReplace() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        ObjectDelta<ShadowType> accountDelta = createModifyAccountShadowReplaceAttributeDelta(
                ACCOUNT_SHADOW_ELAINE_DUMMY_RED_OID, getDummyResourceObject(RESOURCE_DUMMY_RED_NAME),
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Elaine Threepwood");
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(accountDelta);
        display("Input deltas: ", deltas);

        // WHEN
        when();
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, null, task, result);

        // THEN
        then();
        displayDumpable("Preview context", modelContext);
        assertPartialError(result);
        assertSerializable(modelContext);
    }

    /**
     * Changing ACCOUNT fullname (add/delete delta), no user changes.
     * Attempt to make a change to a single-valued attribute for which there is already a strong mapping.
     * As it cannot have both values (from the delta and from the mapping) the preview should fail.
     */
    @Test(enabled = false) // MID-6372
    public void test401ModifyElaineAccountDummyRedDeleteAdd() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        ObjectDelta<ShadowType> accountDelta = createModifyAccountShadowEmptyDelta(ACCOUNT_SHADOW_ELAINE_DUMMY_RED_OID);
        PropertyDelta<String> fullnameDelta = createAttributeAddDelta(getDummyResourceObject(RESOURCE_DUMMY_RED_NAME),
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Elaine Threepwood");
        fullnameDelta.addRealValuesToDelete("Elaine Marley");
        accountDelta.addModification(fullnameDelta);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(accountDelta);
        display("Input deltas: ", deltas);

        // WHEN
        when();
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, null, task, result);
        displayDumpable("Preview context", modelContext);

        // THEN
        then();
        assertPartialError(result);
        assertSerializable(modelContext);
    }

    // the test5xx is testing mappings with blue dummy resource. It has WEAK mappings.

    /**
     * Changing ACCOUNT fullname (replace delta), no user changes.
     */
    @Test
    public void test500ModifyElaineAccountDummyBlueReplace() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        ObjectDelta<ShadowType> accountDelta = createModifyAccountShadowReplaceAttributeDelta(
                ACCOUNT_SHADOW_ELAINE_DUMMY_BLUE_OID, getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME),
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Elaine Threepwood");
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(accountDelta);
        display("Input deltas: ", deltas);

        // WHEN
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, null, task, result);

        // THEN
        displayDumpable("Preview context", modelContext);
        assertNotNull("Null model context", modelContext);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
        assertNotNull("Null model focus context", focusContext);
        assertNull("Unexpected focus primary delta: " + focusContext.getPrimaryDelta(), focusContext.getPrimaryDelta());

        ObjectDelta<UserType> userSecondaryDelta = focusContext.getSummarySecondaryDelta();
        assertSideEffectiveDeltasOnly("focus secondary delta", userSecondaryDelta);

        Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
        assertNotNull("Null model projection context list", projectionContexts);
        assertEquals("Unexpected number of projection contexts", 3, projectionContexts.size());

        ModelProjectionContext accContext = findAccountContext(modelContext, RESOURCE_DUMMY_BLUE_OID);
        assertNotNull("Null model projection context", accContext);

        assertEquals("Wrong policy decision", SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());
        ObjectDelta<ShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
        assertNotNull("No account primary delta", accountPrimaryDelta);
        PrismAsserts.assertModifications(accountPrimaryDelta, 1);
        PrismAsserts.assertPropertyReplace(accountPrimaryDelta,
                getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME),
                "Elaine Threepwood");

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSummarySecondaryDelta();
        assertNull("Unexpected account secondary delta: " + accountSecondaryDelta, accountSecondaryDelta);
        assertSerializable(modelContext);
    }

    /**
     * Changing ACCOUNT fullname (add/delete delta), no user changes.
     */
    @Test
    public void test501ModifyElaineAccountDummyBlueDeleteAdd() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        displayDumpable("elaine blue account before", getDummyAccount(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_ELAINE_DUMMY_BLUE_USERNAME));

        ObjectDelta<ShadowType> accountDelta = createModifyAccountShadowEmptyDelta(ACCOUNT_SHADOW_ELAINE_DUMMY_BLUE_OID);
        PropertyDelta<String> fullnameDelta = createAttributeAddDelta(getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME),
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Elaine Threepwood");
        fullnameDelta.addRealValuesToDelete("Elaine Marley");
        accountDelta.addModification(fullnameDelta);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(accountDelta);
        display("Input deltas: ", deltas);

        // WHEN
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, null, task, result);

        // THEN
        displayDumpable("Preview context", modelContext);
        assertNotNull("Null model context", modelContext);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
        assertNotNull("Null model focus context", focusContext);
        assertNull("Unexpected focus primary delta: " + focusContext.getPrimaryDelta(), focusContext.getPrimaryDelta());

        ObjectDelta<UserType> userSecondaryDelta = focusContext.getSummarySecondaryDelta();
        assertSideEffectiveDeltasOnly("focus secondary delta", userSecondaryDelta);

        Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
        assertNotNull("Null model projection context list", projectionContexts);
        assertEquals("Unexpected number of projection contexts", 3, projectionContexts.size());

        ModelProjectionContext accContext = findAccountContext(modelContext, RESOURCE_DUMMY_BLUE_OID);
        assertNotNull("Null model projection context", accContext);

        assertEquals("Wrong policy decision", SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());
        ObjectDelta<ShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
        assertNotNull("No account primary delta", accountPrimaryDelta);
        PrismAsserts.assertModifications(accountPrimaryDelta, 1);
        PrismAsserts.assertPropertyAdd(accountPrimaryDelta,
                getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME),
                "Elaine Threepwood");
        PrismAsserts.assertPropertyDelete(accountPrimaryDelta,
                getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME),
                "Elaine Marley");

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSummarySecondaryDelta();
        assertNull("Unexpected account secondary delta: " + accountSecondaryDelta, accountSecondaryDelta);
        assertSerializable(modelContext);
    }

    /**
     * Changing USER fullName (replace delta), no account changes.
     */
    @Test
    public void test600ModifyElaineUserDummyReplace() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        ObjectDelta<UserType> userDelta =
                createModifyUserReplaceDelta(USER_ELAINE_OID, UserType.F_FULL_NAME, PolyString.fromOrig("Elaine Threepwood"));
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
        display("Input deltas: ", deltas);

        // WHEN
        when();
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, null, task, result);

        // THEN
        then();

        displayDumpable("Preview context", modelContext);
        assertNotNull("Null model context", modelContext);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
        assertNotNull("Null model focus context", focusContext);
        ObjectDelta<UserType> userPrimaryDelta = focusContext.getPrimaryDelta();
        assertNotNull("No focus primary delta: " + userPrimaryDelta, userPrimaryDelta);
        PrismAsserts.assertModifications(userPrimaryDelta, 1);
        PrismAsserts.assertPropertyReplace(userPrimaryDelta, UserType.F_FULL_NAME, PolyString.fromOrig("Elaine Threepwood"));

        ObjectDelta<UserType> userSecondaryDelta = focusContext.getSummarySecondaryDelta();
        assertSideEffectiveDeltasOnly("focus secondary delta", userSecondaryDelta);

        Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
        assertNotNull("Null model projection context list", projectionContexts);
        assertEquals("Unexpected number of projection contexts", 3, projectionContexts.size());

        // DEFAULT dummy resource: normal mappings
        ModelProjectionContext accContext = findAccountContext(modelContext, RESOURCE_DUMMY_OID);
        assertNotNull("Null model projection context (default)", accContext);

        assertEquals("Wrong policy decision (default)", SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());
        ObjectDelta<ShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
        assertNull("Unexpected account primary delta (default)", accountPrimaryDelta);

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSummarySecondaryDelta();
        assertNotNull("No account secondary delta (default)", accountSecondaryDelta);
        PrismAsserts.assertModifications(accountSecondaryDelta, 1);
        PrismAsserts.assertPropertyReplace(
                accountSecondaryDelta, DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_PATH, "Elaine Threepwood");

        // RED dummy resource: strong mappings
        accContext = findAccountContext(modelContext, RESOURCE_DUMMY_RED_OID);
        assertNotNull("Null model projection context (red)", accContext);

        assertEquals("Wrong policy decision", SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());
        accountPrimaryDelta = accContext.getPrimaryDelta();
        assertNull("Unexpected account primary delta (red)", accountPrimaryDelta);

        accountSecondaryDelta = accContext.getSummarySecondaryDelta();
        assertNotNull("No account secondary delta (red)", accountSecondaryDelta);
        PrismAsserts.assertModifications(accountSecondaryDelta, 1);
        PrismAsserts.assertPropertyReplace(
                accountSecondaryDelta, DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_PATH, "Elaine Threepwood");

        // BLUE dummy resource: weak mappings
        accContext = findAccountContext(modelContext, RESOURCE_DUMMY_BLUE_OID);
        assertNotNull("Null model projection context (blue)", accContext);

        assertEquals("Wrong policy decision", SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());
        accountPrimaryDelta = accContext.getPrimaryDelta();
        assertNull("Unexpected account primary delta (blue)", accountPrimaryDelta);

        accountSecondaryDelta = accContext.getSummarySecondaryDelta();
        PrismAsserts.assertModifications(accountSecondaryDelta, 1);
        PrismAsserts.assertPropertyDelete(
                accountSecondaryDelta, DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_PATH, "null -- Elaine Marley");
        assertSerializable(modelContext);
    }

    /**
     * Changing USER fullName (replace delta), change account fullname (replace delta).
     */
    @Test
    public void test610ModifyElaineUserAccountDummyReplace() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        ObjectDelta<UserType> userDelta =
                createModifyUserReplaceDelta(USER_ELAINE_OID, UserType.F_FULL_NAME, PolyString.fromOrig("Elaine Threepwood"));
        ObjectDelta<ShadowType> accountDelta = createModifyAccountShadowReplaceAttributeDelta(
                ACCOUNT_SHADOW_ELAINE_DUMMY_OID, getDummyResourceObject(),
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Elaine LeChuck");
        ObjectDelta<ShadowType> accountDeltaBlue = createModifyAccountShadowReplaceAttributeDelta(
                ACCOUNT_SHADOW_ELAINE_DUMMY_BLUE_OID, getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME),
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Elaine LeChuck");
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta, accountDelta,
                accountDeltaBlue);
        display("Input deltas: ", deltas);

        // WHEN
        when();
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, null, task, result);

        // THEN
        then();

        displayDumpable("Preview context", modelContext);
        assertNotNull("Null model context", modelContext);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
        assertNotNull("Null model focus context", focusContext);
        ObjectDelta<UserType> userPrimaryDelta = focusContext.getPrimaryDelta();
        assertNotNull("No focus primary delta: " + userPrimaryDelta, userPrimaryDelta);
        PrismAsserts.assertModifications(userPrimaryDelta, 1);
        PrismAsserts.assertPropertyReplace(userPrimaryDelta, UserType.F_FULL_NAME, PolyString.fromOrig("Elaine Threepwood"));

        ObjectDelta<UserType> userSecondaryDelta = focusContext.getSummarySecondaryDelta();
        assertSideEffectiveDeltasOnly("focus secondary delta", userSecondaryDelta);

        Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
        assertNotNull("Null model projection context list", projectionContexts);
        assertEquals("Unexpected number of projection contexts", 3, projectionContexts.size());

        // DEFAULT dummy resource: normal mappings
        ModelProjectionContext accContext = findAccountContext(modelContext, RESOURCE_DUMMY_OID);
        assertNotNull("Null model projection context (default)", accContext);

        assertEquals("Wrong policy decision (default)", SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());
        ObjectDelta<ShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
        assertNotNull("No account primary delta (default)", accountPrimaryDelta);
        PrismAsserts.assertModifications(accountPrimaryDelta, 1);
        PrismAsserts.assertPropertyReplace(
                accountPrimaryDelta, DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_PATH, "Elaine LeChuck");

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSummarySecondaryDelta();
        assertNull("Unexpected account secondary delta: " + accountSecondaryDelta, accountSecondaryDelta);

        // RED dummy resource: strong mappings
        accContext = findAccountContext(modelContext, RESOURCE_DUMMY_RED_OID);
        assertNotNull("Null model projection context (red)", accContext);

        assertEquals("Wrong policy decision", SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());
        accountPrimaryDelta = accContext.getPrimaryDelta();
        assertNull("Unexpected account primary delta (red)", accountPrimaryDelta);
        accountSecondaryDelta = accContext.getSummarySecondaryDelta();
        assertNotNull("No account secondary delta (red)", accountSecondaryDelta);
        PrismAsserts.assertModifications(accountSecondaryDelta, 1);
        PrismAsserts.assertPropertyReplace(
                accountSecondaryDelta, DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_PATH, "Elaine Threepwood");

        // BLUE dummy resource: weak mappings
        accContext = findAccountContext(modelContext, RESOURCE_DUMMY_BLUE_OID);
        assertNotNull("Null model projection context (blue)", accContext);

        assertEquals("Wrong policy decision", SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());
        accountPrimaryDelta = accContext.getPrimaryDelta();
        assertNotNull("No account primary delta (blue)", accountPrimaryDelta);
        PrismAsserts.assertModifications(accountPrimaryDelta, 1);
        PrismAsserts.assertPropertyReplace(
                accountPrimaryDelta, DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_PATH, "Elaine LeChuck");

        accountSecondaryDelta = accContext.getSummarySecondaryDelta();
        PrismAsserts.assertModifications("account secondary delta (blue)", accountSecondaryDelta, 1);
        PrismAsserts.assertPropertyDelete(
                accountSecondaryDelta, DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_PATH, "null -- Elaine Marley");
        assertSerializable(modelContext);
    }

    @Test
    public void test620AddUserCapsize() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        PrismObject<UserType> user = PrismTestUtil.parseObject(USER_CAPSIZE_FILE);
        addAccountLinkRef(user, ACCOUNT_CAPSIZE_DUMMY_DEFAULT_FILE);
        addAccountLinkRef(user, ACCOUNT_CAPSIZE_DUMMY_RED_FILE);
        addAccountLinkRef(user, ACCOUNT_CAPSIZE_DUMMY_BLUE_FILE);
        ObjectDelta<UserType> userDelta = DeltaFactory.Object.createAddDelta(user);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        // WHEN
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, null, task, result);

        // THEN
        displayDumpable("Preview context", modelContext);
        assertNotNull("Null model context", modelContext);

        assertSuccess(result);

        ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
        assertNotNull("Null model focus context", focusContext);
        ObjectDelta<UserType> userPrimaryDelta = focusContext.getPrimaryDelta();
        assertNotNull("No focus primary delta: " + userPrimaryDelta, userPrimaryDelta);
        PrismAsserts.assertIsAdd(userPrimaryDelta);

        ObjectDelta<UserType> userSecondaryDelta = focusContext.getSummarySecondaryDelta();
        assertSideEffectiveDeltasOnly(userSecondaryDelta, "focus secondary delta", ActivationStatusType.ENABLED);

        Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
        assertNotNull("Null model projection context list", projectionContexts);
        assertEquals("Unexpected number of projection contexts", 3, projectionContexts.size());

        // DEFAULT dummy resource: normal mappings
        ModelProjectionContext accContext = findAccountContext(modelContext, RESOURCE_DUMMY_OID);
        assertNotNull("Null model projection context (default)", accContext);

        assertEquals("Wrong policy decision (default)", SynchronizationPolicyDecision.ADD, accContext.getSynchronizationPolicyDecision());
        ObjectDelta<ShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
        assertNotNull("No account primary delta (default)", accountPrimaryDelta);
        PrismAsserts.assertIsAdd(accountPrimaryDelta);

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSummarySecondaryDelta();
        assertNotNull("No account secondary delta (default)", accountSecondaryDelta);
        PrismAsserts.assertModifications(accountSecondaryDelta, 9);
        PrismAsserts.assertNoItemDelta(accountSecondaryDelta, DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_PATH);

        // RED dummy resource: strong mappings
        accContext = findAccountContext(modelContext, RESOURCE_DUMMY_RED_OID);
        assertNotNull("Null model projection context (red)", accContext);

        assertEquals("Wrong policy decision", SynchronizationPolicyDecision.ADD, accContext.getSynchronizationPolicyDecision());
        accountPrimaryDelta = accContext.getPrimaryDelta();
        assertNotNull("No account primary delta (default)", accountPrimaryDelta);
        PrismAsserts.assertIsAdd(accountPrimaryDelta);

        accountSecondaryDelta = accContext.getSummarySecondaryDelta();
        assertNotNull("No account secondary delta (red)", accountSecondaryDelta);
        PrismAsserts.assertModifications(accountSecondaryDelta, 9);
        PrismAsserts.assertPropertyReplace(
                accountSecondaryDelta, DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_PATH, "Kate Capsize");

        // BLUE dummy resource: weak mappings
        accContext = findAccountContext(modelContext, RESOURCE_DUMMY_BLUE_OID);
        assertNotNull("Null model projection context (blue)", accContext);

        assertEquals("Wrong policy decision", SynchronizationPolicyDecision.ADD, accContext.getSynchronizationPolicyDecision());
        accountPrimaryDelta = accContext.getPrimaryDelta();
        assertNotNull("No account primary delta (default)", accountPrimaryDelta);
        PrismAsserts.assertIsAdd(accountPrimaryDelta);

        accountSecondaryDelta = accContext.getSummarySecondaryDelta();
        assertNotNull("No account secondary delta (default)", accountSecondaryDelta);
        PrismAsserts.assertModifications(accountSecondaryDelta, 10);
        PrismAsserts.assertNoItemDelta(accountSecondaryDelta, DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_PATH);
        assertPasswordDelta(accountSecondaryDelta);
        assertSerializable(modelContext);
    }

    // testing multiple resources with dependencies (dummy -> dummy lemon)
    @Test
    public void test630AddUserRogers() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        PrismObject<UserType> user = USER_ROGERS.get();
        addAccountLinkRef(user, ACCOUNT_ROGERS_DUMMY_DEFAULT_FILE);
        addAccountLinkRef(user, ACCOUNT_ROGERS_DUMMY_LEMON_FILE);
        ObjectDelta<UserType> userDelta = DeltaFactory.Object.createAddDelta(user);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        // WHEN
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, null, task, result);

        // THEN
        displayDumpable("Preview context", modelContext);
        assertNotNull("Null model context", modelContext);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
        assertNotNull("Null model focus context", focusContext);
        ObjectDelta<UserType> userPrimaryDelta = focusContext.getPrimaryDelta();
        assertNotNull("No focus primary delta: " + userPrimaryDelta, userPrimaryDelta);
        PrismAsserts.assertIsAdd(userPrimaryDelta);

        ObjectDelta<UserType> userSecondaryDelta = focusContext.getSummarySecondaryDelta();
        // inbound from ship (explicitly specified) to organizationalUnit (dummy resource)
        // inbound from gossip (computed via outbound) to description (lemon resource)
        assertEffectualDeltas(userSecondaryDelta, "focus secondary delta", ActivationStatusType.ENABLED, 2);

        PrismObject<UserType> finalUser = user.clone();
        userSecondaryDelta.applyTo(finalUser);
        PrismAsserts.assertOrigEqualsPolyStringCollectionUnordered("Wrong organizationalUnit attribute",
                finalUser.asObjectable().getOrganizationalUnit(), "The crew of The Sea Monkey");
        assertEquals("Wrong description attribute", "Rum Rogers Sr. must be the best pirate the  has ever seen", finalUser.asObjectable().getDescription());

        Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
        assertNotNull("Null model projection context list", projectionContexts);
        assertEquals("Unexpected number of projection contexts", 2, projectionContexts.size());

        // DEFAULT dummy resource: normal mappings
        ModelProjectionContext accContext = findAccountContext(modelContext, RESOURCE_DUMMY_OID);
        assertNotNull("Null model projection context (default)", accContext);

        assertEquals("Wrong policy decision (default)", SynchronizationPolicyDecision.ADD, accContext.getSynchronizationPolicyDecision());
        ObjectDelta<ShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
        assertNotNull("No account primary delta (default)", accountPrimaryDelta);
        PrismAsserts.assertIsAdd(accountPrimaryDelta);

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSummarySecondaryDelta();
        assertNotNull("No account secondary delta (default)", accountSecondaryDelta);
        // administrativeStatus (ENABLED), enableTimestamp, name, drink, quote, iteration, iterationToken, password/value
        PrismAsserts.assertModifications(accountSecondaryDelta, 9);
        PrismAsserts.assertNoItemDelta(accountSecondaryDelta, DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_PATH);

        // LEMON dummy resource
        accContext = findAccountContext(modelContext, RESOURCE_DUMMY_LEMON.oid);
        assertNotNull("Null model projection context (lemon)", accContext);

        assertEquals("Wrong policy decision", SynchronizationPolicyDecision.ADD, accContext.getSynchronizationPolicyDecision());
        accountPrimaryDelta = accContext.getPrimaryDelta();
        assertNotNull("No account primary delta (default)", accountPrimaryDelta);
        PrismAsserts.assertIsAdd(accountPrimaryDelta);

        accountSecondaryDelta = accContext.getSummarySecondaryDelta();
        assertNotNull("No account secondary delta (lemon)", accountSecondaryDelta);
        // administrativeStatus (ENABLED), enableTimestamp, ship (from organizationalUnit), name, gossip, water, iteration, iterationToken, password/value
        PrismAsserts.assertModifications(accountSecondaryDelta, 10);
        PrismAsserts.assertPropertyReplace(
                accountSecondaryDelta, DUMMY_ACCOUNT_ATTRIBUTE_SHIP_PATH, "The crew of The Sea Monkey");
        PrismAsserts.assertPropertyReplace(accountSecondaryDelta,
                SchemaConstants.ICFS_NAME_PATH,
                "rogers");
        PrismAsserts.assertPropertyAdd(
                accountSecondaryDelta,
                DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_PATH,
                "Rum Rogers Sr. must be the best pirate the  has ever seen");
        PrismAsserts.assertPropertyReplace(
                accountSecondaryDelta,
                DUMMY_ACCOUNT_ATTRIBUTE_WATER_PATH,
                "pirate Rum Rogers Sr. drinks only rum!");
        assertSerializable(modelContext);
    }

    // The 7xx tests try to do various non-common cases

    /**
     * Enable two accounts at once. Both accounts belongs to the same user. But no user delta is here.
     * This may cause problems when constructing the lens context inside model implementation.
     */
    @Test
    public void test700DisableElaineAccountTwoResources() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        ObjectDelta<ShadowType> accountDeltaDefault = createModifyAccountShadowReplaceDelta(ACCOUNT_SHADOW_ELAINE_DUMMY_OID,
                getDummyResourceObject(), ACTIVATION_ADMINISTRATIVE_STATUS_PATH, ActivationStatusType.DISABLED);
        ObjectDelta<ShadowType> accountDeltaBlue = createModifyAccountShadowReplaceDelta(ACCOUNT_SHADOW_ELAINE_DUMMY_BLUE_OID,
                getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME), ACTIVATION_ADMINISTRATIVE_STATUS_PATH, ActivationStatusType.DISABLED);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(accountDeltaDefault, accountDeltaBlue);
        display("Input deltas: ", deltas);

        // WHEN
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, null, task, result);

        // THEN
        displayDumpable("Preview context", modelContext);
        assertNotNull("Null model context", modelContext);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
        assertNotNull("Null model focus context", focusContext);
        assertNull("Unexpected focus primary delta: " + focusContext.getPrimaryDelta(), focusContext.getPrimaryDelta());

        ObjectDelta<UserType> userSecondaryDelta = focusContext.getSummarySecondaryDelta();
        assertSideEffectiveDeltasOnly("focus secondary delta", userSecondaryDelta);

        Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
        assertNotNull("Null model projection context list", projectionContexts);
        assertEquals("Unexpected number of projection contexts", 3, projectionContexts.size());

        ModelProjectionContext accContextDefault = findAccountContext(modelContext, RESOURCE_DUMMY_OID);
        assertNotNull("Null model projection context (default)", accContextDefault);

        assertEquals("Wrong policy decision (default)", SynchronizationPolicyDecision.KEEP, accContextDefault.getSynchronizationPolicyDecision());
        ObjectDelta<ShadowType> accountPrimaryDelta = accContextDefault.getPrimaryDelta();
        assertNotNull("No account primary delta (default)", accountPrimaryDelta);
        PrismAsserts.assertModifications(accountPrimaryDelta, 1);
        PrismAsserts.assertPropertyReplace(accountPrimaryDelta, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, ActivationStatusType.DISABLED);

        ObjectDelta<ShadowType> accountSecondaryDelta = accContextDefault.getSummarySecondaryDelta();
        PrismAsserts.assertModifications(accountSecondaryDelta, 2);
        assertNotNull("No disableTimestamp delta in account secondary delta (default)",
                accountSecondaryDelta.findPropertyDelta(PATH_ACTIVATION_DISABLE_TIMESTAMP));
        PrismAsserts.assertPropertyReplace(accountSecondaryDelta, SchemaConstants.PATH_ACTIVATION_DISABLE_REASON,
                SchemaConstants.MODEL_DISABLE_REASON_EXPLICIT);
        // the other modification is disable timestamp

        ModelProjectionContext accContextBlue = findAccountContext(modelContext, RESOURCE_DUMMY_BLUE_OID);
        assertNotNull("Null model projection context (blue)", accContextBlue);

        assertEquals("Wrong policy decision (blue)", SynchronizationPolicyDecision.KEEP, accContextBlue.getSynchronizationPolicyDecision());
        ObjectDelta<ShadowType> accountPrimaryDeltaBlue = accContextBlue.getPrimaryDelta();
        assertNotNull("No account primary delta (blue)", accountPrimaryDeltaBlue);
        PrismAsserts.assertModifications(accountPrimaryDeltaBlue, 1);
        PrismAsserts.assertPropertyReplace(accountPrimaryDeltaBlue, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, ActivationStatusType.DISABLED);

        ObjectDelta<ShadowType> accountSecondaryDeltaBlue = accContextBlue.getSummarySecondaryDelta();
        PrismAsserts.assertModifications(accountSecondaryDeltaBlue, 2);
        assertNotNull("No disableTimestamp delta in account secondary delta (blue)",
                accountSecondaryDeltaBlue.findPropertyDelta(PATH_ACTIVATION_DISABLE_TIMESTAMP));
        PrismAsserts.assertPropertyReplace(accountSecondaryDeltaBlue, SchemaConstants.PATH_ACTIVATION_DISABLE_REASON,
                SchemaConstants.MODEL_DISABLE_REASON_EXPLICIT);
        assertSerializable(modelContext);
    }

    private ModelProjectionContext findAccountContext(ModelContext<UserType> modelContext, String resourceOid) {
        Collection<? extends ModelProjectionContext> accountContexts = modelContext.findProjectionContexts(
                new ProjectionContextFilter(resourceOid, ACCOUNT, SchemaConstants.INTENT_DEFAULT));
        return MiscUtil.extractSingletonRequired(
                accountContexts,
                () -> new IllegalStateException("Multiple account/default contexts: " + accountContexts),
                () -> new IllegalStateException("No account/default contexts: " + modelContext.getProjectionContexts()));
    }

    /**
     * MID-3845; now the assignment is in the zero set
     */
    @Test
    public void test710PreviewGuybrushHavingRoleSailorOwner() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        assignRole(USER_GUYBRUSH_OID, ROLE_SAILOR_OID, SchemaConstants.ORG_OWNER, task, result);

        ObjectDelta<UserType> empty = prismContext.deltaFor(UserType.class).asObjectDelta(USER_GUYBRUSH_OID);

        // WHEN
        when();
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(singleton(empty),
                executeOptions().evaluateAllAssignmentRelationsOnRecompute(), task, result);

        // THEN
        then();
        displayDumpable("Preview context", modelContext);
        assertNotNull("Null model context", modelContext);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
        assertNotNull("Model focus context missing", focusContext);

        DeltaSetTriple<? extends EvaluatedAssignment> evaluatedAssignmentTriple = modelContext.getEvaluatedAssignmentTriple();
        displayDumpable("evaluatedAssignmentTriple", evaluatedAssignmentTriple);

        assertEquals("Wrong # of evaluated assignments in zero set", 1, evaluatedAssignmentTriple.getZeroSet().size());
        EvaluatedAssignment evaluatedAssignment = evaluatedAssignmentTriple.getZeroSet().iterator().next();
        assertNotNull("Target of evaluated assignment is null", evaluatedAssignment.getTarget());
        assertEquals("Wrong # of zero-set roles in evaluated assignment", 1, evaluatedAssignment.getRoles().getZeroSet().size());
        assertSerializable(modelContext);
    }

    /** MID-6610 */
    @Test
    public void test750PreviewAddUserShadowInsufficientPassword() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        addObject(VALUE_POLICY_PASSWORD, task, result);
        addObject(SECURITY_POLICY, task, result);
        addObject(ROLE_SIMPLE, task, result);
        addObject(USER_JOE, task, result);

        assertSuccess(result);

        when("We're attempting to create account (shadow) with password that doesn't match password policy");

        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .add(new AssignmentType()
                        .targetRef(ROLE_SIMPLE.oid, RoleType.COMPLEX_TYPE))
                .asObjectDelta(USER_JOE.oid);

        ModelContext<UserType> modelContext =
                modelInteractionService.previewChanges(singleton(delta), null, task, result);

        result.computeStatus();

        AssertJUnit.assertNotNull(modelContext);
        AssertJUnit.assertEquals(1, modelContext.getProjectionContexts().size());

        ModelProjectionContext projectionContext = modelContext.getProjectionContexts().iterator().next();
        ObjectDelta<ShadowType> summaryDelta = projectionContext.getSummaryDelta();

        // in reality, it's, add shadow without shadow resourceRef, kind, intent.
        PrismAsserts.assertIsModify(summaryDelta);

        then("Delta should be marked as broken and preview should show add shadow delta");

        ModelContextVisualization mcVisualization = modelInteractionService.visualizeModelContext(modelContext, task, result);
        List<? extends Visualization> secondary = mcVisualization.getSecondary();

        AssertJUnit.assertEquals(1, secondary.size());

        Visualization scene = secondary.get(0);
        AssertJUnit.assertTrue(scene.isBroken());
        AssertJUnit.assertEquals(ChangeType.ADD, scene.getChangeType());

        ObjectDelta<?> sourceDelta = scene.getSourceDelta();
        PrismAsserts.assertPropertyAdd(sourceDelta, ItemPath.create(ShadowType.F_KIND), ACCOUNT);
        PrismAsserts.assertPropertyAdd(sourceDelta, ItemPath.create(ShadowType.F_INTENT), "default");
        PrismAsserts.assertReferenceAdd(sourceDelta, ShadowType.F_RESOURCE_REF, RESOURCE_SIMPLE.oid);
    }

    /**
     * Having projection with both outbound and inbound mappings.
     *
     * When user LC state goes to `archived`, the "preview" and real execution behavior should be the same:
     * account is deleted, but the inbounds are not evaluated, so the user remains unchanged.
     *
     * MID-9853.
     *
     * This originally failed, because:
     *
     * - The real behavior is that when LC is `archived`, shadow gets deleted
     * (wave 0). In wave 1, the rotten projection context is removed. Hence, no inbounds are processed on that already-deleted
     * shadow.
     *
     * - The preview behavior is that the projection context is not rotten, and not removed in wave 1 (actually, that's a good
     * thing; otherwise, we couldn't see the deletion deltas). But that means that inbounds are processed, removing user's
     * `name`, which leads to a "No name in new object" exception.
     *
     * The fix was to replace previewChanges by simulations.
     */
    @Test
    public void test760PreviewArchival() throws CommonException {
        var task = getTestTask();
        var result = task.getResult();
        var userName = getTestNameShort();

        given("user with assigned 'account/test'");
        var userOid = addObject(
                new UserType()
                        .name(userName)
                        .assignment(RESOURCE_DUMMY_MISC.assignmentWithConstructionOf(ACCOUNT, INTENT_DEFAULT)),
                task, result);
        assertUserBefore(userOid)
                .assertLiveLinks(1);

        when("user archival is previewed");
        var modelContext = previewChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_LIFECYCLE_STATE)
                        .replace(SchemaConstants.LIFECYCLE_ARCHIVED)
                        .asObjectDelta(userOid),
                null, task, result);

        then("shadow deletion is there");
        assertSuccess(result);
        assertThat(modelContext.getProjectionContexts())
                .as("projection contexts")
                .singleElement()
                .extracting(projCtx -> projCtx.getSynchronizationPolicyDecision())
                .as("sync policy decision")
                .isEqualTo(SynchronizationPolicyDecision.DELETE);
    }

    /**
     * Tests the situation when a new account is created (in simulation), and then its loading is required in second wave
     * because of strong inbound mappings.
     *
     * MID-10039
     */
    @Test
    public void test770CreatingAccountWithStrongInboundsForNewUser() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var userName = getTestNameShort();

        given("user with assigned 'account/default' on bidirectional resource (in memory)");
        var user = new UserType()
                .name(userName)
                .assignment(RESOURCE_DUMMY_BIDIRECTIONAL.assignmentWithConstructionOf(ACCOUNT, INTENT_DEFAULT));

        when("user is added (in preview mode)");
        var modelContext = previewChanges(user.asPrismObject().createAddDelta(), null, task, result);

        then("there is a single projection context with ADD decision");
        displayDumpable("model context", modelContext);
        assertThat(modelContext.getProjectionContexts())
                .as("projection contexts")
                .singleElement()
                .extracting(projCtx -> projCtx.getSynchronizationPolicyDecision())
                .as("sync decision")
                .isEqualTo(SynchronizationPolicyDecision.ADD);
    }

    /**
     * As {@link #test770CreatingAccountWithStrongInboundsForNewUser()} but having the user already in the repository.
     *
     * MID-10039
     */
    @Test
    public void test780CreatingAccountWithStrongInboundsForExistingUser() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var userName = getTestNameShort();

        given("user in repo");
        var user = new UserType()
                .name(userName);
        var userOid = addObject(user, task, result);

        when("assignment of 'account/default' on bidirectional resource  is added (in preview mode)");
        var modelContext =
                previewChanges(
                        deltaFor(UserType.class)
                                .item(UserType.F_ASSIGNMENT)
                                .add(RESOURCE_DUMMY_BIDIRECTIONAL.assignmentWithConstructionOf(ACCOUNT, INTENT_DEFAULT))
                                .asObjectDelta(userOid),
                        null, task, result);

        then("there is a single projection context with ADD decision");
        displayDumpable("model context", modelContext);
        assertThat(modelContext.getProjectionContexts())
                .as("projection contexts")
                .singleElement()
                .extracting(projCtx -> projCtx.getSynchronizationPolicyDecision())
                .as("sync decision")
                .isEqualTo(SynchronizationPolicyDecision.ADD);
    }

    private record Checkers(
            ObjectChecker<ModelContext<UserType>> ctxChecker,
            ObjectChecker<TestSimulationResult> simChecker) {
    }
}
