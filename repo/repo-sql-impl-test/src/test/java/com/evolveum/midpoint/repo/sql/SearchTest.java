/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql;

import static java.util.Collections.emptySet;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.repo.api.RepoModifyOptions.createExecuteIfNoChanges;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType.F_NAME;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.xml.namespace.QName;

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
public class SearchTest extends BaseSQLRepoTest {

    private static final String DESCRIPTION_TO_FIND = "tralala";
    private static final String ARCHETYPE1_OID = "a71e48fe-f6e2-40f4-ab76-b4ad4a0918ad";

    private String beforeConfigOid;

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

        for (PrismObject object : objects) {
            //noinspection unchecked
            repositoryService.addObject(object, null, result);
        }

        result.recomputeStatus();
        assertTrue(result.isSuccess());
    }

    @Test
    public void iterateEmptySet() throws Exception {
        OperationResult result = new OperationResult("search empty");

        ResultHandler<UserType> handler = (object, parentResult) -> {
            fail();
            return false;
        };

        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_NAME).eqPoly("asdf", "asdf").matchingStrict()
                .build();

        repositoryService.searchObjectsIterative(UserType.class, query, handler, null, false, result);
        result.recomputeStatus();

        assertTrue(result.isSuccess());
    }

    @Test
    public void iterateSet() throws Exception {
        OperationResult result = new OperationResult("search set");

        final List<PrismObject> objects = new ArrayList<>();

        ResultHandler<UserType> handler = (object, parentResult) -> {
            objects.add(object);
            return true;
        };

        repositoryService.searchObjectsIterative(UserType.class, null, handler, null, false, result);
        result.recomputeStatus();

        assertTrue(result.isSuccess());
        assertEquals(4, objects.size());
    }

    @Test
    public void iterateSetWithPaging() throws Exception {
        iterateGeneral(0, 2, 2, "atestuserX00002", "atestuserX00003");
        iterateGeneral(0, 2, 1, "atestuserX00002", "atestuserX00003");
        iterateGeneral(0, 1, 10, "atestuserX00002");
        iterateGeneral(0, 1, 1, "atestuserX00002");
        iterateGeneral(1, 1, 1, "atestuserX00003");
    }

    private void iterateGeneral(int offset, int size, int batch, final String... names) throws Exception {
        OperationResult result = new OperationResult("search general");

        final List<PrismObject> objects = new ArrayList<>();

        ResultHandler<UserType> handler = new ResultHandler<UserType>() {

            private int index = 0;

            @Override
            public boolean handle(PrismObject<UserType> object, OperationResult parentResult) {
                objects.add(object);
                assertEquals("Incorrect object name was read", names[index++], object.asObjectable().getName().getOrig());
                return true;
            }
        };

        SqlRepositoryConfiguration config = ((SqlRepositoryServiceImpl) repositoryService).getConfiguration();
        int oldBatchSize = config.getIterativeSearchByPagingBatchSize();
        config.setIterativeSearchByPagingBatchSize(batch);

        logger.trace(">>>>>> iterateGeneral: offset = " + offset + ", size = " + size + ", batch = " + batch + " <<<<<<");

        ObjectQuery query = prismContext.queryFactory().createQuery();
        query.setPaging(prismContext.queryFactory().createPaging(offset, size, ObjectType.F_NAME, OrderDirection.ASCENDING));
        repositoryService.searchObjectsIterative(UserType.class, query, handler, null, false, result);
        result.recomputeStatus();

        config.setIterativeSearchByPagingBatchSize(oldBatchSize);

        assertTrue(result.isSuccess());
        assertEquals(size, objects.size());
    }

    @Test
    public void caseSensitiveSearchTest() throws Exception {
        final String existingNameOrig = "Test UserX00003";
        final String nonExistingNameOrig = "test UserX00003";
        final String nameNorm = "test userx00003";

        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_FULL_NAME).eqPoly(existingNameOrig, nameNorm).matchingOrig()
                .build();

        OperationResult result = new OperationResult("search");
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find one user", 1, users.size());

        query = prismContext.queryFor(UserType.class)
                .item(UserType.F_FULL_NAME).eqPoly(nonExistingNameOrig, nameNorm).matchingOrig()
                .build();

        users = repositoryService.searchObjects(UserType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Found user (shouldn't) because case insensitive search was used", 0, users.size());
    }

    @Test
    public void roleMembershipSearchTest() throws Exception {
        PrismReferenceValue r456 = itemFactory().createReferenceValue("r456", RoleType.COMPLEX_TYPE);
        r456.setRelation(SchemaConstants.ORG_DEFAULT);
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_ROLE_MEMBERSHIP_REF).ref(r456)
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find one user", 1, users.size());
        assertEquals("Wrong user name", "atestuserX00003", users.get(0).getName().getOrig());

        PrismReferenceValue r123 = itemFactory().createReferenceValue("r123", RoleType.COMPLEX_TYPE);
        query = prismContext.queryFor(UserType.class)
                .item(UserType.F_ROLE_MEMBERSHIP_REF).ref(r123)
                .build();
        users = repositoryService.searchObjects(UserType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find two users", 2, users.size());

        PrismReferenceValue r123approver = itemFactory().createReferenceValue("r123", RoleType.COMPLEX_TYPE);
        r123approver.setRelation(SchemaConstants.ORG_APPROVER);
        query = prismContext.queryFor(UserType.class)
                .item(UserType.F_ROLE_MEMBERSHIP_REF).ref(r123approver)
                .build();
        users = repositoryService.searchObjects(UserType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find no users", 0, users.size());
    }

    @Test
    public void delegatedSearchTest() throws Exception {
        PrismReferenceValue r789 = itemFactory().createReferenceValue("r789", RoleType.COMPLEX_TYPE);
        // intentionally without relation (meaning "member")
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_DELEGATED_REF).ref(r789)
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find one user", 1, users.size());
        assertEquals("Wrong user name", "atestuserX00003", users.get(0).getName().getOrig());

        PrismReferenceValue r123 = itemFactory().createReferenceValue("r123", RoleType.COMPLEX_TYPE);
        query = prismContext.queryFor(UserType.class)
                .item(UserType.F_DELEGATED_REF).ref(r123)
                .build();
        users = repositoryService.searchObjects(UserType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find no users", 0, users.size());
    }

    @Test
    public void personaSearchTest() throws Exception {
        PrismReferenceValue u000 = itemFactory().createReferenceValue("u000", UserType.COMPLEX_TYPE);
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_PERSONA_REF).ref(u000)
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find one user", 1, users.size());
        assertEquals("Wrong user name", "atestuserX00003", users.get(0).getName().getOrig());

        PrismReferenceValue r789 = itemFactory().createReferenceValue("r789", RoleType.COMPLEX_TYPE);
        query = prismContext.queryFor(UserType.class)
                .item(UserType.F_PERSONA_REF).ref(r789)
                .build();
        users = repositoryService.searchObjects(UserType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find no users", 0, users.size());
    }

    @Test
    public void assignmentOrgRefSearchTest() throws Exception {
        PrismReferenceValue o123456 = itemFactory().createReferenceValue("o123456", OrgType.COMPLEX_TYPE);
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, AssignmentType.F_ORG_REF).ref(o123456)
                .build();

        OperationResult result = new OperationResult("search");
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find one user", 1, users.size());
        assertEquals("Wrong user name", "atestuserX00002", users.get(0).getName().getOrig());

        PrismReferenceValue o999 = itemFactory().createReferenceValue("o999", RoleType.COMPLEX_TYPE);
        query = prismContext.queryFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, AssignmentType.F_ORG_REF).ref(o999)
                .build();

        users = repositoryService.searchObjects(UserType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find zero users", 0, users.size());
    }

    @Test
    public void assignmentResourceRefSearchTest() throws Exception {
        PrismReferenceValue resourceRef = itemFactory().createReferenceValue("10000000-0000-0000-0000-000000000004", ResourceType.COMPLEX_TYPE);
        ObjectQuery query = prismContext.queryFor(RoleType.class)
                .item(RoleType.F_ASSIGNMENT, AssignmentType.F_CONSTRUCTION, ConstructionType.F_RESOURCE_REF).ref(resourceRef)
                .build();

        OperationResult result = new OperationResult("search");
        List<PrismObject<RoleType>> roles = repositoryService.searchObjects(RoleType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find one role", 1, roles.size());
        assertEquals("Wrong role name", "Judge", roles.get(0).getName().getOrig());

        PrismReferenceValue resourceRef2 = itemFactory().createReferenceValue("FFFFFFFF-0000-0000-0000-000000000004", ResourceType.COMPLEX_TYPE);
        query = prismContext.queryFor(RoleType.class)
                .item(RoleType.F_ASSIGNMENT, AssignmentType.F_CONSTRUCTION, ConstructionType.F_RESOURCE_REF).ref(resourceRef2)
                .build();
        roles = repositoryService.searchObjects(RoleType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find zero roles", 0, roles.size());
    }

    @Test
    public void roleAssignmentSearchTest() throws Exception {
        PrismReferenceValue r456 = itemFactory().createReferenceValue("r123", RoleType.COMPLEX_TYPE);
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref(r456)
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find one user", 1, users.size());
        assertEquals("Wrong user name", "atestuserX00002", users.get(0).getName().getOrig());

    }

    @Test
    public void orgAssignmentSearchTest() throws Exception {
        PrismReferenceValue org = itemFactory().createReferenceValue("00000000-8888-6666-0000-100000000085", OrgType.COMPLEX_TYPE);
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref(org)
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find one user", 1, users.size());
        assertEquals("Wrong user name", "atestuserX00002", users.get(0).getName().getOrig());

    }

    @Test
    public void orgAssignmentSearchTestNoTargetType() throws Exception {
        PrismReferenceValue org = itemFactory().createReferenceValue("00000000-8888-6666-0000-100000000085", null);
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref(org)
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find one user", 1, users.size());
        assertEquals("Wrong user name", "atestuserX00002", users.get(0).getName().getOrig());
    }

    @Test
    public void orgAssignmentSearchTestByOid() throws Exception {
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref("00000000-8888-6666-0000-100000000085")
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find one user", 1, users.size());
        assertEquals("Wrong user name", "atestuserX00002", users.get(0).getName().getOrig());
        System.out.println("Found user:\n" + users.get(0).debugDump());
    }

    @Test
    public void roleAndOrgAssignmentSearchTest() throws Exception {
        PrismReferenceValue r123 = itemFactory().createReferenceValue("r123", RoleType.COMPLEX_TYPE);
        PrismReferenceValue org = itemFactory().createReferenceValue("00000000-8888-6666-0000-100000000085", OrgType.COMPLEX_TYPE);
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref(r123)
                .and().item(UserType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref(org)
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find one user", 1, users.size());
        assertEquals("Wrong user name", "atestuserX00002", users.get(0).getName().getOrig());

    }

    @Test
    public void notBusinessRoleTypeSearchTest() throws Exception {
        ObjectQuery query = prismContext.queryFor(RoleType.class)
                .not().item(RoleType.F_ROLE_TYPE).eq("business")
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<RoleType>> roles = repositoryService.searchObjects(RoleType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find two roles", 2, roles.size());

        int judge = roles.get(0).getName().getOrig().startsWith("J") ? 0 : 1;
        assertEquals("Wrong role1 name", "Judge", roles.get(judge).getName().getOrig());
        assertEquals("Wrong role2 name", "Admin-owned role", roles.get(1 - judge).getName().getOrig());
    }

    @Test
    public void businessRoleTypeSearchTest() throws Exception {
        ObjectQuery query = prismContext.queryFor(RoleType.class)
                .item(RoleType.F_ROLE_TYPE).eq("business")
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<RoleType>> roles = repositoryService.searchObjects(RoleType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find one role", 1, roles.size());
        assertEquals("Wrong role name", "Pirate", roles.get(0).getName().getOrig());

    }

    @Test
    public void emptyRoleTypeSearchTest() throws Exception {
        ObjectQuery query = prismContext.queryFor(RoleType.class)
                .item(RoleType.F_ROLE_TYPE).isNull()
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<RoleType>> roles = repositoryService.searchObjects(RoleType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find two roles", 2, roles.size());

        int judge = roles.get(0).getName().getOrig().startsWith("J") ? 0 : 1;
        assertEquals("Wrong role1 name", "Judge", roles.get(judge).getName().getOrig());
        assertEquals("Wrong role2 name", "Admin-owned role", roles.get(1 - judge).getName().getOrig());
    }

    @Test
    public void nonEmptyRoleTypeSearchTest() throws Exception {
        ObjectQuery query = prismContext.queryFor(RoleType.class)
                .not().item(RoleType.F_ROLE_TYPE).isNull()
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<RoleType>> roles = repositoryService.searchObjects(RoleType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find one role", 1, roles.size());
        assertEquals("Wrong role name", "Pirate", roles.get(0).getName().getOrig());

    }

    @Test
    public void testIndividualOwnerRef() throws Exception {
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
    public void testOwnerRefWithTypeRestriction() throws Exception {
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
    public void testWildOwnerRef() throws SchemaException {
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
    public void testResourceUp() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(ResourceType.class)
                .item(ResourceType.F_OPERATIONAL_STATE, OperationalStateType.F_LAST_AVAILABILITY_STATUS).eq(AvailabilityStatusType.UP)
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<ResourceType>> resources = repositoryService.searchObjects(ResourceType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find one resource", 1, resources.size());
    }

    @Test
    public void testMultivaluedExtensionPropertySubstringQualified() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(GenericObjectType.class)
                .item(ObjectType.F_EXTENSION, new QName("http://example.com/p", "multivalued")).contains("slava")
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<GenericObjectType>> resources = repositoryService.searchObjects(GenericObjectType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find one object", 1, resources.size());
    }

    @Test
    public void testMultivaluedExtensionPropertyEqualsQualified() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(GenericObjectType.class)
                .item(ObjectType.F_EXTENSION, new QName("http://example.com/p", "multivalued")).eq("Bratislava")
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<GenericObjectType>> resources = repositoryService.searchObjects(GenericObjectType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find one object", 1, resources.size());
    }

    @Test
    public void testMultivaluedExtensionPropertySubstringUnqualified() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(GenericObjectType.class)
                .item(ObjectType.F_EXTENSION, new QName("multivalued")).contains("slava")
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<GenericObjectType>> resources = repositoryService.searchObjects(GenericObjectType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find one object", 1, resources.size());
    }

    @Test
    public void testMultivaluedExtensionPropertyEqualsUnqualified() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(GenericObjectType.class)
                .item(ObjectType.F_EXTENSION, new QName("multivalued")).eq("Bratislava")
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<GenericObjectType>> resources = repositoryService.searchObjects(GenericObjectType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find one object", 1, resources.size());
    }

    @Test
    public void testRoleAttributes() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(RoleType.class)
                .item(RoleType.F_RISK_LEVEL).eq("critical")
                .and().item(RoleType.F_IDENTIFIER).eq("123")
                .and().item(RoleType.F_DISPLAY_NAME).eqPoly("The honest one", "").matchingOrig()
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<RoleType>> roles = repositoryService.searchObjects(RoleType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find one object", 1, roles.size());
    }

    // testing MID-3568
    @Test
    public void caseInsensitiveSearchTest() throws Exception {
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
    public void fullTextSearch() throws Exception {

        OperationResult result = new OperationResult("fullTextSearch");

        Collection<SelectorOptions<GetOperationOptions>> distinct = distinct();

        assertUsersFound(prismContext.queryFor(UserType.class)
                        .fullText("atestuserX00003")
                        .build(),
                false, 1);

        List<PrismObject<UserType>> users = assertUsersFound(prismContext.queryFor(UserType.class)
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
                true, 1);       // MID-4590

        assertUsersFoundBySearch(prismContext.queryFor(UserType.class)
                        .fullText("sollicitudin")
                        .asc(UserType.F_FULL_NAME)
                        .maxSize(100)
                        .build(),
                distinct, 1);
    }

    @Test // MID-4932
    public void fullTextSearchModify() throws Exception {

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
    public void reindex() throws Exception {

        OperationResult result = new OperationResult("reindex");

        assertUsersFound(prismContext.queryFor(UserType.class)
                        .fullText(DESCRIPTION_TO_FIND)
                        .build(),
                false, 0);
        repositoryService.modifyObject(UserType.class, beforeConfigOid, emptySet(), createExecuteIfNoChanges(), result);
        assertUsersFound(prismContext.queryFor(UserType.class)
                        .fullText(DESCRIPTION_TO_FIND)
                        .build(),
                false, 1);
    }

    private Collection<SelectorOptions<GetOperationOptions>> distinct() {
        return SelectorOptions.createCollection(GetOperationOptions.createDistinct());
    }

    @Test
    public void testShadowPendingOperation() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(ShadowType.class)
                .exists(ShadowType.F_PENDING_OPERATION)
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<ShadowType>> shadows = repositoryService.searchObjects(ShadowType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find one object", 1, shadows.size());
    }

    @SuppressWarnings("SameParameterValue")
    private List<PrismObject<UserType>> assertUsersFound(ObjectQuery query, boolean distinct, int expectedCount) throws Exception {
        Collection<SelectorOptions<GetOperationOptions>> options = distinct ? distinct() : null;
        assertObjectsFoundByCount(query, options, expectedCount);
        return assertUsersFoundBySearch(query, options, expectedCount);
    }

    private List<PrismObject<UserType>> assertUsersFoundBySearch(ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, int expectedCount) throws Exception {
        OperationResult result = new OperationResult("search");
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, options, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Wrong # of results found: " + query, expectedCount, users.size());
        return users;
    }

    private void assertObjectsFoundByCount(ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options,
            int expectedCount) throws Exception {
        OperationResult result = new OperationResult("count");
        int count = repositoryService.countObjects(UserType.class, query, options, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Wrong # of results found: " + query, expectedCount, count);
    }

    @Test
    public void testOperationExecutionAny() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(CaseType.class)
                .item(ObjectType.F_OPERATION_EXECUTION, OperationExecutionType.F_STATUS).eq(OperationResultStatusType.FATAL_ERROR)
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<CaseType>> cases = repositoryService.searchObjects(CaseType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find one object", 1, cases.size());
    }

    @Test
    public void testOperationExecutionWithTask() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(CaseType.class)
                .exists(ObjectType.F_OPERATION_EXECUTION)
                .block()
                .item(OperationExecutionType.F_TASK_REF).ref("task-oid-2")
                .and().item(OperationExecutionType.F_STATUS).eq(OperationResultStatusType.SUCCESS)
                .endBlock()
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<CaseType>> cases = repositoryService.searchObjects(CaseType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find one object", 1, cases.size());
    }

    @Test
    public void testOperationExecutionWithTask2() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(CaseType.class)
                .exists(ObjectType.F_OPERATION_EXECUTION)
                .block()
                .item(OperationExecutionType.F_TASK_REF).ref("task-oid-2")
                .and().item(OperationExecutionType.F_STATUS).eq(OperationResultStatusType.FATAL_ERROR)
                .endBlock()
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<CaseType>> cases = repositoryService.searchObjects(CaseType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find no object", 0, cases.size());
    }

    @Test
    public void testExtensionReference() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(GenericObjectType.class)
                .item(ObjectType.F_EXTENSION, new QName("referenceType"))
                .ref("12345678-1234-1234-1234-123456789012")
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<GenericObjectType>> cases = repositoryService.searchObjects(GenericObjectType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find 1 object", 1, cases.size());
    }

    @Test
    public void testExtensionReferenceNotMatching() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(GenericObjectType.class)
                .item(ObjectType.F_EXTENSION, new QName("referenceType"))
                .ref("12345678-1234-1234-1234-123456789xxx")
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<GenericObjectType>> cases = repositoryService.searchObjects(GenericObjectType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find no object", 0, cases.size());
    }

    @Test
    public void testExtensionReferenceNull() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(GenericObjectType.class)
                .item(ObjectType.F_EXTENSION, new QName("referenceType"))
                .isNull()
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<GenericObjectType>> cases = repositoryService.searchObjects(GenericObjectType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find no object", 0, cases.size());
    }

    @Test
    public void testExtensionReferenceNonNull() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(GenericObjectType.class)
                .not().item(ObjectType.F_EXTENSION, new QName("referenceType"))
                .isNull()
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<GenericObjectType>> cases = repositoryService.searchObjects(GenericObjectType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find 1 object", 1, cases.size());
    }

    @Test
    public void testObjectCollection() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(ObjectCollectionType.class)
                .item(ObjectType.F_NAME).eqPoly("collection1", "collection1").matchingOrig()
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<ObjectCollectionType>> collections = repositoryService.searchObjects(ObjectCollectionType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find 1 object", 1, collections.size());
    }

    @Test
    public void testAllObjectCollections() throws SchemaException {
        OperationResult result = new OperationResult("search");
        List<PrismObject<ObjectCollectionType>> collections = repositoryService.searchObjects(ObjectCollectionType.class, null, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find 1 object", 1, collections.size());
    }

    @Test
    public void testFunctionLibrary() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(FunctionLibraryType.class)
                .item(ObjectType.F_NAME).eqPoly("fl1", "fl1").matchingOrig()
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<FunctionLibraryType>> collections = repositoryService.searchObjects(FunctionLibraryType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find 1 object", 1, collections.size());
    }

    @Test
    public void testArchetype() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(ArchetypeType.class)
                .item(ObjectType.F_NAME).eqPoly("archetype1", "archetype1").matchingOrig()
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<ArchetypeType>> collections = repositoryService.searchObjects(ArchetypeType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find 1 object", 1, collections.size());
    }

    @Test
    public void testSearchAssignmentHoldersByArchetypeRef() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(AssignmentHolderType.class)
                .item(AssignmentHolderType.F_ARCHETYPE_REF).ref(ARCHETYPE1_OID)
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<AssignmentHolderType>> objects = repositoryService.searchObjects(AssignmentHolderType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        System.out.println("Objects found: " + objects);
        assertEquals("Should find 3 objects", 3, objects.size());
    }

    @Test
    public void testSearchObjectsByArchetypeRef() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(AssignmentHolderType.class)
                .item(AssignmentHolderType.F_ARCHETYPE_REF).ref(ARCHETYPE1_OID)
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<ObjectType>> objects = repositoryService.searchObjects(ObjectType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        System.out.println("Objects found: " + objects);
        assertEquals("Should find 3 objects", 3, objects.size());
    }

    @Test
    public void testSearchUsersByArchetypeRef() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(AssignmentHolderType.F_ARCHETYPE_REF).ref(ARCHETYPE1_OID)
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<UserType>> objects = repositoryService.searchObjects(UserType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        System.out.println("Users found: " + objects);
        assertEquals("Should find 2 objects", 2, objects.size());
    }

    @Test
    public void testSearchTasksByArchetypeRef() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(TaskType.class)
                .item(AssignmentHolderType.F_ARCHETYPE_REF).ref(ARCHETYPE1_OID)
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<TaskType>> objects = repositoryService.searchObjects(TaskType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        System.out.println("Tasks found: " + objects);
        assertEquals("Should find 1 object", 1, objects.size());
    }

    @Test
    public void testSearchFocus() throws SchemaException {
        OperationResult result = new OperationResult("search");
        List<PrismObject<FocusType>> objects = repositoryService.searchObjects(FocusType.class, null, null, result);
        result.recomputeStatus();
        assertSuccess(result);
        System.out.println("Objects found: " + objects);
        for (PrismObject<?> object : objects) {
            assertTrue("returned object is not a FocusType: " + object, object.asObjectable() instanceof FocusType);
        }
    }

    @Test
    public void testSearchAssignmentHolder() throws SchemaException {
        OperationResult result = new OperationResult("search");
        List<PrismObject<AssignmentHolderType>> objects = repositoryService.searchObjects(AssignmentHolderType.class, null, null, result);
        result.recomputeStatus();
        assertSuccess(result);
        System.out.println("Objects found: " + objects);
        for (PrismObject<?> object : objects) {
            assertTrue("returned object is not a AssignmentHolderType: " + object, object.asObjectable() instanceof AssignmentHolderType);
        }
    }

    @Test
    public void testCaseWorkItemAssignee() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(CaseType.class)
                .item(CaseType.F_WORK_ITEM, CaseWorkItemType.F_ASSIGNEE_REF).ref("5905f321-630f-4de3-abc9-ba3a614aac36")
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<CaseType>> cases = repositoryService.searchObjects(CaseType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find one object", 1, cases.size());
    }

    @Test
    public void testCaseWorkItemCandidate() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(CaseType.class)
                .item(CaseType.F_WORK_ITEM, CaseWorkItemType.F_CANDIDATE_REF).ref("5905f321-630f-4de3-abc9-ba3a614aac36")
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<CaseType>> cases = repositoryService.searchObjects(CaseType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find one object", 1, cases.size());
    }

    @Test
    public void testCaseWorkItemCandidateOther() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(CaseType.class)
                .item(CaseType.F_WORK_ITEM, CaseWorkItemType.F_CANDIDATE_REF).ref("d2bda14f-8571-4c99-bbe4-25c132650998")
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<CaseType>> cases = repositoryService.searchObjects(CaseType.class, query,
                schemaHelper.getOperationOptionsBuilder().distinct().build(), result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find one object", 1, cases.size());
    }

    @Test
    public void testWorkItemCandidate() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(CaseWorkItemType.class)
                .item(CaseWorkItemType.F_CANDIDATE_REF).ref("d2bda14f-8571-4c99-bbe4-25c132650998")
                .build();
        OperationResult result = new OperationResult("search");
        SearchResultList<CaseWorkItemType> workItems = repositoryService.searchContainers(CaseWorkItemType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Wrong # of work items found", 2, workItems.size());
    }

    @Test
    public void testWorkItemCandidateOther() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(CaseWorkItemType.class)
                .item(CaseWorkItemType.F_CANDIDATE_REF).ref("5905f321-630f-4de3-abc9-ba3a614aac36")
                .build();
        OperationResult result = new OperationResult("search");
        SearchResultList<CaseWorkItemType> workItems = repositoryService.searchContainers(CaseWorkItemType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Wrong # of work items found", 1, workItems.size());
    }

    @Test
    public void testWorkItemWithCreateTimestamp() throws SchemaException {
        ObjectQuery query = prismContext.queryFor(CaseWorkItemType.class)
                .not().item(CaseWorkItemType.F_CREATE_TIMESTAMP).isNull()
                .build();
        OperationResult result = new OperationResult("search");
        SearchResultList<CaseWorkItemType> workItems = repositoryService.searchContainers(CaseWorkItemType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Wrong # of work items found", 1, workItems.size());
        assertNotNull("Null createTimestamp", workItems.get(0).getCreateTimestamp());
    }

    /**
     * See MID-5474 (just a quick attempt to replicate)
     */
    @Test
    public void iterateAndModify() throws Exception {
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
        result.recomputeStatus();

        assertTrue(result.isSuccess());
        assertEquals(1, count.get());
    }

    // MID-5515
    @Test
    public void testSearchNameNull() throws Exception {
        OperationResult result = new OperationResult("testSearchNameNull");
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(F_NAME).isNull()
                .build();

        SearchResultList<PrismObject<UserType>> objects = repositoryService.searchObjects(UserType.class, query, null, result);
        result.recomputeStatus();

        assertTrue(result.isSuccess());
        assertEquals("Wrong # of objects found", 0, objects.size());
    }

    // MID-5515
    @Test
    public void testSearchNameNotNull() throws Exception {
        OperationResult result = new OperationResult("testSearchNameNotNull");
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .not().item(F_NAME).isNull()
                .build();

        SearchResultList<PrismObject<UserType>> objects = repositoryService.searchObjects(UserType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());

        int users = repositoryService.countObjects(UserType.class, null, null, result);
        assertEquals("Wrong # of objects found", users, objects.size());
    }
}
