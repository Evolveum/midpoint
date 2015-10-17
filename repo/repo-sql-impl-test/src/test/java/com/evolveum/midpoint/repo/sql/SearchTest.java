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

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.match.PolyStringOrigMatchingRule;
import com.evolveum.midpoint.prism.match.PolyStringStrictMatchingRule;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

//        PrismDomProcessor domProcessor = prismContext.getPrismDomProcessor();
        List<PrismObject<? extends Objectable>> objects = prismContext.parseObjects(
                new File(FOLDER_BASIC, "objects.xml"));
        OperationResult result = new OperationResult("add objects");
        for (PrismObject object : objects) {
            repositoryService.addObject(object, null, result);
        }

        result.recomputeStatus();
        AssertJUnit.assertTrue(result.isSuccess());
    }

    @Test
    public void iterateEmptySet() throws Exception {
        OperationResult result = new OperationResult("search empty");

        ResultHandler handler = new ResultHandler() {
            @Override
            public boolean handle(PrismObject object, OperationResult parentResult) {
                AssertJUnit.fail();
                return false;
            }
        };

        EqualFilter filter = EqualFilter.createEqual(UserType.F_NAME, UserType.class, prismContext,
                PolyStringStrictMatchingRule.NAME, new PolyString("asdf", "asdf"));
        ObjectQuery query = ObjectQuery.createObjectQuery(filter);

        repositoryService.searchObjectsIterative(UserType.class, query, handler, null, false, result);
        result.recomputeStatus();

        AssertJUnit.assertTrue(result.isSuccess());
    }

    @Test
    public void iterateSet() throws Exception {
        OperationResult result = new OperationResult("search set");

        final List<PrismObject> objects = new ArrayList<PrismObject>();

        ResultHandler handler = new ResultHandler() {
            @Override
            public boolean handle(PrismObject object, OperationResult parentResult) {
                objects.add(object);

                return true;
            }
        };

        repositoryService.searchObjectsIterative(UserType.class, null, handler, null, false, result);
        result.recomputeStatus();

        AssertJUnit.assertTrue(result.isSuccess());
        AssertJUnit.assertEquals(3, objects.size());
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
                AssertJUnit.assertEquals("Incorrect object name was read", names[index++], object.asObjectable().getName().getOrig());
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

        AssertJUnit.assertTrue(result.isSuccess());
        AssertJUnit.assertEquals(size, objects.size());
    }

    @Test
    public void caseSensitiveSearchTest() throws Exception {
        final String existingNameOrig = "Test UserX00003";
        final String nonExistingNameOrig = "test UserX00003";
        final String nameNorm = "test userx00003";

        EqualFilter filter = EqualFilter.createEqual(UserType.F_FULL_NAME, UserType.class, prismContext, 
        		PolyStringOrigMatchingRule.NAME, new PolyString(existingNameOrig, nameNorm));
        ObjectQuery query = ObjectQuery.createObjectQuery(filter);

        OperationResult result = new OperationResult("search");
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        result.recomputeStatus();
        AssertJUnit.assertTrue(result.isSuccess());
        AssertJUnit.assertEquals("Should find one user", 1, users.size());

        filter = EqualFilter.createEqual(UserType.F_FULL_NAME, UserType.class, prismContext,
        		PolyStringOrigMatchingRule.NAME, new PolyString(nonExistingNameOrig, nameNorm));
        query = ObjectQuery.createObjectQuery(filter);

        users = repositoryService.searchObjects(UserType.class, query, null, result);
        result.recomputeStatus();
        AssertJUnit.assertTrue(result.isSuccess());
        AssertJUnit.assertEquals("Found user (shouldn't) because case insensitive search was used", 0, users.size());
    }

    @Test
    public void roleMembershipSearchTest() throws Exception {
        PrismReferenceValue r456 = new PrismReferenceValue("r456", RoleType.COMPLEX_TYPE);
        RefFilter filter = RefFilter.createReferenceEqual(new ItemPath(UserType.F_ROLE_MEMBERSHIP_REF), UserType.class, prismContext, r456);
        ObjectQuery query = ObjectQuery.createObjectQuery(filter);

        OperationResult result = new OperationResult("search");
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        result.recomputeStatus();
        AssertJUnit.assertTrue(result.isSuccess());
        AssertJUnit.assertEquals("Should find one user", 1, users.size());
        AssertJUnit.assertEquals("Wrong user name", "atestuserX00003", users.get(0).getName().getOrig());

        PrismReferenceValue r123 = new PrismReferenceValue("r123", RoleType.COMPLEX_TYPE);
        filter = RefFilter.createReferenceEqual(new ItemPath(UserType.F_ROLE_MEMBERSHIP_REF), UserType.class, prismContext, r123);
        query = ObjectQuery.createObjectQuery(filter);

        users = repositoryService.searchObjects(UserType.class, query, null, result);
        result.recomputeStatus();
        AssertJUnit.assertTrue(result.isSuccess());
        AssertJUnit.assertEquals("Should find two users", 2, users.size());
    }

    @Test
    public void assignmentOrgRefSearchTest() throws Exception {
        PrismReferenceValue o123456 = new PrismReferenceValue("o123456", OrgType.COMPLEX_TYPE);
        RefFilter filter = RefFilter.createReferenceEqual(new ItemPath(UserType.F_ASSIGNMENT, AssignmentType.F_ORG_REF), UserType.class, prismContext, o123456);
        ObjectQuery query = ObjectQuery.createObjectQuery(filter);

        OperationResult result = new OperationResult("search");
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        result.recomputeStatus();
        AssertJUnit.assertTrue(result.isSuccess());
        AssertJUnit.assertEquals("Should find one user", 1, users.size());
        AssertJUnit.assertEquals("Wrong user name", "atestuserX00002", users.get(0).getName().getOrig());

        PrismReferenceValue o999 = new PrismReferenceValue("o999", RoleType.COMPLEX_TYPE);
        filter = RefFilter.createReferenceEqual(new ItemPath(UserType.F_ASSIGNMENT, AssignmentType.F_ORG_REF), UserType.class, prismContext, o999);
        query = ObjectQuery.createObjectQuery(filter);

        users = repositoryService.searchObjects(UserType.class, query, null, result);
        result.recomputeStatus();
        AssertJUnit.assertTrue(result.isSuccess());
        AssertJUnit.assertEquals("Should find zero users", 0, users.size());
    }

    @Test
    public void assignmentResourceRefSearchTest() throws Exception {
        PrismReferenceValue resourceRef = new PrismReferenceValue("10000000-0000-0000-0000-000000000004", ResourceType.COMPLEX_TYPE);
        RefFilter filter = RefFilter.createReferenceEqual(new ItemPath(RoleType.F_ASSIGNMENT, AssignmentType.F_CONSTRUCTION, ConstructionType.F_RESOURCE_REF), UserType.class, prismContext, resourceRef);
        ObjectQuery query = ObjectQuery.createObjectQuery(filter);

        OperationResult result = new OperationResult("search");
        List<PrismObject<RoleType>> roles = repositoryService.searchObjects(RoleType.class, query, null, result);
        result.recomputeStatus();
        AssertJUnit.assertTrue(result.isSuccess());
        AssertJUnit.assertEquals("Should find one role", 1, roles.size());
        AssertJUnit.assertEquals("Wrong role name", "Judge", roles.get(0).getName().getOrig());

        PrismReferenceValue resourceRef2 = new PrismReferenceValue("FFFFFFFF-0000-0000-0000-000000000004", ResourceType.COMPLEX_TYPE);
        filter = RefFilter.createReferenceEqual(new ItemPath(RoleType.F_ASSIGNMENT, AssignmentType.F_CONSTRUCTION, ConstructionType.F_RESOURCE_REF), UserType.class, prismContext, resourceRef2);
        query = ObjectQuery.createObjectQuery(filter);

        roles = repositoryService.searchObjects(RoleType.class, query, null, result);
        result.recomputeStatus();
        AssertJUnit.assertTrue(result.isSuccess());
        AssertJUnit.assertEquals("Should find zero roles", 0, roles.size());
    }
  @Test
    public void roleAssignmentSearchTest() throws Exception {
        PrismReferenceValue r456 = new PrismReferenceValue("r123", RoleType.COMPLEX_TYPE);
        RefFilter rFilter = RefFilter.createReferenceEqual(new ItemPath(UserType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF), UserType.class, prismContext, r456);
        

        ObjectQuery query = ObjectQuery.createObjectQuery(rFilter);

        OperationResult result = new OperationResult("search");
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        result.recomputeStatus();
        AssertJUnit.assertTrue(result.isSuccess());
        AssertJUnit.assertEquals("Should find one user", 1, users.size());
        AssertJUnit.assertEquals("Wrong user name", "atestuserX00002", users.get(0).getName().getOrig());

    }
    
    @Test
    public void orgAssignmentSearchTest() throws Exception {
         
        PrismReferenceValue org = new PrismReferenceValue("00000000-8888-6666-0000-100000000085", OrgType.COMPLEX_TYPE);
        RefFilter oFilter = RefFilter.createReferenceEqual(new ItemPath(UserType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF), UserType.class, prismContext, org);
        ObjectQuery query = ObjectQuery.createObjectQuery(oFilter);

        OperationResult result = new OperationResult("search");
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        result.recomputeStatus();
        AssertJUnit.assertTrue(result.isSuccess());
        AssertJUnit.assertEquals("Should find one user", 1, users.size());
        AssertJUnit.assertEquals("Wrong user name", "atestuserX00002", users.get(0).getName().getOrig());

    }
    
    @Test(enabled = false)
    public void roleAndOrgAssignmentSearchTest() throws Exception {
        PrismReferenceValue r456 = new PrismReferenceValue("r123", RoleType.COMPLEX_TYPE);
        RefFilter rFilter = RefFilter.createReferenceEqual(new ItemPath(UserType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF), UserType.class, prismContext, r456);
        
        PrismReferenceValue org = new PrismReferenceValue("00000000-8888-6666-0000-100000000085", OrgType.COMPLEX_TYPE);
        RefFilter oFilter = RefFilter.createReferenceEqual(new ItemPath(UserType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF), UserType.class, prismContext, org);
        ObjectQuery query = ObjectQuery.createObjectQuery(AndFilter.createAnd(rFilter, oFilter));

        OperationResult result = new OperationResult("search");
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        result.recomputeStatus();
        AssertJUnit.assertTrue(result.isSuccess());
        AssertJUnit.assertEquals("Should find one user", 1, users.size());
        AssertJUnit.assertEquals("Wrong user name", "atestuserX00002", users.get(0).getName().getOrig());

    }

    @Test
    public void notBusinessRoleTypeSearchTest() throws Exception {

        EqualFilter equalFilter = EqualFilter.createEqual(new ItemPath(RoleType.F_ROLE_TYPE), RoleType.class, prismContext, "business");
        NotFilter notFilter = NotFilter.createNot(equalFilter);
        ObjectQuery query = ObjectQuery.createObjectQuery(notFilter);

        OperationResult result = new OperationResult("search");
        List<PrismObject<RoleType>> roles = repositoryService.searchObjects(RoleType.class, query, null, result);
        result.recomputeStatus();
        AssertJUnit.assertTrue(result.isSuccess());
        AssertJUnit.assertEquals("Should find one role", 1, roles.size());
        AssertJUnit.assertEquals("Wrong role name", "Judge", roles.get(0).getName().getOrig());

    }

    @Test
    public void businessRoleTypeSearchTest() throws Exception {

        EqualFilter equalFilter = EqualFilter.createEqual(new ItemPath(RoleType.F_ROLE_TYPE), RoleType.class, prismContext, "business");
        ObjectQuery query = ObjectQuery.createObjectQuery(equalFilter);

        OperationResult result = new OperationResult("search");
        List<PrismObject<RoleType>> roles = repositoryService.searchObjects(RoleType.class, query, null, result);
        result.recomputeStatus();
        AssertJUnit.assertTrue(result.isSuccess());
        AssertJUnit.assertEquals("Should find one role", 1, roles.size());
        AssertJUnit.assertEquals("Wrong role name", "Pirate", roles.get(0).getName().getOrig());

    }

    @Test
    public void emptyRoleTypeSearchTest() throws Exception {

        EqualFilter equalFilter = EqualFilter.createEqual(new ItemPath(RoleType.F_ROLE_TYPE), RoleType.class, prismContext, null);
        ObjectQuery query = ObjectQuery.createObjectQuery(equalFilter);

        OperationResult result = new OperationResult("search");
        List<PrismObject<RoleType>> roles = repositoryService.searchObjects(RoleType.class, query, null, result);
        result.recomputeStatus();
        AssertJUnit.assertTrue(result.isSuccess());
        AssertJUnit.assertEquals("Should find one role", 1, roles.size());
        AssertJUnit.assertEquals("Wrong role name", "Judge", roles.get(0).getName().getOrig());
    }

    @Test
    public void nonEmptyRoleTypeSearchTest() throws Exception {

        EqualFilter equalFilter = EqualFilter.createEqual(new ItemPath(RoleType.F_ROLE_TYPE), RoleType.class, prismContext, null);
        NotFilter notFilter = NotFilter.createNot(equalFilter);
        ObjectQuery query = ObjectQuery.createObjectQuery(notFilter);

        OperationResult result = new OperationResult("search");
        List<PrismObject<RoleType>> roles = repositoryService.searchObjects(RoleType.class, query, null, result);
        result.recomputeStatus();
        AssertJUnit.assertTrue(result.isSuccess());
        AssertJUnit.assertEquals("Should find one role", 1, roles.size());
        AssertJUnit.assertEquals("Wrong role name", "Pirate", roles.get(0).getName().getOrig());

    }


}
