/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.audit.api.AuditReferenceValue;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.util.ShadowAssociationsUtil;
import com.evolveum.midpoint.test.DummyAuditService;
import com.evolveum.midpoint.cases.api.AuditingConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.testng.AssertJUnit.*;

public class WfTestUtil {

    public static void checkWorkItemAuditRecords(Map<String, WorkflowResult> expectedResults, DummyAuditService dummyAuditService) {
        List<AuditEventRecord> workItemRecords = dummyAuditService.getRecordsOfType(AuditEventType.WORK_ITEM);
        assertEquals("Unexpected number of work item audit records", expectedResults.size() * 2, workItemRecords.size());
        for (AuditEventRecord record : workItemRecords) {
            if (record.getEventStage() != AuditEventStage.EXECUTION) {
                continue;
            }
            if (record.getDeltas().size() != 1) {
                System.out.println("Record:\n" + record.debugDump());
                fail("Wrong # of deltas in work item audit record: " + record.getDeltas().size());
            }
            ObjectDelta<? extends ObjectType> delta = record.getDeltas().iterator().next().getObjectDelta();
            Containerable valueToAdd = ((PrismContainerValue) delta.getModifications().iterator().next().getValuesToAdd()
                    .iterator().next()).asContainerable();
            String oid;
            if (valueToAdd instanceof AssignmentType) {
                oid = ((AssignmentType) valueToAdd).getTargetRef().getOid();
            } else if (valueToAdd instanceof ShadowAssociationValueType) {
                oid = ShadowAssociationsUtil.getSingleObjectRefRequired((ShadowAssociationValueType) valueToAdd).getOid();
            } else {
                continue;
            }
            assertTrue("Unexpected target to approve: " + oid, expectedResults.containsKey(oid));
            assertEquals("Unexpected result for " + oid + ": " + record.getResult(), expectedResults.get(oid),
                    WorkflowResult.fromNiceWfAnswer(record.getResult()));
        }
    }

    public static void checkWfProcessAuditRecords(Map<String, WorkflowResult> expectedResults, DummyAuditService dummyAuditService) {
        List<AuditEventRecord> records = dummyAuditService.getRecordsOfType(AuditEventType.WORKFLOW_PROCESS_INSTANCE);
        assertEquals("Unexpected number of workflow process instance audit records", expectedResults.size() * 2, records.size());
        for (AuditEventRecord record : records) {
            if (record.getEventStage() != AuditEventStage.EXECUTION) {
                continue;
            }
            Set<AuditReferenceValue> targetRef = record.getReferenceValues(AuditingConstants.AUDIT_TARGET);
            assertEquals("Wrong # of targetRef values in " + record.debugDump(), 1, targetRef.size());
            String oid = targetRef.iterator().next().getOid();
            assertTrue("Unexpected role to approve: " + oid, expectedResults.containsKey(oid));
            assertEquals("Unexpected result for " + oid + ": " + record.getResult(), expectedResults.get(oid),
                    WorkflowResult.fromNiceWfAnswer(record.getResult()));
            if (expectedResults.get(oid) == WorkflowResult.APPROVED) {
                assertEquals("Wrong # of deltas in " + record.debugDump(), 1, record.getDeltas().size());
            }
        }
    }

    public static void assertRef(String what, ObjectReferenceType ref, String oid, boolean targetName, boolean fullObject) {
        assertNotNull(what + " is null", ref);
        assertNotNull(what + " contains no OID", ref.getOid());
        if (oid != null) {
            assertEquals(what + " contains wrong OID", oid, ref.getOid());
        }
        if (targetName) {
            assertNotNull(what + " contains no target name", ref.getTargetName());
        }
        if (fullObject) {
            assertNotNull(what + " contains no object", ref.asReferenceValue().getObject());
        }
    }
}
