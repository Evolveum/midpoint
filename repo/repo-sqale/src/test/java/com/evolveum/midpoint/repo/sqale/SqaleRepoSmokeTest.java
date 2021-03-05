/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.evolveum.midpoint.repo.api.DeleteObjectResult;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class SqaleRepoSmokeTest extends SqaleRepoBaseTest {

    private String sanityUserOid;

    @Test
    public void test000Sanity() {
        assertThat(repositoryService).isNotNull();

        // DB should be empty
        assertCount(QObject.CLASS, 0);
        assertCount(QContainer.CLASS, 0);

        // selects check also mapping to M-classes
        assertThat(select(aliasFor(QObject.CLASS))).isEmpty();
        assertThat(select(aliasFor(QContainer.CLASS))).isEmpty();
    }

    @Test
    public void test100AddObject() throws ObjectAlreadyExistsException, SchemaException {
        OperationResult result = createOperationResult();

        UserType userType = new UserType(prismContext)
                .name("sanity-user");
        sanityUserOid = repositoryService.addObject(userType.asPrismObject(), null, result);

        assertThat(sanityUserOid).isNotNull();
        assertResult(result);
    }

    @Test(enabled = false) // TODO deleteObject not implemented yet
    public void test900DeleteObject() throws ObjectNotFoundException {
        OperationResult result = createOperationResult();

        DeleteObjectResult deleteResult =
                repositoryService.deleteObject(UserType.class, sanityUserOid, result);

        assertThat(deleteResult).isNotNull();
        assertResult(result);
    }
}
