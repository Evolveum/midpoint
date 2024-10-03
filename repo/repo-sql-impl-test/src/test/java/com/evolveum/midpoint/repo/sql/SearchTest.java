/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.prism.PrismConstants.T_OBJECT_REFERENCE;
import static com.evolveum.midpoint.prism.xml.XmlTypeConverter.createXMLGregorianCalendar;
import static com.evolveum.midpoint.repo.api.RepoModifyOptions.createForceReindex;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType.F_NAME;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.query.TypedQuery;

import org.assertj.core.api.Condition;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

@ContextConfiguration(locations = { "../../../../../ctx-test.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class SearchTest extends BaseSQLRepoTest {

    private static final String DESCRIPTION_TO_FIND = "tralala";
    private static final String ARCHETYPE1_OID = "a71e48fe-f6e2-40f4-ab76-b4ad4a0918ad";
    protected static final String ROLE123_OID = "18ffc76e-2ad6-4fb6-a464-29c73a6d571b";

    private String beforeConfigOid;
    private String x00002Oid; // user with 3 assignments

    @Override
    public void initSystem() throws Exception {
        OperationResult result = new OperationResult("add objects");
        PrismObject<UserType> beforeConfig = prismContext.createObjectable(UserType.class)
                .name("before-config")
                .description(DESCRIPTION_TO_FIND)
                .asPrismObject();
        beforeConfigOid = repositoryService.addObject(beforeConfig, null, result);

        FullTextSearchConfigurationType fullTextConfig = new FullTextSearchConfigurationType();
        FullTextSearchIndexedItemsConfigurationType entry = new FullTextSearchIndexedItemsConfigurationType();
        entry.getItem().add(new ItemPathType(ObjectType.F_NAME));
        entry.getItem().add(new ItemPathType(ObjectType.F_DESCRIPTION));
        fullTextConfig.getIndexed().add(entry);
        repositoryService.applyFullTextSearchConfiguration(fullTextConfig);
        logger.info("Applying full text search configuration: {}", fullTextConfig);

        List<PrismObject<? extends Objectable>> objects = prismContext.parserFor(new File(FOLDER_BASIC, "objects.xml")).parseObjects();
        objects.addAll(prismContext.parserFor(new File(FOLDER_BASIC, "objects-2.xml")).parseObjects());

        //noinspection rawtypes
        for (PrismObject object : objects) {
            //noinspection unchecked
            repositoryService.addObject(object, null, result);
            if (object.getName().getOrig().equals("atestuserX00002")) {
                x00002Oid = object.getOid();
            }
        }

        assertThatOperationResult(result).isSuccess();
    }

    @Test
    public void test100IterateEmptySet() throws Exception {
        OperationResult result = new OperationResult("search empty");

        ResultHandler<UserType> handler = (object, parentResult) -> {
            fail();
            return false;
        };

        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_NAME).eqPoly("asdf", "asdf").matchingStrict()
                .build();

        repositoryService.searchObjectsIterative(UserType.class, query, handler, null, false, result);
        assertThatOperationResult(result).isSuccess();
    }

    @Test
    public void test110IterateSet() throws Exception {
        OperationResult result = new OperationResult("search set");

        final List<PrismObject<?>> objects = new ArrayList<>();

        ResultHandler<UserType> handler = (object, parentResult) -> {
            objects.add(object);
            return true;
        };

        repositoryService.searchObjectsIterative(UserType.class, null, handler, null, false, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals(4, objects.size());
    }

    @Test
    public void test120IterateSetWithPaging() throws Exception {
        iterateGeneral(0, 2, 2, "atestuserX00002", "atestuserX00003");
        iterateGeneral(0, 2, 1, "atestuserX00002", "atestuserX00003");
        iterateGeneral(0, 1, 10, "atestuserX00002");
        iterateGeneral(0, 1, 1, "atestuserX00002");
        iterateGeneral(1, 1, 1, "atestuserX00003");
    }

    private void iterateGeneral(int offset, int size, int batch, final String... names) throws Exception {
        OperationResult result = new OperationResult("search general");

        final List<PrismObject<?>> objects = new ArrayList<>();

        ResultHandler<UserType> handler = new ResultHandler<>() {

            private int index = 0;

            @Override
            public boolean handle(PrismObject<UserType> object, OperationResult parentResult) {
                objects.add(object);
                assertEquals("Incorrect object name was read", names[index++], object.asObjectable().getName().getOrig());
                return true;
            }
        };

        SqlRepositoryConfiguration config = ((SqlRepositoryServiceImpl) repositoryService).sqlConfiguration();
        int oldBatchSize = config.getIterativeSearchByPagingBatchSize();
        config.setIterativeSearchByPagingBatchSize(batch);

        logger.trace(">>>>>> iterateGeneral: offset = " + offset + ", size = " + size + ", batch = " + batch + " <<<<<<");

        ObjectQuery query = prismContext.queryFactory().createQuery();
        query.setPaging(prismContext.queryFactory().createPaging(offset, size, ObjectType.F_NAME, OrderDirection.ASCENDING));
        repositoryService.searchObjectsIterative(UserType.class, query, handler, null, false, result);
        assertThatOperationResult(result).isSuccess();

        config.setIterativeSearchByPagingBatchSize(oldBatchSize);

        assertEquals(size, objects.size());
    }

    @Test
    public void test150CaseSensitiveSearch() throws Exception {
        final String existingNameOrig = "Test UserX00003";
        final String nonExistingNameOrig = "test UserX00003";
        final String nameNorm = "test userx00003";

        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_FULL_NAME).eqPoly(existingNameOrig, nameNorm).matchingOrig()
                .build();

        OperationResult result = new OperationResult("search");
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Should find one user", 1, users.size());

        query = prismContext.queryFor(UserType.class)
                .item(UserType.F_FULL_NAME).eqPoly(nonExistingNameOrig, nameNorm).matchingOrig()
                .build();

        users = repositoryService.searchObjects(UserType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Found user (shouldn't) because case insensitive search was used", 0, users.size());
    }

    @Test
    public void test200RoleMembershipSearch() throws Exception {
        PrismReferenceValue r456 = itemFactory().createReferenceValue("r456", RoleType.COMPLEX_TYPE);
        r456.setRelation(SchemaConstants.ORG_DEFAULT);
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_ROLE_MEMBERSHIP_REF).ref(r456)
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Should find one user", 1, users.size());
        assertEquals("Wrong user name", "atestuserX00003", users.get(0).getName().getOrig());

        PrismReferenceValue role123 = itemFactory().createReferenceValue(ROLE123_OID, RoleType.COMPLEX_TYPE);
        query = prismContext.queryFor(UserType.class)
                .item(UserType.F_ROLE_MEMBERSHIP_REF).ref(role123)
                .build();
        users = repositoryService.searchObjects(UserType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Should find two users", 2, users.size());

        PrismReferenceValue r123approver = itemFactory().createReferenceValue(ROLE123_OID, RoleType.COMPLEX_TYPE);
        r123approver.setRelation(SchemaConstants.ORG_APPROVER);
        query = prismContext.queryFor(UserType.class)
                .item(UserType.F_ROLE_MEMBERSHIP_REF).ref(r123approver)
                .build();
        users = repositoryService.searchObjects(UserType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Should find no users", 0, users.size());
    }

    @Test
    public void test210DelegatedSearch() throws Exception {
        PrismReferenceValue r789 = itemFactory().createReferenceValue("r789", RoleType.COMPLEX_TYPE);
        // intentionally without relation (meaning "member")
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_DELEGATED_REF).ref(r789)
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Should find one user", 1, users.size());
        assertEquals("Wrong user name", "atestuserX00003", users.get(0).getName().getOrig());

        PrismReferenceValue r123 = itemFactory().createReferenceValue(ROLE123_OID, RoleType.COMPLEX_TYPE);
        query = prismContext.queryFor(UserType.class)
                .item(UserType.F_DELEGATED_REF).ref(r123)
                .build();
        users = repositoryService.searchObjects(UserType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Should find no users", 0, users.size());
    }

    @Test
    public void test220PersonaSearch() throws Exception {
        PrismReferenceValue u000 = itemFactory().createReferenceValue("u000", UserType.COMPLEX_TYPE);
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_PERSONA_REF).ref(u000)
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Should find one user", 1, users.size());
        assertEquals("Wrong user name", "atestuserX00003", users.get(0).getName().getOrig());

        PrismReferenceValue r789 = itemFactory().createReferenceValue("r789", RoleType.COMPLEX_TYPE);
        query = prismContext.queryFor(UserType.class)
                .item(UserType.F_PERSONA_REF).ref(r789)
                .build();
        users = repositoryService.searchObjects(UserType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Should find no users", 0, users.size());
    }

    @Test
    public void test230SearchUsersWithSingleValueRefNull() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_TENANT_REF).isNull()
                .build();
        OperationResult result = new OperationResult("search");

        queryListener.clear().start();
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        // Mapping of REmbeddedReference duplicates tenantRef_targetOid is null, but it's not a serious problem.
        queryListener.dumpAndStop();
        assertThatOperationResult(result).isSuccess();

        assertThat(users).extracting(u -> u.getName().getOrig())
                .containsExactlyInAnyOrder("before-config", "atestuserX00002", "atestuserX00003");
    }

    @Test
    public void test231SearchUsersWithSingleValueRefNotNull() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .not().item(UserType.F_TENANT_REF).isNull()
                .build();
        OperationResult result = new OperationResult("search");

        queryListener.clear().start();
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        queryListener.dumpAndStop();
        assertThatOperationResult(result).isSuccess();

        assertThat(users).extracting(u -> u.getName().getOrig())
                .containsExactlyInAnyOrder("elaine123");
    }

    @Test
    public void test232SearchUsersWithMultiValueRefNull() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_PARENT_ORG_REF).isNull()
                .build();
        OperationResult result = new OperationResult("search");

        queryListener.clear().start();
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        queryListener.dumpAndStop();
        assertThatOperationResult(result).isSuccess();

        assertThat(users).extracting(u -> u.getName().getOrig())
                .containsExactlyInAnyOrder("before-config", "atestuserX00002", "atestuserX00003");
    }

    @Test
    public void test233SearchUsersWithMultiValueRefNotNull() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .not().item(UserType.F_PARENT_ORG_REF).isNull()
                .build();
        OperationResult result = new OperationResult("search");

        queryListener.clear().start();
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        queryListener.dumpAndStop();
        assertThatOperationResult(result).isSuccess();

        // The result is not treated with DISTINCT and there are two refs, so this is OK.
        assertThat(users).extracting(u -> u.getName().getOrig())
                .containsExactlyInAnyOrder("elaine123", "elaine123");
    }

    @Test
    public void test300AssignmentOrgRefSearch() throws Exception {
        PrismReferenceValue o123456 = itemFactory().createReferenceValue("o123456", OrgType.COMPLEX_TYPE);
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, AssignmentType.F_ORG_REF).ref(o123456)
                .build();

        OperationResult result = new OperationResult("search");
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Should find one user", 1, users.size());
        assertEquals("Wrong user name", "atestuserX00002", users.get(0).getName().getOrig());

        PrismReferenceValue o999 = itemFactory().createReferenceValue("o999", RoleType.COMPLEX_TYPE);
        query = prismContext.queryFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, AssignmentType.F_ORG_REF).ref(o999)
                .build();

        users = repositoryService.searchObjects(UserType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Should find zero users", 0, users.size());
    }

    @Test
    public void test310AssignmentResourceRefSearch() throws Exception {
        PrismReferenceValue resourceRef = itemFactory().createReferenceValue("10000000-0000-0000-0000-000000000004", ResourceType.COMPLEX_TYPE);
        ObjectQuery query = prismContext.queryFor(RoleType.class)
                .item(RoleType.F_ASSIGNMENT, AssignmentType.F_CONSTRUCTION, ConstructionType.F_RESOURCE_REF).ref(resourceRef)
                .build();

        OperationResult result = new OperationResult("search");
        List<PrismObject<RoleType>> roles = repositoryService.searchObjects(RoleType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Should find one role", 1, roles.size());
        assertEquals("Wrong role name", "Judge", roles.get(0).getName().getOrig());

        PrismReferenceValue resourceRef2 = itemFactory().createReferenceValue("FFFFFFFF-0000-0000-0000-000000000004", ResourceType.COMPLEX_TYPE);
        query = prismContext.queryFor(RoleType.class)
                .item(RoleType.F_ASSIGNMENT, AssignmentType.F_CONSTRUCTION, ConstructionType.F_RESOURCE_REF).ref(resourceRef2)
                .build();
        roles = repositoryService.searchObjects(RoleType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Should find zero roles", 0, roles.size());
    }

    @Test
    public void test320RoleAssignmentSearch() throws Exception {
        PrismReferenceValue r123 = itemFactory().createReferenceValue(ROLE123_OID, RoleType.COMPLEX_TYPE);
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref(r123)
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Should find one user", 1, users.size());
        assertEquals("Wrong user name", "atestuserX00002", users.get(0).getName().getOrig());

    }

    @Test
    public void test330OrgAssignmentSearch() throws Exception {
        PrismReferenceValue org = itemFactory().createReferenceValue("00000000-8888-6666-0000-100000000085", OrgType.COMPLEX_TYPE);
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref(org)
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Should find one user", 1, users.size());
        assertEquals("Wrong user name", "atestuserX00002", users.get(0).getName().getOrig());

    }

    @Test
    public void test340OrgAssignmentSearchNoTargetType() throws Exception {
        PrismReferenceValue org = itemFactory().createReferenceValue("00000000-8888-6666-0000-100000000085", null);
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref(org)
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Should find one user", 1, users.size());
        assertEquals("Wrong user name", "atestuserX00002", users.get(0).getName().getOrig());
    }

    @Test
    public void test350OrgAssignmentSearchByOid() throws Exception {
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref("00000000-8888-6666-0000-100000000085")
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Should find one user", 1, users.size());
        assertEquals("Wrong user name", "atestuserX00002", users.get(0).getName().getOrig());
        System.out.println("Found user:\n" + users.get(0).debugDump());
    }

    @Test
    public void test360RoleAndOrgAssignmentSearch() throws Exception {
        PrismReferenceValue r123 = itemFactory().createReferenceValue(ROLE123_OID, RoleType.COMPLEX_TYPE);
        PrismReferenceValue org = itemFactory().createReferenceValue("00000000-8888-6666-0000-100000000085", OrgType.COMPLEX_TYPE);
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref(r123)
                .and().item(UserType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref(org)
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Should find one user", 1, users.size());
        assertEquals("Wrong user name", "atestuserX00002", users.get(0).getName().getOrig());

    }

    @Test
    public void test400NotBusinessRoleTypeSearch() throws Exception {
        ObjectQuery query = prismContext.queryFor(RoleType.class)
                .not().item(RoleType.F_SUBTYPE).eq("business")
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<RoleType>> roles = repositoryService.searchObjects(RoleType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertThat(roles).as("roles")
                .hasSize(3)
                .map(r -> r.getName().getOrig())
                .as("role names")
                .containsExactlyInAnyOrder("Judge", "Admin-owned role", "role123");
    }

    @Test
    public void test410BusinessRoleTypeSearch() throws Exception {
        ObjectQuery query = prismContext.queryFor(RoleType.class)
                .item(RoleType.F_SUBTYPE).eq("business")
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<RoleType>> roles = repositoryService.searchObjects(RoleType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Should find one role", 1, roles.size());
        assertEquals("Wrong role name", "Pirate", roles.get(0).getName().getOrig());

    }

    @Test
    public void test420EmptyRoleTypeSearch() throws Exception {
        ObjectQuery query = prismContext.queryFor(RoleType.class)
                .item(RoleType.F_SUBTYPE).isNull()
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<RoleType>> roles = repositoryService.searchObjects(RoleType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertThat(roles).as("roles")
                .hasSize(3)
                .map(r -> r.getName().getOrig())
                .as("role names")
                .containsExactlyInAnyOrder("Judge", "Admin-owned role", "role123");
    }

    @Test
    public void test430NonEmptyRoleTypeSearch() throws Exception {
        ObjectQuery query = prismContext.queryFor(RoleType.class)
                .not().item(RoleType.F_SUBTYPE).isNull()
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<RoleType>> roles = repositoryService.searchObjects(RoleType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Should find one role", 1, roles.size());
        assertEquals("Wrong role name", "Pirate", roles.get(0).getName().getOrig());

    }

    @Test
    public void test440IndividualOwnerRef() throws Exception {
        testOwnerRef(TaskType.class, SystemObjectsType.USER_ADMINISTRATOR.value(), "Synchronization: Embedded Test OpenDJ");
        testOwnerRef(TaskType.class, null, "Task with no owner");
        testOwnerRef(TaskType.class, "123");

        testOwnerRef(AccessCertificationCampaignType.class, SystemObjectsType.USER_ADMINISTRATOR.value(), "All user assignments 1");
        testOwnerRef(AccessCertificationCampaignType.class, null, "No-owner campaign");
        testOwnerRef(AccessCertificationCampaignType.class, "123");

        testOwnerRef(AccessCertificationDefinitionType.class, SystemObjectsType.USER_ADMINISTRATOR.value(), "Admin-owned definition");
        testOwnerRef(AccessCertificationDefinitionType.class, null);
        testOwnerRef(AccessCertificationDefinitionType.class, "123");
    }

    @Test
    public void test450OwnerRefWithTypeRestriction() throws Exception {
        testOwnerRefWithTypeRestriction(TaskType.class, SystemObjectsType.USER_ADMINISTRATOR.value(), "Synchronization: Embedded Test OpenDJ");
        testOwnerRefWithTypeRestriction(TaskType.class, null, "Task with no owner");
        testOwnerRefWithTypeRestriction(TaskType.class, "123");

        testOwnerRefWithTypeRestriction(AccessCertificationCampaignType.class, SystemObjectsType.USER_ADMINISTRATOR.value(), "All user assignments 1");
        testOwnerRefWithTypeRestriction(AccessCertificationCampaignType.class, null, "No-owner campaign");
        testOwnerRefWithTypeRestriction(AccessCertificationCampaignType.class, "123");

        testOwnerRefWithTypeRestriction(AccessCertificationDefinitionType.class, SystemObjectsType.USER_ADMINISTRATOR.value(), "Admin-owned definition");
        testOwnerRefWithTypeRestriction(AccessCertificationDefinitionType.class, null);
        testOwnerRefWithTypeRestriction(AccessCertificationDefinitionType.class, "123");
    }

    private void testOwnerRef(Class<? extends ObjectType> clazz, String oid, String... names) throws SchemaException {
        ObjectQuery query = prismContext.queryFor(clazz)
                .item(new QName(SchemaConstants.NS_C, "ownerRef")).ref(oid)
                .build();
        checkResult(clazz, clazz, oid, query, names);
    }

    private void testOwnerRefWithTypeRestriction(Class<? extends ObjectType> clazz, String oid, String... names) throws SchemaException {
        ObjectQuery query = prismContext.queryFor(ObjectType.class)
                .type(clazz)
                .item(new QName(SchemaConstants.NS_C, "ownerRef")).ref(oid)
                .build();
        checkResult(ObjectType.class, clazz, oid, query, names);
    }

    private void checkResult(Class<? extends ObjectType> queryClass, Class<? extends ObjectType> realClass, String oid, ObjectQuery query, String[] names)
            throws SchemaException {
        OperationResult result = new OperationResult("search");
        SearchResultList<? extends PrismObject<? extends ObjectType>> objects = repositoryService.searchObjects(queryClass, query, null, result);
        System.out.println(realClass.getSimpleName() + " owned by " + oid + ": " + objects.size());
        assertEquals("Wrong # of found objects", names.length, objects.size());
        Set<String> expectedNames = new HashSet<>(Arrays.asList(names));
        Set<String> realNames = new HashSet<>();
        for (PrismObject<? extends ObjectType> object : objects) {
            realNames.add(object.asObjectable().getName().getOrig());
        }
        assertEquals("Wrong names of found objects", expectedNames, realNames);
    }

    @Test
    public void test560WildOwnerRef() throws SchemaException {
        final String oid = SystemObjectsType.USER_ADMINISTRATOR.value();
        ItemDefinition<?> ownerRefDef = prismContext.getSchemaRegistry()
                .findObjectDefinitionByCompileTimeClass(TaskType.class)
                .findItemDefinition(TaskType.F_OWNER_REF);
        ObjectQuery query = prismContext.queryFor(ObjectType.class)
                .item(ItemPath.create(new QName(SchemaConstants.NS_C, "ownerRef")), ownerRefDef).ref(oid)
                .build();
        OperationResult result = new OperationResult("search");
        try {
            repositoryService.searchObjects(ObjectType.class, query, null, result);
            fail("Ambiguous searchObjects succeeded even if it should have failed.");
        } catch (SystemException e) {
            assertTrue("Wrong exception message: " + e.getMessage(), e.getMessage().contains("Unable to determine root entity for ownerRef"));
        }
    }

    @Test
    public void test600ResourceUp() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(ResourceType.class)
                .item(ResourceType.F_OPERATIONAL_STATE, OperationalStateType.F_LAST_AVAILABILITY_STATUS).eq(AvailabilityStatusType.UP)
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<ResourceType>> resources = repositoryService.searchObjects(ResourceType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Should find one resource", 1, resources.size());
    }

    @Test
    public void test610MultivaluedExtensionPropertySubstringQualified() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(GenericObjectType.class)
                .item(ObjectType.F_EXTENSION, new QName("http://example.com/p", "multivalued")).contains("slava")
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<GenericObjectType>> resources = repositoryService.searchObjects(GenericObjectType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Should find one object", 1, resources.size());
    }

    @Test
    public void test620MultivaluedExtensionPropertyEqualsQualified() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(GenericObjectType.class)
                .item(ObjectType.F_EXTENSION, new QName("http://example.com/p", "multivalued")).eq("Bratislava")
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<GenericObjectType>> resources = repositoryService.searchObjects(GenericObjectType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Should find one object", 1, resources.size());
    }

    @Test
    public void test630MultivaluedExtensionPropertySubstringUnqualified() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(GenericObjectType.class)
                .item(ObjectType.F_EXTENSION, new QName("multivalued")).contains("slava")
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<GenericObjectType>> resources = repositoryService.searchObjects(GenericObjectType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Should find one object", 1, resources.size());
    }

    @Test
    public void test640MultivaluedExtensionPropertyEqualsUnqualified() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(GenericObjectType.class)
                .item(ObjectType.F_EXTENSION, new QName("multivalued")).eq("Bratislava")
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<GenericObjectType>> resources = repositoryService.searchObjects(GenericObjectType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Should find one object", 1, resources.size());
    }

    @Test
    public void test650RoleAttributes() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(RoleType.class)
                .item(RoleType.F_RISK_LEVEL).eq("critical")
                .and().item(RoleType.F_IDENTIFIER).eq("123")
                .and().item(RoleType.F_DISPLAY_NAME).eqPoly("The honest one", "").matchingOrig()
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<RoleType>> roles = repositoryService.searchObjects(RoleType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Should find one object", 1, roles.size());
    }

    // testing MID-3568
    @Test
    public void test660CaseInsensitiveSearch() throws Exception {
        final String existingNameNorm = "test userx00003";
        final String existingNameOrig = "Test UserX00003";
        final String emailLowerCase = "testuserx00003@example.com";
        final String emailVariousCase = "TeStUsErX00003@EXAmPLE.com";

        assertUsersFound(prismContext.queryFor(UserType.class)
                        .item(UserType.F_FULL_NAME).eqPoly(existingNameNorm).matchingNorm()
                        .build(),
                false, 1);

        assertUsersFound(prismContext.queryFor(UserType.class)
                        .item(UserType.F_FULL_NAME).eqPoly(existingNameOrig).matchingNorm()
                        .build(),
                false, 1);

        assertUsersFound(prismContext.queryFor(UserType.class)
                        .item(UserType.F_EMAIL_ADDRESS).eq(emailLowerCase).matchingCaseIgnore()
                        .build(),
                false, 1);

        assertUsersFound(prismContext.queryFor(UserType.class)
                        .item(UserType.F_EMAIL_ADDRESS).eq(emailVariousCase).matchingCaseIgnore()
                        .build(),
                false, 1);

        // comparing polystrings, but providing plain String
        assertUsersFound(prismContext.queryFor(UserType.class)
                        .item(UserType.F_FULL_NAME).eq(existingNameNorm).matchingNorm()
                        .build(),
                false, 1);

        assertUsersFound(prismContext.queryFor(UserType.class)
                        .item(UserType.F_FULL_NAME).eq(existingNameOrig).matchingNorm()
                        .build(),
                false, 1);

        assertUsersFound(prismContext.queryFor(UserType.class)
                        .item(UserType.F_FULL_NAME).containsPoly(existingNameNorm).matchingNorm()
                        .build(),
                false, 1);

        assertUsersFound(prismContext.queryFor(UserType.class)
                        .item(UserType.F_FULL_NAME).containsPoly(existingNameOrig).matchingNorm()
                        .build(),
                false, 1);

        assertUsersFound(prismContext.queryFor(UserType.class)
                        .item(UserType.F_EMAIL_ADDRESS).contains(emailLowerCase).matchingCaseIgnore()
                        .build(),
                false, 1);

        assertUsersFound(prismContext.queryFor(UserType.class)
                        .item(UserType.F_EMAIL_ADDRESS).contains(emailVariousCase).matchingCaseIgnore()
                        .build(),
                false, 1);

        // comparing polystrings, but providing plain String
        assertUsersFound(prismContext.queryFor(UserType.class)
                        .item(UserType.F_FULL_NAME).contains(existingNameNorm).matchingNorm()
                        .build(),
                false, 1);

        assertUsersFound(prismContext.queryFor(UserType.class)
                        .item(UserType.F_FULL_NAME).contains(existingNameOrig).matchingNorm()
                        .build(),
                false, 1);
    }

    @Test
    public void test700FullTextSearch() throws Exception {
        assertUsersFound(prismContext.queryFor(UserType.class)
                        .fullText("atestuserX00003")
                        .build(),
                false, 1);

        assertUsersFound(prismContext.queryFor(UserType.class)
                        .fullText("Pellentesque")
                        .build(),
                true, 1);

        assertUsersFound(prismContext.queryFor(UserType.class)
                        .fullText("viverra")
                        .build(),
                true, 1);       // MID-4590

        assertUsersFound(prismContext.queryFor(UserType.class)
                        .fullText("sollicitudin")
                        .build(),
                true, 0);
    }

    @Test
    public void test710FulltextSearchNestedInOwnedBy() throws Exception {
        given("query for assignments owned by role matching given fulltext search");
        ObjectQuery query = prismContext.queryFor(AssignmentType.class)
                .ownedBy(AbstractRoleType.class)
                .fullText("Judge")
                .build();

        when("executing container search");
        OperationResult result = new OperationResult("search");
        queryListener.clear().start();
        SearchResultList<AssignmentType> assignments =
                repositoryService.searchContainers(AssignmentType.class, query, null, result);
        queryListener.dumpAndStop();

        then("only assignments/inducements from the Judge role are returned");
        assertThatOperationResult(result).isSuccess();
        assertThat(assignments).hasSize(2)
                .allMatch(a -> a.getConstruction() != null && a.getConstruction().getResourceRef() != null)
                .extracting(a -> a.getConstruction().getResourceRef().getOid())
                .containsExactlyInAnyOrder("10000000-0000-0000-0000-000000000004", "10000000-0000-0000-0000-000000000005");
    }

    @Test
    public void test711FulltextSearchNestedInExists() throws Exception {
        given("query for users having assignment to an org matching the fulltext condition");
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .exists(UserType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF, T_OBJECT_REFERENCE)
                .fullText("F0085")
                .build();

        when("executing the search");
        OperationResult result = new OperationResult("search");
        queryListener.clear().start();
        SearchResultList<PrismObject<UserType>> users =
                repositoryService.searchObjects(UserType.class, query, null, result);
        queryListener.dumpAndStop();

        then("only users assigned F0085 org are returned");
        assertThatOperationResult(result).isSuccess();
        assertThat(users)
                .extracting(u -> u.getName().getOrig())
                .containsExactlyInAnyOrder("atestuserX00002");
    }

    @Test
    public void test720FullTextSearchModify() throws Exception {
        OperationResult result = new OperationResult("fullTextSearch");
        Collection<SelectorOptions<GetOperationOptions>> distinct = distinct();

        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class,
                prismContext.queryFor(UserType.class).fullText("Pellentesque").build(),
                distinct(), result);

        String newDescription = "\n"
                + "\t\t\tUt pellentesque massa elit, in varius justo pellentesque ac. Vivamus gravida lectus non odio tempus iaculis sed quis\n"
                + "\t\t\tenim. Praesent sed ante nunc. Etiam euismod urna sit amet mi commodo luctus. Morbi dictum suscipit mauris ac\n"
                + "\t\t\tfacilisis. Phasellus congue luctus nibh, eu gravida sem iaculis in. Fusce bibendum quam sit amet tortor venenatis\n"
                + "\t\t\tmalesuada. Nam non varius nibh. Ut porta sit amet dui ut mollis. Etiam tincidunt ex viverra purus condimentum\n"
                + "\t\t\tinterdum. Sed ornare lacinia finibus. Pellentesque ac tortor scelerisque, sagittis diam nec, vehicula ligula.\n"
                + "\t\t\tPhasellus sodales felis quis fermentum volutpat. Orci varius natoque penatibus et magnis dis parturient montes,\n"
                + "\t\t\tnascetur ridiculus mus. Suspendisse mattis efficitur ligula et pharetra. Morbi risus erat, mollis nec suscipit vel,\n"
                + "\t\t\tconvallis eu ex.\n"
                + "\n"
                + "\t\t\tPellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Curabitur cursus\n"
                + "\t\t\tplacerat nisl, quis egestas ante semper at. Aenean semper nulla in elit tincidunt fringilla. Proin tristique sapien ac\n"
                + "\t\t\tenim blandit, a faucibus dui maximus. Curabitur sodales eget nulla eget mattis. Sed mollis blandit sapien. Aenean\n"
                + "\t\t\tpulvinar condimentum condimentum. Ut et venenatis arcu. Nam a nisl at nibh aliquam luctus. Phasellus eleifend non\n"
                + "\t\t\ttellus eget varius.\n"
                + "\n"
                + "\t\t\tNunc tincidunt sed lacus congue iaculis. Donec vel orci nulla. Phasellus fringilla, erat ac elementum lacinia, ex enim\n"
                + "\t\t\tdictum magna, accumsan dignissim elit mauris vel quam. Fusce vel tellus magna. Quisque sed tellus lectus. Donec id\n"
                + "\t\t\tnibh a lorem rutrum pharetra. Vestibulum vehicula leo ac eros pulvinar, eget tristique purus porta. Aliquam cursus\n"
                + "\t\t\tturpis sed libero eleifend interdum. Sed ac nisi a turpis finibus consectetur. Sed a velit vel nisi semper commodo.\n"
                + "\t\t\tVestibulum vel pulvinar ligula, vitae rutrum leo. Sed efficitur dignissim augue in placerat. Aliquam dapibus mauris\n"
                + "\t\t\teget diam pharetra molestie. Morbi vitae nulla sollicitudin, dignissim tellus a, tincidunt neque.\n";

        logger.info("## changing description ##");
        repositoryService.modifyObject(UserType.class, users.get(0).getOid(),
                prismContext.deltaFor(UserType.class)
                        .item(UserType.F_DESCRIPTION).replace(newDescription)
                        .asItemDeltas(),
                result);

        // just to see SQL used
        logger.info("## changing telephoneNumber ##");
        repositoryService.modifyObject(UserType.class, users.get(0).getOid(),
                prismContext.deltaFor(UserType.class)
                        .item(UserType.F_TELEPHONE_NUMBER).replace("123456")
                        .asItemDeltas(),
                result);

        assertUsersFoundBySearch(prismContext.queryFor(UserType.class)
                        .fullText("sollicitudin")
                        .build(),
                distinct, 1);

        assertUsersFound(prismContext.queryFor(UserType.class)
                        .fullText("viverra")
                        .build(),
                true, 1); // MID-4590

        assertUsersFoundBySearch(prismContext.queryFor(UserType.class)
                        .fullText("sollicitudin")
                        .asc(UserType.F_FULL_NAME)
                        .maxSize(100)
                        .build(),
                distinct, 1);
    }

    @Test // MID-4932
    public void test722FullTextSearchModifyName() throws Exception {
        OperationResult result = new OperationResult("fullTextSearchModify");
        repositoryService.modifyObject(TaskType.class, "777",
                prismContext.deltaFor(UserType.class)
                        .item(TaskType.F_NAME).replace(PolyString.fromOrig("TASK with no owner"))
                        .asItemDeltas(),
                result);

        repositoryService.modifyObject(TaskType.class, "777",
                prismContext.deltaFor(UserType.class)
                        .item(TaskType.F_NAME).replace(PolyString.fromOrig("Task with no owner"))
                        .asItemDeltas(),
                result);
    }

    @Test
    public void test725Reindex() throws Exception {

        OperationResult result = new OperationResult("reindex");

        assertUsersFound(prismContext.queryFor(UserType.class)
                        .fullText(DESCRIPTION_TO_FIND)
                        .build(),
                false, 0);
        repositoryService.modifyObject(UserType.class, beforeConfigOid, emptySet(), createForceReindex(), result);
        assertUsersFound(prismContext.queryFor(UserType.class)
                        .fullText(DESCRIPTION_TO_FIND)
                        .build(),
                false, 1);
    }

    private Collection<SelectorOptions<GetOperationOptions>> distinct() {
        return SelectorOptions.createCollection(GetOperationOptions.createDistinct());
    }

    @Test
    public void test730ShadowPendingOperation() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(ShadowType.class)
                .exists(ShadowType.F_PENDING_OPERATION)
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<ShadowType>> shadows = repositoryService.searchObjects(ShadowType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Should find one object", 1, shadows.size());
    }

    private void assertUsersFound(ObjectQuery query, boolean distinct, int expectedCount) throws Exception {
        Collection<SelectorOptions<GetOperationOptions>> options = distinct ? distinct() : null;
        assertObjectsFoundByCount(query, options, expectedCount);
        assertUsersFoundBySearch(query, options, expectedCount);
    }

    private void assertUsersFoundBySearch(ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, int expectedCount) throws Exception {
        OperationResult result = new OperationResult("search");
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, options, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Wrong # of results found: " + query, expectedCount, users.size());
    }

    private void assertObjectsFoundByCount(ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options,
            int expectedCount) throws Exception {
        OperationResult result = new OperationResult("count");
        int count = repositoryService.countObjects(UserType.class, query, options, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Wrong # of results found: " + query, expectedCount, count);
    }

    @Test
    public void test740OperationExecutionAny() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(CaseType.class)
                .item(ObjectType.F_OPERATION_EXECUTION, OperationExecutionType.F_STATUS).eq(OperationResultStatusType.FATAL_ERROR)
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<CaseType>> cases = repositoryService.searchObjects(CaseType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Should find one object", 1, cases.size());
    }

    @Test
    public void test750OperationExecutionWithTask() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(CaseType.class)
                .exists(ObjectType.F_OPERATION_EXECUTION)
                .block()
                .item(OperationExecutionType.F_TASK_REF).ref("task-oid-2")
                .and().item(OperationExecutionType.F_STATUS).eq(OperationResultStatusType.SUCCESS)
                .endBlock()
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<CaseType>> cases = repositoryService.searchObjects(CaseType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Should find one object", 1, cases.size());
    }

    @Test
    public void test751OperationExecutionWithTask2() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(CaseType.class)
                .exists(ObjectType.F_OPERATION_EXECUTION)
                .block()
                .item(OperationExecutionType.F_TASK_REF).ref("task-oid-2")
                .and().item(OperationExecutionType.F_STATUS).eq(OperationResultStatusType.FATAL_ERROR)
                .endBlock()
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<CaseType>> cases = repositoryService.searchObjects(CaseType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Should find no object", 0, cases.size());
    }

    @Test
    public void test800ExtensionReference() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(GenericObjectType.class)
                .item(ObjectType.F_EXTENSION, new QName("referenceType"))
                .ref("12345678-1234-1234-1234-123456789012")
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<GenericObjectType>> cases = repositoryService.searchObjects(GenericObjectType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Should find 1 object", 1, cases.size());
    }

    @Test
    public void test802ExtensionReferenceNotMatching() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(GenericObjectType.class)
                .item(ObjectType.F_EXTENSION, new QName("referenceType"))
                .ref("12345678-1234-1234-1234-123456789xxx")
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<GenericObjectType>> cases = repositoryService.searchObjects(GenericObjectType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Should find no object", 0, cases.size());
    }

    @Test
    public void test804ExtensionReferenceNull() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(GenericObjectType.class)
                .item(ObjectType.F_EXTENSION, new QName("referenceType"))
                .isNull()
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<GenericObjectType>> cases = repositoryService.searchObjects(GenericObjectType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Should find no object", 0, cases.size());
    }

    @Test
    public void test806ExtensionReferenceNonNull() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(GenericObjectType.class)
                .not().item(ObjectType.F_EXTENSION, new QName("referenceType"))
                .isNull()
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<GenericObjectType>> cases = repositoryService.searchObjects(GenericObjectType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Should find 1 object", 1, cases.size());
    }

    @Test
    public void test810ObjectCollection() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(ObjectCollectionType.class)
                .item(ObjectType.F_NAME).eqPoly("collection1", "collection1").matchingOrig()
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<ObjectCollectionType>> collections = repositoryService.searchObjects(ObjectCollectionType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Should find 1 object", 1, collections.size());
    }

    @Test
    public void test815AllObjectCollections() throws SchemaException {
        OperationResult result = new OperationResult("search");
        List<PrismObject<ObjectCollectionType>> collections = repositoryService.searchObjects(ObjectCollectionType.class, null, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Should find 1 object", 1, collections.size());
    }

    @Test
    public void test820FunctionLibrary() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(FunctionLibraryType.class)
                .item(ObjectType.F_NAME).eqPoly("fl1", "fl1").matchingOrig()
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<FunctionLibraryType>> collections = repositoryService.searchObjects(FunctionLibraryType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Should find 1 object", 1, collections.size());
    }

    @Test
    public void test830Archetype() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(ArchetypeType.class)
                .item(ObjectType.F_NAME).eqPoly("archetype1", "archetype1").matchingOrig()
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<ArchetypeType>> collections = repositoryService.searchObjects(ArchetypeType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Should find 1 object", 1, collections.size());
    }

    @Test
    public void test840SearchAssignmentHoldersByArchetypeRef() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(AssignmentHolderType.class)
                .item(AssignmentHolderType.F_ARCHETYPE_REF).ref(ARCHETYPE1_OID)
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<AssignmentHolderType>> objects = repositoryService.searchObjects(AssignmentHolderType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        System.out.println("Objects found: " + objects);
        assertEquals("Should find 3 objects", 3, objects.size());
    }

    @Test
    public void test850SearchObjectsByArchetypeRef() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(AssignmentHolderType.class)
                .item(AssignmentHolderType.F_ARCHETYPE_REF).ref(ARCHETYPE1_OID)
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<ObjectType>> objects = repositoryService.searchObjects(ObjectType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        System.out.println("Objects found: " + objects);
        assertEquals("Should find 3 objects", 3, objects.size());
    }

    @Test
    public void test860SearchUsersByArchetypeRef() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(AssignmentHolderType.F_ARCHETYPE_REF).ref(ARCHETYPE1_OID)
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<UserType>> objects = repositoryService.searchObjects(UserType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        System.out.println("Users found: " + objects);
        assertEquals("Should find 2 objects", 2, objects.size());
    }

    @Test
    public void test870SearchTasksByArchetypeRef() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(TaskType.class)
                .item(AssignmentHolderType.F_ARCHETYPE_REF).ref(ARCHETYPE1_OID)
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<TaskType>> objects = repositoryService.searchObjects(TaskType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        System.out.println("Tasks found: " + objects);
        assertEquals("Should find 1 object", 1, objects.size());
    }

    @Test
    public void test880SearchFocus() throws SchemaException {
        OperationResult result = new OperationResult("search");
        List<PrismObject<FocusType>> objects = repositoryService.searchObjects(FocusType.class, null, null, result);
        assertThatOperationResult(result).isSuccess();

        System.out.println("Objects found: " + objects);
        for (PrismObject<?> object : objects) {
            assertTrue("returned object is not a FocusType: " + object, object.asObjectable() instanceof FocusType);
        }
    }

    @Test
    public void test890SearchAssignmentHolder() throws SchemaException {
        OperationResult result = new OperationResult("search");
        queryListener.clear().start();
        List<PrismObject<AssignmentHolderType>> objects = repositoryService.searchObjects(AssignmentHolderType.class, null, null, result);
        queryListener.dumpAndStop();
        assertThatOperationResult(result).isSuccess();
        System.out.println("Objects found: " + objects);
        for (PrismObject<?> object : objects) {
            assertTrue("returned object is not a AssignmentHolderType: " + object, object.asObjectable() instanceof AssignmentHolderType);
        }
    }

    @Test
    public void test900CaseWorkItemAssignee() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(CaseType.class)
                .item(CaseType.F_WORK_ITEM, CaseWorkItemType.F_ASSIGNEE_REF).ref("5905f321-630f-4de3-abc9-ba3a614aac36")
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<CaseType>> cases = repositoryService.searchObjects(CaseType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Should find one object", 1, cases.size());
    }

    @Test
    public void test902CaseWorkItemCandidate() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(CaseType.class)
                .item(CaseType.F_WORK_ITEM, CaseWorkItemType.F_CANDIDATE_REF).ref("5905f321-630f-4de3-abc9-ba3a614aac36")
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<CaseType>> cases = repositoryService.searchObjects(CaseType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Should find one object", 1, cases.size());
    }

    @Test
    public void test904CaseWorkItemCandidateOther() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(CaseType.class)
                .item(CaseType.F_WORK_ITEM, CaseWorkItemType.F_CANDIDATE_REF).ref("d2bda14f-8571-4c99-bbe4-25c132650998")
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<CaseType>> cases = repositoryService.searchObjects(CaseType.class, query,
                schemaService.getOperationOptionsBuilder().distinct().build(), result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Should find one object", 1, cases.size());
    }

    @Test
    public void test910WorkItemCandidate() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(CaseWorkItemType.class)
                .item(CaseWorkItemType.F_CANDIDATE_REF).ref("d2bda14f-8571-4c99-bbe4-25c132650998")
                .build();
        OperationResult result = new OperationResult("search");
        SearchResultList<CaseWorkItemType> workItems = repositoryService.searchContainers(CaseWorkItemType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Wrong # of work items found", 2, workItems.size());
    }

    @Test
    public void test912WorkItemCandidateOther() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(CaseWorkItemType.class)
                .item(CaseWorkItemType.F_CANDIDATE_REF).ref("5905f321-630f-4de3-abc9-ba3a614aac36")
                .build();
        OperationResult result = new OperationResult("search");
        SearchResultList<CaseWorkItemType> workItems = repositoryService.searchContainers(CaseWorkItemType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Wrong # of work items found", 1, workItems.size());
    }

    @Test
    public void test914WorkItemWithCreateTimestamp() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(CaseWorkItemType.class)
                .not().item(CaseWorkItemType.F_CREATE_TIMESTAMP).isNull()
                .build();
        OperationResult result = new OperationResult("search");
        SearchResultList<CaseWorkItemType> workItems = repositoryService.searchContainers(CaseWorkItemType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Wrong # of work items found", 1, workItems.size());
        assertNotNull("Null createTimestamp", workItems.get(0).getCreateTimestamp());
    }

    // MID-6799, MID-6393
    @Test
    public void test920AssignmentsForOwner() throws SchemaException {
        given("query for assignments of a specified owner");
        ObjectQuery query = prismContext.queryFor(AssignmentType.class)
                .ownerId(x00002Oid)
                .build();
        OperationResult result = new OperationResult("search");

        when("executing container search");
        SearchResultList<AssignmentType> assignments =
                repositoryService.searchContainers(AssignmentType.class, query, null, result);

        then("all assignments of that owner are returned");
        assertThatOperationResult(result).isSuccess();
        assertThat(assignments).hasSize(3);
    }

    @Test
    public void test921AssignmentsOfSomeType() throws SchemaException {
        given("query for assignments with target ref type of Role");
        ObjectQuery query = prismContext.queryFor(AssignmentType.class)
                .item(AssignmentType.F_TARGET_REF).ref(null, RoleType.COMPLEX_TYPE)
                .and().ownerId(x00002Oid)
                .build();
        OperationResult result = new OperationResult("search");

        when("executing container search");
        SearchResultList<AssignmentType> assignments =
                repositoryService.searchContainers(AssignmentType.class, query, null, result);

        then("only assignment with target ref type equal to Role"
                + " (with default relation which is implied) is returned");
        assertThatOperationResult(result).isSuccess();
        assertThat(assignments).hasSize(1);
        assertThat(assignments.get(0).getTargetRef().getType()).isEqualTo(RoleType.COMPLEX_TYPE);
    }

    @Test
    public void test922AssignmentsWithSpecifiedTargetName() throws SchemaException {
        given("query for assignment to organization with specified name");
        ObjectQuery query = prismContext.queryFor(AssignmentType.class)
                .item(AssignmentType.F_TARGET_REF).ref(null, OrgType.COMPLEX_TYPE)
                .and()
                .item(AssignmentType.F_TARGET_REF, T_OBJECT_REFERENCE, F_NAME)
                .eq("F0085")
                // Skipping owner this time, although this is fishy as it is not currently in the returned values,
                // which means we will get assignments but we will not know which object is their owner.
                .asc(AssignmentType.F_TARGET_REF, T_OBJECT_REFERENCE, F_NAME)
                .build();
        OperationResult result = new OperationResult("search");

        when("executing container search");
        SearchResultList<AssignmentType> assignments =
                repositoryService.searchContainers(AssignmentType.class, query, null, result);

        then("only assignment to the specified organization is returned");
        assertThatOperationResult(result).isSuccess();
        assertThat(assignments).hasSize(1);
        // this OID in object.xml matches the F0085 name, the name itself is not in fetched data
        assertThat(assignments.get(0).getTargetRef().getOid()).isEqualTo("00000000-8888-6666-0000-100000000085");
    }

    @Test
    public void test930AssignmentsAndInducementsOwnedByAbstractRoles() throws SchemaException {
        given("query for assignments or inducements owned by any abstract role");
        ObjectQuery query = prismContext.queryFor(AssignmentType.class)
                .ownedBy(AbstractRoleType.class)
                .block()
                .endBlock()
                .build();
        OperationResult result = new OperationResult("search");

        when("executing container search");
        SearchResultList<AssignmentType> assignments =
                repositoryService.searchContainers(AssignmentType.class, query, null, result);

        then("only assignments/inducements from abstract roles are returned");
        assertThatOperationResult(result).isSuccess();
        assertThat(assignments).hasSize(4)
                // two and two are the same (one from a role, second from a service)
                .areExactly(2, new Condition<>(a -> a.getId().equals(5L)
                        && a.getConstruction().getResourceRef().getOid().equals("10000000-0000-0000-0000-000000000005"),
                        "assignment with ID 5 and construction ref ...005"))
                .areExactly(2, new Condition<>(a -> a.getId().equals(1L)
                        && a.getConstruction().getResourceRef().getOid().equals("10000000-0000-0000-0000-000000000004"),
                        "assignment with ID 1 and construction ref ...004"));
    }

    @Test
    public void test931AssignmentsAndInducementsOwnedByAbstractRolesIncludingInnerFilter() throws SchemaException {
        given("query for assignments or inducements owned by any abstract role with specified name (inner filter)");
        ObjectQuery query = prismContext.queryFor(AssignmentType.class)
                .ownedBy(AbstractRoleType.class)
                .block()
                // the inner condition path starts from AbstractRoleType
                .item(F_NAME).startsWithPoly("Jud") // only assignments/inducements from the Role "Judge"
                .endBlock()
                .and()
                // this path starts from AssignmentType, we're back in the outer query
                .item(AssignmentType.F_CONSTRUCTION, ConstructionType.F_RESOURCE_REF).ref("10000000-0000-0000-0000-000000000004")
                .build();
        OperationResult result = new OperationResult("search");

        when("executing container search");
        SearchResultList<AssignmentType> assignments =
                repositoryService.searchContainers(AssignmentType.class, query, null, result);

        then("only assignments/inducements matching the outer filter from roles matching the inner filter are returned");
        assertThatOperationResult(result).isSuccess();
        assertThat(assignments).singleElement()
                .matches(a -> a.getId().equals(1L)
                        && a.getConstruction().getResourceRef().getOid().equals("10000000-0000-0000-0000-000000000004"));
    }

    @Test
    public void test932AssignmentsOwnedByRole() throws SchemaException {
        given("query for assignments (using path) owned by the specified role");
        ObjectQuery query = prismContext.queryFor(AssignmentType.class)
                .ownedBy(AbstractRoleType.class, AbstractRoleType.F_ASSIGNMENT) // path for assignments only
                .block()
                // the inner condition path starts from AbstractRoleType
                .item(F_NAME).eq("Judge") // only assignments from the Role "Judge"
                .endBlock()
                .build();
        OperationResult result = new OperationResult("search");

        when("executing container search");
        SearchResultList<AssignmentType> assignments =
                repositoryService.searchContainers(AssignmentType.class, query, null, result);

        then("only assignments from the specified role are returned (not inducements)");
        assertThatOperationResult(result).isSuccess();
        assertThat(assignments).singleElement()
                .matches(a -> a.getId().equals(1L)
                        && a.getConstruction().getResourceRef().getOid().equals("10000000-0000-0000-0000-000000000004"));
    }

    @Test
    public void test935WorkItemsOwnedByAccessCertificationCase() throws SchemaException {
        given("query for work items owned by access certification case");
        ObjectQuery query = prismContext.queryFor(AccessCertificationWorkItemType.class)
                .ownedBy(AccessCertificationCaseType.class,
                        AccessCertificationCaseType.F_WORK_ITEM) // valid, but superfluous path
                .block()
                .ownedBy(AccessCertificationCampaignType.class)
                .block()
                .item(F_NAME).eqPoly("All user assignments 1")
                .endBlock()
                .endBlock()
                .and()
                // 3 out of 7 match this condition on the WI itself
                .item(AccessCertificationWorkItemType.F_OUTPUT_CHANGE_TIMESTAMP)
                .gt(createXMLGregorianCalendar("2015-12-04T01:10:14.614+01:00"))
                .build();
        OperationResult result = new OperationResult("search");

        when("executing container search");
        SearchResultList<AccessCertificationWorkItemType> assignments =
                repositoryService.searchContainers(AccessCertificationWorkItemType.class, query, null, result);

        then("only work items for the specific certification case are returned");
        assertThatOperationResult(result).isSuccess();
        assertThat(assignments).hasSize(3);
    }

    @Test
    public void test939OwnedByComplainsAboutInvalidTypesAndPathsCombinations() {
        expect("query fails when ownedBy types (owned and owning) do not make sense");
        OperationResult result = new OperationResult("search");
        assertThatThrownBy(() -> repositoryService.searchContainers(AssignmentType.class,
                prismContext.queryFor(AssignmentType.class)
                        .ownedBy(ObjectType.class)
                        .block()
                        .endBlock()
                        .build(), null, result))
                .isInstanceOf(SystemException.class)
                .hasCauseInstanceOf(QueryException.class)
                .hasMessage("OwnedBy filter with invalid owning type 'ObjectType', type "
                        + "'AssignmentType' can be owned by 'AssignmentHolderType' or its subtype.");
        assertThatOperationResult(result).isFatalError();

        expect("query fails when ownedBy path points to non-owning type");
        assertThatThrownBy(() -> repositoryService.searchContainers(AssignmentType.class,
                prismContext.queryFor(AssignmentType.class)
                        .ownedBy(AbstractRoleType.class, AbstractRoleType.F_RISK_LEVEL) // risk level is not assignment
                        .block()
                        .endBlock()
                        .build(), null, new OperationResult("search")))
                .isInstanceOf(SystemException.class)
                .hasCauseInstanceOf(QueryException.class)
                .hasMessage("OwnedBy filter for type 'AssignmentType' used with invalid path: riskLevel");

        expect("query fails when ownedBy path points to non-owning type (collection version)");
        assertThatThrownBy(() -> repositoryService.searchContainers(AssignmentType.class,
                prismContext.queryFor(AssignmentType.class)
                        .ownedBy(AbstractRoleType.class, AbstractRoleType.F_LINK_REF) // multi-value ref, not assignment
                        .block()
                        .endBlock()
                        .build(), null, new OperationResult("search")))
                .isInstanceOf(SystemException.class)
                .hasCauseInstanceOf(QueryException.class)
                .hasMessage("OwnedBy filter for type 'AssignmentType' used with invalid path: linkRef");

        expect("query fails when ownedBy is used with non-container searches");
        assertThatThrownBy(() -> repositoryService.searchObjects(ObjectType.class,
                prismContext.queryFor(ObjectType.class)
                        .ownedBy(ObjectType.class)
                        .block()
                        .endBlock()
                        .build(), null, new OperationResult("search")))
                .isInstanceOf(SystemException.class)
                .hasCauseInstanceOf(QueryException.class)
                .hasMessageStartingWith("OwnedBy filter is not supported for type 'ObjectType'");
    }

    @Test
    public void test940AssignmentsWithSpecifiedTargetUsingExists() throws SchemaException {
        given("query for assignments with target object with specified name (using exists)");
        ObjectQuery query = prismContext.queryFor(AssignmentType.class)
                .exists(AssignmentType.F_TARGET_REF, T_OBJECT_REFERENCE)
                .item(F_NAME).eq("F0085")
                .asc(AssignmentType.F_TARGET_REF, T_OBJECT_REFERENCE, F_NAME)
                .build();
        OperationResult result = new OperationResult("search");

        when("executing container search");
        SearchResultList<AssignmentType> assignments =
                repositoryService.searchContainers(AssignmentType.class, query, null, result);

        then("only assignments with specified target object name are returned");
        assertThatOperationResult(result).isSuccess();
        assertThat(assignments).singleElement()
                .matches(a -> a.getTargetRef().getOid().equals("00000000-8888-6666-0000-100000000085"));
    }

    @Test
    public void test941AssignmentsWithSpecifiedTargetUsingItem() throws SchemaException {
        given("query for assignments with target object with specified name (using item)");
        ObjectQuery query = prismContext.queryFor(AssignmentType.class)
                .item(AssignmentType.F_TARGET_REF, T_OBJECT_REFERENCE, F_NAME).eq("F0085")
                .asc(AssignmentType.F_TARGET_REF, T_OBJECT_REFERENCE, F_NAME)
                .build();
        OperationResult result = new OperationResult("search");

        when("executing container search");
        queryListener.clear().start();
        SearchResultList<AssignmentType> assignments =
                repositoryService.searchContainers(AssignmentType.class, query, null, result);
        queryListener.dumpAndStop();

        then("only assignments with specified target object name are returned");
        assertThatOperationResult(result).isSuccess();
        assertThat(assignments).singleElement()
                .matches(a -> a.getTargetRef().getOid().equals("00000000-8888-6666-0000-100000000085"));
    }

    @Test(enabled = false, description = "Broken where condition using a wrong joined table")
    public void test942MultiValueRefFilterWithItem() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_PARENT_ORG_REF, T_OBJECT_REFERENCE, F_NAME).eq("F0085")
                .build();
        OperationResult result = new OperationResult("search");
        display("QUERY: " + query);

        when("executing container search");
        queryListener.clear().start();
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        // WHERE name condition is constructed on m_acc_cert_campaign table (error in HQL->SQL)
        queryListener.dumpAndStop();

        then("only assignments with specified target object name are returned");
        assertThatOperationResult(result).isSuccess();
        // two refs match, no DISTINCT use, so it's doubled result
        assertThat(users).extracting(u -> u.getName().getOrig())
                .containsExactlyInAnyOrder("elaine123", "elaine123");
    }

    @Test
    public void test943MultiValueRefTargetWithTargetTypeSpecificCondition() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(UserType.class)
                // Example with multi-value linkRef from Query API docs, this works as expected:
                .item(UserType.F_LINK_REF, T_OBJECT_REFERENCE, ShadowType.F_RESOURCE_REF).ref("ef2bc95b-76e0-48e2-86d6-3d4f02d3e1a2")
                // While the shadow resource ref condition targets the m_shadow, the name condition would break again.
                //.item(UserType.F_LINK_REF, T_OBJECT_REFERENCE, ShadowType.F_NAME).eq("7754e27c-a7cb-4c23-850d-a9a15f71199a")
                .build();
        OperationResult result = new OperationResult("search");
        display("QUERY: " + query);

        when("executing container search");
        queryListener.clear().start();
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        queryListener.dumpAndStop();

        then("only assignments with specified target object name are returned");
        assertThatOperationResult(result).isSuccess();
        assertThat(users).extracting(u -> u.getName().getOrig())
                .containsExactlyInAnyOrder("atestuserX00003");
    }

    @Test
    public void test950AssignmentsWithSpecifiedTargetUsingRefWithTargetFilter() throws SchemaException {
        given("query for assignments with target object using new ref() construct");
        ObjectQuery query = prismContext.queryFor(AssignmentType.class)
                .ref(AssignmentType.F_TARGET_REF)
                .item(F_NAME).eq("F0085")
                .asc(AssignmentType.F_TARGET_REF, T_OBJECT_REFERENCE, F_NAME)
                .build();
        OperationResult result = new OperationResult("search");
        display("QUERY: " + query);

        when("executing container search");
        queryListener.clear().start();
        SearchResultList<AssignmentType> assignments =
                repositoryService.searchContainers(AssignmentType.class, query, null, result);
        queryListener.dumpAndStop(); // order and target filter should share the same join

        then("only assignments with specified target object name are returned");
        assertThatOperationResult(result).isSuccess();
        assertThat(assignments).singleElement()
                .matches(a -> a.getTargetRef().getOid().equals("00000000-8888-6666-0000-100000000085"));
    }

    @Test
    public void test951AssignmentsWithSpecifiedTargetUsingRefWithWrongTargetFilter() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(AssignmentType.class)
                .ref(AssignmentType.F_TARGET_REF)
                .item(F_NAME).eq("F0086") // bad name
                .asc(AssignmentType.F_TARGET_REF, T_OBJECT_REFERENCE, F_NAME)
                .build();

        OperationResult result = new OperationResult("search");
        queryListener.clear().start();
        SearchResultList<AssignmentType> assignments =
                repositoryService.searchContainers(AssignmentType.class, query, null, result);
        queryListener.dumpAndStop();

        assertThatOperationResult(result).isSuccess();
        assertThat(assignments).isEmpty();
    }

    @Test
    public void test952AssignmentsWithTargetRefWithValueAndTargetFilter() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(AssignmentType.class)
                .ref(AssignmentType.F_TARGET_REF, null, SchemaConstants.ORG_DEFAULT, "00000000-8888-6666-0000-100000000085")
                .item(F_NAME).eq("F0085")
                .build();

        OperationResult result = new OperationResult("search");
        queryListener.clear().start();
        SearchResultList<AssignmentType> assignments =
                repositoryService.searchContainers(AssignmentType.class, query, null, result);
        queryListener.dumpAndStop(); // value and filter should be combined using AND

        assertThatOperationResult(result).isSuccess();
        assertThat(assignments).singleElement()
                .matches(a -> a.getTargetRef().getOid().equals("00000000-8888-6666-0000-100000000085"));
    }

    @Test
    public void test953AssignmentsWithTargetRefWithMultipleValuesAndTargetFilter() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(AssignmentType.class)
                .ref(AssignmentType.F_TARGET_REF, null, SchemaConstants.ORG_DEFAULT,
                        "00000000-8888-6666-0000-100000000085",
                        "additional-oid-no-problem") // yeah, old repo doesn't care about OID format
                .item(F_NAME).eq("F0085")
                .build();

        OperationResult result = new OperationResult("search");
        queryListener.clear().start();
        SearchResultList<AssignmentType> assignments =
                repositoryService.searchContainers(AssignmentType.class, query, null, result);
        queryListener.dumpAndStop(); // value and filter should be combined using AND

        assertThatOperationResult(result).isSuccess();
        assertThat(assignments).singleElement()
                .matches(a -> a.getTargetRef().getOid().equals("00000000-8888-6666-0000-100000000085"));
    }

    @Test
    public void test954AssignmentsWithTargetRefWrongRelationAndTargetFilter() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(AssignmentType.class)
                .ref(AssignmentType.F_TARGET_REF, null, SchemaConstants.ORG_DEPUTY)
                .item(F_NAME).eq("F0085")
                .build();

        OperationResult result = new OperationResult("search");
        queryListener.clear().start();
        SearchResultList<AssignmentType> assignments =
                repositoryService.searchContainers(AssignmentType.class, query, null, result);
        queryListener.dumpAndStop();

        assertThatOperationResult(result).isSuccess();
        assertThat(assignments).isEmpty(); // no depute ref matches, even though the name is OK
    }

    @Test
    public void test955AssignmentsWithTargetRefWithGoodValueAndWrongTargetFilter() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(AssignmentType.class)
                .ref(AssignmentType.F_TARGET_REF, null, SchemaConstants.ORG_DEFAULT, "00000000-8888-6666-0000-100000000085")
                .item(F_NAME).eq("F0086") // bad name, nothing will be found, even with good OID above
                .asc(AssignmentType.F_TARGET_REF, T_OBJECT_REFERENCE, F_NAME)
                .build();

        OperationResult result = new OperationResult("search");
        queryListener.clear().start();
        SearchResultList<AssignmentType> assignments =
                repositoryService.searchContainers(AssignmentType.class, query, null, result);
        queryListener.dumpAndStop();

        assertThatOperationResult(result).isSuccess();
        assertThat(assignments).isEmpty();
    }

    @Test
    public void test957AssignmentsWithTargetRefWithTargetFilterUsingTypeFilter() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(AssignmentType.class)
                .ref(AssignmentType.F_TARGET_REF)
                .type(OrgType.class)
                .item(F_NAME).eq("F0085")
                .asc(AssignmentType.F_TARGET_REF, T_OBJECT_REFERENCE, F_NAME)
                .build();

        OperationResult result = new OperationResult("search");
        queryListener.clear().start();
        SearchResultList<AssignmentType> assignments =
                repositoryService.searchContainers(AssignmentType.class, query, null, result);
        queryListener.dumpAndStop();

        assertThatOperationResult(result).isSuccess();
        assertThat(assignments).singleElement()
                .matches(a -> a.getTargetRef().getOid().equals("00000000-8888-6666-0000-100000000085"));
    }

    @Test
    public void test958AssignmentsWithTargetRefWithTargetFilterUsingTypeFilterWithWrongType() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(AssignmentType.class)
                .ref(AssignmentType.F_TARGET_REF)
                .type(RoleType.class) // F0085 is an org, not a role
                .item(F_NAME).eq("F0085")
                .build();

        OperationResult result = new OperationResult("search");
        queryListener.clear().start();
        SearchResultList<AssignmentType> assignments =
                repositoryService.searchContainers(AssignmentType.class, query, null, result);
        queryListener.dumpAndStop();

        assertThatOperationResult(result).isSuccess();
        assertThat(assignments).isEmpty();
    }

    @Test(enabled = false, description = "Broken where condition using a wrong joined table")
    public void test960MultiValueRefFilterWithTargetFilter() throws SchemaException {
        given("query for users with parent org with specified name (target filter)");
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .ref(UserType.F_PARENT_ORG_REF)
                .item(F_NAME).eq("F0085")
                .build();
        OperationResult result = new OperationResult("search");
        display("QUERY: " + query);

        when("executing container search");
        queryListener.clear().start();
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        // WHERE name condition is constructed on m_acc_cert_campaign table (error in HQL->SQL)
        queryListener.dumpAndStop();

        then("only assignments with specified target object name are returned");
        assertThatOperationResult(result).isSuccess();
        // two refs match, no DISTINCT use, so it's doubled result
        assertThat(users).extracting(u -> u.getName().getOrig())
                .containsExactlyInAnyOrder("elaine123", "elaine123");
    }

    @Test(enabled = false, description = "Broken where condition using a wrong joined table."
            + " This actually returns the right result, but for the wrong reasons. If both this and 960 work THEN its OK.")
    public void test961RefFilterWithValueConditionsAndTargetFilterWrongName() throws SchemaException {
        given("query for users with parent org with specified relation (ref condition) and name (target filter)");
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .ref(UserType.F_PARENT_ORG_REF, null, SchemaConstants.ORG_MANAGER)
                .item(F_NAME).eq("F0086")
                .build();
        OperationResult result = new OperationResult("search");
        display("QUERY: " + query);

        when("executing container search");
        queryListener.clear().start();
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        queryListener.dumpAndStop();

        then("no target matches the wrong name");
        assertThatOperationResult(result).isSuccess();
        assertThat(users).isEmpty();
    }

    /**
     * See MID-5474 (just a quick attempt to replicate)
     */
    @Test
    public void test970IterateAndModify() throws Exception {
        OperationResult result = new OperationResult("iterateAndModify");

        AtomicInteger count = new AtomicInteger(0);
        ResultHandler<UserType> handler = (object, parentResult) -> {
            try {
                System.out.println("Modifying " + object);
                List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(UserType.class)
                        .item(UserType.F_DESCRIPTION).replace("desc1")
                        .asItemDeltas();
                repositoryService.modifyObject(UserType.class, object.getOid(), itemDeltas, parentResult);
                System.out.println("OK");
                count.incrementAndGet();
                return true;
            } catch (ObjectNotFoundException | SchemaException | ObjectAlreadyExistsException e) {
                throw new SystemException(e.getMessage(), e);
            }
        };

        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_NAME).eqPoly("atestuserX00002").matchingOrig()
                .build();
        repositoryService.searchObjectsIterative(UserType.class, query, handler, null, true, result);

        assertThatOperationResult(result).isSuccess();
        assertEquals(1, count.get());
    }

    // MID-5515
    @Test
    public void test971SearchNameNull() throws Exception {
        OperationResult result = new OperationResult("testSearchNameNull");
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(F_NAME).isNull()
                .build();

        SearchResultList<PrismObject<UserType>> objects = repositoryService.searchObjects(UserType.class, query, null, result);

        assertThatOperationResult(result).isSuccess();
        assertEquals("Wrong # of objects found", 0, objects.size());
    }

    // MID-5515
    @Test
    public void test972SearchNameNotNull() throws Exception {
        OperationResult result = new OperationResult("testSearchNameNotNull");
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .not().item(F_NAME).isNull()
                .build();

        SearchResultList<PrismObject<UserType>> objects = repositoryService.searchObjects(UserType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();

        int users = repositoryService.countObjects(UserType.class, null, null, result);
        assertEquals("Wrong # of objects found", users, objects.size());
    }

    // MID-4575
    @Test
    public void test980SearchPasswordCreateTimestamp() throws Exception {
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(ItemPath.create(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD,
                        PasswordType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP))
                .lt(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar())).build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertEquals("Should find one user", 1, users.size());
        assertEquals("Wrong user name", "atestuserX00002", users.get(0).getName().getOrig());
    }

    /** MID-9427 */
    @Test
    public void test981SearchByArchetypeName() throws Exception {
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_ARCHETYPE_REF, T_OBJECT_REFERENCE, F_NAME)
                .eqPoly("archetype1")
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertThat(users)
                .as("users found")
                .hasSize(2)
                .map(u -> u.getName().getOrig())
                .as("user names")
                .containsExactlyInAnyOrder("atestuserX00002", "atestuserX00003");

        // The same but arbitrary assignment holders now
        var objects = repositoryService.searchObjects(AssignmentHolderType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertThat(objects)
                .as("objects found")
                .hasSize(3)
                .map(u -> u.getName().getOrig())
                .as("object names")
                .containsExactlyInAnyOrder("atestuserX00002", "atestuserX00003", "Synchronization: Embedded Test OpenDJ");
    }

    /** MID-9427 */
    @Test
    public void test982SearchByRoleMembershipRefName() throws Exception {
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_ROLE_MEMBERSHIP_REF, T_OBJECT_REFERENCE, F_NAME)
                .eqPoly("role123")
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertThat(users)
                .as("users found")
                .hasSize(2)
                .map(u -> u.getName().getOrig())
                .as("user names")
                .containsExactlyInAnyOrder("atestuserX00002", "atestuserX00003");
    }

    /** MID-9427 (double @: linkRef/@/resourceRef/@/name) */
    @Test
    public void test983SearchByResourceName() throws Exception {
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_LINK_REF, T_OBJECT_REFERENCE, ShadowType.F_RESOURCE_REF, T_OBJECT_REFERENCE, F_NAME)
                .eqPoly("dummy")
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        assertThatOperationResult(result).isSuccess();
        assertThat(users)
                .as("users found")
                .hasSize(1)
                .map(u -> u.getName().getOrig())
                .as("user names")
                .containsExactlyInAnyOrder("atestuserX00003");
    }

    @Test
    public void test990searchUsingRefSubfilter() throws Exception {
        OperationResult result = new OperationResult("search");
        var role = repositoryService.addObject(new RoleType().name("Managed Role").asPrismObject(), null, result);
        var correctOrgUnit = repositoryService.addObject(new OrgType().name("foo-bar-baz").asPrismObject(), null, result);
        var incorrectUser = repositoryService.
                addObject(new UserType()
                                .name("incorrect-user")
                                .assignment(new AssignmentType()
                                        .targetRef(role, RoleType.COMPLEX_TYPE, SchemaConstants.ORG_MANAGER))
                                .assignment(new AssignmentType()
                                        .targetRef(correctOrgUnit, OrgType.COMPLEX_TYPE, SchemaConstants.ORG_DEFAULT)
                                )
                                .asPrismObject(),
                null,result);
        var correctUser = repositoryService.
                addObject(new UserType()
                                .name("correct-user")
                                .assignment(new AssignmentType()
                                        .targetRef(role, RoleType.COMPLEX_TYPE, SchemaConstants.ORG_DEFAULT)
                                )
                                .assignment(new AssignmentType()
                                        .targetRef(correctOrgUnit, OrgType.COMPLEX_TYPE, SchemaConstants.ORG_MANAGER)
                                )
                                .asPrismObject(),
                        null,result);

        var query = TypedQuery.parse(UserType.class,
                "assignment/targetRef matches (relation = manager and @ matches (. type OrgType and name contains '-'))");

        var users = repositoryService.searchObjects(query.getType(), query.toObjectQuery(), null, result);

        assertThat(users)
                .as("users found")
                .hasSize(1)
                .map(u -> u.getName().getOrig())
                .containsExactlyInAnyOrder("correct-user");

    }

    @Test
    public void test999MultipleOrdersAreSupportedByFluentApiAndRepository() throws SchemaException {
        given("search users query ordered by family and given name");
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .asc(UserType.F_GIVEN_NAME).desc(UserType.F_FAMILY_NAME)
                .build();

        when("the query is executed");
        OperationResult opResult = createOperationResult();
        List<PrismObject<UserType>> result = repositoryService.searchObjects(UserType.class, query, null, opResult);

        then("sorted users are returned");
        assertThatOperationResult(opResult).isSuccess();
        // Previous test added two new users
        assertThat(result).hasSize(6);
        // Various DB can use various order (default NULL ordering for H2 vs PG) so we just check it works
        // without actually checking the order (we do this properly in the Native repo test).
    }
}
