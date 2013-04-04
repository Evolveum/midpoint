/*
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

package com.evolveum.midpoint.web.page.admin.users.dto;

import java.io.Serializable;

import org.apache.wicket.RestartResponseException;

import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;

/**
 * @author mserbak
 */
public class SubmitResourceDto extends PageAdmin implements Serializable {
	private PrismObject account;
	private String name;
	private String resourceName;
	private boolean selected;
	private SynchronizationPolicyDecision syncPolicy;

	public SubmitResourceDto(PrismObject account, boolean selected) {
		this.account = account;
		this.selected = selected;
	}
	
	public SubmitResourceDto(AccountDto accountDto, boolean selected) {
		this(accountDto.getPrismAccount(), selected);
		this.syncPolicy = accountDto.getSyncPolicy();
	}

	public String getName() {
		return WebMiscUtil.getName(account) == null ? "unknown" : WebMiscUtil.getName(account);
		
	}

	public String getResourceName() {
		if (account == null) {
			return "unknown";
		}
		PrismReference reference = account.findReference(ShadowType.F_RESOURCE_REF);
		if (reference == null || reference.isEmpty()) {
            return "unknown";
        }
		if(reference.getValue().getObject() == null) {
			Task task = createSimpleTask("getResourceName: getResource");
			OperationResult result = new OperationResult("getResourceName: getResource");
			PrismObject<ResourceType> resource = null;
			try {
				resource = getModelService().getObject(ResourceType.class, reference.getValue().getOid(), null, task, result);
				return WebMiscUtil.getName(resource);
			} catch (Exception ex) {
				result.recordFatalError("Unable to get resource object", ex);
				showResultInSession(result);
				throw new RestartResponseException(PageUsers.class);
			}
			
		}
		return WebMiscUtil.getName(reference.getValue().getObject());
	}

	public boolean isSelected() {
		return selected;
	}
	
	public String getSyncPolicy() {
		if(WebMiscUtil.getName(account) != null) {
			if(syncPolicy == null) {
				return getString("SynchronizationPolicyDecision.KEEP");
			}
			return getString("SynchronizationPolicyDecision." + syncPolicy.name());
		}
		return getString("SynchronizationPolicyDecision.ADD");
	}
}
