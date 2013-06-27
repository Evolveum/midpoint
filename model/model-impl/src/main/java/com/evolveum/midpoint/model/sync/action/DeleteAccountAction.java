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

package com.evolveum.midpoint.model.sync.action;

import com.evolveum.midpoint.model.util.Utils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.lens.SynchronizationIntent;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SynchronizationSituationType;

/**
 * @author lazyman
 */
public class DeleteAccountAction extends ModifyUserAction {

	public DeleteAccountAction() {
		super(SynchronizationIntent.DELETE, ACTION_DELETE_ACCOUNT);
	}

	@Override
	public String executeChanges(String userOid, ResourceObjectShadowChangeDescription change,
			ObjectTemplateType userTemplate, SynchronizationSituationType situation,
			AuditEventRecord auditRecord, Task task, OperationResult result) throws SchemaException,
			PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException,
			ObjectAlreadyExistsException, CommunicationException, ConfigurationException,
			SecurityViolationException {

		// found account does not have owner, account should be deleted
		if (StringUtils.isEmpty(userOid)) {
			OperationResult subResult = result.createSubresult(ACTION_DELETE_ACCOUNT);
			String accOid = null;
			if (change.getCurrentShadow() != null) {
				accOid = change.getCurrentShadow().getOid();
			} else if (change.getOldShadow() != null) {
				accOid = change.getOldShadow().getOid();
			} else if (change.getOldShadow() != null) {
				accOid = change.getObjectDelta().getOid();
			}

			if (StringUtils.isEmpty(accOid)) {
				String message = "Can't delete account, account oid is empty or null.";
				subResult.recordFatalError(message);
				throw new SchemaException(message);
			}

            //task.setRequesteeOidImmediate(userOid, result);
            Utils.setRequestee(task, userOid);
            getProvisioningService().deleteObject(ShadowType.class, accOid, null, null, task, subResult);
            Utils.clearRequestee(task);
			subResult.recordSuccess();
			return null;
		} else {
			return super.executeChanges(userOid, change, userTemplate, situation, auditRecord, task, result);
		}
	}
}
