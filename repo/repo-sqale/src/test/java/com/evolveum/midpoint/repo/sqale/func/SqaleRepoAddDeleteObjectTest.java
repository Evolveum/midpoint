/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.func;

import static java.util.Comparator.comparing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static com.evolveum.midpoint.repo.api.RepoAddOptions.createOverwrite;
import static com.evolveum.midpoint.schema.util.SimpleExpressionUtil.velocityExpression;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.*;

import com.evolveum.midpoint.schema.util.ValueMetadataTypeUtil;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.api.DeleteObjectResult;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sqale.SqaleRepoBaseTest;
import com.evolveum.midpoint.repo.sqale.SqaleRepositoryService;
import com.evolveum.midpoint.repo.sqale.jsonb.Jsonb;
import com.evolveum.midpoint.repo.sqale.qmodel.accesscert.*;
import com.evolveum.midpoint.repo.sqale.qmodel.assignment.*;
import com.evolveum.midpoint.repo.sqale.qmodel.cases.MCase;
import com.evolveum.midpoint.repo.sqale.qmodel.cases.QCase;
import com.evolveum.midpoint.repo.sqale.qmodel.cases.workitem.*;
import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainerType;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.connector.MConnector;
import com.evolveum.midpoint.repo.sqale.qmodel.connector.MConnectorHost;
import com.evolveum.midpoint.repo.sqale.qmodel.connector.QConnector;
import com.evolveum.midpoint.repo.sqale.qmodel.connector.QConnectorHost;
import com.evolveum.midpoint.repo.sqale.qmodel.lookuptable.MLookupTableRow;
import com.evolveum.midpoint.repo.sqale.qmodel.lookuptable.QLookupTableRow;
import com.evolveum.midpoint.repo.sqale.qmodel.node.MNode;
import com.evolveum.midpoint.repo.sqale.qmodel.node.QNode;
import com.evolveum.midpoint.repo.sqale.qmodel.notification.QMessageTemplate;
import com.evolveum.midpoint.repo.sqale.qmodel.object.*;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.*;
import com.evolveum.midpoint.repo.sqale.qmodel.report.MReportData;
import com.evolveum.midpoint.repo.sqale.qmodel.report.QReport;
import com.evolveum.midpoint.repo.sqale.qmodel.report.QReportData;
import com.evolveum.midpoint.repo.sqale.qmodel.resource.MResource;
import com.evolveum.midpoint.repo.sqale.qmodel.resource.QResource;
import com.evolveum.midpoint.repo.sqale.qmodel.role.MArchetype;
import com.evolveum.midpoint.repo.sqale.qmodel.role.QArchetype;
import com.evolveum.midpoint.repo.sqale.qmodel.shadow.MShadow;
import com.evolveum.midpoint.repo.sqale.qmodel.shadow.QShadow;
import com.evolveum.midpoint.repo.sqale.qmodel.system.QSystemConfiguration;
import com.evolveum.midpoint.repo.sqale.qmodel.task.MTask;
import com.evolveum.midpoint.repo.sqale.qmodel.task.QTask;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class SqaleRepoAddDeleteObjectTest extends SqaleRepoBaseTest {

    // region basic object/container and various item types tests
    @Test
    public void test100AddNamedUserWithoutOidWorksOk()
            throws ObjectAlreadyExistsException, SchemaException {
        OperationResult result = createOperationResult();

        given("user with a name");
        String userName = "user" + getTestNumber();
        UserType userType = new UserType()
                .name(userName)
                .version("5"); // version will be ignored and set to 1

        when("adding it to the repository");
        String returnedOid = repositoryService.addObject(userType.asPrismObject(), null, result);

        then("operation is successful and user row for it is created");
        assertThatOperationResult(result).isSuccess();
        assertThat(returnedOid).isEqualTo(userType.getOid());

        QUser u = aliasFor(QUser.class);
        MUser row = selectOne(u, u.nameOrig.eq(userName));
        assertThat(row.oid).isEqualTo(UUID.fromString(returnedOid));
        assertThat(row.nameNorm).isNotNull(); // normalized name is stored
        // initial version is set, ignoring provided version
        assertThat(row.version).isEqualTo(SqaleRepositoryService.INITIAL_VERSION_NUMBER);
        // read-only column with value generated/stored in the database
        assertThat(row.objectType).isEqualTo(MObjectType.USER);
        assertThat(row.subtypes).isNull(); // we don't store empty lists as empty arrays
    }

    @Test
    public void test101AddUserWithoutNameFails() {
        OperationResult result = createOperationResult();

        given("user without specified name");
        long baseCount = count(QUser.class);
        UserType userType = new UserType();

        expect("adding it to the repository throws exception and no row is created");
        assertThatThrownBy(() -> repositoryService.addObject(userType.asPrismObject(), null, result))
                .isInstanceOf(SchemaException.class)
                .hasMessage("Attempt to add object without name.");

        and("operation result is fatal error");
        assertThatOperationResult(result).isFatalError()
                .hasMessageContaining("Attempt to add object without name.");

        and("object count in the repository is not changed");
        assertCount(QUser.class, baseCount);
    }

    @Test
    public void test110AddWithoutOidIgnoresOverwriteOption()
            throws ObjectAlreadyExistsException, SchemaException {
        OperationResult result = createOperationResult();

        given("user with a name but without OID");
        String userName = "user" + getTestNumber();
        UserType userType = new UserType()
                .name(userName);

        when("adding it to the repository with overwrite option");
        repositoryService.addObject(userType.asPrismObject(), createOverwrite(), result);

        then("operation is successful and user row for it is created, overwrite is meaningless");
        assertThatOperationResult(result).isSuccess();

        QUser u = aliasFor(QUser.class);
        List<MUser> users = select(u, u.nameOrig.eq(userName));
        assertThat(users).hasSize(1);
        assertThat(users.get(0).oid).isNotNull();
    }

    @Test
    public void test111AddWithOverwriteOption()
            throws ObjectAlreadyExistsException, SchemaException {
        OperationResult result = createOperationResult();

        given("user already in the repository");
        long baseCount = count(QUser.class);
        String userName = "user" + getTestNumber();
        UserType userType = new UserType()
                .name(userName);
        repositoryService.addObject(userType.asPrismObject(), null, result);
        assertThat(count(QUser.class)).isEqualTo(baseCount + 1);

        when("adding it to the repository again with overwrite option");
        userType.setFullName(PolyStringType.fromOrig("Overwritten User"));
        userType.setVersion("5"); // should be ignored
        repositoryService.addObject(userType.asPrismObject(), createOverwrite(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("existing user row is modified/overwritten");
        assertThat(count(QUser.class)).isEqualTo(baseCount + 1); // no change in count
        MUser row = selectObjectByOid(QUser.class, userType.getOid());
        assertThat(row.fullNameOrig).isEqualTo("Overwritten User");

        and("provided version for overwrite is ignored");
        assertThat(row.version).isEqualTo(SqaleRepositoryService.INITIAL_VERSION_NUMBER + 1);
    }

    @Test
    public void test112AddWithOverwriteOptionWithNewOidActsLikeNormalAdd()
            throws ObjectAlreadyExistsException, SchemaException {
        OperationResult result = createOperationResult();

        given("user with random OID is not in the repository");
        long baseCount = count(QUser.class);
        UUID oid = UUID.randomUUID();
        assertThat(selectNullableObjectByOid(QUser.class, oid)).isNull();

        when("adding it to the repository again with overwrite option");
        String userName = "user" + getTestNumber();
        UserType userType = new UserType()
                .oid(oid.toString())
                .name(userName)
                .version("5");
        repositoryService.addObject(userType.asPrismObject(), createOverwrite(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("existing user row is modified/overwritten");
        assertThat(count(QUser.class)).isEqualTo(baseCount + 1); // no change in count
        MUser row = selectObjectByOid(QUser.class, userType.getOid());

        and("provided version for overwrite is ignored");
        assertThat(row.version).isEqualTo(SqaleRepositoryService.INITIAL_VERSION_NUMBER);
    }

    @Test
    public void test113AddWithOverwriteOptionDifferentTypes()
            throws ObjectAlreadyExistsException, SchemaException {
        OperationResult result = createOperationResult();

        given("user already in the repository (type A)");
        long baseCount = count(QObject.CLASS);
        UserType user = new UserType().name("user" + getTestNumber());
        String oid = repositoryService.addObject(user.asPrismObject(), null, result);
        assertThat(count(QObject.CLASS)).isEqualTo(baseCount + 1);

        expect("adding object of different type with the same OID to the repository with overwrite option throws");
        DashboardType dashboard = new DashboardType()
                .name("dashboard" + getTestNumber())
                .oid(oid);
        assertThatThrownBy(() -> repositoryService.addObject(dashboard.asPrismObject(), createOverwrite(), result))
                .isInstanceOf(ObjectAlreadyExistsException.class);

        and("operation is fatal error");
        assertThatOperationResult(result).isFatalError();

        and("nothing is added to the database");
        assertThat(count(QObject.CLASS)).isEqualTo(baseCount + 1);
    }

    @Test
    public void test119ModifyLegacyPreservesData() throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
        OperationResult result = createOperationResult();
        QUserMapping.getUserMapping().setStoreSplitted(false);
        QAssignmentMapping.getAssignmentMapping().setStoreFullObject(false);
        long baseCount = count(QObject.CLASS);

        UserType user = new UserType().name("user" + getTestNumber())
                .assignment(new AssignmentType().targetRef(UUID.randomUUID().toString(), RoleType.COMPLEX_TYPE));
        try {
            given("user already in the repository with legacy format (assignment.full_object is null)");
            String oid = repositoryService.addObject(user.asPrismObject(), null, result);
            assertThat(count(QObject.CLASS)).isEqualTo(baseCount + 1);

            QUserMapping.getUserMapping().setStoreSplitted(true);
            QAssignmentMapping.getAssignmentMapping().setStoreFullObject(true);
            expect("should be readed correctly with assignment present");
            UserType readed = repositoryService.getObject(UserType.class, oid, null, result).asObjectable();
            assertThat(readed.getAssignment()).hasSize(1);
            // Modify with splitted -

            and("when unrelated modification is applied");
            var deltas = prismContext.deltaFor(UserType.class)
                            .item(UserType.F_ACTIVATION).add(new ActivationType().administrativeStatus(ActivationStatusType.ENABLED))
                            .asItemDeltas();
            repositoryService.modifyObject(UserType.class, oid, deltas, result);
            and("assignment should be still there and reindexed");
            readed = repositoryService.getObject(UserType.class, oid, null, result).asObjectable();
            assertThat(readed.getAssignment()).hasSize(1);
            assertThat(readed.getActivation()).isNotNull();


        } finally {
            QUserMapping.getUserMapping().setStoreSplitted(true);
            QAssignmentMapping.getAssignmentMapping().setStoreFullObject(true);
        }
    }

    // detailed container tests are from test200 on, this one has overwrite priority :-)
    @Test
    public void test115OverwriteWithContainers()
            throws ObjectAlreadyExistsException, SchemaException {
        OperationResult result = createOperationResult();

        given("user with container in the repository");
        long baseCount = count(QUser.class);

        UUID assConstructionRef = UUID.randomUUID();
        QName assConstructionRel = QName.valueOf("{https://random.org/ns}const-rel");
        String userName = "user" + getTestNumber();
        UserType user1 = new UserType()
                .name(userName)
                .assignment(new AssignmentType()
                        .id(2L) // assigned CID to make things simple for tracking
                        .construction(new ConstructionType()
                                .resourceRef(assConstructionRef.toString(),
                                        ResourceType.COMPLEX_TYPE, assConstructionRel)));
        repositoryService.addObject(user1.asPrismObject(), null, result);
        assertThat(count(QUser.class)).isEqualTo(baseCount + 1);

        UUID userOid = UUID.fromString(user1.getOid());
        QAssignment<?> qa = QAssignmentMapping.getAssignmentMapping().defaultAlias();
        List<MAssignment> assRows = select(qa, qa.ownerOid.eq(userOid));
        assertThat(assRows).hasSize(1)
                // construction/resourceRef is set
                .anyMatch(aRow -> aRow.resourceRefTargetOid != null
                        && aRow.resourceRefTargetType != null
                        && aRow.resourceRefRelationId != null);

        when("using overwrite with changed container identified by id");
        UserType user2 = new UserType()
                .oid(user1.getOid())
                .version("5") // should be ignored
                .name(userName)
                .assignment(new AssignmentType()
                        .id(2L)
                        // no construction
                        .targetRef(UUID.randomUUID().toString(), RoleType.COMPLEX_TYPE));
        repositoryService.addObject(user2.asPrismObject(), createOverwrite(), result);

        then("operation is successful");
        assertThatOperationResult(result).isSuccess();

        and("identified container is modified/overwritten");
        assertThat(count(QUser.class)).isEqualTo(baseCount + 1); // no change in count
        MUser row = selectObjectByOid(QUser.class, user2.getOid());
        assRows = select(qa, qa.ownerOid.eq(UUID.fromString(user2.getOid())));
        assertThat(assRows).hasSize(1);
        MAssignment assRow = assRows.get(0);
        assertThat(assRow.resourceRefTargetOid).isNull(); // removed
        assertThat(assRow.resourceRefTargetType).isNull(); // removed
        assertThat(assRow.resourceRefRelationId).isNull(); // removed
        assertThat(assRow.targetRefTargetOid).isNotNull(); // added
        assertThat(assRow.targetRefTargetType).isEqualTo(MObjectType.ROLE);

        and("provided version for overwrite is ignored");
        assertThat(row.version).isEqualTo(SqaleRepositoryService.INITIAL_VERSION_NUMBER + 1);
    }

    @Test
    public void test120AddUserWithProvidedOidWorksOk()
            throws ObjectAlreadyExistsException, SchemaException {
        OperationResult result = createOperationResult();

        given("user with provided OID");
        UUID providedOid = UUID.randomUUID();
        String userName = "user" + getTestNumber();
        UserType userType = new UserType()
                .oid(providedOid.toString())
                .name(userName);

        when("adding it to the repository");
        repositoryService.addObject(userType.asPrismObject(), null, result);

        then("operation is successful and user row with provided OID is created");
        assertThatOperationResult(result).isSuccess();

        QUser u = aliasFor(QUser.class);
        List<MUser> users = select(u, u.nameOrig.eq(userName));
        assertThat(users).hasSize(1);

        MUser mUser = users.get(0);
        assertThat(mUser.oid).isEqualTo(providedOid);
        assertThat(mUser.version).isEqualTo(SqaleRepositoryService.INITIAL_VERSION_NUMBER);
    }

    @Test
    public void test121AddSecondObjectWithTheSameOidThrowsObjectAlreadyExists()
            throws ObjectAlreadyExistsException, SchemaException {
        OperationResult result = createOperationResult();

        given("user with provided OID already exists");
        UUID providedOid = UUID.randomUUID();
        UserType user1 = new UserType()
                .oid(providedOid.toString())
                .name("user" + getTestNumber());
        repositoryService.addObject(user1.asPrismObject(), null, result);

        when("adding it another user with the same OID to the repository");
        long baseCount = count(QUser.class);
        UserType user2 = new UserType()
                .oid(providedOid.toString())
                .name("user" + getTestNumber());

        then("operation fails and no new user row is created");
        assertThatThrownBy(() -> repositoryService.addObject(user2.asPrismObject(), null, result))
                .isInstanceOf(ObjectAlreadyExistsException.class);
        assertThatOperationResult(result).isFatalError()
                .hasMessageMatching("Provided OID .* already exists");
        assertCount(QUser.class, baseCount);
    }

    @Test
    public void test122AddSecondObjectWithTheSameOidWithOverwriteIsOk()
            throws ObjectAlreadyExistsException, SchemaException {
        OperationResult result = createOperationResult();

        given("user with provided OID already exists");
        UUID providedOid = UUID.randomUUID();
        UserType user1 = new UserType()
                .oid(providedOid.toString())
                .name("user" + getTestNumber());
        repositoryService.addObject(user1.asPrismObject(), null, result);

        when("adding it again with overwrite without any changes");
        long baseCount = count(QObject.CLASS);
        UserType user2 = new UserType()
                .oid(providedOid.toString())
                .name("user" + getTestNumber());
        repositoryService.addObject(user2.asPrismObject(), createOverwrite(), result);

        then("operation is success and no changes are made (delta is empty)");
        assertThatOperationResult(result).isSuccess();
        assertCount(QObject.CLASS, baseCount); // no new object was created
        MUser row = selectObjectByOid(QUser.class, providedOid);
        assertThat(row.version).isEqualTo(SqaleRepositoryService.INITIAL_VERSION_NUMBER); // no change
    }

    @Test
    public void test150AddOperationUpdatesPerformanceMonitor()
            throws ObjectAlreadyExistsException, SchemaException {
        OperationResult result = createOperationResult();

        given("object to add and cleared performance information");
        UserType userType = new UserType().name("user" + getTestNumber());
        clearPerformanceMonitor();

        when("object is added to the repository");
        repositoryService.addObject(userType.asPrismObject(), null, result);

        then("performance monitor is updated");
        assertThatOperationResult(result).isSuccess();
        assertSingleOperationRecorded(REPO_OP_PREFIX + RepositoryService.OP_ADD_OBJECT);
    }

    @Test
    public void test151OverwriteOperationUpdatesPerformanceMonitor()
            throws ObjectAlreadyExistsException, SchemaException {
        OperationResult result = createOperationResult();

        given("existing object for overwrite and cleared performance information");
        UserType userType = new UserType().name("user" + getTestNumber());
        repositoryService.addObject(userType.asPrismObject(), null, result);

        clearPerformanceMonitor();

        when("object is added to the repository");
        repositoryService.addObject(userType.asPrismObject(), createOverwrite(), result);

        then("performance monitor is updated");
        assertThatOperationResult(result).isSuccess();
        assertSingleOperationRecorded(REPO_OP_PREFIX + RepositoryService.OP_ADD_OBJECT_OVERWRITE);
    }

    @Test
    public void test200AddObjectWithMultivalueContainers()
            throws ObjectAlreadyExistsException, SchemaException {
        OperationResult result = createOperationResult();

        given("user with assignment and ref");
        String userName = "user" + getTestNumber();
        String targetRef1 = UUID.randomUUID().toString();
        String targetRef2 = UUID.randomUUID().toString();
        UserType user = new UserType()
                .name(userName)
                .assignment(new AssignmentType()
                        .targetRef(targetRef1, RoleType.COMPLEX_TYPE))
                .assignment(new AssignmentType()
                        .targetRef(targetRef2, RoleType.COMPLEX_TYPE));

        when("adding it to the repository");
        repositoryService.addObject(user.asPrismObject(), null, result);

        then("object and its container rows are created and container IDs are assigned");
        assertThatOperationResult(result).isSuccess();

        QUser u = aliasFor(QUser.class);
        List<MUser> users = select(u, u.nameOrig.eq(userName));
        assertThat(users).hasSize(1);
        MUser userRow = users.get(0);
        assertThat(userRow.oid).isNotNull();
        assertThat(userRow.containerIdSeq).isEqualTo(3); // next free container number

        QContainer<MContainer, ?> c = aliasFor(QAssignment.class);
        List<MContainer> containers = select(c, c.ownerOid.eq(userRow.oid));
        assertThat(containers).hasSize(2)
                .allMatch(cRow -> cRow.ownerOid.equals(userRow.oid)
                        && cRow.containerType == MContainerType.ASSIGNMENT)
                .extracting(cRow -> cRow.cid)
                .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    public void test201AddObjectWithOidAndMultivalueContainers()
            throws ObjectAlreadyExistsException, SchemaException {
        OperationResult result = createOperationResult();

        given("user with assignment and ref");
        UUID providedOid = UUID.randomUUID();
        String userName = "user" + getTestNumber();
        String targetRef1 = UUID.randomUUID().toString();
        String targetRef2 = UUID.randomUUID().toString();
        UserType user = new UserType()
                .oid(providedOid.toString())
                .name(userName)
                .assignment(new AssignmentType()
                        .targetRef(targetRef1, RoleType.COMPLEX_TYPE))
                .assignment(new AssignmentType()
                        .targetRef(targetRef2, RoleType.COMPLEX_TYPE));

        when("adding it to the repository");
        repositoryService.addObject(user.asPrismObject(), null, result);

        then("object and its container rows are created and container IDs are assigned");
        assertThatOperationResult(result).isSuccess();

        QUser u = aliasFor(QUser.class);
        List<MUser> users = select(u, u.nameOrig.eq(userName));
        assertThat(users).hasSize(1);
        MUser userRow = users.get(0);
        assertThat(userRow.oid).isNotNull();
        assertThat(userRow.containerIdSeq).isEqualTo(3); // next free container number

        QContainer<MContainer, ?> c = aliasFor(QAssignment.class);
        List<MContainer> containers = select(c, c.ownerOid.eq(userRow.oid));
        assertThat(containers).hasSize(2)
                .allMatch(cRow -> cRow.ownerOid.equals(userRow.oid)
                        && cRow.containerType == MContainerType.ASSIGNMENT)
                .extracting(cRow -> cRow.cid)
                .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    public void test202AddObjectWithMultivalueContainersWithExplicitIds()
            throws ObjectAlreadyExistsException, SchemaException {
        OperationResult result = createOperationResult();

        given("user with assignment and ref");
        String userName = "user" + getTestNumber();
        String targetRef1 = UUID.randomUUID().toString();
        String targetRef2 = UUID.randomUUID().toString();
        UserType user = new UserType()
                .name(userName)
                .assignment(new AssignmentType()
                        .id(10L)
                        .targetRef(targetRef1, RoleType.COMPLEX_TYPE))
                .assignment(new AssignmentType()
                        .id(12L)
                        .targetRef(targetRef2, RoleType.COMPLEX_TYPE));

        when("adding it to the repository");
        repositoryService.addObject(user.asPrismObject(), null, result);

        then("object and its container rows are created and container IDs are assigned");
        assertThatOperationResult(result).isSuccess();

        QUser u = aliasFor(QUser.class);
        List<MUser> users = select(u, u.nameOrig.eq(userName));
        assertThat(users).hasSize(1);
        MUser userRow = users.get(0);
        assertThat(userRow.oid).isNotNull();
        assertThat(userRow.containerIdSeq).isEqualTo(13); // next free container number

        QContainer<MContainer, ?> c = aliasFor(QAssignment.class);
        List<MContainer> containers = select(c, c.ownerOid.eq(userRow.oid));
        assertThat(containers).hasSize(2)
                .allMatch(cRow -> cRow.ownerOid.equals(userRow.oid)
                        && cRow.containerType == MContainerType.ASSIGNMENT)
                .extracting(cRow -> cRow.cid)
                .containsExactlyInAnyOrder(10L, 12L);
    }

    @Test
    public void test203AddObjectWithOidAndMultivalueContainersWithExplicitIds()
            throws ObjectAlreadyExistsException, SchemaException {
        OperationResult result = createOperationResult();

        given("user with assignment and ref");
        UUID providedOid = UUID.randomUUID();
        String userName = "user" + getTestNumber();
        String targetRef1 = UUID.randomUUID().toString();
        String targetRef2 = UUID.randomUUID().toString();
        UserType user = new UserType()
                .oid(providedOid.toString())
                .name(userName)
                .assignment(new AssignmentType()
                        .id(10L)
                        .targetRef(targetRef1, RoleType.COMPLEX_TYPE))
                .assignment(new AssignmentType()
                        .id(12L)
                        .targetRef(targetRef2, RoleType.COMPLEX_TYPE));

        when("adding it to the repository");
        repositoryService.addObject(user.asPrismObject(), null, result);

        then("object and its container rows are created and container IDs are assigned");
        assertThatOperationResult(result).isSuccess();

        QUser u = aliasFor(QUser.class);
        List<MUser> users = select(u, u.nameOrig.eq(userName));
        assertThat(users).hasSize(1);
        MUser userRow = users.get(0);
        assertThat(userRow.oid).isNotNull();
        assertThat(userRow.containerIdSeq).isEqualTo(13); // next free container number

        QContainer<MContainer, ?> c = aliasFor(QAssignment.class);
        List<MContainer> containers = select(c, c.ownerOid.eq(userRow.oid));
        assertThat(containers).hasSize(2)
                .allMatch(cRow -> cRow.ownerOid.equals(userRow.oid)
                        && cRow.containerType == MContainerType.ASSIGNMENT)
                .extracting(cRow -> cRow.cid)
                .containsExactlyInAnyOrder(10L, 12L);
    }

    @Test
    public void test205AddObjectWithMultivalueRefs()
            throws ObjectAlreadyExistsException, SchemaException {
        OperationResult result = createOperationResult();

        given("user with ref");
        String userName = "user" + getTestNumber();
        String targetRef1 = UUID.randomUUID().toString();
        String targetRef2 = UUID.randomUUID().toString();
        UserType user = new UserType()
                .name(userName)
                .linkRef(targetRef1, RoleType.COMPLEX_TYPE)
                .linkRef(targetRef2, RoleType.COMPLEX_TYPE);

        when("adding it to the repository");
        repositoryService.addObject(user.asPrismObject(), null, result);

        then("object and its reference rows are created");
        assertThatOperationResult(result).isSuccess();

        QUser u = aliasFor(QUser.class);
        List<MUser> users = select(u, u.nameOrig.eq(userName));
        assertThat(users).hasSize(1);
        MUser userRow = users.get(0);
        assertThat(userRow.oid).isNotNull();
        assertThat(userRow.containerIdSeq).isEqualTo(1); // cid sequence is in initial state

        UUID userOid = UUID.fromString(user.getOid());
        QObjectReference<?> or = QObjectReferenceMapping.getForProjection().defaultAlias();
        List<MReference> projectionRefs = select(or, or.ownerOid.eq(userOid));
        assertThat(projectionRefs).hasSize(2)
                .allMatch(rRow -> rRow.referenceType == MReferenceType.PROJECTION)
                .allMatch(rRow -> rRow.ownerOid.equals(userOid))
                .extracting(rRow -> rRow.targetOid.toString())
                .containsExactlyInAnyOrder(targetRef1, targetRef2);
        // this is the same set of refs queried from the super-table
        QReference<MReference, ?> r = aliasFor(QReference.CLASS);
        List<MReference> refs = select(r, r.ownerOid.eq(userOid));
        assertThat(refs).hasSize(2)
                .allMatch(rRow -> rRow.referenceType == MReferenceType.PROJECTION);
    }

    @Test
    public void test206AddObjectWithMultivalueRefsOnAssignment()
            throws ObjectAlreadyExistsException, SchemaException {
        OperationResult result = createOperationResult();

        given("user with ref");
        String userName = "user" + getTestNumber();
        UUID approverRef1 = UUID.randomUUID();
        UUID approverRef2 = UUID.randomUUID();
        QName approverRelation = QName.valueOf("{https://random.org/ns}conn-rel");
        UserType user = new UserType()
                .name(userName)
                .assignment(new AssignmentType()
                        .metadata(new MetadataType()
                                .createApproverRef(approverRef1.toString(),
                                        UserType.COMPLEX_TYPE, approverRelation)
                                .createApproverRef(approverRef2.toString(), UserType.COMPLEX_TYPE)));

        when("adding it to the repository");
        String oid = repositoryService.addObject(user.asPrismObject(), null, result);

        then("object and its reference rows are created");
        assertThatOperationResult(result).isSuccess();

        MUser userRow = selectObjectByOid(QUser.class, oid);
        assertThat(userRow.oid).isNotNull();

        QAssignmentReference ar =
                QAssignmentReferenceMapping.getForAssignmentCreateApprover().defaultAlias();
        List<MAssignmentReference> projectionRefs = select(ar, ar.ownerOid.eq(userRow.oid));
        assertThat(projectionRefs).hasSize(2)
                .allMatch(rRow -> rRow.referenceType == MReferenceType.ASSIGNMENT_CREATE_APPROVER)
                .allMatch(rRow -> rRow.ownerOid.equals(userRow.oid))
                .allMatch(rRow -> rRow.assignmentCid.equals(1L)) // there's just one container
                .anyMatch(refRowMatcher(approverRef1, approverRelation));
    }

    @Test
    public void test208AddObjectWithRefWithoutTypeImpliedByDefault()
            throws ObjectAlreadyExistsException, SchemaException {
        OperationResult result = createOperationResult();

        given("object with ref without specified target type");
        String userName = "user" + getTestNumber();
        UUID approverRef1 = UUID.randomUUID();
        UserType user = new UserType()
                .name(userName)
                .metadata(new MetadataType().creatorRef(
                        new ObjectReferenceType().oid(approverRef1.toString())));

        when("adding it to the repository");
        String oid = repositoryService.addObject(user.asPrismObject(), null, result);

        then("object and its reference rows are created");
        assertThatOperationResult(result).isSuccess();

        MUser userRow = selectObjectByOid(QUser.class, oid);
        assertThat(userRow.oid).isNotNull();
        assertThat(userRow.creatorRefTargetOid).isEqualTo(approverRef1);
        assertThat(userRow.creatorRefTargetType).isEqualTo(MObjectType.USER); // default from def
    }

    @Test
    public void test290DuplicateCidInsideOneContainerIsCaughtByPrism() {
        expect("object construction with duplicate CID inside container fails immediately");
        assertThatThrownBy(() -> new UserType()
                .assignment(new AssignmentType()
                        .targetRef("ref1", RoleType.COMPLEX_TYPE).id(1L))
                .assignment(new AssignmentType()
                        .targetRef("ref2", RoleType.COMPLEX_TYPE).id(1L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Attempt to add a container value with an id that already exists: 1");
    }

    @Test
    public void test291DuplicateCidInDifferentContainersIsTolerated()
            throws SchemaException, ObjectAlreadyExistsException {
        OperationResult result = createOperationResult();
        long previousUserCount = count(QUser.class);

        given("object with duplicate CID in different containers");
        UserType user = new UserType()
                .name("user" + getTestNumber())
                .assignment(new AssignmentType().id(1L))
                .operationExecution(new OperationExecutionType().id(1L));

        when("adding object to repository throws exception");
        repositoryService.addObject(user.asPrismObject(), null, result);

        then("operation is success");
        assertThatOperationResult(result).isSuccess();
        assertThat(count(QUser.class)).isEqualTo(previousUserCount + 1);
    }
    // endregion

    // region extension items
    @Test
    public void test300AddObjectWithIndexedStringExtension()
            throws ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException {
        OperationResult result = createOperationResult();

        given("object with string extension item");
        String objectName = "user" + getTestNumber();
        UserType object = new UserType()
                .name(objectName)
                .extension(new ExtensionType());
        ExtensionType extensionContainer = object.getExtension();
        addExtensionValue(extensionContainer, "string", "string-value");

        when("adding it to the repository");
        String returnedOid = repositoryService.addObject(object.asPrismObject(), null, result);

        then("operation is successful and ext column contains the value");
        assertThatOperationResult(result).isSuccess();
        assertThat(returnedOid).isEqualTo(object.getOid());

        MUser row = selectObjectByOid(QUser.class, returnedOid);
        assertThat(row.oid).isEqualTo(UUID.fromString(returnedOid));
        assertThat(row.ext).isNotNull();
        assertThat(Jsonb.toMap(row.ext))
                .containsEntry(extensionKey(extensionContainer, "string"), "string-value");

        and("stored object contains the extension item");
        PrismObject<UserType> storedObject =
                repositoryService.getObject(UserType.class, returnedOid, null, result);
        assertThat(storedObject.getExtension().findItem(new ItemName("string")))
                .isNotNull()
                .extracting(i -> i.getRealValue())
                .isEqualTo("string-value");
    }

    @Test
    public void test301AddObjectWithNonIndexedStringExtension()
            throws ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException {
        OperationResult result = createOperationResult();

        given("object with string extension item");
        String objectName = "user" + getTestNumber();
        UserType object = new UserType()
                .name(objectName)
                .extension(new ExtensionType());
        ExtensionType extensionContainer = object.getExtension();
        addExtensionValue(extensionContainer, "string-ni", "string-value");

        when("adding it to the repository");
        String returnedOid = repositoryService.addObject(object.asPrismObject(), null, result);

        then("operation is successful and ext column is null");
        assertThatOperationResult(result).isSuccess();
        assertThat(returnedOid).isEqualTo(object.getOid());

        MUser row = selectObjectByOid(QUser.class, returnedOid);
        assertThat(row.ext).isNull();

        and("stored object contains the extension item");
        PrismObject<UserType> storedObject =
                repositoryService.getObject(UserType.class, returnedOid, null, result);
        assertThat(storedObject.getExtension().findItem(new ItemName("string-ni")))
                .isNotNull()
                .extracting(i -> i.getRealValue())
                .isEqualTo("string-value");
    }

    /**
     * Disabled, as of now, base64binary is supported.
     *
     * **/
    @Test(enabled = false)
    public void test302AddObjectWithExtensionItemOfNonIndexableType()
            throws ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException {
        OperationResult result = createOperationResult();

        given("object with extension item of non-indexable type");
        String objectName = "user" + getTestNumber();
        UserType object = new UserType()
                .name(objectName)
                .extension(new ExtensionType());
        ExtensionType extensionContainer = object.getExtension();
        addExtensionValue(extensionContainer, "blob", "bytes".getBytes(StandardCharsets.UTF_8));

        when("adding it to the repository");
        String returnedOid = repositoryService.addObject(object.asPrismObject(), null, result);

        then("operation is successful and ext column is null");
        assertThatOperationResult(result).isSuccess();
        assertThat(returnedOid).isEqualTo(object.getOid());

        MUser row = selectObjectByOid(QUser.class, returnedOid);
        assertThat(row.ext).isNull();

        and("stored object contains the extension item");
        PrismObject<UserType> storedObject =
                repositoryService.getObject(UserType.class, returnedOid, null, result);
        assertThat(storedObject.getExtension().findItem(new ItemName("blob")))
                .isNotNull();
    }

    @Test
    public void test305AddObjectWithExtensionItemsOfVariousSimpleTypes()
            throws ObjectAlreadyExistsException, SchemaException {
        OperationResult result = createOperationResult();

        given("object with extension items of various simple types");
        String objectName = "user" + getTestNumber();
        UserType object = new UserType()
                .name(objectName)
                .extension(new ExtensionType());
        ExtensionType extensionContainer = object.getExtension();
        addExtensionValue(extensionContainer, "int", 1);
        addExtensionValue(extensionContainer, "short", (short) 2);
        addExtensionValue(extensionContainer, "long", 3L);
        addExtensionValue(extensionContainer, "integer", BigInteger.valueOf(4));
        addExtensionValue(extensionContainer, "decimal",
                new BigDecimal("12345678901234567890.12345678901234567890"));
        addExtensionValue(extensionContainer, "decimal-2", new BigDecimal("12345678901234567890"));
        addExtensionValue(extensionContainer, "decimal-3", new BigDecimal("-1"));
        addExtensionValue(extensionContainer, "double", Double.MAX_VALUE);
        addExtensionValue(extensionContainer, "double-2", -Double.MIN_VALUE);
        addExtensionValue(extensionContainer, "float", Float.MAX_VALUE);
        addExtensionValue(extensionContainer, "float-2", -Float.MIN_VALUE);
        addExtensionValue(extensionContainer, "boolean", true);
        addExtensionValue(extensionContainer, "enum", BeforeAfterType.AFTER);
        Instant dateTime = Instant.now();
        addExtensionValue(extensionContainer, "dateTime",
                MiscUtil.asXMLGregorianCalendar(dateTime));

        when("adding it to the repository");
        String returnedOid = repositoryService.addObject(object.asPrismObject(), null, result);

        then("operation is successful and ext column stores the values");
        assertThatOperationResult(result).isSuccess();
        assertThat(returnedOid).isEqualTo(object.getOid());

        MUser row = selectObjectByOid(QUser.class, returnedOid);
        assertThat(row.ext).isNotNull();
        assertThat(Jsonb.toMap(row.ext))
                .containsEntry(extensionKey(extensionContainer, "int"), 1)
                .containsEntry(extensionKey(extensionContainer, "short"), 2) // returned as Integer
                .containsEntry(extensionKey(extensionContainer, "long"), 3) // returned as Integer
                .containsEntry(extensionKey(extensionContainer, "integer"), 4) // returned as Integer
                .containsEntry(extensionKey(extensionContainer, "decimal"),
                        new BigDecimal("12345678901234567890.12345678901234567890"))
                .containsEntry(extensionKey(extensionContainer, "decimal-2"),
                        new BigInteger("12345678901234567890")) // no decimal part, returned as BInt
                .containsEntry(extensionKey(extensionContainer, "decimal-3"), -1) // Integer
                // Returned as BigInteger. Always prefer String constructor for BigDecimal or
                // BigInteger (that doesn't offer anything for float/double, understandably).
                // The value is "whole number", but string output is in scientific notation, so
                // we need BigDecimal first.
                .containsEntry(extensionKey(extensionContainer, "double"),
                        new BigDecimal(Double.toString(Double.MAX_VALUE)).toBigInteger())
                .containsEntry(extensionKey(extensionContainer, "double-2"),
                        new BigDecimal(Double.toString(-Double.MIN_VALUE)))
                // Returned as BigInteger. Don't use new BD(float), neither BD.valueOf(double),
                // because it changes the float and "thinks up" more non-zero numbers.
                .containsEntry(extensionKey(extensionContainer, "float"),
                        new BigDecimal(Float.toString(Float.MAX_VALUE)).toBigInteger())
                .containsEntry(extensionKey(extensionContainer, "float-2"),
                        new BigDecimal(Float.toString(-Float.MIN_VALUE)))
                .containsEntry(extensionKey(extensionContainer, "boolean"), true)
                .containsEntry(extensionKey(extensionContainer, "enum"), "AFTER")
                // Must be truncated to millis, because XML dateTime drops nanos.
                .containsEntry(extensionKey(extensionContainer, "dateTime"),
                        dateTime.truncatedTo(ChronoUnit.MILLIS).toString());
        // The different types are OK here, must be treated only for index-only extensions.
        // In that case value must be converted to the expected target type; not part of this test.
    }

    @Test
    public void test307AddObjectWithExtensionReferenceAndPolyString()
            throws ObjectAlreadyExistsException, SchemaException {
        OperationResult result = createOperationResult();

        given("object with extension reference and poly string");
        String objectName = "user" + getTestNumber();
        UserType object = new UserType()
                .name(objectName)
                .extension(new ExtensionType());
        ExtensionType extensionContainer = object.getExtension();
        addExtensionValue(extensionContainer, "poly", PolyString.fromOrig("poly-value"));
        String targetOid = UUID.randomUUID().toString();
        QName relation = QName.valueOf("{https://random.org/ns}random-rel-1");
        addExtensionValue(extensionContainer, "ref",
                ref(targetOid, UserType.COMPLEX_TYPE, relation));

        when("adding it to the repository");
        String returnedOid = repositoryService.addObject(object.asPrismObject(), null, result);

        then("operation is successful and ext column stores the values as nested objects");
        assertThatOperationResult(result).isSuccess();
        assertThat(returnedOid).isEqualTo(object.getOid());

        MUser row = selectObjectByOid(QUser.class, returnedOid);
        assertThat(row.ext).isNotNull();
        assertThat(Jsonb.toMap(row.ext))
                .containsEntry(extensionKey(extensionContainer, "poly"),
                        Map.of("o", "poly-value", "n", "polyvalue"))
                .containsEntry(extensionKey(extensionContainer, "ref"),
                        Map.of("o", targetOid,
                                "t", MObjectType.USER.name(),
                                "r", cachedUriId(relation)));
    }

    @Test
    public void test308AddObjectWithExtensionMultiValueItems()
            throws ObjectAlreadyExistsException, SchemaException {
        OperationResult result = createOperationResult();

        given("object with extension reference and poly string");
        String objectName = "user" + getTestNumber();
        UserType object = new UserType()
                .name(objectName)
                .extension(new ExtensionType());
        ExtensionType extensionContainer = object.getExtension();
        addExtensionValue(extensionContainer, "string-mv", "string-value1", "string-value2");
        addExtensionValue(extensionContainer, "poly-mv",
                PolyString.fromOrig("poly-value1"),
                PolyString.fromOrig("poly-value2"),
                PolyString.fromOrig("poly-value3"));
        String targetOid1 = UUID.randomUUID().toString();
        String targetOid2 = UUID.randomUUID().toString();
        QName relation = QName.valueOf("{https://random.org/ns}random-rel-1");
        addExtensionValue(extensionContainer, "ref-mv",
                ref(targetOid1, null, relation), // type is nullable if provided in schema
                ref(targetOid2, UserType.COMPLEX_TYPE));

        when("adding it to the repository");
        String returnedOid = repositoryService.addObject(object.asPrismObject(), null, result);

        then("operation is successful and ext column stores the values as JSON arrays");
        assertThatOperationResult(result).isSuccess();
        assertThat(returnedOid).isEqualTo(object.getOid());

        MUser row = selectObjectByOid(QUser.class, returnedOid);
        assertThat(row.ext).isNotNull();
        assertThat(Jsonb.toMap(row.ext))
                .containsEntry(extensionKey(extensionContainer, "string-mv"),
                        List.of("string-value1", "string-value2"))
                .containsEntry(extensionKey(extensionContainer, "poly-mv"), List.of(
                        Map.of("o", "poly-value1", "n", "polyvalue1"),
                        Map.of("o", "poly-value2", "n", "polyvalue2"),
                        Map.of("o", "poly-value3", "n", "polyvalue3")))
                .containsEntry(extensionKey(extensionContainer, "ref-mv"), List.of(
                        Map.of("o", targetOid1,
                                "t", MObjectType.ORG.name(), // default from schema
                                "r", cachedUriId(relation)),
                        Map.of("o", targetOid2,
                                "t", MObjectType.USER.name(),
                                "r", cachedUriId(SchemaConstants.ORG_DEFAULT))));
    }

    @Test(description = "MID-10288")
    public void test309AddObjectWithEmptyExtensionProperty() throws Exception {
        OperationResult result = createOperationResult();

        given("object with string extension item without value");
        String objectName = "user" + getTestNumber();
        UserType object = new UserType()
                .name(objectName)
                .extension(new ExtensionType());
        ExtensionType extensionContainer = object.getExtension();
        addExtensionValue(extensionContainer, "string-mv", "string-value1", "string-value2");
        addExtensionValue(extensionContainer, "string", null);

        when("adding it to the repository");
        String returnedOid = repositoryService.addObject(object.asPrismObject(), null, result);

        then("operation is successful and ext column contains the value");
        assertThatOperationResult(result).isSuccess();
        assertThat(returnedOid).isEqualTo(object.getOid());

        MUser row = selectObjectByOid(QUser.class, returnedOid);
        assertThat(row.oid).isEqualTo(UUID.fromString(returnedOid));
        assertThat(row.ext).isNotNull();
        assertThat(Jsonb.toMap(row.ext)).doesNotContainKeys(extensionKey(extensionContainer, "string"));
        and("stored object contains the extension item");
    }


    @Test
    public void test310AddObjectWithAssignmentExtensions()
            throws ObjectAlreadyExistsException, SchemaException {
        OperationResult result = createOperationResult();

        given("object with extension items in assignment");
        String objectName = "user" + getTestNumber();
        AssignmentType assignment = new AssignmentType()
                .extension(new ExtensionType());
        UserType object = new UserType()
                .name(objectName)
                .assignment(assignment);
        ExtensionType extensionContainer = assignment.getExtension();
        addExtensionValue(extensionContainer, "string-mv", "string-value1", "string-value2");
        addExtensionValue(extensionContainer, "integer", BigInteger.valueOf(1));
        String targetOid = UUID.randomUUID().toString();
        addExtensionValue(extensionContainer, "ref", ref(targetOid, UserType.COMPLEX_TYPE));

        when("adding it to the repository");
        String returnedOid = repositoryService.addObject(object.asPrismObject(), null, result);

        then("operation is successful and assignment's ext column stores the values");
        assertThatOperationResult(result).isSuccess();
        assertThat(returnedOid).isEqualTo(object.getOid());

        QAssignment<MUser> a = QAssignmentMapping.<MUser>getAssignmentMapping().defaultAlias();
        MAssignment row = selectOne(a, a.ownerOid.eq(UUID.fromString(returnedOid)));
        assertThat(row.ext).isNotNull();
        assertThat(Jsonb.toMap(row.ext))
                .containsEntry(extensionKey(extensionContainer, "string-mv"),
                        List.of("string-value1", "string-value2"))
                .containsEntry(extensionKey(extensionContainer, "integer"), 1) // returned as Integer
                .containsEntry(extensionKey(extensionContainer, "ref"),
                        Map.of("o", targetOid,
                                "t", MObjectType.USER.name(),
                                "r", cachedUriId(SchemaConstants.ORG_DEFAULT)));
    }

    @Test
    public void test320AddShadowWithAttributes()
            throws ObjectAlreadyExistsException, SchemaException {
        OperationResult result = createOperationResult();

        given("shadow with both extensions and custom attributes");
        String objectName = "shadow" + getTestNumber();
        ShadowType object = new ShadowType()
                .name(objectName)
                .resourceRef(UUID.randomUUID().toString(), ResourceType.COMPLEX_TYPE)
                .objectClass(SchemaConstants.RI_ACCOUNT_OBJECT_CLASS)
                .activation(new ActivationType()
                        .disableReason(SchemaConstants.MODEL_DISABLE_REASON_EXPLICIT)
                        .enableTimestamp(XmlTypeConverter.createXMLGregorianCalendar())
                )
                .extension(new ExtensionType());

        ExtensionType extensionContainer = object.getExtension();
        addExtensionValue(extensionContainer, "string", "string-value");

        ShadowAttributesType attributesContainer = new ShadowAttributesHelper(object)
                .set(new QName("https://example.com/p", "string-mv"), DOMUtil.XSD_STRING,
                        "string-value1", "string-value2")
                .attributesContainer();

        when("adding it to the repository");
        String returnedOid = repositoryService.addObject(object.asPrismObject(), null, result);

        then("operation is successful and both ext and attributes column store the values");
        assertThatOperationResult(result).isSuccess();
        assertThat(returnedOid).isEqualTo(object.getOid());

        MShadow row = selectObjectByOid(QShadow.class, returnedOid);
        assertThat(row.ext).isNotNull();
        assertThat(Jsonb.toMap(row.ext))
                .containsEntry(extensionKey(extensionContainer, "string"), "string-value");

        assertThat(row.attributes).isNotNull();
        assertThat(Jsonb.toMap(row.attributes))
                .containsEntry(shadowAttributeKey(attributesContainer, "string-mv"),
                        List.of("string-value1", "string-value2"));
    }
    // endregion

    // region metadata
    @Test
    public void test400AddingUserWithValueMetadata() throws Exception {
        OperationResult result = createOperationResult();

        given("a user with value metadata");
        String objectName = "u" + getTestNumber();
        UUID creatorRefOid = UUID.randomUUID();
        UUID modifierRefOid = UUID.randomUUID();
        QName relation1 = QName.valueOf("{https://random.org/ns}random-rel-1");
        QName relation2 = QName.valueOf("{https://random.org/ns}random-rel-2");

        var storageMetadata = new StorageMetadataType()
                .creatorRef(creatorRefOid.toString(), UserType.COMPLEX_TYPE, relation1)
                .createChannel("create-channel")
                .createTimestamp(MiscUtil.asXMLGregorianCalendar(1L))
                .modifierRef(modifierRefOid.toString(), UserType.COMPLEX_TYPE, relation2)
                .modifyChannel("modify-channel")
                .modifyTimestamp(MiscUtil.asXMLGregorianCalendar(2L));
        var provenanceMetadata = new ProvenanceMetadataType().mappingSpecification(new MappingSpecificationType().mappingName("foo"));

        var assignmentBase = new AssignmentType().targetRef(modifierRefOid.toString(), RoleType.COMPLEX_TYPE);
        var assignment = assignmentBase.clone();
        ValueMetadataTypeUtil.getOrCreateMetadata(assignment)
                .storage(storageMetadata.clone())
                .provenance(provenanceMetadata.clone());
        UserType user = new UserType()
                .name(objectName)
                .assignment(assignment);
        ValueMetadataTypeUtil.getOrCreateMetadata(user)
                .storage(new StorageMetadataType()
                        .creatorRef(creatorRefOid.toString(), UserType.COMPLEX_TYPE, relation1)
                        .createChannel("create-channel")
                        .createTimestamp(MiscUtil.asXMLGregorianCalendar(1L))
                        .modifierRef(modifierRefOid.toString(), UserType.COMPLEX_TYPE, relation2)
                        .modifyChannel("modify-channel")
                        .modifyTimestamp(MiscUtil.asXMLGregorianCalendar(2L)));

        when("adding it to the repository");
        repositoryService.addObject(user.asPrismObject(), null, result);

        then("it is stored OK");
        assertThatOperationResult(result).isSuccess();

        MObject row = selectObjectByOid(QUser.class, user.getOid());
        display("FULL OBJECT: " + new String(row.fullObject, StandardCharsets.UTF_8));

        // currently, we need only some checks here:
        var userReloaded = repositoryService.getObject(UserType.class, user.getOid(), null, result).asObjectable();
        var metadataReloaded = ValueMetadataTypeUtil.getMetadata(userReloaded);
        assertThat(metadataReloaded).isNotNull();
        assertThat(metadataReloaded.getId()).isNotNull();
        var storageReloaded = metadataReloaded.getStorage();
        assertThat(storageReloaded).isNotNull();
        assertThat(storageReloaded.getCreatorRef().getOid()).isEqualTo(creatorRefOid.toString());
        assertThat(storageReloaded.getCreateChannel()).isEqualTo("create-channel");
        // etc (those will be probably ok)


        when("Assignment is added with different metadata");


        var assignmentDifferentMapping = assignmentBase.clone();
        ValueMetadataTypeUtil.getOrCreateMetadata(assignmentDifferentMapping).provenance(new ProvenanceMetadataType()
                .mappingSpecification(new MappingSpecificationType().mappingName("second")));
        var deltas = PrismContext.get().deltaFor(UserType.class).item(UserType.F_ASSIGNMENT).add(assignmentDifferentMapping.clone()).asItemDeltas();
        repositoryService.modifyObject(UserType.class, user.getOid(), deltas, result);

        var doubleMeta = repositoryService.getObject(UserType.class, user.getOid(), null, result);
        then("Assignment should contain both mappings");
        assertThat(doubleMeta.asObjectable().getAssignment().get(0).asPrismContainerValue().getValueMetadata().getValues())
                .extracting(v -> ((ValueMetadataType) v.asContainerable()).getProvenance().getMappingSpecification().getMappingName())
                .containsExactlyInAnyOrder("foo", "second");
        var assigmentId = doubleMeta.asObjectable().getAssignment().get(0).getId();
        when("Assignment with second mapping is deleted");
        deltas = PrismContext.get().deltaFor(UserType.class).item(UserType.F_ASSIGNMENT).delete(assignmentDifferentMapping.clone().id(assigmentId)).asItemDeltas();

        repositoryService.modifyObject(UserType.class, user.getOid(), deltas, result);
        doubleMeta = repositoryService.getObject(UserType.class, user.getOid(), null, result);
        assertThat(doubleMeta.asObjectable().getAssignment().get(0).asPrismContainerValue().getValueMetadata().getValues())
                .extracting(v -> ((ValueMetadataType) v.asContainerable()).getProvenance().getMappingSpecification().getMappingName())
                .containsExactlyInAnyOrder("foo");





    }
    // endregion

    // region insertion of various types

    // this test covers function of QObjectMapping and all the basic object fields
    @Test
    public void test800SystemConfigurationBasicObjectItems() throws Exception {
        OperationResult result = createOperationResult();

        given("system configuration");
        String objectName = "sc" + getTestNumber();
        UUID tenantRefOid = UUID.randomUUID();
        UUID creatorRefOid = UUID.randomUUID();
        UUID modifierRefOid = UUID.randomUUID();
        QName relation1 = QName.valueOf("{https://random.org/ns}random-rel-1");
        QName relation2 = QName.valueOf("{https://random.org/ns}random-rel-2");
        SystemConfigurationType systemConfiguration = new SystemConfigurationType()
                .name(objectName)
                .tenantRef(tenantRefOid.toString(), OrgType.COMPLEX_TYPE, relation1)
                .lifecycleState("lifecycle-state")
                .policySituation("policy-situation-1")
                .policySituation("policy-situation-2")
                .subtype("subtype-1")
                .subtype("subtype-2")
                .metadata(new MetadataType()
                        .creatorRef(creatorRefOid.toString(), UserType.COMPLEX_TYPE, relation1)
                        .createChannel("create-channel")
                        .createTimestamp(MiscUtil.asXMLGregorianCalendar(1L))
                        .modifierRef(modifierRefOid.toString(), UserType.COMPLEX_TYPE, relation2)
                        .modifyChannel("modify-channel")
                        .modifyTimestamp(MiscUtil.asXMLGregorianCalendar(2L)));

        when("adding it to the repository");
        repositoryService.addObject(systemConfiguration.asPrismObject(), null, result);

        then("it is stored and relevant items are in columns");
        assertThatOperationResult(result).isSuccess();

        MObject row = selectObjectByOid(
                QSystemConfiguration.class, systemConfiguration.getOid());
        display("FULL OBJECT: " + new String(row.fullObject, StandardCharsets.UTF_8));
        assertThat(row.nameOrig).isEqualTo(objectName);
        assertThat(row.nameNorm).isEqualTo(objectName); // nothing to normalize here
        assertThat(row.tenantRefTargetOid).isEqualTo(tenantRefOid);
        assertThat(row.tenantRefTargetType).isEqualTo(MObjectType.ORG);
        assertCachedUri(row.tenantRefRelationId, relation1);
        assertThat(row.lifecycleState).isEqualTo("lifecycle-state");
        // complex DB columns
        assertThat(resolveCachedUriIds(row.policySituations))
                .containsExactlyInAnyOrder("policy-situation-1", "policy-situation-2");
        assertThat(row.subtypes).containsExactlyInAnyOrder("subtype-1", "subtype-2");
        // metadata
        assertThat(row.creatorRefTargetOid).isEqualTo(creatorRefOid);
        assertThat(row.creatorRefTargetType).isEqualTo(MObjectType.USER);
        assertCachedUri(row.creatorRefRelationId, relation1);
        assertCachedUri(row.createChannelId, "create-channel");
        assertThat(row.createTimestamp).isEqualTo(Instant.ofEpochMilli(1));
        assertThat(row.modifierRefTargetOid).isEqualTo(modifierRefOid);
        assertThat(row.modifierRefTargetType).isEqualTo(MObjectType.USER);
        assertCachedUri(row.modifierRefRelationId, relation2);
        assertCachedUri(row.modifyChannelId, "modify-channel");
        assertThat(row.modifyTimestamp).isEqualTo(Instant.ofEpochMilli(2));
    }

    @Test
    public void test801ContainerTrigger() throws Exception {
        OperationResult result = createOperationResult();

        given("object with few triggers");
        String objectName = "object" + getTestNumber();
        SystemConfigurationType systemConfiguration = new SystemConfigurationType()
                .name(objectName)
                .trigger(new TriggerType()
                        .id(3L) // one pre-filled CID, with non-first ID
                        .handlerUri("trigger-1-handler-uri")
                        .timestamp(MiscUtil.asXMLGregorianCalendar(1L)))
                .trigger(new TriggerType() // second one without CID
                        .handlerUri("trigger-2-handler-uri")
                        .timestamp(MiscUtil.asXMLGregorianCalendar(2L)));

        when("adding it to the repository");
        repositoryService.addObject(systemConfiguration.asPrismObject(), null, result);

        then("it is stored with its persisted trigger containers");
        assertThatOperationResult(result).isSuccess();

        QTrigger<?> t = aliasFor(QTrigger.CLASS);
        List<MTrigger> containers = select(t, t.ownerOid.eq(UUID.fromString(systemConfiguration.getOid())));
        assertThat(containers).hasSize(2);

        containers.sort(comparing(tr -> tr.cid));
        MTrigger containerRow = containers.get(0);
        assertThat(containerRow.cid).isEqualTo(3); // assigned in advance
        assertCachedUri(containerRow.handlerUriId, "trigger-1-handler-uri");
        assertThat(containerRow.timestamp).isEqualTo(Instant.ofEpochMilli(1));

        containerRow = containers.get(1);
        assertThat(containerRow.cid).isEqualTo(4); // next CID assigned by repo
        assertCachedUri(containerRow.handlerUriId, "trigger-2-handler-uri");
        assertThat(containerRow.timestamp).isEqualTo(Instant.ofEpochMilli(2));

        MObject objectRow = selectObjectByOid(
                QSystemConfiguration.class, systemConfiguration.getOid());
        assertThat(objectRow.containerIdSeq).isEqualTo(5); // next free CID
    }

    @Test
    public void test802ContainerOperationExecution() throws Exception {
        OperationResult result = createOperationResult();

        given("object with few operation executions");
        String objectName = "object" + getTestNumber();
        UUID initiatorRefOid = UUID.randomUUID();
        UUID taskRefOid = UUID.randomUUID();
        QName initiatorRelation = QName.valueOf("{https://random.org/ns}rel-initiator");
        QName taskRelation = QName.valueOf("{https://random.org/ns}rel-task");
        SystemConfigurationType systemConfiguration = new SystemConfigurationType()
                .name(objectName)
                .operationExecution(new OperationExecutionType()
                        .status(OperationResultStatusType.FATAL_ERROR)
                        .recordType(OperationExecutionRecordTypeType.SIMPLE)
                        .initiatorRef(initiatorRefOid.toString(),
                                UserType.COMPLEX_TYPE, initiatorRelation)
                        .taskRef(taskRefOid.toString(), TaskType.COMPLEX_TYPE, taskRelation)
                        .timestamp(MiscUtil.asXMLGregorianCalendar(1L)))
                .operationExecution(new OperationExecutionType()
                        .status(OperationResultStatusType.UNKNOWN)
                        .timestamp(MiscUtil.asXMLGregorianCalendar(2L)));

        when("adding it to the repository");
        repositoryService.addObject(systemConfiguration.asPrismObject(), null, result);

        then("it is stored with its persisted trigger containers");
        assertThatOperationResult(result).isSuccess();

        QOperationExecution<?> oe = aliasFor(QOperationExecution.CLASS);
        List<MOperationExecution> containers =
                select(oe, oe.ownerOid.eq(UUID.fromString(systemConfiguration.getOid())));
        assertThat(containers).hasSize(2);

        containers.sort(comparing(tr -> tr.cid));
        MOperationExecution containerRow = containers.get(0);
        assertThat(containerRow.cid).isEqualTo(1);
        assertThat(containerRow.status).isEqualTo(OperationResultStatusType.FATAL_ERROR);
        assertThat(containerRow.recordType).isEqualTo(OperationExecutionRecordTypeType.SIMPLE);
        assertThat(containerRow.initiatorRefTargetOid).isEqualTo(initiatorRefOid);
        assertThat(containerRow.initiatorRefTargetType).isEqualTo(MObjectType.USER);
        assertCachedUri(containerRow.initiatorRefRelationId, initiatorRelation);
        assertThat(containerRow.taskRefTargetOid).isEqualTo(taskRefOid);
        assertThat(containerRow.taskRefTargetType).isEqualTo(MObjectType.TASK);
        assertCachedUri(containerRow.taskRefRelationId, taskRelation);
        assertThat(containerRow.timestamp).isEqualTo(Instant.ofEpochMilli(1));

        containerRow = containers.get(1);
        assertThat(containerRow.cid).isEqualTo(2);
        assertThat(containerRow.status).isEqualTo(OperationResultStatusType.UNKNOWN);
        assertThat(containerRow.timestamp).isEqualTo(Instant.ofEpochMilli(2));

        // this time we didn't test assigned CID or CID SEQ value on owner (see test801)
    }

    @Test
    public void test803ContainerAssignment() throws Exception {
        OperationResult result = createOperationResult();

        given("object with assignments");
        String objectName = "sc" + getTestNumber();
        UUID orgRefOid = UUID.randomUUID();
        UUID targetRefOid = UUID.randomUUID();
        UUID tenantRefOid = UUID.randomUUID();
        UUID resourceRefOid = UUID.randomUUID();
        UUID creatorRefOid = UUID.randomUUID();
        UUID modifierRefOid = UUID.randomUUID();
        QName relation1 = QName.valueOf("{https://random.org/ns}random-rel-1");
        QName relation2 = QName.valueOf("{https://random.org/ns}random-rel-2");
        SystemConfigurationType object = new SystemConfigurationType()
                .name(objectName)
                .assignment(new AssignmentType()
                        .lifecycleState("lifecycle-state")
                        .order(47)
                        .orgRef(orgRefOid.toString(), OrgType.COMPLEX_TYPE, relation1)
                        .targetRef(targetRefOid.toString(), RoleType.COMPLEX_TYPE, relation2)
                        .tenantRef(tenantRefOid.toString(), OrgType.COMPLEX_TYPE, relation2)
                        // TODO extId, extOid, ext?
                        .policySituation("policy-situation-1")
                        .policySituation("policy-situation-2")
                        .construction(new ConstructionType()
                                .resourceRef(resourceRefOid.toString(),
                                        ResourceType.COMPLEX_TYPE, relation1))
                        .activation(new ActivationType()
                                .administrativeStatus(ActivationStatusType.ENABLED)
                                .effectiveStatus(ActivationStatusType.DISABLED)
                                .enableTimestamp(MiscUtil.asXMLGregorianCalendar(3L))
                                .disableTimestamp(MiscUtil.asXMLGregorianCalendar(4L))
                                .disableReason("disable-reason")
                                .validityStatus(TimeIntervalStatusType.IN)
                                .validFrom(MiscUtil.asXMLGregorianCalendar(5L))
                                .validTo(MiscUtil.asXMLGregorianCalendar(6L))
                                .validityChangeTimestamp(MiscUtil.asXMLGregorianCalendar(7L))
                                .archiveTimestamp(MiscUtil.asXMLGregorianCalendar(8L)))
                        .metadata(new MetadataType()
                                // multi-value approver refs are tested elsewhere
                                .creatorRef(creatorRefOid.toString(), UserType.COMPLEX_TYPE, relation1)
                                .createChannel("create-channel")
                                .createTimestamp(MiscUtil.asXMLGregorianCalendar(1L))
                                .modifierRef(modifierRefOid.toString(), UserType.COMPLEX_TYPE, relation2)
                                .modifyChannel("modify-channel")
                                .modifyTimestamp(MiscUtil.asXMLGregorianCalendar(2L))))
                // one more just to see it stores multiple assignments
                .assignment(new AssignmentType().order(1));

        when("adding it to the repository");
        repositoryService.addObject(object.asPrismObject(), null, result);

        then("it is stored and rows to child tables are inserted");
        assertThatOperationResult(result).isSuccess();

        QAssignment<?> a = QAssignmentMapping.getAssignmentMapping().defaultAlias();
        List<MAssignment> aRows = select(a, a.ownerOid.eq(UUID.fromString(object.getOid())));
        assertThat(aRows).hasSize(2)
                .allMatch(ar -> ar.orderValue != null);

        MAssignment row = aRows.stream()
                .filter(ar -> ar.orderValue == 47)
                .findFirst().orElseThrow();

        assertThat(row.lifecycleState).isEqualTo("lifecycle-state");
        assertThat(row.orderValue).isEqualTo(47);
        assertThat(row.orgRefTargetOid).isEqualTo(orgRefOid);
        assertThat(row.orgRefTargetType).isEqualTo(MObjectType.ORG);
        assertCachedUri(row.orgRefRelationId, relation1);
        assertThat(row.targetRefTargetOid).isEqualTo(targetRefOid);
        assertThat(row.targetRefTargetType).isEqualTo(MObjectType.ROLE);
        assertCachedUri(row.targetRefRelationId, relation2);
        assertThat(row.tenantRefTargetOid).isEqualTo(tenantRefOid);
        assertThat(row.tenantRefTargetType).isEqualTo(MObjectType.ORG);
        assertCachedUri(row.tenantRefRelationId, relation2);
        // complex DB columns
        // TODO EXT
        assertThat(resolveCachedUriIds(row.policySituations))
                .containsExactlyInAnyOrder("policy-situation-1", "policy-situation-2");
        // construction
        assertThat(row.resourceRefTargetOid).isEqualTo(resourceRefOid);
        assertThat(row.resourceRefTargetType).isEqualTo(MObjectType.RESOURCE);
        assertCachedUri(row.resourceRefRelationId, relation1);
        // activation
        assertThat(row.administrativeStatus).isEqualTo(ActivationStatusType.ENABLED);
        assertThat(row.effectiveStatus).isEqualTo(ActivationStatusType.DISABLED);
        assertThat(row.enableTimestamp).isEqualTo(Instant.ofEpochMilli(3));
        assertThat(row.disableTimestamp).isEqualTo(Instant.ofEpochMilli(4));
        assertThat(row.disableReason).isEqualTo("disable-reason");
        assertThat(row.validityStatus).isEqualTo(TimeIntervalStatusType.IN);
        assertThat(row.validFrom).isEqualTo(Instant.ofEpochMilli(5));
        assertThat(row.validTo).isEqualTo(Instant.ofEpochMilli(6));
        assertThat(row.validityChangeTimestamp).isEqualTo(Instant.ofEpochMilli(7));
        assertThat(row.archiveTimestamp).isEqualTo(Instant.ofEpochMilli(8));
        // metadata
        assertThat(row.creatorRefTargetOid).isEqualTo(creatorRefOid);
        assertThat(row.creatorRefTargetType).isEqualTo(MObjectType.USER);
        assertCachedUri(row.creatorRefRelationId, relation1);
        assertCachedUri(row.createChannelId, "create-channel");
        assertThat(row.createTimestamp).isEqualTo(Instant.ofEpochMilli(1));
        assertThat(row.modifierRefTargetOid).isEqualTo(modifierRefOid);
        assertThat(row.modifierRefTargetType).isEqualTo(MObjectType.USER);
        assertCachedUri(row.modifierRefRelationId, relation2);
        assertCachedUri(row.modifyChannelId, "modify-channel");
        assertThat(row.modifyTimestamp).isEqualTo(Instant.ofEpochMilli(2));
    }

    @Test
    public void test808LookupTable() throws Exception {
        OperationResult result = createOperationResult();

        given("lookup table with a couple of rows");
        String objectName = "ltable" + getTestNumber();
        LookupTableType lookupTable = new LookupTableType()
                .name(objectName)
                .row(new LookupTableRowType()
                        .key("row1")
                        .value("value1")
                        .label("label-1")
                        .lastChangeTimestamp(MiscUtil.asXMLGregorianCalendar(1L)))
                .row(new LookupTableRowType()
                        .key("row2")
                        .value("value2")
                        .lastChangeTimestamp(MiscUtil.asXMLGregorianCalendar(2L)))
                .row(new LookupTableRowType()
                        .key("row3"));

        when("adding it to the repository");
        repositoryService.addObject(lookupTable.asPrismObject(), null, result);

        then("it is stored with its persisted trigger containers");
        assertThatOperationResult(result).isSuccess();

        QLookupTableRow ltRow = aliasFor(QLookupTableRow.class);
        List<MLookupTableRow> rows =
                select(ltRow, ltRow.ownerOid.eq(UUID.fromString(lookupTable.getOid())));
        assertThat(rows).hasSize(3);

        rows.sort(comparing(tr -> tr.cid));
        MLookupTableRow containerRow = rows.get(0);
        assertThat(containerRow.cid).isEqualTo(1);
        assertThat(containerRow.key).isEqualTo("row1");
        assertThat(containerRow.value).isEqualTo("value1");
        assertThat(containerRow.labelOrig).isEqualTo("label-1");
        assertThat(containerRow.labelNorm).isEqualTo("label1");
        assertThat(containerRow.lastChangeTimestamp).isEqualTo(Instant.ofEpochMilli(1));

        containerRow = rows.get(1);
        assertThat(containerRow.cid).isEqualTo(2);
        assertThat(containerRow.key).isEqualTo("row2");
        assertThat(containerRow.value).isEqualTo("value2");
        assertThat(containerRow.lastChangeTimestamp).isEqualTo(Instant.ofEpochMilli(2));

        containerRow = rows.get(2);
        assertThat(containerRow.cid).isEqualTo(3);
        assertThat(containerRow.key).isEqualTo("row3");
    }

    @Test
    public void test810ResourceAndItsBusinessApproverReferences() throws Exception {
        OperationResult result = createOperationResult();

        given("resource");
        String objectName = "res" + getTestNumber();
        UUID connectorOid = UUID.randomUUID();
        QName approver1Relation = QName.valueOf("{https://random.org/ns}random-rel-1");
        QName approver2Relation = QName.valueOf("{https://random.org/ns}random-rel-2");
        QName connectorRelation = QName.valueOf("{https://random.org/ns}conn-rel");
        ResourceType resource = new ResourceType()
                .name(objectName)
                .business(new ResourceBusinessConfigurationType()
                        .administrativeState(ResourceAdministrativeStateType.DISABLED)
                        .approverRef(UUID.randomUUID().toString(),
                                UserType.COMPLEX_TYPE, approver1Relation)
                        .approverRef(UUID.randomUUID().toString(),
                                ServiceType.COMPLEX_TYPE, approver2Relation))
                .operationalState(new OperationalStateType()
                        .lastAvailabilityStatus(AvailabilityStatusType.BROKEN))
                .administrativeOperationalState(new AdministrativeOperationalStateType()
                        .administrativeAvailabilityStatus(AdministrativeAvailabilityStatusType.MAINTENANCE))
                .connectorRef(connectorOid.toString(),
                        ConnectorType.COMPLEX_TYPE, connectorRelation)
                .template(false)
                ._abstract(false);

        when("adding it to the repository");
        repositoryService.addObject(resource.asPrismObject(), null, result);

        then("it is stored and relevant items are in columns");
        assertThatOperationResult(result).isSuccess();

        MResource row = selectObjectByOid(QResource.class, resource.getOid());
        assertThat(row.businessAdministrativeState)
                .isEqualTo(ResourceAdministrativeStateType.DISABLED);
        assertThat(row.operationalStateLastAvailabilityStatus)
                .isEqualTo(AvailabilityStatusType.BROKEN);
        assertThat(row.administrativeOperationalStateAdministrativeAvailabilityStatus)
                .isEqualTo(AdministrativeAvailabilityStatusType.MAINTENANCE);
        assertThat(row.connectorRefTargetOid).isEqualTo(connectorOid);
        assertThat(row.connectorRefTargetType).isEqualTo(MObjectType.CONNECTOR);
        assertCachedUri(row.connectorRefRelationId, connectorRelation);
        assertThat(row.template).isFalse();
        assertThat(row.abstractValue).isFalse();

        QObjectReference<?> ref = QObjectReferenceMapping
                .getForResourceBusinessConfigurationApprover().defaultAlias();
        List<MReference> refs = select(ref, ref.ownerOid.eq(row.oid));
        assertThat(refs).hasSize(2);

        refs.sort(comparing(rr -> rr.targetType));
        MReference refRow = refs.get(0);
        assertThat(refRow.referenceType)
                .isEqualTo(MReferenceType.RESOURCE_BUSINESS_CONFIGURATION_APPROVER);
        assertThat(refRow.targetType).isEqualTo(MObjectType.SERVICE);
        assertCachedUri(refRow.relationId, approver2Relation);
    }

    @Test
    public void test811Connector() throws Exception {
        OperationResult result = createOperationResult();

        given("connector");
        String objectName = "conn" + getTestNumber();
        UUID connectorHostOid = UUID.randomUUID();
        QName connectorHostRelation = QName.valueOf("{https://random.org/ns}conn-host-rel");
        ConnectorType connector = new ConnectorType()
                .name(objectName)
                .connectorBundle("com.connector.package")
                .connectorType("ConnectorTypeClass")
                .connectorVersion("1.2.3")
                .framework(SchemaConstants.UCF_FRAMEWORK_URI_BUILTIN)
                .connectorHostRef(connectorHostOid.toString(),
                        ConnectorHostType.COMPLEX_TYPE, connectorHostRelation)
                .targetSystemType("type1")
                .targetSystemType("type2")
                .available(true);

        when("adding it to the repository");
        repositoryService.addObject(connector.asPrismObject(), null, result);

        then("it is stored and relevant items are in columns");
        assertThatOperationResult(result).isSuccess();

        MConnector row = selectObjectByOid(QConnector.class, connector.getOid());
        assertThat(row.connectorBundle).isEqualTo("com.connector.package");
        assertThat(row.connectorType).isEqualTo("ConnectorTypeClass");
        assertThat(row.connectorVersion).isEqualTo("1.2.3");
        assertCachedUri(row.frameworkId, SchemaConstants.UCF_FRAMEWORK_URI_BUILTIN);
        assertThat(row.connectorHostRefTargetOid).isEqualTo(connectorHostOid);
        assertThat(row.connectorHostRefTargetType).isEqualTo(MObjectType.CONNECTOR_HOST);
        assertCachedUri(row.connectorHostRefRelationId, connectorHostRelation);
        assertThat(resolveCachedUriIds(row.targetSystemTypes))
                .containsExactlyInAnyOrder("type1", "type2");
        assertThat(row.available).isTrue();
    }

    @Test
    public void test812ConnectorWithNullConnectorHost() throws Exception {
        OperationResult result = createOperationResult();

        given("connector with no connector host reference");
        String objectName = "conn" + getTestNumber();
        ConnectorType connector = new ConnectorType()
                .name(objectName)
                .connectorBundle("com.connector.package")
                .connectorType("ConnectorTypeClass")
                .connectorVersion("1.2.3")
                .framework(SchemaConstants.UCF_FRAMEWORK_URI_BUILTIN);

        when("adding it to the repository");
        repositoryService.addObject(connector.asPrismObject(), null, result);

        then("it is stored and with null connection host reference");
        assertThatOperationResult(result).isSuccess();

        MConnector row = selectObjectByOid(QConnector.class, connector.getOid());
        assertThat(row.connectorHostRefTargetOid).isNull();
    }

    @Test
    public void test813NonUniqueConnector() {
        OperationResult result = createOperationResult();

        given("connector already existing in the repository");
        String objectName = "conn" + getTestNumber(); // name is unique, but that's not what we test
        ConnectorType connector = new ConnectorType()
                .name(objectName)
                .connectorBundle("com.connector.package")
                // We need unique connectorType + connectorVersion + connectorHostRef.oid (even if NULL)
                .connectorType("ConnectorTypeClass")
                .connectorVersion("1.2.3")
                .framework(SchemaConstants.UCF_FRAMEWORK_URI_BUILTIN);

        expect("adding it to the repository fails");
        assertThatThrownBy(() -> repositoryService.addObject(connector.asPrismObject(), null, result))
                .isInstanceOf(ObjectAlreadyExistsException.class);

        assertThatOperationResult(result).isFatalError()
                .hasMessageContaining("m_connector_typeversion_key");
    }

    @Test
    public void test814ConnectorHost() throws Exception {
        OperationResult result = createOperationResult();

        given("connector host");
        String objectName = "conn-host" + getTestNumber();
        ConnectorHostType connectorHost = new ConnectorHostType()
                .name(objectName)
                .hostname("hostname")
                .port("port");

        when("adding it to the repository");
        repositoryService.addObject(connectorHost.asPrismObject(), null, result);

        then("it is stored and relevant items are in columns");
        assertThatOperationResult(result).isSuccess();

        MConnectorHost row = selectObjectByOid(QConnectorHost.class, connectorHost.getOid());
        assertThat(row.hostname).isEqualTo("hostname");
        assertThat(row.port).isEqualTo("port");
    }

    @Test
    public void test815Report() throws Exception {
        OperationResult result = createOperationResult();

        given("report");
        String objectName = "report" + getTestNumber();
        ReportType report = new ReportType()
                .name(objectName);

        when("adding it to the repository");
        repositoryService.addObject(report.asPrismObject(), null, result);

        then("report is stored");
        assertThatOperationResult(result).isSuccess();

        selectObjectByOid(QReport.class, report.getOid());
    }

    @Test
    public void test816ReportData() throws Exception {
        OperationResult result = createOperationResult();

        given("report data");
        String objectName = "report-data" + getTestNumber();
        UUID reportOid = UUID.randomUUID();
        QName reportRelation = QName.valueOf("{https://random.org/ns}report-rel");
        ReportDataType report = new ReportDataType()
                .name(objectName)
                .reportRef(reportOid.toString(), ReportType.COMPLEX_TYPE, reportRelation);

        when("adding it to the repository");
        repositoryService.addObject(report.asPrismObject(), null, result);

        then("it is stored and relevant items are in columns");
        assertThatOperationResult(result).isSuccess();

        MReportData row = selectObjectByOid(QReportData.class, report.getOid());
        assertThat(row.reportRefTargetOid).isEqualTo(reportOid);
        assertThat(row.reportRefTargetType).isEqualTo(MObjectType.REPORT);
        assertCachedUri(row.reportRefRelationId, reportRelation);
    }

    @Test
    public void test818Shadow() throws Exception {
        OperationResult result = createOperationResult();

        given("shadow");
        String objectName = "shadow" + getTestNumber();
        QName objectClass = QName.valueOf("{https://random.org/ns}shadow-object-class");
        UUID resourceRefOid = UUID.randomUUID();
        QName resourceRefRelation = QName.valueOf("{https://random.org/ns}resource-ref-rel");
        ShadowType shadow = new ShadowType()
                .name(objectName)
                .objectClass(objectClass)
                .resourceRef(resourceRefOid.toString(),
                        ResourceType.COMPLEX_TYPE, resourceRefRelation)
                .intent("intent")
                .tag("tag")
                .kind(ShadowKindType.ACCOUNT)
                // TODO attemptNumber used at all?
                .dead(false)
                .exists(true)
                .fullSynchronizationTimestamp(MiscUtil.asXMLGregorianCalendar(1L))
                .pendingOperation(new PendingOperationType().attemptNumber(1))
                .pendingOperation(new PendingOperationType().attemptNumber(2))
                .primaryIdentifierValue("PID")
                .synchronizationSituation(SynchronizationSituationType.DISPUTED)
                .synchronizationTimestamp(MiscUtil.asXMLGregorianCalendar(2L))
                .correlation(new ShadowCorrelationStateType()
                        .correlationStartTimestamp(MiscUtil.asXMLGregorianCalendar(10L))
                        .correlationEndTimestamp(MiscUtil.asXMLGregorianCalendar(11L))
                        .correlationCaseOpenTimestamp(MiscUtil.asXMLGregorianCalendar(12L))
                        .correlationCaseCloseTimestamp(MiscUtil.asXMLGregorianCalendar(13L))
                        .situation(CorrelationSituationType.EXISTING_OWNER));

        when("adding it to the repository");
        repositoryService.addObject(shadow.asPrismObject(), null, result);

        then("it is stored and relevant items are in columns");
        assertThatOperationResult(result).isSuccess();

        MShadow row = selectObjectByOid(QShadow.class, shadow.getOid());
        assertCachedUri(row.objectClassId, objectClass);
        assertThat(row.resourceRefTargetOid).isEqualTo(resourceRefOid);
        assertThat(row.resourceRefTargetType).isEqualTo(MObjectType.RESOURCE);
        assertCachedUri(row.resourceRefRelationId, resourceRefRelation);
        assertThat(row.intent).isEqualTo("intent");
        assertThat(row.tag).isEqualTo("tag");
        assertThat(row.kind).isEqualTo(ShadowKindType.ACCOUNT);
        assertThat(row.dead).isEqualTo(false);
        assertThat(row.exist).isEqualTo(true);
        assertThat(row.fullSynchronizationTimestamp).isEqualTo(Instant.ofEpochMilli(1));
        assertThat(row.pendingOperationCount).isEqualTo(2);
        assertThat(row.primaryIdentifierValue).isEqualTo("PID");
        assertThat(row.synchronizationSituation).isEqualTo(SynchronizationSituationType.DISPUTED);
        assertThat(row.synchronizationTimestamp).isEqualTo(Instant.ofEpochMilli(2));
        assertThat(row.correlationStartTimestamp).isEqualTo(Instant.ofEpochMilli(10));
        assertThat(row.correlationEndTimestamp).isEqualTo(Instant.ofEpochMilli(11));
        assertThat(row.correlationCaseOpenTimestamp).isEqualTo(Instant.ofEpochMilli(12));
        assertThat(row.correlationCaseCloseTimestamp).isEqualTo(Instant.ofEpochMilli(13));
        assertThat(row.correlationSituation).isEqualTo(CorrelationSituationType.EXISTING_OWNER);
    }

    // This covers mapping of items in QFocusMapping + GenericObject.
    @Test
    public void test820GenericObject() throws Exception {
        OperationResult result = createOperationResult();

        given("generic object");
        String objectName = "go" + getTestNumber();
        GenericObjectType genericObject = new GenericObjectType()
                .name(objectName)
                .costCenter("cost-center")
                .emailAddress("email-address")
                .jpegPhoto(new byte[] { 1, 2, 3, 4, 5 })
                .locale("locale")
                .locality("locality")
                .preferredLanguage("preferred-language")
                .telephoneNumber("telephone-number")
                .timezone("timezone")
                .credentials(new CredentialsType()
                        .password(new PasswordType()
                                .metadata(new MetadataType()
                                        .createTimestamp(MiscUtil.asXMLGregorianCalendar(1L))
                                        .modifyTimestamp(MiscUtil.asXMLGregorianCalendar(2L)))))
                .activation(new ActivationType()
                        .administrativeStatus(ActivationStatusType.ENABLED)
                        .effectiveStatus(ActivationStatusType.DISABLED)
                        .enableTimestamp(MiscUtil.asXMLGregorianCalendar(3L))
                        .disableTimestamp(MiscUtil.asXMLGregorianCalendar(4L))
                        .disableReason("disable-reason")
                        .validityStatus(TimeIntervalStatusType.IN)
                        .validFrom(MiscUtil.asXMLGregorianCalendar(5L))
                        .validTo(MiscUtil.asXMLGregorianCalendar(6L))
                        .validityChangeTimestamp(MiscUtil.asXMLGregorianCalendar(7L))
                        .archiveTimestamp(MiscUtil.asXMLGregorianCalendar(8L))
                        .lockoutStatus(LockoutStatusType.NORMAL))
                // this is the only additionally persisted field for GenericObject
                .subtype("some-custom-object-type-uri");

        when("adding it to the repository");
        repositoryService.addObject(genericObject.asPrismObject(), null, result);

        then("it is stored and relevant items are in columns");
        assertThatOperationResult(result).isSuccess();

        MFocus row = selectObjectByOid(
                QGenericObject.class, UUID.fromString(genericObject.getOid()));
        assertThat(row.costCenter).isEqualTo("cost-center");
        assertThat(row.emailAddress).isEqualTo("email-address");
        assertThat(row.photo).isEqualTo(new byte[] { 1, 2, 3, 4, 5 });
        assertThat(row.locale).isEqualTo("locale");
        assertThat(row.localityOrig).isEqualTo("locality");
        assertThat(row.localityNorm).isEqualTo("locality");
        assertThat(row.preferredLanguage).isEqualTo("preferred-language");
        assertThat(row.telephoneNumber).isEqualTo("telephone-number");
        assertThat(row.timezone).isEqualTo("timezone");

        assertThat(row.passwordCreateTimestamp).isEqualTo(Instant.ofEpochMilli(1));
        assertThat(row.passwordModifyTimestamp).isEqualTo(Instant.ofEpochMilli(2));

        assertThat(row.administrativeStatus).isEqualTo(ActivationStatusType.ENABLED);
        assertThat(row.effectiveStatus).isEqualTo(ActivationStatusType.DISABLED);
        assertThat(row.enableTimestamp).isEqualTo(Instant.ofEpochMilli(3));
        assertThat(row.disableTimestamp).isEqualTo(Instant.ofEpochMilli(4));
        assertThat(row.disableReason).isEqualTo("disable-reason");
        assertThat(row.validityStatus).isEqualTo(TimeIntervalStatusType.IN);
        assertThat(row.validFrom).isEqualTo(Instant.ofEpochMilli(5));
        assertThat(row.validTo).isEqualTo(Instant.ofEpochMilli(6));
        assertThat(row.validityChangeTimestamp).isEqualTo(Instant.ofEpochMilli(7));
        assertThat(row.archiveTimestamp).isEqualTo(Instant.ofEpochMilli(8));
        assertThat(row.lockoutStatus).isEqualTo(LockoutStatusType.NORMAL);

        assertThat(row.subtypes).containsExactlyInAnyOrder("some-custom-object-type-uri");
    }

    // This covers mapping of items in AbstractRole + Archetype + inducement mapping.
    // There is no focus on QFocusMapping that is covered above.
    @Test
    public void test821ArchetypeAndInducement() throws Exception {
        OperationResult result = createOperationResult();

        given("archetype object");
        String objectName = "arch" + getTestNumber();
        ArchetypeType archetype = new ArchetypeType()
                .name(objectName)
                .autoassign(new AutoassignSpecificationType().enabled(true))
                .displayName("display-name")
                .identifier("identifier")
                .requestable(false)
                .riskLevel("extremely-high")
                // we don't need all items here, this is tested in test803ContainerAssignment
                .inducement(new AssignmentType()
                        .order(2)
                        .targetRef(UUID.randomUUID().toString(), RoleType.COMPLEX_TYPE))
                .inducement(new AssignmentType()
                        .order(3)
                        .targetRef(UUID.randomUUID().toString(), RoleType.COMPLEX_TYPE));
        // this is no additional items specific for archetype

        when("adding it to the repository");
        repositoryService.addObject(archetype.asPrismObject(), null, result);

        then("it is stored and relevant items are in columns");
        assertThatOperationResult(result).isSuccess();

        UUID archetypeOid = UUID.fromString(archetype.getOid());
        MArchetype row = selectObjectByOid(QArchetype.class, archetypeOid);
        // all items from MAbstractRole
        assertThat(row.autoAssignEnabled).isTrue();
        assertThat(row.displayNameOrig).isEqualTo("display-name");
        assertThat(row.displayNameNorm).isEqualTo("displayname");
        assertThat(row.identifier).isEqualTo("identifier");
        assertThat(row.requestable).isFalse();
        assertThat(row.riskLevel).isEqualTo("extremely-high");

        QAssignment<?> a = QAssignmentMapping.getAssignmentMapping().defaultAlias();
        assertThat(select(a, a.ownerOid.eq(archetypeOid))).hasSize(2)
                .anyMatch(ar -> ar.orderValue.equals(2))
                .anyMatch(ar -> ar.orderValue.equals(3))
                .allMatch(ar -> ar.targetRefTargetOid != null
                        && ar.targetRefTargetType == MObjectType.ROLE);
    }

    @Test
    public void test825User() throws Exception {
        OperationResult result = createOperationResult();

        given("user object");
        String objectName = "user" + getTestNumber();
        UserType user = new UserType()
                .name(objectName)
                .additionalName("additional-name")
                .employeeNumber("3")
                .familyName("family-name")
                .fullName("full-name")
                .givenName("given-name")
                .honorificPrefix("honorific-prefix")
                .honorificSuffix("honorific-suffix")
                .nickName("nick-name")
                .title("title")
                .organization("org-1")
                .organization("org-2")
                .organizationalUnit("ou-1")
                .organizationalUnit("ou-2");

        when("adding it to the repository");
        repositoryService.addObject(user.asPrismObject(), null, result);

        then("it is stored and relevant items are in columns");
        assertThatOperationResult(result).isSuccess();

        UUID userOid = UUID.fromString(user.getOid());
        MUser row = selectObjectByOid(QUser.class, userOid);
        // all items from MUser
        assertThat(row.additionalNameOrig).isEqualTo("additional-name");
        assertThat(row.additionalNameNorm).isEqualTo("additionalname");
        assertThat(row.employeeNumber).isEqualTo("3");
        assertThat(row.familyNameOrig).isEqualTo("family-name");
        assertThat(row.familyNameNorm).isEqualTo("familyname");
        assertThat(row.fullNameOrig).isEqualTo("full-name");
        assertThat(row.fullNameNorm).isEqualTo("fullname");
        assertThat(row.givenNameOrig).isEqualTo("given-name");
        assertThat(row.givenNameNorm).isEqualTo("givenname");
        assertThat(row.honorificPrefixOrig).isEqualTo("honorific-prefix");
        assertThat(row.honorificPrefixNorm).isEqualTo("honorificprefix");
        assertThat(row.honorificSuffixOrig).isEqualTo("honorific-suffix");
        assertThat(row.honorificSuffixNorm).isEqualTo("honorificsuffix");
        assertThat(row.nickNameOrig).isEqualTo("nick-name");
        assertThat(row.nickNameNorm).isEqualTo("nickname");
        assertThat(row.titleOrig).isEqualTo("title");
        assertThat(row.titleNorm).isEqualTo("title");
        assertThat(row.organizations).isNotNull();
        assertThat(Jsonb.toList(row.organizations)).containsExactlyInAnyOrder(
                Map.of("o", "org-1", "n", "org1"),
                Map.of("o", "org-2", "n", "org2"));
        assertThat(row.organizationUnits).isNotNull();
        assertThat(Jsonb.toList(row.organizationUnits)).containsExactlyInAnyOrder(
                Map.of("o", "ou-1", "n", "ou1"),
                Map.of("o", "ou-2", "n", "ou2"));
    }

    // TODO test for focus' related entities?

    @Test
    public void test830Task() throws Exception {
        OperationResult result = createOperationResult();

        given("task");
        String objectName = "task" + getTestNumber();
        UUID objectRefOid = UUID.randomUUID();
        UUID ownerRefOid = UUID.randomUUID();
        QName relationUri = QName.valueOf("{https://some.uri}someRelation");
        var task = new TaskType()
                .name(objectName)
                .taskIdentifier("task-id")
                .binding(TaskBindingType.LOOSE)
                .completionTimestamp(MiscUtil.asXMLGregorianCalendar(1L))
                .executionState(TaskExecutionStateType.RUNNABLE)
                .result(new OperationResultType()
                        .message("result message")
                        .status(OperationResultStatusType.FATAL_ERROR))
                .handlerUri("handler-uri")
                .lastRunStartTimestamp(MiscUtil.asXMLGregorianCalendar(1L))
                .lastRunFinishTimestamp(MiscUtil.asXMLGregorianCalendar(2L))
                .node("node")
                .objectRef(objectRefOid.toString(), OrgType.COMPLEX_TYPE, relationUri)
                .ownerRef(ownerRefOid.toString(), UserType.COMPLEX_TYPE, relationUri)
                .parent("parent")
                .schedule(new ScheduleType()
                        .recurrence(TaskRecurrenceType.RECURRING))
                .resultStatus(OperationResultStatusType.UNKNOWN)
                .schedulingState(TaskSchedulingStateType.READY)
                .autoScaling(new TaskAutoScalingType()
                        .mode(TaskAutoScalingModeType.DEFAULT))
                .threadStopAction(ThreadStopActionType.RESCHEDULE)
                .waitingReason(TaskWaitingReasonType.OTHER_TASKS)
                .dependent("dep-task-1")
                .dependent("dep-task-2");

        when("adding it to the repository");
        repositoryService.addObject(task.asPrismObject(), null, result);

        then("it is stored and relevant items are in columns");
        assertThatOperationResult(result).isSuccess();

        MTask row = selectObjectByOid(QTask.class, task.getOid());
        assertThat(row.taskIdentifier).isEqualTo("task-id");
        assertThat(row.binding).isEqualTo(TaskBindingType.LOOSE);
        assertThat(row.completionTimestamp).isEqualTo(Instant.ofEpochMilli(1));
        assertThat(row.executionState).isEqualTo(TaskExecutionStateType.RUNNABLE);
        assertThat(row.fullResult).isNotNull();
        assertThat(row.resultStatus).isEqualTo(OperationResultStatusType.UNKNOWN);
        assertCachedUri(row.handlerUriId, "handler-uri");
        assertThat(row.lastRunStartTimestamp).isEqualTo(Instant.ofEpochMilli(1));
        assertThat(row.lastRunFinishTimestamp).isEqualTo(Instant.ofEpochMilli(2));
        assertThat(row.node).isEqualTo("node");
        assertThat(row.objectRefTargetOid).isEqualTo(objectRefOid);
        assertThat(row.objectRefTargetType).isEqualTo(MObjectType.ORG);
        assertCachedUri(row.objectRefRelationId, relationUri);
        assertThat(row.ownerRefTargetOid).isEqualTo(ownerRefOid);
        assertThat(row.ownerRefTargetType).isEqualTo(MObjectType.USER);
        assertCachedUri(row.ownerRefRelationId, relationUri);
        assertThat(row.parent).isEqualTo("parent");
        assertThat(row.recurrence).isEqualTo(TaskRecurrenceType.RECURRING);
        assertThat(row.schedulingState).isEqualTo(TaskSchedulingStateType.READY);
        assertThat(row.autoScalingMode).isEqualTo(TaskAutoScalingModeType.DEFAULT);
        assertThat(row.threadStopAction).isEqualTo(ThreadStopActionType.RESCHEDULE);
        assertThat(row.waitingReason).isEqualTo(TaskWaitingReasonType.OTHER_TASKS);
        assertThat(row.dependentTaskIdentifiers)
                .containsExactlyInAnyOrder("dep-task-1", "dep-task-2");

        and("stored full object does not contain operation result");
        TaskType parsedTask = parseFullObject(row.fullObject);
        assertThat(parsedTask.getResult()).isNull();
    }

    @Test
    public void test838Node() throws Exception {
        OperationResult result = createOperationResult();

        given("node");
        String objectName = "node" + getTestNumber();
        var node = new NodeType()
                .name(objectName)
                .nodeIdentifier("node-47")
                .operationalState(NodeOperationalStateType.STARTING);

        when("adding it to the repository");
        repositoryService.addObject(node.asPrismObject(), null, result);

        then("it is stored and relevant items are in columns");
        assertThatOperationResult(result).isSuccess();

        MNode row = selectObjectByOid(QNode.class, node.getOid());
        assertThat(row.nodeIdentifier).isEqualTo("node-47");
        assertThat(row.operationalState).isEqualTo(NodeOperationalStateType.STARTING);
    }

    @Test
    public void test840AccessCertificationDefinition() throws Exception {
        OperationResult result = createOperationResult();

        given("access certification definition");
        String objectName = "acd" + getTestNumber();
        UUID ownerRefOid = UUID.randomUUID();
        Instant lastCampaignStarted = Instant.ofEpochMilli(1); // 0 means null in MiscUtil
        Instant lastCampaignClosed = Instant.ofEpochMilli(System.currentTimeMillis());
        QName relationUri = QName.valueOf("{https://some.uri}specialRelation");
        var accessCertificationDefinition = new AccessCertificationDefinitionType()
                .name(objectName)
                .handlerUri("d-handler-uri")
                .lastCampaignStartedTimestamp(MiscUtil.asXMLGregorianCalendar(lastCampaignStarted))
                .lastCampaignClosedTimestamp(MiscUtil.asXMLGregorianCalendar(lastCampaignClosed))
                .ownerRef(ownerRefOid.toString(), UserType.COMPLEX_TYPE, relationUri);

        when("adding it to the repository");
        repositoryService.addObject(accessCertificationDefinition.asPrismObject(), null, result);

        then("it is stored and relevant items are in columns");
        assertThatOperationResult(result).isSuccess();

        MAccessCertificationDefinition row = selectObjectByOid(
                QAccessCertificationDefinition.class, accessCertificationDefinition.getOid());
        assertCachedUri(row.handlerUriId, "d-handler-uri");
        assertThat(row.lastCampaignStartedTimestamp).isEqualTo(lastCampaignStarted);
        assertThat(row.lastCampaignClosedTimestamp).isEqualTo(lastCampaignClosed);
        assertThat(row.ownerRefTargetOid).isEqualTo(ownerRefOid);
        assertThat(row.ownerRefTargetType).isEqualTo(MObjectType.USER);
        assertCachedUri(row.ownerRefRelationId, relationUri);
    }

    @Test
    public void test842AccessCertificationCampaign() throws Exception {
        OperationResult result = createOperationResult();

        given("access certification campaign");
        String objectName = "acc" + getTestNumber();

        UUID definitionRefOid = UUID.randomUUID();
        QName definitionRefRelationUri = QName.valueOf("{https://some.uri}definition-relation");
        UUID ownerRefOid = UUID.randomUUID();
        QName ownerRefRelationUri = QName.valueOf("{https://strange.uri}owner-relation");

        Instant startTimestamp = Instant.ofEpochMilli(1234);
        Instant validFrom = Instant.ofEpochMilli(444000);
        Instant validityChangeTimestamp = Instant.ofEpochMilli(444001);
        Instant case1CurrentStageCreateTimestamp = Instant.ofEpochMilli(444333);
        Instant case2CurrentStageCreateTimestamp = Instant.ofEpochMilli(2444333);
        Instant remediedTimestamp = Instant.ofEpochMilli(444555);
        Instant currentStageDeadline = Instant.ofEpochMilli(444666);
        Instant validTo = Instant.ofEpochMilli(999000);
        Instant enableTimestamp = Instant.ofEpochMilli(555000);
        Instant disableTimestamp = Instant.ofEpochMilli(555111);
        Instant archiveTimestamp = Instant.ofEpochMilli(555123);
        Instant endTimestamp = Instant.ofEpochMilli(System.currentTimeMillis());
        String disableReason = "Whatever!";
        String case1CurrentStageOutcome = "Bada BOOM";
        String case2CurrentStageOutcome = "Big bada BOOM";

        Integer case1Iteration = 5;
        Integer case2Iteration = 15;
        UUID caseObjectRefOid = UUID.randomUUID();
        QName caseObjectRefRelationUri = QName.valueOf("{https://other.uri}case-object-ref-relation");
        UUID caseOrgRefOid = UUID.randomUUID();
        QName caseOrgRefRelationUri = QName.valueOf("{https://other.uri}case-org-ref-relation");
        String caseOutcome = "... for ever and ever";
        int caseStageNumber = 8;
        UUID caseTargetRefOid = UUID.randomUUID();
        QName caseTargetRefRelationUri = QName.valueOf("{https://some.uri}case-target-ref-relation");
        UUID caseTenantRefOid = UUID.randomUUID();
        QName caseTenantRefRelationUri = QName.valueOf("{https://some.uri}case-tenant-ref-relation");

        Instant wi1CloseTimestamp = Instant.ofEpochMilli(999123);
        Instant wi2CloseTimestamp = Instant.ofEpochMilli(2999123);
        Instant wi3CloseTimestamp = Instant.ofEpochMilli(3999123);
        Instant wi1OutputChangeTimestamp = Instant.ofEpochMilli(999001);
        Instant wi2OutputChangeTimestamp = Instant.ofEpochMilli(2999001);
        Instant wi3OutputChangeTimestamp = Instant.ofEpochMilli(3999001);
        UUID performer1Oid = UUID.randomUUID();
        QName performer1Relation = QName.valueOf("{https://random.org/ns}wi-performer1-rel");
        UUID performer2Oid = UUID.randomUUID();
        QName performer2Relation = QName.valueOf("{https://random.org/ns}wi-performer2-rel");
        UUID performer3Oid = UUID.randomUUID();
        QName performer3Relation = QName.valueOf("{https://random.org/ns}wi-performer3-rel");

        UUID wi1AssigneeRef1Oid = UUID.fromString("7508a482-d8d1-11eb-9435-9b077bcc684b"); // explicit UUID, to ensure ordering
        QName wi1AssigneeRef1Relation = QName.valueOf("{https://random.org/ns}acwi-1-assignee-1-rel");
        UUID wi1AssigneeRef2Oid = UUID.fromString("77267d0c-d8d1-11eb-9ae9-8fb4530e4e64"); // explicit UUID, to ensure ordering
        QName wi1AssigneeRef2Relation = QName.valueOf("{https://random.org/ns}acwi-1-assignee-2-rel");
        UUID wi1CandidateRef1Oid = UUID.fromString("754f391a-d8d1-11eb-b269-9b322a63128e"); // explicit UUID, to ensure ordering
        QName wi1CandidateRef1Relation = QName.valueOf("{https://random.org/ns}acwi-1-candidate-1-rel");

        UUID wi2AssigneeRef1Oid = UUID.fromString("75cde882-d8d1-11eb-841f-6b8c0f5075f8"); // explicit UUID, to ensure ordering
        QName wi2AssigneeRef1Relation = QName.valueOf("{https://random.org/ns}acwi-2-assignee-1-rel");
        UUID wi2CandidateRef1Oid = UUID.fromString("765709aa-d8d1-11eb-9340-33ef5ec6b0e2"); // explicit UUID, to ensure ordering
        QName wi2CandidateRef1Relation = QName.valueOf("{https://random.org/ns}acwi-2-candidate-1-rel");
        UUID wi2CandidateRef2Oid = UUID.fromString("7576bd0a-d8d1-11eb-9171-bf2238c132b7"); // explicit UUID, to ensure ordering
        QName wi2CandidateRef2Relation = QName.valueOf("{https://random.org/ns}acwi-2-candidate-2-rel");

        UUID wi3AssigneeRef1Oid = UUID.fromString("b9f969b0-d8e4-11eb-aa14-6f0a7aff9550"); // explicit UUID, to ensure ordering
        QName wi3AssigneeRef1Relation = QName.valueOf("{https://random.org/ns}acwi-3-assignee-1-rel");
        UUID wi3CandidateRef1Oid = UUID.fromString("bb23b638-d8e4-11eb-9601-872386b50ce3"); // explicit UUID, to ensure ordering
        QName wi3CandidateRef1Relation = QName.valueOf("{https://random.org/ns}acwi-3-candidate-1-rel");

        var accessCertificationCampaign = new AccessCertificationCampaignType()
                .name(objectName)
                .definitionRef(definitionRefOid.toString(), UserType.COMPLEX_TYPE, definitionRefRelationUri)
                .endTimestamp(MiscUtil.asXMLGregorianCalendar(endTimestamp))
                .handlerUri("c-handler-uri")
                // TODO campaignIteration
                .iteration(3)
                .ownerRef(ownerRefOid.toString(), UserType.COMPLEX_TYPE, ownerRefRelationUri)
                .stageNumber(2)
                .startTimestamp(MiscUtil.asXMLGregorianCalendar(startTimestamp))
                .state(AccessCertificationCampaignStateType.IN_REVIEW_STAGE)
                ._case(new AccessCertificationCaseType()
                        .id(48L)
                        .activation(new ActivationType()
                                .administrativeStatus(ActivationStatusType.ARCHIVED)
                                .archiveTimestamp(MiscUtil.asXMLGregorianCalendar(archiveTimestamp))
                                .disableReason(disableReason)
                                .disableTimestamp(MiscUtil.asXMLGregorianCalendar(disableTimestamp))
                                .effectiveStatus(ActivationStatusType.DISABLED)
                                .enableTimestamp(MiscUtil.asXMLGregorianCalendar(enableTimestamp))
                                .validFrom(MiscUtil.asXMLGregorianCalendar(validFrom))
                                .validTo(MiscUtil.asXMLGregorianCalendar(validTo))
                                .validityChangeTimestamp(MiscUtil.asXMLGregorianCalendar(validityChangeTimestamp))
                                .validityStatus(TimeIntervalStatusType.IN))
                        .currentStageOutcome(case1CurrentStageOutcome)
                        // TODO campaignIteration
                        .iteration(case1Iteration)
                        .objectRef(caseObjectRefOid.toString(), ServiceType.COMPLEX_TYPE, caseObjectRefRelationUri)
                        .orgRef(caseOrgRefOid.toString(), OrgType.COMPLEX_TYPE, caseOrgRefRelationUri)
                        .outcome(caseOutcome)
                        .remediedTimestamp(MiscUtil.asXMLGregorianCalendar(remediedTimestamp))
                        .currentStageDeadline(MiscUtil.asXMLGregorianCalendar(currentStageDeadline))
                        .currentStageCreateTimestamp(MiscUtil.asXMLGregorianCalendar(case1CurrentStageCreateTimestamp))
                        .stageNumber(caseStageNumber)
                        .targetRef(caseTargetRefOid.toString(), RoleType.COMPLEX_TYPE, caseTargetRefRelationUri)
                        .tenantRef(caseTenantRefOid.toString(), OrgType.COMPLEX_TYPE, caseTenantRefRelationUri)
                        .workItem(new AccessCertificationWorkItemType()
                                .id(55L)
                                .closeTimestamp(MiscUtil.asXMLGregorianCalendar(wi1CloseTimestamp))
                                // TODO: iteration -> campaignIteration
                                .iteration(81)
                                .output(new AbstractWorkItemOutputType()
                                        .outcome("almost, but not quite, entirely done"))
                                .outputChangeTimestamp(MiscUtil.asXMLGregorianCalendar(wi1OutputChangeTimestamp))
                                .performerRef(performer1Oid.toString(), UserType.COMPLEX_TYPE, performer1Relation)
                                .stageNumber(21)
                                .assigneeRef(wi1AssigneeRef1Oid.toString(), UserType.COMPLEX_TYPE, wi1AssigneeRef1Relation)
                                .assigneeRef(wi1AssigneeRef2Oid.toString(), UserType.COMPLEX_TYPE, wi1AssigneeRef2Relation)
                                .candidateRef(wi1CandidateRef1Oid.toString(), UserType.COMPLEX_TYPE, wi1CandidateRef1Relation))
                        .workItem(new AccessCertificationWorkItemType()
                                .id(56L)
                                .closeTimestamp(MiscUtil.asXMLGregorianCalendar(wi2CloseTimestamp))
                                // TODO: iteration -> campaignIteration
                                .iteration(82)
                                .output(new AbstractWorkItemOutputType()
                                        .outcome("A tad more than almost, but not quite, entirely done"))
                                .outputChangeTimestamp(MiscUtil.asXMLGregorianCalendar(wi2OutputChangeTimestamp))
                                .performerRef(performer2Oid.toString(), UserType.COMPLEX_TYPE, performer2Relation)
                                .stageNumber(22)
                                .assigneeRef(wi2AssigneeRef1Oid.toString(), UserType.COMPLEX_TYPE, wi2AssigneeRef1Relation)
                                .candidateRef(wi2CandidateRef1Oid.toString(), UserType.COMPLEX_TYPE, wi2CandidateRef1Relation)
                                .candidateRef(wi2CandidateRef2Oid.toString(), OrgType.COMPLEX_TYPE, wi2CandidateRef2Relation)))
                ._case(new AccessCertificationCaseType()
                        .id(49L)
                        .currentStageOutcome(case2CurrentStageOutcome)
                        // TODO campaignIteration
                        .iteration(case2Iteration)
                        .outcome("whatever")
                        .currentStageCreateTimestamp(MiscUtil.asXMLGregorianCalendar(case2CurrentStageCreateTimestamp))
                        .stageNumber(25)
                        .targetRef(caseTargetRefOid.toString(), RoleType.COMPLEX_TYPE, caseTargetRefRelationUri)
                        .workItem(new AccessCertificationWorkItemType()
                                .id(59L)
                                .closeTimestamp(MiscUtil.asXMLGregorianCalendar(wi3CloseTimestamp))
                                // TODO: iteration -> campaignIteration
                                .iteration(83)
                                .output(new AbstractWorkItemOutputType()
                                        .outcome("certainly not quite done"))
                                .outputChangeTimestamp(MiscUtil.asXMLGregorianCalendar(wi3OutputChangeTimestamp))
                                .performerRef(performer3Oid.toString(), UserType.COMPLEX_TYPE, performer3Relation)
                                .stageNumber(21)
                                .assigneeRef(wi3AssigneeRef1Oid.toString(), UserType.COMPLEX_TYPE, wi3AssigneeRef1Relation)
                                .candidateRef(wi3CandidateRef1Oid.toString(), OrgType.COMPLEX_TYPE, wi3CandidateRef1Relation)));

        when("adding it to the repository");
        repositoryService.addObject(accessCertificationCampaign.asPrismObject(), null, result);

        then("it is stored and relevant items are in columns");
        assertThatOperationResult(result).isSuccess();

        MAccessCertificationCampaign row = selectObjectByOid(
                QAccessCertificationCampaign.class, accessCertificationCampaign.getOid());
        assertThat(row.definitionRefTargetOid).isEqualTo(definitionRefOid);
        assertThat(row.definitionRefTargetType).isEqualTo(MObjectType.USER);
        assertCachedUri(row.definitionRefRelationId, definitionRefRelationUri);
        assertThat(row.endTimestamp).isEqualTo(endTimestamp);
        assertCachedUri(row.handlerUriId, "c-handler-uri");
        assertThat(row.campaignIteration).isEqualTo(3);
        assertThat(row.ownerRefTargetOid).isEqualTo(ownerRefOid);
        assertThat(row.ownerRefTargetType).isEqualTo(MObjectType.USER);
        assertCachedUri(row.ownerRefRelationId, ownerRefRelationUri);
        assertThat(row.stageNumber).isEqualTo(2);
        assertThat(row.startTimestamp).isEqualTo(startTimestamp);
        assertThat(row.state).isEqualTo(AccessCertificationCampaignStateType.IN_REVIEW_STAGE);

        QAccessCertificationCase caseAlias = aliasFor(QAccessCertificationCase.class);
        List<MAccessCertificationCase> caseRows = select(caseAlias,
                caseAlias.ownerOid.eq(UUID.fromString(accessCertificationCampaign.getOid())));
        assertThat(caseRows).hasSize(2);
        caseRows.sort(comparing(tr -> tr.cid));

        MAccessCertificationCase caseRow = caseRows.get(0);
        assertThat(caseRow.cid).isEqualTo(48); // assigned in advance
        assertThat(caseRow.containerType).isEqualTo(MContainerType.ACCESS_CERTIFICATION_CASE);
        assertThat(caseRow.administrativeStatus).isEqualTo(ActivationStatusType.ARCHIVED);
        assertThat(caseRow.archiveTimestamp).isEqualTo(archiveTimestamp);
        assertThat(caseRow.disableReason).isEqualTo(disableReason);
        assertThat(caseRow.disableTimestamp).isEqualTo(disableTimestamp);
        assertThat(caseRow.effectiveStatus).isEqualTo(ActivationStatusType.DISABLED);
        assertThat(caseRow.enableTimestamp).isEqualTo(enableTimestamp);
        assertThat(caseRow.validFrom).isEqualTo(validFrom);
        assertThat(caseRow.validTo).isEqualTo(validTo);
        assertThat(caseRow.validityChangeTimestamp).isEqualTo(validityChangeTimestamp);
        assertThat(caseRow.validityStatus).isEqualTo(TimeIntervalStatusType.IN);
        assertThat(caseRow.currentStageOutcome).isEqualTo(case1CurrentStageOutcome);
        assertContainerFullObject(caseRow.fullObject, accessCertificationCampaign.getCase().get(0));
        assertThat(caseRow.campaignIteration).isEqualTo(case1Iteration);
        assertThat(caseRow.objectRefTargetOid).isEqualTo(caseObjectRefOid);
        assertThat(caseRow.objectRefTargetType).isEqualTo(MObjectType.SERVICE);
        assertCachedUri(caseRow.objectRefRelationId, caseObjectRefRelationUri);
        assertThat(caseRow.orgRefTargetOid).isEqualTo(caseOrgRefOid);
        assertThat(caseRow.orgRefTargetType).isEqualTo(MObjectType.ORG);
        assertCachedUri(caseRow.orgRefRelationId, caseOrgRefRelationUri);
        assertThat(caseRow.outcome).isEqualTo(caseOutcome);
        assertThat(caseRow.remediedTimestamp).isEqualTo(remediedTimestamp);
        assertThat(caseRow.currentStageDeadline).isEqualTo(currentStageDeadline);
        assertThat(caseRow.currentStageCreateTimestamp).isEqualTo(case1CurrentStageCreateTimestamp);
        assertThat(caseRow.stageNumber).isEqualTo(caseStageNumber);
        assertThat(caseRow.targetRefTargetOid).isEqualTo(caseTargetRefOid);
        assertThat(caseRow.targetRefTargetType).isEqualTo(MObjectType.ROLE);
        assertCachedUri(caseRow.targetRefRelationId, caseTargetRefRelationUri);
        assertThat(caseRow.tenantRefTargetOid).isEqualTo(caseTenantRefOid);
        assertThat(caseRow.tenantRefTargetType).isEqualTo(MObjectType.ORG);
        assertCachedUri(caseRow.tenantRefRelationId, caseTenantRefRelationUri);

        caseRow = caseRows.get(1);
        assertThat(caseRow.cid).isEqualTo(49); // assigned in advance
        assertThat(caseRow.containerType).isEqualTo(MContainerType.ACCESS_CERTIFICATION_CASE);
        assertThat(caseRow.administrativeStatus).isNull();
        assertThat(caseRow.archiveTimestamp).isNull();
        assertThat(caseRow.disableReason).isNull();
        assertThat(caseRow.disableTimestamp).isNull();
        assertThat(caseRow.effectiveStatus).isNull();
        assertThat(caseRow.enableTimestamp).isNull();
        assertThat(caseRow.validFrom).isNull();
        assertThat(caseRow.validTo).isNull();
        assertThat(caseRow.validityChangeTimestamp).isNull();
        assertThat(caseRow.validityStatus).isNull();
        assertThat(caseRow.currentStageOutcome).isEqualTo(case2CurrentStageOutcome);
        assertContainerFullObject(caseRow.fullObject, accessCertificationCampaign.getCase().get(1));
        assertThat(caseRow.campaignIteration).isEqualTo(case2Iteration);
        assertThat(caseRow.objectRefTargetOid).isNull();
        assertThat(caseRow.objectRefTargetType).isNull();
        assertThat(caseRow.objectRefRelationId).isNull();
        assertThat(caseRow.orgRefTargetOid).isNull();
        assertThat(caseRow.orgRefTargetType).isNull();
        assertThat(caseRow.orgRefRelationId).isNull();
        assertThat(caseRow.outcome).isEqualTo("whatever");
        assertThat(caseRow.remediedTimestamp).isNull();
        assertThat(caseRow.currentStageDeadline).isNull();
        assertThat(caseRow.currentStageCreateTimestamp).isEqualTo(case2CurrentStageCreateTimestamp);
        assertThat(caseRow.stageNumber).isEqualTo(25);
        assertThat(caseRow.targetRefTargetOid).isEqualTo(caseTargetRefOid);
        assertThat(caseRow.targetRefTargetType).isEqualTo(MObjectType.ROLE);
        assertCachedUri(caseRow.targetRefRelationId, caseTargetRefRelationUri);
        assertThat(caseRow.tenantRefTargetOid).isNull();
        assertThat(caseRow.tenantRefTargetType).isNull();
        assertThat(caseRow.tenantRefRelationId).isNull();

        QAccessCertificationWorkItem wiAlias = aliasFor(QAccessCertificationWorkItem.class);
        List<MAccessCertificationWorkItem> wiRows = select(wiAlias,
                wiAlias.ownerOid.eq(UUID.fromString(accessCertificationCampaign.getOid())));
        assertThat(wiRows).hasSize(3);
        wiRows.sort(comparing(tr -> tr.cid));

        MAccessCertificationWorkItem wiRow = wiRows.get(0);
        assertThat(wiRow.cid).isEqualTo(55); // assigned in advance
        assertThat(wiRow.accessCertCaseCid).isEqualTo(48);
        assertThat(wiRow.containerType).isEqualTo(MContainerType.ACCESS_CERTIFICATION_WORK_ITEM);
        assertThat(wiRow.closeTimestamp).isEqualTo(wi1CloseTimestamp);
        assertThat(wiRow.campaignIteration).isEqualTo(81);
        assertThat(wiRow.outcome).isEqualTo("almost, but not quite, entirely done");
        assertThat(wiRow.outputChangeTimestamp).isEqualTo(wi1OutputChangeTimestamp);
        assertThat(wiRow.performerRefTargetOid).isEqualTo(performer1Oid);
        assertThat(wiRow.performerRefTargetType).isEqualTo(MObjectType.USER);
        assertCachedUri(wiRow.performerRefRelationId, performer1Relation);
        assertThat(wiRow.stageNumber).isEqualTo(21);

        wiRow = wiRows.get(1);
        assertThat(wiRow.cid).isEqualTo(56); // assigned in advance
        assertThat(wiRow.accessCertCaseCid).isEqualTo(48);
        assertThat(wiRow.containerType).isEqualTo(MContainerType.ACCESS_CERTIFICATION_WORK_ITEM);
        assertThat(wiRow.closeTimestamp).isEqualTo(wi2CloseTimestamp);
        assertThat(wiRow.campaignIteration).isEqualTo(82);
        assertThat(wiRow.outcome).isEqualTo("A tad more than almost, but not quite, entirely done");
        assertThat(wiRow.outputChangeTimestamp).isEqualTo(wi2OutputChangeTimestamp);
        assertThat(wiRow.performerRefTargetOid).isEqualTo(performer2Oid);
        assertThat(wiRow.performerRefTargetType).isEqualTo(MObjectType.USER);
        assertCachedUri(wiRow.performerRefRelationId, performer2Relation);
        assertThat(wiRow.stageNumber).isEqualTo(22);

        // Assignee refs
        QAccessCertificationWorkItemReference assigneeRefAlias =
                QAccessCertificationWorkItemReferenceMapping.getForCaseWorkItemAssignee().defaultAlias();
        List<MAccessCertificationWorkItemReference> assigneeRefRows = select(assigneeRefAlias,
                assigneeRefAlias.ownerOid.eq(UUID.fromString(accessCertificationCampaign.getOid())));
        assertThat(assigneeRefRows).hasSize(4);
        assigneeRefRows.sort(comparing(tr -> tr.targetOid));

        MAccessCertificationWorkItemReference assigneeRefRow = assigneeRefRows.get(0);
        assertThat(assigneeRefRow.ownerOid).isEqualTo(UUID.fromString(accessCertificationCampaign.getOid()));
        assertThat(assigneeRefRow.ownerType).isEqualTo(MObjectType.ACCESS_CERTIFICATION_CAMPAIGN);
        assertThat(assigneeRefRow.referenceType).isEqualTo(MReferenceType.ACCESS_CERT_WI_ASSIGNEE);
        assertThat(assigneeRefRow.targetType).isEqualTo(MObjectType.USER);
        assertThat(assigneeRefRow.targetOid).isEqualTo(wi3AssigneeRef1Oid);
        assertCachedUri(assigneeRefRow.relationId, wi3AssigneeRef1Relation);
        assertThat(assigneeRefRow.accessCertCaseCid).isEqualTo(49);
        assertThat(assigneeRefRow.accessCertWorkItemCid).isEqualTo(59);

        assigneeRefRow = assigneeRefRows.get(1);
        assertThat(assigneeRefRow.ownerOid).isEqualTo(UUID.fromString(accessCertificationCampaign.getOid()));
        assertThat(assigneeRefRow.ownerType).isEqualTo(MObjectType.ACCESS_CERTIFICATION_CAMPAIGN);
        assertThat(assigneeRefRow.referenceType).isEqualTo(MReferenceType.ACCESS_CERT_WI_ASSIGNEE);
        assertThat(assigneeRefRow.targetType).isEqualTo(MObjectType.USER);
        assertThat(assigneeRefRow.targetOid).isEqualTo(wi1AssigneeRef1Oid);
        assertCachedUri(assigneeRefRow.relationId, wi1AssigneeRef1Relation);
        assertThat(assigneeRefRow.accessCertCaseCid).isEqualTo(48);
        assertThat(assigneeRefRow.accessCertWorkItemCid).isEqualTo(55);

        assigneeRefRow = assigneeRefRows.get(2);
        assertThat(assigneeRefRow.ownerOid).isEqualTo(UUID.fromString(accessCertificationCampaign.getOid()));
        assertThat(assigneeRefRow.ownerType).isEqualTo(MObjectType.ACCESS_CERTIFICATION_CAMPAIGN);
        assertThat(assigneeRefRow.referenceType).isEqualTo(MReferenceType.ACCESS_CERT_WI_ASSIGNEE);
        assertThat(assigneeRefRow.targetType).isEqualTo(MObjectType.USER);
        assertThat(assigneeRefRow.targetOid).isEqualTo(wi2AssigneeRef1Oid);
        assertCachedUri(assigneeRefRow.relationId, wi2AssigneeRef1Relation);
        assertThat(assigneeRefRow.accessCertCaseCid).isEqualTo(48);
        assertThat(assigneeRefRow.accessCertWorkItemCid).isEqualTo(56);

        assigneeRefRow = assigneeRefRows.get(3);
        assertThat(assigneeRefRow.ownerOid).isEqualTo(UUID.fromString(accessCertificationCampaign.getOid()));
        assertThat(assigneeRefRow.ownerType).isEqualTo(MObjectType.ACCESS_CERTIFICATION_CAMPAIGN);
        assertThat(assigneeRefRow.referenceType).isEqualTo(MReferenceType.ACCESS_CERT_WI_ASSIGNEE);
        assertThat(assigneeRefRow.targetType).isEqualTo(MObjectType.USER);
        assertThat(assigneeRefRow.targetOid).isEqualTo(wi1AssigneeRef2Oid);
        assertCachedUri(assigneeRefRow.relationId, wi1AssigneeRef2Relation);
        assertThat(assigneeRefRow.accessCertCaseCid).isEqualTo(48);
        assertThat(assigneeRefRow.accessCertWorkItemCid).isEqualTo(55);

        // Candidate refs
        QAccessCertificationWorkItemReference candidateRefAlias =
                QAccessCertificationWorkItemReferenceMapping.getForCaseWorkItemCandidate().defaultAlias();
        List<MAccessCertificationWorkItemReference> candidateRefRows = select(candidateRefAlias,
                candidateRefAlias.ownerOid.eq(UUID.fromString(accessCertificationCampaign.getOid())));
        assertThat(candidateRefRows).hasSize(4);
        candidateRefRows.sort(comparing(tr -> tr.targetOid));

        MAccessCertificationWorkItemReference candidateRefRow = candidateRefRows.get(0);
        assertThat(candidateRefRow.ownerOid).isEqualTo(UUID.fromString(accessCertificationCampaign.getOid()));
        assertThat(candidateRefRow.ownerType).isEqualTo(MObjectType.ACCESS_CERTIFICATION_CAMPAIGN);
        assertThat(candidateRefRow.referenceType).isEqualTo(MReferenceType.ACCESS_CERT_WI_CANDIDATE);
        assertThat(candidateRefRow.targetType).isEqualTo(MObjectType.ORG);
        assertThat(candidateRefRow.targetOid).isEqualTo(wi3CandidateRef1Oid);
        assertCachedUri(candidateRefRow.relationId, wi3CandidateRef1Relation);
        assertThat(candidateRefRow.accessCertCaseCid).isEqualTo(49);
        assertThat(candidateRefRow.accessCertWorkItemCid).isEqualTo(59);

        candidateRefRow = candidateRefRows.get(1);
        assertThat(candidateRefRow.ownerOid).isEqualTo(UUID.fromString(accessCertificationCampaign.getOid()));
        assertThat(candidateRefRow.ownerType).isEqualTo(MObjectType.ACCESS_CERTIFICATION_CAMPAIGN);
        assertThat(candidateRefRow.referenceType).isEqualTo(MReferenceType.ACCESS_CERT_WI_CANDIDATE);
        assertThat(candidateRefRow.targetType).isEqualTo(MObjectType.USER);
        assertThat(candidateRefRow.targetOid).isEqualTo(wi1CandidateRef1Oid);
        assertCachedUri(candidateRefRow.relationId, wi1CandidateRef1Relation);
        assertThat(candidateRefRow.accessCertCaseCid).isEqualTo(48);
        assertThat(candidateRefRow.accessCertWorkItemCid).isEqualTo(55);

        candidateRefRow = candidateRefRows.get(2);
        assertThat(candidateRefRow.ownerOid).isEqualTo(UUID.fromString(accessCertificationCampaign.getOid()));
        assertThat(candidateRefRow.ownerType).isEqualTo(MObjectType.ACCESS_CERTIFICATION_CAMPAIGN);
        assertThat(candidateRefRow.referenceType).isEqualTo(MReferenceType.ACCESS_CERT_WI_CANDIDATE);
        assertThat(candidateRefRow.targetType).isEqualTo(MObjectType.ORG);
        assertThat(candidateRefRow.targetOid).isEqualTo(wi2CandidateRef2Oid);
        assertCachedUri(candidateRefRow.relationId, wi2CandidateRef2Relation);
        assertThat(candidateRefRow.accessCertCaseCid).isEqualTo(48);
        assertThat(candidateRefRow.accessCertWorkItemCid).isEqualTo(56);

        candidateRefRow = candidateRefRows.get(3);
        assertThat(candidateRefRow.ownerOid).isEqualTo(UUID.fromString(accessCertificationCampaign.getOid()));
        assertThat(candidateRefRow.ownerType).isEqualTo(MObjectType.ACCESS_CERTIFICATION_CAMPAIGN);
        assertThat(candidateRefRow.referenceType).isEqualTo(MReferenceType.ACCESS_CERT_WI_CANDIDATE);
        assertThat(candidateRefRow.targetType).isEqualTo(MObjectType.USER);
        assertThat(candidateRefRow.targetOid).isEqualTo(wi2CandidateRef1Oid);
        assertCachedUri(candidateRefRow.relationId, wi2CandidateRef1Relation);
        assertThat(candidateRefRow.accessCertCaseCid).isEqualTo(48);
        assertThat(candidateRefRow.accessCertWorkItemCid).isEqualTo(56);

    }

    private <C extends Containerable> void assertContainerFullObject(byte[] rowFullObject, C sObject) throws Exception {
        byte[] serializeSObject = serializeFullObject(sObject)
                .getBytes(StandardCharsets.UTF_8);
        assertThat(rowFullObject).isEqualTo(serializeSObject);
    }

    @Test
    public void test850Case() throws Exception {
        OperationResult result = createOperationResult();

        given("case");
        String objectName = "case" + getTestNumber();
        UUID parentOid = UUID.randomUUID();
        QName parentRelation = QName.valueOf("{https://random.org/ns}case-parent-rel");
        UUID objectOid = UUID.randomUUID();
        QName objectRelation = QName.valueOf("{https://random.org/ns}case-object-rel");
        UUID requestorOid = UUID.randomUUID();
        QName requestorRelation = QName.valueOf("{https://random.org/ns}case-requestor-rel");
        UUID targetOid = UUID.randomUUID();
        QName targetRelation = QName.valueOf("{https://random.org/ns}case-target-rel");
        UUID originalAssignee1Oid = UUID.randomUUID();
        QName originalAssignee1Relation = QName.valueOf("{https://random.org/ns}original-assignee1-rel");
        UUID performer1Oid = UUID.randomUUID();
        QName performer1Relation = QName.valueOf("{https://random.org/ns}performer1-rel");
        UUID originalAssignee2Oid = UUID.randomUUID();
        QName originalAssignee2Relation = QName.valueOf("{https://random.org/ns}original-assignee2-rel");
        UUID performer2Oid = UUID.randomUUID();
        QName performer2Relation = QName.valueOf("{https://random.org/ns}performer2-rel");

        UUID wi1AssigneeRef1Oid = UUID.fromString("4be487d2-c833-11eb-ba67-6768439d49a8"); // explicit UUID, to ensure ordering
        QName wi1AssigneeRef1Relation = QName.valueOf("{https://random.org/ns}wi-1-assignee-1-rel");
        UUID wi1CandidateRef1Oid = UUID.fromString("ac4d4a54-c834-11eb-ba57-8b9fef95c25d"); // explicit UUID, to ensure ordering
        QName wi1CandidateRef1Relation = QName.valueOf("{https://random.org/ns}wi-1-candidate-1-rel");
        UUID wi1CandidateRef2Oid = UUID.fromString("9dc675dc-c834-11eb-b31a-17ac6f7bab4f"); // explicit UUID, to ensure ordering
        QName wi1CandidateRef2Relation = QName.valueOf("{https://random.org/ns}wi-1-candidate-2-rel");

        UUID wi2AssigneeRef1Oid = UUID.fromString("d867f3e0-c830-11eb-b7cf-abba51968ecb"); // explicit UUID, to ensure ordering
        QName wi2AssigneeRef1Relation = QName.valueOf("{https://random.org/ns}wi-2-assignee-1-rel");
        UUID wi2AssigneeRef2Oid = UUID.fromString("6a92b1ec-c831-11eb-a075-5f6be1e16c34"); // explicit UUID, to ensure ordering
        QName wi2AssigneeRef2Relation = QName.valueOf("{https://random.org/ns}wi-2-assignee-2-rel");
        UUID wi2CandidateRef1Oid = UUID.fromString("df6388b8-c834-11eb-946a-efa73de3615b"); // explicit UUID, to ensure ordering
        QName wi2CandidateRef1Relation = QName.valueOf("{https://random.org/ns}wi-2-candidate-1-rel");

        CaseType acase = new CaseType()
                .name(objectName)
                .state("closed")
                .closeTimestamp(MiscUtil.asXMLGregorianCalendar(321L))
                .parentRef(parentOid.toString(),
                        CaseType.COMPLEX_TYPE, parentRelation)
                .objectRef(objectOid.toString(),
                        RoleType.COMPLEX_TYPE, objectRelation)
                .requestorRef(requestorOid.toString(),
                        UserType.COMPLEX_TYPE, requestorRelation)
                .targetRef(targetOid.toString(),
                        OrgType.COMPLEX_TYPE, targetRelation)
                .workItem(new CaseWorkItemType()
                        .id(41L)
                        .createTimestamp(MiscUtil.asXMLGregorianCalendar(10000L))
                        .closeTimestamp(MiscUtil.asXMLGregorianCalendar(10100L))
                        .deadline(MiscUtil.asXMLGregorianCalendar(10200L))
                        .originalAssigneeRef(originalAssignee1Oid.toString(),
                                OrgType.COMPLEX_TYPE, originalAssignee1Relation)
                        .performerRef(performer1Oid.toString(),
                                UserType.COMPLEX_TYPE, performer1Relation)
                        .stageNumber(1)
                        .assigneeRef(wi1AssigneeRef1Oid.toString(), UserType.COMPLEX_TYPE, wi1AssigneeRef1Relation)
                        .candidateRef(wi1CandidateRef1Oid.toString(), UserType.COMPLEX_TYPE, wi1CandidateRef1Relation)
                        .candidateRef(wi1CandidateRef2Oid.toString(), UserType.COMPLEX_TYPE, wi1CandidateRef2Relation)
                        .output(new AbstractWorkItemOutputType().outcome("OUTCOME one")))
                .workItem(new CaseWorkItemType()
                        .id(42L)
                        .createTimestamp(MiscUtil.asXMLGregorianCalendar(20000L))
                        .closeTimestamp(MiscUtil.asXMLGregorianCalendar(20100L))
                        .deadline(MiscUtil.asXMLGregorianCalendar(20200L))
                        .originalAssigneeRef(originalAssignee2Oid.toString(),
                                UserType.COMPLEX_TYPE, originalAssignee2Relation)
                        .performerRef(performer2Oid.toString(),
                                UserType.COMPLEX_TYPE, performer2Relation)
                        .stageNumber(2)
                        .assigneeRef(wi2AssigneeRef1Oid.toString(), UserType.COMPLEX_TYPE, wi2AssigneeRef1Relation)
                        .assigneeRef(wi2AssigneeRef2Oid.toString(), UserType.COMPLEX_TYPE, wi2AssigneeRef2Relation)
                        .candidateRef(wi2CandidateRef1Oid.toString(), UserType.COMPLEX_TYPE, wi2CandidateRef1Relation)
                        .output(new AbstractWorkItemOutputType().outcome("OUTCOME two")));

        when("adding it to the repository");
        repositoryService.addObject(acase.asPrismObject(), null, result);

        then("it is stored and relevant items are in columns");
        assertThatOperationResult(result).isSuccess();

        MCase caseRow = selectObjectByOid(QCase.class, acase.getOid());
        assertThat(caseRow.state).isEqualTo("closed");
        assertThat(caseRow.closeTimestamp).isEqualTo(Instant.ofEpochMilli(321));
        assertThat(caseRow.parentRefTargetOid).isEqualTo(parentOid);
        assertThat(caseRow.parentRefTargetType).isEqualTo(MObjectType.CASE);
        assertCachedUri(caseRow.parentRefRelationId, parentRelation);
        assertThat(caseRow.objectRefTargetOid).isEqualTo(objectOid);
        assertThat(caseRow.objectRefTargetType).isEqualTo(MObjectType.ROLE);
        assertCachedUri(caseRow.objectRefRelationId, objectRelation);
        assertThat(caseRow.requestorRefTargetOid).isEqualTo(requestorOid);
        assertThat(caseRow.requestorRefTargetType).isEqualTo(MObjectType.USER);
        assertCachedUri(caseRow.requestorRefRelationId, requestorRelation);
        assertThat(caseRow.targetRefTargetOid).isEqualTo(targetOid);
        assertThat(caseRow.targetRefTargetType).isEqualTo(MObjectType.ORG);
        assertCachedUri(caseRow.targetRefRelationId, targetRelation);

        QCaseWorkItem wiAlias = aliasFor(QCaseWorkItem.class);
        List<MCaseWorkItem> wiRows = select(wiAlias,
                wiAlias.ownerOid.eq(UUID.fromString(acase.getOid())));
        assertThat(wiRows).hasSize(2);
        wiRows.sort(comparing(tr -> tr.cid));

        MCaseWorkItem wiRow = wiRows.get(0);
        assertThat(wiRow.cid).isEqualTo(41); // assigned in advance
        assertThat(wiRow.ownerOid.toString()).isEqualTo(acase.getOid());
        assertThat(wiRow.containerType).isEqualTo(MContainerType.CASE_WORK_ITEM);
        assertThat(wiRow.createTimestamp).isEqualTo(Instant.ofEpochMilli(10000));
        assertThat(wiRow.closeTimestamp).isEqualTo(Instant.ofEpochMilli(10100));
        assertThat(wiRow.deadline).isEqualTo(Instant.ofEpochMilli(10200));
        assertThat(wiRow.originalAssigneeRefTargetOid).isEqualTo(originalAssignee1Oid);
        assertThat(wiRow.originalAssigneeRefTargetType).isEqualTo(MObjectType.ORG);
        assertCachedUri(wiRow.originalAssigneeRefRelationId, originalAssignee1Relation);
        assertThat(wiRow.outcome).isEqualTo("OUTCOME one");
        assertThat(wiRow.performerRefTargetOid).isEqualTo(performer1Oid);
        assertThat(wiRow.performerRefTargetType).isEqualTo(MObjectType.USER);
        assertCachedUri(wiRow.performerRefRelationId, performer1Relation);
        assertThat(wiRow.stageNumber).isEqualTo(1);

        wiRow = wiRows.get(1);
        assertThat(wiRow.cid).isEqualTo(42); // assigned in advance
        assertThat(wiRow.ownerOid.toString()).isEqualTo(acase.getOid());
        assertThat(wiRow.containerType).isEqualTo(MContainerType.CASE_WORK_ITEM);
        assertThat(wiRow.createTimestamp).isEqualTo(Instant.ofEpochMilli(20000));
        assertThat(wiRow.closeTimestamp).isEqualTo(Instant.ofEpochMilli(20100));
        assertThat(wiRow.deadline).isEqualTo(Instant.ofEpochMilli(20200));
        assertThat(wiRow.originalAssigneeRefTargetOid).isEqualTo(originalAssignee2Oid);
        assertThat(wiRow.originalAssigneeRefTargetType).isEqualTo(MObjectType.USER);
        assertCachedUri(wiRow.originalAssigneeRefRelationId, originalAssignee2Relation);
        assertThat(wiRow.outcome).isEqualTo("OUTCOME two");
        assertThat(wiRow.performerRefTargetOid).isEqualTo(performer2Oid);
        assertThat(wiRow.performerRefTargetType).isEqualTo(MObjectType.USER);
        assertCachedUri(wiRow.performerRefRelationId, performer2Relation);
        assertThat(wiRow.stageNumber).isEqualTo(2);

        QCaseWorkItemReference assigneeRefAlias =
                QCaseWorkItemReferenceMapping.getForCaseWorkItemAssignee().defaultAlias();
        List<MCaseWorkItemReference> assigneeRefRows = select(assigneeRefAlias,
                assigneeRefAlias.ownerOid.eq(UUID.fromString(acase.getOid())));
        assertThat(assigneeRefRows).hasSize(3);
        assigneeRefRows.sort(comparing(tr -> tr.targetOid));

        MCaseWorkItemReference assigneeRefRow = assigneeRefRows.get(0);
        assertThat(assigneeRefRow.ownerOid).isEqualTo(UUID.fromString(acase.getOid()));
        assertThat(assigneeRefRow.ownerType).isEqualTo(MObjectType.CASE);
        assertThat(assigneeRefRow.referenceType).isEqualTo(MReferenceType.CASE_WI_ASSIGNEE);
        assertThat(assigneeRefRow.targetType).isEqualTo(MObjectType.USER);
        assertThat(assigneeRefRow.targetOid).isEqualTo(wi2AssigneeRef1Oid);
        assertCachedUri(assigneeRefRow.relationId, wi2AssigneeRef1Relation);
        assertThat(assigneeRefRow.workItemCid).isEqualTo(42);

        assigneeRefRow = assigneeRefRows.get(2);
        assertThat(assigneeRefRow.ownerOid).isEqualTo(UUID.fromString(acase.getOid()));
        assertThat(assigneeRefRow.ownerType).isEqualTo(MObjectType.CASE);
        assertThat(assigneeRefRow.referenceType).isEqualTo(MReferenceType.CASE_WI_ASSIGNEE);
        assertThat(assigneeRefRow.targetType).isEqualTo(MObjectType.USER);
        assertThat(assigneeRefRow.targetOid).isEqualTo(wi2AssigneeRef2Oid);
        assertCachedUri(assigneeRefRow.relationId, wi2AssigneeRef2Relation);
        assertThat(assigneeRefRow.workItemCid).isEqualTo(42);

        assigneeRefRow = assigneeRefRows.get(1);
        assertThat(assigneeRefRow.ownerOid).isEqualTo(UUID.fromString(acase.getOid()));
        assertThat(assigneeRefRow.ownerType).isEqualTo(MObjectType.CASE);
        assertThat(assigneeRefRow.referenceType).isEqualTo(MReferenceType.CASE_WI_ASSIGNEE);
        assertThat(assigneeRefRow.targetType).isEqualTo(MObjectType.USER);
        assertThat(assigneeRefRow.targetOid).isEqualTo(wi1AssigneeRef1Oid);
        assertCachedUri(assigneeRefRow.relationId, wi1AssigneeRef1Relation);
        assertThat(assigneeRefRow.workItemCid).isEqualTo(41);

        QCaseWorkItemReference candidateRefAlias =
                QCaseWorkItemReferenceMapping.getForCaseWorkItemCandidate().defaultAlias();
        List<MCaseWorkItemReference> candidateRefRows = select(candidateRefAlias,
                candidateRefAlias.ownerOid.eq(UUID.fromString(acase.getOid())));
        assertThat(candidateRefRows).hasSize(3);
        candidateRefRows.sort(comparing(tr -> tr.targetOid));

        MCaseWorkItemReference candidateRefRow = candidateRefRows.get(0);
        assertThat(candidateRefRow.ownerOid).isEqualTo(UUID.fromString(acase.getOid()));
        assertThat(candidateRefRow.ownerType).isEqualTo(MObjectType.CASE);
        assertThat(candidateRefRow.referenceType).isEqualTo(MReferenceType.CASE_WI_CANDIDATE);
        assertThat(candidateRefRow.targetType).isEqualTo(MObjectType.USER);
        assertThat(candidateRefRow.targetOid).isEqualTo(wi1CandidateRef2Oid);
        assertCachedUri(candidateRefRow.relationId, wi1CandidateRef2Relation);
        assertThat(candidateRefRow.workItemCid).isEqualTo(41);

        candidateRefRow = candidateRefRows.get(1);
        assertThat(candidateRefRow.ownerOid).isEqualTo(UUID.fromString(acase.getOid()));
        assertThat(candidateRefRow.ownerType).isEqualTo(MObjectType.CASE);
        assertThat(candidateRefRow.referenceType).isEqualTo(MReferenceType.CASE_WI_CANDIDATE);
        assertThat(candidateRefRow.targetType).isEqualTo(MObjectType.USER);
        assertThat(candidateRefRow.targetOid).isEqualTo(wi1CandidateRef1Oid);
        assertCachedUri(candidateRefRow.relationId, wi1CandidateRef1Relation);
        assertThat(candidateRefRow.workItemCid).isEqualTo(41);

        candidateRefRow = candidateRefRows.get(2);
        assertThat(candidateRefRow.ownerOid).isEqualTo(UUID.fromString(acase.getOid()));
        assertThat(candidateRefRow.ownerType).isEqualTo(MObjectType.CASE);
        assertThat(candidateRefRow.referenceType).isEqualTo(MReferenceType.CASE_WI_CANDIDATE);
        assertThat(candidateRefRow.targetType).isEqualTo(MObjectType.USER);
        assertThat(candidateRefRow.targetOid).isEqualTo(wi2CandidateRef1Oid);
        assertCachedUri(candidateRefRow.relationId, wi2CandidateRef1Relation);
        assertThat(candidateRefRow.workItemCid).isEqualTo(42);
    }

    @Test
    public void test860MessageTemplate() throws Exception {
        OperationResult result = createOperationResult();

        given("message template");
        String objectName = "messageTemplate" + getTestNumber();
        var messageTemplate = new MessageTemplateType()
                .name(objectName)
                .defaultContent(new MessageTemplateContentType()
                        .subjectExpression(velocityExpression("subject")));

        when("adding it to the repository");
        repositoryService.addObject(messageTemplate.asPrismObject(), null, result);

        then("it is stored and relevant items are in columns");
        assertThatOperationResult(result).isSuccess();

        MObject row = selectObjectByOid(QMessageTemplate.class, messageTemplate.getOid());
        assertThat(row).isNotNull(); // no additional columns
    }
    // endregion

    // region delete tests
    // when we get here we have a couple of users and some other types stored in the repository
    @Test
    public void test900DeleteOfNonexistentObjectFails() {
        OperationResult result = createOperationResult();

        given("nonexistent OID");
        String oid = UUID.randomUUID().toString();

        expect("deleting object fails");
        assertThatThrownBy(() -> repositoryService.deleteObject(ObjectType.class, oid, result))
                .isInstanceOf(ObjectNotFoundException.class);

        and("operation result is fatal error");
        assertThatOperationResult(result).isFatalError()
                .hasMessageMatching("Object of type 'ObjectType' with OID .* was not found\\.");
    }

    @Test
    public void test910DeleteUsingSupertypeWorksOk() throws Exception {
        OperationResult result = createOperationResult();

        given("any object existing in the repository");
        // we'll sacrifice one of the users for this
        UUID userOid = randomExistingOid(QUser.class);

        when("deleting the object using its supertype");
        DeleteObjectResult deleteResult =
                repositoryService.deleteObject(ObjectType.class, userOid.toString(), result);

        then("object is deleted");
        assertThatOperationResult(result).isSuccess();

        assertThat(deleteResult).isNotNull();
        assertThat(deleteResult.getObjectTextRepresentation()).isNotNull();

        assertThat(selectNullableObjectByOid(QUser.class, userOid)).isNull();
    }

    // slight variation of the above, but using lower-level abstract table
    @Test
    public void test911DeleteUserUsingFocusWorksOk() throws Exception {
        OperationResult result = createOperationResult();

        given("random user existing in the repository");
        // we'll sacrifice one of the users for this
        UUID userOid = randomExistingOid(QUser.class);

        when("deleting it using the focus type");
        DeleteObjectResult deleteResult =
                repositoryService.deleteObject(FocusType.class, userOid.toString(), result);

        then("user is deleted");
        assertThatOperationResult(result).isSuccess();

        assertThat(deleteResult).isNotNull();
        assertThat(deleteResult.getObjectTextRepresentation()).isNotNull();

        assertThat(selectNullableObjectByOid(QUser.class, userOid)).isNull();
    }

    @Test
    public void test920DeleteOperationUpdatesPerformanceMonitor()
            throws ObjectNotFoundException {
        OperationResult result = createOperationResult();

        given("object to delete and cleared performance information");
        UUID userOid = randomExistingOid(QUser.class);
        clearPerformanceMonitor();

        when("object is deleted from the repository");
        repositoryService.deleteObject(FocusType.class, userOid.toString(), result);

        then("performance monitor is updated");
        assertThatOperationResult(result).isSuccess();
        assertSingleOperationRecorded(REPO_OP_PREFIX + RepositoryService.OP_DELETE_OBJECT);
    }

    @Test
    public void test999DeleteAllOtherObjects() throws Exception {
        // this doesn't follow given-when-then, sorry
        OperationResult result = createOperationResult();

        // we didn't create that many in this class, it should be OK to read them all at once
        for (MObject row : select(QObject.CLASS)) {
            DeleteObjectResult deleteResult = repositoryService.deleteObject(
                    row.objectType.getSchemaType(), row.oid.toString(), result);

            assertThat(deleteResult).isNotNull();
        }

        assertThatOperationResult(result).isSuccess();

        assertThat(select(QObject.CLASS)).isEmpty();
        assertThat(select(QContainer.CLASS)).isEmpty();
        assertThat(select(QReference.CLASS)).isEmpty();
    }

    protected <R extends MObject, Q extends QObject<R>> UUID randomExistingOid(Class<Q> queryType) {
        try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession()) {
            Q path = aliasFor(queryType);
            return jdbcSession.newQuery()
                    .from(path)
                    .orderBy(path.oid.asc())
                    .select(path.oid)
                    .fetchFirst();
        }
    }
    // endregion
}
