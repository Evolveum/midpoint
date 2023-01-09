/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.PATH_MODEL_EXTENSION_DRY_RUN;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.ScriptHistoryEntry;
import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestReconScript extends AbstractInternalModelIntegrationTest {

    private static final String TASK_RECON_DUMMY_FILENAME = "src/test/resources/common/task-reconcile-dummy.xml";
    private static final String TASK_RECON_DUMMY_OID = "10000000-0000-0000-5656-565600000004";

    private static final String ACCOUNT_BEFORE_SCRIPT_FILENAME = "src/test/resources/lens/account-before-script.xml";
    private static final String ACCOUNT_BEFORE_SCRIPT_OID = "acc00000-0000-0000-0000-000000001234";

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
        getDummyResource().getScriptHistory().clear();

        importObjectFromFile(new File(TASK_RECON_DUMMY_FILENAME));

        waitForTaskStart(TASK_RECON_DUMMY_OID, false, DEFAULT_TASK_WAIT_TIMEOUT);

        waitForTaskNextRunAssertSuccess(TASK_RECON_DUMMY_OID, false, DEFAULT_TASK_WAIT_TIMEOUT);

        waitForTaskFinish(TASK_RECON_DUMMY_OID, false);

        assertTask(TASK_RECON_DUMMY_OID, "after")
                .display(); // TODO

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
        OperationResult parentResult = createOperationResult();

        ShadowType shadow = parseObjectType(new File(ACCOUNT_BEFORE_SCRIPT_FILENAME), ShadowType.class);

        provisioningService.addObject(shadow.asPrismObject(), null, null, task, parentResult);

        getDummyResource().getScriptHistory().clear();

        waitForTaskStart(TASK_RECON_DUMMY_OID, false, DEFAULT_TASK_WAIT_TIMEOUT);

        waitForTaskNextRunAssertSuccess(TASK_RECON_DUMMY_OID, false, DEFAULT_TASK_WAIT_TIMEOUT);

        waitForTaskFinish(TASK_RECON_DUMMY_OID, true);

        assertTask(TASK_RECON_DUMMY_OID, "after")
                .display(); // TODO

        PrismObject<ShadowType> afterRecon = repositoryService.getObject(ShadowType.class, ACCOUNT_BEFORE_SCRIPT_OID, null, parentResult);
        AssertJUnit.assertNotNull(afterRecon);

        afterRecon.asObjectable();

        PrismObject<FocusType> user = repositoryService.searchShadowOwner(ACCOUNT_BEFORE_SCRIPT_OID, null, parentResult);
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
        PrismObject<TaskType> task = getTask(TASK_RECON_DUMMY_OID);
        OperationResult parentResult = createOperationResult();

        PropertyDelta<?> dryRunDelta =
                prismContext.deltaFactory().property().createModificationReplaceProperty(
                        PATH_MODEL_EXTENSION_DRY_RUN, task.getDefinition(), true);
        Collection<PropertyDelta<?>> modifications = new ArrayList<>();
        modifications.add(dryRunDelta);

        repositoryService.modifyObject(TaskType.class, TASK_RECON_DUMMY_OID, modifications, parentResult);

        getDummyResource().deleteAccountByName("beforeScript");

        waitForTaskStart(TASK_RECON_DUMMY_OID, false);

        waitForTaskNextRunAssertSuccess(TASK_RECON_DUMMY_OID, false);

        waitForTaskFinish(TASK_RECON_DUMMY_OID, false);

        assertTask(TASK_RECON_DUMMY_OID, "after")
                .display(); // TODO

        PrismObject<ShadowType> shadow = repositoryService.getObject(ShadowType.class, ACCOUNT_BEFORE_SCRIPT_OID, null, parentResult);
        AssertJUnit.assertNotNull(shadow);

        PrismObject<FocusType> user = repositoryService.searchShadowOwner(ACCOUNT_BEFORE_SCRIPT_OID, null, parentResult);
        AssertJUnit.assertNotNull("Owner for account " + shadow + " not found. Some problem in dry run occurred.", user);
    }

    @Test
    public void test006TestReconDelete() throws Exception {
        PrismObject<TaskType> task = getTask(TASK_RECON_DUMMY_OID);
        OperationResult parentResult = createOperationResult();

        PropertyDelta<Boolean> dryRunDelta = prismContext.deltaFactory().property().createModificationReplaceProperty(
                PATH_MODEL_EXTENSION_DRY_RUN, task.getDefinition(), false);
        Collection<PropertyDelta<?>> modifications = new ArrayList<>();
        modifications.add(dryRunDelta);

        repositoryService.modifyObject(TaskType.class, TASK_RECON_DUMMY_OID, modifications, parentResult);

        // WHEN
        when();

        waitForTaskStart(TASK_RECON_DUMMY_OID, false);

        waitForTaskNextRunAssertSuccess(TASK_RECON_DUMMY_OID, false);

        waitForTaskFinish(TASK_RECON_DUMMY_OID, false);

        // THEN
        then();

        assertTask(TASK_RECON_DUMMY_OID, "after")
                .display(); // TODO

        assertRepoShadow(ACCOUNT_BEFORE_SCRIPT_OID)
                .display()
                .assertDead()
                .assertIsNotExists();

        PrismObject<FocusType> user = repositoryService.searchShadowOwner(ACCOUNT_BEFORE_SCRIPT_OID, null, parentResult);
        display("Account owner", user);
        AssertJUnit.assertNotNull("Owner for account " + ACCOUNT_BEFORE_SCRIPT_OID + " was not found", user);
    }
}
