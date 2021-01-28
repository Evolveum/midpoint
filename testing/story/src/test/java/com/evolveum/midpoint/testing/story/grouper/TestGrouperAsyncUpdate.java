/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story.grouper;

import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.asserter.ShadowAttributesAsserter;
import com.evolveum.midpoint.test.asserter.prism.PrismPropertyAsserter;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import static org.testng.AssertJUnit.assertNotNull;

/**
 * Test for asynchronous Grouper->midPoint interface (demo/grouper in Internet2 scenario).
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestGrouperAsyncUpdate extends AbstractGrouperTest {

    private static final File CHANGE_110 = new File(TEST_DIR, "change-110-alumni-add.json");
    private static final File CHANGE_115 = new File(TEST_DIR, "change-115-staff-add.json");
    private static final File CHANGE_200 = new File(TEST_DIR, "change-200-banderson-add-alumni.json");
    private static final File CHANGE_210 = new File(TEST_DIR, "change-210-banderson-add-staff.json");
    private static final File CHANGE_220 = new File(TEST_DIR, "change-220-jlewis685-add-alumni.json");
    private static final File CHANGE_221 = new File(TEST_DIR, "change-221-jlewis685-add-staff.json");
    private static final File CHANGE_230 = new File(TEST_DIR, "change-230-nobody-add-alumni.json");
    private static final File CHANGE_250 = new File(TEST_DIR, "change-250-banderson-delete-alumni.json");
    private static final File CHANGE_305 = new File(TEST_DIR, "change-305-staff-rename.json");
    private static final File CHANGE_310 = new File(TEST_DIR, "change-310-staff-delete.json");

    private String orgAlumniOid;
    private String orgStaffOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
//        setGlobalTracingOverride(createModelAndProvisioningLoggingTracingProfile());
    }

//    @Override
//    protected TracingProfileType getTestMethodTracingProfile() {
//        return createModelAndProvisioningLoggingTracingProfile()
//                .fileNamePattern(TEST_METHOD_TRACING_FILENAME_PATTERN);
//    }

    @Test
    public void test000Sanity() throws Exception {
        Task task = getTestTask();

        assertSuccess(modelService.testResource(RESOURCE_LDAP.oid, task));
        assertSuccess(modelService.testResource(RESOURCE_GROUPER.oid, task));
    }

    @Test
    public void test010CreateUsers() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        addObject(USER_BANDERSON, task, result);
        addObject(USER_JLEWIS685, task, result);

        assertSuccess(result);

        assertNotNull("no LDAP entry for banderson", openDJController.fetchEntry(DN_BANDERSON));
        assertNotNull("no LDAP entry for jlewis685", openDJController.fetchEntry(DN_JLEWIS685));
    }

    /**
     * GROUP_ADD event for ref:affiliation:alumni.
     */
    @Test
    public void test110AddAlumni() throws Exception {
        Task task = getTestTask();
        task.setChannel(SchemaConstants.CHANNEL_ASYNC_UPDATE_URI);
        OperationResult result = getTestOperationResult();

        // GIVEN

        MockAsyncUpdateSource.INSTANCE.reset();
        MockAsyncUpdateSource.INSTANCE.prepareMessage(getAmqp091Message(CHANGE_110));
        grouperDummyResource.addGroup(createGroup(ALUMNI_ID, ALUMNI_NAME));

        // WHEN

        ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_GROUPER.oid);
        processUpdates(task, result, coords);

        // THEN

        assertSuccess(result);

        assertMembers(ALUMNI_NAME, task, result);

        orgAlumniOid = assertOrgByName("affiliation_alumni", "alumni after")
                .display()
                .assertLifecycleState("active")
                .extension()
                        .property(EXT_GROUPER_NAME).singleValue().assertValue(ALUMNI_NAME).end().end()
                        .property(EXT_LDAP_DN).singleValue().assertValue(DN_ALUMNI).end().end()
                .end()
                .assertAssignments(1)           // archetype, todo assert target
                .assertDisplayName("Affiliation: alumni")
                .assertIdentifier("alumni")
                .assertLinks(2)                // todo assert details
                .links()
                    .projectionOnResource(RESOURCE_GROUPER.oid)
                        .target()
                            .assertNotDead()
                        .end()
                    .end()
                    .projectionOnResource(RESOURCE_LDAP.oid)
                        .target()
                            .assertNotDead()
                        .end()
                    .end()
                .end()
                .getOid();
    }

    private void processUpdates(Task task, OperationResult result, ResourceShadowDiscriminator coords)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        throw new UnsupportedOperationException();
        //provisioningService.processAsynchronousUpdates(coords, task, result);
    }

    /**
     * GROUP_ADD event for ref:affiliation:staff.
     */
    @Test
    public void test115AddStaff() throws Exception {
        Task task = getTestTask();
        task.setChannel(SchemaConstants.CHANNEL_ASYNC_UPDATE_URI);
        OperationResult result = getTestOperationResult();

        // GIVEN

        MockAsyncUpdateSource.INSTANCE.reset();
        MockAsyncUpdateSource.INSTANCE.prepareMessage(getAmqp091Message(CHANGE_115));
        grouperDummyResource.addGroup(createGroup(STAFF_ID, STAFF_NAME));
        // WHEN

        ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_GROUPER.oid);
        processUpdates(task, result, coords);

        // THEN

        assertSuccess(result);

        assertMembers(STAFF_NAME, task, result);

        orgStaffOid = assertOrgByName("affiliation_staff", "staff after")
                .display()
                .assertLifecycleState("active")
                .extension()
                        .property(EXT_GROUPER_NAME).singleValue().assertValue(STAFF_NAME).end().end()
                        .property(EXT_LDAP_DN).singleValue().assertValue(DN_STAFF).end().end()
                .end()
                .assertAssignments(1)           // archetype, todo assert target
                .assertDisplayName("Affiliation: staff")
                .assertIdentifier("staff")
                .assertLinks(2)                // todo assert details
                .links()
                    .projectionOnResource(RESOURCE_GROUPER.oid)
                        .target()
                            .assertNotDead()
                        .end()
                    .end()
                    .projectionOnResource(RESOURCE_LDAP.oid)
                        .target()
                            .assertNotDead()
                        .end()
                    .end()
                .end()
                .getOid();
    }

    /**
     * Adding ref:affiliation:alumni membership for banderson.
     */
    @Test
    public void test200AddAlumniForAnderson() throws Exception {
        Task task = getTestTask();
        task.setChannel(SchemaConstants.CHANNEL_ASYNC_UPDATE_URI);
        OperationResult result = getTestOperationResult();

        // GIVEN

        MockAsyncUpdateSource.INSTANCE.reset();
        MockAsyncUpdateSource.INSTANCE.prepareMessage(getAmqp091Message(CHANGE_200));
        grouperDummyResource.getGroupByName(ALUMNI_NAME).addMember(BANDERSON_USERNAME);

        rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);

        // WHEN

        ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_GROUPER.oid);
        processUpdates(task, result, coords);

        // THEN

        assertSuccess(result);

        assertMembers(ALUMNI_NAME, task, result, BANDERSON_USERNAME);

        assertUserAfterByUsername(BANDERSON_USERNAME)
                .triggers()
                .assertTriggers(1);

        // Async update is not counted as a connector operation (at least not now). We should have no other ops,
        // in particular we do NOT want the clockwork to run! (MID-5853)
        assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, 0);
    }

    /**
     * Anderson should obtain the assignment.
     */
    @Test
    public void test202RecomputeAnderson() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN

        recomputeUser(USER_BANDERSON.oid, task, result);

        // THEN

        assertSuccess(result);

        assertUserAfterByUsername(BANDERSON_USERNAME)
                .assignments()
                .assertAssignments(2)
                    .assertRole(ROLE_LDAP_BASIC.oid)
                    .assertOrg(orgAlumniOid)
                .end()
                .links()
                    .assertLinks(1)
                    .projectionOnResource(resourceLdap.getOid());

        openDJController.assertUniqueMember(DN_ALUMNI, DN_BANDERSON);
    }

    /**
     * Adding ref:affiliation:staff membership for banderson.
     */
    @Test
    public void test210AddStaffForAnderson() throws Exception {
        Task task = getTestTask();
        task.setChannel(SchemaConstants.CHANNEL_ASYNC_UPDATE_URI);
        OperationResult result = getTestOperationResult();

        // GIVEN

        MockAsyncUpdateSource.INSTANCE.reset();
        MockAsyncUpdateSource.INSTANCE.prepareMessage(getAmqp091Message(CHANGE_210));
        grouperDummyResource.getGroupByName(STAFF_NAME).addMember(BANDERSON_USERNAME);

        // WHEN

        ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_GROUPER.oid);
        processUpdates(task, result, coords);

        // THEN

        assertSuccess(result);

        assertMembers(ALUMNI_NAME, task, result, BANDERSON_USERNAME);
        assertMembers(STAFF_NAME, task, result, BANDERSON_USERNAME);

        assertUserAfterByUsername(BANDERSON_USERNAME)
                .triggers()
                .assertTriggers(1);
    }

    /**
     * Anderson should obtain the second assignment.
     */
    @Test
    public void test212RecomputeAnderson() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN

        recomputeUser(USER_BANDERSON.oid, task, result);

        // THEN

        assertSuccess(result);

        assertUserAfterByUsername(BANDERSON_USERNAME)
                .assignments()
                .assertAssignments(3)
                    .assertRole(ROLE_LDAP_BASIC.oid)
                    .assertOrg(orgAlumniOid)
                    .assertOrg(orgStaffOid)
                .end()
                .links()
                    .assertLinks(1)
                    .projectionOnResource(resourceLdap.getOid());

        openDJController.assertUniqueMember(DN_ALUMNI, DN_BANDERSON);
        openDJController.assertUniqueMember(DN_STAFF, DN_BANDERSON);
    }


    /**
     * Adding ref:affiliation:alumni membership for jlewis685.
     */
    @Test
    public void test220AddAlumniForLewis() throws Exception {
        Task task = getTestTask();
        task.setChannel(SchemaConstants.CHANNEL_ASYNC_UPDATE_URI);
        OperationResult result = getTestOperationResult();

        // GIVEN

        MockAsyncUpdateSource.INSTANCE.reset();
        MockAsyncUpdateSource.INSTANCE.prepareMessage(getAmqp091Message(CHANGE_220));
        grouperDummyResource.getGroupByName(ALUMNI_NAME).addMember(JLEWIS685_USERNAME);

        // WHEN

        ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_GROUPER.oid);
        processUpdates(task, result, coords);

        // THEN

        assertSuccess(result);

        assertMembers(ALUMNI_NAME, task, result, BANDERSON_USERNAME, JLEWIS685_USERNAME);

        assertUserAfterByUsername(JLEWIS685_USERNAME)
                .triggers()
                .assertTriggers(1);
    }

    /**
     * Adding ref:affiliation:staff membership for jlewis685.
     */
    @Test
    public void test221AddStaffForLewis() throws Exception {
        Task task = getTestTask();
        task.setChannel(SchemaConstants.CHANNEL_ASYNC_UPDATE_URI);
        OperationResult result = getTestOperationResult();

        // GIVEN

        MockAsyncUpdateSource.INSTANCE.reset();
        MockAsyncUpdateSource.INSTANCE.prepareMessage(getAmqp091Message(CHANGE_221));
        grouperDummyResource.getGroupByName(STAFF_NAME).addMember(JLEWIS685_USERNAME);

        // WHEN

        ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_GROUPER.oid);
        processUpdates(task, result, coords);

        // THEN

        assertSuccess(result);

        assertMembers(ALUMNI_NAME, task, result, BANDERSON_USERNAME, JLEWIS685_USERNAME);
        assertMembers(STAFF_NAME, task, result, BANDERSON_USERNAME, JLEWIS685_USERNAME);

        assertUserAfterByUsername(JLEWIS685_USERNAME)
                .triggers()
                .assertTriggers(1);
    }

    /**
     * Lewis should obtain two assignments.
     */
    @Test
    public void test222RecomputeLewis() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN

        recomputeUser(USER_JLEWIS685.oid, task, result);

        // THEN

        assertSuccess(result);

        assertUserAfterByUsername(JLEWIS685_USERNAME)
                .assignments()
                    .assertAssignments(3)
                    .assertRole(ROLE_LDAP_BASIC.oid)
                    .assertOrg(orgAlumniOid)
                    .assertOrg(orgStaffOid)
                .end()
                .links()
                    .assertLinks(1)
                    .projectionOnResource(resourceLdap.getOid());

        openDJController.assertUniqueMember(DN_ALUMNI, DN_JLEWIS685);
        openDJController.assertUniqueMember(DN_STAFF, DN_JLEWIS685);
    }

    /**
     * Adding ref:affiliation:alumni membership for non-existing user (nobody).
     */
    @Test
    public void test230AddAlumniForNobody() throws Exception {
        Task task = getTestTask();
        task.setChannel(SchemaConstants.CHANNEL_ASYNC_UPDATE_URI);
        OperationResult result = getTestOperationResult();

        // GIVEN

        MockAsyncUpdateSource.INSTANCE.reset();
        MockAsyncUpdateSource.INSTANCE.prepareMessage(getAmqp091Message(CHANGE_230));
        grouperDummyResource.getGroupByName(ALUMNI_NAME).addMember(NOBODY_USERNAME);

        // WHEN

        ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_GROUPER.oid);
        processUpdates(task, result, coords);

        // THEN

        assertSuccess(result);

        assertMembers(ALUMNI_NAME, task, result, BANDERSON_USERNAME, JLEWIS685_USERNAME, NOBODY_USERNAME);
    }

    /**
     * Deleting ref:affiliation:alumni membership for banderson.
     */
    @Test
    public void test250DeleteAlumniForAnderson() throws Exception {
        Task task = getTestTask();
        task.setChannel(SchemaConstants.CHANNEL_ASYNC_UPDATE_URI);
        OperationResult result = getTestOperationResult();

        // GIVEN

        MockAsyncUpdateSource.INSTANCE.reset();
        MockAsyncUpdateSource.INSTANCE.prepareMessage(getAmqp091Message(CHANGE_250));
        grouperDummyResource.getGroupByName(ALUMNI_NAME).removeMember(BANDERSON_USERNAME);

        // WHEN

        ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_GROUPER.oid);
        processUpdates(task, result, coords);

        // THEN

        assertSuccess(result);

        assertMembers(ALUMNI_NAME, task, result, JLEWIS685_USERNAME, NOBODY_USERNAME);

        assertUserAfterByUsername(BANDERSON_USERNAME)
                .triggers()
                .assertTriggers(1);
    }

    /**
     * Anderson should lose the first assignment.
     */
    @Test
    public void test252RecomputeAnderson() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN

        recomputeUser(USER_BANDERSON.oid, task, result);

        // THEN

        assertSuccess(result);

        assertUserAfterByUsername(BANDERSON_USERNAME)
                .assignments()
                    .assertAssignments(2)
                    .assertRole(ROLE_LDAP_BASIC.oid)
                    .assertOrg(orgStaffOid)
                .end()
                .links()
                    .assertLinks(1)
                    .projectionOnResource(resourceLdap.getOid());

        openDJController.assertUniqueMember(DN_STAFF, DN_BANDERSON);
    }

    /**
     * Renaming ref:affiliation:staff group to ref:affiliation:staff2.
     */
    @Test
    public void test305RenameStaff() throws Exception {
        Task task = getTestTask();
        task.setChannel(SchemaConstants.CHANNEL_ASYNC_UPDATE_URI);
        OperationResult result = getTestOperationResult();

        // GIVEN

        MockAsyncUpdateSource.INSTANCE.reset();
        MockAsyncUpdateSource.INSTANCE.prepareMessage(getAmqp091Message(CHANGE_305));
        grouperDummyResource.renameGroup(STAFF_NAME, STAFF_NAME, STAFF2_NAME);

        executeChanges(deltaFor(UserType.class).item(UserType.F_TRIGGER).replace().asObjectDelta(USER_BANDERSON.oid), null, task, result);
        executeChanges(deltaFor(UserType.class).item(UserType.F_TRIGGER).replace().asObjectDelta(USER_JLEWIS685.oid), null, task, result);

        // WHEN

        ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_GROUPER.oid);
        processUpdates(task, result, coords);

        // THEN

        assertSuccess(result);

        assertOrgByName("affiliation_staff2", "staff2 after rename")
                .display()
                .assertLifecycleState("active")
                .extension()
                    .property(EXT_GROUPER_NAME).singleValue().assertValue(STAFF2_NAME).end().end()
                    .property(EXT_LDAP_DN).singleValue().assertValue(DN_STAFF2).end().end()
                .end()
                .assertAssignments(1)           // archetype, todo assert target
                    .assertDisplayName("Affiliation: staff2")
                    .assertIdentifier("staff2")
                .links()
                    .projectionOnResource(RESOURCE_GROUPER.oid)
                        .target()
                            .display()
                            .assertNotDead()
                        .end()
                    .end()
                    .projectionOnResource(RESOURCE_LDAP.oid)
                        .target()
                            .display()
                            .assertName(DN_STAFF2)
                            .assertNotDead()
                        .end()
                    .end()
                .end();

        openDJController.assertNoEntry(DN_STAFF);
        openDJController.assertUniqueMember(DN_STAFF2, DN_BANDERSON);
        openDJController.assertUniqueMember(DN_STAFF2, DN_JLEWIS685);
    }

    /**
     * Deleting ref:affiliation:staff2 group.
     */
    @Test
    public void test310DeleteStaff() throws Exception {
        Task task = getTestTask();
        task.setChannel(SchemaConstants.CHANNEL_ASYNC_UPDATE_URI);
        OperationResult result = getTestOperationResult();

        // GIVEN

        MockAsyncUpdateSource.INSTANCE.reset();
        MockAsyncUpdateSource.INSTANCE.prepareMessage(getAmqp091Message(CHANGE_310));
        grouperDummyResource.deleteGroupByName(STAFF2_NAME);

        executeChanges(deltaFor(UserType.class).item(UserType.F_TRIGGER).replace().asObjectDelta(USER_BANDERSON.oid), null, task, result);
        executeChanges(deltaFor(UserType.class).item(UserType.F_TRIGGER).replace().asObjectDelta(USER_JLEWIS685.oid), null, task, result);

        // WHEN

        ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_GROUPER.oid);
        processUpdates(task, result, coords);

        // THEN

        assertSuccess(result);

        assertOrgByName("affiliation_staff2", "staff2 after deletion")
                .display()
                .assertLifecycleState("retired")
                .extension()
                    .property(EXT_GROUPER_NAME).singleValue().assertValue(STAFF2_NAME).end().end()
                    .property(EXT_LDAP_DN).singleValue().assertValue(DN_STAFF2).end().end()
                .end()
                .assertAssignments(1)           // archetype, todo assert target
                    .assertDisplayName("Affiliation: staff2")
                    .assertIdentifier("staff2")
                .links()
                    .projectionOnResource(RESOURCE_GROUPER.oid)
                        .target()
                            .assertDead()
                        .end()
                    .end()
                    .projectionOnResource(RESOURCE_LDAP.oid)
                        .target()
                            .assertNotDead()
                        .end()
                    .end()
                .end();
    }

    /**
     * Completes the deletion of staff group.
     */
    @Test
    public void test312ScavengeGroups() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // GIVEN


        // WHEN

        rerunTask(TASK_GROUP_SCAVENGER.oid);

        // THEN

        assertSuccess(result);

        assertNoObject(OrgType.class, orgStaffOid, task, result);
        assertUserAfterByUsername(BANDERSON_USERNAME)
                .assignments()
                    .assertAssignments(1)
                    .assertRole(ROLE_LDAP_BASIC.oid)
                .end()
                .links()
                    .assertLinks(1)
                    .projectionOnResource(resourceLdap.getOid());

        assertUserAfterByUsername(JLEWIS685_USERNAME)
                .assignments()
                    .assertAssignments(2)
                    .assertRole(ROLE_LDAP_BASIC.oid)
                    .assertOrg(orgAlumniOid)
                .end()
                .links()
                    .assertLinks(1)
                    .projectionOnResource(resourceLdap.getOid());

        openDJController.assertNoEntry(DN_STAFF);
        openDJController.assertNoEntry(DN_STAFF2);

        openDJController.assertNoUniqueMember(DN_ALUMNI, DN_BANDERSON);
        openDJController.assertUniqueMember(DN_ALUMNI, DN_JLEWIS685);
    }

    private AsyncUpdateMessageType getAmqp091Message(File file) throws IOException {
        Amqp091MessageType rv = new Amqp091MessageType();
        String json = String.join("\n", IOUtils.readLines(new FileReader(file)));
        rv.setBody(json.getBytes(StandardCharsets.UTF_8));
        return rv;
    }

    @SuppressWarnings({ "SameParameterValue", "UnusedReturnValue" })
    private PrismPropertyAsserter<Object, ShadowAttributesAsserter<Void>> assertMembers(String groupName, Task task,
            OperationResult result, String... expectedUsers)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {

        PrismObject<ShadowType> shadowInRepo = findShadowByName(ShadowKindType.ENTITLEMENT, "group", groupName, resourceGrouper, result);
        assertNotNull("No shadow with name '"+groupName+"'", shadowInRepo);

        Collection<SelectorOptions<GetOperationOptions>> options =
                schemaHelper.getOperationOptionsBuilder()
                        .noFetch()
                        .retrieve()
                        .build();
        PrismObject<ShadowType> shadow = provisioningService
                .getObject(ShadowType.class, shadowInRepo.getOid(), options, task, result);

        return assertShadow(shadow, "after")
                .attributes()
                    .attribute(ATTR_MEMBER.getLocalPart())
                        .assertRealValues(expectedUsers);
    }

    @NotNull
    private DummyGroup createGroup(String id, String name) {
        DummyGroup group = new DummyGroup();
        group.setId(id);
        group.setName(name);
        return group;
    }
}
