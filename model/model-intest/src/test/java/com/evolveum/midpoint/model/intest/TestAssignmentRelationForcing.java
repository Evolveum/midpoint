/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest;

import java.io.File;
import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

/**
 * Replication of the original scenario of issue 11092 ("Duplicating assignments").
 *
 * The customer forces a custom "business member" relation on role-role assignments
 * using an authoritative strong object template mapping: the script runs in absolute
 * relativity mode over the whole 'assignment' item, source and target sets are
 * predefined=all, and assignments to roles carrying the default relation are
 * rewritten (cloneWithoutId + relation change) to the custom relation.
 *
 * On 4.8 the rewritten assignment effectively replaced the original one.
 * On 4.9+ the corrected assignment is added while the original org:default
 * assignment stays, so the role ends up with both - the reported duplication.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestAssignmentRelationForcing extends AbstractEmptyModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/relation-forcing");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final TestObject<ObjectTemplateType> TEMPLATE_REPRO_ROLE =
            TestObject.file(TEST_DIR, "object-template-repro-role.xml", "11092000-0000-0000-0001-000000000002");
    private static final TestObject<ArchetypeType> ARCHETYPE_REPRO_ROLE =
            TestObject.file(TEST_DIR, "archetype-repro-role.xml", "11092000-0000-0000-0001-000000000001");

    private static final String ATTR_CHILD_ROLES = "CHILD_ROLES";
    private static final String ATTR_ROLE_TYPE = "ROLE_TYPE";

    private static final DummyTestResource RESOURCE_DUMMY_REPRO = new DummyTestResource(
            TEST_DIR, "resource-dummy-repro.xml", "11092000-0000-0000-0001-000000000003", "repro",
            controller -> {
                controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                        ATTR_CHILD_ROLES, String.class, false, true);
                controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                        ATTR_ROLE_TYPE, String.class, false, false);
            });

    private static final String NS_BUSINESS_RELATION = "http://example.com/xml/ns/midpoint/relation";
    private static final QName RELATION_BUSINESS_MEMBER = new QName(NS_BUSINESS_RELATION, "member");

    private String parentRoleOid;
    private String childRoleOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        initTestObjects(initTask, initResult, TEMPLATE_REPRO_ROLE, ARCHETYPE_REPRO_ROLE);
        RESOURCE_DUMMY_REPRO.initAndTest(this, initTask, initResult);
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    /**
     * Create two archetyped roles, then assign the child role to the parent role
     * with no (= default) relation. The template mapping is expected to force the
     * business relation, REPLACING the default-relation assignment.
     */
    @Test
    public void test100AssignChildRoleWithDefaultRelation() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        RoleType parent = new RoleType()
                .name("Parent role")
                .beginAssignment()
                    .targetRef(ARCHETYPE_REPRO_ROLE.oid, ArchetypeType.COMPLEX_TYPE)
                .end();
        RoleType child = new RoleType()
                .name("Child role")
                .beginAssignment()
                    .targetRef(ARCHETYPE_REPRO_ROLE.oid, ArchetypeType.COMPLEX_TYPE)
                .end();
        parentRoleOid = addObject(parent.asPrismObject(), task, result);
        childRoleOid = addObject(child.asPrismObject(), task, result);

        when("child role is assigned to parent role with no (default) relation");
        AssignmentType assignmentToAdd = new AssignmentType()
                .targetRef(childRoleOid, RoleType.COMPLEX_TYPE); // no relation, as in the customer's scenario
        ObjectDelta<RoleType> delta = deltaFor(RoleType.class)
                .item(RoleType.F_ASSIGNMENT)
                .add(assignmentToAdd)
                .asObjectDelta(parentRoleOid);
        executeChanges(delta, null, task, result);

        then("parent role has only the archetype assignment and the corrected (business member) assignment");
        assertSuccess(result);
        assertRole(parentRoleOid, "after")
                .display()
                .assignments()
                .by()
                    .targetOid(childRoleOid)
                    .targetRelation(RELATION_BUSINESS_MEMBER)
                    .find()
                .end()
                // The org:default assignment must have been replaced, not kept.
                // On affected versions there are 3 assignments here.
                .assertAssignments(2);
    }
}
