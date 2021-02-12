/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = { "classpath:ctx-task-test.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class CleanupTest extends AbstractTaskManagerTest {

    public static final File FOLDER_BASIC = new File("./src/test/resources/basic");

    @Autowired
    private TaskManagerQuartzImpl taskManager;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private PrismContext prismContext;

    @Test
    public void testTasksCleanup() throws Exception {

        // GIVEN
        final File file = new File(FOLDER_BASIC, "tasks-for-cleanup.xml");
        List<PrismObject<? extends Objectable>> elements = prismContext.parserFor(file).parseObjects();

        OperationResult result = new OperationResult("tasks cleanup");
        for (PrismObject<? extends Objectable> object : elements) {
            String oid = repositoryService.addObject((PrismObject) object, null, result);
            AssertJUnit.assertTrue(StringUtils.isNotEmpty(oid));
        }

        // WHEN
        // because now we can't move system time (we're not using DI for it) we create policy
        // which should always point to 2013-05-07T12:00:00.000+02:00
        final long NOW = System.currentTimeMillis();
        Calendar when = create_2013_05_07_12_00_00_Calendar();
        CleanupPolicyType policy = createPolicy(when, NOW);

        taskManager.cleanupTasks(policy, taskManager.createFakeRunningTask(taskManager.createTaskInstance(), "dummy"), result);

        // THEN
        List<PrismObject<TaskType>> tasks = repositoryService.searchObjects(TaskType.class, null, null, result);
        AssertJUnit.assertNotNull(tasks);
        displayValue("tasks", tasks);
        AssertJUnit.assertEquals(1, tasks.size());

        PrismObject<TaskType> task = tasks.get(0);
        XMLGregorianCalendar timestamp = task.getPropertyRealValue(TaskType.F_COMPLETION_TIMESTAMP,
                XMLGregorianCalendar.class);
        Date finished = timestamp.toGregorianCalendar().getTime();

        Date mark = new Date(NOW);
        Duration duration = policy.getMaxAge();
        duration.addTo(mark);

        AssertJUnit.assertTrue("finished: " + finished + ", mark: " + mark, finished.after(mark));
    }

    private Calendar create_2013_05_07_12_00_00_Calendar() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+2"));
        calendar.set(Calendar.YEAR, 2013);
        calendar.set(Calendar.MONTH, Calendar.MAY);
        calendar.set(Calendar.DAY_OF_MONTH, 7);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar;
    }

    private Duration createDuration(Calendar when, long now) throws Exception {
        long seconds = (now - when.getTimeInMillis()) / 1000;
        return DatatypeFactory.newInstance().newDuration("PT" + seconds + "S").negate();
    }

    private CleanupPolicyType createPolicy(Calendar when, long now) throws Exception {
        CleanupPolicyType policy = new CleanupPolicyType();

        Duration duration = createDuration(when, now);
        policy.setMaxAge(duration);

        return policy;
    }

}
