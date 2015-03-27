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

package com.evolveum.midpoint.wf.impl;

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
import com.evolveum.midpoint.wf.impl.jobs.WfTaskUtil;
import com.evolveum.midpoint.wf.impl.util.JaxbValueContainer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScheduleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.model.model_context_3.LensContextType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
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
        repoAddObjectsFromFile(AbstractWfTest.USERS_AND_ROLES_FILENAME, RoleType.class, initResult);
    }

    @Test(enabled = true)
    public void test010SetGetWfApprovedBy() throws Exception {

        Task task = taskManager.createTaskInstance();
        OperationResult result = new OperationResult("test010SetGetWfApprovedBy");

        task.setOwner(repositoryService.getObject(UserType.class, SystemObjectsType.USER_ADMINISTRATOR.value(), null, result));
        taskManager.switchToBackground(task, result);

        wfTaskUtil.addApprovedBy(task, SystemObjectsType.USER_ADMINISTRATOR.value());
        wfTaskUtil.addApprovedBy(task, SystemObjectsType.USER_ADMINISTRATOR.value());
        wfTaskUtil.addApprovedBy(task, AbstractWfTest.R1BOSS_OID);
        wfTaskUtil.addApprovedBy(task, AbstractWfTest.R2BOSS_OID);
        task.savePendingModifications(result);

        Task task2 = taskManager.getTask(task.getOid(), result);
        PrismReference approvers = wfTaskUtil.getApprovedBy(task2);

        assertEquals("Incorrect number of approvers", 3, approvers.getValues().size());
        assertEquals("Incorrect approvers",
                new HashSet(Arrays.asList(SystemObjectsType.USER_ADMINISTRATOR.value(), AbstractWfTest.R1BOSS_OID, AbstractWfTest.R2BOSS_OID)),
                new HashSet(Arrays.asList(approvers.getValue(0).getOid(), approvers.getValue(1).getOid(), approvers.getValue(2).getOid())));
    }

    @Test(enabled = true)
    public void test100SerializeContext() throws Exception {

        OperationResult result = new OperationResult("test100SerializeContext");

        LensContext<UserType> context = new LensContext<UserType>(UserType.class, prismContext, provisioningService);
        PrismObject<UserType> bill = prismContext.parseObject(new File(USER_BARBOSSA_FILENAME));
        CryptoUtil.encryptValues(protector, bill);
        ObjectDelta<UserType> userDelta = ObjectDelta.createAddDelta(bill);
        LensFocusContext<UserType> focusContext = context.getOrCreateFocusContext();
        focusContext.setPrimaryDelta(userDelta);

        LensContextType contextType = context.toPrismContainer().getValue().asContainerable();
        JaxbValueContainer<LensContextType> container = new JaxbValueContainer<LensContextType>(contextType, prismContext);
        container.clearActualValue();
        System.out.println("XML value = " + container.getXmlValue());
        LensContextType contextTypeRetrieved = container.getValue();
        LensContext<UserType> contextRetrieved = LensContext.fromLensContextType(contextTypeRetrieved, prismContext, provisioningService, result);

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
