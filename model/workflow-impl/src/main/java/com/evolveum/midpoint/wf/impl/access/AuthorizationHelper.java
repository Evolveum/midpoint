/*
 * Copyright (c) 2010-2019 Evolveum
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

package com.evolveum.midpoint.wf.impl.access;

import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.model.api.util.DeputyUtils;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *  Helps with the authorization activities.
 */
@Component
public class AuthorizationHelper {

	@Autowired private SecurityEnforcer securityEnforcer;
	@Autowired private SecurityContextManager securityContextManager;
	@Autowired private RelationRegistry relationRegistry;

	public enum RequestedOperation {
		COMPLETE(ModelAuthorizationAction.COMPLETE_ALL_WORK_ITEMS, null),
		DELEGATE(ModelAuthorizationAction.DELEGATE_ALL_WORK_ITEMS, ModelAuthorizationAction.DELEGATE_OWN_WORK_ITEMS);

		ModelAuthorizationAction actionAll, actionOwn;
		RequestedOperation(ModelAuthorizationAction actionAll, ModelAuthorizationAction actionOwn) {
			this.actionAll = actionAll;
			this.actionOwn = actionOwn;
		}
	}

	public boolean isAuthorized(CaseWorkItemType workItem, RequestedOperation operation, Task task, OperationResult result) throws
			ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
			SecurityViolationException {
		MidPointPrincipal principal;
		try {
			principal = securityContextManager.getPrincipal();
		} catch (SecurityViolationException e) {
			return false;
		}
		if (principal.getOid() == null) {
			return false;
		}
		try {
			if (securityEnforcer.isAuthorized(operation.actionAll.getUrl(), null, AuthorizationParameters.EMPTY, null, task, result)) {
				return true;
			}
			if (operation.actionOwn != null && !securityEnforcer.isAuthorized(operation.actionOwn.getUrl(), null, AuthorizationParameters.EMPTY, null, task, result)) {
				return false;
			}
		} catch (SchemaException e) {
			throw new SystemException(e.getMessage(), e);
		}
		for (ObjectReferenceType assignee : workItem.getAssigneeRef()) {
			if (isEqualOrDeputyOf(principal, assignee.getOid(), relationRegistry)) {
				return true;
			}
		}
		return isAmongCandidates(principal, workItem);
	}

	private boolean isEqualOrDeputyOf(MidPointPrincipal principal, String eligibleUserOid,
			RelationRegistry relationRegistry) {
		return principal.getOid().equals(eligibleUserOid)
				|| DeputyUtils.isDelegationPresent(principal.getUser(), eligibleUserOid, relationRegistry);
	}

	// principal != null, principal.getOid() != null, principal.getUser() != null
	private boolean isAmongCandidates(MidPointPrincipal principal, CaseWorkItemType workItem) {
		for (ObjectReferenceType candidateRef : workItem.getCandidateRef()) {
			if (principal.getOid().equals(candidateRef.getOid())
					|| isMemberOrDeputyOf(principal.getUser(), candidateRef)) {
				return true;
			}
		}
		return false;
	}

	public boolean isAuthorizedToClaim(CaseWorkItemType workItem) {
		MidPointPrincipal principal;
		try {
			principal = securityContextManager.getPrincipal();
		} catch (SecurityViolationException e) {
			return false;
		}
		return principal.getOid() != null && isAmongCandidates(principal, workItem);
	}

	private boolean isMemberOrDeputyOf(UserType userType, ObjectReferenceType userOrRoleRef) {
		return userType.getRoleMembershipRef().stream().anyMatch(ref -> matches(userOrRoleRef, ref))
				|| userType.getDelegatedRef().stream().anyMatch(ref -> matches(userOrRoleRef, ref));
	}

	private boolean matches(ObjectReferenceType userOrRoleRef, ObjectReferenceType targetRef) {
		// TODO check also the reference target type (user vs. abstract role)
		return (relationRegistry.isMember(targetRef.getRelation()) || relationRegistry.isDelegation(targetRef.getRelation()))
				&& targetRef.getOid().equals(userOrRoleRef.getOid());
	}


}
