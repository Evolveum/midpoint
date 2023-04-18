package com.evolveum.midpoint.model.intest;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_DIR;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestShadowMarks extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File(TEST_RESOURCES_DIR, "identities");

    private static final String ATTR_GIVEN_NAME = "givenName";
    private static final String ATTR_FAMILY_NAME = "familyName";
    private static final String ATTR_PERSONAL_NUMBER = "personalNumber";

    private static final String NS_ENT = "http://midpoint.evolveum.com/xml/ns/samples/enterprise";
    private static final ItemName ENT_PERSONAL_NUMBER = new ItemName(NS_ENT, "personalNumber");
    private static final ItemPath PATH_PERSONAL_NUMBER = ItemPath.create(ObjectType.F_EXTENSION, ENT_PERSONAL_NUMBER);

    private static final DummyTestResource RESOURCE_SINGLE = new DummyTestResource(
            TEST_DIR, "resource-single-outbound.xml", "157796ed-d4f2-429d-84f3-00ce4164263b", "single",
            controller -> {
                controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                        ATTR_GIVEN_NAME, String.class, false, false);
                controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                        ATTR_FAMILY_NAME, String.class, false, false);
                controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                        ATTR_PERSONAL_NUMBER, String.class, false, false);
            });

    private static final TestObject<ArchetypeType> ARCHETYPE_PERSON = TestObject.file(
            TEST_DIR, "archetype-person.xml", "3a6f3ddd-ac72-4656-abac-0e306cd29645");
    private static final TestObject<ObjectTemplateType> OBJECT_TEMPLATE_PERSON = TestObject.file(
            TEST_DIR, "object-template-person.xml", "c0d96ed0-bec7-4c6e-9a69-133b0301bdb8");

    private String markNoSyncOid;

    private String markReadOnlyOid;

    private Object markPolicyNoOutbound;

    @BeforeMethod
    public void onNativeOnly() {
        skipIfNotNativeRepository();
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        if (!isNativeRepository()) {
            return;
        }
        super.initSystem(initTask, initResult);
        addObject(OBJECT_TEMPLATE_PERSON, initTask, initResult);
        addObject(ARCHETYPE_PERSON, initTask, initResult);
        initAndTestDummyResource(RESOURCE_SINGLE, initTask, initResult);
        addObject(CommonInitialObjects.ARCHETYPE_OBJECT_MARK, initTask, initResult);
        addObject(CommonInitialObjects.MARK_PROTECTED, initTask, initResult);

        markNoSyncOid = addObject(new MarkType()
                .name("Skip Synchronization")
                .assignment(
                        new AssignmentType()
                        .targetRef(SystemObjectsType.ARCHETYPE_OBJECT_MARK.value(), MarkType.COMPLEX_TYPE))
                .objectOperationPolicy(new ObjectOperationPolicyType()
                        .synchronize(new SynchronizeOperationPolicyConfigurationType()
                                .inbound(new OperationPolicyConfigurationType().enabled(false))
                                )
                        ),  initTask, initResult);
        // Read-only is base for do-not touch
        markReadOnlyOid = addObject(new MarkType()
                .name("Read Only")
                .assignment(
                        new AssignmentType()
                        .targetRef(SystemObjectsType.ARCHETYPE_OBJECT_MARK.value(), MarkType.COMPLEX_TYPE))
                .objectOperationPolicy(new ObjectOperationPolicyType()
                        .add(new OperationPolicyConfigurationType().enabled(false))
                        .modify(new OperationPolicyConfigurationType().enabled(false))
                        .delete(new OperationPolicyConfigurationType().enabled(false))
                        ),  initTask, initResult);

        markPolicyNoOutbound = addObject(new MarkType()
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
                                .outbound(new OperationPolicyConfigurationType().enabled(false))
                                )
                        ), initTask, initResult);
    }

    @Test
    public void test100ImportUserAndMarkNoSync() throws Exception {
        var result = createOperationResult();
        var task = createTask();

        DummyAccount account1 = RESOURCE_SINGLE.controller.addAccount("brown");
        account1.addAttributeValue(ATTR_GIVEN_NAME, "Karl");
        account1.addAttributeValue(ATTR_FAMILY_NAME, "Brown");
        account1.addAttributeValue(ATTR_PERSONAL_NUMBER, "1004444");

        importAccountsRequest()
            .withResourceOid(RESOURCE_SINGLE.oid)
            .withNameValue("brown")
            .execute(result);

        assertNotNull(result);

        // Find user brown
        PrismObject<UserType> userOrig = searchObjectByName(UserType.class, "brown");
        ObjectReferenceType shadow1Ref = userOrig.asObjectable().getLinkRef().get(0);

        account1.replaceAttributeValue(ATTR_FAMILY_NAME, "Brownie");

        importAccountsRequest()
            .withResourceOid(RESOURCE_SINGLE.oid)
            .withNameValue("brown")
            .execute(result);

        PrismObject<UserType> userAfter = searchObjectByName(UserType.class, "brown");
        assertEquals(userAfter.asObjectable().getFamilyName().getOrig(), "Brownie");

        // Mark shadow do not synchronize
        markShadow(shadow1Ref.getOid(), markNoSyncOid, task, result);

        account1.replaceAttributeValue(ATTR_PERSONAL_NUMBER, "555555");

        importAccountsRequest()
        .withResourceOid(RESOURCE_SINGLE.oid)
        .withNameValue("brown")
        .execute(result);

        userAfter = searchObjectByName(UserType.class, "brown");
        assertEquals(userOrig.findItem(PATH_PERSONAL_NUMBER), userAfter.findItem(PATH_PERSONAL_NUMBER));

        recomputeUser(userAfter.getOid());
        userAfter = searchObjectByName(UserType.class, "brown");
        assertEquals(userOrig.findItem(PATH_PERSONAL_NUMBER), userAfter.findItem(PATH_PERSONAL_NUMBER));

        reconcileUser(userAfter.getOid(), getTestTask(), result);
        userAfter = searchObjectByName(UserType.class, "brown");
        assertEquals(userOrig.findItem(PATH_PERSONAL_NUMBER), userAfter.findItem(PATH_PERSONAL_NUMBER));

    }

    @Test
    public void test200ImportUserAndMarkReadOnly() throws Exception {
        var result = createOperationResult();
        var task = createTask();

        DummyAccount account1 = RESOURCE_SINGLE.controller.addAccount("reddy");
        account1.addAttributeValue(ATTR_GIVEN_NAME, "Karl");
        account1.addAttributeValue(ATTR_FAMILY_NAME, "Reddy");
        account1.addAttributeValue(ATTR_PERSONAL_NUMBER, "2004444");

        importAccountsRequest()
            .withResourceOid(RESOURCE_SINGLE.oid)
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
            .withResourceOid(RESOURCE_SINGLE.oid)
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
        DummyAccount account1 = RESOURCE_SINGLE.controller.addAccount("broken");
        account1.addAttributeValue(ATTR_GIVEN_NAME, "Karl");
        account1.addAttributeValue(ATTR_FAMILY_NAME, "Reddy");
        account1.addAttributeValue(ATTR_PERSONAL_NUMBER, "Broken");

        importAccountsRequest()
            .withResourceOid(RESOURCE_SINGLE.oid)
            .withNameValue("broken")
            .withAssertSuccess(false)
            .execute(result);

        assertNotNull(result);
    }

    // Add users / synchronize users
    // Mark shadow "Correlate later" and verify it is not synced

    // Mark Existing user shadow "Do not touch" and verify it is not synced / modified

    // Mark existing user shadow "Invalid data and verify it is not synced / modified"
}
