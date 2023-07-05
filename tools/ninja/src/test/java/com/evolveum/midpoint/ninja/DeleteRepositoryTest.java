/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja;

import org.assertj.core.api.Assertions;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

@ContextConfiguration(locations = "classpath:ctx-ninja-test.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class DeleteRepositoryTest extends NinjaSpringTest {

    @Test
    public void deleteByOid() throws Exception {
        given();

        String type = ObjectTypes.ROLE.name();
        final String oid = SystemObjectsType.ROLE_DELEGATOR.value();

        RoleType role = new RoleType();
        role.setOid(oid);
        role.setName(new PolyStringType("delete by oid"));

        OperationResult result = new OperationResult("delete by oid");

        repository.addObject(role.asPrismObject(), new RepoAddOptions(), result);

        when();

        executeTest(null, null, "-v", "-m", getMidpointHome(), "delete", "-o", oid, "-t", type);

        then();

        try {
            repository.getObject(RoleType.class, oid, GetOperationOptions.createRawCollection(), result);

            Assertions.fail("Get object should fail with object not found exception");
        } catch (ObjectNotFoundException ex) {
            // ignored
        }
    }
}
