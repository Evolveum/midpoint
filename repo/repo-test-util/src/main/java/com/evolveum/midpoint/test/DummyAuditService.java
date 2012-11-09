/**
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */
package com.evolveum.midpoint.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import net.sf.saxon.tree.wrapper.SiblingCountingNode;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;

/**
 * Dummy audit service that only remembers the audit messages in runtime.
 * Only for use in tests.
 *  
 * @author semancik
 *
 */
public class DummyAuditService implements AuditService, Dumpable, DebugDumpable {

	private static DummyAuditService instance = null;
	
	private List<AuditEventRecord> records = new ArrayList<AuditEventRecord>();
	
	public static DummyAuditService getInstance() {
		if (instance == null) {
			instance = new DummyAuditService();
		}
		return instance;
	}
	
	@Override
	public void audit(AuditEventRecord record, Task task) {
		records.add(record.clone());
	}

	public List<AuditEventRecord> getRecords() {
		return records;
	}
	
	public void clear() {
		records.clear();
	}
	
	/**
	 * Asserts that there is a request message followed by execution message.
	 */
	public void assertSimpleRecordSanity() {
		Iterator<AuditEventRecord> iterator = records.iterator();
		int num = 0;
		int numRequests = 0;
		int numExecutions = 0;
		while (iterator.hasNext()) {
			AuditEventRecord record = iterator.next();
			num++;
			assertRecordSanity(""+num+"th record", record);
			
			if (record.getEventStage() == AuditEventStage.REQUEST) {
				numRequests++;
			}
			if (record.getEventStage() == AuditEventStage.EXECUTION) {
				assert numRequests > 0 : "Encountered execution stage audit record without any preceding request: "+record;
				numExecutions++;
			}			
		}
		assert numRequests <= numExecutions : "Strange number of requests and executions; "+numRequests+" requests, "+numExecutions+" executions";
	}

	private void assertRecordSanity(String recordDesc, AuditEventRecord record) {
		assert record != null : "Null audit record ("+recordDesc+")";
		assert !StringUtils.isEmpty(record.getEventIdentifier()) : "No event identifier in audit record ("+recordDesc+")";
		assert !StringUtils.isEmpty(record.getTaskIdentifier()) : "No task identifier in audit record ("+recordDesc+")";
		// TODO
	}

	public void assertRecords(int expectedNumber) {
		assert records.size() == expectedNumber : "Unexpected number of audit records; expected "+expectedNumber+
				" but was "+records.size();
	}
	
	public AuditEventRecord getRequestRecord() {
		assertSingleBatch();
		AuditEventRecord requestRecord = records.get(0);
		assert requestRecord != null : "The first audit record is null";
		assert requestRecord.getEventStage() == AuditEventStage.REQUEST : "The first audit record is not request, it is "+requestRecord;
		return requestRecord;
	}

	public AuditEventRecord getExecutionRecord(int index) {
		assertSingleBatch();
		AuditEventRecord executionRecord = records.get(index+1);
		assert executionRecord != null : "The "+index+"th audit execution record is null";
		assert executionRecord.getEventStage() == AuditEventStage.EXECUTION : "The "+index+"th audit execution record is not execution, it is "+executionRecord;
		return executionRecord;
	}
	
	public List<AuditEventRecord> getExecutionRecords() {
		assertSingleBatch();
		return records.subList(1, records.size());
	}
	
	private void assertSingleBatch() {
		assert records.size() > 1 : "Expected at least two audit records but got "+records.size();
		Iterator<AuditEventRecord> iterator = records.iterator();
		AuditEventRecord requestRecord = iterator.next();
		assert requestRecord.getEventStage() == AuditEventStage.REQUEST : "Expected first record to be request, it was "+requestRecord.getEventStage()+" instead: "+requestRecord;
		while (iterator.hasNext()) {
			AuditEventRecord executionRecord = iterator.next();
			assert executionRecord.getEventStage() == AuditEventStage.EXECUTION : "Expected following record to be execution, it was "+executionRecord.getEventStage()+" instead: "+executionRecord;
		}		
	}

	public void assertAnyRequestDeltas() {
		AuditEventRecord requestRecord = getRequestRecord();
		Collection<ObjectDelta<? extends ObjectType>> requestDeltas = requestRecord.getDeltas();
		assert requestDeltas != null && !requestDeltas.isEmpty() : "Expected some deltas in audit request record but found none";
	}

	public Collection<ObjectDelta<? extends ObjectType>> getExecutionDeltas() {
		return getExecutionDeltas(0);
	}
	
	public Collection<ObjectDelta<? extends ObjectType>> getExecutionDeltas(int index) {
		AuditEventRecord executionRecord = getExecutionRecord(index);
		Collection<ObjectDelta<? extends ObjectType>> deltas = executionRecord.getDeltas();
		assert deltas != null : "Execution audit record has null deltas";
		return deltas;
	}

	public ObjectDelta<?> getExecutionDelta(int index) {
		Collection<ObjectDelta<? extends ObjectType>> deltas = getExecutionDeltas(index);
		assert deltas.size() == 1 : "Execution audit record has more than one deltas, it has "+deltas.size();
		ObjectDelta<?> delta = deltas.iterator().next();
		return delta;
	}
	
	public void assertExecutionDeltaAdd() {
		ObjectDelta<?> delta = getExecutionDelta(0);
		assert delta.isAdd() : "Execution audit record is not add, it is "+delta;
	}
	
	public void assertExecutionSuccess() {
		assertExecutionOutcome(OperationResultStatus.SUCCESS);
	}
	
	public void assertExecutionOutcome(OperationResultStatus expectedStatus) {
		List<AuditEventRecord> executionRecords = getExecutionRecords();
		for (AuditEventRecord executionRecord: executionRecords) {
			assert executionRecord.getOutcome() == expectedStatus : "Expected execution outcome "+expectedStatus+" in audit record but it was "+executionRecord.getOutcome();
		}
	}
	
	public void assertNoRecord() {
		assert records.isEmpty() : "Expected no audit record but some sneaked in: "+records;
	}

	@Override
	public String toString() {
		return "DummyAuditService(" + records + ")";
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("DummyAuditService: ").append(records.size()).append(" records\n");
		DebugUtil.debugDump(sb, records, indent + 1, false);
		return sb.toString();
	}

	@Override
	public String dump() {
		return debugDump();
	}

}
