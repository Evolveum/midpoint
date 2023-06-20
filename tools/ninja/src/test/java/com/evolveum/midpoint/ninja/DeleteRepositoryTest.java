/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja;

import com.evolveum.midpoint.ninja.impl.ActionStateListener;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

import org.assertj.core.api.Assertions;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

public class DeleteRepositoryTest extends BaseTest {

    @BeforeClass
    public void initMidpointHome() throws Exception {
        setupMidpointHome();
    }

    @Test
    public void deleteByOid() throws Exception {
        given();

        String type = ObjectTypes.ROLE.name();
        String oid = SystemObjectsType.ROLE_DELEGATOR.value();

        String[] args = new String[] { "-m", getMidpointHome(), "delete", "-o", oid, "-t", type };

        OperationResult result = new OperationResult("delete by oid");

        ActionStateListener listener = new ActionStateListener() {

            @Override
            public void onBeforeExecution(NinjaContext context) {
                RepositoryService repository = context.getRepository();

                try {
                    PrismObject<?> role = repository.getObject(RoleType.class, oid,
                            GetOperationOptions.createRawCollection(), result);

                    Assert.assertNotNull(role);
                } catch (Exception ex) {
                    Assertions.fail("Couldn't find role that should be deleted by ninja", ex);
                }
            }

            @Override
            public void onAfterExecution(NinjaContext context) {
                RepositoryService repository = context.getRepository();
                try {
                    PrismObject<RoleType> role = repository.getObject(RoleType.class, oid, GetOperationOptions.createRawCollection(), result);

                    Assertions.fail("Get object should fail with object not found exception");
                } catch (ObjectNotFoundException ex) {
                    // ignored
                } catch (Exception ex) {
                    Assertions.fail("Unknown exception", ex);
                }
            }
        };

        executeTest(args, null, null, listener);
    }
}
