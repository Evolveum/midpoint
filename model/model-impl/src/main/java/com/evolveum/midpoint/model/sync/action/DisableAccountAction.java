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

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.lens.SynchronizationIntent;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SynchronizationSituationType;

/**
 * @author Vilo Repan
 */
public class DisableAccountAction extends ModifyUserAction {
	
	private static final Trace LOGGER = TraceManager.getTrace(DisableAccountAction.class);

    public DisableAccountAction() {
        super(SynchronizationIntent.KEEP, ACTION_DISABLE_ACCOUNT);
        setAccountActivationDecision(ActivationDecision.DISABLE);
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
			OperationResult subResult = result.createSubresult(ACTION_DISABLE_ACCOUNT);
			String accOid = null;
			PrismObject<? extends ShadowType> shadow = null;
			if (change.getCurrentShadow() != null) {
				accOid = change.getCurrentShadow().getOid();
				shadow = change.getCurrentShadow();
			} else if (change.getOldShadow() != null) {
				accOid = change.getOldShadow().getOid();
				shadow = change.getOldShadow();
			} else if (change.getOldShadow() != null) {
				accOid = change.getObjectDelta().getOid();
			}

			if (StringUtils.isEmpty(accOid)) {
				String message = "Can't disable account, account oid is empty or null.";
				subResult.recordFatalError(message);
				throw new SchemaException(message);
			}
			
			if (shadow == null) {
				String message = "Can't disable account, account does not exist.";
				subResult.recordFatalError(message);
				throw new SchemaException(message);
			}

			PrismProperty oldValue = shadow.findOrCreateProperty(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
			
			if (oldValue == null){
				String message = "Could not find item for path: " + SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS +" for object: " + shadow.dump() ;
				subResult.recordFatalError(message);
				throw new SchemaException(message);
			}
			
			PropertyDelta delta = new PropertyDelta<Object>(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, oldValue.getDefinition());
			
	        PrismPropertyValue value = new PrismPropertyValue<Object>(ActivationStatusType.DISABLED, OriginType.SYNC_ACTION, null);
	        if (oldValue == null || oldValue.getValue() == null || oldValue.getValue().isEmpty()) {
	            delta.addValueToAdd(value);
	        } else {
	            Collection<PrismPropertyValue<Object>> values = new ArrayList<PrismPropertyValue<Object>>();
	            values.add(value);
	            delta.setValuesToReplace(values);
	        }

	        if (LOGGER.isDebugEnabled()) {
	            LOGGER.trace("{} activation property delta: {}", new Object[]{delta.getClass().getSimpleName(),
	                    delta.debugDump()});
	        }
			
			Collection<? extends ItemDelta> modifications = MiscUtil.createCollection(delta);
			
			getProvisioningService().modifyObject(ShadowType.class, accOid, modifications, null, null, task, subResult);
			subResult.recordSuccess();
			return null;
		} else {
			return super.executeChanges(userOid, change, userTemplate, situation, auditRecord, task, result);
		}
	}
}
