/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.wf.impl.objects;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createAssignmentTo;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.io.File;
import java.io.IOException;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.wf.impl.AbstractWfTestPolicy;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * Tests global object approval rules whose whole approval processing is skipped by stage auto-completion.
 *
 * MID-11101: skipped ADD approvals must not leave empty ADD deltas in workflow decomposition/recomposition.
 */
@ContextConfiguration(locations = { "classpath:ctx-workflow-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestObjectLifecycleApprovalSkipped extends AbstractWfTestPolicy {

    private static final File GLOBAL_POLICY_RULES_FILE =
            new File(AbstractTestObjectLifecycleApproval.TEST_RESOURCE_DIR, "global-policy-rules-skipped.xml");

    private static final String INDUCED_ROLE_OID = "10000001-d34d-b33f-f00d-d34db33ff00d";

    @Override
    protected void updateSystemConfiguration(SystemConfigurationType systemConfiguration) throws SchemaException, IOException {
        super.updateSystemConfiguration(systemConfiguration);
        PrismObject<SystemConfigurationType> rulesContainer = prismContext.parserFor(GLOBAL_POLICY_RULES_FILE).parse();
        systemConfiguration.getGlobalPolicyRule().clear();
        systemConfiguration.getGlobalPolicyRule()
                .addAll(CloneUtil.cloneCollectionMembers(rulesContainer.asObjectable().getGlobalPolicyRule()));
    }

    /**
     * Single skipped object ADD approval: the role should be created directly, without an approval case.
     */
    @Test
    public void test100CreateRoleWithSkippedAddApproval() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        RoleType role = new RoleType(prismContext)
                .name("mid-11101-add-skipped");

        executeChanges(DeltaFactory.Object.createAddDelta(role.asPrismObject()), null, task, result);

        PrismObject<RoleType> roleAfter = searchObjectByName(RoleType.class, "mid-11101-add-skipped");
        assertNotNull("Role was not created", roleAfter);
        assertEquals("Wrong role name", "mid-11101-add-skipped", roleAfter.asObjectable().getName().getOrig());
        assertNull("Unexpected approval case", result.findCaseOid());
        assertEquals("Unexpected open work items", 0, getWorkItems(task, result).size());
    }

    /**
     * Skipped object ADD approval together with skipped deltaFrom inducement approval:
     * the role should be created directly with its inducement preserved.
     */
    @Test
    public void test200CreateRoleWithSkippedAddAndInducementApprovals() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        RoleType role = new RoleType(prismContext)
                .name("mid-11101-add-inducement-skipped")
                .inducement(createAssignmentTo(INDUCED_ROLE_OID, ObjectTypes.ROLE));

        executeChanges(DeltaFactory.Object.createAddDelta(role.asPrismObject()), null, task, result);

        PrismObject<RoleType> roleAfter = searchObjectByName(RoleType.class, "mid-11101-add-inducement-skipped");
        assertNotNull("Role was not created", roleAfter);
        assertEquals("Wrong role name", "mid-11101-add-inducement-skipped", roleAfter.asObjectable().getName().getOrig());
        assertEquals("Wrong inducements count", 1, roleAfter.asObjectable().getInducement().size());
        assertNull("Unexpected approval case", result.findCaseOid());
        assertEquals("Unexpected open work items", 0, getWorkItems(task, result).size());
    }
}
