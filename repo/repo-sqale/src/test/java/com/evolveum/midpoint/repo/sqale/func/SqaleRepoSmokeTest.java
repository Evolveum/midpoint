/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.func;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase.DEFAULT_SCHEMA_NAME;

import java.util.Date;
import java.util.UUID;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.DeleteObjectResult;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sqale.SqaleRepoBaseTest;
import com.evolveum.midpoint.repo.sqale.jsonb.Jsonb;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.MUser;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QUser;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReference;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.perfmon.SqlPerformanceMonitorImpl;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DiagnosticInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Contains a few tests doing stuff all over the repository including a few lower level
 * (sub-repo-API) tests around Querydsl and our adaptation of it.
 */
public class SqaleRepoSmokeTest extends SqaleRepoBaseTest {

    private String sanityUserOid;

    @Test
    public void test000Sanity() {
        assertThat(repositoryService).isNotNull();

        // DB should be empty
        assertCount(QObject.CLASS, 0);
        assertCount(QContainer.CLASS, 0);
        assertCount(QReference.CLASS, 0);
        // we just want the table and count, we don't care about "bean" type here
        FlexibleRelationalPathBase<?> oidTable = new FlexibleRelationalPathBase<>(
                void.class, "oid", DEFAULT_SCHEMA_NAME, "m_object_oid");
        assertCount(oidTable, 0);

        // selects check also mapping to M-classes
        assertThat(select(aliasFor(QObject.CLASS))).isEmpty();
        assertThat(select(aliasFor(QContainer.CLASS))).isEmpty();
        assertThat(select(aliasFor(QReference.CLASS))).isEmpty();
    }

    @Test
    public void test010TestRepositorySelfTest() {
        OperationResult result = createOperationResult();

        when("repository self test is called");
        repositoryService.repositorySelfTest(result);

        expect("operation is successful and contains info about round-trip time to DB");
        assertThatOperationResult(result).isSuccess();
        assertThat(result.getReturn("database-round-trip-ms"))
                .isNotNull()
                .hasSize(1);
    }

    @Test
    public void test100AddObject() throws ObjectAlreadyExistsException, SchemaException {
        OperationResult result = createOperationResult();

        when("correct object is added to the repository");
        UserType user = new UserType(prismContext)
                .name("sanity-user");
        sanityUserOid = repositoryService.addObject(user.asPrismObject(), null, result);

        then("added object is assigned OID and operation is success");
        assertThat(sanityUserOid).isNotNull();
        assertThat(user.getOid()).isEqualTo(sanityUserOid);
        assertThat(selectObjectByOid(QUser.class, sanityUserOid)).isNotNull();
        assertThatOperationResult(result).isSuccess();
    }

    @Test
    public void test200GetObject() throws SchemaException, ObjectNotFoundException {
        OperationResult result = createOperationResult();

        given("cleared performance information");
        SqlPerformanceMonitorImpl pm = repositoryService.getPerformanceMonitor();
        pm.clearGlobalPerformanceInformation();
        assertThat(pm.getGlobalPerformanceInformation().getAllData()).isEmpty();

        when("getObject is called for known OID");
        PrismObject<UserType> object =
                repositoryService.getObject(UserType.class, sanityUserOid, null, result);

        then("object is obtained and performance monitor is updated");
        assertThatOperationResult(result).isSuccess();
        assertThat(object).isNotNull();
        assertSingleOperationRecorded(pm, RepositoryService.OP_GET_OBJECT);
    }

    @Test
    public void test210GetVersion() throws SchemaException, ObjectNotFoundException {
        OperationResult result = createOperationResult();

        when("getVersion is called for known OID");
        String version = repositoryService.getVersion(UserType.class, sanityUserOid, result);

        then("non-null version string is obtained");
        assertThatOperationResult(result).isSuccess();
        assertThat(version).isNotNull();
    }

    @Test
    public void test211GetVersionFailure() {
        OperationResult result = createOperationResult();

        expect("getVersion for non-existent OID throws exception");
        assertThatThrownBy(() -> repositoryService.getVersion(
                UserType.class, UUID.randomUUID().toString(), result))
                .isInstanceOf(ObjectNotFoundException.class);

        and("operation result is fatal error");
        assertThatOperationResult(result).isFatalError();
    }

    // TODO test for getObject() with typical options (here or separate class?)
    //  - ObjectOperationOptions(jpegPhoto:retrieve=INCLUDE)
    //  - ObjectOperationOptions(/:resolveNames)

    @Test
    public void test300AddDiagnosticInformation() throws Exception {
        OperationResult result = createOperationResult();

        given("a task (or any object actually) without diag info");
        TaskType task = new TaskType(prismContext)
                .name("task" + getTestNumber());
        String taskOid = repositoryService.addObject(task.asPrismObject(), null, result);
        PrismObject<TaskType> taskFromDb =
                repositoryService.getObject(TaskType.class, taskOid, null, result);
        assertThat(taskFromDb.asObjectable().getDiagnosticInformation()).isNullOrEmpty();

        when("adding diagnostic info");
        DiagnosticInformationType event = new DiagnosticInformationType()
                .timestamp(XmlTypeConverter.createXMLGregorianCalendar(new Date()))
                .type(SchemaConstants.TASK_THREAD_DUMP_URI)
                .cause("cause")
                .nodeIdentifier("node-id")
                .content("dump");
        repositoryService.addDiagnosticInformation(TaskType.class, taskOid, event, result);

        then("operation is success and the info is there");
        assertThatOperationResult(result).isSuccess();
        taskFromDb = repositoryService.getObject(TaskType.class, taskOid, null, result);
        assertThat(taskFromDb).isNotNull();
        assertThat(taskFromDb.asObjectable().getDiagnosticInformation())
                .isNotEmpty()
                .anyMatch(d -> d.getType().equals(SchemaConstants.TASK_THREAD_DUMP_URI));
    }

    @Test
    public void test800DeleteObject() throws ObjectNotFoundException {
        OperationResult result = createOperationResult();

        DeleteObjectResult deleteResult =
                repositoryService.deleteObject(UserType.class, sanityUserOid, result);

        assertThat(deleteResult).isNotNull();
        assertThatOperationResult(result).isSuccess();
        assertThat(selectNullableObjectByOid(QUser.class, sanityUserOid)).isNull();
    }

    // region low-level tests

    /** This tests our type mapper/converter classes and related column mapping. */
    @Test
    public void test900WorkingWithPgArraysJsonbAndBytea() {
        QUser u = aliasFor(QUser.class);
        MUser user = new MUser();

        String userName = "user" + getTestNumber();
        setName(user, userName);
        user.policySituations = new Integer[] { 1, 2 };
        user.subtypes = new String[] { "subtype1", "subtype2" };
        user.ext = new Jsonb("{\"key\" : \"value\",\n\"number\": 47} "); // more whitespaces/lines
        user.photo = new byte[] { 0, 1, 0, 1 };
        try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession().startTransaction()) {
            jdbcSession.newInsert(u).populate(user).execute();
            jdbcSession.commit();
        }

        MUser row = selectOne(u, u.nameNorm.eq(userName));
        assertThat(row.policySituations).contains(1, 2);
        assertThat(row.subtypes).contains("subtype1", "subtype2");
        assertThat(row.ext.value).isEqualTo("{\"key\": \"value\", \"number\": 47}"); // normalized
        // byte[] is used for fullObject, there is no chance to miss a problem with it
        assertThat(row.photo).hasSize(4);

        // setting NULLs
        try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession().startTransaction()) {
            jdbcSession.newUpdate(u)
                    .setNull(u.policySituations)
                    .set(u.subtypes, (String[]) null) // this should do the same
                    .setNull(u.ext)
                    .setNull(u.photo)
                    .where(u.oid.eq(row.oid))
                    .execute();
            jdbcSession.commit();
        }

        row = selectOne(u, u.nameNorm.eq(userName));
        assertThat(row.policySituations).isNull();
        assertThat(row.subtypes).isNull();
        assertThat(row.ext).isNull();
        // but we never set fullObject to null, so this is a good test for doing so with byte[]
        assertThat(row.photo).isNull();
    }
    // endregion
}
