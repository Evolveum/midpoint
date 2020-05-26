/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import java.io.File;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;

import com.evolveum.midpoint.util.exception.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.namespace.QName;

/**
 * Various tests related to recomputation (or other treatment) of members of changed abstract roles.
 * See also https://wiki.evolveum.com/display/midPoint/Linked+objects.
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMemberRecompute extends AbstractEmptyModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/member-recompute");

    private static final String NS_LINKED = "http://midpoint.evolveum.com/xml/ns/samples/linked";
    private static final QName RECOMPUTE_MEMBERS_NAME = new QName(NS_LINKED, "recomputeMembers");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final TestResource<ObjectTemplateType> TEMPLATE_USER = new TestResource<>(TEST_DIR, "template-user.xml", "7d6bf307-58c2-4ea9-8599-19586623b41a");
    private static final TestResource<ArchetypeType> ARCHETYPE_DEPARTMENT = new TestResource<>(TEST_DIR, "archetype-department.xml", "b685545e-995f-45e0-8d32-92cd3781ef54");

    private static final TestResource<OrgType> ORG_DCS = new TestResource<>(TEST_DIR, "org-dcs.xml", "67720733-9de6-47da-b856-ce063c4a6659");
    private static final TestResource<OrgType> ORG_CC = new TestResource<>(TEST_DIR, "org-cc.xml", "08a8fe26-e8b6-4005-b23d-e7dc1472b209");
    private static final TestResource<OrgType> ORG_IT_STAFF = new TestResource<>(TEST_DIR, "org-it-staff.xml", "51726874-de60-42f1-aab4-a4afb0702833");

    @SuppressWarnings("FieldCanBeLocal") private final int DCS_USERS = 20;
    @SuppressWarnings("FieldCanBeLocal") private final int CC_USERS = 10;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        addObject(TEMPLATE_USER, initTask, initResult);
        addObject(ARCHETYPE_DEPARTMENT, initTask, initResult);
        addObject(ORG_DCS, initTask, initResult);
        addObject(ORG_CC, initTask, initResult);
        addObject(ORG_IT_STAFF, initTask, initResult);

        createUsers("user-dcs-%04d", DCS_USERS, initTask, initResult, ORG_DCS, ORG_IT_STAFF);
        createUsers("user-cc-%04d", CC_USERS, initTask, initResult, ORG_CC, ORG_IT_STAFF);

//        predefinedTestMethodTracing = PredefinedTestMethodTracing.MODEL_LOGGING;
    }

    private void createUsers(String namePattern, int count, Task task, OperationResult result, TestResource<?>... targets)
            throws CommonException {
        for (int i = 0; i < count; i++) {
            UserType user = new UserType(prismContext)
                    .name(String.format(namePattern, i));
            for (TestResource<?> target : targets) {
                user.getAssignment().add(ObjectTypeUtil.createAssignmentTo(target.object, SchemaConstants.ORG_DEFAULT));
            }
            addObject(user.asPrismObject(), task, result);
        }
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Test
    public void test000Sanity() throws Exception {
        assertUserByUsername("user-dcs-0000", "after init")
                .display()
                .assertCostCenter("07210");
        assertUserByUsername("user-cc-0000", "after init")
                .display()
                .assertCostCenter("07330");
    }

    @Test
    public void test100ChangeCostCenter() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        when();
        ObjectDelta<OrgType> delta = deltaFor(OrgType.class)
                .item(OrgType.F_COST_CENTER).replace("07999")
                .asObjectDelta(ORG_DCS.oid);
        executeChanges(delta, null, task, result);

        then();
        assertSuccess(result);
        assertUserAfterByUsername("user-dcs-0000")
                .assertCostCenter("07999");
        assertUserAfterByUsername("user-cc-0000")
                .assertCostCenter("07330");
    }

    @Test
    public void test110ChangeCostCenterNoRecompute() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        when();
        ObjectDelta<OrgType> delta = deltaFor(OrgType.class)
                .item(OrgType.F_COST_CENTER).replace("07777")
                .asObjectDelta(ORG_DCS.oid);

        executeChanges(delta, doNotRecompute(), task, result);

        then();
        assertSuccess(result);
        assertUserAfterByUsername("user-dcs-0000")
                .assertCostCenter("07999");
        assertUserAfterByUsername("user-cc-0000")
                .assertCostCenter("07330");
    }

    @NotNull
    private ModelExecuteOptions doNotRecompute() throws SchemaException {
        ModelExecuteOptionsType optionsBean = new ModelExecuteOptionsType(prismContext);
        //noinspection unchecked
        PrismPropertyDefinition<Boolean> recomputeDef = prismContext.getSchemaRegistry().findPropertyDefinitionByElementName(RECOMPUTE_MEMBERS_NAME);
        PrismProperty<Boolean> recomputeProp = recomputeDef.instantiate();
        recomputeProp.setRealValue(false);
        optionsBean.asPrismContainerValue().findOrCreateContainer(ModelExecuteOptionsType.F_EXTENSION)
                .createNewValue()
                .add(recomputeProp);
        return ModelExecuteOptions.fromModelExecutionOptionsType(optionsBean);
    }
}
