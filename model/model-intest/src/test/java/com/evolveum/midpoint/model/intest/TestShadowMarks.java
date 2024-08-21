package com.evolveum.midpoint.model.intest;

import static com.evolveum.midpoint.model.test.CommonInitialObjects.MARK_UNMANAGED;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_DIR;

import java.io.File;

import com.evolveum.midpoint.test.TestObject;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestShadowMarks extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File(TEST_RESOURCES_DIR, "shadow-marks");

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
                                        .inbound(new OperationPolicyConfigurationType().enabled(false)))),
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
    void test300importUserWithBrokenMapping() throws Exception {
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
    void test400TestShadowUnmanagedToManaged() throws Exception {
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
                .withObjectResolver(createSimpleModelObjectResolver())
                .assertGivenName("John")
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
        assertUser(userOid, "after change on resource while managed")
                .display()
                .assertFamilyName("Even Bigger Tester")
                .assertEffectiveMarks();
    }
}
