/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.func;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.PATH_PASSWORD;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.PATH_PASSWORD_VALUE;

import static java.util.Comparator.comparing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import static com.evolveum.midpoint.schema.constants.MidPointConstants.NS_RI;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.EvaluationTimeType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.InfraItemName;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepoModifyOptions;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sqale.SqaleRepoBaseTest;
import com.evolveum.midpoint.repo.sqale.SqaleUtils;
import com.evolveum.midpoint.repo.sqale.jsonb.Jsonb;
import com.evolveum.midpoint.repo.sqale.qmodel.accesscert.*;
import com.evolveum.midpoint.repo.sqale.qmodel.assignment.*;
import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainerType;
import com.evolveum.midpoint.repo.sqale.qmodel.connector.QConnector;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.MUser;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QUser;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QUserMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
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
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.query.TypedQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ValueMetadataTypeUtil;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class SqaleRepoModifyObjectTest extends SqaleRepoBaseTest {

    private static final long CAMPAIGN_1_CASE_2_ID = 55L;

    private String user1Oid; // typical object
    private String task1Oid; // task has more item type variability
    private String shadow1Oid; // ditto

    private String shadow2Oid; // ditto

    private String service1Oid; // object with integer item
    private String accessCertificationCampaign1Oid;
    private UUID accCertCampaign1Case2ObjectOid;

    @BeforeClass
    public void initObjects() throws Exception {
        OperationResult result = createOperationResult();
        var resouceOid = UUID.randomUUID().toString();
        var resouce2Oid = UUID.randomUUID().toString();

        user1Oid = repositoryService.addObject(
                new UserType().name("user-1").asPrismObject(),
                null, result);
        task1Oid = repositoryService.addObject(
                new TaskType().name("task-1").asPrismObject(),
                null, result);
        shadow1Oid = repositoryService.addObject(
                new ShadowType().name("shadow-1")
                        .resourceRef(resouceOid, ResourceType.COMPLEX_TYPE)
                        .objectClass(SchemaConstants.RI_ACCOUNT_OBJECT_CLASS)
                        .asPrismObject(),
                null, result);

        shadow2Oid = repositoryService.addObject(
                new ShadowType().name("shadow-1")
                        .resourceRef(resouce2Oid, ResourceType.COMPLEX_TYPE)
                        .objectClass(SchemaConstants.RI_ACCOUNT_OBJECT_CLASS)
                        .asPrismObject(),
                null, result);

        service1Oid = repositoryService.addObject(
                new ServiceType().name("service-1").asPrismObject(),
                null, result);
        // This also indirectly tests ability to create a minimal object (mandatory fields only).
        accessCertificationCampaign1Oid = repositoryService.addObject(
                new AccessCertificationCampaignType()
                        .name("campaign-1")
                        .ownerRef(user1Oid, UserType.COMPLEX_TYPE)
                        // TODO: campaignIteration
                        .iteration(1)
                        .stageNumber(2)
                        .asPrismObject(),
                null, result);

        assertThatOperationResult(result).isSuccess();
    }

    // region various types of simple items
    @Test
    public void test100ChangeStringItem()
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
    public void test101ChangeStringItemWithPreviousValueUsingAddModification()
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
    public void test102DeleteStringItem()
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
                // without a value it would not be recognized as delete
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
    public void test110ChangeInstantItem()
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
    public void test111DeleteInstantItem()
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
    public void test115ChangeIntegerItem()
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
    public void test116DeleteIntegerItem()
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
    public void test120ChangeBooleanItem()
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
    public void test121DeleteBooleanItem()
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
    public void test125ChangeTaskFullResult()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        // This one is specific to modifiable MTask.fullResult (the one in MAuditDelta is insert-only, luckily).
        OperationResult result = createOperationResult();

        given("delta changing operation result for task 1");
        ObjectDelta<TaskType> delta = prismContext.deltaFor(TaskType.class)
                .item(TaskType.F_RESULT).replace(new OperationResultType()
                        .message("Result message")
                        .status(OperationResultStatusType.SUCCESS))
                .asObjectDelta(task1Oid);

        and("test row previously having no fullResult (null)");
        MTask originalRow = selectObjectByOid(QTask.class, task1Oid);
        assertThat(originalRow.fullResult).isNull();

        when("modifyObject is called");
        repositoryService.modifyObject(
                TaskType.class, task1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        TaskType task = repositoryService
                .getObject(TaskType.class, task1Oid, null, result)
                .asObjectable();
        assertThat(task.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(task.getResult()).isNull(); // not stored as part of fullObject

        and("externalized fullResult is updated");
        MTask row = selectObjectByOid(QTask.class, task1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.fullResult).isNotNull();

        and("task with operation result can be obtained using options");
        TaskType taskWithResult = repositoryService
                .getObject(TaskType.class, task1Oid, retrieveGetOptions(TaskType.F_RESULT), result)
                .asObjectable();
        assertThat(taskWithResult.getResult()).isNotNull();
    }

    @Test
    public void test126DeleteTaskFullResult()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();

        given("delta changing replacing operation result with no value task 1");
        ObjectDelta<TaskType> delta = prismContext.deltaFor(TaskType.class)
                .item(TaskType.F_RESULT).replace()
                .asObjectDelta(task1Oid);

        and("test row previously having non-null fullResult");
        MTask originalRow = selectObjectByOid(QTask.class, task1Oid);
        assertThat(originalRow.fullResult).isNotNull();

        when("modifyObject is called");
        repositoryService.modifyObject(
                TaskType.class, task1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        TaskType task = repositoryService
                .getObject(TaskType.class, task1Oid, null, result)
                .asObjectable();
        assertThat(task.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));

        and("externalized column is set to null");
        MTask row = selectObjectByOid(QTask.class, task1Oid);
        assertThat(row.fullResult).isNull();
    }

    @Test
    public void test130ChangePolyStringItem()
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
    public void test131DeletePolyStringItem()
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
    public void test140ChangeReferenceItem()
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
    public void test141DeleteReferenceItem()
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
    public void test143ChangeCachedUriItem()
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
    public void test144DeleteCachedUriItem()
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
    public void test146ChangeEnumItem()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();
        MTask originalRow = selectObjectByOid(QTask.class, task1Oid);

        given("delta with execution status change for task 1 adding value");
        ObjectDelta<TaskType> delta = prismContext.deltaFor(TaskType.class)
                .item(TaskType.F_EXECUTION_STATE).add(TaskExecutionStateType.SUSPENDED)
                .asObjectDelta(task1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(TaskType.class, task1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        TaskType taskObject = repositoryService.getObject(TaskType.class, task1Oid, null, result)
                .asObjectable();
        assertThat(taskObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(taskObject.getExecutionState()).isEqualTo(TaskExecutionStateType.SUSPENDED);

        and("externalized column is updated");
        MTask row = selectObjectByOid(QTask.class, task1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.executionState).isEqualTo(TaskExecutionStateType.SUSPENDED);
    }

    @Test
    public void test147DeleteEnumItem()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();

        given("delta with execution status replace to null ('delete') for task 1");
        ObjectDelta<TaskType> delta = prismContext.deltaFor(TaskType.class)
                .item(TaskType.F_EXECUTION_STATE).replace()
                .asObjectDelta(task1Oid);

        and("task row previously having the handler URI value");
        MTask originalRow = selectObjectByOid(QTask.class, task1Oid);
        assertThat(originalRow.executionState).isNotNull();

        when("modifyObject is called");
        repositoryService.modifyObject(TaskType.class, task1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated and execution status is gone");
        TaskType taskObject = repositoryService.getObject(TaskType.class, task1Oid, null, result)
                .asObjectable();
        assertThat(taskObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(taskObject.getExecutionState()).isNull();

        and("externalized column is set to NULL");
        MTask row = selectObjectByOid(QTask.class, task1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.executionState).isNull();
    }

    @Test
    public void test150PendingOperationCountColumn() throws Exception {
        OperationResult result = createOperationResult();

        given("delta adding a pending operation for shadow 1");
        ObjectDelta<ShadowType> delta = prismContext.deltaFor(ShadowType.class)
                .item(ShadowType.F_PENDING_OPERATION).add(new PendingOperationType()
                        .requestTimestamp(MiscUtil.asXMLGregorianCalendar(1L)))
                .asObjectDelta(shadow1Oid);

        and("shadow row previously having zero count of pending operations");
        MShadow originalRow = selectObjectByOid(QShadow.class, shadow1Oid);
        assertThat(originalRow.pendingOperationCount).isZero();

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
        assertThat(shadowObject.getPendingOperation()).hasSize(1);

        and("externalized column is updated");
        MShadow row = selectObjectByOid(QShadow.class, shadow1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.pendingOperationCount).isEqualTo(1);
    }

    @Test
    public void test151PendingOperationItemModification() throws Exception {
        OperationResult result = createOperationResult();

        given("delta changing property inside existing pending operation container");
        Long cid = repositoryService.getObject(ShadowType.class, shadow1Oid, null, result)
                .asObjectable().getPendingOperation().get(0).getId();
        ObjectDelta<ShadowType> delta = prismContext.deltaFor(ShadowType.class)
                .item(ShadowType.F_PENDING_OPERATION, cid, PendingOperationType.F_COMPLETION_TIMESTAMP)
                .add(MiscUtil.asXMLGregorianCalendar(2L))
                .asObjectDelta(shadow1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(
                ShadowType.class, shadow1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        ShadowType shadowObject = repositoryService
                .getObject(ShadowType.class, shadow1Oid, null, result)
                .asObjectable();
        assertThat(shadowObject.getPendingOperation()).hasSize(1);
        assertThat(shadowObject.getPendingOperation().get(0).getCompletionTimestamp())
                .isEqualTo(MiscUtil.asXMLGregorianCalendar(2L));

        and("externalized column is still having (the same) count");
        MShadow row = selectObjectByOid(QShadow.class, shadow1Oid);
        assertThat(row.pendingOperationCount).isEqualTo(1);
    }

    @Test
    public void test152PendingOperationCountStoresZeroForEmptyContainer() throws Exception {
        OperationResult result = createOperationResult();

        given("delta clearing the pending operation container for shadow 1");
        ObjectDelta<ShadowType> delta = prismContext.deltaFor(ShadowType.class)
                .item(ShadowType.F_PENDING_OPERATION).replace()
                .asObjectDelta(shadow1Oid);

        and("shadow row previously having non-zero count of pending operations");
        MShadow originalRow = selectObjectByOid(QShadow.class, shadow1Oid);
        assertThat(originalRow.pendingOperationCount).isNotZero();

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
        assertThat(shadowObject.getPendingOperation()).isNullOrEmpty();

        and("externalized column is updated to zero, not null");
        MShadow row = selectObjectByOid(QShadow.class, shadow1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.pendingOperationCount).isZero();
    }
    // endregion

    // region multi-value refs
    @Test
    public void test160AddingProjectionRefInsertsRowsToTable() throws Exception {
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
        var ret = repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

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
        QObjectReference<?> r = QObjectReferenceMapping.getForProjection().defaultAlias();
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
        QObjectReference<?> r = QObjectReferenceMapping.getForProjection().defaultAlias();
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

        given("delta replacing projection refs with relation for user 1");
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
        QObjectReference<?> r = QObjectReferenceMapping.getForProjection().defaultAlias();
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
                        // like add below, the deletion will be "narrowed" out and ignored
                        new ObjectReferenceType()
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
        QObjectReference<?> r = QObjectReferenceMapping.getForProjection().defaultAlias();
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
        QObjectReference<?> r = QObjectReferenceMapping.getForProjection().defaultAlias();
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
        assertThat(ValueMetadataTypeUtil.getCreateApproverRefs(userObject)).hasSize(2)
                .anyMatch(refMatcher(approverOid1, refRelation))
                .anyMatch(refMatcher(approverOid2, refRelation));

        and("user row version is incremented");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);

        and("externalized refs are inserted to the dedicated table");
        QObjectReference<?> r = QObjectReferenceMapping.getForCreateApprover().defaultAlias();
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

        QObjectReference<?> r = QObjectReferenceMapping.getForCreateApprover().defaultAlias();
        assertThat(count(r, r.ownerOid.eq(UUID.fromString(user1Oid)))).isZero();
        r = QObjectReferenceMapping.getForModifyApprover().defaultAlias();
        assertThat(count(r, r.ownerOid.eq(UUID.fromString(user1Oid)))).isZero();
    }

    @Test
    public void test175ReplacingProjectionRefsWithoutSpecifyingTypeShouldUseDefinition()
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);

        given("delta replacing projection refs with one without specified type");
        UUID refTargetOid = UUID.randomUUID();
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_LINK_REF).replace(
                        new ObjectReferenceType().oid(refTargetOid.toString()))
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
        assertThat(userObject.getLinkRef()).singleElement()
                .matches(r -> r.getType().equals(ShadowType.COMPLEX_TYPE));

        and("externalized refs are inserted to the dedicated table");
        QObjectReference<?> r = QObjectReferenceMapping.getForProjection().defaultAlias();
        List<MReference> refs = select(r, r.ownerOid.eq(UUID.fromString(user1Oid)));
        assertThat(refs).singleElement()
                .matches(rRow -> rRow.targetType == MObjectType.SHADOW);
    }

    // endregion

    // region array/jsonb stored multi-values
    @Test
    public void test180ReplacingSubtypeValuesSetsArrayColumn() throws Exception {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);

        given("delta to replace subtypes with a couple of values");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_SUBTYPE).replace("subtype-1", "subtype-2")
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated and has provided subtypes");
        UserType userObject = repositoryService
                .getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(userObject.getSubtype())
                .containsExactlyInAnyOrder("subtype-1", "subtype-2");

        and("column with subtypes is updated");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.subtypes).containsExactlyInAnyOrder("subtype-1", "subtype-2");
    }

    @Test
    public void test181AddingAndDeletingSubtypeValuesSetsArrayColumn() throws Exception {
        OperationResult result = createOperationResult();

        given("delta for subtypes with both delete and add values");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_SUBTYPE).delete("subtype-2", "wrong").add("subtype-3", "subtype-4")
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated and has expected subtypes");
        UserType userObject = repositoryService
                .getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userObject.getSubtype())
                .containsExactlyInAnyOrder("subtype-1", "subtype-3", "subtype-4");

        and("column with subtypes is updated");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.subtypes).containsExactlyInAnyOrder("subtype-1", "subtype-3", "subtype-4");
    }

    @Test
    public void test182DeletingAllSubtypesByValuesSetsColumnToNull() throws Exception {
        OperationResult result = createOperationResult();

        given("delta deleting all subtype values");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_SUBTYPE).delete("subtype-1", "subtype-3", "subtype-4")
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated and has no subtypes now");
        UserType userObject = repositoryService
                .getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userObject.getSubtype()).isNullOrEmpty();

        and("column with subtypes is set to null");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.subtypes).isNull();
    }

    // this section tests two things: array in container, and integer[] requiring conversion (URIs)
    @Test
    public void test185AddingAssignmentWithPolicySituations() throws Exception {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);

        // Container tests are in 3xx category, but let's focus on policy situations.
        given("delta adding assignment with policy situations");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).add(new AssignmentType()
                        .policySituation("policy-situation-1")
                        .policySituation("policy-situation-2"))
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
        assertThat(userObject.getAssignment().get(0).getPolicySituation())
                .containsExactlyInAnyOrder("policy-situation-1", "policy-situation-2");

        and("policySituation column is set in the assignment row");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);

        QAssignment<?> a = QAssignmentMapping.getAssignmentMapping().defaultAlias();
        MAssignment aRow = selectOne(a, a.ownerOid.eq(UUID.fromString(user1Oid)));
        assertThat(aRow.policySituations)
                .extracting(uriId -> cachedUriById(uriId))
                .containsExactlyInAnyOrder("policy-situation-1", "policy-situation-2");
    }

    @Test
    public void test186AddingAndDeletingAssignmentPolicySituations() throws Exception {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);
        long assignmentId = originalRow.containerIdSeq - 1;

        given("delta both adding and deleting assignment's policy situations");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, assignmentId, AssignmentType.F_POLICY_SITUATION)
                .delete("policy-situation-2", "wrong").add("policy-situation-3")
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        UserType userObject = repositoryService
                .getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userObject.getAssignment().get(0).getPolicySituation())
                .containsExactlyInAnyOrder("policy-situation-1", "policy-situation-3");

        and("column with assignment policy situations is updated accordingly");
        QAssignment<?> a = QAssignmentMapping.getAssignmentMapping().defaultAlias();
        MAssignment aRow = selectOne(a, a.ownerOid.eq(UUID.fromString(user1Oid)));
        assertThat(aRow.policySituations)
                .extracting(uriId -> cachedUriById(uriId))
                .containsExactlyInAnyOrder("policy-situation-1", "policy-situation-3");
    }

    @Test
    public void test187ReplacingAssignmentPolicySituationsValues() throws Exception {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);
        long assignmentId = originalRow.containerIdSeq - 1;

        given("delta replacing assignment's policy situations");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, assignmentId, AssignmentType.F_POLICY_SITUATION)
                .replace("policy-situation-a", "policy-situation-z")
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        UserType userObject = repositoryService
                .getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userObject.getAssignment().get(0).getPolicySituation())
                .containsExactlyInAnyOrder("policy-situation-a", "policy-situation-z");

        and("column with assignment policy situations is updated accordingly");
        QAssignment<?> a = QAssignmentMapping.getAssignmentMapping().defaultAlias();
        MAssignment aRow = selectOne(a, a.ownerOid.eq(UUID.fromString(user1Oid)));
        assertThat(aRow.policySituations)
                .extracting(uriId -> cachedUriById(uriId))
                .containsExactlyInAnyOrder("policy-situation-a", "policy-situation-z");
    }

    @Test
    public void test188ReplacingAssignmentPolicySituationsWithNoValue() throws Exception {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);
        long assignmentId = originalRow.containerIdSeq - 1;

        given("delta replacing assignment's policy situations with no value");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, assignmentId, AssignmentType.F_POLICY_SITUATION)
                .replace()
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        UserType userObject = repositoryService
                .getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userObject.getAssignment().get(0).getPolicySituation()).isNullOrEmpty();

        and("column with assignment policy situations is updated accordingly");
        QAssignment<?> a = QAssignmentMapping.getAssignmentMapping().defaultAlias();
        MAssignment aRow = selectOne(a, a.ownerOid.eq(UUID.fromString(user1Oid)));
        assertThat(aRow.policySituations).isNull();
    }



    @Test
    public void test190ReplacingOrganizationValuesSetsJsonbColumn() throws Exception {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);

        given("delta to replace organizations with a couple of values");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ORGANIZATION).replace(
                        new PolyString("orgmod-1"), new PolyString("orgmod-2"))
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated and has provided organizations");
        UserType userObject = repositoryService
                .getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(userObject.getOrganization())
                .extracting(p -> p.getOrig())
                .containsExactlyInAnyOrder("orgmod-1", "orgmod-2");

        and("column with organizations is updated");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.organizations).isNotNull();
        assertThat(Jsonb.toList(row.organizations)).containsExactlyInAnyOrder(
                Map.of("o", "orgmod-1", "n", "orgmod1"),
                Map.of("o", "orgmod-2", "n", "orgmod2"));
    }

    @Test
    public void test191AddingAndDeletingOrganizationValuesSetsArrayColumn() throws Exception {
        OperationResult result = createOperationResult();

        given("delta for organizations with both delete and add values");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ORGANIZATION)
                .delete(new PolyString("orgmod-2"), new PolyString("wrong"))
                .add(new PolyString("orgmod-3"), new PolyString("orgmod-4"))
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated and has expected organizations");
        UserType userObject = repositoryService
                .getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userObject.getOrganization())
                .extracting(p -> p.getOrig())
                .containsExactlyInAnyOrder("orgmod-1", "orgmod-3", "orgmod-4");

        and("column with organizations is updated");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(Jsonb.toList(row.organizations)).containsExactlyInAnyOrder(
                Map.of("o", "orgmod-1", "n", "orgmod1"),
                Map.of("o", "orgmod-3", "n", "orgmod3"),
                Map.of("o", "orgmod-4", "n", "orgmod4"));
    }

    @Test
    public void test192DeletingAllOrganizationsByValuesSetsColumnToNull() throws Exception {
        OperationResult result = createOperationResult();

        given("delta deleting all subtype values");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ORGANIZATION).delete(new PolyString("orgmod-1"),
                        new PolyString("orgmod-3"), new PolyString("orgmod-4"))
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated and has no subtypes now");
        UserType userObject = repositoryService
                .getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userObject.getOrganization()).isNullOrEmpty();

        and("column with subtypes is set to null");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.organizations).isNull();
    }

    @Test
    public void test193ReindexUserDoesNotLooseData() throws Exception {
        OperationResult result = createOperationResult();
        var userBefore = repositoryService
                .getObject(UserType.class, user1Oid, null, result);
        repositoryService.modifyObject(UserType.class, user1Oid, Collections.emptyList(), RepoModifyOptions.createForceReindex(), result);
        var userAfter = repositoryService.getObject(UserType.class, user1Oid, null, result);
        assertThat(userAfter).isEqualTo(userBefore);
    }


    @Test
    public void test195ConnectorUpdateToNullConnectorHost() throws Exception {
        OperationResult result = createOperationResult();

        given("connector with non-null connector host");
        String oid = repositoryService.addObject(
                new ConnectorType()
                        .name("conn-1")
                        .connectorBundle("com.connector.package")
                        .connectorType("ConnectorTypeClass")
                        .connectorVersion("1.2.3")
                        .framework(SchemaConstants.UCF_FRAMEWORK_URI_BUILTIN)
                        .connectorHostRef(UUID.randomUUID().toString(), ConnectorHostType.COMPLEX_TYPE)
                        .targetSystemType("type1")
                        .targetSystemType("type2")
                        .asPrismObject(), null, result);
        assertThat(selectObjectByOid(QConnector.class, oid).connectorHostRefTargetOid).isNotNull();

        and("delta setting null connector host reference");
        ObjectDelta<UserType> delta = prismContext.deltaFor(ConnectorType.class)
                .item(ConnectorType.F_CONNECTOR_HOST_REF).replace()
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(ConnectorType.class, oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();
        // For a day, placeholder value was used instead of NULL, now this test is nothing special, but stays.
        assertThat(selectObjectByOid(QConnector.class, oid).connectorHostRefTargetOid).isNull();
    }
    // endregion

    // region nested (embedded) single-value containers (e.g. metadata)
    @Test
    public void test200ChangeNestedMetadataItemWithAddModification()
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
        assertThat(ValueMetadataTypeUtil.getStorageMetadata(taskObject).getCreateChannel()).isEqualTo("any://channel");

        and("externalized column is updated");
        MTask row = selectObjectByOid(QTask.class, task1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertCachedUri(row.createChannelId, "any://channel");
    }

    @Test
    public void test201DeleteNestedMetadataItem()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();

        given("delta with metadata/createChannel status replace to null ('delete') for task 1");
        ObjectDelta<TaskType> delta = prismContext.deltaFor(TaskType.class)
                .item(InfraItemName.METADATA, 1, ValueMetadataType.F_STORAGE, MetadataType.F_CREATE_CHANNEL)
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
        var storageMeta = ValueMetadataTypeUtil.getStorageMetadata(taskObject);
        assertTrue(storageMeta == null // if removed with last item
                || storageMeta.getCreateChannel() == null);

        and("externalized column is set to NULL");
        MTask row = selectObjectByOid(QTask.class, task1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.createChannelId).isNull();
    }

    @Test
    public void test202ChangeNestedMetadataItemWithReplaceModification()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();

        given("delta with metadata/createChannel (multi-part path) change for task 1 adding value");
        ObjectDelta<TaskType> delta = prismContext.deltaFor(TaskType.class)
                .item(InfraItemName.METADATA, 1, ValueMetadataType.F_STORAGE, MetadataType.F_CREATE_CHANNEL)
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
        assertThat(ValueMetadataTypeUtil.getStorageMetadata(taskObject).getCreateChannel()).isEqualTo("any://channel");

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
                .item(InfraItemName.METADATA, 1, ValueMetadataType.F_STORAGE).add()
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
        assertThat(ValueMetadataTypeUtil.getStorageMetadata(taskObject).getCreateChannel()).isEqualTo("any://channel");

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
                .item(InfraItemName.METADATA, 1, ValueMetadataType.F_STORAGE).add(new StorageMetadataType()
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

        var storage = ValueMetadataTypeUtil.getStorageMetadata(taskObject);
        assertThat(taskObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(storage.getCreateChannel()).isNull();
        assertThat(storage.getModifyChannel()).isEqualTo("any://modify-channel");
        assertThat(storage.getModifierRef()).isNotNull(); // details checked in row

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
                .item(InfraItemName.METADATA, 1, ValueMetadataType.F_STORAGE).replace(new StorageMetadataType()
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
        var storage = ValueMetadataTypeUtil.getStorageMetadata(taskObject);
        assertThat(taskObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(storage.getCreateChannel()).isEqualTo("any://create-channel");
        assertThat(storage.getModifyChannel()).isEqualTo("any://modify2-channel");
        assertThat(storage.getCreatorRef()).isNotNull(); // details checked in row
        assertThat(storage.getModifierRef()).isNull();

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
                .item(InfraItemName.METADATA, 1, ValueMetadataType.F_STORAGE).replace(new StorageMetadataType())
                .asObjectDelta(task1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(TaskType.class, task1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        TaskType taskObject = repositoryService.getObject(TaskType.class, task1Oid, null, result)
                .asObjectable();
        assertThat(taskObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(ValueMetadataTypeUtil.getStorageMetadata(taskObject)).isNotNull()
                .matches(m -> m.asPrismContainerValue().isEmpty());

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
    public void test207SettingMetadataContainerToNull()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();
        MTask originalRow = selectObjectByOid(QTask.class, task1Oid);

        given("delta replacing metadata with nothing for task 1");
        ObjectDelta<TaskType> delta = prismContext.deltaFor(TaskType.class)
                .item(InfraItemName.METADATA, 1, ValueMetadataType.F_STORAGE).replace()
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
    }

    @Test
    public void test208AddingEmptyNestedMetadataContainer()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();
        MTask originalRow = selectObjectByOid(QTask.class, task1Oid);

        given("delta with empty metadata added for task 1");
        ObjectDelta<TaskType> delta = prismContext.deltaFor(TaskType.class)
                .item(InfraItemName.METADATA, 1, ValueMetadataType.F_STORAGE).add(new StorageMetadataType())
                .asObjectDelta(task1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(TaskType.class, task1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        TaskType taskObject = repositoryService.getObject(TaskType.class, task1Oid, null, result)
                .asObjectable();
        assertThat(taskObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(ValueMetadataTypeUtil.getStorageMetadata(taskObject)).isNotNull()
                .matches(m -> m.asPrismContainerValue().isEmpty());
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
                .replace(new PasswordType()
                        .metadata(new MetadataType()
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
                .replace(new CredentialsType()
                        .password(new PasswordType()
                                .metadata(new MetadataType()
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
    public void test300AddedAssignmentStoresItAndGeneratesMissingId()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);

        given("delta replacing assignments for user 1 with a single one");
        UUID roleOid = UUID.randomUUID();
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .replace(new AssignmentType()
                        .targetRef(roleOid.toString(), RoleType.COMPLEX_TYPE)) // default relation
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        UserType userObject = repositoryService.getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        List<AssignmentType> assignments = userObject.getAssignment();
        assertThat(assignments).isNotNull();
        // next free CID was assigned
        assertThat(assignments.get(0).getId()).isEqualTo(originalRow.containerIdSeq);

        and("assignment row is created");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);

        QAssignment<?> a = QAssignmentMapping.getAssignmentMapping().defaultAlias();
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
        QAssignment<?> a = QAssignmentMapping.getAssignmentMapping().defaultAlias();
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
        UserType userObject = repositoryService.getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        List<AssignmentType> assignments = userObject.getAssignment();
        assertThat(assignments).isNotNull();
        assertThat(assignments.get(0).getOrder()).isEqualTo(47);

        and("assignment row is updated properly");
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
                .add(new AssignmentType()
                                .targetRef(roleOid.toString(), RoleType.COMPLEX_TYPE)
                                .metadata(new MetadataType()
                                        .createChannel("create-channel")
                                        .createApproverRef(UUID.randomUUID().toString(),
                                                UserType.COMPLEX_TYPE)
                                        .createApproverRef(UUID.randomUUID().toString(),
                                                UserType.COMPLEX_TYPE))
                                .order(48),
                        new AssignmentType()
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
        UserType userObject = repositoryService.getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        List<AssignmentType> assignments = userObject.getAssignment();
        assertThat(assignments).hasSize(3)
                .allMatch(a -> a.getId() != null && a.getId() < originalRow.containerIdSeq + 2);

        and("new assignment rows are created");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.containerIdSeq).isEqualTo(originalRow.containerIdSeq + 2);

        QAssignment<?> a = QAssignmentMapping.getAssignmentMapping().defaultAlias();
        List<MAssignment> aRows = select(a, a.ownerOid.eq(UUID.fromString(user1Oid)));
        assertThat(aRows).hasSize(3)
                .anyMatch(aRow -> aRow.cid < originalRow.containerIdSeq) // previous one
                .anyMatch(aRow -> aRow.cid == row.containerIdSeq - 2
                        && aRow.orderValue == 48
                        && aRow.targetRefTargetOid.equals(roleOid)
                        && cachedUriById(aRow.createChannelId).equals("create-channel"))
                .anyMatch(aRow -> aRow.cid == row.containerIdSeq - 1
                        && aRow.resourceRefTargetOid.equals(resourceOid));

        var ar = QAssignmentReferenceMapping.getForAssignmentCreateApprover().defaultAlias();
        List<MAssignmentReference> refRows = select(ar, ar.ownerOid.eq(UUID.fromString(user1Oid))
                .and(ar.assignmentCid.eq(row.containerIdSeq - 2)));
        assertThat(refRows).hasSize(2)
                .allMatch(rr -> rr.targetType == MObjectType.USER);
    }

    @Test
    public void test303ModificationsOnOneAssignmentDoesNotAffectOthers()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);

        given("delta changing item inside single assignments for user 1");
        QAssignment<?> a = QAssignmentMapping.getAssignmentMapping().defaultAlias();
        long assOrder49Cid = selectOne(a,
                a.ownerOid.eq(UUID.fromString(user1Oid)), a.orderValue.eq(49)).cid;
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, assOrder49Cid, AssignmentType.F_ORDER)
                .replace(50)
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        UserType user = repositoryService.getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(user.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        List<AssignmentType> assignments = user.getAssignment();
        assertThat(assignments).hasSize(3)
                .anyMatch(ass -> ass.getOrder() == 47)
                .anyMatch(ass -> ass.getOrder() == 48)
                .anyMatch(ass -> ass.getOrder() == 50);

        and("assignment row is modified");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);

        MAssignment aRow = selectOne(a,
                a.ownerOid.eq(UUID.fromString(user1Oid)), a.cid.eq(assOrder49Cid));
        assertThat(aRow.orderValue).isEqualTo(50);

        and("no other assignment rows are modified");
        assertThat(count(a, a.ownerOid.eq(UUID.fromString(user1Oid)), a.orderValue.eq(50)))
                .isEqualTo(1);
    }

    @Test
    public void test304MultipleModificationsOfExistingAssignment()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);

        given("delta changing multiple assignments for user 1");
        QAssignment<?> a = QAssignmentMapping.getAssignmentMapping().defaultAlias();
        long assOrder48Cid = selectOne(a,
                a.ownerOid.eq(UUID.fromString(user1Oid)), a.orderValue.eq(48)).cid;
        long assOrder50Cid = selectOne(a,
                a.ownerOid.eq(UUID.fromString(user1Oid)), a.orderValue.eq(50)).cid;
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, assOrder48Cid, AssignmentType.F_ORDER)
                .replace(50)
                .item(UserType.F_ASSIGNMENT, assOrder48Cid, AssignmentType.F_METADATA)
                .replace()
                .item(UserType.F_ASSIGNMENT, assOrder50Cid, AssignmentType.F_CONSTRUCTION)
                .replace()
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        UserType user = repositoryService.getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(user.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        List<AssignmentType> assignments = user.getAssignment();
        assertThat(assignments).hasSize(3)
                .anyMatch(ass -> ass.getOrder() == 47) // first one stays 47
                .anyMatch(ass -> ass.getOrder() == 50  // previously 48
                        && ass.getMetadata() == null // removed metadata
                        && ass.getTargetRef() != null)
                .anyMatch(ass -> ass.getOrder() == 50
                        && ass.getMetadata() == null // never had any
                        && ass.getTargetRef() == null
                        && ass.getConstruction() == null); // removed construction

        and("assignment row is created");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);

        assertThat(select(a, a.ownerOid.eq(UUID.fromString(user1Oid)))).hasSize(3)
                .anyMatch(ass -> ass.orderValue == 47) // first one stays 47
                .anyMatch(ass -> ass.orderValue == 50  // previously 48
                        && ass.createChannelId == null // removed metadata
                        && ass.targetRefTargetOid != null)
                .anyMatch(ass -> ass.orderValue == 50
                        && ass.createChannelId == null // never had any
                        && ass.targetRefTargetOid == null
                        && ass.resourceRefTargetOid == null); // removed construction

        // Approver references were removed from the only assignment that had them.
        QAssignmentReference<?> ar =
                QAssignmentReferenceMapping.getForAssignmentCreateApprover().defaultAlias();
        assertThat(count(ar, ar.ownerOid.eq(UUID.fromString(user1Oid)))).isZero();
    }

    @Test
    public void test305DeleteAssignmentByContent() // or by value, or by equality
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);

        given("delta deleting assignments without CID by equality for user 1");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .delete(new AssignmentType().order(50))
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated and assignments with only order 50");
        UserType userObject = repositoryService.getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        List<AssignmentType> assignments = userObject.getAssignment();
        assertThat(assignments).hasSize(2)
                .anyMatch(a -> a.getOrder().equals(47))
                // this one had order AND target ref, so it's no match
                .anyMatch(ass -> ass.getOrder() == 50 && ass.getTargetRef() != null);

        and("corresponding assignment row is deleted");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);

        QAssignment<?> a = QAssignmentMapping.getAssignmentMapping().defaultAlias();
        List<MAssignment> aRows = select(a, a.ownerOid.eq(UUID.fromString(user1Oid)));
        assertThat(aRows).hasSize(2)
                .anyMatch(aRow -> aRow.orderValue.equals(47))
                .anyMatch(ass -> ass.orderValue == 50 && ass.targetRefTargetOid != null);
    }

    @Test
    public void test307RepeatedContainerAdditionDoesNotAddDuplicates()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();
        QAssignment<?> a = QAssignmentMapping.getAssignmentMapping().defaultAlias();
        long origAssignmentCount = count(a, a.ownerOid.eq(UUID.fromString(user1Oid)));

        given("delta replacing assignments for user 1 with a single one applied to object once");
        UUID roleOid = UUID.randomUUID();
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .add(new AssignmentType()
                        .targetRef(roleOid.toString(), RoleType.COMPLEX_TYPE))
                .asObjectDelta(user1Oid);
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);
        assertThatOperationResult(result).isSuccess();
        assertThat(count(a, a.ownerOid.eq(UUID.fromString(user1Oid))))
                .isEqualTo(origAssignmentCount + 1);

        when("the same delta is executed repeatedly");
        // this one is easy, modification is actually narrowed out as it is the same (ignoring ID)
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("no new container should be added");
        assertThatOperationResult(result).isSuccess();
        assertThat(count(a, a.ownerOid.eq(UUID.fromString(user1Oid))))
                .isEqualTo(origAssignmentCount + 1);

        and("serialized form also contains only one more assignment");
        UserType userObject = repositoryService.getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        List<AssignmentType> assignments = userObject.getAssignment();
        assertThat(assignments).hasSize((int) (origAssignmentCount + 1));
    }

    @Test
    public void test308RepeatedContainerAdditionWithDifferentOperationalItemsDoesNotAddDuplicates()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();
        QAssignment<?> a = QAssignmentMapping.getAssignmentMapping().defaultAlias();
        long origAssignmentCount = count(a, a.ownerOid.eq(UUID.fromString(user1Oid)));

        given("delta replacing assignments for user 1 with a single one applied to object once");
        UUID roleOid = UUID.randomUUID();
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .add(new AssignmentType()
                        .targetRef(roleOid.toString(), RoleType.COMPLEX_TYPE)
                        .metadata(new MetadataType()
                                .createTimestamp(MiscUtil.asXMLGregorianCalendar(1L))))
                .asObjectDelta(user1Oid);
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);
        assertThatOperationResult(result).isSuccess();
        assertThat(count(a, a.ownerOid.eq(UUID.fromString(user1Oid))))
                .isEqualTo(origAssignmentCount + 1);

        when("add delta with similar container with only operational items different");
        delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .add(new AssignmentType()
                        .targetRef(roleOid.toString(), RoleType.COMPLEX_TYPE)
                        .metadata(new MetadataType()
                                .createTimestamp(MiscUtil.asXMLGregorianCalendar(2L))))
                .asObjectDelta(user1Oid);
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("serialized form contains no new assignments");
        assertThatOperationResult(result).isSuccess();
        UserType userObject = repositoryService.getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        List<AssignmentType> assignments = userObject.getAssignment();
        assertThat(assignments).hasSize((int) (origAssignmentCount + 1));

        and("no new row is added, but assignment row is updated");
        List<MAssignment> aRows = select(a, a.ownerOid.eq(UUID.fromString(user1Oid)));
        assertThat(aRows).hasSize(assignments.size())
                .anyMatch(aRow -> aRow.targetRefTargetOid.equals(roleOid)
                        && aRow.createTimestamp.equals(Instant.ofEpochMilli(2)));
    }

    @Test
    public void test310AddingAssignmentWithNewPrefilledCid()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);
        QAssignment<?> a = QAssignmentMapping.getAssignmentMapping().defaultAlias();
        int origAssignmentCount = (int) count(a, a.ownerOid.eq(UUID.fromString(user1Oid)));

        given("delta adding assignments with free CID for user 1");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .add(new AssignmentType()
                        .id(originalRow.containerIdSeq)
                        .order(1))
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated and provided CID is used");
        UserType userObject = repositoryService.getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        List<AssignmentType> assignments = userObject.getAssignment();
        assertThat(assignments).hasSize(origAssignmentCount + 1)
                .anyMatch(ass -> ass.getId().equals(originalRow.containerIdSeq));

        and("new assignment row is created");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.containerIdSeq).isEqualTo(originalRow.containerIdSeq + 1);

        List<MAssignment> aRows = select(a, a.ownerOid.eq(UUID.fromString(user1Oid)));
        assertThat(aRows).hasSize(assignments.size())
                .anyMatch(aRow -> aRow.cid.equals(originalRow.containerIdSeq)
                        && aRow.orderValue == 1);
    }

    @Test
    public void test311DeleteAssignmentByCid()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);
        QAssignment<?> a = QAssignmentMapping.getAssignmentMapping().defaultAlias();
        int origAssignmentCount = (int) count(a, a.ownerOid.eq(UUID.fromString(user1Oid)));

        given("delta deleting assignments using CID for user 1");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .delete(new AssignmentType()
                        .id(originalRow.containerIdSeq - 1)) // last added assignment
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        UserType userObject = repositoryService.getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        List<AssignmentType> assignments = userObject.getAssignment();
        assertThat(assignments).hasSize(origAssignmentCount - 1)
                .noneMatch(ass -> ass.getId().equals(originalRow.containerIdSeq - 1));

        and("new assignment row is created");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.containerIdSeq).isEqualTo(originalRow.containerIdSeq); // no need for change

        List<MAssignment> aRows = select(a, a.ownerOid.eq(UUID.fromString(user1Oid)));
        assertThat(aRows).hasSize(assignments.size())
                .noneMatch(aRow -> aRow.cid.equals(originalRow.containerIdSeq - 1));
    }

    @Test
    public void test312AddingAssignmentWithUsedButFreeCid()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);
        QAssignment<?> a = QAssignmentMapping.getAssignmentMapping().defaultAlias();
        int origAssignmentCount = (int) count(a, a.ownerOid.eq(UUID.fromString(user1Oid)));

        // this is NOT recommended in practice, reusing previous CIDs is messy
        given("delta adding assignments with used but now free CID for user 1");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .add(new AssignmentType()
                        .id(originalRow.containerIdSeq - 1)
                        .order(1))
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated and provided CID is used");
        UserType userObject = repositoryService.getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        List<AssignmentType> assignments = userObject.getAssignment();
        assertThat(assignments).hasSize(origAssignmentCount + 1)
                .anyMatch(ass -> ass.getId().equals(originalRow.containerIdSeq - 1));

        and("new assignment row is created");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.containerIdSeq).isEqualTo(originalRow.containerIdSeq); // no change

        List<MAssignment> aRows = select(a, a.ownerOid.eq(UUID.fromString(user1Oid)));
        assertThat(aRows).hasSize(assignments.size())
                .anyMatch(aRow -> aRow.cid.equals(originalRow.containerIdSeq - 1)
                        && aRow.orderValue == 1);
    }

    @Test
    public void test315ReplaceAssignmentWithCid()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);

        given("object with existing container values");
        List<AssignmentType> assignments = repositoryService
                .getObject(UserType.class, user1Oid, null, result)
                .asObjectable()
                .getAssignment()
                .stream()
                .map(ass -> ass.clone() // we need to get it out of original parent
                        .lifecycleState(String.valueOf(ass.getId()))) // some change
                .toList();

        and("delta replacing the values with the same values again (with CIDs already)");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .replace(assignments.stream()
                        .map(ass -> ass.asPrismContainerValue())
                        .collect(Collectors.toList()))
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        UserType userObject = repositoryService.getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        List<AssignmentType> newAssignments = userObject.getAssignment();
        assertThat(newAssignments).hasSize(assignments.size());

        and("new assignment rows replace the old ones");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.containerIdSeq).isEqualTo(originalRow.containerIdSeq); // no need for change

        QAssignment<?> a = QAssignmentMapping.getAssignmentMapping().defaultAlias();
        List<MAssignment> aRows = select(a, a.ownerOid.eq(UUID.fromString(user1Oid)));
        assertThat(aRows).hasSize(assignments.size())
                .allMatch(aRow -> String.valueOf(aRow.cid).equals(aRow.lifecycleState));
    }

    @Test
    public void test320AddingAssignmentWithItemPathEndingWithCidIsIllegal() {
        expect("creating delta adding assignment to path ending with CID throws exception");
        assertThatThrownBy(
                () -> prismContext.deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT, 5).add(
                                new AssignmentType().order(5))
                        .asObjectDelta(user1Oid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid delta path assignment/5."
                        + " Delta path must always point to item, not to value");
    }

    @Test
    public void test321AddingAssignmentWithItemPathEndingWithCidIsIllegal() {
        expect("creating delta replacing assignment at path ending with CID throws exception");
        assertThatThrownBy(
                () -> prismContext.deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT, 5).replace(
                                new AssignmentType().order(5))
                        .asObjectDelta(user1Oid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid delta path assignment/5."
                        + " Delta path must always point to item, not to value");
    }

    @Test
    public void test322DeleteAssignmentWithItemPathEndingWithCidIsIllegal() {
        expect("creating delta deleting assignment path ending with CID throws exception");
        assertThatThrownBy(
                () -> prismContext.deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT, 5).delete()
                        .asObjectDelta(user1Oid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid delta path assignment/5."
                        + " Delta path must always point to item, not to value");
    }

    private @NotNull Collection<SelectorOptions<GetOperationOptions>> retrieveWithCases() {
        return retrieveGetOptions(AccessCertificationCampaignType.F_CASE);
    }

    @Test
    public void test330AddedCertificationCaseStoresItAndGeneratesMissingId()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();
        MAccessCertificationCampaign originalRow = selectObjectByOid(
                QAccessCertificationCampaign.class, accessCertificationCampaign1Oid);

        given("delta adding case for campaign 1");
        UUID targetOid = UUID.randomUUID();
        AccessCertificationCaseType caseBefore = new AccessCertificationCaseType()
                .stageNumber(3)
                .iteration(4)
                .targetRef(targetOid.toString(), RoleType.COMPLEX_TYPE);
        ObjectDelta<AccessCertificationCampaignType> delta =
                prismContext.deltaFor(AccessCertificationCampaignType.class)
                        .item(AccessCertificationCampaignType.F_CASE)
                        .add(caseBefore)
                        .asObjectDelta(accessCertificationCampaign1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(AccessCertificationCampaignType.class,
                accessCertificationCampaign1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        AccessCertificationCampaignType campaignObjectAfter = repositoryService
                .getObject(AccessCertificationCampaignType.class,
                        accessCertificationCampaign1Oid, retrieveWithCases(), result)
                .asObjectable();
        assertThat(campaignObjectAfter.getVersion())
                .isEqualTo(String.valueOf(originalRow.version + 1));
        List<AccessCertificationCaseType> casesAfter = campaignObjectAfter.getCase();
        assertThat(casesAfter).isNotNull();
        // next free CID was assigned
        assertThat(casesAfter.get(0).getId()).isEqualTo(originalRow.containerIdSeq);

        and("campaign row is created");
        MAccessCertificationCampaign row = selectObjectByOid(
                QAccessCertificationCampaign.class, accessCertificationCampaign1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);

        and("case row is created");
        QAccessCertificationCase a = QAccessCertificationCaseMapping.getAccessCertificationCaseMapping().defaultAlias();
        MAccessCertificationCase aRow = selectOne(a, a.ownerOid.eq(UUID.fromString(accessCertificationCampaign1Oid)));
        assertThat(aRow.cid).isEqualTo(originalRow.containerIdSeq);
        assertThat(aRow.containerType).isEqualTo(MContainerType.ACCESS_CERTIFICATION_CASE);
        assertThat(aRow.targetRefTargetOid).isEqualTo(targetOid);
        assertThat(aRow.targetRefTargetType).isEqualTo(MObjectType.ROLE);
        assertCachedUri(aRow.targetRefRelationId, relationRegistry.getDefaultRelation());
        assertThat(aRow.stageNumber).isEqualTo(3);
        assertThat(aRow.campaignIteration).isEqualTo(4);

        assertCertificationCaseFullObject(aRow, caseBefore);
    }

    @Test
    public void test331AddedCertificationCaseStoresItFixedId()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();
        MAccessCertificationCampaign originalRow = selectObjectByOid(
                QAccessCertificationCampaign.class, accessCertificationCampaign1Oid);

        given("delta adding case for campaign 1");
        accCertCampaign1Case2ObjectOid = UUID.randomUUID();
        AccessCertificationCaseType caseBefore = new AccessCertificationCaseType()
                .id(CAMPAIGN_1_CASE_2_ID)
                .stageNumber(5)
                .iteration(7)
                .objectRef(accCertCampaign1Case2ObjectOid.toString(), UserType.COMPLEX_TYPE)
                .outcome("anyone who is capable of getting themselves made"
                        + " President should on no account be allowed to do the job");
        ObjectDelta<AccessCertificationCampaignType> delta =
                prismContext.deltaFor(AccessCertificationCampaignType.class)
                        .item(AccessCertificationCampaignType.F_CASE)
                        .add(caseBefore)
                        .asObjectDelta(accessCertificationCampaign1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(AccessCertificationCampaignType.class,
                accessCertificationCampaign1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        AccessCertificationCampaignType campaignObjectAfter = repositoryService
                .getObject(AccessCertificationCampaignType.class,
                        accessCertificationCampaign1Oid, retrieveWithCases(), result)
                .asObjectable();
        assertThat(campaignObjectAfter.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        List<AccessCertificationCaseType> casesAfter = campaignObjectAfter.getCase();
        assertThat(casesAfter).isNotNull();
        assertThat(casesAfter.get(1).getId()).isEqualTo(CAMPAIGN_1_CASE_2_ID);

        and("campaign row is created");
        MAccessCertificationCampaign row = selectObjectByOid(
                QAccessCertificationCampaign.class, accessCertificationCampaign1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);

        and("case row is created");
        QAccessCertificationCase a = QAccessCertificationCaseMapping.getAccessCertificationCaseMapping().defaultAlias();
        List<MAccessCertificationCase> caseRows = select(a, a.ownerOid.eq(UUID.fromString(accessCertificationCampaign1Oid)));
        assertThat(caseRows).hasSize(2);
        caseRows.sort(comparing(tr -> tr.cid));

        MAccessCertificationCase aRow = caseRows.get(1);
        assertThat(aRow.cid).isEqualTo(CAMPAIGN_1_CASE_2_ID);
        assertThat(aRow.containerType).isEqualTo(MContainerType.ACCESS_CERTIFICATION_CASE);
        assertThat(aRow.targetRefTargetOid).isNull();
        assertThat(aRow.objectRefTargetOid).isEqualTo(accCertCampaign1Case2ObjectOid);
        assertThat(aRow.objectRefTargetType).isEqualTo(MObjectType.USER);
        assertCachedUri(aRow.objectRefRelationId, relationRegistry.getDefaultRelation());
        assertThat(aRow.outcome).isEqualTo("anyone who is capable of getting themselves made"
                + " President should on no account be allowed to do the job");
        assertThat(aRow.stageNumber).isEqualTo(5);
        assertThat(aRow.campaignIteration).isEqualTo(7);

        assertCertificationCaseFullObject(aRow, caseBefore);
    }

    private void assertCertificationCaseFullObject(
            MAccessCertificationCase aRow, AccessCertificationCaseType caseBefore)
            throws SchemaException {
        String fullObjectStr = new String(aRow.fullObject, StandardCharsets.UTF_8);
        display("Case full object:\n" + fullObjectStr);
        // added objects have normalized relations, so we have to do this for original too
        ObjectTypeUtil.normalizeAllRelations(caseBefore.asPrismContainerValue(), relationRegistry);

        // Make sure caseBefore contains also container id, otherwise serialization will be different
        caseBefore.setId(aRow.cid);
        String caseBeforeStr = serializeFullObject(caseBefore);
        assertThat(fullObjectStr).isEqualTo(caseBeforeStr);
    }

    @Test
    public void test332ModifiedCertificationCaseStoresIt()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();
        MAccessCertificationCampaign originalRow = selectObjectByOid(QAccessCertificationCampaign.class, accessCertificationCampaign1Oid);

        given("delta adding case for campaign 1");
        ObjectDelta<AccessCertificationCampaignType> delta = prismContext.deltaFor(AccessCertificationCampaignType.class)
                .item(ItemPath.create(AccessCertificationCampaignType.F_CASE, CAMPAIGN_1_CASE_2_ID, AccessCertificationCaseType.F_OUTCOME))
                .replace("People are the problem")
                .asObjectDelta(accessCertificationCampaign1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(AccessCertificationCampaignType.class,
                accessCertificationCampaign1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        AccessCertificationCampaignType campaignObjectAfter = repositoryService
                .getObject(AccessCertificationCampaignType.class, accessCertificationCampaign1Oid, retrieveWithCases(), result)
                .asObjectable();
        assertThat(campaignObjectAfter.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        List<AccessCertificationCaseType> casesAfter = campaignObjectAfter.getCase();
        assertThat(casesAfter).isNotNull();
        assertThat(casesAfter.get(1).getId()).isEqualTo(CAMPAIGN_1_CASE_2_ID);

        and("campaign row is created");
        MAccessCertificationCampaign row = selectObjectByOid(QAccessCertificationCampaign.class, accessCertificationCampaign1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);

        and("case row is created");
        QAccessCertificationCase a = QAccessCertificationCaseMapping.getAccessCertificationCaseMapping().defaultAlias();

        List<MAccessCertificationCase> caseRows = select(a, a.ownerOid.eq(UUID.fromString(accessCertificationCampaign1Oid)));
        assertThat(caseRows).hasSize(2);
        caseRows.sort(comparing(tr -> tr.cid));

        MAccessCertificationCase aRow = caseRows.get(1);
        assertThat(aRow.cid).isEqualTo(CAMPAIGN_1_CASE_2_ID);
        assertThat(aRow.containerType).isEqualTo(MContainerType.ACCESS_CERTIFICATION_CASE);
        assertThat(aRow.targetRefTargetOid).isNull();
        assertThat(aRow.objectRefTargetOid).isEqualTo(accCertCampaign1Case2ObjectOid);
        assertThat(aRow.objectRefTargetType).isEqualTo(MObjectType.USER);
        assertCachedUri(aRow.objectRefRelationId, relationRegistry.getDefaultRelation());
        assertThat(aRow.outcome).isEqualTo("People are the problem");
        assertThat(aRow.stageNumber).isEqualTo(5);
        assertThat(aRow.campaignIteration).isEqualTo(7);

        // Check that case (container) fullObject was updated, as opposed to campaign (object) fullObject
        PrismContainerValue<AccessCertificationCaseType> fullObjectCval = prismContext
                .parserFor(new String(aRow.fullObject, StandardCharsets.UTF_8))
                .parseItemValue();

        // Unchanged
        assertThat(fullObjectCval.asContainerable().getStageNumber()).isEqualTo(5);
        assertThat(fullObjectCval.asContainerable().getIteration()).isEqualTo(7);
        // Changed
        assertThat(fullObjectCval.asContainerable().getOutcome()).isEqualTo("People are the problem");
    }

    @Test
    public void test333DeleteCertificationCaseById() throws Exception {
        OperationResult result = createOperationResult();

        given("delta remove case for campaign 1");
        ObjectDelta<AccessCertificationCampaignType> delta = prismContext.deltaFor(AccessCertificationCampaignType.class)
                .item(ItemPath.create(AccessCertificationCampaignType.F_CASE))
                .delete(new AccessCertificationCaseType().id(CAMPAIGN_1_CASE_2_ID).asPrismContainerValue())
                .asObjectDelta(accessCertificationCampaign1Oid);
        when("modifyObject is called");
        repositoryService.modifyObject(AccessCertificationCampaignType.class,
                accessCertificationCampaign1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("campaign cases contains only one case");
        AccessCertificationCampaignType campaignObjectAfter = repositoryService
                .getObject(AccessCertificationCampaignType.class, accessCertificationCampaign1Oid, retrieveWithCases(), result)
                .asObjectable();
        assertThat(campaignObjectAfter.getCase().size()).isEqualTo(1);
    }

    @Test
    public void test340AllocateContainerIdentifiers() throws ObjectNotFoundException {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);
        long start = originalRow.containerIdSeq;

        when("allocating 5 new CIDs");
        var ids = repositoryService.allocateContainerIdentifiers(UserType.class, user1Oid, 5, result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("5 IDs are returned");
        assertThat(ids)
                .containsExactlyInAnyOrder(start, start + 1, start + 2, start + 3, start + 4);

        and("CID counter is updated");
        MUser changedRow = selectObjectByOid(QUser.class, user1Oid);
        assertThat(changedRow.containerIdSeq).isEqualTo(start + 5);

        and("version is unchanged");
        assertThat(changedRow.version).isEqualTo(originalRow.version);
    }

    @Test
    public void test399DeleteAllAssignments()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);

        given("delta deleting all assignments from user 1 using replace");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .replace()
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated and has no assignment now");
        UserType userObject = repositoryService.getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        List<AssignmentType> assignments = userObject.getAssignment();
        assertThat(assignments).isEmpty();

        and("there are no assignment rows for the user now");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);

        QAssignment<?> a = QAssignmentMapping.getAssignmentMapping().defaultAlias();
        assertThat(select(a, a.ownerOid.eq(UUID.fromString(user1Oid)))).isEmpty();
    }
    // endregion

    // region extension items
    @Test
    public void test500SettingExtensionItemValue() throws Exception {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);

        given("delta adding string extension item");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(FocusType.F_EXTENSION, new QName("string"))
                .replace("string-value500")
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        UserType user = repositoryService.getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(user.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        ExtensionType extensionContainer = user.getExtension();
        assertThat(extensionContainer).isNotNull()
                .extracting(e -> e.asPrismContainerValue().findItem(new ItemName("string")))
                .isNotNull()
                .extracting(i -> i.getRealValue())
                .isEqualTo("string-value500");

        and("externalized column is updated");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.ext).isNotNull();
        assertThat(Jsonb.toMap(row.ext))
                .containsEntry(extensionKey(extensionContainer, "string"), "string-value500");
    }

    @Test
    public void test501AddingAnotherExtensionItem() throws Exception {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);

        given("delta adding int extension item");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(FocusType.F_EXTENSION, new QName("int"))
                .replace(555)
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        UserType user = repositoryService.getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(user.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        // no check of ext container, we believe in prism here

        and("externalized column is updated");
        ExtensionType extensionContainer = user.getExtension();
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.ext).isNotNull();
        assertThat(Jsonb.toMap(row.ext))
                // old value is still there
                .containsEntry(extensionKey(extensionContainer, "string"), "string-value500")
                // and new one is added
                .containsEntry(extensionKey(extensionContainer, "int"), 555);
    }

    @Test
    public void test502AddingAndDeletingMultipleExtensionItems() throws Exception {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);

        given("delta mixing add, replace and delete of extension item");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(FocusType.F_EXTENSION, new QName("int")).replace(47)
                .item(FocusType.F_EXTENSION, new QName("string-mv")).add("s1", "s2", "s3", "s4")
                .item(FocusType.F_EXTENSION, new QName("string")).delete("string-value500")
                .item(FocusType.F_EXTENSION, new QName("poly-mv")).add(
                        new PolyString("poly-ext1"), new PolyString("poly-ext2"))
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        UserType user = repositoryService.getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(user.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        // no check of ext container, we believe in prism here

        and("externalized column is updated");
        ExtensionType extensionContainer = user.getExtension();
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.ext).isNotNull();
        assertThat(Jsonb.toMap(row.ext))
                .doesNotContainKey(extensionKey(extensionContainer, "string")) // deleted
                .containsEntry(extensionKey(extensionContainer, "int"), 47) // replaced
                .containsEntry(extensionKey(extensionContainer, "string-mv"), // added
                        List.of("s1", "s2", "s3", "s4"))
                .containsEntry(extensionKey(extensionContainer, "poly-mv"),
                        List.of(Map.of("o", "poly-ext1", "n", "polyext1"),
                                Map.of("o", "poly-ext2", "n", "polyext2")));
    }

    @Test
    public void test503AddingAndDeletingValuesFromSingleExtensionItem() throws Exception {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);

        given("delta adding and deleting values from a single extension item");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                // Add+delete of s5 will be eliminated during delta creation (asObjectDelta),
                // because it's not a single modification, but two. Compare with test163.
                // Deletion of s6 is effectively no-op.
                .item(FocusType.F_EXTENSION, new QName("string-mv")).delete("s1", "s5", "s6")
                .item(FocusType.F_EXTENSION, new QName("string-mv")).add("s5", "s7")
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        UserType user = repositoryService.getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(user.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        ExtensionType extension = user.getExtension();
        Item<?, ?> item = extension.asPrismContainerValue().findItem(new ItemName("string-mv"));
        assertThat(item).isNotNull();
        //noinspection unchecked
        assertThat((Collection<String>) item.getRealValues())
                .containsExactlyInAnyOrder("s2", "s3", "s4", "s7");

        and("externalized column is updated");
        ExtensionType extensionContainer = user.getExtension();
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.ext).isNotNull();
        assertThat(Jsonb.toMap(row.ext))
                .containsEntry(extensionKey(extensionContainer, "string-mv"), // changed
                        List.of("s2", "s3", "s4", "s7"))
                .containsEntry(extensionKey(extensionContainer, "int"), 47) // preserved
                .containsEntry(extensionKey(extensionContainer, "poly-mv"),
                        List.of(Map.of("o", "poly-ext1", "n", "polyext1"),
                                Map.of("o", "poly-ext2", "n", "polyext2")));
    }

    @Test
    public void test505SettingExtensionContainerToEmpty() throws Exception {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);

        given("delta setting extension container to empty");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(FocusType.F_EXTENSION).replace(new ExtensionType())
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        UserType user = repositoryService.getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(user.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        ExtensionType extensionContainer = user.getExtension();
        assertThat(extensionContainer).isNotNull()
                .matches(m -> m.asPrismContainerValue().isEmpty());

        and("ext column is cleared");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.ext).isNull();
    }

    @Test
    public void test506SettingExtensionContainerValue() throws Exception {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);

        given("delta adding extension container value");
        ExtensionType extension = new ExtensionType();
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(FocusType.F_EXTENSION).add(extension)
                .asObjectDelta(user1Oid);
        addExtensionValue(extension, "string", "string-value");
        addExtensionValue(extension, "int-mv", 1, 2);
        addExtensionValue(extension, "decimal", new BigDecimal("1E300"));

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        UserType user = repositoryService.getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(user.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        ExtensionType extensionContainer = user.getExtension();
        assertThat(extensionContainer).isNotNull()
                .matches(m -> !m.asPrismContainerValue().isEmpty());

        and("ext column is populated");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.ext).isNotNull();
        assertThat(Jsonb.toMap(row.ext))
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        extensionKey(extensionContainer, "string"), "string-value",
                        extensionKey(extensionContainer, "int-mv"), List.of(1, 2),
                        // retrieved number is converted to BigInteger, as it has no decimal places
                        extensionKey(extensionContainer, "decimal"), new BigDecimal("1E300").toBigInteger()));
    }

    @Test
    public void test507SettingExtensionContainerToNull() throws Exception {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);

        given("delta adding int extension item");
        List<ItemDelta<?, ?>> delta = prismContext.deltaFor(UserType.class)
                .item(FocusType.F_EXTENSION).replace()
                .asItemDeltas();

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta, result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        UserType user = repositoryService.getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(user.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        ExtensionType extensionContainer = user.getExtension();
        assertThat(extensionContainer).isNull();

        and("ext column is cleared");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.ext).isNull();
    }

    @Test(description = "MID-8258")
    public void test510TwoExtensionItemsDifferentOneMissingNamespace() throws Exception {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);

        given("delta replacing extension items with one of the paths missing namespace for the extension container");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(new ItemName("extension"), new QName("int")).replace(510)
                .item(FocusType.F_EXTENSION, new QName("string")).replace("510")
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        UserType user = repositoryService.getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(user.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        // no check of ext container, we believe in prism here

        and("externalized column is updated and both changes are present");
        ExtensionType extensionContainer = user.getExtension();
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);
        assertThat(row.ext).isNotNull();
        assertThat(Jsonb.toMap(row.ext))
                .containsEntry(extensionKey(extensionContainer, "int"), 510)
                .containsEntry(extensionKey(extensionContainer, "string"), "510");
    }

    @Test
    public void test520ModifyReferenceAttributes() throws CommonException {
        OperationResult result = createOperationResult();

        var resourceRef = UUID.randomUUID().toString();

        var accountDef = new TestShadowDefinition(resourceRef,
                SchemaConstants.RI_ACCOUNT_OBJECT_CLASS, ShadowKindType.ACCOUNT, "default");
        var nameAttr = accountDef.defineAttribute("name", DOMUtil.XSD_STRING);
        var groupRef = accountDef.defineReference("group", -1);

        var groupDef = new TestShadowDefinition(resourceRef,
                SchemaConstants.RI_GROUP_OBJECT_CLASS, ShadowKindType.ENTITLEMENT, "default");
        groupDef.defineAttribute("name", DOMUtil.XSD_STRING);
        var ownerRef = groupDef.defineReference("owner", -1);

        given("a shadow in repository");
        ShadowType account = accountDef.newShadow("account");
        var accountOid = repositoryService.addObject(account.asPrismObject(), null, result);

        ShadowType groupAll = groupDef.newShadow("all");
        var groupAllOid = repositoryService.addObject(groupAll.asPrismObject(), null, result);

        ShadowType owner = accountDef.newShadow("owner");
        owner.asPrismObject().findOrCreateItem(ItemPath.create(ShadowType.F_REFERENCE_ATTRIBUTES, groupRef), PrismReference.class)
                .add(new ObjectReferenceType().oid(groupAllOid).asReferenceValue());

        var ownerOid = repositoryService.addObject(owner.asPrismObject(), null, result);

        ShadowType groupLimited = groupDef.newShadow("limited");
        var groupLimitedOid = repositoryService.addObject(groupLimited.asPrismObject(), null, result);

        when("reference attribute group is added");
        var addAllDeltas = accountDef.newDelta()
                .item(ShadowType.F_REFERENCE_ATTRIBUTES, groupRef)
                .add(new ObjectReferenceType().oid(groupAllOid))
                .asItemDeltas();
        repositoryService.modifyObject(ShadowType.class, accountOid, addAllDeltas, result);

        then("reference attributes should be readed back");
        account = repositoryService.getObject(ShadowType.class, accountOid, null, result).asObjectable();

        // FIXME: Add checks
        then("accounts can be found by dereferencing referenceAttributes/group");

        var query = TypedQuery.parse(ShadowType.class, accountDef.objectDefinition,
                "referenceAttributes/ri:group/@/name = 'all'").toObjectQuery();
        var accountsList = repositoryService.searchObjects(ShadowType.class, query, null, result);
        assertThat(accountsList)
                .hasSize(2);
        then("group should be found by referencedBy");

        then("accounts can be found by ref filter with oid");
        query = TypedQuery.parse(ShadowType.class, accountDef.objectDefinition,
                "referenceAttributes/ri:group matches (oid = '" + groupAllOid + "' )").toObjectQuery();
        accountsList = repositoryService.searchObjects(ShadowType.class, query, null, result);
        assertThat(accountsList)
                .hasSize(2);

        when("group all is removed from account");
        repositoryService.modifyObject(ShadowType.class, accountOid,
                accountDef.newDelta()
                        .item(ShadowType.F_REFERENCE_ATTRIBUTES, groupRef)
                        .delete(new ObjectReferenceType().oid(groupAllOid))
                        .asItemDeltas(),
                result);

        then("only owner account should be found as member of all group");
        accountsList = repositoryService.searchObjects(ShadowType.class, query, null, result);
        assertThat(accountsList)
                .hasSize(1)
                .extracting(s -> s.getOid()).containsExactlyInAnyOrder(ownerOid);
    }
    // endregion

    @Test
    public void test530ChangeAttributeType() throws CommonException {
        OperationResult result = createOperationResult();

        given("a shadow in repository");
        ShadowType shadow = new ShadowType().name(getTestNameShort())
                .resourceRef(UUID.randomUUID().toString(), ResourceType.COMPLEX_TYPE)
                .objectClass(SchemaConstants.RI_ACCOUNT_OBJECT_CLASS)
                .kind(ShadowKindType.ACCOUNT)
                .intent("intent");
        ItemName attrName = new ItemName(NS_RI, "a530");
        ItemPath attrPath = ItemPath.create(ShadowType.F_ATTRIBUTES, attrName);
        //noinspection RedundantTypeArguments // actually, it is needed because of ambiguity resolution
        new ShadowAttributesHelper(shadow)
                .<String>set(attrName, DOMUtil.XSD_STRING, 0, 1, "jack");
        var shadowOid = repositoryService.addObject(shadow.asPrismObject(), null, result);

        when("attribute is replaced by PolyString version");
        PrismPropertyDefinition<PolyString> updatedAttrDef =
                prismContext.definitionFactory().newPropertyDefinition(attrName, PolyStringType.COMPLEX_TYPE);
        updatedAttrDef.mutator().setMinOccurs(0);
        updatedAttrDef.mutator().setMaxOccurs(1);
        updatedAttrDef.mutator().setDynamic(true);
        var deltas = prismContext.deltaFor(ShadowType.class)
                .item(attrPath, updatedAttrDef)
                .replace(PolyString.fromOrig("JACK2"))
                .asItemDeltas();
        repositoryService.modifyObject(ShadowType.class, shadowOid, deltas, result);

        then("everything is OK");
        result.computeStatus();
        TestUtil.assertSuccess(result);
        var shadowAfter = repositoryService.getObject(ShadowType.class, shadowOid, null, result);
        PrismProperty<PolyString> valueAfter = shadowAfter.findProperty(attrPath);
        assertThat(valueAfter.getRealValue()).isEqualTo(PolyString.fromOrig("JACK2"));
    }

    @Test
    public void test540ProtectedAttribute() throws CommonException {
        OperationResult result = createOperationResult();

        given("a shadow with protected attribute");
        ShadowType shadow = new ShadowType().name(getTestNameShort())
                .resourceRef(UUID.randomUUID().toString(), ResourceType.COMPLEX_TYPE)
                .objectClass(SchemaConstants.RI_ACCOUNT_OBJECT_CLASS)
                .kind(ShadowKindType.ACCOUNT)
                .intent("intent");
        ItemName attrName = ItemName.from(NS_RI, "a540");
        ItemPath attrPath = ItemPath.create(ShadowType.F_ATTRIBUTES, attrName);
        var initialValue = prismContext.getDefaultProtector().encryptString("rum");
        var shadowAttributesHelper = new ShadowAttributesHelper(shadow);
        //noinspection RedundantTypeArguments
        shadowAttributesHelper.<ProtectedStringType>set(
                attrName, ProtectedStringType.COMPLEX_TYPE, 0, 1, initialValue);

        when("shadow is put into the repository");
        var shadowOid = repositoryService.addObject(shadow.asPrismObject(), null, result);

        then("the attribute is there");
        var shadowFromRepo = repositoryService.getObject(ShadowType.class, shadowOid, null, result);
        assertProtectedAttributeValue(shadowFromRepo, attrPath, initialValue);

        when("attribute value is replaced");
        var valueToUpdate = prismContext.getDefaultProtector().encryptString("beer");

        when("attribute value is replaced");
        var deltas = prismContext.deltaFor(ShadowType.class)
                .item(attrPath, shadowAttributesHelper.getDefinition(attrName))
                .replace(valueToUpdate)
                .asItemDeltas();
        repositoryService.modifyObject(ShadowType.class, shadowOid, deltas, result);

        then("everything is OK, and the value is updated");
        result.computeStatus();
        TestUtil.assertSuccess(result);
        var shadowFromRepoAfter = repositoryService.getObject(ShadowType.class, shadowOid, null, result);
        assertProtectedAttributeValue(shadowFromRepoAfter, attrPath, valueToUpdate);
    }

    @Test
    public void test541BigIntegerAttribute() throws CommonException {
        OperationResult result = createOperationResult();

        given("a shadow with protected attribute");
        ShadowType shadow = new ShadowType().name(getTestNameShort())
                .resourceRef(UUID.randomUUID().toString(), ResourceType.COMPLEX_TYPE)
                .objectClass(SchemaConstants.RI_ACCOUNT_OBJECT_CLASS)
                .kind(ShadowKindType.ACCOUNT)
                .intent("intent");
        ItemName attrName = ItemName.from(NS_RI, "a540");
        ItemPath attrPath = ItemPath.create(ShadowType.F_ATTRIBUTES, attrName);
        var initialValue = BigInteger.TEN.add(BigInteger.valueOf(Long.MAX_VALUE));

        var shadowAttributesHelper = new ShadowAttributesHelper(shadow);
        //noinspection RedundantTypeArguments
        shadowAttributesHelper.<BigInteger>set(
                attrName, DOMUtil.XSD_INTEGER, 0, 1, initialValue);

        when("shadow is put into the repository");
        var shadowOid = repositoryService.addObject(shadow.asPrismObject(), null, result);

        then("the attribute is there");
        var shadowFromRepo = repositoryService.getObject(ShadowType.class, shadowOid, null, result);
        BigInteger readValue =  shadowFromRepo.getAllValues(attrPath).iterator().next().getRealValue();
        assertThat(readValue).isEqualTo(initialValue);
        when("attribute value is replaced");
        var valueToUpdate = BigInteger.TEN;

        when("attribute value is replaced");
        var deltas = prismContext.deltaFor(ShadowType.class)
                .item(attrPath, shadowAttributesHelper.getDefinition(attrName))
                .replace(valueToUpdate)
                .asItemDeltas();
        repositoryService.modifyObject(ShadowType.class, shadowOid, deltas, result);

        then("everything is OK, and the value is updated");
        result.computeStatus();
        TestUtil.assertSuccess(result);
        var shadowFromRepoAfter = repositoryService.getObject(ShadowType.class, shadowOid, null, result);
        readValue =  shadowFromRepoAfter.getAllValues(attrPath).iterator().next().getRealValue();
        assertThat(readValue).isEqualTo(valueToUpdate);

    }

    @Test
    public void test542ByteArrayAttribute() throws CommonException {
        OperationResult result = createOperationResult();

        given("a shadow with protected attribute");
        ShadowType shadow = new ShadowType().name(getTestNameShort())
                .resourceRef(UUID.randomUUID().toString(), ResourceType.COMPLEX_TYPE)
                .objectClass(SchemaConstants.RI_ACCOUNT_OBJECT_CLASS)
                .kind(ShadowKindType.ACCOUNT)
                .intent("intent");
        ItemName attrName = ItemName.from(NS_RI, "a540");
        ItemPath attrPath = ItemPath.create(ShadowType.F_ATTRIBUTES, attrName);
        var initialValue = "foo".getBytes(StandardCharsets.UTF_8);

        var shadowAttributesHelper = new ShadowAttributesHelper(shadow);
        //noinspection RedundantTypeArguments
        shadowAttributesHelper.<byte[]>set(
                attrName, DOMUtil.XSD_BASE64BINARY, 0, 1, initialValue);

        when("shadow is put into the repository");
        var shadowOid = repositoryService.addObject(shadow.asPrismObject(), null, result);

        then("the attribute is there");
        var shadowFromRepo = repositoryService.getObject(ShadowType.class, shadowOid, null, result);
        byte[] readValue =  shadowFromRepo.getAllValues(attrPath).iterator().next().getRealValue();
        assertThat(readValue).isEqualTo(initialValue);
        when("attribute value is replaced");
        var valueToUpdate = "bar".getBytes(StandardCharsets.UTF_8);

        when("attribute value is replaced");
        var deltas = prismContext.deltaFor(ShadowType.class)
                .item(attrPath, shadowAttributesHelper.getDefinition(attrName))
                .replace(valueToUpdate)
                .asItemDeltas();
        repositoryService.modifyObject(ShadowType.class, shadowOid, deltas, result);

        then("everything is OK, and the value is updated");
        result.computeStatus();
        TestUtil.assertSuccess(result);
        var shadowFromRepoAfter = repositoryService.getObject(ShadowType.class, shadowOid, null, result);
        readValue =  shadowFromRepoAfter.getAllValues(attrPath).iterator().next().getRealValue();
        assertThat(readValue).isEqualTo(valueToUpdate);

    }


    @Test(description = "MID-9754")
    public void test550reindexShadowsWithSameAttributeNameDifferentType() throws Exception {

        OperationResult result = createOperationResult();

        ItemName attrName = new ItemName(NS_RI, "conflicting");
        ItemPath attrPath = ItemPath.create(ShadowType.F_ATTRIBUTES, attrName);


        given("a shadow with string attribute `conflicting`");
        ShadowType stringBased = new ShadowType().name("stringShadow")
                .resourceRef(UUID.randomUUID().toString(), ResourceType.COMPLEX_TYPE)
                .objectClass(SchemaConstants.RI_ACCOUNT_OBJECT_CLASS)
                .kind(ShadowKindType.ACCOUNT)
                .intent("intent");
        //noinspection RedundantTypeArguments // actually, it is needed because of ambiguity resolution
        new ShadowAttributesHelper(stringBased)
                .<String>set(attrName, DOMUtil.XSD_STRING, 0, 1, "jack");
        var stringBasedOid = repositoryService.addObject(stringBased.asPrismObject(), null, result);


        given("and a shadow with int attribute `conflicting`");
        ShadowType dateBased = new ShadowType().name("dateShadow")
                .resourceRef(UUID.randomUUID().toString(), ResourceType.COMPLEX_TYPE)
                .objectClass(SchemaConstants.RI_ACCOUNT_OBJECT_CLASS)
                .kind(ShadowKindType.ACCOUNT)
                .intent("intent");
        //noinspection RedundantTypeArguments // actually, it is needed because of ambiguity resolution
        new ShadowAttributesHelper(dateBased)
                .<XMLGregorianCalendar>setOne(attrName, DOMUtil.XSD_DATETIME, 0, 1, XmlTypeConverter.createXMLGregorianCalendar());
        var dateBasedOid = repositoryService.addObject(dateBased.asPrismObject(), null, result);

        then("attributes read from repository are of correct respective types");
        checkShadowCorrectness(attrPath, stringBasedOid, dateBasedOid, result);

        when("shadows are reindexed");
        repositoryService.modifyObject(ShadowType.class, stringBasedOid, Collections.emptyList(),
                RepoModifyOptions.createForceReindex(), result);
        repositoryService.modifyObject(ShadowType.class, dateBasedOid, Collections.emptyList(),
                RepoModifyOptions.createForceReindex(), result);

        then("attributes read from repository are of correct respective types");
        checkShadowCorrectness(attrPath, stringBasedOid, dateBasedOid, result);
    }

    private void checkShadowCorrectness(ItemPath attrPath, String stringBasedOid, String dateBasedOid, OperationResult result) throws SchemaException, ObjectNotFoundException {
        var stringBasedAfter = repositoryService.getObject(ShadowType.class, stringBasedOid, GetOperationOptions.createRawCollection(), result);
        var dateBasedAfter = repositoryService.getObject(ShadowType.class, dateBasedOid, GetOperationOptions.createRawCollection(), result);
        assertThat(stringBasedAfter.getAllValues(attrPath))
                .isNotEmpty()
                .allMatch(v -> {
                    return v.getRealValue() instanceof String;
                }, "string shadow contains strings");
        assertThat(dateBasedAfter.getAllValues(attrPath))
                .isNotEmpty()
                .allMatch(v -> {
                    return v.getRealValue() instanceof XMLGregorianCalendar;
                    }, "date shadow contains date");
    }

    private static void assertProtectedAttributeValue(
            PrismObject<ShadowType> shadow, ItemPath attrPath, ProtectedStringType expected) {
        var propertyFromRepo = shadow.findProperty(attrPath);
        assertThat(propertyFromRepo)
                .as("protected attribute")
                .isNotNull();
        assertThat(propertyFromRepo.getRealValues())
                .as("protected attribute values")
                .hasSize(1)
                .singleElement()
                .isEqualTo(expected);
    }

    // region value metadata
    @Test
    public void test600AddingAndModifyingObjectMetadata()
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);
        var createdOn = XmlTypeConverter.createXMLGregorianCalendar();

        given("delta adding value metadata for user 1");
        UUID approverOid1 = UUID.randomUUID();
        UUID approverOid2 = UUID.randomUUID();
        QName refRelation = QName.valueOf("{https://random.org/ns}approver");
        // Note that this operation may fail if there are some metadata already present - adapt the test in such case.
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(InfraItemName.METADATA)
                .add(new ValueMetadataType()
                        .storage(new StorageMetadataType()
                                .createTimestamp(createdOn))
                        .process(new ProcessMetadataType()
                                .createApproverRef(approverOid1.toString(), UserType.COMPLEX_TYPE, refRelation)
                                .createApproverRef(approverOid2.toString(), UserType.COMPLEX_TYPE, refRelation)))
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
        assertThat(ValueMetadataTypeUtil.getCreateTimestamp(userObject)).isEqualTo(createdOn);
        assertThat(ValueMetadataTypeUtil.getCreateApproverRefs(userObject)).hasSize(2)
                .anyMatch(refMatcher(approverOid1, refRelation))
                .anyMatch(refMatcher(approverOid2, refRelation));

        and("user row version is incremented");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 1);

        and("externalized refs are inserted to the dedicated table");
        QObjectReference<?> r = QObjectReferenceMapping.getForCreateApprover().defaultAlias();
        UUID ownerOid = UUID.fromString(user1Oid);
        List<MReference> refs = select(r, r.ownerOid.eq(ownerOid));
        assertThat(refs).hasSize(2)
                .anyMatch(refRowMatcher(approverOid1, refRelation))
                .anyMatch(refRowMatcher(approverOid2, refRelation));

        when("value metadata for user 1 are updated");
        var modifiedOn = XmlTypeConverter.createXMLGregorianCalendar();
        ObjectDelta<UserType> delta2 = prismContext.deltaFor(UserType.class)
                .item(InfraItemName.METADATA, ValueMetadataTypeUtil.getSingleValueMetadataId(userObject),
                        ValueMetadataType.F_STORAGE, StorageMetadataType.F_MODIFY_TIMESTAMP)
                .replace(modifiedOn)
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta2.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).recompute().isSuccess();

        and("serialized form (fullObject) is updated (but contains the original metadata as well)");
        var userObject2 = repositoryService
                .getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userObject2.getVersion()).isEqualTo(String.valueOf(row.version + 1));
        assertThat(ValueMetadataTypeUtil.getCreateTimestamp(userObject2)).isEqualTo(createdOn);
        assertThat(ValueMetadataTypeUtil.getModifyTimestamp(userObject2)).isEqualTo(modifiedOn);
        assertThat(ValueMetadataTypeUtil.getCreateApproverRefs(userObject2)).hasSize(2)
                .anyMatch(refMatcher(approverOid1, refRelation))
                .anyMatch(refMatcher(approverOid2, refRelation));

        and("user row version is incremented");
        MUser row2 = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row2.version).isEqualTo(row.version + 1);
    }

    @Test
    public void test610AddingAndModifyingAssignmentMetadata()
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);
        var createdOn = XmlTypeConverter.createXMLGregorianCalendar();

        given("delta adding assignment with value metadata for user 1");
        var originOid = UUID.randomUUID();
        var assignment = new AssignmentType()
                .description("empty assignment");
        var provenance = new ProvenanceMetadataType()
                .acquisition(new ProvenanceAcquisitionType()
                        .originRef(originOid.toString(), ServiceType.COMPLEX_TYPE));
        ValueMetadataTypeUtil.getOrCreateStorageMetadata(assignment, provenance)
                .createTimestamp(createdOn);
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .replace(assignment)
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        UserType userRetrieved = repositoryService
                .getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userRetrieved.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(userRetrieved.getAssignment()).hasSize(1);
        var assignmentRetrieved = userRetrieved.getAssignment().get(0);
        var storageRetrieved = ValueMetadataTypeUtil.getStorageMetadata(assignmentRetrieved, provenance);
        assertThat(storageRetrieved).isNotNull();
        assertThat(storageRetrieved.getCreateTimestamp()).isEqualTo(createdOn);

        when("value metadata for the assignment are updated");
        var modifiedOn = XmlTypeConverter.createXMLGregorianCalendar();
        ObjectDelta<UserType> delta2 = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, assignmentRetrieved.getId(),
                        InfraItemName.METADATA, ValueMetadataTypeUtil.getValueMetadataId(assignmentRetrieved, provenance),
                        ValueMetadataType.F_STORAGE, StorageMetadataType.F_MODIFY_TIMESTAMP)
                .replace(modifiedOn)
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta2.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).recompute().isSuccess();

        and("serialized form (fullObject) is updated (but contains the original metadata as well)");
        var userRetrieved2 = repositoryService
                .getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userRetrieved2.getVersion()).isEqualTo(String.valueOf(originalRow.version + 2));
        assertThat(userRetrieved2.getAssignment()).hasSize(1);
        var assignmentRetrieved2 = userRetrieved2.getAssignment().get(0);
        var storageRetrieved2 = ValueMetadataTypeUtil.getStorageMetadata(assignmentRetrieved2, provenance);
        assertThat(storageRetrieved2).isNotNull();
        assertThat(storageRetrieved2.getCreateTimestamp()).isEqualTo(createdOn);
        assertThat(storageRetrieved2.getModifyTimestamp()).isEqualTo(modifiedOn);
    }

    @Test
    public void test620AddingEffectiveMarkRefWithMetadata()
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);

        given("delta adding effectiveMarkRef with value metadata for user 1");
        var markOid = UUID.randomUUID();
        var effectiveMarkRef1 = new ObjectReferenceType()
                .oid(markOid.toString()).type(MarkType.COMPLEX_TYPE);
        var metadataValue1 = new ValueMetadataType()
                .provenance(new ProvenanceMetadataType()
                        .markingRule(new MarkingRuleSpecificationType()
                                .ruleId(123L)
                                .transitional(true)));
        effectiveMarkRef1.asReferenceValue().getValueMetadata().addMetadataValue(
                metadataValue1.asPrismContainerValue());
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_EFFECTIVE_MARK_REF)
                .replace(effectiveMarkRef1)
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        UserType userRetrieved = repositoryService
                .getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        displayValue("user retrieved (XML)", prismContext.xmlSerializer().serialize(userRetrieved.asPrismObject()));
        assertThat(userRetrieved.getVersion()).isEqualTo(String.valueOf(originalRow.version + 1));
        assertThat(userRetrieved.getEffectiveMarkRef()).hasSize(1);
        var effectiveMarkRef1Retrieved = userRetrieved.getEffectiveMarkRef().get(0);
        var metadataRetrieved = effectiveMarkRef1Retrieved.asReferenceValue().getValueMetadata();
        assertThat(metadataRetrieved.getValues()).as("metadata values").hasSize(1);
    }

    // endregion

    @Test(description = "MID-10326")
    public void test700ModifyRoleWithInducementWithRuntimeFilter() throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
        OperationResult result = createOperationResult();
        given("role is created");
        var inducementId = 11L;
        var role = new RoleType().name("test-role")
                .inducement(new AssignmentType().id(inducementId));
        var roleOid = repositoryService.addObject(role.asPrismObject(), null, result);
        given("and inducement is added with targetRef with filter (no oid)");


        var targetOrt = new ObjectReferenceType().type(RoleType.COMPLEX_TYPE);
        var targetPrv = targetOrt.asReferenceValue();
        targetPrv.setFilter(new SearchFilterType("name = \"ad:vending-machine-access\"", PrismNamespaceContext.EMPTY));
        targetPrv.setResolutionTime(EvaluationTimeType.RUN);
        var ind = new AssignmentType()
                .targetRef(targetOrt);
        var deltas = PrismContext.get().deltaFor(RoleType.class).item(RoleType.F_INDUCEMENT,inducementId, AssignmentType.F_TARGET_REF)
                        .add(targetPrv.clone()).asItemDeltas();
        then("operation is successful and modification is executed");
        repositoryService.modifyObject(RoleType.class, roleOid, deltas, result);
    }


    // region precondition and modify dynamically
    @Test
    public void test800ModifyWithPositivePrecondition() throws Exception {
        OperationResult result = createOperationResult();

        given("delta adding int extension item");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(FocusType.F_EMAIL_ADDRESS).replace(getTestNumber() + "@email.com")
                .asObjectDelta(user1Oid);

        when("modifyObject is called with positive precondition");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(),
                // obviously true precondition, we want to use the object, not just return true
                u -> u.asObjectable().getOid() != null,
                null, result);

        then("operation is successful and modification is executed");
        assertThatOperationResult(result).isSuccess();
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.emailAddress).isEqualTo(getTestNumber() + "@email.com");
    }

    @Test
    public void test801ModifyWithNegativePrecondition() throws Exception {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);

        given("delta adding int extension item");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(FocusType.F_EMAIL_ADDRESS).replace(getTestNumber() + "@email.com")
                .asObjectDelta(user1Oid);

        expect("modifyObject called with negative precondition fails");
        Assertions.assertThatThrownBy(() ->
                        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(),
                                // obviously false precondition, we want to use the object, not just return true
                                u -> u.asObjectable().getOid() == null,
                                null, result))
                .hasMessageStartingWith("Modification precondition does not hold for user");

        and("operation is fatal error and modification is not executed");
        assertThatOperationResult(result).isFatalError();
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version);
        assertThat(row.emailAddress).isNotEqualTo(getTestNumber() + "@email.com");
    }

    @Test
    public void test810ModifyDynamically() throws Exception {
        OperationResult result = createOperationResult();

        when("modifyObjectDynamically is called");
        repositoryService.modifyObjectDynamically(UserType.class, user1Oid, null,
                u -> prismContext.deltaFor(UserType.class)
                        .item(FocusType.F_EMAIL_ADDRESS).replace(getTestNumber() + "@email.com")
                        .asItemDeltas(),
                null, result);

        then("operation is successful and modification is executed");
        assertThatOperationResult(result).isSuccess();
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.emailAddress).isEqualTo(getTestNumber() + "@email.com");
    }

    @Test
    public void test811ModifyDynamicallyWithFailingSupplier() {
        OperationResult result = createOperationResult();
        MUser originalRow = selectObjectByOid(QUser.class, user1Oid);

        expect("modifyObjectDynamically with failing supplier fails");
        Assertions.assertThatThrownBy(() ->
                        repositoryService.modifyObjectDynamically(UserType.class, user1Oid, null,
                                u -> {
                                    throw new RuntimeException("Random exception");
                                },
                                null, result))
                .isInstanceOf(SystemException.class)
                .hasMessage("Random exception");

        and("operation is fatal error and modification is not executed");
        assertThatOperationResult(result).isFatalError();
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version);
    }

    @Test
    public void test815ModifyDynamicallyWithObjectNotFound() {
        OperationResult result = createOperationResult();

        expect("modifyObjectDynamically for non-existent object fails");
        Assertions.assertThatThrownBy(() ->
                        repositoryService.modifyObjectDynamically(
                                UserType.class, TestUtil.NON_EXISTENT_OID, null,
                                u -> List.of(),
                                null, result))
                .isInstanceOf(ObjectNotFoundException.class);

        and("operation is fatal error");
        assertThatOperationResult(result).isFatalError();
    }

    @Test
    public void test816ModifyDynamicallyWithObjectNotFound() {
        OperationResult result = createOperationResult();

        when("modifyObjectDynamically for non-existent fails with allow-not-found option");
        Assertions.assertThatThrownBy(() ->
                        repositoryService.modifyObjectDynamically(
                                UserType.class, TestUtil.NON_EXISTENT_OID,
                                SelectorOptions.createCollection(GetOperationOptions.createAllowNotFound()),
                                u -> List.of(),
                                null, result))
                .isInstanceOf(ObjectNotFoundException.class);
        assertThatOperationResult(result).isSuccess();
    }

    // endregion

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
    public void test905ModifyUsingObjectTypeArgumentIsPossible()
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        // This does not say anything about ability to do the same on the model level
        OperationResult result = createOperationResult();

        given("delta with user specific item change");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_EMAIL_ADDRESS).add("new905@email.com")
                .asObjectDelta(user1Oid);

        when("modifyObject is called");
        repositoryService.modifyObject(ObjectType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful and everything works fine");
        assertThatOperationResult(result).isSuccess();

        UserType userObject = repositoryService.getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userObject.getEmailAddress()).isEqualTo("new905@email.com");

        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.emailAddress).isEqualTo("new905@email.com");
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
    public void test920ModifyOperationUpdatesPerformanceMonitor()
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        OperationResult result = createOperationResult();

        given("object modification and cleared performance information");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_EMAIL_ADDRESS).add(getTestNameShort() + "@email.com")
                .asObjectDelta(user1Oid);
        clearPerformanceMonitor();

        when("object is modified in the repository");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("performance monitor is updated");
        assertThatOperationResult(result).isSuccess();
        assertSingleOperationRecorded(REPO_OP_PREFIX + RepositoryService.OP_MODIFY_OBJECT);
    }

    @Test
    public void test950ModifyOperationWithReindexUpdatesPerformanceMonitor()
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        OperationResult result = createOperationResult();
        given("Full object was modified in database");
        clearPerformanceMonitor();

        when("object is modified in the repository");
        RepoModifyOptions options = RepoModifyOptions.createForceReindex();
        repositoryService.modifyObject(UserType.class, user1Oid, Collections.emptyList(), options, result);

        then("performance monitor is updated");
        assertThatOperationResult(result).isSuccess();
        assertSingleOperationRecorded(REPO_OP_PREFIX + RepositoryService.OP_MODIFY_OBJECT);
    }

    /**
     * Disabled: This type of edit (remove of assignment row) does not survive reindex if assignments are not stored in
     * full object.
     */
    @Test(enabled = false)
    public void test951ReindexAfterManualChangeOfFullObject()
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        OperationResult result = createOperationResult();

        given("user existing in the repository");
        UserType user = new UserType().name("corrupted")
                .beginAssignment()
                .policySituation("kept")
                .<UserType>end()
                .beginAssignment()
                .policySituation("removed")
                .end();
        String oid = repositoryService.addObject(user.asPrismObject(), null, result);

        when("full object is modified in the database (indices are desynced from full object)");
        user = repositoryService.getObject(UserType.class, oid, null, result).asObjectable();
        user.getAssignment().remove(1); // remove removed
        user.beginAssignment().policySituation("added");
        QUserMapping mapping = QUserMapping.getUserMapping();
        byte[] fullObject = mapping.createFullObject(user);
        try (JdbcSession session = mapping.repositoryContext().newJdbcSession().startTransaction()) {
            session.newUpdate(mapping.defaultAlias())
                    .set(mapping.defaultAlias().fullObject, fullObject)
                    .where(mapping.defaultAlias().oid.eq(SqaleUtils.oidToUuid(oid)))
                    .execute();
            session.commit();
        }

        assertPolicySituationFound("kept", 1, result);
        assertPolicySituationFound("removed", 1, result);
        assertPolicySituationFound("added", 0, result);

        and("object is reindexed");
        RepoModifyOptions options = RepoModifyOptions.createForceReindex();
        repositoryService.modifyObject(UserType.class, oid, Collections.emptyList(), options, result);

        then("indices are updated according new full object");

        assertPolicySituationFound("kept", 1, result);
        assertPolicySituationFound("removed", 0, result);
        assertPolicySituationFound("added", 1, result);
    }

    /** Tests handling of "incomplete" flag on shadow credentials. */
    @Test
    public void test960IncompleteFlag() throws CommonException {
        OperationResult result = createOperationResult();

        when("password value is marked as incomplete");

        // TODO we should be able to set the incomplete flag more intelligently (by using replace delta on the leaf property)
        //  See MID-10161.
        var password = new PasswordType();
        ShadowUtil.setPasswordIncomplete(password);
        repositoryService.modifyObject(ShadowType.class, shadow1Oid,
                prismContext.deltaFor(ShadowType.class)
                        .item(PATH_PASSWORD)
                        .replace(password)
                        .asItemDeltas(),
                result);

        then("the value is stored as incomplete");
        var shadowFirst = repositoryService.getObject(ShadowType.class, shadow1Oid, null, result);
        assertThat(ShadowUtil.getPasswordValueProperty(shadowFirst.asObjectable()))
                .as("password property")
                .isNotNull()
                .satisfies(p -> assertThat(p.getValues()).as("values").isEmpty())
                .satisfies(p -> assertThat(p.isIncomplete()).as("incomplete flag").isTrue());

        when("real value is provided");
        var value = prismContext.getDefaultProtector().encryptString("abc");
        repositoryService.modifyObject(ShadowType.class, shadow1Oid,
                prismContext.deltaFor(ShadowType.class)
                        .item(PATH_PASSWORD_VALUE)
                        .replace(value)
                        .asItemDeltas(),
                result);

        // Prism takes care of removing the incomplete flag when the real value is set
        then("incomplete flag should be gone");
        var shadowSecond = repositoryService.getObject(ShadowType.class, shadow1Oid, null, result);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertThat(ShadowUtil.getPasswordValueProperty(shadowSecond.asObjectable()))
                .as("password property")
                .isNotNull()
                .satisfies(p -> assertThat(p.isIncomplete()).as("incomplete flag").isFalse())
                .satisfies(p -> assertThat(p.getValues()).as("values").hasSize(1))
                .satisfies(p -> assertThat(p.getRealValue()).as("real value").isEqualTo(value));
    }

    private void assertPolicySituationFound(String situation, int count, OperationResult result) throws SchemaException {
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, AssignmentType.F_POLICY_SITUATION).eq(situation)
                .build();
        SearchResultList<PrismObject<UserType>> found = repositoryService.searchObjects(UserType.class, query, null, result);
        assertEquals(found.size(), count, "Found situation count does not match.");
    }

    // This test assumes assignments are in full object and separate table at same time
    @Test(enabled = false)
    public void test952ReindexFixingColumnsOutOfSync()
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        OperationResult result = createOperationResult();

        given("user existing in the repository");
        String name = "user" + getTestNumber();
        UserType user = new UserType().name(name)
                .emailAddress(name + "@goodmail.com")
                .subtype("subtype-1")
                .subtype("subtype-2")
                .assignment(new AssignmentType()
                        .order(1));
        String oid = repositoryService.addObject(user.asPrismObject(), null, result);

        and("object aggregate is modified in the DB (simulating out-of-sync data)");
        QAssignment<MObject> a = QAssignmentMapping.getAssignmentMapping().defaultAlias();
        UUID oidUuid = UUID.fromString(oid);
        try (JdbcSession jdbcSession = startTransaction()) {
            jdbcSession.newInsert(a)
                    .set(a.ownerOid, oidUuid)
                    .set(a.cid, -1L)
                    .set(a.containerType, MContainerType.ASSIGNMENT)
                    .set(a.ownerType, MObjectType.USER)
                    .set(a.orderValue, 952) // test number, hopefully unique
                    .execute();

            QUser u = QUserMapping.getUserMapping().defaultAlias();
            jdbcSession.newUpdate(u)
                    .set(u.emailAddress, "bad@badmail.com")
                    .set(u.subtypes, new String[] { "subtype-952" })
                    .set(u.costCenter, "invasive value")
                    .where(u.oid.eq(oidUuid))
                    .execute();

            jdbcSession.commit();
        }

        and("search provides obviously bad results");
        assertThat(repositorySearchObjects(UserType.class,
                prismContext.queryFor(UserType.class)
                        .item(UserType.F_EMAIL_ADDRESS).eq(name + "@goodmail.com").build(), result))
                .isEmpty(); // can't find by the original mail, that's wrong
        assertThat(repositorySearchObjects(UserType.class,
                prismContext.queryFor(UserType.class)
                        .item(UserType.F_COST_CENTER).eq("invasive value").build(), result))
                .hasSize(1); // can find by bad value, that's wrong
        assertThat(repositorySearchObjects(UserType.class,
                prismContext.queryFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT, AssignmentType.F_ORDER).eq(952).build(), result))
                .hasSize(1); // can find by wrong assignment
        assertThat(repositorySearchObjects(UserType.class,
                prismContext.queryFor(UserType.class)
                        .item(UserType.F_SUBTYPE).eq("subtype-952").build(), result))
                .hasSize(1); // can find by wrong subtype

        when("reindex is called to fix it");
        repositoryService.modifyObject(UserType.class, oid, Collections.emptyList(),
                RepoModifyOptions.createForceReindex(), result);

        then("the searches work as expected");
        assertThat(repositorySearchObjects(UserType.class,
                prismContext.queryFor(UserType.class)
                        .item(UserType.F_EMAIL_ADDRESS).eq(name + "@goodmail.com").build(), result))
                .hasSize(1)
                .extracting(o -> o.getOid())
                .containsExactlyInAnyOrder(oid); // can find by the original mail, that's good!
        assertThat(repositorySearchObjects(UserType.class,
                prismContext.queryFor(UserType.class)
                        .item(UserType.F_COST_CENTER).eq("invasive value").build(), result))
                .isEmpty(); // good
        assertThat(repositorySearchObjects(UserType.class,
                prismContext.queryFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT, AssignmentType.F_ORDER).eq(952).build(), result))
                .isEmpty(); // good
        assertThat(repositorySearchObjects(UserType.class,
                prismContext.queryFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT, AssignmentType.F_ORDER).eq(1).build(), result))
                .extracting(o -> o.getOid())
                .contains(oid); // good, can have more results, but our user is there too

        and("database values are as good as new");
        MUser userRow = selectObjectByOid(QUser.class, oid);
        assertThat(userRow.emailAddress).isEqualTo(name + "@goodmail.com"); // tested by search above as well
        assertThat(userRow.costCenter).isNull();
        assertThat(userRow.subtypes).containsExactlyInAnyOrder("subtype-1", "subtype-2");

        List<MAssignment> assRows = select(a, a.ownerOid.eq(oidUuid));
        // single row with proper order is returned, wrong row is gone
        assertThat(assRows).hasSize(1);
        assertThat(assRows.get(0).orderValue).isEqualTo(1);
    }

    // This test assumes assignments are in full object and separete table at same time
    @Test(enabled = false)
    public void test955ReindexOfShadowWithAttributes() throws Exception {
        OperationResult result = createOperationResult();

        given("delta adding an attributes container for for shadow 1");
        ShadowType shadow = new ShadowType()
                .name("shadow-" + getTestName());
        ShadowAttributesType attributesContainer = new ShadowAttributesHelper(shadow)
                .set(new QName("https://example.com/p", "string-mv"), DOMUtil.XSD_STRING,
                        "string-value1", "string-value2")
                .attributesContainer();
        String oid = repositoryService.addObject(shadow.asPrismObject(), null, result);

        when("reindex is called");
        repositoryService.modifyObject(
                ShadowType.class, oid, List.of(), RepoModifyOptions.createForceReindex(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) still has attributes");
        ShadowType shadowObject = repositoryService.getObject(ShadowType.class, oid, null, result)
                .asObjectable();
        assertThat(shadowObject.getAttributes()).isNotNull();

        and("externalized attributes are also preserved");
        MShadow row = selectObjectByOid(QShadow.class, oid);
        assertThat(row.attributes).isNotNull();
        Map<String, Object> attributeMap = Jsonb.toMap(row.attributes);
        assertThat(attributeMap).containsEntry(
                shadowAttributeKey(attributesContainer, "string-mv"),
                List.of("string-value1", "string-value2"));
    }

    @Test
    public void test990ChangeOfNonPersistedItemWorksOk()
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

        given("delta with widget addition for user 1 using container add modification");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ADMIN_GUI_CONFIGURATION, AdminGuiConfigurationType.F_HOME_PAGE)
                .add(new HomePageType().id(1L))
                .asObjectDelta(user1Oid);

        when("modifyObject to add homePage container is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ADMIN_GUI_CONFIGURATION, AdminGuiConfigurationType.F_HOME_PAGE, 1L, HomePageType.F_WIDGET)
                .add(new PreviewContainerPanelConfigurationType())
                .asObjectDelta(user1Oid);

        when("modifyObject to add widget container is called");
        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("serialized form (fullObject) is updated");
        UserType userObject = repositoryService.getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        assertThat(userObject.getVersion()).isEqualTo(String.valueOf(originalRow.version + 2)); // two updates there
        assertThat(userObject.getAdminGuiConfiguration().getHomePage()).isNotEmpty();
        assertThat(userObject.getAdminGuiConfiguration().getHomePage().get(0).getWidget()).isNotEmpty();

        and("externalized version is updated");
        MUser row = selectObjectByOid(QUser.class, user1Oid);
        assertThat(row.version).isEqualTo(originalRow.version + 2);
    }
    // endregion

    @Test(expectedExceptions = SystemException.class)
    public void test992UpdateAssignmentWithWrongOid() throws Exception {
        // GIVEN
        final String targetOid = UUID.randomUUID().toString();

        final ObjectReferenceType originalRef = new ObjectReferenceType()
                .oid(targetOid)
                .type(RoleType.COMPLEX_TYPE)
                .relation(SchemaConstants.ORG_DEFAULT);

        AssignmentType a = new AssignmentType();
        a.setTargetRef(originalRef);

        OperationResult result = new OperationResult("updateAssignmentWithWrongOid");

        UserType userObject = repositoryService.getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        ObjectDelta<UserType> delta = userObject.asPrismObject().createModifyDelta();
        delta.addModificationAddContainer(UserType.F_ASSIGNMENT, a);

        delta.addModificationDeleteContainer(
                UserType.F_ASSIGNMENT, userObject.getAssignment().stream().map(i -> i.clone()).toList().toArray(new AssignmentType[0]));

        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        try (JdbcSession session = sqlRepoContext.newJdbcSession()) {
            QAssignment qa = new QAssignment("a");

            UUID uuid = session.newQuery().select(qa.targetRefTargetOid)
                    .from(qa)
                    .where(qa.ownerOid.eq(SqaleUtils.oidToUuid(user1Oid))).fetchFirst();
            AssertJUnit.assertEquals(SqaleUtils.oidToUuid(targetOid), uuid);
        }

        UserType user = repositoryService.getObject(UserType.class, user1Oid, null, result)
                .asObjectable();
        AssertJUnit.assertEquals(1, user.getAssignment().size());

        AssignmentType created = user.getAssignment().get(0);
        AssertJUnit.assertEquals(targetOid, created.getTargetRef().getOid());

        // WHEN
        ObjectReferenceType newRef = new ObjectReferenceType()
                .oid("1234")
                .type(RoleType.COMPLEX_TYPE)
                .relation(SchemaConstants.ORG_DEFAULT);

        delta = user.asPrismObject().createModifyDelta();
        ReferenceDelta refDelta = delta.createReferenceModification(ItemPath.create(UserType.F_ASSIGNMENT, created.getId(), AssignmentType.F_TARGET_REF));
        refDelta.addValuesToAdd(newRef.asReferenceValue());
        refDelta.addValueToDelete(originalRef.asReferenceValue().clone());

        repositoryService.modifyObject(UserType.class, user1Oid, delta.getModifications(), result);

        // THEN
        AssertJUnit.fail("Should fail in repository service modify, since oid in targetRef is invalid");
    }
}
