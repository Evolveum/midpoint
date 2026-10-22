/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web;

import static org.testng.AssertJUnit.*;

import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.LoggingConfigurationManager;
import com.evolveum.midpoint.init.InitialDataImportActivityHandler;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandlerRegistry;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionBean;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.LogfileTestTailer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.InitialDataImportWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-admin-gui-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestCleanStartup extends AbstractModelIntegrationTest {

    private static final String INITIAL_DATA_IMPORT_TASK = """
            <task xmlns="http://midpoint.evolveum.com/xml/ns/public/common/common-3">
                <name>Initial data import test</name>
                <activity>
                    <work>
                        <initialDataImport/>
                    </work>
                </activity>
            </task>
            """;

    @Autowired private InitialDataImportActivityHandler initialDataImportActivityHandler;
    @Autowired private ActivityHandlerRegistry activityHandlerRegistry;

    public TestCleanStartup() {
        super();
        InternalsConfig.setAvoidLoggingChange(true);
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        // The rest of the initialization happens as part of the spring context init
    }

    // work in progress
    @Test
    public void test001Logfiles() throws Exception {
        // GIVEN - system startup and initialization that has already happened
        LogfileTestTailer tailer = new LogfileTestTailer(LoggingConfigurationManager.AUDIT_LOGGER_NAME, false);

        // THEN
        display("Tailing ...");
        tailer.tail();
        display("... done");

        display("Errors", tailer.getErrors());
        display("Warnings", tailer.getWarnings());

        assertMessages("Error", tailer.getErrors(),
                "Unable to find file com/../../keystore.jceks",
                "Provided Icf connector path /C:/tmp is not a directory",
                "Provided Icf connector path C:\\tmp is not a directory",
                "Provided Icf connector path C:\\var\\tmp is not a directory",
                "Provided Icf connector path D:\\var\\tmp is not a directory");

        assertMessages("Warning", tailer.getWarnings());

        tailer.close();
    }

    /**The initial import is a dedicated activity, independent of the scripting infrastructure. See bug MID-12370 */
    @Test
    public void test002InitialDataImportActivityIsRecognizedAndRegistered() throws Exception {
        TaskType task = (TaskType) prismContext.parseObject(INITIAL_DATA_IMPORT_TASK).asObjectable();

        List<WorkDefinitionBean> beans =
                WorkDefinitionUtil.getWorkDefinitionBeans(task.getActivity().getWork());
        assertEquals(1, beans.size());
        assertTrue(beans.get(0) instanceof WorkDefinitionBean.Typed);
        assertTrue(beans.get(0).getBean() instanceof InitialDataImportWorkDefinitionType);

        AbstractWorkDefinition definition = WorkDefinition.fromBean(
                task.getActivity(), ConfigurationItemOrigin.undeterminedSafe());
        assertTrue(definition instanceof InitialDataImportActivityHandler.InitialDataImportWorkDefinition);

        assertSame(initialDataImportActivityHandler, activityHandlerRegistry.getHandler(task.getActivity()));
        assertSame(
                initialDataImportActivityHandler,
                activityHandlerRegistry.getHandler(InitialDataImportActivityHandler.InitialDataImportWorkDefinition.class));
    }

    private void assertMessages(String desc, Collection<String> actualMessages, String... expectedSubstrings) {
        for(String actualMessage: actualMessages) {
            boolean found = false;
            for (String expectedSubstring: expectedSubstrings) {
                if (actualMessage.contains(expectedSubstring)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                AssertJUnit.fail(desc+" \""+actualMessage+"\" was not expected ("+actualMessages.size()+" messages total)");
            }
        }
    }

}
