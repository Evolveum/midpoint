/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static com.evolveum.midpoint.repo.api.RepoAddOptions.createOverwrite;

import java.util.List;
import java.util.UUID;

import org.testng.annotations.Test;

import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainerType;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.MUser;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QUser;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationExecutionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class SqaleRepoAddObjectTest extends SqaleRepoBaseTest {

    @Test
    public void test100AddNamedUserWithoutOidWorksOk()
            throws ObjectAlreadyExistsException, SchemaException {
        OperationResult result = createOperationResult();

        given("user with a name");
        String userName = "user" + getTestNumber();
        UserType userType = new UserType(prismContext)
                .name(userName);

        when("adding it to the repository");
        repositoryService.addObject(userType.asPrismObject(), null, result);

        then("operation is successful and user row for it is created");
        assertResult(result);

        QUser u = aliasFor(QUser.class);
        List<MUser> users = select(u, u.nameOrig.eq(userName));
        assertThat(users).hasSize(1);

        MUser mUser = users.get(0);
        assertThat(mUser.oid).isNotNull();
        assertThat(mUser.nameNorm).isNotNull(); // normalized name is stored
        assertThat(mUser.version).isEqualTo(1); // initial version is set
        // read-only column with value generated/stored in the database
        assertThat(mUser.objectType).isEqualTo(MObjectTypeMapping.USER.code());
    }

    @Test
    public void test101AddUserWithoutNameFails() {
        OperationResult result = createOperationResult();

        given("user without specified name");
        long baseCount = count(QUser.class);
        UserType userType = new UserType(prismContext);

        expect("adding it to the repository throws exception and no row is created");
        assertThatThrownBy(() -> repositoryService.addObject(userType.asPrismObject(), null, result))
                .isInstanceOf(SchemaException.class)
                .hasMessage("Attempt to add object without name.");

        assertThatOperationResult(result).isFatalError()
                .hasMessageContaining("Attempt to add object without name.");
        assertCount(QUser.class, baseCount);
    }

    @Test
    public void test102AddWithoutOidIgnoresOverwriteOption()
            throws ObjectAlreadyExistsException, SchemaException {
        OperationResult result = createOperationResult();

        given("user with a name but without OID");
        String userName = "user" + getTestNumber();
        UserType userType = new UserType(prismContext)
                .name(userName);

        when("adding it to the repository with overwrite option");
        repositoryService.addObject(userType.asPrismObject(), createOverwrite(), result);

        then("operation is successful and user row for it is created, overwrite is meaningless");
        assertResult(result);

        QUser u = aliasFor(QUser.class);
        List<MUser> users = select(u, u.nameOrig.eq(userName));
        assertThat(users).hasSize(1);
        assertThat(users.get(0).oid).isNotNull();
    }

    @Test
    public void test110AddUserWithProvidedOidWorksOk()
            throws ObjectAlreadyExistsException, SchemaException {
        OperationResult result = createOperationResult();

        given("user with provided OID");
        UUID providedOid = UUID.randomUUID();
        String userName = "user" + getTestNumber();
        UserType userType = new UserType(prismContext)
                .oid(providedOid.toString())
                .name(userName);

        when("adding it to the repository");
        repositoryService.addObject(userType.asPrismObject(), null, result);

        then("operation is successful and user row with provided OID is created");
        assertResult(result);

        QUser u = aliasFor(QUser.class);
        List<MUser> users = select(u, u.nameOrig.eq(userName));
        assertThat(users).hasSize(1);

        MUser mUser = users.get(0);
        assertThat(mUser.oid).isEqualTo(providedOid);
        assertThat(mUser.version).isEqualTo(1); // initial version is set
    }

    @Test
    public void test111AddSecondObjectWithTheSameOidThrowsObjectAlreadyExists()
            throws ObjectAlreadyExistsException, SchemaException {
        OperationResult result = createOperationResult();

        given("user with provided OID already exists");
        UUID providedOid = UUID.randomUUID();
        UserType user1 = new UserType(prismContext)
                .oid(providedOid.toString())
                .name("user" + getTestNumber());
        repositoryService.addObject(user1.asPrismObject(), null, result);

        when("adding it another user with the same OID to the repository");
        long baseCount = count(QUser.class);
        UserType user2 = new UserType(prismContext)
                .oid(providedOid.toString())
                .name("user" + getTestNumber() + "-different-name");

        then("operation fails and no new user row is created");
        assertThatThrownBy(() -> repositoryService.addObject(user2.asPrismObject(), null, result))
                .isInstanceOf(ObjectAlreadyExistsException.class);
        assertThatOperationResult(result).isFatalError()
                .hasMessageMatching("Provided OID .* already exists");
        assertCount(QUser.class, baseCount);
    }

    @Test
    public void test200AddObjectWithMultivalueContainers()
            throws ObjectAlreadyExistsException, SchemaException {
        OperationResult result = createOperationResult();

        given("user with assignment and ref");
        String userName = "user" + getTestNumber();
        String targetRef1 = UUID.randomUUID().toString();
        String targetRef2 = UUID.randomUUID().toString();
        UserType user = new UserType(prismContext)
                .name(userName)
                .assignment(new AssignmentType(prismContext)
                        .targetRef(targetRef1, RoleType.COMPLEX_TYPE))
                .assignment(new AssignmentType(prismContext)
                        .targetRef(targetRef2, RoleType.COMPLEX_TYPE));

        when("adding it to the repository");
        repositoryService.addObject(user.asPrismObject(), null, result);

        then("object and its container rows are created and container IDs are assigned");
        assertResult(result);

        QUser u = aliasFor(QUser.class);
        List<MUser> users = select(u, u.nameOrig.eq(userName));
        assertThat(users).hasSize(1);
        MUser userRow = users.get(0);
        assertThat(userRow.oid).isNotNull();
        assertThat(userRow.containerIdSeq).isEqualTo(3); // next free container number

        QContainer<MContainer> c = aliasFor(QContainer.CLASS);
        List<MContainer> containers = select(c, c.ownerOid.eq(userRow.oid));
        // TODO this fails ATM, not implemented yet
        assertThat(containers).hasSize(2)
                .allMatch(cRow -> cRow.ownerOid.equals(userRow.oid)
                        && cRow.containerType == MContainerType.ASSIGNMENT)
                .extracting(cRow -> cRow.cid)
                .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    public void test201AddObjectWithMultivalueRefs()
            throws ObjectAlreadyExistsException, SchemaException {
        OperationResult result = createOperationResult();

        given("user with ref");
        String userName = "user" + getTestNumber();
        String targetRef1 = UUID.randomUUID().toString();
        String targetRef2 = UUID.randomUUID().toString();
        UserType user = new UserType(prismContext)
                .name(userName)
                .linkRef(targetRef1, RoleType.COMPLEX_TYPE)
                .linkRef(targetRef2, RoleType.COMPLEX_TYPE);

        when("adding it to the repository");
        repositoryService.addObject(user.asPrismObject(), null, result);

        then("object and its container rows are created and container IDs are assigned");
        assertResult(result);

        QUser u = aliasFor(QUser.class);
        List<MUser> users = select(u, u.nameOrig.eq(userName));
        assertThat(users).hasSize(1);
        MUser userRow = users.get(0);
        assertThat(userRow.oid).isNotNull();
        assertThat(userRow.containerIdSeq).isEqualTo(1); // cid sequence is in initial state

        // TODO assert ref rows
    }

    @Test
    public void test290DuplicateCidInsideOneContainerIsCaughtByPrism() {
        expect("object construction with duplicate CID inside container fails immediately");
        assertThatThrownBy(() -> new UserType(prismContext)
                .assignment(new AssignmentType()
                        .targetRef("ref1", RoleType.COMPLEX_TYPE).id(1L))
                .assignment(new AssignmentType()
                        .targetRef("ref2", RoleType.COMPLEX_TYPE).id(1L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Attempt to add a container value with an id that already exists: 1");
    }

    @Test
    public void test291DuplicateCidInDifferentContainersIsCaughtByRepo() {
        OperationResult result = createOperationResult();

        given("object with duplicate CID in different containers");
        UserType user = new UserType(prismContext)
                .name("any name")
                .assignment(new AssignmentType().id(1L))
                .operationExecution(new OperationExecutionType().id(1L));

        expect("adding object to repository throws exception");
        assertThatThrownBy(() -> repositoryService.addObject(user.asPrismObject(), null, result))
                .isInstanceOf(SchemaException.class)
                .hasMessage("CID 1 is used repeatedly in the object!");
    }
}
