/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author mederly
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class SearchIterativeTest extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(SearchIterativeTest.class);
    private static final int BASE = 100000;
    private static final int COUNT = 500;

    @Override
    @BeforeClass
    public void beforeClass() throws Exception {
        super.beforeClass();

        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);

        createObjects();
    }

    protected void createObjects() throws com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException, com.evolveum.midpoint.util.exception.SchemaException {
        OperationResult result = new OperationResult("add objects");
        for (int i = BASE; i < BASE + COUNT; i++) {
            UserType user = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class).instantiate().asObjectable();
            user.setOid("user-" + i + "-00");
            user.setName(new PolyStringType(new PolyString("user-"+i)));
            user.setCostCenter(String.valueOf(i));
            repositoryService.addObject(user.asPrismObject(), null, result);
        }

        result.recomputeStatus();
        assertTrue(result.isSuccess());
    }

    @Test
    public void test100SimpleIteration() throws Exception {
        OperationResult result = new OperationResult("test100SimpleIteration");

        final List<PrismObject<UserType>> objects = new ArrayList<>();

        ResultHandler handler = new ResultHandler() {
            @Override
            public boolean handle(PrismObject object, OperationResult parentResult) {
                objects.add(object);
                return true;
            }
        };

        repositoryService.searchObjectsIterative(UserType.class, null, handler, null, true, result);
        result.recomputeStatus();

        assertTrue(result.isSuccess());
        assertObjects(objects, COUNT);
    }

    @Test
    public void test105SimpleIterationNoSequence() throws Exception {
        OperationResult result = new OperationResult("test105SimpleIterationNoSequence");

        final List<PrismObject<UserType>> objects = new ArrayList<>();

        ResultHandler handler = new ResultHandler() {
            @Override
            public boolean handle(PrismObject object, OperationResult parentResult) {
                objects.add(object);
                return true;
            }
        };

        repositoryService.searchObjectsIterative(UserType.class, null, handler, null, false, result);
        result.recomputeStatus();

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

        ResultHandler handler = new ResultHandler() {
            @Override
            public boolean handle(PrismObject object, OperationResult parentResult) {
                objects.add(object);
                try {
                    repositoryService.deleteObject(UserType.class, object.getOid(), parentResult);
                } catch (ObjectNotFoundException e) {
                    throw new SystemException(e);
                }
                return true;
            }
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

        ResultHandler handler = new ResultHandler() {
            @Override
            public boolean handle(PrismObject object, OperationResult parentResult) {
                objects.add(object);
                try {
                    int number = Integer.parseInt(((UserType) object.asObjectable()).getCostCenter());
                    if (number % 2 == 0) {
                        repositoryService.deleteObject(UserType.class, object.getOid(), parentResult);
                    }
                } catch (ObjectNotFoundException e) {
                    throw new SystemException(e);
                }
                return true;
            }
        };

        repositoryService.searchObjectsIterative(UserType.class, null, handler, null, true, result);
        result.recomputeStatus();

        assertTrue(result.isSuccess());
        assertObjects(objects, COUNT);

        int count = repositoryService.countObjects(UserType.class, null, null, result);
        assertEquals("Wrong # of objects after operation", COUNT/2, count);
    }

    @Test
    public void test130AddOneForOne() throws Exception {
        OperationResult result = new OperationResult("test130AddOneForOne");

        final List<PrismObject<UserType>> objects = new ArrayList<>();

        ResultHandler handler = new ResultHandler() {
            @Override
            public boolean handle(PrismObject object, OperationResult parentResult) {
                objects.add(object);
                System.out.print("Got object " + object.getOid());
                try {
                    int number = Integer.parseInt(((UserType) object.asObjectable()).getCostCenter());
                    if (number >= 0) {
                        UserType user = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class).instantiate().asObjectable();
                        user.setOid("user-" + (2*BASE + COUNT - number) + "-FF");
                        user.setName(new PolyStringType(new PolyString("user-new-" + number)));
                        user.setCostCenter(String.valueOf(-number));
                        repositoryService.addObject(user.asPrismObject(), null, parentResult);
                        System.out.print(" ... creating object " + user.getOid());
                    }
                } catch (ObjectAlreadyExistsException|SchemaException e) {
                    throw new SystemException(e);
                }
                System.out.println();
                return true;
            }
        };

        repositoryService.searchObjectsIterative(UserType.class, null, handler, null, true, result);
        result.recomputeStatus();

        assertTrue(result.isSuccess());
        boolean[] map = assertObjects(objects, null);
        for (int i = 0; i < COUNT; i++) {
            if (i % 2 == 0) {
                assertFalse("Object " + (BASE+i) + " does exist but it should not", map[i]);
            } else {
                assertTrue("Object " + (BASE+i) + " does not exist but it should", map[i]);
            }
        }

        System.out.println("Object processed: " + objects.size());

        int count = repositoryService.countObjects(UserType.class, null, null, result);
        assertEquals("Wrong # of objects after operation", COUNT, count);
    }
}
