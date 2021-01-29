/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql;

import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.repo.api.RepositoryService.OP_SEARCH_OBJECTS;

import java.util.ArrayList;
import java.util.List;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sqlbase.perfmon.SqlPerformanceMonitorImpl;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

@ContextConfiguration(locations = { "../../../../../ctx-test.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class SearchIterativeTest extends BaseSQLRepoTest {

    private static final int BASE = 100000;
    private static final int COUNT = 500;       // should be divisible by BATCH
    private static final int BATCH = 50;        // should be synchronized with repo setting

    @Override
    public void initSystem() throws Exception {
        createObjects();
    }

    private void createObjects() throws com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException, com.evolveum.midpoint.util.exception.SchemaException {
        OperationResult result = new OperationResult("add objects");
        for (int i = BASE; i < BASE + COUNT; i++) {
            UserType user = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class).instantiate().asObjectable();
            user.setOid("user-" + i + "-00");
            user.setName(new PolyStringType(new PolyString("user-" + i)));
            user.setCostCenter(String.valueOf(i));
            repositoryService.addObject(user.asPrismObject(), null, result);
        }

        result.recomputeStatus();
        assertTrue(result.isSuccess());
    }

    @Test
    public void test100SimpleSequentialIteration() throws Exception {
        OperationResult result = new OperationResult("test100SimpleSequentialIteration");

        final List<PrismObject<UserType>> objects = new ArrayList<>();

        ResultHandler<UserType> handler = (object, parentResult) -> {
            objects.add(object);
            return true;
        };

        SearchOpAsserter asserter = new SearchOpAsserter();

        repositoryService.searchObjectsIterative(UserType.class, null, handler, null, true, result);
        result.recomputeStatus();

        asserter.assertIncrement(COUNT / BATCH + 1); // extra search is to make sure no other objects are there

        assertTrue(result.isSuccess());
        assertObjects(objects, COUNT);
    }

    @Test  // MID-5339
    public void test101SequentialIterationWithSmallResult() throws Exception {
        OperationResult result = new OperationResult("test101SequentialIterationWithSmallResult");

        final List<PrismObject<UserType>> objects = new ArrayList<>();

        ResultHandler<UserType> handler = (object, parentResult) -> {
            objects.add(object);
            return true;
        };

        SearchOpAsserter asserter = new SearchOpAsserter();

        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_COST_CENTER).eq(String.valueOf(BASE))
                .build();
        repositoryService.searchObjectsIterative(UserType.class, query, handler, null, true, result);
        result.recomputeStatus();

        asserter.assertIncrement(1);

        assertTrue(result.isSuccess());
        assertObjects(objects, 1);
    }

    @Test
    public void test102SimpleSequentialIterationWithMaxSize() throws Exception {
        OperationResult result = new OperationResult("test102SimpleSequentialIterationWithMaxSize");

        final List<PrismObject<UserType>> objects = new ArrayList<>();

        ResultHandler<UserType> handler = (object, parentResult) -> {
            objects.add(object);
            return true;
        };

        SearchOpAsserter asserter = new SearchOpAsserter();

        ObjectQuery query = prismContext.queryFactory().createQuery(prismContext.queryFactory().createPaging(null, 70));
        repositoryService.searchObjectsIterative(UserType.class, query, handler, null, true, result);
        result.recomputeStatus();

        asserter.assertIncrement(2); // assuming 50 + 20

        assertTrue(result.isSuccess());
        assertObjects(objects, 70);
    }

    @Test
    public void test103SimpleSequentialIterationWithCustomPagingLarge() throws Exception {
        OperationResult result = new OperationResult("test103SimpleSequentialIterationWithCustomPagingLarge");

        final List<PrismObject<UserType>> objects = new ArrayList<>();

        ResultHandler<UserType> handler = (object, parentResult) -> {
            objects.add(object);
            return true;
        };

        SearchOpAsserter searchOpAsserter = new SearchOpAsserter();
        OpAsserter countOpAsserter = new OpAsserter(RepositoryService.OP_COUNT_OBJECTS);

        ObjectQuery query = prismContext.queryFactory().createQuery(prismContext.queryFactory().createPaging(1, null));
        repositoryService.searchObjectsIterative(UserType.class, query, handler, null, true, result);
        result.recomputeStatus();

        countOpAsserter.assertIncrement(1); // repo had to switch to simple paging
        searchOpAsserter.assertIncrement(COUNT / BATCH);

        assertTrue(result.isSuccess());
        assertObjects(objects, COUNT - 1);
    }

    @Test
    public void test104SimpleSequentialIterationWithCustomPagingSmall() throws Exception {
        OperationResult result = new OperationResult("test104SimpleSequentialIterationWithCustomPagingSmall");

        final List<PrismObject<UserType>> objects = new ArrayList<>();

        ResultHandler<UserType> handler = (object, parentResult) -> {
            objects.add(object);
            return true;
        };

        SearchOpAsserter asserter = new SearchOpAsserter();

        ObjectQuery query = prismContext.queryFactory().createQuery(prismContext.queryFactory().createPaging(1, 200));
        repositoryService.searchObjectsIterative(UserType.class, query, handler, null, true, result);
        result.recomputeStatus();

        asserter.assertIncrement(1);            // if we are under limit of FETCH_ALL

        assertTrue(result.isSuccess());
        assertObjects(objects, 200);
    }

    @Test
    public void test105SimpleNonSequentialIteration() throws Exception {
        OperationResult result = new OperationResult("test105SimpleNonSequentialIteration");

        final List<PrismObject<UserType>> objects = new ArrayList<>();

        ResultHandler<UserType> handler = (object, parentResult) -> {
            objects.add(object);
            return true;
        };

        SearchOpAsserter searchOpAsserter = new SearchOpAsserter();
        OpAsserter countOpAsserter = new OpAsserter(RepositoryService.OP_COUNT_OBJECTS);

        repositoryService.searchObjectsIterative(UserType.class, null, handler, null, false, result);
        result.recomputeStatus();

        countOpAsserter.assertIncrement(1);
        searchOpAsserter.assertIncrement(COUNT / BATCH);

        assertTrue(result.isSuccess());
        assertObjects(objects, COUNT);
    }

    private boolean[] assertObjects(List<PrismObject<UserType>> objects, Integer count) {
        if (count != null) {
            assertEquals("Wrong # of objects", count.intValue(), objects.size());
        }
        boolean[] numbers = new boolean[COUNT];
        for (PrismObject<UserType> user : objects) {
            UserType userType = user.asObjectable();
            int number = Integer.parseInt(userType.getCostCenter()) - BASE;
            if (number >= 0) {
                if (numbers[number]) {
                    throw new IllegalStateException("User number " + number + " was processed more than once");
                } else {
                    numbers[number] = true;
                }
            }
        }
        return numbers;
    }

    @Test
    public void test110DeleteAll() throws Exception {
        OperationResult result = new OperationResult("test110DeleteAll");

        final List<PrismObject<UserType>> objects = new ArrayList<>();

        ResultHandler<UserType> handler = (object, parentResult) -> {
            objects.add(object);
            try {
                repositoryService.deleteObject(UserType.class, object.getOid(), parentResult);
            } catch (ObjectNotFoundException e) {
                throw new SystemException(e);
            }
            return true;
        };

        repositoryService.searchObjectsIterative(UserType.class, null, handler, null, true, result);
        result.recomputeStatus();

        assertTrue(result.isSuccess());
        assertObjects(objects, COUNT);

        int count = repositoryService.countObjects(UserType.class, null, null, result);
        assertEquals("Wrong # of objects after operation", 0, count);
    }

    @Test
    public void test120DeleteHalf() throws Exception {
        OperationResult result = new OperationResult("test120DeleteHalf");

        createObjects();

        final List<PrismObject<UserType>> objects = new ArrayList<>();

        ResultHandler<UserType> handler = (object, parentResult) -> {
            objects.add(object);
            try {
                int number = Integer.parseInt(object.asObjectable().getCostCenter());
                if (number % 2 == 0) {
                    repositoryService.deleteObject(UserType.class, object.getOid(), parentResult);
                }
            } catch (ObjectNotFoundException e) {
                throw new SystemException(e);
            }
            return true;
        };

        repositoryService.searchObjectsIterative(UserType.class, null, handler, null, true, result);
        result.recomputeStatus();

        assertTrue(result.isSuccess());
        assertObjects(objects, COUNT);

        int count = repositoryService.countObjects(UserType.class, null, null, result);
        assertEquals("Wrong # of objects after operation", COUNT / 2, count);

        ObjectQuery query = prismContext.queryFor(UserType.class)
                .asc(UserType.F_NAME)
                .build();
        List<PrismObject<UserType>> objectsAfter = repositoryService.searchObjects(UserType.class, query, null, result);
        objectsAfter.forEach(o -> System.out.println("Exists: " + o.asObjectable().getName()));
    }

    @Test
    public void test130AddOneForOne() throws Exception {

        OperationResult result = new OperationResult("test130AddOneForOne");

        final List<PrismObject<UserType>> objects = new ArrayList<>();

        ResultHandler<UserType> handler = (object, parentResult) -> {
            objects.add(object);
            System.out.print("Got object " + object.getOid());
            logger.info("Got object {} ({})", object.getOid(), object.asObjectable().getName().getOrig());
            try {
                int number = Integer.parseInt(object.asObjectable().getCostCenter());
                if (number >= 0) {
                    UserType user = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class).instantiate().asObjectable();
                    user.setOid("user-" + (2 * BASE + COUNT - number) + "-FF");
                    user.setName(new PolyStringType(new PolyString("user-new-" + number)));
                    user.setCostCenter(String.valueOf(-number));
                    repositoryService.addObject(user.asPrismObject(), null, parentResult);
                    System.out.print(" ... creating object " + user.getOid());
                }
            } catch (ObjectAlreadyExistsException | SchemaException e) {
                throw new SystemException(e);
            }
            System.out.println();
            return true;
        };

        repositoryService.searchObjectsIterative(UserType.class, null, handler, null, true, result);
        result.recomputeStatus();

        assertTrue(result.isSuccess());
        boolean[] map = assertObjects(objects, null);
        for (int i = 0; i < COUNT; i++) {
            if (i % 2 == 0) {
                assertFalse("Object " + (BASE + i) + " does exist but it should not", map[i]);
            } else {
                assertTrue("Object " + (BASE + i) + " does not exist but it should", map[i]);
            }
        }

        System.out.println("Object processed: " + objects.size());

        int count = repositoryService.countObjects(UserType.class, null, null, result);
        assertEquals("Wrong # of objects after operation", COUNT, count);
    }

    private SqlPerformanceMonitorImpl getPerformanceMonitor() {
        return ((SqlRepositoryServiceImpl) repositoryService).getPerformanceMonitor();
    }

    private class OpAsserter {
        int before;
        String operation;

        OpAsserter(String operation) {
            this.operation = operation;
            before = getPerformanceMonitor().getFinishedOperationsCount(operation);
        }

        void assertIncrement(int expected) {
            int current = getPerformanceMonitor().getFinishedOperationsCount(operation);
            assertEquals("Unexpected number of " + operation + " operations", expected, current - before);
        }
    }

    private class SearchOpAsserter extends OpAsserter {
        SearchOpAsserter() {
            super(OP_SEARCH_OBJECTS);
        }
    }
}
