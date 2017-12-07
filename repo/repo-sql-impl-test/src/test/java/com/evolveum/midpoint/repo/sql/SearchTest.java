/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import java.io.File;
import java.util.*;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class SearchTest extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(SearchTest.class);

    @BeforeClass
    public void beforeClass() throws Exception {
        super.beforeClass();

        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);

		FullTextSearchConfigurationType fullTextConfig = new FullTextSearchConfigurationType();
		FullTextSearchIndexedItemsConfigurationType entry = new FullTextSearchIndexedItemsConfigurationType();
		entry.getItem().add(new ItemPath(ObjectType.F_NAME).asItemPathType());
		entry.getItem().add(new ItemPath(ObjectType.F_DESCRIPTION).asItemPathType());
		fullTextConfig.getIndexed().add(entry);
		repositoryService.applyFullTextSearchConfiguration(fullTextConfig);
		LOGGER.info("Applying full text search configuration: {}", fullTextConfig);

		List<PrismObject<? extends Objectable>> objects = prismContext.parserFor(new File(FOLDER_BASIC, "objects.xml")).parseObjects();
        objects.addAll(prismContext.parserFor(new File(FOLDER_BASIC, "objects-2.xml")).parseObjects());

        OperationResult result = new OperationResult("add objects");
        for (PrismObject object : objects) {
            repositoryService.addObject(object, null, result);
        }

        result.recomputeStatus();
        assertTrue(result.isSuccess());
    }

    @Test
    public void iterateEmptySet() throws Exception {
        OperationResult result = new OperationResult("search empty");

        ResultHandler handler = (object, parentResult) -> {
			fail();
			return false;
		};

        ObjectQuery query = QueryBuilder.queryFor(UserType.class, prismContext)
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

        ResultHandler handler = (object, parentResult) -> {
			objects.add(object);
			return true;
		};

        repositoryService.searchObjectsIterative(UserType.class, null, handler, null, false, result);
        result.recomputeStatus();

        assertTrue(result.isSuccess());
        assertEquals(3, objects.size());
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

        final List<PrismObject> objects = new ArrayList<PrismObject>();

        ResultHandler handler = new ResultHandler() {

            int index = 0;
            @Override
            public boolean handle(PrismObject object, OperationResult parentResult) {
                objects.add(object);
                assertEquals("Incorrect object name was read", names[index++], object.asObjectable().getName().getOrig());
                return true;
            }
        };

        SqlRepositoryConfiguration config = ((SqlRepositoryServiceImpl) repositoryService).getConfiguration();
        int oldbatch = config.getIterativeSearchByPagingBatchSize();
        config.setIterativeSearchByPagingBatchSize(batch);

        LOGGER.trace(">>>>>> iterateGeneral: offset = " + offset + ", size = " + size + ", batch = " + batch + " <<<<<<");

        ObjectQuery query = new ObjectQuery();
        query.setPaging(ObjectPaging.createPaging(offset, size, ObjectType.F_NAME, OrderDirection.ASCENDING));
        repositoryService.searchObjectsIterative(UserType.class, query, handler, null, false, result);
        result.recomputeStatus();

        config.setIterativeSearchByPagingBatchSize(oldbatch);

        assertTrue(result.isSuccess());
        assertEquals(size, objects.size());
    }

    @Test
    public void caseSensitiveSearchTest() throws Exception {
        final String existingNameOrig = "Test UserX00003";
        final String nonExistingNameOrig = "test UserX00003";
        final String nameNorm = "test userx00003";

        ObjectQuery query = QueryBuilder.queryFor(UserType.class, prismContext)
                .item(UserType.F_FULL_NAME).eqPoly(existingNameOrig, nameNorm).matchingOrig()
                .build();

        OperationResult result = new OperationResult("search");
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find one user", 1, users.size());

        query = QueryBuilder.queryFor(UserType.class, prismContext)
                .item(UserType.F_FULL_NAME).eqPoly(nonExistingNameOrig, nameNorm).matchingOrig()
                .build();

        users = repositoryService.searchObjects(UserType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Found user (shouldn't) because case insensitive search was used", 0, users.size());
    }

    @Test
    public void roleMembershipSearchTest() throws Exception {
        PrismReferenceValue r456 = new PrismReferenceValue("r456", RoleType.COMPLEX_TYPE);
        r456.setRelation(SchemaConstants.ORG_DEFAULT);
        ObjectQuery query = QueryBuilder.queryFor(UserType.class, prismContext)
                .item(UserType.F_ROLE_MEMBERSHIP_REF).ref(r456)
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find one user", 1, users.size());
        assertEquals("Wrong user name", "atestuserX00003", users.get(0).getName().getOrig());

        PrismReferenceValue r123 = new PrismReferenceValue("r123", RoleType.COMPLEX_TYPE);
        query = QueryBuilder.queryFor(UserType.class, prismContext)
                .item(UserType.F_ROLE_MEMBERSHIP_REF).ref(r123)
                .build();
        users = repositoryService.searchObjects(UserType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find two users", 2, users.size());

        PrismReferenceValue r123approver = new PrismReferenceValue("r123", RoleType.COMPLEX_TYPE);
        r123approver.setRelation(SchemaConstants.ORG_APPROVER);
        query = QueryBuilder.queryFor(UserType.class, prismContext)
                .item(UserType.F_ROLE_MEMBERSHIP_REF).ref(r123approver)
                .build();
        users = repositoryService.searchObjects(UserType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find no users", 0, users.size());
    }

    @Test
    public void delegatedSearchTest() throws Exception {
        PrismReferenceValue r789 = new PrismReferenceValue("r789", RoleType.COMPLEX_TYPE);
        // intentionally without relation (meaning "member")
        ObjectQuery query = QueryBuilder.queryFor(UserType.class, prismContext)
                .item(UserType.F_DELEGATED_REF).ref(r789)
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find one user", 1, users.size());
        assertEquals("Wrong user name", "atestuserX00003", users.get(0).getName().getOrig());

        PrismReferenceValue r123 = new PrismReferenceValue("r123", RoleType.COMPLEX_TYPE);
        query = QueryBuilder.queryFor(UserType.class, prismContext)
                .item(UserType.F_DELEGATED_REF).ref(r123)
                .build();
        users = repositoryService.searchObjects(UserType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find no users", 0, users.size());
    }

    @Test
    public void personaSearchTest() throws Exception {
        PrismReferenceValue u000 = new PrismReferenceValue("u000", UserType.COMPLEX_TYPE);
        ObjectQuery query = QueryBuilder.queryFor(UserType.class, prismContext)
                .item(UserType.F_PERSONA_REF).ref(u000)
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find one user", 1, users.size());
        assertEquals("Wrong user name", "atestuserX00003", users.get(0).getName().getOrig());

		PrismReferenceValue r789 = new PrismReferenceValue("r789", RoleType.COMPLEX_TYPE);
        query = QueryBuilder.queryFor(UserType.class, prismContext)
                .item(UserType.F_PERSONA_REF).ref(r789)
                .build();
        users = repositoryService.searchObjects(UserType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find no users", 0, users.size());
    }

    @Test
    public void assignmentOrgRefSearchTest() throws Exception {
        PrismReferenceValue o123456 = new PrismReferenceValue("o123456", OrgType.COMPLEX_TYPE);
        ObjectQuery query = QueryBuilder.queryFor(UserType.class, prismContext)
                .item(UserType.F_ASSIGNMENT, AssignmentType.F_ORG_REF).ref(o123456)
                .build();

        OperationResult result = new OperationResult("search");
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find one user", 1, users.size());
        assertEquals("Wrong user name", "atestuserX00002", users.get(0).getName().getOrig());

        PrismReferenceValue o999 = new PrismReferenceValue("o999", RoleType.COMPLEX_TYPE);
        query = QueryBuilder.queryFor(UserType.class, prismContext)
                .item(UserType.F_ASSIGNMENT, AssignmentType.F_ORG_REF).ref(o999)
                .build();

        users = repositoryService.searchObjects(UserType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find zero users", 0, users.size());
    }

    @Test
    public void assignmentResourceRefSearchTest() throws Exception {
        PrismReferenceValue resourceRef = new PrismReferenceValue("10000000-0000-0000-0000-000000000004", ResourceType.COMPLEX_TYPE);
        ObjectQuery query = QueryBuilder.queryFor(RoleType.class, prismContext)
                .item(RoleType.F_ASSIGNMENT, AssignmentType.F_CONSTRUCTION, ConstructionType.F_RESOURCE_REF).ref(resourceRef)
                .build();

        OperationResult result = new OperationResult("search");
        List<PrismObject<RoleType>> roles = repositoryService.searchObjects(RoleType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find one role", 1, roles.size());
        assertEquals("Wrong role name", "Judge", roles.get(0).getName().getOrig());

        PrismReferenceValue resourceRef2 = new PrismReferenceValue("FFFFFFFF-0000-0000-0000-000000000004", ResourceType.COMPLEX_TYPE);
        query = QueryBuilder.queryFor(RoleType.class, prismContext)
                .item(RoleType.F_ASSIGNMENT, AssignmentType.F_CONSTRUCTION, ConstructionType.F_RESOURCE_REF).ref(resourceRef2)
                .build();
        roles = repositoryService.searchObjects(RoleType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find zero roles", 0, roles.size());
    }

    @Test
    public void roleAssignmentSearchTest() throws Exception {
        PrismReferenceValue r456 = new PrismReferenceValue("r123", RoleType.COMPLEX_TYPE);
        ObjectQuery query = QueryBuilder.queryFor(UserType.class, prismContext)
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
        PrismReferenceValue org = new PrismReferenceValue("00000000-8888-6666-0000-100000000085", OrgType.COMPLEX_TYPE);
        ObjectQuery query = QueryBuilder.queryFor(UserType.class, prismContext)
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
        PrismReferenceValue org = new PrismReferenceValue("00000000-8888-6666-0000-100000000085", null);
        ObjectQuery query = QueryBuilder.queryFor(UserType.class, prismContext)
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
        ObjectQuery query = QueryBuilder.queryFor(UserType.class, prismContext)
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
        PrismReferenceValue r123 = new PrismReferenceValue("r123", RoleType.COMPLEX_TYPE);
        PrismReferenceValue org = new PrismReferenceValue("00000000-8888-6666-0000-100000000085", OrgType.COMPLEX_TYPE);
        ObjectQuery query = QueryBuilder.queryFor(UserType.class, prismContext)
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
        ObjectQuery query = QueryBuilder.queryFor(RoleType.class, prismContext)
                .not().item(RoleType.F_ROLE_TYPE).eq("business")
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<RoleType>> roles = repositoryService.searchObjects(RoleType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find two roles", 2, roles.size());

        int judge = roles.get(0).getName().getOrig().startsWith("J") ? 0 : 1;
        assertEquals("Wrong role1 name", "Judge", roles.get(judge).getName().getOrig());
        assertEquals("Wrong role2 name", "Admin-owned role", roles.get(1-judge).getName().getOrig());
    }

    @Test
    public void businessRoleTypeSearchTest() throws Exception {
        ObjectQuery query = QueryBuilder.queryFor(RoleType.class, prismContext)
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
        ObjectQuery query = QueryBuilder.queryFor(RoleType.class, prismContext)
                .item(RoleType.F_ROLE_TYPE).isNull()
                .build();
        OperationResult result = new OperationResult("search");
        List<PrismObject<RoleType>> roles = repositoryService.searchObjects(RoleType.class, query, null, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
        assertEquals("Should find two roles", 2, roles.size());

        int judge = roles.get(0).getName().getOrig().startsWith("J") ? 0 : 1;
        assertEquals("Wrong role1 name", "Judge", roles.get(judge).getName().getOrig());
        assertEquals("Wrong role2 name", "Admin-owned role", roles.get(1-judge).getName().getOrig());
    }

    @Test
    public void nonEmptyRoleTypeSearchTest() throws Exception {
        ObjectQuery query = QueryBuilder.queryFor(RoleType.class, prismContext)
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
        testOwnerRef(RoleType.class, SystemObjectsType.USER_ADMINISTRATOR.value(), "Admin-owned role");
        testOwnerRef(RoleType.class, null, "Judge", "Pirate");
        testOwnerRef(RoleType.class, "123");

        testOwnerRef(OrgType.class, SystemObjectsType.USER_ADMINISTRATOR.value(), "Admin-owned org");
        testOwnerRef(OrgType.class, null, "F0085");
        testOwnerRef(OrgType.class, "123");

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
        testOwnerRefWithTypeRestriction(RoleType.class, SystemObjectsType.USER_ADMINISTRATOR.value(), "Admin-owned role");
        testOwnerRefWithTypeRestriction(RoleType.class, null, "Judge", "Pirate");
        testOwnerRefWithTypeRestriction(RoleType.class, "123");

        testOwnerRefWithTypeRestriction(OrgType.class, SystemObjectsType.USER_ADMINISTRATOR.value(), "Admin-owned org");
        testOwnerRefWithTypeRestriction(OrgType.class, null, "F0085");
        testOwnerRefWithTypeRestriction(OrgType.class, "123");

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
        ObjectQuery query = QueryBuilder.queryFor(clazz, prismContext)
                .item(new QName(SchemaConstants.NS_C, "ownerRef")).ref(oid)
                .build();
        checkResult(clazz, clazz, oid, query, names);
    }

    private void testOwnerRefWithTypeRestriction(Class<? extends ObjectType> clazz, String oid, String... names) throws SchemaException {
        ObjectQuery query = QueryBuilder.queryFor(ObjectType.class, prismContext)
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
        ItemDefinition<?> ownerRefDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(RoleType.class).findItemDefinition(RoleType.F_OWNER_REF);
        ObjectQuery query = QueryBuilder.queryFor(ObjectType.class, prismContext)
                .item(new ItemPath(new QName(SchemaConstants.NS_C, "ownerRef")), ownerRefDef).ref(oid)
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
		ObjectQuery query = QueryBuilder.queryFor(ResourceType.class, prismContext)
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
		ObjectQuery query = QueryBuilder.queryFor(GenericObjectType.class, prismContext)
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
		ObjectQuery query = QueryBuilder.queryFor(GenericObjectType.class, prismContext)
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
		ObjectQuery query = QueryBuilder.queryFor(GenericObjectType.class, prismContext)
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
		ObjectQuery query = QueryBuilder.queryFor(GenericObjectType.class, prismContext)
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
		ObjectQuery query = QueryBuilder.queryFor(RoleType.class, prismContext)
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

        assertUsersFound(QueryBuilder.queryFor(UserType.class, prismContext)
                        .item(UserType.F_FULL_NAME).eqPoly(existingNameNorm).matchingNorm()
                        .build(),
				false, 1);

        assertUsersFound(QueryBuilder.queryFor(UserType.class, prismContext)
                .item(UserType.F_FULL_NAME).eqPoly(existingNameOrig).matchingNorm()
                .build(),
				false, 1);

        assertUsersFound(QueryBuilder.queryFor(UserType.class, prismContext)
                .item(UserType.F_EMAIL_ADDRESS).eq(emailLowerCase).matchingCaseIgnore()
                .build(),
				false, 1);

        assertUsersFound(QueryBuilder.queryFor(UserType.class, prismContext)
                .item(UserType.F_EMAIL_ADDRESS).eq(emailVariousCase).matchingCaseIgnore()
                .build(),
				false, 1);

        // comparing polystrings, but providing plain String
		assertUsersFound(QueryBuilder.queryFor(UserType.class, prismContext)
						.item(UserType.F_FULL_NAME).eq(existingNameNorm).matchingNorm()
						.build(),
				false, 1);

		assertUsersFound(QueryBuilder.queryFor(UserType.class, prismContext)
						.item(UserType.F_FULL_NAME).eq(existingNameOrig).matchingNorm()
						.build(),
				false, 1);

		assertUsersFound(QueryBuilder.queryFor(UserType.class, prismContext)
                        .item(UserType.F_FULL_NAME).containsPoly(existingNameNorm).matchingNorm()
                        .build(),
				false, 1);

        assertUsersFound(QueryBuilder.queryFor(UserType.class, prismContext)
                .item(UserType.F_FULL_NAME).containsPoly(existingNameOrig).matchingNorm()
                .build(),
				false, 1);

        assertUsersFound(QueryBuilder.queryFor(UserType.class, prismContext)
                .item(UserType.F_EMAIL_ADDRESS).contains(emailLowerCase).matchingCaseIgnore()
                .build(),
				false, 1);

        assertUsersFound(QueryBuilder.queryFor(UserType.class, prismContext)
                .item(UserType.F_EMAIL_ADDRESS).contains(emailVariousCase).matchingCaseIgnore()
                .build(),
				false, 1);

		// comparing polystrings, but providing plain String
		assertUsersFound(QueryBuilder.queryFor(UserType.class, prismContext)
						.item(UserType.F_FULL_NAME).contains(existingNameNorm).matchingNorm()
						.build(),
				false, 1);

		assertUsersFound(QueryBuilder.queryFor(UserType.class, prismContext)
						.item(UserType.F_FULL_NAME).contains(existingNameOrig).matchingNorm()
						.build(),
				false, 1);
	}

	@Test
	public void fullTextSearch() throws Exception {

		OperationResult result = new OperationResult("fullTextSearch");

		Collection<SelectorOptions<GetOperationOptions>> distinct = distinct();

		assertUsersFound(QueryBuilder.queryFor(UserType.class, prismContext)
						.fullText("atestuserX00003")
						.build(),
				false, 1);

		List<PrismObject<UserType>> users = assertUsersFound(QueryBuilder.queryFor(UserType.class, prismContext)
						.fullText("Pellentesque")
						.build(),
				true, 1);

		assertUsersFound(QueryBuilder.queryFor(UserType.class, prismContext)
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

		LOGGER.info("## changing description ##");
		repositoryService.modifyObject(UserType.class, users.get(0).getOid(),
				DeltaBuilder.deltaFor(UserType.class, prismContext)
						.item(UserType.F_DESCRIPTION).replace(newDescription)
						.asItemDeltas(),
				result);

		// just to see SQL used
		LOGGER.info("## changing telephoneNumber ##");
		repositoryService.modifyObject(UserType.class, users.get(0).getOid(),
				DeltaBuilder.deltaFor(UserType.class, prismContext)
						.item(UserType.F_TELEPHONE_NUMBER).replace("123456")
						.asItemDeltas(),
				result);

		assertUsersFoundBySearch(QueryBuilder.queryFor(UserType.class, prismContext)
						.fullText("sollicitudin")
						.build(),
				distinct, 1);

		assertUsersFoundBySearch(QueryBuilder.queryFor(UserType.class, prismContext)
						.fullText("sollicitudin")
						.asc(UserType.F_FULL_NAME)
						.maxSize(100)
						.build(),
				distinct, 1);
	}

	private Collection<SelectorOptions<GetOperationOptions>> distinct() {
		return SelectorOptions.createCollection(GetOperationOptions.createDistinct());
	}

	@Test
	public void testShadowPendingOperation() throws SchemaException {
		ObjectQuery query = QueryBuilder.queryFor(ShadowType.class, prismContext)
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
		ObjectQuery query = QueryBuilder.queryFor(CaseType.class, prismContext)
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
		ObjectQuery query = QueryBuilder.queryFor(CaseType.class, prismContext)
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
		ObjectQuery query = QueryBuilder.queryFor(CaseType.class, prismContext)
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
		ObjectQuery query = QueryBuilder.queryFor(GenericObjectType.class, prismContext)
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
		ObjectQuery query = QueryBuilder.queryFor(GenericObjectType.class, prismContext)
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
		ObjectQuery query = QueryBuilder.queryFor(GenericObjectType.class, prismContext)
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
		ObjectQuery query = QueryBuilder.queryFor(GenericObjectType.class, prismContext)
				.not().item(ObjectType.F_EXTENSION, new QName("referenceType"))
					.isNull()
				.build();
		OperationResult result = new OperationResult("search");
		List<PrismObject<GenericObjectType>> cases = repositoryService.searchObjects(GenericObjectType.class, query, null, result);
		result.recomputeStatus();
		assertTrue(result.isSuccess());
		assertEquals("Should find 1 object", 1, cases.size());
	}

}
