/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.rbac;

import static org.testng.AssertJUnit.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.test.TestObject;

import org.jetbrains.annotations.NotNull;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignmentTarget;
import com.evolveum.midpoint.model.api.context.EvaluatedResourceObjectConstruction;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.EvaluationTimeType;

@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestRbac extends AbstractRbacTest {

    private static final String LOCALITY_TORTUGA = "Tortuga";

    private static final TestObject<?> METAROLE_DETECTING_MODIFICATIONS = TestObject.file(
            TEST_DIR, "metarole-detecting-modifications.xml", "91c6756a-1056-4052-9752-e16887912a45");

    private static final TestObject<?> ROLE_DETECTING_MODIFICATIONS = TestObject.file(
            TEST_DIR, "role-detecting-modifications.xml", "42ef2848-3793-4120-8d03-d8e5f8c23237");

    private static final TestObject<RoleType> ROLE_NON_UNASSIGNABLE = TestObject.file(TEST_DIR, "role-non-unassignable.xml", "26081889-83e2-461f-a8cc-4c9ef415a4ff");
    private static final File GLOBAL_POLICY_RULES_ASSIGNMENT_DELETION = new File(TEST_DIR, "global-policy-rules-assignment-deletion.xml");

    private String userSharptoothOid;
    private String userRedskullOid;
    private String userBignoseOid;

    private static final String EXISTING_GOSSIP = "Black spot!";

    private String accountJackRedOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult)
            throws Exception {
        super.initSystem(initTask, initResult);

        repoAdd(METAROLE_DETECTING_MODIFICATIONS, initResult);
        repoAdd(ROLE_DETECTING_MODIFICATIONS, initResult);

        repoAdd(ROLE_NON_UNASSIGNABLE, initResult);
    }

    @Test
    public void test000SanityRolePirate() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        PrismObject<RoleType> rolePirate = modelService.getObject(RoleType.class, ROLE_PIRATE_OID, null, task, result);

        // THEN
        display("Role pirate", rolePirate);
        IntegrationTestTools.displayXml("Role pirate", rolePirate);
        assertNotNull("No pirate", rolePirate);

        PrismAsserts.assertEquivalent(ROLE_PIRATE_FILE, rolePirate);
    }

    @Test
    public void test001SanityRoleProjectOmnimanager() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        PrismObject<RoleType> roleOmnimanager = modelService.getObject(RoleType.class, ROLE_PROJECT_OMNINAMAGER_OID, null, task, result);

        // THEN
        display("Role omnimanager", roleOmnimanager);
        IntegrationTestTools.displayXml("Role omnimanager", roleOmnimanager);
        assertNotNull("No omnimanager", roleOmnimanager);

        ObjectReferenceType targetRef = roleOmnimanager.asObjectable().getInducement().get(0).getTargetRef();
        assertEquals("Wrong targetRef resolutionTime", EvaluationTimeType.RUN, targetRef.getResolutionTime());
        assertNull("targetRef is resolved", targetRef.getOid());
    }

    @Test
    public void test010SearchRequestableRoles() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = prismContext.queryFor(RoleType.class)
                .item(RoleType.F_REQUESTABLE).eq(true)
                .build();

        // WHEN
        List<PrismObject<RoleType>> requestableRoles = modelService.searchObjects(RoleType.class, query, null, task, result);

        // THEN
        display("Requestable roles", requestableRoles);

        assertEquals("Unexpected number of requestable roles", 3, requestableRoles.size());
    }

    @Test
    public void test101JackAssignRolePirate() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        assertSuccess(result);

        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
        // @formatter:off
        var userAfter = assertUserAfter(USER_JACK_OID)
                .assertModifyMetadataComplex(startTs, endTs)
                .assignments()
                    .by().roleOid(ROLE_PIRATE_OID).find()
                        .activation()
                            .assertEffectiveStatus(ActivationStatusType.ENABLED)
                        .end()
                        .valueMetadataSingle()
                            .assertCreateMetadataComplex(startTs, endTs)
                        .end()
                    .end()
                .end()
                .getObject();
        // @formatter:on

        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);
        assertDelegatedRef(userAfter);
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, ROLE_PIRATE_TITLE);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, USER_JACK_LOCALITY);
        // Outbound mapping for weapon is weak, therefore the mapping in role should override it
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, ROLE_PIRATE_WEAPON);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME,
                "Jack Sparrow is the best pirate Caribbean has ever seen");
    }

    protected @NotNull ModelExecuteOptions getDefaultOptions() {
        return ModelExecuteOptions.create();
    }

    /**
     * We modify Jack's "locality". As this is assigned by expression in the role to the dummy account, the account should
     * be updated as well.
     */
    @Test
    public void test102JackModifyUserLocality() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // gossip is a tolerant attribute. Make sure there there is something to tolerate
        DummyAccount jackDummyAccount = getDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME);
        jackDummyAccount.addAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME,
                EXISTING_GOSSIP);

        // WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_LOCALITY, getDefaultOptions(), task, result,
                PolyString.fromOrig(LOCALITY_TORTUGA));

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);
        assertDelegatedRef(userAfter);
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, ROLE_PIRATE_TITLE);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, LOCALITY_TORTUGA);
        // Outbound mapping for weapon is weak, therefore the mapping in role should override it
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, ROLE_PIRATE_WEAPON);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME,
                "Jack Sparrow is the best pirate Tortuga has ever seen", EXISTING_GOSSIP);
    }

    @Test
    public void test110UnAssignRolePirate() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        unassignRole(USER_JACK_OID, ROLE_PIRATE_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedNoRole(userAfter);
        assertRoleMembershipRef(userAfter);
        assertDelegatedRef(userAfter);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    @Test
    public void test120JackAssignRolePirateWhileAlreadyHasAccount() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);

        // Make sure that the account has explicit intent
        account.asObjectable().setKind(ShadowKindType.ACCOUNT);
        account.asObjectable().setIntent(SchemaConstants.INTENT_DEFAULT);

        // Make sure that the existing account has the same value as is set by the role
        // This causes problems if the resource does not tolerate duplicate values in deltas. But provisioning
        // should work around that.
        TestUtil.setAttribute(account,
                getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME),
                DOMUtil.XSD_STRING, ROLE_PIRATE_TITLE);

        ObjectDelta<UserType> delta = prismContext.deltaFactory().object()
                .createModificationAddReference(UserType.class, USER_JACK_OID,
                        UserType.F_LINK_REF, account);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);

        // We need to switch off the enforcement for this operation. Otherwise we won't be able to create the account
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        modelService.executeChanges(deltas, getDefaultOptions(), task, result);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        // Precondition (simplified)
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "rum");

        // gossip is a tolerant attribute. Make sure there there is something to tolerate
        DummyAccount jackDummyAccount = getDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME);
        jackDummyAccount.addAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME,
                EXISTING_GOSSIP);

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 1);
        assertLiveLinks(userJack, 1);
        assertAssignedRole(userJack, ROLE_PIRATE_OID);
        assertRoleMembershipRef(userJack, ROLE_PIRATE_OID);
        assertDelegatedRef(userJack);
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", ROLE_PIRATE_TITLE);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", LOCALITY_TORTUGA);
        // The account already has a value for 'weapon', it should be unchanged.
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "rum");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME,
                "Jack Sparrow is the best pirate Tortuga has ever seen", EXISTING_GOSSIP);

    }

    @Test
    public void test121JackAssignAccountImplicitIntent() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Precondition (simplified)
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);

        // WHEN
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_OID, null, task, result);        // TODO options?

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 2);
        assertLiveLinks(userJack, 1);
        assertAssignedRole(userJack, ROLE_PIRATE_OID);
        assertRoleMembershipRef(userJack, ROLE_PIRATE_OID);
        assertDelegatedRef(userJack);
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", ROLE_PIRATE_TITLE);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", LOCALITY_TORTUGA);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME,
                "Jack Sparrow is the best pirate Tortuga has ever seen", EXISTING_GOSSIP);

    }

    @Test
    public void test122JackAssignAccountExplicitIntent() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Precondition (simplified)
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);

        // WHEN
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_OID, SchemaConstants.INTENT_DEFAULT, task, result);        // TODO options?

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 3);
        assertLiveLinks(userJack, 1);
        assertAssignedRole(userJack, ROLE_PIRATE_OID);
        assertRoleMembershipRef(userJack, ROLE_PIRATE_OID);
        assertDelegatedRef(userJack);
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", ROLE_PIRATE_TITLE);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", LOCALITY_TORTUGA);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME,
                "Jack Sparrow is the best pirate Tortuga has ever seen", EXISTING_GOSSIP);

    }

    @Test
    public void test127UnAssignAccountImplicitIntent() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        unassignAccountFromUser(USER_JACK_OID, RESOURCE_DUMMY_OID, null, task, result);            // TODO options?

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 2);
        assertLiveLinks(userJack, 1);
        assertAssignedRole(userJack, ROLE_PIRATE_OID);
        assertRoleMembershipRef(userJack, ROLE_PIRATE_OID);
        assertDelegatedRef(userJack);
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", ROLE_PIRATE_TITLE);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", LOCALITY_TORTUGA);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME,
                "Jack Sparrow is the best pirate Tortuga has ever seen", EXISTING_GOSSIP);
    }

    @Test
    public void test128UnAssignAccountExplicitIntent() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        unassignAccountFromUser(USER_JACK_OID, RESOURCE_DUMMY_OID, SchemaConstants.INTENT_DEFAULT, task, result);        // TODO options?

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 1);
        assertLiveLinks(userJack, 1);
        assertAssignedRole(userJack, ROLE_PIRATE_OID);
        assertRoleMembershipRef(userJack, ROLE_PIRATE_OID);
        assertDelegatedRef(userJack);
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", ROLE_PIRATE_TITLE);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", LOCALITY_TORTUGA);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME,
                "Jack Sparrow is the best pirate Tortuga has ever seen", EXISTING_GOSSIP);
    }

    @Test
    public void test129UnAssignRolePirate() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        unassignRole(USER_JACK_OID, ROLE_PIRATE_OID, getDefaultOptions(), task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 0);
        assertRoleMembershipRef(userJack);
        assertDelegatedRef(userJack);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    @Test
    public void test130JackAssignRolePirateWithSeaInAssignment() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismContainer<?> extension = getAssignmentExtensionInstance();
        PrismSchema piracySchema = getPiracySchema();
        //noinspection unchecked
        PrismPropertyDefinition<String> seaPropDef = piracySchema.findPropertyDefinitionByElementName(PIRACY_SEA_QNAME);
        PrismProperty<String> seaProp = seaPropDef.instantiate();
        seaProp.setRealValue("Caribbean");
        extension.add(seaProp);

        // WHEN
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, extension, getDefaultOptions(), task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);
        assertDelegatedRef(userAfter);
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", ROLE_PIRATE_TITLE);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", LOCALITY_TORTUGA);
        // Outbound mapping for weapon is weak, therefore the mapping in role should override it
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", ROLE_PIRATE_WEAPON);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME,
                "Jack Sparrow is the best pirate Tortuga has ever seen");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DUMMY_ACCOUNT_ATTRIBUTE_SEA_NAME,
                "jack sailed Caribbean, immediately Caribbean, role , with this The Seven Seas while focused on Caribbean (in Pirate)");
    }

    @Test
    public void test132JackUnAssignRolePirateWithSeaInAssignment() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismContainer<?> extension = getAssignmentExtensionInstance();
        PrismSchema piracySchema = getPiracySchema();
        //noinspection unchecked
        PrismPropertyDefinition<String> seaPropDef = piracySchema.findPropertyDefinitionByElementName(PIRACY_SEA_QNAME);
        PrismProperty<String> seaProp = seaPropDef.instantiate();
        seaProp.setRealValue("Caribbean");
        extension.add(seaProp);

        // WHEN
        unassignRole(USER_JACK_OID, ROLE_PIRATE_OID, extension, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 0);
        assertRoleMembershipRef(userJack);
        assertDelegatedRef(userJack);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    /**
     * The value for sea is set in Adriatic Pirate role extension.
     */
    @Test
    public void test134JackAssignRoleAdriaticPirate() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        assignRole(USER_JACK_OID, ROLE_ADRIATIC_PIRATE_OID, getDefaultOptions(), task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_ADRIATIC_PIRATE_OID);
        assertRoleMembershipRef(userAfter, ROLE_ADRIATIC_PIRATE_OID, ROLE_PIRATE_OID);
        assertDelegatedRef(userAfter);
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", ROLE_PIRATE_TITLE);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", LOCALITY_TORTUGA);
        // Outbound mapping for weapon is weak, therefore the mapping in role should override it
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", ROLE_PIRATE_WEAPON);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME,
                "Jack Sparrow is the best pirate Tortuga has ever seen");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DUMMY_ACCOUNT_ATTRIBUTE_SEA_NAME,
                "jack sailed Adriatic, immediately Adriatic, role , with this The Seven Seas while focused on  (in Pirate)");
    }

    /**
     * Check if all the roles are visible in preview changes
     */
    @Test
    public void test135PreviewChangesEmptyDelta() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        ObjectDelta<UserType> delta = user.createModifyDelta();

        // WHEN
        // The following options provide the pre-4.9-like behavior of previewChanges()
        var options = getDefaultOptions()
                .firstClickOnly()
                .previewPolicyRulesEnforcement();
        ModelContext<ObjectType> modelContext = modelInteractionService.previewChanges(List.of(delta), options, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        DeltaSetTriple<? extends EvaluatedAssignment> evaluatedAssignmentTriple = modelContext.getEvaluatedAssignmentTriple();
        PrismAsserts.assertTripleNoPlus(evaluatedAssignmentTriple);
        PrismAsserts.assertTripleNoMinus(evaluatedAssignmentTriple);
        Collection<? extends EvaluatedAssignment> evaluatedAssignments = evaluatedAssignmentTriple.getZeroSet();
        assertEquals("Wrong number of evaluated assignments", 1, evaluatedAssignments.size());
        EvaluatedAssignment evaluatedAssignment = evaluatedAssignments.iterator().next();
        DeltaSetTriple<? extends EvaluatedAssignmentTarget> rolesTriple = evaluatedAssignment.getRoles();
        PrismAsserts.assertTripleNoPlus(rolesTriple);
        PrismAsserts.assertTripleNoMinus(rolesTriple);
        Collection<? extends EvaluatedAssignmentTarget> evaluatedRoles = rolesTriple.getZeroSet();
        assertEquals("Wrong number of evaluated role", 2, evaluatedRoles.size());
        assertEvaluatedRole(evaluatedRoles, ROLE_ADRIATIC_PIRATE_OID);
        assertEvaluatedRole(evaluatedRoles, ROLE_PIRATE_OID);
    }

    @Test
    public void test136JackUnAssignRoleAdriaticPirate() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        unassignRole(USER_JACK_OID, ROLE_ADRIATIC_PIRATE_OID, getDefaultOptions(), task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 0);
        assertRoleMembershipRef(userJack);
        assertDelegatedRef(userJack);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    /**
     * Even though we assign Adriatic Pirate role which has a sea set in its extension the
     * sea set in user's extension should override it.
     */
    @Test
    public void test137JackAssignRoleAdriaticPirateWithSeaInAssignment() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismContainer<?> extension = getAssignmentExtensionInstance();
        PrismSchema piracySchema = getPiracySchema();
        //noinspection unchecked
        PrismPropertyDefinition<String> seaPropDef = piracySchema.findPropertyDefinitionByElementName(PIRACY_SEA_QNAME);
        PrismProperty<String> seaProp = seaPropDef.instantiate();
        seaProp.setRealValue("Caribbean");
        extension.add(seaProp);

        // WHEN
        assignRole(USER_JACK_OID, ROLE_ADRIATIC_PIRATE_OID, extension, getDefaultOptions(), task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_ADRIATIC_PIRATE_OID);
        assertRoleMembershipRef(userAfter, ROLE_ADRIATIC_PIRATE_OID, ROLE_PIRATE_OID);
        assertDelegatedRef(userAfter);
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", ROLE_PIRATE_TITLE);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", LOCALITY_TORTUGA);
        // Outbound mapping for weapon is weak, therefore the mapping in role should override it
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", ROLE_PIRATE_WEAPON);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME,
                "Jack Sparrow is the best pirate Tortuga has ever seen");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DUMMY_ACCOUNT_ATTRIBUTE_SEA_NAME,
                "jack sailed Caribbean, immediately Adriatic, role , with this The Seven Seas while focused on Caribbean (in Pirate)");
    }

    @Test
    public void test139JackUnAssignRoleAdriaticPirateWithSeaInAssignment() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismContainer<?> extension = getAssignmentExtensionInstance();
        PrismSchema piracySchema = getPiracySchema();
        //noinspection unchecked
        PrismPropertyDefinition<String> seaPropDef = piracySchema.findPropertyDefinitionByElementName(PIRACY_SEA_QNAME);
        PrismProperty<String> seaProp = seaPropDef.instantiate();
        seaProp.setRealValue("Caribbean");
        extension.add(seaProp);

        // WHEN
        unassignRole(USER_JACK_OID, ROLE_ADRIATIC_PIRATE_OID, extension, getDefaultOptions(), task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 0);
        assertRoleMembershipRef(userJack);
        assertDelegatedRef(userJack);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    @Test
    public void test144JackAssignRoleBlackSeaPirate() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        assignRole(USER_JACK_OID, ROLE_BLACK_SEA_PIRATE_OID, getDefaultOptions(), task, result);

        // THEN
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_BLACK_SEA_PIRATE_OID);
        assertRoleMembershipRef(userAfter, ROLE_BLACK_SEA_PIRATE_OID, ROLE_PIRATE_OID);
        assertDelegatedRef(userAfter);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", ROLE_PIRATE_TITLE);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", LOCALITY_TORTUGA);
        // Outbound mapping for weapon is weak, therefore the mapping in role should override it
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", ROLE_PIRATE_WEAPON);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME,
                "Jack Sparrow is the best pirate Tortuga has ever seen");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DUMMY_ACCOUNT_ATTRIBUTE_SEA_NAME,
                "jack sailed Marmara Sea, immediately Marmara Sea, role Black Sea, with this The Seven Seas while focused on  (in Pirate)");
    }

    @Test
    public void test146JackUnAssignRoleBlackSeaPirate() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        unassignRole(USER_JACK_OID, ROLE_BLACK_SEA_PIRATE_OID, getDefaultOptions(), task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 0);
        assertRoleMembershipRef(userJack);
        assertDelegatedRef(userJack);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    @Test
    public void test147JackAssignRoleBlackSeaPirateWithSeaInAssignment() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismContainer<?> extension = getAssignmentExtensionInstance();
        PrismSchema piracySchema = getPiracySchema();
        //noinspection unchecked
        PrismPropertyDefinition<String> seaPropDef = piracySchema.findPropertyDefinitionByElementName(PIRACY_SEA_QNAME);
        PrismProperty<String> seaProp = seaPropDef.instantiate();
        seaProp.setRealValue("Caribbean");
        extension.add(seaProp);

        // WHEN
        assignRole(USER_JACK_OID, ROLE_BLACK_SEA_PIRATE_OID, extension, getDefaultOptions(), task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_BLACK_SEA_PIRATE_OID);
        assertRoleMembershipRef(userAfter, ROLE_BLACK_SEA_PIRATE_OID, ROLE_PIRATE_OID);
        assertDelegatedRef(userAfter);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", ROLE_PIRATE_TITLE);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", LOCALITY_TORTUGA);
        // Outbound mapping for weapon is weak, therefore the mapping in role should override it
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", ROLE_PIRATE_WEAPON);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME,
                "Jack Sparrow is the best pirate Tortuga has ever seen");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DUMMY_ACCOUNT_ATTRIBUTE_SEA_NAME,
                "jack sailed Caribbean, immediately Marmara Sea, role Black Sea, with this The Seven Seas while focused on Caribbean (in Pirate)");
    }

    @Test
    public void test149JackUnAssignRoleBlackSeaPirateWithSeaInAssignment() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismContainer<?> extension = getAssignmentExtensionInstance();
        PrismSchema piracySchema = getPiracySchema();
        //noinspection unchecked
        PrismPropertyDefinition<String> seaPropDef = piracySchema.findPropertyDefinitionByElementName(PIRACY_SEA_QNAME);
        PrismProperty<String> seaProp = seaPropDef.instantiate();
        seaProp.setRealValue("Caribbean");
        extension.add(seaProp);

        // WHEN
        unassignRole(USER_JACK_OID, ROLE_BLACK_SEA_PIRATE_OID, extension, getDefaultOptions(), task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 0);
        assertRoleMembershipRef(userJack);
        assertDelegatedRef(userJack);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    @Test
    public void test154JackAssignRoleIndianOceanPirate() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_INDIAN_OCEAN_PIRATE_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        display("Result", result);
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_INDIAN_OCEAN_PIRATE_OID);
        assertRoleMembershipRef(userAfter, ROLE_INDIAN_OCEAN_PIRATE_OID, ROLE_PIRATE_OID);
        assertDelegatedRef(userAfter);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", ROLE_PIRATE_TITLE);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", LOCALITY_TORTUGA);
        // Outbound mapping for weapon is weak, therefore the mapping in role should override it
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", ROLE_PIRATE_WEAPON);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME,
                "Jack Sparrow is the best pirate Tortuga has ever seen");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DUMMY_ACCOUNT_ATTRIBUTE_SEA_NAME,
                "jack sailed Indian Ocean, immediately , role Indian Ocean, with this The Seven Seas while focused on  (in Pirate)");
    }

    @Test
    public void test156JackUnAssignRoleIndianOceanPirate() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        unassignRole(USER_JACK_OID, ROLE_INDIAN_OCEAN_PIRATE_OID, getDefaultOptions(), task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignments(userJack, 0);
        assertRoleMembershipRef(userJack);
        assertDelegatedRef(userJack);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    /**
     * Approver relation is not supposed to give any role privileges.
     * MID-3580
     */
    @Test
    public void test160JackAssignRolePirateApprover() throws Exception {
        testJackAssignRolePirateRelationNoPrivs(SchemaConstants.ORG_APPROVER);
    }

    /**
     * MID-3580
     */
    @Test
    public void test162JackUnassignRolePirateApprover() throws Exception {
        testJackUnassignRolePirateRelationNoPrivs(SchemaConstants.ORG_APPROVER);

    }

    /**
     * Owner relation is not supposed to give any role privileges.
     * MID-3580
     */
    @Test
    public void test164JackAssignRolePirateOwner() throws Exception {
        testJackAssignRolePirateRelationNoPrivs(SchemaConstants.ORG_OWNER);
    }

    /**
     * MID-3580
     */
    @Test
    public void test166JackUnassignRolePirateOwner() throws Exception {
        testJackUnassignRolePirateRelationNoPrivs(SchemaConstants.ORG_OWNER);
    }

    /**
     * Unknown custom relation is not supposed to give any role privileges.
     * MID-3580
     */
    @Test
    public void test168JackAssignRolePirateComplicated() throws Exception {
        testJackAssignRolePirateRelationNoPrivs(RELATION_COMPLICATED_QNAME);
    }

    /**
     * MID-3580
     */
    @Test
    public void test169JackUnassignRolePirateComplicated() throws Exception {
        testJackUnassignRolePirateRelationNoPrivs(RELATION_COMPLICATED_QNAME);
    }

    private void testJackAssignRolePirateRelationNoPrivs(QName relation) throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        assertNoAssignments(userBefore);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);

        // WHEN (1)
        when();
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, relation, getDefaultOptions(), task, result);

        // THEN (1)
        then();
        assertSuccess(result);

        assertJackAssignRolePirateRelationNoPrivs(relation);

        // WHEN (2)
        when();
        recomputeUser(USER_JACK_OID, getDefaultOptions(), task, result);

        // THEN (2)
        then();
        assertSuccess(result);

        assertJackAssignRolePirateRelationNoPrivs(relation);
    }

    private void assertJackAssignRolePirateRelationNoPrivs(QName relation) throws Exception {
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        // Still needs to be as a member, although with the right relation.
        assertRoleMembershipRef(userAfter, relation, ROLE_PIRATE_OID);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    private void testJackUnassignRolePirateRelationNoPrivs(QName relation) throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        unassignRole(USER_JACK_OID, ROLE_PIRATE_OID, relation, getDefaultOptions(), task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertNoAssignments(userAfter);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    // TODO: assign with owner relation
    // TODO: assign with custom(unknown) relation

    /**
     * Import role with dynamic target resolution.
     * Make sure that the reference is NOT resolved at import time.
     */
    @Test
    public void test200ImportRoleAllTreasure() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        importObjectFromFile(ROLE_ALL_TREASURE_FILE, task, result);

        // THEN
        assertSuccess(result);

        PrismObject<RoleType> roleAfter = getObject(RoleType.class, ROLE_ALL_TREASURE_OID);
        display("Role after", roleAfter);
        ObjectReferenceType targetRef = roleAfter.asObjectable().getInducement().get(0).getTargetRef();
        assertNull("Unexpected OID in targetRef", targetRef.getOid());
    }

    /**
     * General simple test for roles with dynamic target resolution.
     */
    @Test
    public void test202JackAssignRoleAllTreasure() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        assertAssignments(userBefore, 0);

        // WHEN
        assignRole(USER_JACK_OID, ROLE_ALL_TREASURE_OID, getDefaultOptions(), task, result);

        // THEN
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_ALL_TREASURE_OID);
        assertRoleMembershipRef(userAfter, ROLE_ALL_TREASURE_OID,
                ROLE_TREASURE_BRONZE_OID, ROLE_TREASURE_SILVER_OID);
        assertDelegatedRef(userAfter);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title",
                "Silver treasure", "Bronze treasure");
    }

    /**
     * Add gold treasure role. This should be picked up by the dynamic
     * "all treasure" role.
     */
    @Test
    public void test204AddGoldTreasureAndRecomputeJack() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        addObject(ROLE_TREASURE_GOLD_FILE);

        // WHEN
        recomputeUser(USER_JACK_OID, getDefaultOptions(), task, result);

        // THEN
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_ALL_TREASURE_OID);
        assertRoleMembershipRef(userAfter, ROLE_ALL_TREASURE_OID,
                ROLE_TREASURE_BRONZE_OID, ROLE_TREASURE_SILVER_OID, ROLE_TREASURE_GOLD_OID);
        assertDelegatedRef(userAfter);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title",
                "Silver treasure", "Bronze treasure", "Golden treasure");
    }

    /**
     * MID-3966
     */
    @Test
    public void test206JackAssignRoleAllLoot() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        assignRole(USER_JACK_OID, ROLE_ALL_LOOT_OID, getDefaultOptions(), task, result);

        // THEN
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_ALL_TREASURE_OID);
        assertRoleMembershipRef(userAfter, ROLE_ALL_TREASURE_OID,
                ROLE_TREASURE_BRONZE_OID, ROLE_TREASURE_SILVER_OID, ROLE_TREASURE_GOLD_OID,
                ROLE_ALL_LOOT_OID, ROLE_LOOT_DIAMONDS_OID);
        assertDelegatedRef(userAfter);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title",
                "Silver treasure", "Bronze treasure", "Golden treasure", "Diamond loot");
    }

    @Test
    public void test208JackUnassignRoleAllLoot() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        unassignRole(USER_JACK_OID, ROLE_ALL_LOOT_OID, getDefaultOptions(), task, result);

        // THEN
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_ALL_TREASURE_OID);
        assertRoleMembershipRef(userAfter, ROLE_ALL_TREASURE_OID,
                ROLE_TREASURE_BRONZE_OID, ROLE_TREASURE_SILVER_OID, ROLE_TREASURE_GOLD_OID);
        assertDelegatedRef(userAfter);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title",
                "Silver treasure", "Bronze treasure", "Golden treasure");
    }

    @Test
    public void test209JackUnassignRoleAllTreasure() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        unassignRole(USER_JACK_OID, ROLE_ALL_TREASURE_OID, getDefaultOptions(), task, result);

        // THEN
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 0);

        assertNoDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME);
    }

    /**
     * MID-3966
     */
    @Test(enabled = false) // MID-3966
    public void test210JackAssignRoleAllYouCanGet() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        assignRole(USER_JACK_OID, ROLE_ALL_YOU_CAN_GET_OID, getDefaultOptions(), task, result);

        // THEN
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_ALL_YOU_CAN_GET_OID);
        assertRoleMembershipRef(userAfter, ROLE_ALL_YOU_CAN_GET_OID,
                ROLE_TREASURE_BRONZE_OID, ROLE_TREASURE_SILVER_OID, ROLE_TREASURE_GOLD_OID,
                ROLE_LOOT_DIAMONDS_OID);
        assertDelegatedRef(userAfter);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title",
                "Silver treasure", "Bronze treasure", "Golden treasure", "Diamond loot");
    }

    @Test(enabled = false) // MID-3966
    public void test219JackUnassignRoleAllYouCanGet() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        unassignRole(USER_JACK_OID, ROLE_ALL_YOU_CAN_GET_OID, getDefaultOptions(), task, result);

        // THEN
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 0);

        assertNoDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME);
    }

    //////////////////////
    // Following tests use POSITIVE enforcement mode
    /////////////////////

    @Test
    public void test501JackAssignRolePirate() throws Exception {
        // IMPORTANT: Changing the assignment policy
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, getDefaultOptions(), task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);
        assertDelegatedRef(userAfter);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", ROLE_PIRATE_TITLE);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", LOCALITY_TORTUGA);
        // Outbound mapping for weapon is weak, therefore the mapping in role should override it
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", ROLE_PIRATE_WEAPON);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME,
                "Jack Sparrow is the best pirate Tortuga has ever seen");
    }

    /**
     * We modify Jack's "locality". As this is assigned by expression in the role to the dummy account, the account should
     * be updated as well.
     */
    @Test
    public void test502JackModifyUserLocality() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // gossip is a tolerant attribute. Make sure there there is something to tolerate
        DummyAccount jackDummyAccount = getDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME);
        jackDummyAccount.addAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME,
                EXISTING_GOSSIP);

        // WHEN
        when();
        modifyUserReplace(
                USER_JACK_OID, UserType.F_LOCALITY,
                getDefaultOptions(), task, result,
                PolyString.fromOrig("Isla de Muerta"));

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);
        assertDelegatedRef(userAfter);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", ROLE_PIRATE_TITLE);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", "Isla de Muerta");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", ROLE_PIRATE_WEAPON);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME,
                "Jack Sparrow is the best pirate Isla de Muerta has ever seen",
                "Jack Sparrow is the best pirate Tortuga has ever seen", // Positive enforcement. This vales are not removed.
                EXISTING_GOSSIP);
    }

    /**
     * Assignment policy is POSITIVE, therefore the account should remain.
     */
    @Test
    public void test510UnAssignRolePirate() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        unassignRole(USER_JACK_OID, ROLE_PIRATE_OID, getDefaultOptions(), task, result);

        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignedNoRole(userJack);
        assertRoleMembershipRef(userJack);
        assertDelegatedRef(userJack);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", "Isla de Muerta");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME,
                "Jack Sparrow is the best pirate Isla de Muerta has ever seen", // Positive enforcement. This vales are not removed.
                "Jack Sparrow is the best pirate Tortuga has ever seen", // Positive enforcement. This vales are not removed.
                EXISTING_GOSSIP);
    }

    /**
     * This should go fine without any policy violation error.
     */
    @Test
    public void test511DeleteAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        String accountOid = userJack.asObjectable().getLinkRef().iterator().next().getOid();

        ObjectDelta<ShadowType> accountDelta = prismContext.deltaFactory().object()
                .createDeleteDelta(ShadowType.class, accountOid);
        // Use modification of user to delete account. Deleting account directly is tested later.
        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createModificationDeleteReference(UserType.class, USER_JACK_OID, UserType.F_LINK_REF,
                        accountOid);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta, accountDelta);

        // WHEN
        modelService.executeChanges(deltas, getDefaultOptions(), task, result);

        // THEN
        userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignedNoRole(userJack);
        assertRoleMembershipRef(userJack);
        assertDelegatedRef(userJack);
        assertNoLinkedAccount(userJack);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    @Test
    public void test520JackAssignRolePirate() throws Exception {
        // IMPORTANT: Changing the assignment policy
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, getDefaultOptions(), task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);
        assertDelegatedRef(userAfter);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", ROLE_PIRATE_TITLE);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", "Isla de Muerta");
        // Outbound mapping for weapon is weak, therefore the mapping in role should override it
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", ROLE_PIRATE_WEAPON);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME,
                "Jack Sparrow is the best pirate Isla de Muerta has ever seen");
    }

    @Test
    public void test521JackUnassignRolePirateDeleteAccount() throws Exception {
        // IMPORTANT: Changing the assignment policy
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        Collection<ItemDelta<?, ?>> modifications = new ArrayList<>();
        modifications.add(createAssignmentModification(ROLE_PIRATE_OID, RoleType.COMPLEX_TYPE, null, null, null, false));
        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createModifyDelta(USER_JACK_OID, modifications, UserType.class);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        String accountOid = userJack.asObjectable().getLinkRef().iterator().next().getOid();
        ObjectDelta<ShadowType> accountDelta = prismContext.deltaFactory().object()
                .createDeleteDelta(ShadowType.class, accountOid);
        // This all goes in the same context with user, explicit unlink should not be necessary
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta, accountDelta);

        // WHEN
        modelService.executeChanges(deltas, getDefaultOptions(), task, result);

        // THEN
        userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignedNoRole(userJack);
        assertRoleMembershipRef(userJack);
        assertDelegatedRef(userJack);
        assertNoLinkedAccount(userJack);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    @Test
    public void test530JackAssignRoleCleric() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        assignRole(USER_JACK_OID, ROLE_CLERIC_OID, getDefaultOptions(), task, result);

        // THEN
        assertAssignedRole(USER_JACK_OID, ROLE_CLERIC_OID, result);
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Holy man");
    }

    @Test
    public void test532JackModifyAssignmentRoleCleric() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> user = getObject(UserType.class, USER_JACK_OID);

        ItemPath itemPath = ItemPath.create(UserType.F_ASSIGNMENT, user.asObjectable().getAssignment().get(0).getId(),
                AssignmentType.F_DESCRIPTION);
        ObjectDelta<UserType> assignmentDelta = prismContext.deltaFactory().object().createModificationReplaceProperty(
                UserType.class, USER_JACK_OID, itemPath, "soul");

        // WHEN
        when();
        modelService.executeChanges(MiscSchemaUtil.createCollection(assignmentDelta), getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_CLERIC_OID);
        assertRoleMembershipRef(userAfter, ROLE_CLERIC_OID);
        assertDelegatedRef(userAfter);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Holy soul");
    }

    @Test
    public void test539JackUnAssignRoleCleric() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> user = getObject(UserType.class, USER_JACK_OID);

        AssignmentType assignmentType = new AssignmentType();
        assignmentType.setId(user.asObjectable().getAssignment().get(0).getId());
        ObjectDelta<UserType> assignmentDelta = prismContext.deltaFactory().object().createModificationDeleteContainer(
                UserType.class, USER_JACK_OID, UserType.F_ASSIGNMENT, assignmentType);

        // WHEN
        when();
        executeChanges(assignmentDelta, getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after", userJack);
        assertAssignedNoRole(userJack);
        assertRoleMembershipRef(userJack);
        assertDelegatedRef(userJack);
        assertNoLinkedAccount(userJack);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    /**
     * Wannabe role is conditional. All conditions should be false now. So no provisioning should happen.
     */
    @Test
    public void test540JackAssignRoleWannabe() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_WANNABE_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_WANNABE_OID);
        assertRoleMembershipRef(userAfter);
        assertDelegatedRef(userAfter);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    /**
     * Remove honorificSuffix. This triggers a condition in Wannabe role inducement.
     * But as the whole role has false condition nothing should happen.
     */
    @Test
    public void test541JackRemoveHonorificSuffixWannabe() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        modifyUserReplace(USER_JACK_OID, UserType.F_HONORIFIC_SUFFIX, getDefaultOptions(), task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_WANNABE_OID);
        assertRoleMembershipRef(userAfter);
        assertDelegatedRef(userAfter);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    /**
     * Modify employeeType. This triggers a condition in Wannabe role.
     */
    @Test
    public void test542JackModifySubtypeWannabe() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        modifyUserReplace(USER_JACK_OID, UserType.F_SUBTYPE, getDefaultOptions(), task, result, "wannabe");

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_WANNABE_OID);
        assertRoleMembershipRef(userAfter, ROLE_WANNABE_OID);
        assertDelegatedRef(userAfter);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Wannabe Cpt. Where's the rum?");
    }

    /**
     * Remove honorificPrefix. This triggers a condition in Wannabe role and should remove an account.
     */
    @Test
    public void test543JackRemoveHonorificPrefixWannabe() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        modifyUserReplace(USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, getDefaultOptions(), task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_WANNABE_OID);
        assertRoleMembershipRef(userAfter, ROLE_WANNABE_OID);
        assertDelegatedRef(userAfter);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    /**
     * Set honorificSuffix. This triggers conditions and adds a sub-role Honorable Wannabe.
     */
    @Test
    public void test544JackSetHonorificSuffixWannabe() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        modifyUserReplace(
                USER_JACK_OID, UserType.F_HONORIFIC_SUFFIX, getDefaultOptions(), task, result,
                PolyString.fromOrig("PhD."));

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_WANNABE_OID);
        assertRoleMembershipRef(userAfter, ROLE_WANNABE_OID, ROLE_HONORABLE_WANNABE_OID);
        assertDelegatedRef(userAfter);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, "Arr!", "Whatever. -- jack");
    }

    /**
     * Restore honorificPrefix. The title should be replaced again.
     */
    @Test
    public void test545JackRestoreHonorificPrefixWannabe() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        modifyUserReplace(
                USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, getDefaultOptions(), task, result,
                PolyString.fromOrig("captain"));

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_WANNABE_OID);
        assertRoleMembershipRef(userAfter, ROLE_WANNABE_OID, ROLE_HONORABLE_WANNABE_OID);
        assertDelegatedRef(userAfter);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Wannabe captain Where's the rum?");
    }

    /**
     * The account should be gone - regardless of the condition state.
     */
    @Test
    public void test549JackUnassignRoleWannabe() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        unassignRole(USER_JACK_OID, ROLE_WANNABE_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertNotAssignedRole(userAfter, ROLE_WANNABE_OID);
        assertRoleMembershipRef(userAfter);
        assertDelegatedRef(userAfter);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    @Test
    public void test600JackAssignRoleJudge() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        assignRole(USER_JACK_OID, ROLE_JUDGE_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_JUDGE_OID);
        assertRoleMembershipRef(userAfter, ROLE_JUDGE_OID);
        assertDelegatedRef(userAfter);

        assertAssignedRole(USER_JACK_OID, ROLE_JUDGE_OID, result);
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Honorable Justice");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "mouth", "pistol");
//        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME,
//                "Jack Sparrow is the best pirate Caribbean has ever seen");
    }

    /**
     * Judge and pirate are excluded roles. This should fail.
     */
    @Test
    public void test602JackAssignRolePirate() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        try {
            // WHEN
            assignRole(USER_JACK_OID, ROLE_PIRATE_OID, getDefaultOptions(), task, result);

            AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
            displayExpectedException(e);
            assertMessage(e, "Violation of SoD policy: Role \"Judge\" excludes role \"Pirate\", they cannot be assigned at the same time");
        }

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertFailure(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_JUDGE_OID);
        assertRoleMembershipRef(userAfter, ROLE_JUDGE_OID);
        assertDelegatedRef(userAfter);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Honorable Justice");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "mouth", "pistol");
    }

    @Test
    public void test605JackUnAssignRoleJudgeAssignRolePirate() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<UserType> userDelta = createAssignmentUserDelta(USER_JACK_OID, ROLE_JUDGE_OID, RoleType.COMPLEX_TYPE, null, null, false);
        userDelta.addModification(createAssignmentModification(ROLE_PIRATE_OID, RoleType.COMPLEX_TYPE, null, null, null, true));

        // WHEN
        when();
        modelService.executeChanges(MiscSchemaUtil.createCollection(userDelta), getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);
        assertDelegatedRef(userAfter);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", ROLE_PIRATE_TITLE);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "location", "Isla de Muerta");
        // Even though Jack is a pirate now the mapping from the role is not applied.
        // the mapping is weak and the account already has a value for weapon from the times
        // Jack was a judge. So it is unchanged
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "mouth", "pistol");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME,
                "Jack Sparrow is the best pirate Isla de Muerta has ever seen");
    }

    @Test
    public void test609JackUnAssignRolePirate() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        unassignRole(USER_JACK_OID, ROLE_PIRATE_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    @Test
    public void test610ElaineAssignRoleGovernor() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_ELAINE_OID);
        display("User before", userBefore);

        assertAssignees(ROLE_GOVERNOR_OID, 0);

        // WHEN
        assignRole(USER_ELAINE_OID, ROLE_GOVERNOR_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_ELAINE_OID);
        display("User after", userAfter);
        assertAssignedRole(userAfter, ROLE_GOVERNOR_OID);
        assertRoleMembershipRef(userAfter, ROLE_GOVERNOR_OID);
        assertDelegatedRef(userAfter);

        assertAssignedRole(USER_ELAINE_OID, ROLE_GOVERNOR_OID, result);
        assertDefaultDummyAccount(ACCOUNT_ELAINE_DUMMY_USERNAME, ACCOUNT_ELAINE_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_ELAINE_DUMMY_USERNAME, "title", "Her Excellency Governor");

        assertAssignees(ROLE_GOVERNOR_OID, 1);
    }

    /**
     * Governor has maxAssignees=1
     */
    @Test
    public void test612JackAssignRoleGovernor() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        try {
            // WHEN
            assignRole(USER_JACK_OID, ROLE_GOVERNOR_OID, getDefaultOptions(), task, result);

            AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
            displayExpectedException(e);
            assertMessage(e, "Role \"Governor\" requires at most 1 assignees with the relation of \"default\". The operation would result in 2 assignees.");
        }

        // THEN
        then();
        assertFailure(result);

        assertNoAssignments(USER_JACK_OID);

        assertAssignees(ROLE_GOVERNOR_OID, 1);
    }

    /**
     * Governor has maxAssignees=0 for 'approver'
     */
    @Test
    public void test613JackAssignRoleGovernorAsApprover() throws Exception {
        if (!testMultiplicityConstraintsForNonDefaultRelations()) {
            return;
        }

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        try {
            // WHEN
            assignRole(USER_JACK_OID, ROLE_GOVERNOR_OID, SchemaConstants.ORG_APPROVER, getDefaultOptions(), task, result);

            AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
            displayExpectedException(e);
            assertMessage(e, "Role \"Governor\" requires at most 0 assignees with the relation of \"approver\". The operation would result in 1 assignees.");
        }

        // THEN
        then();
        assertFailure(result);

        assertNoAssignments(USER_JACK_OID);

        assertAssignees(ROLE_GOVERNOR_OID, 1);
    }

    /**
     * Role cannibal has minAssignees=2. It is assigned to nobody. Even though assigning
     * it to lemonhead would result in assignees=1 which violates the policy, the assignment
     * should pass because it makes the situation better.
     */
    @Test
    public void test620LemonheadAssignRoleCanibal() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> user = createUser(USER_LEMONHEAD_NAME, USER_LEMONHEAD_FULLNAME, true);
        addObject(user);

        assertAssignees(ROLE_CANNIBAL_OID, 0);

        // WHEN
        when();
        assignRole(user.getOid(), ROLE_CANNIBAL_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        assertSuccess(result);

        assertAssignedRole(user.getOid(), ROLE_CANNIBAL_OID, result);
        assertDefaultDummyAccount(USER_LEMONHEAD_NAME, USER_LEMONHEAD_FULLNAME, true);
        assertDefaultDummyAccountAttribute(USER_LEMONHEAD_NAME, "title", "Voracious Cannibal");

        assertAssignees(ROLE_CANNIBAL_OID, 1);
        assertAssignees(ROLE_GOVERNOR_OID, 1);
    }

    @Test
    public void test622SharptoothAssignRoleCanibal() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> user = createUser(USER_SHARPTOOTH_NAME, USER_SHARPTOOTH_FULLNAME, true);
        addObject(user);
        userSharptoothOid = user.getOid();

        assertAssignees(ROLE_CANNIBAL_OID, 1);

        // WHEN
        when();
        assignRole(user.getOid(), ROLE_CANNIBAL_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        assertSuccess(result);

        assertAssignedRole(user.getOid(), ROLE_CANNIBAL_OID, result);
        assertDefaultDummyAccount(USER_SHARPTOOTH_NAME, USER_SHARPTOOTH_FULLNAME, true);
        assertDefaultDummyAccountAttribute(USER_SHARPTOOTH_NAME, "title", "Voracious Cannibal");

        assertAssignees(ROLE_CANNIBAL_OID, 2);
        assertAssignees(ROLE_GOVERNOR_OID, 1);
    }

    @Test
    public void test624RedskullAssignRoleCanibal() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> user = createUser(USER_REDSKULL_NAME, USER_REDSKULL_FULLNAME, true);
        addObject(user);
        userRedskullOid = user.getOid();

        assertAssignees(ROLE_CANNIBAL_OID, 2);

        // WHEN
        when();
        assignRole(user.getOid(), ROLE_CANNIBAL_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        assertSuccess(result);

        assertAssignedRole(user.getOid(), ROLE_CANNIBAL_OID, result);
        assertDefaultDummyAccount(USER_REDSKULL_NAME, USER_REDSKULL_FULLNAME, true);
        assertDefaultDummyAccountAttribute(USER_REDSKULL_NAME, "title", "Voracious Cannibal");

        assertAssignees(ROLE_CANNIBAL_OID, 3);
        assertAssignees(ROLE_GOVERNOR_OID, 1);
    }

    @Test
    public void test625BignoseAssignRoleCanibal() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> user = createUser(USER_BIGNOSE_NAME, USER_BIGNOSE_FULLNAME, true);
        addObject(user);
        userBignoseOid = user.getOid();

        assertAssignees(ROLE_CANNIBAL_OID, 3);

        try {
            // WHEN
            when();
            assignRole(user.getOid(), ROLE_CANNIBAL_OID, getDefaultOptions(), task, result);

            AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
            displayExpectedException(e);
            assertMessage(e, "Role \"Cannibal\" requires at most 3 assignees with the relation of \"default\". The operation would result in 4 assignees.");
        }

        // THEN
        then();
        assertFailure(result);

        assertNoAssignments(user.getOid());

        assertAssignees(ROLE_CANNIBAL_OID, 3);
        assertAssignees(ROLE_GOVERNOR_OID, 1);
    }

    @Test
    public void test627SharptoothUnassignRoleCanibal() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        assertAssignees(ROLE_CANNIBAL_OID, 3);

        // WHEN
        when();
        unassignRole(userSharptoothOid, ROLE_CANNIBAL_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        assertSuccess(result);

        assertNoAssignments(userSharptoothOid);
        assertNoDummyAccount(USER_SHARPTOOTH_NAME);

        assertAssignees(ROLE_CANNIBAL_OID, 2);
        assertAssignees(ROLE_GOVERNOR_OID, 1);
    }

    @Test
    public void test628RedskullUnassignRoleCanibal() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        assertAssignees(ROLE_CANNIBAL_OID, 2);

        try {
            // WHEN
            when();
            unassignRole(userRedskullOid, ROLE_CANNIBAL_OID, getDefaultOptions(), task, result);

            AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
            displayExpectedException(e);
            assertMessage(e, "Role \"Cannibal\" requires at least 2 assignees with the relation of \"default\". The operation would result in 1 assignees.");
        }

        // THEN
        then();
        assertFailure(result);

        assertAssignedRole(userRedskullOid, ROLE_CANNIBAL_OID, result);
        assertDefaultDummyAccount(USER_REDSKULL_NAME, USER_REDSKULL_FULLNAME, true);
        assertDefaultDummyAccountAttribute(USER_REDSKULL_NAME, "title", "Voracious Cannibal");

        assertAssignees(ROLE_CANNIBAL_OID, 2);
        assertAssignees(ROLE_GOVERNOR_OID, 1);
    }

    @Test
    public void test630RappAssignRoleCannibalAsOwner() throws Exception {

        if (!testMultiplicityConstraintsForNonDefaultRelations()) {
            return;
        }

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        assertAssignees(ROLE_CANNIBAL_OID, 2);

        // WHEN
        assignRole(USER_RAPP_OID, ROLE_CANNIBAL_OID, SchemaConstants.ORG_OWNER, getDefaultOptions(), task, result);

        // THEN
        then();
        assertSuccess(result);

        assertAssignees(ROLE_CANNIBAL_OID, 2);
        assertAssignees(ROLE_CANNIBAL_OID, SchemaConstants.ORG_OWNER, 1);
    }

    /**
     * We are going to violate minAssignees constraint in cannibal role.
     */
    @Test
    public void test632RappUnassignRoleCannibalAsOwner() throws Exception {

        if (!testMultiplicityConstraintsForNonDefaultRelations()) {
            return;
        }

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        assertAssignees(ROLE_CANNIBAL_OID, 2);
        assertAssignees(ROLE_CANNIBAL_OID, SchemaConstants.ORG_OWNER, 1);

        try {
            // WHEN
            when();
            // null namespace to test no-namespace "owner" relation
            unassignRole(USER_RAPP_OID, ROLE_CANNIBAL_OID, QNameUtil.nullNamespace(SchemaConstants.ORG_OWNER), getDefaultOptions(), task, result);

            AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
            displayExpectedException(e);
            assertMessage(e, "Role \"Cannibal\" requires at least 1 assignees with the relation of \"owner\". The operation would result in 0 assignees.");
        }

        // THEN
        then();
        assertFailure(result);

        assertAssignees(ROLE_CANNIBAL_OID, 2);
        assertAssignees(ROLE_CANNIBAL_OID, SchemaConstants.ORG_OWNER, 1);
    }

    /**
     * Preparing for MID-3979: adding second owner
     */
    @Test
    public void test634BignoseAssignRoleCannibalAsOwner() throws Exception {

        if (!testMultiplicityConstraintsForNonDefaultRelations()) {
            return;
        }

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        assertAssignees(ROLE_CANNIBAL_OID, 2);
        assertAssignees(ROLE_CANNIBAL_OID, SchemaConstants.ORG_OWNER, 1);

        // WHEN
        assignRole(userBignoseOid, ROLE_CANNIBAL_OID, SchemaConstants.ORG_OWNER, getDefaultOptions(), task, result);

        // THEN
        then();
        assertSuccess(result);

        assertUserAfter(userBignoseOid)
                .assignments()
                .assertAssignments(1)
                .assertRole(ROLE_CANNIBAL_OID, SchemaConstants.ORG_OWNER);

        assertAssignees(ROLE_CANNIBAL_OID, 2);
        assertAssignees(ROLE_CANNIBAL_OID, SchemaConstants.ORG_OWNER, 2);
    }

    /**
     * MID-3979: removing second owner should go well
     */
    @Test
    public void test636BignoseUnassignRoleCannibalAsOwner() throws Exception {

        if (!testMultiplicityConstraintsForNonDefaultRelations()) {
            return;
        }

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        assertAssignees(ROLE_CANNIBAL_OID, 2);
        assertAssignees(ROLE_CANNIBAL_OID, SchemaConstants.ORG_OWNER, 2);

        // WHEN
        unassignRole(userBignoseOid, ROLE_CANNIBAL_OID, SchemaConstants.ORG_OWNER, getDefaultOptions(), task, result);

        // THEN
        then();
        assertSuccess(result);

        assertUserAfter(userBignoseOid)
                .assignments()
                .assertNone();

        assertAssignees(ROLE_CANNIBAL_OID, 2);
        assertAssignees(ROLE_CANNIBAL_OID, SchemaConstants.ORG_OWNER, 1);
    }

    @Test
    public void test649ElaineUnassignRoleGovernor() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_ELAINE_OID);
        display("User before", userBefore);

        assertAssignees(ROLE_GOVERNOR_OID, 1);

        // WHEN
        when();
        unassignRole(USER_ELAINE_OID, ROLE_GOVERNOR_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_ELAINE_OID);
        display("User after", userAfter);
        assertAssignedNoRole(userAfter);

        assertAssignees(ROLE_GOVERNOR_OID, 0);
    }

    @Test
    public void test650BignoseAssignRoleCannibalAsOwner() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        assertUserBefore(userBignoseOid)
                .assignments()
                .assertNone();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        assignRole(userBignoseOid, ROLE_CANNIBAL_OID, SchemaConstants.ORG_OWNER, getDefaultOptions(), task, result);

        // THEN
        then();
        assertSuccess(result);

        assertUserAfter(userBignoseOid)
                .assignments()
                .assertAssignments(1)
                .assertRole(ROLE_CANNIBAL_OID, SchemaConstants.ORG_OWNER);
    }

    @Test
    public void test651BignoseAssignRoleCannibalAsApprover() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        assignRole(userBignoseOid, ROLE_CANNIBAL_OID, SchemaConstants.ORG_APPROVER, getDefaultOptions(), task, result);

        // THEN
        then();
        assertSuccess(result);

        assertUserAfter(userBignoseOid)
                .assignments()
                .assertAssignments(2)
                .assertRole(ROLE_CANNIBAL_OID, SchemaConstants.ORG_OWNER)
                .assertRole(ROLE_CANNIBAL_OID, SchemaConstants.ORG_APPROVER);
    }

    /**
     * MID-4952
     */
    @Test
    public void test655BignoseAssignRoleCannibal() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        assignRole(userBignoseOid, ROLE_CANNIBAL_OID, SchemaConstants.ORG_DEFAULT, getDefaultOptions(), task, result);

        // THEN
        then();
        assertSuccess(result);

        assertUserAfter(userBignoseOid)
                .assignments()
                .assertAssignments(3)
                .assertRole(ROLE_CANNIBAL_OID, SchemaConstants.ORG_DEFAULT)
                .assertRole(ROLE_CANNIBAL_OID, SchemaConstants.ORG_OWNER)
                .assertRole(ROLE_CANNIBAL_OID, SchemaConstants.ORG_APPROVER);
    }

    @Test
    public void test656BignoseUnassignRoleCannibal() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        unassignRole(userBignoseOid, ROLE_CANNIBAL_OID, SchemaConstants.ORG_DEFAULT, getDefaultOptions(), task, result);

        // THEN
        then();
        assertSuccess(result);

        assertUserAfter(userBignoseOid)
                .assignments()
                .assertAssignments(2)
                .assertRole(ROLE_CANNIBAL_OID, SchemaConstants.ORG_OWNER)
                .assertRole(ROLE_CANNIBAL_OID, SchemaConstants.ORG_APPROVER);
    }

    @Test
    public void test658BignoseUnassignRoleCannibalAsOwner() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        Collection<ItemDelta<?, ?>> modifications = new ArrayList<>();
        modifications.add((createAssignmentModification(ROLE_CANNIBAL_OID, RoleType.COMPLEX_TYPE, SchemaConstants.ORG_OWNER, null, null, false)));
        modifications.add((createAssignmentModification(ROLE_CANNIBAL_OID, RoleType.COMPLEX_TYPE, SchemaConstants.ORG_APPROVER, null, null, false)));
        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createModifyDelta(userBignoseOid, modifications, UserType.class);

        // WHEN
        executeChanges(userDelta, getDefaultOptions(), task, result);

        // THEN
        then();
        assertSuccess(result);

        assertUserAfter(userBignoseOid)
                .assignments()
                .assertNone();
    }

    @Test
    public void test700JackAssignRoleJudge() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_JUDGE_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        assertSuccess(result);

        assertAssignedRole(USER_JACK_OID, ROLE_JUDGE_OID, result);
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Honorable Justice");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "mouth", "pistol");
    }

    @Test
    public void test701JackModifyJudgeDeleteConstructionRecompute() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        when();
        modifyRoleDeleteInducementAndRecomputeMembers(ROLE_JUDGE_OID, 1111L, getDefaultOptions(), task);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertInProgressOrSuccess(result);

        assertAssignedRole(USER_JACK_OID, ROLE_JUDGE_OID, result);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    @Test
    public void test702JackModifyJudgeAddInducementHonorabilityRecompute() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        when();
        modifyRoleAddInducementTargetAndRecomputeMembers(ROLE_JUDGE_OID, ROLE_HONORABILITY_OID, getDefaultOptions());

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertInProgressOrSuccess(result);

        assertAssignedRole(USER_JACK_OID, ROLE_JUDGE_OID, result);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "Honorable");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", "mouth", "pistol");

        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "title", "Bloody Honorable");
    }

    @Test
    public void test703JackModifyJudgeDeleteInducementHonorabilityRecompute() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        modifyRoleDeleteInducementTarget(ROLE_JUDGE_OID, ROLE_HONORABILITY_OID, getDefaultOptions());
        PrismObject<RoleType> roleJudge = modelService.getObject(RoleType.class, ROLE_JUDGE_OID, null, task, result);
        display("Role judge", roleJudge);

        // WHEN
        when();
        recomputeUser(USER_JACK_OID, ModelExecuteOptions.create(getDefaultOptions()).reconcile(), task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);

        assertAssignedRole(userAfter, ROLE_JUDGE_OID);
        accountJackRedOid = getSingleLinkOid(userAfter);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);

        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, false);
        assertDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "title", "Bloody Honorable");
    }

    @Test
    public void test709JackUnAssignRoleJudge() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        when();
        unassignRole(USER_JACK_OID, ROLE_JUDGE_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertAssignedNoRole(USER_JACK_OID, result);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    @Test
    public void test710JackAssignRoleEmpty() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_EMPTY_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertAssignedRole(USER_JACK_OID, ROLE_EMPTY_OID, result);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    @Test
    public void test712JackModifyEmptyRoleAddInducementPirateRecompute() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // By default, empty role is really empty. If we want to recompute members, it has to have the following metarole.
        assignRole(RoleType.class, ROLE_EMPTY_OID, METAROLE_RECOMPUTE_MEMBERS.oid, task, result);

        // WHEN
        when();
        modifyRoleAddInducementTargetAndRecomputeMembers(ROLE_EMPTY_OID, ROLE_PIRATE_OID, getDefaultOptions());

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertInProgressOrSuccess(result);

        assertAssignedRole(USER_JACK_OID, ROLE_EMPTY_OID, result);
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", ROLE_PIRATE_TITLE);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "weapon", ROLE_PIRATE_WEAPON);

        // Let's clean up things.
        unassignRole(RoleType.class, ROLE_EMPTY_OID, METAROLE_RECOMPUTE_MEMBERS.oid, task, result);
    }

    @Test
    public void test714JackModifyEmptyRoleDeleteInducementPirateRecompute() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        modifyRoleDeleteInducementTarget(ROLE_EMPTY_OID, ROLE_PIRATE_OID, getDefaultOptions());

        // WHEN
        when();
        recomputeUser(USER_JACK_OID, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertAssignedRole(USER_JACK_OID, ROLE_EMPTY_OID, result);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    @Test
    public void test719JackUnAssignRoleEmpty() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        when();
        unassignRole(USER_JACK_OID, ROLE_EMPTY_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertAssignedNoRole(USER_JACK_OID, result);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    @Test
    public void test720JackAssignRoleGovernorTenantRef() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        assertAssignees(ROLE_GOVERNOR_OID, 0);

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        when();
        assignParametricRole(USER_JACK_OID, ROLE_GOVERNOR_OID, null, ORG_SCUMM_BAR_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertAssignedRole(USER_JACK_OID, ROLE_GOVERNOR_OID, result);
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Her Excellency Governor of Scumm Bar");

        assertAssignees(ROLE_GOVERNOR_OID, 1);
    }

    @Test
    public void test729JackUnassignRoleGovernorTenantRef() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        assertAssignees(ROLE_GOVERNOR_OID, 1);

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        when();
        unassignParametricRole(USER_JACK_OID, ROLE_GOVERNOR_OID, null, ORG_SCUMM_BAR_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertAssignedNoRole(USER_JACK_OID, result);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);

        assertAssignees(ROLE_GOVERNOR_OID, 0);
    }

    /**
     * MID-3365
     */
    @Test
    public void test750JackAssignRoleOmnimanager() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_PROJECT_OMNINAMAGER_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignedRole(userAfter, ROLE_PROJECT_OMNINAMAGER_OID);

        assertHasOrg(userAfter, ORG_SAVE_ELAINE_OID, SchemaConstants.ORG_MANAGER);
        assertHasOrg(userAfter, ORG_KIDNAP_AND_MARRY_ELAINE_OID, SchemaConstants.ORG_MANAGER);
    }

    /**
     * MID-3365
     */
    @Test
    public void test755AddProjectAndRecomputeJack() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        assertAssignedRole(userBefore, ROLE_PROJECT_OMNINAMAGER_OID);

        addObject(ORG_PROJECT_RECLAIM_BLACK_PEARL_FILE);

        // WHEN
        when();
        recomputeUser(USER_JACK_OID, ModelExecuteOptions.create(getDefaultOptions()).reconcile(), task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignedRole(userAfter, ROLE_PROJECT_OMNINAMAGER_OID);

        assertHasOrg(userAfter, ORG_SAVE_ELAINE_OID, SchemaConstants.ORG_MANAGER);
        assertHasOrg(userAfter, ORG_KIDNAP_AND_MARRY_ELAINE_OID, SchemaConstants.ORG_MANAGER);
        assertHasOrg(userAfter, ORG_PROJECT_RECLAIM_BLACK_PEARL_OID, SchemaConstants.ORG_MANAGER);
    }

    /**
     * MID-3365
     */
    @Test
    public void test759JackUnassignRoleOmnimanager() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        when();
        unassignRole(USER_JACK_OID, ROLE_PROJECT_OMNINAMAGER_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertNotAssignedRole(userAfter, ROLE_PROJECT_OMNINAMAGER_OID);

        assertHasNoOrg(userAfter);
    }

    /**
     * Assign role with weak construction. Nothing should happen (no account).
     * MID-2850
     */
    @Test
    public void test760JackAssignRoleWeakGossiper() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        assertAssignedNoRole(userBefore);
        assertLiveLinks(userBefore, 1);
        assertLinked(userBefore, accountJackRedOid);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, false);

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_WEAK_GOSSIPER_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignedRole(userAfter, ROLE_WEAK_GOSSIPER_OID);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    /**
     * MID-2850
     */
    @Test
    public void test761JackRecompute() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        assertAssignedRole(userBefore, ROLE_WEAK_GOSSIPER_OID);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);

        // WHEN
        when();
        recomputeUser(USER_JACK_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignedRole(userAfter, ROLE_WEAK_GOSSIPER_OID);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    /**
     * MID-2850
     */
    @Test
    public void test762JackReconcile() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        assertAssignedRole(userBefore, ROLE_WEAK_GOSSIPER_OID);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);

        // WHEN
        when();
        reconcileUser(USER_JACK_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignedRole(userAfter, ROLE_WEAK_GOSSIPER_OID);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    /**
     * MID-2850, MID-4119
     */
    @Test
    public void test763PreviewChanges() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        assertAssignedRole(userBefore, ROLE_WEAK_GOSSIPER_OID);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);

        ObjectDelta<UserType> delta = prismContext.deltaFactory().object().createEmptyModifyDelta(UserType.class, USER_JACK_OID
        );

        // WHEN
        when();
        // The following options provide the pre-4.9-like behavior of previewChanges()
        // (for unknown reason we don't use default options here)
        var options = ModelExecuteOptions.create()
                .firstClickOnly()
                .previewPolicyRulesEnforcement();
        ModelContext<ObjectType> context = modelInteractionService.previewChanges(List.of(delta), options, task, result);

        // THEN
        then();
        assertSuccess(result);

        displayDumpable("Preview context", context);
        assertNotNull("Null focus context", context.getFocusContext());
        assertEquals("Wrong number of projection contexts", 1, context.getProjectionContexts().size());
        DeltaSetTriple<? extends EvaluatedAssignment> evaluatedAssignmentTriple = context.getEvaluatedAssignmentTriple();
        assertNotNull("null evaluatedAssignmentTriple", evaluatedAssignmentTriple);
        assertTrue("Unexpected plus set in evaluatedAssignmentTriple", evaluatedAssignmentTriple.getPlusSet().isEmpty());
        assertTrue("Unexpected minus set in evaluatedAssignmentTriple", evaluatedAssignmentTriple.getMinusSet().isEmpty());
        Collection<? extends EvaluatedAssignment> assignmentZeroSet = evaluatedAssignmentTriple.getZeroSet();
        assertNotNull("null zero set in evaluatedAssignmentTriple", assignmentZeroSet);
        assertEquals("Wrong size of zero set in evaluatedAssignmentTriple", 1, assignmentZeroSet.size());
        EvaluatedAssignment evaluatedAssignment = assignmentZeroSet.iterator().next();
        displayDumpable("Evaluated weak assignment", evaluatedAssignment);

        DeltaSetTriple<EvaluatedResourceObjectConstruction> evaluatedConstructions = evaluatedAssignment.getEvaluatedConstructions(task, result);
        assertTrue("Unexpected plus set in evaluatedConstructions", evaluatedConstructions.getPlusSet().isEmpty());
        assertTrue("Unexpected minus set in evaluatedConstructions", evaluatedConstructions.getMinusSet().isEmpty());
        Collection<EvaluatedResourceObjectConstruction> constructionsZeroSet = evaluatedConstructions.getZeroSet();
        assertEquals("Wrong size of zero set in evaluatedConstructions", 1, constructionsZeroSet.size());
        EvaluatedResourceObjectConstruction evaluatedConstruction = constructionsZeroSet.iterator().next();
        displayDumpable("Evaluated weak evaluatedConstruction", evaluatedConstruction);
        assertTrue("Construction not weak", evaluatedConstruction.isWeak());

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignedRole(userAfter, ROLE_WEAK_GOSSIPER_OID);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    /**
     * Assign role with normal construction. Account should be created.
     * Both the normal and the weak construction should be applied.
     * MID-2850
     */
    @Test
    public void test764JackAssignRoleSailor() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        assertAssignedRole(userBefore, ROLE_WEAK_GOSSIPER_OID);

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_SAILOR_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignedRole(userAfter, ROLE_WEAK_GOSSIPER_OID);
        assertAssignedRole(userAfter, ROLE_SAILOR_OID);

        assertDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK, ROLE_SAILOR_DRINK);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, "Pssst! hear this: dead men tell no tales");

    }

    /**
     * MID-2850
     */
    @Test
    public void test765JackRecompute() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        when();
        recomputeUser(USER_JACK_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignedRole(userAfter, ROLE_WEAK_GOSSIPER_OID);
        assertAssignedRole(userAfter, ROLE_SAILOR_OID);

        assertDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK, ROLE_SAILOR_DRINK);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, "Pssst! hear this: dead men tell no tales");

    }

    /**
     * MID-2850
     */
    @Test
    public void test766JackReconcile() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        when();
        reconcileUser(USER_JACK_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignedRole(userAfter, ROLE_WEAK_GOSSIPER_OID);
        assertAssignedRole(userAfter, ROLE_SAILOR_OID);

        assertDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK, ROLE_SAILOR_DRINK);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, "Pssst! hear this: dead men tell no tales");

    }

    /**
     * Unassign role with weak construction. The values given by this construction
     * should be removed, but the other values should remain.
     * MID-2850
     */
    @Test
    public void test767JackUnAssignRoleWeakGossiper() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        when();
        unassignRole(USER_JACK_OID, ROLE_WEAK_GOSSIPER_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignedRole(userAfter, ROLE_SAILOR_OID);

        assertDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK, ROLE_SAILOR_DRINK);
        assertNoDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME);

    }

    /**
     * MID-2850
     */
    @Test
    public void test768JackRecompute() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        when();
        recomputeUser(USER_JACK_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignedRole(userAfter, ROLE_SAILOR_OID);

        assertDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK, ROLE_SAILOR_DRINK);
        assertNoDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME);

    }

    /**
     * MID-2850
     */
    @Test
    public void test769JackUnAssignRoleSailor() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        when();
        unassignRole(USER_JACK_OID, ROLE_SAILOR_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignedNoRole(userAfter);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);

    }

    /**
     * Now assign the normal role first.
     * MID-2850
     */
    @Test
    public void test770JackAssignRoleSailor() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_SAILOR_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignedRole(userAfter, ROLE_SAILOR_OID);

        assertDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK, ROLE_SAILOR_DRINK);
        assertNoDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME);

    }

    /**
     * Assign role with weak construction. It should be activated in a
     * usual way.
     * MID-2850
     */
    @Test
    public void test772JackAssignRoleGossiper() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_WEAK_GOSSIPER_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignedRole(userAfter, ROLE_WEAK_GOSSIPER_OID);
        assertAssignedRole(userAfter, ROLE_SAILOR_OID);

        assertDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK, ROLE_SAILOR_DRINK);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, "Pssst! hear this: dead men tell no tales");

    }

    /**
     * Unassign normal role. Even though the role with weak construction remains,
     * it should not be applied. The account should be gone.
     * MID-2850
     */
    @Test
    public void test774JackUnAssignRoleSailor() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        when();
        unassignRole(USER_JACK_OID, ROLE_SAILOR_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignedRole(userAfter, ROLE_WEAK_GOSSIPER_OID);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);

    }

    /**
     * Unassign role with weak construction. Nothing should really happen.
     * MID-2850
     */
    @Test
    public void test775JackUnAssignRoleGossiper() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        when();
        unassignRole(USER_JACK_OID, ROLE_WEAK_GOSSIPER_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignedNoRole(userAfter);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);

    }

    /**
     * Assign both roles together (weak and normal).
     * MID-2850
     */
    @Test
    public void test778JackAssignRoleGossiperAndSailor() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        ObjectDelta<UserType> userDelta = createAssignmentUserDelta(USER_JACK_OID, ROLE_WEAK_GOSSIPER_OID, RoleType.COMPLEX_TYPE,
                null, null, null, true);
        userDelta.addModification(createAssignmentModification(ROLE_SAILOR_OID, RoleType.COMPLEX_TYPE,
                null, null, null, true));
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        // WHEN
        when();
        modelService.executeChanges(deltas, getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignedRole(userAfter, ROLE_WEAK_GOSSIPER_OID);
        assertAssignedRole(userAfter, ROLE_SAILOR_OID);

        assertDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK, ROLE_SAILOR_DRINK);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, "Pssst! hear this: dead men tell no tales");

    }

    /**
     * Unassign both roles together (weak and normal).
     * MID-2850
     */
    @Test
    public void test779JackUnassignRoleGossiperAndSailor() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        ObjectDelta<UserType> userDelta = createAssignmentUserDelta(USER_JACK_OID, ROLE_WEAK_GOSSIPER_OID, RoleType.COMPLEX_TYPE,
                null, null, null, false);
        userDelta.addModification(createAssignmentModification(ROLE_SAILOR_OID, RoleType.COMPLEX_TYPE,
                null, null, null, false));
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        // WHEN
        when();
        modelService.executeChanges(deltas, getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignedNoRole(userAfter);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);

    }

    /**
     * Assign role with weak construction. Nothing should happen (no account).
     * MID-2850, MID-3662
     */
    @Test
    public void test780JackAssignRoleWeakSinger() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        assertAssignedNoRole(userBefore);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_WEAK_SINGER_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignedRole(userAfter, ROLE_WEAK_SINGER_OID);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    /**
     * Assign another role with weak construction. Still nothing
     * should happen (no account).
     * MID-2850, MID-3662
     */
    @Test
    public void test781JackAssignRoleWeakGossiper() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        assertAssignedRole(userBefore, ROLE_WEAK_SINGER_OID);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_WEAK_GOSSIPER_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignedRoles(userAfter, ROLE_WEAK_GOSSIPER_OID, ROLE_WEAK_SINGER_OID);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    /**
     * Assign role with normal construction. Account should be created.
     * Both the normal and both weak constructions should be applied.
     * MID-2850, MID-3662
     */
    @Test
    public void test782JackAssignRoleSailor() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        assertAssignedRole(userBefore, ROLE_WEAK_GOSSIPER_OID);

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_SAILOR_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignedRoles(userAfter, ROLE_WEAK_GOSSIPER_OID, ROLE_WEAK_SINGER_OID, ROLE_SAILOR_OID);

        assertDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK, ROLE_SAILOR_DRINK);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, "Pssst! hear this: dead men tell no tales");
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, ROLE_WEAK_SINGER_TITLE);

    }

    /**
     * Unassign role with normal construction. The account should be gone,
     * the two weak roles should not be applied.
     * MID-2850, MID-3662
     */
    @Test
    public void test783JackUnassignRoleSailor() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        assertAssignedRole(userBefore, ROLE_WEAK_SINGER_OID);

        // WHEN
        when();
        unassignRole(USER_JACK_OID, ROLE_SAILOR_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignedRoles(userAfter, ROLE_WEAK_GOSSIPER_OID, ROLE_WEAK_SINGER_OID);
        assertNotAssignedRole(userAfter, ROLE_SAILOR_OID);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    /**
     * MID-2850, MID-3662
     */
    @Test
    public void test784JackUnAssignRoleWeakSinger() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);

        // WHEN
        when();
        unassignRole(USER_JACK_OID, ROLE_WEAK_SINGER_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignedRoles(userAfter, ROLE_WEAK_GOSSIPER_OID);
        assertNotAssignedRole(userAfter, ROLE_SAILOR_OID);
        assertNotAssignedRole(userAfter, ROLE_WEAK_SINGER_OID);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    /**
     * MID-2850, MID-3662
     */
    @Test
    public void test785JackUnAssignRoleGossiper() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        when();
        unassignRole(USER_JACK_OID, ROLE_WEAK_GOSSIPER_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignedNoRole(userAfter);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);

    }

    /**
     * Assign both roles with weak construction together. Nothing should happen.
     * MID-2850, MID-3662
     */
    @Test
    public void test786JackAssignRoleGossiperAndSinger() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        ObjectDelta<UserType> userDelta = createAssignmentUserDelta(USER_JACK_OID, ROLE_WEAK_GOSSIPER_OID, RoleType.COMPLEX_TYPE,
                null, null, null, true);
        userDelta.addModification(createAssignmentModification(ROLE_WEAK_SINGER_OID, RoleType.COMPLEX_TYPE,
                null, null, null, true));
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        // WHEN
        when();
        modelService.executeChanges(deltas, getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignedRoles(userAfter, ROLE_WEAK_GOSSIPER_OID, ROLE_WEAK_SINGER_OID);
        assertNotAssignedRole(userAfter, ROLE_SAILOR_OID);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    /**
     * Unassign both roles with weak construction together. Nothing should happen.
     * MID-2850, MID-3662
     */
    @Test
    public void test788JackUnassignRoleGossiperAndSinger() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        ObjectDelta<UserType> userDelta = createAssignmentUserDelta(USER_JACK_OID, ROLE_WEAK_GOSSIPER_OID, RoleType.COMPLEX_TYPE,
                null, null, null, false);
        userDelta.addModification(createAssignmentModification(ROLE_WEAK_SINGER_OID, RoleType.COMPLEX_TYPE,
                null, null, null, false));
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        // WHEN
        when();
        modelService.executeChanges(deltas, getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignedNoRole(userAfter);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    /**
     * Assign role with weak construction. Nothing should happen (no account).
     * Preparation for following tests.
     * MID-2850, MID-3662
     */
    @Test
    public void test790JackAssignRoleWeakSinger() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        assertAssignedNoRole(userBefore);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_WEAK_SINGER_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignedRole(userAfter, ROLE_WEAK_SINGER_OID);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    /**
     * Switch: weak -> weak (strong absent)
     * Switch one role with weak construction for another role with weak
     * construction (in one operation). Still nothing should happen.
     * This is the test that really reproduces MID-3662.
     * MID-2850, MID-3662
     */
    @Test
    public void test791JackSwitchRolesGossiperAndSinger() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);

        ObjectDelta<UserType> userDelta = createAssignmentUserDelta(USER_JACK_OID, ROLE_WEAK_GOSSIPER_OID, RoleType.COMPLEX_TYPE,
                null, null, null, true);
        userDelta.addModification(createAssignmentModification(ROLE_WEAK_SINGER_OID, RoleType.COMPLEX_TYPE,
                null, null, null, false));
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        // WHEN
        when();
        modelService.executeChanges(deltas, getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignedRoles(userAfter, ROLE_WEAK_GOSSIPER_OID);
        assertNotAssignedRole(userAfter, ROLE_SAILOR_OID);
        assertNotAssignedRole(userAfter, ROLE_WEAK_SINGER_OID);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    /**
     * MID-2850, MID-3662
     */
    @Test
    public void test792JackAssignRoleSailor() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        assertAssignedRole(userBefore, ROLE_WEAK_GOSSIPER_OID);

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_SAILOR_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignedRoles(userAfter, ROLE_WEAK_GOSSIPER_OID, ROLE_SAILOR_OID);
        assertNotAssignedRole(userAfter, ROLE_WEAK_SINGER_OID);

        assertDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK, ROLE_SAILOR_DRINK);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, "Pssst! hear this: dead men tell no tales");

    }

    /**
     * Switch: weak -> weak (strong present)
     * Switch one role with weak construction for another role with weak
     * construction (in one operation). There is also strong construction.
     * Therefore the account should remain, just the attributes should be
     * changed.
     * MID-2850, MID-3662
     */
    @Test
    public void test793JackSwitchRolesSingerAndGossiper() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        ObjectDelta<UserType> userDelta = createAssignmentUserDelta(USER_JACK_OID, ROLE_WEAK_GOSSIPER_OID, RoleType.COMPLEX_TYPE,
                null, null, null, false);
        userDelta.addModification(createAssignmentModification(ROLE_WEAK_SINGER_OID, RoleType.COMPLEX_TYPE,
                null, null, null, true));
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        // WHEN
        when();
        modelService.executeChanges(deltas, getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignedRoles(userAfter, ROLE_WEAK_SINGER_OID, ROLE_SAILOR_OID);
        assertNotAssignedRole(userAfter, ROLE_WEAK_GOSSIPER_OID);

        assertDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK, ROLE_SAILOR_DRINK);
        assertNoDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, ROLE_WEAK_SINGER_TITLE);
    }

    /**
     * Switch: strong -> weak
     * MID-2850, MID-3662
     */
    @Test
    public void test794JackSwitchRolesSailorAndGossiper() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        ObjectDelta<UserType> userDelta = createAssignmentUserDelta(USER_JACK_OID, ROLE_WEAK_GOSSIPER_OID, RoleType.COMPLEX_TYPE,
                null, null, null, true);
        userDelta.addModification(createAssignmentModification(ROLE_SAILOR_OID, RoleType.COMPLEX_TYPE,
                null, null, null, false));
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        // WHEN
        when();
        modelService.executeChanges(deltas, getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignedRoles(userAfter, ROLE_WEAK_SINGER_OID, ROLE_WEAK_GOSSIPER_OID);
        assertNotAssignedRole(userAfter, ROLE_SAILOR_OID);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    /**
     * Switch: weak -> strong
     * MID-2850, MID-3662
     */
    @Test
    public void test795JackSwitchRolesSingerAndSailor() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        ObjectDelta<UserType> userDelta = createAssignmentUserDelta(USER_JACK_OID, ROLE_WEAK_SINGER_OID, RoleType.COMPLEX_TYPE,
                null, null, null, false);
        userDelta.addModification(createAssignmentModification(ROLE_SAILOR_OID, RoleType.COMPLEX_TYPE,
                null, null, null, true));
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        // WHEN
        when();
        modelService.executeChanges(deltas, getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignedRoles(userAfter, ROLE_WEAK_GOSSIPER_OID, ROLE_SAILOR_OID);
        assertNotAssignedRole(userAfter, ROLE_WEAK_SINGER_OID);

        assertDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK, ROLE_SAILOR_DRINK);
        assertNoDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, "Pssst! hear this: dead men tell no tales");
    }

    /**
     * Switch: strong -> strong (weak present)
     * MID-2850, MID-3662
     */
    @Test
    public void test796JackSwitchRolesSailorAndGovernor() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        ObjectDelta<UserType> userDelta = createAssignmentUserDelta(USER_JACK_OID, ROLE_SAILOR_OID, RoleType.COMPLEX_TYPE,
                null, null, null, false);
        userDelta.addModification(createAssignmentModification(ROLE_GOVERNOR_OID, RoleType.COMPLEX_TYPE,
                null, null, null, true));
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        // WHEN
        when();
        modelService.executeChanges(deltas, getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignedRoles(userAfter, ROLE_WEAK_GOSSIPER_OID, ROLE_GOVERNOR_OID);
        assertNotAssignedRole(userAfter, ROLE_WEAK_SINGER_OID);
        assertNotAssignedRole(userAfter, ROLE_SAILOR_OID);

        assertDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Her Excellency Governor");
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, "Pssst! hear this: dead men tell no tales");
    }

    /**
     * Cleanup
     * MID-2850, MID-3662
     */
    @Test
    public void test799JackUnassignGovernorAndWeakGossiper() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        ObjectDelta<UserType> userDelta = createAssignmentUserDelta(USER_JACK_OID, ROLE_WEAK_GOSSIPER_OID, RoleType.COMPLEX_TYPE,
                null, null, null, false);
        userDelta.addModification(createAssignmentModification(ROLE_GOVERNOR_OID, RoleType.COMPLEX_TYPE,
                null, null, null, false));
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        // WHEN
        when();
        modelService.executeChanges(deltas, getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignedNoRole(userAfter);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    @Test
    public void test800ModifyRoleImmutable() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        try {
            // WHEN
            when();
            modifyObjectReplaceProperty(RoleType.class, ROLE_IMMUTABLE_OID, RoleType.F_DESCRIPTION,
                    getDefaultOptions(), task, result, "whatever");

            AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
            // THEN
            then();
            result.computeStatus();
            TestUtil.assertFailure(result);
        }

        PrismObject<RoleType> roleAfter = getObject(RoleType.class, ROLE_IMMUTABLE_OID);
        PrismAsserts.assertPropertyValue(roleAfter, RoleType.F_DESCRIPTION, ROLE_IMMUTABLE_DESCRIPTION);
    }

    /**
     * This should go well. The global immutable role has enforced modification,
     * but not addition.
     */
    @Test
    public void test802AddGlobalImmutableRole() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<RoleType> role = PrismTestUtil.parseObject(ROLE_IMMUTABLE_GLOBAL_FILE);
        display("Role before", role);

        // WHEN
        when();
        addObject(role, getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<RoleType> roleAfter = getObject(RoleType.class, ROLE_IMMUTABLE_GLOBAL_OID);
        display("Role before", roleAfter);
        assertNotNull("No role added", roleAfter);
    }

    @Test
    public void test804ModifyRoleImmutableGlobalIdentifier() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        try {
            // WHEN
            when();
            modifyObjectReplaceProperty(RoleType.class, ROLE_IMMUTABLE_GLOBAL_OID, RoleType.F_IDENTIFIER,
                    getDefaultOptions(), task, result, "whatever");

            AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
            // THEN
            then();
            result.computeStatus();
            TestUtil.assertFailure(result);
        }

        PrismObject<RoleType> roleAfter = getObject(RoleType.class, ROLE_IMMUTABLE_GLOBAL_OID);
        PrismAsserts.assertPropertyValue(roleAfter, RoleType.F_DESCRIPTION, ROLE_IMMUTABLE_GLOBAL_DESCRIPTION);
        PrismAsserts.assertPropertyValue(roleAfter, RoleType.F_IDENTIFIER, ROLE_IMMUTABLE_GLOBAL_IDENTIFIER);
    }

    @Test
    public void test805ModifyRoleImmutableGlobalDescription() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        try {
            // WHEN
            when();
            modifyObjectReplaceProperty(RoleType.class, ROLE_IMMUTABLE_GLOBAL_OID, RoleType.F_DESCRIPTION,
                    getDefaultOptions(), task, result, "whatever");

            AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
            // THEN
            then();
            result.computeStatus();
            TestUtil.assertFailure(result);
        }

        PrismObject<RoleType> roleAfter = getObject(RoleType.class, ROLE_IMMUTABLE_GLOBAL_OID);
        PrismAsserts.assertPropertyValue(roleAfter, RoleType.F_DESCRIPTION, ROLE_IMMUTABLE_GLOBAL_DESCRIPTION);
        PrismAsserts.assertPropertyValue(roleAfter, RoleType.F_IDENTIFIER, ROLE_IMMUTABLE_GLOBAL_IDENTIFIER);
    }

    /**
     * This should go well. The global immutable role has enforced modification,
     * but not addition.
     */
    @Test
    public void test812AddGlobalImmutableDescriptionRole() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<RoleType> role = PrismTestUtil.parseObject(ROLE_IMMUTABLE_DESCRIPTION_GLOBAL_FILE);
        display("Role before", role);

        // WHEN
        when();
        addObject(role, getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<RoleType> roleAfter = getObject(RoleType.class, ROLE_IMMUTABLE_DESCRIPTION_GLOBAL_OID);
        display("Role after", roleAfter);
        assertNotNull("No role added", roleAfter);
    }

    /**
     * This should go well again. The constraint is related to modification of description, not identifier.
     */
    @Test
    public void test814ModifyRoleImmutableDescriptionGlobalIdentifier() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        final String NEW_VALUE = "whatever";
        modifyObjectReplaceProperty(RoleType.class, ROLE_IMMUTABLE_DESCRIPTION_GLOBAL_OID, RoleType.F_IDENTIFIER,
                getDefaultOptions(), task, result, NEW_VALUE);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<RoleType> roleAfter = getObject(RoleType.class, ROLE_IMMUTABLE_DESCRIPTION_GLOBAL_OID);
        display("Role after", roleAfter);
        assertEquals("Wrong new identifier value", NEW_VALUE, roleAfter.asObjectable().getIdentifier());
    }

    @Test
    public void test815ModifyRoleImmutableGlobalDescription() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        try {
            // WHEN
            when();
            modifyObjectReplaceProperty(RoleType.class, ROLE_IMMUTABLE_DESCRIPTION_GLOBAL_OID, RoleType.F_DESCRIPTION,
                    getDefaultOptions(), task, result, "whatever");

            AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
            // THEN
            then();
            result.computeStatus();
            TestUtil.assertFailure(result);
        }

        PrismObject<RoleType> roleAfter = getObject(RoleType.class, ROLE_IMMUTABLE_DESCRIPTION_GLOBAL_OID);
        PrismAsserts.assertPropertyValue(roleAfter, RoleType.F_DESCRIPTION, ROLE_IMMUTABLE_DESCRIPTION_GLOBAL_DESCRIPTION);
    }

    @Test
    public void test820ModifyRoleImmutableGlobalAddExtension() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("a role with unmodifiable 'sea' property");
        repoAdd(ROLE_IMMUTABLE_SEA_GLOBAL, result);

        try {
            when("a container with 'sea' property is added");
            PrismContainerValue<?> extensionContainerValue =
                    prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(RoleType.class)
                            .findContainerDefinition(RoleType.F_EXTENSION)
                            .instantiate()
                            .getValue();
            extensionContainerValue.createProperty(EXT_SEA)
                    .setRealValue("Caribbean");
            executeChanges(
                    prismContext.deltaFor(RoleType.class)
                            .item(RoleType.F_EXTENSION)
                            .add(extensionContainerValue.clone())
                            .asObjectDelta(ROLE_IMMUTABLE_SEA_GLOBAL.oid),
                    null, task, result);

            AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
            then("failure should occur");
            displayExpectedException(e);
            result.computeStatus();
            TestUtil.assertFailure(result);
        }

        assertRoleAfter(ROLE_IMMUTABLE_SEA_GLOBAL.oid)
                .assertNoItem(RoleType.F_EXTENSION);
    }

    @Test
    public void test826AddNonCreateableRole() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<RoleType> role = PrismTestUtil.parseObject(ROLE_NON_CREATEABLE_FILE);
        display("Role before", role);

        try {
            // WHEN
            when();
            addObject(role, getDefaultOptions(), task, result);

            AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
            // THEN
            then();
            result.computeStatus();
            TestUtil.assertFailure(result);
        }

        assertNoObject(RoleType.class, ROLE_NON_CREATEABLE_OID);
    }

    @Test
    public void test826bAddCreateableRole() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<RoleType> role = PrismTestUtil.parseObject(ROLE_CREATEABLE_FILE);
        display("Role before", role);

        // WHEN
        when();
        addObject(role, getDefaultOptions(), task, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNotNull("object does not exist", getObject(RoleType.class, ROLE_CREATEABLE_OID));     // would get exception anyway
    }

    /**
     * This role has a metarole which has immutable policy rule in the
     * inducement.
     */
    @Test
    public void test827AddImmutableAssignRole() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<RoleType> role = PrismTestUtil.parseObject(ROLE_IMMUTABLE_ASSIGN_FILE);
        display("Role before", role);

        try {
            // WHEN
            when();
            addObject(role, getDefaultOptions(), task, result);

            AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
            // THEN
            then();
            result.computeStatus();
            TestUtil.assertFailure(result);
        }

        assertNoObject(RoleType.class, ROLE_IMMUTABLE_ASSIGN_OID);
    }

    /**
     * The untouchable metarole has immutable policy rule in the
     * inducement. So it will apply to member roles, but not to the
     * metarole itself. Try if we can modify the metarole.
     */
    @Test
    public void test828ModifyUntouchableMetarole() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        modifyObjectReplaceProperty(RoleType.class, ROLE_META_UNTOUCHABLE_OID, RoleType.F_DESCRIPTION,
                getDefaultOptions(), task, result, "Touche!");

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<RoleType> roleAfter = getObject(RoleType.class, ROLE_META_UNTOUCHABLE_OID);
        PrismAsserts.assertPropertyValue(roleAfter, RoleType.F_DESCRIPTION, "Touche!");
    }

    @Test
    public void test830ModifyRoleJudge() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        modifyObjectReplaceProperty(RoleType.class, ROLE_JUDGE_OID, RoleType.F_DESCRIPTION,
                getDefaultOptions(), task, result, "whatever");

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<RoleType> roleAfter = getObject(RoleType.class, ROLE_JUDGE_OID);
        PrismAsserts.assertPropertyValue(roleAfter, RoleType.F_DESCRIPTION, "whatever");
    }

    @Test
    public void test840AssignRoleNonAssignable() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userJackBefore = getUser(USER_JACK_OID);
        display("user jack", userJackBefore);
        assertNoAssignments(userJackBefore);

        try {
            // WHEN
            when();
            assignRole(USER_JACK_OID, ROLE_NON_ASSIGNABLE_OID, getDefaultOptions(), task, result);

            AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
            // THEN
            then();
            result.computeStatus();
            TestUtil.assertFailure(result);
        }

        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("user after", userJackAfter);
        assertNoAssignments(userJackAfter);
    }

    @Test
    public void test850JackAssignRoleBloodyFool() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_BLOODY_FOOL_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignedRole(userAfter, ROLE_BLOODY_FOOL_OID);

        assertDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Fool", "Simpleton");

        displayDumpable("Simpleton groups", getDummyResource().getGroupByName(GROUP_SIMPLETONS_NAME));

        assertDummyGroupMember(null, GROUP_FOOLS_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertDummyGroupMember(null, GROUP_SIMPLETONS_NAME, ACCOUNT_JACK_DUMMY_USERNAME);

    }

    @Test
    public void test855JackModifyFoolMetaroleDeleteInducement() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        PrismObject<RoleType> roleBefore = getObject(RoleType.class, ROLE_META_FOOL_OID);
        display("Role meta fool before", roleBefore);
        assertInducements(roleBefore, 2);

        // WHEN
        when();
        modifyRoleDeleteInducement(ROLE_META_FOOL_OID, 10002L, getDefaultOptions(), task);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<RoleType> roleAfter = getObject(RoleType.class, ROLE_META_FOOL_OID);
        display("Role meta fool after", roleAfter);
        assertInducements(roleAfter, 1);
    }

    @Test
    public void test857JackReconcile() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        when();
        reconcileUser(USER_JACK_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignedRole(userAfter, ROLE_BLOODY_FOOL_OID);

        assertDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);

        // Title attribute is tolerant. As there is no delta then there is no reason to remove
        // the Simpleton value.
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Fool", "Simpleton");

        displayDumpable("Simpleton groups", getDummyResource().getGroupByName(GROUP_SIMPLETONS_NAME));

        assertDummyGroupMember(null, GROUP_FOOLS_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        // Group association is non-tolerant. It should be removed.
        assertNoDummyGroupMember(null, GROUP_SIMPLETONS_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
    }

    @Test
    public void test870AssignRoleScreaming() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        notificationManager.setDisabled(false);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userJackBefore = getUser(USER_JACK_OID);
        display("user jack", userJackBefore);

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_SCREAMING_OID, getDefaultOptions(), task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("user after", userJackAfter);

        displayDumpable("dummy transport", dummyTransport);
        List<Message> messages = dummyTransport.getMessages("dummy:policyRuleNotifier");
        assertNotNull("No notification messages", messages);
        assertEquals("Wrong # of notification messages", 1, messages.size());
    }

    /**
     * MID-4132: the global policy rules immutable-user-from-pluto-XXX selection is based on current object state
     * (not the new one) so it is _not_ selected in the primary state.
     * <p>
     * So, this test basically confirms the behavior described in MID-4132.
     * If this behavior changes, so should this test.
     */
    @Test
    public void test880GlobalRuleOnChange() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userJackBefore = getUser(USER_JACK_OID);
        display("user jack", userJackBefore);

        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_DESCRIPTION).replace("Came from Pluto")
                .asObjectDelta(USER_JACK_OID);

        // WHEN

        when();
        executeChangesAssertSuccess(delta, getDefaultOptions(), task, result);
    }

    /**
     * MID-4856
     */
    @Test
    public void test890DeleteRoleUndeletable() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        try {
            // WHEN
            when();
            deleteObject(RoleType.class, ROLE_UNDELETABLE_OID, task, result);
            AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
            // THEN
            then();
            result.computeStatus();
            TestUtil.assertFailure(result);
        }
    }

    /**
     * Workaround for MID-4856 (important before the issue was fixed).
     */
    @Test
    public void test892DeleteRoleUndeletableGlobal() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        try {
            // WHEN
            when();
            deleteObject(RoleType.class, ROLE_UNDELETABLE_GLOBAL_OID, task, result);
            AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
            // THEN
            then();
            System.out.println("Got expected exception: " + e.getMessage());
            result.computeStatus();
            TestUtil.assertFailure(result);
        }
    }

    @Test
    public void test900ModifyDetectingRole() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        login(userAdministrator);
        var task = getTestTask();
        var result = task.getResult();

        when("role is modified");
        ObjectDelta<UserType> delta = prismContext.deltaFor(RoleType.class)
                .item(RoleType.F_NAME).replace(PolyString.fromOrig("modified"))
                .asObjectDelta(ROLE_DETECTING_MODIFICATIONS.oid);
        executeChanges(delta, null, task, result);

        then("script was run, resulting in description being set");
        assertSuccess(result);
        assertRoleAfter(ROLE_DETECTING_MODIFICATIONS.oid)
                .assertName("modified") // the original change
                .assertDescription("Modified by administrator; segments: 2; immediateRole: modified"); // inserted by the script
    }

    /**
     * User that fails policy rule induced script.
     *
     * MID-6753
     */
    @Test
    public void test910UserFailingScript() throws Exception {
        given();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        when();
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_DESCRIPTION).replace("modified")
                .asObjectDelta(USER_FAILING_SCRIPT.oid);
        executeChanges(delta, null, task, result);

        // THEN
        then();
        assertPartialError(result);
    }

    /**
     * Tests role that adds recompute trigger with trigger customizer.
     *
     * MID-6076.
     */
    @Test
    public void test920AddRecomputeTrigger() throws Exception {
        given();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        UserType user = new UserType()
                .name("test920")
                .beginAssignment()
                    .targetRef(ROLE_ADDING_RECOMPUTE_TRIGGER.oid, RoleType.COMPLEX_TYPE)
                .end();
        addObject(user, task, result);

        when();
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_DESCRIPTION).replace("modified")
                .asObjectDelta(user.getOid());
        executeChanges(delta, null, task, result);

        then();
        assertSuccess(result);

        assertUserAfter(user.getOid())
                .triggers()
                    .single()
                        .assertOriginDescription("added by role");
    }

    /**
     * MID-7093
     */
    @Test
    public void test930NonUnassignableRole() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        UserType user = new UserType()
                .name("test930")
                .beginAssignment()
                    .targetRef(ROLE_NON_UNASSIGNABLE.oid, RoleType.COMPLEX_TYPE)
                .end();
        repoAddObject(user.asPrismObject(), result);

        transplantGlobalPolicyRulesAdd(GLOBAL_POLICY_RULES_ASSIGNMENT_DELETION, task, result);

        when();

        try {
            unassignRole(user.getOid(), ROLE_NON_UNASSIGNABLE.oid, task, result);
            fail("unexpected success");
        } catch (PolicyViolationException e) {
            then();
            displayExpectedException(e);
        } catch (Exception e) {
            then();
            throw new AssertionError("Unexpected exception: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean testMultiplicityConstraintsForNonDefaultRelations() {
        return true;
    }
}
