/**
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.audit.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.task.api.LightweightIdentifier;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author semancik
 *
 */
@Service
public class AuditServiceImpl implements AuditService {
	
	private static final Trace LOGGER = TraceManager.getTrace(AuditServiceImpl.class);

	@Autowired
	private LightweightIdentifierGenerator lightweightIdentifierGenerator;
	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.audit.AuditService#audit(com.evolveum.midpoint.common.audit.AuditEventRecord)
	 */
	@Override
	public void audit(AuditEventRecord record, Task task) {
		
		completeRecord(record, task);
		recordRecord(record);

	}

	/**
	 * Complete the record with data that can be computed or discovered from the environment
	 */
	private void completeRecord(AuditEventRecord record, Task task) {
		LightweightIdentifier id = null;
		if (record.getEventIdentifier() == null) {
			id = lightweightIdentifierGenerator.generate();
			record.setEventIdentifier(id.toString());
		}
		if (record.getTimestamp() == null) {
			if (id == null) {
				record.setTimestamp(System.currentTimeMillis());
			} else {
				// To be consistent with the ID
				record.setTimestamp(id.getTimestamp());
			}
		}
		// TODO
	}
	
	private void recordRecord(AuditEventRecord record) {
		// FIXME: hardcoded auditing to a system log
		LOGGER.info("{}",record);
	}

}
