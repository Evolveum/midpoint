package com.evolveum.midpoint.testing.schrodinger.scenarios;

import com.codeborne.selenide.Selenide;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.testing.schrodinger.AbstractSchrodingerTest;

import org.testng.annotations.Test;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class CleanupPoliciesTest extends AbstractSchrodingerTest {

    public static final File CLOSED_TASKS_XML = new File("./src/test/resources/component/objects/tasks/closed-tasks.xml");

    @Override
    protected List<File> getObjectListToImport(){
        return Collections.singletonList(CLOSED_TASKS_XML);
    }

    @Test
    public void test00100closedTasksCleanup() {
        basicPage
                .listTasks()
                    .table()
                        .assertCurrentTableContains("ClosedTask1")
                        .assertCurrentTableContains("ClosedTask2")
                    .and()
                .cleanupPolicy()
                    .closedTasksCleanupInterval("P6M")
                    .and()
                .clickSave()
                .feedback()
                    .assertSuccess()
                    .and()
                .listTasks()
                    .table()
                        .search()
                            .byName()
                            .inputValue("Cleanup")
                            .updateSearch()
                            .and()
                        .clickByName("Cleanup")
                            .clickRunNow();
        Selenide.sleep(MidPoint.TIMEOUT_LONG_1_M);
        basicPage
                .listTasks()
                .table()
                .assertCurrentTableDoesntContain("ClosedTask1")
                .assertCurrentTableDoesntContain("ClosedTask2");
    }
}
