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

package com.evolveum.midpoint.task.quartzimpl;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sql.type.XMLGregorianCalendarType;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {"classpath:ctx-task.xml",
        "classpath:ctx-task-test.xml",
        "classpath:ctx-repo-cache.xml",
        "classpath:ctx-repo-common.xml",
        "classpath:ctx-expression-test.xml",
        "classpath*:ctx-repository-test.xml",
        "classpath:ctx-audit.xml",
        "classpath:ctx-security.xml",
        "classpath:ctx-common.xml",
        "classpath:ctx-configuration-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class CleanupTest extends AbstractTestNGSpringContextTests {

    private static final Trace LOGGER = TraceManager.getTrace(CleanupTest.class);

    public static final File FOLDER_REPO = new File("./src/test/resources/repo");

    @Autowired
    private TaskManagerQuartzImpl taskManager;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private  PrismContext prismContext;

    @Test
    public void testTasksCleanup() throws Exception {

        // GIVEN
        final File file = new File(FOLDER_REPO, "tasks-for-cleanup.xml");
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
        Calendar when = create_2013_07_12_12_00_Calendar();
        CleanupPolicyType policy = createPolicy(when, NOW);

        taskManager.cleanupTasks(policy, taskManager.createTaskInstance(), result);

        // THEN
        List<PrismObject<TaskType>> tasks = repositoryService.searchObjects(TaskType.class, null, null, result);
        AssertJUnit.assertNotNull(tasks);
        display("tasks", tasks);
        AssertJUnit.assertEquals(1, tasks.size());

        PrismObject<TaskType> task = tasks.get(0);
        XMLGregorianCalendar timestamp = task.getPropertyRealValue(TaskType.F_COMPLETION_TIMESTAMP,
                XMLGregorianCalendar.class);
        Date finished = XMLGregorianCalendarType.asDate(timestamp);

        Date mark = new Date(NOW);
        Duration duration = policy.getMaxAge();
        duration.addTo(mark);

        AssertJUnit.assertTrue("finished: " + finished + ", mark: " + mark, finished.after(mark));
    }

    private Calendar create_2013_07_12_12_00_Calendar() {
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
