/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.impl.lens.projector.focus.AssignmentProcessor;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.test.asserter.UserAsserter;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import java.io.File;

/**
 *
 */
public class TestAssignedMappings extends AbstractLensTest {

    protected static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "lens/focusMappings");

    private static final TestResource ROLE_SIMPLE = new TestResource(TEST_DIR, "role-simple.xml", "de0c9c11-eb2e-4b6a-9200-a4306a5c8d6c");
    private static final TestResource USER_JIM = new TestResource(TEST_DIR, "user-jim.xml", "6ce717d6-414f-4d91-948a-923f02191399");
    
    private static final TestResource ROLE_ORGANIZER = new TestResource(TEST_DIR, "role-organizer.xml", "7d31a3c2-cecf-4e3d-8740-988b37848a7c");
    private static final TestResource USER_ADAM = new TestResource(TEST_DIR, "user-adam.xml", "cf10f112-a731-45cd-8dfb-1b3fe9375c14");

    private static final TestResource USER_FRODO = new TestResource(TEST_DIR, "user-frodo.xml", "786919b7-23c9-4a38-90e7-5a1efd0ab853");
    private static final TestResource ROLE_BEARABLE = new TestResource(TEST_DIR, "metarole-bearable.xml", "2421b2c5-8563-4ba7-9a87-f9ef4b169620");
    private static final TestResource SERVICE_RING = new TestResource(TEST_DIR, "service-ring.xml", "7540cf28-a143-4eea-9379-75cae5d212cb");
    private static final TestResource SERVICE_STING = new TestResource(TEST_DIR, "service-sting.xml", "f8b109bf-d393-47b9-8eec-41d74e39a992");
    private static final TestResource ROLE_PROPAGATOR = new TestResource(TEST_DIR, "role-propagator.xml", "8e56a98a-bcb9-4178-94f2-5488da473132");

    @Autowired private AssignmentProcessor assignmentProcessor;
    @Autowired private Clock clock;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAdd(USER_JIM, initResult);
        repoAdd(ROLE_SIMPLE, initResult);

        repoAdd(ROLE_ORGANIZER, initResult);
        addObject(USER_ADAM, initTask, initResult);

        repoAdd(ROLE_BEARABLE, initResult);
        repoAdd(SERVICE_RING, initResult);
        repoAdd(SERVICE_STING, initResult);
        repoAdd(ROLE_PROPAGATOR, initResult);
        repoAdd(USER_FRODO, initResult);
    }

    /**
     * Assign "simple" role to jim.
     * Focus mappings should be applied in correct order: name -> fullName -> description -> title -> honorificPrefix.
     */
    @Test
    public void test100AssignSimpleToJim() throws Exception {
    	final String TEST_NAME = "test100AssignSimpleToJack";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestAssignedMappings.class.getName() + "." + TEST_NAME);
        setModelLoggingTracing(task);
        OperationResult result = task.getResult();

        // WHEN
        assignRole(USER_JIM.oid, ROLE_SIMPLE.oid, task, result);

        // THEN
        assertSuccess(result);
        PrismObject<UserType> jimAfter = getUserFromRepo(USER_JIM.oid);
        UserType jimAfterBean = jimAfter.asObjectable();
        System.out.println("name = " + jimAfterBean.getName());
        System.out.println("fullName = " + jimAfterBean.getFullName());
        System.out.println("description = " + jimAfterBean.getDescription());
        System.out.println("title = " + jimAfterBean.getTitle());
        System.out.println("honorificPrefix = " + jimAfterBean.getHonorificPrefix());
        new UserAsserter<>(jimAfter)
                .display()
                .assertName("jim")
                .assertFullName("jim")
                .assertDescription("jim")
                .assertTitle("jim")
                .assertPolyStringProperty(UserType.F_HONORIFIC_PREFIX, "jim");
    }

    /**
     * Unassign "organizer" role from adam.
     * This is to test chaining in the negative case when combined with change of source value.
     */
    @Test
    public void test150UnassignOrganizerFromAdam() throws Exception {
    	final String TEST_NAME = "test150UnassignOrganizerFromAdam";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestAssignedMappings.class.getName() + "." + TEST_NAME);
        setModelLoggingTracing(task);
        OperationResult result = task.getResult();

        PrismObject<UserType> adamBefore = getUser(USER_ADAM.oid);
        new UserAsserter<>(adamBefore)
                .display("adam before")
                .assertAssignments(1);

        // WHEN
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_DESCRIPTION).replace("troublemaker")
                .item(UserType.F_ASSIGNMENT).delete(adamBefore.asObjectable().getAssignment().get(0).asPrismContainerValue().clone())
                .asObjectDeltaCast(USER_ADAM.oid);

        executeChanges(delta, null, task, result);

        // THEN
        assertSuccess(result);
        PrismObject<UserType> adamAfter = getUserFromRepo(USER_ADAM.oid);
        UserType adamAfterBean = adamAfter.asObjectable();
        System.out.println("name = " + adamAfterBean.getName());
        System.out.println("description = " + adamAfterBean.getDescription());
        System.out.println("organization = " + adamAfterBean.getOrganization());
        System.out.println("organizationalUnit = " + adamAfterBean.getOrganizationalUnit());
        new UserAsserter<>(adamAfter)
                .display()
                .assertName("adam")
                .assertDescription("troublemaker")
                .assertOrganizationalUnits()
                .assertOrganizations();
    }

    /**
     * Assign "ring" service to frodo.
     * Focus mappings should be applied in correct order: name -> fullName -> description -> title -> honorificPrefix.
     */
    @Test
    public void test200AssignRingToFrodo() throws Exception {
    	final String TEST_NAME = "test200AssignRingToFrodo";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestAssignedMappings.class.getName() + "." + TEST_NAME);
        setModelLoggingTracing(task);
        OperationResult result = task.getResult();

        // WHEN
        assignService(USER_FRODO.oid, SERVICE_RING.oid, task, result);

        // THEN
        assertSuccess(result);
        PrismObject<UserType> frodoAfter = getUserFromRepo(USER_FRODO.oid);
        UserType frodoAfterBean = frodoAfter.asObjectable();
        System.out.println("name = " + frodoAfterBean.getName());
        System.out.println("fullName = " + frodoAfterBean.getFullName());
        System.out.println("description = " + frodoAfterBean.getDescription());
        System.out.println("title = " + frodoAfterBean.getTitle());
        System.out.println("honorificPrefix = " + frodoAfterBean.getHonorificPrefix());
        new UserAsserter<>(frodoAfter)
                .display()
                .assertName("frodo")
                .assertTitle("ring-bearer")
                .assertPolyStringProperty(UserType.F_HONORIFIC_PREFIX, "ring-bearer")
                .assertFullName("frodo, the ring-bearer")
                .assertDescription("frodo, the ring-bearer");
    }
}
