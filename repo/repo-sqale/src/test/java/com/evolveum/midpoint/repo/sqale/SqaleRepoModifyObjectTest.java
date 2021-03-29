/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.Assertions;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.MUser;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QUser;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class SqaleRepoModifyObjectTest extends SqaleRepoBaseTest {

    private String user1Oid;

    @BeforeClass
    public void initObjects() throws Exception {
        OperationResult result = createOperationResult();

        user1Oid = repositoryService.addObject(new UserType(prismContext)
                        .name("user-1")
                        .asPrismObject(),
                null, result);

        assertThatOperationResult(result).isSuccess();
    }

    @Test
    public void test100ChangeStringAttribute()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();

        given("delta with a email change for user 1");
        ObjectDelta<UserType> modificationReplaceProperty =
                prismContext.deltaFactory().object().createModificationReplaceProperty(
                        UserType.class, user1Oid, UserType.F_EMAIL_ADDRESS, "new@email.com");

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid,
                modificationReplaceProperty.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        UserType userObject = repositoryService.getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userObject.getEmailAddress()).isEqualTo("new@email.com");

        and("externalized column is updated too");
        MUser user = selectObjectByOid(QUser.class, user1Oid);
        assertThat(user.emailAddress).isEqualTo("new@email.com"); // TODO this fails now, obviously
    }

    @Test
    public void test900ModificationsMustNotBeNull() {
        OperationResult result = createOperationResult();

        given("null modifications");

        expect("modifyObject throws exception");
        //noinspection ConstantConditions
        Assertions.assertThatThrownBy(() ->
                repositoryService.modifyObject(UserType.class, user1Oid, null, result))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Modifications must not be null.");
    }
}
