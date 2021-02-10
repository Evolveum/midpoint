package com.evolveum.midpoint.testing.schrodinger.scenarios;

import com.evolveum.midpoint.testing.schrodinger.AbstractSchrodingerTest;

import org.testng.annotations.Test;

public class CleanupPoliciesTest extends AbstractSchrodingerTest {

    @Test
    public void test00100closedTasksCleanup() {
        basicPage
                .cleanupPolicy()
                    .closedTasksCleanupInterval("P6M");
        basicPage
                .listTasks()
                    .table()
                        .search()
                            .byName()
                            .inputValue("Cleanup task")
                            .updateSearch()
                            .and()
                        .clickByName("Cleanup task")
                            .clickRunNow();
    }
}
