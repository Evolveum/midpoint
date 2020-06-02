/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.page;

import com.codeborne.selenide.Selenide;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.AssignmentHolderBasicTab;
import com.evolveum.midpoint.schrodinger.component.common.PrismForm;
import com.evolveum.midpoint.schrodinger.page.AssignmentHolderDetailsPage;
import com.evolveum.midpoint.schrodinger.page.task.ListTasksPage;
import com.evolveum.midpoint.schrodinger.page.task.TaskPage;
import com.evolveum.midpoint.schrodinger.page.user.ListUsersPage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.testing.schrodinger.AbstractSchrodingerTest;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author skublik
 */

public class TaskPageTest extends AbstractSchrodingerTest {

    @Test
    public void test001createNewTask() {

        String name = "NewTest";
        String handler = "Recompute task";
        Selenide.sleep(MidPoint.TIMEOUT_MEDIUM_6_S);
        TaskPage task = basicPage.newTask();
        task.setHandlerUriForNewTask(handler);
        task.selectTabBasic()
                .form()
                    .addAttributeValue("name", name)
                    .selectOption("recurrence","Single")
                    .selectOption("objectType","User")
                    .and()
                .and()
             .clickSave()
                .feedback()
                    .isSuccess();

        ListTasksPage tasksPage = basicPage.listTasks();
        PrismForm<AssignmentHolderBasicTab<TaskPage>> taskForm = tasksPage
                .table()
                    .search()
                        .byName()
                            .inputValue(name)
                            .updateSearch()
                        .and()
                    .clickByName(name)
                        .selectTabBasic()
                            .form();

        Assert.assertTrue(taskForm.compareInputAttributeValue("name", name));
        Assert.assertTrue(taskForm.compareInputAttributeValue("handlerUri", handler));
    }
}
