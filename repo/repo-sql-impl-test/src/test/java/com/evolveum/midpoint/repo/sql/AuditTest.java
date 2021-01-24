/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.audit.api.AuditReferenceValue;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sql.audit.AuditSqlQueryContext;
import com.evolveum.midpoint.repo.sql.audit.beans.MAuditEventRecord;
import com.evolveum.midpoint.repo.sql.audit.beans.MAuditRefValue;
import com.evolveum.midpoint.repo.sql.audit.querymodel.QAuditEventRecord;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.repo.sqlbase.*;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.task.api.test.NullTaskImpl;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@ContextConfiguration(locations = { "../../../../../ctx-test.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AuditTest extends BaseSQLRepoTest {

    @Test
    public void test100AuditSimple() throws QueryException {
        when();
        AuditEventRecord record = new AuditEventRecord();
        record.addPropertyValue("prop1", "val1.1");
        record.addPropertyValue("prop1", "val1.2");
        record.addPropertyValue("prop2", "val2");
        record.addPropertyValue("prop3", null);
        AuditReferenceValue refVal1_1 =
                new AuditReferenceValue("oid1.1", UserType.COMPLEX_TYPE, poly("user1.1"));
        AuditReferenceValue refVal1_2 =
                new AuditReferenceValue("oid1.2", RoleType.COMPLEX_TYPE, poly("role1.2"));
        AuditReferenceValue refVal2 = new AuditReferenceValue("oid2", null, poly("object2"));
        AuditReferenceValue refVal3 = new AuditReferenceValue();
        record.addReferenceValue("ref1", refVal1_1);
        record.addReferenceValue("ref1", refVal1_2);
        record.addReferenceValue("ref2", refVal2);
        record.addReferenceValue("ref3", refVal3);
        logger.info("Adding audit record {}", record);
        auditService.audit(record, new NullTaskImpl());

        then();
        System.out.println("Record written:\n" + record.debugDump());
        System.out.println("Repo ID: " + record.getRepoId());
        MAuditEventRecord loaded = getAuditEventRecord(1, 0);
        System.out.println("Record loaded:\n" + loaded);
        assertThat(loaded.properties).withFailMessage("Wrong # of properties").hasSize(3);
        assertThat(loaded.properties.get("prop1")).describedAs("prop1 values")
                .containsExactlyInAnyOrder("val1.1", "val1.2");
        assertThat(loaded.properties.get("prop2")).describedAs("prop2 values")
                .containsExactlyInAnyOrder("val2");
        assertThat(loaded.properties.get("prop3")).describedAs("prop3 values")
                .containsExactlyInAnyOrder((String) null);
        assertThat(loaded.refValues).withFailMessage("Wrong # of references").hasSize(3);
        assertThat(loaded.refValues.get("ref1")).describedAs("ref1 values")
                .flatExtracting(this::toAuditReferenceValue)
                .containsExactlyInAnyOrder(refVal1_1, refVal1_2);
        assertThat(loaded.refValues.get("ref2")).describedAs("ref2 values")
                .flatExtracting(this::toAuditReferenceValue)
                .containsExactlyInAnyOrder(refVal2);
        assertThat(loaded.refValues.get("ref3")).describedAs("ref3 values")
                .flatExtracting(this::toAuditReferenceValue)
                .containsExactlyInAnyOrder(refVal3);
    }

    @Test
    public void test110AuditSecond() throws QueryException {
        when();
        AuditEventRecord record = new AuditEventRecord();
        record.addPropertyValue("prop", "val");
        logger.info("Adding audit record {}", record);
        auditService.audit(record, new NullTaskImpl());

        then();
        System.out.println("Record written:\n" + record.debugDump());
        System.out.println("Repo ID: " + record.getRepoId());
        MAuditEventRecord loaded = getAuditEventRecord(2, 1);
        System.out.println("Record loaded:\n" + loaded);
        assertThat(loaded.properties).describedAs("# of properties").hasSize(1);
        assertThat(loaded.properties.get("prop"))
                .withFailMessage("Wrong prop values")
                .containsExactlyInAnyOrder("val");
        // not initialized if nothing is there, so it's null (meaning empty)
        assertThat(loaded.refValues).describedAs("# of references").isNullOrEmpty();
    }

    @Test
    public void test200AuditDelta() {
        AuditEventRecord record = new AuditEventRecord();
        record.setChannel("http://midpoint.evolveum.com/xml/ns/public/common/channels-3#import");
        record.setEventIdentifier("1511974895961-0-1");
        record.setEventStage(AuditEventStage.EXECUTION);
        record.setEventType(AuditEventType.ADD_OBJECT);

        ObjectDeltaOperation<UserType> delta = new ObjectDeltaOperation<>();
        delta.setObjectDelta(
                prismContext.deltaFactory().object().createModificationAddReference(
                        UserType.class, "1234", UserType.F_LINK_REF, "123"));
        record.addDelta(delta);

        delta = new ObjectDeltaOperation<>();
        delta.setObjectDelta(
                prismContext.deltaFactory().object().createModificationAddReference(
                        UserType.class, "1234", UserType.F_LINK_REF, "124"));
        record.addDelta(delta);

        auditService.audit(record, new NullTaskImpl());
    }

    private PolyString poly(String orig) {
        return new PolyString(orig, prismContext.getDefaultPolyStringNormalizer().normalize(orig));
    }

    /**
     * Creates {@link AuditReferenceValue} equivalent of {@link MAuditRefValue} for comparison.
     */
    private AuditReferenceValue toAuditReferenceValue(MAuditRefValue refValue) {
        AuditReferenceValue arv = new AuditReferenceValue();
        arv.setOid(refValue.oid);
        arv.setType(RUtil.stringToQName(refValue.type));
        if (refValue.targetNameOrig != null || refValue.targetNameNorm != null) {
            arv.setTargetName(new PolyString(refValue.targetNameOrig, refValue.targetNameNorm));
        }
        return arv;
    }

    @Autowired
    private SqlAuditServiceFactory auditServiceFactory;

    private MAuditEventRecord getAuditEventRecord(int expectedCount, int index)
            throws QueryException {
        // "create" does not actually create a new audit service, but returns the existing one
        SqlRepoContext sqlRepoContext = auditServiceFactory.createAuditService().getSqlRepoContext();
        SqlTransformerContext transformerContext = new SqlTransformerContext(prismContext, null);
        SqlQueryContext<AuditEventRecordType, QAuditEventRecord, MAuditEventRecord> context =
                AuditSqlQueryContext.from(
                        AuditEventRecordType.class, transformerContext, sqlRepoContext);
        QAuditEventRecord aer = context.root();
        context.sqlQuery().orderBy(aer.id.asc());

        try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession().startReadOnlyTransaction()) {
            PageOf<MAuditEventRecord> result = context.executeQuery(jdbcSession.connection())
                    .map(t -> t.get(aer));

            assertThat(result).hasSize(expectedCount);
            return result.get(index);
        }
    }
}
