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

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.sql.data.audit.RAuditEventRecord;
import com.evolveum.midpoint.repo.sql.data.common.container.Container;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.RTask;
import com.evolveum.midpoint.repo.sql.type.XMLGregorianCalendarType;
import com.evolveum.midpoint.repo.sql.util.MidPointNamingStrategy;
import com.evolveum.midpoint.repo.sql.util.SimpleTaskAdapter;
import com.evolveum.midpoint.repo.sql.util.UnicodeSQLServer2008Dialect;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CleanupPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.File;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class CleanupTest extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(CleanupTest.class);

    @Test(enabled = false)
    public void testTasksCleanup() throws Exception {
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
        // doExperimentalCleanup(when);

        // THEN
        List<PrismObject<TaskType>> tasks = repositoryService.searchObjects(TaskType.class, null, null, result);
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

    private void doExperimentalCleanup(Calendar when) {
        //experimental stuff before integration to main implementation
        Session session = getFactory().openSession();
        try {
            session.beginTransaction();

            MidPointNamingStrategy namingStrategy = new MidPointNamingStrategy();
            final String taskTableName = namingStrategy.classToTableName(RTask.class.getSimpleName());
            final String objectTableName = namingStrategy.classToTableName(RObject.class.getSimpleName());
            final String containerTableName = namingStrategy.classToTableName(Container.class.getSimpleName());

            final String completionTimestampColumn = "completionTimestamp";

            Dialect dialect = Dialect.getDialect(sessionFactoryBean.getHibernateProperties());
            //create temporary table
            String prefix = "";
            if (UnicodeSQLServer2008Dialect.class.equals(dialect.getClass())) {
                prefix = "#";   //this creates temporary table as with global scope
            }
            final String tempTable = prefix + dialect.generateTemporaryTableName(taskTableName);

            StringBuilder sb = new StringBuilder();
            sb.append(dialect.getCreateTemporaryTableString());
            sb.append(' ').append(tempTable).append(" (oid ");
            sb.append(dialect.getTypeName(Types.VARCHAR, 36, 0, 0));
            sb.append(" not null)");
            sb.append(dialect.getCreateTemporaryTablePostfix());

            SQLQuery query = session.createSQLQuery(sb.toString());
            query.executeUpdate();

            //fill temporary table
            sb = new StringBuilder();
            sb.append("insert into ").append(tempTable).append(' ');            //todo improve this insert
            sb.append("select t.oid as oid from ").append(taskTableName).append(" t");
            sb.append(" inner join ").append(objectTableName).append(" o on t.id = o.id and t.oid = o.oid");
            sb.append(" inner join ").append(containerTableName).append(" c on t.id = c.id and t.oid = c.oid");
            sb.append(" where t.").append(completionTimestampColumn).append(" < ?");

            query = session.createSQLQuery(sb.toString());
            query.setParameter(0, new Timestamp(when.getTimeInMillis()));
            query.executeUpdate();

            //drop records from m_task, m_object, m_container
            sb = new StringBuilder();
            sb.append("delete from ").append(taskTableName);
            sb.append(" where id = 0 and (oid in (select oid from ").append(tempTable).append("))");
            session.createSQLQuery(sb.toString()).executeUpdate();

            sb = new StringBuilder();
            sb.append("delete from ").append(objectTableName);
            sb.append(" where id = 0 and (oid in (select oid from ").append(tempTable).append("))");
            session.createSQLQuery(sb.toString()).executeUpdate();

            sb = new StringBuilder();
            sb.append("delete from ").append(containerTableName);
            sb.append(" where id = 0 and (oid in (select oid from ").append(tempTable).append("))");
            long count = session.createSQLQuery(sb.toString()).executeUpdate();

            LOGGER.info("REMOVED {} OBJECTS.", new Object[]{count});

            //drop temporary table
            if (dialect.dropTemporaryTableAfterUse()) {
                LOGGER.info("Dropping temporary table.");
                sb = new StringBuilder();
                sb.append(dialect.getDropTemporaryTableString());
                sb.append(' ').append(tempTable);

                session.createSQLQuery(sb.toString()).executeUpdate();
            }

            session.getTransaction().commit();
        } finally {
            session.close();
        }
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
        long seconds = (now - when.getTimeInMillis()) / 1000;
        return DatatypeFactory.newInstance().newDuration("PT" + seconds + "S").negate();
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
            record.addDelta(createObjectDeltaOperation(i));
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

            Date finished = new Date(record.getTimestamp().getTime());

            Date mark = new Date(NOW);
            Duration duration = policy.getMaxAge();
            duration.addTo(mark);

            AssertJUnit.assertTrue("finished: " + finished + ", mark: " + mark, finished.after(mark));

            session.getTransaction().commit();
        } finally {
            session.close();
        }
    }

    private ObjectDeltaOperation createObjectDeltaOperation(int i) throws Exception {
        ObjectDeltaOperation delta = new ObjectDeltaOperation();
        delta.setExecutionResult(new OperationResult("asdf"));
        UserType user = new UserType();
        prismContext.adopt(user);
        PolyStringType name = new PolyStringType();
        name.setOrig("a" + i);
        name.setNorm("a" + i);
        user.setName(name);

        delta.setObjectDelta(ObjectDelta.createAddDelta(user.asPrismObject()));

        return delta;
    }
}
