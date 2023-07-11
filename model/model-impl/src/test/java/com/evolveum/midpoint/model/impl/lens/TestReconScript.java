/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.List;

import com.evolveum.midpoint.test.TestTask;

import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.ScriptHistoryEntry;
import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestReconScript extends AbstractInternalModelIntegrationTest {

    private static final File DIR_LENS = new File("src/test/resources/lens");
    private static final TestTask TASK_RECON_DUMMY =
            TestTask.file(DIR_LENS, "task-reconcile-dummy.xml", "8bf1e8d3-75c0-40ee-9c65-d96ee709f007");

    private static final String ACCOUNT_BEFORE_SCRIPT_FILENAME = "src/test/resources/lens/account-before-script.xml";
    private static final String ACCOUNT_BEFORE_SCRIPT_OID = "acc00000-0000-0000-0000-000000001234";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initTestObjects(initTask, initResult, TASK_RECON_DUMMY);
    }

    @Test
    public void test001TestReconcileScriptsWhenProvisioning() throws Exception {
        Task task = getTestTask();
        OperationResult result = createOperationResult();

        ObjectDelta<UserType> delta = createModifyUserAddAccount(USER_JACK_OID, getDummyResourceObject());
        task.setChannel(QNameUtil.qNameToUri(SchemaConstants.CHANNEL_RECON));
        modelService.executeChanges(List.of(delta), executeOptions().reconcile(), task, result);

        ObjectDelta<UserType> delta2 =
                createModifyUserReplaceDelta(USER_JACK_OID, UserType.F_FULL_NAME, new PolyString("tralala"));
        modelService.executeChanges(List.of(delta2), executeOptions().reconcile(), task, result);

        ObjectDelta<UserType> delta3 =
                createModifyUserReplaceDelta(USER_BARBOSSA_OID, UserType.F_FULL_NAME, new PolyString("tralala"));
        modelService.executeChanges(List.of(delta3), executeOptions().reconcile(), task, result);

        List<ScriptHistoryEntry> scriptHistory = getDummyResource().getScriptHistory();
        assertThat(scriptHistory).as("script history").isNotEmpty();
        for (ScriptHistoryEntry script : scriptHistory) {
            String userName = (String) script.getParams().get("midpoint_usercn");
            String idPath = (String) script.getParams().get("midpoint_idpath");
            String tempPath = (String) script.getParams().get("midpoint_temppath");
            logger.trace("userName {} idPath {} tempPath {}", userName, idPath, tempPath);
            if (!idPath.contains(userName)) {
                AssertJUnit.fail("Expected that idPath will contain userName [idPath: " + idPath + ", userName " + userName + "]");
            }

            if (!tempPath.contains(userName)) {
                AssertJUnit.fail("Expected that tempPath will contain userName [idPath: " + idPath + ", userName " + userName + "]");
            }
        }
    }

    @Test
    public void test002TestReconcileScriptsWhenReconciling() throws Exception {
        OperationResult result = createOperationResult();

        getDummyResource().getScriptHistory().clear();

        TASK_RECON_DUMMY.rerun(result);

        TASK_RECON_DUMMY.assertAfter();

        List<ScriptHistoryEntry> scriptHistory = getDummyResource().getScriptHistory();
        assertThat(scriptHistory).as("script history").isNotEmpty();
        for (ScriptHistoryEntry script : scriptHistory) {
            String userName = (String) script.getParams().get("midpoint_usercn");
            String idPath = (String) script.getParams().get("midpoint_idpath");
            String tempPath = (String) script.getParams().get("midpoint_temppath");
            logger.trace("userName {} idPath {} tempPath {}", userName, idPath, tempPath);
            if (!idPath.contains(userName)) {
                AssertJUnit.fail("Expected that idPath will contain userName [idPath: " + idPath + ", userName " + userName + "]");
            }

            if (!tempPath.contains(userName)) {
                AssertJUnit.fail("Expected that tempPath will contain userName [idPath: " + idPath + ", userName " + userName + "]");
            }
        }
    }

    @Test
    public void test003TestReconcileScriptsAddUserAction() throws Exception {
        Task task = getTestTask();
        OperationResult result = createOperationResult();

        ShadowType shadow = parseObjectType(new File(ACCOUNT_BEFORE_SCRIPT_FILENAME), ShadowType.class);

        provisioningService.addObject(shadow.asPrismObject(), null, null, task, result);

        getDummyResource().getScriptHistory().clear();

        TASK_RECON_DUMMY.rerun(result);

        TASK_RECON_DUMMY.assertAfter();

        PrismObject<ShadowType> afterRecon = repositoryService.getObject(ShadowType.class, ACCOUNT_BEFORE_SCRIPT_OID, null, result);
        AssertJUnit.assertNotNull(afterRecon);

        afterRecon.asObjectable();

        PrismObject<FocusType> user = repositoryService.searchShadowOwner(ACCOUNT_BEFORE_SCRIPT_OID, null, result);
        AssertJUnit.assertNotNull("Owner for account " + shadow.asPrismObject() + " not found. Some problem in recon occurred.", user);

        List<ScriptHistoryEntry> scriptHistory = getDummyResource().getScriptHistory();
        assertThat(scriptHistory).as("script history").isNotEmpty();
        for (ScriptHistoryEntry script : scriptHistory) {

            String userName = (String) script.getParams().get("midpoint_usercn");
            String idPath = (String) script.getParams().get("midpoint_idpath");
            String tempPath = (String) script.getParams().get("midpoint_temppath");
            logger.trace("userName {} idPath {} tempPath {}", userName, idPath, tempPath);
            if (!idPath.contains(userName)) {
                AssertJUnit.fail("Expected that idPath will contain userName [idPath: " + idPath + ", userName " + userName + "]");
            }

            if (!tempPath.contains(userName)) {
                AssertJUnit.fail("Expected that tempPath will contain userName [idPath: " + idPath + ", userName " + userName + "]");
            }
        }
    }

    @Test
    public void test005TestDryRunDelete() throws Exception {
        OperationResult result = createOperationResult();

        // It would be cleaner to create a separate task for the dry run
        setTaskExecutionMode(ExecutionModeType.DRY_RUN, result);

        getDummyResource().deleteAccountByName("beforeScript");

        TASK_RECON_DUMMY.rerun(result);

        TASK_RECON_DUMMY.assertAfter()
                .assertClockworkRunCount(0); // Dry run = no clockwork

        PrismObject<ShadowType> shadow = repositoryService.getObject(ShadowType.class, ACCOUNT_BEFORE_SCRIPT_OID, null, result);
        AssertJUnit.assertNotNull(shadow);

        PrismObject<FocusType> user = repositoryService.searchShadowOwner(ACCOUNT_BEFORE_SCRIPT_OID, null, result);
        // Actually, even in full execution, the owner is not touched. There is no "delete" reaction. And the dead link does not
        // prevent the searchShadowOwner from returning the owner. Hence, this assert does not work as expected.
        AssertJUnit.assertNotNull("Owner for account " + shadow + " not found. Some problem in dry run occurred.", user);
    }

    private void setTaskExecutionMode(ExecutionModeType mode, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        repositoryService.modifyObject(
                TaskType.class,
                TASK_RECON_DUMMY.oid,
                prismContext.deltaFor(TaskType.class)
                        .item(TaskType.F_ACTIVITY, ActivityDefinitionType.F_EXECUTION, ActivityExecutionModeDefinitionType.F_MODE)
                        .replace(mode)
                        .asItemDeltas(),
                result);
    }

    @Test
    public void test006TestReconDelete() throws Exception {
        OperationResult result = createOperationResult();

        // It would be cleaner to create a separate task for the dry run
        setTaskExecutionMode(ExecutionModeType.FULL, result);

        when();

        TASK_RECON_DUMMY.rerun(result);

        then();

        TASK_RECON_DUMMY.assertAfter();

        assertRepoShadow(ACCOUNT_BEFORE_SCRIPT_OID)
                .display()
                .assertDead()
                .assertIsNotExists();

        PrismObject<FocusType> user = repositoryService.searchShadowOwner(ACCOUNT_BEFORE_SCRIPT_OID, null, result);
        display("Account owner", user);
        AssertJUnit.assertNotNull("Owner for account " + ACCOUNT_BEFORE_SCRIPT_OID + " was not found", user);
    }
}
