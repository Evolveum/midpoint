/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipalManager;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.RelationTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.evolveum.midpoint.model.intest.AbstractConfiguredModelIntegrationTest.*;

@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestBrokenAuthz extends AbstractEmptyModelIntegrationTest {

    protected static final String ROLE_SUPERUSER_OID = "00000000-0000-0000-0000-000000000004";

    @Autowired
    private GuiProfiledPrincipalManager guiProfiledPrincipalManager;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        addObject(USER_JACK_FILE, initTask, initResult);
        // jack has superuser role
        assignRole(USER_JACK_OID, ROLE_SUPERUSER_OID, RelationTypes.MEMBER.getRelation());
    }

    @Test
    public void testBrokenAuthz() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> user = getObject(UserType.class, USER_JACK_OID);
        display("user before", user);

        // Login as jack
        login(user.getName().getOrig());
        MidPointPrincipal midpointPrincipal = AuthUtil.getMidpointPrincipal();

        AtomicReference<MidPointPrincipal> session = new AtomicReference<>();
        session.set(midpointPrincipal);

        // Run CompiledProfile refresh in a separate thread
        CompletableFuture<Void> refreshTask = CompletableFuture.runAsync(() -> {
            try {
                for (int i = 0; i < 50; i++) {
                    MidPointPrincipal midPointPrincipal = session.get();

                    guiProfiledPrincipalManager.refreshCompiledProfile((GuiProfiledPrincipal) midPointPrincipal);
                }
            } catch (Exception ignore) {
            }
        });

        for (int i = 0; i < 50; i++) {
            try {
                // assign
                AssignmentType assignment = new AssignmentType();
                assignment.targetRef(ROLE_END_USER.oid, RoleType.COMPLEX_TYPE);

                ObjectDelta<UserType> delta = user.createModifyDelta();
                delta.addModificationAddContainer(RoleType.F_ASSIGNMENT, assignment.asPrismContainerValue());

                modelService.executeChanges(List.of(delta), null, task, result);

                // unassign
                ObjectDelta<UserType> delta2 = user.createModifyDelta();
                delta2.addModificationDeleteContainer(RoleType.F_ASSIGNMENT, assignment.clone().asPrismContainerValue());

                modelService.executeChanges(List.of(delta2), null, task, result);

            } catch (SecurityViolationException e) {
                // Jack has the superuser role and should not encounter permission errors,
                // but when the CompiledProfile is refreshed in a separate thread,
                // permission errors can occur depending on the timing.
                fail("Detected broken authz: " + e.getMessage());
            }
        }

        refreshTask.get(10, TimeUnit.SECONDS);
    }
}
