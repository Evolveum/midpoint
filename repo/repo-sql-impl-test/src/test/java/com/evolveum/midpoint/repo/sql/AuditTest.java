/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql;

import static org.testng.AssertJUnit.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.audit.api.AuditReferenceValue;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sql.data.audit.RAuditEventRecord;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.task.api.test.NullTaskImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@ContextConfiguration(locations = { "../../../../../ctx-test.xml" })
public class AuditTest extends BaseSQLRepoTest {

    @Test
    public void test100AuditSimple() {
        // WHEN
        AuditEventRecord record = new AuditEventRecord();
        record.addPropertyValue("prop1", "val1.1");
        record.addPropertyValue("prop1", "val1.2");
        record.addPropertyValue("prop2", "val2");
        record.addPropertyValue("prop3", null);
        AuditReferenceValue refVal1_1 = new AuditReferenceValue("oid1.1", UserType.COMPLEX_TYPE, poly("user1.1"));
        AuditReferenceValue refVal1_2 = new AuditReferenceValue("oid1.2", RoleType.COMPLEX_TYPE, poly("role1.2"));
        AuditReferenceValue refVal2 = new AuditReferenceValue("oid2", null, poly("object2"));
        AuditReferenceValue refVal3 = new AuditReferenceValue();
        record.addReferenceValue("ref1", refVal1_1);
        record.addReferenceValue("ref1", refVal1_2);
        record.addReferenceValue("ref2", refVal2);
        record.addReferenceValue("ref3", refVal3);
        logger.info("Adding audit record {}", record);
        auditService.audit(record, new NullTaskImpl());

        // THEN
        System.out.println("Record written:\n" + record.debugDump());
        System.out.println("Repo ID: " + record.getRepoId());
        AuditEventRecord loaded = getAuditEventRecord(1, 0);
        System.out.println("Record loaded:\n" + loaded.debugDump());
        System.out.println("Repo ID: " + loaded.getRepoId());
        assertEquals("Wrong # of properties", 3, loaded.getProperties().size());
        assertEquals("Wrong prop1 values", new HashSet<>(Arrays.asList("val1.1", "val1.2")), loaded.getPropertyValues("prop1"));
        assertEquals("Wrong prop2 values", new HashSet<>(Collections.singletonList("val2")), loaded.getPropertyValues("prop2"));
        assertEquals("Wrong prop3 values", new HashSet<>(Collections.singletonList(null)), loaded.getPropertyValues("prop3"));
        assertEquals("Wrong # of references", 3, loaded.getReferences().size());
        assertEquals("Wrong ref1 values", new HashSet<>(Arrays.asList(refVal1_1, refVal1_2)), loaded.getReferenceValues("ref1"));
        assertEquals("Wrong ref2 values", new HashSet<>(Collections.singletonList(refVal2)), loaded.getReferenceValues("ref2"));
        assertEquals("Wrong ref3 values", new HashSet<>(Collections.singletonList(refVal3)), loaded.getReferenceValues("ref3"));

    }

    private PolyString poly(String orig) {
        return new PolyString(orig, prismContext.getDefaultPolyStringNormalizer().normalize(orig));
    }

    @Test
    public void test110AuditSecond() {
        // WHEN
        AuditEventRecord record = new AuditEventRecord();
        record.addPropertyValue("prop", "val");
        logger.info("Adding audit record {}", record);
        auditService.audit(record, new NullTaskImpl());

        // THEN
        System.out.println("Record written:\n" + record.debugDump());
        System.out.println("Repo ID: " + record.getRepoId());
        AuditEventRecord loaded = getAuditEventRecord(2, 1);
        System.out.println("Record loaded:\n" + loaded.debugDump());
        System.out.println("Repo ID: " + loaded.getRepoId());
        assertEquals("Wrong # of properties", 1, loaded.getProperties().size());
        assertEquals("Wrong prop values", new HashSet<>(Collections.singletonList("val")), loaded.getPropertyValues("prop"));
        assertEquals("Wrong # of references", 0, loaded.getReferences().size());
    }

    @Test
    public void testAudit() {
        AuditEventRecord record = new AuditEventRecord();
        record.setChannel("http://midpoint.evolveum.com/xml/ns/public/provisioning/channels-3#import");
        record.setEventIdentifier("1511974895961-0-1");
        record.setEventStage(AuditEventStage.EXECUTION);
        record.setEventType(AuditEventType.ADD_OBJECT);

        ObjectDeltaOperation delta = new ObjectDeltaOperation();
        delta.setObjectDelta(prismContext.deltaFactory().object().createModificationAddReference(UserType.class, "1234", UserType.F_LINK_REF,
                "123"));
        record.getDeltas().add(delta);

        delta = new ObjectDeltaOperation();
        delta.setObjectDelta(prismContext.deltaFactory().object().createModificationAddReference(UserType.class, "1234", UserType.F_LINK_REF,
                "124"));
        record.getDeltas().add(delta);

        auditService.audit(record, new NullTaskImpl());
    }

    private AuditEventRecord getAuditEventRecord(int expectedCount, int index) {
        Session session = getFactory().openSession();
        try {
            session.beginTransaction();

            Query query = session.createQuery("from " + RAuditEventRecord.class.getSimpleName() + " order by id");
            List<RAuditEventRecord> records = query.list();
            assertEquals(expectedCount, records.size());
            AuditEventRecord eventRecord = RAuditEventRecord.fromRepo(records.get(index), prismContext, baseHelper.getConfiguration().isUsingSQLServer());
            session.getTransaction().commit();
            return eventRecord;
        } finally {
            session.close();
        }

    }

}

