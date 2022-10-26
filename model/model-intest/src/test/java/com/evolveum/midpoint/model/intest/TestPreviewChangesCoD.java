/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest;

import java.io.File;
import java.util.Arrays;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

/**
 * Created by Viliam Repan (lazyman).
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestPreviewChangesCoD extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/preview-cod");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final File OBJECT_TEMPLATE_ORG = new File(TEST_DIR, "object-template-org.xml");

    private static final File ORG_CHILD = new File(TEST_DIR, "org-child.xml");

    private static final File ROLE_META = new File(TEST_DIR, "meta-role.xml");

    private static final String ROLE_META_OID = "1ac00214-ffd0-49db-a1b9-51b46a0e9ae1";

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        addObject(OBJECT_TEMPLATE_ORG, initTask, initResult);
        addObject(ROLE_META, initTask, initResult);
    }

    @Test
    public void test100OrgNotProvisioned() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        final Integer ORG_COUNT = modelService.countObjects(OrgType.class, null, null, task, result);

        when();

        PrismObject<OrgType> orgChild = prismContext.parseObject(ORG_CHILD);
        ObjectDelta delta = orgChild.createAddDelta();

        ModelContext<OrgType> context = modelInteractionService.previewChanges(Arrays.asList(delta), ModelExecuteOptions.create(), task, result);

        then();

        AssertJUnit.assertNotNull(context);
        AssertJUnit.assertEquals("Orgs were created", ORG_COUNT, modelService.countObjects(OrgType.class, null, null, task, result));
    }

    @Test
    public void test150OrgNotProvisionedWithMetarole() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        final Integer ORG_COUNT = modelService.countObjects(OrgType.class, null, null, task, result);
        final Integer ROLE_COUNT = modelService.countObjects(RoleType.class, null, null, task, result);

        when();

        PrismObject<OrgType> orgChild = prismContext.parseObject(ORG_CHILD);
        // we'll add assignment to meta role
        orgChild.asObjectable().getAssignment().add(new AssignmentType().targetRef(ROLE_META_OID, RoleType.COMPLEX_TYPE));
        ObjectDelta delta = orgChild.createAddDelta();

        System.out.println(delta.debugDump());

        ModelContext<OrgType> context = modelInteractionService.previewChanges(Arrays.asList(delta), ModelExecuteOptions.create(), task, result);

        then();

        AssertJUnit.assertNotNull(context);
        AssertJUnit.assertEquals("Orgs were created", ORG_COUNT, modelService.countObjects(OrgType.class, null, null, task, result));
        AssertJUnit.assertEquals("Roles were created", ROLE_COUNT, modelService.countObjects(RoleType.class, null, null, task, result));
    }

    @Test
    public void test200EmptyOrgProvisioned() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();

        then();

    }

    @Test
    public void test300UserInOrgProvisioned() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();

        then();
    }

    @Test
    public void test400AssociationTargetSearch() throws Exception {

    }

    @Test
    public void test500ReferenceTargetSearch() throws Exception {

    }
}
