/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.wf.impl.legacy;

import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.tasks.WfTaskUtil;
import com.evolveum.midpoint.wf.impl.util.JaxbValueContainer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashSet;

import static org.testng.AssertJUnit.assertEquals;

/**
 * @author mederly
 */
@ContextConfiguration(locations = {"classpath:ctx-workflow-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestInfrastructure extends AbstractInternalModelIntegrationTest {          // todo use weaker class (faster initialization)

    protected static final Trace LOGGER = TraceManager.getTrace(TestInfrastructure.class);

    @Autowired
    private TaskManager taskManager;

    @Autowired
    private WfTaskUtil wfTaskUtil;

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    @Override
    public void initSystem(Task initTask, OperationResult initResult)
            throws Exception {
        super.initSystem(initTask, initResult);
        repoAddObjectsFromFile(AbstractWfTestLegacy.USERS_AND_ROLES_FILE, RoleType.class, initResult);
    }

    @Test(enabled = true)
    public void test100SerializeContext() throws Exception {
    	final String TEST_NAME = "test100SerializeContext";

    	Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        LensContext<UserType> context = new LensContext<UserType>(UserType.class, prismContext, provisioningService);
        PrismObject<UserType> bill = prismContext.parseObject(USER_BARBOSSA_FILE);
        CryptoUtil.encryptValues(protector, bill);
        ObjectDelta<UserType> userDelta = ObjectDelta.createAddDelta(bill);
        LensFocusContext<UserType> focusContext = context.getOrCreateFocusContext();
        focusContext.setPrimaryDelta(userDelta);

        LensContextType contextType = context.toLensContextType();
        JaxbValueContainer<LensContextType> container = new JaxbValueContainer<LensContextType>(contextType, prismContext);
        container.clearActualValue();
        System.out.println("XML value = " + container.getXmlValue());
        LensContextType contextTypeRetrieved = container.getValue();
        LensContext<UserType> contextRetrieved = LensContext.fromLensContextType(contextTypeRetrieved, prismContext, provisioningService, task, result);

        assertEquals("Context after serialization/deserialization does not match context before it (object to add is changed)", context.getFocusContext().getPrimaryDelta().getObjectToAdd(), contextRetrieved.getFocusContext().getPrimaryDelta().getObjectToAdd());
    }

    @Test(enabled = true)
    public void test101SerializeJaxb() throws Exception {

        OperationResult result = new OperationResult("test101SerializeJaxb");

        ScheduleType scheduleType = new ScheduleType();
        scheduleType.setInterval(100);

        JaxbValueContainer<ScheduleType> container = new JaxbValueContainer<ScheduleType>(scheduleType, prismContext);
        container.clearActualValue();
        System.out.println("XML value = " + container.getXmlValue());
        ScheduleType scheduleTypeRetrieved = container.getValue();

        assertEquals("Object after serialization/deserialization does not match original one", scheduleType, scheduleTypeRetrieved);
    }
}
