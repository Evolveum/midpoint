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
