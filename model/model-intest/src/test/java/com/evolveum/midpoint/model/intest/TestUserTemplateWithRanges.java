/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import java.io.File;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.test.TestObject;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommonException;

/**
 * User template with "mapping range" features.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestUserTemplateWithRanges extends AbstractInitializedModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/object-template-ranges");

    private static final QName MANAGER_ID_QNAME = new QName("http://sample.evolveum.com/xml/ns/sample-idm/extension", "managerId");

    private static final File ROLE_BLOODY_NOSE_FILE = new File(TEST_DIR, "role-bloody-nose.xml");
    private static final String ROLE_BLOODY_NOSE_OID = "ed34d3fe-1f0b-11e9-8e31-b3cfa585868a";
    private static final String ROLE_BLOODY_NOSE_NAME = "Bloody Nose";

    private static final File ORG_MONKEY_ISLAND_LOCAL_FILE = new File(TEST_DIR, "org-monkey-island-local.xml");

    private static final File USER_TEMPLATE_RANGES_FILE = new File(TEST_DIR, "user-template-ranges.xml");
    private static final String USER_TEMPLATE_RANGES_OID = "f486e3a7-6970-416e-8fe2-995358f59c46";

    private static final TestObject<ObjectTemplateType> USER_TEMPLATE_MID_5953 = TestObject.file(
            TEST_DIR, "user-template-mid-5953.xml", "55acacd1-2b42-4af1-9e98-d2d54293e4e9");

    private static final String SUBTYPE_MID_5953 = "mid-5953"; // range application in inactive (condition false->false) mappings

    private static final String BLOODY_ASSIGNMENT_SUBTYPE = "bloody";

    private static final XMLGregorianCalendar GUYBRUSH_FUNERAL_DATE_123456_CAL =
            XmlTypeConverter.createXMLGregorianCalendar(2222, 1, 2, 3, 4, 5);

    private static final XMLGregorianCalendar GUYBRUSH_FUNERAL_DATE_22222_CAL =
            XmlTypeConverter.createXMLGregorianCalendar(2222, 2, 2, 22, 22, 22);

    @Override
    protected boolean doAddOrgstruct() {
        return false; // we use our own
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        repoAddObjectsFromFile(ORG_MONKEY_ISLAND_LOCAL_FILE, OrgType.class, initResult);

        repoAddObjectFromFile(USER_TEMPLATE_RANGES_FILE, initResult);
        repoAddObjectFromFile(ROLE_BLOODY_NOSE_FILE, initResult);
        repoAdd(USER_TEMPLATE_MID_5953, initResult);

        setDefaultObjectTemplate(UserType.COMPLEX_TYPE, USER_TEMPLATE_RANGES_OID, initResult);
        setDefaultObjectTemplate(UserType.COMPLEX_TYPE, SUBTYPE_MID_5953, USER_TEMPLATE_MID_5953.oid, initResult);

        changeEmployeeIdRaw("EM100", initTask, initResult);
    }

    /**
     * Recomputation should give Elaine a manager-type assignment to Gov office.
     */
    @Test
    public void test100RecomputeElaine() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        recomputeUser(USER_ELAINE_OID, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userElaine = getUser(USER_ELAINE_OID);
        display("elaine after recompute", userElaine);
        assertAssignedOrg(userElaine, ORG_GOVERNOR_OFFICE_OID, SchemaConstants.ORG_MANAGER);
        assertHasOrg(userElaine, ORG_GOVERNOR_OFFICE_OID, SchemaConstants.ORG_MANAGER);
    }

    /**
     * After changing manager info in Gov office org,
     * recomputation should remove Elaine a manager-type assignment to Gov office.
     */
    @Test
    public void test110ChangeManagerAndRecomputeElaine() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        changeManagerRaw("xxxxx", task, result);
        recomputeUser(USER_ELAINE_OID, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userElaine = getUser(USER_ELAINE_OID);
        display("elaine after change manager ID + recompute", userElaine);
        assertNotAssignedOrg(userElaine, ORG_GOVERNOR_OFFICE_OID, SchemaConstants.ORG_MANAGER);
        assertHasNoOrg(userElaine);        // this is even stronger
    }

    /**
     * After setting manager in Gov office org back to EM100,
     * recomputation should add Elaine a manager-type assignment to Gov office.
     * In addition, we made her a member of Gov office as a preparation for test130.
     */
    @Test
    public void test120RestoreManagerAndRecomputeElaineAgain() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        assignOrg(USER_ELAINE_OID, ORG_GOVERNOR_OFFICE_OID, null);

        // WHEN
        changeManagerRaw("EM100", task, result);
        recomputeUser(USER_ELAINE_OID, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userElaine = getUser(USER_ELAINE_OID);
        display("elaine after restore of manager ID + recompute", userElaine);
        assertAssignedOrg(userElaine, ORG_GOVERNOR_OFFICE_OID, SchemaConstants.ORG_MANAGER);
        assertAssignedOrg(userElaine, ORG_GOVERNOR_OFFICE_OID, null);
        assertHasOrg(userElaine, ORG_GOVERNOR_OFFICE_OID, SchemaConstants.ORG_MANAGER);
        assertHasOrg(userElaine, ORG_GOVERNOR_OFFICE_OID, null);
    }

    /**
     * After changing manager info in Gov office org again,
     * recomputation should remove Elaine a manager-type assignment to Gov office.
     * But it should keep her as a member.
     */
    @Test
    public void test140ChangeManagerAndRecomputeElaineAgain() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        changeManagerRaw("xxxxx", task, result);
        recomputeUser(USER_ELAINE_OID, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userElaine = getUser(USER_ELAINE_OID);
        display("elaine after change of manager ID + recompute", userElaine);
        assertNotAssignedOrg(userElaine, ORG_GOVERNOR_OFFICE_OID, SchemaConstants.ORG_MANAGER);
        assertAssignedOrg(userElaine, ORG_GOVERNOR_OFFICE_OID);
        assertHasNoOrg(userElaine, ORG_GOVERNOR_OFFICE_OID, SchemaConstants.ORG_MANAGER);
        assertHasOrg(userElaine, ORG_GOVERNOR_OFFICE_OID);
    }


    /*
     * 2xx series: testing ranges with conditions
     * ==========================================
     */

    /**
     * Simple add values, nothing special. (Preparing ground for next test.)
     */
    @Test
    public void test200SimpleOrgUnitAddition() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_ORGANIZATIONAL_UNIT)
                        .add(PolyString.fromOrig("U1"),
                                PolyString.fromOrig("U2"),
                                PolyString.fromOrig("U3"),
                                PolyString.fromOrig("U4"))
                        .item(UserType.F_ORGANIZATION).add(PolyString.fromOrig("O1"))
                        .asObjectDelta(USER_JACK_OID),
                null, task, result);

        // THEN
        assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("jack", userJack);
        PrismAsserts.assertPropertyValue(userJack, UserType.F_ORGANIZATIONAL_UNIT,
                new PolyString("U1", "u1"),
                new PolyString("U2", "u2"),
                new PolyString("U3", "u3"),
                new PolyString("U4", "u4"));
        PrismAsserts.assertPropertyValue(userJack, UserType.F_ORGANIZATION,
                new PolyString("O1", "o1"),
                new PolyString("OU: U1 emp1234", "ou u1 emp1234"),
                new PolyString("OU: U2 emp1234", "ou u2 emp1234"),
                new PolyString("OU: U3 emp1234", "ou u3 emp1234"),
                new PolyString("OU: U4 emp1234", "ou u4 emp1234"));
    }

    /**
     * OrganizationalUnit U1 disappearing + recompute. Computed value of 'OU: U1 ...' should be removed.
     * Note that condition is true -> true.
     */
    @Test
    public void test210RemoveUnit1() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_ORGANIZATIONAL_UNIT).delete(PolyString.fromOrig("U1"))
                        .asObjectDelta(USER_JACK_OID),
                executeOptions().raw(), task, result);

        recomputeUser(USER_JACK_OID, task, result);

        // THEN
        assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("jack", userJack);
        PrismAsserts.assertPropertyValue(userJack, UserType.F_ORGANIZATIONAL_UNIT,
                new PolyString("U2", "u2"),
                new PolyString("U3", "u3"),
                new PolyString("U4", "u4"));
        PrismAsserts.assertPropertyValue(userJack, UserType.F_ORGANIZATION,
                new PolyString("O1", "o1"),
                new PolyString("OU: U2 emp1234", "ou u2 emp1234"),
                new PolyString("OU: U3 emp1234", "ou u3 emp1234"),
                new PolyString("OU: U4 emp1234", "ou u4 emp1234"));
    }

    /**
     * OrganizationalUnit U2 disappearing.
     * Then, along with recompute we erase employeeNumber.
     * Mapping 'ceases to exist' and all of its range should be deleted.
     * Condition: true -> false
     */
    @Test
    public void test220RemoveUnit2AndNumber() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_ORGANIZATIONAL_UNIT).delete(PolyString.fromOrig("U2"))
                        .asObjectDelta(USER_JACK_OID),
                executeOptions().raw(), task, result);

        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_EMPLOYEE_NUMBER).replace()
                        .asObjectDelta(USER_JACK_OID),
                null, task, result);

        // THEN
        assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("jack", userJack);
        PrismAsserts.assertPropertyValue(userJack, UserType.F_ORGANIZATIONAL_UNIT,
                new PolyString("U3", "u3"),
                new PolyString("U4", "u4"));
        PrismAsserts.assertPropertyValue(userJack, UserType.F_ORGANIZATION,
                new PolyString("O1", "o1"));
    }

    /**
     * Introduced OU: nonsense.
     * Restoring employeeNumber.
     * All missing 'OU: *' should be re-added, 'OU: nonsense' should be removed.
     * Condition: false -> true
     */
    @Test
    public void test230RestoreNumber() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_ORGANIZATION).add(PolyString.fromOrig("OU: nonsense"))
                        .asObjectDelta(USER_JACK_OID),
                executeOptions().raw(), task, result);

        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_EMPLOYEE_NUMBER).replace("emp1234")
                        .asObjectDelta(USER_JACK_OID),
                null, task, result);

        // THEN
        assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("jack", userJack);
        PrismAsserts.assertPropertyValue(userJack, UserType.F_ORGANIZATIONAL_UNIT,
                new PolyString("U3", "u3"),
                new PolyString("U4", "u4"));
        PrismAsserts.assertPropertyValue(userJack, UserType.F_ORGANIZATION,
                new PolyString("O1", "o1"),
                new PolyString("OU: U3 emp1234", "ou u3 emp1234"),
                new PolyString("OU: U4 emp1234", "ou u4 emp1234"));
    }

    @Test
    public void test300GuybrushBloodyNose() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_TITLE).add(PolyString.fromOrig(ROLE_BLOODY_NOSE_NAME))
                        .asObjectDelta(USER_GUYBRUSH_OID),
                null, task, result);

        // THEN
        then();
        assertSuccess(result);

        // @formatter:off
        assertUserAfter(USER_GUYBRUSH_OID)
            .assertTitle(ROLE_BLOODY_NOSE_NAME)
            .assignments()
                .single()
                    .assertTargetOid(ROLE_BLOODY_NOSE_OID)
                    .assertSubtype(BLOODY_ASSIGNMENT_SUBTYPE)
                    .activation()
                        .assertNoValidTo();
        // @formatter:on
    }

    @Test
    public void test309GuybrushNotBloody() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_TITLE).delete(PolyString.fromOrig(ROLE_BLOODY_NOSE_NAME))
                        .asObjectDelta(USER_GUYBRUSH_OID),
                null, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertUserAfter(USER_GUYBRUSH_OID)
                .assertNoTitle()
                .assignments().assertNone();
    }

    @Test
    public void test310GuybrushBloodyNoseFuneral() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        modifyUserReplace(USER_GUYBRUSH_OID, getExtensionPath(PIRACY_FUNERAL_TIMESTAMP), task, result,
                GUYBRUSH_FUNERAL_DATE_123456_CAL);

        // WHEN
        when();
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_TITLE).add(PolyString.fromOrig(ROLE_BLOODY_NOSE_NAME))
                        .asObjectDelta(USER_GUYBRUSH_OID),
                null, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertUserAfter(USER_GUYBRUSH_OID)
                .assertTitle(ROLE_BLOODY_NOSE_NAME)
                .assignments()
                .single()
                .assertTargetOid(ROLE_BLOODY_NOSE_OID)
                .assertSubtype(BLOODY_ASSIGNMENT_SUBTYPE)
                .activation()
                .assertValidTo(GUYBRUSH_FUNERAL_DATE_123456_CAL);

    }

    @Test
    public void test319GuybrushNoBloodyNoseFuneral() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_TITLE).delete(PolyString.fromOrig(ROLE_BLOODY_NOSE_NAME))
                        .asObjectDelta(USER_GUYBRUSH_OID),
                null, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertUserAfter(USER_GUYBRUSH_OID)
                .assertNoTitle()
                .assignments()
                .assertNone();

    }

    @Test
    public void test320GuybrushBloodyNose() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        modifyUserReplace(USER_GUYBRUSH_OID, getExtensionPath(PIRACY_FUNERAL_TIMESTAMP), task, result
                /* no value */);

        // WHEN
        when();
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_TITLE).add(PolyString.fromOrig(ROLE_BLOODY_NOSE_NAME))
                        .asObjectDelta(USER_GUYBRUSH_OID),
                null, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertUserAfter(USER_GUYBRUSH_OID)
                .assertTitle(ROLE_BLOODY_NOSE_NAME)
                .assignments()
                .single()
                .assertTargetOid(ROLE_BLOODY_NOSE_OID)
                .assertSubtype(BLOODY_ASSIGNMENT_SUBTYPE)
                .activation()
                .assertNoValidTo();

    }

    @Test
    public void test322GuybrushSetFuneral() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        modifyUserReplace(USER_GUYBRUSH_OID, getExtensionPath(PIRACY_FUNERAL_TIMESTAMP), task, result,
                GUYBRUSH_FUNERAL_DATE_123456_CAL);

        // THEN
        then();
        assertSuccess(result);

        assertUserAfter(USER_GUYBRUSH_OID)
                .assertTitle(ROLE_BLOODY_NOSE_NAME)
                .assignments()
                .single()
                .assertTargetOid(ROLE_BLOODY_NOSE_OID)
                .assertSubtype(BLOODY_ASSIGNMENT_SUBTYPE)
                .activation()
                .assertValidTo(GUYBRUSH_FUNERAL_DATE_123456_CAL);

    }

    @Test
    public void test324GuybrushSetFuneral22222() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        modifyUserReplace(USER_GUYBRUSH_OID, getExtensionPath(PIRACY_FUNERAL_TIMESTAMP), task, result,
                GUYBRUSH_FUNERAL_DATE_22222_CAL);

        // THEN
        then();
        assertSuccess(result);

        // @formatter:off
        assertUserAfter(USER_GUYBRUSH_OID)
            .assertTitle(ROLE_BLOODY_NOSE_NAME)
            .assignments()
                .single()
                    .assertTargetOid(ROLE_BLOODY_NOSE_OID)
                    .assertSubtype(BLOODY_ASSIGNMENT_SUBTYPE)
                    .activation()
                        .assertValidTo(GUYBRUSH_FUNERAL_DATE_22222_CAL);
        // @formatter:on
    }

    /**
     * MID-5063
     */
    @Test
    public void test326GuybrushNoFuneral() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        modifyUserReplace(USER_GUYBRUSH_OID, getExtensionPath(PIRACY_FUNERAL_TIMESTAMP), task, result
                /* no value */);

        // THEN
        then();
        assertSuccess(result);

        // @formatter:off
        assertUserAfter(USER_GUYBRUSH_OID)
            .assertTitle(ROLE_BLOODY_NOSE_NAME)
            .assignments()
                .single()
                    .assertTargetOid(ROLE_BLOODY_NOSE_OID)
                    .assertSubtype(BLOODY_ASSIGNMENT_SUBTYPE)
                    .activation()
                        .assertNoValidTo();
        // @formatter:on
    }

    @Test
    public void test329GuybrushNoBloodyNose() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_TITLE).delete(PolyString.fromOrig(ROLE_BLOODY_NOSE_NAME))
                        .asObjectDelta(USER_GUYBRUSH_OID),
                null, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertUserAfter(USER_GUYBRUSH_OID)
                .assertNoTitle()
                .assignments().assertNone();
    }

    /**
     * MID-5953
     */
    @Test
    public void test350DisabledMappingRange() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        UserType user = new UserType(prismContext)
                .name("test350")
                .subtype(SUBTYPE_MID_5953)
                .beginAssignment()
                    .targetRef(ROLE_SUPERUSER_OID, RoleType.COMPLEX_TYPE)
                .end();
        repoAddObject(user.asPrismObject(), result);

        when();
        recomputeUser(user.getOid(), task, result);

        // THEN
        then();
        assertSuccess(result);

        assertUserAfter(user.getOid())
                .assertAssignments(0);
    }

    private void changeManagerRaw(String id, Task task, OperationResult result) throws CommonException {
        executeChanges(
                deltaFor(OrgType.class)
                        .item(OrgType.F_EXTENSION, MANAGER_ID_QNAME).replace(id)
                        .asObjectDelta(ORG_GOVERNOR_OFFICE_OID),
                executeOptions().raw(), task, result);
    }

    private void changeEmployeeIdRaw(String id, Task initTask, OperationResult initResult) throws CommonException {
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_EMPLOYEE_NUMBER).replace(id)
                        .asObjectDelta(USER_ELAINE_OID),
                executeOptions().raw(), initTask, initResult);
    }
}
