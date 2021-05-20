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
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

public class AbstractTaskManagerTest extends AbstractIntegrationTest {

    private static final String CYCLE_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/cycle-task-handler";
    private static final String SINGLE_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/single-task-handler";
    private static final String SINGLE_WB_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/single-wb-task-handler";
    private static final String PARTITIONED_WB_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/partitioned-wb-task-handler";
    private static final String PARTITIONED_WB_TASK_HANDLER_URI_1 = PARTITIONED_WB_TASK_HANDLER_URI + "#1";
    private static final String PARTITIONED_WB_TASK_HANDLER_URI_2 = PARTITIONED_WB_TASK_HANDLER_URI + "#2";
    private static final String PARTITIONED_WB_TASK_HANDLER_URI_3 = PARTITIONED_WB_TASK_HANDLER_URI + "#3";
    private static final String PARALLEL_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/parallel-task-handler";
    private static final String LONG_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/long-task-handler";

    private static final File USER_ADMINISTRATOR_FILE = new File(COMMON_DIR, "user-administrator.xml");
    static final File SYSTEM_CONFIGURATION_FILE = new File(COMMON_DIR, "system-configuration.xml");

    @Autowired protected RepositoryService repositoryService;
    @Autowired protected TaskManagerQuartzImpl taskManager;
    @Autowired protected TaskStateManager taskStateManager;
    @Autowired protected LocalScheduler localScheduler;
    @Autowired protected PrismContext prismContext;
    @Autowired protected SchemaService schemaService;

    MockSingleTaskHandler singleHandler1;
    MockParallelTaskHandler parallelTaskHandler;

    private void initHandlers() {
        MockCycleTaskHandler cycleHandler = new MockCycleTaskHandler();
        taskManager.registerHandler(CYCLE_TASK_HANDLER_URI, cycleHandler);

        singleHandler1 = new MockSingleTaskHandler("1", taskManager);
        taskManager.registerHandler(SINGLE_TASK_HANDLER_URI, singleHandler1);

        parallelTaskHandler = new MockParallelTaskHandler("1", taskManager);
        taskManager.registerHandler(PARALLEL_TASK_HANDLER_URI, parallelTaskHandler);
        MockLongTaskHandler longTaskHandler = new MockLongTaskHandler("1", taskManager);
        taskManager.registerHandler(LONG_TASK_HANDLER_URI, longTaskHandler);
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

    <T extends ObjectType> PrismObject<T> add(TestResource<T> testResource, OperationResult result) throws Exception {
        return addObjectFromFile(testResource.file.getAbsolutePath(), result);
    }

    @SuppressWarnings("UnusedReturnValue")
    <T extends ObjectType> PrismObject<T> addObjectFromFile(String filePath) throws Exception {
        return addObjectFromFile(filePath, createOperationResult("addObjectFromFile"));
    }

    private <T extends ObjectType> PrismObject<T> addObjectFromFile(String filePath, OperationResult result) throws Exception {
        PrismObject<T> object = PrismTestUtil.parseObject(new File(filePath));
        try {
            add(object, result);
        } catch (ObjectAlreadyExistsException e) {
            delete(object, result);
            add(object, result);
        }
        logger.trace("Object from {} added to repository.", filePath);
        return object;
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
