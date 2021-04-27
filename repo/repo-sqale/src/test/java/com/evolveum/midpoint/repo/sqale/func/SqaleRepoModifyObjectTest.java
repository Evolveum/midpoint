/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.func;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.testng.Assert.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.xml.namespace.QName;

import org.assertj.core.api.Assertions;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sqale.SqaleRepoBaseTest;
import com.evolveum.midpoint.repo.sqale.qmodel.assignment.*;
import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainerType;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.MUser;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QUser;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.MReference;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QObjectReference;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QObjectReferenceMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.role.MService;
import com.evolveum.midpoint.repo.sqale.qmodel.role.QService;
import com.evolveum.midpoint.repo.sqale.qmodel.shadow.MShadow;
import com.evolveum.midpoint.repo.sqale.qmodel.shadow.QShadow;
import com.evolveum.midpoint.repo.sqale.qmodel.task.MTask;
import com.evolveum.midpoint.repo.sqale.qmodel.task.QTask;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class SqaleRepoModifyObjectTest extends SqaleRepoBaseTest {

    private String user1Oid; // typical object
    private String task1Oid; // task has more attribute type variability
    private String shadow1Oid; // ditto
    private String service1Oid; // object with integer attribute

    @BeforeClass
    public void initObjects() throws Exception {
        OperationResult result = createOperationResult();

        user1Oid = repositoryService.addObject(
                new UserType(prismContext).name("user-1").asPrismObject(),
                null, result);
        task1Oid = repositoryService.addObject(
                new TaskType(prismContext).name("task-1").asPrismObject(),
                null, result);
        shadow1Oid = repositoryService.addObject(
                new ShadowType(prismContext).name("shadow-1").asPrismObject(),
                null, result);
        service1Oid = repositoryService.addObject(
                new ServiceType(prismContext).name("service-1").asPrismObject(),
                null, result);

        assertThatOperationResult(result).isSuccess();
    }

    // region various types of simple items
    @Test
    public void test100ChangeStringAttribute()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);

        given("delta with email change for user 1 using property add modification");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_EMAIL_ADDRESS).add("new@email.com")
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        UserType userObject = repositoryService.getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(userObject.getEmailAddress()).isEqualTo("new@email.com");

        and("externalized column is updated");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.emailAddress).isEqualTo("new@email.com");
    }

    /**
     * NOTE: This test documents current behavior where {@link ItemDelta#applyTo(Item)} interprets
     * ADD for single-value item as REPLACE if the value is not empty.
     * This behavior is likely to change.
     */
    @Test
    public void test101ChangeStringAttributeWithPreviousValueUsingAddModification()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);

        given("delta with email change for user 1 using property add modification");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_EMAIL_ADDRESS).add("new2@email.com")
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated and old value is overridden");
        UserType userObject = repositoryService.getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(userObject.getEmailAddress()).isEqualTo("new2@email.com");

        and("externalized column is updated");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.emailAddress).isEqualTo("new2@email.com");
    }

    @Test
    public void test102DeleteStringAttribute()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);

        given("delta with email replace to null ('delete') for user 1");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_EMAIL_ADDRESS).replace()
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated and email is gone");
        UserType userObject = repositoryService.getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(userObject.getEmailAddress()).isNull();

        and("externalized column is set to NULL");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.emailAddress).isNull();
    }

    @Test
    public void test103StringReplacePreviousNullValueIsOk()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();

        given("delta with email change for user 1 using property replace modification");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_EMAIL_ADDRESS).replace("newer@email.com")
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        UserType userObject = repositoryService.getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userObject.getEmailAddress()).isEqualTo("newer@email.com");

        and("externalized column is set to NULL");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.emailAddress).isEqualTo("newer@email.com");
    }

    @Test
    public void test104StringDeleteWithWrongValueDoesNotChangeAnything()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);

        given("delta with email delete for user 1 using wrong previous value");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                // without value it would not be recognized as delete
                .item(UserType.F_EMAIL_ADDRESS).delete("x")
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        // Internal note: the delta is filter by prismObject.narrowModifications
        and("serialized form (fullObject) is not changed and previous email is still there");
        UserType userObject = repositoryService.getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userObject.getEmailAddress()).isEqualTo("newer@email.com");

        and("externalized column is updated");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.emailAddress).isEqualTo("newer@email.com");
        // This is rather implementation detail, but it shows that when narrowed modifications are
        // empty, we don't bother with update at all which is more efficient.
        assertThat(row.version).isEqualTo(originalRow.version);
    }

    @Test
    public void test105StringReplaceWithExistingValueWorksOk()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();

        given("delta with email replace for user 1 (email has previous value)");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                // without value it would not be recognized as delete
                .item(UserType.F_EMAIL_ADDRESS).replace("newest@email.com")
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is changed and email value is replaced");
        UserType userObject = repositoryService.getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userObject.getEmailAddress()).isEqualTo("newest@email.com");

        and("externalized column is updated");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.emailAddress).isEqualTo("newest@email.com");
    }

    @Test
    public void test106StringDeletedUsingTheRightValue()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();

        given("delta with email delete for user 1 using valid previous value");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                // without value it would not be recognized as delete
                .item(UserType.F_EMAIL_ADDRESS).delete("newest@email.com")
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        // Internal note: the delta is filter by prismObject.narrowModifications
        and("serialized form (fullObject) is changed and email is gone");
        UserType userObject = repositoryService.getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userObject.getEmailAddress()).isNull();

        and("externalized column is set to NULL");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.emailAddress).isNull();
    }

    @Test
    public void test110ChangeInstantAttribute()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();
        MTask originalRow = selectObjectByOid(QTask.class, task1Oid);

        given("delta with last run start timestamp change for task 1 adding value");
        ObjectDelta<TaskType> delta = prismContext.deltaFor(TaskType.class)
                .item(TaskType.F_LAST_RUN_START_TIMESTAMP)
                .add(MiscUtil.asXMLGregorianCalendar(1L))
                .asObjectDelta(task1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(TaskType.class, task1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        TaskType taskObject = repositoryService.getObject(TaskType.class, task1Oid, null, result)
                .asObjectable();
        assertThat(taskObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(taskObject.getLastRunStartTimestamp())
                .isEqualTo(MiscUtil.asXMLGregorianCalendar(1L));

        and("externalized column is updated");
        MTask row = selectObjectByOid(QTask.class, task1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.lastRunStartTimestamp).isEqualTo(Instant.ofEpochMilli(1));
    }

    /*
    We don't bother with replace tests for these other simple types, if update works for
    setting, it must work for replacing (it's the same code like for String).
    We test nulls just to be sure there is no JDBC type trick there (NULL is a bit special).
    */

    @Test
    public void test111DeleteInstantAttribute()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();

        given("delta with last run start timestamp replace to null ('delete') for task 1");
        ObjectDelta<TaskType> delta = prismContext.deltaFor(TaskType.class)
                .item(TaskType.F_LAST_RUN_START_TIMESTAMP).replace()
                .asObjectDelta(task1Oid);

        and("task row previously having the timestamp value");
        MTask originalRow = selectObjectByOid(QTask.class, task1Oid);
        assertThat(originalRow.lastRunStartTimestamp).isNotNull();

        when("modifyObject is called");
        repositoryService.modifyObject(TaskType.class, task1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated and last run start timestamp is gone");
        TaskType taskObject = repositoryService.getObject(TaskType.class, task1Oid, null, result)
                .asObjectable();
        assertThat(taskObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(taskObject.getLastRunStartTimestamp()).isNull();

        and("externalized column is set to NULL");
        MTask row = selectObjectByOid(QTask.class, task1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.lastRunStartTimestamp).isNull();
    }

    @Test
    public void test115ChangeIntegerAttribute()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();
        MService originalRow = selectObjectByOid(QService.class, service1Oid);

        given("delta with display order change for service 1");
        ObjectDelta<ServiceType> delta = prismContext.deltaFor(ServiceType.class)
                .item(ServiceType.F_DISPLAY_ORDER).replace(5)
                .asObjectDelta(service1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(
                ServiceType.class, service1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        ServiceType serviceObject = repositoryService
                .getObject(ServiceType.class, service1Oid, null, result)
                .asObjectable();
        assertThat(serviceObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(serviceObject.getDisplayOrder()).isEqualTo(5);

        and("externalized column is updated");
        MService row = selectObjectByOid(QService.class, service1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.displayOrder).isEqualTo(5);
    }

    @Test
    public void test116DeleteIntegerAttribute()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();

        given("delta with display order replace to null for service 1");
        ObjectDelta<ServiceType> delta = prismContext.deltaFor(ServiceType.class)
                .item(ServiceType.F_DISPLAY_ORDER).replace()
                .asObjectDelta(service1Oid);

        and("service row previously having the display order value");
        MService originalRow = selectObjectByOid(QService.class, service1Oid);
        assertThat(originalRow.displayOrder).isNotNull();

        when("modifyObject is called");
        repositoryService.modifyObject(ServiceType.class, service1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated and display order is gone");
        ServiceType serviceObject = repositoryService
                .getObject(ServiceType.class, service1Oid, null, result)
                .asObjectable();
        assertThat(serviceObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(serviceObject.getDisplayOrder()).isNull();

        and("externalized column is set to NULL");
        MService row = selectObjectByOid(QService.class, service1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.displayOrder).isNull();
    }

    @Test
    public void test120ChangeBooleanAttribute()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();

        given("delta with boolean dead change for shadow 1");
        ObjectDelta<ShadowType> delta = prismContext.deltaFor(ShadowType.class)
                .item(ShadowType.F_DEAD).add(true)
                .asObjectDelta(shadow1Oid);

        and("shadow row previously having dead property empty (null)");
        MShadow originalRow = selectObjectByOid(QShadow.class, shadow1Oid);
        assertThat(originalRow.dead).isNull();

        when("modifyObject is called");
        repositoryService.modifyObject(
                ShadowType.class, shadow1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        ShadowType shadowObject = repositoryService
                .getObject(ShadowType.class, shadow1Oid, null, result)
                .asObjectable();
        assertThat(shadowObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(shadowObject.isDead()).isTrue();

        and("externalized column is updated");
        MShadow row = selectObjectByOid(QShadow.class, shadow1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.dead).isTrue();
    }

    @Test
    public void test121DeleteBooleanAttribute()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();

        given("delta with dead boolean replace to null for shadow 1");
        ObjectDelta<ShadowType> delta = prismContext.deltaFor(ShadowType.class)
                .item(ShadowType.F_DEAD).replace()
                .asObjectDelta(shadow1Oid);

        and("shadow row previously having the display order value");
        MShadow originalRow = selectObjectByOid(QShadow.class, shadow1Oid);
        assertThat(originalRow.dead).isNotNull();

        when("modifyObject is called");
        repositoryService.modifyObject(
                ShadowType.class, shadow1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated and dead is null");
        ShadowType shadowObject = repositoryService
                .getObject(ShadowType.class, shadow1Oid, null, result)
                .asObjectable();
        assertThat(shadowObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(shadowObject.isDead()).isNull();

        and("externalized column is set to NULL");
        MShadow row = selectObjectByOid(QShadow.class, shadow1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.dead).isNull();
    }

    @Test
    public void test130ChangePolyStringAttribute()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();

        given("delta with polystring nickname change for user 1");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_NICK_NAME).add(new PolyString("nick-name"))
                .asObjectDelta(user1Oid);

        and("user row previously having dead property empty (null)");
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);
        assertThat(originalRow.nickNameOrig).isNull();
        assertThat(originalRow.nickNameNorm).isNull();

        when("modifyObject is called");
        repositoryService.modifyObject(
                UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        UserType userObject = repositoryService
                .getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));

        PolyStringType nickName = userObject.getNickName();
        assertThat(nickName.getOrig()).isEqualTo("nick-name");
        assertThat(nickName.getNorm()).isEqualTo("nickname");

        and("externalized column is updated");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.nickNameOrig).isEqualTo("nick-name");
        assertThat(row.nickNameNorm).isEqualTo("nickname");
    }

    @Test
    public void test131DeletePolyStringAttribute()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();

        given("delta with polystring nickname replace with null for user 1");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_NICK_NAME).replace()
                .asObjectDelta(user1Oid);

        and("user row previously having the nickname value");
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);
        assertThat(originalRow.nickNameOrig).isNotNull();
        assertThat(originalRow.nickNameNorm).isNotNull();

        when("modifyObject is called");
        repositoryService.modifyObject(
                UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated and nickname is gone");
        UserType userObject = repositoryService
                .getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(userObject.getNickName()).isNull();

        and("externalized column is set to NULL");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.nickNameOrig).isNull();
        assertThat(row.nickNameNorm).isNull();
    }

    @Test
    public void test135ObjectNameCanBeChanged()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);

        given("delta with object name change for user 1");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(ObjectType.F_NAME).add(new PolyString("user-1-changed"))
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(
                UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        UserType userObject = repositoryService
                .getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));

        PolyStringType name = userObject.getName();
        assertThat(name.getOrig()).isEqualTo("user-1-changed");
        assertThat(name.getNorm()).isEqualTo("user1changed");

        and("externalized column is updated");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.nameOrig).isEqualTo("user-1-changed");
        assertThat(row.nameNorm).isEqualTo("user1changed");
    }

    @Test
    public void test136ObjectNameCantBeRemoved()
            throws ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);

        given("delta with object name replace with null for user 1");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_NAME).replace()
                .asObjectDelta(user1Oid);

        expect("modifyObject throws exception");
        assertThatThrownBy(() -> repositoryService.modifyObject(
                UserType.class, user1Oid, delta.getModifications(), result))
                .isInstanceOf(SystemException.class)
                .hasCauseInstanceOf(com.querydsl.core.QueryException.class);

        and("operation is fatal error");
        assertThatOperationResult(result).isFatalError();

        and("serialized form (fullObject) is not updated");
        UserType userObject = repositoryService
                .getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userObject.getVersion()).isEqualTo(String.valueOf(originalRow.version));
        assertThat(userObject.getName()).isNotNull();

        and("externalized column is set to NULL");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version);
        assertThat(row.nameOrig).isNotNull();
        assertThat(row.nameNorm).isNotNull();
    }

    @Test
    public void test140ChangeReferenceAttribute()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();
        MTask originalRow = selectObjectByOid(QTask.class, task1Oid);

        given("delta with owner reference change for task 1 adding value");
        UUID ownerTaskOid = UUID.randomUUID();
        QName ownerTaskRelation = QName.valueOf("{https://random.org/ns}owner-task-rel");
        ObjectDelta<TaskType> delta = prismContext.deltaFor(TaskType.class)
                .item(TaskType.F_OWNER_REF)
                .add(new ObjectReferenceType().oid(ownerTaskOid.toString())
                        .type(UserType.COMPLEX_TYPE)
                        .relation(ownerTaskRelation))
                .asObjectDelta(task1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(TaskType.class, task1Oid, delta.getModifications(), result);

        then("operation is successful, repository doesn't check target existence");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        TaskType taskObject = repositoryService.getObject(TaskType.class, task1Oid, null, result)
                .asObjectable();
        assertThat(taskObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));

        ObjectReferenceType ownerRef = taskObject.getOwnerRef();
        assertThat(ownerRef).isNotNull();
        assertThat(ownerRef.getOid()).isEqualTo(ownerTaskOid.toString());
        assertThat(ownerRef.getType()).isEqualTo(UserType.COMPLEX_TYPE);
        assertThat(ownerRef.getRelation()).isEqualTo(ownerTaskRelation);

        and("externalized column is updated");
        MTask row = selectObjectByOid(QTask.class, task1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.ownerRefTargetOid).isEqualTo(ownerTaskOid);
        assertThat(row.ownerRefTargetType).isEqualTo(MObjectType.USER);
        assertCachedUri(row.ownerRefRelationId, ownerTaskRelation);
    }

    @Test
    public void test141DeleteReferenceAttribute()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();

        given("delta with owner reference replace to null ('delete') for task 1");
        ObjectDelta<TaskType> delta = prismContext.deltaFor(TaskType.class)
                .item(TaskType.F_OWNER_REF).replace()
                .asObjectDelta(task1Oid);

        and("task row previously having the owner reference value");
        MTask originalRow = selectObjectByOid(QTask.class, task1Oid);
        assertThat(originalRow.ownerRefTargetOid).isNotNull();

        when("modifyObject is called");
        repositoryService.modifyObject(TaskType.class, task1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated and owner ref is gone");
        TaskType taskObject = repositoryService.getObject(TaskType.class, task1Oid, null, result)
                .asObjectable();
        assertThat(taskObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(taskObject.getOwnerRef()).isNull();

        and("externalized column is set to NULL");
        MTask row = selectObjectByOid(QTask.class, task1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.ownerRefTargetOid).isNull();
        assertThat(row.ownerRefTargetType).isNull();
        assertThat(row.ownerRefRelationId).isNull();
    }

    @Test
    public void test143ChangeCachedUriAttribute()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();
        MTask originalRow = selectObjectByOid(QTask.class, task1Oid);

        given("delta with handler change for task 1 adding value");
        ObjectDelta<TaskType> delta = prismContext.deltaFor(TaskType.class)
                .item(TaskType.F_HANDLER_URI).add("any://handler/uri")
                .asObjectDelta(task1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(TaskType.class, task1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        TaskType taskObject = repositoryService.getObject(TaskType.class, task1Oid, null, result)
                .asObjectable();
        assertThat(taskObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(taskObject.getHandlerUri()).isEqualTo("any://handler/uri");

        and("externalized column is updated");
        MTask row = selectObjectByOid(QTask.class, task1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertCachedUri(row.handlerUriId, "any://handler/uri");
    }

    @Test
    public void test144DeleteCachedUriAttribute()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();

        given("delta with handler replace to null ('delete') for task 1");
        ObjectDelta<TaskType> delta = prismContext.deltaFor(TaskType.class)
                .item(TaskType.F_HANDLER_URI).replace()
                .asObjectDelta(task1Oid);

        and("task row previously having the handler URI value");
        MTask originalRow = selectObjectByOid(QTask.class, task1Oid);
        assertThat(originalRow.handlerUriId).isNotNull();

        when("modifyObject is called");
        repositoryService.modifyObject(TaskType.class, task1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated and handler URI is gone");
        TaskType taskObject = repositoryService.getObject(TaskType.class, task1Oid, null, result)
                .asObjectable();
        assertThat(taskObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(taskObject.getHandlerUri()).isNull();

        and("externalized column is set to NULL");
        MTask row = selectObjectByOid(QTask.class, task1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.handlerUriId).isNull();
    }

    @Test
    public void test146ChangeEnumAttribute()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();
        MTask originalRow = selectObjectByOid(QTask.class, task1Oid);

        given("delta with execution status change for task 1 adding value");
        ObjectDelta<TaskType> delta = prismContext.deltaFor(TaskType.class)
                .item(TaskType.F_EXECUTION_STATUS).add(TaskExecutionStateType.SUSPENDED)
                .asObjectDelta(task1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(TaskType.class, task1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        TaskType taskObject = repositoryService.getObject(TaskType.class, task1Oid, null, result)
                .asObjectable();
        assertThat(taskObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(taskObject.getExecutionStatus()).isEqualTo(TaskExecutionStateType.SUSPENDED);

        and("externalized column is updated");
        MTask row = selectObjectByOid(QTask.class, task1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.executionStatus).isEqualTo(TaskExecutionStateType.SUSPENDED);
    }

    @Test
    public void test147DeleteEnumAttribute()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();

        given("delta with execution status replace to null ('delete') for task 1");
        ObjectDelta<TaskType> delta = prismContext.deltaFor(TaskType.class)
                .item(TaskType.F_EXECUTION_STATUS).replace()
                .asObjectDelta(task1Oid);

        and("task row previously having the handler URI value");
        MTask originalRow = selectObjectByOid(QTask.class, task1Oid);
        assertThat(originalRow.executionStatus).isNotNull();

        when("modifyObject is called");
        repositoryService.modifyObject(TaskType.class, task1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated and execution status is gone");
        TaskType taskObject = repositoryService.getObject(TaskType.class, task1Oid, null, result)
                .asObjectable();
        assertThat(taskObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(taskObject.getExecutionStatus()).isNull();

        and("externalized column is set to NULL");
        MTask row = selectObjectByOid(QTask.class, task1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.executionStatus).isNull();
    }
    // endregion

    // region multi-value refs
    @Test
    public void test160AddingProjectionRefInsertsRowsToTable()
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);

        given("delta adding projection ref to non-existent shadow for user 1");
        UUID refTargetOid = UUID.randomUUID();
        QName refRelation = QName.valueOf("{https://random.org/ns}projection-rel1");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_LINK_REF).add(new ObjectReferenceType()
                        .oid(refTargetOid.toString())
                        .type(ShadowType.COMPLEX_TYPE)
                        .relation(refRelation))
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful, ref target doesn't have to exist");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        UserType userObject = repositoryService
                .getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(userObject.getLinkRef()).hasSize(1)
                .anyMatch(refMatcher(refTargetOid, refRelation));

        and("user row version is incremented");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);

        and("externalized refs are inserted to the dedicated table");
        QObjectReference<?> r = QObjectReferenceMapping.INSTANCE_PROJECTION.defaultAlias();
        UUID ownerOid = UUID.fromString(user1Oid);
        List<MReference> refs = select(r, r.ownerOid.eq(ownerOid));
        assertThat(refs).hasSize(1)
                .anyMatch(refRowMatcher(refTargetOid, MObjectType.SHADOW, refRelation))
                .allMatch(ref -> ref.ownerOid.equals(ownerOid));
    }

    @Test
    public void test161AddingMoreProjectionRefsInsertsRowsToTable()
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);

        given("delta adding projection refs to the same target with different relation for user 1");
        UUID refTargetOid = UUID.randomUUID();
        QName refRelation1 = QName.valueOf("{https://random.org/ns}projection-rel1");
        QName refRelation2 = QName.valueOf("{https://random.org/ns}projection-rel2");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_LINK_REF).add(
                        new ObjectReferenceType().oid(refTargetOid.toString())
                                .type(ShadowType.COMPLEX_TYPE).relation(refRelation1),
                        new ObjectReferenceType().oid(refTargetOid.toString())
                                .type(ShadowType.COMPLEX_TYPE).relation(refRelation2))
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        UserType userObject = repositoryService
                .getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(userObject.getLinkRef()).hasSize(3); // no more checks, we believe in Prism

        and("user row version is incremented");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);

        and("externalized refs are inserted to the dedicated table");
        QObjectReference<?> r = QObjectReferenceMapping.INSTANCE_PROJECTION.defaultAlias();
        List<MReference> refs = select(r, r.ownerOid.eq(UUID.fromString(user1Oid)));
        assertThat(refs).hasSize(3)
                .anyMatch(refRowMatcher(refTargetOid, refRelation1))
                .anyMatch(refRowMatcher(refTargetOid, refRelation2));
    }

    @Test
    public void test162ReplacingProjectionRefs()
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);

        given("delta replacing projection refs w relation for user 1");
        UUID refTargetOid = UUID.randomUUID();
        QName refRelation1 = QName.valueOf("{https://random.org/ns}projection-rel3");
        QName refRelation2 = QName.valueOf("{https://random.org/ns}projection-rel4");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_LINK_REF).replace(
                        new ObjectReferenceType().oid(refTargetOid.toString())
                                .type(ShadowType.COMPLEX_TYPE).relation(refRelation1),
                        new ObjectReferenceType().oid(refTargetOid.toString())
                                .type(ShadowType.COMPLEX_TYPE).relation(refRelation2))
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        UserType userObject = repositoryService
                .getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(userObject.getLinkRef()).hasSize(2); // no more checks, we believe in Prism

        and("user row version is incremented");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);

        and("externalized refs are inserted to the dedicated table");
        QObjectReference<?> r = QObjectReferenceMapping.INSTANCE_PROJECTION.defaultAlias();
        List<MReference> refs = select(r, r.ownerOid.eq(UUID.fromString(user1Oid)));
        assertThat(refs).hasSize(2) // new added, previous three or so are gone
                .anyMatch(refRowMatcher(refTargetOid, refRelation1))
                .anyMatch(refRowMatcher(refTargetOid, refRelation2));
    }

    @Test
    public void test163ReplacingProjectionRefs()
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        OperationResult result = createOperationResult();

        given("user 1 with a few projection refs");
        UUID refTargetOid1 = UUID.randomUUID();
        UUID refTargetOid2 = UUID.randomUUID();
        QName refRelation1 = QName.valueOf("{https://random.org/ns}projection-rel1");
        QName refRelation2 = QName.valueOf("{https://random.org/ns}projection-rel2");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_LINK_REF).replace(
                        new ObjectReferenceType().oid(refTargetOid1.toString())
                                .type(ShadowType.COMPLEX_TYPE).relation(refRelation1), // to delete
                        new ObjectReferenceType().oid(refTargetOid1.toString())
                                .type(ShadowType.COMPLEX_TYPE).relation(refRelation2),
                        new ObjectReferenceType().oid(refTargetOid2.toString())
                                .type(ShadowType.COMPLEX_TYPE).relation(refRelation1),
                        new ObjectReferenceType().oid(refTargetOid2.toString())
                                .type(ShadowType.COMPLEX_TYPE).relation(refRelation2)) // to delete
                .asObjectDelta(user1Oid);
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        and("delta both adding and deleting multiple projection refs");
        UUID refTargetOid3 = UUID.randomUUID();
        UUID refTargetOid4 = UUID.randomUUID();
        delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_LINK_REF)
                .delete(new ObjectReferenceType() // type is ignored/ignorable
                                .oid(refTargetOid1.toString()).relation(refRelation1),
                        new ObjectReferenceType()
                                .oid(refTargetOid2.toString()).relation(refRelation2),
                        new ObjectReferenceType() // nonexistent anyway
                                .oid(refTargetOid3.toString()).relation(refRelation2),
                        new ObjectReferenceType() // like add bellow, will be "narrowed" out
                                .oid(refTargetOid3.toString()).relation(refRelation1))
                .add(new ObjectReferenceType().oid(refTargetOid3.toString())
                                .type(ShadowType.COMPLEX_TYPE).relation(refRelation1),
                        // delete above will be "narrowed" out, this WILL be added
                        new ObjectReferenceType().oid(refTargetOid3.toString())
                                .type(ShadowType.COMPLEX_TYPE).relation(refRelation2),
                        new ObjectReferenceType().oid(refTargetOid4.toString())
                                .type(ShadowType.COMPLEX_TYPE).relation(refRelation1),
                        new ObjectReferenceType().oid(refTargetOid4.toString())
                                .type(ShadowType.COMPLEX_TYPE).relation(refRelation2))
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        UserType userObject = repositoryService
                .getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(userObject.getLinkRef()).hasSize(6)
                .anyMatch(refMatcher(refTargetOid1, refRelation2))
                .anyMatch(refMatcher(refTargetOid2, refRelation1))
                .anyMatch(refMatcher(refTargetOid3, refRelation1))
                .anyMatch(refMatcher(refTargetOid3, refRelation2))
                .anyMatch(refMatcher(refTargetOid4, refRelation1))
                .anyMatch(refMatcher(refTargetOid4, refRelation2));

        and("user row version is incremented");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);

        and("externalized refs are inserted and deleted accordingly");
        QObjectReference<?> r = QObjectReferenceMapping.INSTANCE_PROJECTION.defaultAlias();
        List<MReference> refs = select(r, r.ownerOid.eq(UUID.fromString(user1Oid)));
        assertThat(refs).hasSize(6)
                .anyMatch(refRowMatcher(refTargetOid1, refRelation2))
                .anyMatch(refRowMatcher(refTargetOid2, refRelation1))
                .anyMatch(refRowMatcher(refTargetOid3, refRelation1))
                .anyMatch(refRowMatcher(refTargetOid3, refRelation2))
                .anyMatch(refRowMatcher(refTargetOid4, refRelation1))
                .anyMatch(refRowMatcher(refTargetOid4, refRelation2));
    }

    @Test
    public void test164DeletingAllProjectionRefsUsingReplace()
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);

        given("delta to replace projection refs with no value");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_LINK_REF).replace()
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated and has no projection refs now");
        UserType userObject = repositoryService
                .getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(userObject.getLinkRef()).isEmpty();

        and("user row version is incremented");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);

        and("externalized refs are inserted and deleted accordingly");
        QObjectReference<?> r = QObjectReferenceMapping.INSTANCE_PROJECTION.defaultAlias();
        assertThat(count(r, r.ownerOid.eq(UUID.fromString(user1Oid)))).isZero();
    }

    @Test
    public void test170AddingCreateApproverRefsUnderMetadata()
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);

        given("delta adding create approver refs for user 1");
        UUID approverOid1 = UUID.randomUUID();
        UUID approverOid2 = UUID.randomUUID();
        QName refRelation = QName.valueOf("{https://random.org/ns}approver");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_METADATA, MetadataType.F_CREATE_APPROVER_REF)
                .add(new ObjectReferenceType().oid(approverOid1.toString())
                                .type(UserType.COMPLEX_TYPE).relation(refRelation),
                        new ObjectReferenceType().oid(approverOid2.toString())
                                .type(UserType.COMPLEX_TYPE).relation(refRelation))
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        UserType userObject = repositoryService
                .getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(userObject.getMetadata().getCreateApproverRef()).hasSize(2)
                .anyMatch(refMatcher(approverOid1, refRelation))
                .anyMatch(refMatcher(approverOid2, refRelation));

        and("user row version is incremented");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);

        and("externalized refs are inserted to the dedicated table");
        QObjectReference<?> r = QObjectReferenceMapping.INSTANCE_OBJECT_CREATE_APPROVER.defaultAlias();
        UUID ownerOid = UUID.fromString(user1Oid);
        List<MReference> refs = select(r, r.ownerOid.eq(ownerOid));
        assertThat(refs).hasSize(2)
                .anyMatch(refRowMatcher(approverOid1, refRelation))
                .anyMatch(refRowMatcher(approverOid2, refRelation));
    }

    @Test
    public void test171DeletingMetadataContainerRemovesContainedRefs()
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);

        given("delta to replace metadata with no value");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_METADATA).replace()
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated and has no metadata");
        UserType userObject = repositoryService
                .getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(userObject.getMetadata()).isNull();

        and("externalized refs under metadata are removed");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);

        QObjectReference<?> r = QObjectReferenceMapping.INSTANCE_OBJECT_CREATE_APPROVER.defaultAlias();
        assertThat(count(r, r.ownerOid.eq(UUID.fromString(user1Oid)))).isZero();
        r = QObjectReferenceMapping.INSTANCE_OBJECT_MODIFY_APPROVER.defaultAlias();
        assertThat(count(r, r.ownerOid.eq(UUID.fromString(user1Oid)))).isZero();
    }
    // endregion

    // region nested (embedded) single-value containers (e.g. metadata)
    @Test
    public void test200ChangeNestedMetadataAttributeWithAddModification()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();
        MTask originalRow = selectObjectByOid(QTask.class, task1Oid);

        given("delta with metadata/createChannel (multi-part path) change for task 1 adding value");
        ObjectDelta<TaskType> delta = prismContext.deltaFor(TaskType.class)
                .item(ObjectType.F_METADATA, MetadataType.F_CREATE_CHANNEL)
                .add("any://channel")
                .asObjectDelta(task1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(TaskType.class, task1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        TaskType taskObject = repositoryService.getObject(TaskType.class, task1Oid, null, result)
                .asObjectable();
        assertThat(taskObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(taskObject.getMetadata().getCreateChannel()).isEqualTo("any://channel");

        and("externalized column is updated");
        MTask row = selectObjectByOid(QTask.class, task1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertCachedUri(row.createChannelId, "any://channel");
    }

    @Test
    public void test201DeleteNestedMetadataAttribute()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();

        given("delta with metadata/createChannel status replace to null ('delete') for task 1");
        ObjectDelta<TaskType> delta = prismContext.deltaFor(TaskType.class)
                .item(ObjectType.F_METADATA, MetadataType.F_CREATE_CHANNEL)
                .replace()
                .asObjectDelta(task1Oid);

        and("task row previously having the createChannelId value");
        MTask originalRow = selectObjectByOid(QTask.class, task1Oid);
        assertThat(originalRow.createChannelId).isNotNull();

        when("modifyObject is called");
        repositoryService.modifyObject(TaskType.class, task1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated and create channel is gone");
        TaskType taskObject = repositoryService.getObject(TaskType.class, task1Oid, null, result)
                .asObjectable();
        assertThat(taskObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertTrue(taskObject.getMetadata() == null // if removed with last item
                || taskObject.getMetadata().getCreateChannel() == null);

        and("externalized column is set to NULL");
        MTask row = selectObjectByOid(QTask.class, task1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.createChannelId).isNull();
    }

    @Test
    public void test202ChangeNestedMetadataAttributeWithReplaceModification()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();

        given("delta with metadata/createChannel (multi-part path) change for task 1 adding value");
        ObjectDelta<TaskType> delta = prismContext.deltaFor(TaskType.class)
                .item(ObjectType.F_METADATA, MetadataType.F_CREATE_CHANNEL)
                .replace("any://channel")
                .asObjectDelta(task1Oid);

        and("task row previously having no value in createChannelId (is null)");
        MTask originalRow = selectObjectByOid(QTask.class, task1Oid);
        assertThat(originalRow.createChannelId).isNull();

        when("modifyObject is called");
        repositoryService.modifyObject(TaskType.class, task1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        TaskType taskObject = repositoryService.getObject(TaskType.class, task1Oid, null, result)
                .asObjectable();
        assertThat(taskObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(taskObject.getMetadata().getCreateChannel()).isEqualTo("any://channel");

        and("externalized column is updated");
        MTask row = selectObjectByOid(QTask.class, task1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertCachedUri(row.createChannelId, "any://channel");
    }

    @Test
    public void test203AddingEmptyValueForNestedMetadataChangesNothing()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();

        given("delta with metadata add with no value for task 1");
        ObjectDelta<TaskType> delta = prismContext.deltaFor(TaskType.class)
                .item(ObjectType.F_METADATA).add()
                .asObjectDelta(task1Oid);

        and("task row previously having some value in metadata container");
        MTask originalRow = selectObjectByOid(QTask.class, task1Oid);
        assertThat(originalRow.createChannelId).isNotNull();

        when("modifyObject is called");
        repositoryService.modifyObject(TaskType.class, task1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("but nothing was updated (modifications narrowed to empty)");
        TaskType taskObject = repositoryService.getObject(TaskType.class, task1Oid, null, result)
                .asObjectable();
        assertThat(taskObject.getVersion()).isEqualTo(String.valueOf(originalRow.version));
        assertThat(taskObject.getMetadata().getCreateChannel()).isEqualTo("any://channel");

        MTask row = selectObjectByOid(QTask.class, task1Oid);
        assertThat(row.version).isEqualTo(originalRow.version);
        assertCachedUri(row.createChannelId, "any://channel");
    }

    // This one is questionable, it is technically a replace and perhaps should refuse to override
    // existing container but if it works on prism level, it must work on repository level too.
    @Test
    public void test204SetNestedMetadataContainerWithAdd()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();

        given("delta with metadata change for task 1 adding value");
        UUID modifierRefOid = UUID.randomUUID();
        QName modifierRelation = QName.valueOf("{https://random.org/ns}modifier-rel");
        ObjectDelta<TaskType> delta = prismContext.deltaFor(TaskType.class)
                .item(ObjectType.F_METADATA).add(new MetadataType()
                        .modifyChannel("any://modify-channel")
                        .modifierRef(modifierRefOid.toString(),
                                UserType.COMPLEX_TYPE, modifierRelation))
                .asObjectDelta(task1Oid);

        and("task row previously having some value in metadata container");
        MTask originalRow = selectObjectByOid(QTask.class, task1Oid);
        assertThat(originalRow.createChannelId).isNotNull();

        when("modifyObject is called");
        repositoryService.modifyObject(TaskType.class, task1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        TaskType taskObject = repositoryService.getObject(TaskType.class, task1Oid, null, result)
                .asObjectable();
        assertThat(taskObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(taskObject.getMetadata().getCreateChannel()).isNull();
        assertThat(taskObject.getMetadata().getModifyChannel()).isEqualTo("any://modify-channel");
        assertThat(taskObject.getMetadata().getModifierRef()).isNotNull(); // details checked in row

        and("externalized column is updated");
        MTask row = selectObjectByOid(QTask.class, task1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.createChannelId).isNull(); // overwritten by complete container value
        assertCachedUri(row.modifyChannelId, "any://modify-channel");
        assertThat(row.modifierRefTargetOid).isEqualTo(modifierRefOid);
        assertThat(row.modifierRefTargetType).isEqualTo(MObjectType.USER);
        assertCachedUri(row.modifierRefRelationId, modifierRelation);
    }

    @Test
    public void test205SetNestedMetadataContainerWithReplace()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();
        MTask originalRow = selectObjectByOid(QTask.class, task1Oid);

        given("delta with metadata change for task 1 replacing value");
        UUID creatorRefOid = UUID.randomUUID();
        QName creatorRelation = QName.valueOf("{https://random.org/ns}modifier-rel");
        ObjectDelta<TaskType> delta = prismContext.deltaFor(TaskType.class)
                .item(ObjectType.F_METADATA).replace(new MetadataType()
                        .createChannel("any://create-channel")
                        .modifyChannel("any://modify2-channel")
                        .creatorRef(creatorRefOid.toString(),
                                UserType.COMPLEX_TYPE, creatorRelation))
                .asObjectDelta(task1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(TaskType.class, task1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        TaskType taskObject = repositoryService.getObject(TaskType.class, task1Oid, null, result)
                .asObjectable();
        assertThat(taskObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(taskObject.getMetadata().getCreateChannel()).isEqualTo("any://create-channel");
        assertThat(taskObject.getMetadata().getModifyChannel()).isEqualTo("any://modify2-channel");
        assertThat(taskObject.getMetadata().getCreatorRef()).isNotNull(); // details checked in row
        assertThat(taskObject.getMetadata().getModifierRef()).isNull();

        and("externalized column is updated");
        MTask row = selectObjectByOid(QTask.class, task1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertCachedUri(row.createChannelId, "any://create-channel");
        assertThat(row.creatorRefTargetOid).isEqualTo(creatorRefOid);
        assertThat(row.creatorRefTargetType).isEqualTo(MObjectType.USER);
        assertCachedUri(row.creatorRefRelationId, creatorRelation);
        assertCachedUri(row.modifyChannelId, "any://modify2-channel");
        assertThat(row.modifierRefTargetOid).isNull();
        assertThat(row.modifierRefTargetType).isNull();
        assertThat(row.modifierRefRelationId).isNull();
    }

    @Test
    public void test205DeleteNestedMetadataContainerWithReplace()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();
        MTask originalRow = selectObjectByOid(QTask.class, task1Oid);

        given("delta with metadata replaced with no value for task 1");
        ObjectDelta<TaskType> delta = prismContext.deltaFor(TaskType.class)
                .item(ObjectType.F_METADATA).replace()
                .asObjectDelta(task1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(TaskType.class, task1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        TaskType taskObject = repositoryService.getObject(TaskType.class, task1Oid, null, result)
                .asObjectable();
        assertThat(taskObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(taskObject.getMetadata()).isNull();

        and("all externalized columns for metadata are cleared");
        MTask row = selectObjectByOid(QTask.class, task1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.creatorRefTargetOid).isNull();
        assertThat(row.creatorRefTargetType).isNull();
        assertThat(row.creatorRefRelationId).isNull();
        assertThat(row.createChannelId).isNull();
        assertThat(row.createTimestamp).isNull();
        assertThat(row.modifierRefTargetOid).isNull();
        assertThat(row.modifierRefTargetType).isNull();
        assertThat(row.modifierRefRelationId).isNull();
        assertThat(row.modifyChannelId).isNull();
        assertThat(row.modifyTimestamp).isNull();
    }

    @Test
    public void test206SetNestedMetadataWithEmptyContainer()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();
        MTask originalRow = selectObjectByOid(QTask.class, task1Oid);

        given("delta with metadata replaced with no value for task 1");
        ObjectDelta<TaskType> delta = prismContext.deltaFor(TaskType.class)
                .item(ObjectType.F_METADATA).replace(new MetadataType())
                .asObjectDelta(task1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(TaskType.class, task1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        TaskType taskObject = repositoryService.getObject(TaskType.class, task1Oid, null, result)
                .asObjectable();
        assertThat(taskObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(taskObject.getMetadata()).isNull();

        and("all externalized columns for metadata are cleared");
        MTask row = selectObjectByOid(QTask.class, task1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.creatorRefTargetOid).isNull();
        assertThat(row.creatorRefTargetType).isNull();
        assertThat(row.creatorRefRelationId).isNull();
        assertThat(row.createChannelId).isNull();
        assertThat(row.createTimestamp).isNull();
        assertThat(row.modifierRefTargetOid).isNull();
        assertThat(row.modifierRefTargetType).isNull();
        assertThat(row.modifierRefRelationId).isNull();
        assertThat(row.modifyChannelId).isNull();
        assertThat(row.modifyTimestamp).isNull();
    }

    @Test
    public void test207AddingEmptyNestedMetadataContainer()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();
        MTask originalRow = selectObjectByOid(QTask.class, task1Oid);

        given("delta with empty metadata added for task 1");
        ObjectDelta<TaskType> delta = prismContext.deltaFor(TaskType.class)
                .item(ObjectType.F_METADATA).add(new MetadataType())
                .asObjectDelta(task1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(TaskType.class, task1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        TaskType taskObject = repositoryService.getObject(TaskType.class, task1Oid, null, result)
                .asObjectable();
        // this one is not narrowed to empty modifications, version is incremented
        assertThat(taskObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        // but empty container is not left in the full object
        assertThat(taskObject.getMetadata()).isNull();

        and("all externalized columns for metadata are set (or left) null");
        MTask row = selectObjectByOid(QTask.class, task1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.creatorRefTargetOid).isNull();
        assertThat(row.creatorRefTargetType).isNull();
        assertThat(row.creatorRefRelationId).isNull();
        assertThat(row.createChannelId).isNull();
        assertThat(row.createTimestamp).isNull();
        assertThat(row.modifierRefTargetOid).isNull();
        assertThat(row.modifierRefTargetType).isNull();
        assertThat(row.modifierRefRelationId).isNull();
        assertThat(row.modifyChannelId).isNull();
        assertThat(row.modifyTimestamp).isNull();
    }

    @Test
    public void test210ChangeDeeplyNestedFocusPasswordCreateTimestamp()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);

        given("delta adding credential/password/metadata/createTimestamp value for user 1");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(FocusType.F_CREDENTIALS, CredentialsType.F_PASSWORD,
                        PasswordType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP)
                .add(MiscUtil.asXMLGregorianCalendar(1L))
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        UserType user = repositoryService.getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(user.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(user.getCredentials().getPassword().getMetadata()
                .getCreateTimestamp().getMillisecond()).isEqualTo(1);

        and("externalized column is updated");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.passwordCreateTimestamp).isEqualTo(Instant.ofEpochMilli(1));
    }

    @Test
    public void test211DeleteDeeplyNestedFocusPasswordCreateTimestamp()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();

        given("delta with metadata/createChannel status replace to null ('delete') for user 1");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(FocusType.F_CREDENTIALS, CredentialsType.F_PASSWORD,
                        PasswordType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP)
                .replace()
                .asObjectDelta(user1Oid);

        and("user row previously having the passwordCreateTimestamp value");
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);
        assertThat(originalRow.passwordCreateTimestamp).isNotNull();

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated and password create timestamp is gone");
        UserType userObject = repositoryService.getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertTrue(userObject.getCredentials() == null // if removed with last item
                || userObject.getCredentials().getPassword() == null
                || userObject.getCredentials().getPassword().getMetadata() == null
                || userObject.getCredentials().getPassword().getMetadata()
                .getCreateTimestamp() == null);

        and("externalized column is set to NULL");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.passwordCreateTimestamp).isNull();
    }

    @Test
    public void test212AddingDeeplyNestedEmbeddedContainer()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);

        given("delta adding whole credential/password container user 1");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(FocusType.F_CREDENTIALS, CredentialsType.F_PASSWORD)
                .replace(new PasswordType(prismContext)
                        .metadata(new MetadataType(prismContext)
                                .modifyTimestamp(MiscUtil.asXMLGregorianCalendar(1L))))
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        UserType user = repositoryService.getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(user.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(user.getCredentials().getPassword().getMetadata()
                .getModifyTimestamp().getMillisecond()).isEqualTo(1);

        and("externalized column is updated");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.passwordCreateTimestamp).isNull(); // not set, left as null
        assertThat(row.passwordModifyTimestamp).isEqualTo(Instant.ofEpochMilli(1));
    }

    @Test
    public void test213OverwritingParentOfDeeplyNestedEmbeddedContainer()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);

        given("delta adding whole credential/password container user 1");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(FocusType.F_CREDENTIALS)
                .replace(new CredentialsType(prismContext)
                        .password(new PasswordType(prismContext)
                                .metadata(new MetadataType(prismContext)
                                        .createTimestamp(MiscUtil.asXMLGregorianCalendar(1L)))))
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        UserType user = repositoryService.getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(user.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(user.getCredentials().getPassword().getMetadata()
                .getCreateTimestamp().getMillisecond()).isEqualTo(1);
        assertThat(user.getCredentials().getPassword().getMetadata().getModifyTimestamp())
                .isNull();

        and("externalized column is updated");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.passwordModifyTimestamp).isNull(); // cleared
        assertThat(row.passwordCreateTimestamp).isEqualTo(Instant.ofEpochMilli(1));
    }
    // endregion

    // region multi-value containers (e.g. assignments)
    @Test
    public void test300AddAssignmentStoresItAndGeneratesMissingId()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);

        given("delta adding assignment for user 1");
        UUID roleOid = UUID.randomUUID();
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .add(new AssignmentType(prismContext)
                        .targetRef(roleOid.toString(), RoleType.COMPLEX_TYPE)) // default relation
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        UserType UserObject = repositoryService.getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(UserObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        List<AssignmentType> assignments = UserObject.getAssignment();
        assertThat(assignments).isNotNull();
        // next free CID was assigned
        assertThat(assignments.get(0).getId()).isEqualTo(originalRow.containerIdSeq);

        and("assignment row is created");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);

        QAssignment<?> a = QAssignmentMapping.INSTANCE.defaultAlias();
        MAssignment aRow = selectOne(a, a.ownerOid.eq(UUID.fromString(user1Oid)));
        assertThat(aRow.cid).isEqualTo(originalRow.containerIdSeq);
        assertThat(aRow.containerType).isEqualTo(MContainerType.ASSIGNMENT);
        assertThat(aRow.targetRefTargetOid).isEqualTo(roleOid);
        assertThat(aRow.targetRefTargetType).isEqualTo(MObjectType.ROLE);
        assertCachedUri(aRow.targetRefRelationId, relationRegistry.getDefaultRelation());
    }

    @Test
    public void test301ReplaceItemUnderMultiValueAssignment()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();

        given("delta replacing single-value item inside assignment for user 1");
        QAssignment<?> a = QAssignmentMapping.INSTANCE.defaultAlias();
        MAssignment origAssignmentRow = selectOne(a, a.ownerOid.eq(UUID.fromString(user1Oid)));
        assertThat(origAssignmentRow.orderValue).isNull(); // wasn't previously set
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, origAssignmentRow.cid, AssignmentType.F_ORDER)
                .replace(47)
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        UserType UserObject = repositoryService.getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(UserObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        List<AssignmentType> assignments = UserObject.getAssignment();
        assertThat(assignments).isNotNull();
        assertThat(assignments.get(0).getOrder()).isEqualTo(47);

        and("assignment row is created");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);

        MAssignment aRow = selectOne(a, a.ownerOid.eq(UUID.fromString(user1Oid)));
        assertThat(aRow.cid).isEqualTo(origAssignmentRow.cid); // CID should not change
        assertThat(aRow.orderValue).isEqualTo(47);
        assertThat(aRow.targetRefTargetOid).isNotNull(); // target ref is still present
    }

    @Test
    public void test302AddingMoreAssignmentsIncludingNestedContainersAndRefs()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);

        given("delta adding two more assignments for user 1");
        UUID roleOid = UUID.randomUUID();
        UUID resourceOid = UUID.randomUUID();
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .add(new AssignmentType(prismContext)
                                .targetRef(roleOid.toString(), RoleType.COMPLEX_TYPE)
                                .metadata(new MetadataType()
                                        .createChannel("create-channel")
                                        .createApproverRef(UUID.randomUUID().toString(),
                                                UserType.COMPLEX_TYPE)
                                        .createApproverRef(UUID.randomUUID().toString(),
                                                UserType.COMPLEX_TYPE))
                                .order(48),
                        new AssignmentType(prismContext)
                                .construction(new ConstructionType()
                                        .resourceRef(resourceOid.toString(),
                                                ResourceType.COMPLEX_TYPE))
                                .order(49))
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        UserType UserObject = repositoryService.getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(UserObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        List<AssignmentType> assignments = UserObject.getAssignment();
        assertThat(assignments).hasSize(3)
                .allMatch(a -> a.getId() != null && a.getId() < originalRow.containerIdSeq + 2);

        and("assignment rows are created");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.containerIdSeq).isEqualTo(originalRow.containerIdSeq + 2);

        QAssignment<?> a = QAssignmentMapping.INSTANCE.defaultAlias();
        List<MAssignment> aRows = select(a, a.ownerOid.eq(UUID.fromString(user1Oid)));
        assertThat(aRows).hasSize(3)
                .anyMatch(aRow -> aRow.cid < originalRow.containerIdSeq) // previous one
                .anyMatch(aRow -> aRow.cid == row.containerIdSeq - 2
                        && aRow.orderValue == 48
                        && aRow.targetRefTargetOid.equals(roleOid)
                        && cachedUriById(aRow.createChannelId).equals("create-channel"))
                .anyMatch(aRow -> aRow.cid == row.containerIdSeq - 1
                        && aRow.resourceRefTargetOid.equals(resourceOid));

        QAssignmentReference ar =
                QAssignmentReferenceMapping.INSTANCE_ASSIGNMENT_CREATE_APPROVER.defaultAlias();
        List<MAssignmentReference> refRows = select(ar, ar.ownerOid.eq(UUID.fromString(user1Oid))
                .and(ar.assignmentCid.eq(row.containerIdSeq - 2)));
        assertThat(refRows).hasSize(2)
                .allMatch(rr -> rr.targetType == MObjectType.USER);
    }

    // TODO ok to fail now
    @Test
    public void test309DeleteAssignmentDeletesItFromTable()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);

        given("delta deleting assignment from user 1");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .delete(new AssignmentType(prismContext).id(1L))
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated and has no assignment now");
        UserType UserObject = repositoryService.getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(UserObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        List<AssignmentType> assignments = UserObject.getAssignment();
        assertThat(assignments).isEmpty();

        and("externalized column is updated");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);

        QAssignment<?> a = QAssignmentMapping.INSTANCE.defaultAlias();
        assertThat(select(a, a.ownerOid.eq(UUID.fromString(user1Oid)))).isEmpty();
    }

    // TODO: "indexed" containers: .item(ItemPath.create(UserType.F_ASSIGNMENT, 1, AssignmentType.F_EXTENSION))
    // endregion

    // TODO test for multi-value (e.g. subtypes) with item delta with both add and delete lists
    //  But with current implementation this can go to the first hundred section...?

    // TODO: photo test, should work fine, but it is kinda special, not part of full object

    // region other tests
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

    @Test
    public void test910ModificationsOfNonexistentObjectFails() throws SchemaException {
        OperationResult result = createOperationResult();

        given("delta with object name replace with null for user 1");
        UUID nonexistentOid = UUID.randomUUID();
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_NAME).replace("new-name")
                .asObjectDelta(nonexistentOid.toString());

        expect("modifyObject throws exception");
        Assertions.assertThatThrownBy(() ->
                repositoryService.modifyObject(UserType.class, nonexistentOid.toString(),
                        delta.getModifications(), result))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessageMatching("Object of type 'UserType' with OID .* was not found\\.");

        and("operation is fatal error");
        assertThatOperationResult(result).isFatalError();
    }

    @Test
    public void test990ChangeOfNonPersistedAttributeWorksOk()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);

        given("delta with email change for user 1 using property add modification");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_DESCRIPTION).replace("Description only in serialized form.")
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        UserType userObject = repositoryService.getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(userObject.getDescription()).isEqualTo("Description only in serialized form.");

        and("externalized version is updated");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
    }

    @Test
    public void test991ChangeInsideNonPersistedContainerWorksOk()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);

        given("delta with email change for user 1 using property add modification");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_BEHAVIOR, BehaviorType.F_AUTHENTICATION,
                        AuthenticationBehavioralDataType.F_FAILED_LOGINS).replace(5)
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        UserType userObject = repositoryService.getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(userObject.getBehavior().getAuthentication().getFailedLogins()).isEqualTo(5);

        and("externalized version is updated");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
    }
    // endregion
}
