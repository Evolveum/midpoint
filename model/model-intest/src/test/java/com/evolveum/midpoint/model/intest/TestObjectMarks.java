package com.evolveum.midpoint.model.intest;

import static com.evolveum.midpoint.model.test.CommonInitialObjects.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;
import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_DIR;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType.ACCOUNT;

import java.io.File;
import java.util.List;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.util.exception.CommonException;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Test both shadow marks and marks on focus objects.
 * (Note that object marks that are strictly related to policy rules should be covered by test classes dealing with policy rules.)
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestObjectMarks extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File(TEST_RESOURCES_DIR, "object-marks");

    private static final String ATTR_GIVEN_NAME = "givenName";
    private static final String ATTR_FAMILY_NAME = "familyName";
    private static final String ATTR_PERSONAL_NUMBER = "personalNumber";
    private static final String ATTR_TYPE = "type";

    private static final DummyTestResource RESOURCE_SHADOW_MARKS = new DummyTestResource(
            TEST_DIR, "resource-shadow-marks.xml", "cae6ef05-210c-4102-97f7-647936b47a12", "shadow-marks",
            controller -> {
                controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                        ATTR_GIVEN_NAME, String.class, false, false);
                controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                        ATTR_FAMILY_NAME, String.class, false, false);
                controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                        ATTR_PERSONAL_NUMBER, String.class, false, false);
                controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                        ATTR_TYPE, String.class, false, false);
            });

    private static final TestObject<MarkType> MARK_HAS_UNMANAGED_PROJECTION = TestObject.file(
            TEST_DIR, "mark-has-unmanaged-projection.xml", "367a4c90-9618-449c-bfd2-6af078d2c5dd");

    private static final String TYPE_TESTER = "tester";
    private static final String INTENT_TESTER = "tester";
    private static final String TYPE_DEVELOPER = "developer";
    private static final String INTENT_DEVELOPER = "developer";

    private static final String PRIVILEGED_ACCESS_MARK_NAME = "Privileged access";
    private static final String PRIVILEGED_ACCESS_POLICY_NAME = "Privileged access policy";

    private String markNoSyncOid;

    private String markReadOnlyOid;

    private Object markPolicyNoOutbound;

    @Override
    protected boolean requiresNativeRepository() {
        return true;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        CommonInitialObjects.addMarks(this, initTask, initResult);

        initAndTestDummyResource(RESOURCE_SHADOW_MARKS, initTask, initResult);
        initTestObjects(initTask, initResult,
                MARK_HAS_UNMANAGED_PROJECTION);

        markNoSyncOid = addObject(
                new MarkType()
                        .name("Skip Synchronization")
                        .assignment(
                                new AssignmentType()
                                        .targetRef(SystemObjectsType.ARCHETYPE_OBJECT_MARK.value(), MarkType.COMPLEX_TYPE))
                        .objectOperationPolicy(new ObjectOperationPolicyType()
                                .synchronize(new SynchronizeOperationPolicyConfigurationType()
                                        .inbound(new SyncInboundOperationPolicyConfigurationType()
                                                .enabled(SyncInboundOperationPolicyEnabledType.FALSE)))),
                initTask, initResult);

        // Read-only is base for do-not touch
        markReadOnlyOid = addObject(
                new MarkType()
                        .name("Read Only")
                        .assignment(
                                new AssignmentType()
                                        .targetRef(SystemObjectsType.ARCHETYPE_OBJECT_MARK.value(), MarkType.COMPLEX_TYPE))
                        .objectOperationPolicy(new ObjectOperationPolicyType()
                                .add(new OperationPolicyConfigurationType().enabled(false))
                                .modify(new OperationPolicyConfigurationType().enabled(false))
                                .delete(new OperationPolicyConfigurationType().enabled(false))),
                initTask, initResult);

        markPolicyNoOutbound = addObject(
                new MarkType()
                        .name("No Outbound when name administrator")
                        .assignment(
                                new AssignmentType()
                                        .targetRef(SystemObjectsType.ARCHETYPE_OBJECT_MARK.value(), MarkType.COMPLEX_TYPE))
                        .policyRule(new GlobalPolicyRuleType()
                                .focusSelector(new ObjectSelectorType().type(ShadowType.COMPLEX_TYPE))
                                .policyConstraints(new PolicyConstraintsType())
                                .evaluationTarget(PolicyRuleEvaluationTargetType.PROJECTION)
                        )
                        .objectOperationPolicy(new ObjectOperationPolicyType()
                                .synchronize(new SynchronizeOperationPolicyConfigurationType()
                                        .outbound(new OperationPolicyConfigurationType().enabled(false)))),
                initTask, initResult);
    }

    @Test
    public void test100ImportUserAndMarkNoSync() throws Exception {
        var result = createOperationResult();
        var task = createTask();

        DummyAccount account1 = RESOURCE_SHADOW_MARKS.controller.addAccount("brown");
        account1.addAttributeValue(ATTR_GIVEN_NAME, "Karl");
        account1.addAttributeValue(ATTR_FAMILY_NAME, "Brown");
        account1.addAttributeValue(ATTR_PERSONAL_NUMBER, "1004444");

        importAccountsRequest()
                .withResourceOid(RESOURCE_SHADOW_MARKS.oid)
                .withNameValue("brown")
                .execute(result);

        // Find user brown
        PrismObject<UserType> userOrig = searchObjectByName(UserType.class, "brown");
        ObjectReferenceType shadow1Ref = userOrig.asObjectable().getLinkRef().get(0);

        account1.replaceAttributeValue(ATTR_FAMILY_NAME, "Brownie");

        importAccountsRequest()
                .withResourceOid(RESOURCE_SHADOW_MARKS.oid)
                .withNameValue("brown")
                .execute(result);

        PrismObject<UserType> userAfter = searchObjectByName(UserType.class, "brown");
        assertEquals(userAfter.asObjectable().getFamilyName().getOrig(), "Brownie");

        // Mark shadow do not synchronize
        markShadow(shadow1Ref.getOid(), markNoSyncOid, task, result);

        account1.replaceAttributeValue(ATTR_PERSONAL_NUMBER, "555555");

        importAccountsRequest()
                .withResourceOid(RESOURCE_SHADOW_MARKS.oid)
                .withNameValue("brown")
                .execute(result);

        userAfter = searchObjectByName(UserType.class, "brown");
        assertEquals(userOrig.asObjectable().getPersonalNumber(), userAfter.asObjectable().getPersonalNumber());

        recomputeUser(userAfter.getOid());
        userAfter = searchObjectByName(UserType.class, "brown");
        assertEquals(userOrig.asObjectable().getPersonalNumber(), userAfter.asObjectable().getPersonalNumber());

        reconcileUser(userAfter.getOid(), getTestTask(), result);
        userAfter = searchObjectByName(UserType.class, "brown");
        assertEquals(userOrig.asObjectable().getPersonalNumber(), userAfter.asObjectable().getPersonalNumber());

    }

    @Test
    public void test200ImportUserAndMarkReadOnly() throws Exception {
        var result = createOperationResult();
        var task = createTask();

        DummyAccount account1 = RESOURCE_SHADOW_MARKS.controller.addAccount("reddy");
        account1.addAttributeValue(ATTR_GIVEN_NAME, "Karl");
        account1.addAttributeValue(ATTR_FAMILY_NAME, "Reddy");
        account1.addAttributeValue(ATTR_PERSONAL_NUMBER, "2004444");

        importAccountsRequest()
                .withResourceOid(RESOURCE_SHADOW_MARKS.oid)
                .withNameValue("reddy")
                .execute(result);

        assertNotNull(result);

        // Find user reddy
        PrismObject<UserType> userOrig = searchObjectByName(UserType.class, "reddy");
        ObjectReferenceType shadow1Ref = userOrig.asObjectable().getLinkRef().get(0);

        var renamed = PolyString.fromOrig("Browny");
        var modifyResult = createOperationResult();
        modifyObjectReplaceProperty(UserType.class, userOrig.getOid(), UserType.F_GIVEN_NAME, task, modifyResult, renamed);
        assertSuccess(modifyResult);

        assertEquals(account1.getAttributeValue(ATTR_GIVEN_NAME), "Browny");

        // when(description);
        // Mark shadow do read-only
        markShadow(shadow1Ref.getOid(), markReadOnlyOid, task, result);

        renamed = new PolyString("Karly");
        modifyObjectReplaceProperty(UserType.class, userOrig.getOid(), UserType.F_GIVEN_NAME, task, modifyResult, renamed);
        assertEquals(account1.getAttributeValue(ATTR_GIVEN_NAME), "Browny");

        // Changes from resource should be imported (inbound enabled)
        account1.replaceAttributeValue(ATTR_GIVEN_NAME, "Renamed");
        importAccountsRequest()
                .withResourceOid(RESOURCE_SHADOW_MARKS.oid)
                .withNameValue("reddy")
                .withTracing()
                .execute(result);

        PrismObject<UserType> userAfterImport = searchObjectByName(UserType.class, "reddy");
        assertEquals(userAfterImport.asObjectable().getGivenName().getOrig(), "Renamed");

        // We should be able to remove shadow mark

    }

    @Test
    public void test300ImportUserWithBrokenMapping() throws Exception {
        var result = createOperationResult();
        DummyAccount account1 = RESOURCE_SHADOW_MARKS.controller.addAccount("broken");
        account1.addAttributeValue(ATTR_GIVEN_NAME, "Karl");
        account1.addAttributeValue(ATTR_FAMILY_NAME, "Reddy");
        account1.addAttributeValue(ATTR_PERSONAL_NUMBER, "Broken");

        importAccountsRequest()
                .withResourceOid(RESOURCE_SHADOW_MARKS.oid)
                .withNameValue("broken")
                .withAssertSuccess(false)
                .execute(result);

        assertNotNull(result);
    }

    // Add users / synchronize users
    // Mark shadow "Correlate later" and verify it is not synced

    // Mark Existing user shadow "Do not touch" and verify it is not synced / modified

    // Mark existing user shadow "Invalid data and verify it is not synced / modified"

    /**
     * The `account/tester` object type uses the following approach:
     *
     * . Each new account is marked as `Unmanaged`, that means that only inbound mappings are executed.
     * . After removing that mark, the default of `Managed` applies, which means that only outbounds are executed.
     *
     * In this method, a new `tester` account appears. It should be first `Unmanaged`, then manually switched to `Managed`.
     * We check that the mappings work as expected.
     */
    @Test
    public void test400TestShadowUnmanagedToManaged() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var userName = getTestNameShort();

        given("a new tester account is created on the resource");
        var account = RESOURCE_SHADOW_MARKS.controller.addAccount(userName)
                .addAttributeValue(ATTR_GIVEN_NAME, "John")
                .addAttributeValue(ATTR_FAMILY_NAME, "Tester")
                .addAttributeValue(ATTR_TYPE, TYPE_TESTER);

        when("the account is imported");
        importAccountsRequest()
                .withResourceOid(RESOURCE_SHADOW_MARKS.oid)
                .withNameValue(userName)
                .withWholeObjectClass(RI_ACCOUNT_OBJECT_CLASS)
                .executeOnForeground(result);

        then("the user is there, account is Unmanaged");
        var userAsserter = assertUserByUsername(userName, "after initial import")
                .display()
                .displayXml()
                .withObjectResolver(createSimpleModelObjectResolver())
                .assertGivenName("John")
                .assertFamilyName("Tester")
                .assertEffectiveMarks(MARK_HAS_UNMANAGED_PROJECTION.oid);
        var shadowAsserter = userAsserter
                .singleLink()
                .resolveTarget()
                .display()
                .displayXml()
                .asShadow()
                .assertIntent(INTENT_TESTER)
                .assertEffectiveMarks(MARK_UNMANAGED.oid);
        var userOid = userAsserter.getOid();
        var shadowOid = shadowAsserter.getOid();

        when("there is a change on the resource and the user is reconciled");
        account.replaceAttributeValue(ATTR_FAMILY_NAME, "Big Tester");
        invalidateShadowCacheIfNeeded(RESOURCE_SHADOW_MARKS.oid);
        reconcileUser(userOid, task, result);

        then("the user is changed");
        assertUser(userOid, "after change on resource while unmanaged")
                .assertFamilyName("Big Tester");

        when("there is a change in midPoint");
        modifyObjectReplaceProperty(UserType.class, userOid, UserType.F_FAMILY_NAME, task, result,
                PolyString.fromOrig("Lesser Tester"));

        then("it is not propagated onto resource");
        assertDummyAccountByUsername(RESOURCE_SHADOW_MARKS.name, userName)
                .assertAttribute(ATTR_FAMILY_NAME, "Big Tester");

        when("the user is reconciled");
        reconcileUser(userOid, task, result);

        then("the original value is on the resource and in the repository");
        assertDummyAccountByUsername(RESOURCE_SHADOW_MARKS.name, userName)
                .assertAttribute(ATTR_FAMILY_NAME, "Big Tester");
        assertUser(userOid, "after reconciliation")
                .assertFamilyName("Big Tester");

        when("the shadow is unmarked");
        markShadowExcluded(shadowOid, MARK_UNMANAGED.oid, task, result);
        assertShadow(getShadowModel(shadowOid), "after unmarked")
                .assertEffectiveMarks();

        and("there is a change in midPoint");
        modifyObjectReplaceProperty(UserType.class, userOid, UserType.F_FAMILY_NAME, task, result,
                PolyString.fromOrig("Even Bigger Tester"));

        then("it is propagated onto resource");
        assertDummyAccountByUsername(RESOURCE_SHADOW_MARKS.name, userName)
                .assertAttribute(ATTR_FAMILY_NAME, "Even Bigger Tester");

        when("there is a change on resource and the user is reconciled");
        account.replaceAttributeValue(ATTR_FAMILY_NAME, "Mega Tester");
        invalidateShadowCacheIfNeeded(RESOURCE_SHADOW_MARKS.oid);
        reconcileUser(userOid, task, result);

        then("the user in midPoint is not changed; and it is no longer marked as having unmanaged projections");
        assertUser(userOid, "after change on resource while managed + reconciled")
                .display()
                .assertFamilyName("Even Bigger Tester")
                .assertEffectiveMarks();
        assertDummyAccountByUsername(RESOURCE_SHADOW_MARKS.name, userName)
                .assertAttribute(ATTR_FAMILY_NAME, "Even Bigger Tester");

        when("there is a change on resource, and the user is imported");
        account.replaceAttributeValue(ATTR_FAMILY_NAME, "Mega Tester - Again");
        importAccountsRequest()
                .withResourceOid(RESOURCE_SHADOW_MARKS.oid)
                .withNameValue(userName)
                .withWholeObjectClass(RI_ACCOUNT_OBJECT_CLASS)
                .executeOnForeground(result);

        then("the user in midPoint is not changed, and the change on the resource is cancelled");
        assertUser(userOid, "after change on resource while managed + imported")
                .display()
                .assertFamilyName("Even Bigger Tester")
                .assertEffectiveMarks();
        assertDummyAccountByUsername(RESOURCE_SHADOW_MARKS.name, userName)
                .assertAttribute(ATTR_FAMILY_NAME, "Even Bigger Tester");
    }

    /**
     * A user (`account/tester`) is `unmanaged`, but has an assignment that provides him with a shadow on the resource.
     * When the account is deleted, midPoint should still apply the operation policy of `unmanaged` regarding this resource.
     * Otherwise, the account would get immediately re-created.
     */
    @Test
    public void test410TestUnmanagedShadowDeletion() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var userName = getTestNameShort();

        given("a new tester account is created on the resource");
        RESOURCE_SHADOW_MARKS.controller.addAccount(userName)
                .addAttributeValue(ATTR_GIVEN_NAME, "Phoenix")
                .addAttributeValue(ATTR_FAMILY_NAME, "Tester")
                .addAttributeValue(ATTR_TYPE, TYPE_TESTER);

        when("the account is imported");
        importAccountsRequest()
                .withResourceOid(RESOURCE_SHADOW_MARKS.oid)
                .withNameValue(userName)
                .withWholeObjectClass(RI_ACCOUNT_OBJECT_CLASS)
                .executeOnForeground(result);

        then("the user is there, account is Unmanaged");
        var userAsserter = assertUserByUsername(userName, "after initial import")
                .display()
                .withObjectResolver(createSimpleModelObjectResolver())
                .assertGivenName("Phoenix")
                .assertFamilyName("Tester")
                .assertEffectiveMarks(MARK_HAS_UNMANAGED_PROJECTION.oid);
        var shadowAsserter = userAsserter
                .singleLink()
                .resolveTarget()
                .display()
                .assertIntent(INTENT_TESTER)
                .assertEffectiveMarks(MARK_UNMANAGED.oid);
        var userOid = userAsserter.getOid();
        var shadowOid = shadowAsserter.getOid();

        when("an assignment of 'account/tester' is created");
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT)
                        .add(RESOURCE_SHADOW_MARKS.assignmentWithConstructionOf(ACCOUNT, INTENT_TESTER))
                        .asObjectDelta(userOid),
                null, task, result);

        and("the account is deleted and discovered as dead");
        RESOURCE_SHADOW_MARKS.controller.deleteAccount(userName);
        try {
            var shadow = provisioningService.getShadow(shadowOid, null, task, result);
            assertShadow(shadow.getBean(), "after deletion")
                    .assertIsDead(true);
        } catch (ObjectNotFoundException e) {
            displayExpectedException(e);
        }

        then("the account should be gone");
        assertSuccess(result);
        assertNoDummyAccount(RESOURCE_SHADOW_MARKS.name, userName);
        assertUserAfter(userOid)
                .assertLinks(0, 1);
    }

    /**
     * Default operation policy for `account/developer` is `unmanaged`.
     * When we try to create such an account (by assigning the construction), the creation should not be attempted.
     *
     * Executed via "real" user addition operation.
     *
     * MID-9979
     */
    @Test
    public void test420AssignUnmanagedAccount() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var userName = getTestNameShort();

        when("a user with account/developer is created (default policy is 'unmanaged')");
        var userOid = traced(() -> addObject(
                new UserType()
                        .name(userName)
                        .assignment(RESOURCE_SHADOW_MARKS.assignmentWithConstructionOf(ACCOUNT, INTENT_DEVELOPER)),
                task, result));

        then("everything is OK, no account is created");
        assertSuccess(result);
        assertUserAfter(userOid)
                .assertLinks(0, 0);
    }

    /**
     * As {@link #test420AssignUnmanagedAccount()} but using preview changes instead.
     *
     * MID-9979
     */
    @Test
    public void test425AssignUnmanagedAccountPreviewChanges() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var userName = getTestNameShort();

        given("a user");
        var userOid = addObject(
                new UserType()
                        .name(userName),
                task, result);

        when("a preview of assigning account/developer is executed (default policy is 'unmanaged')");
        var lensContext = previewChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT)
                        .add(RESOURCE_SHADOW_MARKS.assignmentWithConstructionOf(ACCOUNT, INTENT_DEVELOPER))
                        .asObjectDelta(userOid),
                null, task, result);

        then("everything is OK, and there are no projection contexts");
        assertSuccess(result);
        displayDumpable("lens context", lensContext);
        assertThat(lensContext.getProjectionContexts())
                .as("projection contexts")
                .isEmpty();
    }

    /**
     * Tests lifecycle-aware default operation policy for `account/developer` (MID-9972):
     * for production, it is `unmanaged`, while we are experimenting with `managed` for the development mode.
     */
    @Test
    public void test500TestLifecycleAwareDefaultOperationPolicy() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var userName = getTestNameShort();

        given("a new developer account is created on the resource");
        RESOURCE_SHADOW_MARKS.controller.addAccount(userName)
                .addAttributeValue(ATTR_GIVEN_NAME, "John")
                .addAttributeValue(ATTR_FAMILY_NAME, "Developer")
                .addAttributeValue(ATTR_TYPE, TYPE_DEVELOPER);

        when("the account is imported");
        importAccountsRequest()
                .withResourceOid(RESOURCE_SHADOW_MARKS.oid)
                .withNameValue(userName)
                .withWholeObjectClass(RI_ACCOUNT_OBJECT_CLASS)
                .executeOnForeground(result);

        then("the user is there");
        var userAsserter = assertUserByUsername(userName, "after initial import")
                .display()
                .withObjectResolver(createSimpleModelObjectResolver())
                .assertGivenName("John")
                .assertFamilyName("Developer");
        var shadowAsserter = userAsserter
                .singleLink()
                .resolveTarget()
                .display()
                .assertIntent(INTENT_DEVELOPER);
        var userOid = userAsserter.getOid();
        var shadowOid = shadowAsserter.getOid();

        then("the shadow has 'unmanaged' policy (as a default)");
        var shadow = provisioningService.getShadow(shadowOid, null, task, result);
        assertThat(shadow.getEffectiveOperationPolicyRequired().getSynchronize().getOutbound().isEnabled())
                .as("outbound sync policy for %s", shadow)
                .isEqualTo(false);

        when("a user change is simulated in the development mode");
        ObjectDelta<UserType> givenNameDelta = deltaFor(UserType.class)
                .item(UserType.F_GIVEN_NAME).replace(PolyString.fromOrig("Johnny"))
                .asObjectDelta(userOid);
        givenNameDelta.freeze();

        var simResult1 =
                executeDeltasInDevelopmentSimulationMode(
                        List.of(givenNameDelta),
                        defaultSimulationDefinition(),
                        task, result);

        then("there should be a givenName change in the account");
        assertProcessedObjects(simResult1, "in development mode")
                .by().objectType(ShadowType.class).find(po ->
                        po.delta(d ->
                                d.assertModification(
                                        ShadowType.F_ATTRIBUTES.append(ATTR_GIVEN_NAME),
                                        "Johnny")));

        when("a user change is simulated in the production mode");
        var simResult2 =
                executeInProductionSimulationMode(
                        List.of(givenNameDelta),
                        defaultSimulationDefinition(),
                        task, result);

        then("the account should not be going to be changed");
        assertProcessedObjects(simResult2, "in production mode")
                .by().objectType(ShadowType.class).find(po ->
                        po.assertState(ObjectProcessingStateType.UNMODIFIED));
    }

    /**
     * Tests lifecycle-aware policy statements (loosely related to MID-9972):
     * an account is protected, but we simulate the removal of this mark.
     */
    @Test
    public void test510TestDevelopmentModePolicyStatement() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var userName = "_" + getTestNameShort();

        given("a new protected developer account is created on the resource");
        RESOURCE_SHADOW_MARKS.controller.addAccount(userName)
                .addAttributeValue(ATTR_GIVEN_NAME, "Protected")
                .addAttributeValue(ATTR_FAMILY_NAME, "Developer")
                .addAttributeValue(ATTR_TYPE, TYPE_DEVELOPER);

        when("the account is tried to be imported");
        importAccountsRequest()
                .withResourceOid(RESOURCE_SHADOW_MARKS.oid)
                .withNameValue(userName)
                .withWholeObjectClass(RI_ACCOUNT_OBJECT_CLASS)
                .executeOnForeground(result);

        then("the user is NOT there, but the shadow is");
        assertNoUserByUsername(userName);
        var shadowAfterImport = findShadowRequest()
                .withResource(RESOURCE_SHADOW_MARKS.getObjectable())
                .withTypeIdentification(ResourceObjectTypeIdentification.of(ACCOUNT, INTENT_DEVELOPER))
                .withNameValue(userName)
                .findRequired(task, result);
        assertShadow(shadowAfterImport.getPrismObject(), "after import")
                .display()
                .assertProtected()
                .getObjectable();

        when("the shadow is marked as unmanaged, not protected, but only in the development mode");
        markShadow(
                shadowAfterImport.getOid(),
                PolicyStatementTypeType.APPLY,
                MARK_UNMANAGED.oid,
                SchemaConstants.LIFECYCLE_PROPOSED,
                task,
                result);
        markShadow(
                shadowAfterImport.getOid(),
                PolicyStatementTypeType.EXCLUDE,
                MARK_PROTECTED.oid,
                SchemaConstants.LIFECYCLE_PROPOSED,
                task,
                result);

        then("the shadow is still protected (in production mode - persistent effects)");
        importAccountsRequest()
                .withResourceOid(RESOURCE_SHADOW_MARKS.oid)
                .withNameValue(userName)
                .withWholeObjectClass(RI_ACCOUNT_OBJECT_CLASS)
                .executeOnForeground(result);
        assertNoUserByUsername(userName);

        and("the shadow is still protected (in production mode - simulated effects)");
        var simResult1 = importAccountsRequest()
                .withResourceOid(RESOURCE_SHADOW_MARKS.oid)
                .withNameValue(userName)
                .withWholeObjectClass(RI_ACCOUNT_OBJECT_CLASS)
                .simulatedProduction()
                .executeOnForegroundSimulated(null, task, result);
        assertProcessedObjects(simResult1, "in production mode")
                .assertSize(0); // ignored because it's protected

        and("the shadow is no longer protected (in development mode - simulated effects)");
        var simResult2 = importAccountsRequest()
                .withResourceOid(RESOURCE_SHADOW_MARKS.oid)
                .withNameValue(userName)
                .withWholeObjectClass(RI_ACCOUNT_OBJECT_CLASS)
                .simulatedDevelopment()
                .executeOnForegroundSimulated(null, task, result);
        assertProcessedObjects(simResult2, "in development mode")
                .by().objectType(UserType.class).find(po -> po.assertState(ObjectProcessingStateType.ADDED))
                .by().objectType(ShadowType.class).find(po -> po.assertState(ObjectProcessingStateType.UNMODIFIED));
    }

    /**
     * Recomputation of a user that has some marks should produce no deltas.
     *
     * MID-10121
     */
    @Test
    public void test600ObjectMarkOnRecompute() throws CommonException {
        var task = getTestTask();
        var result = task.getResult();
        var userName = getTestNameShort();

        given("a user with a mark");
        var user = new UserType()
                .name(userName)
                .policyStatement(new PolicyStatementType()
                        .type(PolicyStatementTypeType.APPLY)
                        .markRef(MARK_DO_NOT_TOUCH.oid, MarkType.COMPLEX_TYPE));
        var oid = addObject(user, task, result);

        assertUser(oid, "before")
                .assertEffectiveMarks(MARK_DO_NOT_TOUCH.oid);

        when("a user is recomputed (preview)");
        var reconcileOption = ModelExecuteOptions.create().reconcile();
        var context = previewChanges(
                deltaFor(UserType.class)
                        .asObjectDelta(oid),
                reconcileOption, task, result);

        then("there are no deltas (in preview)");
        assertPreviewContext(context)
                .focusContext()
                .assertNoSecondaryDelta();

        and("the mark is there");
        assertUser(oid, "before")
                .assertEffectiveMarks(MARK_DO_NOT_TOUCH.oid);

        when("the user is recomputed (for real)");
        dummyAuditService.clear();
        recomputeUser(oid, reconcileOption, task, result);

        then("there are no records in audit");
        dummyAuditService.assertNoRecord();

        and("the mark is there");
        assertUser(oid, "before")
                .assertEffectiveMarks(MARK_DO_NOT_TOUCH.oid);
    }

    /**
     *
     * MID-10641
     */
    @Test(enabled = false)
    public void test700ObjectMarkOnUnassign() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var roleName = getTestNameShort();

        given("a privileged classification policy inducing a mark 'Privileged access'");
        var markOid = addObject(
                new MarkType()
                        .name(PRIVILEGED_ACCESS_MARK_NAME),
                task, result);

        var policyOid = addObject(
                new PolicyType()
                        .name(PRIVILEGED_ACCESS_POLICY_NAME)
                        .inducement(
                                new AssignmentType()
                                        .policyRule(
                                                new PolicyRuleType()
                                                        .policyConstraints(
                                                                new PolicyConstraintsType()
                                                                        .alwaysTrue(
                                                                                new AlwaysTruePolicyConstraintType()
                                                                                        .name("mark-focus-always-true")
                                                                        )
                                                        )
                                                        .markRef(markOid, MarkType.COMPLEX_TYPE)
                                                        .policyActions(
                                                                new PolicyActionsType()
                                                                        .record(new RecordPolicyActionType())
                                                        )
                                        )
                        ),
                task, result);


        when("the role with policy assignment is created and marked");
        var roleOid = addObject(
                new RoleType()
                        .name(roleName)
                        .assignment(
                                new AssignmentType()
                                        .targetRef(policyOid, PolicyType.COMPLEX_TYPE)),
                task, result);

        then("the role is marked");
        assertRole(roleOid, "before")
                .assertEffectiveMarks(markOid);

        when("unassign a privileged classification policy from the role");
        unassign(PolicyType.class, policyOid, roleOid, task, result);

        then("the role is not marked anymore");
        assertRole(roleOid, "after unmarked")
                .assertEffectiveMarks();
    }

}
