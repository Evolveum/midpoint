/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl;

import java.io.File;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.BeforeSuite;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.quartzimpl.quartz.LocalScheduler;
import com.evolveum.midpoint.task.quartzimpl.tasks.TaskStateManager;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

public class AbstractTaskManagerTest extends AbstractIntegrationTest {

    private static final String MOCK_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/mock-task-handler";
    private static final String MOCK_PARALLEL_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/parallel-task-handler";

    private static final File USER_ADMINISTRATOR_FILE = new File(COMMON_DIR, "user-administrator.xml");

    @Autowired protected RepositoryService repositoryService;
    @Autowired protected TaskManagerQuartzImpl taskManager;
    @Autowired protected TaskStateManager taskStateManager;
    @Autowired protected LocalScheduler localScheduler;
    @Autowired protected PrismContext prismContext;
    @Autowired protected SchemaService schemaService;

    MockTaskHandler mockTaskHandler;
    MockParallelTaskHandler mockParallelTaskHandler;

    private void initHandlers() {
        mockTaskHandler = new MockTaskHandler();
        taskManager.registerHandler(MOCK_TASK_HANDLER_URI, mockTaskHandler);

        mockParallelTaskHandler = new MockParallelTaskHandler(taskManager);
        taskManager.registerHandler(MOCK_PARALLEL_TASK_HANDLER_URI, mockParallelTaskHandler);
    }

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    public void initialize() throws Exception {
        initHandlers();
        addObjectFromFile(USER_ADMINISTRATOR_FILE.getPath());
    }

    <T extends ObjectType> PrismObject<T> add(TestObject<T> testObject, OperationResult result) throws Exception {
        PrismObject<T> object = testObject.get();
        addObject(object, result);
        return object;
    }

    @SuppressWarnings("UnusedReturnValue")
    <T extends ObjectType> PrismObject<T> addObjectFromFile(String filePath) throws Exception {
        return addObjectFromFile(filePath, createOperationResult("addObjectFromFile"));
    }

    private <T extends ObjectType> PrismObject<T> addObjectFromFile(String filePath, OperationResult result) throws Exception {
        PrismObject<T> object = PrismTestUtil.parseObject(new File(filePath));
        addObject(object, result);
        logger.trace("Object from {} added to repository.", filePath);
        return object;
    }

    private <T extends ObjectType> void addObject(PrismObject<T> object, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        try {
            add(object, result);
        } catch (ObjectAlreadyExistsException e) {
            delete(object, result);
            add(object, result);
        }
    }

    protected void add(PrismObject<? extends ObjectType> object, OperationResult result)
            throws ObjectAlreadyExistsException, SchemaException {
        if (object.canRepresent(TaskType.class)) {
            //noinspection unchecked,rawtypes
            taskManager.addTask((PrismObject) object, result);
        } else {
            repositoryService.addObject(object, null, result);
        }
    }

    protected void delete(PrismObject<? extends ObjectType> object, OperationResult result) throws ObjectNotFoundException, SchemaException {
        if (object.canRepresent(TaskType.class)) {
            taskManager.deleteTask(object.getOid(), result);
        } else {
            repositoryService.deleteObject(ObjectType.class, object.getOid(), result);            // correct?
        }
    }
}
