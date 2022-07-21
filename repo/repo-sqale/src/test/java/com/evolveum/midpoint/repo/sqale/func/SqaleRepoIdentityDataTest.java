/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.func;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.sqale.SqaleRepoBaseTest;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

// TEMPORARY CODE
public class SqaleRepoIdentityDataTest extends SqaleRepoBaseTest {

    public static final File TEST_DIR = new File("src/test/resources/identity");

    private static final File FILE_USER_WITH_IDENTITY_DATA = new File(TEST_DIR, "user-with-identity-data.xml");

    @BeforeClass
    public void initObjects() {
        OperationResult result = createOperationResult();

        // TODO

        assertThatOperationResult(result).isSuccess();
    }

    @Test
    public void test100AddUserWithIdentityData()
            throws CommonException, IOException {
        OperationResult result = createOperationResult();

        given("user with identity data");
        PrismObject<UserType> userToAdd = prismContext.parseObject(FILE_USER_WITH_IDENTITY_DATA);
        displayDumpable("user to add", userToAdd);
        displayValue("user to add (XML)", prismContext.xmlSerializer().serialize(userToAdd));

        when("addObject is called");
        String oid = repositoryService.addObject(userToAdd, null, result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("user can be obtained from repo, by default without identities");
        OperationResult getResult = createOperationResult();
        UserType user = repositoryService.getObject(UserType.class, oid, null, getResult).asObjectable();
        assertThatOperationResult(getResult).isSuccess();
        // container is marked incomplete and its value is empty
        assertThat(user.asPrismObject().findContainer(FocusType.F_IDENTITIES).isIncomplete()).isTrue();
        assertThat(((PrismContainerValue<?>) user.getIdentities().asPrismContainerValue()).isEmpty()).isTrue();

        and("user can be obtained with identities using options");
        Collection<SelectorOptions<GetOperationOptions>> getOptions = SchemaService.get()
                .getOperationOptionsBuilder().item(FocusType.F_IDENTITIES).retrieve().build();
        OperationResult getWithIdentitiesResult = createOperationResult();
        UserType user2 = repositoryService.getObject(UserType.class, oid, getOptions, getWithIdentitiesResult).asObjectable();
        assertThatOperationResult(getWithIdentitiesResult).isSuccess();
        // TODO fix
//        assertThat(user2.asPrismObject().findContainer(FocusType.F_IDENTITIES).isIncomplete()).isFalse();

        // TODO more in-depth check of identity
    }
}
