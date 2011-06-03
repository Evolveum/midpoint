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

package com.evolveum.midpoint.model.action;

import java.util.ArrayList;
import java.util.List;

import javax.xml.ws.Holder;

import com.evolveum.midpoint.model.ModelService;
import com.evolveum.midpoint.model.SynchronizationException;
import com.evolveum.midpoint.model.xpath.SchemaHandling;
import com.evolveum.midpoint.provisioning.service.ProvisioningService;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.FaultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.ObjectNotFoundFaultType;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;

/**
 * 
 * @author Vilo Repan
 */
public abstract class BaseAction implements Action {

	private ModelService model;
	private ProvisioningService provisioning;
	private RepositoryPortType repository;
	private SchemaHandling schemaHandling;
	private List<Object> parameters;

	@Override
	public List<Object> getParameters() {
		if (parameters == null) {
			parameters = new ArrayList<Object>();
		}
		return parameters;
	}

	@Override
	public void setParameters(List<Object> parameters) {
		this.parameters = parameters;
	}

	protected UserType getUser(String oid, OperationResultType resultType) throws SynchronizationException {
		if (oid == null) {
			return null;
		}

		try {
			ObjectType object = model.getObject(oid, new PropertyReferenceListType(),
					new Holder<OperationResultType>(resultType));
			if (object == null) {
				return null;
			}
			if (!(object instanceof UserType)) {
				throw new SynchronizationException("Returned object is null or not type of "
						+ UserType.class.getName() + ".");
			}
			return (UserType) object;
		} catch (com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage ex) {
			FaultType info = ex.getFaultInfo();
			if (info == null || !(info instanceof ObjectNotFoundFaultType)) {
				throw new SynchronizationException("Can't get user. Unknown error occured.", ex,
						ex.getFaultInfo());
			}
		}

		return null;
	}

	public void setModel(ModelService model) {
		this.model = model;
	}

	protected ModelService getModel() {
		return model;
	}

	public ProvisioningService getProvisioning() {
		return provisioning;
	}

	public void setProvisioning(ProvisioningService provisioning) {
		this.provisioning = provisioning;
	}

	public RepositoryPortType getRepository() {
		return repository;
	}

	public void setRepository(RepositoryPortType repository) {
		this.repository = repository;
	}

	public SchemaHandling getSchemaHandling() {
		return schemaHandling;
	}

	public void setSchemaHandling(SchemaHandling schemaHandling) {
		this.schemaHandling = schemaHandling;
	}

	protected ScriptsType getScripts(ResourceType resource) {
		ScriptsType scripts = resource.getScripts();
		if (scripts == null) {
			scripts = new ScriptsType();
		}

		return scripts;
	}
}
