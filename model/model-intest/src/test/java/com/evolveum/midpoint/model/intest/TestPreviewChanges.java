/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import static java.util.Collections.singleton;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.model.api.ModelExecuteOptions.createEvaluateAllAssignmentRelationsOnRecompute;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.PATH_ACTIVATION_DISABLE_TIMESTAMP;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.model.api.context.*;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.ObjectChecker;
import com.evolveum.midpoint.test.ObjectSource;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestPreviewChanges extends AbstractInitializedModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/preview");

    // LEMON dummy resource has a STRICT dependency on default dummy resource
    protected static final File RESOURCE_DUMMY_LEMON_FILE = new File(TEST_DIR, "resource-dummy-lemon.xml");
    protected static final String RESOURCE_DUMMY_LEMON_OID = "10000000-0000-0000-0000-000000000504";
    protected static final String RESOURCE_DUMMY_LEMON_NAME = "lemon";

    static final File USER_ROGERS_FILE = new File(TEST_DIR, "user-rogers.xml");
    static final File ACCOUNT_ROGERS_DUMMY_DEFAULT_FILE = new File(TEST_DIR, "account-rogers-dummy-default.xml");
    static final File ACCOUNT_ROGERS_DUMMY_LEMON_FILE = new File(TEST_DIR, "account-rogers-dummy-lemon.xml");

    private String accountGuybrushOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initDummyResourcePirate(RESOURCE_DUMMY_LEMON_NAME,
                RESOURCE_DUMMY_LEMON_FILE, RESOURCE_DUMMY_LEMON_OID, initTask, initResult);

        // Elaine is in inconsistent state. Account attributes do not match the mappings.
        // We do not want that here, as it would add noise to preview operations.
        reconcileUser(USER_ELAINE_OID, initTask, initResult);
    }

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

        ObjectChecker<ModelContext<UserType>> checker = modelContext -> assertAddAccount(modelContext, false);

        modifyUserAddAccountImplicit(accountSource, checker);
        modifyUserAddAccountExplicit(accountSource, checker);
        modifyUserAddAccountImplicitExplicitSame(accountSource, checker);
        modifyUserAddAccountImplicitExplicitSameReverse(accountSource, checker);
        modifyUserAddAccountImplicitExplicitEqual(accountSource, checker);
        modifyUserAddAccountImplicitExplicitEqualReverse(accountSource, checker);
        modifyUserAddAccountImplicitExplicitNotEqual(accountSource);
        modifyUserAddAccountImplicitExplicitNotEqualReverse(accountSource);
    }

    @Test
    public void test101ModifyUserAddAccountNoAttributesBundle() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        ObjectSource<PrismObject<ShadowType>> accountSource = new ObjectSource<PrismObject<ShadowType>>() {
            @Override
            public PrismObject<ShadowType> get() {
                try {
                    PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);
                    account.removeContainer(ShadowType.F_ATTRIBUTES);
                    return account;
                } catch (SchemaException | IOException e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
            }
        };

        ObjectChecker<ModelContext<UserType>> checker = new ObjectChecker<ModelContext<UserType>>() {
            @Override
            public void check(ModelContext<UserType> modelContext) {
                assertAddAccount(modelContext, true);
            }
        };

        modifyUserAddAccountImplicit(accountSource, checker);
        modifyUserAddAccountExplicit(accountSource, checker);
        modifyUserAddAccountImplicitExplicitSame(accountSource, checker);
        modifyUserAddAccountImplicitExplicitSameReverse(accountSource, checker);
        modifyUserAddAccountImplicitExplicitEqual(accountSource, checker);
        modifyUserAddAccountImplicitExplicitEqualReverse(accountSource, checker);
        modifyUserAddAccountImplicitExplicitNotEqual(accountSource);
        modifyUserAddAccountImplicitExplicitNotEqualReverse(accountSource);
    }

    private void modifyUserAddAccountImplicit(ObjectSource<PrismObject<ShadowType>> accountSource,
            ObjectChecker<ModelContext<UserType>> checker) throws Exception {
        // GIVEN
        Task task = createPlainTask("modifyUserAddAccountImplicit");
        OperationResult result = task.getResult();

        PrismObject<ShadowType> account = accountSource.get();

        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(UserType.class, USER_JACK_OID);
        PrismReferenceValue accountRefVal = itemFactory().createReferenceValue();
        accountRefVal.setObject(account);
        ReferenceDelta accountRefDelta = prismContext.deltaFactory().reference().createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
        userDelta.addModification(accountRefDelta);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        doPreview(deltas, checker, task, result);
    }

    private void modifyUserAddAccountExplicit(ObjectSource<PrismObject<ShadowType>> accountSource,
            ObjectChecker<ModelContext<UserType>> checker) throws Exception {
        // GIVEN
        Task task = createPlainTask("modifyUserAddAccountExplicit");
        OperationResult result = task.getResult();

        PrismObject<ShadowType> account = accountSource.get();

        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(UserType.class, USER_JACK_OID);
        ObjectDelta<ShadowType> accountDelta = account.createAddDelta();
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta, accountDelta);

        doPreview(deltas, checker, task, result);
    }

    private void modifyUserAddAccountImplicitExplicitSame(
            ObjectSource<PrismObject<ShadowType>> accountSource,
            ObjectChecker<ModelContext<UserType>> checker) throws Exception {
        // GIVEN
        Task task = createPlainTask("modifyUserAddAccountImplicitExplicitSame");
        OperationResult result = task.getResult();

        PrismObject<ShadowType> account = accountSource.get();

        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(UserType.class, USER_JACK_OID);
        PrismReferenceValue accountRefVal = itemFactory().createReferenceValue();
        accountRefVal.setObject(account);
        ReferenceDelta accountRefDelta = prismContext.deltaFactory().reference().createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
        userDelta.addModification(accountRefDelta);
        ObjectDelta<ShadowType> accountDelta = account.createAddDelta();
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta, accountDelta);

        doPreview(deltas, checker, task, result);
    }

    private void modifyUserAddAccountImplicitExplicitSameReverse(
            ObjectSource<PrismObject<ShadowType>> accountSource,
            ObjectChecker<ModelContext<UserType>> checker) throws Exception {
        // GIVEN
        Task task = createPlainTask("modifyUserAddAccountImplicitExplicitSameReverse");
        OperationResult result = task.getResult();

        PrismObject<ShadowType> account = accountSource.get();

        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(UserType.class, USER_JACK_OID);
        PrismReferenceValue accountRefVal = itemFactory().createReferenceValue();
        accountRefVal.setObject(account);
        ReferenceDelta accountRefDelta = prismContext.deltaFactory().reference().createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
        userDelta.addModification(accountRefDelta);
        ObjectDelta<ShadowType> accountDelta = account.createAddDelta();
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(accountDelta, userDelta);

        doPreview(deltas, checker, task, result);
    }

    private void modifyUserAddAccountImplicitExplicitEqual(
            ObjectSource<PrismObject<ShadowType>> accountSource,
            ObjectChecker<ModelContext<UserType>> checker) throws Exception {
        // GIVEN
        Task task = createPlainTask("modifyUserAddAccountImplicitExplicitEqual");
        OperationResult result = task.getResult();

        PrismObject<ShadowType> account = accountSource.get();

        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(UserType.class, USER_JACK_OID);
        PrismReferenceValue accountRefVal = itemFactory().createReferenceValue();
        accountRefVal.setObject(account.clone());
        ReferenceDelta accountRefDelta = prismContext.deltaFactory().reference().createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
        userDelta.addModification(accountRefDelta);
        ObjectDelta<ShadowType> accountDelta = account.createAddDelta();
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta, accountDelta);

        doPreview(deltas, checker, task, result);
    }

    private void modifyUserAddAccountImplicitExplicitEqualReverse(
            ObjectSource<PrismObject<ShadowType>> accountSource,
            ObjectChecker<ModelContext<UserType>> checker) throws Exception {
        // GIVEN
        Task task = createPlainTask("modifyUserAddAccountImplicitExplicitEqualReverse");
        OperationResult result = task.getResult();

        PrismObject<ShadowType> account = accountSource.get();

        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(UserType.class, USER_JACK_OID);
        PrismReferenceValue accountRefVal = itemFactory().createReferenceValue();
        accountRefVal.setObject(account.clone());
        ReferenceDelta accountRefDelta = prismContext.deltaFactory().reference().createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
        userDelta.addModification(accountRefDelta);
        ObjectDelta<ShadowType> accountDelta = account.createAddDelta();
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(accountDelta, userDelta);

        doPreview(deltas, checker, task, result);
    }

    private void modifyUserAddAccountImplicitExplicitNotEqual(
            ObjectSource<PrismObject<ShadowType>> accountSource) throws Exception {
        // GIVEN
        Task task = createPlainTask("modifyUserAddAccountImplicitExplicitNotEqual");
        OperationResult result = task.getResult();

        PrismObject<ShadowType> account = accountSource.get();

        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(UserType.class, USER_JACK_OID);
        PrismReferenceValue accountRefVal = itemFactory().createReferenceValue();
        accountRefVal.setObject(account.clone());
        ReferenceDelta accountRefDelta = prismContext.deltaFactory().reference().createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
        userDelta.addModification(accountRefDelta);
        // Let's make the account different. This should cause the preview to fail
        account.asObjectable().setDescription("aye!");
        ObjectDelta<ShadowType> accountDelta = account.createAddDelta();
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta, accountDelta);

        doPreviewFail(deltas, task, result);
    }

    private void modifyUserAddAccountImplicitExplicitNotEqualReverse(
            ObjectSource<PrismObject<ShadowType>> accountSource) throws Exception {
        // GIVEN
        Task task = createPlainTask("modifyUserAddAccountImplicitExplicitNotEqualReverse");
        OperationResult result = task.getResult();

        PrismObject<ShadowType> account = accountSource.get();

        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(UserType.class, USER_JACK_OID);
        PrismReferenceValue accountRefVal = itemFactory().createReferenceValue();
        accountRefVal.setObject(account.clone());
        ReferenceDelta accountRefDelta = prismContext.deltaFactory().reference().createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
        userDelta.addModification(accountRefDelta);
        // Let's make the account different. This should cause the preview to fail
        account.asObjectable().setDescription("aye!");
        ObjectDelta<ShadowType> accountDelta = account.createAddDelta();
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(accountDelta, userDelta);

        doPreviewFail(deltas, task, result);
    }

    private void doPreview(Collection<ObjectDelta<? extends ObjectType>> deltas,
            ObjectChecker<ModelContext<UserType>> checker, Task task, OperationResult result)
            throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException,
            ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {
        display("Input deltas: ", deltas);

        // WHEN
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, new ModelExecuteOptions(), task, result);

        // THEN
        displayDumpable("Preview context", modelContext);
        checker.check(modelContext);

        assertSuccess(result);
    }

    private void doPreviewFail(
            Collection<ObjectDelta<? extends ObjectType>> deltas, Task task, OperationResult result)
            throws PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException,
            ObjectAlreadyExistsException, CommunicationException, ConfigurationException,
            SecurityViolationException {
        display("Input deltas: ", deltas);

        try {
            // WHEN
            modelInteractionService.previewChanges(deltas, new ModelExecuteOptions(), task, result);
            AssertJUnit.fail("Expected exception, but it haven't come");
        } catch (SchemaException e) {
            displayExpectedException(e);
        }

        result.computeStatus();
        TestUtil.assertFailure(result);
    }

    private void assertAddAccount(ModelContext<UserType> modelContext, boolean expectFullNameDelta) {
        assertNotNull("Null model context", modelContext);

        ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
        assertNotNull("Null model focus context", focusContext);
        assertNull("Unexpected focus primary delta: " + focusContext.getPrimaryDelta(), focusContext.getPrimaryDelta());
        assertSideEffectiveDeltasOnly(focusContext.getSecondaryDelta(), "focus secondary delta", ActivationStatusType.ENABLED);

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
        assertEquals(getDummyResourceController().getAccountObjectClass(),
                accountToAddPrimary.findProperty(ShadowType.F_OBJECT_CLASS).getRealValue());
        PrismReference resourceRef = accountToAddPrimary.findReference(ShadowType.F_RESOURCE_REF);
        assertEquals(getDummyResourceObject().getOid(), resourceRef.getOid());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
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
                .assertObjectForms(1)
                .assertUserDashboardWidgets(0)
                .assertObjectCollectionViews(3);

        RichHyperlinkType link = compiledGuiProfile.getUserDashboardLink().get(0);
        assertEquals("Bad link label", "Foo", link.getLabel());
        assertEquals("Bad link targetUrl", "/foo", link.getTargetUrl());

        assertEquals("Bad timezone targetUrl", "Jamaica", compiledGuiProfile.getDefaultTimezone());
    }

    @Test
    public void test150GetGuybrushRefinedObjectClassDef() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<ShadowType> shadow = getShadowModel(ACCOUNT_SHADOW_GUYBRUSH_OID);

        // WHEN
        RefinedObjectClassDefinition rOCDef = modelInteractionService.getEditObjectClassDefinition(shadow,
                getDummyResourceObject(), AuthorizationPhaseType.REQUEST, task, result);

        // THEN
        assertSuccess(result);

        displayDumpable("Refined object class", rOCDef);
        assertNotNull("Null config", rOCDef);

        display("Password credentials outbound", rOCDef.getPasswordOutbound());
        assertNotNull("Assert not null", rOCDef.getPasswordOutbound());
    }

    @Test
    public void test200ModifyUserGuybrushDeleteAccount() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_GUYBRUSH_DUMMY_FILE);

        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(UserType.class, USER_GUYBRUSH_OID);
        PrismReferenceValue accountRefVal = itemFactory().createReferenceValue();
        accountRefVal.setObject(account);
        ReferenceDelta accountDelta = prismContext.deltaFactory().reference().createModificationDelete(UserType.F_LINK_REF, getUserDefinition(), account);
        userDelta.addModification(accountDelta);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
        display("Input deltas: ", deltas);

        // WHEN
        when();
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, new ModelExecuteOptions(), task, result);

        // THEN
        then();
        displayDumpable("Preview context", modelContext);
        assertNotNull("Null model context", modelContext);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
        assertNotNull("Null model focus context", focusContext);
        assertNull("Unexpected focus primary delta: " + focusContext.getPrimaryDelta(), focusContext.getPrimaryDelta());
        assertSideEffectiveDeltasOnly("focus secondary delta", focusContext.getSecondaryDelta());

        Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
        assertNotNull("Null model projection context list", projectionContexts);
        assertEquals("Unexpected number of projection contexts", 1, projectionContexts.size());
        ModelProjectionContext accContext = projectionContexts.iterator().next();
        assertNotNull("Null model projection context", accContext);

        assertEquals("Wrong policy decision", SynchronizationPolicyDecision.DELETE, accContext.getSynchronizationPolicyDecision());
        ObjectDelta<ShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
        assertEquals(ChangeType.DELETE, accountPrimaryDelta.getChangeType());

    }

    @Test
    public void test210GuybrushAddAccount() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);
        ObjectDelta<ShadowType> accountDelta = DeltaFactory.Object.createAddDelta(account);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(accountDelta);
        display("Input deltas: ", deltas);

        // WHEN
        when();
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, new ModelExecuteOptions(), task, result);

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
        assertEquals(getDummyResourceController().getAccountObjectClass(),
                accountToAddPrimary.findProperty(ShadowType.F_OBJECT_CLASS).getRealValue());
        PrismReference resourceRef = accountToAddPrimary.findReference(ShadowType.F_RESOURCE_REF);
        assertEquals(getDummyResourceObject().getOid(), resourceRef.getOid());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        PrismAsserts.assertModifications(accountSecondaryDelta, 1);
    }

    @Test
    public void test212ModifyUserAddAccountRef() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(UserType.class, USER_GUYBRUSH_OID);
        ReferenceDelta accountDelta = prismContext.deltaFactory().reference().createModificationAdd(UserType.F_LINK_REF, getUserDefinition(),
                ACCOUNT_SHADOW_GUYBRUSH_OID);
        userDelta.addModification(accountDelta);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
        displayDumpable("Input deltas: ", userDelta);

        // WHEN
        when();
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, new ModelExecuteOptions(), task, result);

        // THEN
        then();
        displayDumpable("Preview context", modelContext);
        assertNotNull("Null model context", modelContext);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
        assertNotNull("Null model focus context", focusContext);
        assertNull("Unexpected focus primary delta: " + focusContext.getPrimaryDelta(), focusContext.getPrimaryDelta());

        assertSideEffectiveDeltasOnly("focus secondary delta", focusContext.getSecondaryDelta());

        Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
        assertNotNull("Null model projection context list", projectionContexts);
        assertEquals("Unexpected number of projection contexts", 1, projectionContexts.size());
        ModelProjectionContext accContext = projectionContexts.iterator().next();
        assertNotNull("Null model projection context", accContext);

        assertEquals("Wrong policy decision", SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());
        ObjectDelta<ShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
        assertNull("Unexpected account primary delta", accountPrimaryDelta);

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertEquals("Unexpected size of account secondary delta: " + accountSecondaryDelta, 2, accountSecondaryDelta.getModifications().size());
        PrismAsserts.assertPropertyAdd(accountSecondaryDelta,
                dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME), "rum");
        PrismAsserts.assertPropertyAdd(accountSecondaryDelta,
                dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME), "Arr!");
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
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(MiscSchemaUtil.createCollection(delta),
                null, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertPreviewJackAssignRolePirate(modelContext);
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
                .assertNone();

        ObjectDelta<UserType> delta = createAssignmentAssignmentHolderDelta(UserType.class, USER_JACK_OID,
                ROLE_PIRATE_OID, RoleType.COMPLEX_TYPE, null, null, null, true);

        // WHEN
        when();
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(MiscSchemaUtil.createCollection(delta),
                ModelExecuteOptions.createReconcile(), task, result);

        // THEN
        then();
        assertSuccess(result);

        assertPreviewJackAssignRolePirate(modelContext);
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

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
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
        ModelExecuteOptions.createReconcile();

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
        assertNull("Unexpected focus secondary delta", focusContext.getSecondaryDelta());

        Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
        assertNotNull("Null model projection context list", projectionContexts);
        assertEquals("Unexpected number of projection contexts", 1, projectionContexts.size());
        ModelProjectionContext accContext = projectionContexts.iterator().next();
        assertNotNull("Null model projection context", accContext);
        assertNotNull("Missing account primary delta", accContext.getPrimaryDelta());
        assertNull("Unexpected account secondary delta", accContext.getSecondaryDelta());
        assertEquals(ChangeType.MODIFY, accContext.getPrimaryDelta().getChangeType());

        assertAccountDefaultDummyAttributeModify(accContext.getPrimaryDelta(),
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME,
                null, // old
                null, // add
                null, // delete
                new String[] { "Mighty Pirate Guybrush Threepwood" });  // replace

        PrismAsserts.assertModifications(accContext.getPrimaryDelta(), 1);
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

        ObjectDelta<ShadowType> accountDelta = prismContext.deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
                accountGuybrushOid, dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
                "The Mad Monkey");

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
        assertNotNull("Missing focus secondary delta", focusContext.getSecondaryDelta());

        Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
        assertNotNull("Null model projection context list", projectionContexts);
        assertEquals("Unexpected number of projection contexts", 1, projectionContexts.size());
        ModelProjectionContext accContext = projectionContexts.iterator().next();
        assertNotNull("Null model projection context", accContext);
        assertNotNull("Missing account primary delta", accContext.getPrimaryDelta());
        assertNull("Unexpected account secondary delta", accContext.getSecondaryDelta());
        assertEquals(ChangeType.MODIFY, accContext.getPrimaryDelta().getChangeType());

        assertAccountDefaultDummyAttributeModify(accContext.getPrimaryDelta(),
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME,
                null, // old
                null, // add
                null, // delete
                new String[] { "The Mad Monkey" });  // replace

        PrismAsserts.assertModifications(accContext.getPrimaryDelta(), 1);
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

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
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

        ModelExecuteOptions options = ModelExecuteOptions.createReconcile();

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

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
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

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertEquals(ChangeType.MODIFY, accountSecondaryDelta.getChangeType());

        assertAccountDefaultDummyAttributeModify(accountSecondaryDelta,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
                new String[] { RESOURCE_DUMMY_DRINK }, // old
                new String[] { ROLE_SAILOR_DRINK }, // add
                null, // delete
                null);  // replace

        PrismAsserts.assertModifications(accountSecondaryDelta, 1);
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

        DeltaSetTriple<? extends EvaluatedAssignment<?>> evaluatedAssignmentTriple = modelContext.getEvaluatedAssignmentTriple();
        displayDumpable("evaluatedAssignmentTriple", evaluatedAssignmentTriple);

        assertEquals("Wrong # of evaluated assignments in plus set", 1, evaluatedAssignmentTriple.getPlusSet().size());
        EvaluatedAssignment<?> evaluatedAssignment = evaluatedAssignmentTriple.getPlusSet().iterator().next();
        assertNotNull("Target of evaluated assignment is null", evaluatedAssignment.getTarget());
        assertEquals("Wrong # of zero-set roles in evaluated assignment", 1, evaluatedAssignment.getRoles().getZeroSet().size());
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

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertEquals(ChangeType.MODIFY, accountSecondaryDelta.getChangeType());

        assertAccountDummyAttributeModify(accountSecondaryDelta,
                RESOURCE_DUMMY_RELATIVE_NAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME,
                null, // old
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
                ModelExecuteOptions.createReconcile(), task, result);

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

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
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

    private <T> void assertAccountDefaultDummyAttributeModify(ObjectDelta<ShadowType> accountDelta,
            String attrName,
            T[] expectedOldValues, T[] expectedAddValues, T[] expectedDeleteValues, T[] expectedReplaceValues) {
        ItemPath itemPath = getDummyResourceController().getAttributePath(attrName);
        assertAccountItemModify(accountDelta, itemPath, expectedOldValues, expectedAddValues, expectedDeleteValues, expectedReplaceValues);
    }

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
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, new ModelExecuteOptions(), task, result);

        // THEN
        displayDumpable("Preview context", modelContext);
        assertNotNull("Null model context", modelContext);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
        assertNotNull("Null model focus context", focusContext);
        assertNull("Unexpected focus primary delta: " + focusContext.getPrimaryDelta(), focusContext.getPrimaryDelta());

        assertSideEffectiveDeltasOnly("focus secondary delta", focusContext.getSecondaryDelta());

        Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
        assertNotNull("Null model projection context list", projectionContexts);
        assertEquals("Unexpected number of projection contexts", 3, projectionContexts.size());

        ModelProjectionContext accContext = modelContext.findProjectionContext(
                new ResourceShadowDiscriminator(RESOURCE_DUMMY_OID, ShadowKindType.ACCOUNT, null, null, false));
        assertNotNull("Null model projection context", accContext);

        assertEquals("Wrong policy decision", SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());
        ObjectDelta<ShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
        assertNotNull("No account primary delta", accountPrimaryDelta);
        PrismAsserts.assertModifications(accountPrimaryDelta, 1);
        PrismAsserts.assertPropertyReplace(accountPrimaryDelta,
                getAttributePath(getDummyResourceObject(), DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME),
                "Elaine Threepwood");

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNull("Unexpected account secondary delta: " + accountSecondaryDelta, accountSecondaryDelta);
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
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, new ModelExecuteOptions(), task, result);

        // THEN
        displayDumpable("Preview context", modelContext);
        assertNotNull("Null model context", modelContext);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
        assertNotNull("Null model focus context", focusContext);
        assertNull("Unexpected focus primary delta: " + focusContext.getPrimaryDelta(), focusContext.getPrimaryDelta());

        assertSideEffectiveDeltasOnly("focus secondary delta", focusContext.getSecondaryDelta());

        Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
        assertNotNull("Null model projection context list", projectionContexts);
        assertEquals("Unexpected number of projection contexts", 3, projectionContexts.size());

        ModelProjectionContext accContext = modelContext.findProjectionContext(
                new ResourceShadowDiscriminator(RESOURCE_DUMMY_OID, ShadowKindType.ACCOUNT, null, null, false));
        assertNotNull("Null model projection context", accContext);

        assertEquals("Wrong policy decision", SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());
        ObjectDelta<ShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
        assertNotNull("No account primary delta", accountPrimaryDelta);
        PrismAsserts.assertModifications(accountPrimaryDelta, 1);
        PrismAsserts.assertPropertyAdd(accountPrimaryDelta,
                getDummyResourceController().getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME), "Elaine Threepwood");
        PrismAsserts.assertPropertyDelete(accountPrimaryDelta,
                getDummyResourceController().getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME), "Elaine Marley");

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNull("Unexpected account secondary delta: " + accountSecondaryDelta, accountSecondaryDelta);
    }

    /**
     * Changing ACCOUNT fullname (replace delta), no user changes.
     * Attempt to make a change to a single-valued attribute or which there is already a strong mapping.
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
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, new ModelExecuteOptions(), task, result);

        // THEN
        then();
        displayDumpable("Preview context", modelContext);
        assertPartialError(result);
    }

    /**
     * Changing ACCOUNT fullname (add/delete delta), no user changes.
     * Attempt to make a change to a single-valued attribute or which there is already a strong mapping.
     * As it cannot have both values (from the delta and from the mapping) the preview should fail.
     */
    @Test
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
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, new ModelExecuteOptions(), task, result);
        displayDumpable("Preview context", modelContext);

        // THEN
        then();
        assertPartialError(result);
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
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, new ModelExecuteOptions(), task, result);

        // THEN
        displayDumpable("Preview context", modelContext);
        assertNotNull("Null model context", modelContext);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
        assertNotNull("Null model focus context", focusContext);
        assertNull("Unexpected focus primary delta: " + focusContext.getPrimaryDelta(), focusContext.getPrimaryDelta());

        ObjectDelta<UserType> userSecondaryDelta = focusContext.getSecondaryDelta();
        assertSideEffectiveDeltasOnly("focus secondary delta", userSecondaryDelta);

        Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
        assertNotNull("Null model projection context list", projectionContexts);
        assertEquals("Unexpected number of projection contexts", 3, projectionContexts.size());

        ModelProjectionContext accContext = modelContext.findProjectionContext(
                new ResourceShadowDiscriminator(RESOURCE_DUMMY_BLUE_OID, ShadowKindType.ACCOUNT, null, null, false));
        assertNotNull("Null model projection context", accContext);

        assertEquals("Wrong policy decision", SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());
        ObjectDelta<ShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
        assertNotNull("No account primary delta", accountPrimaryDelta);
        PrismAsserts.assertModifications(accountPrimaryDelta, 1);
        PrismAsserts.assertPropertyReplace(accountPrimaryDelta,
                getAttributePath(getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME), DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME),
                "Elaine Threepwood");

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNull("Unexpected account secondary delta: " + accountSecondaryDelta, accountSecondaryDelta);
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
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, new ModelExecuteOptions(), task, result);

        // THEN
        displayDumpable("Preview context", modelContext);
        assertNotNull("Null model context", modelContext);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
        assertNotNull("Null model focus context", focusContext);
        assertNull("Unexpected focus primary delta: " + focusContext.getPrimaryDelta(), focusContext.getPrimaryDelta());

        ObjectDelta<UserType> userSecondaryDelta = focusContext.getSecondaryDelta();
        assertSideEffectiveDeltasOnly("focus secondary delta", userSecondaryDelta);

        Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
        assertNotNull("Null model projection context list", projectionContexts);
        assertEquals("Unexpected number of projection contexts", 3, projectionContexts.size());

        ModelProjectionContext accContext = modelContext.findProjectionContext(
                new ResourceShadowDiscriminator(RESOURCE_DUMMY_BLUE_OID, ShadowKindType.ACCOUNT, null, null, false));
        assertNotNull("Null model projection context", accContext);

        assertEquals("Wrong policy decision", SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());
        ObjectDelta<ShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
        assertNotNull("No account primary delta", accountPrimaryDelta);
        PrismAsserts.assertModifications(accountPrimaryDelta, 1);
        PrismAsserts.assertPropertyAdd(accountPrimaryDelta,
                getAttributePath(getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME), DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME),
                "Elaine Threepwood");
        PrismAsserts.assertPropertyDelete(accountPrimaryDelta,
                getAttributePath(getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME), DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME),
                "Elaine Marley");

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNull("Unexpected account secondary delta: " + accountSecondaryDelta, accountSecondaryDelta);
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

        ObjectDelta<UserType> userDelta = createModifyUserReplaceDelta(USER_ELAINE_OID, UserType.F_FULL_NAME,
                PrismTestUtil.createPolyString("Elaine Threepwood"));
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
        display("Input deltas: ", deltas);

        // WHEN
        when();
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, new ModelExecuteOptions(), task, result);

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
        PrismAsserts.assertPropertyReplace(userPrimaryDelta, UserType.F_FULL_NAME, PrismTestUtil.createPolyString("Elaine Threepwood"));

        ObjectDelta<UserType> userSecondaryDelta = focusContext.getSecondaryDelta();
        assertSideEffectiveDeltasOnly("focus secondary delta", userSecondaryDelta);

        Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
        assertNotNull("Null model projection context list", projectionContexts);
        assertEquals("Unexpected number of projection contexts", 3, projectionContexts.size());

        // DEFAULT dummy resource: normal mappings
        ModelProjectionContext accContext = modelContext.findProjectionContext(
                new ResourceShadowDiscriminator(RESOURCE_DUMMY_OID, ShadowKindType.ACCOUNT, null, null, false));
        assertNotNull("Null model projection context (default)", accContext);

        assertEquals("Wrong policy decision (default)", SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());
        ObjectDelta<ShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
        assertNull("Unexpected account primary delta (default)", accountPrimaryDelta);

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNotNull("No account secondary delta (default)", accountSecondaryDelta);
        PrismAsserts.assertModifications(accountSecondaryDelta, 1);
        PrismAsserts.assertPropertyReplace(accountSecondaryDelta,
                getDummyResourceController().getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME),
                "Elaine Threepwood");

        // RED dummy resource: strong mappings
        accContext = modelContext.findProjectionContext(
                new ResourceShadowDiscriminator(RESOURCE_DUMMY_RED_OID, ShadowKindType.ACCOUNT, null, null, false));
        assertNotNull("Null model projection context (red)", accContext);

        assertEquals("Wrong policy decision", SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());
        accountPrimaryDelta = accContext.getPrimaryDelta();
        assertNull("Unexpected account primary delta (red)", accountPrimaryDelta);

        accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNotNull("No account secondary delta (red)", accountSecondaryDelta);
        PrismAsserts.assertModifications(accountSecondaryDelta, 1);
        PrismAsserts.assertPropertyReplace(accountSecondaryDelta,
                getAttributePath(getDummyResourceObject(RESOURCE_DUMMY_RED_NAME), DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME),
                "Elaine Threepwood");

        // BLUE dummy resource: weak mappings
        accContext = modelContext.findProjectionContext(
                new ResourceShadowDiscriminator(RESOURCE_DUMMY_BLUE_OID, ShadowKindType.ACCOUNT, null, null, false));
        assertNotNull("Null model projection context (blue)", accContext);

        assertEquals("Wrong policy decision", SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());
        accountPrimaryDelta = accContext.getPrimaryDelta();
        assertNull("Unexpected account primary delta (blue)", accountPrimaryDelta);

        accountSecondaryDelta = accContext.getSecondaryDelta();
        PrismAsserts.assertModifications(accountSecondaryDelta, 1);
        PrismAsserts.assertPropertyDelete(accountSecondaryDelta,
                getAttributePath(getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME), DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME),
                "null -- Elaine Marley");
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

        ObjectDelta<UserType> userDelta = createModifyUserReplaceDelta(USER_ELAINE_OID, UserType.F_FULL_NAME,
                PrismTestUtil.createPolyString("Elaine Threepwood"));
        ObjectDelta<ShadowType> accountDelta = createModifyAccountShadowReplaceAttributeDelta(
                ACCOUNT_SHADOW_ELAINE_DUMMY_OID, getDummyResourceObject(),
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Elaine LeChuck");
        // Cannot change the attribute on RED resource. It would conflict with the strong mapping and therefore fail.
//        ObjectDelta<ResourceObjectShadowType> accountDeltaRed = createModifyAccountShadowReplaceAttributeDelta(
//                ACCOUNT_SHADOW_ELAINE_DUMMY_RED_OID, resourceDummyRed,
//                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Elaine LeChuck");
        ObjectDelta<ShadowType> accountDeltaBlue = createModifyAccountShadowReplaceAttributeDelta(
                ACCOUNT_SHADOW_ELAINE_DUMMY_BLUE_OID, getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME),
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Elaine LeChuck");
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta, accountDelta,
                accountDeltaBlue);
        display("Input deltas: ", deltas);

        // WHEN
        when();
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, new ModelExecuteOptions(), task, result);

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
        PrismAsserts.assertPropertyReplace(userPrimaryDelta, UserType.F_FULL_NAME, PrismTestUtil.createPolyString("Elaine Threepwood"));

        ObjectDelta<UserType> userSecondaryDelta = focusContext.getSecondaryDelta();
        assertSideEffectiveDeltasOnly("focus secondary delta", userSecondaryDelta);

        Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
        assertNotNull("Null model projection context list", projectionContexts);
        assertEquals("Unexpected number of projection contexts", 3, projectionContexts.size());

        // DEFAULT dummy resource: normal mappings
        ModelProjectionContext accContext = modelContext.findProjectionContext(
                new ResourceShadowDiscriminator(RESOURCE_DUMMY_OID, ShadowKindType.ACCOUNT, null, null, false));
        assertNotNull("Null model projection context (default)", accContext);

        assertEquals("Wrong policy decision (default)", SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());
        ObjectDelta<ShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
        assertNotNull("No account primary delta (default)", accountPrimaryDelta);
        PrismAsserts.assertModifications(accountPrimaryDelta, 1);
        PrismAsserts.assertPropertyReplace(accountPrimaryDelta,
                getAttributePath(getDummyResourceObject(), DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME),
                "Elaine LeChuck");

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNull("Unexpected account secondary delta: " + accountSecondaryDelta, accountSecondaryDelta);

        // RED dummy resource: strong mappings
        accContext = modelContext.findProjectionContext(
                new ResourceShadowDiscriminator(RESOURCE_DUMMY_RED_OID, ShadowKindType.ACCOUNT, null, null, false));
        assertNotNull("Null model projection context (red)", accContext);

        assertEquals("Wrong policy decision", SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());
        accountPrimaryDelta = accContext.getPrimaryDelta();
        assertNull("Unexpected account primary delta (red)", accountPrimaryDelta);
        accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNotNull("No account secondary delta (red)", accountSecondaryDelta);
        PrismAsserts.assertModifications(accountSecondaryDelta, 1);
        PrismAsserts.assertPropertyReplace(accountSecondaryDelta,
                getAttributePath(getDummyResourceObject(RESOURCE_DUMMY_RED_NAME), DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME),
                "Elaine Threepwood");

        // BLUE dummy resource: weak mappings
        accContext = modelContext.findProjectionContext(
                new ResourceShadowDiscriminator(RESOURCE_DUMMY_BLUE_OID, ShadowKindType.ACCOUNT, null, null, false));
        assertNotNull("Null model projection context (blue)", accContext);

        assertEquals("Wrong policy decision", SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());
        accountPrimaryDelta = accContext.getPrimaryDelta();
        assertNotNull("No account primary delta (blue)", accountPrimaryDelta);
        PrismAsserts.assertModifications(accountPrimaryDelta, 1);
        PrismAsserts.assertPropertyReplace(accountPrimaryDelta,
                getAttributePath(getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME), DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME),
                "Elaine LeChuck");

        accountSecondaryDelta = accContext.getSecondaryDelta();
        PrismAsserts.assertModifications("account secondary delta (blue)", accountSecondaryDelta, 1);
        PrismAsserts.assertPropertyDelete(accountSecondaryDelta,
                getAttributePath(getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME), DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME),
                "null -- Elaine Marley");
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
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, new ModelExecuteOptions(), task, result);

        // THEN
        displayDumpable("Preview context", modelContext);
        assertNotNull("Null model context", modelContext);

        assertSuccess(result);

        ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
        assertNotNull("Null model focus context", focusContext);
        ObjectDelta<UserType> userPrimaryDelta = focusContext.getPrimaryDelta();
        assertNotNull("No focus primary delta: " + userPrimaryDelta, userPrimaryDelta);
        PrismAsserts.assertIsAdd(userPrimaryDelta);

        ObjectDelta<UserType> userSecondaryDelta = focusContext.getSecondaryDelta();
        assertSideEffectiveDeltasOnly(userSecondaryDelta, "focus secondary delta", ActivationStatusType.ENABLED);

        Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
        assertNotNull("Null model projection context list", projectionContexts);
        assertEquals("Unexpected number of projection contexts", 3, projectionContexts.size());

        // DEFAULT dummy resource: normal mappings
        ModelProjectionContext accContext = modelContext.findProjectionContext(
                new ResourceShadowDiscriminator(RESOURCE_DUMMY_OID, ShadowKindType.ACCOUNT, null, null, false));
        assertNotNull("Null model projection context (default)", accContext);

        assertEquals("Wrong policy decision (default)", SynchronizationPolicyDecision.ADD, accContext.getSynchronizationPolicyDecision());
        ObjectDelta<ShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
        assertNotNull("No account primary delta (default)", accountPrimaryDelta);
        PrismAsserts.assertIsAdd(accountPrimaryDelta);

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNotNull("No account secondary delta (default)", accountSecondaryDelta);
        PrismAsserts.assertModifications(accountSecondaryDelta, 9);
        PrismAsserts.assertNoItemDelta(accountSecondaryDelta,
                getAttributePath(getDummyResourceObject(), DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME));

        // RED dummy resource: strong mappings
        accContext = modelContext.findProjectionContext(
                new ResourceShadowDiscriminator(RESOURCE_DUMMY_RED_OID, ShadowKindType.ACCOUNT, null, null, false));
        assertNotNull("Null model projection context (red)", accContext);

        assertEquals("Wrong policy decision", SynchronizationPolicyDecision.ADD, accContext.getSynchronizationPolicyDecision());
        accountPrimaryDelta = accContext.getPrimaryDelta();
        assertNotNull("No account primary delta (default)", accountPrimaryDelta);
        PrismAsserts.assertIsAdd(accountPrimaryDelta);

        accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNotNull("No account secondary delta (red)", accountSecondaryDelta);
        PrismAsserts.assertModifications(accountSecondaryDelta, 9);
        PrismAsserts.assertPropertyReplace(accountSecondaryDelta,
                getAttributePath(getDummyResourceObject(RESOURCE_DUMMY_RED_NAME), DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME),
                "Kate Capsize");

        // BLUE dummy resource: weak mappings
        accContext = modelContext.findProjectionContext(
                new ResourceShadowDiscriminator(RESOURCE_DUMMY_BLUE_OID, ShadowKindType.ACCOUNT, null, null, false));
        assertNotNull("Null model projection context (blue)", accContext);

        assertEquals("Wrong policy decision", SynchronizationPolicyDecision.ADD, accContext.getSynchronizationPolicyDecision());
        accountPrimaryDelta = accContext.getPrimaryDelta();
        assertNotNull("No account primary delta (default)", accountPrimaryDelta);
        PrismAsserts.assertIsAdd(accountPrimaryDelta);

        accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNotNull("No account secondary delta (default)", accountSecondaryDelta);
        PrismAsserts.assertModifications(accountSecondaryDelta, 10);
        PrismAsserts.assertNoItemDelta(accountSecondaryDelta,
                getAttributePath(getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME), DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME));
        assertPasswordDelta(accountSecondaryDelta);
    }

    // testing multiple resources with dependencies (dummy -> dummy lemon)
    @Test
    public void test630AddUserRogers() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        PrismObject<UserType> user = PrismTestUtil.parseObject(USER_ROGERS_FILE);
        addAccountLinkRef(user, ACCOUNT_ROGERS_DUMMY_DEFAULT_FILE);
        addAccountLinkRef(user, ACCOUNT_ROGERS_DUMMY_LEMON_FILE);
        ObjectDelta<UserType> userDelta = DeltaFactory.Object.createAddDelta(user);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        // WHEN
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, new ModelExecuteOptions(), task, result);

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

        ObjectDelta<UserType> userSecondaryDelta = focusContext.getSecondaryDelta();
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
        ModelProjectionContext accContext = modelContext.findProjectionContext(
                new ResourceShadowDiscriminator(RESOURCE_DUMMY_OID, ShadowKindType.ACCOUNT, null, null, false));
        assertNotNull("Null model projection context (default)", accContext);

        assertEquals("Wrong policy decision (default)", SynchronizationPolicyDecision.ADD, accContext.getSynchronizationPolicyDecision());
        ObjectDelta<ShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
        assertNotNull("No account primary delta (default)", accountPrimaryDelta);
        PrismAsserts.assertIsAdd(accountPrimaryDelta);

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNotNull("No account secondary delta (default)", accountSecondaryDelta);
        // administrativeStatus (ENABLED), enableTimestamp, name, drink, quote, iteration, iterationToken, password/value
        PrismAsserts.assertModifications(accountSecondaryDelta, 9);
        PrismAsserts.assertNoItemDelta(accountSecondaryDelta,
                getAttributePath(getDummyResourceObject(), DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME));

        // LEMON dummy resource
        accContext = modelContext.findProjectionContext(
                new ResourceShadowDiscriminator(RESOURCE_DUMMY_LEMON_OID, ShadowKindType.ACCOUNT, null, null, false));
        assertNotNull("Null model projection context (lemon)", accContext);

        assertEquals("Wrong policy decision", SynchronizationPolicyDecision.ADD, accContext.getSynchronizationPolicyDecision());
        accountPrimaryDelta = accContext.getPrimaryDelta();
        assertNotNull("No account primary delta (default)", accountPrimaryDelta);
        PrismAsserts.assertIsAdd(accountPrimaryDelta);

        accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNotNull("No account secondary delta (lemon)", accountSecondaryDelta);
        // administrativeStatus (ENABLED), enableTimestamp, ship (from organizationalUnit), name, gossip, water, iteration, iterationToken, password/value
        PrismAsserts.assertModifications(accountSecondaryDelta, 10);
        PrismAsserts.assertPropertyReplace(accountSecondaryDelta,
                getAttributePath(getDummyResourceObject(RESOURCE_DUMMY_LEMON_NAME), DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
                "The crew of The Sea Monkey");
        PrismAsserts.assertPropertyReplace(accountSecondaryDelta,
                ItemPath.create(ShadowType.F_ATTRIBUTES, SchemaConstants.ICFS_NAME),
                "rogers");
        PrismAsserts.assertPropertyAdd(accountSecondaryDelta,
                getAttributePath(getDummyResourceObject(RESOURCE_DUMMY_LEMON_NAME), DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME),
                "Rum Rogers Sr. must be the best pirate the  has ever seen");
        PrismAsserts.assertPropertyReplace(accountSecondaryDelta,
                getAttributePath(getDummyResourceObject(RESOURCE_DUMMY_LEMON_NAME), DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WATER_NAME),
                "pirate Rum Rogers Sr. drinks only rum!");
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
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, new ModelExecuteOptions(), task, result);

        // THEN
        displayDumpable("Preview context", modelContext);
        assertNotNull("Null model context", modelContext);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
        assertNotNull("Null model focus context", focusContext);
        assertNull("Unexpected focus primary delta: " + focusContext.getPrimaryDelta(), focusContext.getPrimaryDelta());

        ObjectDelta<UserType> userSecondaryDelta = focusContext.getSecondaryDelta();
        assertSideEffectiveDeltasOnly("focus secondary delta", userSecondaryDelta);

        Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
        assertNotNull("Null model projection context list", projectionContexts);
        assertEquals("Unexpected number of projection contexts", 3, projectionContexts.size());

        ModelProjectionContext accContextDefault = modelContext.findProjectionContext(
                new ResourceShadowDiscriminator(RESOURCE_DUMMY_OID, ShadowKindType.ACCOUNT, null, null, false));
        assertNotNull("Null model projection context (default)", accContextDefault);

        assertEquals("Wrong policy decision (default)", SynchronizationPolicyDecision.KEEP, accContextDefault.getSynchronizationPolicyDecision());
        ObjectDelta<ShadowType> accountPrimaryDelta = accContextDefault.getPrimaryDelta();
        assertNotNull("No account primary delta (default)", accountPrimaryDelta);
        PrismAsserts.assertModifications(accountPrimaryDelta, 1);
        PrismAsserts.assertPropertyReplace(accountPrimaryDelta, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, ActivationStatusType.DISABLED);

        ObjectDelta<ShadowType> accountSecondaryDelta = accContextDefault.getSecondaryDelta();
        PrismAsserts.assertModifications(accountSecondaryDelta, 2);
        assertNotNull("No disableTimestamp delta in account secodary delta (default)",
                accountSecondaryDelta.findPropertyDelta(PATH_ACTIVATION_DISABLE_TIMESTAMP));
        PrismAsserts.assertPropertyReplace(accountSecondaryDelta, SchemaConstants.PATH_ACTIVATION_DISABLE_REASON,
                SchemaConstants.MODEL_DISABLE_REASON_EXPLICIT);
        // the other modification is disable timestamp

        ModelProjectionContext accContextBlue = modelContext.findProjectionContext(
                new ResourceShadowDiscriminator(RESOURCE_DUMMY_BLUE_OID, ShadowKindType.ACCOUNT, null, null, false));
        assertNotNull("Null model projection context (blue)", accContextBlue);

        assertEquals("Wrong policy decision (blue)", SynchronizationPolicyDecision.KEEP, accContextBlue.getSynchronizationPolicyDecision());
        ObjectDelta<ShadowType> accountPrimaryDeltaBlue = accContextBlue.getPrimaryDelta();
        assertNotNull("No account primary delta (blue)", accountPrimaryDeltaBlue);
        PrismAsserts.assertModifications(accountPrimaryDeltaBlue, 1);
        PrismAsserts.assertPropertyReplace(accountPrimaryDeltaBlue, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, ActivationStatusType.DISABLED);

        ObjectDelta<ShadowType> accountSecondaryDeltaBlue = accContextBlue.getSecondaryDelta();
        PrismAsserts.assertModifications(accountSecondaryDeltaBlue, 2);
        assertNotNull("No disableTimestamp delta in account secondary delta (blue)",
                accountSecondaryDeltaBlue.findPropertyDelta(PATH_ACTIVATION_DISABLE_TIMESTAMP));
        PrismAsserts.assertPropertyReplace(accountSecondaryDeltaBlue, SchemaConstants.PATH_ACTIVATION_DISABLE_REASON,
                SchemaConstants.MODEL_DISABLE_REASON_EXPLICIT);
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

        ObjectDelta<UserType> empty = prismContext.deltaFor(UserType.class).asObjectDeltaCast(USER_GUYBRUSH_OID);

        // WHEN
        when();
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(singleton(empty),
                createEvaluateAllAssignmentRelationsOnRecompute(), task, result);

        // THEN
        then();
        displayDumpable("Preview context", modelContext);
        assertNotNull("Null model context", modelContext);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
        assertNotNull("Model focus context missing", focusContext);

        DeltaSetTriple<? extends EvaluatedAssignment<?>> evaluatedAssignmentTriple = modelContext.getEvaluatedAssignmentTriple();
        displayDumpable("evaluatedAssignmentTriple", evaluatedAssignmentTriple);

        assertEquals("Wrong # of evaluated assignments in zero set", 1, evaluatedAssignmentTriple.getZeroSet().size());
        EvaluatedAssignment<?> evaluatedAssignment = evaluatedAssignmentTriple.getZeroSet().iterator().next();
        assertNotNull("Target of evaluated assignment is null", evaluatedAssignment.getTarget());
        assertEquals("Wrong # of zero-set roles in evaluated assignment", 1, evaluatedAssignment.getRoles().getZeroSet().size());
    }

}
