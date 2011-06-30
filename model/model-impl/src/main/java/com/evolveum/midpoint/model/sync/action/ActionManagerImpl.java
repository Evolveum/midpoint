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
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.model.sync.action;

import java.util.Map;

import com.evolveum.midpoint.api.logging.LoggingUtils;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.model.xpath.SchemaHandling;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.xml.ns._public.model.model_1.ModelPortType;

/**
 * 
 * @author Vilo Repan
 */
public class ActionManagerImpl<T extends Action> implements ActionManager<T> {

	private static transient Trace trace = TraceManager.getTrace(ActionManagerImpl.class);
	private Map<String, Class<T>> actionMap;
	private ModelPortType model;
	private ProvisioningService provisioning;
	private RepositoryService repository;
	private SchemaHandling schemaHandling;

	@Override
	public void setActionMapping(Map<String, Class<T>> actionMap) {
		this.actionMap = actionMap;
	}

	@Override
	public Action getActionInstance(String uri) {
		Class<T> clazz = actionMap.get(uri);
		if (clazz == null) {
			return null;
		}

		Action action = null;
		try {
			action = clazz.newInstance();
			((BaseAction) action).setModel(model);
			((BaseAction) action).setProvisioning((ProvisioningService) provisioning);
			((BaseAction) action).setSchemaHandling(schemaHandling);
			((BaseAction) action).setRepository(repository);
		} catch (Exception ex) {
			LoggingUtils.logException(trace, "Couln't create action instance", ex);
		}

		return action;
	}

	public void setModel(ModelPortType model) {
		this.model = model;
	}

	public void setProvisioning(ProvisioningService provisioning) {
		this.provisioning = provisioning;
	}

	public void setSchemaHandling(SchemaHandling schemaHandling) {
		this.schemaHandling = schemaHandling;
	}

	public void setRepository(RepositoryService repository) {
		this.repository = repository;
	}
}
