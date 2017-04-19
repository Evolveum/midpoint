/*
 * Copyright (c) 2010-2016 Evolveum
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

package com.evolveum.midpoint.wf.impl;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.test.DummyAuditService;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author mederly
 */
public class WfTestUtil {

	public static void checkWorkItemAuditRecords(Map<String, WorkflowResult> expectedResults, DummyAuditService dummyAuditService) {
		// TODO
//		List<AuditEventRecord> workItemRecords = dummyAuditService.getRecordsOfType(AuditEventType.WORK_ITEM);
//		assertEquals("Unexpected number of work item audit records", expectedResults.size() * 2, workItemRecords.size());
//		for (AuditEventRecord record : workItemRecords) {
//			if (record.getEventStage() != AuditEventStage.EXECUTION) {
//				continue;
//			}
//			if (record.getDeltas().size() != 1) {
//				fail("Wrong # of deltas in work item audit record: " + record.getDeltas().size());
//			}
//			ObjectDelta<? extends ObjectType> delta = record.getDeltas().iterator().next().getObjectDelta();
//			Containerable valueToAdd = ((PrismContainerValue) delta.getModifications().iterator().next().getValuesToAdd()
//					.iterator().next()).asContainerable();
//			String oid;
//			if (valueToAdd instanceof AssignmentType) {
//				oid = ((AssignmentType) valueToAdd).getTargetRef().getOid();
//			} else if (valueToAdd instanceof ShadowAssociationType) {
//				oid = ((ShadowAssociationType) valueToAdd).getShadowRef().getOid();
//			} else {
//				continue;
//			}
//			assertNotNull("Unexpected target to approve: " + oid, expectedResults.containsKey(oid));
//			assertEquals("Unexpected result for " + oid + ": " + record.getResult(), expectedResults.get(oid),
//					WorkflowResult.fromNiceWfAnswer(record.getResult()));
//		}
	}

	public static void checkWfProcessAuditRecords(Map<String, WorkflowResult> expectedResults, DummyAuditService dummyAuditService) {
		List<AuditEventRecord> records = dummyAuditService.getRecordsOfType(AuditEventType.WORKFLOW_PROCESS_INSTANCE);
		assertEquals("Unexpected number of workflow process instance audit records", expectedResults.size() * 2, records.size());
		for (AuditEventRecord record : records) {
			if (record.getEventStage() != AuditEventStage.EXECUTION) {
				continue;
			}
			ObjectDelta<? extends ObjectType> delta = record.getDeltas().iterator().next().getObjectDelta();
			if (!delta.getModifications().isEmpty()) {
				AssignmentType assignmentType = (AssignmentType) ((PrismContainerValue) delta.getModifications().iterator().next()
						.getValuesToAdd().iterator().next()).asContainerable();
				String oid = assignmentType.getTargetRef().getOid();
				assertNotNull("Unexpected role to approve: " + oid, expectedResults.containsKey(oid));
				assertEquals("Unexpected result for " + oid + ": " + record.getResult(), expectedResults.get(oid),
						WorkflowResult.fromNiceWfAnswer(record.getResult()));
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
