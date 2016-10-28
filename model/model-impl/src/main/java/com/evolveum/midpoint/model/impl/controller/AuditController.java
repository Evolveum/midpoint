/**
 * Copyright (c) 2016 Evolveum
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
package com.evolveum.midpoint.model.impl.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.model.api.ModelAuditService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 *
 */
@Component
public class AuditController implements ModelAuditService {
	
	@Autowired
	private AuditService auditService;

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.audit.api.AuditService#audit(com.evolveum.midpoint.audit.api.AuditEventRecord, com.evolveum.midpoint.task.api.Task)
	 */
	@Override
	public void audit(AuditEventRecord record, Task task) {
		// TODO: authorizations
		auditService.audit(record, task);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.audit.api.AuditService#listRecords(java.lang.String, java.util.Map)
	 */
	@Override
	public List<AuditEventRecord> listRecords(String query, Map<String, Object> params) {
		// TODO: authorizations
		return auditService.listRecords(query, params);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.audit.api.AuditService#countObjects(java.lang.String, java.util.Map)
	 */
	@Override
	public long countObjects(String query, Map<String, Object> params) {
		// TODO: authorizations
		return auditService.countObjects(query, params);
	}

	@Override
	public void cleanupAudit(CleanupPolicyType policy, OperationResult parentResult) {
		// TODO: authorizations
		auditService.cleanupAudit(policy, parentResult);
	}
	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.audit.api.AuditService#supportsRetrieval()
	 */
	@Override
	public boolean supportsRetrieval() {
		return auditService.supportsRetrieval();
	}

	@Override
	public <O extends ObjectType> PrismObject<O> reconstructObject(String oid, String eventIdentifier) {
		// TODO: authorizations
		// TODO Auto-generated method stub
		return null;
	}

}
