/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.sql.data.audit.RAuditEventRecord;
import com.evolveum.midpoint.repo.sql.type.XMLGregorianCalendarType;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.repo.sql.util.SimpleTaskAdapter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class CleanupTest extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(CleanupTest.class);

    @Test
    public void testTasksCleanup() throws Exception {
        //not a great solution, but better than nothing probably. disabling test if it's h2 DB
        if (isH2used()) {
            LOGGER.info("NOTE: SKIPPING THIS TEST, BECAUSE IT WOULD FAIL ON H2.");
            return;
        }

        // GIVEN
        final File file = new File(FOLDER_BASIC, "tasks.xml");
        List<PrismObject<? extends Objectable>> elements = prismContext.getPrismDomProcessor().parseObjects(file);

        OperationResult result = new OperationResult("tasks cleanup");
        for (int i = 0; i < elements.size(); i++) {
            PrismObject object = elements.get(i);

            String oid = repositoryService.addObject(object, null, result);
            AssertJUnit.assertTrue(StringUtils.isNotEmpty(oid));
        }

        // WHEN
        // because now we can't move system time (we're not using DI for it) we create policy
        // which should always point to 2013-05-07T12:00:00.000+02:00
        final long NOW = System.currentTimeMillis();
        Calendar when = create_2013_07_12_12_00_Calendar();
        CleanupPolicyType policy = createPolicy(when, NOW);

        repositoryService.cleanupTasks(policy, result);

        // THEN
        List<PrismObject<TaskType>> tasks = repositoryService.searchObjects(TaskType.class, null, result);
        AssertJUnit.assertNotNull(tasks);
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
        Calendar calendar = Calendar.getInstance();
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
        DatatypeFactory factory = DatatypeFactory.newInstance();
        Duration duration = factory.newDuration(now - when.getTimeInMillis());
        return duration.negate();
    }

    private CleanupPolicyType createPolicy(Calendar when, long now) throws Exception {
        CleanupPolicyType policy = new CleanupPolicyType();

        Duration duration = createDuration(when, now);
        policy.setMaxAge(duration);

        return policy;
    }

    @Test
    public void testAuditCleanup() throws Exception {
        //GIVEN
        Calendar calendar = create_2013_07_12_12_00_Calendar();
        for (int i = 0; i < 3; i++) {
            long timestamp = calendar.getTimeInMillis();
            AuditEventRecord record = new AuditEventRecord();
            record.setTimestamp(timestamp);
            LOGGER.info("Adding audit record with timestamp {}", new Object[]{new Date(timestamp)});

            auditService.audit(record, new SimpleTaskAdapter());

            calendar.add(Calendar.HOUR_OF_DAY, 1);
        }

        Session session = getFactory().openSession();
        try {
            session.beginTransaction();

            Query query = session.createQuery("select count(*) from " + RAuditEventRecord.class.getSimpleName());
            Long count = (Long) query.uniqueResult();

            AssertJUnit.assertEquals(3L, (long) count);
            session.getTransaction().commit();
        } finally {
            session.close();
        }

        //WHEN
        calendar = create_2013_07_12_12_00_Calendar();
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        calendar.add(Calendar.MINUTE, 1);

        final long NOW = System.currentTimeMillis();
        CleanupPolicyType policy = createPolicy(calendar, NOW);

        OperationResult result = new OperationResult("Cleanup audit");
        auditService.cleanupAudit(policy, result);
        result.recomputeStatus();

        //THEN
        AssertJUnit.assertTrue(result.isSuccess());

        session = getFactory().openSession();
        try {
            session.beginTransaction();

            Query query = session.createQuery("from " + RAuditEventRecord.class.getSimpleName());
            List<RAuditEventRecord> records = query.list();

            AssertJUnit.assertEquals(1, records.size());
            RAuditEventRecord record = records.get(0);

            Date finished = new Date(record.getTimestamp());

            Date mark = new Date(NOW);
            Duration duration = policy.getMaxAge();
            duration.addTo(mark);

            AssertJUnit.assertTrue("finished: " + finished + ", mark: " + mark, finished.after(mark));

            session.getTransaction().commit();
        } finally {
            session.close();
        }
    }
}
