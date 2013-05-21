/*
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
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */

package com.evolveum.midpoint.model.sync.action;

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

//	@Autowired(required = true)
//	private ProvisioningService provisioning;

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

			getProvisioningService().deleteObject(ShadowType.class, accOid, null, null, task, subResult);
			subResult.recordSuccess();
			return null;
		} else {
			return super.executeChanges(userOid, change, userTemplate, situation, auditRecord, task, result);
		}
	}
}
