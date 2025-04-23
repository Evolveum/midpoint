/*
 * Copyright (c) 2016-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.orgstruct;

import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.File;

/**
 * Caribbean orgstruct tests, assigning accounts to ordinary org members and managers.
 * This configuration is using archetypes.
 * @author semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public abstract class AbstractOrgStructCaribbeanTest extends AbstractInitializedModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/orgstruct");

    static final TestObject<UserType> USER_GIBBS = TestObject.file(
            TEST_DIR, "user-gibbs.xml", "aca242ae-a29e-11e6-8bb4-1f8a1be2bd79");
    public static final String USER_GIBBS_USERNAME = "gibbs";

    static final TestObject<UserType> USER_PINTEL = TestObject.file(
            TEST_DIR, "user-pintel.xml", "16522760-a2a3-11e6-bf77-8baa83388f4b");
    public static final String USER_PINTEL_USERNAME = "pintel";

    protected static final String ORG_CARIBBEAN_THE_CROWN_OID = "00000000-8888-6666-0000-c00000000002";
    protected static final String ORG_CARIBBEAN_JAMAICA_OID = "00000000-8888-6666-0000-c00000000003";
    protected static final String ORG_CARIBBEAN_DEPARTMENT_OF_THINGS_OID = "00000000-8888-6666-0000-c00000000004";
    protected static final String ORG_CARIBBEAN_DEPARTMENT_OF_PEOPLE_OID = "00000000-8888-6666-0000-c00000000005";
    protected static final String ORG_CARIBBEAN_ENTERTAINMENT_SECTION_OID = "00000000-8888-6666-0000-c00000000006";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initTestObjects(
                initTask, initResult,
                USER_GIBBS,
                USER_PINTEL);
    }

    protected abstract File getOrgCaribbeanFile();

    /**
     * MID-3448
     */
    @Test
    public void test100AddOrgCaribbean() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        repoAddObjectsFromFile(getOrgCaribbeanFile(), OrgType.class, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<OrgType> orgJamaica = getObject(OrgType.class, ORG_CARIBBEAN_JAMAICA_OID);
        display("Jamaica", orgJamaica);
        assertAssignedOrgs(orgJamaica, ORG_CARIBBEAN_THE_CROWN_OID);
        assertHasOrgs(orgJamaica, ORG_CARIBBEAN_THE_CROWN_OID);

        PrismObject<OrgType> orgMonkeyGovernor = getObject(OrgType.class, ORG_GOVERNOR_OFFICE_OID);
        display("Moneky Governor", orgMonkeyGovernor);
        assertHasNoOrg(orgMonkeyGovernor, ORG_GOVERNOR_OFFICE_OID);

        PrismObject<OrgType> orgDoT = getObject(OrgType.class, ORG_CARIBBEAN_DEPARTMENT_OF_THINGS_OID);
        display("Department of Things", orgDoT);
        assertAssignedOrgs(orgDoT, ORG_CARIBBEAN_JAMAICA_OID);
        assertHasOrgs(orgDoT, ORG_CARIBBEAN_JAMAICA_OID);
        assertHasNoOrg(orgDoT, ORG_GOVERNOR_OFFICE_OID);
    }

    /**
     * MID-3448
     */
    @Test
    public void test102RecomputeJamaica() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        modelService.recompute(OrgType.class, ORG_CARIBBEAN_JAMAICA_OID, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<OrgType> orgJamaica = getObject(OrgType.class, ORG_CARIBBEAN_JAMAICA_OID);
        display("Jamaica", orgJamaica);
        assertAssignedOrgs(orgJamaica, ORG_CARIBBEAN_THE_CROWN_OID);
        assertHasOrgs(orgJamaica, ORG_CARIBBEAN_THE_CROWN_OID);
        assertHasNoOrg(orgJamaica, ORG_GOVERNOR_OFFICE_OID);

        PrismObject<OrgType> orgMonkeyGovernor = getObject(OrgType.class, ORG_GOVERNOR_OFFICE_OID);
        display("Monkey Governor", orgMonkeyGovernor);
        assertHasNoOrg(orgMonkeyGovernor, ORG_CARIBBEAN_JAMAICA_OID);
    }

    /**
     * MID-3448
     */
    @Test
    public void test103ReconcileJamaica() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        reconcileOrg(ORG_CARIBBEAN_JAMAICA_OID, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<OrgType> orgJamaica = getObject(OrgType.class, ORG_CARIBBEAN_JAMAICA_OID);
        display("Jamaica", orgJamaica);
        assertAssignedOrgs(orgJamaica, ORG_CARIBBEAN_THE_CROWN_OID);
        assertHasOrgs(orgJamaica, ORG_CARIBBEAN_THE_CROWN_OID);
        assertHasNoOrg(orgJamaica, ORG_GOVERNOR_OFFICE_OID);

        PrismObject<OrgType> orgMonkeyGovernor = getObject(OrgType.class, ORG_GOVERNOR_OFFICE_OID);
        display("Monkey Governor", orgMonkeyGovernor);
        assertHasNoOrg(orgMonkeyGovernor, ORG_CARIBBEAN_JAMAICA_OID);
    }

    /**
     * MID-3448
     */
    @Test
    public void test104RecomputeGovernor() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        modelService.recompute(OrgType.class, ORG_GOVERNOR_OFFICE_OID, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<OrgType> orgJamaica = getObject(OrgType.class, ORG_CARIBBEAN_JAMAICA_OID);
        display("Jamaica", orgJamaica);
        assertAssignedOrgs(orgJamaica, ORG_CARIBBEAN_THE_CROWN_OID);
        assertHasOrgs(orgJamaica, ORG_CARIBBEAN_THE_CROWN_OID);
        assertHasNoOrg(orgJamaica, ORG_GOVERNOR_OFFICE_OID);

        PrismObject<OrgType> orgMonkeyGovernor = getObject(OrgType.class, ORG_GOVERNOR_OFFICE_OID);
        display("Monkey Governor", orgMonkeyGovernor);
        assertHasNoOrg(orgMonkeyGovernor, ORG_CARIBBEAN_JAMAICA_OID);
    }

    /**
     * MID-3448
     */
    @Test
    public void test105ReconcileGovernor() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        reconcileOrg(ORG_GOVERNOR_OFFICE_OID, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<OrgType> orgJamaica = getObject(OrgType.class, ORG_CARIBBEAN_JAMAICA_OID);
        display("Jamaica", orgJamaica);
        assertAssignedOrgs(orgJamaica, ORG_CARIBBEAN_THE_CROWN_OID);
        assertHasOrgs(orgJamaica, ORG_CARIBBEAN_THE_CROWN_OID);
        assertHasNoOrg(orgJamaica, ORG_GOVERNOR_OFFICE_OID);

        PrismObject<OrgType> orgMonkeyGovernor = getObject(OrgType.class, ORG_GOVERNOR_OFFICE_OID);
        display("Monkey Governor", orgMonkeyGovernor);
        assertHasNoOrg(orgMonkeyGovernor, ORG_CARIBBEAN_JAMAICA_OID);
    }

    /**
     * Jamaica has an inducement to Monkey Island Governor Office.
     * Sub-orgs of Jamaica should appear under Governor office.
     * <p>
     * MID-3448
     */
    @Test
    public void test106RecomputeDoT() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        modelService.recompute(OrgType.class, ORG_CARIBBEAN_DEPARTMENT_OF_THINGS_OID,
                executeOptions().reconcileFocus(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<OrgType> orgJamaica = getObject(OrgType.class, ORG_CARIBBEAN_JAMAICA_OID);
        display("Jamaica", orgJamaica);
        assertAssignedOrgs(orgJamaica, ORG_CARIBBEAN_THE_CROWN_OID);
        assertHasOrgs(orgJamaica, ORG_CARIBBEAN_THE_CROWN_OID);
        assertHasNoOrg(orgJamaica, ORG_GOVERNOR_OFFICE_OID);

        PrismObject<OrgType> orgMonkeyGovernor = getObject(OrgType.class, ORG_GOVERNOR_OFFICE_OID);
        display("Monkey Governor", orgMonkeyGovernor);
        assertHasNoOrg(orgMonkeyGovernor, ORG_CARIBBEAN_JAMAICA_OID);

        PrismObject<OrgType> orgDoT = getObject(OrgType.class, ORG_CARIBBEAN_DEPARTMENT_OF_THINGS_OID);
        display("Department of Things", orgDoT);
        assertAssignedOrgs(orgDoT, ORG_CARIBBEAN_JAMAICA_OID);
        assertHasOrgs(orgDoT, ORG_CARIBBEAN_JAMAICA_OID, ORG_GOVERNOR_OFFICE_OID);
    }

    /**
     * MID-3448
     */
    @Test
    public void test107ReconcileDoT() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        reconcileOrg(ORG_CARIBBEAN_DEPARTMENT_OF_THINGS_OID, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<OrgType> orgJamaica = getObject(OrgType.class, ORG_CARIBBEAN_JAMAICA_OID);
        display("Jamaica", orgJamaica);
        assertAssignedOrgs(orgJamaica, ORG_CARIBBEAN_THE_CROWN_OID);
        assertHasOrgs(orgJamaica, ORG_CARIBBEAN_THE_CROWN_OID);
        assertHasNoOrg(orgJamaica, ORG_GOVERNOR_OFFICE_OID);

        PrismObject<OrgType> orgMonkeyGovernor = getObject(OrgType.class, ORG_GOVERNOR_OFFICE_OID);
        display("Monkey Governor", orgMonkeyGovernor);
        assertHasNoOrg(orgMonkeyGovernor, ORG_CARIBBEAN_JAMAICA_OID);

        PrismObject<OrgType> orgDoT = getObject(OrgType.class, ORG_CARIBBEAN_DEPARTMENT_OF_THINGS_OID);
        display("Department of Things", orgDoT);
        assertAssignedOrgs(orgDoT, ORG_CARIBBEAN_JAMAICA_OID);
        assertHasOrgs(orgDoT, ORG_CARIBBEAN_JAMAICA_OID, ORG_GOVERNOR_OFFICE_OID);
    }

    /**
     * Department of People (DoP) has in inducement to Monkey Island Scumm Bar.
     * But that inducement is limited to UserType.
     * Therefore sub-orgs of DoP should not appear under Scumm Bar.
     * <p>
     * Related to MID-3448
     */
    @Test
    public void test110RecomputeDoP() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        modelService.recompute(OrgType.class, ORG_CARIBBEAN_DEPARTMENT_OF_PEOPLE_OID, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<OrgType> orgEntertainmentSection = getObject(OrgType.class, ORG_CARIBBEAN_ENTERTAINMENT_SECTION_OID);
        display("Entertainment Section", orgEntertainmentSection);
        assertHasNoOrg(orgEntertainmentSection, ORG_SCUMM_BAR_OID);

        PrismObject<OrgType> orgScummBar = getObject(OrgType.class, ORG_SCUMM_BAR_OID);
        display("Scumm Bar", orgScummBar);
        assertHasNoOrg(orgScummBar, ORG_CARIBBEAN_ENTERTAINMENT_SECTION_OID);

    }

    /**
     * Department of People (DoP) has in inducement to Monkey Island Scumm Bar.
     * That inducement is limited to UserType.
     * Therefore sub-orgs of DoP should not appear under Scumm Bar.
     * But when Jack is assigned to the DoP he should also appear under Scumm Bar.
     * <p>
     * Related to MID-3448
     */
    @Test
    public void test115AssignJackToDoP() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        assertNoDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, USER_JACK_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_JACK_USERNAME);

        PrismObject<UserType> userJackBefore = getUser(USER_JACK_OID);
        dumpFocus("User Jack before", userJackBefore);

        // WHEN
        when();
        assignOrg(USER_JACK_OID, ORG_CARIBBEAN_DEPARTMENT_OF_PEOPLE_OID, null);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<OrgType> orgEntertainmentSection = getObject(OrgType.class, ORG_CARIBBEAN_ENTERTAINMENT_SECTION_OID);
        display("Entertainment Section", orgEntertainmentSection);
        assertHasNoOrg(orgEntertainmentSection, ORG_SCUMM_BAR_OID);

        PrismObject<OrgType> orgScummBar = getObject(OrgType.class, ORG_SCUMM_BAR_OID);
        display("Scumm Bar", orgScummBar);
        assertHasNoOrg(orgScummBar, ORG_CARIBBEAN_ENTERTAINMENT_SECTION_OID);

        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        dumpFocus("User Jack after", userJackAfter);
        assertHasOrgs(userJackAfter, ORG_CARIBBEAN_DEPARTMENT_OF_PEOPLE_OID, ORG_SCUMM_BAR_OID);
        assertRoleMembershipRef(userJackAfter, ORG_CARIBBEAN_DEPARTMENT_OF_PEOPLE_OID, ORG_SCUMM_BAR_OID);
        assertAccount(userJackAfter, RESOURCE_DUMMY_OID); // From Scumm Bar
        assertAccount(userJackAfter, RESOURCE_DUMMY_YELLOW_OID);
        assertNoDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_JACK_USERNAME);
        assertLiveLinks(userJackAfter, 2);

        assertDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, USER_JACK_USERNAME);
        assertDummyAccountAttribute(RESOURCE_DUMMY_YELLOW_NAME, USER_JACK_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Pirate");

        assertNoDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_JACK_USERNAME);

    }

    /**
     * Barbossa is a manager. He should get the red account from the piracy metarole.
     * But he should NOT get the yellow account.
     */
    @Test
    public void test120AssignBarbossaDoTManager() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        assertNoDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, USER_BARBOSSA_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME);

        // WHEN
        when();
        assignOrg(USER_BARBOSSA_OID, ORG_CARIBBEAN_DEPARTMENT_OF_THINGS_OID, SchemaConstants.ORG_MANAGER);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        dumpFocus("User barbossa after", userBarbossaAfter);
        assertHasOrgs(userBarbossaAfter, ORG_CARIBBEAN_DEPARTMENT_OF_THINGS_OID);
        assertRoleMembershipRef(userBarbossaAfter, ORG_CARIBBEAN_DEPARTMENT_OF_THINGS_OID);
        assertAccount(userBarbossaAfter, RESOURCE_DUMMY_RED_OID);
        assertLiveLinks(userBarbossaAfter, 1);

        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME);
        assertDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Captain");

        assertNoDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, USER_BARBOSSA_USERNAME);

    }

    /**
     * MID-3472
     */
    @Test
    public void test130AssignGibbsAsJacksDeputy() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        assertNoDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, USER_GIBBS_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_GIBBS_USERNAME);

        // WHEN
        when();
        assignDeputy(USER_GIBBS.oid, USER_JACK_OID, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userGibbsAfter = getUser(USER_GIBBS.oid);
        dumpFocus("User Gibbs after", userGibbsAfter);
        assertHasOrgs(userGibbsAfter, ORG_CARIBBEAN_DEPARTMENT_OF_PEOPLE_OID, ORG_SCUMM_BAR_OID);
        assertRoleMembershipRef(userGibbsAfter);
        assertDelegatedRef(userGibbsAfter, ORG_CARIBBEAN_DEPARTMENT_OF_PEOPLE_OID, ORG_SCUMM_BAR_OID, USER_JACK_OID);
        assertAccount(userGibbsAfter, RESOURCE_DUMMY_OID); // From Scumm Bar
        assertAccount(userGibbsAfter, RESOURCE_DUMMY_YELLOW_OID);
        assertLiveLinks(userGibbsAfter, 2);

        assertDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, USER_GIBBS_USERNAME);
        assertDummyAccountAttribute(RESOURCE_DUMMY_YELLOW_NAME, USER_GIBBS_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Pirate");

        assertNoDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_GIBBS_USERNAME);
    }

    /**
     * MID-3472
     */
    @Test
    public void test140AssignPintelAsBarbossasDeputy() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        assertNoDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, USER_PINTEL_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_PINTEL_USERNAME);

        // WHEN
        when();
        assignDeputy(USER_PINTEL.oid, USER_BARBOSSA_OID, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userPintelAfter = getUser(USER_PINTEL.oid);
        dumpFocus("User pintel after", userPintelAfter);
        assertHasOrgs(userPintelAfter, ORG_CARIBBEAN_DEPARTMENT_OF_THINGS_OID);
        assertRoleMembershipRef(userPintelAfter);
        assertDelegatedRef(userPintelAfter, ORG_CARIBBEAN_DEPARTMENT_OF_THINGS_OID, USER_BARBOSSA_OID);
        assertAccount(userPintelAfter, RESOURCE_DUMMY_RED_OID);
        assertLiveLinks(userPintelAfter, 1);

        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_PINTEL_USERNAME);
        assertDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, USER_PINTEL_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Captain");

        assertNoDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, USER_PINTEL_USERNAME);

    }

}
