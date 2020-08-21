/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.jetbrains.annotations.NotNull;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.audit.api.AuditReferenceValue;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sql.data.audit.*;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.test.NullTaskImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

@ContextConfiguration(locations = { "../../../../../ctx-test.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AuditTest extends BaseSQLRepoTest {

    @Test
    public void test100AuditSimple() {
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
        AuditEventRecord loaded = getAuditEventRecord(1, 0);
        System.out.println("Record loaded:\n" + loaded.debugDump());
        System.out.println("Repo ID: " + loaded.getRepoId());
        assertThat(loaded.getProperties()).withFailMessage("Wrong # of properties").hasSize(3);
        assertThat(loaded.getPropertyValues("prop1")).withFailMessage("Wrong prop1 values")
                .containsExactlyInAnyOrder("val1.1", "val1.2");
        assertThat(loaded.getPropertyValues("prop2")).withFailMessage("Wrong prop2 values")
                .containsExactlyInAnyOrder("val2");
        assertThat(loaded.getPropertyValues("prop3")).withFailMessage("Wrong prop3 values")
                .containsExactlyInAnyOrder((String) null);
        assertThat(loaded.getReferences()).withFailMessage("Wrong # of references").hasSize(3);
        assertThat(loaded.getReferenceValues("ref1")).withFailMessage("Wrong ref1 values")
                .containsExactlyInAnyOrder(refVal1_1, refVal1_2);
        assertThat(loaded.getReferenceValues("ref2")).withFailMessage("Wrong ref2 values")
                .containsExactlyInAnyOrder(refVal2);
        assertThat(loaded.getReferenceValues("ref3")).withFailMessage("Wrong ref3 values")
                .containsExactlyInAnyOrder(refVal3);
    }

    private PolyString poly(String orig) {
        return new PolyString(orig, prismContext.getDefaultPolyStringNormalizer().normalize(orig));
    }

    @Test
    public void test110AuditSecond() {
        when();
        AuditEventRecord record = new AuditEventRecord();
        record.addPropertyValue("prop", "val");
        logger.info("Adding audit record {}", record);
        auditService.audit(record, new NullTaskImpl());

        then();
        System.out.println("Record written:\n" + record.debugDump());
        System.out.println("Repo ID: " + record.getRepoId());
        AuditEventRecord loaded = getAuditEventRecord(2, 1);
        System.out.println("Record loaded:\n" + loaded.debugDump());
        System.out.println("Repo ID: " + loaded.getRepoId());
        assertThat(loaded.getProperties()).withFailMessage("Wrong # of properties").hasSize(1);
        assertThat(loaded.getPropertyValues("prop"))
                .withFailMessage("Wrong prop values")
                .containsExactlyInAnyOrder("val");
        assertThat(loaded.getReferences()).withFailMessage("Wrong # of references").isEmpty();
    }

    @Test
    public void testAudit() {
        AuditEventRecord record = new AuditEventRecord();
        record.setChannel("http://midpoint.evolveum.com/xml/ns/public/provisioning/channels-3#import");
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

    // TODO MID-6318 migrate to other types, perhaps QAuditEventRecord or utilize searchObject or SqlQueryExecutor at least
    private AuditEventRecord getAuditEventRecord(int expectedCount, int index) {
        try (Session session = getFactory().openSession()) {
            session.beginTransaction();
            Query<RAuditEventRecord> query = session.createQuery(
                    "from " + RAuditEventRecord.class.getSimpleName() + " order by id",
                    RAuditEventRecord.class);
            List<RAuditEventRecord> records = query.list();
            assertThat(records).hasSize(expectedCount);
            AuditEventRecord eventRecord = fromRepo(records.get(index));
            session.getTransaction().commit();
            return eventRecord;
        }
    }

    // TODO MID-6318 both fromRepo methods moved and changed from R* classes, this was the only usage, migrate to non-R types
    private AuditEventRecord fromRepo(RAuditEventRecord repo) {
        AuditEventRecord audit = new AuditEventRecord();
        audit.setChannel(repo.getChannel());
        audit.setEventIdentifier(repo.getEventIdentifier());
        if (repo.getEventStage() != null) {
            audit.setEventStage(repo.getEventStage().getStage());
        }
        if (repo.getEventType() != null) {
            audit.setEventType(repo.getEventType().getType());
        }
        audit.setHostIdentifier(repo.getHostIdentifier());
        audit.setRemoteHostAddress(repo.getRemoteHostAddress());
        audit.setNodeIdentifier(repo.getNodeIdentifier());
        audit.setMessage(repo.getMessage());

        if (repo.getOutcome() != null) {
            audit.setOutcome(repo.getOutcome().getStatus());
        }
        audit.setParameter(repo.getParameter());
        audit.setResult(repo.getResult());
        audit.setSessionIdentifier(repo.getSessionIdentifier());
        audit.setRequestIdentifier(repo.getRequestIdentifier());
        audit.setTaskIdentifier(repo.getTaskIdentifier());
        audit.setTaskOid(repo.getTaskOID());
        if (repo.getTimestamp() != null) {
            audit.setTimestamp(repo.getTimestamp().getTime());
        }

        for (RTargetResourceOid resourceOID : repo.getResourceOids()) {
            audit.getResourceOids().add(resourceOID.getResourceOid());
        }

        List<ObjectDeltaOperation<?>> odos = new ArrayList<>();
        for (RObjectDeltaOperation rodo : repo.getDeltas()) {
            try {
                ObjectDeltaOperation<?> odo = fromRepo(rodo);
                odos.add(odo);
            } catch (Exception ex) {
                // TODO: for now this is OK, if we cannot parse delta, just skip it...
                // Have to be resolved later.
            }
        }

        audit.addDeltas(odos);

        for (RAuditPropertyValue rPropertyValue : repo.getPropertyValues()) {
            audit.addPropertyValue(rPropertyValue.getName(), rPropertyValue.getValue());
        }
        for (RAuditReferenceValue rRefValue : repo.getReferenceValues()) {
            audit.addReferenceValue(rRefValue.getName(), fromRepo(rRefValue));
        }

        audit.setRepoId(repo.getId());

        return audit;
        // initiator, attorney, target, targetOwner
    }

    private AuditReferenceValue fromRepo(RAuditReferenceValue refValue) {
        return new AuditReferenceValue(refValue.getOid(),
                RUtil.stringToQName(refValue.getType()),
                RPolyString.fromRepo(refValue.getTargetName(), prismContext));
    }

    @NotNull
    private ObjectDeltaOperation<?> fromRepo(RObjectDeltaOperation operation)
            throws DtoTranslationException {

        ObjectDeltaOperation<?> odo = new ObjectDeltaOperation<>();
        try {
            if (operation.getDelta() != null) {
                byte[] data = operation.getDelta();
                String serializedDelta = RUtil.getSerializedFormFromBytes(data,
                        baseHelper.getConfiguration().isUsingSQLServer());

                ObjectDeltaType delta = prismContext.parserFor(serializedDelta)
                        .parseRealValue(ObjectDeltaType.class);
                odo.setObjectDelta(DeltaConvertor.createObjectDelta(delta, prismContext));
            }
            if (operation.getFullResult() != null) {
                byte[] data = operation.getFullResult();
                String serializedResult = RUtil.getSerializedFormFromBytes(data,
                        baseHelper.getConfiguration().isUsingSQLServer());

                OperationResultType resultType = prismContext.parserFor(serializedResult)
                        .parseRealValue(OperationResultType.class);
                odo.setExecutionResult(OperationResult.createOperationResult(resultType));
            }
            odo.setObjectName(RPolyString.fromRepo(operation.getObjectName(), prismContext));
            odo.setResourceOid(operation.getResourceOid());
            odo.setResourceName(RPolyString.fromRepo(operation.getResourceName(), prismContext));
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }

        return odo;
    }
}
