/*
 * Copyright (c) 2018-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.archetypes;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.test.asserter.UserAsserter;
import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestArchetypes extends AbstractArchetypesTest {

    public static final File TEST_DIR = new File("src/test/resources/archetypes");

    private static final File SYSTEM_CONFIGURATION_ARCHETYPES_FILE = new File(TEST_DIR, "system-configuration-archetypes.xml");

    private static final String VIEW_ALL_EMPLOYEES_NAME = "all-employees";
    private static final String VIEW_ACTIVE_EMPLOYEES_IDENTIFIER = "active-employees";
    private static final String VIEW_BUSINESS_ROLES_IDENTIFIER = "business-roles-view";
    private static final String VIEW_BUSINESS_ROLES_LABEL = "Business";

    private static final File ARCHETYPE_CONTRACTOR_FILE = new File(TEST_DIR, "archetype-contractor.xml");
    private static final String ARCHETYPE_CONTRACTOR_OID = "3911cac2-78a6-11e9-8b5e-4b5bdb0c81d5";

    private static final File ARCHETYPE_TEST_FILE = new File(TEST_DIR, "archetype-test.xml");
    private static final String ARCHETYPE_TEST_OID = "a8df34a8-f6f0-11e8-b98e-eb03652d943f";

    private static final File ARCHETYPE_BUSINESS_ROLE_FILE = new File(TEST_DIR, "archetype-business-role.xml");
    private static final String ARCHETYPE_BUSINESS_ROLE_OID = "018e7340-199a-11e9-ad93-2b136d1c7ecf";
    private static final String ARCHETYPE_BUSINESS_ROLE_ICON_CSS_CLASS = "fe fe-business";
    private static final String ARCHETYPE_BUSINESS_ROLE_ICON_COLOR = "green";

    private static final File USER_MEATHOOK_FILE = new File(TEST_DIR, "user-meathook.xml");
    private static final String USER_MEATHOOK_OID = "f79fc10e-78a8-11e9-92ec-cf427cb6e7a0";

    private static final File USER_WANNABE_FILE = new File(TEST_DIR, "user-wannabe.xml");
    private static final String USER_WANNABE_OID = "28038d88-d3eb-11e9-87fb-cff5e050b6f9";

    private static final File USER_SELF_MADE_MAN_FILE = new File(TEST_DIR, "user-self-made-man.xml");
    private static final String USER_SELF_MADE_MAN_OID = "065c4592-0787-11ea-af06-f7eae18b6b4a";

    private static final File USER_FRAUDSTER_FILE = new File(TEST_DIR, "user-fraudster.xml");
    private static final String USER_FRAUDSTER_OID = "99b36382-078e-11ea-b9a9-b393552ec165";

    private static final File ROLE_EMPLOYEE_BASE_FILE = new File(TEST_DIR, "role-employee-base.xml");
    private static final String ROLE_EMPLOYEE_BASE_OID = "e869d6c4-f6ef-11e8-b51f-df3e51bba129";

    public static final File ROLE_USER_ADMINISTRATOR_FILE = new File(TEST_DIR, "role-user-administrator.xml");
    protected static final String ROLE_USER_ADMINISTRATOR_OID = "6ae02e34-f8b0-11e8-9c40-87e142b606fe";

    private static final File ROLE_BUSINESS_CAPTAIN_FILE = new File(TEST_DIR, "role-business-captain.xml");
    private static final String ROLE_BUSINESS_CAPTAIN_OID = "9f65f4b6-199b-11e9-a4c1-2f6b7eb1ebae";

    private static final File ROLE_BUSINESS_BOSUN_FILE = new File(TEST_DIR, "role-business-bosun.xml");
    protected static final String ROLE_BUSINESS_BOSUN_OID = "186b29c6-199c-11e9-9acc-3f8cc307573b";

    private static final File COLLECTION_ACTIVE_EMPLOYEES_FILE = new File(TEST_DIR, "collection-active-employees.xml");
    protected static final String COLLECTION_ACTIVE_EMPLOYEES_OID = "f61bcb4a-f8ae-11e8-9f5c-c3e7f27ee878";

    private static final File USER_TEMPLATE_ARCHETYPES_GLOBAL_FILE = new File(TEST_DIR, "user-template-archetypes-global.xml");
    private static final String USER_TEMPLATE_ARCHETYPES_GLOBAL_OID = "dab200ae-65dc-11e9-a8d3-27e5b1538f19";

    private static final File USER_TEMPLATE_CONTRACTOR_FILE = new File(TEST_DIR, "user-template-contractor.xml");
    protected static final String USER_TEMPLATE_CONTRACTOR_OID = "f72193e4-78a6-11e9-a0b6-6f5ad4cfbb37";

    private static final File ARCHETYPE_AUXILIARY_MANAGER_FILE = new File(TEST_DIR, "archetype-auxiliary-manager.xml");
    private static final String ARCHETYPE_AUXILIARY_MANAGER_OID = "ab061953-44e0-496b-aca4-23c6caf0eb60";

    private static final String CONTRACTOR_EMPLOYEE_NUMBER = "CONTRACTOR";

    private static final String ATTR_STUDENT = "student";

    private static final DummyTestResource RESOURCE_SOURCE = new DummyTestResource(
            TEST_DIR, "resource-dummy-source.xml", "3d87a26a-9a95-4c43-be3c-207b5ffe9474", "source",
            controller -> controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                    ATTR_STUDENT, String.class, false, false));

    private static final DummyTestResource RESOURCE_LAB = new DummyTestResource(
            TEST_DIR, "resource-dummy-lab.xml", "a353d845-64dd-439e-b0ce-3f9fe095c060", "lab");

    private static final TestResource<ArchetypeType> ARCHETYPE_STUDENT =
            new TestResource<>(TEST_DIR, "archetype-student.xml", "ca7bd614-9b1b-402c-a73c-0f571cb8b1c9");

    private static final TestResource<RoleType> ROLE_LAB_ACCESS =
            new TestResource<>(TEST_DIR, "role-lab-access.xml", "0d69d2c9-d1f4-4cfc-acb3-af8a71db81d1");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAddObjectFromFile(ROLE_EMPLOYEE_BASE_FILE, initResult);
        repoAddObjectFromFile(ROLE_USER_ADMINISTRATOR_FILE, initResult);
        repoAddObjectFromFile(ARCHETYPE_EMPLOYEE_FILE, initResult);
        repoAddObjectFromFile(ARCHETYPE_CONTRACTOR_FILE, initResult);
        repoAddObjectFromFile(COLLECTION_ACTIVE_EMPLOYEES_FILE, initResult);
        repoAddObjectFromFile(USER_TEMPLATE_ARCHETYPES_GLOBAL_FILE, initResult);
        repoAddObjectFromFile(USER_TEMPLATE_CONTRACTOR_FILE, initResult);
        repoAddObjectFromFile(ARCHETYPE_AUXILIARY_MANAGER_FILE, initResult);

        addObject(SHADOW_GROUP_DUMMY_TESTERS_FILE, initTask, initResult);

        addObject(ARCHETYPE_STUDENT, initTask, initResult);
        addObject(ROLE_LAB_ACCESS, initTask, initResult);
        initAndTestDummyResource(RESOURCE_SOURCE, initTask, initResult);
        initAndTestDummyResource(RESOURCE_LAB, initTask, initResult);

        setDefaultObjectTemplate(UserType.COMPLEX_TYPE, USER_TEMPLATE_ARCHETYPES_GLOBAL_OID, initResult);
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_ARCHETYPES_FILE;
    }

    /**
     * Test sanity of test setup. User jack, without any archetype.
     */
    @Test
    public void test020SanityJack() throws Exception {
        PrismObject<UserType> user = assertUserBefore(USER_JACK_OID)
            .assertFullName(USER_JACK_FULL_NAME)
            .getObject();

        assertEditSchema(user);
    }

    /**
     * Archetype "test" object is added to repo. It's not assigned yet.
     */
    @Test
    public void test050AddArchetypeTest() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("'test' archetype is added to repo");
        addObject(ARCHETYPE_TEST_FILE, task, result);

        then("'test' archetype is in repo");
        assertSuccess(result);

        PrismObject<ArchetypeType> archetypeTest = modelService.getObject(ArchetypeType.class, ARCHETYPE_TEST_OID, null, task, result);
        display("Archetype test", archetypeTest);
    }

    /**
     * More archetypes and roles are added to repo. No assignments to users yet.
     */
    @Test
    public void test060AddArchetypesAndRoles() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("'business role' archetype plus some business roles are added to repo");
        addObject(ARCHETYPE_BUSINESS_ROLE_FILE, task, result);
        addObject(ROLE_BUSINESS_CAPTAIN_FILE, task, result);
        addObject(ROLE_BUSINESS_BOSUN_FILE, task, result);

        then("business role(s) should be there with the correct archetype");
        assertSuccess(result);

        PrismObject<RoleType> roleBusinessCaptainAfter = assertRoleAfter(ROLE_BUSINESS_CAPTAIN_OID)
            .assertArchetypeRef(ARCHETYPE_BUSINESS_ROLE_OID)
            .getObject();

        // @formatter:off
        assertAssignmentHolderSpecification(roleBusinessCaptainAfter)
            .assignmentObjectRelations()
                .assertItems(2)
                .by()
                    .relations(SchemaConstants.ORG_APPROVER, SchemaConstants.ORG_OWNER)
                .find()
                    .assertArchetypeOid(ARCHETYPE_EMPLOYEE_OID)
                    .assertObjectType(UserType.COMPLEX_TYPE)
                    .assertDescription("Only employees may be owners/approvers for business role.")
                    .end()
                .by()
                    .relation(SchemaConstants.ORG_DEFAULT)
                .find()
                    .assertObjectType(UserType.COMPLEX_TYPE)
                    .assertNoArchetype();
        // @formatter:on
    }

    @Test
    public void test070AssignGuybrushUserAdministrator() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("administrator is assigned to guybrush");
        assignRole(USER_GUYBRUSH_OID, ROLE_USER_ADMINISTRATOR_OID, task, result);

        then("he should have 1 role and 0 archetypes");
        assertSuccess(result);

        assertUserAfter(USER_GUYBRUSH_OID)
                .assertNoArchetypeRef()
                .assertRoleMemberhipRefs(1)
                .assertCostCenter("Archetype null: null isEmployee: false");
    }

    /**
     * Jack is assigned the `employee` archetype.
     *
     * Note that the `costCenter` should be set appropriately in the first projector run.
     *
     * There are no asserts to see this, yet.
     */
    @Test
    public void test100AssignJackArchetypeEmployee() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("jack is assigned employee archetype");

        assignArchetype(USER_JACK_OID, ARCHETYPE_EMPLOYEE_OID, task, result);

        then("archetype is assigned and costCenter is correctly set");
        assertSuccess(result);

        // @formatter:off
        PrismObject<UserType> userAfter = assertUserAfter(USER_JACK_OID)
            .assignments()
                .assertAssignments(1)
                .assertArchetype(ARCHETYPE_EMPLOYEE_OID)
                .end()
            .assertArchetypeRef(ARCHETYPE_EMPLOYEE_OID)
            .roleMembershipRefs()
                .assertRoleMemberhipRefs(1)
                .assertArchetype(ARCHETYPE_EMPLOYEE_OID)
                .end()
            // MID-5243
            .assertCostCenter(costCenterEmployee())
            .getObject();
        // @formatter:on

        and("archetype policy is recognized for jack");
        assertArchetypePolicy(userAfter)
            .displayType()
                .assertLabel(ARCHETYPE_EMPLOYEE_DISPLAY_LABEL)
                .assertPluralLabel(ARCHETYPE_EMPLOYEE_DISPLAY_PLURAL_LABEL);

        // MID-5277
        and("schema from template is applied");
        assertEditSchema(userAfter);
    }

    private String costCenterEmployee() {
        return "Archetype " + ARCHETYPE_EMPLOYEE_OID + ": archetype:" + ARCHETYPE_EMPLOYEE_OID + "(Employee) isEmployee: true";
    }

    @Test
    public void test102SearchEmployeeArchetypeRef() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = queryFor(UserType.class)
            .item(UserType.F_ARCHETYPE_REF).ref(ARCHETYPE_EMPLOYEE_OID)
            .build();

        when("searching by archetypeRef = employee");

        SearchResultList<PrismObject<UserType>> searchResults = modelService.searchObjects(UserType.class, query, null, task, result);

        then("user jack is found");
        assertSuccess(result);
        display("Search results", searchResults);
        assertEquals("Wrong number of search results", 1, searchResults.size());
        PrismObject<UserType> foundUser = searchResults.get(0);
        // @formatter:off
        assertUser(foundUser, "found user")
            .assertName(USER_JACK_USERNAME)
            .assertOid(USER_JACK_OID)
            .assignments()
                .assertAssignments(1)
                .assertArchetype(ARCHETYPE_EMPLOYEE_OID)
                .end()
            .assertArchetypeRef(ARCHETYPE_EMPLOYEE_OID)
            .roleMembershipRefs()
                .assertRoleMemberhipRefs(1)
                .assertArchetype(ARCHETYPE_EMPLOYEE_OID)
                .end();
        // @formatter:on
    }

    @Test
    public void test104GetGuybrushCompiledGuiProfile() throws Exception {
        // GIVEN
        login(USER_GUYBRUSH_USERNAME);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        CompiledGuiProfile compiledGuiProfile = modelInteractionService.getCompiledGuiProfile(task, result);

        // THEN
        assertSuccess(result);

        loginAdministrator();

        // @formatter:off
        ObjectFilter allEmployeesViewFilter = assertCompiledGuiProfile(compiledGuiProfile)
            .assertAdditionalMenuLinks(0)
            .assertUserDashboardLinks(0)
            .assertObjectForms(2)
            .assertUserDashboardWidgets(0)
            .objectCollectionViews()
                .assertViews(3)
                .by()
                    .identifier(VIEW_ACTIVE_EMPLOYEES_IDENTIFIER)
                .find()
                    .assertName(VIEW_ACTIVE_EMPLOYEES_IDENTIFIER)
                    .assertFilter()
                    .end()
                .by()
                    .identifier(VIEW_BUSINESS_ROLES_IDENTIFIER)
                .find()
                    .assertName(VIEW_BUSINESS_ROLES_IDENTIFIER)
                    .assertFilter()
                    .displayType()
                        .assertLabel(VIEW_BUSINESS_ROLES_LABEL) // Overridden in view definition
                        .icon() // Inherited from archetype
                            .assertCssClass(ARCHETYPE_BUSINESS_ROLE_ICON_CSS_CLASS)
                            .assertColor(ARCHETYPE_BUSINESS_ROLE_ICON_COLOR)
                            .end()
                        .end()
                    .end()
                .by()
                    .identifier(VIEW_ALL_EMPLOYEES_NAME)
                .find()
                    .assertName(VIEW_ALL_EMPLOYEES_NAME)
                    .assertFilter()
                    .getFilter();
        // @formatter:on

        ObjectQuery viewQuery = prismContext.queryFactory().createQuery(allEmployeesViewFilter, null);
        SearchResultList<PrismObject<UserType>> searchResults =
                modelService.searchObjects(UserType.class, viewQuery, null, task, result);

        display("Search results", searchResults);
        assertEquals("Wrong number of search results", 1, searchResults.size());
        PrismObject<UserType> foundUser = searchResults.get(0);
        assertUser(foundUser, "found user")
            .assertName(USER_JACK_USERNAME)
            .assertOid(USER_JACK_OID);
    }

    @Test
    public void test109UnassignJackArchetypeEmployee() throws Exception {
        loginAdministrator();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        unassignArchetype(USER_JACK_OID, ARCHETYPE_EMPLOYEE_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        // @formatter:off
        PrismObject<UserType> userAfter = assertUserAfter(USER_JACK_OID)
            .assignments()
                .assertAssignments(0)
                .end()
            .assertNoArchetypeRef()
            .roleMembershipRefs()
                .assertRoleMemberhipRefs(0)
                .end()
            .assertCostCenter("Archetype null: null isEmployee: false")
            .getObject();
        // @formatter:on

        // Archetype policy derived from system config (global)
        assertArchetypePolicy(userAfter)
            .assertNoDisplay()
            .assertObjectTemplate(USER_TEMPLATE_ARCHETYPES_GLOBAL_OID);
    }

    /**
     * Jack is assigned 'employee base' role. That role induces 'employee' archetype!
     *
     * Here, the `costCenter` is computed only on the second projector run. The first run does not see
     * the archetype, because it is computed after the template is evaluated.
     */
    @Test(enabled = false) // Disabled, because we do not support indirectly-assigned archetypes.
    public void test110AssignJackRoleEmployeeBase() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        assignRole(USER_JACK_OID, ROLE_EMPLOYEE_BASE_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        // @formatter:off
        PrismObject<UserType> userAfter = assertUserAfter(USER_JACK_OID)
            .assignments()
                .assertAssignments(1)
                .assertRole(ROLE_EMPLOYEE_BASE_OID)
                .end()
            .assertArchetypeRef(ARCHETYPE_EMPLOYEE_OID)
            .roleMembershipRefs()
                .assertRoleMemberhipRefs(2)
                .assertArchetype(ARCHETYPE_EMPLOYEE_OID)
                .assertRole(ROLE_EMPLOYEE_BASE_OID)
                .end()
            .assertCostCenter(costCenterEmployee())
            .getObject();

        assertArchetypePolicy(userAfter).display()
            .displayType()
                .assertLabel(ARCHETYPE_EMPLOYEE_DISPLAY_LABEL)
                .assertPluralLabel(ARCHETYPE_EMPLOYEE_DISPLAY_PLURAL_LABEL);

        assertAssignmentTargetSpecification(userAfter)
            .assignmentObjectRelations()
                .assertItems(2)
                .by()
                    .targetType(RoleType.COMPLEX_TYPE)
                    .relation(SchemaConstants.ORG_DEFAULT)
                .find()
                    .assertDescription("Any user can have business role (can be a member).")
                    .assertArchetypeOid(ARCHETYPE_BUSINESS_ROLE_OID)
                    .assertObjectType(RoleType.COMPLEX_TYPE)
                    .end()
                .by()
                    .targetType(RoleType.COMPLEX_TYPE)
                    .relations(SchemaConstants.ORG_OWNER, SchemaConstants.ORG_APPROVER)
                .find()
                    .assertDescription("Only employees may be owners/approvers for business role.")
                    .assertArchetypeOid(ARCHETYPE_BUSINESS_ROLE_OID)
                    .assertObjectType(RoleType.COMPLEX_TYPE);
        // @formatter:on
    }

    @Test(enabled = false) // Disabled, as test110 is disabled as well.
    public void test115UnassignJackRoleEmployeeBase() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        unassignRole(USER_JACK_OID, ROLE_EMPLOYEE_BASE_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        // @formatter:off
        PrismObject<UserType> userAfter = assertUserAfter(USER_JACK_OID)
            .assignments()
                .assertAssignments(0)
                .end()
            .assertNoArchetypeRef()
            .roleMembershipRefs()
                .assertRoleMemberhipRefs(0)
                .end()
            .getObject();

        // Archetype policy derived from system config (global)
        assertArchetypePolicy(userAfter)
            .assertNoDisplay()
            .assertObjectTemplate(USER_TEMPLATE_ARCHETYPES_GLOBAL_OID);

        assertAssignmentTargetSpecification(userAfter)
            .assignmentObjectRelations()
                .assertItems(1)
                .by()
                    .targetType(RoleType.COMPLEX_TYPE)
                    .relation(SchemaConstants.ORG_DEFAULT)
                .find()
                    .assertDescription("Any user can have business role (can be a member).")
                    .assertArchetypeOid(ARCHETYPE_BUSINESS_ROLE_OID)
                    .assertObjectType(RoleType.COMPLEX_TYPE);
        // @formatter:on
    }

    @Test
    public void test120AssignJackArchetypeTest() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        assignArchetype(USER_JACK_OID, ARCHETYPE_TEST_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        // @formatter:off
        assertUserAfter(USER_JACK_OID)
            .assignments()
                .assertAssignments(1)
                .assertArchetype(ARCHETYPE_TEST_OID)
                .end()
            .assertArchetypeRef(ARCHETYPE_TEST_OID)
            .roleMembershipRefs()
                .assertRoleMemberhipRefs(1)
                .assertArchetype(ARCHETYPE_TEST_OID)
                .end()
            .singleLink()
                .target()
                    .assertResource(RESOURCE_DUMMY_OID)
                    .assertKind(ShadowKindType.ACCOUNT)
                    .assertIntent(INTENT_TEST)
                    .end()
                .end();
        // @formatter:on
    }

    /**
     * should fail because we try to assign two structural archetypes which is not supported
     */
    @Test
    public void test121AssignJackArchetypeEmployee() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        try {
            assignArchetype(USER_JACK_OID, ARCHETYPE_EMPLOYEE_OID, task, result);
            fail("Unexpected behavior, two structural archetypes assigned without failure");
        } catch (SchemaException e) {
            //This is expected
        }

        // THEN
        then();
        assertFailure(result);

        // @formatter:off
        assertUserAfter(USER_JACK_OID)
                .assignments()
                    .assertAssignments(1)
                    .assertArchetype(ARCHETYPE_TEST_OID)
                    .end()
                .assertArchetypeRef(ARCHETYPE_TEST_OID)
                .roleMembershipRefs()
                    .assertRoleMemberhipRefs(1)
                    .assertArchetype(ARCHETYPE_TEST_OID)
                    .end()
                .singleLink()
                    .target()
                        .assertResource(RESOURCE_DUMMY_OID)
                        .assertKind(ShadowKindType.ACCOUNT)
                        .assertIntent(INTENT_TEST)
                        .end()
                    .end();
        // @formatter:on
    }


    @Test
    public void test129UnassignJackArchetypeTest() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        unassignArchetype(USER_JACK_OID, ARCHETYPE_TEST_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        // @formatter:off
        assertUserAfter(USER_JACK_OID)
            .assignments()
                .assertAssignments(0)
                .end()
            .assertNoArchetypeRef()
            .roleMembershipRefs()
                .assertRoleMemberhipRefs(0)
                .end()
            .links()
                .assertNoLiveLinks();
        // @formatter:on
    }

    /**
     * Contractor archetype has a different object template.
     */
    @Test
    public void test130AssignJackArchetypeContractor() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        assignArchetype(USER_JACK_OID, ARCHETYPE_CONTRACTOR_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        // @formatter:off
        assertUserAfter(USER_JACK_OID)
            .assignments()
                .assertAssignments(1)
                .assertArchetype(ARCHETYPE_CONTRACTOR_OID)
                .end()
            .assertArchetypeRef(ARCHETYPE_CONTRACTOR_OID)
            .roleMembershipRefs()
                .assertRoleMemberhipRefs(1)
                .assertArchetype(ARCHETYPE_CONTRACTOR_OID)
                .end()
            .assertEmployeeNumber(CONTRACTOR_EMPLOYEE_NUMBER);
        // @formatter:on
    }

    /**
     * Make sure everything is nice and steady and the archetype template is still applied.
     */
    @Test
    public void test132JackContractorRecompute() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        recomputeUser(USER_JACK_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        // @formatter:off
        assertUserAfter(USER_JACK_OID)
            .assignments()
                .assertAssignments(1)
                .assertArchetype(ARCHETYPE_CONTRACTOR_OID)
                .end()
            .assertArchetypeRef(ARCHETYPE_CONTRACTOR_OID)
            .roleMembershipRefs()
                .assertRoleMemberhipRefs(1)
                .assertArchetype(ARCHETYPE_CONTRACTOR_OID)
                .end()
            .assertEmployeeNumber(CONTRACTOR_EMPLOYEE_NUMBER);
        // @formatter:on
    }

    @Test
    public void test133AssignJackAuxiliaryManagerArchetype() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        assignArchetype(USER_JACK_OID, ARCHETYPE_AUXILIARY_MANAGER_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        UserAsserter<Void> jackAsserter = assertUserAfter(USER_JACK_OID);

        // @formatter:off
        jackAsserter
                .assignments()
                .assertAssignments(2)
                    .assertArchetype(ARCHETYPE_CONTRACTOR_OID)
                    .assertArchetype(ARCHETYPE_AUXILIARY_MANAGER_OID)
                .end()
                .assertArchetypeRefs(2)
                    .assertHasArchetype(ARCHETYPE_CONTRACTOR_OID)
                    .assertHasArchetype(ARCHETYPE_AUXILIARY_MANAGER_OID)
                .roleMembershipRefs()
                    .assertRoleMemberhipRefs(2)
                .end()
                .assertEmployeeNumber(CONTRACTOR_EMPLOYEE_NUMBER)
                .assertCostCenter("1234");

        PrismObject<UserType> userJackAfter = jackAsserter.getObject();
        assertArchetypePolicy(userJackAfter).display()
                .displayType()
                    .assertLabel("Contractor")
                .end()
                .adminGuiConfig()
                .objectDetails()
                    .assertType(UserType.COMPLEX_TYPE)
                    .panel()
                        .byIdentifier("main")
                        .visibility(UserInterfaceElementVisibilityType.HIDDEN)
                    .end();
        // @formatter:on
    }

    @Test
    public void test134UnassignJackArchetypeAuxiliaryManager() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        unassignArchetype(USER_JACK_OID, ARCHETYPE_AUXILIARY_MANAGER_OID, task, result);

        then();
        assertSuccess(result);

        // @formatter:off
        assertUserAfter(USER_JACK_OID)
                .assignments()
                    .assertAssignments(1)
                .end()
                .assertArchetypeRef(ARCHETYPE_CONTRACTOR_OID)
                .assertRoleMemberhipRefs(1)
                .end();
        // @formatter:on
    }

    /**
     * Contractor archetype has a different object template. We unassign the archetype
     * now. But the employeeNumber given by the template stays. There is no reason to change it.
     */
    @Test
    public void test135UnassignJackArchetypeContractor() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        unassignArchetype(USER_JACK_OID, ARCHETYPE_CONTRACTOR_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        // @formatter:off
        assertUserAfter(USER_JACK_OID)
        .assignments()
            .assertAssignments(0)
            .end()
        .assertNoArchetypeRef()
        .roleMembershipRefs()
            .assertRoleMemberhipRefs(0)
            .end()
        .links()
            .assertNoLiveLinks()
            .end()
        .assertEmployeeNumber(CONTRACTOR_EMPLOYEE_NUMBER);
        // @formatter:on
    }

    /**
     * Change employeeNumber and recompute. As the contractor template should no longer
     * be applied, the employee number should not be forced back to contractor.
     */
    @Test
    public void test137JackEmpnoAndRecompute() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        modifyUserReplace(USER_JACK_OID, UserType.F_EMPLOYEE_NUMBER, task, result, "Number ONE");

        // WHEN
        when();

        recomputeUser(USER_JACK_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        // @formatter:off
        assertUserAfter(USER_JACK_OID)
        .assignments()
            .assertAssignments(0)
            .end()
        .assertNoArchetypeRef()
        .roleMembershipRefs()
            .assertRoleMemberhipRefs(0)
            .end()
        .links()
            .assertNoLiveLinks()
            .end()
        .assertEmployeeNumber("Number ONE");
        // @formatter:on
    }

    /**
     * Meathook is a contractor. Contractor archetype has a different object template.
     * This template should be applied when adding the object.
     */
    @Test
    public void test140AddMeathookContractor() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        addObject(USER_MEATHOOK_FILE, task, result);

        // THEN
        then();
        assertSuccess(result);

        // @formatter:off
        assertUserAfter(USER_MEATHOOK_OID)
            .assignments()
                .assertAssignments(1)
                .assertArchetype(ARCHETYPE_CONTRACTOR_OID)
                .end()
            .assertArchetypeRef(ARCHETYPE_CONTRACTOR_OID)
            .roleMembershipRefs()
                .assertRoleMemberhipRefs(1)
                .assertArchetype(ARCHETYPE_CONTRACTOR_OID)
                .end()
            .assertEmployeeNumber(CONTRACTOR_EMPLOYEE_NUMBER);
        // @formatter:on
    }

    /**
     * Add "Wanabe" user with an contractor archetype, but in DRAFT lifecycle state.
     * Even though assignments are not active in draft state, archetype assignment should be active anyway.
     * MID-5583
     */
    @Test
    public void test150AddWannabe() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        addObject(USER_WANNABE_FILE, task, result);

        // THEN
        then();
        assertSuccess(result);

        // @formatter:off
        assertUserAfter(USER_WANNABE_OID)
                .assertLifecycleState(SchemaConstants.LIFECYCLE_DRAFT)
                .assignments()
                    .assertAssignments(1)
                    .assertArchetype(ARCHETYPE_CONTRACTOR_OID)
                    .end()
                .assertArchetypeRef(ARCHETYPE_CONTRACTOR_OID)
                .roleMembershipRefs()
                    .assertRoleMemberhipRefs(1)
                    .assertArchetype(ARCHETYPE_CONTRACTOR_OID)
                    .end()
                .assertEmployeeNumber(CONTRACTOR_EMPLOYEE_NUMBER);
        // @formatter:on
    }

    /**
     * Add "Self Made Man" user with an archetypeRef. We usually do not allow archetypeRef. But in this case the ref
     * matches the assignment. We want to allow this. If we do not allow this, then we cannot re-import a "made" user.
     * MID-5909
     */
    @Test
    public void test160AddSelfMadeMan() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        addObject(USER_SELF_MADE_MAN_FILE, task, result);

        // THEN
        then();
        assertSuccess(result);

        // @formatter:off
        assertUserAfter(USER_SELF_MADE_MAN_OID)
                .assignments()
                    .assertAssignments(1)
                    .assertArchetype(ARCHETYPE_CONTRACTOR_OID)
                    .end()
                .assertArchetypeRef(ARCHETYPE_CONTRACTOR_OID)
                .roleMembershipRefs()
                    .assertRoleMemberhipRefs(1)
                    .assertArchetype(ARCHETYPE_CONTRACTOR_OID)
                    .end();
        // @formatter:on
    }

    /**
     * Add "fraudster" user with an archetypeRef. In this case the archetypeRef does not match the assignment.
     * This operation should be denied.
     * MID-5909
     */
    @Test
    public void test162AddFraudster() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // precondition
        assertNoObject(UserType.class, USER_FRAUDSTER_OID);

        try {
            // WHEN
            when();

            addObject(USER_FRAUDSTER_FILE, task, result);

            assertNotReached();
        } catch (PolicyViolationException e) {
            displayExpectedException(e);
        }

        // THEN
        then();
        assertFailure(result);

        assertNoObject(UserType.class, USER_FRAUDSTER_OID);
    }

    @Test
    public void test200AssignJackBarbossaArchetypeEmployee() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        assignArchetype(USER_JACK_OID, ARCHETYPE_EMPLOYEE_OID, task, result);
        assignArchetype(USER_BARBOSSA_OID, ARCHETYPE_EMPLOYEE_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertUserAfter(USER_JACK_OID)
            .assertArchetypeRef(ARCHETYPE_EMPLOYEE_OID);

        assertUserAfter(USER_BARBOSSA_OID)
            .assertArchetypeRef(ARCHETYPE_EMPLOYEE_OID);
    }

    @Test
    public void test202GetGuybrushCompiledGuiProfileActiveEmployeesView() throws Exception {
        // GIVEN
        login(USER_GUYBRUSH_USERNAME);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        CompiledGuiProfile compiledGuiProfile = modelInteractionService.getCompiledGuiProfile(task, result);

        // THEN
        assertSuccess(result);

        loginAdministrator();

        // @formatter:off
        ObjectFilter activeEmployeesViewFilter = assertCompiledGuiProfile(compiledGuiProfile)
            .objectCollectionViews()
                .assertViews(3)
                .by()
                    .identifier(VIEW_ACTIVE_EMPLOYEES_IDENTIFIER)
                .find()
                    .assertName(VIEW_ACTIVE_EMPLOYEES_IDENTIFIER)
                    .assertFilter()
                    .getFilter();
        // @formatter:on

        ObjectQuery viewQuery = prismContext.queryFactory().createQuery(activeEmployeesViewFilter, null);
        SearchResultList<PrismObject<UserType>> searchResults = modelService.searchObjects(UserType.class, viewQuery, null, task, result);

        display("Search results", searchResults);
        assertEquals("Wrong number of search results", 2, searchResults.size());

    }

    @Test
    public void test203DisableBarbossa() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        modifyUserReplace(USER_BARBOSSA_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.DISABLED);

        // THEN
        then();
        assertSuccess(result);

        assertUserAfter(USER_JACK_OID)
            .assertArchetypeRef(ARCHETYPE_EMPLOYEE_OID);

        assertUserAfter(USER_BARBOSSA_OID)
            .assertArchetypeRef(ARCHETYPE_EMPLOYEE_OID);

    }

    @Test
    public void test205GetGuybrushCompiledGuiProfileActiveEmployeesView() throws Exception {
        // GIVEN
        login(USER_GUYBRUSH_USERNAME);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        CompiledGuiProfile compiledGuiProfile = modelInteractionService.getCompiledGuiProfile(task, result);

        // THEN
        assertSuccess(result);

        loginAdministrator();

        // @formatter:off
        ObjectFilter activeEmployeesViewFilter = assertCompiledGuiProfile(compiledGuiProfile)
            .objectCollectionViews()
                .assertViews(3)
                .by()
                    .identifier(VIEW_ACTIVE_EMPLOYEES_IDENTIFIER)
                .find()
                    .assertName(VIEW_ACTIVE_EMPLOYEES_IDENTIFIER)
                    .assertFilter()
                    .getFilter();
        // @formatter:on

        ObjectQuery viewQuery = prismContext.queryFactory().createQuery(activeEmployeesViewFilter, null);
        SearchResultList<PrismObject<UserType>> searchResults = modelService.searchObjects(UserType.class, viewQuery, null, task, result);

        display("Search results", searchResults);
        assertEquals("Wrong number of search results", 1, searchResults.size());
    }

    /**
     * Jack is an `Employee`. When we try to assign `Contractor` archetype, it should fail.
     */
    @Test
    public void test300AssignJackContractorArchetypeFail() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("assigning Contractor archetype");

        try {
            assignArchetype(USER_JACK_OID, ARCHETYPE_CONTRACTOR_OID, task, result);
            assertNotReached();
        } catch (SchemaException e) {
            displayExpectedException(e);
        }

        then("it failed, and the Contractor archetype is not assigned");
        assertUserAfter(USER_JACK_OID).assignments()
                .by()
                    .targetOid(ARCHETYPE_CONTRACTOR_OID)
                    .targetType(ArchetypeType.COMPLEX_TYPE)
                    .assertNone();
    }

    /**
     * Student John Smith is created manually.
     *
     * The {@link MidpointFunctions#hasArchetype(ObjectType, String)} should return true even in the first
     * projector run, so the lab account should be created.
     *
     * MID-7694
     */
    @Test
    public void test400CreateJohnManually() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("creating John Smith");
        UserType user = new UserType()
                .name("john")
                .fullName("John Smith")
                .beginAssignment()
                    .targetRef(ARCHETYPE_STUDENT.oid, ArchetypeType.COMPLEX_TYPE)
                .end();
        addObject(user, task, result);

        then("John has a lab account");
        assertSuccess(result);
        assertUserAfter(user.getOid())
                .assertLinks(1, 0);

        assertDummyAccountByUsername(RESOURCE_LAB.name, "john")
                .display();
    }

    /**
     * Student Mary Smith is created via import from the source.
     *
     * The {@link MidpointFunctions#hasArchetype(ObjectType, String)} should return true even in the first
     * projector run, so the lab account should be created.
     *
     * MID-7694
     */
    @Test
    public void test410ImportMary() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("Mary has an account on source");
        DummyAccount account = RESOURCE_SOURCE.controller.addAccount("mary");
        account.addAttributeValue(ATTR_STUDENT, "true");

        when("importing from the source");
        modelService.importFromResource(RESOURCE_SOURCE.oid, RI_ACCOUNT_OBJECT_CLASS, task, result);
        waitForTaskCloseOrSuspend(task.getOid(), 10000);

        then("Mary has a lab account");
        assertTask(task.getOid(), "after")
                .display()
                .assertClosed()
                .assertSuccess();

        // @formatter:off
        assertUserAfterByUsername("mary")
                .assignments()
                    .assertArchetype(ARCHETYPE_STUDENT.oid)
                    .assertRole(ROLE_LAB_ACCESS.oid)
                .end()
                .roleMembershipRefs()
                    .assertArchetype(ARCHETYPE_STUDENT.oid)
                    .assertRole(ROLE_LAB_ACCESS.oid)
                .end()
                .assertLinks(2, 0);
        // @formatter:on

        assertDummyAccountByUsername(RESOURCE_LAB.name, "mary")
                .display();
    }

    // TODO: object template in archetype
    // TODO: correct application of object template for new object (not yet stored)

    // TODO: assertArchetypeSpec() for new object (not yet stored)

    // TODO: assignmentRelation (assertArchetypeSpec)

    private void assertEditSchema(PrismObject<UserType> user) throws CommonException {
        PrismObjectDefinition<UserType> editDef = getEditObjectDefinition(user);

        // This has overridden lookup def in object template
         PrismPropertyDefinition<String> preferredLanguageDef = editDef.findPropertyDefinition(UserType.F_PREFERRED_LANGUAGE);
         assertNotNull("No definition for preferredLanguage in user", preferredLanguageDef);
         assertEquals("Wrong preferredLanguage displayName", "Language", preferredLanguageDef.getDisplayName());
         assertTrue("preferredLanguage not readable", preferredLanguageDef.canRead());
         PrismReferenceValue valueEnumerationRef = preferredLanguageDef.getValueEnumerationRef();
         assertNotNull("No valueEnumerationRef for preferredLanguage", valueEnumerationRef);
         assertEquals("Wrong valueEnumerationRef OID for preferredLanguage", LOOKUP_LANGUAGES_OID, valueEnumerationRef.getOid());
    }
}
